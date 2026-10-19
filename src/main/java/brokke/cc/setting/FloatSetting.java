package brokke.cc.setting;

import java.util.function.Supplier;

public class FloatSetting extends NumberSetting {
   public FloatSetting(String var1, float var2, float var3, float var4, float var5) {
      super(var1, var2, var3, var4, var5);
   }

   public float getFloatValue() {
      return (float)super.getValue();
   }

   public void setValue(float var1) {
      super.setValue(var1);
   }

   public FloatSetting showWhen(Supplier<Boolean> var1) {
      this.visibleWhen(var1);
      return this;
   }
}
