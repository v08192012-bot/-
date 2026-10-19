package brokke.cc.animation;

public class SmoothValue {
   private double currentValue;
   private double targetValue;
   private final double smoothingFactor;

   public SmoothValue(double initialValue, double smoothingFactor) {
      this.currentValue = initialValue;
      this.targetValue = initialValue;
      this.smoothingFactor = smoothingFactor;
   }

   public void setTargetValue(double targetValue) {
      this.targetValue = targetValue;
   }

   public double update() {
      if (Math.abs(this.targetValue - this.currentValue) > 0.001) {
         this.currentValue = this.currentValue + (this.targetValue - this.currentValue) * this.smoothingFactor;
      } else {
         this.currentValue = this.targetValue;
      }

      return this.currentValue;
   }

   public boolean isSettled() {
      return Math.abs(this.targetValue - this.currentValue) < 0.001;
   }
}
