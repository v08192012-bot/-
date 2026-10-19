package brokke.cc.nav;

public class Waypoint {
   private double x;
   private double y;
   private double z;
   private String name;

   public Waypoint(double var1, double var3, double var5, String var7) {
      this.x = var1;
      this.y = var3;
      this.z = var5;
      this.name = var7;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public String getName() {
      return this.name;
   }

   public void setX(double var1) {
      this.x = var1;
   }

   public void setY(double var1) {
      this.y = var1;
   }

   public void setZ(double var1) {
      this.z = var1;
   }

   public void setName(String var1) {
      this.name = var1;
   }
}
