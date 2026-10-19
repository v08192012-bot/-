package brokke.cc.animation;

public interface Easing {
   Easing BAKEK = cubicBezier(0.45F, 1.45F, 0.49F, 1.15F);
   Easing BAKEK_SMALLER = cubicBezier(0.45F, 1.45F, 0.43F, 0.91F);
   Easing BAKEK_PAGES = cubicBezier(0.1F, 1.07F, 0.34F, 1.04F);
   Easing BAKEK_SIZE = cubicBezier(0.27F, 1.09F, 0.49F, 1.06F);
   Easing FIGMA_EASE_IN_OUT = cubicBezier(0.42F, 0.0, 0.58F, 1.0);
   Easing SMOOTH_STEP = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float progress = elapsed / duration;
      return change * (progress * progress * (3.0F - 2.0F * progress)) + startValue;
   };
   Easing LINEAR = (elapsed, startValue, change, duration) -> change * elapsed / duration + startValue;
   Easing QUAD_OUT = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float progress = elapsed / duration;
      return -change * progress * (progress - 2.0F) + startValue;
   };
   Easing CUBIC_OUT = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float shiftedProgress = elapsed / duration - 1.0F;
      return change * (shiftedProgress * shiftedProgress * shiftedProgress + 1.0F) + startValue;
   };
   Easing CUBIC_IN_OUT = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float doubledProgress = elapsed / (duration / 2.0F);
      return doubledProgress < 1.0F ? change / 2.0F * doubledProgress * doubledProgress * doubledProgress + startValue : change / 2.0F * ((doubledProgress - 2.0F) * (doubledProgress - 2.0F) * (doubledProgress - 2.0F) + 2.0F) + startValue;
   };
   Easing EXPO_OUT = (elapsed, startValue, change, duration) -> elapsed == duration ? startValue + change : change * (-((float)Math.pow(2.0, -10.0F * elapsed / duration)) + 1.0F) + startValue;
   Easing SPRING = cubicBezier(0.2F, 1.02F, 0.35F, 1.0);
   Easing SPRING_SOFT = cubicBezier(0.16F, 1.0, 0.3F, 1.0);
   Easing ELASTIC_OUT = (elapsed, startValue, change, duration) -> {
      if (elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float period = duration * 0.3F;
      float phaseShift = period / 4.0F;
      float shiftedProgress = elapsed / duration - 1.0F;
      return change * ((float)Math.pow(2.0, -10.0F * shiftedProgress) * (float)Math.sin((shiftedProgress - phaseShift) * (float) (Math.PI * 2) / period) + 1.0F) + startValue;
   };
   Easing BACK_OUT = cubicBezier(0.34F, 1.56F, 0.64F, 1.0);
   Easing SMOOTH_OUT = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float progress = elapsed / duration;
      return change * (1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress)) + startValue;
   };
   Easing BOUNCE_OUT = (elapsed, startValue, change, duration) -> {
      if (elapsed <= 0.0F) {
         return startValue;
      } else if (elapsed >= duration) {
         return startValue + change;
      } else {
         float progress = elapsed / duration;
         if (progress < 0.36363637F) {
            return change * (7.5625F * progress * progress) + startValue;
         } else if (progress < 0.72727275F) {
            progress -= 0.54545456F;
            return change * (7.5625F * progress * progress + 0.75F) + startValue;
         } else if (progress < 0.90909094F) {
            progress -= 0.8181818F;
            return change * (7.5625F * progress * progress + 0.9375F) + startValue;
         } else {
            progress -= 0.95454544F;
            return change * (7.5625F * progress * progress + 0.984375F) + startValue;
         }
      }
   };
   Easing GENTLE = (elapsed, startValue, change, duration) -> {
      if (duration <= 0.0F || elapsed <= 0.0F) {
         return startValue;
      }

      if (elapsed >= duration) {
         return startValue + change;
      }

      float progress = elapsed / duration;
      float easedProgress = progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
      return change * easedProgress + startValue;
   };

   static Easing cubicBezier(double controlX1, double controlY1, double controlX2, double controlY2) {
      return new Easing() {
         @Override
         public float ease(float elapsed, float startValue, float change, float duration) {
            if (duration <= 0.0F || elapsed <= 0.0F) {
               return startValue;
            }

            if (elapsed >= duration) {
               return startValue + change;
            }

            float progress = Math.clamp(elapsed / duration, 0.0F, 1.0F);
            float parameter = this.solveBezierParameter((float)controlX1, (float)controlX2, progress);
            float easedProgress = this.bezierValue(parameter, (float)controlY1, (float)controlY2);
            return startValue + change * easedProgress;
         }

         private float solveBezierParameter(float firstControl, float secondControl, float progress) {
            float parameter = progress;

            for (int iteration = 0; iteration < 8; iteration++) {
               float coordinate = this.bezierCoordinate(parameter, firstControl, secondControl);
               float derivative = this.bezierDerivative(parameter, firstControl, secondControl);
               if (Math.abs(coordinate - progress) < 1.0E-5F || Math.abs(derivative) < 1.0E-6F) {
                  break;
               }

               parameter -= (coordinate - progress) / derivative;
               parameter = Math.max(0.0F, Math.min(1.0F, parameter));
            }

            return parameter;
         }

         private float bezierCoordinate(float parameter, float firstControl, float secondControl) {
            return 3.0F * (1.0F - parameter) * (1.0F - parameter) * parameter * firstControl + 3.0F * (1.0F - parameter) * parameter * parameter * secondControl + parameter * parameter * parameter;
         }

         private float bezierDerivative(float parameter, float firstControl, float secondControl) {
            return 3.0F * ((1.0F - parameter) * (1.0F - 3.0F * parameter) * firstControl + (2.0F * parameter - 3.0F * parameter * parameter) * secondControl) + 3.0F * parameter * parameter;
         }

         private float bezierValue(float parameter, float firstControl, float secondControl) {
            return 3.0F * (1.0F - parameter) * (1.0F - parameter) * parameter * firstControl + 3.0F * (1.0F - parameter) * parameter * parameter * secondControl + parameter * parameter * parameter;
         }
      };
   }

   float ease(float elapsed, float startValue, float change, float duration);
}
