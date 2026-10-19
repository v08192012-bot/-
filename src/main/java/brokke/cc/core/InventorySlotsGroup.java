package brokke.cc.core;

import java.util.stream.IntStream;

public class InventorySlotsGroup extends SlotGroup<InventorySlot> {
   public InventorySlotsGroup() {
      super(IntStream.range(0, 27).mapToObj(InventorySlot::new).toList());
   }
}
