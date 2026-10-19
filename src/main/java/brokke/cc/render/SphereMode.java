package brokke.cc.render;

public enum SphereMode {
   a,
   b,
   c,
   d,
   e;

   @Override
   public String toString() {
      return switch (this) {
         case a -> "CERBERUS";
         case b -> "MYTHIC";
         case c -> "LEGENDARY";
         case d -> "TALISMAN";
         case e -> "NONE";
      };
   }
}
