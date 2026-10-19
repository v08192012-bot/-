package brokke.cc.animation;

public final class Easings3 {
   public static final EasingFunction LINEAR = inputProgress -> clampProgress(inputProgress);
   public static final EasingFunction EASE_IN_QUAD = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress * progress;
   };
   public static final EasingFunction EASE_OUT_QUAD = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return 1.0F - (1.0F - progress) * (1.0F - progress);
   };
   public static final EasingFunction EASE_IN_OUT_QUAD = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F ? 2.0F * progress * progress : 1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 2.0) / 2.0F;
   };
   public static final EasingFunction EASE_IN_CUBIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress * progress * progress;
   };
   public static final EasingFunction EASE_OUT_CUBIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      float inverseProgress = 1.0F - progress;
      return 1.0F - inverseProgress * inverseProgress * inverseProgress;
   };
   public static final EasingFunction EASE_IN_OUT_CUBIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F ? 4.0F * progress * progress * progress : 1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 3.0) / 2.0F;
   };
   public static final EasingFunction EASE_IN_QUART = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress * progress * progress * progress;
   };
   public static final EasingFunction EASE_OUT_QUART = inputProgress -> {
      float progress = clampProgress(inputProgress);
      float inverseProgress = 1.0F - progress;
      return 1.0F - inverseProgress * inverseProgress * inverseProgress * inverseProgress;
   };
   public static final EasingFunction EASE_IN_OUT_QUART = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F ? 8.0F * progress * progress * progress * progress : 1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 4.0) / 2.0F;
   };
   public static final EasingFunction EASE_IN_QUINT = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress * progress * progress * progress * progress;
   };
   public static final EasingFunction EASE_OUT_QUINT = inputProgress -> {
      float progress = clampProgress(inputProgress);
      float inverseProgress = 1.0F - progress;
      return 1.0F - inverseProgress * inverseProgress * inverseProgress * inverseProgress * inverseProgress;
   };
   public static final EasingFunction EASE_IN_OUT_QUINT = inputProgress -> {
      float progress = clampProgress(inputProgress);
      if (progress < 0.5F) {
         float firstHalfProgress = progress * 2.0F;
         return 0.5F * firstHalfProgress * firstHalfProgress * firstHalfProgress * firstHalfProgress * firstHalfProgress;
      } else {
         float secondHalfProgress = (progress - 0.5F) * 2.0F;
         float inverseProgress = 1.0F - secondHalfProgress;
         return 1.0F - 0.5F * inverseProgress * inverseProgress * inverseProgress * inverseProgress * inverseProgress;
      }
   };
   public static final EasingFunction EASE_IN_SINE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return 1.0F - (float)Math.cos(progress * Math.PI / 2.0);
   };
   public static final EasingFunction EASE_OUT_SINE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return (float)Math.sin(progress * Math.PI / 2.0);
   };
   public static final EasingFunction EASE_IN_OUT_SINE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return -((float)(Math.cos(Math.PI * progress) - 1.0)) / 2.0F;
   };
   private static final float C1 = 1.70158F;
   private static final float C2 = 2.5949094F;
   private static final float C3 = 2.70158F;
   public static final EasingFunction EASE_IN_BACK = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return 2.70158F * progress * progress * progress - 1.70158F * progress * progress;
   };
   public static final EasingFunction EASE_OUT_BACK = inputProgress -> {
      float progress = clampProgress(inputProgress);
      float shiftedProgress = progress - 1.0F;
      return 1.0F + 2.70158F * (float)Math.pow(shiftedProgress, 3.0) + 1.70158F * (float)Math.pow(shiftedProgress, 2.0);
   };
   public static final EasingFunction EASE_IN_OUT_BACK = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F
         ? (float)Math.pow(2.0F * progress, 2.0) * (7.189819F * progress - 2.5949094F) / 2.0F
         : ((float)Math.pow(2.0F * progress - 2.0F, 2.0) * (3.5949094F * (progress * 2.0F - 2.0F) + 2.5949094F) + 2.0F) / 2.0F;
   };
   public static final EasingFunction EASE_IN_CIRC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return 1.0F - (float)Math.sqrt(1.0F - (float)Math.pow(progress, 2.0));
   };
   public static final EasingFunction EASE_OUT_CIRC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return (float)Math.sqrt(1.0F - (float)Math.pow(progress - 1.0F, 2.0));
   };
   public static final EasingFunction EASE_IN_OUT_CIRC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F
         ? (1.0F - (float)Math.sqrt(1.0F - (float)Math.pow(2.0F * progress, 2.0))) / 2.0F
         : ((float)Math.sqrt(1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 2.0)) + 1.0F) / 2.0F;
   };
   private static final float C4 = (float) (Math.PI * 2.0 / 3.0);
   private static final float C5 = (float) Math.PI * 4.0F / 9.0F;
   public static final EasingFunction EASE_IN_ELASTIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress == 0.0F
         ? 0.0F
         : (progress == 1.0F ? 1.0F : -((float)Math.pow(2.0, 10.0F * progress - 10.0F)) * (float)Math.sin((progress * 10.0F - 10.75F) * (float) (Math.PI * 2.0 / 3.0)));
   };
   public static final EasingFunction EASE_OUT_ELASTIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress == 0.0F
         ? 0.0F
         : (progress == 1.0F ? 1.0F : (float)Math.pow(2.0, -10.0F * progress) * (float)Math.sin((progress * 10.0F - 0.75F) * (float) (Math.PI * 2.0 / 3.0)) + 1.0F);
   };
   public static final EasingFunction EASE_IN_OUT_ELASTIC = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress == 0.0F
         ? 0.0F
         : (
            progress == 1.0F
               ? 1.0F
               : (
                  progress < 0.5F
                     ? -((float)Math.pow(2.0, 20.0F * progress - 10.0F) * (float)Math.sin((20.0F * progress - 11.125F) * ((float) Math.PI * 4.0F / 9.0F))) / 2.0F
                     : (float)Math.pow(2.0, -20.0F * progress + 10.0F) * (float)Math.sin((20.0F * progress - 11.125F) * ((float) Math.PI * 4.0F / 9.0F)) / 2.0F + 1.0F
               )
         );
   };
   public static final EasingFunction EASE_IN_BOUNCE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return 1.0F - bounceOut(1.0F - progress);
   };
   public static final EasingFunction EASE_OUT_BOUNCE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return bounceOut(progress);
   };
   public static final EasingFunction EASE_IN_OUT_BOUNCE = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress < 0.5F ? (1.0F - bounceOut(1.0F - 2.0F * progress)) / 2.0F : (1.0F + bounceOut(2.0F * progress - 1.0F)) / 2.0F;
   };
   public static final EasingFunction SMOOTH_STEP = inputProgress -> {
      float progress = clampProgress(inputProgress);
      return progress * progress * (3.0F - 2.0F * progress);
   };
   public static final EasingFunction FIGMA_EASE_IN_OUT = cubicBezier(0.42F, 0.0F, 0.58F, 1.0F);

   private Easings3() {
   }

   private static float clampProgress(float inputProgress) {
      if (inputProgress <= 0.0F) {
         return 0.0F;
      } else {
         return inputProgress >= 1.0F ? 1.0F : inputProgress;
      }
   }

   private static float bounceOut(float progress) {
      float bounceCoefficient = 7.5625F;
      float bounceDivisor = 2.75F;
      if (progress < 1.0F / bounceDivisor) {
         return bounceCoefficient * progress * progress;
      } else if (progress < 2.0F / bounceDivisor) {
         float shiftedProgress = progress - 1.5F / bounceDivisor;
         return bounceCoefficient * shiftedProgress * shiftedProgress + 0.75F;
      } else if (progress < 2.5F / bounceDivisor) {
         float shiftedProgress = progress - 2.25F / bounceDivisor;
         return bounceCoefficient * shiftedProgress * shiftedProgress + 0.9375F;
      } else {
         float shiftedProgress = progress - 2.625F / bounceDivisor;
         return bounceCoefficient * shiftedProgress * shiftedProgress + 0.984375F;
      }
   }

   private static EasingFunction cubicBezier(float controlX1, float controlY1, float controlX2, float controlY2) {
      return inputProgress -> {
         float progress = clampProgress(inputProgress);
         if (!(progress <= 0.0F) && !(progress >= 1.0F)) {
            float lowerBound = 0.0F;
            float upperBound = 1.0F;
            float parameter = progress;

            for (int iteration = 0; iteration < 14; iteration++) {
               float coordinate = bezierCoordinate(parameter, controlX1, controlX2);
               if (coordinate < progress) {
                  lowerBound = parameter;
               } else {
                  upperBound = parameter;
               }

               parameter = (lowerBound + upperBound) * 0.5F;
            }

            return clampProgress(bezierCoordinate(parameter, controlY1, controlY2));
         } else {
            return progress;
         }
      };
   }

   private static float bezierCoordinate(float parameter, float firstControl, float secondControl) {
      float inverseParameter = 1.0F - parameter;
      return 3.0F * inverseParameter * inverseParameter * parameter * firstControl + 3.0F * inverseParameter * parameter * parameter * secondControl + parameter * parameter * parameter;
   }
}
