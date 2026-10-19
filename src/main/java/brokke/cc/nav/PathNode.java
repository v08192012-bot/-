package brokke.cc.nav;

import brokke.cc.core.MovementType;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;

public class PathNode implements Comparable<PathNode> {
   private final BlockPos pos;
   private PathNode parent;
   private double gCost;
   private double hCost;
   private MovementType type;

   public PathNode(BlockPos var1, PathNode var2, double var3, double var5, MovementType var7) {
      this.pos = var1;
      this.parent = var2;
      this.gCost = var3;
      this.hCost = var5;
      this.type = var7;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public PathNode getParent() {
      return this.parent;
   }

   public void setParent(PathNode var1) {
      this.parent = var1;
   }

   public double getGCost() {
      return this.gCost;
   }

   public void setGCost(double var1) {
      this.gCost = var1;
   }

   public double getHCost() {
      return this.hCost;
   }

   public void setHCost(double var1) {
      this.hCost = var1;
   }

   public double e() {
      return this.gCost + this.hCost;
   }

   public MovementType getType() {
      return this.type;
   }

   public void setType(MovementType var1) {
      this.type = var1;
   }

   public int compareTo(PathNode var1) {
      return this.b(var1);
   }

   public int b(PathNode var1) {
      int var2 = Double.compare(this.e(), var1.e());
      return var2 == 0 ? Double.compare(this.hCost, var1.hCost) : var2;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         PathNode var2 = (PathNode)var1;
         return Objects.equals(this.pos, var2.pos);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.pos != null ? this.pos.hashCode() : 0;
   }
}
