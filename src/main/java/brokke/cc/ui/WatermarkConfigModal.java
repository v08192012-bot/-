package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.hud.WatermarkHud;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventScroll;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class WatermarkConfigModal {
   private static final Identifier LOGO_TEX = Identifier.of("brokke", "textures/watermark_icon.png");
   private static final List<WatermarkConfigModal.a> ITEMS = new ArrayList<>();
   private static boolean open = false;
   private static float x = 60.0F;
   private static float y = 60.0F;
   private static final float W = 118.0F;
   private static final float HEADER_H = 19.0F;
   private static final float ITEM_H = 13.5F;
   private static boolean draggingWindow = false;
   private static float dragOffsetX = 0.0F;
   private static float dragOffsetY = 0.0F;
   private static final Animation openAnim = new Animation();
   private static float scrollOffset = 0.0F;
   private static float targetScrollOffset = 0.0F;
   private static boolean wasLmb = false;
   private static boolean wasRmb = false;
   private static final WatermarkConfigModal INSTANCE = new WatermarkConfigModal();

   @EventHandler
   public void onScroll(EventScroll var1) {
      if (open) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.getWindow() != null) {
            double var3 = var2.getWindow().getScaleFactor();
            float var5 = (float)(var2.mouse.getX() / var3) * RenderUtil.e();
            float var6 = (float)(var2.mouse.getY() / var3) * RenderUtil.e();
            if (a(var5, var6, var1.getVertical())) {
               var1.cancel();
            }
         }
      }
   }

   public static WatermarkConfigModal getInstance() {
      return INSTANCE;
   }

   public static boolean isOpen() {
      return open;
   }

   public static void a(boolean var0) {
      open = var0;
      if (open) {
         f();
      }
   }

   public static void c() {
      a(!open);
   }

   public static void a(float var0, float var1) {
      x = var0;
      y = var1;
      open = true;
      f();
   }

   public static List<WatermarkConfigModal.a> getItems() {
      return ITEMS;
   }

   private static float e() {
      float var0 = RenderUtil.b();
      float var1 = 23.0F + ITEMS.size() * 13.5F + 3.0F;
      return Math.min(var1, var0 - 20.0F);
   }

   private static void f() {
      float var0 = RenderUtil.a();
      float var1 = RenderUtil.b();
      float var2 = e();
      if (x + 118.0F > var0 - 6.0F) {
         x = Math.max(6.0F, var0 - 118.0F - 6.0F);
      }

      if (x < 6.0F) {
         x = 6.0F;
      }

      if (y < 6.0F) {
         y = 6.0F;
      }

      if (y + var2 > var1 - 6.0F) {
         y = Math.max(6.0F, var1 - var2 - 6.0F);
      }
   }

   public static boolean b(float var0, float var1) {
      if (!open) {
         return false;
      }

      float var2 = e();
      return var0 >= x && var0 <= x + 118.0F && var1 >= y && var1 <= y + var2;
   }

   public static boolean a(double var0, double var2, int var4) {
      if (!open) {
         return false;
      }

      float var5 = (float)var0;
      float var6 = (float)var2;
      float var7 = e();
      if (var4 == 0) {
         float var8 = x + 118.0F - 10.0F;
         float var9 = y + 9.5F;
         if (var5 >= var8 - 5.0F && var5 <= var8 + 7.0F && var6 >= var9 - 5.0F && var6 <= var9 + 7.0F) {
            open = false;
            return true;
         }

         if (var5 >= x && var5 <= x + 118.0F && var6 >= y && var6 <= y + 19.0F) {
            draggingWindow = true;
            dragOffsetX = var5 - x;
            dragOffsetY = var6 - y;
            return true;
         }

         float var10 = y + 19.0F + 2.0F;
         float var11 = var7 - 19.0F - 4.0F;
         if (var5 >= x && var5 <= x + 118.0F && var6 >= var10 && var6 <= var10 + var11) {
            float var12 = var6 - var10 + scrollOffset;
            int var13 = (int)(var12 / 13.5F);
            if (var13 >= 0 && var13 < ITEMS.size()) {
               WatermarkConfigModal.a var14 = ITEMS.get(var13);
               WatermarkHud.b(var14.getId());
               ConfigManager.onSettingChanged();
               return true;
            }
         }

         if (!b(var5, var6)) {
            open = false;
            return true;
         }
      }

      return b(var5, var6);
   }

   public static boolean b(double var0, double var2, int var4) {
      if (var4 == 0 && draggingWindow) {
         draggingWindow = false;
         return true;
      } else {
         return false;
      }
   }

   public static boolean a(double var0, double var2, int var4, double var5, double var7) {
      if (draggingWindow && var4 == 0) {
         x = (float)var0 - dragOffsetX;
         y = (float)var2 - dragOffsetY;
         f();
         return true;
      } else {
         return false;
      }
   }

   public static boolean a(double var0, double var2, double var4) {
      if (!open) {
         return false;
      } else {
         float var6 = (float)var0;
         float var7 = (float)var2;
         if (b(var6, var7)) {
            float var8 = e() - 19.0F - 4.0F;
            float var9 = ITEMS.size() * 13.5F;
            float var10 = Math.max(0.0F, var9 - var8);
            targetScrollOffset = Math.clamp(targetScrollOffset - (float)var4 * 18.0F, 0.0F, var10);
            return true;
         } else {
            return false;
         }
      }
   }

   public static void a(float var0, float var1, float var2) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3 != null && var3.currentScreen == null) {
         open = false;
      }

      if (var3 != null && var3.getWindow() != null && var3.currentScreen instanceof ChatScreen) {
         long var4 = var3.getWindow().getHandle();
         boolean var6 = GLFW.glfwGetMouseButton(var4, 0) == 1;
         boolean var7 = GLFW.glfwGetMouseButton(var4, 1) == 1;
         boolean var8 = var6 && !wasLmb;
         boolean var9 = var7 && !wasRmb;
         boolean var10 = !var6 && wasLmb;
         wasLmb = var6;
         wasRmb = var7;
         if (var8) {
            a((double)var0, (double)var1, 0);
         } else if (var9) {
            a((double)var0, (double)var1, 1);
         } else if (var10) {
            b(var0, var1, 0);
         }

         if (draggingWindow && var6) {
            x = var0 - dragOffsetX;
            y = var1 - dragOffsetY;
            f();
         }
      }

      if (open || !(openAnim.getValue() <= 0.01F)) {
         openAnim.setEasing(Easing.SPRING_SOFT);
         openAnim.animate(open ? 1.0F : 0.0F, var2, 5.5F);
         float var30 = Math.clamp(openAnim.getValue(), 0.0F, 1.0F);
         if (!(var30 <= 0.01F)) {
            float var5 = e();
            float var31 = y + 19.0F + 2.0F;
            float var32 = var5 - 19.0F - 4.0F;
            float var33 = ITEMS.size() * 13.5F;
            float var34 = Math.max(0.0F, var33 - var32);
            targetScrollOffset = Math.clamp(targetScrollOffset, 0.0F, var34);
            scrollOffset = scrollOffset + (targetScrollOffset - scrollOffset) * Math.min(1.0F, var2 * 14.0F);
            Matrix4f var35 = RenderUtil.d();
            Color var11 = Theme.getInstance().getAccent();
            float var12 = (1.0F - var30) * 4.0F;
            float var13 = y + var12;
            HudConfig.a(var35, "watermark_settings", x, var13, 118.0F, var5, 7.0F, (int)(180.0F * var30) << 24);
            Msdf.a(var35, MsdfManager.BOLD, "Элементы", x + 8.0F, var13 + 5.5F, 6.75F, a(Color.WHITE, 255.0F * var30));
            float var14 = x + 118.0F - 10.0F;
            float var15 = var13 + 9.5F;
            boolean var16 = var0 >= var14 - 4.0F && var0 <= var14 + 8.0F && var1 >= var15 - 4.0F && var1 <= var15 + 8.0F;
            BufferBuilder var17 = ColorPipeline.a();
            if (var17 != null) {
               int var18 = var16 ? a(new Color(255, 95, 110), 255.0F * var30) : a(new Color(150, 155, 165), 200.0F * var30);
               ColorPipeline.a(var17, var14 - 2.5F, var15 - 2.5F, var14 + 2.5F, var15 + 2.5F, 1.2F, var18);
               ColorPipeline.a(var17, var14 + 2.5F, var15 - 2.5F, var14 - 2.5F, var15 + 2.5F, 1.2F, var18);
               ColorPipeline.a(var17, x + 6.0F, var13 + 19.0F, x + 118.0F - 6.0F, var13 + 19.0F, 0.6F, (int)(25.0F * var30) << 24 | 16777215);
               ColorPipeline.a(var17, false);
            }

            boolean var36 = var33 > var32;
            if (var36) {
               Scissor.b(x, var31, 118.0, var32);
            }

            for (int var19 = 0; var19 < ITEMS.size(); var19++) {
               WatermarkConfigModal.a var20 = ITEMS.get(var19);
               float var21 = var31 + var19 * 13.5F - scrollOffset;
               if (!(var21 + 13.5F < var31) && !(var21 > var31 + var32)) {
                  boolean var22 = WatermarkHud.a(var20.getId());
                  boolean var23 = var0 >= x + 2.0F && var0 <= x + 118.0F - 2.0F && var1 >= var21 && var1 <= var21 + 13.5F;
                  if (var23) {
                     RenderUtil.a(var35, x + 3.0F, var21 + 0.5F, 112.0F, 12.5F, 3.0F, 3.0F, 3.0F, 3.0F, false, (int)(25.0F * var30) << 24 | 16777215);
                  }

                  if (var22) {
                     Msdf.a(var35, MsdfManager.DICONS, "m", x + 5.5F, var21 + 3.75F, 6.5F, a(var11, 255.0F * var30));
                  }

                  float var24 = x + 15.5F;
                  float var25 = var21 + 3.75F;
                  Color var26 = var22 ? var11 : new Color(135, 140, 150);
                  if (var20.getFontType() == WatermarkConfigModal.b.TEXTURE && var20.getTexture() != null) {
                     RenderUtil.a(var35, var24, var25 - 0.5F, 7.5F, var20.getTexture(), 0.0F, (int)(255.0F * var30) << 24 | var26.getRGB() & 16777215);
                  } else if (var20.getFontType() == WatermarkConfigModal.b.ICONS2) {
                     Msdf.a(var35, MsdfManager.ICONS2, var20.getIcon(), var24, var25, 6.5F, a(var26, 255.0F * var30));
                  } else if (var20.getFontType() == WatermarkConfigModal.b.ICONS) {
                     Msdf.a(var35, MsdfManager.ICONS, var20.getIcon(), var24, var25, 6.5F, a(var26, 255.0F * var30));
                  } else {
                     Msdf.a(var35, MsdfManager.DICONS, var20.getIcon(), var24, var25, 6.5F, a(var26, 255.0F * var30));
                  }

                  float var27 = x + 26.0F;
                  float var28 = var21 + 3.75F;
                  Color var29 = var22 ? Color.WHITE : new Color(155, 160, 170);
                  Msdf.a(var35, MsdfManager.BOLD, var20.getLabel(), var27, var28, 5.75F, a(var29, 255.0F * var30));
               }
            }

            if (var36) {
               Scissor.c();
            }

            if (var34 > 0.0F) {
               float var37 = var32 - 6.0F;
               float var38 = Math.max(12.0F, var37 * (var32 / var33));
               float var39 = var31 + 3.0F + scrollOffset / var34 * (var37 - var38);
               RenderUtil.a(var35, x + 118.0F - 3.5F, var39, 1.75F, var38, 0.85F, 0.85F, 0.85F, 0.85F, false, (int)(70.0F * var30) << 24 | 16777215);
            }
         }
      }
   }

   private static int a(Color var0, float var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0.getRGB() & 16777215;
   }

   static {
      ITEMS.add(new WatermarkConfigModal.a("logo", "Логотип", "", WatermarkConfigModal.b.TEXTURE, LOGO_TEX));
      ITEMS.add(new WatermarkConfigModal.a("avatar", "Аватар", "e", WatermarkConfigModal.b.ICONS2, null));
      ITEMS.add(new WatermarkConfigModal.a("user", "Никнейм", "e", WatermarkConfigModal.b.ICONS2, null));
      ITEMS.add(new WatermarkConfigModal.a("fps", "FPS", "f", WatermarkConfigModal.b.ICONS2, null));
      ITEMS.add(new WatermarkConfigModal.a("time", "Время", "x", WatermarkConfigModal.b.ICONS2, null));
      ITEMS.add(new WatermarkConfigModal.a("ping", "Пинг", "p", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("jitter", "Джиттер", "w", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("ip", "IP", "@", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("cpu", "CPU", "c", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("ram", "RAM", "r", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("gpu", "GPU", "g", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("kd", "Соотношение K/D", "%", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("cpu_freq", "Частота CPU", "^", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("cpu_cores", "Ядра CPU", "#", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("var", "VAR", "v", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("loss", "LOSS", "!", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("net_problem", "Проблемы сети", "?", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("preset", "Пресет", "*", WatermarkConfigModal.b.DICONS, null));
      ITEMS.add(new WatermarkConfigModal.a("pvp_timer", "Таймер PvP", "s", WatermarkConfigModal.b.DICONS, null));
      EventBus.register(INSTANCE);
   }

   public static final class a {
      private final String id;
      private final String label;
      private final String icon;
      private final WatermarkConfigModal.b fontType;
      private final Identifier texture;

      public a(String var1, String var2, String var3, WatermarkConfigModal.b var4, Identifier var5) {
         this.id = var1;
         this.label = var2;
         this.icon = var3;
         this.fontType = var4;
         this.texture = var5;
      }

      public String getId() {
         return this.id;
      }

      public String getLabel() {
         return this.label;
      }

      public String getIcon() {
         return this.icon;
      }

      public WatermarkConfigModal.b getFontType() {
         return this.fontType;
      }

      public Identifier getTexture() {
         return this.texture;
      }
   }

   public enum b {
      DICONS,
      ICONS2,
      ICONS,
      TEXTURE;
   }
}
