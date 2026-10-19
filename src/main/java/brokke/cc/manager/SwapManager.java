package brokke.cc.manager;

import brokke.cc.ui.HiddenInventoryScreen;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class SwapManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int OFFHAND_BUTTON = 40;

   public static int a(Predicate<ItemStack> var0) {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (var0.test(mc.player.getInventory().getStack(var1))) {
            return var1;
         }
      }

      for (int var2 = 9; var2 < 36; var2++) {
         if (var0.test(mc.player.getInventory().getStack(var2))) {
            return var2;
         }
      }

      return -1;
   }

   public static int a(ItemStack var0) {
      return var0 != null && !var0.isEmpty() ? a(var1 -> a(var1, var0)) : -1;
   }

   public static boolean b(ItemStack var0) {
      if (mc.player == null || mc.interactionManager == null) {
         return false;
      }

      if (var0 != null && !var0.isEmpty()) {
         if (a(mc.player.getOffHandStack(), var0)) {
            return true;
         }

         int var1 = a(var0);
         return var1 != -1 && a(var1);
      } else {
         return false;
      }
   }

   public static boolean a(int var0) {
      if (mc.player != null && mc.interactionManager != null && mc.player.networkHandler != null) {
         return var0 >= 0 && var0 <= 35 ? a(var0, 40) : false;
      } else {
         return false;
      }
   }

   public static boolean a() {
      return false;
   }

   public static boolean b() {
      if (mc.player == null) {
         return false;
      } else if (mc.currentScreen instanceof HiddenInventoryScreen) {
         mc.setScreen(null);
         return true;
      } else {
         return false;
      }
   }

   public static boolean a(int var0, int var1) {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      }

      if (var0 >= 0 && var0 <= 35) {
         int var2 = b(var0);
         if (var2 == 45 && var1 == 40) {
            return false;
         }

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var2, var1, SlotActionType.SWAP, mc.player);
         return true;
      } else {
         return false;
      }
   }

   public static int b(int var0) {
      return var0 < 9 ? var0 + 36 : var0;
   }

   public static boolean c() {
      if (mc.player == null || mc.player.networkHandler == null) {
         return false;
      }

      if (mc.player.isSpectator()) {
         return false;
      }

      mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      return true;
   }

   public static boolean a(ItemStack var0, ItemStack var1) {
      if (var0 == null || var1 == null || var0.isEmpty() || var1.isEmpty()) {
         return false;
      } else if (!var0.isOf(var1.getItem())) {
         return false;
      } else if (!var1.isOf(Items.TOTEM_OF_UNDYING)) {
         return var1.isOf(Items.PLAYER_HEAD) ? var0.getName().getString().equals(var1.getName().getString()) : true;
      } else {
         return !var1.hasEnchantments() ? true : var0.hasEnchantments() && var0.getEnchantments().equals(var1.getEnchantments());
      }
   }

   private SwapManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
