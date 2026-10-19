package brokke.cc.rotation;

import brokke.cc.core.AuraChecks;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.combat.AimAssist;
import brokke.cc.util.Easing;
import brokke.cc.util.GCDUtil;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Режим SkyTime ауры на модели SpaceTime (GRU + MDN) + анти-AI слои:
 *
 *   1) Дискретный выбор easing-кривой каждые 10-20с (нелинейный профиль ускорения);
 *   2) Temporal distraction: раз в 200-800мс цель смещается на 10-200мс (сбивает
 *      детекторы "слишком точного следования");
 *   3) GCD-вариация: ~10% тиков без фикса кванта мыши (обход "Perfect GCD");
 *   4) Отвод после удара + anchor-wander мультипоинт (AimAnchor).
 */
public class SkyTimeRotation extends RotationsSystem {
   private static final String MODEL_RESOURCE = "assets/brokke/rotation/spacetime.bin";
   private static final long STEP_NANOS = 45000000L;
   private static final long STATE_GAP_NANOS = 200000000L;
   private static final double MAX_DELTA_TIME = 0.05;
   private static final float RECOIL_TAU = 0.12F;
   private static final int ROTATION_PRIORITY = 1;
   private static final int FORCED_SAMPLES = 4;
   private static final float DEFAULT_SPEED = 70.0F;
   private static final float GCD_SKIP_CHANCE = 0.1F;

   private static SpaceTimeModel model;
   private static boolean modelTried;

   private final Random random = new Random();
   private final SpaceTimeDriver driver = new SpaceTimeDriver(ensureModel());
   private final AimAnchor aimAnchor = new AimAnchor();
   private LivingEntity trackedTarget;
   private float requestedYaw;
   private float requestedPitch;
   private float recoilYaw;
   private float recoilPitch;
   private long lastUpdateNanos;
   private boolean visibleAim;
   private float fov = 120.0F;
   private float speed = DEFAULT_SPEED;
   private boolean jitter;
   private boolean turnAway = true;

   // анти-AI: смена easing-кривой
   private int easingIndex;
   private long nextEasingSwapNanos;
   // анти-AI: temporal distraction
   private long nextDistractNanos;
   private long distractStartNanos;
   private long distractEndNanos;
   private float distractYaw;
   private float distractPitch;

   private static SpaceTimeModel ensureModel() {
      if (!modelTried) {
         modelTried = true;
         model = SpaceTimeModel.load(MODEL_RESOURCE);
      }

      return model;
   }

   public void setFov(float var1) {
      this.fov = MathHelper.clamp(var1, 1.0F, 180.0F);
   }

   public void setSpeed(float var1) {
      this.speed = MathHelper.clamp(var1, 50.0F, 100.0F);
   }

   public void setJitter(boolean var1) {
      this.jitter = var1;
   }

   public void setTurnAway(boolean var1) {
      this.turnAway = var1;
   }

   public void setVisibleAim(boolean var1) {
      this.visibleAim = var1;
   }

   public void a() {
      this.trackedTarget = null;
      this.recoilYaw = 0.0F;
      this.recoilPitch = 0.0F;
      this.aimAnchor.reset();
      this.lastUpdateNanos = 0L;
      this.nextEasingSwapNanos = 0L;
      this.nextDistractNanos = 0L;
      this.distractStartNanos = 0L;
      this.distractEndNanos = 0L;
      this.driver.invalidate();
      if (mc.player != null) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
      } else {
         this.requestedYaw = 0.0F;
         this.requestedPitch = 0.0F;
      }
   }

   public void b() {
      this.driver.onAttack();
      if (mc.player != null && this.trackedTarget != null) {
         float var1 = this.random.nextBoolean() ? 1.0F : -1.0F;
         float var2 = this.random.nextBoolean() ? 1.0F : -1.0F;
         if (this.turnAway) {
            this.recoilYaw = var1 * this.d(25.0F, 45.0F);
            this.recoilPitch = var2 * this.d(1.0F, 6.0F);
         } else {
            this.recoilYaw = var1 * this.d(0.04F, 0.13F);
            this.recoilPitch = var2 * this.d(0.08F, 0.42F);
         }
      }
   }

   public boolean e(LivingEntity var1) {
      if (mc.player == null || var1 == null) {
         return true;
      } else {
         Vec3d var2 = var1.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
         float var3 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
         float var4 = (float) (-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
         float var5 = Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw()));
         float var6 = Math.abs(var4 - mc.player.getPitch());
         return (float) Math.hypot(var5, var6) <= this.fov;
      }
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player == null || mc.world == null || var1 == null) {
         return;
      }

      RotationStorage var2 = RotationStorage.instance;
      if (var2 != null && var2.getCurrentPriority() > ROTATION_PRIORITY) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
         this.lastUpdateNanos = 0L;
         this.driver.invalidate();
         return;
      }

      long var3 = System.nanoTime();
      if (this.trackedTarget != var1) {
         this.a(var1, var3);
      }

      long var5 = this.lastUpdateNanos == 0L ? STEP_NANOS : var3 - this.lastUpdateNanos;
      if (var5 < STEP_NANOS) {
         return;
      }

      this.lastUpdateNanos = var3;
      if (var5 > STATE_GAP_NANOS) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
         this.driver.invalidate();
      }

      double var7 = Math.min(var5 / 1.0E9, MAX_DELTA_TIME);
      this.aimAnchor.update(var7, var3);
      this.a(var1, var7, var3);
   }

   private void a(LivingEntity var1, long var2) {
      this.trackedTarget = var1;
      this.lastUpdateNanos = var2;
      this.recoilYaw = 0.0F;
      this.recoilPitch = 0.0F;
      this.aimAnchor.acquire(var2);
      this.driver.invalidate();
      if (mc.player != null) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
      }
   }

   private void a(LivingEntity var1, double var2, long var4) {
      Box var6 = this.c(var1);
      Vec3d var7 = this.aimAnchor.point(var6, AuraChecks.a(mc, var1));
      if (this.jitter) {
         var7 = var7.add(
            this.d(-0.03F, 0.03F) * var6.getLengthX(),
            this.d(-0.03F, 0.03F) * var6.getLengthY(),
            this.d(-0.03F, 0.03F) * var6.getLengthZ()
         );
      }

      Vec3d var8 = var7.subtract(mc.player.getEyePos());
      float var9 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var8.z, var8.x)) - 90.0);
      float var10 = (float) (-Math.toDegrees(Math.atan2(var8.y, var8.horizontalLength())));
      float var11 = (float) Math.exp(-var2 / RECOIL_TAU);
      this.recoilYaw *= var11;
      this.recoilPitch *= var11;
      if (Math.abs(this.recoilYaw) < 0.01F) {
         this.recoilYaw = 0.0F;
      }

      if (Math.abs(this.recoilPitch) < 0.01F) {
         this.recoilPitch = 0.0F;
      }

      var9 = MathHelper.wrapDegrees(var9 + this.recoilYaw);
      var10 = MathHelper.clamp(var10 + this.recoilPitch, -89.0F, 89.0F);
      float[] var18 = this.distract(var4, var9, var10);
      float[] var12 = this.a(var18[0], var18[1], mc.player.getEyePos().distanceTo(var7), var2);
      AimAssist var19 = FunctionManager.a(AimAssist.class);
      if (var19 != null) {
         float var20 = var12[0];
         float var21 = var12[1];
         double var22 = var19.correction(SpaceTimeDriver.wrap(var18[0] - (this.requestedYaw + var20)), var20, (float) var2, false);
         double var24 = var19.correction(var18[1] - (this.requestedPitch + var21), var21, (float) var2, true);
         var12[0] = SpaceTimeDriver.wrap(var20 + (float) var22);
         var12[1] = var21 + (float) var24;
      }

      float var13 = MathHelper.wrapDegrees(this.requestedYaw + var12[0]);
      float var14 = MathHelper.clamp(this.requestedPitch + var12[1], -90.0F, 90.0F);
      float var15 = GCDUtil.a();
      if (var15 > 1.0E-5F && ThreadLocalRandom.current().nextFloat() >= GCD_SKIP_CHANCE) {
         float var16 = MathHelper.wrapDegrees(var13 - mc.player.getYaw());
         var16 -= var16 % var15;
         var13 = MathHelper.wrapDegrees(mc.player.getYaw() + var16);
         float var17 = var14 - mc.player.getPitch();
         var17 -= var17 % var15;
         var14 = MathHelper.clamp(mc.player.getPitch() + var17, -90.0F, 90.0F);
      }

      if (Float.isFinite(var13) && Float.isFinite(var14)) {
         this.requestedYaw = var13;
         this.requestedPitch = var14;
         RotationStorage.a(new Rotation2(var13, var14), 360.0F, 360.0F, 45.0F, 45.0F, 2, ROTATION_PRIORITY, this.visibleAim);
      } else {
         this.a();
      }
   }

   /**
    * Temporal distraction: раз в 200-800мс goal смещается на 10-200мс по easing-кривой,
    * которая каждые 10-20с меняется — нелинейный профиль вместо статичного синуса.
    */
   private float[] distract(long now, float yaw, float pitch) {
      if (now >= this.nextEasingSwapNanos) {
         this.easingIndex = ThreadLocalRandom.current().nextInt(Easing.COUNT);
         this.nextEasingSwapNanos = now + 10000000000L + (long) (this.random.nextDouble() * 1.0E10);
      }

      if (now >= this.distractEndNanos && now >= this.nextDistractNanos) {
         this.distractStartNanos = now;
         this.distractEndNanos = now + 10000000L + (long) (this.random.nextDouble() * 190000000L);
         this.distractYaw = (this.random.nextBoolean() ? 1.0F : -1.0F) * this.d(1.0F, 5.0F);
         this.distractPitch = (this.random.nextBoolean() ? 1.0F : -1.0F) * this.d(0.5F, 3.0F);
         this.nextDistractNanos = this.distractEndNanos + 200000000L + (long) (this.random.nextDouble() * 600000000L);
      }

      float env = 0.0F;
      if (now < this.distractEndNanos && this.distractEndNanos > this.distractStartNanos) {
         float p = (float) (now - this.distractStartNanos) / (float) (this.distractEndNanos - this.distractStartNanos);
         env = p < 0.5F ? Easing.apply(this.easingIndex, p * 2.0F) : Easing.apply(this.easingIndex, (1.0F - p) * 2.0F);
      }

      return new float[]{MathHelper.wrapDegrees(yaw + this.distractYaw * env), MathHelper.clamp(pitch + this.distractPitch * env, -90.0F, 90.0F)};
   }

   /** Шаг к цели: нейросетевой драйвер SpaceTime или сглаживание, если веса не загрузились. */
   private float[] a(float goalYaw, float goalPitch, double dist, double dt) {
      if (model != null) {
         return this.driver.tick(goalYaw, goalPitch, this.requestedYaw, this.requestedPitch, dist, this.scale(), FORCED_SAMPLES, this.freezeBonus());
      } else {
         float var5 = -0.016666668F / (float) Math.log(1.0 - MathHelper.clamp(this.speed / 100.0F, 0.1F, 0.999F));
         float var6 = 1.0F - (float) Math.exp(-(float) dt / var5);
         return new float[]{SpaceTimeDriver.wrap(goalYaw - this.requestedYaw) * var6, (goalPitch - this.requestedPitch) * var6};
      }
   }

   /** Настройка "Скорость ротации": масштаб дельт модели. */
   private float scale() {
      return MathHelper.clamp(this.speed / DEFAULT_SPEED, 0.6F, 1.5F);
   }

   /** Настройка "Скорость ротации": через сколько тиков покоя форсировать движение. */
   private int freezeBonus() {
      int var1 = model != null ? model.freezeCut : 42;
      int var2 = Math.round(var1 * (100.0F - this.speed) / 50.0F);
      return MathHelper.clamp(var2, 4, var1);
   }

   private float d(float var1, float var2) {
      return var1 + this.random.nextFloat() * (var2 - var1);
   }
}
