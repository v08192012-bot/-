package brokke.cc.mixins.client;

import brokke.cc.core.AutoBuyEngine;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
   @Inject(method = "openGameMenu(Z)V", at = @At("HEAD"), cancellable = true)
   private void onOpenGameMenu(boolean var1, CallbackInfo var2) {
      if (AutoBuyEngine.a()) {
         var2.cancel();
      }
   }

   @Inject(method = "getWindowTitle()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
   private void brokke$setWindowTitle(CallbackInfoReturnable<String> var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2 != null && var2.getSession() != null && var2.getSession().getUsername() != null && !var2.getSession().getUsername().isBlank()) {
         var1.setReturnValue(var2.getSession().getUsername());
      } else {
         var1.setReturnValue("Shelka new");
      }
   }
}
