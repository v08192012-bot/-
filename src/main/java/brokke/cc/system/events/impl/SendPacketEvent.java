package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.network.packet.Packet;

public class SendPacketEvent extends Event {
   private Packet<?> packet;

   public SendPacketEvent(Packet<?> var1) {
      this.packet = var1;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> var1) {
      this.packet = var1;
   }
}
