package brokke.cc.rotation;

public final class HolyWorldTerrainProfile {
   public static final double NO_HEIGHT = -4096.0;
   private final double[] heights;
   private final double stepSize;
   private int size;

   public HolyWorldTerrainProfile(int var1, double var2) {
      this.heights = new double[var1];
      this.stepSize = var2;
   }

   public void a() {
      this.size = 0;
   }

   public void a(double var1) {
      if (this.size < this.heights.length) {
         this.heights[this.size++] = var1;
      }
   }

   public int b() {
      return this.heights.length;
   }

   public double getStepSize() {
      return this.stepSize;
   }

   public double b(double var1) {
      if (this.size == 0) {
         return -4096.0;
      } else {
         int var3 = this.d(var1);
         if (var3 < 0) {
            return this.heights[0];
         } else {
            return var3 >= this.size - 1 ? this.heights[this.size - 1] : Math.min(this.heights[var3], this.heights[var3 + 1]);
         }
      }
   }

   public double c(double var1) {
      if (this.size == 0) {
         return -4096.0;
      } else {
         int var3 = this.d(var1);
         if (var3 < 0) {
            return this.heights[0];
         } else {
            return var3 >= this.size - 1 ? this.heights[this.size - 1] : Math.max(this.heights[var3], this.heights[var3 + 1]);
         }
      }
   }

   private int d(double var1) {
      double var3 = var1 / this.stepSize;
      return var3 <= 0.0 ? -1 : (int)var3;
   }
}
