package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.network.packet.Packet;

public class EventPacket extends Event {
   private final Packet<?> packet;

   public EventPacket(Packet<?> var1) {
      this.packet = var1;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
