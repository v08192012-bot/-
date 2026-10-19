package brokke.cc.mixins.render;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.TextFactoryEvent;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
   @ModifyVariable(
      method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private static String adjustText(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         TextFactoryEvent var1 = new TextFactoryEvent(var0);
         EventBus.post(var1);
         return var1.getText();
      } else {
         return var0;
      }
   }
}
