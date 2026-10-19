package brokke.cc.util;

/**
 * Easing-функции вместо линейного/рандомного плавности.
 * Профиль ускорения нелинейный — античит-детекторы смотрят именно на него.
 */
public final class Easing {
   public static final int COUNT = 6;
   private Easing() {
   }

   public static float apply(int index, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      switch (Math.floorMod(index, COUNT)) {
         case 0:
            return easeInOutSine(t);
         case 1:
            return easeOutQuart(t);
         case 2:
            return easeOutCubic(t);
         case 3:
            return easeInOutCubic(t);
         case 4:
            return easeOutSine(t);
         default:
            return easeInOutQuad(t);
      }
   }

   public static float easeInOutSine(float t) {
      return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
   }

   public static float easeOutQuart(float t) {
      return 1.0F - (float) Math.pow(1.0F - t, 4.0);
   }

   public static float easeOutCubic(float t) {
      return 1.0F - (float) Math.pow(1.0F - t, 3.0);
   }

   public static float easeInOutCubic(float t) {
      return t < 0.5F ? 4.0F * t * t * t : (float) (1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0);
   }

   public static float easeOutSine(float t) {
      return (float) Math.sin(t * Math.PI / 2.0);
   }

   public static float easeInOutQuad(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0 * t + 2.0, 2.0) / 2.0F;
   }
}
