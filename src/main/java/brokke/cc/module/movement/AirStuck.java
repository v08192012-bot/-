package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@NewFunction(name = "AirStuck", description = "Позволяет зависать в воздухе", category = Category.MOVEMENT)
public class AirStuck extends Function {
   private World lastWorld;

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.lastWorld = mc.world;
         mc.player.setVelocity(Vec3d.ZERO);
         super.onEnable();
      } else {
         this.setEnabled(false);
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      var1.setForward(0.0F);
      var1.setStrafe(0.0F);
      var1.setJump(false);
      var1.setSneak(false);
      var1.setSprint(false);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && mc.world == this.lastWorld) {
         mc.player.setVelocity(Vec3d.ZERO);
         mc.player.velocityDirty = true;
      } else {
         this.setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      this.lastWorld = null;
      super.onDisable();
   }
}
