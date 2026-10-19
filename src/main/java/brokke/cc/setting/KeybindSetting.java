package brokke.cc.setting;

import brokke.cc.config.ConfigManager;

public class KeybindSetting extends Setting {
   private int keybind;

   public KeybindSetting(String var1, int var2) {
      super(var1);
      this.keybind = var2;
   }

   public void setKeybind(int var1) {
      this.keybind = var1;
      ConfigManager.onSettingChanged();
   }

   public int getKeybind() {
      return this.keybind;
   }
}
