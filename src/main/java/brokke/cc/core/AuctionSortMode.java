package brokke.cc.core;

import brokke.cc.util.TextNormalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;

enum AuctionSortMode {
   CHEAPEST("сначала дешевые"),
   EXPENSIVE("сначала дорогие"),
   CHEAPEST_PER_UNIT("сначала дешевые за ед. товара"),
   EXPENSIVE_PER_UNIT("сначала дорогие за ед. товара"),
   NEWEST("сначала новые"),
   OLDEST("сначала старые");

   static final String SORT_KEYWORD = "сортировка";
   private static final String SORT_ITEM = "minecraft:hopper";
   private static final String POSITIVE_MARKERS = "✔✓☑➜▶►»›>*";
   private static final String NEGATIVE_MARKERS = "✖✕✗✘×-";
   private static final AuctionSortMode[] ALL = values();
   private static final AuctionSortMode[] MATCH_ORDER = new AuctionSortMode[]{CHEAPEST_PER_UNIT, EXPENSIVE_PER_UNIT, CHEAPEST, EXPENSIVE, NEWEST, OLDEST};
   private final String label;
   private final String normalizedLabel;

   AuctionSortMode(String nullxx) {
      this.label = nullxx;
      this.normalizedLabel = TextNormalizer.a(nullxx);
   }

   int a(AuctionSortMode var1) {
      return (var1.ordinal() - this.ordinal() + ALL.length) % ALL.length;
   }

   String getLabel() {
      return this.label;
   }

   static int a(GenericContainerScreenHandler var0) {
      int var1 = var0.getRows() * 9;

      for (int var2 = 0; var2 < var1; var2++) {
         ItemStack var3 = var0.getSlot(var2).getStack();
         if (!var3.isEmpty()) {
            String var4 = Registries.ITEM.getId(var3.getItem()).toString();
            if (var4.equals("minecraft:hopper") && b(var3)) {
               return var2;
            }
         }
      }

      for (int var5 = 0; var5 < var1; var5++) {
         ItemStack var6 = var0.getSlot(var5).getStack();
         if (!var6.isEmpty() && b(var6)) {
            return var5;
         }
      }

      return -1;
   }

   static AuctionSortMode b(GenericContainerScreenHandler var0) {
      int var1 = a(var0);
      return var1 < 0 ? null : a(var0.getSlot(var1).getStack());
   }

   static AuctionSortMode a(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      LoreComponent var2 = var0.get(DataComponentTypes.LORE);
      if (var2 == null) {
         return null;
      }

      for (Text var4 : var2.lines()) {
         String var5 = var4.getString();
         AuctionSortMode var6 = a(var5);
         if (var6 != null) {
            var1.add(new AuctionSortMode.a(var6, var5, b(var5)));
         }
      }

      if (var1.isEmpty()) {
         return null;
      }

      HashMap<Character, Integer> var10 = new HashMap<>();

      for (AuctionSortMode.a var13 : (Iterable<AuctionSortMode.a>)(Iterable<?>)(var1)) {
         if (var13.marker != 0) {
            var10.merge(var13.marker, 1, Integer::sum);
         }
      }

      AuctionSortMode var12 = null;
      int var14 = Integer.MIN_VALUE;
      boolean var15 = false;

      for (AuctionSortMode.a var8 : (Iterable<AuctionSortMode.a>)(Iterable<?>)(var1)) {
         int var9 = a(var8, var10, var1.size());
         if (var9 > var14) {
            var12 = var8.mode;
            var14 = var9;
            var15 = false;
         } else if (var9 == var14) {
            var15 = true;
         }
      }

      return !var15 && var14 > 0 ? var12 : null;
   }

   private static boolean b(ItemStack var0) {
      if (TextNormalizer.b(var0.getName().getString(), "сортировка")) {
         return true;
      }

      LoreComponent var1 = var0.get(DataComponentTypes.LORE);
      if (var1 == null) {
         return false;
      }

      for (Text var3 : var1.lines()) {
         if (TextNormalizer.b(var3.getString(), "сортировка")) {
            return true;
         }
      }

      return false;
   }

   private static AuctionSortMode a(String var0) {
      String var1 = TextNormalizer.a(var0);
      if (var1.isEmpty()) {
         return null;
      }

      for (AuctionSortMode var5 : MATCH_ORDER) {
         if (var1.contains(var5.normalizedLabel)) {
            return var5;
         }
      }

      return null;
   }

   private static char b(String var0) {
      String var1 = var0 == null ? "" : var0.trim();
      if (var1.isEmpty()) {
         return '\u0000';
      }

      char var2 = var1.charAt(0);
      return Character.isLetterOrDigit(var2) ? '\u0000' : var2;
   }

   private static int a(AuctionSortMode.a var0, Map<Character, Integer> var1, int var2) {
      byte var3 = 1;
      if (var0.marker != 0) {
         int var4 = var1.getOrDefault(var0.marker, 0);
         if (var4 == 1 && var2 > 1 && var1.size() > 1) {
            var3 = (byte)(var3 + 20);
         }

         if ("✔✓☑➜▶►»›>*".indexOf(var0.marker) >= 0) {
            var3 = (byte)(var3 + 20);
         }

         if ("✖✕✗✘×-".indexOf(var0.marker) >= 0) {
            var3 = (byte)(var3 - 20);
         }
      }

      return var3;
   }

   static final class a {
      final AuctionSortMode mode;
      private final String raw;
      final char marker;

      a(AuctionSortMode var1, String var2, char var3) {
         this.mode = var1;
         this.raw = var2;
         this.marker = var3;
      }
   }
}
