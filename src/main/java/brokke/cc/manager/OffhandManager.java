package brokke.cc.manager;

import brokke.cc.module.player.GuiMove;
import brokke.cc.util.ItemUtility;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class OffhandManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private OffhandManager.a state = OffhandManager.a.IDLE;
   private ItemStack savedStack = ItemStack.EMPTY;
   private int savedSlot = -1;
   private int returnTicks = 0;

   public boolean a() {
      return this.state == OffhandManager.a.EQUIPPED;
   }

   public void b() {
      this.state = OffhandManager.a.IDLE;
      this.savedStack = ItemStack.EMPTY;
      this.savedSlot = -1;
      this.returnTicks = 0;
   }

   public void a(Item var1, boolean var2, Predicate<ItemStack> var3, BooleanSupplier var4) {
      if (mc.player != null && mc.world != null && var1 != null) {
         ItemStack var5 = mc.player.getOffHandStack();
         boolean var6 = var5.isOf(var1);
         if (var2) {
            if (var4.getAsBoolean()) {
               this.a(var1, var6, var5, var3);
            }
         } else if (this.state != OffhandManager.a.IDLE && var4.getAsBoolean()) {
            this.b(var5);
         }
      }
   }

   private void a(Item var1, boolean var2, ItemStack var3, Predicate<ItemStack> var4) {
      if (var2 && var1 == Items.TOTEM_OF_UNDYING) {
         int var9 = a(var3);
         Integer var6 = this.a(true);
         if (var6 == null) {
            this.state = OffhandManager.a.EQUIPPED;
         } else {
            ItemStack var7 = this.a(var6);
            int var8 = a(var7);
            if (var8 >= var9) {
               this.state = OffhandManager.a.EQUIPPED;
            } else {
               if (this.savedStack.isEmpty()) {
                  this.savedStack = var3.copy();
               }

               this.savedSlot = -1;
               this.b(var6);
               this.state = OffhandManager.a.EQUIPPED;
            }
         }
      } else if (var2) {
         this.state = OffhandManager.a.EQUIPPED;
      } else if (var4.test(var3)) {
         Integer var5 = this.a(true);
         if (var5 != null) {
            if (this.savedStack.isEmpty()) {
               this.savedStack = var3.copy();
            }

            this.savedSlot = var5;
            this.b(var5);
            this.state = OffhandManager.a.EQUIPPED;
         }
      }
   }

   private void b(ItemStack var1) {
      if (this.savedStack.isEmpty()) {
         Integer var5 = this.a(false);
         if (var5 != null && var1.isOf(Items.TOTEM_OF_UNDYING)) {
            int var3 = a(var1);
            int var4 = a(this.a(var5));
            if (var4 > var3) {
               this.b(var5);
            }
         }

         this.b();
      } else if (ItemStack.areEqual(var1, this.savedStack)) {
         this.b();
      } else if (var1.isOf(Items.TOTEM_OF_UNDYING) && this.savedStack.isOf(Items.TOTEM_OF_UNDYING) && a(var1) >= a(this.savedStack)) {
         this.b();
      } else {
         if (this.state != OffhandManager.a.RETURNING) {
            this.state = OffhandManager.a.RETURNING;
            this.returnTicks = 5;
         }

         if (this.returnTicks-- <= 0) {
            this.b();
         } else {
            Integer var2 = this.c();
            if (var2 == null) {
               this.b();
            } else {
               this.c(var2);
            }
         }
      }
   }

   private Integer c() {
      if (this.savedSlot != -1) {
         ItemStack var1 = this.a(this.savedSlot);
         if (ItemStack.areEqual(var1, this.savedStack)) {
            return this.savedSlot;
         }

         this.savedSlot = -1;
      }

      for (int var3 = 9; var3 <= 44; var3++) {
         ItemStack var2 = this.a(var3);
         if (ItemStack.areEqual(var2, this.savedStack)) {
            return var3;
         }
      }

      return null;
   }

   private Integer a(boolean var1) {
      Integer var2 = null;
      int var3 = var1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;

      for (int var4 = 9; var4 <= 44; var4++) {
         ItemStack var5 = this.a(var4);
         if (var5.isOf(Items.TOTEM_OF_UNDYING)) {
            int var6 = a(var5);
            if (var1) {
               if (var6 < var3) {
                  var3 = var6;
                  var2 = var4;
               }
            } else if (var6 > var3) {
               var3 = var6;
               var2 = var4;
            }
         }
      }

      return var2;
   }

   public static int a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty() && var0.isOf(Items.TOTEM_OF_UNDYING)) {
         int var1 = ItemUtility.f(var0);
         if (var1 > 1) {
            return var1 * 10;
         }

         ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.ENCHANTMENTS);
         return var2 != null && !var2.isEmpty() ? 1 : 0;
      } else {
         return Integer.MAX_VALUE;
      }
   }

   private ItemStack a(int var1) {
      if (mc.player != null && mc.player.playerScreenHandler != null) {
         return var1 >= 0 && var1 < mc.player.playerScreenHandler.slots.size() ? mc.player.playerScreenHandler.getSlot(var1).getStack() : ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private void b(int var1) {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 40, SlotActionType.SWAP, mc.player);
      }
   }

   private void c(int var1) {
      if (mc.player != null && mc.interactionManager != null) {
         GuiMove.bypassQueue = true;

         try {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, 40, SlotActionType.SWAP, mc.player);
         } finally {
            GuiMove.bypassQueue = false;
         }
      }
   }

   public enum a {
      IDLE,
      EQUIPPED,
      RETURNING;
   }
}
