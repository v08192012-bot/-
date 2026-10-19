package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FinishEatEvent extends Event {
   private final PlayerEntity user;
   private final ItemStack stack;

   public FinishEatEvent(PlayerEntity var1, ItemStack var2) {
      this.user = var1;
      this.stack = var2;
   }

   public PlayerEntity getUser() {
      return this.user;
   }

   public ItemStack getStack() {
      return this.stack;
   }
}
