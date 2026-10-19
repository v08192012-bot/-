package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static float[] a(Entity var0) {
      if (mc.player != null && var0 != null) {
         Vec3d var1 = mc.player.getEyePos();
         Vec3d var2 = var0.getEntityPos().add(0.0, var0.getHeight() / 2.0, 0.0);
         double var3 = var2.x - var1.x;
         double var5 = var2.y - var1.y;
         double var7 = var2.z - var1.z;
         double var9 = Math.sqrt(var3 * var3 + var7 * var7);
         float var11 = (float)(Math.toDegrees(Math.atan2(var7, var3)) - 90.0);
         float var12 = (float)(-Math.toDegrees(Math.atan2(var5, var9)));
         return new float[]{var11, var12};
      } else {
         return new float[]{0.0F, 0.0F};
      }
   }

   public static float[] a(double var0, double var2, double var4) {
      if (mc.player == null) {
         return new float[]{0.0F, 0.0F};
      }

      Vec3d var6 = mc.player.getEyePos();
      double var7 = var0 - var6.x;
      double var9 = var2 - var6.y;
      double var11 = var4 - var6.z;
      double var13 = Math.sqrt(var7 * var7 + var11 * var11);
      float var15 = (float)(Math.toDegrees(Math.atan2(var11, var7)) - 90.0);
      float var16 = (float)(-Math.toDegrees(Math.atan2(var9, var13)));
      return new float[]{var15, var16};
   }

   public static float a(float var0, float var1) {
      return MathHelper.wrapDegrees(var0 - var1);
   }

   public static float b(float var0, float var1) {
      float var2 = MathHelper.wrapDegrees(var1 - var0);
      return var0 + var2;
   }
}
