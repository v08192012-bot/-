package brokke.cc.core;

import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.UpdateEvent;

public class StopHandler extends BaseHandler {
   private int ticks = -1;

   public int getTicks() {
      return this.ticks;
   }

   public void setTicks(int var1) {
      this.ticks = var1;
   }

   public boolean b() {
      return this.ticks != -1;
   }

   public boolean c() {
      return this.ticks == 0;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (this.ticks > 0) {
         this.ticks--;
      } else if (this.ticks == 0) {
         this.ticks = -1;
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onMoveInput(EventMoveInput var1) {
      if (this.ticks >= 0) {
         var1.setForward(0.0F);
         var1.setStrafe(0.0F);
         var1.setJump(false);
      }
   }
}
