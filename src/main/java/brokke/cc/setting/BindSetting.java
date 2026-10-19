package brokke.cc.setting;

import brokke.cc.config.ConfigManager;

public class BindSetting extends Setting {
   public static BindSetting LISTENING = null;
   private int key;
   private Runnable runnable;

   public BindSetting(String var1, int var2) {
      super(var1);
      this.key = var2;
   }

   public void setKey(int var1) {
      this.key = var1;
      if (this.runnable != null) {
         this.runnable.run();
      }

      ConfigManager.onSettingChanged();
   }

   public void onChanged(Runnable var1) {
      this.runnable = var1;
      var1.run();
   }

   public boolean matchesKey(int var1) {
      return this.key != -1 && this.key == var1;
   }

   public int getKey() {
      return this.key;
   }
}
