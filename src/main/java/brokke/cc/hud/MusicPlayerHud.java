package brokke.cc.hud;

import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import com.google.gson.JsonObject;

public class MusicPlayerHud {
   private static final MusicPlayerHud INSTANCE = new MusicPlayerHud();
   private static boolean enabled;
   private float x = 7.0F;
   private float y = 46.0F;
   private float targetX = 7.0F;
   private float targetY = 46.0F;
   private boolean initPos;

   public void a() {
   }

   public static void b() {
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void a(boolean var0) {
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
   }

   public static void b(JsonObject var0) {
   }

   public static float[] d() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.targetX = var0;
      INSTANCE.targetY = var1;
      INSTANCE.initPos = true;
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
   }
}
