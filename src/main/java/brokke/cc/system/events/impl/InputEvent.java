package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class InputEvent extends Event {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private float yaw;

   public InputEvent(float var1, float var2, boolean var3, boolean var4, boolean var5) {
      this.forward = var1;
      this.strafe = var2;
      this.jump = var3;
      this.sneak = var4;
      this.sprint = var5;
      this.yaw = 0.0F;
   }

   public float getForward() {
      return this.forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setForward(float var1) {
      this.forward = var1;
   }

   public void setStrafe(float var1) {
      this.strafe = var1;
   }

   public void setJump(boolean var1) {
      this.jump = var1;
   }

   public void setSneak(boolean var1) {
      this.sneak = var1;
   }

   public void setSprint(boolean var1) {
      this.sprint = var1;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }
}
