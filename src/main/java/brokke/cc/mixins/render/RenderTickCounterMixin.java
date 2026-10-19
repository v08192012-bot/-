package brokke.cc.mixins.render;

import brokke.cc.manager.TimerController;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dynamic.class)
public class RenderTickCounterMixin {
   @Shadow
   private float dynamicDeltaTicks;

   @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", shift = Shift.AFTER))
   private void brokke$multiplyDeltaTicks(long var1, CallbackInfoReturnable<Integer> var3) {
      this.dynamicDeltaTicks = this.dynamicDeltaTicks * TimerController.getTimerSpeed();
   }
}
