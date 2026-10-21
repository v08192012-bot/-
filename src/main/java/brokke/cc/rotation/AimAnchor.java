package brokke.cc.rotation;

import java.util.Random;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Блуждающая точка прицела внутри хитбокса цели (anchor-wander).
 * Используется и режимом SkyTime, и рекордером датасета NeuroRecorder,
 * чтобы goal-фичи на записи и на инференсе считались одинаково.
 */
public final class AimAnchor {
   private static final long REPICK_MIN_NANOS = 400000000L;
   private static final long REPICK_MAX_NANOS = 1200000000L;

   private static final int HISTORY = 24;

   private final Random random = new Random();
   /** История уже отработанных точек: новую цель выбираем подальше от всех прошлых. */
   private final double[] historyX = new double[HISTORY];
   private final double[] historyY = new double[HISTORY];
   private final double[] historyZ = new double[HISTORY];
   private int historyCount;
   private int historyIndex;
   private double anchorX;
   private double anchorY;
   private double anchorZ;
   private double goalX;
   private double goalY;
   private double goalZ;
   private double velX;
   private double velY;
   private double velZ;
   private long nextRepickNanos;

   public AimAnchor() {
      this.reset();
   }

   public void reset() {
      this.anchorX = this.goalX = 0.5;
      this.anchorY = this.goalY = 0.62;
      this.anchorZ = this.goalZ = 0.5;
      this.velX = this.velY = this.velZ = 0.0;
      this.nextRepickNanos = 0L;
      this.historyCount = 0;
      this.historyIndex = 0;
   }

   /** Новая цель: случайная стартовая точка внутри хитбокса + первый repick. */
   public void acquire(long now) {
      this.anchorX = this.d(0.3, 0.7);
      this.anchorY = this.d(0.4, 0.8);
      this.anchorZ = this.d(0.3, 0.7);
      this.velX = this.velY = this.velZ = 0.0;
      this.historyCount = 0;
      this.historyIndex = 0;
      this.repick(now);
   }

   /** Движение точки: периодический repick цели + интегрирование скоростей с отскоками. */
   public void update(double dt, long now) {
      if (now >= this.nextRepickNanos) {
         this.repick(now);
      }

      double s = dt * 60.0;
      this.anchorX = MathHelper.clamp(this.anchorX + this.velX * s, 0.12, 0.88);
      this.anchorY = MathHelper.clamp(this.anchorY + this.velY * s, 0.22, 0.92);
      this.anchorZ = MathHelper.clamp(this.anchorZ + this.velZ * s, 0.12, 0.88);
      if (this.anchorX == 0.12 && this.velX < 0.0 || this.anchorX == 0.88 && this.velX > 0.0) {
         this.velX = -this.velX * this.d(0.6F, 1.0F);
      }

      if (this.anchorY == 0.22 && this.velY < 0.0 || this.anchorY == 0.92 && this.velY > 0.0) {
         this.velY = -this.velY * this.d(0.6F, 1.0F);
      }

      if (this.anchorZ == 0.12 && this.velZ < 0.0 || this.anchorZ == 0.88 && this.velZ > 0.0) {
         this.velZ = -this.velZ * this.d(0.6F, 1.0F);
      }
   }

   /** Точка прицела: блуждающий anchor внутри бокса или голова цели. */
   public Vec3d point(Box box, boolean useAnchor) {
      if (useAnchor) {
         return new Vec3d(
            box.minX + box.getLengthX() * this.anchorX, box.minY + box.getLengthY() * this.anchorY, box.minZ + box.getLengthZ() * this.anchorZ
         );
      }

      return new Vec3d(box.getCenter().x, box.maxY - 0.1, box.getCenter().z);
   }

   private void repick(long now) {
      this.nextRepickNanos = now + this.a(REPICK_MIN_NANOS, REPICK_MAX_NANOS);
      double x = this.anchorX;
      double y = this.anchorY;
      double z = this.anchorZ;

      // 16 кандидатов, берём самый далёкий от истории точек: тогда прицел не возвращается
      // в одно и то же место хитбокса и «рисунок» наводки не повторяется.
      double bestScore = -1.0;
      for (int i = 0; i < 16; i++) {
         double cx = this.d(0.12, 0.88);
         double cy = this.d(0.22, 0.92);
         double cz = this.d(0.12, 0.88);
         double score = this.freshness(cx, cy, cz) * 1.7 + this.random.nextDouble() * 0.05;
         if (score > bestScore) {
            bestScore = score;
            x = cx;
            y = cy;
            z = cz;
         }
      }

      this.remember(x, y, z);
      this.goalX = x;
      this.goalY = y;
      this.goalZ = z;
      double len = Math.max(
         1.0E-8,
         (x - this.anchorX) * (x - this.anchorX) + (y - this.anchorY) * (y - this.anchorY) + (z - this.anchorZ) * (z - this.anchorZ)
      );
      double norm = Math.sqrt(len);
      double speedX = this.d(0.003, 0.03);
      double speedY = this.d(0.001, 0.03);
      this.velX = (x - this.anchorX) / norm * speedX;
      this.velY = (y - this.anchorY) / norm * speedY;
      this.velZ = (z - this.anchorZ) / norm * speedX;
   }

   /** Минимальное расстояние до уже отработанных точек. */
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

   private long a(long min, long max) {
      return min + (long) (this.random.nextDouble() * (double) (max - min));
   }

   private double d(double min, double max) {
      return min + this.random.nextDouble() * (max - min);
   }

   private float d(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }
}
