package brokke.cc.rotation.testnerv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

/**
 * Точный перенос сети режима TestNerv из Opus (формат {@code neurotest-bc-camera-v1}).
 *
 * Это не эвристика и не упрощение: те же признаки, тот же GRU (2 слоя по 256), те же головы
 * yaw/pitch, та же дискретизация шага по сетке сенсы, тот же сэмплер с температурой, те же
 * ветки пределов/предохранителя/дискриминатора хода и та же проверка эталона при загрузке.
 * Модель читается из того же файла байт-в-байт ({@code bc_camera_testnerv.json}).
 */
public final class TestNervNet {
   public static final String FORMAT = "neurotest-bc-camera-v1";
   public static final double REFERENCE_FEATURE_TOLERANCE = 1.0E-4;
   public static final double REFERENCE_LOGIT_TOLERANCE = 0.002;
   public static final double PLAYER_HEIGHT_STANDING = 1.8;
   public static final double PLAYER_HEIGHT_SNEAKING = 1.5;
   public static final double PLAYER_EYE_STANDING = 1.62;
   public static final double PLAYER_EYE_SNEAKING = 1.27;

   public int featureCount;
   public int hiddenSize;
   public int layers;
   public int embeddingSize;
   public int binsPerSide;
   public int classCount;
   public int clip;
   public int windowSize;
   public boolean keysNow;
   public boolean intent;
   public int[] droppedFeatures = new int[0];
   public volatile double holdRatio = -1.0;
   public double[] intentErrorEdges;
   public double[] intentErrorProbabilities;
   public double intentErrorScale;
   public double guardMargin;
   public double guardFrontAngle = 45.0;
   public boolean guardByMask;
   public double[][] subBinTable;
   public LimitTable limitTable;
   public double temperature = 1.0;
   public double[] binEdges;
   public double[][] inputWeights;
   public double[] inputBias;
   public double[] embedding;
   public double[][][] recurrentInputWeights;
   public double[][][] recurrentHiddenWeights;
   public double[][] recurrentInputBias;
   public double[][] recurrentHiddenBias;
   public double[][] yawHiddenWeights;
   public double[][] yawOutputWeights;
   public double[][] pitchHiddenWeights;
   public double[][] pitchOutputWeights;
   public double[] yawHiddenBias;
   public double[] yawOutputBias;
   public double[] pitchHiddenBias;
   public double[] pitchOutputBias;
   public double[][] yawEmbedding;
   public String status = "";
   public String source = "?";
   public Guide[] guides = new Guide[0];
   public int guideSampleCount;
   public double guideWeight;

   /** Один признаковый ряд: состояние игрока, цели и предыдущего поворота на тике. */
   public static final class Tick {
      public double positionX = Double.NaN;
      public double positionY = Double.NaN;
      public double positionZ = Double.NaN;
      public int keys;
      public boolean onGround;
      public boolean horizontalCollision;
      public boolean attacked;
      public double targetX = Double.NaN;
      public double targetY = Double.NaN;
      public double targetZ = Double.NaN;
      public int targetId = -1;
      public int targetCandidates;
      public boolean targetSneaking;
      public double yaw;
      public double pitch;
      public double deltaYaw;
      public double deltaPitch;
      public boolean rotationValid = true;
   }

   /**
    * Состояние эпизода: кольцевой буфер последних тиков, скрытое состояние GRU и накопители
    * остатка сетки. Живёт столько же, сколько мир.
    */
   public final class State {
      public final Tick[] ticks = new Tick[TestNervNet.this.windowSize];
      public int tickCount;
      public double[][] hidden = new double[TestNervNet.this.layers][TestNervNet.this.hiddenSize];
      public double accumulatedYaw;
      public double accumulatedPitch;
      public final double[][] guideState = new double[TestNervNet.this.guides.length][];
      public double[] limitReference;
      public double[] yawHistory;
      public double[] pitchHistory;
      public boolean[] validHistory;
      public int historyCount;
      public int acceptedCount;
      public int clampedCount;

      public State() {
         for (int i = 0; i < this.guideState.length; i++) {
            this.guideState[i] = new double[TestNervNet.this.guides[i].hiddenSize];
         }
      }

      public void push(Tick tick) {
         this.ticks[this.tickCount % TestNervNet.this.windowSize] = tick;
         this.tickCount++;
      }

      public Tick tick(int back) {
         return this.ticks[(this.tickCount - 1 - back) % TestNervNet.this.windowSize];
      }

      public int size() {
         return Math.min(this.tickCount, TestNervNet.this.windowSize);
      }

      public void resetAccumulators() {
         this.accumulatedYaw = 0.0;
         this.accumulatedPitch = 0.0;
      }
   }

   /** Пределы людей из модели: удерживают шаг в человеческом диапазоне. */
   public static final class LimitTable {
      public double[][] players;
      public double hugeYaw;
      public double hugePitch;
      public double flipYaw;
      public double flipPitch;
      public double bigProbability = 40.0;
      public double flipProbability = 15.0;
      public int historyLength = 200;
      public long accepted;
      public long clamped;

      /**
       * Точный перенос расчёта пределов: последние шаги, накопленный уход за 3/19 тиков,
       * частота «огромных» шагов за 200 тиков и частота разворотов на 1–3 тика.
       */
      public double[] selectBounds(
         double[] yawHistory,
         double[] pitchHistory,
         boolean[] validHistory,
      int length,
      double[] reference,
      double bigSample,
      double flipSample
      ) {
         double[] yaw = new double[length];
         double[] pitch = new double[length];
         for (int i = 0; i < length; i++) {
            yaw[i] = validHistory[i] ? yawHistory[i] : 0.0;
            pitch[i] = validHistory[i] ? pitchHistory[i] : 0.0;
         }

         double lastYaw = length > 0 ? yaw[length - 1] : 0.0;
         double lastPitch = length > 0 ? pitch[length - 1] : 0.0;
         double minYaw = Math.max(-reference[0], lastYaw - reference[1]);
         double maxYaw = Math.min(reference[0], lastYaw + reference[1]);
         double recentYaw = 0.0;
         double absoluteYaw = 0.0;

         for (int i = Math.max(0, length - 3); i < length; i++) {
            recentYaw += yaw[i];
         }

         for (int i = Math.max(0, length - 19); i < length; i++) {
            absoluteYaw += Math.abs(yaw[i]);
         }

         minYaw = Math.max(minYaw, -reference[2] - recentYaw);
         maxYaw = Math.min(maxYaw, reference[2] - recentYaw);
         double allowedDrift = Math.max(20.0 * reference[3] - absoluteYaw, 0.0);
         minYaw = Math.max(minYaw, -allowedDrift);
         maxYaw = Math.min(maxYaw, allowedDrift);
         int start = Math.max(0, length - 199);
         int hugeCount = 0;
         int flipCount = 0;

         for (int i = start; i < length; i++) {
            if (validHistory[i] && Math.abs(yaw[i]) > this.bigProbability) {
               hugeCount++;
            }

            boolean flipped = false;

            for (int back = 1; back <= 3 && i - back >= start; back++) {
               int previous = i - back;
               if (Math.abs(yaw[i]) > this.flipProbability
                  && Math.abs(yaw[previous]) > this.flipProbability
                  && Math.signum(yaw[i]) != Math.signum(yaw[previous])
                  && validHistory[i]
                  && validHistory[previous]) {
                  flipped = true;
               }
            }

            if (flipped) {
               flipCount++;
            }
         }

         double hugeRatio = Math.max(
            0.0, Math.min(1.0, (this.hugePitch - hugeCount) / Math.max(this.hugePitch - this.hugeYaw, 1.0E-9))
         );
         boolean clampHuge = bigSample > (hugeCount >= this.hugeYaw ? hugeRatio : 1.0);

         if (clampHuge) {
            minYaw = Math.max(minYaw, -this.bigProbability);
            maxYaw = Math.min(maxYaw, this.bigProbability);
         }

         double flipRatio = Math.max(
            0.0, Math.min(1.0, (this.flipPitch - flipCount) / Math.max(this.flipPitch - this.flipYaw, 1.0E-9))
         );
         boolean clampFlip = flipSample > (flipCount >= this.flipYaw ? flipRatio : 1.0);
         double lastSign = 0.0;

         for (int back = 0; back < Math.min(3, length); back++) {
            double value = yaw[length - 1 - back];
            if (lastSign == 0.0 && Math.abs(value) > this.flipProbability) {
               lastSign = Math.signum(value);
            }
         }

         if (clampFlip && lastSign > 0.0) {
            minYaw = Math.max(minYaw, -this.flipProbability);
         }

         if (clampFlip && lastSign < 0.0) {
            maxYaw = Math.min(maxYaw, this.flipProbability);
         }

         double minPitch = Math.max(-reference[4], lastPitch - reference[5]);
         double maxPitch = Math.min(reference[4], lastPitch + reference[5]);

         if (minYaw > maxYaw) {
            minYaw = maxYaw = (minYaw + maxYaw) / 2.0;
         }

         if (minPitch > maxPitch) {
            minPitch = maxPitch = (minPitch + maxPitch) / 2.0;
         }

         return new double[]{minYaw, maxYaw, minPitch, maxPitch};
      }
   }

   /** Дискриминатор хода из модели (в этом файле модели его нет, но ветка обязана работать). */
   public static final class Guide {
      public int[] columns;
      public int hiddenSize;
      public double[][] inputWeights;
      public double[] inputBias;
      public double[][] recurrentInputWeights;
      public double[][] recurrentHiddenWeights;
      public double[] recurrentInputBias;
      public double[] recurrentHiddenBias;
      public double[] outputWeights;
      public double outputBias;

      public double[] encode(double[] features) {
         double[] selected = new double[this.columns.length];

         for (int i = 0; i < this.columns.length; i++) {
            selected[i] = features[this.columns[i]];
         }

         double[] result = TestNervNet.linear(this.inputWeights, this.inputBias, selected);

         for (int i = 0; i < result.length; i++) {
            result[i] = TestNervNet.softsign(result[i]);
         }

         return result;
      }

      public double[] step(double[] previous, double[] encoded) {
         double[] fromInput = TestNervNet.linear(this.recurrentInputWeights, this.recurrentInputBias, encoded);
         double[] fromHidden = TestNervNet.linear(this.recurrentHiddenWeights, this.recurrentHiddenBias, previous);
         double[] next = new double[this.hiddenSize];

         for (int i = 0; i < this.hiddenSize; i++) {
            double resetGate = TestNervNet.sigmoid(fromInput[i] + fromHidden[i]);
            double updateGate = TestNervNet.sigmoid(fromInput[this.hiddenSize + i] + fromHidden[this.hiddenSize + i]);
            double candidate = Math.tanh(fromInput[2 * this.hiddenSize + i] + resetGate * fromHidden[2 * this.hiddenSize + i]);
            next[i] = (1.0 - updateGate) * candidate + updateGate * previous[i];
         }

         return next;
      }

      public double score(double[] hidden) {
         double score = this.outputBias;

         for (int i = 0; i < this.hiddenSize; i++) {
            score += this.outputWeights[i] * hidden[i];
         }

         return score;
      }
   }

   /** Состояние игрока и цели на текущем тике, с которого строятся признаки. */
   public State newState() {
      return new State();
   }

   public int extraFeatureCount() {
      return (this.keysNow ? 7 : 0) + (this.intent ? 1 : 0);
   }

   public boolean hasGuide() {
      return this.guides.length > 0 && this.guideSampleCount > 1;
   }

   public boolean isLoaded() {
      return this.inputWeights != null;
   }

   public static double wrapDegrees(double value) {
      double wrapped = (value + 180.0) % 360.0;
      if (wrapped < 0.0) {
         wrapped += 360.0;
      }

      return wrapped - 180.0;
   }

   public static double asinh(double value) {
      double absolute = Math.abs(value);
      return Math.copySign(Math.log(absolute + Math.sqrt(absolute * absolute + 1.0)), value);
   }

   public static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public static double nanToZero(double value) {
      return Double.isNaN(value) ? 0.0 : value;
   }

   public static boolean allFinite(double... values) {
      for (double value : values) {
         if (Double.isNaN(value)) {
            return false;
         }
      }

      return true;
   }

   public static double softsign(double value) {
      return 0.5 * value * (1.0 + erf(value / Math.sqrt(2.0)));
   }

   public static double erf(double value) {
      double absolute = Math.abs(value);
      double t = 1.0 / (1.0 + 0.5 * absolute);
      double result = t
         * Math.exp(
            -absolute * absolute
               - 1.26551223
               + t
                  * (
                     1.00002368
                        + t
                           * (
                              0.37409196
                                 + t
                                    * (
                                       0.09678418
                                          + t
                                             * (
                                                -0.18628806
                                                   + t
                                                      * (
                                                         0.27886807
                                                            + t
                                                               * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))
                                                      )
                                             )
                                    )
                           )
                  )
         );
      return value >= 0.0 ? 1.0 - result : result - 1.0;
   }

   public static double sigmoid(double value) {
      return 1.0 / (1.0 + Math.exp(-value));
   }

   public static double[] linear(double[][] weights, double[] bias, double[] input) {
      double[] output = new double[weights.length];

      for (int row = 0; row < weights.length; row++) {
         double sum = bias[row];
         double[] weightsRow = weights[row];

         for (int column = 0; column < input.length; column++) {
            sum += weightsRow[column] * input[column];
         }

         output[row] = sum;
      }

      return output;
   }

   public static double[] direction(double x, double y, double z) {
      return new double[]{
         Math.toDegrees(Math.atan2(-x, z)),
         -Math.toDegrees(Math.atan2(y, Math.hypot(x, z)))
      };
   }

   public static double[] rotationVector(double yaw, double pitch) {
      double yawRadians = Math.toRadians(yaw);
      double pitchRadians = Math.toRadians(pitch);
      return new double[]{
         -Math.sin(yawRadians) * Math.cos(pitchRadians),
         -Math.sin(pitchRadians),
         Math.cos(yawRadians) * Math.cos(pitchRadians)
      };
   }

   public static double[] playerEyePosition(Tick tick) {
      boolean sneaking = (tick.keys >> 5 & 1) != 0;
      return new double[]{tick.positionX, tick.positionY + (sneaking ? PLAYER_EYE_SNEAKING : PLAYER_EYE_STANDING), tick.positionZ};
   }

   public static double[] targetOffset(Tick tick, boolean valid) {
      if (!valid) {
         return new double[]{0.0, 0.0, 1.0};
      }

      double height = tick.targetSneaking ? PLAYER_HEIGHT_SNEAKING : PLAYER_HEIGHT_STANDING;
      double[] eye = playerEyePosition(tick);
      return new double[]{
         tick.targetX - eye[0],
         tick.targetY + height / 2.0 - eye[1],
         tick.targetZ - eye[2]
      };
   }

   /** Пересечение луча взгляда с коробкой цели (признак видимости). */
   public static boolean hasLineOfSight(double[] eye, double[] direction, double[] target, double height) {
      double[] minimum = new double[]{target[0] - 0.3, target[1], target[2] - 0.3};
      double[] maximum = new double[]{target[0] + 0.3, target[1] + height, target[2] + 0.3};
      double enter = Double.NEGATIVE_INFINITY;
      double exit = Double.POSITIVE_INFINITY;
      boolean hasEnter = false;
      boolean hasExit = false;

      for (int axis = 0; axis < 3; axis++) {
         double inverse = 1.0 / direction[axis];
         double near = (minimum[axis] - eye[axis]) * inverse;
         double far = (maximum[axis] - eye[axis]) * inverse;
         double lower = !Double.isNaN(near) && !Double.isNaN(far) ? Math.min(near, far) : Double.NaN;
         double upper = !Double.isNaN(near) && !Double.isNaN(far) ? Math.max(near, far) : Double.NaN;

         if (!Double.isNaN(lower)) {
            enter = hasEnter ? Math.max(enter, lower) : lower;
            hasEnter = true;
         }

         if (!Double.isNaN(upper)) {
            exit = hasExit ? Math.min(exit, upper) : upper;
            hasExit = true;
         }
      }

      return hasEnter && hasExit ? exit >= Math.max(enter, 0.0) && enter < 6.0 : false;
   }

   public double normalizeRunLength(double runLength) {
      return Math.log1p(runLength) / Math.log1p(this.clip);
   }

   public double runLength(boolean[] flags) {
      int last = flags.length - 1;

      for (int index = last; index >= 0; index--) {
         if (flags[index]) {
            return Math.min(last - index, this.clip);
         }
      }

      return this.clip;
   }

   /**
    * Полный набор признаков текущего тика (42 числа): поворот, ввод, цель, геометрия,
    * видимость, скорости и счётчики атак/переключений.
    */
   public double[] buildFeatures(State state, double grid) {
      int size = state.size();
      Tick[] ticks = new Tick[size];

      for (int index = 0; index < size; index++) {
         ticks[index] = state.tick(size - 1 - index);
      }

      double[] features = new double[this.featureCount];
      Tick last = ticks[size - 1];
      double deltaYaw = last.rotationValid ? last.deltaYaw : 0.0;
      double deltaPitch = last.rotationValid ? last.deltaPitch : 0.0;
      features[0] = clamp(deltaYaw / 10.0, -6.0, 6.0);
      features[1] = asinh(deltaYaw / 0.5) / 4.0;
      features[2] = clamp(deltaPitch / 10.0, -6.0, 6.0);
      features[3] = asinh(deltaPitch / 0.5) / 4.0;
      features[4] = last.rotationValid ? 1.0 : 0.0;
      features[5] = last.pitch / 90.0;
      features[6] = Math.log(grid / 0.15);
      boolean[] idle = new boolean[size];
      boolean[] moving = new boolean[size];

      for (int index = 0; index < size; index++) {
         double tickYaw = ticks[index].rotationValid ? ticks[index].deltaYaw : 0.0;
         double tickPitch = ticks[index].rotationValid ? ticks[index].deltaPitch : 0.0;
         moving[index] = tickYaw != 0.0 || tickPitch != 0.0;
         idle[index] = !moving[index];
      }

      features[7] = this.normalizeRunLength(this.runLength(idle));
      features[8] = this.normalizeRunLength(this.runLength(moving));
      features[9] = last.onGround ? 1.0 : 0.0;
      features[10] = last.horizontalCollision ? 1.0 : 0.0;

      for (int bit = 0; bit < 7; bit++) {
         features[11 + bit] = last.keys >> bit & 1;
      }

      double yawRadians = Math.toRadians(last.yaw);
      double[] forward = new double[]{-Math.sin(yawRadians), 0.0, Math.cos(yawRadians)};
      double[] right = new double[]{-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians)};
      Tick previous = size > 1 ? ticks[size - 2] : null;
      double motionX = previous == null ? 0.0 : nanToZero(last.positionX - previous.positionX);
      double motionY = previous == null ? 0.0 : nanToZero(last.positionY - previous.positionY);
      double motionZ = previous == null ? 0.0 : nanToZero(last.positionZ - previous.positionZ);
      features[18] = clamp((motionX * forward[0] + motionZ * forward[2]) / 0.3, -3.0, 3.0);
      features[19] = clamp((motionX * right[0] + motionZ * right[2]) / 0.3, -3.0, 3.0);
      features[20] = clamp(motionY / 0.3, -3.0, 3.0);
      boolean[] targetValid = new boolean[size];
      double[] targetYaw = new double[size];
      double[] targetPitch = new double[size];

      for (int index = 0; index < size; index++) {
         Tick tick = ticks[index];
         targetValid[index] = tick.targetId >= 0
            && allFinite(tick.targetX, tick.targetY, tick.targetZ)
            && allFinite(tick.positionX, tick.positionY, tick.positionZ);
         double[] offset = targetOffset(tick, targetValid[index]);
         double[] direction = direction(offset[0], offset[1], offset[2]);
         targetYaw[index] = direction[0];
         targetPitch[index] = direction[1];
      }

      boolean hasTarget = targetValid[size - 1];
      double[] offset = targetOffset(last, hasTarget);
      double targetHeight = last.targetSneaking ? PLAYER_HEIGHT_SNEAKING : PLAYER_HEIGHT_STANDING;
      double yawError = hasTarget ? wrapDegrees(targetYaw[size - 1] - last.yaw) : 0.0;
      double pitchError = hasTarget ? targetPitch[size - 1] - last.pitch : 0.0;
      double topPitch = direction(offset[0], offset[1] + targetHeight / 2.0, offset[2])[1];
      double bottomPitch = direction(offset[0], offset[1] - targetHeight / 2.0, offset[2])[1];
      double horizontalDistance = Math.max(Math.hypot(offset[0], offset[2]), 0.05);
      double distance = Math.max(Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1] + offset[2] * offset[2]), 0.05);
      features[21] = hasTarget ? 1.0 : 0.0;
      features[22] = clamp(yawError / 30.0, -6.0, 6.0);
      features[23] = asinh(yawError / 2.0) / 3.0;
      features[24] = clamp(pitchError / 30.0, -6.0, 6.0);
      features[25] = asinh(pitchError / 2.0) / 3.0;
      features[26] = hasTarget ? asinh((topPitch - last.pitch) / 2.0) / 3.0 : 0.0;
      features[27] = hasTarget ? asinh((bottomPitch - last.pitch) / 2.0) / 3.0 : 0.0;
      features[28] = hasTarget ? Math.log(Math.toDegrees(Math.atan(0.3 / horizontalDistance))) / 3.0 : 0.0;
      features[29] = hasTarget ? Math.log(distance) / 2.0 : 0.0;

      if (hasTarget) {
         double[] eye = playerEyePosition(last);
         features[30] = hasLineOfSight(eye, rotationVector(last.yaw, last.pitch), new double[]{last.targetX, last.targetY, last.targetZ}, targetHeight)
            ? 1.0
            : 0.0;
      }

      boolean[] sameTarget = new boolean[size];
      boolean[] switchedTarget = new boolean[size];

      for (int index = 0; index < size; index++) {
         sameTarget[index] = targetValid[index] && index > 0 && targetValid[index - 1] && ticks[index].targetId == ticks[index - 1].targetId;
         switchedTarget[index] = targetValid[index] && !sameTarget[index] || !targetValid[index];
      }

      boolean trackingSame = sameTarget[size - 1];

      if (trackingSame) {
         features[31] = asinh(wrapDegrees(targetYaw[size - 1] - targetYaw[size - 2]) / 0.5) / 4.0;
         features[32] = asinh((targetPitch[size - 1] - targetPitch[size - 2]) / 0.5) / 4.0;
         double targetMotionX = nanToZero(last.targetX - previous.targetX);
         double targetMotionY = nanToZero(last.targetY - previous.targetY);
         double targetMotionZ = nanToZero(last.targetZ - previous.targetZ);
         features[33] = clamp((targetMotionX * forward[0] + targetMotionZ * forward[2]) / 0.3, -3.0, 3.0);
         features[34] = clamp((targetMotionX * right[0] + targetMotionZ * right[2]) / 0.3, -3.0, 3.0);
         features[35] = clamp(targetMotionY / 0.3, -3.0, 3.0);
      }

      features[36] = hasTarget && last.targetSneaking ? 1.0 : 0.0;
      features[37] = hasTarget ? Math.min(last.targetCandidates, 3) / 3.0 : 0.0;
      features[38] = hasTarget && !trackingSame ? 1.0 : 0.0;
      features[39] = hasTarget ? this.normalizeRunLength(this.runLength(switchedTarget)) : 0.0;
      boolean[] attacks = new boolean[size];

      for (int index = 0; index < size; index++) {
         attacks[index] = ticks[index].attacked;
      }

      features[40] = last.attacked ? 1.0 : 0.0;
      features[41] = this.normalizeRunLength(this.runLength(attacks));
      return features;
   }

   /** Вход сети: признаки тика, нули на «выброшенных» входах и добавки intent/keys. */
   public double[] networkInput(double[] features, int keys) {
      double[] input = new double[this.featureCount + this.extraFeatureCount()];
      System.arraycopy(features, 0, input, 0, this.featureCount);

      for (int index : this.droppedFeatures) {
         input[index] = 0.0;
      }

      int offset = this.featureCount;

      if (this.keysNow) {
         for (int bit = 0; bit < 7; bit++) {
            input[offset++] = keys >> bit & 1;
         }
      }

      if (this.intent) {
         input[offset] = this.holdProbability(features);
      }

      return input;
   }

   /** Вероятность «держать прицел на цели»; при заданной доле (0..1) — ровно она. */
   public double holdProbability(double[] features) {
      if (this.holdRatio >= 0.0) {
         return features[21] > 0.5 ? this.holdRatio : 0.0;
      }

      if (this.intentErrorEdges == null) {
         return 1.0;
      }

      if (features[21] <= 0.5) {
         return 0.0;
      }

      double error = Math.abs(Math.sinh(features[23] * 3.0) * 2.0);
      int bucket = 0;

      while (bucket + 1 < this.intentErrorEdges.length && this.intentErrorEdges[bucket + 1] <= error) {
         bucket++;
      }

      bucket = Math.min(bucket, this.intentErrorProbabilities.length - 1);
      return Math.min(1.0, this.intentErrorScale * this.intentErrorProbabilities[bucket]);
   }

   public double[] forward(State state, double[] input) {
      return this.forwardLayers(state, input, 0);
   }

   public double[] forwardLayers(State state, double[] input, int startLayer) {
      int inputSize = this.featureCount + this.extraFeatureCount();
      double[] buffer = new double[inputSize + this.embeddingSize];
      System.arraycopy(input, 0, buffer, 0, inputSize);
      System.arraycopy(this.embedding, 0, buffer, inputSize, this.embeddingSize);
      double[] hidden = linear(this.inputWeights, this.inputBias, buffer);

      for (int index = 0; index < hidden.length; index++) {
         hidden[index] = softsign(hidden[index]);
      }

      for (int layer = startLayer; layer < this.layers; layer++) {
         double[] previous = state.hidden[layer];
         double[] fromInput = linear(this.recurrentInputWeights[layer], this.recurrentInputBias[layer], hidden);
         double[] fromHidden = linear(this.recurrentHiddenWeights[layer], this.recurrentHiddenBias[layer], previous);
         double[] next = new double[this.hiddenSize];

         for (int unit = 0; unit < this.hiddenSize; unit++) {
            double reset = sigmoid(fromInput[unit] + fromHidden[unit]);
            double update = sigmoid(fromInput[this.hiddenSize + unit] + fromHidden[this.hiddenSize + unit]);
            double candidate = Math.tanh(fromInput[2 * this.hiddenSize + unit] + reset * fromHidden[2 * this.hiddenSize + unit]);
            next[unit] = (1.0 - update) * candidate + update * previous[unit];
         }

         state.hidden[layer] = next;
         hidden = next;
      }

      return hidden;
   }

   public double[] yawLogits(double[] hidden) {
      double[] result = linear(this.yawHiddenWeights, this.yawHiddenBias, hidden);

      for (int index = 0; index < result.length; index++) {
         result[index] = softsign(result[index]);
      }

      return linear(this.yawOutputWeights, this.yawOutputBias, result);
   }

   public double[] pitchLogits(double[] hidden, int yawClass) {
      double[] input = new double[hidden.length + this.yawEmbedding[yawClass].length];
      System.arraycopy(hidden, 0, input, 0, hidden.length);
      System.arraycopy(this.yawEmbedding[yawClass], 0, input, hidden.length, this.yawEmbedding[yawClass].length);
      double[] result = linear(this.pitchHiddenWeights, this.pitchHiddenBias, input);

      for (int index = 0; index < result.length; index++) {
         result[index] = softsign(result[index]);
      }

      return linear(this.pitchOutputWeights, this.pitchOutputBias, result);
   }

   /** Сэмплирование класса: температура применяется так же, как в Opus. */
   public int sampleClass(double[] logits, double sample) {
      double maximum = Double.NEGATIVE_INFINITY;

      for (double logit : logits) {
         maximum = Math.max(maximum, logit);
      }

      double total = 0.0;
      double[] weights = new double[logits.length];

      for (int index = 0; index < weights.length; index++) {
         weights[index] = Math.exp(logits[index] - maximum);
         total += weights[index];
      }

      if (this.temperature != 1.0) {
         double noMove = weights[0] / total;
         double top = Double.NEGATIVE_INFINITY;

         for (int index = 1; index < weights.length; index++) {
            top = Math.max(top, logits[index] / this.temperature);
         }

         double rest = 0.0;

         for (int index = 1; index < weights.length; index++) {
            weights[index] = Math.exp(logits[index] / this.temperature - top);
            rest += weights[index];
         }

         for (int index = 1; index < weights.length; index++) {
            weights[index] = weights[index] / rest * (1.0 - noMove);
         }

         weights[0] = noMove;
         total = 1.0;
      }

      double target = sample * total;
      double accumulated = 0.0;

      for (int index = 0; index < weights.length; index++) {
         accumulated += weights[index];
         if (target < accumulated) {
            return index;
         }
      }

      return weights.length - 1;
   }

   public double classToStep(int yawClass, double grid, Random random) {
      if (yawClass == 0) {
         return 0.0;
      }

      boolean positive = yawClass <= this.binsPerSide;
      int bin = Math.max(0, Math.min(this.binsPerSide - 1, positive ? yawClass - 1 : yawClass - 1 - this.binsPerSide));
      double lower = Math.log(this.binEdges[bin]);
      double upper = Math.log(this.binEdges[bin + 1]);

      if (this.subBinTable != null) {
         double[] subBins = this.subBinTable[yawClass];
         double sample = random.nextDouble();
         double accumulated = 0.0;
         int chosen = subBins.length - 1;

         for (int index = 0; index < subBins.length; index++) {
            accumulated += subBins[index];
            if (sample < accumulated) {
               chosen = index;
               break;
            }
         }

         double span = (upper - lower) / subBins.length;
         lower += span * chosen;
         upper = lower + span;
      }

      double continuous = Math.exp(lower + (upper - lower) * random.nextDouble());
      double firstPixel = Math.ceil(Math.exp(lower) / grid);
      double lastPixel = Math.ceil(Math.exp(upper) / grid) - 1.0;
      double sample = random.nextDouble();
      double step = lastPixel >= firstPixel ? (firstPixel + Math.floor(sample * (lastPixel - firstPixel + 1.0))) * grid : continuous;
      return positive ? step : -step;
   }

   /** Ветка «предохранитель маской»: классы вне коридора запрещены. */
   public double[] sampleGuarded(State state, double[] features, double[] input, double grid, Random random) {
      double[] corridor = guardCorridor(features);
      double[] hidden = this.forward(state, input);
      int yawClass = this.sampleClass(maskBins(this.yawLogits(hidden), corridor[0], corridor[1]), random.nextDouble());
      int pitchClass = this.sampleClass(maskBins(this.pitchLogits(hidden, yawClass), corridor[2], corridor[3]), random.nextDouble());
      double yawStep = Math.max(corridor[0], Math.min(corridor[1], this.classToStep(yawClass, grid, random)));
      double pitchStep = Math.max(corridor[2], Math.min(corridor[3], this.classToStep(pitchClass, grid, random)));
      return new double[]{yawStep, pitchStep};
   }

   /** Коридор допустимых шагов из текущего прицела и размера хитбокса. */
   public double[] guardCorridor(double[] features) {
      double wide = 1.0E9;

      if (!(this.guardMargin > 0.0) || features[21] <= 0.5 || !(Math.abs(sinhScaled(features[23])) < this.guardFrontAngle)) {
         return new double[]{-wide, wide, -wide, wide};
      }

      double yawError = sinhScaled(features[23]);
      double topError = sinhScaled(features[26]);
      double bottomError = sinhScaled(features[27]);
      double reach = Math.max(Math.abs(yawError), Math.exp(features[28] * 3.0) + this.guardMargin);
      return new double[]{
         yawError - reach,
         yawError + reach,
         Math.min(topError - this.guardMargin, 0.0),
         Math.max(bottomError + this.guardMargin, 0.0)
      };
   }

   /** Ограничение шага коридором (мягкая ветка предохранителя). */
   public double[] applyGuard(double[] features, double yawStep, double pitchStep) {
      if (!(this.guardMargin > 0.0) || features[21] <= 0.5) {
         return new double[]{yawStep, pitchStep};
      }

      double yawError = sinhScaled(features[23]);
      double topError = sinhScaled(features[26]);
      double bottomError = sinhScaled(features[27]);
      double coneAngle = Math.exp(features[28] * 3.0);

      if (!(Math.abs(yawError) < this.guardFrontAngle)) {
         return new double[]{yawStep, pitchStep};
      }

      double reach = Math.max(Math.abs(yawError), coneAngle + this.guardMargin);
      double error = yawError - yawStep;

      if (Math.abs(error) > reach) {
         yawStep = yawError - Math.signum(error) * reach;
      }

      double minimumPitch = Math.min(topError - this.guardMargin, 0.0);
      double maximumPitch = Math.max(bottomError + this.guardMargin, 0.0);
      pitchStep = Math.max(minimumPitch, Math.min(maximumPitch, pitchStep));
      return new double[]{yawStep, pitchStep};
   }

   public double binLower(int cls) {
      return cls == 0 ? 0.0 : (cls <= this.binsPerSide ? this.binEdges[cls - 1] : -this.binEdges[cls - this.binsPerSide]);
   }

   public double binUpper(int cls) {
      return cls == 0 ? 0.0 : (cls <= this.binsPerSide ? this.binEdges[cls] : -this.binEdges[cls - 1 - this.binsPerSide]);
   }

   public double[] maskBins(double[] logits, double min, double max) {
      double[] masked = logits.clone();

      for (int index = 0; index < masked.length; index++) {
         if (this.binUpper(index) < min || this.binLower(index) > max) {
            masked[index] = -1.0E9;
         }
      }

      return masked;
   }

   public double[] sanitizeBins(double[] candidate, double min, double max) {
      double[] masked = this.maskBins(candidate, min, max);

      for (double value : masked) {
         if (value > -1.0E8) {
            return masked;
         }
      }

      return candidate;
   }

   public double sinhScaled(double value) {
      return Math.sinh(value * 3.0) * 2.0;
   }

   /** Шаг по сетке с накоплением остатка: ровно то, что применяет Opus к прицелу. */
   public double[] applyToState(State state, double yawStep, double pitchStep, double grid, double currentPitch) {
      state.accumulatedYaw += yawStep;
      state.accumulatedPitch += pitchStep;
      double yawPixels = Math.rint(state.accumulatedYaw / grid);
      double pitchPixels = Math.rint(state.accumulatedPitch / grid);
      double yawDelta = yawPixels * grid;
      double pitchDelta = pitchPixels * grid;
      state.accumulatedYaw -= yawDelta;
      state.accumulatedPitch -= pitchDelta;
      double pitch = Math.max(-90.0, Math.min(90.0, currentPitch + pitchDelta));
      return new double[]{yawDelta, pitch - currentPitch};
   }

   /** Ветка пределов людей (модель с {@code limits}); в текущем файле не используется. */
   public double[] sampleWithLimits(State state, double[] features, double[] input, double grid, Random random) {
      LimitTable limits = this.limitTable;

      if (state.limitReference == null) {
         state.limitReference = limits.players[random.nextInt(limits.players.length)];
         state.yawHistory = new double[limits.historyLength];
         state.pitchHistory = new double[limits.historyLength];
         state.validHistory = new boolean[limits.historyLength];
      }

      if (state.historyCount == limits.historyLength) {
         System.arraycopy(state.yawHistory, 1, state.yawHistory, 0, limits.historyLength - 1);
         System.arraycopy(state.pitchHistory, 1, state.pitchHistory, 0, limits.historyLength - 1);
         System.arraycopy(state.validHistory, 1, state.validHistory, 0, limits.historyLength - 1);
         state.historyCount--;
      }

      state.yawHistory[state.historyCount] = Math.sinh(features[1] * 4.0) * 0.5;
      state.pitchHistory[state.historyCount] = Math.sinh(features[3] * 4.0) * 0.5;
      state.validHistory[state.historyCount] = features[4] > 0.5;
      state.historyCount++;
      double[] bounds = limits.selectBounds(
         state.yawHistory,
         state.pitchHistory,
         state.validHistory,
         state.historyCount,
         state.limitReference,
         random.nextDouble(),
         random.nextDouble()
      );
      double[] hidden = this.forward(state, input);
      int yawClass = this.sampleClass(this.sanitizeBins(this.yawLogits(hidden), bounds[0], bounds[1]), random.nextDouble());
      int pitchClass = this.sampleClass(this.sanitizeBins(this.pitchLogits(hidden, yawClass), bounds[2], bounds[3]), random.nextDouble());
      double yawStep = this.classToStep(yawClass, grid, random);
      double pitchStep = this.classToStep(pitchClass, grid, random);
      double clampedYaw = Math.max(bounds[0], Math.min(bounds[1], yawStep));
      double clampedPitch = Math.max(bounds[2], Math.min(bounds[3], pitchStep));
      limits.accepted++;

      if (Math.abs(clampedYaw - yawStep) > 1.0E-9 || Math.abs(clampedPitch - pitchStep) > 1.0E-9) {
         limits.clamped++;
      }

      return new double[]{clampedYaw, clampedPitch};
   }

   /** Обычная ветка: одна выборка пары классов и шаг по сетке. */
   public double[] sampleDirect(State state, double[] input, double grid, Random random) {
      double[] hidden = this.forward(state, input);
      int yawClass = this.sampleClass(this.yawLogits(hidden), random.nextDouble());
      int pitchClass = this.sampleClass(this.pitchLogits(hidden, yawClass), random.nextDouble());
      return new double[]{this.classToStep(yawClass, grid, random), this.classToStep(pitchClass, grid, random)};
   }

   /** Ветка дискриминатора хода: лучший кандидат по оценке дискриминаторов. */
   public double[] sampleGuided(State state, double[] features, double[] input, double grid, Random random) {
      for (int index = 0; index < this.guides.length; index++) {
         state.guideState[index] = this.guides[index].step(state.guideState[index], this.guides[index].encode(features));
      }

      double[] hidden = this.forward(state, input);
      double[] yawLogits = this.yawLogits(hidden);
      int candidates = this.guideSampleCount;
      double[][] steps = new double[candidates][];
      double[] scores = new double[candidates];
      double maximum = Double.NEGATIVE_INFINITY;

      for (int candidateIndex = 0; candidateIndex < candidates; candidateIndex++) {
         int yawClass = this.sampleClass(yawLogits, random.nextDouble());
         int pitchClass = this.sampleClass(this.pitchLogits(hidden, yawClass), random.nextDouble());
         double yawStep = this.classToStep(yawClass, grid, random);
         double pitchStep = this.classToStep(pitchClass, grid, random);
         steps[candidateIndex] = new double[]{yawStep, pitchStep};
         double[] virtualFeatures = this.featuresAfterStep(features, yawStep, pitchStep);
         double score = 0.0;

         for (int guideIndex = 0; guideIndex < this.guides.length; guideIndex++) {
            Guide guide = this.guides[guideIndex];
            score += guide.score(guide.step(state.guideState[guideIndex], guide.encode(virtualFeatures)));
         }

         scores[candidateIndex] = this.guideWeight * score;
         maximum = Math.max(maximum, scores[candidateIndex]);
      }

      double total = 0.0;

      for (int index = 0; index < candidates; index++) {
         scores[index] = Math.exp(scores[index] - maximum);
         total += scores[index];
      }

      double sample = random.nextDouble() * total;
      double accumulated = 0.0;

      for (int index = 0; index < candidates; index++) {
         accumulated += scores[index];
         if (sample < accumulated) {
            return steps[index];
         }
      }

      return steps[candidates - 1];
   }

   /** Признаки «как будто шаг уже сделан» — для оценки дискриминатором хода. */
   public double[] featuresAfterStep(double[] features, double yawStep, double pitchStep) {
      double[] after = features.clone();
      after[0] = clamp(yawStep / 10.0, -6.0, 6.0);
      after[1] = asinh(yawStep / 0.5) / 4.0;
      after[2] = clamp(pitchStep / 10.0, -6.0, 6.0);
      after[3] = asinh(pitchStep / 0.5) / 4.0;
      after[4] = 1.0;
      double pitch = clamp(features[5] * 90.0 + pitchStep, -90.0, 90.0);
      after[5] = pitch / 90.0;
      boolean hasTarget = features[21] > 0.5;
      double yawError = Math.sinh(features[23] * 3.0) * 2.0 - yawStep;
      double pitchError = Math.sinh(features[25] * 3.0) * 2.0 - pitchStep;
      after[22] = hasTarget ? clamp(yawError / 30.0, -6.0, 6.0) : 0.0;
      after[23] = hasTarget ? asinh(yawError / 2.0) / 3.0 : 0.0;
      after[24] = hasTarget ? clamp(pitchError / 30.0, -6.0, 6.0) : 0.0;
      after[25] = hasTarget ? asinh(pitchError / 2.0) / 3.0 : 0.0;

      for (int index = 26; index <= 27; index++) {
         double coneError = Math.sinh(features[index] * 3.0) * 2.0 - pitchStep;
         after[index] = hasTarget ? asinh(coneError / 2.0) / 3.0 : 0.0;
      }

      double expandedDistance = Math.exp(features[28] * 3.0);
      double topError = Math.sinh(after[26] * 3.0) * 2.0;
      double bottomError = Math.sinh(after[27] * 3.0) * 2.0;
      after[30] = hasTarget && Math.abs(yawError) < expandedDistance && topError <= 0.0 && bottomError >= 0.0 ? 1.0 : 0.0;
      return after;
   }

   public static double[] readDoubles(JsonArray array) {
      double[] values = new double[array.size()];

      for (int index = 0; index < values.length; index++) {
         values[index] = array.get(index).getAsDouble();
      }

      return values;
   }

   public static double[] readTensor(JsonObject weights, String name) {
      JsonObject tensor = weights.getAsJsonObject(name);
      byte[] raw = Base64.getDecoder().decode(tensor.get("b64").getAsString());
      ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
      double[] values = new double[raw.length / 4];

      for (int index = 0; index < values.length; index++) {
         values[index] = buffer.getFloat();
      }

      return values;
   }

   public static double[][] readMatrix(JsonObject weights, String name) {
      JsonArray shape = weights.getAsJsonObject(name).getAsJsonArray("shape");
      int rows = shape.get(0).getAsInt();
      int columns = shape.get(1).getAsInt();
      double[] flat = readTensor(weights, name);
      double[][] matrix = new double[rows][columns];

      for (int row = 0; row < rows; row++) {
         System.arraycopy(flat, row * columns, matrix[row], 0, columns);
      }

      return matrix;
   }

   public static double referenceValue(double value) {
      return value > 1.0E29 ? Double.NaN : value;
   }

   /** Загрузка модели с проверкой эталона (те же тесты, что и в Opus). */
   public static TestNervNet load(InputStream stream) throws IOException {
      InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
      JsonObject root;

      try {
         root = JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Throwable error) {
         try {
            reader.close();
         } catch (Throwable closeError) {
            error.addSuppressed(closeError);
         }

         throw error;
      }

      reader.close();

      if (!FORMAT.equals(root.get("format").getAsString())) {
         throw new IOException("формат " + root.get("format").getAsString() + ", ожидался " + FORMAT);
      }

      TestNervNet net = new TestNervNet();
      net.featureCount = root.get("nf").getAsInt();
      net.hiddenSize = root.get("h").getAsInt();
      net.layers = root.get("layers").getAsInt();
      net.embeddingSize = root.get("emb").getAsInt();
      net.binsPerSide = root.get("nb").getAsInt();
      net.classCount = root.get("ncls").getAsInt();
      net.clip = root.get("clip").getAsInt();
      net.windowSize = root.get("window").getAsInt();
      net.keysNow = root.has("keys_now") && root.get("keys_now").getAsBoolean();
      net.intent = root.has("intent") && root.get("intent").getAsBoolean();

      if (root.has("intent_hold") && root.get("intent_hold").isJsonObject()) {
         net.holdRatio = root.getAsJsonObject("intent_hold").get("ref").getAsDouble();
      }

      if (root.has("intent_err") && root.get("intent_err").isJsonObject()) {
         JsonObject intentError = root.getAsJsonObject("intent_err");
         net.intentErrorScale = intentError.get("k").getAsDouble();
         net.intentErrorEdges = readDoubles(intentError.getAsJsonArray("edges"));
         net.intentErrorProbabilities = readDoubles(intentError.getAsJsonArray("p"));
      }

      String guardNote = "";

      if (root.has("guard") && root.get("guard").isJsonObject()) {
         JsonObject guard = root.getAsJsonObject("guard");
         net.guardMargin = guard.get("m").getAsDouble();
         net.guardFrontAngle = guard.get("front").getAsDouble();
         net.guardByMask = guard.has("mode") && "mask".equals(guard.get("mode").getAsString());
         JsonObject reference = guard.getAsJsonObject("ref");
         JsonArray inputs = reference.getAsJsonArray("x");
         JsonArray directions = reference.getAsJsonArray("d");
         JsonArray outputs = reference.getAsJsonArray("out");
         double maximumError = 0.0;

         for (int index = 0; index < inputs.size(); index++) {
            double[] direction = readDoubles(directions.get(index).getAsJsonArray());
            double[] expected = readDoubles(outputs.get(index).getAsJsonArray());
            double[] actual = net.applyGuard(readDoubles(inputs.get(index).getAsJsonArray()), direction[0], direction[1]);
            maximumError = Math.max(maximumError, Math.max(Math.abs(actual[0] - expected[0]), Math.abs(actual[1] - expected[1])));
         }

         if (!(maximumError < 1.0E-6)) {
            throw new IOException("эталон предохранителя не сошёлся: max |Δ| " + maximumError);
         }

         guardNote = String.format(
            "; предохранитель M %.1f° %s (эталон %d, max |Δ| %.1e)",
            net.guardMargin,
            net.guardByMask ? "выбором" : "обрезкой",
            inputs.size(),
            maximumError
         );
      }

      if (root.has("sub_table") && root.get("sub_table").isJsonObject()) {
         JsonArray table = root.getAsJsonObject("sub_table").getAsJsonArray("p");
         net.subBinTable = new double[table.size()][];

         for (int index = 0; index < table.size(); index++) {
            net.subBinTable[index] = readDoubles(table.get(index).getAsJsonArray());
         }

         guardNote = guardNote + String.format("; подбин по людям (%d×%d)", net.subBinTable.length, net.subBinTable[0].length);
      }

      if (root.has("drop")) {
         JsonArray drop = root.getAsJsonArray("drop");
         net.droppedFeatures = new int[drop.size()];

         for (int index = 0; index < drop.size(); index++) {
            net.droppedFeatures[index] = drop.get(index).getAsInt();
         }
      }

      JsonArray edges = root.getAsJsonArray("edges");
      net.binEdges = new double[edges.size()];

      for (int index = 0; index < edges.size(); index++) {
         net.binEdges[index] = edges.get(index).getAsDouble();
      }

      if (root.has("limits") && root.get("limits").isJsonObject()) {
         JsonObject limitsJson = root.getAsJsonObject("limits");
         LimitTable limits = new LimitTable();
         JsonArray players = limitsJson.getAsJsonArray("players");
         limits.players = new double[players.size()][];

         for (int index = 0; index < players.size(); index++) {
            limits.players[index] = readDoubles(players.get(index).getAsJsonArray());
         }

         JsonObject window = limitsJson.getAsJsonObject("window");
         double[] huge = readDoubles(window.getAsJsonArray("huge"));
         double[] flip = readDoubles(window.getAsJsonArray("flip"));
         limits.hugeYaw = huge[0];
         limits.hugePitch = huge[1];
         limits.flipYaw = flip[0];
         limits.flipPitch = flip[1];
         limits.bigProbability = limitsJson.get("big").getAsDouble();
         limits.flipProbability = limitsJson.get("flip").getAsDouble();
         limits.historyLength = limitsJson.get("hist").getAsInt();
         JsonArray reference = limitsJson.getAsJsonArray("ref");
         double maximumError = 0.0;
         int classMismatches = 0;

         for (int index = 0; index < reference.size(); index++) {
            JsonObject row = reference.get(index).getAsJsonObject();
            JsonArray history = row.getAsJsonArray("h");
            int length = history.size();
            double[] yawHistory = new double[length];
            double[] pitchHistory = new double[length];
            boolean[] validHistory = new boolean[length];

            for (int step = 0; step < length; step++) {
               double[] values = readDoubles(history.get(step).getAsJsonArray());
               yawHistory[step] = values[0];
               pitchHistory[step] = values[1];
               validHistory[step] = values[2] > 0.5;
            }

            double[] actual = limits.selectBounds(
               yawHistory,
               pitchHistory,
               validHistory,
               length,
               limits.players[row.get("player").getAsInt()],
               row.get("u_big").getAsDouble(),
               row.get("u_flip").getAsDouble()
            );
            double[] expected = readDoubles(row.getAsJsonArray("out"));

            for (int step = 0; step < 4; step++) {
               maximumError = Math.max(maximumError, Math.abs(actual[step] - expected[step]));
            }

            double[] probe = new double[net.classCount];
            double[] yawMasked = net.sanitizeBins(probe, actual[0], actual[1]);
            double[] pitchMasked = net.sanitizeBins(probe, actual[2], actual[3]);
            double[] allowedYaw = readDoubles(row.getAsJsonArray("allow_yaw"));
            double[] allowedPitch = readDoubles(row.getAsJsonArray("allow_pitch"));

            for (int cls = 0; cls < net.classCount; cls++) {
               if (yawMasked[cls] > -1.0E8 != allowedYaw[cls] > 0.5 || pitchMasked[cls] > -1.0E8 != allowedPitch[cls] > 0.5) {
                  classMismatches++;
               }
            }
         }

         if (!(maximumError < 1.0E-6) || classMismatches > 0) {
            throw new IOException("эталон пределов не сошёлся: max |Δ| " + maximumError + ", классов не так " + classMismatches);
         }

         net.limitTable = limits;
         guardNote = guardNote + String.format("; пределы людей: %d игроков (эталон %d, max |Δ| %.1e)", limits.players.length, reference.size(), maximumError);
      }

      JsonObject weights = root.getAsJsonObject("weights");
      net.inputWeights = readMatrix(weights, "inp.0.weight");
      net.inputBias = readTensor(weights, "inp.0.bias");
      net.embedding = readTensor(weights, "pemb0");
      net.recurrentInputWeights = new double[net.layers][][];
      net.recurrentHiddenWeights = new double[net.layers][][];
      net.recurrentInputBias = new double[net.layers][];
      net.recurrentHiddenBias = new double[net.layers][];

      for (int layer = 0; layer < net.layers; layer++) {
         net.recurrentInputWeights[layer] = readMatrix(weights, "rnn.weight_ih_l" + layer);
         net.recurrentHiddenWeights[layer] = readMatrix(weights, "rnn.weight_hh_l" + layer);
         net.recurrentInputBias[layer] = readTensor(weights, "rnn.bias_ih_l" + layer);
         net.recurrentHiddenBias[layer] = readTensor(weights, "rnn.bias_hh_l" + layer);
      }

      net.yawHiddenWeights = readMatrix(weights, "yaw.0.weight");
      net.yawHiddenBias = readTensor(weights, "yaw.0.bias");
      net.yawOutputWeights = readMatrix(weights, "yaw.2.weight");
      net.yawOutputBias = readTensor(weights, "yaw.2.bias");
      net.pitchHiddenWeights = readMatrix(weights, "pitch.0.weight");
      net.pitchHiddenBias = readTensor(weights, "pitch.0.bias");
      net.pitchOutputWeights = readMatrix(weights, "pitch.2.weight");
      net.pitchOutputBias = readTensor(weights, "pitch.2.bias");
      net.yawEmbedding = readMatrix(weights, "yemb.weight");
      String guideNote = "";

      if (root.has("guide") && !root.get("guide").isJsonNull()) {
         JsonObject guideJson = root.getAsJsonObject("guide");
         net.guideSampleCount = guideJson.get("n").getAsInt();
         net.guideWeight = guideJson.get("lam").getAsDouble();
         JsonArray discs = guideJson.getAsJsonArray("discs");
         net.guides = new Guide[discs.size()];
         JsonArray referenceRows = guideJson.getAsJsonArray("ref_rows");
         double maximumError = 0.0;

         for (int index = 0; index < discs.size(); index++) {
            JsonObject disc = discs.get(index).getAsJsonObject();
            JsonObject discWeights = disc.getAsJsonObject("weights");
            Guide guide = new Guide();
            double[] columns = readDoubles(disc.getAsJsonArray("cols"));
            guide.columns = new int[columns.length];

            for (int column = 0; column < columns.length; column++) {
               guide.columns[column] = (int)columns[column];
            }

            guide.hiddenSize = disc.get("h").getAsInt();
            guide.inputWeights = readMatrix(discWeights, "inp.0.weight");
            guide.inputBias = readTensor(discWeights, "inp.0.bias");
            guide.recurrentInputWeights = readMatrix(discWeights, "rnn.weight_ih_l0");
            guide.recurrentHiddenWeights = readMatrix(discWeights, "rnn.weight_hh_l0");
            guide.recurrentInputBias = readTensor(discWeights, "rnn.bias_ih_l0");
            guide.recurrentHiddenBias = readTensor(discWeights, "rnn.bias_hh_l0");
            guide.outputWeights = readMatrix(discWeights, "out.weight")[0];
            guide.outputBias = readTensor(discWeights, "out.bias")[0];
            net.guides[index] = guide;
            double[] expectedScores = readDoubles(disc.getAsJsonArray("ref_scores"));
            double[] hidden = new double[guide.hiddenSize];

            for (int row = 0; row < referenceRows.size(); row++) {
               hidden = guide.step(hidden, guide.encode(readDoubles(referenceRows.get(row).getAsJsonArray())));
               maximumError = Math.max(maximumError, Math.abs(guide.score(hidden) - expectedScores[row]));
            }
         }

         guideNote = String.format(
            "; выбор хода: %d дискр., N %d, lam %.1f, эталон оценок %.2e",
            net.guides.length,
            net.guideSampleCount,
            net.guideWeight,
            maximumError
         );

         if (!(maximumError <= 0.002)) {
            throw new IOException("ЭТАЛОН ДИСКРИМИНАТОРА НЕ СОШЁЛСЯ" + guideNote);
         }
      }

      net.source = root.get("source").getAsString().replace(',', ';') + (net.hasGuide() ? "+guide" + net.guides.length : "");
      net.status = net.verifyReference(root.getAsJsonArray("ref")) + guideNote + guardNote + "; модель " + root.get("source").getAsString();
      return net;
   }

   /**
    * Проверка сети на записанных эталонных эпизодах: признаки и логиты обязаны совпасть
    * с эталоном. Это тот же тест, который Opus выполняет при загрузке модели.
    */
   public String verifyReference(JsonArray rows) throws IOException {
      double featureError = 0.0;
      double logitError = 0.0;
      int ticks = 0;

      for (JsonElement element : rows) {
         JsonObject row = element.getAsJsonObject();
         double grid = row.get("grid").getAsDouble();
         double[] yaw = readDoubles(row.getAsJsonArray("yaw"));
         double[] pitch = readDoubles(row.getAsJsonArray("pitch"));
         double[] mineYaw = readDoubles(row.getAsJsonArray("my"));
         double[] minePitch = readDoubles(row.getAsJsonArray("mp"));
         double[] valid = readDoubles(row.getAsJsonArray("ok"));
         JsonObject record = row.getAsJsonObject("rec");
         JsonArray positions = record.getAsJsonArray("pos");
         JsonArray targets = record.getAsJsonArray("tgt");
         double[] keys = readDoubles(record.getAsJsonArray("keys"));
         double[] attacks = readDoubles(record.getAsJsonArray("attack"));
         double[] targetIds = readDoubles(record.getAsJsonArray("tgt_id"));
         double[] candidates = readDoubles(record.getAsJsonArray("n_cand"));
         double[] sneaking = readDoubles(record.getAsJsonArray("tgt_sneak"));
         double[] onGround = readDoubles(record.getAsJsonArray("on_ground"));
         double[] collision = readDoubles(record.getAsJsonArray("hcoll"));
         JsonArray referenceFeatures = row.getAsJsonArray("G");
         int start = row.get("start").getAsInt();
         JsonArray referenceYawLogits = row.getAsJsonArray("ly");
         JsonArray referencePitchLogits = row.getAsJsonArray("lp");
         double[] yawClasses = readDoubles(row.getAsJsonArray("yc"));
         State state = this.newState();

         for (int index = 0; index < yaw.length; index++) {
            Tick tick = new Tick();
            JsonArray position = positions.get(index).getAsJsonArray();
            JsonArray target = targets.get(index).getAsJsonArray();
            tick.positionX = referenceValue(position.get(0).getAsDouble());
            tick.positionY = referenceValue(position.get(1).getAsDouble());
            tick.positionZ = referenceValue(position.get(2).getAsDouble());
            tick.targetX = referenceValue(target.get(0).getAsDouble());
            tick.targetY = referenceValue(target.get(1).getAsDouble());
            tick.targetZ = referenceValue(target.get(2).getAsDouble());
            tick.keys = (int)keys[index];
            tick.attacked = attacks[index] > 0.0;
            tick.targetId = (int)targetIds[index];
            tick.targetCandidates = (int)candidates[index];
            tick.targetSneaking = sneaking[index] > 0.0;
            tick.onGround = onGround[index] > 0.0;
            tick.horizontalCollision = collision[index] > 0.0;
            tick.yaw = yaw[index];
            tick.pitch = pitch[index];
            tick.deltaYaw = mineYaw[index];
            tick.deltaPitch = minePitch[index];
            tick.rotationValid = valid[index] > 0.0;
            state.push(tick);
            double[] features = this.buildFeatures(state, grid);
            double[] expectedFeatures = readDoubles(referenceFeatures.get(index).getAsJsonArray());

            for (int feature = 0; feature < this.featureCount; feature++) {
               featureError = Math.max(featureError, Math.abs(features[feature] - expectedFeatures[feature]));
            }

            if (index >= start) {
               double[] previousFeatures = index == 0
                  ? new double[this.featureCount]
                  : readDoubles(referenceFeatures.get(index - 1).getAsJsonArray());
               double[] input = this.networkInput(previousFeatures, (int)keys[index]);
               double[] hidden = this.forward(state, input);
               double[] yawLogits = this.yawLogits(hidden);
               double[] pitchLogits = this.pitchLogits(hidden, (int)yawClasses[index - start]);
               double[] expectedYaw = readDoubles(referenceYawLogits.get(index - start).getAsJsonArray());
               double[] expectedPitch = readDoubles(referencePitchLogits.get(index - start).getAsJsonArray());

               for (int cls = 0; cls < this.classCount; cls++) {
                  logitError = Math.max(logitError, Math.max(Math.abs(yawLogits[cls] - expectedYaw[cls]), Math.abs(pitchLogits[cls] - expectedPitch[cls])));
               }
            }

            ticks++;
         }
      }

      String summary = String.format("эталон: %d тиков, признаки макс. расхождение %.2e, логиты %.2e", ticks, featureError, logitError);

      if (featureError <= REFERENCE_FEATURE_TOLERANCE && logitError <= REFERENCE_LOGIT_TOLERANCE) {
         return summary;
      } else {
         throw new IOException("ЭТАЛОН НЕ СОШЁЛСЯ — " + summary);
      }
   }
}
