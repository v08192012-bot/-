package brokke.cc.mixins.render;

import brokke.cc.core.AutoBuyEngine;
import net.minecraft.client.option.InactivityFpsLimiter;
import net.minecraft.client.option.InactivityFpsLimiter.LimitReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public class InactivityFpsLimiterMixin {
   @Shadow
   private int maxFps;

   @Inject(method = "getLimitReason()Lnet/minecraft/client/option/InactivityFpsLimiter$LimitReason;", at = @At("RETURN"), cancellable = true)
   private void keepFpsWhileIdle(CallbackInfoReturnable<LimitReason> callback) {
      LimitReason reason = (LimitReason)callback.getReturnValue();
      if (reason == LimitReason.SHORT_AFK || reason == LimitReason.LONG_AFK) {
         callback.setReturnValue(LimitReason.NONE);
      }
   }

   @Inject(method = "update()I", at = @At("HEAD"), cancellable = true)
   private void onUpdate(CallbackInfoReturnable<Integer> var1) {
      if (AutoBuyEngine.a()) {
         var1.setReturnValue(Math.max(60, this.maxFps));
      }
   }

   @Inject(method = "shouldDisableProfilerTimeout()Z", at = @At("HEAD"), cancellable = true)
   private void onShouldDisableProfilerTimeout(CallbackInfoReturnable<Boolean> var1) {
      if (AutoBuyEngine.a()) {
         var1.setReturnValue(true);
      }
   }
}
