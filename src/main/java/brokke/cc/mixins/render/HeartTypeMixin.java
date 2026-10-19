package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud$HeartType")
public class HeartTypeMixin {
   @Inject(method = "fromPlayerState(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/client/gui/hud/InGameHud$HeartType;", at = @At("HEAD"), cancellable = true)
   private static void onFromPlayerState(PlayerEntity var0, CallbackInfoReturnable<Object> var1) {
      if (NoRender.y()) {
         Object var2 = var1.getReturnValue();
         if (var2 instanceof Enum var3 && "WITHERED".equals(var3.name())) {
            var1.setReturnValue(Enum.valueOf(var3.getDeclaringClass(), "NORMAL"));
         }
      }
   }
}
