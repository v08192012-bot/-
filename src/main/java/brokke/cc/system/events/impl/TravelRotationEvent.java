package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.entity.LivingEntity;

public class TravelRotationEvent extends Event {
   private static TravelRotationEvent lastRotationEvent;
   public float pitch;
   public float yaw;
   public final LivingEntity entity;
   public boolean modified = false;

   public TravelRotationEvent(LivingEntity var1, float var2, float var3) {
      this.entity = var1;
      this.pitch = var2;
      this.yaw = var3;
      lastRotationEvent = this;
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      }

      if (var1 instanceof TravelRotationEvent var2) {
         if (!var2.canEqual(this)) {
            return false;
         }

         if (!super.equals(var1)) {
            return false;
         }

         if (Float.compare(this.getPitch(), var2.getPitch()) != 0) {
            return false;
         }

         if (Float.compare(this.getYaw(), var2.getYaw()) != 0) {
            return false;
         }

         if (this.isModified() != var2.isModified()) {
            return false;
         }

         LivingEntity var3 = this.getEntity();
         LivingEntity var4 = var2.getEntity();
         return var3 == null ? var4 == null : var3.equals(var4);
      } else {
         return false;
      }
   }

   protected boolean canEqual(Object var1) {
      return var1 instanceof TravelRotationEvent;
   }

   @Override
   public int hashCode() {
      byte var1 = 59;
      int var2 = super.hashCode();
      var2 = var2 * 59 + Float.floatToIntBits(this.getPitch());
      var2 = var2 * 59 + Float.floatToIntBits(this.getYaw());
      var2 = var2 * 59 + (this.isModified() ? 79 : 97);
      LivingEntity var3 = this.getEntity();
      return var2 * 59 + (var3 == null ? 43 : var3.hashCode());
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public boolean isModified() {
      return this.modified;
   }

   public void setPitch(float var1) {
      this.pitch = var1;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }

   public void setModified(boolean var1) {
      this.modified = var1;
   }

   @Override
   public String toString() {
      return "TravelRotationEvent(pitch="
         + this.getPitch()
         + ", yaw="
         + this.getYaw()
         + ", entity="
         + this.getEntity()
         + ", modified="
         + this.isModified()
         + ")";
   }

   public static TravelRotationEvent getLastRotationEvent() {
      return lastRotationEvent;
   }
}
