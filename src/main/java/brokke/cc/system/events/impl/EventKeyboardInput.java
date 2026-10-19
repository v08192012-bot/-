package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.util.math.MathHelper;

public class EventKeyboardInput extends Event {
   private float movementForward;
   private float movementSideways;

   public void setYaw(float var1, float var2) {
      float var3 = this.getMovementForward();
      float var4 = this.getMovementSideways();
      double var5 = MathHelper.wrapDegrees(Math.toDegrees(this.direction(var2, var3, var4)));
      if (var3 != 0.0F || var4 != 0.0F) {
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = Float.MAX_VALUE;

         for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
            for (float var11 = -1.0F; var11 <= 1.0F; var11++) {
               if (var11 != 0.0F || var10 != 0.0F) {
                  double var12 = MathHelper.wrapDegrees(Math.toDegrees(this.direction(var1, var10, var11)));
                  double var14 = Math.abs(MathHelper.wrapDegrees(var5 - var12));
                  if (var14 < var9) {
                     var9 = (float)var14;
                     var7 = var10;
                     var8 = var11;
                  }
               }
            }
         }

         this.setMovementForward(var7);
         this.setMovementSideways(var8);
      }
   }

   private double direction(float var1, double var2, double var4) {
      if (var2 < 0.0) {
         var1 += 180.0F;
      }

      float var6 = 1.0F;
      if (var2 < 0.0) {
         var6 = -0.5F;
      } else if (var2 > 0.0) {
         var6 = 0.5F;
      }

      if (var4 > 0.0) {
         var1 -= 90.0F * var6;
      }

      if (var4 < 0.0) {
         var1 += 90.0F * var6;
      }

      return Math.toRadians(var1);
   }

   public EventKeyboardInput(float var1, float var2) {
      this.movementForward = var1;
      this.movementSideways = var2;
   }

   public float getMovementForward() {
      return this.movementForward;
   }

   public float getMovementSideways() {
      return this.movementSideways;
   }

   public void setMovementForward(float var1) {
      this.movementForward = var1;
   }

   public void setMovementSideways(float var1) {
      this.movementSideways = var1;
   }
}
