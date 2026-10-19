package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;

@NewFunction(name = "NoRotate", description = "Запрещает серверу вращать камеру", category = Category.PLAYER)
public class NoRotate extends Function {
   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.player != null && var1.getPacket() instanceof PlayerPositionLookS2CPacket var2) {
         var2.relatives().remove(PositionFlag.X_ROT);
         var2.relatives().remove(PositionFlag.Y_ROT);
      }
   }
}
