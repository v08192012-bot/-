package brokke.cc.util;

import brokke.cc.core.ArmorSlot;
import brokke.cc.core.HotbarSlot;
import brokke.cc.core.HotbarSlotsGroup;
import brokke.cc.core.InventorySlot;
import brokke.cc.core.ItemSlot;
import brokke.cc.core.OffhandSlot;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public final class InventoryUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static HotbarSlot a(int var0) {
      return new HotbarSlot(var0);
   }

   public static InventorySlot b(int var0) {
      return new InventorySlot(var0);
   }

   public static ArmorSlot c(int var0) {
      return new ArmorSlot(var0);
   }

   public static ArmorSlot a() {
      return c(3);
   }

   public static ArmorSlot b() {
      return c(2);
   }

   public static ArmorSlot c() {
      return c(1);
   }

   public static ArmorSlot d() {
      return c(0);
   }

   public static OffhandSlot e() {
      return new OffhandSlot();
   }

   public static boolean a(Item var0) {
      return e().a(var0);
   }

   public static boolean a(Predicate<ItemStack> var0) {
      return e().a(var0);
   }

   public static boolean f() {
      return e().e();
   }

   public static void a(ItemSlot var0, ItemSlot var1) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         var0.g();
         var1.g();
         if (!var1.e()) {
            var0.g();
         }
      }
   }

   public static void d(int var0) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 0, SlotActionType.QUICK_MOVE, mc.player);
      }
   }

   public static void a(int var0, int var1) {
      a(var0, var1, false);
   }

   public static void a(int var0, int var1, boolean var2) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 0, SlotActionType.PICKUP, mc.player);
         if (var2) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 0, SlotActionType.PICKUP, mc.player);
         }
      }
   }

   public static void b(int var0, int var1) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 1, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static void c(int var0, int var1) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 1, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static void d(int var0, int var1) {
      if (mc.getNetworkHandler() != null && mc.player != null && mc.interactionManager != null) {
         if (var0 < 0 || var0 == 45 && var1 == 40) {
            return;
         }

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, var1, SlotActionType.SWAP, mc.player);
      }
   }

   public static boolean a(ItemSlot var0, int var1) {
      HotbarSlot var2 = a(var1);
      a(var0, var2);
      return true;
   }

   public static boolean b(ItemSlot var0, int var1) {
      ArmorSlot var2 = c(var1);
      a(var0, var2);
      return true;
   }

   public static void a(ItemSlot var0) {
      if (var0 != null && var0.get45() != 45) {
         d(var0.get45(), 40);
      }
   }

   @NotNull
   public static HotbarSlot g() {
      return mc.player != null && mc.player.getInventory() != null ? a(mc.player.getInventory().getSelectedSlot()) : new HotbarSlot(0);
   }

   public static void e(int var0) {
      if (mc.player != null && mc.player.getInventory() != null && mc.getNetworkHandler() != null) {
         if (var0 < 0 || var0 > 8) {
            throw new IllegalArgumentException("Hotbar slot ID must be between 0 and 8");
         }

         mc.player.getInventory().setSelectedSlot(var0);
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
      }
   }

   public static void a(HotbarSlot var0) {
      e(var0.getSlotId());
   }

   public static boolean b(Item var0) {
      HotbarSlot var1 = new HotbarSlotsGroup().a(var0);
      if (var1 != null) {
         a(var1);
         return true;
      } else {
         return false;
      }
   }

   public static int b(Predicate<ItemStack> var0) {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         for (int var1 = 0; var1 < mc.player.currentScreenHandler.slots.size(); var1++) {
            ItemStack var2 = mc.player.currentScreenHandler.getSlot(var1).getStack();
            if (var0.test(var2)) {
               return var1;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static int c(Item var0) {
      return b(var1 -> var1.getItem() == var0);
   }

   public static void d(Item var0) {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.player.getOffHandStack().getItem() == var0) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         } else {
            for (int var1 = 0; var1 < 9; var1++) {
               if (mc.player.getInventory().getStack(var1).getItem() == var0) {
                  int var2 = mc.player.getInventory().getSelectedSlot();
                  e(var1);
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  e(var2);
                  return;
               }
            }
         }
      }
   }

   private InventoryUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
