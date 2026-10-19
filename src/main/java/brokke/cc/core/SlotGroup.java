package brokke.cc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SlotGroup<T extends ItemSlot> {
   protected final List<T> slots;

   public SlotGroup(List<T> var1) {
      this.slots = var1;
   }

   @Nullable
   public T a(Item var1) {
      return this.slots.stream().filter(var1xx -> var1xx.a(var1)).findFirst().orElse(null);
   }

   @Nullable
   public T a(Predicate<ItemStack> var1) {
      return this.slots.stream().filter(var1xx -> var1xx.a(var1)).findFirst().orElse(null);
   }

   public List<T> b(Item var1) {
      return this.slots.stream().filter(var1xx -> var1xx.a(var1)).toList();
   }

   public List<T> b(Predicate<ItemStack> var1) {
      return this.slots.stream().filter(var1xx -> var1xx.a(var1)).toList();
   }

   @Nullable
   public T a() {
      return this.slots.stream().filter(ItemSlot::e).findFirst().orElse(null);
   }

   public boolean c(Item var1) {
      return this.slots.stream().anyMatch(var1xx -> var1xx.a(var1));
   }

   public int d(Item var1) {
      return this.slots.stream().filter(var1xx -> var1xx.a(var1)).mapToInt(var0 -> var0.a().getCount()).sum();
   }

   public SlotGroup<ItemSlot> a(SlotGroup<? extends ItemSlot> var1) {
      ArrayList var2 = new ArrayList(this.slots.size() + var1.slots.size());
      var2.addAll(this.slots);
      var2.addAll(var1.slots);
      return new SlotGroup<>(var2);
   }

   public SlotGroup<ItemSlot> a(ItemSlot var1) {
      ArrayList var2 = new ArrayList<>(this.slots);
      var2.add(var1);
      return new SlotGroup<>(var2);
   }

   public List<T> getSlots() {
      return this.slots;
   }
}
