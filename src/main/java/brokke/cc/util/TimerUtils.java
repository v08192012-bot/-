package brokke.cc.util;

public class TimerUtils {
   private long millis;

   public TimerUtils() {
      this.a();
   }

   public boolean a(float var1) {
      return (float)System.currentTimeMillis() - var1 >= (float)this.millis;
   }

   public boolean a(long var1) {
      return System.currentTimeMillis() - this.millis >= var1;
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
