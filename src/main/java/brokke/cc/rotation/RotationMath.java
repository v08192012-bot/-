package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationMath {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Vec3d a(LivingEntity var0) {
      if (mc.player != null && var0 != null) {
         Vec3d var1 = mc.player.getEyePos();
         return new Vec3d(
            MathHelper.clamp(var1.x, var0.getBoundingBox().minX, var0.getBoundingBox().maxX),
            MathHelper.clamp(var1.y, var0.getBoundingBox().minY, var0.getBoundingBox().maxY),
            MathHelper.clamp(var1.z, var0.getBoundingBox().minZ, var0.getBoundingBox().maxZ)
         );
      } else {
         return Vec3d.ZERO;
      }
   }

   public static Rotation2 a(Vec3d var0) {
      if (mc.player == null) {
         return Rotation2.ZERO;
      }

      double var1 = var0.getX();
      double var3 = var0.getY();
      double var5 = var0.getZ();
      double var7 = var1 - mc.player.getX();
      double var9 = var3 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
      double var11 = var5 - mc.player.getZ();
      double var13 = Math.sqrt(var7 * var7 + var11 * var11);
      float var15 = (float)Math.toDegrees(Math.atan2(var11, var7)) - 90.0F;
      float var16 = (float)(-Math.toDegrees(Math.atan2(var9, var13)));
      return new Rotation2(var15, var16);
   }

   public static double a() {
      double var0 = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      double var2 = var0 * var0 * var0;
      return var2 * 8.0 * 0.15;
   }

   public static Rotation2 a(Rotation2 var0) {
      double var1 = a();
      float var3 = (float)(var0.getYaw() - var0.getYaw() % var1);
      float var4 = (float)(var0.getPitch() - var0.getPitch() % var1);
      return new Rotation2(var3, var4);
   }

   public static float a(float var0, float var1) {
      float var2 = var1 - var0;

      while (var2 > 180.0F) {
         var2 -= 360.0F;
      }

      while (var2 < -180.0F) {
         var2 += 360.0F;
      }

      return var2;
   }

   public static float b(float var0, float var1) {
      float var2 = var0 % 360.0F;
      if (var2 < 0.0F) {
         var2 += 360.0F;
      }

      float var3 = var1 % 360.0F;
      if (var3 < 0.0F) {
         var3 += 360.0F;
      }

      int var4 = (int)(var0 / 360.0F);
      if (var0 < 0.0F && var0 % 360.0F != 0.0F) {
         var4--;
      }

      float var5 = var3 + var4 * 360.0F;
      float var6 = var5 - var0;
      if (var6 > 180.0F) {
         var5 -= 360.0F;
      } else if (var6 < -180.0F) {
         var5 += 360.0F;
      }

      return var5;
   }

   private RotationMath() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
