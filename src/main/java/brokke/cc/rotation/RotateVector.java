package brokke.cc.rotation;

import brokke.cc.util.MathUtil;

public class RotateVector {
   public float yaw;
   public float pitch;

   public RotateVector(float var1, float var2) {
      this.yaw = var1;
      this.pitch = MathUtil.clamp(var2, -90.0F, 90.0F);
   }

   public void a(float var1) {
      this.pitch = MathUtil.clamp(var1, -90.0F, 90.0F);
   }

   public void a(float var1, float var2) {
      this.yaw = var1;
      this.pitch = MathUtil.clamp(var2, -90.0F, 90.0F);
   }

   public void a(RotateVector var1) {
      this.a(var1.getYaw(), var1.getPitch());
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

   @Override
   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      }

      if (var1 instanceof RotateVector var2) {
         if (!var2.a((Object)this)) {
            return false;
         } else {
            return Float.compare(this.getYaw(), var2.getYaw()) != 0 ? false : Float.compare(this.getPitch(), var2.getPitch()) == 0;
         }
      } else {
         return false;
      }
   }

   protected boolean a(Object var1) {
      return var1 instanceof RotateVector;
   }

   @Override
   public int hashCode() {
      byte var1 = 59;
      int var2 = 1;
      var2 = var2 * 59 + Float.floatToIntBits(this.getYaw());
      return var2 * 59 + Float.floatToIntBits(this.getPitch());
   }

   @Override
   public String toString() {
      return "RotateVector(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }
}
