package brokke.cc.mixins.render;

import brokke.cc.core.AutoBuyEngine;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
   @Inject(method = "isMinimized()Z", at = @At("HEAD"), cancellable = true)
   private void onIsMinimized(CallbackInfoReturnable<Boolean> var1) {
      if (AutoBuyEngine.a()) {
         var1.setReturnValue(false);
      }
   }
}
