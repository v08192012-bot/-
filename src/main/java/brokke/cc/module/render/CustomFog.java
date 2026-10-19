package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.CustomFogRenderer;
import brokke.cc.render.RenderContext;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import java.awt.Color;

@NewFunction(name = "CustomFog", description = "Кастомный туман", category = Category.RENDER)
public class CustomFog extends Function {
   private static final String BLUR = "Блюр";
   private static final String COLOR = "Цветной";
   private static CustomFog instance;
   private final ModeSetting mode = new ModeSetting("Режим", "Блюр", "Цветной");
   private final NumberSetting start = new NumberSetting("Начало", 16.0, 0.0, 256.0, 1.0);
   private final NumberSetting dispersion = new NumberSetting("Рассеянность", 64.0, 1.0, 256.0, 1.0);
   private final NumberSetting strength = new NumberSetting("Сила", 0.9, 0.05, 1.0, 0.05);
   private final NumberSetting blurRadius = new NumberSetting("Радиус блюра", 10.0, 1.0, 32.0, 1.0);
   private final BooleanSetting fogSky = new BooleanSetting("Туман на небе", true);

   public static CustomFog k() {
      if (instance == null) {
         instance = FunctionManager.a(CustomFog.class);
      }

      return instance;
   }

   public CustomFog() {
      this.addSettings(this.mode, this.start, this.dispersion, this.strength, this.blurRadius, this.fogSky);
      this.mode.onChanged(() -> {
         this.blurRadius.setVisible(this.mode.isMode("Блюр"));
         this.fogSky.setVisible(this.mode.isMode("Цветной"));
      });
   }

   public NumberSetting getStart() {
      return this.start;
   }

   public NumberSetting getDispersion() {
      return this.dispersion;
   }

   public NumberSetting getStrength() {
      return this.strength;
   }

   public boolean o() {
      return this.mode.isMode("Цветной");
   }

   @Override
   public void onEnable() {
      CustomFogRenderer.getInstance().b();
   }

   @Override
   public void onDisable() {
      CustomFogRenderer.getInstance().b();
   }

   public static void p() {
      CustomFog var0 = k();
      if (var0 != null && var0.isActive()) {
         var0.q();
      }
   }

   private void q() {
      if (mc.player != null && mc.world != null && RenderContext.WORLD_READY) {
         Color var1 = Theme.getInstance().getAccent();
         CustomFogRenderer.a var2 = new CustomFogRenderer.a();
         var2.colorMode = this.mode.isMode("Цветной");
         var2.fogSky = this.mode.isMode("Цветной") && this.fogSky.isToggled();
         var2.start = (float)this.start.getValue();
         var2.dispersion = (float)this.dispersion.getValue();
         var2.strength = (float)this.strength.getValue();
         var2.blurRadius = (float)this.blurRadius.getValue();
         var2.red = var1.getRed() / 255.0F;
         var2.green = var1.getGreen() / 255.0F;
         var2.blue = var1.getBlue() / 255.0F;
         CustomFogRenderer.getInstance().a(mc, RenderContext.PROJECTION_MATRIX, var2);
      }
   }
}
