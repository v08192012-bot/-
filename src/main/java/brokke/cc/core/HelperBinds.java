package brokke.cc.core;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.module.misc.ServerHelper;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.KeyHelper;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class HelperBinds {
   private static final HelperBinds INSTANCE = new HelperBinds();
   private static final String ID = "helperbinds";
   private static boolean enabled = false;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private float x = 7.0F;
   private float y = 7.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private float panelW;
   private float panelH;
   private float itemY;
   private float itemSize;
   private final List<ServerHelper.b> visibleBinds = new ArrayList<>();
   private final List<Float> itemXs = new ArrayList<>();
   private final List<Float> pillXs = new ArrayList<>();
   private final List<Float> pillWs = new ArrayList<>();

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
            float[] var10 = HudConfig.b("helperbinds", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.x = var10[0];
            this.y = var10[1];
         } else if (var9) {
            ConfigManager.onSettingChanged();
         }
      }
   }

   private void d() {
      this.visibleBinds.clear();
      ServerHelper var1 = FunctionManager.a(ServerHelper.class);
      if (var1 != null) {
         List var2 = null;
         if (var1.k()) {
            var2 = var1.m();
         } else if (var1.l()) {
            var2 = var1.n();
         }

         if (var2 != null) {
            for (int var3 = 0; var3 < var2.size(); var3++) {
               ServerHelper.b var4 = (ServerHelper.b)var2.get(var3);
               int var5;
               if ((var5 = var4.getBind().getKey()) != -1 && var5 != 0) {
                  this.visibleBinds.add(var4);
               }
            }
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.itemXs.clear();
         this.d();
         int var3 = this.visibleBinds.isEmpty() && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var3 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var4;
         if (!((var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
            float var5 = (1.0F - var4) * 4.0F;
            float var6 = 14.0F;
            float var7 = 5.0F;
            float var10000 = 3.0F;
            float var9 = 4.0F;
            this.itemSize = 10.0F;
            float var10 = 5.0F;
            int var11;
            int var12 = (var11 = (int)(180.0F * var4)) << 24;
            float var13 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
            float var14 = this.y + var5;
            if (!this.visibleBinds.isEmpty()) {
               this.pillXs.clear();
               this.pillWs.clear();
               float var30 = this.x;

               for (int var31 = 0; var31 < this.visibleBinds.size(); var31++) {
                  ServerHelper.b var33 = this.visibleBinds.get(var31);
                  String var35 = KeyHelper.a(var33.getBind().getKey());
                  float var37 = Msdf.a(MsdfManager.BOLD, var35, 6.5F);
                  float var39 = var7 + this.itemSize + 3.5F + var13 + 3.5F + var37 + var7;
                  this.pillXs.add(var30);
                  this.pillWs.add(var39);
                  var30 += var39;
                  if (var31 < this.visibleBinds.size() - 1) {
                     var30 += var9;
                  }
               }

               this.panelW = var30 - this.x;
               this.panelH = var6;
               float var32 = HudConfig.a("helperbinds");
               float var34 = this.panelW * var32;
               float var36 = this.panelH * var32;
               if (!this.initPos) {
                  this.x = RenderUtil.a() - var34 - 10.0F;
                  this.y = RenderUtil.b() - 40.0F;
                  this.initPos = true;
               }

               this.a(var2, var34, var36);
               HudConfig.c("helperbinds", this.x, this.y, var34, var36);
               Matrix4f var38 = RenderUtil.d();
               var38.translate(this.x, var14, 0.0F);
               var38.scale(var32, var32, 1.0F);
               var38.translate(-this.x, -var14, 0.0F);
               this.itemY = var14 + (var6 - this.itemSize) * 0.5F;
               if (var11 > 0 && InterfaceModule.l()) {
                  int var40 = Math.round(140.0F * (var11 / 255.0F));
                  RenderUtil.b(var38, this.x, var14, this.panelW, this.panelH, var10, 4.5F, 0.4F, 2.5F, var40 << 24);
               }

               for (int var41 = 0; var41 < this.visibleBinds.size(); var41++) {
                  float var43 = this.pillXs.get(var41);
                  float var45 = this.pillWs.get(var41);
                  HudConfig.a(var38, var43, var14, var45, var6, var10, var12);
                  this.itemXs.add(var43 + var7);
               }

               int var42 = Math.round(160.0F * var4) << 24 | 7238530;
               int var44 = Math.round(255.0F * var4) << 24 | 15790325;
               float var46 = var14 + (var6 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
               float var47 = var14 + (var6 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
               Msdf.b();

               for (int var48 = 0; var48 < this.visibleBinds.size(); var48++) {
                  ServerHelper.b var49 = this.visibleBinds.get(var48);
                  String var50 = KeyHelper.a(var49.getBind().getKey());
                  float var28 = this.pillXs.get(var48) + var7 + this.itemSize + 3.5F;
                  Msdf.a(var38, MsdfManager.MONTSERRAT, "|", var28, var46, 6.5F, var42);
                  float var29 = var28 + var13 + 3.5F;
                  Msdf.a(var38, MsdfManager.BOLD, var50, var29, var47, 6.5F, var44);
               }

               Msdf.c();
               HudConfig.c("helperbinds");
            } else if (var2.currentScreen instanceof ChatScreen) {
               float var15 = Msdf.a(MsdfManager.ICONS2, "x", 7.0F);
               String var16 = "Хелпер бинды";
               float var17 = Msdf.a(MsdfManager.BOLD, var16, 6.5F);
               float var18 = var7 + var15 + 3.5F + var13 + 3.5F + var17 + var7;
               this.panelW = var18;
               this.panelH = var6;
               float var19 = HudConfig.a("helperbinds");
               float var20 = this.panelW * var19;
               float var21 = this.panelH * var19;
               if (!this.initPos) {
                  this.x = RenderUtil.a() - var20 - 10.0F;
                  this.y = RenderUtil.b() - 40.0F;
                  this.initPos = true;
               }

               this.a(var2, var20, var21);
               HudConfig.c("helperbinds", this.x, this.y, var20, var21);
               Matrix4f var22 = RenderUtil.d();
               var22.translate(this.x, var14, 0.0F);
               var22.scale(var19, var19, 1.0F);
               var22.translate(-this.x, -var14, 0.0F);
               HudConfig.a(var22, "helperbinds", this.x, var14, var18, var6, var10, var12);
               float var23 = Msdf.a(MsdfManager.ICONS2, 7.0F);
               float var24 = var14 + (var6 - var23) / 2.0F;
               Color var25 = Theme.getInstance().getAccent();
               int var26 = Math.round(255.0F * var4) << 24 | (var25 != null ? var25.getRGB() & 16777215 : 16777215);
               Msdf.a(var22, MsdfManager.ICONS2, "x", this.x + var7, var24, 7.0F, var26);
               Msdf.a(
                  var22,
                  MsdfManager.MONTSERRAT,
                  "|",
                  this.x + var7 + var15 + 3.5F,
                  var14 + (var6 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F,
                  6.5F,
                  Math.round(160.0F * var4) << 24 | 7238530
               );
               Msdf.a(
                  var22,
                  MsdfManager.BOLD,
                  var16,
                  this.x + var7 + var15 + 3.5F + var13 + 3.5F,
                  var14 + (var6 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F,
                  6.5F,
                  Math.round(255.0F * var4) << 24 | 15790325
               );
               HudConfig.c("helperbinds");
            }
         }
      } else {
         this.visibleBinds.clear();
         this.itemXs.clear();
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (enabled && this.mc.player != null && !this.mc.options.hudHidden && !this.visibleBinds.isEmpty()) {
         DrawContext var2 = var1.getContext();
         float var3 = HudConfig.a("helperbinds");
         float var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F);
         float var5 = (1.0F - var4) * 4.0F;
         float var6 = this.y + var5;
         float var7 = 2.0F / this.mc.getWindow().getScaleFactor();

         for (int var8 = 0; var8 < this.visibleBinds.size() && var8 < this.itemXs.size(); var8++) {
            ServerHelper.b var9 = this.visibleBinds.get(var8);
            float var10 = this.itemXs.get(var8);
            float var11 = this.itemY;
            if (!ClickGui.a(var10, var11, this.itemSize, this.itemSize)) {
               int var12 = 0;

               for (int var13 = 0; var13 < this.mc.player.getInventory().size(); var13++) {
                  ItemStack var14 = this.mc.player.getInventory().getStack(var13);
                  if (!var14.isEmpty() && var14.getItem() == var9.getItem()) {
                     var12 += var14.getCount();
                  }
               }

               ItemStack var16 = new ItemStack(var9.getItem(), Math.max(var12, 1));
               Matrix3x2fStack var17 = var2.getMatrices();
               var17.pushMatrix();
               var17.translate(this.x * var7, var6 * var7);
               var17.scale(var3, var3);
               var17.translate((var10 - this.x + this.itemSize * 0.5F) * var7, (var11 - var6 + this.itemSize * 0.5F) * var7);
               float var15 = this.itemSize / 16.0F * var7;
               var17.scale(var15, var15);
               var17.translate(-8.0F, -8.0F);
               var2.drawItem(var16, 0, 0);
               var2.drawStackOverlay(this.mc.textRenderer, var16, 0, 0);
               var17.popMatrix();
            }
         }
      }
   }

   private static int a(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRGB() & 16777215;
   }
}
