package brokke.cc.animation;

public abstract class Easings {
   public static float quadraticIn(float progress) {
      return progress * progress;
   }

   public static float quadraticOut(float progress) {
      return progress * (2.0F - progress);
   }

   public static float quadraticInOut(float progress) {
      return progress < 0.5F ? 2.0F * progress * progress : -1.0F + (4.0F - 2.0F * progress) * progress;
   }

   public static float cubicIn(float progress) {
      return progress * progress * progress;
   }

   public static float cubicOut(float progress) {
      float shiftedProgress = progress - 1.0F;
      return shiftedProgress * shiftedProgress * shiftedProgress + 1.0F;
   }

   public static float cubicInOut(float progress) {
      return progress < 0.5F ? 4.0F * progress * progress * progress : (progress - 1.0F) * (2.0F * progress - 2.0F) * (2.0F * progress - 2.0F) + 1.0F;
   }

   public static float backOut(float progress) {
      float overshoot = 1.70158F;
      float overshootPlusOne = overshoot + 1.0F;
      return 1.0F + overshootPlusOne * (float)Math.pow(progress - 1.0F, 3.0) + overshoot * (float)Math.pow(progress - 1.0F, 2.0);
   }
}
