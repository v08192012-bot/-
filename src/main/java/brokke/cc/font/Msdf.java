package brokke.cc.font;

import brokke.cc.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;

public class Msdf {
   public static MsdfFont MONTSERRAT;
   public static MsdfFont ICONS;
   public static MsdfFont BOLD;
   public static MsdfFont ICONS2;
   public static MsdfFont DICONS;
   private static boolean useFallback = false;

   public static void setUseFallback(boolean var0) {
      useFallback = var0;
   }

   public static boolean a() {
      MsdfManager.a();
      MsdfPipeline.a();
      return MsdfManager.getDefaultFont() != null && MsdfManager.getDefaultFont().isLoaded();
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      a(var0, var1, var2, var3, var4, var5, false);
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, int var5, boolean var6) {
      if (!useFallback && a()) {
         a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, var5, var6);
      } else {
         e(var0, var1, var2, var3, var4, var5);
      }
   }

   public static void a(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6, boolean var7) {
      MsdfFont var8 = MsdfManager.a(var1);
      if (!useFallback && var8 != null && var8.isLoaded()) {
         a(var0, var8, var2, var3, var4, var5, var6, var7);
      } else {
         e(var0, var2, var3, var4, var5, var6);
      }
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6, boolean var7) {
      a(var0, var1, var2, var3, var4, var5, var6, 0.0F, var7);
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6, float var7, boolean var8) {
      if (a()) {
         if (var1 == null) {
            var1 = MsdfManager.getDefaultFont();
         }

         if (var8) {
            MsdfFont var9 = var1;
            RenderUtil.addRunnable(() -> MsdfPipeline.a(RenderUtil.c(), var9, var2, var3, var4, var5, var6, var7, 500.0F));
         } else if (var1 != null && var1.isLoaded()) {
            MsdfPipeline.a(RenderUtil.c(), var1, var2, var3, var4, var5, var6, var7, 0.0F);
         } else {
            e(var0, var2, var3, var4, var5, var6);
         }
      }
   }

   public static void a(Matrix4f var0, String var1, float var2, float var3, float var4, int var5) {
      if (a()) {
         MsdfPipeline.a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, var5, 0.0F, 0.0F);
      }
   }

   public static void a(Matrix4f var0, String var1, String var2, float var3, float var4, float var5, int var6) {
      if (a()) {
         MsdfPipeline.a(var0, MsdfManager.a(var1), var2, var3, var4, var5, var6, 0.0F, 0.0F);
      }
   }

   public static void a(Matrix4f var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6) {
      if (a()) {
         MsdfPipeline.a(var0, var1, var2, var3, var4, var5, var6, 0.0F, 0.0F);
      }
   }

   public static void b() {
      MsdfPipeline.b();
   }

   public static void c() {
      MsdfPipeline.c();
   }

   private static void e(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      var0.drawText(var6.textRenderer, var1, (int)var2, (int)var3, var5, false);
   }

   public static float a(String var0, float var1) {
      return a(MsdfManager.getDefaultFont(), var0, var1);
   }

   public static float a(String var0, String var1, float var2) {
      return a(MsdfManager.a(var0), var1, var2);
   }

   public static float a(MsdfFont var0, String var1, float var2) {
      return var0 == null ? 0.0F : var0.a(var1, var2);
   }

   public static float a(float var0) {
      return a(MsdfManager.getDefaultFont(), var0);
   }

   public static float b(String var0, float var1) {
      return a(MsdfManager.a(var0), var1);
   }

   public static float a(MsdfFont var0, float var1) {
      return var0 == null ? var1 : var0.getLineHeight() * var1;
   }

   public static void b(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      a(var0, var1, var2 - a(var1, var4) / 2.0F, var3, var4, var5, false);
   }

   public static void a(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6) {
      a(var0, var1, var2, var3 - a(var1, var2, var5) / 2.0F, var4, var5, var6, false);
   }

   public static void c(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      a(var0, var1, var2 - a(var1, var4), var3, var4, var5, false);
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, int var5, int var6) {
      float var7 = var2 - a(var1, var4) / 2.0F;
      a(var0, MsdfManager.getDefaultFont(), var1, var7, var3, var4, var5, var6, false);
   }

   public static void a(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6, int var7) {
      float var8 = var3 - a(var1, var2, var5) / 2.0F;
      a(var0, MsdfManager.a(var1), var2, var8, var4, var5, var6, var7, false);
   }

   public static void b(DrawContext var0, String var1, float var2, float var3, float var4, int var5, int var6) {
      float var7 = var2 - a(var1, var4);
      a(var0, MsdfManager.getDefaultFont(), var1, var7, var3, var4, var5, var6, false);
   }

   public static void b(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6, int var7) {
      float var8 = var3 - a(var1, var2, var5);
      a(var0, MsdfManager.a(var1), var2, var8, var4, var5, var6, var7, false);
   }

   public static void b(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6) {
      a(var0, var1, var2, var3 - a(var1, var2, var5), var4, var5, var6, false);
   }

   public static void d(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      a(var0, var1, var2 + 1.0F, var3 + 1.0F, var4, 1140850688, false);
      a(var0, var1, var2, var3, var4, var5, false);
   }

   public static void c(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6) {
      a(var0, var1, var2, var3 + 1.0F, var4 + 1.0F, var5, 1140850688, false);
      a(var0, var1, var2, var3, var4, var5, var6, false);
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, int var5, int var6, float var7, boolean var8) {
      a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6, int var7, float var8, boolean var9) {
      if (a()) {
         if (var1 == null) {
            var1 = MsdfManager.getDefaultFont();
         }

         if (var9) {
            MsdfFont var10 = var1;
            RenderUtil.addRunnable(() -> MsdfGlowPipeline.a(RenderUtil.c(), var10, var2, var3, var4, var5, var6, var7, var8, 500.0F));
         } else if (var1 != null && var1.isLoaded()) {
            MsdfGlowPipeline.a(RenderUtil.c(), var1, var2, var3, var4, var5, var6, var7, var8, 0.0F);
         }
      }
   }

   public static void a(DrawContext var0, String var1, String var2, float var3, float var4, float var5, int var6, int var7, float var8, boolean var9) {
      MsdfFont var10 = MsdfManager.a(var1);
      a(var0, var10, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, float var5) {
      a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, var5, false);
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, float var6, boolean var7) {
      if (var7) {
         RenderUtil.addRunnable(() -> a(var0, var1, var2, var3, var4, var5, var6, false));
      } else if (var1 != null && !var2.isEmpty()) {
         int[] var8 = new int[var2.length()];
         long var9 = System.currentTimeMillis();

         for (int var11 = 0; var11 < var2.length(); var11++) {
            float var12 = ((float)var9 * var6 / 1000.0F + var11 * 0.1F) % 1.0F;
            var8[var11] = a(var12, 1.0F, 1.0F);
         }

         MsdfPipeline.a(RenderUtil.c(), var1, var2, var3, var4, var5, var8, 500.0F);
      }
   }

   public static void c(DrawContext var0, String var1, float var2, float var3, float var4, int var5, int var6) {
      a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, var5, var6, false);
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6, int var7, boolean var8) {
      if (var8) {
         RenderUtil.addRunnable(() -> a(var0, var1, var2, var3, var4, var5, var6, var7, false));
      } else if (var1 != null && !var2.isEmpty()) {
         int[] var9 = new int[var2.length()];

         for (int var10 = 0; var10 < var2.length(); var10++) {
            float var11 = var2.length() > 1 ? (float)var10 / (var2.length() - 1) : 0.0F;
            var9[var10] = a(var6, var7, var11);
         }

         MsdfPipeline.a(RenderUtil.c(), var1, var2, var3, var4, var5, var9, 500.0F);
      }
   }

   public static void a(DrawContext var0, String var1, float var2, float var3, float var4, int... var5) {
      a(var0, MsdfManager.getDefaultFont(), var1, var2, var3, var4, false, var5);
   }

   public static void a(DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, boolean var6, int... var7) {
      if (var6) {
         RenderUtil.addRunnable(() -> a(var0, var1, var2, var3, var4, var5, false, var7));
      } else if (var1 != null && !var2.isEmpty() && var7.length != 0) {
         float var8 = RenderUtil.l() ? 500.0F : 0.0F;
         MsdfPipeline.a(RenderUtil.c(), var1, var2, var3, var4, var5, var7, var8);
      }
   }

   private static int a(float var0, float var1, float var2) {
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;
      if (var1 == 0.0F) {
         var3 = var4 = var5 = (int)(var2 * 255.0F + 0.5F);
      } else {
         float var6 = (var0 - (float)Math.floor(var0)) * 6.0F;
         float var7 = var6 - (float)Math.floor(var6);
         float var8 = var2 * (1.0F - var1);
         float var9 = var2 * (1.0F - var1 * var7);
         float var10 = var2 * (1.0F - var1 * (1.0F - var7));
         switch ((int)var6) {
            case 0:
               var3 = (int)(var2 * 255.0F + 0.5F);
               var4 = (int)(var10 * 255.0F + 0.5F);
               var5 = (int)(var8 * 255.0F + 0.5F);
               break;
            case 1:
               var3 = (int)(var9 * 255.0F + 0.5F);
               var4 = (int)(var2 * 255.0F + 0.5F);
               var5 = (int)(var8 * 255.0F + 0.5F);
               break;
            case 2:
               var3 = (int)(var8 * 255.0F + 0.5F);
               var4 = (int)(var2 * 255.0F + 0.5F);
               var5 = (int)(var10 * 255.0F + 0.5F);
               break;
            case 3:
               var3 = (int)(var8 * 255.0F + 0.5F);
               var4 = (int)(var9 * 255.0F + 0.5F);
               var5 = (int)(var2 * 255.0F + 0.5F);
               break;
            case 4:
               var3 = (int)(var10 * 255.0F + 0.5F);
               var4 = (int)(var8 * 255.0F + 0.5F);
               var5 = (int)(var2 * 255.0F + 0.5F);
               break;
            case 5:
               var3 = (int)(var2 * 255.0F + 0.5F);
               var4 = (int)(var8 * 255.0F + 0.5F);
               var5 = (int)(var9 * 255.0F + 0.5F);
         }
      }

      return 0xFF000000 | var3 << 16 | var4 << 8 | var5;
   }

   private static int a(int var0, int var1, float var2) {
      int var3 = var0 >> 24 & 0xFF;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >> 24 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      int var11 = (int)(var3 + (var7 - var3) * var2);
      int var12 = (int)(var4 + (var8 - var4) * var2);
      int var13 = (int)(var5 + (var9 - var5) * var2);
      int var14 = (int)(var6 + (var10 - var6) * var2);
      return var11 << 24 | var12 << 16 | var13 << 8 | var14;
   }
}
