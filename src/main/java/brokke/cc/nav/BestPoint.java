package brokke.cc.nav;

import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationUtils;
import brokke.cc.util.MathUtils;
import brokke.cc.util.RayTraceUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;

public final class BestPoint {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static Vec3d rotationPoint = Vec3d.ZERO;
   private static Vec3d rotationMotion = Vec3d.ZERO;

   private BestPoint() {
   }

   public static Vec3d getRotationPoint() {
      return rotationPoint;
   }

   public static Vec3d a(Entity var0) {
      if (mc.player != null && var0 != null) {
         Box var1 = var0.getBoundingBox();
         Vec3d var2 = mc.player.getEyePos();
         return new Vec3d(
            MathHelper.clamp(var2.x, var1.minX, var1.maxX), MathHelper.clamp(var2.y, var1.minY, var1.maxY), MathHelper.clamp(var2.z, var1.minZ, var1.maxZ)
         );
      } else {
         return var0 != null ? var0.getEntityPos() : Vec3d.ZERO;
      }
   }

   public static Vec3d b(Entity var0) {
      Box var1 = var0.getBoundingBox();
      double var2 = var1.maxX - var1.minX;
      double var4 = var1.maxY - var1.minY;
      double var6 = var1.maxZ - var1.minZ;
      double var8 = var1.minX + var2 / 2.0;
      double var10 = var1.minY + var4 * 0.7;
      double var12 = var1.minZ + var6 / 2.0;
      double var14 = System.currentTimeMillis() / 50.0;
      int var16 = var0.getId();
      double var17 = Math.sin(var14 + var16) * (var2 * 0.45);
      double var19 = Math.cos(var14 * 0.8 + var16) * (var4 * 0.1);
      double var21 = Math.cos(var14 * 1.2 + var16) * (var6 * 0.45);
      return new Vec3d(var8 + var17, var10 + var19, var12 + var21);
   }

   public static Vec3d c(Entity var0) {
      Box var1 = var0.getBoundingBox();
      double var2 = var1.maxX - var1.minX;
      double var4 = var1.maxY - var1.minY;
      double var6 = var1.maxZ - var1.minZ;
      double var8 = var1.minX + var2 / 2.0;
      double var10 = var1.minY + var4 * 0.65;
      double var12 = var1.minZ + var6 / 2.0;
      double var14 = System.currentTimeMillis() / 65.0;
      int var16 = var0.getId();
      double var17 = Math.sin(var14 + var16) * (var2 * 0.7);
      double var19 = Math.cos(var14 * 0.8 + var16) * (var4 * 0.4);
      double var21 = Math.cos(var14 * 1.2 + var16) * (var6 * 0.7);
      return new Vec3d(var8 + var17, var10 + var19, var12 + var21);
   }

   public static Vec3d a(Entity var0, Vec3d var1, double var2) {
      if (var1 != null && mc.player != null && mc.world != null) {
         if (b(var0, var1, var2)) {
            return var1;
         }

         Box var4 = var0.getBoundingBox();
         double var5 = 0.12;
         Vec3d var7 = null;
         double var8 = Double.MAX_VALUE;

         for (double var10 = var4.minX; var10 <= var4.maxX; var10 += var5) {
            for (double var12 = var4.minY; var12 <= var4.maxY; var12 += var5) {
               for (double var14 = var4.minZ; var14 <= var4.maxZ; var14 += var5) {
                  Vec3d var16 = new Vec3d(var10, var12, var14);
                  if (b(var0, var16, var2)) {
                     double var17 = var16.squaredDistanceTo(var1);
                     if (var17 < var8) {
                        var8 = var17;
                        var7 = var16;
                     }
                  }
               }
            }
         }

         return var7 != null ? var7 : var1;
      } else {
         return var1;
      }
   }

   private static boolean b(Entity var0, Vec3d var1, double var2) {
      if (mc.player != null && mc.world != null) {
         Vec3d var4 = mc.player.getEyePos();
         double var5 = var4.distanceTo(var1);
         if (var5 > var2) {
            return false;
         }

         Vec3d var7 = var1.subtract(var4).normalize();
         if (!RayTraceUtil.a(var7, var5 + 0.2, var0.getBoundingBox())) {
            return false;
         }

         BlockHitResult var8 = RayTraceUtil.a(var4, var1, ShapeType.COLLIDER, mc.player);
         return var8.getType() == Type.MISS || var4.squaredDistanceTo(var8.getPos()) >= var4.squaredDistanceTo(var1) - 1.0E-4;
      } else {
         return false;
      }
   }

   public static Vec3d a(Entity var0, double var1) {
      if (mc.player == null) {
         return var0.getEntityPos();
      }

      float var3 = 0.005F;
      float var4 = 0.015F;
      float var5 = 0.0015F;
      float var6 = 0.015F;
      double var7 = var0.getBoundingBox().getLengthX();
      double var9 = var0.getBoundingBox().getLengthY();
      double var11 = var0.getBoundingBox().getLengthZ();
      if (rotationMotion.equals(Vec3d.ZERO)) {
         rotationMotion = new Vec3d(MathUtils.randomDouble(-0.02F, 0.02F), MathUtils.randomDouble(-0.02F, 0.02F), MathUtils.randomDouble(-0.02F, 0.02F));
      }

      if (rotationPoint.equals(Vec3d.ZERO)) {
         rotationPoint = new Vec3d(0.0, var9 * 0.5, 0.0);
      }

      rotationPoint = rotationPoint.add(rotationMotion);
      double var13 = (var7 - 0.1) / 2.0;
      double var15 = (var11 - 0.1) / 2.0;
      if (rotationPoint.x >= var13) {
         rotationMotion = new Vec3d(-MathUtils.randomDouble(var3, var4), rotationMotion.getY(), rotationMotion.getZ());
      } else if (rotationPoint.x <= -var13) {
         rotationMotion = new Vec3d(MathUtils.randomDouble(var3, var4), rotationMotion.getY(), rotationMotion.getZ());
      }

      if (rotationPoint.y >= var9 * 0.75) {
         rotationMotion = new Vec3d(rotationMotion.getX(), -MathUtils.randomDouble(var5, var6), rotationMotion.getZ());
      } else if (rotationPoint.y <= var9 * 0.3) {
         rotationMotion = new Vec3d(rotationMotion.getX(), MathUtils.randomDouble(var5, var6), rotationMotion.getZ());
      }

      if (rotationPoint.z >= var15) {
         rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), -MathUtils.randomDouble(var3, var4));
      } else if (rotationPoint.z <= -var15) {
         rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), MathUtils.randomDouble(var3, var4));
      }

      rotationPoint = rotationPoint.add(MathUtils.randomDouble(-0.05F, 0.05F), 0.0, MathUtils.randomDouble(-0.05F, 0.05F));
      if (!RayTraceUtil.a(mc.player.getRotationVector(), var1, var0.getBoundingBox())) {
         float var18 = (float)(var7 / 2.0) * 0.8F;

         for (float var19 = -var18; var19 <= var18; var19 += 0.1F) {
            for (float var20 = -var18; var20 <= var18; var20 += 0.1F) {
               for (float var21 = (float)(var9 * 0.9); var21 >= var9 * 0.3; var21 -= 0.1F) {
                  Vec3d var22 = new Vec3d(var0.getX() + var19, var0.getY() + var21, var0.getZ() + var20);
                  Rotation2 var17 = RotationUtils.a(var22);
                  if (RayTraceUtil.a(var17.d(), var1, var0.getBoundingBox())) {
                     rotationPoint = new Vec3d(var19, var21, var20);
                     return var0.getEntityPos().add(rotationPoint);
                  }
               }
            }
         }
      }

      return var0.getEntityPos().add(rotationPoint);
   }
}
