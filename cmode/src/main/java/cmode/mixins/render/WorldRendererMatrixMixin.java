package cmode.mixins.render;

import cmode.render.RenderContext;
import cmode.system.events.EventBus;
import cmode.system.events.impl.Render3DEvent;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMatrixMixin {
   @Unique
   private Camera cmode_capturedCamera;
   @Unique
   private float cmode_capturedTickDelta;

   @Inject(
      method = "render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
      at = @At("HEAD")
   )
   private void captureWorldMatrices(
      ObjectAllocator var1,
      RenderTickCounter var2,
      boolean var3,
      Camera var4,
      Matrix4f var5,
      Matrix4f var6,
      Matrix4f var7,
      GpuBufferSlice var8,
      Vector4f var9,
      boolean var10,
      CallbackInfo var11
   ) {
      RenderContext.a(var5, var6, var9);
      this.cmode_capturedCamera = var4;
      this.cmode_capturedTickDelta = var2.getTickProgress(true);
      
   }

   @Inject(
      method = "render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
      at = @At("RETURN")
   )
   private void fireRender3DEvent(
      ObjectAllocator var1,
      RenderTickCounter var2,
      boolean var3,
      Camera var4,
      Matrix4f var5,
      Matrix4f var6,
      Matrix4f var7,
      GpuBufferSlice var8,
      Vector4f var9,
      boolean var10,
      CallbackInfo var11
   ) {
      if (this.cmode_capturedCamera != null) {
         MatrixStack var12 = new MatrixStack();
         var12.multiplyPositionMatrix(var5);
         EventBus.post(new Render3DEvent(var12, this.cmode_capturedTickDelta, this.cmode_capturedCamera));
         this.cmode_capturedCamera = null;
      }
   }
}
