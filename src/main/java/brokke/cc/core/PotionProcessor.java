package brokke.cc.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class PotionProcessor implements ItemFilter {
   private final List<PotionCondition> conditions = new ArrayList<>();

   public List<PotionCondition> getConditions() {
      return this.conditions;
   }

   public PotionProcessor a(PotionCondition var1) {
      this.conditions.add(var1);
      return this;
   }

   @Override
   public boolean a(ItemStack var1) {
      return this.conditions.stream().allMatch(var1xx -> var1xx.a(var1));
   }
}
