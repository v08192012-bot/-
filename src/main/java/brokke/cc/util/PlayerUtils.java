package brokke.cc.util;

import brokke.cc.core.IMinecraft;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils implements IMinecraft {
   public static boolean canMove = true;

   public static Vec3d a(float var0, float var1) {
      float var2 = var0 * (float) (Math.PI / 180.0);
      float var3 = -var1 * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var3);
      float var5 = MathHelper.sin(var3);
      float var6 = MathHelper.cos(var2);
      float var7 = MathHelper.sin(var2);
      return new Vec3d(var5 * var6, -var7, var4 * var6);
   }

   public static EntityHitResult a(double var0, float var2, float var3, Predicate<Entity> var4) {
      Entity var5 = mc.getCameraEntity();
      if (var5 != null && mc.world != null) {
         Vec3d var6 = var5.getCameraPosVec(1.0F);
         float var7 = var3 * (float) (Math.PI / 180.0);
         float var8 = -var2 * (float) (Math.PI / 180.0);
         float var9 = (float)Math.cos(var7);
         float var10 = (float)Math.sin(var7);
         float var11 = (float)Math.cos(var8);
         float var12 = (float)Math.sin(var8);
         Vec3d var13 = new Vec3d(var12 * var9, -var10, var11 * var9);
         Vec3d var14 = var6.add(var13.x * var0, var13.y * var0, var13.z * var0);
         Box var15 = var5.getBoundingBox().stretch(var13.multiply(var0)).expand(1.0, 1.0, 1.0);
         return ProjectileUtil.raycast(var5, var6, var14, var15, var4, var0 * var0);
      } else {
         return null;
      }
   }

   public static Entity a(Entity var0, float var1, float var2, double var3) {
      Entity var5 = mc.getCameraEntity();
      if (var5 != null && mc.world != null && var0 != null) {
         Box var6 = var5.getBoundingBox();
         Box var7 = var0.getBoundingBox();
         Vec3d var8 = var5.getEyePos();
         Vec3d var9 = a(var2, var1);
         Vec3d var10 = var8.add(var9.x * var3, var9.y * var3, var9.z * var3);
         if (var6.intersects(var7)) {
            EntityHitResult var11 = a(var3, var1, var2, var1x -> var1x == var0 && !var1x.isSpectator() && var1x.canHit());
            if (var11 != null && var11.getEntity() == var0) {
               return var0;
            }
         }

         EntityHitResult var12 = ProjectileUtil.raycast(
            var5, var8, var10, var7.expand(1.0), var1x -> var1x == var0 && !var1x.isSpectator() && var1x.canHit(), var3 * var3
         );
         return var12 != null && var8.distanceTo(var12.getPos()) <= var3 ? var12.getEntity() : null;
      } else {
         return null;
      }
   }

   public static Entity a(Entity var0, float var1, float var2, double var3, float var5) {
      Entity var6 = mc.getCameraEntity();
      if (var6 != null && mc.world != null && var0 != null) {
         Vec3d var7 = var6.getEyePos();
         Vec3d var8 = Vec3d.fromPolar(var2, var1);
         Vec3d var9 = var7.add(var8.multiply(var3));
         Box var10 = var0.getBoundingBox();
         if (var5 != 1.0F) {
            double var11 = var10.getLengthX() * (var5 - 1.0) * 0.5;
            double var13 = var10.getLengthY() * (var5 - 1.0) * 0.5;
            double var15 = var10.getLengthZ() * (var5 - 1.0) * 0.5;
            var10 = var10.expand(var11, var13, var15);
         }

         EntityHitResult var17 = ProjectileUtil.raycast(
            var6, var7, var9, var10.expand(1.0), var1x -> var1x == var0 && !var1x.isSpectator() && var1x.canHit(), var3 * var3
         );
         return var17 != null ? var17.getEntity() : null;
      } else {
         return null;
      }
   }
}
