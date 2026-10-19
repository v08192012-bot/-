package brokke.cc.core;

import net.minecraft.item.ItemStack;

public class InventorySlot extends ItemSlot {
   private final int slotId;

   public InventorySlot(int var1) {
      if (var1 >= 0 && var1 <= 26) {
         this.slotId = var1;
      } else {
         throw new IllegalArgumentException("Inventory Slot ID must be between 0 and 26");
      }
   }

   @Override
   public ItemStack a() {
      return mc.player != null && mc.player.getInventory() != null ? mc.player.getInventory().getStack(this.slotId + 9) : ItemStack.EMPTY;
   }

   @Override
   public int get45() {
      return this.slotId + 9;
   }

   public int getSlotId() {
      return this.slotId;
   }
}
