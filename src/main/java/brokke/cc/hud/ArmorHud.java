package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.ClickGui;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class ArmorHud {
   private static final ArmorHud INSTANCE = new ArmorHud();
   private static final String ID = "armor";
   private static boolean enabled = false;
   private static final float HEADER_H = 14.0F;
   private static final float ROW_H = 13.0F;
   private static final float ROW_GAP = 2.5F;
   private static final float PAD_X = 5.0F;
   private static final float ITEM_SIZE = 11.0F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
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
   private static float cachedTextH = -1.0F;
   private static final Color SEP_COLOR = new Color(110, 115, 130);
   private static final Color WARN_COLOR = new Color(242, 153, 74);
   private static final Color CRIT_COLOR = new Color(235, 87, 87);
   private static final String[] PERCENT_STRINGS = new String[101];
   private static final String[] COUNT_STRINGS = new String[65];
   private final List<ArmorHud.a> slotPool = new ArrayList<>();
   private final List<ArmorHud.a> currentSlots = new ArrayList<>();

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

         if (var8 == 0) {
            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            this.x = var6 - this.dragDX;
            this.y = var7 - this.dragDY;
         }
      }
   }

   private void d() {
      this.currentSlots.clear();
      if (this.mc.player != null) {
         int var1 = 0;

         for (EquipmentSlot var5 : ARMOR_SLOTS) {
            ItemStack var6 = this.mc.player.getEquippedStack(var5);
            if (!var6.isEmpty()) {
               this.currentSlots.add(this.a(var1++, var6));
            }
         }

         ItemStack var8 = this.mc.player.getMainHandStack();
         if (!var8.isEmpty()) {
            this.currentSlots.add(this.a(var1++, var8));
         }

         ItemStack var9 = this.mc.player.getOffHandStack();
         if (!var9.isEmpty()) {
            this.currentSlots.add(this.a(var1++, var9));
         }
      }
   }

   private ArmorHud.a a(int var1, ItemStack var2) {
      while (this.slotPool.size() <= var1) {
         this.slotPool.add(new ArmorHud.a());
      }

      ArmorHud.a var3 = this.slotPool.get(var1);
      if (var2.isDamageable()) {
         int var4 = var2.getMaxDamage();
         int var5 = var2.getDamage();
         int var6;
         Color var7 = (var6 = Math.clamp((int)Math.round((1.0 - (double)var5 / var4) * 100.0), 0, 100)) > 50
            ? Color.WHITE
            : (var6 > 25 ? WARN_COLOR : CRIT_COLOR);
         var3.a(var2, PERCENT_STRINGS[var6], var7);
      } else if (var2.getCount() > 1) {
         int var8;
         String var9 = (var8 = var2.getCount()) <= 64 ? COUNT_STRINGS[var8] : var8 + "x";
         var3.a(var2, var9, Color.WHITE);
      } else {
         var3.a(var2, "", Color.WHITE);
      }

      return var3;
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.d();
         int var3 = this.currentSlots.isEmpty() && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var3 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var4;
         if (!((var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
            float var5 = (1.0F - var4) * 4.0F;
            if (cachedHeaderIconW < 0.0F) {
               cachedHeaderIconW = Msdf.a(MsdfManager.DICONS, "v", 7.0F);
               cachedHeaderIconH = Msdf.a(MsdfManager.DICONS, 7.0F);
               cachedSepW = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
               cachedSepH = Msdf.a(MsdfManager.MONTSERRAT, 6.5F);
               cachedHeaderTextW = Msdf.a(MsdfManager.BOLD, "Armor", 7.0F);
               cachedHeaderTextH = Msdf.a(MsdfManager.BOLD, 7.0F);
               cachedTextH = Msdf.a(MsdfManager.BOLD, 6.5F);
            }

            float var6 = cachedHeaderIconW;
            float var7 = cachedSepW;
            float var8 = cachedHeaderTextW;
            float var9;
            float var10 = var9 = 5.0F + var6 + 3.5F + var7 + 3.5F + var8 + 5.0F;

            for (ArmorHud.a var12 : this.currentSlots) {
               float var13 = var12.text.isEmpty() ? 0.0F : 3.5F + var7 + 3.5F + Msdf.a(MsdfManager.BOLD, var12.text, 6.5F);
               float var14;
               if ((var14 = 16.0F + var13 + 5.0F) > var10) {
                  var10 = var14;
               }
            }

            float var26 = Math.max(var9, var10);
            float var27 = 14.0F + (this.currentSlots.isEmpty() ? 0.0F : 2.5F + this.currentSlots.size() * 15.5F);
            float var28 = HudConfig.a("armor");
            float var29 = var26 * var28;
            float var15 = var27 * var28;
            if (!this.initPos) {
               this.x = 7.0F;
               this.y = RenderUtil.b() - var15 - 40.0F;
               this.initPos = true;
            }

            this.a(var2, var29, var15);
            HudConfig.c("armor", this.x, this.y, var29, var15);
            float var16 = this.y + var5;
            Matrix4f var17 = RenderUtil.d();
            var17.translate(this.x, var16, 0.0F);
            var17.scale(var28, var28, 1.0F);
            var17.translate(-this.x, -var16, 0.0F);
            Color var18 = Theme.getInstance().getAccent();
            HudConfig.a(var17, "armor", this.x, var16, var9, 14.0F, 5.0F, new Color(0, 0, 0, (int)(180.0F * var4)));
            float var19 = var16 + 14.0F + 2.5F;

            for (ArmorHud.a var21 : this.currentSlots) {
               float var22 = var21.text.isEmpty() ? 0.0F : 3.5F + var7 + 3.5F + Msdf.a(MsdfManager.BOLD, var21.text, 6.5F);
               float var23 = 16.0F + var22 + 5.0F;
               HudConfig.a(var17, this.x, var19, var23, 13.0F, 4.5F, (int)(180.0F * var4) << 24);
               var19 += 15.5F;
            }

            Msdf.b();
            Msdf.a(var17, MsdfManager.DICONS, "v", this.x + 5.0F, var16 + (14.0F - cachedHeaderIconH) / 2.0F, 7.0F, a(var18, 255.0F * var4));
            Msdf.a(var17, MsdfManager.MONTSERRAT, "|", this.x + 5.0F + var6 + 3.5F, var16 + (14.0F - cachedSepH) / 2.0F, 6.5F, a(SEP_COLOR, 160.0F * var4));
            Msdf.a(
               var17,
               MsdfManager.BOLD,
               "Armor",
               this.x + 5.0F + var6 + 3.5F + var7 + 3.5F,
               var16 + (14.0F - cachedHeaderTextH) / 2.0F,
               7.0F,
               a(Color.WHITE, 255.0F * var4)
            );
            float var30 = var16 + 14.0F + 2.5F;

            for (ArmorHud.a var32 : this.currentSlots) {
               if (!var32.text.isEmpty()) {
                  float var33 = this.x + 5.0F + 11.0F + 3.5F;
                  float var34 = var30 + (13.0F - cachedSepH) / 2.0F;
                  Msdf.a(var17, MsdfManager.MONTSERRAT, "|", var33, var34, 6.5F, a(SEP_COLOR, 160.0F * var4));
                  float var24 = var33 + var7 + 3.5F;
                  float var25 = var30 + (13.0F - cachedTextH) / 2.0F;
                  Msdf.a(var17, MsdfManager.BOLD, var32.text, var24, var25, 6.5F, a(var32.textColor, 255.0F * var4));
               }

               var30 += 15.5F;
            }

            Msdf.c();
         }
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (enabled && this.mc.player != null && !this.mc.options.hudHidden && !this.currentSlots.isEmpty()) {
         DrawContext var2 = var1.getContext();
         float var3 = HudConfig.a("armor");
         float var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F);
         float var5 = (1.0F - var4) * 4.0F;
         float var6;
         float var7 = (var6 = this.y + var5) + 14.0F + 2.5F;

         for (ArmorHud.a var9 : this.currentSlots) {
            float var10 = this.x + 5.0F;
            float var11 = var7 + 1.0F;
            if (!ClickGui.a(var10, var11, 11.0F, 11.0F)) {
               this.a(var2, var9.stack, var10, var11, 11.0F, var3, this.x, var6);
            }

            var7 += 15.5F;
         }

         HudConfig.c("armor");
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
      var10.popMatrix();
   }

   private static int a(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRGB() & 16777215;
   }

   static {
      for (int var0 = 0; var0 <= 100; var0++) {
         PERCENT_STRINGS[var0] = var0 + "%";
      }

      for (int var1 = 0; var1 <= 64; var1++) {
         COUNT_STRINGS[var1] = var1 + "x";
      }
   }

   static final class a {
      ItemStack stack;
      String text;
      Color textColor;

      void a(ItemStack var1, String var2, Color var3) {
         this.stack = var1;
         this.text = var2;
         this.textColor = var3;
      }
   }
}
