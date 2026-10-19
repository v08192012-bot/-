package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class EventSync extends Event {
   public float yaw;
   public float pitch;

   @Override
   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      }

      if (var1 instanceof EventSync var2) {
         if (!var2.canEqual(this)) {
            return false;
         } else if (!super.equals(var1)) {
            return false;
         } else {
            return Float.compare(this.getYaw(), var2.getYaw()) != 0 ? false : Float.compare(this.getPitch(), var2.getPitch()) == 0;
         }
      } else {
         return false;
      }
   }

   protected boolean canEqual(Object var1) {
      return var1 instanceof EventSync;
   }

   @Override
   public int hashCode() {
      byte var1 = 59;
      int var2 = super.hashCode();
      var2 = var2 * 59 + Float.floatToIntBits(this.getYaw());
      return var2 * 59 + Float.floatToIntBits(this.getPitch());
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }

   public void setPitch(float var1) {
      this.pitch = var1;
   }

   @Override
   public String toString() {
      return "EventSync(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public EventSync(float var1, float var2) {
      this.yaw = var1;
      this.pitch = var2;
   }
}
