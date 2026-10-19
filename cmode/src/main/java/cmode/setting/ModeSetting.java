package cmode.setting;

import cmode.config.ConfigManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting {
   private String activeMode;
   private final List<String> modes;
   private Runnable runnable;

   public ModeSetting(String var1, String... var2) {
      super(var1);
      this.modes = new ArrayList<>(Arrays.asList(var2));
      this.activeMode = this.modes.isEmpty() ? "" : this.modes.get(0);
   }

   public ModeSetting(String var1, String var2, String[] var3) {
      super(var1);
      this.modes = new ArrayList<>(Arrays.asList(var3));
      if (this.modes.contains(var2)) {
         this.activeMode = var2;
      } else if (!this.modes.isEmpty()) {
         this.activeMode = this.modes.get(0);
      } else {
         this.activeMode = var2;
      }
   }

   public ModeSetting withMode(String var1) {
      if (this.modes.contains(var1)) {
         this.activeMode = var1;
      }

      return this;
   }

   public void setMode(String var1) {
      if (var1 != null) {
         for (String var3 : this.modes) {
            if (var3.equalsIgnoreCase(var1)) {
               if (var3.equals(this.activeMode)) {
                  return;
               }

               this.activeMode = var3;
               if (this.runnable != null) {
                  this.runnable.run();
               }

               ConfigManager.onSettingChanged();
               return;
            }
         }
      }
   }

   public void selectMode(String var1) {
      this.setMode(var1);
   }

   public boolean isSelected(String var1) {
      return this.isMode(var1);
   }

   public boolean isMode(String var1) {
      return this.activeMode != null && this.activeMode.equalsIgnoreCase(var1);
   }

   public String getActiveMode() {
      return this.activeMode;
   }

   public int getSelectedIndex() {
      return this.modes.indexOf(this.activeMode);
   }

   public String getSelectedMode() {
      return this.activeMode;
   }

   public String normalizeMode(String var1) {
      return var1;
   }

   public ModeSetting showWhen(Supplier<Boolean> var1) {
      this.visibleWhen(var1);
      return this;
   }

   public void setModes(List<String> var1) {
      if (var1 != null && !var1.isEmpty()) {
         this.modes.clear();
         this.modes.addAll(var1);
         if (!this.modes.contains(this.activeMode)) {
            this.activeMode = this.modes.get(0);
         }
      }
   }

   public boolean containsString(String var1) {
      return this.modes.contains(var1);
   }

   public void applyMode(String var1) {
      this.setMode(var1);
   }

   public String getValue() {
      return this.activeMode;
   }

   public void onChanged(Runnable var1) {
      this.runnable = var1;
      var1.run();
   }

   public String getSelection() {
      return this.activeMode;
   }

   public List<String> getModes() {
      return this.modes;
   }
}
