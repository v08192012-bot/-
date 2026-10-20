package brokke.cc.util;

import brokke.cc.core.SlotSearchResult;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public final class HotbarUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static int cachedSlot = -1;

   public static boolean a(Item var0) {
      return var0 instanceof AxeItem ? true : Registries.ITEM.getId(var0).getPath().endsWith("_axe");
   }

   public static boolean b(Item var0) {
      return Registries.ITEM.getId(var0).getPath().endsWith("_pickaxe");
   }

   public static boolean c(Item var0) {
      return Registries.ITEM.getId(var0).getPath().endsWith("_sword");
   }

   public static boolean d(Item var0) {
      return var0 instanceof ShovelItem ? true : Registries.ITEM.getId(var0).getPath().endsWith("_shovel");
   }

   public static boolean e(Item var0) {
      if (var0 == null) {
         return false;
      } else {
         return !(var0 instanceof MaceItem) && var0 != Items.MACE ? Registries.ITEM.getId(var0).getPath().endsWith("mace") : true;
      }
   }

   public static SlotSearchResult a() {
      return a(var0 -> e(var0.getItem()));
   }

   public static boolean f(Item var0) {
      if (var0 == null) {
         return false;
      } else {
         return var0 == Items.WIND_CHARGE ? true : Registries.ITEM.getId(var0).getPath().endsWith("wind_charge");
      }
   }

   public static SlotSearchResult b() {
      return a(var0 -> f(var0.getItem()));
   }

   public static int g(Item var0) {
      if (mc.player == null) {
         return 0;
      }

      int var1 = 0;

      for (int var2 = 0; var2 < mc.player.getInventory().size(); var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.isOf(var0)) {
            var1 += var3.getCount();
         }
      }

      return var1;
   }

   public static SlotSearchResult c() {
      return a(var0 -> a(var0.getItem()), false);
   }

   public static SlotSearchResult d() {
      return a(var0 -> a(var0.getItem()), true);
   }

   public static SlotSearchResult e() {
      return a(var0 -> b(var0.getItem()), false);
   }

   public static SlotSearchResult f() {
      return g();
   }

   public static SlotSearchResult g() {
      return a(var0 -> b(var0.getItem()), true);
   }

   public static SlotSearchResult h() {
      return a(var0 -> c(var0.getItem()), false);
   }

   public static SlotSearchResult i() {
      return a(var0 -> c(var0.getItem()), true);
   }

   public static SlotSearchResult j() {
      return a(
         var0 -> var0.isOf(Items.SKELETON_SKULL)
            || var0.isOf(Items.WITHER_SKELETON_SKULL)
            || var0.isOf(Items.CREEPER_HEAD)
            || var0.isOf(Items.PLAYER_HEAD)
            || var0.isOf(Items.ZOMBIE_HEAD)
      );
   }

   public static int k() {
      if (mc.player == null) {
         return -1;
      }

      ItemStack var0 = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (var0.isOf(Items.ELYTRA) && var0.getDamage() < var0.getMaxDamage() - 1) {
         return -2;
      }

      for (int var1 = 0; var1 < 36; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.isOf(Items.ELYTRA) && var2.getDamage() < var2.getMaxDamage() - 1) {
            return var1 < 9 ? var1 + 36 : var1;
         }
      }

      return -1;
   }

   public static SlotSearchResult a(HotbarUtil.a var0) {
      if (mc.player != null) {
         if (var0.isValid(mc.player.getOffHandStack())) {
            return SlotSearchResult.a(mc.player.getOffHandStack());
         }

         for (int var1 = 0; var1 < 9; var1++) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (var0.isValid(var2)) {
               return new SlotSearchResult(var1, true, var2);
            }
         }
      }

      return SlotSearchResult.getNotFoundResult();
   }

   public static SlotSearchResult a(List<Item> var0) {
      return a(var1 -> var0.contains(var1.getItem()));
   }

   public static SlotSearchResult a(Item... var0) {
      return a(Arrays.asList(var0));
   }

   public static SlotSearchResult b(HotbarUtil.a var0) {
      if (mc.player != null) {
         for (int var1 = 35; var1 >= 0; var1--) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (var0.isValid(var2)) {
               return new SlotSearchResult(var1, true, var2);
            }
         }
      }

      return SlotSearchResult.getNotFoundResult();
   }

   public static SlotSearchResult b(List<Item> var0) {
      return b(var1 -> var0.contains(var1.getItem()));
   }

   public static SlotSearchResult b(Item... var0) {
      return b(Arrays.asList(var0));
   }

   public static SlotSearchResult c(@NotNull List<Block> var0) {
      return a(var0.stream().map(Block::asItem).toList());
   }

   public static SlotSearchResult a(Block... var0) {
      return a(Arrays.stream(var0).map(Block::asItem).toList());
   }

   public static SlotSearchResult d(@NotNull List<Block> var0) {
      return b(var0.stream().map(Block::asItem).toList());
   }

   public static SlotSearchResult b(Block... var0) {
      return b(Arrays.stream(var0).map(Block::asItem).toList());
   }

   public static void l() {
      if (mc.player != null) {
         cachedSlot = mc.player.getInventory().getSelectedSlot();
      }
   }

   public static void m() {
      if (cachedSlot != -1) {
         b(cachedSlot);
      }

      cachedSlot = -1;
   }

   public static void a(int var0) {
      l();
      b(var0);
   }

   public static void b(int var0) {
      if (mc.player != null && mc.getNetworkHandler() != null && var0 >= 0 && var0 <= 8 && mc.player.getInventory().getSelectedSlot() != var0) {
         mc.player.getInventory().setSelectedSlot(var0);
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(var0));
      }
   }

   /**
    * Переключение слота с пакетом, но БЕЗ смены клиентского слота (silent switch).
    * Античиты (Shard/Sloth) считают такой «выбор слота и возврат» невозможным — BadPackets.
    * Поэтому всегда меняем и клиентский слот: сервер и клиент остаются синхронны.
    */
   public static void c(int var0) {
      if (mc.player != null && mc.getNetworkHandler() != null && var0 >= 0 && var0 <= 8 && mc.player.getInventory().getSelectedSlot() != var0) {
         mc.player.getInventory().setSelectedSlot(var0);
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(var0));
      }
   }

   public static SlotSearchResult n() {
      if (mc.player == null) {
         return SlotSearchResult.getNotFoundResult();
      }

      Item var0 = mc.player.getMainHandStack().getItem();
      return !c(var0) && !b(var0) && !a(var0) && !d(var0)
         ? a(var0x -> c(var0x.getItem()) || b(var0x.getItem()) || a(var0x.getItem()) || d(var0x.getItem()))
         : new SlotSearchResult(mc.player.getInventory().getSelectedSlot(), true, mc.player.getMainHandStack());
   }

   public static float a(@NotNull ItemStack var0, PlayerEntity var1) {
      if (mc.player != null && mc.world != null) {
         float var2 = a(var0);
         if (mc.player.fallDistance > 0.0) {
            var2 += var2 / 2.0F;
         }

         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int var3 = Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            var2 += 3.0F * var3;
         }

         return DamageUtil.getDamageLeft(
            var1, var2, mc.world.getDamageSources().generic(), var1.getArmor(), (float)var1.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)
         );
      } else {
         return 0.0F;
      }
   }

   public static SlotSearchResult o() {
      return a(var0 -> var0.getItem() instanceof BedItem);
   }

   public static SlotSearchResult p() {
      return b(var0 -> var0.getItem() instanceof BedItem);
   }

   public static Item a(String var0) {
      if (var0 == null) {
         return Items.AIR;
      }

      String var1 = var0.toLowerCase();

      for (Block var3 : Registries.BLOCK) {
         if (var3.getTranslationKey().replace("block.minecraft.", "").equals(var1)) {
            return Item.fromBlock(var3);
         }
      }

      for (Item var5 : Registries.ITEM) {
         if (var5.getTranslationKey().replace("item.minecraft.", "").equals(var1)) {
            return var5;
         }
      }

      return Items.DIRT;
   }

   public static int q() {
      if (mc.player == null) {
         return 0;
      }

      int var0 = 0;

      for (int var1 = 0; var1 < mc.player.getInventory().size(); var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.getItem() instanceof BedItem) {
            var0 += var2.getCount();
         }
      }

      return var0;
   }

   private static SlotSearchResult a(HotbarUtil.a var0, boolean var1) {
      if (mc.player == null) {
         return SlotSearchResult.getNotFoundResult();
      }

      int var2 = -1;
      float var3 = 0.0F;
      int var4 = var1 ? 8 : 35;

      for (int var5 = 0; var5 <= var4; var5++) {
         ItemStack var6 = mc.player.getInventory().getStack(var5);
         if (var0.isValid(var6)) {
            float var7 = a(var6);
            if (var7 > var3) {
               var3 = var7;
               var2 = var5;
            }
         }
      }

      return var2 == -1 ? SlotSearchResult.getNotFoundResult() : new SlotSearchResult(var2, true, mc.player.getInventory().getStack(var2));
   }

   private static float a(ItemStack var0) {
      AttributeModifiersComponent var1 = var0.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      double var2 = 1.0;

      for (Entry var5 : var1.modifiers()) {
         if (var5.attribute().equals(EntityAttributes.ATTACK_DAMAGE)) {
            var2 += var5.modifier().value();
         }
      }

      return (float)var2;
   }

   public static boolean h(Item var0) {
      return mc.player != null && (mc.player.getMainHandStack().isOf(var0) || mc.player.getOffHandStack().isOf(var0));
   }

   public static Hand i(Item var0) {
      if (mc.player == null) {
         return null;
      } else if (mc.player.getOffHandStack().isOf(var0)) {
         return Hand.OFF_HAND;
      } else {
         return mc.player.getMainHandStack().isOf(var0) ? Hand.MAIN_HAND : null;
      }
   }

   private HotbarUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public interface a {
      boolean isValid(ItemStack var1);
   }
}
