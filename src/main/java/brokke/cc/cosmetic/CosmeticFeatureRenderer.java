package brokke.cc.cosmetic;

import brokke.cc.module.render.CosmeticSync;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> var1) {
      super(var1);
   }

   public void render(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, PlayerEntityRenderState var4, float var5, float var6) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      if (var2 == null || var4 == null || var4.spectator || var7.player == null || var7.world == null) {
         return;
      }

      Entity var8 = var7.world.getEntityById(var4.id);
      if (!(var8 instanceof AbstractClientPlayerEntity var9)) {
         return;
      }

      int[] var10;
      if (var9 == var7.player) {
         List<Integer> var11 = LocalCosmetics.e();
         var10 = new int[var11.size()];

         for (int var12 = 0; var12 < var11.size(); var12++) {
            var10[var12] = var11.get(var12);
         }
      } else {
         // Косметика другого юзера чита приходит маячком (CosmeticSync).
         if (!CosmeticSync.showRemote()) {
            return;
         }

         String var20 = var9.getGameProfile() != null ? var9.getGameProfile().name() : var9.getName().getString();
         var10 = RemoteCosmetics.idsFor(var9.getUuid(), var20);
         if (var10 == null) {
            return;
         }
      }

      for (int var21 : var10) {
         if (!"cape".equals(LocalCosmetics.b(var21))) {
            CosmeticModel var22 = LocalCosmetics.g(var21);
            if (var22 != null && var22.getTextureId() != null) {
               RenderLayer var23 = RenderLayers.entityCutoutNoCull(var22.getTextureId());
               AbstractClientPlayerEntity var24 = var9;
               int var25 = var3;
               float var26 = var6;
               var2.submitCustom(var1, var23, (var4x, var5x) -> {
                  MatrixStack var6x = new MatrixStack();
                  var6x.multiplyPositionMatrix(var4x.getPositionMatrix());
                  CosmeticRenderer.a().a(var22, var24, var6x, var5x, var25, this.getContextModel(), var26);
               });
            }
         }
      }
   }
}
