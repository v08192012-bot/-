package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.interfaces.ISimpleOption;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.client.option.SimpleOption;

@NewFunction(name = "Full Bright", description = "Максимальная яркость", category = Category.RENDER)
public class FullBright extends Function {
   public final NumberSetting brightness = new NumberSetting("Сила света", 5.0, 1.0, 10.0, 0.5);
   private double prevGamma = -1.0;
   private boolean applied = false;

   public FullBright() {
      this.addSettings(this.brightness);
      this.brightness.onChanged(() -> {
         if (this.isActive() && mc.options != null) {
            SimpleOption var1 = mc.options.getGamma();
            ((ISimpleOption)(Object)var1).setValueRaw(this.k());
         }
      });
   }

   private double k() {
      double var1 = this.brightness.getValue();
      double var3 = this.brightness.getMin();
      double var5 = this.brightness.getMax();
      double var7 = Math.clamp((var1 - var3) / (var5 - var3), 0.0, 1.0);
      return 1.0 + 14.0 * Math.pow(var7, 2.3);
   }

   private void l() {
      if (mc.options != null) {
         SimpleOption var1 = mc.options.getGamma();
         if (!this.applied) {
            this.prevGamma = (Double)var1.getValue();
            this.applied = true;
         }

         ((ISimpleOption)(Object)var1).setValueRaw(this.k());
      }
   }

   private void m() {
      if (this.applied && mc.options != null) {
         SimpleOption var1 = mc.options.getGamma();
         ((ISimpleOption)(Object)var1).setValueRaw(Math.clamp(this.prevGamma >= 0.0 ? this.prevGamma : 1.0, 0.0, 1.0));
         this.applied = false;
      } else {
         this.applied = false;
      }
   }

   @Override
   public void onEnable() {
      this.l();
   }

   @Override
   public void onDisable() {
      this.m();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.l();
   }
}
