package cmode.mixins.render;

import cmode.font.MsdfManager;
import cmode.render.BlurPipeline;
import cmode.render.GuiBatch;
import cmode.render.LiquidGlassPipeline;
import cmode.system.events.EventBus;
import cmode.system.events.impl.Render2DEvent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V", at = @At("HEAD"))
   private void cmodeOnRenderHead(RenderTickCounter var1, boolean var2, CallbackInfo var3) {
      GuiBatch.beginFrame();
      BlurPipeline.b();
      LiquidGlassPipeline.b();
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void cmodeOnRenderWorld(RenderTickCounter var1, CallbackInfo var2) {
      MsdfManager.a();
      GuiBatch.begin();

      try {
         EventBus.post(Render2DEvent.get(var1));
      } finally {
         GuiBatch.end();
      }
   }
}
