package brokke.cc.util;

import brokke.cc.core.AutoBuyEntry;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;

public final class DeltaInventoryUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private DeltaInventoryUtil() {
   }

   public static int a(Item var0) {
      if (mc.player == null) {
         return 0;
      }

      int var1 = 0;

      for (int var2 = 0; var2 < 36; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.getItem() == var0) {
            var1 += var3.getCount();
         }
      }

      return var1;
   }

   public static boolean a(ItemStack var0, RegistryKey<Enchantment> var1, int var2) {
      return mc.world == null
         ? false
         : var0.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
               .getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(var1))
            >= var2;
   }

   public static boolean a(ItemStack var0, String var1) {
      return mc.player == null
         ? false
         : var0.getTooltip(TooltipContext.DEFAULT, mc.player, TooltipType.BASIC).stream().anyMatch(var1xx -> var1xx.getString().contains(var1));
   }

   public static int a(Item var0, boolean var1) {
      if (mc.player == null) {
         return -1;
      }

      int var2 = var1 ? 9 : 36;

      for (int var3 = 0; var3 < var2; var3++) {
         ItemStack var4 = mc.player.getInventory().getStack(var3);
         if (!var4.isEmpty() && var4.getItem() == var0) {
            return var3;
         }
      }

      return -1;
   }

   public static int b(Item var0) {
      return a(var0, false);
   }

   public static int a(Item var0, boolean var1, boolean var2) {
      if (mc.player == null) {
         return -1;
      }

      int var3 = var1 ? 9 : 36;

      for (int var4 = 0; var4 < var3; var4++) {
         ItemStack var5 = mc.player.getInventory().getStack(var4);
         if (!var5.isEmpty() && var5.getItem() == var0 && (!var2 || !var5.hasGlint())) {
            return var4;
         }
      }

      return -1;
   }

   public static int a(boolean var0) {
      if (mc.player == null) {
         return -1;
      }

      int var1 = var0 ? 9 : 36;

      for (int var2 = 0; var2 < var1; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (a(var3)) {
            return var2;
         }
      }

      for (int var4 = 0; var4 < var1; var4++) {
         ItemStack var5 = mc.player.getInventory().getStack(var4);
         if (!var5.isEmpty() && var5.isOf(Items.SPLASH_POTION)) {
            return var4;
         }
      }

      return -1;
   }

   public static boolean a(ItemStack var0) {
      if (var0 == null || var0.isEmpty() || !var0.isOf(Items.SPLASH_POTION)) {
         return false;
      }

      if (PotionUtility.a(var0, StatusEffects.INSTANT_HEALTH)) {
         return true;
      }

      String var1 = var0.getName().getString().toLowerCase();
      return !var1.contains("исцелен") && !var1.contains("святая вода") && !var1.contains("heal")
         ? a(var0, "исцелен") || a(var0, "Исцелен") || a(var0, "Instant Health") || a(var0, "Мгновенное лечение")
         : true;
   }

   public static int a(ItemStack var0, boolean var1) {
      if (mc.player != null && var0 != null && !var0.isEmpty()) {
         int var2 = var1 ? 9 : 36;

         for (int var3 = 0; var3 < var2; var3++) {
            ItemStack var4 = mc.player.getInventory().getStack(var3);
            if (!var4.isEmpty()
               && var4.getItem() == var0.getItem()
               && Objects.equals(var4.get(DataComponentTypes.ATTRIBUTE_MODIFIERS), var0.get(DataComponentTypes.ATTRIBUTE_MODIFIERS))) {
               return var3;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static int b(ItemStack var0, boolean var1) {
      if (mc.player != null && var0 != null && !var0.isEmpty()) {
         AutoBuyEntry var2 = Arrays.stream(AutoBuyEntry.values()).filter(var1x -> var1x.a(var0)).findFirst().orElse(null);
         if (var2 == null) {
            return a(var0.getItem(), var1);
         }

         int var3 = IntStream.range(0, var1 ? 9 : 36).filter(var1x -> var2.a(mc.player.getInventory().getStack(var1x))).findFirst().orElse(-1);
         return var3 == -1 && !var1 ? IntStream.range(0, 36).filter(var1x -> {
            BundleContentsComponent var2x = mc.player.getInventory().getStack(var1x).get(DataComponentTypes.BUNDLE_CONTENTS);
            if (var2x != null) {
               Stream<ItemStack> var3x = var2x.stream();
               return var3x.anyMatch(var2::a);
            } else {
               return false;
            }
         }).findFirst().orElse(-1) : var3;
      } else {
         return -1;
      }
   }

   public static int a(ItemStack var0, ItemStack var1) {
      if (var0 != null && !var0.isEmpty() && var1 != null && !var1.isEmpty()) {
         AutoBuyEntry var2 = Arrays.stream(AutoBuyEntry.values()).filter(var1xx -> var1xx.a(var1)).findFirst().orElse(null);
         BundleContentsComponent var3 = var0.get(DataComponentTypes.BUNDLE_CONTENTS);
         return var3 == null ? -1 : IntStream.range(0, var3.size()).filter(var3xx -> {
            ItemStack var4 = var3.get(var3xx);
            return var2 != null ? var2.a(var4) : var4.getItem() == var1.getItem();
         }).findFirst().orElse(-1);
      } else {
         return -1;
      }
   }

   public static int c(ItemStack var0, boolean var1) {
      if (mc.player != null && var0 != null && !var0.isEmpty()) {
         AutoBuyEntry var2 = Arrays.stream(AutoBuyEntry.values()).filter(var1x -> var1x.a(var0)).findFirst().orElse(null);
         return var2 == null
            ? a(var0.getItem())
            : IntStream.range(0, var1 ? 9 : 36).mapToObj(var0x -> mc.player.getInventory().getStack(var0x)).mapToInt(var1x -> {
               BundleContentsComponent var2x = var1x.get(DataComponentTypes.BUNDLE_CONTENTS);
               if (var2x != null) {
                  return var2x.stream().filter(var2::a).mapToInt(ItemStack::getCount).sum();
               } else {
                  return var2.a(var1x) ? var1x.getCount() : 0;
               }
            }).sum();
      } else {
         return 0;
      }
   }

   public static int c(Item var0) {
      if (mc.player == null) {
         return -1;
      }

      int var1 = -1;

      for (int var2 = 0; var2 < 45; var2++) {
         if (var2 != 40) {
            ItemStack var3 = mc.player.getInventory().getStack(var2);
            if (var3.getItem() == var0) {
               AttributeModifiersComponent var4 = var3.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
               if (var4 != null && !var4.modifiers().isEmpty()) {
                  return var2;
               }

               if (var1 == -1) {
                  var1 = var2;
               }
            }
         }
      }

      return var1;
   }

   public static int a() {
      if (mc.world != null && mc.player != null) {
         Registry var0 = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
         Reference var1 = var0.getOrThrow(Enchantments.PROTECTION);
         int var2 = -1;
         double var3 = 0.0;

         for (int var5 = 0; var5 < 36; var5++) {
            ItemStack var6 = mc.player.getInventory().getStack(var5);
            EquippableComponent var7 = var6.get(DataComponentTypes.EQUIPPABLE);
            if (var7 != null && var7.slot() == EquipmentSlot.CHEST && !var6.isEmpty() && !var6.isOf(Items.ELYTRA)) {
               double var8 = EnchantmentHelper.getLevel(var1, var6);
               AttributeModifiersComponent var10 = var6.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
               if (var10 != null) {
                  for (Entry var12 : var10.modifiers()) {
                     RegistryEntry var13 = var12.attribute();
                     if (var13.equals(EntityAttributes.ARMOR) || var13.equals(EntityAttributes.ARMOR_TOUGHNESS)) {
                        var8 += var12.modifier().value();
                     }
                  }
               }

               if (var8 > var3) {
                  var3 = var8;
                  var2 = var5;
               }
            }
         }

         return var2;
      } else {
         return -1;
      }
   }
}
