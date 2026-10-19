package brokke.cc.animation;

import brokke.cc.config.SpringConfig;

public final class SpringFloatAnimator implements AnimationSystem.AnimatedObject {
   private static final float MIN_DELTA_SECONDS = 1.0E-4F;
   private static final float MAX_DELTA_SECONDS = 0.016666668F;
   private final AnimationSystem animationSystem;
   private final SpringConfig config;
   private final float minValue;
   private final float maxValue;
   private final float positionTolerance;
   private final float velocityTolerance;
   private float value;
   private float target;
   private float velocity;
   private EasingFunction outputTransform = EasingFunction.linear();

   public SpringFloatAnimator(AnimationSystem animationSystem, SpringConfig config, float initialValue, float minValue, float maxValue, float positionTolerance, float velocityTolerance) {
      if (animationSystem == null) {
         throw new IllegalArgumentException("animationSystem must not be null");
      }

      if (config == null) {
         throw new IllegalArgumentException("config must not be null");
      }

      if (minValue > maxValue) {
         throw new IllegalArgumentException("minValue must be <= maxValue");
      }

      if (!(positionTolerance <= 0.0F) && !(velocityTolerance <= 0.0F)) {
         this.animationSystem = animationSystem;
         this.config = config;
         this.minValue = minValue;
         this.maxValue = maxValue;
         this.positionTolerance = positionTolerance;
         this.velocityTolerance = velocityTolerance;
         float clampedValue = this.clampValue(initialValue);
         this.value = clampedValue;
         this.target = clampedValue;
         this.velocity = 0.0F;
      } else {
         throw new IllegalArgumentException("tolerances must be > 0");
      }
   }

   public void setOutputTransform(EasingFunction outputTransform) {
      this.outputTransform = outputTransform == null ? EasingFunction.linear() : outputTransform;
   }

   public void reset(float value) {
      float clampedValue = this.clampValue(value);
      this.value = clampedValue;
      this.target = clampedValue;
      this.velocity = 0.0F;
      this.animationSystem.unregister(this);
   }

   public void setTarget(float target) {
      float clampedTarget = this.clampValue(target);
      if (Math.abs(clampedTarget - this.target) <= this.positionTolerance * 0.25F) {
         this.target = clampedTarget;
         if (this.isSettled()) {
            this.reset(clampedTarget);
         }
      } else {
         this.target = clampedTarget;
         this.animationSystem.register(this);
      }
   }

   public float getTransformedValue() {
      float progress = 0.0F;
      float valueRange = this.maxValue - this.minValue;
      if (valueRange > 0.0F) {
         progress = (this.value - this.minValue) / valueRange;
      }

      float transformedProgress = this.outputTransform.ease(clampProgress(progress));
      return this.minValue + transformedProgress * valueRange;
   }

   public float getValue() {
      return this.value;
   }

   public float getTarget() {
      return this.target;
   }

   public boolean isSettled() {
      float distanceToTarget = Math.abs(this.target - this.value);
      return distanceToTarget <= this.positionTolerance && Math.abs(this.velocity) <= this.velocityTolerance;
   }

   @Override
   public boolean update(float deltaSeconds) {
      float stepSeconds = deltaSeconds;
      if (stepSeconds < MIN_DELTA_SECONDS) {
         stepSeconds = MIN_DELTA_SECONDS;
      } else if (stepSeconds > MAX_DELTA_SECONDS) {
         stepSeconds = MAX_DELTA_SECONDS;
      }

      float angularFrequency = (float)((Math.PI * 2) * this.config.getFrequencyHz());
      float dampingCoefficient = 2.0F * this.config.getDampingRatio() * angularFrequency;
      float stiffness = angularFrequency * angularFrequency;
      float previousDisplacement = this.value - this.target;
      float acceleration = -stiffness * previousDisplacement - dampingCoefficient * this.velocity;
      this.velocity += acceleration * stepSeconds;
      this.value = this.value + this.velocity * stepSeconds;
      if (this.value < this.minValue) {
         this.value = this.minValue;
         this.velocity = 0.0F;
         return false;
      }

      if (this.value > this.maxValue) {
         this.value = this.maxValue;
         this.velocity = 0.0F;
         return false;
      }

      float displacement = this.value - this.target;
      if ((!(previousDisplacement > 0.0F) || !(displacement < 0.0F)) && (!(previousDisplacement < 0.0F) || !(displacement > 0.0F))) {
         if (this.isSettled()) {
            this.value = this.target;
            this.velocity = 0.0F;
            return false;
         } else {
            return true;
         }
      } else {
         this.value = this.target;
         this.velocity = 0.0F;
         return false;
      }
   }

   private float clampValue(float value) {
      if (value <= this.minValue) {
         return this.minValue;
      } else {
         return value >= this.maxValue ? this.maxValue : value;
      }
   }

   private static float clampProgress(float progress) {
      if (progress <= 0.0F) {
         return 0.0F;
      } else {
         return progress >= 1.0F ? 1.0F : progress;
      }
   }
}
