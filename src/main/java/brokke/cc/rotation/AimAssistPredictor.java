package brokke.cc.rotation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistPredictor {
   private final List<Vec3d> positions = new ArrayList<>();
   private final List<Long> timestamps = new ArrayList<>();
   private int trackedEntityId = -1;
   private Vec3d predictedPosition = Vec3d.ZERO;
   private Vec3d lastVelocity = Vec3d.ZERO;
   private Vec3d acceleration = Vec3d.ZERO;

   public void a(LivingEntity var1, float var2, int var3) {
      if (var1 == null) {
         this.c();
      } else {
         int var4 = var1.getId();
         if (var4 != this.trackedEntityId) {
            this.trackedEntityId = var4;
            this.c();
         }

         double var5 = var1.lastRenderX + (var1.getX() - var1.lastRenderX) * var2;
         double var7 = var1.lastRenderY + (var1.getY() - var1.lastRenderY) * var2;
         double var9 = var1.lastRenderZ + (var1.getZ() - var1.lastRenderZ) * var2;
         Vec3d var11 = new Vec3d(var5, var7, var9);
         this.positions.add(var11);
         this.timestamps.add(System.currentTimeMillis());

         while (this.positions.size() > var3) {
            this.positions.remove(0);
            this.timestamps.remove(0);
         }

         this.d();
      }
   }

   private void d() {
      if (this.positions.size() < 2) {
         this.lastVelocity = Vec3d.ZERO;
         this.acceleration = Vec3d.ZERO;
      } else {
         ArrayList var1 = new ArrayList();

         for (int var2 = 1; var2 < this.positions.size(); var2++) {
            var1.add(this.positions.get(var2).subtract(this.positions.get(var2 - 1)));
         }

         this.lastVelocity = (Vec3d)var1.get(var1.size() - 1);
         if (var1.size() >= 2) {
            ArrayList var9 = new ArrayList();

            for (int var3 = 1; var3 < var1.size(); var3++) {
               var9.add(((Vec3d)var1.get(var3)).subtract((Vec3d)var1.get(var3 - 1)));
            }

            Vec3d var10 = Vec3d.ZERO;
            double var4 = 0.0;

            for (int var6 = 0; var6 < var9.size(); var6++) {
               double var7 = Math.pow(0.85, var9.size() - 1 - var6);
               var10 = var10.add(((Vec3d)var9.get(var6)).multiply(var7));
               var4 += var7;
            }

            this.acceleration = var4 > 0.0 ? var10.multiply(1.0 / var4) : Vec3d.ZERO;
         }
      }
   }

   public Vec3d getLastVelocity() {
      return this.lastVelocity;
   }

   public Vec3d getAcceleration() {
      return this.acceleration;
   }

   public Vec3d a(LivingEntity var1, int var2, float var3, boolean var4, boolean var5, int var6) {
      if (var1 != null && var2 > 0) {
         double var7 = var1.lastRenderX + (var1.getX() - var1.lastRenderX) * var3;
         double var9 = var1.lastRenderY + (var1.getY() - var1.lastRenderY) * var3;
         double var11 = var1.lastRenderZ + (var1.getZ() - var1.lastRenderZ) * var3;
         Vec3d var13 = new Vec3d(var7, var9, var11);
         Vec3d var14 = Vec3d.ZERO;
         if (var4 && this.positions.size() >= 3) {
            double var15 = var2 * 0.05;
            var14 = var14.add(this.lastVelocity.multiply(var2).add(this.acceleration.multiply(0.5 * var15 * var15)));
         }

         if (var5 && this.positions.size() >= 5) {
            Vec3d var17 = this.a(var2, var6);
            if (var17 != null) {
               var14 = var14.multiply(0.6).add(var17.multiply(0.4));
            }
         }

         Vec3d var18 = this.a(var1, var2);
         if (var18 != null) {
            var14 = var14.multiply(0.7).add(var18.multiply(0.3));
         }

         this.predictedPosition = var13.add(var14);
         return this.predictedPosition;
      } else {
         return var1 != null ? new Vec3d(var1.getX(), var1.getY(), var1.getZ()) : Vec3d.ZERO;
      }
   }

   private Vec3d a(int var1, int var2) {
      int var3 = Math.min(this.positions.size(), var2);
      if (var3 < 5) {
         return null;
      }

      if (this.a(var3)) {
         return this.b(var1, var3);
      }

      if (this.c(var3)) {
         return this.lastVelocity.multiply(var1);
      }

      Vec3d var4 = Vec3d.ZERO;
      double var5 = 0.0;

      for (int var7 = 1; var7 < var3; var7++) {
         Vec3d var8 = this.positions.get(var7).subtract(this.positions.get(var7 - 1));
         double var9 = Math.pow(0.9, var3 - var7);
         var4 = var4.add(var8.multiply(var9));
         var5 += var9;
      }

      return var5 > 0.0 ? var4.multiply(var1 / var5) : Vec3d.ZERO;
   }

   private boolean a(int var1) {
      if (var1 < 5) {
         return false;
      }

      ArrayList<Double> var2 = new ArrayList<>();

      for (int var3 = 2; var3 < var1; var3++) {
         Vec3d var4 = this.positions.get(var3 - 1).subtract(this.positions.get(var3 - 2));
         Vec3d var5 = this.positions.get(var3).subtract(this.positions.get(var3 - 1));
         double var6 = Math.atan2(var5.z, var5.x) - Math.atan2(var4.z, var4.x);
         var2.add(Math.abs(a(var6)));
      }

      double var8 = var2.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
      double var9 = var2.stream().mapToDouble(var2x -> Math.pow(var2x - var8, 2.0)).average().orElse(1.0);
      return var9 < 0.1 && var8 > 0.05;
   }

   private Vec3d b(int var1, int var2) {
      Vec3d var3 = this.b(var2);
      Vec3d var4 = this.positions.get(var2 - 1);
      Vec3d var5 = this.positions.get(var2 - 2);
      double var6 = Math.atan2(var4.z - var3.z, var4.x - var3.x);
      double var8 = Math.atan2(var5.z - var3.z, var5.x - var3.x);
      double var10 = a(var6 - var8);
      double var12 = var4.subtract(var3).horizontalLength();
      double var14 = var6 + var10 * var1;
      double var16 = var3.x + var12 * Math.cos(var14);
      double var18 = var3.z + var12 * Math.sin(var14);
      double var20 = var4.y + this.lastVelocity.y * var1;
      return new Vec3d(var16 - var4.x, var20 - var4.y, var18 - var4.z);
   }

   private Vec3d b(int var1) {
      if (var1 < 3) {
         return this.positions.get(var1 - 1);
      }

      Vec3d var2 = this.positions.get(var1 - 3);
      Vec3d var3 = this.positions.get(var1 - 2);
      Vec3d var4 = this.positions.get(var1 - 1);
      double var5 = 2.0 * (var2.x * (var3.z - var4.z) + var3.x * (var4.z - var2.z) + var4.x * (var2.z - var3.z));
      if (Math.abs(var5) < 0.001) {
         return var3;
      }

      double var7 = (
            (var2.x * var2.x + var2.z * var2.z) * (var3.z - var4.z)
               + (var3.x * var3.x + var3.z * var3.z) * (var4.z - var2.z)
               + (var4.x * var4.x + var4.z * var4.z) * (var2.z - var3.z)
         )
         / var5;
      double var9 = (
            (var2.x * var2.x + var2.z * var2.z) * (var4.x - var3.x)
               + (var3.x * var3.x + var3.z * var3.z) * (var2.x - var4.x)
               + (var4.x * var4.x + var4.z * var4.z) * (var3.x - var2.x)
         )
         / var5;
      return new Vec3d(var7, var3.y, var9);
   }

   private boolean c(int var1) {
      if (var1 < 3) {
         return false;
      }

      Vec3d var2 = this.positions.get(var1 - 1).subtract(this.positions.get(var1 - 2));
      Vec3d var3 = this.positions.get(var1 - 2).subtract(this.positions.get(var1 - 3));
      double var4 = var2.length() * var3.length() + 0.001;
      double var6 = var2.dotProduct(var3);
      double var8 = Math.acos(MathHelper.clamp(var6 / var4, -1.0, 1.0));
      return var8 < 0.15;
   }

   private Vec3d a(LivingEntity var1, int var2) {
      if (var1 instanceof PlayerEntity var3) {
         double var4 = var3.getVelocity().x;
         double var6 = var3.getVelocity().y;
         double var8 = var3.getVelocity().z;
         boolean var10 = var3.isOnGround();
         Vec3d var11 = Vec3d.ZERO;

         for (int var12 = 0; var12 < var2; var12++) {
            var6 -= 0.08;
            if (var10) {
               var4 *= 0.546;
               var8 *= 0.546;
            } else {
               var4 *= 0.98;
               var8 *= 0.98;
            }

            var11 = var11.add(var4, var6, var8);
            if (var11.y < var1.getY() - var1.getHeight()) {
               var10 = true;
               var6 = 0.0;
            }
         }

         return var11;
      } else {
         return null;
      }
   }

   public static void a(double[] var0, double var1, double var3, float var5, LivingEntity var6, boolean var7, float var8, int var9) {
      double var10 = b(var6);
      double var12 = var6.getVelocity().x;
      double var14 = var6.getVelocity().y;
      double var16 = var6.getVelocity().z;
      boolean var18 = var6.isOnGround();
      double var19 = var1 - var6.getX();
      double var21 = var3 - var6.getZ();
      double var23 = Math.sqrt(var19 * var19 + var21 * var21);
      if (var23 > 0.001) {
         var19 /= var23;
         var21 /= var23;
      } else {
         double var25 = Math.toRadians(var5);
         var19 = -Math.sin(var25);
         var21 = Math.cos(var25);
      }

      double var42 = 0.4 * (1.0 - var10);
      double var36 = var12 / 2.0 - var19 * var42;
      double var37 = var18 ? Math.min(0.4, var14 / 2.0 + var42) : var14;
      double var39 = var16 / 2.0 - var21 * var42;
      float var27 = var8 + (var7 ? 1.0F : 0.0F);
      if (var27 > 0.0F) {
         double var28 = var27 * 0.5 * (1.0 - var10);
         double var30 = Math.toRadians(var5);
         double var32 = Math.sin(var30);
         double var34 = -Math.cos(var30);
         var36 = var36 / 2.0 - var32 * var28;
         var37 = var18 ? Math.min(0.4, var37 / 2.0 + var28) : var37;
         var39 = var39 / 2.0 - var34 * var28;
      }

      double var43 = 0.0;
      double var44 = 0.0;
      double var45 = 0.0;

      for (int var46 = 0; var46 < var9; var46++) {
         var43 += var36;
         var44 += var14 = var37 - 0.08;
         var45 += var39;
         var36 *= 0.91;
         var37 = var14 * 0.98;
         var39 *= 0.91;
         if (var44 < 0.0) {
            var44 = 0.0;
            var37 = 0.0;
         }
      }

      var0[0] = var43;
      var0[1] = var44;
      var0[2] = var45;
   }

   public static float a(LivingEntity var0) {
      try {
         return (float)var0.getAttributeValue(EntityAttributes.ATTACK_KNOCKBACK);
      } catch (Exception var2) {
         return 0.0F;
      }
   }

   public static double b(LivingEntity var0) {
      try {
         return Math.min(var0.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE), 1.0);
      } catch (Exception var2) {
         return 0.0;
      }
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

   public void c() {
      this.positions.clear();
      this.timestamps.clear();
      this.trackedEntityId = -1;
      this.predictedPosition = Vec3d.ZERO;
      this.lastVelocity = Vec3d.ZERO;
      this.acceleration = Vec3d.ZERO;
   }
}
