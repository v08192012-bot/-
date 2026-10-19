package brokke.cc.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.registry.entry.RegistryEntry;

public final class PotionUtility {
   public static boolean a(ItemStack var0, RegistryEntry<StatusEffect> var1) {
      if (var0 != null && !var0.isEmpty()) {
         if (!(var0.getItem() instanceof PotionItem)) {
            return false;
         }

         PotionContentsComponent var2 = var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var2 == null) {
            return false;
         }

         for (StatusEffectInstance var4 : var2.getEffects()) {
            if (var4.getEffectType() == var1) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static List<StatusEffectInstance> a(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null || var0.isEmpty()) {
         return var1;
      }

      if (!(var0.getItem() instanceof PotionItem)) {
         return var1;
      }

      PotionContentsComponent var2 = var0.get(DataComponentTypes.POTION_CONTENTS);
      if (var2 == null) {
         return var1;
      }

      for (StatusEffectInstance var4 : var2.getEffects()) {
         var1.add(var4);
      }

      return var1;
   }

   private PotionUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
