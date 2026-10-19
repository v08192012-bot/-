package cmode.mixins.network;

import cmode.manager.CommandManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
   @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
   private void cmodeOnSendChatMessage(String var1, CallbackInfo var2) {
      if (CommandManager.handle(var1)) {
         var2.cancel();
      }
   }
}
