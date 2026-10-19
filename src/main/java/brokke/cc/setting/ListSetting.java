package brokke.cc.setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ListSetting extends MultiSetting {
   public List<BooleanSetting> settings;
   private boolean expanded;

   public ListSetting(String var1, BooleanSetting... var2) {
      super(var1, Arrays.stream(var2).map(Setting::getName).toArray(String[]::new));
      this.settings = List.of(var2);

      for (BooleanSetting var6 : var2) {
         if (var6.isEnabled()) {
            super.setOption(var6.getName(), true);
         }
      }
   }

   public static ListSetting of(String var0, BooleanSetting... var1) {
      return new ListSetting(var0, var1);
   }

   public boolean isSelected(String var1) {
      BooleanSetting var2 = this.findSetting(var1);
      return var2 != null ? var2.isEnabled() : this.isOptionEnabled(var1);
   }

   public void setSelected(String var1, boolean var2) {
      BooleanSetting var3 = this.findSetting(var1);
      if (var3 != null) {
         var3.setValue(var2);
      }

      super.setOption(var1, var2);
   }

   @Override
   public void toggleOption(String var1) {
      super.toggleOption(var1);
      BooleanSetting var2 = this.findSetting(var1);
      if (var2 != null) {
         var2.setValue(this.isOptionEnabled(var1));
      }
   }

   @Override
   public void setOption(String var1, boolean var2) {
      super.setOption(var1, var2);
      BooleanSetting var3 = this.findSetting(var1);
      if (var3 != null) {
         var3.setValue(var2);
      }
   }

   public ListSetting showWhen(Supplier<Boolean> var1) {
      this.visibleWhen(var1);
      return this;
   }

   private BooleanSetting findSetting(String var1) {
      if (this.settings == null) {
         return null;
      }

      for (BooleanSetting var3 : this.settings) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public List<BooleanSetting> getSettings() {
      return this.settings;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setSettings(List<BooleanSetting> var1) {
      this.settings = var1;
   }

   public void setExpanded(boolean var1) {
      this.expanded = var1;
   }
}
