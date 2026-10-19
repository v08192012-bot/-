package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.mixins.interfaces.IItemCooldownEntry;
import brokke.cc.mixins.interfaces.IItemCooldownManager;
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
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class CoolDownsHud {
   private static final CoolDownsHud INSTANCE = new CoolDownsHud();
   private static final String ID = "cooldowns";
   private static boolean enabled = false;
   private static final float PAD_X = 6.0F;
   private static final float PAD_Y = 5.0F;
   private static final float ROW_H = 11.5F;
   private static final float ROW_GAP = 2.5F;
   private static final float ITEM_SIZE = 9.5F;
   private static final float ITEM_GAP = 4.0F;
   private static final float MIN_TIME_GAP = 10.0F;
   private static final float TIMER_ICON_SIZE = 7.5F;
   private static final float TIMER_GAP = 6.5F;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private float x = 7.0F;
   private float y = 140.0F;
   private float targetX = 7.0F;
   private float targetY = 140.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private final List<CoolDownsHud.a> currentItems = new ArrayList<>();

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
            float[] var9 = HudConfig.b("cooldowns", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.targetX = var9[0];
            this.targetY = var9[1];
         }

         if (this.dragging || Math.abs(this.targetX - this.x) > 0.05F || Math.abs(this.targetY - this.y) > 0.05F) {
            float[] var10 = HudConfig.a("cooldowns", this.x, this.y, this.targetX, this.targetY);
            this.x = var10[0];
            this.y = var10[1];
         }
      }
   }

   private String a(int var1) {
      int var2;
      int var3 = (var2 = (var1 + 19) / 20) / 60;
      int var4 = var2 % 60;
      return var3 + ":" + (var4 < 10 ? "0" + var4 : String.valueOf(var4));
   }

   private void d() {
      this.currentItems.clear();
      if (this.mc.player != null) {
         if (this.mc.player.getItemCooldownManager() instanceof IItemCooldownManager var1) {
            Map var15 = var1.getEntries();
            int var3 = var1.getTick();
            if (var15 != null && !var15.isEmpty()) {
               for (Entry var5 : (Iterable<Entry>)(Iterable<?>)(var15.entrySet())) {
                  int var21;
                  if (var5.getValue() instanceof IItemCooldownEntry var6 && (var21 = var6.getEndTick() - var3) > 0) {
                     Identifier var8 = (Identifier)var5.getKey();
                     ItemStack var9 = null;

                     for (int var10 = 0; var10 < this.mc.player.getInventory().size(); var10++) {
                        ItemStack var11 = this.mc.player.getInventory().getStack(var10);
                        if (!var11.isEmpty() && var8.equals(this.mc.player.getItemCooldownManager().getGroup(var11))) {
                           var9 = var11;
                           break;
                        }
                     }

                     if (var9 == null && Registries.ITEM.containsId(var8)) {
                        Item var22 = Registries.ITEM.get(var8);
                        var9 = var22.getDefaultStack();
                     }

                     if (var9 == null) {
                        var9 = Items.BARRIER.getDefaultStack();
                     }

                     String var23 = var9.getName().getString();
                     String var24 = this.a(var21);
                     float var12 = Msdf.a(MsdfManager.BOLD, var23, 6.5F);
                     float var13 = Msdf.a(MsdfManager.BOLD, var24, 6.5F);
                     this.currentItems.add(new CoolDownsHud.a(var9, var23, var24, var12, var13));
                  }
               }
            }
         }

         if (this.currentItems.isEmpty() && this.mc.currentScreen instanceof ChatScreen) {
            ItemStack var14 = Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack();
            String var16 = var14.getName().getString();
            String var17 = "1:15";
            this.currentItems.add(new CoolDownsHud.a(var14, var16, var17, Msdf.a(MsdfManager.BOLD, var16, 6.5F), Msdf.a(MsdfManager.BOLD, var17, 6.5F)));
            ItemStack var18 = Items.GOLDEN_APPLE.getDefaultStack();
            String var19 = var18.getName().getString();
            String var20 = "0:06";
            this.currentItems.add(new CoolDownsHud.a(var18, var19, var20, Msdf.a(MsdfManager.BOLD, var19, 6.5F), Msdf.a(MsdfManager.BOLD, var20, 6.5F)));
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.d();
         int var3 = this.currentItems.isEmpty() && !(var2.currentScreen instanceof ChatScreen) ? 0 : 1;
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var3 != 0 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var4;
         if (!((var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
            float var5 = (1.0F - var4) * 4.0F;
            float var6 = Msdf.a(MsdfManager.DICONS, "s", 7.5F);
            float var7 = Msdf.a(MsdfManager.DICONS, 7.5F);
            float var8 = 0.0F;

            for (CoolDownsHud.a var10 : this.currentItems) {
               float var11;
               if ((var11 = 13.5F + var10.nameW + 10.0F + var10.timeW) > var8) {
                  var8 = var11;
               }
            }

            float var30 = 6.0F + var6 + 6.5F + var8 + 6.0F;
            int var31 = Math.max(1, this.currentItems.size());
            float var32 = 10.0F + var31 * 11.5F + (var31 - 1) * 2.5F;
            float var12 = HudConfig.a("cooldowns");
            float var13 = var30 * var12;
            float var14 = var32 * var12;
            if (!this.initPos) {
               this.x = 7.0F;
               this.y = 140.0F;
               this.targetX = 7.0F;
               this.targetY = 140.0F;
               this.initPos = true;
            }

            this.a(var2, var13, var14);
            HudConfig.c("cooldowns", this.x, this.y, var13, var14);
            float var15 = this.y + var5;
            Matrix4f var16 = RenderUtil.d();
            var16.translate(this.x, var15, 0.0F);
            var16.scale(var12, var12, 1.0F);
            var16.translate(-this.x, -var15, 0.0F);
            Color var17 = Theme.getInstance().getAccent();
            int var18 = (int)(180.0F * var4);
            HudConfig.a(var16, "cooldowns", this.x, var15, var30, var32, 6.0F, var18 << 24);
            float var19 = this.x + 6.0F;
            float var20 = var15 + (var32 - var7) / 2.0F;
            Msdf.b();
            Msdf.a(var16, MsdfManager.DICONS, "s", var19, var20, 7.5F, a(var17, 255.0F * var4));
            float var21 = var19 + var6 + 6.5F;
            float var22 = this.x + var30 - 6.0F;
            float var23 = Msdf.a(MsdfManager.BOLD, 6.5F);

            for (int var24 = 0; var24 < this.currentItems.size(); var24++) {
               CoolDownsHud.a var25 = this.currentItems.get(var24);
               float var27 = var15 + 5.0F + var24 * 14.0F + (11.5F - var23) / 2.0F;
               float var28 = var21 + 9.5F + 4.0F;
               Msdf.a(var16, MsdfManager.BOLD, var25.name, var28, var27, 6.5F, a(Color.WHITE, 255.0F * var4));
               float var29 = var22 - var25.timeW;
               Msdf.a(var16, MsdfManager.BOLD, var25.timeText, var29, var27, 6.5F, a(var17, 255.0F * var4));
            }

            Msdf.c();
         }
      } else {
         this.currentItems.clear();
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      float var2;
      if (enabled
         && this.mc.player != null
         && !this.mc.options.hudHidden
         && !this.currentItems.isEmpty()
         && !((var2 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
         DrawContext var3 = var1.getContext();
         float var4 = HudConfig.a("cooldowns");
         float var5 = (1.0F - var2) * 4.0F;
         float var6 = this.y + var5;
         float var7 = Msdf.a(MsdfManager.DICONS, "s", 7.5F);
         float var8 = this.x + 6.0F + var7 + 6.5F;

         for (int var9 = 0; var9 < this.currentItems.size(); var9++) {
            CoolDownsHud.a var10 = this.currentItems.get(var9);
            float var11 = var8;
            float var13 = var6 + 5.0F + var9 * 14.0F + 1.0F;
            if (!ClickGui.a(var11, var13, 9.5F, 9.5F)) {
               this.a(var3, var10.stack, var11, var13, 9.5F, var4, this.x, var6);
            }
         }

         HudConfig.c("cooldowns");
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

   public static final class a {
      public final ItemStack stack;
      public final String name;
      public final String timeText;
      public final float nameW;
      public final float timeW;

      public a(ItemStack var1, String var2, String var3, float var4, float var5) {
         this.stack = var1;
         this.name = var2;
         this.timeText = var3;
         this.nameW = var4;
         this.timeW = var5;
      }
   }
}
