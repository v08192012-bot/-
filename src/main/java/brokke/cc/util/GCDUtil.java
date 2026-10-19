package brokke.cc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

public class GCDUtil {
   public static float a(float var0) {
      return b(var0) * a();
   }

   public static float a() {
      return (float)(b() * 0.15);
   }

   public static float b() {
      double var0 = 0.5;
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2 != null && var2.options != null) {
         var0 = var2.options.getMouseSensitivity().getValue();
      }

      double var3 = var0 * 0.6F + 0.2F;
      return (float)(var3 * var3 * var3 * 8.0);
   }

   public static float b(float var0) {
      return Math.round(var0 / a());
   }

   public static double c() {
      SimpleOption var0 = MinecraftClient.getInstance().options.getMouseSensitivity();
      return (Double)var0.getValue();
   }
}
