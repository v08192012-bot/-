package brokke.cc.core;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> a() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> b() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> c() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> d() {
      return new OffhandSlotGroup();
   }
}
