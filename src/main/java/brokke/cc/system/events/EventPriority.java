package brokke.cc.system.events;

public enum EventPriority {
   LOWEST(0),
   LOW(1),
   NORMAL(2),
   HIGH(3),
   HIGHEST(4),
   MONITOR(5);

   private final int level;

   EventPriority(int nullxx) {
      this.level = nullxx;
   }

   public int getLevel() {
      return this.level;
   }
}
