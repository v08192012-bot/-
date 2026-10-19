package brokke.cc.config;

import brokke.cc.core.ItemDatabase;
import brokke.cc.core.ItemEntry;
import brokke.cc.core.PurchaseRecord;
import brokke.cc.nav.Marker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

public class AutoBuyConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static AutoBuyConfig INSTANCE = new AutoBuyConfig();
   public boolean enabled = false;
   public int clickIntervalTicks = 1;
   public boolean autoRefresh = true;
   public int refreshIntervalTicks = -1;
   public int refreshSlot = -1;
   public boolean minPriceEnabled = true;
   public long minPrice = 15000L;
   public boolean autoSell = false;
   public int sellDiscountPercent = 5;
   public int relistDiscountPercent = 8;
   public boolean autoRelist = false;
   public int relistIntervalSeconds = 180;
   public boolean autoParse = true;
   public int parseIntervalSeconds = 600;
   public boolean autoPay = false;
   public long autoPayKeepBalance = 5000000L;
   public String autoPayNick = "";
   public int autoPayIntervalSeconds = 300;
   public int parsePercent = 80;
   public long defaultPriceIfMissing = 50000000L;
   public float delayPoint0 = 0.1F;
   public float delayPoint1 = 0.14F;
   public float delayPoint2 = 0.3F;
   public float delayPoint3 = 0.28F;
   public float refreshRandomizationPoint0 = 0.0F;
   public float refreshRandomizationPoint1 = 0.33F;
   public float refreshRandomizationPoint2 = 0.66F;
   public float refreshRandomizationPoint3 = 1.0F;
   public List<ItemEntry> entries = new ArrayList<>();
   public List<String> banList = new ArrayList<>();
   public List<PurchaseRecord> history = new ArrayList<>();
   public int historyMaxSize = 500;

   public static AutoBuyConfig getInstance() {
      return INSTANCE;
   }

   public ItemEntry a(String var1) {
      if (var1 == null) {
         return null;
      }

      for (ItemEntry var3 : this.entries) {
         if (var3.enabled && var3.maxUnitPrice > 0L && var3.a(var1)) {
            return var3;
         }
      }

      return null;
   }

   public boolean b(String var1) {
      if (var1 != null && !var1.isBlank()) {
         for (String var3 : this.banList) {
            if (var3 != null && var3.equalsIgnoreCase(var1)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void c(String var1) {
      if (var1 != null) {
         String var2 = var1.trim();
         if (!var2.isEmpty() && !this.b(var2)) {
            this.banList.add(var2);
            g();
         }
      }
   }

   public void d(String var1) {
      this.banList.removeIf(var1xx -> var1xx != null && var1xx.equalsIgnoreCase(var1));
      g();
   }

   public void a(PurchaseRecord var1) {
      if (var1 != null) {
         this.history.add(0, var1);

         while (this.history.size() > Math.max(1, this.historyMaxSize)) {
            this.history.remove(this.history.size() - 1);
         }

         g();
      }
   }

   public void b() {
      this.enabled = false;
      this.autoSell = false;
      this.autoRelist = false;
      this.autoParse = true;
      this.autoPay = false;
      this.c();
      this.d();
      g();
   }

   public void c() {
      this.delayPoint0 = 0.1F;
      this.delayPoint1 = 0.14F;
      this.delayPoint2 = 0.3F;
      this.delayPoint3 = 0.28F;
   }

   public void d() {
      this.refreshRandomizationPoint0 = 0.0F;
      this.refreshRandomizationPoint1 = 0.33F;
      this.refreshRandomizationPoint2 = 0.66F;
      this.refreshRandomizationPoint3 = 1.0F;
   }

   public void e() {
      this.delayPoint0 = e(this.delayPoint0);
      this.delayPoint1 = e(this.delayPoint1);
      this.delayPoint2 = e(this.delayPoint2);
      this.delayPoint3 = e(this.delayPoint3);
   }

   public void f() {
      this.refreshRandomizationPoint0 = e(this.refreshRandomizationPoint0);
      this.refreshRandomizationPoint1 = e(this.refreshRandomizationPoint1);
      this.refreshRandomizationPoint2 = e(this.refreshRandomizationPoint2);
      this.refreshRandomizationPoint3 = e(this.refreshRandomizationPoint3);
   }

   public float a(int var1) {
      return switch (var1) {
         case 0 -> this.delayPoint0;
         case 1 -> this.delayPoint1;
         case 2 -> this.delayPoint2;
         default -> this.delayPoint3;
      };
   }

   public void a(int var1, float var2) {
      float var3 = e(var2);
      switch (var1) {
         case 0:
            this.delayPoint0 = var3;
            break;
         case 1:
            this.delayPoint1 = var3;
            break;
         case 2:
            this.delayPoint2 = var3;
            break;
         default:
            this.delayPoint3 = var3;
      }
   }

   public float b(int var1) {
      return switch (var1) {
         case 0 -> this.refreshRandomizationPoint0;
         case 1 -> this.refreshRandomizationPoint1;
         case 2 -> this.refreshRandomizationPoint2;
         default -> this.refreshRandomizationPoint3;
      };
   }

   public void b(int var1, float var2) {
      float var3 = e(var2);
      switch (var1) {
         case 0:
            this.refreshRandomizationPoint0 = var3;
            break;
         case 1:
            this.refreshRandomizationPoint1 = var3;
            break;
         case 2:
            this.refreshRandomizationPoint2 = var3;
            break;
         default:
            this.refreshRandomizationPoint3 = var3;
      }
   }

   public float a(float var1) {
      this.e();
      float var2 = e(var1);
      float var3 = var2 * 3.0F;
      int var4 = Math.min(2, (int)Math.floor(var3));
      float var5 = var3 - var4;
      float var6 = this.a(var4);
      float var7 = this.a(var4 + 1);
      return var6 + (var7 - var6) * var5;
   }

   public float b(float var1) {
      return this.a(var1);
   }

   public int a(float var1, int var2, int var3) {
      return Math.round(var2 + (var3 - var2) * this.b(var1));
   }

   public int c(float var1) {
      float var2 = this.a(var1);
      return Math.max(1, (int)Math.ceil(var2 * 20.0F));
   }

   public int d(float var1) {
      this.f();
      float var2 = e(var1);
      float var3 = var2 * 3.0F;
      int var4 = Math.min(2, (int)Math.floor(var3));
      float var5 = var3 - var4;
      float var6 = this.b(var4);
      float var7 = this.b(var4 + 1);
      float var8 = var6 + (var7 - var6) * var5;
      return 50 + (int)(var8 * 950.0F);
   }

   private static Path k() {
      return FabricLoader.getInstance().getConfigDir().resolve("brokke").resolve("autobuy.json");
   }

   public static void g() {
      try {
         Path var0 = k();
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, GSON.toJson(INSTANCE));
         ConfigManager.onSettingChanged();
      } catch (Exception var1) {
         System.err.println("[AutoBuy] save failed: " + var1);
      }
   }

   public static JsonObject h() {
      AutoBuyConfig var0 = getInstance();
      JsonObject var1 = new JsonObject();
      var1.addProperty("minPriceEnabled", var0.minPriceEnabled);
      var1.addProperty("minPrice", var0.minPrice);
      var1.addProperty("autoRefresh", var0.autoRefresh);
      var1.addProperty("refreshSlot", var0.refreshSlot);
      var1.addProperty("clickIntervalTicks", var0.clickIntervalTicks);
      var1.addProperty("refreshIntervalTicks", var0.refreshIntervalTicks);
      var1.addProperty("sellDiscountPercent", var0.sellDiscountPercent);
      var1.addProperty("relistDiscountPercent", var0.relistDiscountPercent);
      var1.addProperty("relistIntervalSeconds", var0.relistIntervalSeconds);
      var1.addProperty("parseIntervalSeconds", var0.parseIntervalSeconds);
      var1.addProperty("parsePercent", var0.parsePercent);
      var1.addProperty("autoPayKeepBalance", var0.autoPayKeepBalance);
      var1.addProperty("autoPayNick", var0.autoPayNick);
      var1.addProperty("autoPayIntervalSeconds", var0.autoPayIntervalSeconds);
      var1.addProperty("delayPoint0", var0.delayPoint0);
      var1.addProperty("delayPoint1", var0.delayPoint1);
      var1.addProperty("delayPoint2", var0.delayPoint2);
      var1.addProperty("delayPoint3", var0.delayPoint3);
      var1.addProperty("refreshRandomizationPoint0", var0.refreshRandomizationPoint0);
      var1.addProperty("refreshRandomizationPoint1", var0.refreshRandomizationPoint1);
      var1.addProperty("refreshRandomizationPoint2", var0.refreshRandomizationPoint2);
      var1.addProperty("refreshRandomizationPoint3", var0.refreshRandomizationPoint3);
      JsonArray var2 = new JsonArray();

      for (ItemEntry var4 : var0.entries) {
         JsonObject var5 = new JsonObject();
         if (var4.itemId != null) {
            var5.addProperty("itemId", var4.itemId);
         }

         if (var4.displayName != null) {
            var5.addProperty("displayName", var4.displayName);
         }

         if (var4.searchQuery != null) {
            var5.addProperty("searchQuery", var4.searchQuery);
         }

         if (var4.loreKeyword != null) {
            var5.addProperty("loreKeyword", var4.loreKeyword);
         }

         if (var4.potionEffect != null) {
            var5.addProperty("potionEffect", var4.potionEffect);
         }

         var5.addProperty("maxUnitPrice", var4.maxUnitPrice);
         var5.addProperty("marketPrice", var4.marketPrice);
         var5.addProperty("minUnitPrice", var4.minUnitPrice);
         var5.addProperty("minDurabilityPercent", var4.minDurabilityPercent);
         var5.addProperty("isArmor", var4.isArmor);
         var5.addProperty("matchAnySword", var4.matchAnySword);
         var5.addProperty("requireCustom", var4.requireCustom);
         var5.addProperty("enabled", var4.enabled);
         var5.addProperty("parseEnabled", var4.parseEnabled);
         if (var4.markers != null && !var4.markers.isEmpty()) {
            JsonArray var6 = new JsonArray();

            for (Marker var8 : var4.markers) {
               JsonObject var9 = new JsonObject();
               var9.addProperty("type", var8.type.name());
               if (var8.key != null) {
                  var9.addProperty("key", var8.key);
               }

               if (var8.value != null) {
                  var9.addProperty("value", var8.value);
               }

               var6.add(var9);
            }

            var5.add("markers", var6);
         }

         var2.add(var5);
      }

      var1.add("entries", var2);
      if (var0.banList != null) {
         JsonArray var10 = new JsonArray();

         for (String var12 : var0.banList) {
            var10.add(var12);
         }

         var1.add("banList", var10);
      }

      return var1;
   }

   public static void a(JsonObject var0) {
      if (var0 != null) {
         AutoBuyConfig var1 = getInstance();
         if (var0.has("minPriceEnabled")) {
            var1.minPriceEnabled = var0.get("minPriceEnabled").getAsBoolean();
         }

         if (var0.has("minPrice")) {
            var1.minPrice = var0.get("minPrice").getAsLong();
         }

         if (var0.has("autoRefresh")) {
            var1.autoRefresh = var0.get("autoRefresh").getAsBoolean();
         }

         if (var0.has("refreshSlot")) {
            var1.refreshSlot = var0.get("refreshSlot").getAsInt();
         }

         if (var0.has("clickIntervalTicks")) {
            var1.clickIntervalTicks = var0.get("clickIntervalTicks").getAsInt();
         }

         if (var0.has("refreshIntervalTicks")) {
            var1.refreshIntervalTicks = var0.get("refreshIntervalTicks").getAsInt();
         }

         if (var0.has("sellDiscountPercent")) {
            var1.sellDiscountPercent = var0.get("sellDiscountPercent").getAsInt();
         }

         if (var0.has("relistDiscountPercent")) {
            var1.relistDiscountPercent = var0.get("relistDiscountPercent").getAsInt();
         }

         if (var0.has("relistIntervalSeconds")) {
            var1.relistIntervalSeconds = var0.get("relistIntervalSeconds").getAsInt();
         }

         if (var0.has("parseIntervalSeconds")) {
            var1.parseIntervalSeconds = var0.get("parseIntervalSeconds").getAsInt();
         }

         if (var0.has("parsePercent")) {
            var1.parsePercent = var0.get("parsePercent").getAsInt();
         }

         if (var0.has("autoPayKeepBalance")) {
            var1.autoPayKeepBalance = var0.get("autoPayKeepBalance").getAsLong();
         }

         if (var0.has("autoPayNick")) {
            var1.autoPayNick = var0.get("autoPayNick").getAsString();
         }

         if (var0.has("autoPayIntervalSeconds")) {
            var1.autoPayIntervalSeconds = var0.get("autoPayIntervalSeconds").getAsInt();
         }

         if (var0.has("delayPoint0")) {
            var1.delayPoint0 = var0.get("delayPoint0").getAsFloat();
         }

         if (var0.has("delayPoint1")) {
            var1.delayPoint1 = var0.get("delayPoint1").getAsFloat();
         }

         if (var0.has("delayPoint2")) {
            var1.delayPoint2 = var0.get("delayPoint2").getAsFloat();
         }

         if (var0.has("delayPoint3")) {
            var1.delayPoint3 = var0.get("delayPoint3").getAsFloat();
         }

         if (var0.has("refreshRandomizationPoint0")) {
            var1.refreshRandomizationPoint0 = var0.get("refreshRandomizationPoint0").getAsFloat();
         }

         if (var0.has("refreshRandomizationPoint1")) {
            var1.refreshRandomizationPoint1 = var0.get("refreshRandomizationPoint1").getAsFloat();
         }

         if (var0.has("refreshRandomizationPoint2")) {
            var1.refreshRandomizationPoint2 = var0.get("refreshRandomizationPoint2").getAsFloat();
         }

         if (var0.has("refreshRandomizationPoint3")) {
            var1.refreshRandomizationPoint3 = var0.get("refreshRandomizationPoint3").getAsFloat();
         }

         if (var0.has("entries") && var0.get("entries").isJsonArray()) {
            ArrayList var2 = new ArrayList();

            for (JsonElement var4 : var0.getAsJsonArray("entries")) {
               if (var4.isJsonObject()) {
                  JsonObject var5 = var4.getAsJsonObject();
                  ItemEntry var6 = new ItemEntry();
                  if (var5.has("itemId")) {
                     var6.itemId = var5.get("itemId").getAsString();
                  }

                  if (var5.has("displayName")) {
                     var6.displayName = var5.get("displayName").getAsString();
                  }

                  if (var5.has("searchQuery")) {
                     var6.searchQuery = var5.get("searchQuery").getAsString();
                  }

                  if (var5.has("loreKeyword")) {
                     var6.loreKeyword = var5.get("loreKeyword").getAsString();
                  }

                  if (var5.has("potionEffect")) {
                     var6.potionEffect = var5.get("potionEffect").getAsString();
                  }

                  if (var5.has("maxUnitPrice")) {
                     var6.maxUnitPrice = var5.get("maxUnitPrice").getAsLong();
                  }

                  if (var5.has("marketPrice")) {
                     var6.marketPrice = var5.get("marketPrice").getAsLong();
                  }

                  if (var5.has("minUnitPrice")) {
                     var6.minUnitPrice = var5.get("minUnitPrice").getAsLong();
                  }

                  if (var5.has("minDurabilityPercent")) {
                     var6.minDurabilityPercent = var5.get("minDurabilityPercent").getAsInt();
                  }

                  if (var5.has("isArmor")) {
                     var6.isArmor = var5.get("isArmor").getAsBoolean();
                  }

                  if (var5.has("matchAnySword")) {
                     var6.matchAnySword = var5.get("matchAnySword").getAsBoolean();
                  }

                  if (var5.has("requireCustom")) {
                     var6.requireCustom = var5.get("requireCustom").getAsBoolean();
                  }

                  if (var5.has("enabled")) {
                     var6.enabled = var5.get("enabled").getAsBoolean();
                  }

                  if (var5.has("parseEnabled")) {
                     var6.parseEnabled = var5.get("parseEnabled").getAsBoolean();
                  }

                  if (var5.has("markers") && var5.get("markers").isJsonArray()) {
                     var6.markers = new ArrayList<>();

                     for (JsonElement var8 : var5.getAsJsonArray("markers")) {
                        if (var8.isJsonObject()) {
                           JsonObject var9 = var8.getAsJsonObject();
                           String var10 = var9.has("type") ? var9.get("type").getAsString() : "";
                           String var11 = var9.has("key") ? var9.get("key").getAsString() : "";
                           String var12 = var9.has("value") ? var9.get("value").getAsString() : "";

                           try {
                              Marker.a var13 = Marker.a.valueOf(var10);
                              var6.markers.add(new Marker(var13, var11, var12));
                           } catch (Exception var15) {
                           }
                        }
                     }
                  }

                  var2.add(var6);
               }
            }

            var1.entries = var2;
         }

         if (var0.has("banList") && var0.get("banList").isJsonArray()) {
            ArrayList var16 = new ArrayList();

            for (JsonElement var19 : var0.getAsJsonArray("banList")) {
               var16.add(var19.getAsString());
            }

            var1.banList = var16;
         }

         var1.e();
         var1.f();
         var1.j();

         try {
            Path var17 = k();
            Files.createDirectories(var17.getParent());
            Files.writeString(var17, GSON.toJson(INSTANCE));
         } catch (Exception var14) {
         }
      }
   }

   public static void i() {
      try {
         Path var0 = k();
         if (Files.exists(var0)) {
            String var1 = Files.readString(var0);
            AutoBuyConfig var2 = (AutoBuyConfig)GSON.fromJson(var1, AutoBuyConfig.class);
            if (var2 != null) {
               INSTANCE = var2;
               if (INSTANCE.entries == null) {
                  INSTANCE.entries = new ArrayList<>();
               }

               if (INSTANCE.banList == null) {
                  INSTANCE.banList = new ArrayList<>();
               }

               if (INSTANCE.history == null) {
                  INSTANCE.history = new ArrayList<>();
               }

               if (INSTANCE.autoPayNick == null) {
                  INSTANCE.autoPayNick = "";
               }

               INSTANCE.e();
               INSTANCE.f();
               INSTANCE.enabled = false;
            }
         }
      } catch (Exception var3) {
         System.err.println("[AutoBuy] load failed: " + var3);
      }

      INSTANCE.j();
      g();
   }

   public void j() {
      for (ItemEntry var2 : this.entries) {
         if ("minecraft:popped_winlend_fruit".equals(var2.itemId)
            || "popped_winlend_fruit".equals(var2.itemId)
            || "Трапка".equalsIgnoreCase(var2.displayName) && (var2.itemId == null || !var2.itemId.equals("minecraft:popped_chorus_fruit"))) {
            var2.itemId = "minecraft:popped_chorus_fruit";
         }

         if (!var2.d()) {
            var2.minDurabilityPercent = 0;
         }

         if ("minecraft:tripwire_hook".equals(var2.itemId)
            && (
               "обычная отмычка".equalsIgnoreCase(var2.searchQuery)
                  || "редкая отмычка".equalsIgnoreCase(var2.searchQuery)
                  || "уникальная отмычка".equalsIgnoreCase(var2.searchQuery)
                  || "секретная отмычка".equalsIgnoreCase(var2.searchQuery)
            )) {
            var2.searchQuery = "отмычка";
         }

         if (var2.displayName != null) {
            String var3 = var2.displayName.toLowerCase(Locale.ROOT);
            if (!var3.contains("скорость 3") && !var3.contains("скорость iii")) {
               if (!var3.contains("мифическая") || !var3.contains("урон 3") && !var3.contains("урон iii")) {
                  if (!var3.contains("мифическая") || !var3.contains("броня 3") && !var3.contains("броня iii")) {
                     if (!var3.contains("незеритовый меч") || !var3.contains("острота 7") && !var3.contains("острота vii")) {
                        if (var3.equals("динамит а") || var3.equals("динамит a")) {
                           var2.searchQuery = "динамит";
                           var2.loreKeyword = "3 раза";
                        } else if (var3.equals("динамит б") || var3.equals("динамит b")) {
                           var2.searchQuery = "динамит";
                           var2.loreKeyword = "10 раз";
                        } else if (var3.contains("черепаш")) {
                           var2.searchQuery = "черепашьей";
                           var2.loreKeyword = null;
                        }
                     } else {
                        var2.displayName = "Незеритовый меч (Острота 7)";
                        var2.searchQuery = "незеритовый меч";
                        var2.loreKeyword = "Острота 7";
                     }
                  } else {
                     var2.searchQuery = "сфера на броня 3";
                  }
               } else {
                  var2.searchQuery = "сфера на урон 3";
               }
            } else {
               var2.searchQuery = "сфера на скорость 3";
            }
         }

         if ("minecraft:dragon_egg".equals(var2.itemId)
            || var2.displayName != null
               && var2.displayName.toLowerCase(Locale.ROOT).contains("осколочное")
               && var2.displayName.toLowerCase(Locale.ROOT).contains("энд")) {
            var2.itemId = "minecraft:dragon_egg";
            var2.displayName = "Осколочное яйцо энд";
            var2.searchQuery = "осколочное яйцо энд";
            var2.loreKeyword = null;
         }

         if ("minecraft:snowball".equals(var2.itemId)) {
            var2.searchQuery = "снежок";
            var2.loreKeyword = null;
            var2.requireCustom = false;
         }

         if ("minecraft:enchanted_golden_apple".equals(var2.itemId)) {
            var2.searchQuery = "зачарованное яблоко";
         }
      }

      for (ItemEntry var6 : ItemDatabase.LIST) {
         ItemEntry var7 = this.a(var6);
         if (var7 == null) {
            ItemEntry var4 = new ItemEntry();
            var4.itemId = var6.itemId;
            var4.displayName = var6.displayName;
            var4.searchQuery = var6.searchQuery;
            var4.loreKeyword = var6.loreKeyword;
            var4.potionEffect = var6.potionEffect;
            var4.requireCustom = var6.requireCustom;
            var4.matchAnySword = var6.matchAnySword;
            var4.markers = new ArrayList<>(var6.markers);
            var4.enabled = false;
            var4.parseEnabled = true;
            var4.maxUnitPrice = 0L;
            var4.minDurabilityPercent = var6.minDurabilityPercent;
            this.entries.add(var4);
         } else {
            var7.displayName = var6.displayName;
            var7.requireCustom = var6.requireCustom;
            var7.matchAnySword = var6.matchAnySword;
            if (var7.loreKeyword == null) {
               var7.loreKeyword = var6.loreKeyword;
            }

            if (var6.searchQuery != null && !var6.searchQuery.isBlank()) {
               var7.searchQuery = var6.searchQuery;
            } else if (var7.searchQuery == null) {
               var7.searchQuery = var6.searchQuery;
            }

            if (var7.potionEffect == null) {
               var7.potionEffect = var6.potionEffect;
            }

            if (var7.markers == null || var7.markers.isEmpty()) {
               var7.markers = new ArrayList<>(var6.markers);
            }

            if (!var7.d()) {
               var7.minDurabilityPercent = 0;
            }
         }
      }
   }

   private ItemEntry a(ItemEntry var1) {
      for (ItemEntry var3 : this.entries) {
         boolean var4 = var3.itemId != null && var3.itemId.equals(var1.itemId);
         boolean var5 = e(var3.displayName).equals(e(var1.displayName));
         boolean var6 = e(var3.loreKeyword).equals(e(var1.loreKeyword));
         if (var4 && var5 && var6 && var3.matchAnySword == var1.matchAnySword) {
            return var3;
         }
      }

      return null;
   }

   private static String e(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
   }

   private static float e(float var0) {
      return Math.max(0.0F, Math.min(1.0F, var0));
   }
}
