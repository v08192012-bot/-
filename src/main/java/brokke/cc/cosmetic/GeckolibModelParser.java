package brokke.cc.cosmetic;

public class GeckolibModelParser {
   public GeoModel a(CosmeticModel var1) {
      try {
         String var2 = var1.getRawModelJson();
         return var2 == null ? null : GeoModelParser.a(var2);
      } catch (Exception var3) {
         return null;
      }
   }
}
