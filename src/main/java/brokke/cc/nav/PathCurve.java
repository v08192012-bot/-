package brokke.cc.nav;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PathCurve {
   private final double x0;
   private final double z0;
   private final double x1;
   private final double z1;
   private final double ux;
   private final double uz;
   private final double nx;
   private final double nz;
   private final double totalDist;
   private final double phi1;
   private final double phi2;
   private final boolean hasCurve;

   public PathCurve(double var1, double var3, double var5, double var7) {
      this.x0 = var1;
      this.z0 = var3;
      this.x1 = var5;
      this.z1 = var7;
      double var9 = var5 - var1;
      double var11 = var7 - var3;
      double var13 = Math.hypot(var9, var11);
      this.totalDist = var13;
      this.ux = 0.0;
      this.uz = 0.0;
      this.nx = 0.0;
      this.nz = 0.0;
      this.phi1 = 0.0;
      this.phi2 = 0.0;
      this.hasCurve = false;
   }

   public boolean a() {
      return false;
   }

   public double a(double var1) {
      return 0.0;
   }

   public double a(double var1, double var3) {
      return 0.0;
   }

   public static Vec3d a(List<PathNode> var0, double var1) {
      if (var0 != null && !var0.isEmpty()) {
         int var3 = var0.size();
         if (var3 == 1) {
            BlockPos var35 = var0.get(0).getPos();
            return new Vec3d(var35.getX() + 0.5, var35.getY() + 0.1, var35.getZ() + 0.5);
         } else if (var1 <= 0.0) {
            BlockPos var34 = var0.get(0).getPos();
            return new Vec3d(var34.getX() + 0.5, var34.getY() + 0.1, var34.getZ() + 0.5);
         } else if (var1 >= var3 - 1) {
            BlockPos var33 = var0.get(var3 - 1).getPos();
            return new Vec3d(var33.getX() + 0.5, var33.getY() + 0.1, var33.getZ() + 0.5);
         } else {
            int var4 = (int)Math.floor(var1);
            double var5 = var1 - var4;
            int var7 = Math.max(0, var4 - 1);
            int var8 = var4;
            int var9 = Math.min(var3 - 1, var4 + 1);
            int var10 = Math.min(var3 - 1, var4 + 2);
            Vec3d var11 = a(var0.get(var7));
            Vec3d var12 = a(var0.get(var8));
            Vec3d var13 = a(var0.get(var9));
            Vec3d var14 = a(var0.get(var10));
            double var15 = var5 * var5;
            double var17 = var15 * var5;
            double var19 = -0.5 * var17 + var15 - 0.5 * var5;
            double var21 = 1.5 * var17 - 2.5 * var15 + 1.0;
            double var23 = -1.5 * var17 + 2.0 * var15 + 0.5 * var5;
            double var25 = 0.5 * var17 - 0.5 * var15;
            double var27 = var11.x * var19 + var12.x * var21 + var13.x * var23 + var14.x * var25;
            double var29 = var11.y * var19 + var12.y * var21 + var13.y * var23 + var14.y * var25;
            double var31 = var11.z * var19 + var12.z * var21 + var13.z * var23 + var14.z * var25;
            return new Vec3d(var27, var29, var31);
         }
      } else {
         return Vec3d.ZERO;
      }
   }

   public static List<PathCurve.a> a(List<PathNode> var0, int var1) {
      ArrayList var2 = new ArrayList();
      if (var0 != null && var0.size() >= 2) {
         int var3 = var0.size();
         int var4 = Math.max(1, var1);
         Vec3d var5 = a(var0, 0.0);

         for (int var6 = 0; var6 < var3 - 1; var6++) {
            for (int var7 = 1; var7 <= var4; var7++) {
               double var8 = var6 + (double)var7 / var4;
               Vec3d var10 = a(var0, var8);
               var2.add(new PathCurve.a(var5, var10, var6));
               var5 = var10;
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private static Vec3d a(PathNode var0) {
      BlockPos var1 = var0.getPos();
      return new Vec3d(var1.getX() + 0.5, var1.getY() + 0.1, var1.getZ() + 0.5);
   }

   public static Vec3d a(List<PathNode> var0, double var1, World var3, long var4) {
      return var0 != null && !var0.isEmpty() ? a(var0, var1) : Vec3d.ZERO;
   }

   private static boolean a(World var0, BlockPos var1) {
      return PathFinder.d(var0, var1) && PathFinder.d(var0, var1.up())
         ? PathFinder.e(var0, var1.down()) || PathFinder.b(var0, var1) || PathFinder.b(var0, var1.down())
         : false;
   }

   public record a(Vec3d start, Vec3d end, int nodeIndex) {
      public Vec3d getStart() {
         return this.start;
      }

      public Vec3d getEnd() {
         return this.end;
      }

      public int getNodeIndex() {
         return this.nodeIndex;
      }
   }
}
