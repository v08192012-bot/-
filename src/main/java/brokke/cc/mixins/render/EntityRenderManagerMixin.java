package brokke.cc.mixins.render;

import brokke.cc.module.misc.Optimizer;
import brokke.cc.module.render.ExstraOptimizer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderManager.class)
public class EntityRenderManagerMixin {
   @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
   private <E extends Entity> void onShouldRender(E var1, Frustum var2, double var3, double var5, double var7, CallbackInfoReturnable<Boolean> var9) {
      if (Optimizer.a(var1, var3, var5, var7) || ExstraOptimizer.cullEntity(var1, var3, var5, var7)) {
         var9.setReturnValue(false);
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private <S extends EntityRenderState> void onRender(
      S var1, CameraRenderState var2, double var3, double var5, double var7, MatrixStack var9, OrderedRenderCommandQueue var10, CallbackInfo var11
   ) {
      if (Optimizer.a(var1, var3, var5, var7) || ExstraOptimizer.cullEntityState(var1, var3, var5, var7)) {
         var11.cancel();
      }
   }
}
