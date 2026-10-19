package brokke.cc.config;

public final class SpringConfig {
   private final float frequencyHz;
   private final float dampingRatio;

   private SpringConfig(float var1, float var2) {
      if (var1 <= 0.0F) {
         throw new IllegalArgumentException("frequencyHz must be > 0");
      }

      if (var2 <= 0.0F) {
         throw new IllegalArgumentException("dampingRatio must be > 0");
      }

      this.frequencyHz = var1;
      this.dampingRatio = var2;
   }

   public static SpringConfig a(float var0, float var1) {
      return new SpringConfig(var0, var1);
   }

   public float getFrequencyHz() {
      return this.frequencyHz;
   }

   public float getDampingRatio() {
      return this.dampingRatio;
   }
}
