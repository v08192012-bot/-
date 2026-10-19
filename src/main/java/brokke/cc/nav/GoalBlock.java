package brokke.cc.nav;

import net.minecraft.util.math.BlockPos;

public class GoalBlock implements NavGoal {
   private final BlockPos target;

   public GoalBlock(BlockPos var1) {
      this.target = var1;
   }

   public BlockPos getTarget() {
      return this.target;
   }

   @Override
   public boolean a(BlockPos var1) {
      if (var1.equals(this.target)) {
         return true;
      } else {
         return var1.getX() == this.target.getX() && var1.getZ() == this.target.getZ() ? Math.abs(var1.getY() - this.target.getY()) <= 1 : false;
      }
   }

   @Override
   public double b(BlockPos var1) {
      double var2 = var1.getX() - this.target.getX();
      double var4 = var1.getZ() - this.target.getZ();
      double var6;
      if (Math.abs(var1.getY() - this.target.getY()) <= 1) {
         var6 = 0.0;
      } else if (var1.getY() > this.target.getY()) {
         var6 = var1.getY() - (this.target.getY() + 1);
      } else {
         var6 = this.target.getY() - var1.getY();
      }

      return Math.sqrt(var2 * var2 + var6 * var6 + var4 * var4);
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
