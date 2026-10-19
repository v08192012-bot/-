package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
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

public class KeybindsHud {
   private static final KeybindsHud INSTANCE = new KeybindsHud();
   private static boolean enabled = false;
   private static final float HEADER_H = 14.0F;
   private static final float ROW_H = 13.0F;
   private static final float ROW_GAP = 2.5F;
   private static final float PAD_X = 5.5F;
   private final Animation entryAnim = new Animation();
   private final Animation widthAnim = new Animation();
   private final Map<Function, Animation> animations = new LinkedHashMap<>();
   private final Map<Function, Animation> toggleAnimations = new LinkedHashMap<>();
   private final List<Function> activeModules = new ArrayList<>();
   private static final Color SEP_COLOR = new Color(110, 115, 130);
   private float x = 7.0F;
   private float y = 7.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private static float cachedHeaderIconW = -1.0F;
   private static float cachedHeaderIconH = -1.0F;
   private static float cachedSepW = -1.0F;
   private static float cachedSepH = -1.0F;
   private static float cachedHeaderTextW = -1.0F;
   private static float cachedHeaderTextH = -1.0F;
   private static float cachedNameH = -1.0F;
   private static float cachedBOpenW = -1.0F;
   private static float cachedBCloseW = -1.0F;
   private static float cachedBindH = -1.0F;

   public static float[] a() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.initPos = true;
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

   public static void c() {
      EventBus.register(INSTANCE);
   }

   private Animation a(Function var1) {
      return this.animations.computeIfAbsent(var1, var0 -> new Animation(0.0F));
   }

   private Animation b(Function var1) {
      return this.toggleAnimations.computeIfAbsent(var1, var0 -> new Animation(var0.isActive() ? 1.0F : 0.0F));
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
            float[] var10 = HudConfig.b("keybinds", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
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
         byte var3 = 0;

         for (Function var5 : FunctionManager.getFunctions()) {
            if (var5.getKeybind() != 0 && var5.getKeybind() != -1 && var5.isActive()) {
               var3 = 1;
               break;
            }
         }

         int var48 = var3 == 0 && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var48 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var49;
         if ((var49 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F) {
            this.animations.clear();
            this.toggleAnimations.clear();
         } else {
            float var6 = (1.0F - var49) * 4.0F;
            Color var7 = Theme.getInstance().getAccent();
            this.activeModules.clear();

            for (Function var9 : FunctionManager.getFunctions()) {
               if (var9.getKeybind() != 0 && var9.getKeybind() != -1) {
                  Animation var11 = this.a(var9);
                  var11.setEasing(Easing.SPRING_SOFT);
                  var11.animate(var9.isActive() ? 1.0F : 0.0F, 0.0F, 5.5F);
                  if (var11.getValue() > 0.01F || var9.isActive() && var48 != 0) {
                     this.activeModules.add(var9);
                  }
               }
            }

            if (cachedHeaderIconW < 0.0F) {
               cachedHeaderIconW = Msdf.a(MsdfManager.DICONS, "C", 7.0F);
               cachedHeaderIconH = Msdf.a(MsdfManager.DICONS, 7.0F);
               cachedSepW = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
               cachedSepH = Msdf.a(MsdfManager.MONTSERRAT, 6.5F);
               cachedHeaderTextW = Msdf.a(MsdfManager.BOLD, "Hotkeys", 7.0F);
               cachedHeaderTextH = Msdf.a(MsdfManager.BOLD, 7.0F);
               cachedNameH = Msdf.a(MsdfManager.BOLD, 6.5F);
               cachedBOpenW = Msdf.a(MsdfManager.BOLD, "[", 6.25F);
               cachedBCloseW = Msdf.a(MsdfManager.BOLD, "]", 6.25F);
               cachedBindH = Msdf.a(MsdfManager.BOLD, 6.25F);
            }

            float var50 = cachedHeaderIconW;
            float var51 = cachedSepW;
            float var10 = cachedHeaderTextW;
            float var52 = 5.5F + var50 + 3.5F + var51 + 3.5F + var10 + 5.5F;
            float var12 = this.y + var6;
            float var13 = cachedHeaderIconH;
            float var14 = var12 + (14.0F - var13) * 0.5F;
            float var15 = 14.0F;
            float var16 = 7.5F;
            float var17 = 3.75F;
            float var18 = var16 - 2.0F;
            if (!this.activeModules.isEmpty()) {
               float var19 = cachedBOpenW;
               float var20 = cachedBCloseW;
               float var21 = var52;

               for (Function var23 : this.activeModules) {
                  String var24 = KeyHelper.a(var23.getKeybind());
                  float var25 = Msdf.a(MsdfManager.BOLD, var23.getName(), 6.5F);
                  float var26 = Msdf.a(MsdfManager.BOLD, var24, 6.25F);
                  float var27 = var19 + var26 + var20;
                  float var28;
                  if ((var28 = 5.5F + var15 + 3.5F + var51 + 3.5F + var25 + 4.5F + var27 + 5.5F) > var21) {
                     var21 = var28;
                  }
               }

               float var56 = Math.max(var52, var21);
               this.widthAnim.setEasing(Easing.SPRING_SOFT);
               this.widthAnim.animate(var56, 0.0F, 6.0F);
               float var58 = Math.max(var52, this.widthAnim.getValue());
               float var60 = 16.5F + this.activeModules.size() * 15.5F;
               float var61 = HudConfig.a("keybinds");
               float var62 = var58 * var61;
               float var63 = var60 * var61;
               if (!this.initPos) {
                  this.x = RenderUtil.a() * 0.5F - var62 * 0.5F;
                  this.y = RenderUtil.b() - 60.0F;
                  this.initPos = true;
               }

               this.a(var2, var62, var63);
               HudConfig.c("keybinds", this.x, this.y, var62, var63);
               Matrix4f var64 = RenderUtil.d();
               var64.translate(this.x, var12, 0.0F);
               var64.scale(var61, var61, 1.0F);
               var64.translate(-this.x, -var12, 0.0F);
               HudConfig.a(var64, "keybinds", this.x, var12, var52, 14.0F, 5.0F, new Color(0, 0, 0, (int)(180.0F * var49)));
               float var29 = var12 + 14.0F + 2.5F;

               for (Function var31 : this.activeModules) {
                  Animation var32 = this.a(var31);
                  float var33;
                  if (!((var33 = var32.getValue()) <= 0.01F)) {
                     Animation var34 = this.b(var31);
                     var34.setEasing(Easing.SPRING_SOFT);
                     var34.animate(var31.isActive() ? 1.0F : 0.0F, 0.0F, 8.0F);
                     float var35 = Math.clamp(var34.getValue(), 0.0F, 1.0F);
                     String var36 = KeyHelper.a(var31.getKeybind());
                     float var37 = Msdf.a(MsdfManager.BOLD, var31.getName(), 6.5F);
                     float var38 = Msdf.a(MsdfManager.BOLD, var36, 6.25F);
                     float var39 = var19 + var38 + var20;
                     float var40 = 5.5F + var15 + 3.5F + var51 + 3.5F + var37 + 4.5F + var39 + 5.5F;
                     HudConfig.a(var64, this.x, var29, var40, 13.0F, 4.5F, (int)(180.0F * var33 * var49) << 24);
                     float var41 = this.x + 5.5F;
                     float var42 = var29 + (13.0F - var16) * 0.5F;
                     float var43 = var41 + 1.0F + (var15 - 2.0F - var18) * var35;
                     float var44 = var42 + 1.0F;
                     int var45 = a(1645346, var7.getRGB(), 0.25F * var35);
                     RenderUtil.a(var64, var41, var42, var15, var16, var17, var17, var17, var17, Math.round(180.0F * var33 * var49) << 24 | var45);
                     int var46 = a(5593705, var7.getRGB(), var35);
                     RenderUtil.a(var64, var41, var42, var15, var16, var17, 0.75F, Math.round((160.0F + 95.0F * var35) * var33 * var49) << 24 | var46);
                     int var47 = a(9212315, 16777215, var35);
                     RenderUtil.a(
                        var64,
                        var43,
                        var44,
                        var18,
                        var18,
                        var18 * 0.5F,
                        var18 * 0.5F,
                        var18 * 0.5F,
                        var18 * 0.5F,
                        Math.round(255.0F * var33 * var49) << 24 | var47
                     );
                     var29 += 15.5F * var33;
                  }
               }

               Msdf.b();
               Msdf.a(var64, MsdfManager.DICONS, "C", this.x + 5.5F, var14, 7.0F, a(var7, 255.0F * var49));
               Msdf.a(var64, MsdfManager.MONTSERRAT, "|", this.x + 5.5F + var50 + 3.5F, var12 + (14.0F - cachedSepH) * 0.5F, 6.5F, a(SEP_COLOR, 160.0F * var49));
               Msdf.a(
                  var64,
                  MsdfManager.BOLD,
                  "Hotkeys",
                  this.x + 5.5F + var50 + 3.5F + var51 + 3.5F,
                  var12 + (14.0F - cachedHeaderTextH) * 0.5F,
                  7.0F,
                  a(Color.WHITE, 255.0F * var49)
               );
               float var65 = var12 + 14.0F + 2.5F;

               for (Function var67 : this.activeModules) {
                  Animation var68 = this.a(var67);
                  float var69;
                  if (!((var69 = var68.getValue()) <= 0.01F)) {
                     float var70 = 255.0F * var69 * var49;
                     String var71 = KeyHelper.a(var67.getKeybind());
                     float var72 = Msdf.a(MsdfManager.BOLD, var67.getName(), 6.5F);
                     float var73 = Msdf.a(MsdfManager.BOLD, var71, 6.25F);
                     float var74;
                     float var75 = (var74 = this.x + 5.5F) + var15 + 3.5F;
                     float var76 = var65 + (13.0F - cachedSepH) * 0.5F;
                     Msdf.a(var64, MsdfManager.MONTSERRAT, "|", var75, var76, 6.5F, a(SEP_COLOR, 160.0F * var69 * var49));
                     float var77 = var75 + var51 + 3.5F;
                     float var78 = var65 + (13.0F - cachedNameH) * 0.5F;
                     Msdf.a(var64, MsdfManager.BOLD, var67.getName(), var77, var78, 6.5F, a(Color.WHITE, var70));
                     float var79 = var77 + var72 + 4.5F;
                     float var80 = var65 + (13.0F - cachedBindH) * 0.5F;
                     Msdf.a(var64, MsdfManager.BOLD, "[", var79, var80, 6.25F, a(SEP_COLOR, 160.0F * var69 * var49));
                     Msdf.a(var64, MsdfManager.BOLD, var71, var79 + var19, var80, 6.25F, a(var7, var70));
                     Msdf.a(var64, MsdfManager.BOLD, "]", var79 + var19 + var73, var80, 6.25F, a(SEP_COLOR, 160.0F * var69 * var49));
                     var65 += 15.5F * var69;
                  }
               }

               Msdf.c();
               this.animations
                  .entrySet()
                  .removeIf(
                     var0 -> (!var0.getKey().isActive() || var0.getKey().getKeybind() == 0 || var0.getKey().getKeybind() == -1)
                        && var0.getValue().getValue() <= 0.01F
                  );
               HudConfig.c("keybinds");
            } else if (var2.currentScreen instanceof ChatScreen) {
               this.widthAnim.setEasing(Easing.SPRING_SOFT);
               this.widthAnim.animate(var52, 0.0F, 6.0F);
               float var53 = Math.max(var52, this.widthAnim.getValue());
               float var54 = HudConfig.a("keybinds");
               float var55 = var53 * var54;
               float var57 = 14.0F * var54;
               if (!this.initPos) {
                  this.x = RenderUtil.a() * 0.5F - var55 * 0.5F;
                  this.y = RenderUtil.b() - 60.0F;
                  this.initPos = true;
               }

               HudConfig.c("keybinds", this.x, this.y, var55, var57);
               this.a(var2, var55, var57);
               Matrix4f var59 = RenderUtil.d();
               var59.translate(this.x, var12, 0.0F);
               var59.scale(var54, var54, 1.0F);
               var59.translate(-this.x, -var12, 0.0F);
               HudConfig.a(var59, "keybinds", this.x, var12, var52, 14.0F, 5.0F, new Color(0, 0, 0, (int)(180.0F * var49)));
               Msdf.b();
               Msdf.a(var59, MsdfManager.DICONS, "C", this.x + 5.5F, var14, 7.0F, a(var7, 255.0F * var49));
               Msdf.a(var59, MsdfManager.MONTSERRAT, "|", this.x + 5.5F + var50 + 3.5F, var12 + (14.0F - cachedSepH) * 0.5F, 6.5F, a(SEP_COLOR, 160.0F * var49));
               Msdf.a(
                  var59,
                  MsdfManager.BOLD,
                  "Hotkeys",
                  this.x + 5.5F + var50 + 3.5F + var51 + 3.5F,
                  var12 + (14.0F - cachedHeaderTextH) * 0.5F,
                  7.0F,
                  a(Color.WHITE, 255.0F * var49)
               );
               Msdf.c();
               HudConfig.c("keybinds");
            }
         }
      }
   }

   private static int a(int var0, int var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      int var3 = var0 >> 16 & 0xFF;
      int var4 = var0 >> 8 & 0xFF;
      int var5 = var0 & 0xFF;
      int var6 = var1 >> 16 & 0xFF;
      int var7 = var1 >> 8 & 0xFF;
      int var8 = var1 & 0xFF;
      int var9 = (int)(var3 + (var6 - var3) * var2);
      int var10 = (int)(var4 + (var7 - var4) * var2);
      int var11 = (int)(var5 + (var8 - var5) * var2);
      return var9 << 16 | var10 << 8 | var11;
   }

   private static int a(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRGB() & 16777215;
   }
}
