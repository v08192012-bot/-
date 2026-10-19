package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.util.ItemUtility;
import java.awt.Color;
import net.minecraft.item.ItemStack;

@NewFunction(name = "ArmorDurability", description = "Красит броню по прочности: зелёный = фулл, красный = почти сломан", category = Category.RENDER)
public class ArmorDurability extends Function {
   public int a(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         ItemUtility.a var2 = ItemUtility.v(var1);
         if (var2 != null && var2.getMax() > 0) {
            float var3 = (float)var2.getCurrent() / var2.getMax();
            var3 = Math.clamp(var3, 0.0F, 1.0F);
            float var4 = var3 * 0.33333334F;
            return 0xFF000000 | Color.HSBtoRGB(var4, 1.0F, 1.0F) & 16777215;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }
}
