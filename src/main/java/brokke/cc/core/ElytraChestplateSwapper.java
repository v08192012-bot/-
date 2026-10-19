package brokke.cc.core;

import brokke.cc.rotation.HolyWorldElytraUtil;
import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class ElytraChestplateSwapper {
   private static final int CHEST_SLOT = 6;

   private ElytraChestplateSwapper() {
   }

   public static boolean a(MinecraftClient var0) {
      return var0.player != null && var0.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   public static boolean b(MinecraftClient var0) {
      if (var0.player == null || var0.interactionManager == null || var0.currentScreen != null || var0.player.currentScreenHandler == null) {
         return false;
      }

      if (a(var0)) {
         return true;
      }

      Slot var1 = d(var0);
      if (var1 == null) {
         return false;
      }

      a(var0, var1.id);
      return true;
   }

   public static boolean c(MinecraftClient var0) {
      if (var0.player == null || var0.interactionManager == null || var0.currentScreen != null || var0.player.currentScreenHandler == null) {
         return false;
      }

      if (!a(var0)) {
         return true;
      }

      Slot var1 = e(var0);
      if (var1 == null) {
         return false;
      }

      a(var0, var1.id);
      return true;
   }

   private static void a(MinecraftClient var0, int var1) {
      int var2 = var0.player.currentScreenHandler.syncId;
      var0.interactionManager.clickSlot(var2, var1, 0, SlotActionType.PICKUP, var0.player);
      var0.interactionManager.clickSlot(var2, 6, 0, SlotActionType.PICKUP, var0.player);
      var0.interactionManager.clickSlot(var2, var1, 0, SlotActionType.PICKUP, var0.player);
   }

   private static Slot d(MinecraftClient var0) {
      return var0.player != null && var0.player.currentScreenHandler != null
         ? var0.player
            .currentScreenHandler
            .slots
            .stream()
            .filter(var0x -> var0x.id >= 9 && var0x.id <= 44)
            .filter(var0x -> var0x.getStack().isOf(Items.ELYTRA))
            .findFirst()
            .orElse(null)
         : null;
   }

   private static Slot e(MinecraftClient var0) {
      return var0.player != null && var0.player.currentScreenHandler != null
         ? var0.player
            .currentScreenHandler
            .slots
            .stream()
            .filter(var0x -> var0x.id >= 9 && var0x.id <= 44)
            .filter(var0x -> a(var0x.getStack()))
            .max(Comparator.comparingDouble(var0x -> b(var0x.getStack())))
            .orElse(null)
         : null;
   }

   private static boolean a(ItemStack var0) {
      return HolyWorldElytraUtil.a(var0);
   }

   private static double b(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         double var1 = 0.0;
         AttributeModifiersComponent var3 = var0.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
         if (var3 != null) {
            for (Entry var5 : var3.modifiers()) {
               if (var5.attribute() == EntityAttributes.ARMOR || var5.attribute() == EntityAttributes.ARMOR_TOUGHNESS) {
                  var1 += var5.modifier().value();
               }
            }
         }

         return var1;
      } else {
         return 0.0;
      }
   }
}
