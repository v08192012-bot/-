package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventMoveVelocity extends Event {
   public Vec3d movement;

   public EventMoveVelocity(Vec3d var1) {
      this.movement = var1;
   }

   public Vec3d getMovement() {
      return this.movement;
   }

   public void setMovement(Vec3d var1) {
      this.movement = var1;
   }
}
