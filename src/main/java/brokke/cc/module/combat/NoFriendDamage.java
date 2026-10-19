package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.manager.FriendManager;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import net.minecraft.entity.player.PlayerEntity;

@NewFunction(name = "No Friend Damage", description = "Не даёт ударить друга", category = Category.COMBAT)
public class NoFriendDamage extends Function {
   @EventHandler
   public void onAttack(AttackEntityEvent var1) {
      if (mc.player != null && var1.getPlayer() == mc.player && var1.getTarget() instanceof PlayerEntity var2 && FriendManager.a(var2.getName().getString())) {
         var1.setCancelled(true);
      }
   }
}
