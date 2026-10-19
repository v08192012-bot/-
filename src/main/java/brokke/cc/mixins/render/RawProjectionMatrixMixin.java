package brokke.cc.mixins.render;

import brokke.cc.render.RenderContext;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.RawProjectionMatrix;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RawProjectionMatrix.class)
public class RawProjectionMatrixMixin {
   @Inject(method = "set(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"))
   private void onSet(Matrix4f var1, CallbackInfoReturnable<GpuBufferSlice> var2) {
      if (var1 != null) {
         RenderContext.WORLD_PROJECTION.set(var1);
         RenderContext.VALID = true;
      }
   }
}
