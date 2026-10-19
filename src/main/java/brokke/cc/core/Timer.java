package brokke.cc.core;

import net.minecraft.client.MinecraftClient;

public class Timer {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private long startTime = System.currentTimeMillis();
   private long millis;

   public Timer() {
      this.b();
   }

   public static Timer a() {
      return new Timer();
   }

   public boolean a(long var1) {
      return System.currentTimeMillis() - var1 >= this.millis;
   }

   public void b() {
      this.millis = System.currentTimeMillis();
   }

   public long c() {
      return System.currentTimeMillis() - this.millis;
   }

   public double d() {
      return mc.getCurrentFps() > 0 ? 1.0 / mc.getCurrentFps() : 1.0;
   }

   public boolean b(long var1) {
      boolean var3 = this.d(System.nanoTime() - this.millis) >= var1;
      if (var3) {
         this.b();
      }

      return var3;
   }

   public boolean c(long var1) {
      return System.currentTimeMillis() - this.startTime > var1;
   }

   public long d(long var1) {
      return var1 / 1000000L;
   }

   public long e() {
      return System.currentTimeMillis() - this.startTime;
   }

   public void setStartTime(long var1) {
      this.startTime = var1;
   }

   public long getMillis() {
      return this.millis;
   }

   public void setMillis(long var1) {
      this.millis = var1;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void g(long var1) {
      this.startTime = var1;
   }
}
