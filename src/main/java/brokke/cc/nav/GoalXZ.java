package brokke.cc.nav;

import net.minecraft.util.math.BlockPos;

public class GoalXZ implements NavGoal {
   private final int x;
   private final int z;

   public GoalXZ(int var1, int var2) {
      this.x = var1;
      this.z = var2;
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   @Override
   public boolean a(BlockPos var1) {
      return var1.getX() == this.x && var1.getZ() == this.z;
   }

   @Override
   public double b(BlockPos var1) {
      double var2 = var1.getX() - this.x;
      double var4 = var1.getZ() - this.z;
      return Math.sqrt(var2 * var2 + var4 * var4);
   }

   @Override
   public boolean a() {
      return true;
   }

   @Override
   public double b() {
      return this.x + 0.5;
   }

   @Override
   public double c() {
      return this.z + 0.5;
   }
}
