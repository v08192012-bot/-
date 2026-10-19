package brokke.cc.cosmetic;

import brokke.cc.config.ConfigManager;
import brokke.cc.core.ModelPosition;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Identifier;

public final class LocalCosmetics {
   private static final int MAX_SCAN_INDEX = 256;
   private static final List<LocalCosmetics.a> ENTRIES = g();
   private static final LinkedHashMap<String, Integer> SELECTED = new LinkedHashMap<>();
   private static final Map<Integer, CosmeticModel> MODELS = new HashMap<>();

   private LocalCosmetics() {
   }

   public static int a() {
      return ENTRIES.size();
   }

   public static String a(int var0) {
      return i(var0).name;
   }

   public static String b(int var0) {
      return i(var0).type;
   }

   public static int c(int var0) {
      return i(var0).index;
   }

   public static Identifier d(int var0) {
      return Identifier.of("brokke", "textures/cosmetics/cosmetic_" + i(var0).index + ".png");
   }

   public static int b() {
      return SELECTED.isEmpty() ? -1 : SELECTED.values().stream().reduce((var0, var1) -> var1).orElse(-1);
   }

   public static boolean e(int var0) {
      return SELECTED.containsValue(var0);
   }

   public static String c() {
      int var0 = b();
      return var0 < 0 ? "" : b(var0);
   }

   public static Identifier d() {
      int var0 = b();
      return var0 < 0 ? null : d(var0);
   }

   public static List<Integer> e() {
      return List.copyOf(SELECTED.values());
   }

   public static JsonObject f() {
      JsonObject var0 = new JsonObject();

      for (Entry var2 : SELECTED.entrySet()) {
         var0.addProperty((String)var2.getKey(), i((Integer)var2.getValue()).index);
      }

      return var0;
   }

   public static void a(JsonObject var0) {
      SELECTED.clear();
      MODELS.clear();
      if (var0 != null) {
         for (String var2 : var0.keySet()) {
            int var3 = var0.get(var2).getAsInt();
            int var4 = j(var3);
            if (var4 >= 0) {
               SELECTED.put(var2, var4);
               h(var4);
            }
         }
      }
   }

   public static void f(int var0) {
      String var1 = b(var0);
      Integer var2 = SELECTED.get(var1);
      if (var2 != null && var2 == var0) {
         SELECTED.remove(var1);
      } else {
         SELECTED.put(var1, var0);
         h(var0);
      }

      ConfigManager.onSettingChanged();
   }

   public static CosmeticModel g(int var0) {
      return h(var0);
   }

   private static CosmeticModel h(int var0) {
      int var1 = i(var0).index;
      return MODELS.computeIfAbsent(var1, var1x -> {
         try (InputStream var2 = LocalCosmetics.class.getResourceAsStream("/assets/brokke/cosmetics/models/cosmetic_" + var1x + ".json")) {
            if (var2 == null) {
               return null;
            }

            CosmeticModel var3 = CosmeticLoader.a().a(new String(var2.readAllBytes(), StandardCharsets.UTF_8));
            if (var3 != null) {
               var3.setTextureId(Identifier.of("brokke", "textures/cosmetics/cosmetic_" + var1x + ".png"));
               String var4 = i(var0).type;
               var3.setType(var4);
               if ("wings".equalsIgnoreCase(var4) || "bodywear".equalsIgnoreCase(var4)) {
                  var3.setPosition(ModelPosition.BODY);
               }
            }

            return var3;
         } catch (Exception var7) {
            return null;
         }
      });
   }

   private static LocalCosmetics.a i(int var0) {
      if (var0 >= 0 && var0 < ENTRIES.size()) {
         return ENTRIES.get(var0);
      } else {
         throw new IndexOutOfBoundsException("Cosmetic index " + var0 + " out of " + ENTRIES.size());
      }
   }

   private static int j(int var0) {
      for (int var1 = 0; var1 < ENTRIES.size(); var1++) {
         if (ENTRIES.get(var1).index == var0) {
            return var1;
         }
      }

      return -1;
   }

   private static List<LocalCosmetics.a> g() {
      ArrayList<LocalCosmetics.a> var0 = new ArrayList<>();

      for (int var1 = 0; var1 < 256; var1++) {
         String var2 = "/assets/brokke/cosmetics/models/cosmetic_" + var1 + ".json";

         try (InputStream var3 = LocalCosmetics.class.getResourceAsStream(var2)) {
            if (var3 != null) {
               String var4 = new String(var3.readAllBytes(), StandardCharsets.UTF_8);
               JsonObject var5 = JsonParser.parseString(var4).getAsJsonObject();
               String var6 = var5.has("name") ? var5.get("name").getAsString() : "Cosmetic " + (var1 + 1);
               String var7 = var5.has("type") ? var5.get("type").getAsString() : "";
               int var8 = var5.has("pos") ? var5.get("pos").getAsInt() : -1;
               String var9 = a(var1, var6, var7, var8);
               if (!a(var1, var6, var9)) {
                  var0.add(new LocalCosmetics.a(var1, a(var6, var1), var9));
               }
            }
         } catch (Exception var12) {
         }
      }

      var0.sort(Comparator.comparingInt(var0x -> var0x.index));
      return List.copyOf(var0);
   }

   private static boolean a(int var0, String var1, String var2) {
      if (!"cape".equalsIgnoreCase(var2) && var0 > 13) {
         String var3 = var1 == null ? "" : var1.toLowerCase();
         if (var3.contains("cape")) {
            return true;
         } else if (var3.contains("fluger_wings") || var3.contains("fluger wings") || var0 == 19) {
            return true;
         } else if (var3.contains("bro9i_trident") || var3.contains("bro9i trident") || var0 == 20) {
            return true;
         } else if (var3.contains("spider wings") || var3.contains("spider_wings") || var0 == 25) {
            return true;
         } else if (!var3.contains("bro9i_hat") && !var3.contains("bro9i hat") && var0 != 54) {
            return !var3.contains("akvi4_penguin") && !var3.contains("akvi4 penguin") && var0 != 55
               ? var3.contains("bear hat") || var3.contains("bear_hat") || var0 == 57
               : true;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static String a(String var0, int var1) {
      String var2 = var0 != null && !var0.isBlank() ? var0 : "Cosmetic " + (var1 + 1);
      if (var2.startsWith("pulse_")) {
         var2 = var2.substring("pulse_".length());
      }

      if (var2.startsWith("brokke_")) {
         var2 = var2.substring("brokke_".length());
      }

      return var2.replace('_', ' ').trim();
   }

   private static String a(int var0, String var1, String var2, int var3) {
      if (var2 != null && !var2.isBlank()) {
         return var2.trim().toLowerCase();
      } else {
         String var4 = var1 == null ? "" : var1.toLowerCase();
         if (var4.contains("cape")) {
            return "cape";
         } else if (var4.contains("wing")) {
            return "wings";
         } else if (var4.contains("pet") || var4.contains("bee") || var4.contains("radish")) {
            return "pet";
         } else if (var4.contains("hat") || var4.contains("nimb") || var3 == 2) {
            return "hat";
         } else if (var0 <= 13) {
            return "cape";
         } else if (var0 <= 26) {
            return "wings";
         } else if (var0 <= 37) {
            return "bodywear";
         } else {
            return var0 <= 49 ? "pet" : "bodywear";
         }
      }
   }

   public static final class a {
      public final int index;
      public final String name;
      public final String type;

      a(int var1, String var2, String var3) {
         this.index = var1;
         this.name = var2;
         this.type = var3;
      }
   }
}
