package brokke.cc.config;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.ui.WatermarkConfigModal;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class HudConfig {
   public static final String KEYBINDS = "keybinds";
   public static final String POTIONS = "potions";
   public static final String COOLDOWNS = "cooldowns";
   public static final String TARGET = "target";
   public static final String SCOREBOARD = "scoreboard";
   public static final String ARRAYLIST = "arraylist";
   private static final float PAD = 4.5F;
   private static final float CHIP_SIZE = 6.25F;
   private static final float SIZE_BTN_W = 12.0F;
   private static final float SIZE_BTN_H = 9.0F;
   private static final float SIZE_GAP = 5.0F;
   private static final float SIZE_LABEL_SIZE = 6.25F;
   private static final float ELEM_GAP = 5.0F;
   private static final float ELEM_LABEL = 6.25F;
   private static final float TOGGLE_H = 11.0F;
   private static final float TOGGLE_GAP = 3.0F;
   private static final float TOGGLE_BOX_W = 16.0F;
   private static final Color TEXT = new Color(240, 240, 245);
   private static final Color TEXT_DIM = new Color(150, 150, 158);
   private static final Map<String, Float> SCALES = new HashMap<>();
   private static final Map<String, Boolean> LAST_RIGHT = new HashMap<>();
   private static final Map<String, Boolean> LAST_MIDDLE = new HashMap<>();
   private static final float[] scaleRange = new float[]{0.5F, 2.0F};
   private static final List<HudConfig.a> activeGridLines = new ArrayList<>();
   private static final Map<String, float[]> TARGET_POS = new ConcurrentHashMap<>();
   private static final Map<String, Long> LAST_SMOOTH_TIME = new ConcurrentHashMap<>();
   private static String openId;
   private static float openX;
   private static float openY;
   private static boolean wasLeft;
   private static boolean draggingScale;
   private static final Animation popupAnim = new Animation();
   private static final Map<String, float[]> SMOOTH_RESULT = new ConcurrentHashMap<>();
   private static final Map<String, List<HudConfig.b>> TOGGLES = new LinkedHashMap<>();
   public static final Map<String, float[]> HUD_BOUNDS = new ConcurrentHashMap<>();
   public static boolean isViewModelDragging = false;

   private static float d(String var0) {
      return Math.clamp((a(var0) - scaleRange[0]) / (scaleRange[1] - scaleRange[0]), 0.0F, 1.0F);
   }

   private static float b(float var0) {
      return scaleRange[0] + Math.clamp(var0, 0.0F, 1.0F) * (scaleRange[1] - scaleRange[0]);
   }

   private static float d() {
      return openY + 4.5F;
   }

   private static float e() {
      return d();
   }

   private static float f() {
      return e() + Msdf.a(6.25F) + 5.0F;
   }

   private HudConfig() {
   }

   public static boolean a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      long var1;
      return var0 != null && var0.getWindow() != null
         ? GLFW.glfwGetKey(var1 = var0.getWindow().getHandle(), 341) == 1 || GLFW.glfwGetKey(var1, 345) == 1
         : false;
   }

   public static float a(float var0) {
      return var0;
   }

   public static float a(float var0, float var1) {
      return var0;
   }

   public static void a(String var0, float var1, float var2) {
      TARGET_POS.put(var0, new float[]{var1, var2});
   }

   public static float[] a(String var0, float var1, float var2, float var3, float var4) {
      float[] var5 = TARGET_POS.computeIfAbsent(var0, var0x -> new float[2]);
      var5[0] = var3;
      var5[1] = var4;
      long var6 = System.nanoTime();
      Long var8 = LAST_SMOOTH_TIME.put(var0, var6);
      float[] var9 = SMOOTH_RESULT.computeIfAbsent(var0, var0x -> new float[2]);
      if (var8 == null) {
         var9[0] = var3;
         var9[1] = var4;
         return var9;
      }

      float var10 = Math.clamp((float)(var6 - var8) / 1.0E9F, 0.001F, 0.05F);
      float var11 = 1.0F - (float)Math.exp(-25.0 * var10);
      float var12 = var1 + (var3 - var1) * var11;
      float var13 = var2 + (var4 - var2) * var11;
      if (Math.abs(var3 - var12) < 0.05F) {
         var12 = var3;
      }

      if (Math.abs(var4 - var13) < 0.05F) {
         var13 = var4;
      }

      var9[0] = var12;
      var9[1] = var13;
      return var9;
   }

   public static void b() {
      activeGridLines.clear();
      ConfigManager.onSettingChanged();
   }

   public static float[] b(String var0, float var1, float var2, float var3, float var4) {
      if (a()) {
         activeGridLines.clear();
         return new float[]{var1, var2};
      }

      float var5 = var1;
      float var6 = var2;
      float var7 = RenderUtil.a();
      float var8 = RenderUtil.b();
      ArrayList var9 = new ArrayList();
      float var10 = 5.0F;
      var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.EDGE, var10));
      var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.EDGE, Math.max(var10, var8 - var10)));
      var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.EDGE, var10));
      var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.EDGE, Math.max(var10, var7 - var10)));
      var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.CENTER, var7 * 0.5F));
      var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.CENTER, var8 * 0.5F));
      var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.CENTER, var7 * 0.25F));
      var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.CENTER, var7 * 0.75F));
      var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.CENTER, var8 * 0.25F));
      var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.CENTER, var8 * 0.75F));

      for (Entry var12 : HUD_BOUNDS.entrySet()) {
         if (var0 == null || !var0.equals(var12.getKey())) {
            float[] var13 = (float[])var12.getValue();
            if (var13 != null && var13.length >= 4) {
               float var14 = var13[0];
               float var15 = var13[1];
               float var16 = var13[2];
               float var17 = var13[3];
               if (!(var16 <= 0.0F) && !(var17 <= 0.0F)) {
                  var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.EDGE, var15));
                  var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.EDGE, var15 + var17));
                  var9.add(new HudConfig.a(HudConfig.a.b.HORIZONTAL, HudConfig.a.Align.CENTER, var15 + var17 * 0.5F));
                  var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.EDGE, var14));
                  var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.EDGE, var14 + var16));
                  var9.add(new HudConfig.a(HudConfig.a.b.VERTICAL, HudConfig.a.Align.CENTER, var14 + var16 * 0.5F));
               }
            }
         }
      }

      float var28 = 10.0F;
      float var29 = 11.0F;
      float var30 = var28 + 1.0F;
      HudConfig.a var31 = null;
      HudConfig.a var32 = null;
      float var33 = var5;
      float var34 = var6;

      for (HudConfig.a var19 : (Iterable<HudConfig.a>)(Iterable<?>)(var9)) {
         if (var19.getType() == HudConfig.a.b.VERTICAL) {
            float var20;
            float var21;
            if (var19.getAlign() == HudConfig.a.Align.CENTER) {
               var20 = var19.getPos() - var3 * 0.5F;
               var21 = Math.abs(var5 - var20);
            } else {
               float var26;
               float var22 = var26 = var19.getPos();
               float var23 = var26 - var3;
               float var24 = Math.abs(var5 - var22);
               float var25 = Math.abs(var5 - var23);
               if (var24 <= var25) {
                  var20 = var22;
                  var21 = var24;
               } else {
                  var20 = var23;
                  var21 = var25;
               }
            }

            if (var21 < var29 && var21 <= var28) {
               var29 = var21;
               var31 = var19;
               var33 = var20;
            }
         } else {
            float var35;
            float var36;
            if (var19.getAlign() == HudConfig.a.Align.CENTER) {
               var35 = var19.getPos() - var4 * 0.5F;
               var36 = Math.abs(var6 - var35);
            } else {
               float var27;
               float var37 = var27 = var19.getPos();
               float var38 = var27 - var4;
               float var39 = Math.abs(var6 - var37);
               float var40 = Math.abs(var6 - var38);
               if (var39 <= var40) {
                  var35 = var37;
                  var36 = var39;
               } else {
                  var35 = var38;
                  var36 = var40;
               }
            }

            if (var36 < var30 && var36 <= var28) {
               var30 = var36;
               var32 = var19;
               var34 = var35;
            }
         }
      }

      activeGridLines.clear();
      if (var31 != null) {
         var31.setActive(true);
         activeGridLines.add(var31);
         var5 = var33;
      }

      if (var32 != null) {
         var32.setActive(true);
         activeGridLines.add(var32);
         var6 = var34;
      }

      return new float[]{var5, var6};
   }

   public static void a(DrawContext var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (!(var1.currentScreen instanceof ChatScreen)) {
         activeGridLines.clear();
      } else {
         int var2 = RenderUtil.a();
         int var3 = RenderUtil.b();
         Matrix4f var4 = RenderUtil.d();
         if (!activeGridLines.isEmpty()) {
            BufferBuilder var5 = ColorPipeline.a();
            if (var5 != null) {
               byte var6 = -1;

               for (HudConfig.a var8 : activeGridLines) {
                  if (var8 != null && var8.isActive()) {
                     if (var8.getType() == HudConfig.a.b.VERTICAL) {
                        float var15;
                        ColorPipeline.a(var5, var15 = var8.getPos(), 0.0F, var15, var3, 1.0F, var6);
                     } else {
                        float var16;
                        ColorPipeline.a(var5, 0.0F, var16 = var8.getPos(), var2, var16, 1.0F, var6);
                     }
                  }
               }

               ColorPipeline.a(var5, false);
            }
         }

         boolean var17;
         String var18 = (var17 = a()) ? "Свободное перемещение [Ctrl активен]" : "Сетка выравнивания [Зажмите Ctrl для свободного перемещения]";
         float var19 = 6.25F;
         float var20;
         float var9 = (var20 = Msdf.a(MsdfManager.BOLD, var18, var19)) + 12.0F;
         float var10 = 12.0F;
         float var11 = (var2 - var9) / 2.0F;
         float var12 = 6.0F;
         RenderUtil.a(var4, var11, var12, var9, var10, 3.5F, 3.5F, 3.5F, 3.5F, false, new Color(0, 0, 0, 150));
         RenderUtil.a(var4, var11, var12, var9, var10, 3.5F, 0.5F, var17 ? Theme.getInstance().getAccent() : new Color(255, 255, 255, 35));
         float var13 = var11 + (var9 - var20) / 2.0F;
         float var14 = var12 + (var10 - Msdf.a(MsdfManager.BOLD, var19)) / 2.0F - 0.5F;
         Msdf.a(var4, MsdfManager.BOLD, var18, var13, var14, var19, b(var17 ? Theme.getInstance().getAccent() : new Color(220, 225, 230), 220.0F));
      }
   }

   public static float a(String var0) {
      return SCALES.getOrDefault(var0, 1.0F);
   }

   public static void a(String var0, float var1) {
      SCALES.put(var0, Math.clamp(var1, 0.5F, 2.0F));
      ConfigManager.onSettingChanged();
   }

   public static void b(String var0, float var1) {
      SCALES.put(var0, Math.clamp(var1, 0.5F, 2.0F));
   }

   public static Map<String, Float> getScales() {
      return SCALES;
   }

   public static boolean b(String var0) {
      return true;
   }

   public static void a(String var0, List<HudConfig.b> var1) {
      TOGGLES.put(var0, var1);
   }

   private static boolean e(String var0) {
      List var1 = TOGGLES.get(var0);
      return var1 != null && !var1.isEmpty();
   }

   private static float g() {
      return openY + 4.5F + 9.5F + 3.25F + 5.0F;
   }

   private static List<float[]> h() {
      ArrayList var0 = new ArrayList();
      if (!e(openId)) {
         return var0;
      }

      List var1 = TOGGLES.get(openId);
      float var2 = g() + Msdf.a(6.25F) + 3.0F;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         float var4 = var2 + var3 * 14.0F;
         var0.add(new float[]{openX + 4.5F, var4, i() - 9.0F, 11.0F, var3});
      }

      return var0;
   }

   public static void a(String var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      if (var6 != null) {
         a(RenderUtil.c(), var0, var1, var2, var3, var4, var5, var6.getRGB());
      }
   }

   public static void a(String var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      a(RenderUtil.c(), var0, var1, var2, var3, var4, var5, var6);
   }

   public static void a(Matrix4f var0, String var1, float var2, float var3, float var4, float var5, float var6, Color var7) {
      if (var7 != null) {
         a(var0, var1, var2, var3, var4, var5, var6, var7.getRGB());
      }
   }

   public static void a(Matrix4f var0, String var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      int var8;
      if ((var8 = var7 >> 24 & 0xFF) > 0) {
         int var9 = var7 & 16777215;
         int var10;
         if (InterfaceModule.l() && (var10 = Math.round(140.0F * (var8 / 255.0F))) > 0) {
            RenderUtil.b(var0, var2, var3, var4, var5, var6, 4.5F, 0.4F, 2.5F, var10 << 24);
         }

         Theme.a var15 = Theme.getInstance().getGuiStyle();
         int var11 = !"panel".equals(var1)
               && !"search".equals(var1)
               && !"theme".equals(var1)
               && !"events_btn".equals(var1)
               && !"events_modal".equals(var1)
               && !"cosmetics_btn".equals(var1)
               && !"cosmetics_modal".equals(var1)
               && !"autobuy_modal".equals(var1)
               && !"autosbor_modal".equals(var1)
               && !"autosbor_tooltip".equals(var1)
               && !"autosbor_configs".equals(var1)
            ? 0
            : 1;
         int var12;
         if ((var12 = var11 != 0 && var15 != Theme.a.BLUR ? 0 : 1) != 0 && var8 > 10) {
            RenderUtil.a(var0, var2, var3, var4, var5, var6, 10.0F);
         }

         int var13 = var12 != 0 ? Math.min(var8, 120) : Math.min(var8, 245);
         RenderUtil.a(var0, var2, var3, var4, var5, var6, var13 << 24 | var9);
         int var14;
         if ((var14 = Math.round(25.0F * (var8 / 255.0F))) > 0) {
            RenderUtil.a(var0, var2, var3, var4, var5, var6, 0.75F, var14 << 24 | 16777215);
         }
      }
   }

   public static void b(String var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      if (var6 != null) {
         RenderUtil.a(var1, var2, var3, var4, var5, var6.getRGB());
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      int var7;
      if (var6 != null && (var7 = var6.getAlpha()) > 0) {
         int var8 = var6.getRGB() & 16777215;
         int var9;
         if (InterfaceModule.l() && (var9 = Math.round(140.0F * (var7 / 255.0F))) > 0) {
            RenderUtil.b(var0, var1, var2, var3, var4, var5, 4.5F, 0.4F, 2.5F, var9 << 24);
         }

         byte var12 = 1;
         if (var7 > 10) {
            RenderUtil.a(var0, var1, var2, var3, var4, var5, 10.0F);
         }

         int var10 = var12 != 0 ? Math.min(var7, 120) : Math.min(var7, 245);
         RenderUtil.a(var0, var1, var2, var3, var4, var5, var10 << 24 | var8);
         int var11;
         if ((var11 = Math.round(20.0F * (var7 / 255.0F))) > 0) {
            RenderUtil.a(var0, var1, var2, var3, var4, var5, 0.5F, var11 << 24 | 16777215);
         }
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      int var7;
      if ((var7 = var6 >> 24 & 0xFF) > 0) {
         int var8 = var6 & 16777215;
         int var9;
         if (InterfaceModule.l() && (var9 = Math.round(140.0F * (var7 / 255.0F))) > 0) {
            RenderUtil.b(var0, var1, var2, var3, var4, var5, 4.5F, 0.4F, 2.5F, var9 << 24);
         }

         byte var12 = 1;
         if (var7 > 10) {
            RenderUtil.a(var0, var1, var2, var3, var4, var5, 10.0F);
         }

         int var10 = var12 != 0 ? Math.min(var7, 120) : Math.min(var7, 245);
         RenderUtil.a(var0, var1, var2, var3, var4, var5, var10 << 24 | var8);
         int var11;
         if ((var11 = Math.round(20.0F * (var7 / 255.0F))) > 0) {
            RenderUtil.a(var0, var1, var2, var3, var4, var5, 0.5F, var11 << 24 | 16777215);
         }
      }
   }

   public static boolean b(float var0, float var1) {
      if (isViewModelDragging) {
         return true;
      } else if (WatermarkConfigModal.isOpen() && WatermarkConfigModal.b(var0, var1)) {
         return true;
      } else {
         return openId == null ? false : var0 >= openX && var0 <= openX + i() && var1 >= openY && var1 <= openY + j();
      }
   }

   public static boolean c(float var0, float var1) {
      if (openId != null && b(var0, var1)) {
         return true;
      }

      for (float[] var3 : HUD_BOUNDS.values()) {
         if (var3 != null
            && var3.length >= 4
            && var0 >= var3[0] - 3.0F
            && var0 <= var3[0] + var3[2] + 3.0F
            && var1 >= var3[1] - 3.0F
            && var1 <= var3[1] + var3[3] + 3.0F) {
            return true;
         }
      }

      return false;
   }

   public static void c(String var0, float var1, float var2, float var3, float var4) {
      HUD_BOUNDS.put(var0, new float[]{var1, var2, var3, var4});
      MinecraftClient var5 = MinecraftClient.getInstance();
      if (!(var5.currentScreen instanceof ChatScreen)) {
         if (var0.equals(openId)) {
            openId = null;
         }

         LAST_RIGHT.put(var0, false);
         LAST_MIDDLE.put(var0, false);
         HUD_BOUNDS.remove(var0);
      } else {
         float var6 = a(var5);
         float var7 = b(var5);
         long var8;
         int var10;
         int var11 = (var10 = GLFW.glfwGetMouseButton(var8 = var5.getWindow().getHandle(), 1) == 1 ? 1 : 0) != 0 && !Boolean.TRUE.equals(LAST_RIGHT.get(var0))
            ? 1
            : 0;
         LAST_RIGHT.put(var0, var10 != 0);
         int var12;
         int var13 = (var12 = GLFW.glfwGetMouseButton(var8, 2) == 1 ? 1 : 0) != 0 && !Boolean.TRUE.equals(LAST_MIDDLE.get(var0)) ? 1 : 0;
         LAST_MIDDLE.put(var0, var12 != 0);
         int var14 = var6 >= var1 && var6 <= var1 + var3 && var7 >= var2 && var7 <= var2 + var4 ? 1 : 0;
         if (var11 != 0 && var14 != 0) {
            if ("watermark".equals(var0)) {
               return;
            }

            openId = var0.equals(openId) ? null : var0;
            draggingScale = false;
            wasLeft = GLFW.glfwGetMouseButton(var8, 0) == 1;
         }

         if (var0.equals(openId)) {
            if (!draggingScale) {
               openX = var1 + var3 + 3.0F;
               openY = var2;
               k();
            }

            int var15;
            int var16 = (var15 = GLFW.glfwGetMouseButton(var8, 0) == 1 ? 1 : 0) != 0 && !wasLeft ? 1 : 0;
            wasLeft = var15 != 0;
            float var17 = openX + 4.5F;
            float var18 = i() - 9.0F;
            float var19 = f();
            int var20 = var6 >= var17 - 5.0F && var6 <= var17 + var18 + 5.0F && var7 >= e() - 2.0F && var7 <= var19 + 8.0F ? 1 : 0;
            if (var13 != 0 && var20 != 0) {
               a(var0, 1.0F);
               return;
            }

            if (draggingScale) {
               if (var15 != 0) {
                  SCALES.put(var0, b((var6 - var17) / var18));
                  return;
               }

               draggingScale = false;
               ConfigManager.onSettingChanged();
               return;
            }

            if (var16 != 0 && var20 != 0) {
               draggingScale = true;
               SCALES.put(var0, b((var6 - var17) / var18));
               return;
            }

            if (var16 != 0) {
               for (float[] var22 : h()) {
                  if (var6 >= var22[0] && var6 <= var22[0] + var22[2] && var7 >= var22[1] && var7 <= var22[1] + var22[3]) {
                     HudConfig.b var23 = TOGGLES.get(var0).get((int)var22[4]);
                     var23.a(!var23.b());
                     ConfigManager.onSettingChanged();
                     return;
                  }
               }

               if (!b(var6, var7) && var14 == 0) {
                  openId = null;
               }
            }
         }
      }
   }

   public static void c(String var0) {
      if (!"watermark".equals(var0) && var0.equals(openId)) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         if (!(var1.currentScreen instanceof ChatScreen)) {
            openId = null;
         } else {
            popupAnim.setEasing(Easing.SPRING_SOFT);
            popupAnim.animate(1.0F, 0.0F, 6.0F);
            float var2 = Math.clamp(popupAnim.getValue(), 0.0F, 1.0F);
            float var3 = (1.0F - var2) * 4.0F;
            float var4 = i();
            float var5 = j();
            Color var6 = Theme.getInstance().getAccent();
            Matrix4f var7 = RenderUtil.d();
            float var8 = 255.0F * var2;
            a(var7, "hud_popup", openX, openY + var3, var4, var5, 7.0F, Math.round(180.0F * var2) << 24);
            float var9 = openY + var3 + 4.5F;
            String var10 = Math.round(a(var0) * 100.0F) + "%";
            float var11 = Msdf.a(MsdfManager.BOLD, var10, 6.75F);
            Msdf.a(var7, MsdfManager.BOLD, "Размер", openX + 6.0F, var9, 6.75F, b(new Color(200, 200, 210), var8));
            Msdf.a(var7, MsdfManager.BOLD, var10, openX + var4 - 6.0F - var11, var9, 6.75F, b(Color.WHITE, var8));
            float var12 = openX + 6.0F;
            float var13 = var4 - 12.0F;
            float var14 = var9 + 9.5F;
            float var15 = 3.25F;
            float var16 = d(var0);
            RenderUtil.a(var7, var12, var14, var13, var15, 1.6F, 1.6F, 1.6F, 1.6F, false, Math.round(200.0F * var2) << 24 | 3604480 | 14080 | 65);
            RenderUtil.a(
               var7,
               var12,
               var14,
               var13 * var16,
               var15,
               1.6F,
               1.6F,
               1.6F,
               1.6F,
               false,
               (int)Math.clamp(255.0F * var2, 0.0F, 255.0F) << 24 | var6.getRGB() & 16777215
            );
            float var17 = 6.5F;
            float var18 = var12 + var13 * var16 - var17 / 2.0F;
            float var19 = var14 + var15 / 2.0F - var17 / 2.0F;
            RenderUtil.b(var7, var18, var19, var17, var17, var17 / 2.0F, 2.5F, 0.4F, 2.0F, Math.round(160.0F * var2) << 24);
            RenderUtil.a(
               var7, var18, var19, var17, var17, var17 / 2.0F, var17 / 2.0F, var17 / 2.0F, var17 / 2.0F, false, Math.round(255.0F * var2) << 24 | 16777215
            );
            if (e(var0)) {
               float var20 = openY + var3 + 4.5F + 9.5F + 3.25F + 5.0F;
               Msdf.a(var7, MsdfManager.BOLD, "Элементы", openX + 6.0F, var20, 6.25F, b(new Color(200, 200, 210), var8));
               List var21 = TOGGLES.get(var0);
               float var22 = var20 + Msdf.a(MsdfManager.BOLD, 6.25F) + 3.0F;

               for (int var23 = 0; var23 < var21.size(); var23++) {
                  HudConfig.b var24 = (HudConfig.b)var21.get(var23);
                  boolean var25 = var24.b();
                  float var26 = var22 + var23 * 14.0F;
                  Msdf.a(
                     var7, MsdfManager.BOLD, var24.a(), openX + 6.0F, var26 + (11.0F - Msdf.a(MsdfManager.BOLD, 6.25F)) / 2.0F - 0.5F, 6.25F, b(TEXT_DIM, var8)
                  );
                  float var27 = openX + var4 - 6.0F - 11.0F;
                  float var28 = var26 + 0.0F;
                  if (var25) {
                     RenderUtil.a(var7, var27, var28, 11.0F, 11.0F, 3.0F, 3.0F, 3.0F, 3.0F, false, a(var6, 180.0F * var2));
                     RenderUtil.a(var7, var27, var28, 11.0F, 11.0F, 3.0F, 0.5F, a(var6, 220.0F * var2));
                     float var29 = Msdf.a(MsdfManager.DICONS, "m", 7.5F);
                     float var30 = Msdf.a(MsdfManager.DICONS, 7.5F);
                     Msdf.a(var7, MsdfManager.DICONS, "m", var27 + (11.0F - var29) / 2.0F, var28 + (11.0F - var30) / 2.0F, 7.5F, b(Color.WHITE, 255.0F * var2));
                  } else {
                     RenderUtil.a(var7, var27, var28, 11.0F, 11.0F, 3.0F, 3.0F, 3.0F, 3.0F, false, a(new Color(30, 32, 40), 180.0F * var2));
                     RenderUtil.a(var7, var27, var28, 11.0F, 11.0F, 3.0F, 0.5F, a(new Color(65, 72, 85), 180.0F * var2));
                     float var31 = Msdf.a(MsdfManager.DICONS, "u", 7.0F);
                     float var32 = Msdf.a(MsdfManager.DICONS, 7.0F);
                     Msdf.a(
                        var7,
                        MsdfManager.DICONS,
                        "u",
                        var27 + (11.0F - var31) / 2.0F,
                        var28 + (11.0F - var32) / 2.0F,
                        7.0F,
                        b(new Color(175, 175, 185), 180.0F * var2)
                     );
                  }
               }
            }
         }
      }
   }

   private static float i() {
      float var0 = 82.0F;
      if (e(openId)) {
         float var1 = 0.0F;

         for (HudConfig.b var3 : TOGGLES.get(openId)) {
            var1 = Math.max(var1, Msdf.a(MsdfManager.BOLD, var3.a(), 6.25F));
         }

         var0 = Math.max(var0, 12.0F + var1 + 16.0F + 16.0F);
      }

      return var0;
   }

   private static float j() {
      if (!e(openId)) {
         return 22.0F;
      }

      int var0;
      float var1 = (var0 = TOGGLES.get(openId).size()) * 11.0F + Math.max(0, var0 - 1) * 3.0F;
      return 22.25F + Msdf.a(MsdfManager.BOLD, 6.25F) + 3.0F + var1 + 4.5F;
   }

   private static void k() {
      float var0 = RenderUtil.a() - i() - 2.0F;
      float var1 = RenderUtil.b() - j() - 2.0F;
      openX = Math.clamp(openX, 2.0F, Math.max(2.0F, var0));
      openY = Math.clamp(openY, 2.0F, Math.max(2.0F, var1));
   }

   private static float a(MinecraftClient var0) {
      double var1 = var0.getWindow().getScaleFactor();
      return (float)(var0.mouse.getX() / var1) * RenderUtil.e();
   }

   private static float b(MinecraftClient var0) {
      double var1 = var0.getWindow().getScaleFactor();
      return (float)(var0.mouse.getY() / var1) * RenderUtil.e();
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
   }

   private static int b(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRGB() & 16777215;
   }

   public static final class a {
      private final HudConfig.a.b type;
      private final HudConfig.a.Align align;
      private final float pos;
      private boolean active;

      public a(HudConfig.a.b var1, HudConfig.a.Align var2, float var3) {
         this.type = var1;
         this.align = var2;
         this.pos = var3;
      }

      public HudConfig.a.b getType() {
         return this.type;
      }

      public HudConfig.a.Align getAlign() {
         return this.align;
      }

      public float getPos() {
         return this.pos;
      }

      public boolean isActive() {
         return this.active;
      }

      public void setActive(boolean var1) {
         this.active = var1;
      }

      public enum Align {
         EDGE,
         CENTER;
      }

      public enum b {
         HORIZONTAL,
         VERTICAL;
      }
   }

   public interface b {
      String a();

      boolean b();

      void a(boolean var1);
   }
}
