package cmode.mixins.input;

import cmode.ui.ClickGui;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("HEAD"), cancellable = true)
   private void cmodeOnKey(long var1, int var3, KeyInput var4, CallbackInfo var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      if (var3 == 1 && var4.key() == GLFW.GLFW_KEY_SLASH && var6.currentScreen == null) {
         var6.setScreen(ClickGui.INSTANCE);
         var5.cancel();
      }
   }
}
