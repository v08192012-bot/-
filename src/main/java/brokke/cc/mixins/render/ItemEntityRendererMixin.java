package brokke.cc.mixins.render;

import brokke.cc.core.IItemEntityRenderState;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.ItemPhysic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
@Environment(EnvType.CLIENT)
public class ItemEntityRendererMixin {
   @Unique
   private ItemEntityRenderState currentState;

   @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
   private void captureGround(ItemEntity var1, ItemEntityRenderState var2, float var3, CallbackInfo var4) {
      ((IItemEntityRenderState)var2).setOnGround(isRestingOnGround(var1));
   }

   @Unique
   private static boolean isRestingOnGround(ItemEntity var0) {
      if (var0.isOnGround() || var0.verticalCollision) {
         return true;
      }

      if (var0.getEntityWorld() == null) {
         return false;
      }

      double var1 = var0.getVelocity().y;
      if (Math.abs(var1) > 0.15) {
         return false;
      }

      Box var3 = var0.getBoundingBox().offset(0.0, -0.15, 0.0);
      return !var0.getEntityWorld().isSpaceEmpty(var0, var3);
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD")
   )
   private void captureState(ItemEntityRenderState var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5) {
      this.currentState = var1;
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("RETURN")
   )
   private void releaseState(ItemEntityRenderState var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5) {
      this.currentState = null;
   }

   @Redirect(
      method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V")
   )
   private void translate(MatrixStack var1, float var2, float var3, float var4) {
      ItemPhysic var5 = FunctionManager.a(ItemPhysic.class);
      if (var5 != null && var5.isActive()) {
         var3 = 0.0F;
      }

      var1.translate(var2, var3, var4);
   }

   @Redirect(
      method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V")
   )
   private void multiply(MatrixStack var1, Quaternionfc var2) {
      ItemPhysic var3 = FunctionManager.a(ItemPhysic.class);
      if (var3 == null || !var3.isActive()) {
         var1.multiply(var2);
      } else if (this.currentState != null && ((IItemEntityRenderState)(Object)this.currentState).isOnGround()) {
         var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      } else if (this.currentState != null) {
         float var4 = ItemEntity.getRotation(this.currentState.age, this.currentState.uniqueOffset) * 300.0F;
         var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var4));
      } else {
         var1.multiply(var2);
      }
   }
}
