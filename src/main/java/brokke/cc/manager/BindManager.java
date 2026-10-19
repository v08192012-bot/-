package brokke.cc.manager;

import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.Setting;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.KeyEvent;
import net.minecraft.client.MinecraftClient;

public class BindManager {
   private static final BindManager INSTANCE = new BindManager();

   public static void a() {
      EventBus.register(INSTANCE);
   }

   @EventHandler(priority = EventPriority.LOW)
   public void onKey(KeyEvent var1) {
      if (var1.getAction() == 1) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2.currentScreen == null) {
            int var3 = var1.getKey();
            if (var3 != 0) {
               for (Function var5 : FunctionManager.getFunctions()) {
                  if (var5.getKeybind() != 0 && var5.getKeybind() != -1 && var5.getKeybind() == var3) {
                     var5.toggle();
                  }

                  for (Setting var7 : var5.getSettings()) {
                     if (var7 instanceof BooleanSetting var8 && var8.getKeybindId() != 0 && var8.getKeybindId() != -1 && var8.getKeybindId() == var3) {
                        var8.applyValue(!var8.isToggled());
                     }
                  }
               }
            }
         }
      }
   }
}
