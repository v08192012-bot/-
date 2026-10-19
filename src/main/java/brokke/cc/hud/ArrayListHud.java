package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.Setting;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.util.KeyHelper;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class ArrayListHud {
   private static final ArrayListHud INSTANCE = new ArrayListHud();
   private static boolean enabled = true;
   private static final float ROW_H = 11.0F;
   private static final float ROW_GAP = 1.0F;
   private static final float PAD_X = 5.0F;
   private static final float FONT_SIZE = 6.5F;
   private static final float ICON_SIZE = 6.5F;
   private static final float ICON_GAP = 3.5F;
   private static final float RADIUS = 3.5F;
   private final Animation entryAnim = new Animation();
   private final Map<Function, Animation> animations = new LinkedHashMap<>();
   private final List<Function> activeModules = new ArrayList<>();
   private float x = 7.0F;
   private float y = 50.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private boolean rightAnchored;
   private float anchorEdge;
   private boolean anchorReady;
   private static float cachedNameH = -1.0F;
   private static float cachedIconH = -1.0F;
   static boolean silverShimmer = true;
   static boolean hideRender = true;
   static boolean themeColor = false;
   static boolean sortByLength = true;
   static boolean showSuffix = false;
   static boolean showBinds = false;
   static boolean showCategoryIcons = true;

   public static float[] a() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.initPos = true;
      INSTANCE.anchorReady = false;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      enabled = var0;
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
   }

   public static void b(JsonObject var0) {
      if (var0.has("enabled")) {
         enabled = var0.get("enabled").getAsBoolean();
      }
   }

   public static void c(JsonObject var0) {
      var0.addProperty("silverShimmer", silverShimmer);
      var0.addProperty("hideRender", hideRender);
      var0.addProperty("themeColor", themeColor);
      var0.addProperty("sortByLength", sortByLength);
      var0.addProperty("showSuffix", showSuffix);
      var0.addProperty("showBinds", showBinds);
      var0.addProperty("showCategoryIcons", showCategoryIcons);
   }

   public static void d(JsonObject var0) {
      if (var0.has("silverShimmer")) {
         silverShimmer = var0.get("silverShimmer").getAsBoolean();
      }

      if (var0.has("hideRender")) {
         hideRender = var0.get("hideRender").getAsBoolean();
      }

      if (var0.has("themeColor")) {
         themeColor = var0.get("themeColor").getAsBoolean();
      }

      if (var0.has("sortByLength")) {
         sortByLength = var0.get("sortByLength").getAsBoolean();
      }

      if (var0.has("showSuffix")) {
         showSuffix = var0.get("showSuffix").getAsBoolean();
      }

      if (var0.has("showBinds")) {
         showBinds = var0.get("showBinds").getAsBoolean();
      }

      if (var0.has("showCategoryIcons")) {
         showCategoryIcons = var0.get("showCategoryIcons").getAsBoolean();
      }
   }

   public static void c() {
      EventBus.register(INSTANCE);
      List var0 = List.of(new HudConfig.b() {
         @Override
         public String a() {
            return "Серебро";
         }

         @Override
         public boolean b() {
            return ArrayListHud.silverShimmer;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.silverShimmer = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "Иконки";
         }

         @Override
         public boolean b() {
            return ArrayListHud.showCategoryIcons;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.showCategoryIcons = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "Скрыть Render";
         }

         @Override
         public boolean b() {
            return ArrayListHud.hideRender;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.hideRender = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "По длине";
         }

         @Override
         public boolean b() {
            return ArrayListHud.sortByLength;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.sortByLength = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "Цвет темы";
         }

         @Override
         public boolean b() {
            return ArrayListHud.themeColor;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.themeColor = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "Суффиксы";
         }

         @Override
         public boolean b() {
            return ArrayListHud.showSuffix;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.showSuffix = var1;
            ConfigManager.onSettingChanged();
         }
      }, new HudConfig.b() {
         @Override
         public String a() {
            return "Бинды";
         }

         @Override
         public boolean b() {
            return ArrayListHud.showBinds;
         }

         @Override
         public void a(boolean var1) {
            ArrayListHud.showBinds = var1;
            ConfigManager.onSettingChanged();
         }
      });
      HudConfig.a("arraylist", var0);
   }

   private Animation a(Function var1) {
      return this.animations.computeIfAbsent(var1, var0 -> new Animation(0.0F));
   }

   private String b(Function var1) {
      for (Setting var3 : var1.getSettings()) {
         if (var3 instanceof ModeSetting var4) {
            return var4.getSelection();
         }
      }

      return null;
   }

   private float c(Function var1) {
      float var2 = 0.0F;
      if (showCategoryIcons && var1.getCategory() != null) {
         String var3 = var1.getCategory().getIcon();
         if (var3 != null && !var3.isEmpty()) {
            var2 += Msdf.a(MsdfManager.DICONS, var3, 6.5F) + 3.5F;
         }
      }

      String var7 = var1.getName();
      float var6 = var2 + Msdf.a(MsdfManager.BOLD, var7, 6.5F);
      if (showSuffix) {
         String var4 = this.b(var1);
         if (var4 != null && !var4.isEmpty()) {
            var6 += 3.0F + Msdf.a(MsdfManager.BOLD, var4, 6.5F);
         }
      }

      int var8;
      if (showBinds && (var8 = var1.getKeybind()) != 0 && var8 != -1) {
         String var5 = "[" + KeyHelper.a(var8) + "]";
         var6 += 3.0F + Msdf.a(MsdfManager.BOLD, var5, 6.5F);
      }

      return var6;
   }

   private void a(MinecraftClient var1, float var2, float var3) {
      if (!(var1.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
      } else {
         double var4 = var1.getWindow().getScaleFactor();
         float var6 = (float)(var1.mouse.getX() / var4) * RenderUtil.e();
         float var7 = (float)(var1.mouse.getY() / var4) * RenderUtil.e();
         int var8;
         if ((var8 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1 ? 1 : 0) != 0
            && !this.wasPressed
            && !HudConfig.b(var6, var7)
            && var6 >= this.x - 2.0F
            && var6 <= this.x + var2 + 2.0F
            && var7 >= this.y - 2.0F
            && var7 <= this.y + var3 + 2.0F) {
            this.dragging = true;
            this.dragDX = var6 - this.x;
            this.dragDY = var7 - this.y;
         }

         boolean var9 = this.dragging;
         if (var8 == 0) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            float[] var10 = HudConfig.b("arraylist", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.x = var10[0];
            this.y = var10[1];
         } else if (var9) {
            ConfigManager.onSettingChanged();
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.activeModules.clear();

         for (Function var4 : FunctionManager.getFunctions()) {
            if (!(var4 instanceof InterfaceModule) && (!hideRender || var4.getCategory() != Category.RENDER)) {
               Animation var5 = this.a(var4);
               var5.setEasing(Easing.SPRING_SOFT);
               var5.animate(var4.isActive() ? 1.0F : 0.0F, 0.0F, 5.5F);
               if (var5.getValue() > 0.01F || var4.isActive()) {
                  this.activeModules.add(var4);
               }
            }
         }

         if (sortByLength) {
            this.activeModules.sort((var1x, var2x) -> {
               float var3 = this.c(var1x);
               int var5x;
               return (var5x = Float.compare(this.c(var2x), var3)) != 0 ? var5x : var1x.getName().compareTo(var2x.getName());
            });
         } else {
            this.activeModules.sort((var0, var1x) -> var0.getName().compareToIgnoreCase(var1x.getName()));
         }

         int var34 = this.activeModules.isEmpty() && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var34 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var35;
         if ((var35 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F) {
            this.animations.clear();
         } else {
            float var36 = (1.0F - var35) * 4.0F;
            if (cachedNameH < 0.0F) {
               cachedNameH = Msdf.a(MsdfManager.BOLD, 6.5F);
            }

            if (cachedIconH < 0.0F) {
               cachedIconH = Msdf.a(MsdfManager.DICONS, 6.5F);
            }

            long var6 = System.currentTimeMillis();
            if (!this.activeModules.isEmpty()) {
               float var8 = 0.0F;
               float var9 = 0.0F;

               for (Function var11 : this.activeModules) {
                  Animation var12 = this.a(var11);
                  float var13;
                  if (!((var13 = var12.getValue()) <= 0.01F)) {
                     float var14 = this.c(var11);
                     float var15;
                     if ((var15 = 5.0F + var14 + 5.0F) > var8) {
                        var8 = var15;
                     }

                     var9 += 12.0F * var13;
                  }
               }

               if (var9 > 0.0F) {
                  var9--;
               }

               float var39 = var8;
               float var41 = RenderUtil.a();
               float var44 = HudConfig.a("arraylist");
               float var47 = var39 * var44;
               float var50 = Math.max(11.0F * var44, var9 * var44);
               if (!this.initPos || this.x < 0.0F || this.y < 0.0F) {
                  this.x = 7.0F;
                  this.y = 50.0F;
                  this.initPos = true;
                  this.rightAnchored = false;
                  this.anchorEdge = this.x;
                  this.anchorReady = true;
               }

               this.a(var2, var47, var50);
               HudConfig.c("arraylist", this.x, this.y, var47, var50);
               if (var41 > 30.0F) {
                  if (!this.anchorReady) {
                     this.rightAnchored = this.x + var47 / 2.0F >= var41 / 2.0F;
                     this.anchorEdge = this.rightAnchored ? this.x + var47 : this.x;
                     this.anchorReady = true;
                  }

                  if (this.dragging) {
                     this.rightAnchored = this.x + var47 / 2.0F >= var41 / 2.0F;
                     this.anchorEdge = this.rightAnchored ? this.x + var47 : this.x;
                  } else {
                     this.x = this.rightAnchored ? this.anchorEdge - var47 : this.anchorEdge;
                  }

                  if (this.x > var41 - 2.0F) {
                     this.x = var41 - 2.0F;
                     if (this.rightAnchored) {
                        this.anchorEdge = this.x + var47;
                     }
                  } else if (this.x < 2.0F) {
                     this.x = 2.0F;
                     if (!this.rightAnchored) {
                        this.anchorEdge = this.x;
                     }
                  }
               }

               float var52 = this.y + var36;
               Matrix4f var16 = RenderUtil.d();
               var16.translate(this.x, var52, 0.0F);
               var16.scale(var44, var44, 1.0F);
               var16.translate(-this.x, -var52, 0.0F);
               float var17 = var52;

               for (Function var19 : this.activeModules) {
                  Animation var20 = this.a(var19);
                  float var21;
                  if (!((var21 = var20.getValue()) <= 0.01F)) {
                     float var22 = this.c(var19);
                     float var23 = 5.0F + var22 + 5.0F;
                     float var24 = (1.0F - var21) * (this.rightAnchored ? 10.0F : -10.0F);
                     float var25 = (this.rightAnchored ? this.x + var39 - var23 : this.x) + var24;
                     HudConfig.a(var16, var25, var17, var23, 11.0F, 3.5F, (int)(180.0F * var21 * var35) << 24);
                     var17 += 12.0F * var21;
                  }
               }

               Color var57 = Theme.getInstance().getAccent();
               Msdf.b();
               float var55 = var52;

               for (Function var62 : this.activeModules) {
                  Animation var65 = this.a(var62);
                  float var68;
                  if (!((var68 = var65.getValue()) <= 0.01F)) {
                     float var71 = this.c(var62);
                     float var74 = (1.0F - var68) * (this.rightAnchored ? 10.0F : -10.0F);
                     float var75 = (this.rightAnchored ? this.x + var39 - 5.0F - var71 : this.x + 5.0F) + var74;
                     float var26 = var55 + (11.0F - cachedNameH) * 0.5F - 0.5F;
                     float var27 = var55 + (11.0F - cachedIconH) * 0.5F - 0.5F;
                     float var28 = 255.0F * var68 * var35;
                     int var29;
                     if (themeColor) {
                        var29 = a(var57, var28);
                     } else if (silverShimmer) {
                        var29 = b(var55 * 0.045F - (float)var6 * 0.0018F, var28);
                     } else {
                        var29 = a(Color.WHITE, var28);
                     }

                     int var30 = a(var57, var28);
                     if (showCategoryIcons && var62.getCategory() != null) {
                        String var31 = var62.getCategory().getIcon();
                        if (var31 != null && !var31.isEmpty()) {
                           Msdf.a(var16, MsdfManager.DICONS, var31, var75, var27, 6.5F, var30);
                           var75 += Msdf.a(MsdfManager.DICONS, var31, 6.5F) + 3.5F;
                        }
                     }

                     Msdf.a(var16, MsdfManager.BOLD, var62.getName(), var75, var26, 6.5F, var29);
                     float var76 = var75 + Msdf.a(MsdfManager.BOLD, var62.getName(), 6.5F);
                     if (showSuffix) {
                        String var85 = this.b(var62);
                        if (var85 != null && !var85.isEmpty()) {
                           var75 = var76 + 3.0F;
                           int var32 = silverShimmer ? b(var55 * 0.045F - (float)var6 * 0.0018F + 0.2F, var28 * 0.7F) : a(new Color(150, 155, 170), var28);
                           Msdf.a(var16, MsdfManager.BOLD, var85, var75, var26, 6.5F, var32);
                           var76 = var75 + Msdf.a(MsdfManager.BOLD, var85, 6.5F);
                        }
                     }

                     int var86;
                     if (showBinds && (var86 = var62.getKeybind()) != 0 && var86 != -1) {
                        var75 = var76 + 3.0F;
                        String var87 = "[" + KeyHelper.a(var86) + "]";
                        int var33 = silverShimmer ? b(var55 * 0.045F - (float)var6 * 0.0018F + 0.35F, var28 * 0.55F) : a(new Color(120, 125, 140), var28);
                        Msdf.a(var16, MsdfManager.BOLD, var87, var75, var26, 6.5F, var33);
                     }

                     var55 += 12.0F * var68;
                  }
               }

               Msdf.c();
               this.animations.entrySet().removeIf(var0 -> !var0.getKey().isActive() && var0.getValue().getValue() <= 0.01F);
               HudConfig.c("arraylist");
            } else if (var2.currentScreen instanceof ChatScreen) {
               String[] var37 = new String[]{"ArrayList", "Auto Crystal", "Attack Aura"};
               Category[] var38 = new Category[]{Category.RENDER, Category.COMBAT, Category.COMBAT};
               float var40 = 0.0F;

               for (int var42 = 0; var42 < var37.length; var42++) {
                  float var45 = Msdf.a(MsdfManager.BOLD, var37[var42], 6.5F);
                  if (showCategoryIcons && var38[var42] != null) {
                     var45 += Msdf.a(MsdfManager.DICONS, var38[var42].getIcon(), 6.5F) + 3.5F;
                  }

                  float var48;
                  if ((var48 = 5.0F + var45 + 5.0F) > var40) {
                     var40 = var48;
                  }
               }

               float var43 = var40;
               float var46 = 35.0F;
               float var49 = HudConfig.a("arraylist");
               float var51 = var43 * var49;
               float var53 = var46 * var49;
               float var54 = RenderUtil.a();
               if (!this.initPos || this.x < 0.0F || this.y < 0.0F) {
                  this.x = 7.0F;
                  this.y = 50.0F;
                  this.initPos = true;
                  this.rightAnchored = false;
                  this.anchorEdge = this.x;
                  this.anchorReady = true;
               }

               this.a(var2, var51, var53);
               HudConfig.c("arraylist", this.x, this.y, var51, var53);
               if (var54 > 30.0F) {
                  if (!this.anchorReady) {
                     this.rightAnchored = this.x + var51 / 2.0F >= var54 / 2.0F;
                     this.anchorEdge = this.rightAnchored ? this.x + var51 : this.x;
                     this.anchorReady = true;
                  }

                  if (this.dragging) {
                     this.rightAnchored = this.x + var51 / 2.0F >= var54 / 2.0F;
                     this.anchorEdge = this.rightAnchored ? this.x + var51 : this.x;
                  } else {
                     this.x = this.rightAnchored ? this.anchorEdge - var51 : this.anchorEdge;
                  }
               }

               float var56 = this.y + var36;
               Matrix4f var58 = RenderUtil.d();
               var58.translate(this.x, var56, 0.0F);
               var58.scale(var49, var49, 1.0F);
               var58.translate(-this.x, -var56, 0.0F);
               float var60 = var56;

               for (int var63 = 0; var63 < var37.length; var63++) {
                  float var66 = Msdf.a(MsdfManager.BOLD, var37[var63], 6.5F);
                  if (showCategoryIcons && var38[var63] != null) {
                     var66 += Msdf.a(MsdfManager.DICONS, var38[var63].getIcon(), 6.5F) + 3.5F;
                  }

                  float var69 = 5.0F + var66 + 5.0F;
                  float var72 = this.rightAnchored ? this.x + var43 - var69 : this.x;
                  HudConfig.a(var58, var72, var60, var69, 11.0F, 3.5F, (int)(180.0F * var35) << 24);
                  var60 += 12.0F;
               }

               Color var64 = Theme.getInstance().getAccent();
               Msdf.b();
               float var61 = var56;

               for (int var67 = 0; var67 < var37.length; var67++) {
                  String var70 = var37[var67];
                  Category var73 = var38[var67];
                  float var79 = Msdf.a(MsdfManager.BOLD, var70, 6.5F);
                  if (showCategoryIcons && var73 != null) {
                     var79 += Msdf.a(MsdfManager.DICONS, var73.getIcon(), 6.5F) + 3.5F;
                  }

                  float var80 = this.rightAnchored ? this.x + var43 - 5.0F - var79 : this.x + 5.0F;
                  float var81 = var61 + (11.0F - cachedNameH) * 0.5F - 0.5F;
                  float var82 = var61 + (11.0F - cachedIconH) * 0.5F - 0.5F;
                  int var83;
                  if (themeColor) {
                     var83 = a(var64, 210.0F * var35);
                  } else if (silverShimmer) {
                     var83 = b(var61 * 0.045F - (float)var6 * 0.0018F, 210.0F * var35);
                  } else {
                     var83 = a(Color.WHITE, 200.0F * var35);
                  }

                  int var84 = a(var64, 210.0F * var35);
                  if (showCategoryIcons && var73 != null) {
                     Msdf.a(var58, MsdfManager.DICONS, var73.getIcon(), var80, var82, 6.5F, var84);
                     var80 += Msdf.a(MsdfManager.DICONS, var73.getIcon(), 6.5F) + 3.5F;
                  }

                  Msdf.a(var58, MsdfManager.BOLD, var70, var80, var81, 6.5F, var83);
                  var61 += 12.0F;
               }

               Msdf.c();
               HudConfig.c("arraylist");
            }
         }
      }
   }

   private static int b(float var0, float var1) {
      float var2 = var0 - (float)Math.floor(var0);
      Color var3 = new Color(255, 255, 255);
      Color var4 = new Color(130, 138, 154);
      Color var5 = new Color(230, 235, 245);
      Color var6 = new Color(175, 183, 198);
      Color var7;
      if (var2 < 0.25F) {
         var7 = a(var3, var4, var2 / 0.25F);
      } else if (var2 < 0.5F) {
         var7 = a(var4, var5, (var2 - 0.25F) / 0.25F);
      } else if (var2 < 0.75F) {
         var7 = a(var5, var6, (var2 - 0.5F) / 0.25F);
      } else {
         var7 = a(var6, var3, (var2 - 0.75F) / 0.25F);
      }

      return a(var7, var1);
   }

   private static Color a(Color var0, Color var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      int var3 = (int)(var0.getRed() + (var1.getRed() - var0.getRed()) * var2);
      int var4 = (int)(var0.getGreen() + (var1.getGreen() - var0.getGreen()) * var2);
      int var5 = (int)(var0.getBlue() + (var1.getBlue() - var0.getBlue()) * var2);
      return new Color(var3, var4, var5);
   }

   private static int a(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRGB() & 16777215;
   }
}
