package brokke.cc.core;

import java.util.stream.StreamSupport;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

public record PotionCondition(RegistryEntry<StatusEffect> effect, int level, int duration) {
   public PotionCondition {
      if (effect == null) {
         throw new IllegalArgumentException("Effect cannot be null");
      }
   }

   public boolean a(ItemStack var1) {
      PotionContentsComponent var2 = var1.get(DataComponentTypes.POTION_CONTENTS);
      return var2 == null
         ? false
         : StreamSupport.stream(var2.getEffects().spliterator(), false)
            .anyMatch(var1x -> var1x.getEffectType().equals(this.effect) && var1x.getAmplifier() + 1 == this.level && var1x.getDuration() >= this.duration);
   }

   public RegistryEntry<StatusEffect> getEffect() {
      return this.effect;
   }

   public int getLevel() {
      return this.level;
   }

   public int getDuration() {
      return this.duration;
   }
}
