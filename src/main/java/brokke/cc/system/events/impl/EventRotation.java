package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class EventRotation extends Event {
   private float yaw;
   private float pitch;
   private float partialTicks;

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }

   public void setPitch(float var1) {
      this.pitch = var1;
   }

   public void setPartialTicks(float var1) {
      this.partialTicks = var1;
   }

   public EventRotation(float var1, float var2, float var3) {
      this.yaw = var1;
      this.pitch = var2;
      this.partialTicks = var3;
   }
}
