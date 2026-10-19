package brokke.cc.rotation;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * SpaceTime: однослоевой GRU (17 -> 32) + MDN-голова (19 = 1 freeze + 3 компоненты * 6).
 * Веса читаются из spacetime.bin (big-endian).
 *
 * Формат spacetime.bin:
 *   int hidden, int mixtures, int freezeCut, float yawLimit, float pitchLimit
 *   float[101] errorQuantiles
 *   float[96][17] wi, float[96][32] wh, float[96] bi, float[96] bh
 *   float[19][32] outW, float[19] outB
 */
public final class SpaceTimeModel {
   public static final int FEATURES = 17;
   public static final int HIDDEN = 32;
   public static final int MIXTURES = 3;
   public static final int OUT = 1 + 6 * MIXTURES;
   public static final float ATTACK_WINDOW = 20.0F;
   public static final float RHO_CLAMP = 0.95F;

   public final int freezeCut;
   public final float yawLimit;
   public final float pitchLimit;
   private final float[] errorCurve;
   private final float[][] wi;
   private final float[][] wh;
   private final float[] bi;
   private final float[] bh;
   private final float[][] outW;
   private final float[] outB;

   private SpaceTimeModel(DataInputStream in) throws Exception {
      int hidden = in.readInt();
      int mixtures = in.readInt();
      if (hidden != HIDDEN || mixtures != MIXTURES) {
         throw new IllegalStateException("bad spacetime.bin: hidden=" + hidden + " mixtures=" + mixtures);
      }

      this.freezeCut = in.readInt();
      this.yawLimit = in.readFloat();
      this.pitchLimit = in.readFloat();
      this.errorCurve = readVec(in);
      this.wi = readMat(in);
      this.wh = readMat(in);
      this.bi = readVec(in);
      this.bh = readVec(in);
      this.outW = readMat(in);
      this.outB = readVec(in);
   }

   public static SpaceTimeModel load(String resource) {
      try (InputStream is = SpaceTimeModel.class.getClassLoader().getResourceAsStream(resource)) {
         if (is == null) {
            return null;
         }

         return load(is);
      } catch (Exception var3) {
         return null;
      }
   }

   /** Загрузка обученных весов из файла (run/brokke/neuro/spacetime.bin). */
   public static SpaceTimeModel load(java.nio.file.Path path) {
      try (InputStream is = java.nio.file.Files.newInputStream(path)) {
         return load(is);
      } catch (Exception var3) {
         return null;
      }
   }

   public static SpaceTimeModel load(InputStream input) {
      try (DataInputStream in = new DataInputStream(input)) {
         return new SpaceTimeModel(in);
      } catch (Exception var3) {
         return null;
      }
   }

   private static float[] readVec(DataInputStream in) throws Exception {
      float[] v = new float[in.readInt()];

      for (int i = 0; i < v.length; i++) {
         v[i] = in.readFloat();
      }

      return v;
   }

   private static float[][] readMat(DataInputStream in) throws Exception {
      int rows = in.readInt();
      int cols = in.readInt();
      float[][] m = new float[rows][cols];

      for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
            m[r][c] = in.readFloat();
         }
      }

      return m;
   }

   public static float squash(float x) {
      return (float) (Math.log(x + Math.sqrt(x * x + 1.0)) / 3.0);
   }

   public static float sigmoid(float x) {
      return 1.0F / (1.0F + (float) Math.exp(-x));
   }

   /** Сколько входных фич ждёт модель: 17 у старой (без jump), 18 у обученной в клиенте. */
   public int features() {
      return this.wi[0].length;
   }

   public float[] newHidden() {
      return new float[HIDDEN];
   }

   /** Один GRU-шаг (порядок гейтов r, z, n — как в PyTorch GRU). */
   public float[] gruStep(float[] x, float[] h) {
      float[] preI = new float[3 * HIDDEN];
      float[] preH = new float[3 * HIDDEN];

      for (int i = 0; i < 3 * HIDDEN; i++) {
         float a = this.bi[i];
         float b = this.bh[i];
         float[] wr = this.wi[i];
         float[] hr = this.wh[i];

         for (int j = 0; j < wr.length; j++) {
            a += wr[j] * x[j];
         }

         for (int j = 0; j < hr.length; j++) {
            b += hr[j] * h[j];
         }

         preI[i] = a;
         preH[i] = b;
      }

      float[] out = new float[HIDDEN];

      for (int i = 0; i < HIDDEN; i++) {
         float r = sigmoid(preI[i] + preH[i]);
         float z = sigmoid(preI[HIDDEN + i] + preH[HIDDEN + i]);
         float n = (float) Math.tanh(preI[2 * HIDDEN + i] + r * preH[2 * HIDDEN + i]);
         out[i] = (1.0F - z) * n + z * h[i];
      }

      return out;
   }

   public float[] head(float[] h) {
      float[] o = new float[OUT];

      for (int i = 0; i < OUT; i++) {
         float s = this.outB[i];
         float[] w = this.outW[i];

         for (int j = 0; j < HIDDEN; j++) {
            s += w[j] * h[j];
         }

         o[i] = s;
      }

      return o;
   }

   /** Выбор компоненты k по log-весам o[1 + 6k] (Gumbel-max). */
   public int sampleMixture(float[] o, float uniform) {
      float max = o[1];

      for (int k = 1; k < MIXTURES; k++) {
         max = Math.max(max, o[1 + 6 * k]);
      }

      float sum = 0.0F;

      for (int k = 0; k < MIXTURES; k++) {
         sum += (float) Math.exp(o[1 + 6 * k] - max);
      }

      float t = sum * Math.max(uniform, 1.0E-6F);

      for (int k = 0; k < MIXTURES; k++) {
         t -= (float) Math.exp(o[1 + 6 * k] - max);
         if (t <= 0.0F) {
            return k;
         }
      }

      return MIXTURES - 1;
   }

   /**
    * Сэмпл одной оси из компоненты k:
    * mean = clamp(o[1+6k+meanIdx], -4, 4) (asinh-пространство),
    * sigma = exp(clamp(o[1+6k+sigmaIdx], -4, 1.5)),
    * value = sinh(mean + sigma * z), clamp по yaw/pitch-лимиту.
    */
   public float sampleAxis(float[] o, int k, int meanIdx, int sigmaIdx, float z) {
      float mean = Math.max(-4.0F, Math.min(4.0F, o[1 + 6 * k + meanIdx]));
      float sigma = (float) Math.exp(Math.max(-4.0F, Math.min(1.5F, o[1 + 6 * k + sigmaIdx])));
      float lim = meanIdx == 1 ? this.yawLimit : this.pitchLimit;
      float v = (float) Math.sinh(mean + sigma * z);
      return Math.max(-lim, Math.min(lim, v));
   }

   /** rho = tanh(o[1+6k+5]) * 0.95 — корреляция двух осей. */
   public float rho(int k, float[] o) {
      return (float) Math.tanh(o[1 + 6 * k + 5]) * RHO_CLAMP;
   }

   /** error-curve: линейная интерполяция квантилей (адаптивная сетка квантования). */
   public float errorQuantile(float q) {
      if (this.errorCurve.length == 0) {
         return 0.0F;
      }

      q = Math.max(0.0F, Math.min(1.0F, q));
      float pos = q * (this.errorCurve.length - 1);
      int i = (int) pos;
      if (i >= this.errorCurve.length - 1) {
         return this.errorCurve[this.errorCurve.length - 1];
      }

      return this.errorCurve[i] + (this.errorCurve[i + 1] - this.errorCurve[i]) * (pos - i);
   }
}
