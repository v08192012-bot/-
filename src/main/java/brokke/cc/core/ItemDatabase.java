package brokke.cc.core;

import brokke.cc.nav.Marker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ItemDatabase {
   public static final List<ItemEntry> LIST = new ArrayList<>();

   private ItemDatabase() {
   }

   public static void a() {
      LIST.clear();
      LIST.addAll(b());
   }

   private static List<ItemEntry> b() {
      ArrayList var0 = new ArrayList();

      try (InputStream var1 = ItemDatabase.class.getResourceAsStream("/assets/brokke/autobuy-items.json")) {
         if (var1 == null) {
            return var0;
         }

         String var2 = new String(var1.readAllBytes(), StandardCharsets.UTF_8);
         JsonObject var3 = JsonParser.parseString(var2).getAsJsonObject();
         if (var3.has("items") && var3.get("items").isJsonArray()) {
            for (JsonElement var6 : var3.getAsJsonArray("items")) {
               JsonObject var7 = var6.getAsJsonObject();
               ItemEntry var8 = new ItemEntry();
               var8.itemId = a(var7, "itemId", "");
               var8.displayName = a(var7, "displayName", null);
               var8.searchQuery = a(var7, "searchQuery", null);
               var8.loreKeyword = a(var7, "loreKeyword", null);
               var8.requireCustom = a(var7, "custom");
               var8.matchAnySword = a(var7, "matchAnySword");
               var8.potionEffect = a(var7, "potionEffect", null);
               if (var7.has("markers") && var7.get("markers").isJsonArray()) {
                  for (JsonElement var10 : var7.getAsJsonArray("markers")) {
                     JsonObject var11 = var10.getAsJsonObject();
                     String var12 = a(var11, "type", "");
                     String var13 = a(var11, "key", "");
                     String var14 = a(var11, "value", "");
                     switch (var12) {
                        case "PBV":
                           var8.markers.add(Marker.a(var13, var14));
                           break;
                        case "CUSTOM_DATA":
                           var8.markers.add(Marker.b(var13, var14));
                           break;
                        case "CUSTOM_ENCH":
                           var8.markers.add(Marker.a(var13));
                     }
                  }
               }

               var8.minDurabilityPercent = 0;
               var0.add(var8);
            }

            return var0;
         } else {
            return var0;
         }
      } catch (Throwable var19) {
         return new ArrayList<>();
      }
   }

   private static String a(JsonObject var0, String var1, String var2) {
      return var0.has(var1) && !var0.get(var1).isJsonNull() ? var0.get(var1).getAsString() : var2;
   }

   private static boolean a(JsonObject var0, String var1) {
      return var0.has(var1) && !var0.get(var1).isJsonNull() && var0.get(var1).getAsBoolean();
   }
}
