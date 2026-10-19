package brokke.cc.nav;

import brokke.cc.render.ColorPipeline;
import brokke.cc.render.Render3DUtil;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render3DEvent;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PathRenderer {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private boolean enabled = true;

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.enabled && this.mc.player != null && this.mc.world != null) {
         List var2 = NavigationManager.getInstance().j();
         if (var2 != null && !var2.isEmpty()) {
            int var3 = NavigationManager.getInstance().getMovementController().getCurrentIndex();
            Color var4 = Theme.getInstance().getAccent();
            int var5 = var4.getAlpha() << 24 | var4.getRed() << 16 | var4.getGreen() << 8 | var4.getBlue();
            int var6 = 1619034240;
            BufferBuilder var7 = Render3DUtil.a();
            if (var7 != null) {
               try {
                  for (PathCurve.a var10 : (Iterable<PathCurve.a>)(Iterable<?>)(PathCurve.a(var2, 4))) {
                     float[][] var11 = WorldToScreen.a(var10.getStart(), var10.getEnd());
                     if (var11 != null) {
                        int var12 = var10.getNodeIndex() < var3 ? var6 : var5;
                        ColorPipeline.a(var7, var11[0][0], var11[0][1], var11[1][0], var11[1][1], 2.5F, var12);
                     }
                  }

                  if (!var2.isEmpty()) {
                     BlockPos var16 = ((PathNode)var2.get(var2.size() - 1)).getPos();
                     Vec3d var17 = new Vec3d(var16.getX() + 0.5, var16.getY() + 0.5, var16.getZ() + 0.5);
                     Render3DUtil.a(var7, var17, 1.0F, var4, 2.0F);
                  }
               } finally {
                  Render3DUtil.a(var7);
               }
            }
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean var1) {
      this.enabled = var1;
   }
}
