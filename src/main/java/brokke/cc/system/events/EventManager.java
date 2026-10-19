package brokke.cc.system.events;

import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.ui.ClickGui;
import net.minecraft.client.MinecraftClient;

public class EventManager {
   MinecraftClient mc = MinecraftClient.getInstance();
   public static ClickGui clickGui = new ClickGui();

   @EventHandler
   public void Events(KeyEvent var1) {
      int var2 = var1.getKey();
      int var3 = var1.getAction();
      if (var2 == 344 && var3 == 1) {
         if (this.mc.currentScreen == clickGui) {
            if (clickGui.isClosing()) {
               clickGui.p();
            } else {
               clickGui.close();
            }
         } else {
            if (this.mc.currentScreen != null && this.mc.currentScreen != clickGui) {
               clickGui.parentScreen = this.mc.currentScreen;
            } else {
               clickGui.parentScreen = null;
            }

            clickGui.n();
            this.mc.setScreen(clickGui);
         }
      }
   }
}
