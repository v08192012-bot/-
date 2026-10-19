package brokke.cc.util;

import net.minecraft.client.MinecraftClient;

public class GCDUtil2 {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static float a(float var0) {
      return b(var0) * a();
   }

   public static float a() {
      return (float)(b() * 0.15);
   }

   public static float b() {
      double var0 = 0.5;
      if (mc != null && mc.options != null) {
         var0 = mc.options.getMouseSensitivity().getValue();
      }

      double var2 = var0 * 0.6F + 0.2F;
      return (float)(var2 * var2 * var2 * 8.0);
   }

   public static float b(float var0) {
      return Math.round(var0 / a());
   }
}
