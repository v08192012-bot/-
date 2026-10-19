package brokke.cc.animation;

public abstract class Interpolator {
   private static final double EPSILON = 1.0E-12;
   public static final Interpolator DISCRETE = new Interpolator() {
      @Override
      protected double curve(double progress) {
         return Math.abs(progress - 1.0) < EPSILON ? 1.0 : 0.0;
      }

      @Override
      public String toString() {
         return "Interpolator.DISCRETE";
      }
   };
   public static final Interpolator LINEAR = new Interpolator() {
      @Override
      protected double curve(double progress) {
         return progress;
      }

      @Override
      public String toString() {
         return "Interpolator.LINEAR";
      }
   };
   public static final Interpolator EASE_BOTH = new Interpolator() {
      @Override
      protected double curve(double progress) {
         return Interpolator.clampProgress(progress < 0.2 ? 3.125 * progress * progress : (progress > 0.8 ? -3.125 * progress * progress + 6.25 * progress - 2.125 : 1.25 * progress - 0.125));
      }

      @Override
      public String toString() {
         return "Interpolator.EASE_BOTH";
      }
   };
   public static final Interpolator EASE_IN = new Interpolator() {
      @Override
      protected double curve(double progress) {
         return Interpolator.clampProgress(progress < 0.2 ? 2.7777777777777777 * progress * progress : 1.1111111111111112 * progress - 0.1111111111111111);
      }

      @Override
      public String toString() {
         return "Interpolator.EASE_IN";
      }
   };
   public static final Interpolator EASE_OUT = new Interpolator() {
      @Override
      protected double curve(double progress) {
         return Interpolator.clampProgress(
            progress > 0.8 ? -2.7777777777777777 * progress * progress + 5.555555555555555 * progress + -1.7777777777777777 : 1.1111111111111112 * progress
         );
      }

      @Override
      public String toString() {
         return "Interpolator.EASE_OUT";
      }
   };

   protected Interpolator() {
   }

   public boolean interpolate(boolean startValue, boolean endValue, double progress) {
      return Math.abs(this.curve(progress) - 1.0) < EPSILON ? endValue : startValue;
   }

   public double interpolate(double startValue, double endValue, double progress) {
      return startValue + (endValue - startValue) * this.curve(progress);
   }

   public int interpolate(int startValue, int endValue, double progress) {
      return startValue + (int)Math.round((endValue - startValue) * this.curve(progress));
   }

   public long interpolate(long startValue, long endValue, double progress) {
      return startValue + Math.round((endValue - startValue) * this.curve(progress));
   }

   static double clampProgress(double progress) {
      return progress < 0.0 ? 0.0 : (progress > 1.0 ? 1.0 : progress);
   }

   protected abstract double curve(double progress);
}
