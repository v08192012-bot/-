package brokke.cc.animation;

public class SimpleLinearAnimation {
   float target = 0.0F;
   float startValue = 0.0F;
   float currentValue = 0.0F;
   long startTime = 0L;
   int durationMillis;
   private EasingFunction easing = Easings3.EASE_OUT_CUBIC;

   public SimpleLinearAnimation(int durationMillis) {
      this.durationMillis = durationMillis;
   }

   public SimpleLinearAnimation(long durationMillis, EasingFunction easing) {
      this.durationMillis = (int)durationMillis;
      this.easing = easing;
   }

   public SimpleLinearAnimation() {
      this.durationMillis = 250;
   }

   public void setDuration(int durationMillis) {
      this.durationMillis = Math.max(1, durationMillis);
   }

   public void setEasing(EasingFunction easing) {
      if (easing != null) {
         this.easing = easing;
      }
   }

   public float getTarget() {
      return this.target;
   }

   public void fadeIn() {
      if (this.target != 255.0F) {
         this.update();
         this.startValue = this.currentValue;
         this.target = 255.0F;
         this.startTime = System.currentTimeMillis();
      }
   }

   public void fadeOut() {
      if (this.target != 0.0F) {
         this.update();
         this.startValue = this.currentValue;
         this.target = 0.0F;
         this.startTime = System.currentTimeMillis();
      }
   }

   public void setVisibleImmediately(boolean visible) {
      this.target = visible ? 255.0F : 0.0F;
      this.startValue = this.target;
      this.currentValue = this.target;
      this.startTime = System.currentTimeMillis() - this.durationMillis - 1L;
   }

   public int getOpacity() {
      this.update();
      return Math.round(this.currentValue);
   }

   public float getProgress() {
      this.update();
      return this.currentValue / 255.0F;
   }

   public boolean isFinished() {
      long nowMillis = System.currentTimeMillis();
      return nowMillis - this.startTime >= this.durationMillis;
   }

   public boolean isAnimating() {
      return !this.isFinished() || this.currentValue != this.target;
   }

   private void update() {
      long nowMillis = System.currentTimeMillis();
      long elapsedMillis = nowMillis - this.startTime;
      if (elapsedMillis <= 0L) {
         this.currentValue = this.startValue;
      } else if (elapsedMillis >= this.durationMillis) {
         this.currentValue = this.startValue + (this.target - this.startValue) * this.easing.ease(1.0F);
      } else {
         float progress = (float)elapsedMillis / this.durationMillis;
         float easedProgress = this.easing.ease(progress);
         this.currentValue = this.startValue + (this.target - this.startValue) * easedProgress;
      }
   }
}
