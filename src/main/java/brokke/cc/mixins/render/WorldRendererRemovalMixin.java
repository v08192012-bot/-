package brokke.cc.mixins.render;

import brokke.cc.module.render.NoRender;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.SubmittableBatch;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererRemovalMixin {
   @Shadow
   private SubmittableBatch particleBatch;

   @Inject(method = "renderParticles(Lnet/minecraft/client/render/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderParticles(FrameGraphBuilder var1, GpuBufferSlice var2, CallbackInfo var3) {
      if (NoRender.q()) {
         if (this.particleBatch != null) {
            this.particleBatch.onFrameEnd();
         }

         var3.cancel();
      }
   }

   @Inject(method = "renderWeather(Lnet/minecraft/client/render/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderWeather(FrameGraphBuilder var1, GpuBufferSlice var2, CallbackInfo var3) {
      if (NoRender.r()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderClouds(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/option/CloudRenderMode;Lnet/minecraft/util/math/Vec3d;JFIF)V", at = @At("HEAD"), cancellable = true)
   private void onRenderClouds(FrameGraphBuilder var1, CloudRenderMode var2, Vec3d var3, long var4, float var6, int var7, float var8, CallbackInfo var9) {
      if (NoRender.s()) {
         var9.cancel();
      }
   }

   @Inject(method = "renderBlockEntities(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderBlockEntities(MatrixStack var1, WorldRenderState var2, OrderedRenderCommandQueueImpl var3, CallbackInfo var4) {
      if (NoRender.t()) {
         var4.cancel();
      }
   }

   @Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
   private void onHasBlindnessOrDarkness(Camera var1, CallbackInfoReturnable<Boolean> var2) {
      if (NoRender.z()) {
         var2.setReturnValue(false);
      }
   }
}
