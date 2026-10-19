package brokke.cc.manager;

public class TimerController {
   private static float timerSpeed = 1.0F;

   public static void a(float var0) {
      timerSpeed = Math.max(0.01F, var0);
   }

   public static float getTimerSpeed() {
      return timerSpeed;
   }

   public static void b() {
      timerSpeed = 1.0F;
   }
}
