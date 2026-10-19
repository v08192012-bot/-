package brokke.cc.core;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

public record AttributeCondition(RegistryEntry<EntityAttribute> attribute, Operation operation, double amount) {
   public AttributeCondition {
      if (attribute == null) {
         throw new IllegalArgumentException("Attribute cannot be null");
      }
   }

   public AttributeCondition(RegistryEntry<EntityAttribute> var1, double var2, Operation var4) {
      this(var1, var4, var2);
   }

   public boolean a(RegistryEntry<EntityAttribute> var1, EntityAttributeModifier var2) {
      return var1.equals(this.attribute) && Math.abs(var2.value() - this.amount) < 0.001 && var2.operation() == this.operation;
   }

   public boolean a(ItemStack var1) {
      AttributeModifiersComponent var2 = var1.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);

      for (Entry var4 : var2.modifiers()) {
         if (this.a(var4.attribute(), var4.modifier())) {
            return true;
         }
      }

      return false;
   }

   public RegistryEntry<EntityAttribute> getAttribute() {
      return this.attribute;
   }

   public Operation getOperation() {
      return this.operation;
   }

   public double getAmount() {
      return this.amount;
   }
}
