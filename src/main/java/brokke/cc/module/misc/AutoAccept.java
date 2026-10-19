package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.manager.FriendManager;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.util.ServerUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@NewFunction(name = "AutoAccept", description = "Автоматически принимает телепортацию", category = Category.MISC)
public class AutoAccept extends Function {
   private final ModeSetting acceptMode = new ModeSetting("Принимать", "Всех", "Только друзей");

   public AutoAccept() {
      this.addSettings(this.acceptMode);
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof GameMessageS2CPacket var2
         && mc.player != null
         && var2.content().getString().contains("телепортироваться")
         && !ServerUtility.hasCT
         && this.a(var2.content().getString())) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
      }
   }

   private boolean a(String var1) {
      if (this.acceptMode.isSelected("Всех")) {
         return true;
      }

      if (this.acceptMode.isSelected("Только друзей")) {
         if (FriendManager.a(var1.split(" ")[1])
            || FriendManager.a(var1.replace("\u0a77 просит телепортироваться к Вам.\u0a77§l [ੲ§l✔\u0a77§l]\u0a77§l [\u0a7c§l✗\u0a77§l]", "").replace("੶", ""))
            || FriendManager.a(var1.replace("➝ Ник: ", ""))) {
            return true;
         }

         if (var1.contains("телепортироваться")) {
            String[] var2 = var1.split(" ");
            return var2.length >= 2 && FriendManager.a(var2[2]);
         }
      }

      return false;
   }
}
