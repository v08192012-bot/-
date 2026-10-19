package brokke.cc.nav;

import net.minecraft.util.math.BlockPos;

public class GoalNear implements NavGoal {
   private final BlockPos target;
   private final double radius;

   public GoalNear(BlockPos var1, double var2) {
      this.target = var1;
      this.radius = var2;
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public double getRadius() {
      return this.radius;
   }

   @Override
   public boolean a(BlockPos var1) {
      double var2 = var1.getX() + 0.5 - (this.target.getX() + 0.5);
      double var4 = var1.getY() - this.target.getY();
      double var6 = var1.getZ() + 0.5 - (this.target.getZ() + 0.5);
      return var2 * var2 + var4 * var4 + var6 * var6 <= this.radius * this.radius;
   }

   @Override
   public double b(BlockPos var1) {
      double var2 = var1.getX() - this.target.getX();
      double var4 = var1.getY() - this.target.getY();
      double var6 = var1.getZ() - this.target.getZ();
      double var8 = Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
      return Math.max(0.0, var8 - this.radius);
   }

   @Override
   public boolean a() {
      return true;
   }

   @Override
   public double b() {
      return this.target.getX() + 0.5;
   }

   @Override
   public double c() {
      return this.target.getZ() + 0.5;
   }
}
