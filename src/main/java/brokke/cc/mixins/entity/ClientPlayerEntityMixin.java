package brokke.cc.mixins.entity;

import brokke.cc.module.movement.NoPush;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventPostSync;
import brokke.cc.system.events.impl.EventSync;
import brokke.cc.system.events.impl.EventUpdatePost;
import brokke.cc.system.events.impl.NoSlowEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
   @Inject(method = "tick()V", at = @At("HEAD"))
   private void update(CallbackInfo var1) {
      EventBus.post(new UpdateEvent());
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void updatePost(CallbackInfo var1) {
      EventBus.post(new EventUpdatePost());
   }

   @Inject(method = "sendMovementPackets()V", at = @At("HEAD"), cancellable = true)
   private void onSendMovementPackets(CallbackInfo var1) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)(Object)this;
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player != null && var3.world != null) {
         EventSync var4 = new EventSync(var2.getYaw(), var2.getPitch());
         EventBus.post(var4);
         if (var4.isCancelled()) {
            var1.cancel();
         }
      }
   }

   @Inject(method = "sendMovementPackets()V", at = @At("RETURN"))
   private void afterSendMovementPackets(CallbackInfo var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null) {
         EventBus.post(new EventPostSync());
      }
   }

   @Inject(method = "pushOutOfBlocks(DD)V", at = @At("HEAD"), cancellable = true)
   private void onPushOutOfBlocks(double var1, double var3, CallbackInfo var5) {
      if (NoPush.Instance != null && NoPush.Instance.isActive() && NoPush.blocks.isToggled()) {
         var5.cancel();
      }
   }

   @Redirect(
      method = "applyMovementSpeedFactors(Lnet/minecraft/util/math/Vec2f;)Lnet/minecraft/util/math/Vec2f;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")
   )
   private boolean applyMovementSpeedFactors(ClientPlayerEntity var1) {
      NoSlowEvent var2 = new NoSlowEvent();
      EventBus.post(var2);
      return var2.isCancelled() ? false : var1.isUsingItem();
   }
}
