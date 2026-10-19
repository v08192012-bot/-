package brokke.cc.util;

import brokke.cc.core.IMinecraft;

public final class SensUtility implements IMinecraft {
   public static float a(float var0) {
      float var1 = a();
      if (Float.isFinite(var1) && !(var1 <= 0.0F)) {
         float var2 = Math.round(var0 / var1) * var1;
         return var2 == 0.0F && var0 != 0.0F ? Math.copySign(var1, var0) : var2;
      } else {
         return var0;
      }
   }

   public static float a() {
      return (float)(b() * 0.15);
   }

   public static float b() {
      float var0;
      return mc != null && mc.options != null && mc.options.getMouseSensitivity() != null
         ? (var0 = (float)(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2)) * var0 * var0 * 8.0F
         : 0.0F;
   }

   public static float b(float var0) {
      float var1 = a();
      return Float.isFinite(var1) && !(var1 <= 0.0F) ? Math.round(var0 / var1) : var0;
   }

   private SensUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
