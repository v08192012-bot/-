package brokke.cc.rotation;

public enum RotationPriority {
   NOT_IMPORTANT(-2),
   NORMAL(0),
   TO_TARGET(2),
   OVERRIDE(5),
   USE_ITEM(4),
   MAX(6);

   private final int priority;

   RotationPriority(int nullxx) {
      this.priority = nullxx;
   }

   public int getPriority() {
      return this.priority;
   }
}
