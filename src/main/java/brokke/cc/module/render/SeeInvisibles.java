package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import java.awt.Color;

@NewFunction(name = "SeeInvisibles", description = "Показывает невидимых игроков", category = Category.RENDER)
public class SeeInvisibles extends Function {
   private static SeeInvisibles instance;
   private final NumberSetting opacity = new NumberSetting("Прозрачность", 0.5, 0.1, 1.0, 0.05);
   private final ModeSetting colorMode = new ModeSetting("Режим цвета", "Свой", "От темы");
   private final ColorSetting color = new ColorSetting("Цвет", new Color(-1, true));

   public static SeeInvisibles k() {
      if (instance == null) {
         instance = FunctionManager.a(SeeInvisibles.class);
      }

      return instance;
   }

   public SeeInvisibles() {
      this.color.visibleWhen(() -> this.colorMode.isSelected("Свой"));
      this.colorMode.onChanged(() -> this.color.setVisible(this.colorMode.isSelected("Свой")));
      this.addSettings(this.colorMode, this.color, this.opacity);
   }

   public NumberSetting getOpacity() {
      return this.opacity;
   }

   public ModeSetting getColorMode() {
      return this.colorMode;
   }

   public ColorSetting getColor() {
      return this.color;
   }
}
