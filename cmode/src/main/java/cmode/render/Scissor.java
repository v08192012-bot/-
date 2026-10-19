package cmode.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;

public class Scissor {
   private static final double[][] STACK = new double[32][4];
   private static int stackSize = 0;

   public static void a(int var0, int var1, int var2, int var3) {
      b(var0, var1, Math.max(0, var2 - var0), Math.max(0, var3 - var1));
   }

   public static void a(double var0, double var2, double var4, double var6) {
      b(var0, var2, Math.max(0.0, var4 - var0), Math.max(0.0, var6 - var2));
   }

   public static void a() {
      c();
   }

   public static void b(double var0, double var2, double var4, double var6) {
      a(var0, var2, var4, var6, false);
   }

   public static void a(double var0, double var2, double var4, double var6, boolean var8) {
      if (var8) {
         RenderUtil.addRunnable(() -> a(var0, var2, var4, var6, false));
      } else {
         GuiBatch.flush();
         double var9 = var0;
         double var11 = var2;
         double var13 = Math.max(0.0, var4);
         double var15 = Math.max(0.0, var6);
         if (stackSize > 0) {
            double[] var17 = STACK[stackSize - 1];
            double var18 = Math.max(var9, var17[0]);
            double var20 = Math.max(var11, var17[1]);
            double var22 = Math.min(var9 + var13, var17[0] + var17[2]) - var18;
            double var24 = Math.min(var11 + var15, var17[1] + var17[3]) - var20;
            var9 = var18;
            var11 = var20;
            var13 = Math.max(0.0, var22);
            var15 = Math.max(0.0, var24);
         }

         if (stackSize < STACK.length) {
            STACK[stackSize][0] = var9;
            STACK[stackSize][1] = var11;
            STACK[stackSize][2] = var13;
            STACK[stackSize][3] = var15;
            stackSize++;
         }

         c(var9, var11, var13, var15);
      }
   }

   public static void a(double var0, double var2) {
      a(var0, var2, false);
   }

   public static void a(double var0, double var2, boolean var4) {
      double var5 = RenderUtil.a();
      a(0.0, var0, var5, var2, var4);
   }

   public static void b() {
      b(false);
   }

   public static void a(boolean var0) {
      b(var0);
   }

   public static void c() {
      b(false);
   }

   public static void b(boolean var0) {
      if (var0) {
         RenderUtil.addRunnable(() -> b(false));
      } else {
         GuiBatch.flush();
         if (stackSize > 0) {
            stackSize--;
         }

         if (stackSize == 0) {
            GlStateManager._disableScissorTest();
         } else {
            double[] var1 = STACK[stackSize - 1];
            c(var1[0], var1[1], var1[2], var1[3]);
         }
      }
   }

   public static void a(float var0, Color var1) {
      if (stackSize != 0 && !(var0 <= 0.0F)) {
         double[] var2 = STACK[stackSize - 1];
         if (!(var2[2] <= 0.0) && !(var2[3] <= 0.0)) {
            Color var3 = new Color(var1.getRed(), var1.getGreen(), var1.getBlue(), 0);
            float var4 = (float)var2[0];
            float var5 = (float)var2[1];
            float var6 = (float)var2[2];
            float var7 = (float)var2[3];
            float var8 = Math.min(var0, Math.min(var6, var7) * 0.5F);
            RenderUtil.a(var4, var5, var6, var8, 0.0F, 0.0F, 0.0F, 0.0F, var1, var1, var3, var1, var1, var3, var3, var3, var3);
            RenderUtil.a(var4, var5 + var7 - var8, var6, var8, 0.0F, 0.0F, 0.0F, 0.0F, var3, var3, var3, var1, var1, var3, var1, var1, var3);
            RenderUtil.a(
               var4, var5 + var8, var8, Math.max(0.0F, var7 - var8 * 2.0F), 0.0F, 0.0F, 0.0F, 0.0F, var1, var3, var3, var1, var3, var3, var1, var3, var3
            );
            RenderUtil.a(
               var4 + var6 - var8,
               var5 + var8,
               var8,
               Math.max(0.0F, var7 - var8 * 2.0F),
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               var3,
               var3,
               var1,
               var3,
               var3,
               var1,
               var3,
               var3,
               var1
            );
         }
      }
   }

   public static void apply(RenderPass pass) {
      if (stackSize != 0) {
         double[] bounds = STACK[stackSize - 1];
         MinecraftClient client = MinecraftClient.getInstance();
         float scale = client.getWindow().getWidth() / Math.max(1.0F, RenderUtil.a());
         int x = Math.max(0, (int)Math.floor(bounds[0] * scale));
         int y = Math.max(0, (int)Math.floor(client.getWindow().getHeight() - (bounds[1] + bounds[3]) * scale));
         int width = Math.max(0, (int)Math.ceil(bounds[2] * scale));
         int height = Math.max(0, (int)Math.ceil(bounds[3] * scale));
         pass.enableScissor(x, y, width, height);
      }
   }

   private static void c(double var0, double var2, double var4, double var6) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      float var9 = var8.getWindow().getWidth() / Math.max(1.0F, RenderUtil.a());
      int var10 = (int)Math.floor(var0 * var9);
      int var11 = (int)Math.floor(var8.getWindow().getHeight() - (var2 + var6) * var9);
      int var12 = (int)Math.ceil(var4 * var9);
      int var13 = (int)Math.ceil(var6 * var9);
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(Math.max(0, var10), Math.max(0, var11), Math.max(0, var12), Math.max(0, var13));
   }
}
