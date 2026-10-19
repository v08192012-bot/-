package brokke.cc.render;

import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class Render3DUtil {
   private Render3DUtil() {
   }

   public static Vec3d a(Entity var0, float var1) {
      return var0.getLerpedPos(var1);
   }

   public static float[] a(Vec3d var0) {
      return WorldToScreen.a(var0);
   }

   public static void a(BufferBuilder var0, Vec3d var1, Vec3d var2, Color var3, float var4) {
      float[][] var5 = WorldToScreen.a(var1, var2);
      if (var5 != null) {
         int var6 = var3.getAlpha() << 24 | var3.getRed() << 16 | var3.getGreen() << 8 | var3.getBlue();
         ColorPipeline.a(var0, var5[0][0], var5[0][1], var5[1][0], var5[1][1], var4, var6);
      }
   }

   public static void a(BufferBuilder var0, Vec3d var1, float var2, Color var3, float var4) {
      float var5 = var2 * 0.5F;
      Vec3d[] var6 = new Vec3d[]{
         new Vec3d(var1.x - var5, var1.y - var5, var1.z - var5),
         new Vec3d(var1.x + var5, var1.y - var5, var1.z - var5),
         new Vec3d(var1.x + var5, var1.y - var5, var1.z + var5),
         new Vec3d(var1.x - var5, var1.y - var5, var1.z + var5),
         new Vec3d(var1.x - var5, var1.y + var5, var1.z - var5),
         new Vec3d(var1.x + var5, var1.y + var5, var1.z - var5),
         new Vec3d(var1.x + var5, var1.y + var5, var1.z + var5),
         new Vec3d(var1.x - var5, var1.y + var5, var1.z + var5)
      };
      int[][] var7 = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

      for (int[] var11 : var7) {
         a(var0, var6[var11[0]], var6[var11[1]], var3, var4);
      }
   }

   public static BufferBuilder a() {
      return ColorPipeline.a();
   }

   public static void a(BufferBuilder var0) {
      if (var0 != null) {
         ColorPipeline.a(var0, false);
      }
   }
}
