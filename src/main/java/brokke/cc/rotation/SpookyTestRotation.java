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
 * Режим SpookyTest: ротация под античит SpookyTime, легитная версия.
 *
 * Задача — не «дёрганый aim», а рука: прицел разгоняется и тормозит плавно, никогда
 * не меняет направление на противоположное десятки раз в секунду и всегда стоит внутри
 * хитбокса. Поэтому движение построено на втором порядке (скорость + ограничение
 * ускорения), а не на «доля от ошибки + шум каждый тик».
 *
 * Что здесь есть:
 *   1) Плавающая точка прицела в ГРАДУСАХ (не в блоках): смещение ограничено ±1.35° по
 *      yaw и ±0.95° по pitch, меняется плавно. Раньше точка гуляла по всему хитбоксу —
 *      это давало до ±4° на дистанции 3 блока и выглядело как тряска.
 *   2) Ограничение скорости (16°/10° за тик) и УСКОРЕНИЯ (2.6°/1.8° за тик²): переводка
 *      получается S-кривой, без рывков и без «телепорта» прицела.
 *   3) Мёртвая зона и гашение на подлёте: у цели прицел не перелетает и не болтается
 *      туда-сюда, а останавливается.
 *   4) Шум Орнштейна-Уленбека малой амплитуды (±0.30°/±0.18°), включается только когда
 *      прицел уже ведёт цель — во время переводки он не мешает и не даёт «пилы».
 *   5) Редкие паузы «рука замерла» и мягкие перелёты (0.5–1.1°, только по ходу движения).
 *   6) Квантование в сетку GCD с НАКОПЛЕНИЕМ ОСТАТКА: шаг меньше пикселя не теряется и не
 *      превращается в дрожь, а одиночные однопиксельные развороты гасятся.
 *   7) Плавный возврат камеры после потери цели (25 мс удержания + 75 мс интерполяции).
 */
public class SpookyTestRotation extends RotationsSystem {
   private static final int ROTATION_PRIORITY = 3;
   private static final float PITCH_LIMIT = 89.5F;

   /** Предельная скорость прицела за тик: больше — уже «флик», который ловят. */
   private static final float MAX_YAW_STEP = 16.0F;
   private static final float MAX_PITCH_STEP = 10.0F;
   /** Предельное ускорение: именно оно делает переводку плавной, а не ступенчатой. */
   private static final float MAX_YAW_ACCEL = 2.6F;
   private static final float MAX_PITCH_ACCEL = 1.8F;
   /** Доля ошибки, которую прицел выбирает за тик (до ограничителей выше). */
   private static final float RESPONSE = 0.42F;
   /** Зона, в которой прицел считается «на цели» и не двигается. */
   private static final float DEADZONE_YAW = 0.28F;
   private static final float DEADZONE_PITCH = 0.22F;

   /** Сколько миллисекунд после удара ротация держится неподвижно. */
   private static final long POST_ATTACK_HOLD_MS = 130L;
   private static final long HOLD_BEFORE_RETURN_MS = 25L;
   private static final long RETURN_INTERPOLATION_MS = 75L;

   /** Амплитуда «гуляния» прицела внутри хитбокса, в градусах. */
   private static final double WANDER_AMPLITUDE_YAW = 1.35;
   private static final double WANDER_AMPLITUDE_PITCH = 0.95;

   private final Random random = new Random();

   /** Смещение точки прицела внутри хитбокса (в градусах) и его скорость. */
   private double wanderYaw;
   private double wanderPitch;
   private double wanderVelYaw;
   private double wanderVelPitch;

   /** Текущая угловая скорость прицела, °/тик. */
   private float velocityYaw;
   private float velocityPitch;

   /** Шум Орнштейна-Уленбека. */
   private double noiseYaw;
   private double noisePitch;

   /** Мягкий перелёт: сколько тиков ещё длится и когда следующий. */
   private int overshootTicks;
   private int overshootCooldown = 70;
   private float overshootForce;

   /** Короткие паузы «рука замерла». */
   private int pauseTicks;
   private int pauseCooldown = 70;

   /** Остаток квантования, чтобы шаг меньше пикселя накапливался, а не дрожал. */
   private double residualYaw;
   private double residualPitch;
   /** Знак последнего отправленного шага — гасим однопиксельные развороты. */
   private int lastPixelSignYaw;
   private int lastPixelSignPitch;

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
      // а после потери цели кадром занимается обработчик события тика.
      EventBus.register(this);
   }

   public void setVisibleAim(boolean value) {
      this.visibleAim = value;
   }

   /** «Скорость SpookyTest», %: масштаб скорости и ускорения (100% = базовая). */
   public void setSpeed(float percent) {
      this.speedScale = MathHelper.clamp(percent / 100.0F, 0.5F, 1.5F);
   }

   /** Полный сброс: выключение модуля, смена цели, смена режима. */
   public void a() {
      this.trackedTarget = null;
      this.lastCooldown = 1.0;
      this.lastAttackMillis = 0L;
      this.returning = false;
      this.wanderYaw = this.wanderPitch = 0.0;
      this.wanderVelYaw = this.wanderVelPitch = 0.0;
      this.velocityYaw = this.velocityPitch = 0.0F;
      this.noiseYaw = this.noisePitch = 0.0;
      this.overshootTicks = 0;
      this.overshootCooldown = 70;
      this.pauseTicks = 0;
      this.pauseCooldown = 70;
      this.residualYaw = this.residualPitch = 0.0;
      this.lastPixelSignYaw = this.lastPixelSignPitch = 0;
   }

   /** Начать плавный возврат к взгляду игрока (цель пропала). */
   public void beginReturn() {
      if (mc.player == null) {
         return;
      }

      this.returning = true;
      this.returnHoldUntil = System.currentTimeMillis() + HOLD_BEFORE_RETURN_MS;
      this.returnStartMillis = 0L;
      this.returnFromYaw = mc.player.getYaw();
      this.returnFromPitch = mc.player.getPitch();
      this.velocityYaw = this.velocityPitch = 0.0F;
      this.wanderVelYaw = this.wanderVelPitch = 0.0;
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
         // Смена цели: не тащим импульс с прошлой цели, начинаем с нуля.
         this.velocityYaw = this.velocityPitch = 0.0F;
         this.wanderVelYaw = this.wanderVelPitch = 0.0;
      }

      long now = System.currentTimeMillis();
      this.trackAttack(now);

      Box box = this.c(target);
      if (box == null) {
         return;
      }

      Vec3d eyes = mc.player.getEyePos();
      double baseX = (box.minX + box.maxX) * 0.5;
      double baseY = box.minY + (box.maxY - box.minY) * 0.62;
      double baseZ = (box.minZ + box.maxZ) * 0.5;
      double dx = baseX - eyes.x;
      double dy = baseY - eyes.y;
      double dz = baseZ - eyes.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (distance < 0.05) {
         return;
      }

      float baseYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      float basePitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
      this.updateWander();
      float wantYaw = baseYaw + (float)this.wanderYaw;
      float wantPitch = MathHelper.clamp(basePitch + (float)this.wanderPitch, -PITCH_LIMIT, PITCH_LIMIT);

      float currentYaw = mc.player.getYaw();
      float currentPitch = mc.player.getPitch();

      // Окно после удара: рука «отдыхает», прицел стоит. Раньше здесь был рандом
      // ±0.35° — именно он превращал бой в дрожь.
      if (this.lastAttackMillis > 0L && now - this.lastAttackMillis < POST_ATTACK_HOLD_MS) {
         this.velocityYaw *= 0.60F;
         this.velocityPitch *= 0.60F;
         this.publish(currentYaw, currentPitch);
         return;
      }

      if (this.tickPause()) {
         this.velocityYaw *= 0.70F;
         this.velocityPitch *= 0.70F;
         this.publish(currentYaw, currentPitch);
         return;
      }

      float errYaw = MathHelper.wrapDegrees(wantYaw - currentYaw);
      float errPitch = wantPitch - currentPitch;
      float absYaw = Math.abs(errYaw);
      float absPitch = Math.abs(errPitch);

      float stepYaw = this.stepAxis(errYaw, absYaw, true, distance);
      float stepPitch = this.stepAxis(errPitch, absPitch, false, distance);

      // Шум и перелёт считаются РОВНО один раз за тик и прибавляются к шагу, а не к
      // состоянию скорости: иначе шум интегрировался бы в случайное блуждание.
      this.updateNoise();
      float noise = absYaw < 10.0F ? (float)this.noiseYaw : 0.0F;
      float noisePitch = absPitch < 10.0F ? (float)this.noisePitch : 0.0F;
      float overshoot = Math.abs(this.velocityYaw) > 1.0F ? Math.copySign(this.tickOvershoot(), this.velocityYaw) : 0.0F;

      stepYaw += noise + overshoot;
      stepPitch += noisePitch;

      // «Дыхание» — две медленные синусоиды, амплитуда маленькая и плавная.
      stepYaw += (float)(Math.sin(now / 640.0) * 0.020);
      stepPitch += (float)(Math.cos(now / 820.0) * 0.014);

      // У самой цели гасим шаг: перелёт и возврат — это и есть «болтанка».
      if (absYaw < 0.60F) {
         stepYaw *= 0.45F;
      }

      if (absPitch < 0.50F) {
         stepPitch *= 0.45F;
      }

      this.publish(currentYaw + stepYaw, currentPitch + stepPitch);
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
         this.returning = false;
         return;
      }

      long now = System.currentTimeMillis();
      float cameraYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
      float cameraPitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : mc.player.getPitch();

      if (now < this.returnHoldUntil) {
         this.publish(this.returnFromYaw, this.returnFromPitch);
         return;
      }

      if (this.returnStartMillis == 0L) {
         this.returnStartMillis = now;
      }

      float progress = MathHelper.clamp((now - this.returnStartMillis) / (float)RETURN_INTERPOLATION_MS, 0.0F, 1.0F);
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

      this.publish(yaw, pitch);
   }

   // ------------------------------------------------------------------ движение

   /**
    * Один ограничитель на ось: целевая скорость из ошибки, ограничение ускорения,
    * торможение на подлёте и мёртвая зона.
    */
   private float stepAxis(float error, float absError, boolean yawAxis, double distance) {
      float maxStep = (yawAxis ? MAX_YAW_STEP : MAX_PITCH_STEP) * this.speedScale;
      float maxAccel = (yawAxis ? MAX_YAW_ACCEL : MAX_PITCH_ACCEL) * this.speedScale;
      float velocity = yawAxis ? this.velocityYaw : this.velocityPitch;

      // Мёртвая зона: прицел уже на цели — стоим, а не «доигрываем» пиксели.
      float deadzone = yawAxis ? DEADZONE_YAW : DEADZONE_PITCH;
      if (absError < deadzone && Math.abs(velocity) < 0.25F) {
         velocity = 0.0F;
         this.setVelocity(yawAxis, 0.0F);
         return 0.0F;
      }

      float distanceFactor = yawAxis ? 1.0F : 0.92F;
      if (distance > 4.0) {
         distanceFactor *= 0.94F;
      }

      float desired = error * RESPONSE * this.speedScale * distanceFactor;
      desired = MathHelper.clamp(desired, -maxStep, maxStep);

      // Торможение, когда цель уже близко: скорость не больше остатка ошибки.
      if (absError < 3.0F) {
         desired = MathHelper.clamp(desired, -absError * 0.8F - 0.05F, absError * 0.8F + 0.05F);
      }

      float accel = MathHelper.clamp(desired - velocity, -maxAccel, maxAccel);
      velocity += accel;

      // Перелёт: движемся в сторону, противоположную ошибке — тормозим быстрее.
      if (error * velocity < 0.0F) {
         velocity *= 0.55F;
      }

      velocity = MathHelper.clamp(velocity, -maxStep, maxStep);
      this.setVelocity(yawAxis, velocity);
      return velocity;
   }

   private void setVelocity(boolean yawAxis, float value) {
      if (yawAxis) {
         this.velocityYaw = value;
      } else {
         this.velocityPitch = value;
      }
   }

   /** Плавное «гуляние» прицела внутри хитбокса: ограничено и по амплитуде, и по скорости. */
   private void updateWander() {
      if (this.random.nextFloat() < 0.08F) {
         this.wanderVelYaw = this.randomRange(-0.11F, 0.11F);
         this.wanderVelPitch = this.randomRange(-0.08F, 0.08F);
      } else {
         this.wanderVelYaw += this.randomRange(-0.022F, 0.022F);
         this.wanderVelPitch += this.randomRange(-0.016F, 0.016F);
      }

      this.wanderVelYaw *= 0.90;
      this.wanderVelPitch *= 0.90;

      this.wanderYaw = MathHelper.clamp(this.wanderYaw + this.wanderVelYaw, -WANDER_AMPLITUDE_YAW, WANDER_AMPLITUDE_YAW);
      this.wanderPitch = MathHelper.clamp(this.wanderPitch + this.wanderVelPitch, -WANDER_AMPLITUDE_PITCH, WANDER_AMPLITUDE_PITCH);

      if (Math.abs(this.wanderYaw) >= WANDER_AMPLITUDE_YAW * 0.9) {
         this.wanderVelYaw = -this.wanderVelYaw * 0.6;
      }

      if (Math.abs(this.wanderPitch) >= WANDER_AMPLITUDE_PITCH * 0.9) {
         this.wanderVelPitch = -this.wanderVelPitch * 0.6;
      }
   }

   /** Шум Орнштейна-Уленбека: маленькая амплитуда, плавный, без скачков. */
   private void updateNoise() {
      this.noiseYaw += -0.50 * this.noiseYaw + 0.085 * this.random.nextGaussian();
      this.noisePitch += -0.50 * this.noisePitch + 0.055 * this.random.nextGaussian();
      this.noiseYaw = MathHelper.clamp(this.noiseYaw, -0.30, 0.30);
      this.noisePitch = MathHelper.clamp(this.noisePitch, -0.18, 0.18);
   }

   /** Редкий мягкий перелёт — только по ходу уже начатого движения. */
   private float tickOvershoot() {
      if (--this.overshootCooldown <= 0 && this.overshootTicks <= 0) {
         this.overshootTicks = 3;
         this.overshootCooldown = 50 + this.random.nextInt(50);
         this.overshootForce = (this.random.nextBoolean() ? 1.0F : -1.0F) * this.randomRange(0.5F, 1.1F);
      }

      if (this.overshootTicks > 0) {
         float value = this.overshootForce * (this.overshootTicks / 4.0F);
         this.overshootTicks--;
         return value;
      }

      return 0.0F;
   }

   /** Короткая пауза: рука замирает на 1–2 тика, раз в 70–130 тиков. */
   private boolean tickPause() {
      if (--this.pauseCooldown <= 0 && this.pauseTicks <= 0) {
         this.pauseTicks = 1 + this.random.nextInt(2);
         this.pauseCooldown = 70 + this.random.nextInt(60);
      }

      if (this.pauseTicks > 0) {
         this.pauseTicks--;
         return true;
      }

      return false;
   }

   // ------------------------------------------------------------------ вывод

   /**
    * Квантование в сетку GCD с накоплением остатка. Остаток не даёт «потерять» движение
    * меньше пикселя (из-за него раньше прицел мелко дрожал), а одиночные однопиксельные
    * развороты гасятся совсем: два подряд противоположных шага по 0.15° — очевидный признак.
    */
   private void publish(float desiredYaw, float desiredPitch) {
      if (mc.player == null) {
         return;
      }

      float gcd = GCDUtil.a();
      float yaw = desiredYaw;
      float pitch = MathHelper.clamp(desiredPitch, -PITCH_LIMIT, PITCH_LIMIT);

      if (gcd > 1.0E-4F) {
         double accYaw = (desiredYaw - mc.player.getYaw()) + this.residualYaw;
         double accPitch = (desiredPitch - mc.player.getPitch()) + this.residualPitch;
         int pixelsYaw = (int)Math.round(accYaw / gcd);
         int pixelsPitch = (int)Math.round(accPitch / gcd);

         // Однопиксельный разворот после разворота в прошлом тике — гасим.
         if (Math.abs(pixelsYaw) <= 1 && this.lastPixelSignYaw != 0 && Integer.signum(pixelsYaw) * this.lastPixelSignYaw < 0) {
            pixelsYaw = 0;
         }

         if (Math.abs(pixelsPitch) <= 1 && this.lastPixelSignPitch != 0 && Integer.signum(pixelsPitch) * this.lastPixelSignPitch < 0) {
            pixelsPitch = 0;
         }

         yaw = mc.player.getYaw() + pixelsYaw * gcd;
         pitch = MathHelper.clamp(mc.player.getPitch() + pixelsPitch * gcd, -PITCH_LIMIT, PITCH_LIMIT);

         double appliedYaw = pixelsYaw * gcd;
         double appliedPitch = pixelsPitch * gcd;
         this.residualYaw = MathHelper.clamp(accYaw - appliedYaw, -gcd, gcd);
         this.residualPitch = MathHelper.clamp(accPitch - appliedPitch, -gcd, gcd);

         if (pixelsYaw != 0) {
            this.lastPixelSignYaw = Integer.signum(pixelsYaw);
         }

         if (pixelsPitch != 0) {
            this.lastPixelSignPitch = Integer.signum(pixelsPitch);
         }
      }

      this.push(yaw, pitch);
   }

   /**
    * Отправка в общий пайплайн. Скорость ставим ровно на собственную дельту: сглаживание
    * в хранилище съело бы пиксели, которые уже посчитаны по GCD.
    */
   private void push(float yaw, float pitch) {
      if (mc.player == null) {
         return;
      }

      float deltaYaw = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
      float deltaPitch = pitch - mc.player.getPitch();

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

      this.updateBody(yaw);
   }

   /** Голова и корпус доворачиваются за прицелом, как у живой модели. */
   private void updateBody(float yaw) {
      if (mc.player == null) {
         return;
      }

      mc.player.setHeadYaw(yaw);
      float bodyDelta = MathHelper.wrapDegrees(yaw - mc.player.bodyYaw);
      mc.player.bodyYaw += MathHelper.clamp(bodyDelta, -18.0F, 18.0F);
   }

   // ------------------------------------------------------------------ состояние

   private void trackAttack(long now) {
      if (mc.player == null) {
         return;
      }

      double cooldown = mc.player.getAttackCooldownProgress(0.0F);
      if (cooldown < this.lastCooldown - 1.0E-4) {
         this.lastAttackMillis = now;
         // После удара точка прицела «дёргается» чуть сильнее — так делает и живая рука.
         this.wanderVelYaw += this.randomRange(-0.05F, 0.05F);
         this.wanderVelPitch += this.randomRange(-0.04F, 0.04F);
      }

      this.lastCooldown = cooldown;
   }

   private float randomRange(float minimum, float maximum) {
      return minimum + this.random.nextFloat() * (maximum - minimum);
   }
}
