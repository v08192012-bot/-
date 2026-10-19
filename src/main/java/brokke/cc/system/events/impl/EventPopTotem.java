package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.entity.player.PlayerEntity;

public class EventPopTotem extends Event {
   private final PlayerEntity player;

   public EventPopTotem(PlayerEntity var1) {
      this.player = var1;
   }

   public PlayerEntity getPlayer() {
      return this.player;
   }
}
