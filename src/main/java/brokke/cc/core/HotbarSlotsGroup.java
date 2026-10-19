package brokke.cc.core;

import java.util.stream.IntStream;

public class HotbarSlotsGroup extends SlotGroup<HotbarSlot> {
   public HotbarSlotsGroup() {
      super(IntStream.range(0, 9).mapToObj(HotbarSlot::new).toList());
   }
}
