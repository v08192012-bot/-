package brokke.cc.nav;

public class Marker {
   public Marker.a type;
   public String key;
   public String value;

   public Marker() {
   }

   public Marker(Marker.a var1, String var2, String var3) {
      this.type = var1;
      this.key = var2;
      this.value = var3;
   }

   public static Marker a(String var0, String var1) {
      return new Marker(Marker.a.PBV, var0, var1);
   }

   public static Marker a(String var0) {
      return new Marker(Marker.a.CUSTOM_ENCH, var0, null);
   }

   public static Marker b(String var0, String var1) {
      return new Marker(Marker.a.CUSTOM_DATA, var0, var1);
   }

   @Override
   public String toString() {
      return this.type + "{" + this.key + (this.value == null ? "" : "=" + this.value) + "}";
   }

   public enum a {
      PBV,
      CUSTOM_ENCH,
      CUSTOM_DATA;
   }
}
