package brokke.cc.mixins.item;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.FinishEatEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
   @Inject(method = "finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;", at = @At("TAIL"))
   private void onFinishUsing(World var1, LivingEntity var2, CallbackInfoReturnable<ItemStack> var3) {
      if (var1.isClient() && var2 instanceof PlayerEntity var4) {
         EventBus.post(new FinishEatEvent(var4, (ItemStack)(Object)this));
      }
   }
}
