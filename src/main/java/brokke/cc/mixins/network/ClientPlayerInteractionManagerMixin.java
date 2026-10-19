package brokke.cc.mixins.network;

import brokke.cc.core.FakePlayerEntity;
import brokke.cc.module.combat.AutoCrystal;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.player.NoInteract;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.AttackEntityEvent;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
   @Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void onAttackEntity(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      AttackEntityEvent var4 = new AttackEntityEvent(var1, var2);
      EventBus.post(var4);
      if (var4.isCancelled()) {
         var3.cancel();
      } else if (var2 instanceof FakePlayerEntity var5) {
         var5.a(var1);
      }
   }

   @Inject(method = "interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
   private void onInteractItem(PlayerEntity var1, Hand var2, CallbackInfoReturnable<ActionResult> var3) {
      AutoCrystal var4 = AutoCrystal.INSTANCE;
      if (var4 != null && var4.isActive() && var4.l()) {
         var3.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onInteractBlock(ClientPlayerEntity var1, Hand var2, BlockHitResult var3, CallbackInfoReturnable<ActionResult> var4) {
      AutoCrystal var5 = AutoCrystal.INSTANCE;
      if (var5 != null && var5.isActive() && var5.l() && !var5.isPlacingInternal()) {
         var4.setReturnValue(ActionResult.PASS);
      } else if (var1 != null && var1.getEntityWorld() != null) {
         NoInteract var6 = FunctionManager.a(NoInteract.class);
         Block var7 = var1.getEntityWorld().getBlockState(var3.getBlockPos()).getBlock();
         if (var6 != null && var6.a(var7)) {
            var4.setReturnValue(ActionResult.PASS);
         }
      }
   }
}
