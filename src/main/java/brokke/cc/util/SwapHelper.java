package brokke.cc.util;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.ui.HiddenInventoryScreen;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SwapHelper {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Queue<SwapHelper.c> swapQueue = new ArrayDeque<>();
   private static SwapHelper.c currentSwap = null;
   private static boolean initialized = false;
   private static String offhandOwner = null;

   public static void a() {
      if (!initialized) {
         EventBus.register(new SwapHelper.a());
         initialized = true;
      }
   }

   public static boolean b() {
      return false;
   }

   public static void a(SwapHelper.b var0, Runnable var1) {
      if (var1 != null) {
         try {
            var1.run();
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   public static boolean a(String var0) {
      if (offhandOwner != null && !offhandOwner.equals(var0)) {
         return false;
      }

      offhandOwner = var0;
      return true;
   }

   public static void b(String var0) {
      if (var0.equals(offhandOwner)) {
         offhandOwner = null;
      }
   }

   public static boolean c(String var0) {
      return offhandOwner != null && !offhandOwner.equals(var0);
   }

   public static boolean c() {
      return false;
   }

   public static void d() {
      if (mc.player != null && mc.currentScreen instanceof HiddenInventoryScreen) {
         mc.setScreen(null);
      }
   }

   public static boolean a(int var0) {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      }

      if (var0 >= 0 && var0 <= 35) {
         int var1 = b(var0);
         if (var1 == 45) {
            return false;
         }

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 40, SlotActionType.SWAP, mc.player);
         return true;
      } else {
         return false;
      }
   }

   private static void c(int var0) {
      if (mc.player != null && mc.interactionManager != null && var0 >= 0 && var0 <= 8) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, var0, SlotActionType.SWAP, mc.player);
      }
   }

   private static void b(int var0, int var1) {
      if (mc.player != null && mc.interactionManager != null && (var0 != 45 || var1 != 40)) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, var1, SlotActionType.SWAP, mc.player);
      }
   }

   private static int f() {
      if (mc.player == null) {
         return -1;
      }

      for (int var0 = 0; var0 < 9; var0++) {
         if (mc.player.getInventory().getStack(var0).isEmpty()) {
            return var0;
         }
      }

      return 0;
   }

   private static void d(int var0) {
      if (mc.player != null && mc.player.networkHandler != null && var0 >= 0 && var0 <= 8 && mc.player.getInventory().getSelectedSlot() != var0) {
         mc.player.getInventory().setSelectedSlot(var0);
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var0));
      }
   }

   public static void e() {
      if (mc.player != null && mc.player.networkHandler != null && !mc.player.isSpectator()) {
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }
   }

   public static int b(int var0) {
      return var0 < 9 ? var0 + 36 : var0;
   }

   public static boolean a(int var0, int var1) {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      } else if (var0 < 9 || var0 > 35) {
         return false;
      } else if (var1 >= 0 && var1 <= 8) {
         int var2 = b(var0);
         b(var2, var1);
         return true;
      } else {
         return false;
      }
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

   public static class a {
      @EventHandler(priority = EventPriority.HIGHEST)
      public void onUpdate(UpdateEvent var1) {
      }
   }

   public enum b {
      HOLYWORLD,
      GRIM;
   }

   public static class c {
      private final SwapHelper.b mode;
      private final Runnable action;
      private int tick = 0;

      public c(SwapHelper.b var1, Runnable var2) {
         this.mode = var1;
         this.action = var2;
      }

      public SwapHelper.b getMode() {
         return this.mode;
      }

      public int getTick() {
         return this.tick;
      }
   }
}
