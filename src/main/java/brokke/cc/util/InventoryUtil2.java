package brokke.cc.util;

import brokke.cc.render.SphereMode;
import brokke.cc.render.SphereTextures;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class InventoryUtil2 {
   private static final MinecraftClient a = MinecraftClient.getInstance();

   private InventoryUtil2() {
   }

   public static void a() {
      if (a.player != null) {
         if (a.player.currentScreenHandler != null && !(a.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            a.player.closeHandledScreen();
         } else if (a.currentScreen != null) {
            a.setScreen(null);
         }
      }
   }

   public static void a(Slot var0, Hand var1, boolean var2) {
      if (var0 != null && var0.id != -1) {
         int var3 = var1 == Hand.MAIN_HAND ? a.player.getInventory().getSelectedSlot() : 40;
         a(var0, var3, var2);
      }
   }

   public static void a(Slot var0, int var1, boolean var2) {
      a(var0, var1, SlotActionType.SWAP, false);
      if (var2) {
         b();
      }
   }

   public static void a(Slot var0, int var1, SlotActionType var2, boolean var3) {
      if (var0 != null) {
         a(var0.id, var1, var2, var3);
      }
   }

   public static void a(int var0, int var1, SlotActionType var2, boolean var3) {
      if (a.player != null && a.interactionManager != null && var0 >= 0 && (var0 != 45 || var1 != 40 || var2 != SlotActionType.SWAP)) {
         a(a.player.currentScreenHandler.syncId, var0, var1, var2, var3);
      }
   }

   public static void a(int var0, int var1, int var2, SlotActionType var3, boolean var4) {
      if (a.player != null && a.interactionManager != null && var1 >= 0 && (var1 != 45 || var2 != 40 || var3 != SlotActionType.SWAP)) {
         a.interactionManager.clickSlot(var0, var1, var2, var3, a.player);
      }
   }

   public static void b() {
      if (a.player != null && a.player.currentScreenHandler != null) {
         a.player.getInventory().markDirty();
         a.player.currentScreenHandler.sendContentUpdates();
      }
   }

   public static Slot a(Item var0, Predicate<Slot> var1) {
      return a(var0, Comparator.comparingInt(var0x -> var0x.id), var1);
   }

   public static Slot a(Item var0, Comparator<Slot> var1, Predicate<Slot> var2) {
      return g().filter(var1x -> var1x.getStack().isOf(var0)).filter(var2).max(var1).orElse(null);
   }

   public static Slot c() {
      return a(
         Items.PLAYER_HEAD,
         var0 -> {
            ItemStack var1 = var0.getStack();
            if (var1.isEmpty()) {
               return false;
            }

            NbtComponent var2 = var1.get(DataComponentTypes.CUSTOM_DATA);
            if (var2 == null) {
               return false;
            }

            NbtCompound var3 = var2.copyNbt();
            return var3.getCompound("SkullOwner")
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
         }
      );
   }

   public static Slot a(SphereMode var0) {
      if (var0 == SphereMode.d) {
         return d();
      }

      if (var0 == SphereMode.e) {
         return null;
      }

      String var1 = SphereTextures.a(var0);
      return a(
         Items.PLAYER_HEAD,
         var1xx -> {
            ItemStack var2 = var1xx.getStack();
            NbtComponent var3 = var2.get(DataComponentTypes.CUSTOM_DATA);
            if (var3 == null) {
               return false;
            }

            NbtCompound var4 = var3.copyNbt();
            return var4.getCompound("SkullOwner")
               .flatMap(var0xx -> var0xx.getCompound("Properties"))
               .flatMap(var0xx -> var0xx.getList("textures"))
               .filter(var0xx -> !var0xx.isEmpty())
               .flatMap(var0xx -> var0xx.getCompound(0))
               .flatMap(var0xx -> var0xx.getString("Value"))
               .map(var1xxxx -> var1xxxx.equals(var1))
               .orElse(false);
         }
      );
   }

   public static Slot d() {
      return a.player == null
         ? null
         : a.player.currentScreenHandler.slots.stream().filter(var0 -> var0.getStack().isOf(Items.TOTEM_OF_UNDYING)).max(h()).orElse(null);
   }

   public static Comparator<Slot> e() {
      return Comparator.comparing(var0 -> c(var0.getStack()));
   }

   public static Predicate<Slot> a(Slot var0) {
      return var1 -> var1 != var0 && var1.id != 45 && var1.id != 46;
   }

   public static boolean a(ItemStack var0) {
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
               "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19"
            )
         )
         .orElse(false);
   }

   public static boolean f() {
      return a.player != null && a.options != null
         ? a.options.forwardKey.isPressed()
            || a.options.backKey.isPressed()
            || a.options.leftKey.isPressed()
            || a.options.rightKey.isPressed()
            || a.options.jumpKey.isPressed()
            || a.player.isSprinting()
         : false;
   }

   public static Stream<Slot> g() {
      return a.player == null ? Stream.empty() : a.player.currentScreenHandler.slots.stream();
   }

   public static NbtCompound b(ItemStack var0) {
      NbtComponent var1 = var0.get(DataComponentTypes.CUSTOM_DATA);
      return var1 == null ? null : var1.copyNbt();
   }

   private static Comparator<Slot> h() {
      return Comparator.<Slot>comparingInt(var0 -> c(var0.getStack()) ? 1 : 0)
         .thenComparing((var0, var1) -> a(var0.getStack(), var1.getStack()))
         .thenComparingInt(var0 -> var0.id)
         .reversed();
   }

   private static boolean c(ItemStack var0) {
      ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
      ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
      return var1 != null && !var1.isEmpty() || var2 != null && !var2.isEmpty();
   }

   private static int a(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = var0.get(DataComponentTypes.CUSTOM_DATA);
      NbtComponent var3 = var1.get(DataComponentTypes.CUSTOM_DATA);
      NbtCompound var4 = var2 == null ? null : var2.copyNbt();
      NbtCompound var5 = var3 == null ? null : var3.copyNbt();
      if (var4 == null && var5 != null) {
         return -1;
      }

      if (var4 != null && var5 == null) {
         return 1;
      }

      if (var4 == null) {
         return 0;
      }

      boolean var6 = var4.getCompound("SkullOwner").flatMap(var0x -> var0x.getCompound("sphereEffect")).isPresent();
      boolean var7 = var5.getCompound("SkullOwner").flatMap(var0x -> var0x.getCompound("sphereEffect")).isPresent();
      if (var6 && !var7) {
         return 1;
      }

      if (!var6 && var7) {
         return -1;
      }

      if (!var6) {
         return 0;
      }

      String var8 = var4.getCompound("SkullOwner").flatMap(var0x -> var0x.getString("sphereEffect")).orElse("");
      String var9 = var5.getCompound("SkullOwner").flatMap(var0x -> var0x.getString("sphereEffect")).orElse("");
      return Integer.compare(var9.length(), var8.length());
   }
}
