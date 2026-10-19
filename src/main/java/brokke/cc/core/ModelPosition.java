package brokke.cc.core;

public enum ModelPosition {
   FREE(-1),
   BODY(1),
   HEAD(3),
   ABOVE_HEAD(-1),
   RIGHT_ARM(-1),
   LEFT_ARM(-1),
   RIGHT_LEG(2),
   LEFT_LEG(0);

   private final int armorSlot;

   ModelPosition(int nullxx) {
      this.armorSlot = nullxx;
   }

   public int a() {
      return this.ordinal();
   }

   public int getArmorSlot() {
      return this.armorSlot;
   }

   public static ModelPosition a(int var0) {
      return var0 >= 0 && var0 < values().length ? values()[var0] : BODY;
   }
}
