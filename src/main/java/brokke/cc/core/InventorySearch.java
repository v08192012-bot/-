package brokke.cc.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class InventorySearch {
   private static final MinecraftClient a = MinecraftClient.getInstance();

   private InventorySearch() {
   }

   public static int a(Item var0) {
      if (a.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (a.player.getInventory().getStack(var1).isOf(var0)) {
            return var1;
         }
      }

      for (int var2 = 9; var2 < 36; var2++) {
         if (a.player.getInventory().getStack(var2).isOf(var0)) {
            return var2;
         }
      }

      return -1;
   }

   public static int a() {
      if (a.player == null) {
         return -1;
      }

      int var0 = -1;
      int var1 = -1;

      for (int var2 = 0; var2 < 36; var2++) {
         ItemStack var3 = a.player.getInventory().getStack(var2);
         if (var3.isOf(Items.TOTEM_OF_UNDYING)) {
            if (c(var3)) {
               var0 = var2;
               break;
            }

            if (var1 == -1) {
               var1 = var2;
            }
         }
      }

      return var0 != -1 ? var0 : var1;
   }

   public static int b(Item var0) {
      return var0 == Items.TOTEM_OF_UNDYING ? a() : a(var0);
   }

   public static int b() {
      if (a.player == null) {
         return -1;
      }

      for (int var0 = 0; var0 < 36; var0++) {
         if (b(a.player.getInventory().getStack(var0))) {
            return var0;
         }
      }

      return -1;
   }

   public static int c(Item var0) {
      if (a.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 36; var1++) {
         if (a.player.getInventory().getStack(var1).isOf(var0)) {
            return var1;
         }
      }

      return -1;
   }

   public static int c() {
      if (a.player == null) {
         return -1;
      }

      for (int var0 = 9; var0 < 36; var0++) {
         if (a.player.getInventory().getStack(var0).isOf(Items.ELYTRA)) {
            return var0;
         }
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (a.player.getInventory().getStack(var1).isOf(Items.ELYTRA)) {
            return var1;
         }
      }

      return -1;
   }

   public static int d() {
      if (a.player == null) {
         return -1;
      }

      for (int var0 = 9; var0 < 36; var0++) {
         if (a(a.player.getInventory().getStack(var0))) {
            return var0;
         }
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (a(a.player.getInventory().getStack(var1))) {
            return var1;
         }
      }

      return -1;
   }

   public static boolean a(ItemStack var0) {
      return var0.isEmpty()
         ? false
         : var0.isOf(Items.DIAMOND_CHESTPLATE)
            || var0.isOf(Items.NETHERITE_CHESTPLATE)
            || var0.isOf(Items.IRON_CHESTPLATE)
            || var0.isOf(Items.GOLDEN_CHESTPLATE)
            || var0.isOf(Items.CHAINMAIL_CHESTPLATE)
            || var0.isOf(Items.LEATHER_CHESTPLATE);
   }

   public static boolean b(ItemStack var0) {
      if (!var0.isEmpty() && var0.isOf(Items.PLAYER_HEAD)) {
         NbtComponent var1 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var1 == null) {
            return false;
         }

         NbtCompound var2 = var1.copyNbt();
         return var2.getCompound("SkullOwner")
            .flatMap(var0x -> var0x.getCompound("Properties"))
            .flatMap(var0x -> var0x.getList("textures"))
            .filter(var0x -> !var0x.isEmpty())
            .flatMap(var0x -> var0x.getCompound(0))
            .flatMap(var0x -> var0x.getString("Value"))
            .map(
               var0x -> var0x.equals(
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0="
               )
            )
            .orElse(false);
      } else {
         return false;
      }
   }

   public static boolean c(ItemStack var0) {
      if (var0.isEmpty()) {
         return false;
      }

      ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
      ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
      return var1 != null && !var1.isEmpty() || var2 != null && !var2.isEmpty();
   }

   public static Text d(ItemStack var0) {
      return var0.getName();
   }
}
