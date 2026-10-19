package brokke.cc.core;

import brokke.cc.util.InventoryUtil;
import net.minecraft.item.ItemStack;

public class ArmorSlot extends ItemSlot {
   private final int armorSlotIndex;

   public ArmorSlot(int var1) {
      if (var1 >= 0 && var1 <= 3) {
         this.armorSlotIndex = var1;
      } else {
         throw new IllegalArgumentException("Armor Slot Index must be between 0 and 3");
      }
   }

   @Override
   public ItemStack a() {
      return InventoryUtil.d(this.armorSlotIndex);
   }

   @Override
   public int get45() {
      return 8 - this.armorSlotIndex;
   }

   public int getArmorSlotIndex() {
      return this.armorSlotIndex;
   }
}
