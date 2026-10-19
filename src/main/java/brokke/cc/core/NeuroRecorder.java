package brokke.cc.core;

import brokke.cc.manager.CommandManager;
import brokke.cc.module.combat.Aura;
import brokke.cc.rotation.AimAnchor;
import brokke.cc.rotation.SpaceTimeDriver;
import brokke.cc.rotation.SpaceTimeModel;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Рекордер датасета для обучения SpaceTime (см. tools/neuro/train_spacetime.py).
 *
 * Каждый тик, пока идёт запись и рядом есть цель, в CSV пишутся:
 *   x0..x16 — ровно те 17 фичей, которые инференс-драйвер SpaceTime подаёт в GRU
 *            (сжатые asinh/3, с прошлыми дельтами и freeze/free-таймерами — состояние
 *            ведётся так же, как в SpaceTimeDriver);
 *   dyaw, dpitch — реальные дельты поворота игрока за тик (метки);
 *   dist, yaw, pitch — метаданные для анализа.
 *
 * Формат CSV заголовок: x0,...,x16,dyaw,dpitch,dist,yaw,pitch
 *
 * Запуск: команда .neuro record [минуты] [nobot]
 */
public final class NeuroRecorder {
   public static final NeuroRecorder INSTANCE = new NeuroRecorder();
   private static final String MODEL_RESOURCE = "assets/brokke/rotation/spacetime.bin";
   private static final String CSV_HEADER = "x0,x1,x2,x3,x4,x5,x6,x7,x8,x9,x10,x11,x12,x13,x14,x15,x16,dyaw,dpitch,dist,yaw,pitch\n";
   private static final int COLUMN_COUNT = 22;
   private static final int FLUSH_INTERVAL = 256;
   private static final float SPAN = SpaceTimeDriver.SPAN_INIT;
   private static final float FREEZE_EPS = 0.05F;
   private static final double MAX_TARGET_DIST = 6.0;
   private static float cachedFreezeCut = -1.0F;

   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final AimAnchor anchor = new AimAnchor();
   private BufferedWriter writer;
   private Path currentFile;
   private boolean recording;
   private long deadlineNanos;
   private long lastTickNanos;
   private int rows;
   private int flushCounter;
   private boolean warnedAura;

   // состояние фич (как в SpaceTimeDriver)
   private LivingEntity currentTarget;
   private boolean hasPrev;
   private float prevYaw;
   private float prevPitch;
   private float prevGoalYaw;
   private float prevGoalPitch;
   private float prevErrYaw;
   private float prevErrPitch;
   private float prevStepYaw;
   private float prevStepPitch;
   private float prevPrevStepYaw;
   private float prevPrevStepPitch;
   private int freeze;
   private float sinceAttack = SpaceTimeModel.ATTACK_WINDOW;

   private NeuroRecorder() {
   }

   public static Path dir() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      Path var1 = var0 != null && var0.runDirectory != null ? var0.runDirectory.toPath() : Path.of("run");
      return var1.resolve("brokke").resolve("neuro").resolve("data");
   }

   /** @return null — ок, иначе сообщение об ошибке */
   public String start(int minutes) {
      if (this.recording) {
         return "Запись уже идёт: " + this.rows + " строк";
      }

      if (this.mc.player == null || this.mc.world == null) {
         return "Нужно быть в мире";
      }

      if (Aura.INSTANCE != null && Aura.INSTANCE.l()) {
         return "Выключи ауру — иначе в датасет попадут её повороты";
      }

      try {
         Path dir = dir();
         Files.createDirectories(dir);
         Path file = dir.resolve("dataset_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
         this.writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
         this.writer.write(CSV_HEADER);
         this.currentFile = file;
      } catch (Exception var4) {
         this.writer = null;
         this.currentFile = null;
         return "Не удалось открыть файл в " + dir();
      }

      this.recording = true;
      this.warnedAura = false;
      this.deadlineNanos = System.nanoTime() + minutes * 60_000_000_000L;
      this.lastTickNanos = 0L;
      this.rows = 0;
      this.flushCounter = 0;
      this.sinceAttack = SpaceTimeModel.ATTACK_WINDOW;
      this.resetState();
      return null;
   }

   public String stop(String reason) {
      if (!this.recording) {
         return "Запись не идёт.";
      }

      this.recording = false;
      this.close();
      return "§7Запись остановлена (" + reason + "): §f" + this.rows + " строк §7→ §f" + this.currentFile;
   }

   public String status() {
      if (!this.recording) {
         return "§bNeuro-рекордер: §7остановлен. Файлы в §f" + dir();
      }

      long leftSec = Math.max(0L, (this.deadlineNanos - System.nanoTime()) / 1_000_000_000L);
      return "§bNeuro-рекордер: §aидёт запись §7(строк: §f"
         + this.rows
         + "§7, осталось: §f"
         + leftSec / 60L
         + "м "
         + leftSec % 60L
         + "с§7) → §f"
         + this.currentFile;
   }

   public boolean isRecording() {
      return this.recording;
   }

   @EventHandler
   public void onAttack(AttackEntityEvent var1) {
      if (this.recording && var1.getPlayer() == this.mc.player) {
         this.sinceAttack = 0.0F;
      }
   }

   @EventHandler
   public void onTick(UpdateEvent var1) {
      if (!this.recording) {
         return;
      }

      if (this.mc.player == null || this.mc.world == null) {
         CommandManager.d(this.stop("нет мира"));
         return;
      }

      if (System.nanoTime() >= this.deadlineNanos) {
         CommandManager.d(this.stop("время вышло"));
         CommandManager.d("§7Датасет готов к обучению: §fpython tools/neuro/train_spacetime.py --data \""
            + dir().resolve("*.csv")
            + "\"");
         return;
      }

      if (Aura.INSTANCE != null && Aura.INSTANCE.l()) {
         if (!this.warnedAura) {
            this.warnedAura = true;
            CommandManager.d("§e[Neuro] §7Запись на паузе: включена аура");
         }

         this.resetState();
         return;
      }

      if (this.warnedAura) {
         this.warnedAura = false;
         CommandManager.d("§a[Neuro] §7Запись продолжена");
      }

      LivingEntity target = this.pickTarget();
      if (target == null) {
         this.resetState();
         return;
      }

      long now = System.nanoTime();
      if (this.lastTickNanos != 0L && now - this.lastTickNanos > 500000000L) {
         this.resetState();
      }

      double dt = this.lastTickNanos == 0L ? 0.05 : Math.min((now - this.lastTickNanos) / 1.0E9, 0.05);
      this.lastTickNanos = now;
      if (target != this.currentTarget || !this.hasPrev) {
         this.resetState();
         this.currentTarget = target;
         this.anchor.acquire(now);
      }

      this.anchor.update(dt, now);
      Box box = target.getBoundingBox();
      Vec3d point = this.anchor.point(box, AuraChecks.a(this.mc, target));
      Vec3d delta = point.subtract(this.mc.player.getEyePos());
      float goalYaw = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
      float goalPitch = MathHelper.clamp((float) (-Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()))), -90.0F, 90.0F);
      float dist = (float) this.mc.player.getEyePos().distanceTo(point);
      float yaw = this.mc.player.getYaw();
      float pitch = this.mc.player.getPitch();

      if (!this.hasPrev) {
         this.prevYaw = yaw;
         this.prevPitch = pitch;
         this.prevGoalYaw = goalYaw;
         this.prevGoalPitch = goalPitch;
         this.prevErrYaw = SpaceTimeDriver.wrap(goalYaw - yaw);
         this.prevErrPitch = goalPitch - pitch;
         this.prevStepYaw = 0.0F;
         this.prevStepPitch = 0.0F;
         this.prevPrevStepYaw = 0.0F;
         this.prevPrevStepPitch = 0.0F;
         this.freeze = 0;
         this.hasPrev = true;
         return;
      }

      float stepYaw = SpaceTimeDriver.wrap(yaw - this.prevYaw);
      float stepPitch = pitch - this.prevPitch;
      float errYaw = SpaceTimeDriver.wrap(this.prevGoalYaw - yaw);
      float errPitch = this.prevGoalPitch - pitch;
      this.sinceAttack = Math.min(this.sinceAttack + 1.0F, SpaceTimeModel.ATTACK_WINDOW);

      float freezeCut = this.modelFreezeCut();
      float[] x = new float[17];
      x[0] = SpaceTimeModel.squash(errYaw);
      x[1] = SpaceTimeModel.squash(errPitch);
      x[2] = SpaceTimeModel.squash(SpaceTimeDriver.wrap(errYaw - this.prevErrYaw));
      x[3] = SpaceTimeModel.squash(errPitch - this.prevErrPitch);
      x[4] = SpaceTimeModel.squash(SpaceTimeDriver.wrap(goalYaw - this.prevGoalYaw));
      x[5] = SpaceTimeModel.squash(goalPitch - this.prevGoalPitch);
      x[6] = SpaceTimeModel.squash(this.prevStepYaw);
      x[7] = SpaceTimeModel.squash(this.prevStepPitch);
      x[8] = SpaceTimeModel.squash(this.prevPrevStepYaw);
      x[9] = SpaceTimeModel.squash(this.prevPrevStepPitch);
      x[10] = SpaceTimeModel.squash(errYaw / SPAN);
      x[11] = SpaceTimeModel.squash(errPitch / SPAN);
      x[12] = (float) (Math.log(Math.max(dist, 0.05F) + 0.5F) / 2.0);
      x[13] = (float) (Math.log(SPAN) / 3.0);
      x[14] = Math.abs(errYaw) <= SPAN && Math.abs(errPitch) <= SPAN ? 1.0F : 0.0F;
      x[15] = Math.min(this.sinceAttack, SpaceTimeModel.ATTACK_WINDOW) / SpaceTimeModel.ATTACK_WINDOW;
      x[16] = Math.min(this.freeze, (int) freezeCut) / freezeCut;

      this.write(x, stepYaw, stepPitch, dist, yaw, pitch);

      this.prevErrYaw = errYaw;
      this.prevErrPitch = errPitch;
      this.freeze = Math.abs(stepYaw) < FREEZE_EPS && Math.abs(stepPitch) < FREEZE_EPS ? this.freeze + 1 : 0;
      this.prevPrevStepYaw = this.prevStepYaw;
      this.prevPrevStepPitch = this.prevStepPitch;
      this.prevStepYaw = stepYaw;
      this.prevStepPitch = stepPitch;
      this.prevGoalYaw = goalYaw;
      this.prevGoalPitch = goalPitch;
      this.prevYaw = yaw;
      this.prevPitch = pitch;
   }

   /**
    * Порядок фич обязан совпадать со SpaceTimeDriver.tick:
    * x6/x7 — прошлый шаг (step предыдущего тика), x8/x9 — позапрошлый.
    */
   private void write(float[] x, float stepYaw, float stepPitch, float dist, float yaw, float pitch) {
      if (this.writer == null) {
         return;
      }

      StringBuilder sb = new StringBuilder(COLUMN_COUNT * 12);

      for (int i = 0; i < x.length; i++) {
         if (i > 0) {
            sb.append(',');
         }

         sb.append(x[i]);
      }

      sb.append(',').append(stepYaw).append(',').append(stepPitch).append(',').append(dist).append(',').append(yaw).append(',').append(pitch).append('\n');

      try {
         this.writer.write(sb.toString());
         this.rows++;
         if (++this.flushCounter >= FLUSH_INTERVAL) {
            this.flushCounter = 0;
            this.writer.flush();
         }
      } catch (Exception var9) {
         this.stop("ошибка записи: " + var9.getMessage());
      }
   }

   private LivingEntity pickTarget() {
      if (this.mc.world == null || this.mc.player == null) {
         return null;
      }

      double maxSq = MAX_TARGET_DIST * MAX_TARGET_DIST;
      LivingEntity best = null;

      for (Entity var5 : this.mc.world.getEntities()) {
         if (var5 instanceof LivingEntity var6 && var6 != this.mc.player && var6.isAlive() && !(var6 instanceof ArmorStandEntity)) {
            double var7 = this.mc.player.squaredDistanceTo(var6);
            if (var7 < maxSq) {
               maxSq = var7;
               best = var6;
            }
         }
      }

      return best;
   }

   private float modelFreezeCut() {
      if (cachedFreezeCut < 0.0F) {
         SpaceTimeModel var1 = SpaceTimeModel.load(MODEL_RESOURCE);
         cachedFreezeCut = var1 != null ? (float) Math.max(1, var1.freezeCut) : 42.0F;
      }

      return cachedFreezeCut;
   }

   private void resetState() {
      this.currentTarget = null;
      this.hasPrev = false;
      this.freeze = 0;
      this.prevStepYaw = 0.0F;
      this.prevStepPitch = 0.0F;
      this.prevPrevStepYaw = 0.0F;
      this.prevPrevStepPitch = 0.0F;
   }

   private void close() {
      if (this.writer != null) {
         try {
            this.writer.flush();
            this.writer.close();
         } catch (Exception var2) {
         }

         this.writer = null;
      }

      this.resetState();
      this.lastTickNanos = 0L;
   }
}
