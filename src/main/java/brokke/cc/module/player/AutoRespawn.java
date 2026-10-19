package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.client.gui.screen.DeathScreen;

@NewFunction(name = "AutoRespawn", description = "Автоматическое возрождение при смерти", category = Category.PLAYER)
public class AutoRespawn extends Function {
   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && (mc.player.isDead() || mc.player.getHealth() <= 0.0F || mc.currentScreen instanceof DeathScreen)) {
         mc.player.requestRespawn();
         if (mc.currentScreen instanceof DeathScreen) {
            mc.setScreen(null);
         }
      }
   }
}
