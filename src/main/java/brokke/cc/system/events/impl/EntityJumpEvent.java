package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.entity.Entity;

public class EntityJumpEvent extends Event {
   private final Entity entity;

   public EntityJumpEvent(Entity var1) {
      this.entity = var1;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
