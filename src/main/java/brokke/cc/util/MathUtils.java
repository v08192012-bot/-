package brokke.cc.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MathUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static FastRandom fastRandomize = new FastRandom();

   public static double a(float var0, double var1, double var3) {
      if (var1 < 0.0) {
         var0 += 180.0F;
      }

      float var5 = 1.0F;
      if (var1 < 0.0) {
         var5 = -0.5F;
      } else if (var1 > 0.0) {
         var5 = 0.5F;
      }

      if (var3 > 0.0) {
         var0 -= 90.0F * var5;
      }

      if (var3 < 0.0) {
         var0 += 90.0F * var5;
      }

      return Math.toRadians(var0);
   }

   public static float randomFloat(double var0, double var2) {
      return var0 > var2 ? (float)(fastRandomize.nextFloat() * (var0 - var2) + var2) : (float)(fastRandomize.nextFloat() * (var2 - var0) + var0);
   }

   public static double a(Entity var0) {
      double var1 = var0.getX() - var0.lastRenderX;
      double var3 = var0.getY() - var0.lastRenderY;
      double var5 = var0.getZ() - var0.lastRenderZ;
      double var7 = Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5);
      return var7 * 20.0;
   }

   public static float a() {
      if (mc.player == null) {
         return 0.0F;
      }

      double var0 = mc.player.getX() - mc.player.lastRenderX;
      double var2 = mc.player.getY() - mc.player.lastRenderY;
      double var4 = mc.player.getZ() - mc.player.lastRenderZ;
      double var6 = Math.sqrt(var0 * var0 + var2 * var2 + var4 * var4);
      float var8 = 1.0F;
      float var9 = (float)(var6 * var8 * 20.0);
      return Math.round(var9 * 10.0F) / 10.0F;
   }

   public static double b(Entity var0) {
      double var1 = 1.5;
      if (var0 == null) {
         return 1.5;
      }

      double var3 = a();
      double var5 = 0.00342;
      double var7 = var3 * 0.00342;
      return 1.5 + var7;
   }

   public static float a(float var0, float var1) {
      ThreadLocalRandom var2 = ThreadLocalRandom.current();
      double var3 = var2.nextDouble();
      double var5 = var2.nextDouble();
      double var7 = var2.nextGaussian() * 0.02F;
      double var9 = Math.pow(var3, 1.0 + var2.nextDouble() * 0.7);
      double var11 = (var5 * 0.8 + 0.1) * (Math.log1p(var3 * 3.0) * 0.5 + 0.5);
      return (float)(var0 + (var1 - var0) * var9 * var11 + var7);
   }

   public static double randomDouble(double var0, double var2) {
      return ThreadLocalRandom.current().nextDouble() * (var2 - var0) + var0;
   }

   public static boolean isInsideRectangle(double var0, double var2, double var4, double var6, double var8, double var10) {
      return var8 >= var0 && var10 >= var2 && var8 < var0 + var4 && var10 < var2 + var6;
   }

   public static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   public static Vec3d lerp(Vec3d var0, Vec3d var1, float var2) {
      return new Vec3d(
         lerp((float)var0.getX(), (float)var1.getX(), var2),
         lerp((float)var0.getY(), (float)var1.getY(), var2),
         lerp((float)var0.getZ(), (float)var1.getZ(), var2)
      );
   }

   public static Vec3d interpolatePosition(Entity var0, float var1) {
      double var2 = MathHelper.lerp(var1, var0.lastRenderX, var0.getX());
      double var4 = MathHelper.lerp(var1, var0.lastRenderY, var0.getY());
      double var6 = MathHelper.lerp(var1, var0.lastRenderZ, var0.getZ());
      return new Vec3d(var2, var4, var6);
   }

   public static double interpolateReverse(double var0, double var2, double var4) {
      return var2 + (var0 - var2) * var4;
   }

   public static float roundToTenths(float var0) {
      return Math.round(var0 * 10.0F) / 10.0F;
   }

   public static double roundToStep(double var0, double var2) {
      if (var2 <= 0.0) {
         return var0;
      }

      double var4 = Math.round(var0 / var2) * var2;
      return Math.round(var4 * 100.0) / 100.0;
   }

   public static float clampedLerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * clamp(var2, 0.0F, 1.0F);
   }

   public static float clamp(float var0, float var1, float var2) {
      return var0 <= var1 ? var1 : Math.min(var0, var2);
   }

   public static double clampBetween(double var0, double var2, double var4) {
      return Math.max(var0, Math.min(var2, var4));
   }

   public static double lerp(double var0, double var2, double var4) {
      return var0 + var4 * (var2 - var0);
   }

   public static float lerp(float var0, float var1, double var2) {
      return (float)(var0 + var2 * (var1 - var0));
   }

   public static int lerp(int var0, int var1, double var2) {
      return (int)Math.round(var0 + var2 * (var1 - var0));
   }
}
