package brokke.cc.animation;

public final class Easings2 {
   public static final double BACK_OVERSHOOT = 1.70158;
   public static final double BACK_IN_OUT_OVERSHOOT = 2.5949095;
   public static final double BACK_OVERSHOOT_PLUS_ONE = 2.70158;
   public static final double ELASTIC_PERIOD = Math.PI * 2.0 / 3.0;
   public static final double ELASTIC_IN_OUT_PERIOD = Math.PI * 4.0 / 9.0;
   public static final Easing2 LINEAR = progress -> progress;
   public static final Easing2 QUAD_IN = powerIn(2);
   public static final Easing2 QUAD_OUT = powerOut(2);
   public static final Easing2 QUAD_IN_OUT = powerInOut(2.0);
   public static final Easing2 CUBIC_IN = powerIn(3);
   public static final Easing2 CUBIC_OUT = powerOut(3);
   public static final Easing2 CUBIC_IN_OUT = powerInOut(3.0);
   public static final Easing2 QUART_IN = powerIn(4);
   public static final Easing2 QUART_OUT = powerOut(4);
   public static final Easing2 QUART_IN_OUT = powerInOut(4.0);
   public static final Easing2 QUINT_IN = powerIn(5);
   public static final Easing2 QUINT_OUT = powerOut(5);
   public static final Easing2 QUINT_IN_OUT = powerInOut(5.0);
   public static final Easing2 SINE_IN = progress -> 1.0 - Math.cos(progress * Math.PI / 2.0);
   public static final Easing2 SINE_OUT = progress -> Math.sin(progress * Math.PI / 2.0);
   public static final Easing2 SINE_IN_OUT = progress -> -(Math.cos(Math.PI * progress) - 1.0) / 2.0;
   public static final Easing2 CIRC_IN = progress -> 1.0 - Math.sqrt(1.0 - Math.pow(progress, 2.0));
   public static final Easing2 CIRC_OUT = progress -> Math.sqrt(1.0 - Math.pow(progress - 1.0, 2.0));
   public static final Easing2 CIRC_IN_OUT = progress -> progress < 0.5
      ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * progress, 2.0))) / 2.0
      : (Math.sqrt(1.0 - Math.pow(-2.0 * progress + 2.0, 2.0)) + 1.0) / 2.0;
   public static final Easing2 ELASTIC_IN = progress -> progress != 0.0 && progress != 1.0
      ? Math.pow(-2.0, 10.0 * progress - 10.0) * Math.sin((progress * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0))
      : progress;
   public static final Easing2 ELASTIC_OUT = progress -> progress != 0.0 && progress != 1.0
      ? Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0
      : progress;
   public static final Easing2 ELASTIC_IN_OUT = progress -> {
      if (progress != 0.0 && progress != 1.0) {
         return progress < 0.5
            ? -(Math.pow(2.0, 20.0 * progress - 10.0) * Math.sin((20.0 * progress - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
            : Math.pow(2.0, -20.0 * progress + 10.0) * Math.sin((20.0 * progress - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0;
      } else {
         return progress;
      }
   };
   public static final Easing2 EXPO_IN = progress -> progress != 0.0 ? Math.pow(2.0, 10.0 * progress - 10.0) : progress;
   public static final Easing2 EXPO_OUT = progress -> progress != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * progress) : progress;
   public static final Easing2 EXPO_IN_OUT = progress -> {
      if (progress != 0.0 && progress != 1.0) {
         return progress < 0.5 ? Math.pow(2.0, 20.0 * progress - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * progress + 10.0)) / 2.0;
      } else {
         return progress;
      }
   };
   public static final Easing2 BACK_IN = progress -> 2.70158 * Math.pow(progress, 3.0) - 1.70158 * Math.pow(progress, 2.0);
   public static final Easing2 BACK_OUT = progress -> 1.0 + 2.70158 * Math.pow(progress - 1.0, 3.0) + 1.70158 * Math.pow(progress - 1.0, 2.0);
   public static final Easing2 BACK_IN_OUT = progress -> progress < 0.5
      ? Math.pow(2.0 * progress, 2.0) * (7.189819 * progress - 2.5949095) / 2.0
      : (Math.pow(2.0 * progress - 2.0, 2.0) * (3.5949095 * (progress * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
   public static final Easing2 BOUNCE_OUT = progress -> {
      double bounceCoefficient = 7.5625;
      double bounceDivisor = 2.75;
      if (progress < 1.0 / bounceDivisor) {
         return bounceCoefficient * Math.pow(progress, 2.0);
      } else if (progress < 2.0 / bounceDivisor) {
         return bounceCoefficient * Math.pow(progress - 1.5 / bounceDivisor, 2.0) + 0.75;
      } else {
         return progress < 2.5 / bounceDivisor ? bounceCoefficient * Math.pow(progress - 2.25 / bounceDivisor, 2.0) + 0.9375 : bounceCoefficient * Math.pow(progress - 2.625 / bounceDivisor, 2.0) + 0.984375;
      }
   };
   public static final Easing2 BOUNCE_IN = progress -> 1.0 - BOUNCE_OUT.ease(1.0 - progress);
   public static final Easing2 BOUNCE_IN_OUT = progress -> progress < 0.5
      ? (1.0 - BOUNCE_OUT.ease(1.0 - 2.0 * progress)) / 2.0
      : (1.0 + BOUNCE_OUT.ease(2.0 * progress - 1.0)) / 2.0;

   private Easings2() {
   }

   public static Easing2 powerIn(double exponent) {
      return progress -> Math.pow(progress, exponent);
   }

   public static Easing2 powerIn(int exponent) {
      return powerIn((double)exponent);
   }

   public static Easing2 powerOut(double exponent) {
      return progress -> 1.0 - Math.pow(1.0 - progress, exponent);
   }

   public static Easing2 powerOut(int exponent) {
      return powerOut((double)exponent);
   }

   public static Easing2 powerInOut(double exponent) {
      return progress -> progress < 0.5 ? Math.pow(2.0, exponent - 1.0) * Math.pow(progress, exponent) : 1.0 - Math.pow(-2.0 * progress + 2.0, exponent) / 2.0;
   }
}
