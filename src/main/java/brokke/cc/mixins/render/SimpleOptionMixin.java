package brokke.cc.mixins.render;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
   @Shadow
   @Final
   Text text;
   @Shadow
   T value;

   @Inject(method = "setValue(Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
   public void setGammaValue(T var1, CallbackInfo var2) {
      if (this.text.equals(Text.translatable("options.gamma"))) {
         this.value = var1;
         var2.cancel();
      }
   }
}
