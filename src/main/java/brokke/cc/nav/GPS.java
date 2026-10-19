package brokke.cc.nav;

import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.ui.ClickGui;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public class GPS {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final ItemStack COMPASS_STACK = new ItemStack(Items.COMPASS);
   private static final float FONT_SIZE = 6.75F;
   private static final float ICON_SIZE = 9.5F;
   private static final float PAD_X = 5.0F;
   private static final float SEP_PAD = 3.5F;
   private static final float GAP = 3.5F;
   private static final float BOX_H = 14.5F;
   private static final float RADIUS = 5.0F;

   public static void a() {
      EventBus.register(new GPS());
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         List var2 = WaypointStorage.getInstance().getWaypoints();
         if (!var2.isEmpty()) {
            DrawContext var3 = var1.getContext();
            Color var4 = Theme.getInstance().getAccent();
            int var5 = var4.getAlpha() << 24 | var4.getRed() << 16 | var4.getGreen() << 8 | var4.getBlue();

            for (Waypoint var7 : (Iterable<Waypoint>)(Iterable<?>)(var2)) {
               Vec3d var8 = new Vec3d(var7.getX() + 0.5, var7.getY() + 1.0, var7.getZ() + 0.5);
               float[] var9 = WorldToScreen.a(var8);
               if (var9 != null) {
                  double var10 = Math.sqrt(mc.player.squaredDistanceTo(var8));
                  String var12 = Math.round(var10) + "м";
                  String var13 = var7.getName() != null && !var7.getName().isBlank() ? var7.getName() : "GPS";
                  this.a(var3, var9[0], var9[1], var13, var12, var10, var5);
               }
            }
         }
      }
   }

   private void a(DrawContext var1, float var2, float var3, String var4, String var5, double var6, int var8) {
      float var9 = (float)MathHelper.clamp(1.0 - var6 / 150.0, 0.75, 1.0);
      float var10 = Msdf.a(MsdfManager.BOLD, var4, 6.75F);
      float var11 = Msdf.a(MsdfManager.BOLD, var5, 6.75F);
      float var12 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
      float var13 = 18.0F + var12 + 3.5F + var10 + 3.5F + var11 + 5.0F;
      float var14 = var2 - var13 / 2.0F;
      float var15 = var3 - 14.5F;
      if (!ClickGui.a(var2 - var13 * var9 / 2.0F, var3 - 14.5F * var9, var13 * var9, 14.5F * var9)) {
         Matrix4f var16 = this.a(var2, var3, var9);
         HudConfig.a(var16, "gps", var14, var15, var13, 14.5F, 5.0F, -1275068416);
         float var17 = var14 + 5.0F;
         float var18 = var15 + 2.5F;
         float var19 = 2.0F / mc.getWindow().getScaleFactor();
         float var20 = 0.59375F * var9 * var19;
         float var21 = var2 + (var17 + 4.75F - var2) * var9;
         float var22 = var3 + (var18 + 4.75F - var3) * var9;
         Matrix3x2fStack var23 = var1.getMatrices();
         var23.pushMatrix();
         var23.translate(var21 * var19, var22 * var19);
         var23.scale(var20, var20);
         var23.translate(-8.0F, -8.0F);

         try {
            var1.drawItem(COMPASS_STACK, 0, 0);
         } catch (Exception var27) {
         }

         var23.popMatrix();
         float var24 = var17 + 9.5F + 3.5F;
         float var25 = var15 + (14.5F - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
         Msdf.a(var16, MsdfManager.MONTSERRAT, "|", var24, var25, 6.5F, -1603374206);
         var24 += var12 + 3.5F;
         float var26 = var15 + (14.5F - Msdf.a(MsdfManager.BOLD, 6.75F)) / 2.0F;
         Msdf.a(var16, MsdfManager.BOLD, var4, var24, var26, 6.75F, -1);
         var24 += var10 + 3.5F;
         Msdf.a(var16, MsdfManager.BOLD, var5, var24, var26, 6.75F, var8);
      }
   }

   private Matrix4f a(float var1, float var2, float var3) {
      Matrix4f var4 = RenderUtil.d();
      var4.translate(var1, var2, 0.0F);
      var4.scale(var3, var3, 1.0F);
      var4.translate(-var1, -var2, 0.0F);
      return var4;
   }
}
