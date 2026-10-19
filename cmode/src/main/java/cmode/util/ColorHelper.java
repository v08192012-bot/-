package cmode.util;

import java.awt.Color;

public class ColorHelper {
   public static int[] a(Color... var0) {
      int[] var1 = new int[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = var0[var2].getRGB();
      }

      return var1;
   }

   public static int a(Color var0) {
      return var0.getRGB();
   }

   public static int a(int var0, int var1, float var2) {
      int var3 = var0 >> 24 & 0xFF;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >> 24 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      return (int)(var3 + (var7 - var3) * var2) << 24
         | (int)(var4 + (var8 - var4) * var2) << 16
         | (int)(var5 + (var9 - var5) * var2) << 8
         | (int)(var6 + (var10 - var6) * var2);
   }
}
