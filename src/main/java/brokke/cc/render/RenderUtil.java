package brokke.cc.render;

import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfFont;
import brokke.cc.font.MsdfManager;
import brokke.cc.util.ColorHelper;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class RenderUtil {
   private static final List<Runnable> OVERRIDE_TASKS = new ArrayList<>();
   private static final float Z_OVERRIDE = 0.0F;
   private static final int FIXED_GUI_SCALE = 2;
   private static final Matrix4f CACHED_PROJECTION = new Matrix4f();
   private static int cachedScaledWidth = -1;
   private static int cachedScaledHeight = -1;
   private static final ThreadLocal<Matrix4f[]> MATRIX_POOL = ThreadLocal.withInitial(() -> {
      Matrix4f[] var0 = new Matrix4f[16];

      for (int var1 = 0; var1 < var0.length; var1++) {
         var0[var1] = new Matrix4f();
      }

      return var0;
   });
   private static final ThreadLocal<int[]> MATRIX_INDEX = ThreadLocal.withInitial(() -> new int[1]);

   public static void addRunnable(Runnable var0) {
      OVERRIDE_TASKS.add(var0);
   }

   public static int a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null && var0.getWindow() != null) {
         Window var1 = var0.getWindow();
         return (var1.getWidth() + 2 - 1) / 2;
      } else {
         return 960;
      }
   }

   public static int b() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null && var0.getWindow() != null) {
         Window var1 = var0.getWindow();
         return (var1.getHeight() + 2 - 1) / 2;
      } else {
         return 540;
      }
   }

   public static Matrix4f c() {
      int var0 = a();
      int var1 = b();
      if (var0 != cachedScaledWidth || var1 != cachedScaledHeight) {
         cachedScaledWidth = var0;
         cachedScaledHeight = var1;
         CACHED_PROJECTION.setOrtho(0.0F, var0, var1, 0.0F, -1000.0F, 1000.0F);
      }

      return CACHED_PROJECTION;
   }

   public static Matrix4f d() {
      Matrix4f[] var0 = MATRIX_POOL.get();
      int[] var1 = MATRIX_INDEX.get();
      Matrix4f var2 = var0[var1[0]];
      var1[0] = var1[0] + 1 & 15;
      var2.set(c());
      return var2;
   }

   public static float e() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null && var0.getWindow() != null) {
         Window var1 = var0.getWindow();
         double var2 = var1.getScaleFactor();
         return (float)var2 / 2.0F;
      } else {
         return 1.0F;
      }
   }

   public static float a(float var0) {
      return var0 * e();
   }

   public static float b(float var0) {
      return var0 * e();
   }

   public static void a(
      Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9, Color... var10
   ) {
      if (var9) {
         OVERRIDE_TASKS.add(() -> RectPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, ColorHelper.a(var10)));
      } else {
         RectPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, ColorHelper.a(var10));
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9, int var10) {
      if (var9) {
         OVERRIDE_TASKS.add(() -> RectPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, var10));
      } else {
         RectPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, var10);
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
      RectPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, var9);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, boolean var6, int var7) {
      a(var0, var1, var2, var3, var4, var5, var5, var5, var5, var6, var7);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      RectPipeline.a(var0, var1, var2, var3, var4, var5, var5, var5, var5, 0.0F, var6);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8, int var9) {
      a(c(), var0, var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, Color... var8) {
      a(c(), var0, var1, var2, var3, var4, var5, var6, var7, false, var8);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      RectPipeline.a(c(), var0, var1, var2, var3, var4, var5, var6, var7, 0.0F, var8);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, Color... var5) {
      a(c(), var0, var1, var2, var3, var4, var4, var4, var4, false, var5);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, int var5) {
      RectPipeline.a(c(), var0, var1, var2, var3, var4, var4, var4, var4, 0.0F, var5);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, int... var5) {
      RectPipeline.a(c(), var0, var1, var2, var3, var4, var4, var4, var4, 0.0F, var5);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, Color... var6) {
      a(var0, var1, var2, var3, var4, var5, var5, var5, var5, false, var6);
   }

   public static void f() {
      RectPipeline.b();
   }

   public static void g() {
      RectPipeline.c();
   }

   public static void h() {
      OutlinePipeline.b();
   }

   public static void i() {
      OutlinePipeline.c();
   }

   public static void j() {
      BlurPipeline.e();
   }

   public static void k() {
      BlurPipeline.f();
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5) {
      BlurPipeline.a(c(), var0, var1, var2, var3, var4, 0.0F);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      BlurPipeline.a(var0, var1, var2, var3, var4, var5, 0.0F);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      BlurPipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, boolean var6) {
      BlurPipeline.a(c(), var0, var1, var2, var3, var4, 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4) {
      BlurPipeline.a(c(), var0, var1, var2, var3, var4, 0.0F);
      LiquidGlassPipeline.a(c(), var0, var1, var2, var3, var4, 0.0F);
      a(var0, var1, var2, var3, var4, 1.0F, new Color[]{new Color(255, 255, 255, 35)});
   }

   public static void b(float var0, float var1, float var2, float var3, float var4) {
      a(var0, var1, var2, var3, var4);
   }

   public static void c(float var0, float var1, float var2, float var3, float var4) {
      LiquidGlassPipeline.b(c(), var0, var1, var2, var3, var4, 0.0F);
   }

   public static void a(
      Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, Color... var10
   ) {
      OutlinePipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, 0.0F, ColorHelper.a(var10));
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10) {
      OutlinePipeline.a(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, 0.0F, var10);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, Color... var9) {
      OutlinePipeline.a(c(), var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, ColorHelper.a(var9));
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
      OutlinePipeline.a(c(), var0, var1, var2, var3, var4, var5, var6, var7, var8, 0.0F, var9);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, Color... var7) {
      OutlinePipeline.a(var0, var1, var2, var3, var4, var5, var5, var5, var5, var6, 0.0F, ColorHelper.a(var7));
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      OutlinePipeline.a(var0, var1, var2, var3, var4, var5, var5, var5, var5, var6, 0.0F, var7);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, Color... var6) {
      OutlinePipeline.a(c(), var0, var1, var2, var3, var4, var4, var4, var4, var5, 0.0F, ColorHelper.a(var6));
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      OutlinePipeline.a(c(), var0, var1, var2, var3, var4, var4, var4, var4, var5, 0.0F, var6);
   }

   public static void a(DrawContext var0, float var1, float var2, String var3, float var4, Color var5, Color var6, float var7) {
      Msdf.a(var0, var3, var1, var2, var4, ColorHelper.a(var5), ColorHelper.a(var6), var7, false);
   }

   public static void a(DrawContext var0, MsdfFont var1, float var2, float var3, String var4, float var5, Color var6, Color var7, float var8) {
      Msdf.a(var0, var1, var4, var2, var3, var5, ColorHelper.a(var6), ColorHelper.a(var7), var8, false);
   }

   public static void a(DrawContext var0, float var1, float var2, String var3, float var4, Color var5) {
      Msdf.a(var0, var3, var1, var2, var4, ColorHelper.a(var5), false);
   }

   public static void a(DrawContext var0, float var1, float var2, String var3, float var4, Color var5, String var6) {
      if (var6.equals("center")) {
         Msdf.b(var0, var3, var1, var2, var4, ColorHelper.a(var5));
      } else if (var6.equals("right")) {
         Msdf.c(var0, var3, var1, var2, var4, ColorHelper.a(var5));
      } else {
         Msdf.a(var0, var3, var1, var2, var4, ColorHelper.a(var5));
      }
   }

   public static void a(DrawContext var0, float var1, float var2, String var3, float var4, Color var5, Color var6, String var7) {
      if (var7.equals("center")) {
         Msdf.a(var0, var3, var1, var2, var4, ColorHelper.a(var5), ColorHelper.a(var6));
      } else if (var7.equals("right")) {
         Msdf.b(var0, var3, var1, var2, var4, ColorHelper.a(var5), ColorHelper.a(var6));
      } else {
         Msdf.c(var0, var3, var1, var2, var4, ColorHelper.a(var5), ColorHelper.a(var6));
      }
   }

   public static void a(DrawContext var0, MsdfFont var1, float var2, float var3, String var4, float var5, Color var6) {
      Msdf.a(var0, var1, var4, var2, var3, var5, ColorHelper.a(var6), false);
   }

   public static void a(DrawContext var0, float var1, float var2, String var3, float var4, Color... var5) {
      Msdf.a(var0, MsdfManager.MONTSERRAT, var3, var1, var2, var4, false, ColorHelper.a(var5));
   }

   public static void a(float var0, float var1, float var2, Identifier var3, float var4, Color var5) {
      a(c(), var0, var1, var2, var3, var4, var5);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, Identifier var4, float var5, Color var6) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      AbstractTexture var8 = var7.getTextureManager().getTexture(var4);
      TexturePipeline.a(var0, var1, var2, var3, var8.getGlTextureView(), ColorHelper.a(var6), var5, 0.0F);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, Identifier var4, float var5, int var6) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      AbstractTexture var8 = var7.getTextureManager().getTexture(var4);
      TexturePipeline.a(var0, var1, var2, var3, var8.getGlTextureView(), var6, var5, 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, Identifier var4, float var5, Color var6) {
      a(c(), var0, var1, var2, var3, var4, var5, var6);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, Identifier var5, float var6, Color var7) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      AbstractTexture var9 = var8.getTextureManager().getTexture(var5);
      TexturePipeline.a(var0, var1, var2, var3, var4, var9.getGlTextureView(), ColorHelper.a(var7), var6, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, Identifier var5, float var6, int var7) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      AbstractTexture var9 = var8.getTextureManager().getTexture(var5);
      TexturePipeline.a(var0, var1, var2, var3, var4, var9.getGlTextureView(), var7, var6, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   public static void a(float var0, float var1, float var2, Color var3) {
      GlowPipeline.a(c(), var0, var1, var2, ColorHelper.a(var3), 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      a(var0, var1, var2, var3, var4, var5, 0.0F, 1.0F, 0.0F, var6);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, Color var7) {
      a(var0, var1, var2, var3, var4, var5, 0.0F, 1.0F, Math.clamp(var6, 0.0F, 1.0F), var7);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, Color var9) {
      GlowOutlinePipeline.a(c(), var0, var1, var2, var3, var4, ColorHelper.a(var9), var5, var6, Math.clamp(var7, 0.0F, 1.0F), var8, 1.0F, 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, Color var8) {
      if (!(var5 <= 0.0F) && !(var6 <= 0.0F) && var8 != null) {
         b(c(), var0, var1, var2, var3, var4, var5, var6, var7, var8.getRGB());
      }
   }

   public static void b(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      if (!(var5 <= 0.0F) && !(var6 <= 0.0F)) {
         b(c(), var0, var1, var2, var3, var4, var5, var6, var7, var8);
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, Color var9) {
      if (!(var6 <= 0.0F) && !(var7 <= 0.0F) && var9 != null) {
         b(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9.getRGB());
      }
   }

   public static void b(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
      if (!(var6 <= 0.0F) && !(var7 <= 0.0F)) {
         GlowOutlinePipeline.a(var0, var1, var2, var3, var4, 0.0F, var9, var5, var6, Math.clamp(var7, 0.0F, 1.0F), Math.max(0.1F, var8), 1.0F, 0.0F);
      }
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, Color var5) {
      a(var0, var1, var2, var3, var4, 4.5F, 0.4F, 2.5F, var5);
   }

   public static void b(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      b(var0, var1, var2, var3, var4, var5, 4.5F, 0.4F, 2.5F, var6);
   }

   public static void b(float var0, float var1, float var2, float var3, float var4, int var5) {
      b(c(), var0, var1, var2, var3, var4, 4.5F, 0.4F, 2.5F, var5);
   }

   public static boolean l() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0.currentScreen == null || var0.currentScreen instanceof ChatScreen;
   }

   public static void b(float var0, float var1, float var2, float var3, float var4, Color var5) {
      switch (Theme.getInstance().getGuiStyle()) {
         case NORMAL:
            a(var0, var1, var2, var3, var4);
            a(var0, var1, var2, var3, var4, new Color[]{var5});
            break;
         case BLUR:
            a(var0, var1, var2, var3, var4);
            a(var0, var1, var2, var3, var4, 10.0F);
            a(var0, var1, var2, var3, var4, new Color[]{var5});
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, Color var6, Color var7, float var8) {
      CircleProgressPipeline.a(var0, var1, var2, var3, var4, var5, ColorHelper.a(var6), ColorHelper.a(var7), var8, 0.0F);
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, Color var5, Color var6, float var7) {
      a(c(), var0, var1, var2, var3, var4, var5, var6, var7);
   }
}
