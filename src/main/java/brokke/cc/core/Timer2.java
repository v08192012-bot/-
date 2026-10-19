package brokke.cc.core;

public class Timer2 {
   private long millis;

   public Timer2() {
      this.a();
   }

   public boolean a(long var1) {
      return System.currentTimeMillis() - var1 >= this.millis;
   }

   public void a() {
      this.millis = System.currentTimeMillis();
   }

   public long b() {
      return System.currentTimeMillis() - this.millis;
   }

   public long getMillis() {
      return this.millis;
   }

   public void setMillis(long var1) {
      this.millis = var1;
   }
}
