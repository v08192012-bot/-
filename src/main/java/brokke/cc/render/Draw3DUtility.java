package brokke.cc.render;

import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class Draw3DUtility {
   private Draw3DUtility() {
   }

   public static BufferBuilder a() {
      return ColorPipeline.a();
   }

   public static void a(BufferBuilder var0) {
      if (var0 != null) {
         ColorPipeline.a(var0, true);
      }
   }

   public static BufferBuilder b() {
      return ColorPipeline.a();
   }

   public static void b(BufferBuilder var0) {
      if (var0 != null) {
         ColorPipeline.a(var0, true);
      }
   }

   public static void a(Matrix4f var0, BufferBuilder var1, Box var2, Color var3) {
      int var4 = var3.getAlpha() << 24 | var3.getRed() << 16 | var3.getGreen() << 8 | var3.getBlue();
      float var5 = (float)var2.minX;
      float var6 = (float)var2.minY;
      float var7 = (float)var2.minZ;
      float var8 = (float)var2.maxX;
      float var9 = (float)var2.maxY;
      float var10 = (float)var2.maxZ;
      Draw3DUtility.a[] var11 = new Draw3DUtility.a[]{
         new Draw3DUtility.a(var5, var6, var7),
         new Draw3DUtility.a(var8, var6, var7),
         new Draw3DUtility.a(var8, var6, var10),
         new Draw3DUtility.a(var5, var6, var10),
         new Draw3DUtility.a(var5, var9, var7),
         new Draw3DUtility.a(var8, var9, var7),
         new Draw3DUtility.a(var8, var9, var10),
         new Draw3DUtility.a(var5, var9, var10)
      };
      int[][] var12 = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

      for (int[] var16 : var12) {
         a(var0, var1, var11[var16[0]], var11[var16[1]], var4);
      }
   }

   public static void b(Matrix4f var0, BufferBuilder var1, Box var2, Color var3) {
      int var4 = var3.getAlpha() << 24 | var3.getRed() << 16 | var3.getGreen() << 8 | var3.getBlue();
      float var5 = (float)var2.minX;
      float var6 = (float)var2.minY;
      float var7 = (float)var2.minZ;
      float var8 = (float)var2.maxX;
      float var9 = (float)var2.maxY;
      float var10 = (float)var2.maxZ;
      Draw3DUtility.a[] var11 = new Draw3DUtility.a[]{
         new Draw3DUtility.a(var5, var6, var7),
         new Draw3DUtility.a(var8, var6, var7),
         new Draw3DUtility.a(var8, var6, var10),
         new Draw3DUtility.a(var5, var6, var10),
         new Draw3DUtility.a(var5, var9, var7),
         new Draw3DUtility.a(var8, var9, var7),
         new Draw3DUtility.a(var8, var9, var10),
         new Draw3DUtility.a(var5, var9, var10)
      };
      a(var0, var1, var11[0], var11[6], var4);
      a(var0, var1, var11[1], var11[7], var4);
      a(var0, var1, var11[2], var11[4], var4);
      a(var0, var1, var11[3], var11[5], var4);
   }

   public static void c(Matrix4f var0, BufferBuilder var1, Box var2, Color var3) {
      float var4 = var3.getRed() / 255.0F;
      float var5 = var3.getGreen() / 255.0F;
      float var6 = var3.getBlue() / 255.0F;
      float var7 = var3.getAlpha() / 255.0F;
      float var8 = (float)var2.minX;
      float var9 = (float)var2.minY;
      float var10 = (float)var2.minZ;
      float var11 = (float)var2.maxX;
      float var12 = (float)var2.maxY;
      float var13 = (float)var2.maxZ;
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var11, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var9, var10).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var13).color(var4, var5, var6, var7);
      var1.vertex(var0, var8, var12, var10).color(var4, var5, var6, var7);
   }

   public static void a(Matrix4f var0, BufferBuilder var1, Vec3d var2, Vec3d var3, Color var4) {
      int var5 = var4.getAlpha() << 24 | var4.getRed() << 16 | var4.getGreen() << 8 | var4.getBlue();
      a(var0, var1, new Draw3DUtility.a((float)var2.x, (float)var2.y, (float)var2.z), new Draw3DUtility.a((float)var3.x, (float)var3.y, (float)var3.z), var5);
   }

   private static void a(Matrix4f var0, BufferBuilder var1, Draw3DUtility.a var2, Draw3DUtility.a var3, int var4) {
      float var5 = var3.x - var2.x;
      float var6 = var3.y - var2.y;
      float var7 = var3.z - var2.z;
      float var8 = (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
      if (!(var8 < 1.0E-6F)) {
         float var9 = -var6 / var8;
         float var10 = var5 / var8;
         float var11 = 0.0F;
         float var12 = (float)Math.sqrt(var9 * var9 + var10 * var10);
         if (var12 < 1.0E-6F) {
            var9 = 0.0F;
            var10 = -var7 / var8;
            var11 = var5 / var8;
            var12 = (float)Math.sqrt(var10 * var10 + var11 * var11);
         }

         float var13 = 0.003F;
         var9 *= var13;
         var10 *= var13;
         var11 *= var13;
         float var14 = (var4 >> 16 & 0xFF) / 255.0F;
         float var15 = (var4 >> 8 & 0xFF) / 255.0F;
         float var16 = (var4 & 0xFF) / 255.0F;
         float var17 = (var4 >> 24 & 0xFF) / 255.0F;
         var1.vertex(var0, var2.x + var9, var2.y + var10, var2.z + var11).color(var14, var15, var16, var17);
         var1.vertex(var0, var3.x + var9, var3.y + var10, var3.z + var11).color(var14, var15, var16, var17);
         var1.vertex(var0, var3.x - var9, var3.y - var10, var3.z - var11).color(var14, var15, var16, var17);
         var1.vertex(var0, var2.x + var9, var2.y + var10, var2.z + var11).color(var14, var15, var16, var17);
         var1.vertex(var0, var3.x - var9, var3.y - var10, var3.z - var11).color(var14, var15, var16, var17);
         var1.vertex(var0, var2.x - var9, var2.y - var10, var2.z - var11).color(var14, var15, var16, var17);
      }
   }

   static class a {
      final float x;
      final float y;
      final float z;

      a(float var1, float var2, float var3) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
      }
   }
}
