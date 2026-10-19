package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;

@NewFunction(name = "Beautifully", description = "Улучшения визуала ванильного Minecraft", category = Category.RENDER)
public class Beautifully extends Function {
   private final BooleanSetting foodSaturation = new BooleanSetting("Насыщенность (Еда)", true);
   private final NumberSetting saturation = new NumberSetting("Насыщенность", 1.2, 0.0, 2.0, 0.05);
   private final NumberSetting warmth = new NumberSetting("Теплота", 0.0, 0.0, 1.0, 0.05);

   public Beautifully() {
      this.addSettings(this.foodSaturation, this.saturation, this.warmth);
   }

   public static boolean k() {
      Beautifully var0 = FunctionManager.a(Beautifully.class);
      return var0 != null && var0.isActive() && var0.foodSaturation.getValue();
   }

   public static boolean l() {
      Beautifully var0 = FunctionManager.a(Beautifully.class);
      return var0 != null && var0.isActive() && ((float)var0.saturation.getValue() != 1.0F || (float)var0.warmth.getValue() != 0.0F);
   }

   public static float m() {
      Beautifully var0 = FunctionManager.a(Beautifully.class);
      return var0 == null ? 1.0F : (float)var0.saturation.getValue();
   }

   public static float n() {
      Beautifully var0 = FunctionManager.a(Beautifully.class);
      return var0 == null ? 0.0F : (float)var0.warmth.getValue();
   }
}
