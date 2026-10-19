package brokke.cc.core;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.AutoBuyConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfFont;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.misc.AutoBuy;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.ui.ClickGui;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Matrix4f;

public final class AutoBuyOverlay {
   public static final int COL_W = 112;
   private static final int BTN_H = 17;
   private static final float BTN_GAP = 3.5F;
   private static final Map<String, Animation> TOGGLE_ANIMS = new HashMap<>();
   private static HandledScreen<?> lastScreen = null;
   private static int lastGx;
   private static int lastGy;
   private static int lastGw;
   private static int lastGh;
   private static int lastMouseX;
   private static int lastMouseY;
   private static boolean hasPendingRender = false;

   private static Animation a(String var0, boolean var1) {
      return TOGGLE_ANIMS.computeIfAbsent(var0, var1xx -> {
         Animation var2 = new Animation(var1 ? 1.0F : 0.0F);
         var2.setEasing(Easing.SPRING_SOFT);
         return var2;
      });
   }

   private AutoBuyOverlay() {
   }

   public static boolean a(HandledScreen<?> var0) {
      AutoBuy var1 = FunctionManager.a(AutoBuy.class);
      if (var1 != null && !var1.k()) {
         return false;
      } else if (!(var0 instanceof GenericContainerScreen var2)) {
         return false;
      } else {
         if (AutoBuyEngine.a(var2)) {
            return true;
         }

         if (var2.getScreenHandler().getRows() != 6) {
            return false;
         }

         String var3 = var2.getTitle().getString();
         return var3 != null && var3.toLowerCase(Locale.ROOT).contains("аукцион");
      }
   }

   public static void a(HandledScreen<?> var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      lastScreen = var0;
      lastGx = var1;
      lastGy = var2;
      lastGw = var3;
      lastGh = var4;
      lastMouseX = var5;
      lastMouseY = var6;
      hasPendingRender = true;
   }

   public static void a(MinecraftClient var0, GuiRenderState var1, GuiRenderer var2, FogRenderer var3) {
      if (hasPendingRender) {
         hasPendingRender = false;
         if (var0 == null || var0.currentScreen == null || var0.currentScreen != lastScreen) {
            lastScreen = null;
         } else if (!a(lastScreen)) {
            lastScreen = null;
         } else {
            a(var0, lastGx, lastGy, lastGw, lastGh, lastMouseX, lastMouseY, var1, var2, var3);
         }
      }
   }

   public static void a(
      MinecraftClient var0, int var1, int var2, int var3, int var4, int var5, int var6, GuiRenderState var7, GuiRenderer var8, FogRenderer var9
   ) {
      if (var0 != null
         && var0.getWindow() != null
         && !var0.getWindow().isMinimized()
         && var0.getWindow().getScaledWidth() > 0
         && var0.getWindow().getScaledHeight() > 0) {
         if (!Msdf.a()) {
            MsdfManager.a();
         }

         AutoBuyConfig var10 = AutoBuyConfig.getInstance();
         boolean var11 = AutoBuyEngine.INSTANCE.parser.c();
         int var12 = Theme.getInstance().getAccent().getRGB();
         int var13 = a(var1, var3, var0);
         float var14 = var2 + 2.0F;
         int var15 = var0.getWindow().getScaledWidth();
         int var16 = var0.getWindow().getScaledHeight();
         Matrix4f var17 = new Matrix4f().setOrtho(0.0F, var15, var16, 0.0F, -1000.0F, 1000.0F);
         MsdfFont var18 = MsdfManager.BOLD != null ? MsdfManager.BOLD : MsdfManager.getDefaultFont();
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "AutoBuy", var10.enabled, var5, var6, var12, var18);
         var14 += 3.5F;
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "AutoSell", var10.autoSell, var5, var6, var12, var18);
         var14 += 3.5F;
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "Релист", var10.autoRelist, var5, var6, var12, var18);
         var14 += 3.5F;
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "Автопарс", var10.autoParse, var5, var6, var12, var18);
         var14 += 3.5F;
         String var19 = var11 ? "СТОП" : "ПУСК";
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "Парс цен", var19, var11, var5, var6, var12, var18);
         var14 += 3.5F;
         var14 = a(var17, var13, var14, 112.0F, 17.0F, "Меню AutoBuy", var5, var6, var12, var18);
         float var35 = var14 + 3.5F;
         if (var11) {
            String var20 = AutoBuyEngine.INSTANCE.parser.d();
            if (InterfaceModule.l()) {
               RenderUtil.b(var17, var13, var35, 112.0F, 16.0F, 3.5F, 4.5F, 0.35F, 2.5F, 1342177280);
            }

            if (InterfaceModule.k()) {
               RenderUtil.a(var17, var13, var35, 112.0F, 16.0F, 3.5F, 10.0F);
            }

            int var21 = InterfaceModule.k() ? 120 : 235;
            RenderUtil.a(var17, var13, var35, 112.0F, 16.0F, 3.5F, var21 << 24 | 1184796);
            RenderUtil.a(var17, var13, var35, 112.0F, 16.0F, 3.5F, 0.55F, a(var12, 160.0F));
            String var22 = a(var18, var20, 4.5F, 102.0F);
            float var23 = Msdf.a(var18, 4.5F);
            Msdf.a(var17, var18, var22, var13 + 5.0F, var35 + (16.0F - var23) / 2.0F - 0.5F, 4.5F, var12);
         }
      }
   }

   public static boolean a(double var0, double var2, int var4, int var5, int var6, int var7, int var8) {
      if (var4 != 0) {
         return false;
      }

      MinecraftClient var9 = MinecraftClient.getInstance();
      int var10 = a(var5, var7, var9);
      float var11 = var6 + 2.0F;
      if (a(var0, var2, var10, var11, 112.0F, 17.0F)) {
         AutoBuyConfig var21 = AutoBuyConfig.getInstance();
         var21.enabled = !var21.enabled;
         AutoBuy var13 = FunctionManager.a(AutoBuy.class);
         if (var13 != null && var13.isActive() != var21.enabled) {
            var13.setEnabled(var21.enabled);
         }

         AutoBuyConfig.g();
         return true;
      } else {
         float var14 = var11 + 20.5F;
         if (a(var0, var2, var10, var14, 112.0F, 17.0F)) {
            AutoBuyConfig var20 = AutoBuyConfig.getInstance();
            var20.autoSell = !var20.autoSell;
            AutoBuyConfig.g();
            return true;
         }

         var11 = var14 + 20.5F;
         if (a(var0, var2, var10, var11, 112.0F, 17.0F)) {
            AutoBuyConfig var19 = AutoBuyConfig.getInstance();
            var19.autoRelist = !var19.autoRelist;
            AutoBuyConfig.g();
            return true;
         }

         float var16 = var11 + 20.5F;
         if (a(var0, var2, var10, var16, 112.0F, 17.0F)) {
            AutoBuyConfig var12 = AutoBuyConfig.getInstance();
            var12.autoParse = !var12.autoParse;
            AutoBuyConfig.g();
            return true;
         }

         var11 = var16 + 20.5F;
         if (a(var0, var2, var10, var11, 112.0F, 17.0F)) {
            if (AutoBuyEngine.INSTANCE.parser.c()) {
               AutoBuyEngine.INSTANCE.parser.b();
            } else {
               AutoBuyEngine.INSTANCE.parser.a();
            }

            return true;
         } else {
            var11 += 20.5F;
            if (a(var0, var2, var10, var11, 112.0F, 17.0F)) {
               ClickGui.j();
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public static int a(int var0, int var1, MinecraftClient var2) {
      int var3 = var0 + var1 + 6;
      int var4 = var2.getWindow().getScaledWidth();
      if (var3 + 112 > var4 - 4) {
         var3 = var0 - 112 - 6;
      }

      return Math.max(2, var3);
   }

   private static float a(
      Matrix4f var0, float var1, float var2, float var3, float var4, String var5, boolean var6, double var7, double var9, int var11, MsdfFont var12
   ) {
      boolean var13 = a(var7, var9, var1, var2, var3, var4);
      int var14 = var13 ? 1777191 : 1184796;
      int var15 = var6 ? a(var14, var11, var13 ? 0.16F : 0.09F) : var14;
      int var16 = InterfaceModule.k() ? 120 : 235;
      int var17 = var16 << 24 | var15;
      if (InterfaceModule.l()) {
         RenderUtil.b(var0, var1, var2, var3, var4, 4.5F, 4.5F, 0.35F, 2.5F, 1342177280);
      }

      if (InterfaceModule.k()) {
         RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 10.0F);
      }

      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, var17);
      int var18 = var6 ? a(var11, var13 ? 210.0F : 130.0F) : (var13 ? a(-1, 50.0F) : a(-1, 24.0F));
      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 0.65F, var18);
      float var19 = 5.5F;
      float var20 = var2 + (var4 - Msdf.a(var12, var19)) / 2.0F - 0.5F;
      int var21 = var6 ? -1 : (var13 ? -1775374 : -6249036);
      Msdf.a(var0, var12, var5, var1 + 7.5F, var20, var19, var21);
      Animation var22 = a(var5, var6);
      var22.animate(var6 ? 1.0F : 0.0F, 0.0F, 8.5F);
      float var23 = Math.clamp(var22.getValue(), 0.0F, 1.0F);
      float var24 = 18.0F;
      float var25 = 10.0F;
      float var26 = 5.0F;
      float var27 = var1 + var3 - var24 - 5.5F;
      float var28 = var2 + (var4 - var25) / 2.0F;
      int var29 = a(1159734059, a(-13486527, var11, 0.9F), var23);
      int var30 = a(a(-1, 35.0F), a(var11, 150.0F), var23);
      RenderUtil.a(var0, var27, var28, var24, var25, var26, var29);
      RenderUtil.a(var0, var27, var28, var24, var25, var26, 0.5F, var30);
      float var31 = 7.6F;
      float var32 = 3.8F;
      float var33 = var27 + 1.2F + (var24 - 2.4F - var31) * var23;
      float var34 = var28 + 1.2F;
      int var35 = a(-2828062, -1, var23);
      RenderUtil.a(var0, var33, var34, var31, var31, var32, var35);
      return var2 + var4;
   }

   private static float a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      String var5,
      String var6,
      boolean var7,
      double var8,
      double var10,
      int var12,
      MsdfFont var13
   ) {
      boolean var14 = a(var8, var10, var1, var2, var3, var4);
      int var15 = var14 ? 1777191 : 1184796;
      int var16 = var7 ? a(var15, -50384, var14 ? 0.16F : 0.09F) : var15;
      int var17 = InterfaceModule.k() ? 120 : 235;
      int var18 = var17 << 24 | var16;
      if (InterfaceModule.l()) {
         RenderUtil.b(var0, var1, var2, var3, var4, 4.5F, 4.5F, 0.35F, 2.5F, 1342177280);
      }

      if (InterfaceModule.k()) {
         RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 10.0F);
      }

      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, var18);
      int var19 = var7 ? a(-47803, var14 ? 220.0F : 150.0F) : (var14 ? a(var12, 160.0F) : a(-1, 24.0F));
      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 0.65F, var19);
      float var20 = 5.5F;
      float var21 = var2 + (var4 - Msdf.a(var13, var20)) / 2.0F - 0.5F;
      int var22 = var7 ? -1 : (var14 ? -1775374 : -6249036);
      Msdf.a(var0, var13, var5, var1 + 7.5F, var21, var20, var22);
      float var23 = 4.4F;
      float var24 = Msdf.a(var13, var6, var23);
      float var25 = var24 + 8.5F;
      float var26 = 10.5F;
      float var27 = var1 + var3 - var25 - 5.5F;
      float var28 = var2 + (var4 - var26) / 2.0F;
      if (var7) {
         RenderUtil.a(var0, var27, var28, var25, var26, 2.5F, a(-47803, 60.0F));
         RenderUtil.a(var0, var27, var28, var25, var26, 2.5F, 0.55F, -47803);
         Msdf.a(var0, var13, var6, var27 + (var25 - var24) / 2.0F, var28 + (var26 - Msdf.a(var13, var23)) / 2.0F - 0.5F, var23, -39322);
      } else {
         RenderUtil.a(var0, var27, var28, var25, var26, 2.5F, var14 ? a(var12, 45.0F) : 419430399);
         RenderUtil.a(var0, var27, var28, var25, var26, 2.5F, 0.5F, var14 ? var12 : 905969663);
         Msdf.a(var0, var13, var6, var27 + (var25 - var24) / 2.0F, var28 + (var26 - Msdf.a(var13, var23)) / 2.0F - 0.5F, var23, var14 ? var12 : -6249036);
      }

      return var2 + var4;
   }

   private static float a(Matrix4f var0, float var1, float var2, float var3, float var4, String var5, double var6, double var8, int var10, MsdfFont var11) {
      boolean var12 = a(var6, var8, var1, var2, var3, var4);
      int var13 = var12 ? 1777191 : 1184796;
      int var14 = var12 ? a(var13, var10, 0.12F) : var13;
      int var15 = InterfaceModule.k() ? 120 : 235;
      int var16 = var15 << 24 | var14;
      if (InterfaceModule.l()) {
         RenderUtil.b(var0, var1, var2, var3, var4, 4.5F, 4.5F, 0.35F, 2.5F, 1342177280);
      }

      if (InterfaceModule.k()) {
         RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 10.0F);
      }

      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, var16);
      int var17 = var12 ? a(var10, 210.0F) : a(-1, 24.0F);
      RenderUtil.a(var0, var1, var2, var3, var4, 4.5F, 0.65F, var17);
      float var18 = 6.5F;
      float var19 = Msdf.a(MsdfManager.DICONS, "C", var18);
      float var20 = 5.3F;
      float var21 = Msdf.a(var11, var5, var20);
      float var22 = 4.5F;
      float var23 = var19 + var22 + var21;
      float var24 = var1 + (var3 - var23) / 2.0F;
      int var25 = var12 ? var10 : -4406832;
      int var26 = var12 ? var10 : -6249035;
      Msdf.a(var0, MsdfManager.DICONS, "C", var24, var2 + (var4 - Msdf.a(MsdfManager.DICONS, var18)) / 2.0F - 0.2F, var18, var26);
      Msdf.a(var0, var11, var5, var24 + var19 + var22, var2 + (var4 - Msdf.a(var11, var20)) / 2.0F - 0.5F, var20, var25);
      return var2 + var4;
   }

   private static boolean a(double var0, double var2, float var4, float var5, float var6, float var7) {
      return var0 >= var4 && var0 <= var4 + var6 && var2 >= var5 && var2 <= var5 + var7;
   }

   private static int a(int var0, int var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      int var3 = var0 >>> 24 & 0xFF;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >>> 24 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      int var11 = (int)(var4 + (var8 - var4) * var2);
      int var12 = (int)(var5 + (var9 - var5) * var2);
      int var13 = (int)(var6 + (var10 - var6) * var2);
      int var14 = (int)(var3 + (var7 - var3) * var2);
      return var14 << 24 | var11 << 16 | var12 << 8 | var13;
   }

   private static int a(int var0, float var1) {
      int var2 = Math.clamp((int)var1, 0, 255);
      return var2 << 24 | var0 & 16777215;
   }

   private static int a(Color var0, float var1) {
      return var0 == null ? 0 : a(var0.getRGB(), var1);
   }

   private static String a(MsdfFont var0, String var1, float var2, float var3) {
      if (var1 != null && !var1.isEmpty() && !(var3 <= 0.0F)) {
         if (var0 == null) {
            return var1;
         }

         if (Msdf.a(var0, var1, var2) <= var3) {
            return var1;
         }

         for (int var4 = var1.length(); var4 > 0; var4--) {
            String var5 = var1.substring(0, var4) + "…";
            if (Msdf.a(var0, var5, var2) <= var3) {
               return var5;
            }
         }

         return "";
      } else {
         return "";
      }
   }
}
