package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class EventLook extends Event {
   private double yaw;
   private double pitch;
   private final double originalYaw;
   private final double originalPitch;

   public EventLook(double var1, double var3) {
      this.yaw = var1;
      this.pitch = var3;
      this.originalYaw = var1;
      this.originalPitch = var3;
   }

   public double getYaw() {
      return this.yaw;
   }

   public double getPitch() {
      return this.pitch;
   }

   public void setYaw(double var1) {
      this.yaw = var1;
   }

   public void setPitch(double var1) {
      this.pitch = var1;
   }

   public double getOriginalYaw() {
      return this.originalYaw;
   }

   public double getOriginalPitch() {
      return this.originalPitch;
   }
}
