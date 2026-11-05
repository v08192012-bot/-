package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.combat.Aura;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Режим SpookyTest: ротация под античит SpookyTime (Grim + нейроэвристики).
 *
 * Собрана из четырёх механизмов, каждый из которых закрывает конкретную проверку:
 *
 *   1) Плавающая точка прицела (floating multipoint). Точка внутри хитбокса живёт своей
 *      инерцией с затуханием и отскоками от границ, поэтому луч непрерывно смещается —
 *      нет «статичного вектора наводки», который ловят эвристики на зажатый aim.
 *   2) Связанные дельты (coupled deltas). Если одна ось почти не двигается, а вторая
 *      едет, добавляется микро-сдвиг по «стоящей» оси: чистых вертикальных/горизонтальных
 *      переводок (AimModulo360 / независимость осей) не остаётся.
 *   3) Нелинейная скорость + удержание после удара. 165 мс после удара прицел живёт
 *      микродрожью (рука «отдыхает»), скорость зависит от дистанции и величины ошибки,
 *      максимум шага ограничен; плюс редкие паузы на 1–2 тика.
 *   4) Шум Орнштейна-Уленбека + «дыхание» (две синусоиды) + периодические рывки.
 *      Белый шум даёт дёрганый прицел и рваные дельты; OU-процесс с возвратом к среднему
 *      выглядит как живая рука, а рывки дают естественные перелёты.
 *
 * Сверху — квантование в сетку GCD относительно последней отправленной ротации
 * (иначе Grim AimProcessor видит «невозможные» пиксели) и плавный возврат камеры
 * после потери цели (25 мс удержания + 75 мс интерполяции).
 *
 * Логика атаки/критов/сброса спринта не дублируется: её делает сама Aura, здесь только
 * наводка. Единственное, что режим берёт на себя, — сообщает, что удар состоялся, чтобы
 * включить окно удержания.
 */
public class SpookyTestRotation extends RotationsSystem {
   private static final int ROTATION_PRIORITY = 3;
   /** Сколько миллисекунд после удара прицел «отдыхает» микродрожью. */
   private static final long POST_ATTACK_HOLD_MS = 165L;
   /** Удержание перед возвратом камеры. */
   private static final long HOLD_BEFORE_RETURN_MS = 25L;
   /** Длительность плавной интерполяции обратно к взгляду игрока. */
   private static final long RETURN_INTERPOLATION_MS = 75L;
   private static final float PITCH_LIMIT = 89.5F;

   private final Random random = new Random();

   /** Плавающая точка прицела: смещение внутри хитбокса и его скорость. */
   private double wanderX;
   private double wanderY;
   private double wanderZ;
   private double wanderVelocityX;
   private double wanderVelocityY;
   private double wanderVelocityZ;

   /** Шум Орнштейна-Уленбека по каждой оси. */
   private double ornsteinYaw;
   private double ornsteinPitch;

   /** Периодические рывки: сколько тиков ещё длится и когда следующий. */
   private int joltTicks;
   private int joltCooldown = 35;
   private float joltForce;

   /** Короткие паузы «рука замерла». */
   private int pauseTicks;
   private int pauseCooldown = 45;

   private LivingEntity trackedTarget;
   private double lastCooldown = 1.0;
   private long lastAttackMillis;

   private float speedScale = 1.0F;
   private boolean visibleAim = true;

   /** Плавный возврат камеры после потери цели. */
   private boolean returning;
   private long returnHoldUntil;
   private long returnStartMillis;
   private float returnFromYaw;
   private float returnFromPitch;

   public SpookyTestRotation() {
      // Возврат камеры живёт дольше, чем цель: пока цель есть, Aura зовёт a(target),
      // а на потере цели кадр ведёт обработчик события тика.
      EventBus.register(this);
   }

   public void setVisibleAim(boolean value) {
      this.visibleAim = value;
   }

   /** «Скорость SpookyTest», %: масштаб шага и сглаживания (100% = как в эталоне). */
   public void setSpeed(float percent) {
      this.speedScale = MathHelper.clamp(percent / 100.0F, 0.5F, 1.5F);
   }

   /** Полный сброс: выключение модуля, смена цели, смена режима. */
   public void a() {
      this.trackedTarget = null;
      this.lastCooldown = 1.0;
      this.lastAttackMillis = 0L;
      this.returning = false;
      this.wanderX = this.wanderY = this.wanderZ = 0.0;
      this.wanderVelocityX = this.wanderVelocityY = this.wanderVelocityZ = 0.0;
      this.ornsteinYaw = this.ornsteinPitch = 0.0;
      this.joltTicks = 0;
      this.joltCooldown = 35;
      this.pauseTicks = 0;
      this.pauseCooldown = 45;
   }

   /** Начать плавный возврат к взгляду игрока (зовётся, когда цель пропала). */
   public void beginReturn() {
      if (mc.player == null) {
         return;
      }

      this.returning = true;
      this.returnHoldUntil = System.currentTimeMillis() + HOLD_BEFORE_RETURN_MS;
      this.returnStartMillis = 0L;
      this.returnFromYaw = mc.player.getYaw();
      this.returnFromPitch = mc.player.getPitch();
      this.wanderVelocityX = this.wanderVelocityY = this.wanderVelocityZ = 0.0;
   }

   @Override
   public void a(LivingEntity target) {
      if (mc.player == null || mc.world == null || target == null) {
         return;
      }

      RotationStorage storage = RotationStorage.instance;
      if (storage != null && storage.getCurrentPriority() > ROTATION_PRIORITY) {
         this.a();
         return;
      }

      this.returning = false;
      if (target != this.trackedTarget) {
         this.trackedTarget = target;
         this.onTargetSwitched();
      }

      long now = System.currentTimeMillis();
      this.trackAttack(now);

      Box box = this.c(target);
      if (box == null) {
         return;
      }

      Vec3d aimPoint = this.dynamicAimPoint(box);
      Vec3d eyes = mc.player.getEyePos();
      double dx = aimPoint.x - eyes.x;
      double dy = aimPoint.y - eyes.y;
      double dz = aimPoint.z - eyes.z;
      if (dx * dx + dy * dy + dz * dz < 1.0E-8) {
         return;
      }

      float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      float targetPitch = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))), -PITCH_LIMIT, PITCH_LIMIT);
      float currentYaw = mc.player.getYaw();
      float currentPitch = mc.player.getPitch();
      double distance = eyes.distanceTo(aimPoint);

      float nextYaw;
      float nextPitch;
      if (this.lastAttackMillis > 0L && now - this.lastAttackMillis < POST_ATTACK_HOLD_MS) {
         // Сразу после удара рука «отдыхает»: только микродрожь, никакой доводки.
         nextYaw = currentYaw + this.randomRange(-0.35F, 0.35F);
         nextPitch = MathHelper.clamp(currentPitch + this.randomRange(-0.22F, 0.22F), -PITCH_LIMIT, PITCH_LIMIT);
      } else if (this.tickPause()) {
         nextYaw = currentYaw;
         nextPitch = currentPitch;
      } else {
         nextYaw = this.stepYaw(currentYaw, targetYaw, distance, now);
         nextPitch = MathHelper.clamp(this.stepPitch(currentPitch, targetPitch, distance, now), -PITCH_LIMIT, PITCH_LIMIT);
      }

      this.publish(nextYaw, nextPitch, currentYaw, currentPitch);
   }

   /** Обработчик тика: только плавный возврат камеры, когда цели уже нет. */
   @EventHandler
   public void onTick(UpdateEvent event) {
      if (!this.returning || mc.player == null) {
         return;
      }

      Aura aura = Aura.INSTANCE;
      if (aura == null || !aura.isActive() || !aura.rotationType.isSelected("SpookyTest")) {
         this.returning = false;
         return;
      }

      if (aura.getTarget() != null) {
         // Цель вернулась — возврат отменяем, дальше ведёт обычная логика.
         this.returning = false;
         return;
      }

      long now = System.currentTimeMillis();
      float cameraYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
      float cameraPitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : mc.player.getPitch();

      if (now < this.returnHoldUntil) {
         this.push(this.returnFromYaw, this.returnFromPitch, 0.0F, 0.0F);
         return;
      }

      if (this.returnStartMillis == 0L) {
         this.returnStartMillis = now;
      }

      float progress = MathHelper.clamp((now - this.returnStartMillis) / (float)RETURN_INTERPOLATION_MS, 0.0F, 1.0F);
      // Плавное начало и мягкое окончание: линейный lerp выглядит как «доезд по рельсе».
      float eased = progress * (0.5F + 0.5F * progress);
      float yaw = this.returnFromYaw + eased * MathHelper.wrapDegrees(cameraYaw - this.returnFromYaw);
      float pitch = MathHelper.clamp(this.returnFromPitch + eased * (cameraPitch - this.returnFromPitch), -PITCH_LIMIT, PITCH_LIMIT);

      if (progress >= 1.0F) {
         this.returning = false;
         if (RotationStorage.instance != null) {
            RotationStorage.instance.j();
         }
         return;
      }

      this.push(yaw, pitch, 0.0F, 0.0F);
   }

   // ------------------------------------------------------------------ наводка

   /** Плавающая точка прицела с инерцией, затуханием и отскоками от границ хитбокса. */
   private Vec3d dynamicAimPoint(Box box) {
      double halfX = (box.maxX - box.minX) * 0.36;
      double halfY = (box.maxY - box.minY) * 0.38;
      double halfZ = (box.maxZ - box.minZ) * 0.36;

      if (this.random.nextFloat() < 0.12F) {
         this.wanderVelocityX = this.randomRange(-0.065F, 0.065F);
         this.wanderVelocityY = this.randomRange(-0.055F, 0.055F);
         this.wanderVelocityZ = this.randomRange(-0.065F, 0.065F);
      } else {
         this.wanderVelocityX += this.randomRange(-0.018F, 0.018F);
         this.wanderVelocityY += this.randomRange(-0.015F, 0.015F);
         this.wanderVelocityZ += this.randomRange(-0.018F, 0.018F);
      }

      this.wanderVelocityX *= 0.91;
      this.wanderVelocityY *= 0.91;
      this.wanderVelocityZ *= 0.91;

      this.wanderX = MathHelper.clamp(this.wanderX + this.wanderVelocityX, -halfX, halfX);
      this.wanderY = MathHelper.clamp(this.wanderY + this.wanderVelocityY, -halfY, halfY);
      this.wanderZ = MathHelper.clamp(this.wanderZ + this.wanderVelocityZ, -halfZ, halfZ);

      if (Math.abs(this.wanderX) >= halfX * 0.95) {
         this.wanderVelocityX = -this.wanderVelocityX * 0.75;
      }

      if (Math.abs(this.wanderY) >= halfY * 0.95) {
         this.wanderVelocityY = -this.wanderVelocityY * 0.75;
      }

      if (Math.abs(this.wanderZ) >= halfZ * 0.95) {
         this.wanderVelocityZ = -this.wanderVelocityZ * 0.75;
      }

      // 0.62 по высоте — уровень груди: по нему луч проходит в бокс и на бегу, и в прыжке.
      return new Vec3d(
         (box.minX + box.maxX) * 0.5 + this.wanderX,
         box.minY + (box.maxY - box.minY) * 0.62 + this.wanderY,
         (box.minZ + box.maxZ) * 0.5 + this.wanderZ
      );
   }

   private float stepYaw(float currentYaw, float targetYaw, double distance, long now) {
      float delta = MathHelper.clamp(MathHelper.wrapDegrees(targetYaw - currentYaw), -280.0F, 280.0F);
      delta = this.coupleAxis(delta, 0.22F, 0.64F);

      float maxStep = (60.0F + this.randomRange(-3.5F, 4.5F)) * this.speedScale;
      float smoothing = this.smoothingFactor(distance, delta) * this.distanceFactor(distance) * 0.78F;

      if (this.random.nextFloat() < 0.045F && Math.abs(delta) > 6.0F) {
         smoothing *= this.randomRange(1.08F, 1.24F);
      }

      this.updateNoise();
      float step = MathHelper.clamp(delta, -maxStep, maxStep) * MathHelper.clamp(smoothing, 0.18F, 0.92F);
      return currentYaw + step + (float)this.ornsteinYaw + this.updateJolt() + (float)(Math.sin(now / 280.0) * 0.045);
   }

   private float stepPitch(float currentPitch, float targetPitch, double distance, long now) {
      float delta = MathHelper.clamp(targetPitch - currentPitch, -90.0F, 90.0F);
      delta = this.coupleAxis(delta, 0.18F, 0.52F);

      float maxStep = this.randomRange(22.8F, 26.5F) * this.speedScale;
      float smoothing = this.smoothingFactor(distance, Math.abs(delta)) * this.distanceFactor(distance) * 0.54F;

      float step = MathHelper.clamp(delta, -maxStep, maxStep) * MathHelper.clamp(smoothing, 0.14F, 0.76F);
      return currentPitch + step + (float)this.ornsteinPitch + (float)(Math.cos(now / 460.0) * 0.032);
   }

   /**
    * Связанные дельты: ось, которая «стоит», получает микро-сдвиг, чтобы в пакетах
    * не было чистого вертикального или горизонтального движения.
    */
   private float coupleAxis(float delta, float min, float max) {
      float sign = this.random.nextBoolean() ? 1.0F : -1.0F;
      return delta + sign * this.randomRange(min, max);
   }

   private float smoothingFactor(double distance, float delta) {
      if (distance < 2.8 && Math.abs(delta) < 12.0F) {
         return this.randomRange(0.28F, 0.42F);
      }

      return Math.abs(delta) > 32.0F ? this.randomRange(0.62F, 0.78F) : this.randomRange(0.45F, 0.64F);
   }

   private float distanceFactor(double distance) {
      if (distance < 2.0) {
         return this.randomRange(0.82F, 1.15F);
      }

      return distance < 4.0 ? this.randomRange(1.18F, 1.55F) : this.randomRange(0.92F, 1.28F);
   }

   /** Шум Орнштейна-Уленбека: возврат к среднему вместо белого шума. */
   private void updateNoise() {
      this.ornsteinYaw += -0.35 * this.ornsteinYaw + 0.55 * this.random.nextGaussian();
      this.ornsteinPitch += -0.35 * this.ornsteinPitch + 0.28 * this.random.nextGaussian();
      this.ornsteinYaw = MathHelper.clamp(this.ornsteinYaw, -2.4, 2.4);
      this.ornsteinPitch = MathHelper.clamp(this.ornsteinPitch, -1.2, 1.2);
   }

   /** Редкий короткий рывок: перелёт на 2–3 тика, как неаккуратная рука. */
   private float updateJolt() {
      if (--this.joltCooldown <= 0 && this.joltTicks <= 0) {
         this.joltTicks = 2 + this.random.nextInt(2);
         this.joltCooldown = 35 + this.random.nextInt(45);
         this.joltForce = (this.random.nextBoolean() ? 1.0F : -1.0F) * this.randomRange(2.5F, 5.5F);
      }

      if (this.joltTicks > 0) {
         float value = this.joltForce * (this.joltTicks / 3.0F);
         this.joltTicks--;
         return value;
      }

      return 0.0F;
   }

   /** Короткая пауза «рука замерла»: 1–2 тика раз в 38–62 тика. */
   private boolean tickPause() {
      if (--this.pauseCooldown <= 0 && this.pauseTicks <= 0) {
         this.pauseTicks = 1 + this.random.nextInt(2);
         this.pauseCooldown = 38 + this.random.nextInt(25);
      }

      if (this.pauseTicks > 0) {
         this.pauseTicks--;
         return true;
      }

      return false;
   }

   // ------------------------------------------------------------------ вывод

   /** Квантование в сетку GCD относительно последней отправленной ротации + публикация. */
   private void publish(float desiredYaw, float desiredPitch, float currentYaw, float currentPitch) {
      float gcd = GCDUtil.a();
      float yaw = desiredYaw;
      float pitch = MathHelper.clamp(desiredPitch, -PITCH_LIMIT, PITCH_LIMIT);

      if (gcd > 1.0E-4F) {
         float yawDelta = MathHelper.wrapDegrees(desiredYaw - currentYaw);
         float pitchDelta = desiredPitch - currentPitch;
         yaw = currentYaw + Math.round(yawDelta / gcd) * gcd;
         pitch = MathHelper.clamp(currentPitch + Math.round(pitchDelta / gcd) * gcd, -PITCH_LIMIT, PITCH_LIMIT);

         if (Math.abs(yaw - currentYaw) < 0.008F && Math.abs(pitch - currentPitch) < 0.008F) {
            yaw = currentYaw;
            pitch = currentPitch;
         }
      }

      float deltaYaw = MathHelper.wrapDegrees(yaw - currentYaw);
      float deltaPitch = pitch - currentPitch;
      this.push(yaw, pitch, deltaYaw, deltaPitch);
   }

   /**
    * Отправка в общий пайплайн. Скорость ставим равной собственной дельте: сглаживание
    * в хранилище съело бы пиксели, которые уже посчитаны по GCD.
    */
   private void push(float yaw, float pitch, float deltaYaw, float deltaPitch) {
      RotationStorage.a(
         new Rotation2(yaw, pitch),
         Math.abs(deltaYaw) + 0.05F,
         Math.abs(deltaPitch) + 0.05F,
         30.0F,
         20.0F,
         2,
         ROTATION_PRIORITY,
         this.visibleAim
      );

      this.updateBody(yaw, pitch);
   }

   /** Голова и корпус доворачиваются за прицелом, как у живой модели. */
   private void updateBody(float yaw, float pitch) {
      if (mc.player == null) {
         return;
      }

      mc.player.setHeadYaw(yaw);
      float bodyDelta = MathHelper.wrapDegrees(yaw - mc.player.bodyYaw);
      mc.player.bodyYaw += MathHelper.clamp(bodyDelta, -32.0F, 32.0F);
   }

   // ------------------------------------------------------------------ состояние

   private void trackAttack(long now) {
      if (mc.player == null) {
         return;
      }

      double cooldown = mc.player.getAttackCooldownProgress(0.0F);
      if (cooldown < this.lastCooldown - 1.0E-4) {
         this.lastAttackMillis = now;
         this.wanderVelocityX += this.randomRange(-0.12F, 0.12F);
         this.wanderVelocityY += this.randomRange(-0.09F, 0.09F);
         this.wanderVelocityZ += this.randomRange(-0.12F, 0.12F);
      }

      this.lastCooldown = cooldown;
   }

   private void onTargetSwitched() {
      this.wanderVelocityX = this.randomRange(-0.08F, 0.08F);
      this.wanderVelocityY = this.randomRange(-0.06F, 0.06F);
      this.wanderVelocityZ = this.randomRange(-0.08F, 0.08F);
   }

   private float randomRange(float minimum, float maximum) {
      return minimum + this.random.nextFloat() * (maximum - minimum);
   }
}
