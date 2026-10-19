package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.ClickGui;
import com.google.gson.JsonObject;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class InventoryHud {
   private static final InventoryHud INSTANCE = new InventoryHud();
   private static final String ID = "inventory";
   private static boolean enabled = false;
   private static final float HEADER_PAD_X = 5.0F;
   private static final float HEADER_H = 13.0F;
   private static final float ROW_GAP = 3.5F;
   private static final float GRID_PAD_X = 4.0F;
   private static final float GRID_PAD_Y = 4.0F;
   private static final float SLOT = 11.5F;
   private static final float GAP = 1.5F;
   private static final int COLS = 9;
   private static final int ROWS = 3;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private float x = 7.0F;
   private float y = 70.0F;
   private float targetX = 7.0F;
   private float targetY = 70.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;

   public static float[] a() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.targetX = var0;
      INSTANCE.targetY = var1;
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
            this.dragDX = var6 - this.targetX;
            this.dragDY = var7 - this.targetY;
         }

         if (var8 == 0) {
            if (this.dragging) {
               HudConfig.b();
               ConfigManager.onSettingChanged();
            }

            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            float[] var9 = HudConfig.b("inventory", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.targetX = var9[0];
            this.targetY = var9[1];
         }

         if (this.dragging || Math.abs(this.targetX - this.x) > 0.05F || Math.abs(this.targetY - this.y) > 0.05F) {
            float[] var10 = HudConfig.a("inventory", this.x, this.y, this.targetX, this.targetY);
            this.x = var10[0];
            this.y = var10[1];
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null) {
         int var3 = enabled && !var2.options.hudHidden ? 1 : 0;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var3 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var4;
         if (!((var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
            float var5 = Msdf.a(MsdfManager.DICONS, "D", 6.5F);
            float var6 = Msdf.a(MsdfManager.DICONS, 6.5F);
            float var7 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
            float var8 = Msdf.a(MsdfManager.MONTSERRAT, 6.5F);
            float var9 = Msdf.a(MsdfManager.BOLD, "Inventory", 6.5F);
            float var10 = Msdf.a(MsdfManager.BOLD, 6.5F);
            float var11 = 5.0F + var5 + 3.5F + var7 + 3.5F + var9 + 5.0F;
            float var12 = 115.5F;
            float var13 = 37.5F;
            float var14 = 8.0F + var12;
            float var15 = 8.0F + var13;
            float var16 = Math.max(var11, var14);
            float var17 = 16.5F + var15;
            float var18 = HudConfig.a("inventory");
            float var19 = var16 * var18;
            float var20 = var17 * var18;
            if (!this.initPos) {
               this.x = 7.0F;
               this.y = 70.0F;
               this.targetX = this.x;
               this.targetY = this.y;
               this.initPos = true;
            }

            this.a(var2, var19, var20);
            HudConfig.c("inventory", this.x, this.y, var19, var20);
            float var21 = (1.0F - var4) * 4.0F;
            float var22 = this.y + var21;
            Matrix4f var23 = RenderUtil.d();
            var23.translate(this.x, var22, 0.0F);
            var23.scale(var18, var18, 1.0F);
            var23.translate(-this.x, -var22, 0.0F);
            Color var24 = Theme.getInstance().getAccent();
            HudConfig.a(var23, "inventory", this.x, var22 + 3.0F, var11, 13.0F, 5.0F, (int)(180.0F * var4) << 24);
            float var25 = var22 + 6.5F;
            Msdf.a(var23, MsdfManager.DICONS, "U", this.x + 5.0F, var25 + 3.0F - var6 * 0.5F, 6.5F, a(var24, 255.0F * var4));
            Msdf.a(
               var23,
               MsdfManager.MONTSERRAT,
               "|",
               this.x + 5.0F + var5 + 3.5F,
               var25 + 3.0F - 0.5F - var8 * 0.5F,
               6.5F,
               a(new Color(110, 115, 130), 160.0F * var4)
            );
            Msdf.a(
               var23,
               MsdfManager.BOLD,
               "Inventory",
               this.x + 5.0F + var5 + 3.5F + var7 + 3.5F,
               var25 - 0.5F + 3.0F - var10 * 0.5F,
               6.5F,
               a(Color.WHITE, 255.0F * var4)
            );
            float var26 = var22 + 13.0F + 3.5F;
            HudConfig.a(var23, "inventory", this.x, var26, var14, var15, 5.0F, (int)(180.0F * var4) << 24);
            float var27 = this.x + 4.0F;
            float var28 = var26 + 4.0F;
            BufferBuilder var29 = ColorPipeline.a();
            if (var29 != null) {
               int var30 = a(new Color(0, 0, 0), 65.0F * var4);
               int var31 = a(new Color(255, 255, 255), 14.0F * var4);

               for (int var32 = 0; var32 < 3; var32++) {
                  for (int var33 = 0; var33 < 9; var33++) {
                     float var34 = this.x + (var27 + var33 * 13.0F - this.x) * var18;
                     float var35 = var22 + (var28 + var32 * 13.0F - var22) * var18;
                     int var36 = 9 + var32 * 9 + var33;
                     ItemStack var37 = var2.player.getInventory().getStack(var36);
                     ColorPipeline.a(var29, var34, var35, 11.5F * var18, 11.5F * var18, var37.isEmpty() ? var30 : var31);
                  }
               }

               ColorPipeline.a(var29, false);
            }
         }
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      float var2;
      if (this.mc.player != null && !this.mc.options.hudHidden && !((var2 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
         DrawContext var3 = var1.getContext();
         float var4 = HudConfig.a("inventory");
         float var5 = (1.0F - var2) * 4.0F;
         float var6 = this.y + var5;
         float var7 = this.x + 4.0F;
         float var8 = var6 + 13.0F + 3.5F + 4.0F;

         for (int var9 = 0; var9 < 3; var9++) {
            for (int var10 = 0; var10 < 9; var10++) {
               int var11 = 9 + var9 * 9 + var10;
               ItemStack var12 = this.mc.player.getInventory().getStack(var11);
               if (!var12.isEmpty()) {
                  float var13 = var7 + var10 * 13.0F;
                  float var14 = var8 + var9 * 13.0F;
                  if (!ClickGui.a(var13, var14, 11.5F, 11.5F)) {
                     this.a(var3, var12, var13, var14, 11.5F, var4, this.x, var6);
                  }
               }
            }
         }

         HudConfig.c("inventory");
      }
   }

   private void a(DrawContext var1, ItemStack var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = 2.0F / this.mc.getWindow().getScaleFactor();
      Matrix3x2fStack var10 = var1.getMatrices();
      var10.pushMatrix();
      var10.translate(var7 * var9, var8 * var9);
      var10.scale(var6, var6);
      var10.translate((var3 - var7 + var5 * 0.5F) * var9, (var4 - var8 + var5 * 0.5F) * var9);
      float var11 = var5 / 16.0F * var9;
      var10.scale(var11, var11);
      var10.translate(-8.0F, -8.0F);
      var1.drawItem(var2, 0, 0);
      var1.drawStackOverlay(this.mc.textRenderer, var2, 0, 0);
      var10.popMatrix();
   }

   private static int a(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }
}
