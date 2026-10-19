package brokke.cc.core;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum SwapItem {
   a("Shield", Items.SHIELD),
   b("Gapple", Items.ENCHANTED_GOLDEN_APPLE),
   c("Totem", Items.TOTEM_OF_UNDYING),
   d("PlayerHead", Items.PLAYER_HEAD),
   e("Firework", Items.FIREWORK_ROCKET);

   private final String f;
   private final Item g;

   SwapItem(String nullxx, Item nullxxx) {
      this.f = nullxx;
      this.g = nullxxx;
   }

   public Item getG() {
      return this.g;
   }

   @Override
   public String toString() {
      return this.f;
   }
}
