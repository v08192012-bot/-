package brokke.cc.rotation;

public final class HolyWorldTrajectoryPlanner {
   private static final float[] PITCH_CANDIDATES = new float[]{-60.0F, -35.0F, -15.0F, 0.0F, 15.0F, 40.0F, 65.0F, 90.0F};
   private final HolyWorldElytraPhysics.a[] currentStates = a(PITCH_CANDIDATES.length);
   private final HolyWorldElytraPhysics.a[] nextStates = a(PITCH_CANDIDATES.length);
   private final double[] currentScores = new double[PITCH_CANDIDATES.length];
   private final double[] nextScores = new double[PITCH_CANDIDATES.length];
   private final float[] currentPitches = new float[PITCH_CANDIDATES.length];
   private final float[] nextPitches = new float[PITCH_CANDIDATES.length];
   private final float[] evaluatedPitches = new float[PITCH_CANDIDATES.length];
   private final int horizonSteps;
   private final HolyWorldElytraPhysics.a tempState = new HolyWorldElytraPhysics.a();

   public HolyWorldTrajectoryPlanner(int var1) {
      this.horizonSteps = Math.max(2, var1);
   }

   private static HolyWorldElytraPhysics.a[] a(int var0) {
      HolyWorldElytraPhysics.a[] var1 = new HolyWorldElytraPhysics.a[var0];

      for (int var2 = 0; var2 < var0; var2++) {
         var1[var2] = new HolyWorldElytraPhysics.a();
      }

      return var1;
   }

   public float a(HolyWorldElytraPhysics.a var1, float var2, float var3, HolyWorldTerrainProfile var4, int var5) {
      float var6 = var2;
      double var7 = Double.NEGATIVE_INFINITY;
      int var9 = 0;

      for (float var13 : PITCH_CANDIDATES) {
         float var14 = a(var2, var13, var3);
         boolean var15 = false;

         for (int var16 = 0; var16 < var9; var16++) {
            if (this.evaluatedPitches[var16] == var14) {
               var15 = true;
               break;
            }
         }

         if (!var15) {
            this.evaluatedPitches[var9++] = var14;
            double var18 = this.b(var1, var14, var3, var4, var5);
            if (var18 > var7) {
               var7 = var18;
               var6 = var14;
            }
         }
      }

      return var6;
   }

   private double b(HolyWorldElytraPhysics.a var1, float var2, float var3, HolyWorldTerrainProfile var4, int var5) {
      for (int var6 = 0; var6 < PITCH_CANDIDATES.length; var6++) {
         this.currentScores[var6] = Double.NEGATIVE_INFINITY;
      }

      this.currentStates[0].a(var1);
      this.currentScores[0] = a(this.currentStates[0], var2, var4, var5);
      this.currentPitches[0] = var2;

      for (int var13 = 1; var13 < this.horizonSteps; var13++) {
         for (int var7 = 0; var7 < PITCH_CANDIDATES.length; var7++) {
            this.nextScores[var7] = Double.NEGATIVE_INFINITY;
         }

         for (int var15 = 0; var15 < PITCH_CANDIDATES.length; var15++) {
            if (this.currentScores[var15] != Double.NEGATIVE_INFINITY) {
               for (int var8 = 0; var8 < PITCH_CANDIDATES.length; var8++) {
                  float var9 = a(this.currentPitches[var15], PITCH_CANDIDATES[var8], var3);
                  this.tempState.a(this.currentStates[var15]);
                  double var10 = this.currentScores[var15] + a(this.tempState, var9, var4, var5);
                  if (var10 > this.nextScores[var8]) {
                     this.nextScores[var8] = var10;
                     this.nextPitches[var8] = var9;
                     this.nextStates[var8].a(this.tempState);
                  }
               }
            }
         }

         for (int var16 = 0; var16 < PITCH_CANDIDATES.length; var16++) {
            this.currentScores[var16] = this.nextScores[var16];
            this.currentPitches[var16] = this.nextPitches[var16];
            if (this.nextScores[var16] != Double.NEGATIVE_INFINITY) {
               this.currentStates[var16].a(this.nextStates[var16]);
            }
         }
      }

      double var14 = Double.NEGATIVE_INFINITY;

      for (double var11 : this.currentScores) {
         var14 = Math.max(var14, var11);
      }

      return var14;
   }

   private static double a(HolyWorldElytraPhysics.a var0, float var1, HolyWorldTerrainProfile var2, int var3) {
      double var4 = var0.posX;
      double var6 = var4 + var0.motionX;
      double var8 = var0.motionX;
      boolean var10 = HolyWorldElytraPhysics.a(var0, var1, var2.b(var4), var2.b(var6), var2.c(var6), var3);
      double var11 = var10 ? 50.0 + 100.0 * Math.max(0.0, var8) : 0.0;
      return var0.posX - var4 - var11;
   }

   private static float a(float var0, float var1, float var2) {
      float var3 = var1 - var0;
      if (var3 > var2) {
         return var0 + var2;
      } else {
         return var3 < -var2 ? var0 - var2 : var1;
      }
   }
}
