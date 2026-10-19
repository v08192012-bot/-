package brokke.cc.core;

import net.minecraft.item.ItemStack;

public class OffhandSlot extends ItemSlot {
   @Override
   public ItemStack a() {
      return mc.player != null ? mc.player.getOffHandStack() : ItemStack.EMPTY;
   }

   @Override
   public int get45() {
      return 45;
   }
}
