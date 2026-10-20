package brokke.cc.rotation;

import brokke.cc.core.AuraChecks;
import brokke.cc.neuro.NeuroModels;
import brokke.cc.util.GCDUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Режим «Нейро» для ауры.
 *
 * Это выход GRU+MDN модели (SpaceTime): фичи «ошибка до цели, её скорость,
 * прошлые шаги, дистанция, таймер с последнего удара, freeze-счётчик» -> намерение
 * модели -> dyaw/dpitch за тик -> GCD-квантование под текущую чувствительность мыши.
 *
 * В отличие от SkyTime здесь нет искусственных слоёв (easing, distraction, отвод).
 * Вместо случайного сэмпла из смеси берётся её ожидание и пропускается через
 * инерцию скорости с ограничением ускорения (см. SpaceTimeDriver, параметр smooth):
 * руки человека так и двигаются — разгон, дуга, торможение. При «Плавности» 0
 * поведение прежнее (чистый сэмпл), при 100 — максимально мягко.
 * Веса берутся из run/brokke/neuro/spacetime.bin (обученные в клиенте), иначе
 * из встроенного spacetime.bin.
 */
public class NeuroRotation extends RotationsSystem {
   private static final long STEP_NANOS = 45000000L;
   private static final long STATE_GAP_NANOS = 500000000L;
   private static final double MAX_DELTA_TIME = 0.05;
   private static final int ROTATION_PRIORITY = 1;
   private static final int FORCED_SAMPLES = 4;
   private static final float DEFAULT_SPEED = 70.0F;
   private static final float DEFAULT_SMOOTH = 0.7F;
   private static final float DEFAULT_FOV = 120.0F;

   private final AimAnchor aimAnchor = new AimAnchor();
   private SpaceTimeDriver driver;
   private int driverRevision = -1;
   private LivingEntity trackedTarget;
   private float requestedYaw;
   private float requestedPitch;
   private long lastUpdateNanos;
   private boolean visibleAim;
   private float speed = DEFAULT_SPEED;
   private float fov = DEFAULT_FOV;
   /** 0 — чистый выход модели, 1 — максимально плавно (инерция + ограничение ускорения). */
   private float smooth = DEFAULT_SMOOTH;

   public NeuroRotation() {
      this.a();
   }

   public void setVisibleAim(boolean var1) {
      this.visibleAim = var1;
   }

   public void setSpeed(float var1) {
      this.speed = MathHelper.clamp(var1, 50.0F, 100.0F);
   }

   public void setFov(float var1) {
      this.fov = MathHelper.clamp(var1, 30.0F, 180.0F);
   }

   /** @param percent 0..100 из настроек ауры */
   public void setSmooth(float percent) {
      this.smooth = MathHelper.clamp(percent / 100.0F, 0.0F, 1.0F);
   }

   /** Сброс состояния: смена цели, выключение режима, загрузка новых весов. */
   public void a() {
      this.trackedTarget = null;
      this.aimAnchor.reset();
      this.lastUpdateNanos = 0L;
      this.driver = null;
      this.driverRevision = -1;
      if (mc.player != null) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
      }
   }

   /** Атака: сбрасываем таймер «тиков с последнего удара» (фича x15). */
   public void b() {
      if (this.driver != null) {
         this.driver.onAttack();
      }
   }

   private SpaceTimeDriver driver() {
      int var1 = NeuroModels.revision();
      if (this.driver == null || this.driverRevision != var1) {
         this.driverRevision = var1;
         this.driver = new SpaceTimeDriver(NeuroModels.get());
      }

      return this.driver;
   }

   /** Цель в поле зрения (как e() у SkyTime). */
   public boolean e(LivingEntity var1) {
      if (mc.player == null || var1 == null) {
         return true;
      }

      Vec3d var2 = var1.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      float var3 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
      float var4 = (float) (-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
      float var5 = Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw()));
      float var6 = Math.abs(var4 - mc.player.getPitch());
      return (float) Math.hypot(var5, var6) <= this.fov;
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
         this.driver = null;
         this.driverRevision = -1;
         return;
      }

      long var3 = System.nanoTime();
      if (this.trackedTarget != var1) {
         this.trackedTarget = var1;
         this.lastUpdateNanos = var3;
         this.aimAnchor.acquire(var3);
         this.driver = null;
         this.driverRevision = -1;
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
      }

      long var5 = this.lastUpdateNanos == 0L ? STEP_NANOS : var3 - this.lastUpdateNanos;
      if (var5 < STEP_NANOS) {
         return;
      }

      this.lastUpdateNanos = var3;
      if (var5 > STATE_GAP_NANOS) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
         this.driver = null;
         this.driverRevision = -1;
      }

      SpaceTimeDriver var7 = this.driver();
      if (var7 == null) {
         return;
      }

      double var8 = Math.min(var5 / 1.0E9, MAX_DELTA_TIME);
      this.aimAnchor.update(var8, var3);
      Box var10 = this.c(var1);
      Vec3d var11 = this.aimAnchor.point(var10, AuraChecks.a(mc, var1));
      Vec3d var12 = var11.subtract(mc.player.getEyePos());
      float var13 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var12.z, var12.x)) - 90.0);
      float var14 = (float) (-Math.toDegrees(Math.atan2(var12.y, var12.horizontalLength())));
      float[] var15 = var7.tick(
         var13,
         var14,
         this.requestedYaw,
         this.requestedPitch,
         mc.player.getEyePos().distanceTo(var11),
         this.scale(),
         FORCED_SAMPLES,
         this.freezeBonus(),
         !var1.isOnGround(),
         this.smooth
      );
      float var16 = MathHelper.wrapDegrees(this.requestedYaw + var15[0]);
      float var17 = MathHelper.clamp(this.requestedPitch + var15[1], -89.5F, 89.5F);

      // GCD: живой игрок двигает мышь квантами, шаг модели тоже округляем под сенсу.
      // Округление (а не отбрасывание остатка) важно: суб-квантовые шаги иначе теряются
      // и прицел «залипает» на полкванта в стороне от цели.
      float var18 = GCDUtil.a();
      if (var18 > 1.0E-5F) {
         float var19 = MathHelper.wrapDegrees(var16 - mc.player.getYaw());
         var19 = Math.round(var19 / var18) * var18;
         var16 = MathHelper.wrapDegrees(mc.player.getYaw() + var19);
         float var20 = var17 - mc.player.getPitch();
         var20 = Math.round(var20 / var18) * var18;
         var17 = MathHelper.clamp(mc.player.getPitch() + var20, -89.5F, 89.5F);
      }

      if (!Float.isFinite(var16) || !Float.isFinite(var17)) {
         this.a();
         return;
      }

      this.requestedYaw = var16;
      this.requestedPitch = var17;
      RotationStorage.a(new Rotation2(var16, var17), 360.0F, 360.0F, 45.0F, 45.0F, 2, ROTATION_PRIORITY, this.visibleAim);
   }

   private float scale() {
      return MathHelper.clamp(this.speed / DEFAULT_SPEED, 0.6F, 1.5F);
   }

   private int freezeBonus() {
      SpaceTimeDriver var1 = this.driver;
      int var2 = 42;
      if (var1 != null) {
         SpaceTimeModel var3 = NeuroModels.get();
         if (var3 != null) {
            var2 = var3.freezeCut;
         }
      }

      int var4 = Math.round(var2 * (100.0F - this.speed) / 50.0F);
      return MathHelper.clamp(var4, 4, var2);
   }
}
