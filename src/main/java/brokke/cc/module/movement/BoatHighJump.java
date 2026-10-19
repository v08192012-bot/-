package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.Box;

@NewFunction(name = "BoatHighJump", description = "Высокий прыжок рядом с лодкой (HW)", category = Category.MOVEMENT)
public class BoatHighJump extends Function {
   private boolean jumped = false;

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && mc.player.getVehicle() == null) {
         Entity var2 = this.k();
         if (mc.player.isOnGround()) {
            if (mc.options.jumpKey.isPressed() && var2 != null && !this.jumped) {
               mc.player.setVelocity(mc.player.getVelocity().x, 0.82, mc.player.getVelocity().z);
               this.jumped = true;
            }
         } else {
            this.jumped = false;
         }
      }
   }

   private Entity k() {
      if (mc.player != null && mc.world != null) {
         Box var1 = mc.player.getBoundingBox().expand(1.0);

         for (Entity var3 : mc.world.getEntities()) {
            if (var3 instanceof AbstractBoatEntity var4 && !var4.hasPassenger(mc.player) && var4.getBoundingBox().intersects(var1)) {
               return var4;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public void onDisable() {
      this.jumped = false;
      super.onDisable();
   }
}
