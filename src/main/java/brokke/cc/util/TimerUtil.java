package brokke.cc.util;

public class TimerUtil {
   private long lastMs = System.currentTimeMillis();

   public boolean a(long var1) {
      return System.currentTimeMillis() - this.lastMs >= var1;
   }

   public void a() {
      this.lastMs = System.currentTimeMillis();
   }

   public long b() {
      return System.currentTimeMillis() - this.lastMs;
   }
}
