package brokke.cc.mixins.gui;

import brokke.cc.ui.MainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
   @Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
   private void onInit(CallbackInfo var1) {
      MinecraftClient.getInstance().setScreen(new MainMenuScreen());
      var1.cancel();
   }
}
