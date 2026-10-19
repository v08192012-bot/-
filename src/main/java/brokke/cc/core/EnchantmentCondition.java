package brokke.cc.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;

public class EnchantmentCondition implements ItemFilter {
   private final RegistryKey<Enchantment> enchantmentKey;
   private ItemType type;
   private int requiredLevel;

   public EnchantmentCondition(RegistryKey<Enchantment> var1) {
      this(var1, 0, ItemType.ON);
   }

   public EnchantmentCondition(RegistryKey<Enchantment> var1, int var2) {
      this(var1, var2, ItemType.ON);
   }

   public EnchantmentCondition(RegistryKey<Enchantment> var1, int var2, ItemType var3) {
      this.enchantmentKey = var1;
      this.type = var3;
      this.requiredLevel = var2;
   }

   public RegistryKey<Enchantment> getEnchantmentKey() {
      return this.enchantmentKey;
   }

   public ItemType getType() {
      return this.type;
   }

   public int getRequiredLevel() {
      return this.requiredLevel;
   }

   @Override
   public boolean a(ItemStack var1) {
      if (this.type == ItemType.OFF) {
         return true;
      }

      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world == null) {
         return false;
      }

      Reference var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(this.enchantmentKey);
      ItemEnchantmentsComponent var4 = var1.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
      int var5 = var4.getLevel(var3);
      int var6 = this.requiredLevel == 0 ? 1 : this.requiredLevel;
      return this.type == ItemType.DENY ? var5 < var6 : !var4.isEmpty() && var5 >= var6;
   }
}
