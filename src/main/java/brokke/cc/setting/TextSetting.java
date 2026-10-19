package brokke.cc.setting;

import brokke.cc.config.ConfigManager;

public class TextSetting extends Setting {
   public static TextSetting LISTENING = null;
   private String value;

   public TextSetting(String var1, String var2) {
      super(var1);
      this.value = var2;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String var1) {
      this.value = var1;
      ConfigManager.onSettingChanged();
   }

   public String getText() {
      return this.value;
   }
}
