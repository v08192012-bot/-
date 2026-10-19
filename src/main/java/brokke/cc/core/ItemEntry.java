package brokke.cc.core;

import brokke.cc.nav.Marker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemEntry {
   public String itemId;
   public String displayName;
   public String searchQuery;
   public String loreKeyword;
   public String potionEffect;
   public long maxUnitPrice;
   public long marketPrice;
   public long minUnitPrice;
   public int minDurabilityPercent;
   public boolean isArmor;
   public boolean matchAnySword;
   public boolean requireCustom;
   public boolean enabled = true;
   public boolean parseEnabled = true;
   public List<Marker> markers = new ArrayList<>();

   public ItemEntry() {
   }

   public ItemEntry(String var1, long var2) {
      this.itemId = var1;
      this.maxUnitPrice = var2;
   }

   public ItemEntry(String var1, String var2, long var3) {
      this.itemId = var1;
      this.displayName = var2;
      this.maxUnitPrice = var3;
   }

   public boolean a(String var1) {
      if (var1 == null) {
         return false;
      } else {
         return this.matchAnySword ? var1.endsWith("_sword") : var1.equals(this.itemId);
      }
   }

   public String a() {
      String var1 = this.displayName != null && !this.displayName.isBlank() ? this.displayName : this.itemId;
      if (this.loreKeyword != null && !this.loreKeyword.isBlank()) {
         var1 = var1 + " [" + this.loreKeyword + "]";
      }

      if (this.matchAnySword) {
         var1 = "Меч+" + var1;
      }

      return var1;
   }

   public String b() {
      if (this.searchQuery != null && !this.searchQuery.isBlank()) {
         return this.searchQuery;
      } else {
         return this.displayName != null && !this.displayName.isBlank() ? this.displayName : this.itemId;
      }
   }

   public List<Marker> c() {
      if (this.markers != null && !this.markers.isEmpty()) {
         return this.markers;
      }

      String var1 = this.displayName == null ? "" : this.displayName.trim().toLowerCase(Locale.ROOT);
      String var2 = this.searchQuery == null ? "" : this.searchQuery.trim().toLowerCase(Locale.ROOT);

      for (ItemEntry var4 : ItemDatabase.LIST) {
         if (var4.markers != null && !var4.markers.isEmpty()) {
            String var5 = var4.displayName == null ? "" : var4.displayName.toLowerCase(Locale.ROOT);
            String var6 = var4.searchQuery == null ? "" : var4.searchQuery.toLowerCase(Locale.ROOT);
            boolean var7 = (var4.matchAnySword || var4.itemId != null && var4.itemId.equals(this.itemId))
               && (!var4.matchAnySword || this.itemId != null && this.itemId.endsWith("_sword"));
            if (var7) {
               if (!var1.isEmpty() && var1.equals(var5)) {
                  return var4.markers;
               }

               if (!var2.isEmpty() && var2.equals(var6)) {
                  return var4.markers;
               }
            }
         }
      }

      return this.markers != null ? this.markers : new ArrayList<>();
   }

   public boolean d() {
      if (this.displayName != null) {
         String var1 = this.displayName.toLowerCase(Locale.ROOT);
         if (var1.contains("джейк") || var1.contains("jake")) {
            return false;
         }
      }

      if (this.searchQuery != null) {
         String var3 = this.searchQuery.toLowerCase(Locale.ROOT);
         if (var3.contains("джейк") || var3.contains("jake")) {
            return false;
         }
      }

      if (this.markers != null) {
         for (Marker var2 : this.markers) {
            if ("enchantments:spawner-getter-enchant".equals(var2.key)) {
               return false;
            }
         }
      }

      if (this.isArmor || this.matchAnySword) {
         return true;
      }

      if (this.itemId == null) {
         return false;
      }

      String var5 = this.itemId.toLowerCase(Locale.ROOT);
      return var5.endsWith("_sword")
         || var5.endsWith("_pickaxe")
         || var5.endsWith("_helmet")
         || var5.endsWith("_chestplate")
         || var5.endsWith("_leggings")
         || var5.endsWith("_boots")
         || var5.endsWith("_axe")
         || var5.endsWith("_shovel")
         || var5.endsWith("_hoe")
         || var5.contains("elytra")
         || var5.contains("mace");
   }
}
