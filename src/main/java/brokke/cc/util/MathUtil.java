package brokke.cc.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MathUtil {
   public static float clamp(float var0, float var1, float var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   public static int clamp(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   public static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   public static float interpolate(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   public static boolean isInsideRectangle(float var0, float var1, float var2, float var3, float var4, float var5) {
      return var0 >= var2 && var0 <= var2 + var4 && var1 >= var3 && var1 <= var3 + var5;
   }

   public static boolean isInsideRectangle(double var0, double var2, float var4, float var5, float var6, float var7) {
      return var0 >= var4 && var0 <= var4 + var6 && var2 >= var5 && var2 <= var5 + var7;
   }

   public static double roundDecimals(double var0, double var2) {
      double var4 = Math.pow(10.0, var2);
      return Math.round(var0 * var4) / var4;
   }

   public static Vec3d rotationToDirection(float var0, float var1) {
      float var2 = var0 * (float) (Math.PI / 180.0);
      float var3 = -var1 * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var3);
      float var5 = MathHelper.sin(var3);
      float var6 = MathHelper.cos(var2);
      float var7 = MathHelper.sin(var2);
      return new Vec3d(var5 * var6, -var7, var4 * var6);
   }

   public static float lerpAngle(float var0, float var1, float var2) {
      float var3 = (var1 - var0 + 180.0F) % 360.0F - 180.0F;
      if (var3 < -180.0F) {
         var3 += 360.0F;
      }

      return var0 + var3 * var2;
   }

   public static float a(float var0) {
      return var0 / 6.0F;
   }

   public static float divide(float var0, float var1) {
      return var0 / var1;
   }

   public static float b(float var0) {
      return var0 / 6500.0F;
   }

   private MathUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
