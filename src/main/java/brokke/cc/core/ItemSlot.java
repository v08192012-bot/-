package brokke.cc.core;

import brokke.cc.util.InventoryUtility;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public abstract class ItemSlot {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();

   public abstract ItemStack a();

   public abstract int get45();

   public int c() {
      return mc.player != null && mc.player.currentScreenHandler != null ? mc.player.currentScreenHandler.syncId : 0;
   }

   public Item d() {
      return this.a().getItem();
   }

   public boolean e() {
      return this.a().isEmpty();
   }

   public boolean a(Item var1) {
      return this.a().getItem() == var1;
   }

   public boolean a(Predicate<ItemStack> var1) {
      return var1.test(this.a());
   }

   public void a(ItemSlot var1) {
      InventoryUtility.a(this, var1);
   }

   public void f() {
      InventoryUtility.a(this);
   }

   public void g() {
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(this.c(), this.get45(), 0, SlotActionType.PICKUP, mc.player);
      }
   }
}
