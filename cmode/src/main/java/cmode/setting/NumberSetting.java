package cmode.setting;

import cmode.config.ConfigManager;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public class NumberSetting extends Setting {
   private double value;
   private final double defaultValue;
   private double min;
   private double max;
   private double step;
   private Function<Double, String> formatter;
   private Runnable runnable;

   public NumberSetting(String var1, double var2, double var4, double var6, double var8) {
      super(var1);
      this.defaultValue = var2;
      this.value = Math.clamp(var2, var4, var6);
      this.min = var4;
      this.max = var6;
      this.step = var8;
   }

   public NumberSetting(String var1, double var2, DoubleSupplier var4, DoubleSupplier var5, double var6) {
      super(var1);
      this.defaultValue = var2;
      this.value = Math.clamp(var2, var4.getAsDouble(), var5.getAsDouble());
      this.min = var4.getAsDouble();
      this.max = var5.getAsDouble();
      this.step = var6;
   }

   public void resetToDefault() {
      this.setValue(this.defaultValue);
   }

   public void setValue(double var1) {
      double var3 = Math.clamp(var1, this.min, this.max);
      if (Double.compare(this.value, var3) != 0) {
         this.value = var3;
         if (this.runnable != null) {
            this.runnable.run();
         }

         ConfigManager.onSettingChanged();
      }
   }

   public NumberSetting withFormatter(Function<Double, String> var1) {
      this.formatter = var1;
      return this;
   }

   public void onChanged(Runnable var1) {
      this.runnable = var1;
      var1.run();
   }

   public double getValue() {
      return this.value;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public double getMin() {
      return this.min;
   }

   public double getMax() {
      return this.max;
   }

   public double getStep() {
      return this.step;
   }

   public void setMin(double var1) {
      this.min = var1;
   }

   public void setMax(double var1) {
      this.max = var1;
   }

   public void setStep(double var1) {
      this.step = var1;
   }

   public Function<Double, String> getFormatter() {
      return this.formatter;
   }
}
