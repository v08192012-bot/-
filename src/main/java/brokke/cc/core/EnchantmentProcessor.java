package brokke.cc.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;

public class EnchantmentProcessor implements ItemFilter {
   private final List<EnchantmentCondition> conditions = new ArrayList<>();

   public List<EnchantmentCondition> getConditions() {
      return this.conditions;
   }

   public EnchantmentProcessor a(EnchantmentCondition var1) {
      this.conditions.add(var1);
      return this;
   }

   public EnchantmentProcessor a(RegistryKey<Enchantment> var1) {
      return this.a(new EnchantmentCondition(var1));
   }

   public EnchantmentProcessor a(RegistryKey<Enchantment> var1, int var2) {
      return this.a(new EnchantmentCondition(var1, var2));
   }

   public EnchantmentProcessor b(RegistryKey<Enchantment> var1) {
      return this.a(new EnchantmentCondition(var1, 0, ItemType.DENY));
   }

   public EnchantmentProcessor b(RegistryKey<Enchantment> var1, int var2) {
      return this.a(new EnchantmentCondition(var1, var2, ItemType.DENY));
   }

   public EnchantmentProcessor b() {
      return this.b(Enchantments.THORNS).b(Enchantments.KNOCKBACK).b(Enchantments.BINDING_CURSE).b(Enchantments.VANISHING_CURSE);
   }

   @Override
   public boolean a(ItemStack var1) {
      return this.conditions.stream().allMatch(var1xx -> var1xx.a(var1));
   }
}
