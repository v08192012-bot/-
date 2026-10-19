package brokke.cc.render;

import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfFont;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;

public class TextScissor {
   private static final Map<String, Float> scrollOffsets = new HashMap<>();
   private static final Map<String, Long> lastUpdateTimes = new HashMap<>();

   public static void a(
      DrawContext var0,
      MsdfFont var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      int var8,
      int var9,
      boolean var10,
      String var11,
      float var12,
      float var13,
      float var14
   ) {
      if (var1 != null && var2 != null && !var2.isEmpty()) {
         float var15 = Msdf.a(var1, var2, var5);
         if (var15 <= var6) {
            Msdf.a(var0, var1, var2, var3, var4, var5, var8, var10);
         } else {
            long var16 = System.currentTimeMillis();
            long var18 = lastUpdateTimes.getOrDefault(var11, var16);
            float var20 = Math.min((float)(var16 - var18) / 1000.0F, 0.05F);
            lastUpdateTimes.put(var11, var16);
            float var21 = scrollOffsets.getOrDefault(var11, 0.0F);
            boolean var22 = var12 >= var3 && var12 <= var3 + var6 && var13 >= var4 - 2.0F && var13 <= var4 + var14 + 2.0F;
            float var23 = var15 - var6;
            float var24 = 0.0F;
            if (var22) {
               var24 = -(var23 + 4.0F);
            }

            float var25 = Math.max(0.8F, 3.0F - var23 / 60.0F);
            float var26 = Math.max(1.5F, 5.0F - var23 / 80.0F);
            float var27 = var22 ? var25 : var26;
            float var38 = var21 + (var24 - var21) * Math.min(var20 * var27, 1.0F);
            if (Math.abs(var38 - var24) < 0.05F) {
               var38 = var24;
            }

            scrollOffsets.put(var11, var38);
            float var28 = var3 + var38;
            Scissor.a(var3, var4 - 2.0F, var6, var5 + 6.0F, var10);
            Msdf.a(var0, var1, var2, var28, var4, var5, var8, var10);
            Scissor.b(var10);
            int var29 = var9 >> 16 & 0xFF;
            int var30 = var9 >> 8 & 0xFF;
            int var31 = var9 & 0xFF;
            int var32 = var9 >> 24 & 0xFF;
            Color var33 = new Color(var29, var30, var31, 0);
            Color var34 = new Color(var29, var30, var31, var32);
            float var35 = var3 + var6 - var7;
            float var36 = var4 - 2.0F;
            float var37 = var5 + 6.0F;
            RenderUtil.a(var35, var36, var7, var37, 0.0F, 0.0F, 0.0F, 0.0F, var33, var34, var34, var33);
         }
      }
   }

   public static void a(
      DrawContext var0,
      MsdfFont var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      int var7,
      int var8,
      boolean var9,
      String var10,
      float var11,
      float var12,
      float var13
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, 14.0F, var7, var8, var9, var10, var11, var12, var13);
   }

   public static void a(
      DrawContext var0, MsdfFont var1, String var2, float var3, float var4, float var5, float var6, float var7, int var8, int var9, boolean var10
   ) {
      if (var1 != null && var2 != null && !var2.isEmpty()) {
         float var11 = Msdf.a(var1, var2, var5);
         if (var11 <= var6) {
            Msdf.a(var0, var1, var2, var3, var4, var5, var8, var10);
         } else {
            Scissor.a(var3, var4 - 2.0F, var6, var5 + 6.0F, var10);
            Msdf.a(var0, var1, var2, var3, var4, var5, var8, var10);
            Scissor.b(var10);
            int var12 = var9 >> 16 & 0xFF;
            int var13 = var9 >> 8 & 0xFF;
            int var14 = var9 & 0xFF;
            int var15 = var9 >> 24 & 0xFF;
            Color var16 = new Color(var12, var13, var14, 0);
            Color var17 = new Color(var12, var13, var14, var15);
            float var18 = var3 + var6 - var7;
            float var19 = var4 - 2.0F;
            float var20 = var5 + 6.0F;
            RenderUtil.a(var18, var19, var7, var20, 0.0F, 0.0F, 0.0F, 0.0F, var16, var17, var17, var16);
         }
      }
   }

   public static void a(String var0) {
      scrollOffsets.remove(var0);
      lastUpdateTimes.remove(var0);
   }
}
