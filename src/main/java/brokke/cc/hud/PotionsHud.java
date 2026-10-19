package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class PotionsHud {
   private static final PotionsHud INSTANCE = new PotionsHud();
   private static boolean enabled = false;
   private static final float HEADER_H = 14.0F;
   private static final float ROW_H = 13.0F;
   private static final float ROW_GAP = 2.5F;
   private static final float PAD_X = 5.5F;
   private static final float ICON_SIZE = 8.5F;
   private final Animation entryAnim = new Animation();
   private final Map<StatusEffect, Animation> animations = new LinkedHashMap<>();
   private final Map<StatusEffect, PotionsHud.a> snapshots = new HashMap<>();
   private float x = 7.0F;
   private float y = 40.0F;
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
   private static float cachedTextH = -1.0F;
   private static float cachedFbIconH = -1.0F;

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

   private Animation a(StatusEffect var1) {
      return this.animations.computeIfAbsent(var1, var0 -> new Animation());
   }

   private static String a(int var0) {
      return switch (var0) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(var0);
      };
   }

   private static String a(int var0, boolean var1) {
      if (var1) {
         return "inf";
      }

      int var2;
      int var3 = (var2 = Math.max(0, var0 / 20)) / 60;
      int var4 = var2 % 60;
      return (var3 < 10 ? "0" + var3 : String.valueOf(var3)) + ":" + (var4 < 10 ? "0" + var4 : String.valueOf(var4));
   }

   private void a(StatusEffectInstance var1) {
      StatusEffect var2 = var1.getEffectType().value();
      PotionsHud.a var3 = this.snapshots.computeIfAbsent(var2, var0 -> new PotionsHud.a());
      var3.type = var2;
      var3.baseName = I18n.translate(var1.getTranslationKey());
      var3.amplifier = var1.getAmplifier() + 1;
      var3.duration = var1.getDuration();
      var3.infinite = var1.isInfinite();

      try {
         MinecraftClient var4 = MinecraftClient.getInstance();
         Identifier var5 = Registries.STATUS_EFFECT.getId(var2);
         Identifier var6 = Identifier.of(var5.getNamespace(), "textures/mob_effect/" + var5.getPath() + ".png");
         var3.entryView = var4.getTextureManager().getTexture(var6).getGlTextureView();
      } catch (Exception var7) {
         var3.entryView = null;
      }
   }

   private List<StatusEffect> a(List<StatusEffectInstance> var1, Set<StatusEffect> var2) {
      ArrayList var3 = new ArrayList();

      for (StatusEffectInstance var5 : var1) {
         StatusEffect var6 = var5.getEffectType().value();
         if (!var3.contains(var6)) {
            var3.add(var6);
         }
      }

      for (StatusEffect var8 : this.animations.keySet()) {
         if (!var2.contains(var8) && !var3.contains(var8)) {
            var3.add(var8);
         }
      }

      return var3;
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

         if (var8 == 0) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            float[] var9 = HudConfig.b("potions", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.x = var9[0];
            this.y = var9[1];
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         ArrayList var3 = new ArrayList<>(var2.player.getStatusEffects());
         int var4 = var3.isEmpty() && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var4 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var5;
         if ((var5 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F) {
            this.animations.clear();
            this.snapshots.clear();
         } else {
            float var6 = (1.0F - var5) * 4.0F;
            Color var7 = Theme.getInstance().getAccent();
            if (cachedHeaderIconW < 0.0F) {
               cachedHeaderIconW = Msdf.a(MsdfManager.DICONS, "E", 7.0F);
               cachedHeaderIconH = Msdf.a(MsdfManager.DICONS, 7.0F);
               cachedSepW = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
               cachedSepH = Msdf.a(MsdfManager.MONTSERRAT, 6.5F);
               cachedHeaderTextW = Msdf.a(MsdfManager.BOLD, "Potions", 7.0F);
               cachedHeaderTextH = Msdf.a(MsdfManager.BOLD, 7.0F);
               cachedTextH = Msdf.a(MsdfManager.BOLD, 6.5F);
               cachedFbIconH = Msdf.a(MsdfManager.DICONS, 7.0F);
            }

            float var8 = cachedHeaderIconW;
            float var9 = cachedSepW;
            float var10 = cachedHeaderTextW;
            float var11 = 5.5F + var8 + 3.5F + var9 + 3.5F + var10 + 5.5F;
            if (var3.isEmpty()) {
               for (Entry var13 : this.animations.entrySet()) {
                  ((Animation)var13.getValue()).setEasing(Easing.SPRING_SOFT);
                  ((Animation)var13.getValue()).animate(0.0F, 0.0F, 5.5F);
               }

               if (!(var2.currentScreen instanceof ChatScreen) && this.animations.isEmpty()) {
                  return;
               }

               if (var3.isEmpty() && var2.currentScreen instanceof ChatScreen) {
                  float var41 = HudConfig.a("potions");
                  float var45 = var11 * var41;
                  float var48 = 14.0F * var41;
                  if (!this.initPos) {
                     this.x = 7.0F;
                     this.y = 40.0F;
                     this.initPos = true;
                  }

                  float var51 = this.y + var6;
                  HudConfig.c("potions", this.x, this.y, var45, var48);
                  this.a(var2, var45, var48);
                  Matrix4f var53 = RenderUtil.d();
                  var53.translate(this.x, var51, 0.0F);
                  var53.scale(var41, var41, 1.0F);
                  var53.translate(-this.x, -var51, 0.0F);
                  float var55 = cachedHeaderIconH;
                  float var57 = var51 + (14.0F - var55) / 2.0F;
                  HudConfig.a(var53, "potions", this.x, var51, var11, 14.0F, 5.0F, (int)(180.0F * var5) << 24);
                  Msdf.b();
                  Msdf.a(var53, MsdfManager.DICONS, "E", this.x + 5.5F, var57, 7.0F, b(var7, 255.0F * var5));
                  Msdf.a(
                     var53,
                     MsdfManager.MONTSERRAT,
                     "|",
                     this.x + 5.5F + var8 + 3.5F,
                     var51 + (14.0F - cachedSepH) / 2.0F,
                     6.5F,
                     b(new Color(110, 115, 130), 160.0F * var5)
                  );
                  Msdf.a(
                     var53,
                     MsdfManager.BOLD,
                     "Potions",
                     this.x + 5.5F + var8 + 3.5F + var9 + 3.5F,
                     var51 + (14.0F - cachedHeaderTextH) / 2.0F,
                     7.0F,
                     b(Color.WHITE, 255.0F * var5)
                  );
                  Msdf.c();
                  HudConfig.c("potions");
                  return;
               }
            }

            HashSet var40 = new HashSet();

            for (StatusEffectInstance var14 : (Iterable<StatusEffectInstance>)(Iterable<?>)(var3)) {
               StatusEffect var15 = var14.getEffectType().value();
               var40.add(var15);
               this.a(var15).setEasing(Easing.SPRING_SOFT);
               this.a(var15).animate(1.0F, 0.0F, 5.5F);
               this.a(var14);
            }

            for (Entry var46 : this.animations.entrySet()) {
               if (!var40.contains(var46.getKey())) {
                  ((Animation)var46.getValue()).setEasing(Easing.SPRING_SOFT);
                  ((Animation)var46.getValue()).animate(0.0F, 0.0F, 6.0F);
               }
            }

            List var44 = this.a(var3, var40);
            float var47 = var11;

            for (StatusEffect var16 : (Iterable<StatusEffect>)(Iterable<?>)(var44)) {
               PotionsHud.a var17 = this.snapshots.get(var16);
               if (var17 != null) {
                  String var18 = a(var17.duration, var17.infinite);
                  String var19 = var17.amplifier > 1 ? var17.baseName + " " + a(var17.amplifier) : var17.baseName;
                  float var20 = Msdf.a(MsdfManager.BOLD, var18, 6.5F);
                  float var21 = Msdf.a(MsdfManager.BOLD, var19, 6.5F);
                  float var22;
                  if ((var22 = 17.5F + var9 + 3.5F + var20 + 5.0F + var21 + 5.5F) > var47) {
                     var47 = var22;
                  }
               }
            }

            float var50 = Math.max(var11, var47);
            float var52 = 16.5F + var44.size() * 15.5F;
            float var54 = HudConfig.a("potions");
            float var56 = var50 * var54;
            float var58 = var52 * var54;
            if (!this.initPos) {
               this.x = 7.0F;
               this.y = 40.0F;
               this.initPos = true;
            }

            this.a(var2, var56, var58);
            HudConfig.c("potions", this.x, this.y, var56, var58);
            float var59 = this.y + var6;
            Matrix4f var60 = RenderUtil.d();
            var60.translate(this.x, var59, 0.0F);
            var60.scale(var54, var54, 1.0F);
            var60.translate(-this.x, -var59, 0.0F);
            float var61 = cachedHeaderIconH;
            float var23 = var59 + (14.0F - var61) / 2.0F;
            HudConfig.a(var60, "potions", this.x, var59, var11, 14.0F, 5.0F, (int)(180.0F * var5) << 24);
            float var24 = var59 + 14.0F + 2.5F;

            for (StatusEffect var26 : (Iterable<StatusEffect>)(Iterable<?>)(var44)) {
               Animation var27 = this.a(var26);
               float var28 = var27.getValue();
               PotionsHud.a var29 = this.snapshots.get(var26);
               if (!(var28 <= 0.01F) && var29 != null) {
                  String var30 = a(var29.duration, var29.infinite);
                  String var31 = var29.amplifier > 1 ? var29.baseName + " " + a(var29.amplifier) : var29.baseName;
                  float var32 = Msdf.a(MsdfManager.BOLD, var30, 6.5F);
                  float var33 = Msdf.a(MsdfManager.BOLD, var31, 6.5F);
                  float var34 = 17.5F + var9 + 3.5F + var32 + 5.0F + var33 + 5.5F;
                  float var35 = 255.0F * var28 * var5;
                  HudConfig.a(var60, this.x, var24, var34, 13.0F, 4.5F, (int)(180.0F * var28 * var5) << 24);
                  float var36 = this.x + 5.5F;
                  float var37 = var24 + 2.25F;
                  if (var29.entryView != null) {
                     TexturePipeline.b(var60, var36, var37, 8.5F, 8.5F, var29.entryView, b(Color.WHITE, var35), 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
                  }

                  var24 += 15.5F * var28;
               }
            }

            Msdf.b();
            Msdf.a(var60, MsdfManager.DICONS, "E", this.x + 5.5F, var23, 7.0F, b(var7, 255.0F * var5));
            Msdf.a(
               var60,
               MsdfManager.MONTSERRAT,
               "|",
               this.x + 5.5F + var8 + 3.5F,
               var59 + (14.0F - cachedSepH) / 2.0F,
               6.5F,
               b(new Color(110, 115, 130), 160.0F * var5)
            );
            Msdf.a(
               var60,
               MsdfManager.BOLD,
               "Potions",
               this.x + 5.5F + var8 + 3.5F + var9 + 3.5F,
               var59 + (14.0F - cachedHeaderTextH) / 2.0F,
               7.0F,
               b(Color.WHITE, 255.0F * var5)
            );
            float var62 = var59 + 14.0F + 2.5F;

            for (StatusEffect var64 : (Iterable<StatusEffect>)(Iterable<?>)(var44)) {
               Animation var65 = this.a(var64);
               float var66 = var65.getValue();
               PotionsHud.a var67 = this.snapshots.get(var64);
               if (!(var66 <= 0.01F) && var67 != null) {
                  String var68 = a(var67.duration, var67.infinite);
                  String var69 = var67.amplifier > 1 ? var67.baseName + " " + a(var67.amplifier) : var67.baseName;
                  float var70 = Msdf.a(MsdfManager.BOLD, var68, 6.5F);
                  float var71 = 255.0F * var66 * var5;
                  float var72 = this.x + 5.5F;
                  if (var67.entryView == null) {
                     float var73 = var62 + (13.0F - cachedFbIconH) / 2.0F;
                     Msdf.a(var60, MsdfManager.DICONS, "E", var72, var73, 7.0F, b(var7, var71));
                  }

                  float var74 = var72 + 8.5F + 3.5F;
                  float var75 = var62 + (13.0F - cachedSepH) / 2.0F;
                  Msdf.a(var60, MsdfManager.MONTSERRAT, "|", var74, var75, 6.5F, b(new Color(110, 115, 130), 160.0F * var66 * var5));
                  float var76 = var74 + var9 + 3.5F;
                  float var38 = var62 + (13.0F - cachedTextH) / 2.0F;
                  Msdf.a(var60, MsdfManager.BOLD, var68, var76, var38, 6.5F, b(new Color(180, 185, 200), var71));
                  float var39 = var76 + var70 + 5.0F;
                  Msdf.a(var60, MsdfManager.BOLD, var69, var39, var38, 6.5F, b(Color.WHITE, var71));
                  var62 += 15.5F * var66;
               }
            }

            Msdf.c();
            HudConfig.c("potions");
         }
      }
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
   }

   private static int b(Color var0, float var1) {
      Color var2 = a(var0, var1);
      return var2.getAlpha() << 24 | var2.getRed() << 16 | var2.getGreen() << 8 | var2.getBlue();
   }

   static final class a {
      StatusEffect type;
      String baseName;
      int amplifier;
      int duration;
      boolean infinite;
      GpuTextureView entryView;
   }
}
