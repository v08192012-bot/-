package brokke.cc.core;

import brokke.cc.util.HotbarUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SlotSearchResult(int slot, boolean found, ItemStack stack) {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final SlotSearchResult NOT_FOUND_RESULT = new SlotSearchResult(-1, false, ItemStack.EMPTY);

   public static SlotSearchResult getNotFoundResult() {
      return NOT_FOUND_RESULT;
   }

   @NotNull
   public static SlotSearchResult a(ItemStack var0) {
      return new SlotSearchResult(999, true, var0);
   }

   public boolean b() {
      return mc.player == null ? false : this.c() || mc.player.getInventory().getSelectedSlot() == this.slot;
   }

   public boolean c() {
      return this.slot == 999;
   }

   public boolean d() {
      return this.slot >= 0 && this.slot < 9;
   }

   public void e() {
      if (this.found && this.d()) {
         HotbarUtil.b(this.slot);
      }
   }

   public void f() {
      if (this.found && this.d()) {
         HotbarUtil.c(this.slot);
      }
   }

   public int getSlot() {
      return this.slot;
   }

   public boolean isFound() {
      return this.found;
   }

   public ItemStack getStack() {
      return this.stack;
   }
}
