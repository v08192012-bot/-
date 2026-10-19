package brokke.cc.animation;

import net.minecraft.util.math.MathHelper;

public class DecelerateAnimation {
   private long startTime;
   private long duration = 500L;
   private float startValue = 0.0F;
   private float targetValue = 1.0F;
   private DecelerateAnimation.Direction direction = DecelerateAnimation.Direction.FORWARDS;

   public DecelerateAnimation() {
      this.startTime = System.currentTimeMillis();
   }

   public DecelerateAnimation setDuration(long duration) {
      this.duration = duration;
      return this;
   }

   public DecelerateAnimation setStartValue(float startValue) {
      this.startValue = startValue;
      this.startTime = System.currentTimeMillis();
      return this;
   }

   public DecelerateAnimation setTargetValue(float targetValue) {
      this.targetValue = targetValue;
      this.startTime = System.currentTimeMillis();
      return this;
   }

   public DecelerateAnimation setDirection(DecelerateAnimation.Direction direction) {
      this.direction = direction;
      this.startTime = System.currentTimeMillis();
      return this;
   }

   public float getValue() {
      long elapsedMillis = System.currentTimeMillis() - this.startTime;
      float progress = MathHelper.clamp((float)elapsedMillis / (float)this.duration, 0.0F, 1.0F);
      float easedProgress = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
      if (this.direction == DecelerateAnimation.Direction.BACKWARDS) {
         easedProgress = 1.0F - easedProgress;
      }

      return MathHelper.lerp(easedProgress, this.startValue, this.targetValue);
   }

   public boolean isFinished() {
      long elapsedMillis = System.currentTimeMillis() - this.startTime;
      return elapsedMillis >= this.duration;
   }

   public DecelerateAnimation restart() {
      this.startTime = System.currentTimeMillis();
      return this;
   }

   public enum Direction {
      FORWARDS,
      BACKWARDS;
   }
}
