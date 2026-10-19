package brokke.cc.mixins.render;

import brokke.cc.module.movement.FreeCam;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.NoRender;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
   @Inject(method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V", at = @At("HEAD"), cancellable = true)
   private static void onRenderFireOverlay(MatrixStack var0, VertexConsumerProvider var1, Sprite var2, CallbackInfo var3) {
      if (NoRender.l()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderInWallOverlay(Lnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
   private static void onRenderInWallOverlay(Sprite var0, MatrixStack var1, VertexConsumerProvider var2, CallbackInfo var3) {
      if (NoRender.m() || NoRender.n() || FunctionManager.a(FreeCam.class) != null && FunctionManager.a(FreeCam.class).isActive()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderFloatingItem(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderFloatingItem(MatrixStack var1, float var2, OrderedRenderCommandQueue var3, CallbackInfo var4) {
      if (NoRender.u()) {
         var4.cancel();
      }
   }
}
