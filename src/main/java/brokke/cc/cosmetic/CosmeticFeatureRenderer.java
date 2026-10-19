package brokke.cc.cosmetic;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> var1) {
      super(var1);
   }

   public void render(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, PlayerEntityRenderState var4, float var5, float var6) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      ClientPlayerEntity var8 = var7.player;
      if (var8 != null && var2 != null && var4 != null && !var4.spectator && var4.id == var8.getId()) {
         List var9 = LocalCosmetics.e();
         if (!var9.isEmpty()) {
            for (Integer var11 : (Iterable<Integer>)(Iterable<?>)(var9)) {
               if (var11 != null && !"cape".equals(LocalCosmetics.b(var11))) {
                  CosmeticModel var12 = LocalCosmetics.g(var11);
                  if (var12 != null && var12.getTextureId() != null) {
                     RenderLayer var13 = RenderLayers.entityCutoutNoCull(var12.getTextureId());
                     var2.submitCustom(var1, var13, (var5x, var6xx) -> {
                        MatrixStack var7x = new MatrixStack();
                        var7x.multiplyPositionMatrix(var5x.getPositionMatrix());
                        CosmeticRenderer.a().a(var12, var8, var7x, var6xx, var3, this.getContextModel(), var6);
                     });
                  }
               }
            }
         }
      }
   }
}
