package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlindnessEffectFogModifier.class)
public class BlindnessFogModifierMixin {
   @Inject(method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void onApplyStartEndModifier(FogData var1, Camera var2, ClientWorld var3, float var4, RenderTickCounter var5, CallbackInfo var6) {
      if (NoRender.z()) {
         var6.cancel();
      }
   }

   @Inject(method = "applyDarknessModifier(Lnet/minecraft/entity/LivingEntity;FF)F", at = @At("HEAD"), cancellable = true)
   private void onApplyDarknessModifier(LivingEntity var1, float var2, float var3, CallbackInfoReturnable<Float> var4) {
      if (NoRender.z()) {
         var4.setReturnValue(var2);
      }
   }
}
