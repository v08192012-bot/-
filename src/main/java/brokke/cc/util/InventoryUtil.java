package brokke.cc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static int a(Item var0) {
      if (mc.player == null) {
         return -1;
      }

      PlayerInventory var1 = mc.player.getInventory();

      for (int var2 = 0; var2 < 36; var2++) {
         if (var1.getStack(var2).getItem() == var0) {
            return var2;
         }
      }

      return -1;
   }

   public static int b(Item var0) {
      if (mc.player == null) {
         return -1;
      } else {
         return mc.player.getOffHandStack().getItem() == var0 ? 45 : -1;
      }
   }

   public static void a(int var0) {
      if (mc.player != null && mc.interactionManager != null) {
         int var1 = var0 < 9 ? var0 + 36 : var0;
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var1, 40, SlotActionType.SWAP, mc.player);
      }
   }

   public static void b(int var0) {
      if (mc.player != null && mc.player.networkHandler != null && var0 >= 0 && var0 <= 8) {
         mc.player.getInventory().setSelectedSlot(var0);
      }
   }

   public static int a() {
      return mc.player == null ? 0 : mc.player.getInventory().getSelectedSlot();
   }

   public static ItemStack b() {
      return mc.player == null ? ItemStack.EMPTY : mc.player.getOffHandStack();
   }

   public static boolean c() {
      return b().isEmpty();
   }

   public static ItemStack c(int var0) {
      return mc.player != null && var0 >= 0 && var0 <= 8 ? mc.player.getInventory().getStack(var0) : ItemStack.EMPTY;
   }

   public static ItemStack d(int var0) {
      if (mc.player == null) {
         return ItemStack.EMPTY;
      }

      EquipmentSlot[] var1 = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
      return var0 >= 0 && var0 <= 3 ? mc.player.getEquippedStack(var1[var0]) : ItemStack.EMPTY;
   }

   public static void e(int var0) {
      if (mc.player != null && mc.interactionManager != null) {
         SwapHelper.a(var0);
      }
   }
}
