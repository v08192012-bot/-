package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class AttackEntityEvent extends Event {
   private final PlayerEntity player;
   private final Entity target;

   public AttackEntityEvent(PlayerEntity var1, Entity var2) {
      this.player = var1;
      this.target = var2;
   }

   public PlayerEntity getPlayer() {
      return this.player;
   }

   public Entity getTarget() {
      return this.target;
   }
}
