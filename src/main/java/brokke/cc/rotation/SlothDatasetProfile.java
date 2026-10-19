package brokke.cc.rotation;

import java.util.Random;
import net.minecraft.util.math.MathHelper;

public final class SlothDatasetProfile {
   private static final int LEGIT_ROWS = 2645;
   private static final int LEGIT_GROUPS = 3;
   private static final float YAW_DELTA_MAX_MEDIAN = 22.0488F;
   private static final float YAW_DELTA_MAX_Q3 = 37.3496F;
   private static final float PITCH_DELTA_MAX_MEDIAN = 3.3F;
   private static final float PITCH_DELTA_MAX_Q3 = 6.45F;
   private static final float YAW_ACCEL_MEDIAN = 2.9684F;
   private static final float PITCH_ACCEL_MEDIAN = 0.6671F;
   private static final float YAW_JERK_MEDIAN = 5.4489F;
   private static final float PITCH_JERK_MEDIAN = 1.3854F;
   private static final float YAW_AUTOCORRELATION = 0.7387F;
   private static final float PITCH_AUTOCORRELATION = 0.6763F;
   private static final float YAW_PITCH_CORRELATION = 0.4618F;
   private static final float SNAP_RATIO_Q3 = 0.0F;
   private static final float CHEAT_YAW_AUTOCORRELATION = 0.4101F;
   private static final float CHEAT_PITCH_AUTOCORRELATION = 0.7136F;
   private static final float CHEAT_PITCH_JERK_MEDIAN = 7.61F;
   private static final float CHEAT_YAW_ENTROPY = 3.4415F;
   private static final float LEGIT_YAW_ENTROPY = 3.4295F;
   private static final long[] TRACK_DURATIONS = new long[]{188L, 204L, 221L, 238L, 254L, 272L, 291L, 314L};
   private static final int[] TRACK_WEIGHTS = new int[]{9, 17, 25, 31, 29, 22, 13, 6};
   private static final long[] ACTION_DURATIONS = new long[]{158L, 174L, 190L, 207L, 225L, 244L, 265L, 288L};
   private static final int[] ACTION_WEIGHTS = new int[]{6, 13, 24, 31, 29, 22, 12, 5};
   private static final long[] JUMP_DURATIONS = new long[]{176L, 194L, 213L, 233L, 254L, 276L, 299L, 324L};
   private static final int[] JUMP_WEIGHTS = new int[]{7, 16, 26, 31, 28, 19, 10, 4};
   private static final double[] LATERAL_AMPLITUDES = new double[]{-0.052, 0.034, -0.026, 0.061, -0.038, 0.022, -0.071, 0.045};
   private static final int[] MOUSE_REPORT_INTERVALS_MICROS = new int[]{
      14500, 15600, 16500, 17400, 18200, 19000, 19750, 20350, 21000, 21800, 22700, 23800, 25100, 26700, 28600, 31200
   };
   private static final float RESPONSE_BOOST = 1.2F;

   private SlothDatasetProfile() {
   }

   public static int a(Random var0, boolean var1) {
      return a(var0, var1 ? ACTION_WEIGHTS : TRACK_WEIGHTS);
   }

   static int a(Random var0) {
      return a(var0, JUMP_WEIGHTS);
   }

   public static long a(boolean var0, int var1) {
      long var2 = var0 ? ACTION_DURATIONS[var1] : TRACK_DURATIONS[var1];
      return Math.round((float)var2 * a(c(var1, var0 ? 0.41 : 0.23), 0.955F, 1.045F));
   }

   static long a(int var0) {
      return Math.round((float)JUMP_DURATIONS[var0] * a(c(var0, 0.67), 0.96F, 1.04F));
   }

   public static double a(boolean var0, int var1, double var2) {
      double var4 = a(var2);
      double var6 = 0.14564998F;
      double var8 = var0 ? 1.64 + var6 * 0.45 + var1 * 0.045 : 1.78 + var6 * 0.45 + var1 * 0.04;
      double var10 = var0 ? 1.82 + var6 * 0.45 - var1 * 0.035 : 1.96 + var6 * 0.45 - var1 * 0.04;
      double var12 = a(var4, var8, var10);
      if (var1 == 6 && var4 > 0.66) {
         var12 += Math.sin((var4 - 0.66) / 0.34 * Math.PI) * 0.0 * 0.58;
      }

      return var12;
   }

   public static double b(boolean var0, int var1, double var2) {
      double var4 = a(var2);
      double var6 = Math.sin(Math.PI * var4);
      double var8 = 0.31402400612831116 + (var0 ? 0.12 : 0.0);
      return LATERAL_AMPLITUDES[var1] * var6 * (0.72 + var8 * (1.0 - var4));
   }

   static double a(int var0, double var1) {
      double var3 = a(var1);
      double var5 = 1.68 + var0 * 0.055;
      double var7 = 1.82 - var0 * 0.025;
      return a(var3, var5, var7);
   }

   static double b(int var0, double var1) {
      double var3 = a(var1);
      double var5 = Math.sin(Math.PI * var3);
      return LATERAL_AMPLITUDES[(var0 + 3) % LATERAL_AMPLITUDES.length] * var5 * (0.9 + 0.16 * Math.sin((Math.PI * 2) * var3));
   }

   static int b(Random var0) {
      return MOUSE_REPORT_INTERVALS_MICROS[var0.nextInt(MOUSE_REPORT_INTERVALS_MICROS.length)];
   }

   static float c(Random var0) {
      return a(var0, 0.91F, 1.09F);
   }

   static float d(Random var0) {
      return a(var0, 0.84F, 1.18F);
   }

   public static float e(Random var0) {
      return var0.nextFloat() < 0.15F ? a(var0, 746.99207F, 700.0F) : a(var0, 440.97598F, 746.99207F);
   }

   public static float f(Random var0) {
      return a(var0, 66.0F, 129.0F);
   }

   public static float g(Random var0) {
      return a(var0, 1691.988F, 2404.404F) * 1.2F;
   }

   public static float h(Random var0) {
      return a(var0, 400.26F, 600.39F) * 1.2F;
   }

   public static float i(Random var0) {
      return a(var0, 2196.616F, 2998.084F) * 1.2F;
   }

   public static float j(Random var0) {
      return a(var0, 510.3315F, 763.8295F) * 1.2F;
   }

   public static float k(Random var0) {
      return a(var0, 36507.633F, 52854.332F);
   }

   public static float l(Random var0) {
      float var1 = MathHelper.clamp(0.18204994F, 0.5F, 1.0F);
      return a(var0, 7065.5405F, 1.3854F * (6900.0F + 1100.0F * var1));
   }

   static float m(Random var0) {
      return a(var0, 0.82F, 1.08F);
   }

   static float n(Random var0) {
      return a(var0, -0.08F, 0.035F);
   }

   static long a(Random var0, float var1, boolean var2) {
      if (!var2 && !(var1 < 6.0F)) {
         float var3 = 0.7075F;
         float var4 = 8.0F + var1 * (0.2F - var3 * 0.04F);
         float var5 = (float)var0.nextGaussian() * 2.5F;
         return Math.round(MathHelper.clamp(var4 + var5, 6.0F, 42.0F));
      } else {
         return Math.round(a(var0, 0.0F, 5.0F));
      }
   }

   public static float o(Random var0) {
      float var1 = 0.017F * (1.0F - a() * 0.35F);
      return a(var0, 0.051F - var1, 0.051F + var1);
   }

   static float b(Random var0, boolean var1) {
      float var2 = -0.0119998455F;
      float var3 = var2 * 0.018F;
      return var1 ? a(var0, 0.024F, 0.052F + var3 * 0.35F) : a(var0, 0.044F, 0.096F + var3);
   }

   static float a() {
      return MathHelper.clamp(0.1F * Math.min(1.0F, 0.330625F), 0.0F, 1.0F);
   }

   private static int a(Random var0, int[] var1) {
      int var2 = 0;

      for (int var6 : var1) {
         var2 += var6;
      }

      int var7 = var0.nextInt(var2);

      for (int var8 = 0; var8 < var1.length; var8++) {
         var7 -= var1[var8];
         if (var7 < 0) {
            return var8;
         }
      }

      return var1.length - 1;
   }

   private static float a(Random var0, float var1, float var2) {
      float var3 = (var0.nextFloat() + var0.nextFloat() + var0.nextFloat()) / 3.0F;
      return var1 + (var2 - var1) * var3;
   }

   private static float a(double var0, float var2, float var3) {
      double var4 = var0 - Math.floor(var0);
      return var2 + (var3 - var2) * (float)var4;
   }

   private static double c(int var0, double var1) {
      return Math.sin((var0 + 1.0) * 12.9898 + var1 * 78.233) * 43758.5453;
   }

   private static double a(double var0, double var2, double var4) {
      if (var0 <= 0.0) {
         return 0.0;
      }

      if (var0 >= 1.0) {
         return 1.0;
      }

      double var6 = Math.pow(var0, var2);
      double var8 = Math.pow(1.0 - var0, var4);
      return var6 / (var6 + var8);
   }

   private static double a(double var0) {
      return Math.max(0.0, Math.min(1.0, var0));
   }
}
