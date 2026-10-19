package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.particle.BillboardParticleSubmittable.Buffers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.LayeredCustomCommandRenderer.VerticesCache;
import net.minecraft.client.render.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BillboardParticleSubmittable.class)
public abstract class BillboardParticleSubmittableMixin {
   @Shadow
   private int particles;

   @Shadow
   public abstract void onFrameEnd();

   @Inject(method = "submit(Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
   private void onSubmitQueue(OrderedRenderCommandQueue var1, CameraRenderState var2, CallbackInfo var3) {
      if (NoRender.q()) {
         this.onFrameEnd();
         var3.cancel();
      }
   }

   @Inject(method = "submit(Lnet/minecraft/client/render/command/LayeredCustomCommandRenderer$VerticesCache;)Lnet/minecraft/client/particle/BillboardParticleSubmittable$Buffers;", at = @At("HEAD"), cancellable = true)
   private void onSubmit(VerticesCache var1, CallbackInfoReturnable<Buffers> var2) {
      if (NoRender.q() || this.particles <= 0 || this.particles * 4L >= 16777215L || this.particles > 100000) {
         this.onFrameEnd();
         var2.setReturnValue(null);
      }
   }
}
