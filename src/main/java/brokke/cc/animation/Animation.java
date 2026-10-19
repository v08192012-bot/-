package brokke.cc.animation;

public class Animation {
   private long duration = 280L;
   private float value;
   private Easing easing = Easing.SPRING_SOFT;
   private long startTime;
   private float startValue;
   private float targetValue;
   private boolean done = true;

   public Animation() {
      this(0.0F);
   }

   public Animation(float initialValue) {
      this.value = initialValue;
      this.startValue = initialValue;
      this.targetValue = initialValue;
   }

   public void reset(float value) {
      this.value = value;
      this.startValue = value;
      this.targetValue = value;
      this.done = true;
   }

   public void animate(float target, float unusedStart, float speed) {
      this.animate(target, unusedStart, speed, this.easing, System.currentTimeMillis());
   }

   public void animate(float target, float unusedStart, float speed, Easing easing) {
      this.animate(target, unusedStart, speed, easing, System.currentTimeMillis());
   }

   public void animate(float target, float unusedStart, float speed, long nowMillis) {
      this.animate(target, unusedStart, speed, this.easing, nowMillis);
   }

   public void animate(float target, float unusedStart, float speed, Easing easing, long nowMillis) {
      long durationMillis = (long)Math.clamp(1800.0F / Math.max(speed, 0.01F), 100.0F, 1200.0F);
      if (target != this.targetValue || this.duration != durationMillis) {
         this.duration = durationMillis;
         if (target != this.targetValue) {
            this.startValue = this.value;
            this.startTime = nowMillis;
            this.done = false;
         }

         this.targetValue = target;
      }

      if (this.done) {
         this.value = this.targetValue;
      } else {
         long elapsedMillis = nowMillis - this.startTime;
         if (elapsedMillis >= this.duration) {
            this.value = this.targetValue;
            this.done = true;
         } else {
            float progress = Math.clamp((float)elapsedMillis / (float)this.duration, 0.0F, 1.0F);
            float easedProgress = easing.ease(progress, 0.0F, 1.0F, 1.0F);
            this.value = this.startValue + (this.targetValue - this.startValue) * easedProgress;
         }
      }
   }

   public boolean isDone() {
      return this.done;
   }

   public void setEasing(Easing easing) {
      this.easing = easing;
   }

   public float getValue() {
      return this.value;
   }
}
