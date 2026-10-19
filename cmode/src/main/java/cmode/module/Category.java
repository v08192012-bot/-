package cmode.module;

public enum Category {
   COMBAT("Combat", "V"),
   MOVEMENT("Movement", "I"),
   RENDER("Render", "t"),
   PLAYER("Player", "L"),
   MISC("Misc", "D");

   private final String name;
   private final String icon;

   Category(String nullxx, String nullxxx) {
      this.name = nullxx;
      this.icon = nullxxx;
   }

   public String getName() {
      return this.name;
   }

   public String getIcon() {
      return this.icon;
   }
}
