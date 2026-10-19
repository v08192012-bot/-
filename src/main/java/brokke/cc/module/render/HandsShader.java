package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.NoiseShader;
import brokke.cc.render.Theme;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HandEvent;
import java.awt.Color;
import net.minecraft.client.option.Perspective;

@NewFunction(name = "HandsShader", description = "Накладывает шейдер на руку от первого лица", category = Category.RENDER)
public class HandsShader extends Function {
   private final ModeSetting mode = new ModeSetting(
      "Режим", "Радиация", "Плазма", "Радуга", "Туманность", "Закат", "Галактика", "Жидкое стекло", "Свечение", "Slime", "Огонь", "Отсталый", "Космос"
   );
   private final ModeSetting colorMode = new ModeSetting("Режим цвета", "От темы", "Свой", "Радуга");
   private final ColorSetting color = new ColorSetting("Цвет", new Color(130, 80, 255));
   private final NumberSetting opacity = new NumberSetting("Непрозрачность", 0.7, 0.0, 1.0, 0.05);
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 3.0, 0.1);
   private final NumberSetting scale = new NumberSetting("Масштаб", 1.0, 0.2, 3.0, 0.1);
   private final NoiseShader noiseShader = new NoiseShader();

   public HandsShader() {
      this.addSettings(this.mode, this.colorMode, this.color, this.opacity, this.speed, this.scale);
      this.colorMode.onChanged(this::r);
   }

   private void r() {
      this.color.visibleWhen(() -> this.colorMode.isSelected("Свой"));
   }

   @EventHandler
   public void onHand(HandEvent var1) {
      if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         if (var1.isPre()) {
            this.noiseShader.a();
         }

         if (var1.isPost()) {
            boolean var3 = this.colorMode.isSelected("Радуга");
            Color var2;
            if (this.colorMode.isSelected("Свой")) {
               var2 = this.color.getColor();
            } else {
               var2 = Theme.getInstance().getAccent();
            }

            if (var2 == null) {
               var2 = Color.WHITE;
            }

            float[] var4 = new float[]{var2.getRed() / 255.0F, var2.getGreen() / 255.0F, var2.getBlue() / 255.0F, (float)this.opacity.getValue()};
            this.noiseShader.a(var4, Math.max(0, this.mode.getSelectedIndex()), (float)this.speed.getValue(), (float)this.scale.getValue(), var3);
         }
      }
   }

   public ModeSetting getMode() {
      return this.mode;
   }

   public ModeSetting getColorMode() {
      return this.colorMode;
   }

   public ColorSetting getColor() {
      return this.color;
   }

   public NumberSetting getOpacity() {
      return this.opacity;
   }

   public NumberSetting getSpeed() {
      return this.speed;
   }

   public NumberSetting getScale() {
      return this.scale;
   }

   public NoiseShader getNoiseShader() {
      return this.noiseShader;
   }
}
