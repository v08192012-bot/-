package brokke.cc.rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistMultipoint {
   private static final Random RANDOM = new Random();
   private final List<AimAssistMultipoint.d> points = new ArrayList<>();
   private int selectedIndex = 0;
   private long lastSelectionTime = 0L;
   private float pointInterval = 0.12F;
   private AimAssistMultipoint.c movementType = AimAssistMultipoint.c.STATIONARY;
   private AimAssistMultipoint.a distanceCategory = AimAssistMultipoint.a.MID;
   private final Vec3d[] positionHistory = new Vec3d[15];
   private int historyIndex = 0;
   private int historyCount = 0;
   private int pointCount = 8;
   private float pointSpread = 0.6F;
   private boolean adaptive = true;
   private int currentPointCount = 8;
   private float currentSpread = 0.6F;
   private float currentInterval = 0.12F;

   public void a(int var1, float var2, boolean var3) {
      this.pointCount = var1;
      this.pointSpread = var2;
      this.adaptive = var3;
   }

   public void a(float var1) {
      this.pointInterval = MathHelper.clamp(var1, 0.05F, 0.5F);
   }

   public void a(Vec3d var1, LivingEntity var2, Vec3d var3, float var4) {
      if (var2 != null && var1 != null) {
         AimAssistMultipoint.b var5 = this.a(var2);
         this.a(var2.getEntityPos());
         this.movementType = this.c();
         this.distanceCategory = this.b(var4);
         this.a(var5);
         this.points.clear();
         this.selectedIndex = 0;
         this.lastSelectionTime = System.currentTimeMillis();
         this.a(var1, var2, var4, var5);
         this.points.sort((var0, var1x) -> Float.compare(var1x.weight, var0.weight));
      } else {
         this.points.clear();
         this.currentPointCount = 0;
      }
   }

   private AimAssistMultipoint.b a(LivingEntity var1) {
      if (var1.isTouchingWater()) {
         return AimAssistMultipoint.b.WATER;
      } else if (var1.isInLava()) {
         return AimAssistMultipoint.b.LAVA;
      } else if (var1.isOnGround()) {
         return AimAssistMultipoint.b.GROUND;
      } else {
         return var1 instanceof PlayerEntity var2 && var2.isClimbing() ? AimAssistMultipoint.b.LADDER : AimAssistMultipoint.b.AIR;
      }
   }

   private void a(Vec3d var1) {
      this.positionHistory[this.historyIndex] = var1;
      this.historyIndex = (this.historyIndex + 1) % this.positionHistory.length;
      if (this.historyCount < this.positionHistory.length) {
         this.historyCount++;
      }
   }

   private AimAssistMultipoint.c c() {
      if (this.historyCount < 5) {
         return AimAssistMultipoint.c.STATIONARY;
      }

      ArrayList<Double> var1 = new ArrayList<>();
      ArrayList<Double> var2 = new ArrayList<>();

      for (int var3 = 1; var3 < Math.min(10, this.historyCount); var3++) {
         int var4 = (this.historyIndex - var3 + this.positionHistory.length) % this.positionHistory.length;
         int var5 = (this.historyIndex - var3 - 1 + this.positionHistory.length) % this.positionHistory.length;
         if (this.positionHistory[var4] != null && this.positionHistory[var5] != null) {
            var1.add(this.positionHistory[var4].distanceTo(this.positionHistory[var5]));
            if (var3 >= 2) {
               int var6 = (this.historyIndex - var3 - 2 + this.positionHistory.length) % this.positionHistory.length;
               if (this.positionHistory[var6] != null) {
                  Vec3d var7 = this.positionHistory[var5].subtract(this.positionHistory[var6]);
                  Vec3d var8 = this.positionHistory[var4].subtract(this.positionHistory[var5]);
                  double var9 = Math.atan2(var8.z, var8.x) - Math.atan2(var7.z, var7.x);
                  var2.add(Math.abs(a(var9)));
               }
            }
         }
      }

      if (var1.isEmpty()) {
         return AimAssistMultipoint.c.STATIONARY;
      }

      double var11 = var1.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
      if (var11 < 0.01) {
         return AimAssistMultipoint.c.STATIONARY;
      }

      if (var2.size() >= 3) {
         double var12 = var2.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
         double var14 = var2.stream().mapToDouble(var2x -> Math.pow(var2x - var12, 2.0)).average().orElse(1.0);
         if (var14 < 0.15 && var12 > 0.05 && var12 < 0.5) {
            return AimAssistMultipoint.c.CURVING;
         }

         if (var12 < 0.1) {
            return AimAssistMultipoint.c.LINEAR;
         }
      }

      double var13 = var1.stream().mapToDouble(var2x -> Math.pow(var2x - var11, 2.0)).average().orElse(1.0);
      return var13 < 0.05 ? AimAssistMultipoint.c.ACCELERATING : AimAssistMultipoint.c.IRREGULAR;
   }

   private AimAssistMultipoint.a b(float var1) {
      if (var1 < 3.0F) {
         return AimAssistMultipoint.a.CLOSE;
      } else {
         return var1 < 6.0F ? AimAssistMultipoint.a.MID : AimAssistMultipoint.a.FAR;
      }
   }

   private void a(AimAssistMultipoint.b var1) {
      if (!this.adaptive) {
         this.currentPointCount = this.pointCount;
         this.currentSpread = this.pointSpread;
         this.currentInterval = this.pointInterval;
      } else {
         int var2 = this.pointCount;
         float var3 = this.pointSpread;
         float var4 = this.pointInterval;
         switch (var1) {
            case AIR:
               var2 = (int)(var2 * 1.2);
               var3 *= 1.1F;
               var4 *= 0.9F;
               break;
            case WATER:
               var2 = (int)(var2 * 0.7);
               var3 *= 0.6F;
               var4 *= 1.3F;
               break;
            case LAVA:
               var2 = (int)(var2 * 0.6);
               var3 *= 0.5F;
               var4 *= 1.5F;
               break;
            case LADDER:
               var2 = (int)(var2 * 0.8);
               var3 *= 0.7F;
               var4 *= 1.2F;
         }

         switch (this.movementType) {
            case LINEAR:
               var2 = (int)(var2 * 1.1);
               var3 *= 0.9F;
               var4 *= 0.9F;
               break;
            case CURVING:
               var2 = (int)(var2 * 1.4);
               var3 *= 1.2F;
               var4 *= 0.8F;
               break;
            case IRREGULAR:
               var2 = (int)(var2 * 1.3);
               var3 *= 1.3F;
               var4 *= 0.7F;
               break;
            case ACCELERATING:
               var2 = (int)(var2 * 0.8);
               var3 *= 0.8F;
               var4 *= 1.1F;
         }

         switch (this.distanceCategory) {
            case CLOSE:
               var2 = (int)(var2 * 1.3);
               var3 *= 0.7F;
               var4 *= 0.8F;
               break;
            case FAR:
               var2 = (int)(var2 * 0.7);
               var3 *= 1.4F;
               var4 *= 1.2F;
         }

         this.currentPointCount = MathHelper.clamp(var2, 3, 50);
         this.currentSpread = MathHelper.clamp(var3, 0.2F, 2.0F);
         this.currentInterval = MathHelper.clamp(var4, 0.05F, 0.5F);
      }
   }

   private void a(Vec3d var1, LivingEntity var2, float var3, AimAssistMultipoint.b var4) {
      double var5 = var2.getWidth() / 2.0;
      double var7 = var2.getHeight();
      float var9 = 0.8F;
      float var10 = 0.9F;
      float var11 = 0.5F;
      float var12 = 0.35F;
      float var13 = 0.45F;
      float var14 = 0.2F;
      if (var4 == AimAssistMultipoint.b.AIR) {
         var9 = 0.6F;
         var10 = 0.8F;
         var11 = 0.7F;
         var12 = 0.25F;
         var13 = 0.45F;
         var14 = 0.3F;
      } else if (var4 == AimAssistMultipoint.b.WATER || var4 == AimAssistMultipoint.b.LAVA) {
         var9 = 0.9F;
         var10 = 0.7F;
         var11 = 0.3F;
         var12 = 0.5F;
         var13 = 0.35F;
         var14 = 0.15F;
      }

      int var15 = (int)(this.currentPointCount * var12);
      int var16 = (int)(this.currentPointCount * var13);
      int var17 = this.currentPointCount - var15 - var16;

      for (int var18 = 0; var18 < var15; var18++) {
         float var19 = (float)(RANDOM.nextDouble() * Math.PI * 2.0);
         float var20 = (float)(var5 * this.currentSpread * (0.2 + RANDOM.nextDouble() * 0.5));
         double var21 = var1.x + Math.cos(var19) * var20;
         double var23 = var1.y + var7 * (0.7 + RANDOM.nextDouble() * 0.25);
         double var25 = var1.z + Math.sin(var19) * var20;
         this.points.add(new AimAssistMultipoint.d(var21, var23, var25, var9, 0.8F));
      }

      for (int var29 = 0; var29 < var16; var29++) {
         float var31 = (float)(RANDOM.nextDouble() * Math.PI * 2.0);
         float var33 = (float)(var5 * this.currentSpread * (0.3 + RANDOM.nextDouble() * 0.5));
         double var35 = var1.x + Math.cos(var31) * var33;
         double var37 = var1.y + var7 * (0.4 + RANDOM.nextDouble() * 0.3);
         double var39 = var1.z + Math.sin(var31) * var33;
         this.points.add(new AimAssistMultipoint.d(var35, var37, var39, var10, 0.65F));
      }

      for (int var30 = 0; var30 < var17; var30++) {
         float var32 = (float)(RANDOM.nextDouble() * Math.PI * 2.0);
         float var34 = (float)(var5 * this.currentSpread * RANDOM.nextDouble());
         double var36 = var1.x + Math.cos(var32) * var34;
         double var38 = var1.y + var7 * (0.12 + RANDOM.nextDouble() * 0.28);
         double var40 = var1.z + Math.sin(var32) * var34;
         this.points.add(new AimAssistMultipoint.d(var36, var38, var40, var11, 0.35F));
      }
   }

   public double[] a() {
      AimAssistMultipoint.d var1 = this.d();
      return var1 == null ? null : new double[]{var1.x, var1.y, var1.z};
   }

   private AimAssistMultipoint.d d() {
      if (this.points.isEmpty()) {
         return null;
      }

      long var1 = System.currentTimeMillis();
      if ((float)(var1 - this.lastSelectionTime) < this.currentInterval * 1000.0F) {
         return this.selectedIndex < this.points.size() ? this.points.get(this.selectedIndex) : this.points.get(0);
      }

      this.lastSelectionTime = var1;
      if (this.points.size() > 1) {
         this.selectedIndex = this.e();
      } else {
         this.selectedIndex = 0;
      }

      AimAssistMultipoint.d var3 = this.points.get(this.selectedIndex);
      var3.selectionCount++;
      return var3;
   }

   private int e() {
      float var1 = 0.0F;

      for (AimAssistMultipoint.d var3 : this.points) {
         var1 += var3.weight * var3.confidence;
      }

      if (var1 <= 0.0F) {
         return RANDOM.nextInt(this.points.size());
      }

      float var6 = RANDOM.nextFloat() * var1;
      float var7 = 0.0F;

      for (int var4 = 0; var4 < this.points.size(); var4++) {
         AimAssistMultipoint.d var5 = this.points.get(var4);
         var7 += var5.weight * var5.confidence;
         if (var6 <= var7) {
            return var4;
         }
      }

      return this.points.size() - 1;
   }

   private static double a(double var0) {
      double var2 = var0 % (Math.PI * 2);
      if (var2 < -Math.PI) {
         var2 += Math.PI * 2;
      }

      if (var2 > Math.PI) {
         var2 -= Math.PI * 2;
      }

      return var2;
   }

   public void b() {
      this.points.clear();
      this.selectedIndex = 0;
      this.lastSelectionTime = 0L;
      this.historyIndex = 0;
      this.historyCount = 0;
      Arrays.fill(this.positionHistory, null);
      this.movementType = AimAssistMultipoint.c.STATIONARY;
      this.distanceCategory = AimAssistMultipoint.a.MID;
      this.currentPointCount = this.pointCount;
      this.currentSpread = this.pointSpread;
      this.currentInterval = this.pointInterval;
   }

   public enum a {
      CLOSE,
      MID,
      FAR;
   }

   public enum b {
      GROUND,
      AIR,
      WATER,
      LAVA,
      LADDER;
   }

   public enum c {
      STATIONARY,
      LINEAR,
      CURVING,
      IRREGULAR,
      ACCELERATING;
   }

   public static class d {
      public final double x;
      public final double y;
      public final double z;
      public final float weight;
      public final float confidence;
      public int selectionCount = 0;

      public d(double var1, double var3, double var5, float var7, float var8) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         this.weight = var7;
         this.confidence = var8;
      }
   }
}
