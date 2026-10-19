package brokke.cc.core;

import net.minecraft.item.ItemStack;

public class HotbarSlot extends ItemSlot {
   private final int slotId;

   public HotbarSlot(int var1) {
      if (var1 >= 0 && var1 <= 8) {
         this.slotId = var1;
      } else {
         throw new IllegalArgumentException("Hotbar Slot ID must be between 0 and 8");
      }
   }

   @Override
   public ItemStack a() {
      return mc.player != null && mc.player.getInventory() != null ? mc.player.getInventory().getStack(this.slotId) : ItemStack.EMPTY;
   }

   @Override
   public int get45() {
      return 36 + this.slotId;
   }

   public int getSlotId() {
      return this.slotId;
   }
}
