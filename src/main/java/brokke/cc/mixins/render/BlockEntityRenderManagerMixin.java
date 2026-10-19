package brokke.cc.mixins.render;

import brokke.cc.module.misc.Optimizer;
import brokke.cc.module.render.ExstraOptimizer;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderManager.class)
public class BlockEntityRenderManagerMixin {
   @Inject(
      method = "render(Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private <S extends BlockEntityRenderState> void onRender(S var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5) {
      if (Optimizer.a(var1) || ExstraOptimizer.cullBlockEntity(var1)) {
         var5.cancel();
      }
   }
}
