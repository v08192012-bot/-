package cmode.setting;

import cmode.config.ConfigManager;
import java.awt.Color;

public class ColorSetting extends Setting {
   private Color color;
   private float hue = 0.0F;
   private float saturation = 1.0F;
   private float brightness = 1.0F;

   public ColorSetting(String var1, Color var2) {
      super(var1);
      this.setColor(var2);
   }

   public void setColor(Color var1) {
      this.color = var1;
      if (var1 != null) {
         float[] var2 = Color.RGBtoHSB(var1.getRed(), var1.getGreen(), var1.getBlue(), null);
         if (var2[1] > 0.001F) {
            this.hue = var2[0];
         }

         this.saturation = var2[1];
         this.brightness = var2[2];
      }

      ConfigManager.onSettingChanged();
   }

   public void setHue(float var1) {
      this.hue = Math.clamp(var1, 0.0F, 1.0F);
      this.updateColor();
   }

   public void setSaturation(float var1) {
      this.saturation = Math.clamp(var1, 0.0F, 1.0F);
      this.updateColor();
   }

   public void setBrightness(float var1) {
      this.brightness = Math.clamp(var1, 0.0F, 1.0F);
      this.updateColor();
   }

   public void setHsb(float var1, float var2, float var3) {
      this.hue = Math.clamp(var1, 0.0F, 1.0F);
      this.saturation = Math.clamp(var2, 0.0F, 1.0F);
      this.brightness = Math.clamp(var3, 0.0F, 1.0F);
      this.updateColor();
   }

   public void setSaturationAndBrightness(float var1, float var2) {
      this.saturation = Math.clamp(var1, 0.0F, 1.0F);
      this.brightness = Math.clamp(var2, 0.0F, 1.0F);
      this.updateColor();
   }

   private void updateColor() {
      int var1 = this.color != null ? this.color.getAlpha() : 255;
      int var2 = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
      this.color = new Color(var1 << 24 | var2 & 16777215, true);
      ConfigManager.onSettingChanged();
   }

   public float getHue() {
      return this.hue;
   }

   public float getSaturation() {
      return this.saturation;
   }

   public float getBrightness() {
      return this.brightness;
   }

   public int getArgb() {
      return this.color != null ? this.color.getRGB() : -1;
   }

   public Color getColor() {
      return this.color;
   }
}
