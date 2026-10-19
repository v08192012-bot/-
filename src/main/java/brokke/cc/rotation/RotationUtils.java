package brokke.cc.rotation;

import brokke.cc.util.GCDUtil2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;

public class RotationUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static HitResult a(double var0, float var2, float var3) {
      if (mc.player != null && mc.world != null) {
         Vec3d var4 = mc.player.getCameraPosVec(1.0F);
         Vec3d var5 = a(var3, var2);
         Vec3d var6 = var4.add(var5.x * var0, var5.y * var0, var5.z * var0);
         return mc.world.raycast(new RaycastContext(var4, var6, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      } else {
         return null;
      }
   }

   public static Vec3d a(Entity var0) {
      if (mc.player == null) {
         return new Vec3d(var0.getX(), var0.getY() + var0.getHeight() * 0.5, var0.getZ());
      }

      Vec3d var1 = mc.player.getEyePos();
      Box var2 = var0.getBoundingBox();
      double var3 = 0.1;
      Vec3d var5 = null;
      double var6 = Double.MAX_VALUE;

      for (double var8 = var2.minX; var8 <= var2.maxX; var8 += var3) {
         for (double var10 = var2.minY; var10 <= var2.maxY; var10 += var3) {
            for (double var12 = var2.minZ; var12 <= var2.maxZ; var12 += var3) {
               Vec3d var14 = new Vec3d(var8, var10, var12);
               double var15 = var1.distanceTo(var14);
               if (var15 < var6) {
                  var6 = var15;
                  var5 = var14;
               }
            }
         }
      }

      return var5 != null ? var5 : new Vec3d(var0.getX(), var0.getY() + var0.getHeight() * 0.5, var0.getZ());
   }

   public static Rotation2 a(Vec3d var0) {
      return new Rotation2(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var0.z, var0.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(var0.y, Math.hypot(var0.x, var0.z))))
      );
   }

   @NotNull
   public static Vec3d a(float var0, float var1) {
      return new Vec3d(
         MathHelper.sin(-var1 * (float) (Math.PI / 180.0)) * MathHelper.cos(var0 * (float) (Math.PI / 180.0)),
         -MathHelper.sin(var0 * (float) (Math.PI / 180.0)),
         MathHelper.cos(-var1 * (float) (Math.PI / 180.0)) * MathHelper.cos(var0 * (float) (Math.PI / 180.0))
      );
   }

   public static Vec2f b(Entity var0) {
      return a(var0.getX(), var0.getY(), var0.getZ());
   }

   public static Vec2f b(Vec3d var0) {
      return a(var0.x, var0.y, var0.z);
   }

   public static Vec2f a(double var0, double var2, double var4) {
      if (mc.player == null) {
         return new Vec2f(0.0F, 0.0F);
      }

      double var6 = var0 - mc.player.getX();
      double var8 = var2 - mc.player.getEyeY();
      double var10 = var4 - mc.player.getZ();
      double var12 = MathHelper.sqrt((float)(var6 * var6 + var10 * var10));
      float var14 = (float)(MathHelper.atan2(var10, var6) * (180.0 / Math.PI) - 90.0);
      float var15 = (float)(-MathHelper.atan2(var8, var12) * (180.0 / Math.PI));
      return new Vec2f(var14, var15);
   }

   public static float[] a(Direction var0) {
      if (mc.player == null) {
         return new float[]{0.0F, 0.0F};
      }

      return switch (var0) {
         case DOWN -> new float[]{mc.player.getYaw(), 90.0F};
         case UP -> new float[]{mc.player.getYaw(), -90.0F};
         case NORTH -> new float[]{180.0F, mc.player.getPitch()};
         case SOUTH -> new float[]{0.0F, mc.player.getPitch()};
         case WEST -> new float[]{90.0F, mc.player.getPitch()};
         case EAST -> new float[]{-90.0F, mc.player.getPitch()};
      };
   }

   public static float[] a(float[] var0) {
      float var1 = GCDUtil2.a();
      var0[0] -= var0[0] % var1;
      var0[1] -= var0[1] % var1;
      return new float[]{var0[0], var0[1]};
   }

   public static float a(float var0) {
      return b(var0) * GCDUtil2.a();
   }

   public static float b(float var0) {
      return Math.round(var0 / GCDUtil2.a());
   }
}
