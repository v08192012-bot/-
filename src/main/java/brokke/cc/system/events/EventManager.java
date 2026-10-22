package brokke.cc.system.events;

import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.render.Theme;
import brokke.cc.ui.ClickGui;
import brokke.cc.ui.CsClickGui;
import net.minecraft.client.MinecraftClient;

public class EventManager {
   MinecraftClient mc = MinecraftClient.getInstance();
   public static ClickGui clickGui = new ClickGui();
   public static CsClickGui csClickGui = new CsClickGui();

   @EventHandler
   public void Events(KeyEvent var1) {
      int var2 = var1.getKey();
      int var3 = var1.getAction();
      if (var2 == 344 && var3 == 1) {
         if (Theme.getInstance().getGuiMode() == Theme.b.CS) {
            // CS-гуи: своё окно, закрывается с анимацией (повторное нажатие добивает).
            if (this.mc.currentScreen == csClickGui) {
               csClickGui.close();
            } else {
               csClickGui.parentScreen = this.mc.currentScreen != null && this.mc.currentScreen != csClickGui
                  ? this.mc.currentScreen
                  : null;
               csClickGui.n();
               this.mc.setScreen(csClickGui);
            }
         } else if (this.mc.currentScreen == clickGui) {
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
