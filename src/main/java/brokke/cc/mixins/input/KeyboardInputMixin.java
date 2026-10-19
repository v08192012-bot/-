package brokke.cc.mixins.input;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardInputMixin {
   @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("HEAD"))
   private void onKey(long var1, int var3, KeyInput var4, CallbackInfo var5) {
      EventBus.post(new KeyEvent(var3, var4.key()));
   }
}
