package brokke.cc.core;

import java.util.stream.IntStream;

public class ArmorSlotsGroup extends SlotGroup<ArmorSlot> {
   public ArmorSlotsGroup() {
      super(IntStream.range(0, 4).mapToObj(ArmorSlot::new).toList());
   }
}
