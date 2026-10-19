package brokke.cc.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class DescriptionProcessor implements ItemFilter {
   private final List<DescriptionCondition> conditions = new ArrayList<>();

   public List<DescriptionCondition> getConditions() {
      return this.conditions;
   }

   public DescriptionProcessor a(String var1) {
      this.conditions.add(new DescriptionCondition(var1));
      return this;
   }

   public DescriptionProcessor a(String var1, int var2) {
      this.conditions.add(new DescriptionCondition(var1, var2));
      return this;
   }

   public DescriptionProcessor b(String var1) {
      this.conditions.add(new DescriptionCondition(var1, 0, ItemType.DENY));
      return this;
   }

   @Override
   public boolean a(ItemStack var1) {
      return this.conditions.stream().allMatch(var1xx -> var1xx.a(var1));
   }
}
