package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
   private void onRender(DrawContext var1, CallbackInfo var2) {
      if (NoRender.w()) {
         var2.cancel();
      }
   }
}
