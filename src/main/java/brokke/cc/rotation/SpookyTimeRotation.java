package brokke.cc.rotation;

import brokke.cc.core.AuraChecks;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Режим SpookyTime («SmartYaw»): аура не «ведёт» прицел непрерывно, а работает как рука
 * живого игрока — довелась до цели, замерла на ней и стоит, пока цель не уедет из-под
 * прицела, после чего делает новую переводку в ДРУГУЮ точку хитбокса и снова замирает.
 *
 * Почему так, а не плавное слежение: непрерывное идеальное ведение — это ровная,
 * детерминированная траектория, её и ловят античиты. Здесь движение дискретное
 * (переводка → покой → переводка), точки не повторяются, а скорости каждый тик разные.
 *
 * Логика тика:
 *   1) два рейкаста: «вижу ли цель вообще» и «лежит ли прицел точно в хитбоксе»;
 *   2) прицел в хитбоксе  → стоим (только микро-дрожь, если включена);
 *   3) прицел ушёл       → переводка в новую точку с рандомной скоростью;
 *   4) yaw и pitch едут с разными скоростями и всегда с перекосом друг в друга,
 *      поэтому чистых поворотов «ровно по yaw» или «ровно по pitch» не бывает;
 *   5) в конце путь кладётся на сетку GCD и уходит в RotationStorage (общий бюджет).
 */
public class SpookyTimeRotation extends RotationsSystem {
   private static final int ROTATION_PRIORITY = 3;
   /** Сколько последних точек помним, чтобы новая никогда не повторила старую. */
   private static final int HISTORY = 48;
   private static final float DEFAULT_SPEED = 70.0F;
   private static final float TWO_PI = (float)(Math.PI * 2.0);

   /**
    * Профили наводки, от спокойного к агрессивному: {min, max, softness}.
    * softness — «мягкость» выхода на точку: чем больше, тем раньше начинает тормозить.
    */
   private static final float[][] PROFILES = {
      {0.6F, 2.6F, 9.0F},
      {0.9F, 4.5F, 7.5F},
      {1.4F, 7.5F, 5.5F},
      {2.0F, 13.0F, 4.2F},
      {3.0F, 24.0F, 3.0F}
   };

   private final Random random = new Random();

   /** История точек прицела в нормализованных координатах хитбокса. */
   private final double[] historyX = new double[HISTORY];
   private final double[] historyY = new double[HISTORY];
   private final double[] historyZ = new double[HISTORY];
   private int historyCount;
   private int historyIndex;

   private LivingEntity trackedTarget;
   private int tick;
   private boolean hasPoint;
   private boolean locked;
   private float speedScale = 1.0F;
   private boolean micro = true;
   private boolean visibleAim = true;

   /** Текущая точка прицела в нормализованных координатах хитбокса. */
   private double pointX = 0.5;
   private double pointY = 0.75;
   private double pointZ = 0.5;

   /** «Настроение» руки: фазы и частоты синусов, перекос осей, прогноз. */
   private float phaseA;
   private float phaseB;
   private float phaseC;
   private float freqA = 0.3F;
   private float freqB = 0.7F;
   private float freqC = 1.7F;
   private float pitchRatio = 0.6F;
   private float bleedYaw = 0.1F;
   private float bleedPitch = 0.16F;
   private float leadTicks = 2.5F;

   /** Микро-дрожь в покое: амплитуда и сколько тиков ей ещё жить. */
   private int invisibleTicks;

   private float microYaw;
   private float microPitch;
   private int microTicks;
   private int nextMicroTick;

   public void setVisibleAim(boolean var1) {
      this.visibleAim = var1;
   }

   /** «Скорость SpookyTime»: общий масштаб скоростей переводок. */
   public void setSpeed(float var1) {
      this.speedScale = MathHelper.clamp(var1 / DEFAULT_SPEED, 0.55F, 1.55F);
   }

   /** Микро-дрожь в покое: выключить — прицел будет стоять абсолютно неподвижно. */
   public void setMicro(boolean var1) {
      this.micro = var1;
   }

   /** Полный сброс: выключение модуля, смена цели, смена режима. */
   public void a() {
      this.trackedTarget = null;
      this.tick = 0;
      this.hasPoint = false;
      this.locked = false;
      this.historyCount = 0;
      this.historyIndex = 0;
      this.microTicks = 0;
      this.nextMicroTick = 0;
      this.invisibleTicks = 0;
      this.microYaw = 0.0F;
      this.microPitch = 0.0F;
      this.rollMood();
   }

   /**
    * Удар нанесён: перекатываем «настроение руки», чтобы следующая переводка была
    * не похожа на предыдущую, и разрешаем микро-дрожь чуть раньше.
    */
   public void b() {
      this.rollMood();
      this.nextMicroTick = Math.min(this.nextMicroTick, this.tick + 2 + this.random.nextInt(4));
   }

   public boolean isLocked() {
      return this.locked;
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player == null || mc.world == null || var1 == null) {
         return;
      }

      RotationStorage var2 = RotationStorage.instance;
      if (var2 != null && var2.getCurrentPriority() > ROTATION_PRIORITY) {
         this.a();
         return;
      }

      this.tick++;
      if (this.trackedTarget != var1) {
         this.trackedTarget = var1;
         this.hasPoint = false;
         this.locked = false;
         this.historyCount = 0;
         this.historyIndex = 0;
         this.microTicks = 0;
         this.nextMicroTick = this.tick + 4 + this.random.nextInt(8);
         this.rollMood();
      }

      Box var3 = this.c(var1);
      Vec3d var4 = mc.player.getEyePos();
      float var5 = mc.player.getYaw();
      float var6 = mc.player.getPitch();
      double var7 = Math.min(6.0, var4.distanceTo(var3.getCenter()) + 1.6);

      // Первый рейкаст: прицел уже лежит в хитбоксе → стоим.
      if (this.onTarget(var3, var4, var5, var6, var7)) {
         this.locked = true;
         this.hasPoint = false;
         this.hold(var3, var4, var5, var6, var7);
         return;
      }

      this.locked = false;

      // Второй рейкаст: цель вообще видна. Не видна — доворачиваемся резче (как в гайде),
      // видна — идём спокойно, чтобы не «телепортировать» прицел.
      boolean var8 = AuraChecks.a(mc, var1);

      if (!this.hasPoint) {
         this.pickPoint(var1, var3, var4);
         this.rollMood();
         this.hasPoint = true;
      }

      Vec3d var9 = this.aimPoint(var1, var3, var4);
      Vec3d var10 = var9.subtract(var4);
      float var11 = (float)(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0));
      float var12 = (float)(-Math.toDegrees(Math.atan2(var10.y, var10.horizontalLength())));
      float var13 = MathHelper.wrapDegrees(var11 - var5);
      float var14 = var12 - var6;
      float var15 = (float)Math.hypot(var13, var14);

      // Перекос осей: yaw всегда заезжает на pitch и наоборот — ровных линий не бывает.
      var11 = MathHelper.wrapDegrees(var11 + MathHelper.clamp(var14 * this.bleedYaw, -4.0F, 4.0F));
      var12 = MathHelper.clamp(var12 + MathHelper.clamp(var13 * this.bleedPitch, -6.0F, 6.0F), -89.0F, 89.0F);

      Vec3d var16 = var1.getVelocity();
      double var17 = Math.hypot(var16.x, var16.z);
      float var18 = (float)Math.min(1.0, var17 / 0.25);
      double var19 = var4.distanceTo(var9);

      int var21 = MathHelper.clamp((int)(var15 / 8.0F + this.random.nextFloat() * 1.4F - 0.2F), 0, PROFILES.length - 1);
      float var22 = PROFILES[var21][1] * this.speedScale;
      float var23 = PROFILES[var21][2] * (0.7F + this.random.nextFloat() * 0.6F);
      float var24 = var22 * (0.55F + 0.75F * Math.min(1.0F, var15 / 6.0F));
      float var25 = var24 * (var15 / (var15 + var23));

      // Скорость каждый тик разная: три синуса с рандомными фазами/частотами
      // плюс вклад скорости самой цели. Одинаковых чисел тут не бывает.
      var25 *= 1.0F
         + 0.18F * MathHelper.sin(this.tick * this.freqA + this.phaseA)
         + 0.13F * MathHelper.sin(this.tick * this.freqB + this.phaseB)
         + 0.07F * MathHelper.sin(this.tick * this.freqC + this.phaseC);
      var25 *= 0.85F + 0.35F * var18;

      if (!var8) {
         // Цель не видна: разгон за ~400 мс до полных 28°/тик, дальше держим пол 15°,
         // чтобы прицел не «залипал» в стороне (как в гайде по SpookyTime).
         this.invisibleTicks = Math.min(this.invisibleTicks + 1, 60);
         float var27 = 0.45F + 0.55F * Math.min(1.0F, this.invisibleTicks / 8.0F);
         var25 = Math.min(var25 * 1.8F * var27, 28.0F);
         if (this.invisibleTicks > 8 && var15 > 5.0F) {
            var25 = Math.max(var25, 15.0F * this.speedScale);
         }
      } else {
         this.invisibleTicks = 0;
      }

      // Цель вплотную и двигается: не разгоняемся, иначе проскакиваем хитбокс.
      if (var19 < 0.9 && var17 > 0.08) {
         var25 = Math.min(var25, 12.0F + 6.0F * this.random.nextFloat());
      }

      var25 = MathHelper.clamp(var25, 0.3F, 26.0F);
      float var26 = Math.max(0.25F, Math.min(var25 * this.pitchRatio, 15.0F));
      if (Math.abs(var14) > 8.0F) {
         var26 = Math.max(var26, 8.0F);
      }

      this.publish(var11, var12, var25, var26);
   }

   /** Прицел в хитбоксе: почти не двигаемся, только микро-дрожь. */
   private void hold(Box var1, Vec3d var2, float var3, float var4, double var5) {
      float var7 = var3;
      float var8 = var4;
      if (this.micro) {
         if (this.microTicks <= 0 && this.tick >= this.nextMicroTick) {
            // Дрожь почти всегда по двум осям с разным весом — как у живой руки.
            float var9 = this.d(0.05F, 0.3F) * this.speedScale * (this.random.nextBoolean() ? 1.0F : -1.0F);
            this.microYaw = var9 * this.d(0.6F, 1.0F);
            this.microPitch = var9 * this.d(0.35F, 0.8F) * (this.random.nextBoolean() ? 1.0F : -1.0F);
            this.microTicks = 1 + this.random.nextInt(2);
            this.nextMicroTick = this.tick + 5 + this.random.nextInt(13);
         }

         if (this.microTicks > 0) {
            this.microTicks--;
            float var10 = MathHelper.wrapDegrees(var3 + this.microYaw);
            float var11 = MathHelper.clamp(var4 + this.microPitch, -89.0F, 89.0F);
            if (this.onTarget(var1, var2, var10, var11, var5)) {
               var7 = var10;
               var8 = var11;
            } else {
               // Дрожь увела бы прицел из хитбокса — ведём её в другую сторону.
               this.microYaw = -this.microYaw;
               this.microPitch = -this.microPitch;
            }
         }
      }

      float var12 = this.micro ? 0.35F : 0.0F;
      this.publish(var7, var8, var12, var12 * this.pitchRatio);
   }

   /** Отправка углов в общий пайплайн: бюджет тика, GCD и silent-камера — там. */
   private void publish(float var1, float var2, float var3, float var4) {
      if (!Float.isFinite(var1) || !Float.isFinite(var2) || !Float.isFinite(var3) || !Float.isFinite(var4)) {
         this.a();
         return;
      }

      RotationStorage.a(
         new Rotation2(var1, var2),
         Math.max(0.0F, var3),
         Math.max(0.0F, var4),
         30.0F,
         20.0F,
         2,
         ROTATION_PRIORITY,
         this.visibleAim
      );
   }

   /** Прицел лежит в хитбоксе цели (расширен на пару сантиметров от края). */
   private boolean onTarget(Box var1, Vec3d var2, float var3, float var4, double var5) {
      Box var7 = var1.expand(0.04);
      if (var7.contains(var2)) {
         return true;
      }

      Vec3d var8 = Vec3d.fromPolar(var4, var3).normalize();
      Vec3d var9 = var2.add(var8.multiply(var5));
      return var7.raycast(var2, var9).isPresent();
   }

   /**
    * Новая точка прицела. Берём 24 кандидата и выбираем самый далёкий от истории —
    * так точки не повторяются и не липнут рядом друг с другом.
    */
   private void pickPoint(LivingEntity var1, Box var2, Vec3d var3) {
      double var4 = var3.y - var1.getY();
      double var5 = var1.getHeight();
      double var6;
      double var7;
      if (var4 > var5 + 0.15) {
         // Цель ниже нас — ложимся в верхнюю часть тела.
         var6 = 0.62;
         var7 = 0.92;
      } else if (var4 < -0.1) {
         // Цель выше — берём нижнюю часть.
         var6 = 0.12;
         var7 = 0.45;
      } else {
         // Ровный бой — грудь/плечи (height * 0.75 по гайду).
         var6 = 0.55;
         var7 = 0.88;
      }

      double var8 = 0.5;
      double var9 = 0.75;
      double var10 = 0.5;
      double var11 = -1.0;

      for (int var12 = 0; var12 < 24; var12++) {
         double var13 = this.d(0.18, 0.82);
         double var14 = this.d(var6, var7);
         double var15 = this.d(0.18, 0.82);
         double var16 = this.freshness(var13, var14, var15) + this.random.nextDouble() * 0.04;
         if (var16 > var11) {
            var11 = var16;
            var8 = var13;
            var9 = var14;
            var10 = var15;
         }
      }

      this.pointX = var8;
      this.pointY = var9;
      this.pointZ = var10;
      this.remember(var8, var9, var10);
   }

   /** Минимальное расстояние до уже использованных точек (0 — история пуста). */
   private double freshness(double var1, double var3, double var5) {
      if (this.historyCount == 0) {
         return 1.0;
      }

      double var7 = Double.MAX_VALUE;
      for (int var9 = 0; var9 < this.historyCount; var9++) {
         double var10 = var1 - this.historyX[var9];
         double var12 = (var3 - this.historyY[var9]) * 0.4;
         double var14 = var5 - this.historyZ[var9];
         double var16 = var10 * var10 + var12 * var12 + var14 * var14;
         if (var16 < var7) {
            var7 = var16;
         }
      }

      return Math.sqrt(var7);
   }

   private void remember(double var1, double var3, double var5) {
      this.historyX[this.historyIndex] = var1;
      this.historyY[this.historyIndex] = var3;
      this.historyZ[this.historyIndex] = var5;
      this.historyIndex = (this.historyIndex + 1) % HISTORY;
      if (this.historyCount < HISTORY) {
         this.historyCount++;
      }
   }

   /** Точка прицела в мире: тело + половина прогноза движения («на середину»). */
   private Vec3d aimPoint(LivingEntity var1, Box var2, Vec3d var3) {
      Vec3d var4 = new Vec3d(
         var2.minX + var2.getLengthX() * this.pointX,
         var2.minY + var2.getLengthY() * this.pointY,
         var2.minZ + var2.getLengthZ() * this.pointZ
      );
      Vec3d var5 = var1.getVelocity();
      Vec3d var6 = new Vec3d(var5.x, 0.0, var5.z).multiply(this.leadTicks * 0.5);
      double var7 = var6.length();
      if (var7 > 0.55) {
         var6 = var6.multiply(0.55 / var7);
      }

      return var4.add(var6);
   }

   /** Перекатывает «настроение руки»: так ни одна переводка не похожа на предыдущую. */
   private void rollMood() {
      this.phaseA = this.random.nextFloat() * TWO_PI;
      this.phaseB = this.random.nextFloat() * TWO_PI;
      this.phaseC = this.random.nextFloat() * TWO_PI;
      this.freqA = this.d(0.19F, 0.47F);
      this.freqB = this.d(0.55F, 1.05F);
      this.freqC = this.d(1.4F, 2.6F);
      this.pitchRatio = this.d(0.42F, 0.78F);
      this.bleedYaw = this.d(0.05F, 0.15F) * (this.random.nextBoolean() ? 1.0F : -1.0F);
      this.bleedPitch = this.d(0.09F, 0.21F) * (this.random.nextBoolean() ? 1.0F : -1.0F);
      this.leadTicks = this.d(1.5F, 4.5F);
   }

   private float d(float var1, float var2) {
      return var1 + this.random.nextFloat() * (var2 - var1);
   }

   private double d(double var1, double var3) {
      return var1 + this.random.nextDouble() * (var3 - var1);
   }
}
