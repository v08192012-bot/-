package brokke.cc.rotation;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Драйвер SpaceTime: фичи (ошибка до цели, её скорость, прошлые дельты, дистанция,
 * on-target, freeze-таймер) -> GRU-шаг -> 19 логитов -> выбор компоненты (Gumbel-max),
 * сэмпл из 2D-Gaussian с корреляцией rho, asinh-обратный squash -> дельты yaw/pitch,
 * квантованные по сетке error-квантиля.
 *
 * Если модель "заморожена" (freeze-head) и freeze-счётчик не выдержан — держим прицел (0,0).
 * Если выдержан — форс-сэмпл; если он дал (0,0) — пинок по оси с большей ошибкой.
 */
public final class SpaceTimeDriver {
   public static final float SPAN_INIT = 20.0F;
   private static final float ATTACK_WINDOW = SpaceTimeModel.ATTACK_WINDOW;

   private final SpaceTimeModel model;
   private float[] hidden;
   private final float yawSpan = SPAN_INIT;
   private final float pitchSpan = SPAN_INIT;
   private float lastGoalYaw;
   private float lastGoalPitch;
   private float errYaw;
   private float errPitch;
   private float prevErrYaw;
   private float prevErrPitch;
   private float stepYaw;
   private float stepPitch;
   private float prevStepYaw;
   private float prevStepPitch;
   private int freeze;
   private boolean onTarget;
   private float sinceAttack = ATTACK_WINDOW;

   public SpaceTimeDriver(SpaceTimeModel model) {
      this.model = model;
   }

   /** Сбросить состояние (смена цели / пауза). */
   public void invalidate() {
      this.hidden = null;
      this.stepYaw = 0.0F;
      this.stepPitch = 0.0F;
      this.prevStepYaw = 0.0F;
      this.prevStepPitch = 0.0F;
      this.freeze = 0;
      this.onTarget = false;
   }

   /** Выстрел: обнулить таймер "тик с момента последней атаки". */
   public void onAttack() {
      this.sinceAttack = 0.0F;
   }

   public static float wrap(float d) {
      return (d + 180.0F) % 360.0F - 180.0F;
   }

   /**
    * @param goalYaw/goalPitch   текущий желаемый прицел (eyePos -> цель)
    * @param aimYaw/aimPitch     текущий применённый прицел
    * @param dist                дистанция до цели
    * @param scale               пользовательский множитель дельт (1.0 = как в модели)
    * @param forcedSamples       число сэмплов при форс-движении
    * @param freezeBonus         лимит freeze-счётчика (меньше = чаще движется)
    * @return [stepYaw, stepPitch]
    */
   public float[] tick(float goalYaw, float goalPitch, float aimYaw, float aimPitch, double dist, float scale, int forcedSamples, int freezeBonus) {
      if (this.model == null) {
         return new float[]{0.0F, 0.0F};
      }

      if (this.hidden == null) {
         this.reset(goalYaw, goalPitch, aimYaw, aimPitch);
      }

      this.sinceAttack = Math.min(this.sinceAttack + 1.0F, ATTACK_WINDOW);

      float goalSpeedYaw = wrap(goalYaw - this.lastGoalYaw);
      float goalSpeedPitch = goalPitch - this.lastGoalPitch;
      this.lastGoalYaw = goalYaw;
      this.lastGoalPitch = goalPitch;
      float totalErrYaw = wrap(goalYaw - aimYaw);
      float totalErrPitch = goalPitch - aimPitch;

      float[] x = new float[SpaceTimeModel.FEATURES];
      x[0] = SpaceTimeModel.squash(this.errYaw);
      x[1] = SpaceTimeModel.squash(this.errPitch);
      x[2] = SpaceTimeModel.squash(wrap(this.errYaw - this.prevErrYaw));
      x[3] = SpaceTimeModel.squash(this.errPitch - this.prevErrPitch);
      x[4] = SpaceTimeModel.squash(goalSpeedYaw);
      x[5] = SpaceTimeModel.squash(goalSpeedPitch);
      x[6] = SpaceTimeModel.squash(this.stepYaw);
      x[7] = SpaceTimeModel.squash(this.stepPitch);
      x[8] = SpaceTimeModel.squash(this.prevStepYaw);
      x[9] = SpaceTimeModel.squash(this.prevStepPitch);
      x[10] = SpaceTimeModel.squash(this.errYaw / this.yawSpan);
      x[11] = SpaceTimeModel.squash(this.errPitch / this.pitchSpan);
      x[12] = (float) (Math.log(Math.max((float) dist, 0.05F) + 0.5F) / 2.0);
      x[13] = (float) (Math.log(this.yawSpan) / 3.0);
      x[14] = this.onTarget ? 1.0F : 0.0F;
      x[15] = Math.min(this.sinceAttack, ATTACK_WINDOW) / ATTACK_WINDOW;
      x[16] = (float) this.freeze / (float) Math.max(1, this.model.freezeCut);

      this.hidden = this.model.gruStep(x, this.hidden);
      float[] o = this.model.head(this.hidden);

      float outStepYaw = 0.0F;
      float outStepPitch = 0.0F;
      boolean forceMove = this.freeze >= Math.min(freezeBonus, this.model.freezeCut);
      float freezeProb = SpaceTimeModel.sigmoid(o[0]);
      ThreadLocalRandom rnd = ThreadLocalRandom.current();

      if (forceMove || rnd.nextFloat() >= freezeProb) {
         float bestScore = Float.MAX_VALUE;

         for (int s = 0; s < forcedSamples; s++) {
            int k = this.model.sampleMixture(o, rnd.nextFloat());
            float rho = this.model.rho(k, o);
            float z1 = (float) rnd.nextGaussian();
            float z2 = (float) rnd.nextGaussian();
            float corr = (float) Math.sqrt(Math.max(1.0E-6F, 1.0F - rho * rho));
            float candYaw = scale * this.model.sampleAxis(o, k, 1, 3, z1);
            float candPitch = scale * this.model.sampleAxis(o, k, 2, 4, z1 * rho + z2 * corr);

            float q = this.model.errorQuantile(clamp01((float) Math.hypot(this.errYaw / this.yawSpan, this.errPitch / this.pitchSpan)));
            if (q > 1.0E-4F) {
               candYaw = Math.round(candYaw / q) * q;
               candPitch = Math.round(candPitch / q) * q;
            }

            float score = (float) Math.hypot(wrap(totalErrYaw - candYaw) / this.yawSpan, (totalErrPitch - candPitch) / this.pitchSpan);
            if (score < bestScore) {
               bestScore = score;
               outStepYaw = candYaw;
               outStepPitch = candPitch;
            }
         }
      }

      if (forceMove && outStepYaw == 0.0F && outStepPitch == 0.0F) {
         float q = this.model.errorQuantile(1.0F);
         if (q <= 0.0F) {
            q = 0.5F;
         }

         if (Math.abs(totalErrYaw) >= Math.abs(totalErrPitch)) {
            outStepYaw = Math.copySign(q, totalErrYaw);
         } else {
            outStepPitch = Math.copySign(q, totalErrPitch);
         }
      }

      this.freeze = outStepYaw == 0.0F && outStepPitch == 0.0F ? this.freeze + 1 : 0;

      float newPitch = Math.max(-90.0F, Math.min(90.0F, aimPitch + outStepPitch));
      outStepPitch = newPitch - aimPitch;

      this.prevStepYaw = this.stepYaw;
      this.prevStepPitch = this.stepPitch;
      this.stepYaw = outStepYaw;
      this.stepPitch = outStepPitch;
      this.prevErrYaw = this.errYaw;
      this.prevErrPitch = this.errPitch;
      this.errYaw = wrap(totalErrYaw - outStepYaw);
      this.errPitch = totalErrPitch - outStepPitch;
      this.onTarget = Math.abs(this.errYaw) <= this.yawSpan && Math.abs(this.errPitch) <= this.pitchSpan;

      return new float[]{this.stepYaw, this.stepPitch};
   }

   private void reset(float goalYaw, float goalPitch, float aimYaw, float aimPitch) {
      this.hidden = this.model.newHidden();
      this.lastGoalYaw = goalYaw;
      this.lastGoalPitch = goalPitch;
      this.errYaw = wrap(goalYaw - aimYaw);
      this.errPitch = goalPitch - aimPitch;
      this.prevErrYaw = this.errYaw;
      this.prevErrPitch = this.errPitch;
      this.stepYaw = 0.0F;
      this.stepPitch = 0.0F;
      this.prevStepYaw = 0.0F;
      this.prevStepPitch = 0.0F;
      this.freeze = 0;
      this.onTarget = false;
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }
}
