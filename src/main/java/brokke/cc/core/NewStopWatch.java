package brokke.cc.core;

public class NewStopWatch {
   private long startTime;

   public NewStopWatch() {
      this.a();
   }

   public boolean a(double var1) {
      return System.currentTimeMillis() - var1 >= this.startTime;
   }

   public boolean b(double var1) {
      boolean var3 = this.a(var1);
      if (var3) {
         this.a();
      }

      return var3;
   }

   public void a() {
      this.startTime = System.currentTimeMillis();
   }

   public int b() {
      return Math.toIntExact(System.currentTimeMillis() - this.startTime);
   }

   public NewStopWatch a(long var1) {
      this.startTime = System.currentTimeMillis() - var1;
      return this;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void setStartTime(long var1) {
      this.startTime = var1;
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (var1 instanceof NewStopWatch var2) {
         return !var2.a(this) ? false : this.getStartTime() == var2.getStartTime();
      } else {
         return false;
      }
   }

   protected boolean a(Object var1) {
      return var1 instanceof NewStopWatch;
   }

   @Override
   public int hashCode() {
      byte var1 = 59;
      byte var2 = 1;
      long var3 = this.getStartTime();
      return var2 * 59 + (int)(var3 >>> 32 ^ var3);
   }

   @Override
   public String toString() {
      return "NewStopWatch(startTime=" + this.getStartTime() + ")";
   }
}
