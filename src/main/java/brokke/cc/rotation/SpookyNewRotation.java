package brokke.cc.rotation;

import brokke.cc.rotation.spooky.AimRotation;
import brokke.cc.rotation.spooky.SpookyMath;
import brokke.cc.rotation.spooky.SpookyModel;
import brokke.cc.rotation.spooky.SpookyRotation;
import java.io.InputStream;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Режим SpookyNew: ротация из нейросети (модель {@code neurotest-motor-v1}, формат ZENMOTOR).
 *
 * В отличие от {@link SpookyTimeRotation} (синусы и профили, написанные руками) здесь решение
 * каждый тик принимает обученная политика: 47 признаков → MLP+GRU → смесь из 5 гауссиан →
 * команда в asinh-масштабе → огибающая «человеческой» скорости → квантование в целые пиксели
 * мыши с переносом остатка. На выходе получается ровно то, что делала живая рука на записях:
 * доводка рывками по 0.15°, перелёты, паузы, разные скорости каждый тик.
 *
 * Сам движок лежит в {@code brokke.cc.rotation.spooky} и не зависит от Minecraft — его
 * можно прогнать автономно ({@link brokke.cc.rotation.spooky.SpookySelfCheck}) и сверить с
 * эталонными тиками модели.
 */
public class SpookyNewRotation extends RotationsSystem {
   private static final int ROTATION_PRIORITY = 3;
   private static final String MODEL_RESOURCE = "/assets/brokke/rotation/spooky_motor.bin.gz";
   private static final Object LOAD_LOCK = new Object();

   private static volatile SpookyModel sharedModel;
   private static boolean loadFailed;
   private static boolean warned;

   private final Random random = new Random();
   private SpookyRotation engine;
   private LivingEntity trackedTarget;
   private double lastCooldown = 1.0;
   private int ticksSinceAttack = 50;
   private double commandScale = 1.0;
   private boolean visibleAim = true;

   /** Прогрев модели в фоне: 7 МБ весов не должны давать фриз на первом ударе. */
   public static void preload() {
      Thread thread = new Thread(() -> model(), "shelka-spookynew-preload");
      thread.setDaemon(true);
      thread.start();
   }

   private static SpookyModel model() {
      SpookyModel local = sharedModel;
      if (local != null) {
         return local;
      }

      synchronized (LOAD_LOCK) {
         if (sharedModel != null) {
            return sharedModel;
         }

         if (loadFailed) {
            return null;
         }

         try (InputStream in = SpookyNewRotation.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (in == null) {
               throw new IllegalStateException("нет ресурса " + MODEL_RESOURCE);
            }

            sharedModel = SpookyModel.load(in);
            return sharedModel;
         } catch (Throwable error) {
            loadFailed = true;
            System.err.println("[Shelka] SpookyNew: модель не загрузилась — " + error);
            return null;
         }
      }
   }

   public void setVisibleAim(boolean value) {
      this.visibleAim = value;
   }

   /** «Скорость SpookyNew», %: множитель команды до огибающей (100% = поведение модели). */
   public void setSpeed(float percent) {
      this.commandScale = MathHelper.clamp(percent / 100.0F, 0.5F, 2.0F);
   }

   /** Полный сброс: выключение модуля, смена цели, смена режима. */
   public void a() {
      this.trackedTarget = null;
      this.ticksSinceAttack = 50;
      this.lastCooldown = 1.0;
      if (this.engine != null) {
         this.engine.reset();
      }
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

      SpookyModel model = model();
      if (model == null) {
         this.warnOnce();
         return;
      }

      double sensitivity = 0.5;
      try {
         sensitivity = mc.options.getMouseSensitivity().getValue();
      } catch (Throwable ignored) {
      }

      double pixelStep = SpookyMath.pixelStep(MathHelper.clamp((float)sensitivity, 0.0F, 1.0F));
      if (this.engine == null) {
         this.engine = new SpookyRotation(model, pixelStep, this.random);
      }

      this.engine.setPixelStep(pixelStep);
      this.engine.setCommandScale(this.commandScale);

      if (target != this.trackedTarget) {
         this.trackedTarget = target;
         this.engine.reset();
         this.ticksSinceAttack = 50;
         this.lastCooldown = 1.0;
      }

      // Кулдаун атаки просел — значит удар был: счётчик «тиков с удара» начинается заново.
      double cooldown = mc.player.getAttackCooldownProgress(0.0F);
      if (cooldown < this.lastCooldown - 1.0E-4) {
         this.ticksSinceAttack = 0;
      } else if (this.ticksSinceAttack < 50) {
         this.ticksSinceAttack++;
      }

      this.lastCooldown = cooldown;

      Box box = this.c(target);
      SpookyRotation.Target aimTarget = new SpookyRotation.Target(
         target.getId(),
         !target.isDead(),
         box.minX,
         box.minY,
         box.minZ,
         box.maxX,
         box.maxY,
         box.maxZ
      );

      Vec3d eyes = mc.player.getEyePos();
      SpookyRotation.PlayerState state = new SpookyRotation.PlayerState(
         eyes.x,
         eyes.y,
         eyes.z,
         mc.options.forwardKey.isPressed(),
         mc.options.backKey.isPressed(),
         mc.options.leftKey.isPressed(),
         mc.options.rightKey.isPressed(),
         mc.options.jumpKey.isPressed(),
         mc.player.isOnGround(),
         mc.player.isSprinting(),
         mc.player.isSneaking(),
         cooldown,
         this.ticksSinceAttack
      );

      AimRotation current = new AimRotation(mc.player.getYaw(), mc.player.getPitch());
      AimRotation next = this.engine.tick(current, aimTarget, state, mc.player.age);
      if (next == null) {
         return;
      }

      float deltaYaw = MathHelper.wrapDegrees(next.yaw - current.yaw);
      float deltaPitch = MathHelper.clamp(next.pitch - current.pitch, -90.0F, 90.0F);

      // Модель уже отдала готовую дельту на этот тик, поэтому скорость в хранилище ротаций
      // ставим ровно на неё: сглаживание съело бы «человеческие» пиксели. Тик «паузы»
      // (дельта 0) тоже публикуем — иначе хранилище решит, что цель потеряна, и начнёт
      // возвращать прицел к камере, ломая состояние GRU.
      RotationStorage.a(
         new Rotation2(next.yaw, next.pitch),
         Math.abs(deltaYaw) + 0.05F,
         Math.abs(deltaPitch) + 0.05F,
         30.0F,
         20.0F,
         2,
         ROTATION_PRIORITY,
         this.visibleAim
      );
   }

   private void warnOnce() {
      if (!warned && mc.player != null) {
         warned = true;
         mc.player.sendMessage(Text.literal("§cSpookyNew: модель не загрузилась, режим выключен"), false);
      }
   }

   public boolean isReady() {
      return this.engine != null && sharedModel != null;
   }
}
