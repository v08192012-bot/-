package brokke.cc.setting;

import brokke.cc.config.ConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiSetting extends Setting {
   private final Map<String, Boolean> options = new HashMap<>();
   private final List<String> enabled = new ArrayList<>();

   public MultiSetting(String var1, String... var2) {
      super(var1);

      for (String var6 : var2) {
         this.options.put(var6, false);
         this.enabled.add(var6);
      }
   }

   public boolean isOptionEnabled(String var1) {
      return this.options.getOrDefault(var1, false);
   }

   public void toggleOption(String var1) {
      if (this.options.containsKey(var1)) {
         this.options.put(var1, !this.options.get(var1));
         ConfigManager.onSettingChanged();
      }
   }

   public void setOption(String var1, boolean var2) {
      if (this.options.containsKey(var1)) {
         Boolean var3 = this.options.put(var1, var2);
         if (var3 == null || var3 != var2) {
            ConfigManager.onSettingChanged();
         }
      }
   }

   public List<String> getEnabled() {
      return this.enabled;
   }
}
