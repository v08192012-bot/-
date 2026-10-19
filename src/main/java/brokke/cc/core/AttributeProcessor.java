package brokke.cc.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class AttributeProcessor implements ItemFilter {
   private final List<AttributeCondition> conditions = new ArrayList<>();

   public AttributeProcessor a(AttributeCondition var1) {
      this.conditions.add(var1);
      return this;
   }

   @Override
   public boolean a(ItemStack var1) {
      return this.conditions.stream().allMatch(var1xx -> var1xx.a(var1));
   }
}
