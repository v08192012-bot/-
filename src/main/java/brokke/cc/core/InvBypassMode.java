package brokke.cc.core;

public enum InvBypassMode {
   a,
   b;

   @Override
   public String toString() {
      return switch (this) {
         case a -> "HolyWorld";
         case b -> "Advanced";
      };
   }
}
