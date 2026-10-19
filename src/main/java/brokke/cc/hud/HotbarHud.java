package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.player.AuctionHelper;
import brokke.cc.module.FunctionManager;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public class HotbarHud {
   private static final HotbarHud INSTANCE = new HotbarHud();
   private static final String ID = "hotbar";
   private static boolean enabled = false;
   private static final float PAD = 2.0F;
   private static final float SLOT = 18.0F;
   private static final float ITEM = 15.0F;
   private static final float GAP = 2.0F;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation selectAnim = new Animation();
   private final Animation offhandAnim = new Animation();
   private float x;
   private float y;
   private float panelW;
   private float panelH;
   private final float[] slotX = new float[9];
   private float slotY;
   private float offX;
   private float offY;
   private float offSlotX;
   private float offSlotY;
   private float offW;
   private float offH;

   public static float[] a() {
      float var0 = 182.0F;
      float var1 = 22.0F;
      return new float[]{(RenderUtil.a() - var0) * 0.5F, RenderUtil.b() - var1};
   }

   public static void a(float var0, float var1) {
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

   private void d() {
      this.panelW = 182.0F;
      this.panelH = 22.0F;
      this.x = (RenderUtil.a() - this.panelW) * 0.5F;
      this.y = RenderUtil.b() - this.panelH;
      this.slotY = this.y + 2.0F;

      for (int var1 = 0; var1 < 9; var1++) {
         this.slotX[var1] = this.x + 2.0F + var1 * 20.0F;
      }

      this.offW = 22.0F;
      this.offH = this.panelH;
      float var3 = 4.0F;
      int var2 = this.mc.player != null && this.mc.player.getMainArm() == Arm.LEFT ? 1 : 0;
      this.offX = var2 != 0 ? this.x + this.panelW + var3 : this.x - this.offW - var3;
      this.offY = this.y;
      this.offSlotX = this.offX + 2.0F;
      this.offSlotY = this.slotY;
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.d();
         boolean var3;
         if (var3 = var2.currentScreen instanceof ChatScreen) {
            HudConfig.HUD_BOUNDS.put("hotbar", new float[]{this.x, this.y, this.panelW, this.panelH});
         } else {
            HudConfig.HUD_BOUNDS.remove("hotbar");
            HudConfig.HUD_BOUNDS.remove("hotbar_offhand");
         }

         Matrix4f var4 = RenderUtil.d();
         Color var5 = Theme.getInstance().getAccent();
         HudConfig.a(var4, "hotbar", this.x, this.y, this.panelW, this.panelH - 1.0F, 6.0F, -1358954496);
         ItemStack var6 = var2.player.getOffHandStack();
         int var7 = var6.isEmpty() && !var3 ? 0 : 1;
         this.offhandAnim.setEasing(Easing.SPRING_SOFT);
         this.offhandAnim.animate(var7 != 0 ? 1.0F : 0.0F, 0.0F, 6.0F);
         float var8;
         if ((var8 = Math.clamp(this.offhandAnim.getValue(), 0.0F, 1.0F)) > 0.001F) {
            if (var3) {
               HudConfig.HUD_BOUNDS.put("hotbar_offhand", new float[]{this.offX, this.offY, this.offW, this.offH});
            }

            HudConfig.a(var4, "hotbar", this.offX, this.offY, this.offW, this.panelH - 1.0F, 6.0F, (int)(175.0F * var8) << 24);
            RenderUtil.a(var4, this.offSlotX, this.offSlotY, 18.0F, 18.0F, 4.0F, (int)(90.0F * var8) << 24);
         }

         int var9 = 1509949440;

         for (int var10 = 0; var10 < 9; var10++) {
            RenderUtil.a(var4, this.slotX[var10], this.slotY, 18.0F, 18.0F, 4.0F, var9);
         }

         int var22 = Math.max(0, Math.min(8, var2.player.getInventory().getSelectedSlot()));
         this.selectAnim.setEasing(Easing.SPRING_SOFT);
         this.selectAnim.animate(var22, 0.0F, 12.0F);
         float var11 = this.selectAnim.getValue();
         float var12 = this.x + 2.0F + var11 * 20.0F;
         int var13 = 1174405120 | var5.getRGB() & 16777215;
         RenderUtil.a(var4, var12, this.slotY, 18.0F, 18.0F, 4.0F, var13);
         RenderUtil.a(var4, var12 + 9.0F - 5.0F, this.slotY + 18.0F - 1.2F, 10.0F, 1.4F, 0.7F, var5.getRGB());
         Msdf.b();

         for (int var14 = 0; var14 < 9; var14++) {
            ItemStack var15 = var2.player.getInventory().getStack(var14);
            if (var15.isEmpty()) {
               String var16 = String.valueOf(var14 + 1);
               float var17 = Msdf.a(MsdfManager.BOLD, var16, 7.0F);
               float var18 = Msdf.a(MsdfManager.BOLD, 7.0F);
               float var19 = this.slotX[var14] + (18.0F - var17) * 0.5F;
               float var20 = this.slotY + (18.0F - var18) * 0.5F;
               int var21 = var14 == var22 ? -587202561 : 1862270975;
               Msdf.a(var4, MsdfManager.BOLD, var16, var19, var20, 7.0F, var21);
            }
         }

         Msdf.c();
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (enabled && this.mc.player != null && !this.mc.options.hudHidden) {
         this.d();
         DrawContext var2 = var1.getContext();

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = this.mc.player.getInventory().getStack(var3);
            if (!var4.isEmpty()) {
               float var5 = this.slotX[var3] + 1.5F;
               float var6 = this.slotY + 1.5F;
               if (!ClickGui.a(var5, var6, 15.0F, 15.0F)) {
                  this.a(var2, var4, var5, var6, 15.0F);
               }
            }
         }

         ItemStack var7 = this.mc.player.getOffHandStack();
         if (!var7.isEmpty()) {
            float var8 = this.offSlotX + 1.5F;
            float var9 = this.offSlotY + 1.5F;
            if (!ClickGui.a(var8, var9, 15.0F, 15.0F)) {
               this.a(var2, var7, var8, var9, 15.0F);
            }
         }
      }
   }

   private void a(DrawContext var1, ItemStack var2, float var3, float var4, float var5) {
      float var6 = 2.0F / this.mc.getWindow().getScaleFactor();
      Matrix3x2fStack var7 = var1.getMatrices();
      var7.pushMatrix();
      var7.translate((var3 + var5 * 0.5F) * var6, (var4 + var5 * 0.5F) * var6);
      float var8 = var5 / 16.0F * var6;
      var7.scale(var8, var8);
      var7.translate(-8.0F, -8.0F);
      var1.drawItem(var2, 0, 0);
      var1.drawStackOverlay(this.mc.textRenderer, var2, 0, 0, "");
      String var9 = null;
      AuctionHelper var10 = FunctionManager.a(AuctionHelper.class);
      if (var10 != null) {
         var9 = var10.a(var2);
      }

      if (var9 == null && var2.getCount() > 1) {
         var9 = String.valueOf(var2.getCount());
      }

      if (var9 != null) {
         int var11 = this.mc.textRenderer.getWidth(var9);
         int var12 = (16 - var11) / 2;
         byte var13 = 9;
         var1.drawText(this.mc.textRenderer, var9, var12, var13, -1, true);
      }

      var7.popMatrix();
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
   }

   private static int b(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }
}
