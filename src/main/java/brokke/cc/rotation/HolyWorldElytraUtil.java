package brokke.cc.rotation;

import brokke.cc.module.FunctionManager;
import brokke.cc.module.misc.ServerHelper;
import brokke.cc.util.ServerUtility;
import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class HolyWorldElytraUtil {
   private static final int CHEST_SLOT = 6;

   private HolyWorldElytraUtil() {
   }

   public static boolean a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0.player != null && var0.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   public static boolean b() {
      ServerHelper var0 = FunctionManager.a(ServerHelper.class);
      return ServerUtility.f() || var0 != null && var0.l();
   }

   public static boolean c() {
      return a(b());
   }

   public static boolean a(boolean var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var0 && var1.player != null && var1.interactionManager != null && var1.currentScreen == null) {
         Slot var2 = a() ? e() : d();
         if (var2 != null) {
            int var3 = var1.player.currentScreenHandler.syncId;
            var1.interactionManager.clickSlot(var3, var2.id, 0, SlotActionType.PICKUP, var1.player);
            var1.interactionManager.clickSlot(var3, 6, 0, SlotActionType.PICKUP, var1.player);
            var1.interactionManager.clickSlot(var3, var2.id, 0, SlotActionType.PICKUP, var1.player);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Slot d() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0.player != null && var0.player.currentScreenHandler != null
         ? var0.player
            .currentScreenHandler
            .slots
            .stream()
            .filter(var0x -> a(var0x.id))
            .filter(var0x -> var0x.getStack().isOf(Items.ELYTRA))
            .findFirst()
            .orElse(null)
         : null;
   }

   private static Slot e() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0.player != null && var0.player.currentScreenHandler != null
         ? var0.player
            .currentScreenHandler
            .slots
            .stream()
            .filter(var0x -> a(var0x.id))
            .filter(var0x -> a(var0x.getStack()))
            .max(Comparator.comparingInt(var0x -> var0x.id))
            .orElse(null)
         : null;
   }

   private static boolean a(int var0) {
      return var0 >= 36 && var0 <= 44;
   }

   public static boolean a(ItemStack var0) {
      if (var0 == null || var0.isEmpty() || var0.isOf(Items.ELYTRA)) {
         return false;
      }

      if (var0.get(DataComponentTypes.GLIDER) != null) {
         return false;
      }

      EquippableComponent var1 = var0.get(DataComponentTypes.EQUIPPABLE);
      return var1 != null
         ? var1.slot() == EquipmentSlot.CHEST
         : var0.isOf(Items.NETHERITE_CHESTPLATE)
            || var0.isOf(Items.DIAMOND_CHESTPLATE)
            || var0.isOf(Items.CHAINMAIL_CHESTPLATE)
            || var0.isOf(Items.IRON_CHESTPLATE)
            || var0.isOf(Items.GOLDEN_CHESTPLATE)
            || var0.isOf(Items.LEATHER_CHESTPLATE);
   }
}
