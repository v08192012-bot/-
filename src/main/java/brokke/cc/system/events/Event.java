package brokke.cc.system.events;

public abstract class Event {
   private boolean cancelled = false;

   public Event() {
      this.cancelled = false;
   }

   public void cancel() {
      this.cancelled = true;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean var1) {
      this.cancelled = var1;
   }
}
