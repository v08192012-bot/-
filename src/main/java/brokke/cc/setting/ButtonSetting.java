package brokke.cc.setting;

import java.util.function.Supplier;

public class ButtonSetting extends Setting {
   private final Runnable action;
   private Supplier<String> nameSupplier;

   public ButtonSetting(String var1, Runnable var2) {
      super(var1);
      this.action = var2;
   }

   public ButtonSetting(Supplier<String> var1, Runnable var2) {
      super("");
      this.nameSupplier = var1;
      this.action = var2;
   }

   @Override
   public String getName() {
      return this.nameSupplier != null ? this.nameSupplier.get() : super.getName();
   }

   public Runnable getAction() {
      return this.action;
   }
}
