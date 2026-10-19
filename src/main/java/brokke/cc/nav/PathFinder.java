package brokke.cc.nav;

import brokke.cc.core.MovementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WitherRoseBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

public class PathFinder {
   private static final int MAX_ITERATIONS = 8000;
   private static final int[][] CARDINALS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int[][] DIAGONALS = new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

   public static List<PathNode> a(World var0, BlockPos var1, NavGoal var2) {
      return a(var0, var1, var2, null, null);
   }

   public static List<PathNode> a(World var0, BlockPos var1, NavGoal var2, BlockPos var3) {
      return a(var0, var1, var2, var3, null);
   }

   public static List<PathNode> a(World var0, BlockPos var1, NavGoal var2, BlockPos var3, BlockPos var4) {
      if (var0 != null && var1 != null && var2 != null) {
         BlockPos var5 = var1;
         if (!d(var0, var5) && e(var0, var5)) {
            var5 = var5.up();
         } else if (!b(var0, var5) && b(var0, var5.down())) {
            var5 = var5.down();
         } else if (!e(var0, var5.down()) && !b(var0, var5) && !b(var0, var5.down())) {
            for (int var6 = 1; var6 <= 3; var6++) {
               if (e(var0, var5.down(var6 + 1))) {
                  var5 = var5.down(var6);
                  break;
               }
            }
         }

         PathCurve var30 = null;
         if (var2.a()) {
            BlockPos var7 = var3 != null ? var3 : var5;
            var30 = new PathCurve(var7.getX() + 0.5, var7.getZ() + 0.5, var2.b(), var2.c());
         }

         List var31 = Collections.emptyList();
         if (var0 != null) {
            Box var8 = new Box(var5).expand(48.0);
            List var9 = var0.getEntitiesByClass(
               LivingEntity.class, var8, var0x -> var0x != null && var0x.isAlive() && (var0x instanceof HostileEntity || var0x instanceof SlimeEntity)
            );
            if (!var9.isEmpty()) {
               var31 = new ArrayList(var9.size());

               for (LivingEntity var11 : (Iterable<LivingEntity>)(Iterable<?>)(var9)) {
                  var31.add(var11.getEntityPos());
               }
            }
         }

         PriorityQueue var32 = new PriorityQueue();
         HashMap var33 = new HashMap();
         PathNode var34 = new PathNode(var5, null, 0.0, var2.b(var5), MovementType.WALK);
         var32.add(var34);
         var33.put(var5, 0.0);
         PathNode var35 = var34;
         double var12 = var34.getHCost();
         PathNode var14 = null;
         double var15 = Double.MAX_VALUE;
         PathNode var17 = null;
         double var18 = 0.0;
         int var20 = 0;

         while (!var32.isEmpty() && var20++ < 8000) {
            PathNode var21 = (PathNode)var32.poll();
            Double var22 = (Double)var33.get(var21.getPos());
            if (var22 == null || !(var21.getGCost() > var22)) {
               if (var2.a(var21.getPos())) {
                  return a(var21);
               }

               if (var21 != var34) {
                  if (var21.getHCost() < var15) {
                     var15 = var21.getHCost();
                     var14 = var21;
                  }

                  if (var21.getGCost() > var18) {
                     var18 = var21.getGCost();
                     var17 = var21;
                  }
               }

               if (var21.getHCost() < var12) {
                  var12 = var21.getHCost();
                  var35 = var21;
               }

               for (PathNode var25 : (Iterable<PathNode>)(Iterable<?>)(a(var0, var21, var2, var30, var4, var31))) {
                  BlockPos var26 = var25.getPos();
                  double var27 = var25.getGCost();
                  Double var29 = (Double)var33.get(var26);
                  if (var29 == null || var27 < var29) {
                     var33.put(var26, var27);
                     var32.add(var25);
                     if (var25.getHCost() < var12) {
                        var12 = var25.getHCost();
                        var35 = var25;
                     }

                     if (var25.getHCost() < var15) {
                        var15 = var25.getHCost();
                        var14 = var25;
                     }

                     if (var25.getGCost() > var18) {
                        var18 = var25.getGCost();
                        var17 = var25;
                     }
                  }
               }
            }
         }

         if (var35 != null && var35 != var34) {
            return a(var35);
         } else if (var17 != null && var17 != var34 && var17.getGCost() >= 2.0) {
            return a(var17);
         } else if (var14 != null && var14 != var34) {
            return a(var14);
         } else {
            return var17 != null && var17 != var34 ? a(var17) : Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   private static List<PathNode> a(World var0, PathNode var1, NavGoal var2, PathCurve var3, BlockPos var4, List<Vec3d> var5) {
      ArrayList var6 = new ArrayList();
      BlockPos var7 = var1.getPos();

      for (int[] var11 : CARDINALS) {
         int var12 = var11[0];
         int var13 = var11[1];
         a(var0, var1, var7, var12, var13, 1.0, false, var2, var3, var4, var5, var6);
      }

      for (int[] var25 : DIAGONALS) {
         int var27 = var25[0];
         int var28 = var25[1];
         a(var0, var1, var7, var27, var28, 1.4142, true, var2, var3, var4, var5, var6);
      }

      if (h(var0, var7)) {
         BlockPos var16 = var7.up();
         if (h(var0, var16) || d(var0, var16) && d(var0, var16.up())) {
            var6.add(new PathNode(var16, var1, var1.getGCost() + 1.1, var2.b(var16), MovementType.WALK));
         }

         BlockPos var19 = var7.down();
         if (h(var0, var19) || d(var0, var19) && (e(var0, var19.down()) || h(var0, var19.down()))) {
            var6.add(new PathNode(var19, var1, var1.getGCost() + 1.0, var2.b(var19), MovementType.WALK));
         }
      }

      if (b(var0, var7)) {
         BlockPos var17 = var7.up();
         if (b(var0, var17)) {
            int var20 = c(var0, var17);
            double var23 = var20 >= 100 ? 45.0 : (var20 <= 2 ? 0.0 : (var20 - 2) * 2.5);
            var6.add(new PathNode(var17, var1, var1.getGCost() + 0.85 + var23, var2.b(var17), MovementType.SWIM));
         }

         BlockPos var21 = var7.down();
         if (b(var0, var21)) {
            int var24 = c(var0, var21);
            double var26 = var24 >= 100 ? 45.0 : (var24 <= 2 ? 0.0 : (var24 - 2) * 2.5);
            double var29 = e(var0, var21.down()) ? 0.35 : 0.0;
            var6.add(new PathNode(var21, var1, var1.getGCost() + 0.85 + var26 + var29, var2.b(var21), MovementType.SWIM));
         }
      }

      return var6;
   }

   private static void a(
      World var0,
      PathNode var1,
      BlockPos var2,
      int var3,
      int var4,
      double var5,
      boolean var7,
      NavGoal var8,
      PathCurve var9,
      BlockPos var10,
      List<Vec3d> var11,
      List<PathNode> var12
   ) {
      BlockPos var13 = var2.add(var3, 0, var4);
      if (a(var0, var13)) {
         if (var7) {
            if (b(var0, var2) && !b(var0, var13)) {
               return;
            }

            BlockPos var14 = var2.add(var3, 0, 0);
            BlockPos var15 = var2.add(0, 0, var4);
            if (!d(var0, var14) || !d(var0, var14.up()) || !d(var0, var15) || !d(var0, var15.up())) {
               return;
            }
         }

         double var32 = 0.0;
         if (var9 != null && var9.a()) {
            double var16 = var9.a(var13.getX() + 0.5, var13.getZ() + 0.5);
            var32 = 0.08 * Math.min(var16, 6.0);
         }

         if (var10 != null
            && Math.abs(var13.getX() - var10.getX()) <= 2
            && Math.abs(var13.getZ() - var10.getZ()) <= 2
            && Math.abs(var13.getY() - var10.getY()) <= 2) {
            var5 += 80.0;
         }

         double var33 = 0.0;
         if (!d(var0, var13.east())
            || !d(var0, var13.west())
            || !d(var0, var13.north())
            || !d(var0, var13.south())
            || !d(var0, var13.east().up())
            || !d(var0, var13.west().up())
            || !d(var0, var13.north().up())
            || !d(var0, var13.south().up())) {
            var33 = 0.05;
         }

         double var18 = 0.0;
         if (var11 != null && !var11.isEmpty()) {
            for (Vec3d var21 : var11) {
               double var22 = var13.getX() + 0.5 - var21.x;
               double var24 = var13.getY() - var21.y;
               double var26 = var13.getZ() + 0.5 - var21.z;
               double var28 = var22 * var22 + var26 * var26;
               if (Math.abs(var24) < 3.5 && var28 < 36.0) {
                  double var30 = Math.sqrt(var28);
                  if (var30 < 2.5) {
                     var18 += (2.5 - var30) * 12.0 + 8.0;
                  } else {
                     var18 += (6.0 - var30) * 2.0;
                  }
               }
            }

            if (var18 > 35.0) {
               var18 = 35.0;
            }
         }

         double var34 = 0.0;
         if (!b(var0, var13)) {
            for (int[] var25 : CARDINALS) {
               BlockPos var53 = var13.add(var25[0], 0, var25[1]);
               if (d(var0, var53) && !e(var0, var53.down(1)) && !e(var0, var53.down(2)) && !e(var0, var53.down(3)) && !b(var0, var53.down(1))) {
                  var34 += 0.85;
               }
            }
         }

         if (d(var0, var13) && d(var0, var13.up())) {
            if (!b(var0, var13) && (e(var0, var13.down()) || h(var0, var13) || h(var0, var13.down()))) {
               if (var7) {
                  BlockPos var40 = var2.add(var3, 0, 0);
                  BlockPos var45 = var2.add(0, 0, var4);
                  if (!e(var0, var40.down()) && !e(var0, var45.down())) {
                     return;
                  }
               }

               var12.add(
                  new PathNode(
                     var13, var1, var1.getGCost() + var5 + var32 + var33 + var34 + var18, var8.b(var13), var7 ? MovementType.WALK_DIAGONAL : MovementType.WALK
                  )
               );
               return;
            }

            if (b(var0, var13)) {
               int var39 = c(var0, var13);
               double var44 = var39 >= 100 ? 45.0 : (var39 <= 2 ? 0.0 : (var39 - 2) * 2.5);
               double var52 = e(var0, var13.down()) ? 0.35 : 0.0;
               var12.add(new PathNode(var13, var1, var1.getGCost() + var5 * 0.85 + var44 + var52 + var32 + var33 + var18, var8.b(var13), MovementType.SWIM));
               return;
            }

            if (b(var0, var13.down())) {
               int var38 = c(var0, var13.down());
               double var43 = var38 >= 100 ? 45.0 : (var38 <= 2 ? 0.0 : (var38 - 2) * 2.5);
               var12.add(
                  new PathNode(var13.down(), var1, var1.getGCost() + var5 * 0.85 + var43 + var32 + var33 + var18, var8.b(var13.down()), MovementType.SWIM)
               );
               return;
            }

            for (int var36 = 1; var36 <= 3; var36++) {
               BlockPos var41 = var13.down(var36);
               if (!d(var0, var41.up())) {
                  break;
               }

               boolean var47 = true;
               if (var7) {
                  BlockPos var49 = var2.add(var3, 0, 0);
                  BlockPos var54 = var2.add(0, 0, var4);
                  if (!d(var0, var49.down(var36 - 1)) || !d(var0, var54.down(var36 - 1))) {
                     var47 = false;
                  }
               }

               if (var47
                  && d(var0, var41)
                  && d(var0, var41.up())
                  && (e(var0, var41.down()) || b(var0, var41) || h(var0, var41) || h(var0, var41.down()))
                  && (!b(var0, var41) || c(var0, var41) < 100)) {
                  boolean var50 = g(var0, var41.down()) || h(var0, var41) || h(var0, var41.down());
                  MovementType var55 = var50 ? MovementType.WALK : MovementType.FALL_DOWN;
                  var12.add(new PathNode(var41, var1, var1.getGCost() + var5 + var36 * 0.35 + var32 + var33 + var34 + var18, var8.b(var41), var55));
                  return;
               }
            }
         }

         BlockPos var37 = var13.up(1);
         boolean var42 = !var7;
         if (var42
            && d(var0, var2.up(2))
            && d(var0, var37)
            && d(var0, var37.up())
            && (e(var0, var37.down()) || h(var0, var37) || h(var0, var37.down()))
            && (!b(var0, var2) || !b(var0, var2.up(1)))) {
            boolean var48 = g(var0, var37.down());
            boolean var51 = h(var0, var37) || h(var0, var37.down());
            MovementType var56 = !var48 && !var51 ? MovementType.JUMP_UP : (var7 ? MovementType.WALK_DIAGONAL : MovementType.WALK);
            double var27 = !var48 && !var51 ? 0.3 : 0.05;
            var12.add(new PathNode(var37, var1, var1.getGCost() + var5 + var27 + var32 + var33 + var34 + var18, var8.b(var37), var56));
         }
      }
   }

   public static boolean a(World var0, BlockPos var1) {
      return var0 == null ? false : var0.isChunkLoaded(var1.getX() >> 4, var1.getZ() >> 4);
   }

   public static boolean b(World var0, BlockPos var1) {
      if (var0 != null && var1.getY() >= var0.getBottomY() && var1.getY() < var0.getTopYInclusive()) {
         FluidState var2 = var0.getFluidState(var1);
         if (var2.isEmpty()) {
            return false;
         } else if (var2.getFluid() == Fluids.WATER) {
            return true;
         } else {
            return var2.getFluid() == Fluids.FLOWING_WATER ? var2.getLevel() >= 7 : false;
         }
      } else {
         return false;
      }
   }

   public static int c(World var0, BlockPos var1) {
      if (var0 == null) {
         return 0;
      }

      int var2 = 0;
      Mutable var3 = var1.mutableCopy();

      while (var2 < 24) {
         var3.move(Direction.UP);
         if (!b(var0, var3)) {
            BlockState var4 = var0.getBlockState(var3);
            if (var4.isAir() || var4.getFluidState().isEmpty() && d(var0, var3)) {
               return var2;
            }

            return 999;
         }

         var2++;
      }

      return var2;
   }

   public static boolean d(World var0, BlockPos var1) {
      if (var0 != null && var1.getY() >= var0.getBottomY() && var1.getY() < var0.getTopYInclusive()) {
         BlockState var2 = var0.getBlockState(var1);
         if (var2.isAir()) {
            return true;
         } else {
            Block var3 = var2.getBlock();
            if (!(var3 instanceof CactusBlock)
               && !(var3 instanceof SweetBerryBushBlock)
               && !(var3 instanceof WitherRoseBlock)
               && !(var3 instanceof FireBlock)
               && !(var3 instanceof CampfireBlock)
               && !(var3 instanceof MagmaBlock)
               && !(var3 instanceof CobwebBlock)) {
               FluidState var4 = var2.getFluidState();
               return var4.getFluid() != Fluids.LAVA && var4.getFluid() != Fluids.FLOWING_LAVA ? var2.getCollisionShape(var0, var1).isEmpty() : false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean e(World var0, BlockPos var1) {
      if (var0 != null && var1.getY() >= var0.getBottomY() && var1.getY() < var0.getTopYInclusive()) {
         BlockState var2 = var0.getBlockState(var1);
         if (var2.isAir()) {
            return false;
         } else {
            Block var3 = var2.getBlock();
            if (!(var3 instanceof CactusBlock)
               && !(var3 instanceof SweetBerryBushBlock)
               && !(var3 instanceof WitherRoseBlock)
               && !(var3 instanceof FireBlock)
               && !(var3 instanceof CampfireBlock)
               && !(var3 instanceof MagmaBlock)
               && !(var3 instanceof CobwebBlock)
               && !(var3 instanceof FenceBlock)
               && !(var3 instanceof FenceGateBlock)
               && !(var3 instanceof WallBlock)) {
               FluidState var4 = var2.getFluidState();
               return var4.getFluid() != Fluids.LAVA && var4.getFluid() != Fluids.FLOWING_LAVA ? !var2.getCollisionShape(var0, var1).isEmpty() : false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean f(World var0, BlockPos var1) {
      if (var0 != null && var1 != null) {
         boolean var2 = !d(var0, var1.east()) || !d(var0, var1.east().up());
         boolean var3 = !d(var0, var1.west()) || !d(var0, var1.west().up());
         boolean var4 = !d(var0, var1.north()) || !d(var0, var1.north().up());
         boolean var5 = !d(var0, var1.south()) || !d(var0, var1.south().up());
         return var2 && var3 || var4 && var5;
      } else {
         return false;
      }
   }

   public static boolean g(World var0, BlockPos var1) {
      if (var0 != null && var1 != null) {
         BlockState var2 = var0.getBlockState(var1);
         Block var3 = var2.getBlock();
         return var3 instanceof StairsBlock || var3 instanceof SlabBlock;
      } else {
         return false;
      }
   }

   public static boolean h(World var0, BlockPos var1) {
      if (var0 != null && var1 != null) {
         BlockState var2 = var0.getBlockState(var1);
         Block var3 = var2.getBlock();
         return var3 instanceof LadderBlock || var3 instanceof VineBlock || var3 instanceof ScaffoldingBlock || var2.isIn(BlockTags.CLIMBABLE);
      } else {
         return false;
      }
   }

   private static List<PathNode> a(PathNode var0) {
      ArrayList var1 = new ArrayList();

      for (PathNode var2 = var0; var2 != null; var2 = var2.getParent()) {
         var1.add(var2);
      }

      Collections.reverse(var1);
      return var1;
   }
}
