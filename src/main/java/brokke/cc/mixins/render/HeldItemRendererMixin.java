package brokke.cc.mixins.render;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.SwingAnimation;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.HandRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
   @Shadow
   public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, MatrixStack var4, OrderedRenderCommandQueue var5, int var6);

   @Shadow
   public abstract void renderArm(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Arm var4);

   @Shadow
   public abstract void renderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);

   @ModifyVariable(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private float onModifyPitch(float var1) {
      return FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : var1;
   }

   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10,
      CallbackInfo var11
   ) {
      boolean var12 = var4 == Hand.MAIN_HAND;
      Arm var13 = var12 ? var1.getMainArm() : var1.getMainArm().getOpposite();
      boolean var14 = var13 == Arm.RIGHT;
      var8.push();
      HandRenderEvent var15 = new HandRenderEvent(var13, var5, var6, var7, var8, var4);
      EventBus.post(var15);
      if (var15.isCancelled()) {
         var11.cancel();
         if (!var6.isEmpty()) {
            this.renderItem(var1, var6, var14 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, var8, var9, var10);
         } else if (!var1.isInvisible()) {
            this.renderArmHoldingItem(var8, var9, var10, var7, var5, var13);
         }

         var8.pop();
      }
   }

   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At("RETURN")
   )
   private void onRenderFirstPersonItemEnd(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10,
      CallbackInfo var11
   ) {
      var8.pop();
   }

   @Inject(method = "swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V", at = @At("HEAD"), cancellable = true)
   private void onSwingArm(float var1, MatrixStack var2, int var3, Arm var4, CallbackInfo var5) {
      SwingAnimation var6 = FunctionManager.a(SwingAnimation.class);
      if (var6 != null && var6.a(var2, var1, var4, var3)) {
         var5.cancel();
      }
   }

   @Redirect(method = "updateHeldItems()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getHandEquippingProgress(F)F"))
   private float onGetHandEquippingProgress(ClientPlayerEntity var1, float var2) {
      SwingAnimation var3 = FunctionManager.a(SwingAnimation.class);
      return var3 != null && var3.isActive() && var3.q() && var3.a(var1.getMainHandStack()) ? 1.0F : var1.getHandEquippingProgress(var2);
   }

   @ModifyVariable(method = "applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V", at = @At("HEAD"), argsOnly = true)
   private float onApplyEquipOffset(float var1) {
      SwingAnimation var2 = FunctionManager.a(SwingAnimation.class);
      if (var2 != null && var2.isActive() && var2.q()) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.player != null && var3.player.getHandSwingProgress(1.0F) > 0.0F && var2.a(var3.player.getMainHandStack())) {
            return 0.0F;
         }
      }

      return var1;
   }
}
