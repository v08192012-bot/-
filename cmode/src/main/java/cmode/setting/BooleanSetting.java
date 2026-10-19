package cmode.setting;

import cmode.config.ConfigManager;
import java.util.function.Supplier;

public class BooleanSetting extends Setting {
   public static BooleanSetting LISTENING = null;
   private boolean enabled;
   private int keybind = -1;
   private Runnable runnable;

   public BooleanSetting(String var1, boolean var2) {
      super(var1);
      this.enabled = var2;
   }

   public static BooleanSetting of(String var0, boolean var1) {
      return new BooleanSetting(var0, var1);
   }

   public boolean getValue() {
      return this.enabled;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isActive() {
      return this.enabled;
   }

   public void setValue(boolean var1) {
      this.applyValue(var1);
   }

   public void setEnabled(boolean var1) {
      this.applyValue(var1);
   }

   public boolean isOn() {
      return this.enabled;
   }

   public int getKeybind() {
      return this.keybind;
   }

   public void setKeybind(int var1) {
      this.applyKeybind(var1);
   }

   public void applyValue(boolean var1) {
      if (this.enabled != var1) {
         this.enabled = var1;
         if (this.runnable != null) {
            this.runnable.run();
         }

         ConfigManager.onSettingChanged();
      }
   }

   public void applyKeybind(int var1) {
      if (this.keybind != var1) {
         this.keybind = var1;
         ConfigManager.onSettingChanged();
      }
   }

   public BooleanSetting showWhen(Supplier<Boolean> var1) {
      this.visibleWhen(var1);
      return this;
   }

   public void onChanged(Runnable var1) {
      this.runnable = var1;
      var1.run();
   }

   public boolean isToggled() {
      return this.enabled;
   }

   public int getKeybindId() {
      return this.keybind;
   }
}
