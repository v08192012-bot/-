package cmode.setting;

import java.util.function.Supplier;

public class Setting {
   String name;
   private Supplier<Boolean> visibleSupplier = () -> true;

   public Setting(String var1) {
      this.name = var1;
   }

   public <T extends Setting> T visibleWhen(Supplier<Boolean> var1) {
      this.visibleSupplier = var1;
      return (T)this;
   }

   public void setVisible(boolean var1) {
      this.visibleSupplier = () -> var1;
   }

   public boolean isVisible() {
      return this.visibleSupplier == null || this.visibleSupplier.get();
   }

   public String getName() {
      return this.name;
   }
}
