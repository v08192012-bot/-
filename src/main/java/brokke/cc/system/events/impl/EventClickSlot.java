package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.screen.slot.SlotActionType;

public class EventClickSlot extends Event {
   private final int windowId;
   private final int slotId;
   private final int button;
   private final SlotActionType actionType;

   public EventClickSlot(int var1, int var2, int var3, SlotActionType var4) {
      this.windowId = var1;
      this.slotId = var2;
      this.button = var3;
      this.actionType = var4;
   }

   public int getWindowId() {
      return this.windowId;
   }

   public int getSlotId() {
      return this.slotId;
   }

   public int getButton() {
      return this.button;
   }

   public SlotActionType getActionType() {
      return this.actionType;
   }
}
