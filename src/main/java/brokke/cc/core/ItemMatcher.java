package brokke.cc.core;

import brokke.cc.nav.Marker;
import brokke.cc.util.TextNormalizer;
import java.util.List;
import java.util.Locale;

public final class ItemMatcher {
   private ItemMatcher() {
   }

   public static boolean a(ItemEntry var0, Lot var1) {
      if (var0 == null || var1 == null) {
         return false;
      }

      if (a(var0)) {
         return a(var1);
      }

      if (b(var0)) {
         return b(var1);
      }

      if (c(var0)) {
         return c(var1);
      }

      if (d(var0)) {
         return d(var1);
      }

      if (e(var0)) {
         return b(var0, var1);
      }

      if (f(var0)) {
         return e(var1);
      }

      if (g(var0)) {
         return f(var1);
      }

      if (h(var0)) {
         return g(var1);
      }

      if (i(var0)) {
         return h(var1);
      }

      if (j(var0)) {
         return i(var1);
      }

      if (k(var0)) {
         return j(var1);
      }

      if (l(var0)) {
         return k(var1);
      }

      if (var0.matchAnySword) {
         if (var1.itemId == null || !var1.itemId.endsWith("_sword")) {
            return false;
         }
      } else if (var0.itemId == null || !var0.itemId.equals(var1.itemId)) {
         return false;
      }

      if (var0.d() && var0.minDurabilityPercent > 0 && var1.durability < var0.minDurabilityPercent) {
         return false;
      }

      if (var0.potionEffect != null && !var0.potionEffect.isBlank() && !var1.potionEffects.contains(var0.potionEffect)) {
         return false;
      }

      List var2 = var0.c();
      boolean var3 = var2 != null && !var2.isEmpty();
      boolean var4 = false;
      if (var3) {
         for (Marker var6 : (Iterable<Marker>)(Iterable<?>)(var2)) {
            if (a(var6, var1)) {
               var4 = true;
               break;
            }
         }
      }

      boolean var8 = true;
      if (var0.loreKeyword != null && !var0.loreKeyword.isBlank()) {
         boolean var9 = TextNormalizer.b(var1.lore, var0.loreKeyword);
         boolean var7 = TextNormalizer.b(var1.name, var0.loreKeyword);
         var8 = var9 || var7;
      }

      if (var0.requireCustom) {
         if (!var4 && (!var8 || var0.loreKeyword == null || var0.loreKeyword.isBlank())) {
            return false;
         }
      } else if (var3 && !var4 && (!var8 || var0.loreKeyword == null || var0.loreKeyword.isBlank())) {
         return false;
      }

      if (!var8) {
         return false;
      }

      boolean var10 = var0.displayName != null && !var0.displayName.isBlank();
      if (var10) {
         String var11 = var0.displayName.replaceAll("\\s*\\([^)]*\\)", "").replaceAll("\\s*\\[[^]]*\\]", "").trim();
         if (!var0.matchAnySword && !TextNormalizer.a(var11, var1.name)) {
            return false;
         }
      }

      return true;
   }

   private static boolean a(ItemEntry var0) {
      if (var0 != null && var0.itemId != null && var0.itemId.endsWith("_sword")) {
         String var1 = var0.loreKeyword == null ? "" : var0.loreKeyword.toLowerCase(Locale.ROOT);
         if (!var1.contains("острота 7") && !var1.contains("острота vii") && !var1.contains("sharpness 7") && !var1.contains("sharpness vii")) {
            String var2 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
            return var2.contains("острота 7") || var2.contains("острота vii") || var2.contains("sharpness 7") || var2.contains("sharpness vii");
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean a(Lot var0) {
      if (var0 == null || var0.itemId == null || !var0.itemId.endsWith("_sword")) {
         return false;
      } else if (var0.sharpness >= 7) {
         return true;
      } else {
         String var1 = (var0.name == null ? "" : var0.name) + " " + (var0.lore == null ? "" : var0.lore);
         String var2 = TextNormalizer.a(var1);
         if (!var2.contains("острота 7") && !var2.contains("острота vii") && !var2.contains("sharpness 7") && !var2.contains("sharpness vii")) {
            String var3 = var1.toLowerCase(Locale.ROOT);
            return var3.contains("острота 7")
               || var3.contains("острота vii")
               || var3.contains("sharpness 7")
               || var3.contains("sharpness vii")
               || var3.contains("острота Ⅶ")
               || var3.contains("sharpness Ⅶ")
               || var3.contains("острота ⅶ")
               || var3.contains("sharpness ⅶ");
         } else {
            return true;
         }
      }
   }

   private static boolean b(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      String var2 = var0.searchQuery == null ? "" : var0.searchQuery.toLowerCase(Locale.ROOT);
      if (!var2.contains("урон 3") && !var2.contains("урон iii")) {
         if (!var1.contains("мифическая") || !var1.contains("урон 3") && !var1.contains("урон iii")) {
            if (var0.markers != null) {
               for (Marker var4 : var0.markers) {
                  if (var4 != null && var4.value != null && (var4.value.contains("мифическая_сфера3") || var4.value.contains("mythical3"))) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean b(Lot var0) {
      if (var0 != null && var0.itemId != null && var0.itemId.equals("minecraft:player_head")) {
         String var1 = ((var0.name == null ? "" : var0.name) + " " + (var0.lore == null ? "" : var0.lore)).toLowerCase(Locale.ROOT);
         boolean var2 = var1.contains("урон 3") || var1.contains("урон iii") || var1.contains("damage 3") || var1.contains("damage iii");
         boolean var3 = var1.contains("броня 2") || var1.contains("броня ii") || var1.contains("armor 2") || var1.contains("armor ii");
         if (var2 && var3) {
            return true;
         }

         if (var0.customData != null) {
            for (String var5 : var0.customData.values()) {
               if (var5 != null && (var5.contains("мифическая_сфера3") || var5.contains("mythical3"))) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean c(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      String var2 = var0.searchQuery == null ? "" : var0.searchQuery.toLowerCase(Locale.ROOT);
      if (!var2.contains("броня 3") && !var2.contains("броня iii")) {
         if (!var1.contains("мифическая") || !var1.contains("броня 3") && !var1.contains("броня iii")) {
            if (var0.markers != null) {
               for (Marker var4 : var0.markers) {
                  if (var4 != null && var4.value != null && (var4.value.contains("мифическая_сфера2") || var4.value.contains("mythical2"))) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean c(Lot var0) {
      if (var0 != null && var0.itemId != null && var0.itemId.equals("minecraft:player_head")) {
         String var1 = ((var0.name == null ? "" : var0.name) + " " + (var0.lore == null ? "" : var0.lore)).toLowerCase(Locale.ROOT);
         boolean var2 = var1.contains("броня 3") || var1.contains("броня iii") || var1.contains("armor 3") || var1.contains("armor iii");
         boolean var3 = var1.contains("урон 2") || var1.contains("урон ii") || var1.contains("damage 2") || var1.contains("damage ii");
         if (var2 && var3) {
            return true;
         }

         if (var0.customData != null) {
            for (String var5 : var0.customData.values()) {
               if (var5 != null && (var5.contains("мифическая_сфера2") || var5.contains("mythical2"))) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean d(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      String var2 = var0.loreKeyword == null ? "" : var0.loreKeyword.toLowerCase(Locale.ROOT);
      String var3 = var0.searchQuery == null ? "" : var0.searchQuery.toLowerCase(Locale.ROOT);
      if (!var1.contains("скорость 3")
         && !var1.contains("скорость iii")
         && !var2.contains("скорость iii")
         && !var2.contains("скорость 3")
         && !var3.contains("скорость 3")) {
         if (var0.markers != null) {
            for (Marker var5 : var0.markers) {
               if (var5 != null && var5.value != null && (var5.value.contains("легендарная_сфера_скорость") || var5.value.contains("speed3"))) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean d(Lot var0) {
      if (var0 != null && var0.itemId != null && var0.itemId.equals("minecraft:player_head")) {
         String var1 = ((var0.name == null ? "" : var0.name) + " " + (var0.lore == null ? "" : var0.lore)).toLowerCase(Locale.ROOT);
         if (!var1.contains("урон") && !var1.contains("броня") && !var1.contains("damage") && !var1.contains("armor")) {
            boolean var2 = var1.contains("скорость 3") || var1.contains("скорость iii") || var1.contains("speed 3") || var1.contains("speed iii");
            if (var2) {
               return true;
            }

            if (var0.customData != null) {
               for (String var4 : var0.customData.values()) {
                  if (var4 != null && (var4.contains("легендарная_сфера_скорость") || var4.contains("hms-speed") || var4.contains("speed3"))) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean e(ItemEntry var0) {
      if (var0 != null && var0.itemId != null && var0.itemId.equals("minecraft:tripwire_hook")) {
         String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
         return var1.contains("отмычк");
      } else {
         return false;
      }
   }

   private static boolean b(ItemEntry var0, Lot var1) {
      if (var1 != null && var1.itemId != null && var1.itemId.equals("minecraft:tripwire_hook")) {
         String var2 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
         String var3 = "";
         String var4 = "";
         if (var2.contains("обычн")) {
            var3 = "обычн";
            var4 = "normal";
         } else if (var2.contains("редк")) {
            var3 = "редк";
            var4 = "rare";
         } else if (var2.contains("уникальн")) {
            var3 = "уникальн";
            var4 = "unique";
         } else if (var2.contains("секретн")) {
            var3 = "секретн";
            var4 = "secret";
         }

         if (var1.pbv != null && !var4.isEmpty()) {
            String var5 = var1.pbv.get("mysticalship:ship-barrel-key");
            if (var5 != null && var5.equalsIgnoreCase(var4)) {
               return true;
            }
         }

         String var6 = ((var1.name == null ? "" : var1.name) + " " + (var1.lore == null ? "" : var1.lore)).toLowerCase(Locale.ROOT);
         return !var3.isEmpty() && var6.contains(var3);
      } else {
         return false;
      }
   }

   private static boolean f(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:dragon_egg".equals(var0.itemId)) {
         return true;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      return var1.contains("осколочное") && (var1.contains("энд") || var1.contains("дракон"));
   }

   private static boolean e(Lot var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:dragon_egg".equals(var0.itemId)) {
         return true;
      }

      String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
      return var1.contains("осколочное") && var1.contains("энд");
   }

   private static boolean g(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:enchanted_golden_apple".equals(var0.itemId)) {
         return true;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      return var1.contains("зачарованное") && (var1.contains("яблок") || var1.contains("золот"));
   }

   private static boolean f(Lot var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:enchanted_golden_apple".equals(var0.itemId)) {
         return true;
      }

      String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
      return var1.contains("зачарованное") && (var1.contains("яблок") || var1.contains("золот"));
   }

   private static boolean h(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:snowball".equals(var0.itemId)) {
         return true;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      return var1.contains("снежок") || var1.contains("снеж");
   }

   private static boolean g(Lot var0) {
      if (var0 == null) {
         return false;
      }

      if ("minecraft:snowball".equals(var0.itemId)) {
         return true;
      }

      String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
      return var1.contains("снежок") || var1.contains("снеж");
   }

   private static boolean i(ItemEntry var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
      String var2 = var0.searchQuery == null ? "" : var0.searchQuery.toLowerCase(Locale.ROOT);
      return var1.contains("черепаш") || var2.contains("черепаш");
   }

   private static boolean h(Lot var0) {
      if (var0 == null || var0.itemId == null) {
         return false;
      } else if (!var0.itemId.equals("minecraft:potion") && !var0.itemId.equals("minecraft:splash_potion") && !var0.itemId.equals("minecraft:lingering_potion")
         )
       {
         return false;
      } else {
         String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
         String var2 = (var0.lore == null ? "" : var0.lore).toLowerCase(Locale.ROOT);
         if (!var1.contains("черепаш") && !var2.contains("черепаш")) {
            return var2.contains("сопротивлен") && var2.contains("замедлен")
               ? true
               : var0.potionEffects != null && var0.potionEffects.contains("minecraft:slowness") && var0.potionEffects.contains("minecraft:resistance");
         } else {
            return true;
         }
      }
   }

   private static boolean j(ItemEntry var0) {
      if (var0 != null && "minecraft:tnt".equals(var0.itemId)) {
         String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
         String var2 = var0.loreKeyword == null ? "" : var0.loreKeyword.toLowerCase(Locale.ROOT);
         return var1.contains("динамит а") || var1.contains("динамит a") || var2.contains("3 раз");
      } else {
         return false;
      }
   }

   private static boolean i(Lot var0) {
      if (var0 != null && "minecraft:tnt".equals(var0.itemId)) {
         String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
         String var2 = (var0.lore == null ? "" : var0.lore).toLowerCase(Locale.ROOT);
         if (var2.contains("3 раз") || var2.contains("3 раза")) {
            return true;
         }

         if (!var1.contains("[a]") && !var1.contains("[а]") && !var1.contains("динамит а") && !var1.contains("динамит a")) {
            if (var0.customData != null) {
               String var3 = var0.customData.get("pyrotechnic-item.name");
               if (var3 != null && (var3.equalsIgnoreCase("A") || var3.equalsIgnoreCase("A1"))) {
                  return true;
               }
            }

            return false;
         } else {
            return !var1.contains("b") && !var1.contains("б") && !var1.contains("b2") && !var1.contains("б2");
         }
      } else {
         return false;
      }
   }

   private static boolean k(ItemEntry var0) {
      if (var0 != null && "minecraft:tnt".equals(var0.itemId)) {
         String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
         String var2 = var0.loreKeyword == null ? "" : var0.loreKeyword.toLowerCase(Locale.ROOT);
         return (var1.contains("динамит б") || var1.contains("динамит b") || var2.contains("10 раз")) && !var1.contains("б2") && !var1.contains("b2");
      } else {
         return false;
      }
   }

   private static boolean j(Lot var0) {
      if (var0 != null && "minecraft:tnt".equals(var0.itemId)) {
         String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
         String var2 = (var0.lore == null ? "" : var0.lore).toLowerCase(Locale.ROOT);
         if (var1.contains("b2") || var1.contains("б2")) {
            return false;
         }

         if (var2.contains("10 раз")) {
            return true;
         }

         if (!var1.contains("[b]") && !var1.contains("[б]") && !var1.contains("динамит б") && !var1.contains("динамит b")) {
            if (var0.customData != null) {
               String var3 = var0.customData.get("pyrotechnic-item.name");
               if (var3 != null && (var3.equalsIgnoreCase("B") || var3.equalsIgnoreCase("B1"))) {
                  return true;
               }
            }

            return false;
         } else {
            return !var1.contains("b2") && !var1.contains("б2");
         }
      } else {
         return false;
      }
   }

   private static boolean l(ItemEntry var0) {
      if (var0 != null && "minecraft:tnt".equals(var0.itemId)) {
         String var1 = var0.displayName == null ? "" : var0.displayName.toLowerCase(Locale.ROOT);
         return var1.equals("динамит") || var1.equals("tnt");
      } else {
         return false;
      }
   }

   private static boolean k(Lot var0) {
      if (var0 == null || !"minecraft:tnt".equals(var0.itemId)) {
         return false;
      } else if (!i(var0) && !j(var0)) {
         String var1 = (var0.name == null ? "" : var0.name).toLowerCase(Locale.ROOT);
         return !var1.contains("[")
               && !var1.contains("]")
               && !var1.contains("b2")
               && !var1.contains("б2")
               && !var1.contains("с4")
               && !var1.contains("c4")
               && !var1.contains("волна")
               && !var1.contains("стиллер")
            ? var0.customData == null || var0.customData.isEmpty()
            : false;
      } else {
         return false;
      }
   }

   public static boolean a(Marker var0, Lot var1) {
      if (var0 != null && var0.type != null && var0.key != null && var1 != null) {
         switch (var0.type) {
            case PBV:
               String var3 = var1.pbv.get(var0.key);
               if (var3 == null) {
                  return false;
               }

               return var0.value == null || var0.value.isEmpty() || var3.equals(var0.value);
            case CUSTOM_ENCH:
               return var1.customEnch.contains(var0.key);
            case CUSTOM_DATA:
               String var2 = var1.customData.get(var0.key);
               if (var2 == null) {
                  return false;
               }

               return var0.value == null || var0.value.isEmpty() || var2.equals(var0.value);
            default:
               return false;
         }
      } else {
         return false;
      }
   }
}
