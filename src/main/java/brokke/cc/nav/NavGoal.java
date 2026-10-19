package brokke.cc.nav;

import net.minecraft.util.math.BlockPos;

public interface NavGoal {
   boolean a(BlockPos var1);

   double b(BlockPos var1);

   default boolean a() {
      return false;
   }

   default double b() {
      return 0.0;
   }

   default double c() {
      return 0.0;
   }
}
