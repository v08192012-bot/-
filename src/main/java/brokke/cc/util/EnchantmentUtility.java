package brokke.cc.util;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public final class EnchantmentUtility {
   public static void a(ItemStack var0, Object2IntMap<RegistryEntry<Enchantment>> var1) {
      var1.clear();
      if (!var0.isEmpty()) {
         ItemEnchantmentsComponent var2 = var0.getItem() == Items.ENCHANTED_BOOK ? var0.get(DataComponentTypes.STORED_ENCHANTMENTS) : var0.getEnchantments();
         if (var2 != null) {
            for (Entry var4 : var2.getEnchantmentEntries()) {
               var1.put((RegistryEntry<net.minecraft.enchantment.Enchantment>)var4.getKey(), var4.getIntValue());
            }
         }
      }
   }

   @SafeVarargs
   public static boolean a(ItemStack var0, RegistryKey<Enchantment>... var1) {
      if (var0.isEmpty()) {
         return false;
      }

      Object2IntArrayMap var2 = new Object2IntArrayMap();
      a(var0, var2);

      for (RegistryKey var6 : var1) {
         if (!b(var2, var6)) {
            return false;
         }
      }

      return true;
   }

   public static int a(ItemStack var0, RegistryKey<Enchantment> var1) {
      if (var0.isEmpty()) {
         return 0;
      }

      Object2IntArrayMap var2 = new Object2IntArrayMap();
      a(var0, var2);
      return a(var2, var1);
   }

   public static int a(Object2IntMap<RegistryEntry<Enchantment>> var0, RegistryKey<Enchantment> var1) {
      ObjectIterator var2 = Object2IntMaps.fastIterable(var0).iterator();

      while (var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         if (((RegistryEntry<net.minecraft.enchantment.Enchantment>)var3.getKey()).matchesKey(var1)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   private static boolean b(Object2IntMap<RegistryEntry<Enchantment>> var0, RegistryKey<Enchantment> var1) {
      ObjectIterator var2 = var0.keySet().iterator();

      while (var2.hasNext()) {
         RegistryEntry var3 = (RegistryEntry)var2.next();
         if (var3.matchesKey(var1)) {
            return true;
         }
      }

      return false;
   }

   private EnchantmentUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
