package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.SubmittableBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
   @Inject(method = "addToBatch(Lnet/minecraft/client/render/SubmittableBatch;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;F)V", at = @At("HEAD"), cancellable = true)
   private void onAddToBatch(SubmittableBatch var1, Frustum var2, Camera var3, float var4, CallbackInfo var5) {
      if (NoRender.q()) {
         var5.cancel();
      }
   }
}
