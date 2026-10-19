package brokke.cc.cosmetic;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;

public class CosmeticsPanel {
   public static final float W = 380.0F;
   public static final float H = 210.0F;
   private static final float HEADER_H = 19.0F;
   private static final float TABS_H = 14.0F;
   private static final float CARD_W = 85.0F;
   private static final float CARD_H = 46.0F;
   private static final float CARD_GAP = 5.0F;
   private static final int COLUMNS = 4;
   private static final String[] TAB_LABELS = new String[]{"Все", "Крылья", "Тело", "Питомцы", "Шляпы"};
   private static final String[] TAB_TYPES = new String[]{"", "wings", "bodywear", "pet", "hat"};
   public final Animation openAnim = new Animation();
   private final Animation scrollAnim = new Animation();
   private float scrollTarget = 0.0F;
   private float maxScroll = 0.0F;
   private int currentTab = 0;
   private final List<float[]> clickBoxes = new ArrayList<>();
   private final List<Runnable> clickActions = new ArrayList<>();
   private final float[] closeBtnBox = new float[4];

   public CosmeticsPanel() {
      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.scrollAnim.setEasing(Easing.SMOOTH_STEP);
   }

   public void a(DrawContext var1, float var2, float var3, int var4, int var5, float var6, boolean var7) {
      this.openAnim.animate(var7 ? 1.0F : 0.0F, var6, 3.8F);
      float var8 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (!(var8 <= 0.005F)) {
         this.clickBoxes.clear();
         this.clickActions.clear();
         float var9 = var2;
         float var10 = var3;
         HudConfig.a("cosmetics_modal", var9, var10, 380.0F, 210.0F, 7.0F, new Color(0, 0, 0, (int)(245.0F * var8)));
         RenderUtil.a(var9, var10, 380.0F, 210.0F, 7.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var8))});
         float var11 = var9 + 8.0F;
         float var12 = var10 + 5.5F;
         RenderUtil.a(var1, MsdfManager.BOLD, var11, var12, "Косметика", 7.0F, new Color(245, 245, 250, (int)(255.0F * var8)));
         Color var13 = Theme.getInstance().getAccent();
         Color var14 = new Color(var13.getRed(), var13.getGreen(), var13.getBlue(), (int)(255.0F * var8));
         int var15 = LocalCosmetics.e().size();
         String var16 = "Надето: " + var15 + "/4";
         float var17 = Msdf.a(MsdfManager.BOLD, var16, 5.5F);
         RenderUtil.a(var1, MsdfManager.BOLD, var9 + 380.0F - 32.0F - var17, var12 + 1.0F, var16, 5.5F, var14);
         float var18 = var9 + 380.0F - 10.0F;
         float var19 = var10 + 8.5F;
         this.closeBtnBox[0] = var18 - 5.0F;
         this.closeBtnBox[1] = var19 - 5.0F;
         this.closeBtnBox[2] = 10.0F;
         this.closeBtnBox[3] = 10.0F;
         boolean var20 = var4 >= this.closeBtnBox[0]
            && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var5 >= this.closeBtnBox[1]
            && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3];
         BufferBuilder var21 = ColorPipeline.a();
         if (var21 != null) {
            Color var22 = var20 ? var14 : new Color(170, 175, 185);
            int var23 = (int)(255.0F * var8) << 24 | var22.getRGB() & 16777215;
            ColorPipeline.a(var21, var18 - 2.5F, var19 - 2.5F, var18 + 2.5F, var19 + 2.5F, 1.2F, var23);
            ColorPipeline.a(var21, var18 + 2.5F, var19 - 2.5F, var18 - 2.5F, var19 + 2.5F, 1.2F, var23);
            ColorPipeline.a(var21, false);
         }

         float var40 = var10 + 19.0F;
         float var41 = 3.5F;
         float var24 = 0.0F;

         for (int var25 = 0; var25 < TAB_LABELS.length; var25++) {
            var24 += Msdf.a(MsdfManager.BOLD, TAB_LABELS[var25], 5.25F) + 8.0F;
            if (var25 < TAB_LABELS.length - 1) {
               var24 += var41;
            }
         }

         float var42 = var9 + (380.0F - var24) / 2.0F;

         for (int var26 = 0; var26 < TAB_LABELS.length; var26++) {
            String var27 = TAB_LABELS[var26];
            float var28 = Msdf.a(MsdfManager.BOLD, var27, 5.25F) + 8.0F;
            float var29 = 12.0F;
            boolean var30 = var26 == this.currentTab;
            boolean var31 = var4 >= var42 && var4 <= var42 + var28 && var5 >= var40 && var5 <= var40 + var29;
            Color var32 = var30 ? var14 : (var31 ? new Color(255, 255, 255, (int)(35.0F * var8)) : new Color(255, 255, 255, (int)(12.0F * var8)));
            Color var33 = var30
               ? new Color(0, 0, 0, (int)(255.0F * var8))
               : (var31 ? new Color(255, 255, 255, (int)(255.0F * var8)) : new Color(180, 185, 195, (int)(255.0F * var8)));
            RenderUtil.a(var42, var40, var28, var29, 3.0F, new Color[]{var32});
            float var34 = var42 + (var28 - Msdf.a(MsdfManager.BOLD, var27, 5.25F)) / 2.0F;
            float var35 = var40 + (var29 - Msdf.a(MsdfManager.BOLD, 5.25F)) / 2.0F - 0.5F;
            RenderUtil.a(var1, MsdfManager.BOLD, var34, var35, var27, 5.25F, var33);
            int var36 = var26;
            this.clickBoxes.add(new float[]{var42, var40, var28, var29});
            this.clickActions.add(() -> {
               if (this.currentTab != var36) {
                  this.currentTab = var36;
                  this.scrollTarget = 0.0F;
               }
            });
            var42 += var28 + var41;
         }

         float var43 = var40 + 14.0F + 5.0F;
         float var44 = 210.0F - (var43 - var10) - 6.0F;
         float var45 = 364.0F;
         float var46 = var9 + 12.5F;
         List var47 = this.a();
         int var48 = Math.max(1, (var47.size() + 4 - 1) / 4);
         float var49 = var48 * 46.0F + (var48 - 1) * 5.0F;
         this.maxScroll = Math.max(0.0F, var49 - var44);
         if (this.scrollTarget < -this.maxScroll) {
            this.scrollTarget = -this.maxScroll;
         }

         if (this.scrollTarget > 0.0F) {
            this.scrollTarget = 0.0F;
         }

         this.scrollAnim.animate(this.scrollTarget, var6, 8.0F);
         float var50 = this.scrollAnim.getValue();
         Scissor.b(var9 + 6.0F, var43, var45 + 4.0F, var44);

         for (int var51 = 0; var51 < var47.size(); var51++) {
            int var52 = (Integer)var47.get(var51);
            int var53 = var51 % 4;
            int var37 = var51 / 4;
            float var38 = var46 + var53 * 90.0F;
            float var39 = var43 + var50 + var37 * 51.0F;
            if (var39 + 46.0F >= var43 && var39 <= var43 + var44) {
               this.a(var1, var52, var38, var39, 85.0F, 46.0F, var4, var5, var8);
            }
         }

         Scissor.c();
      }
   }

   private void a(DrawContext var1, int var2, float var3, float var4, float var5, float var6, int var7, int var8, float var9) {
      boolean var10 = LocalCosmetics.e(var2);
      boolean var11 = var7 >= var3 && var7 <= var3 + var5 && var8 >= var4 && var8 <= var4 + var6;
      Color var12 = Theme.getInstance().getAccent();
      Color var13 = new Color(var12.getRed(), var12.getGreen(), var12.getBlue(), (int)(255.0F * var9));
      Color var14 = var10
         ? new Color(var12.getRed(), var12.getGreen(), var12.getBlue(), (int)(35.0F * var9))
         : (var11 ? new Color(255, 255, 255, (int)(18.0F * var9)) : new Color(255, 255, 255, (int)(8.0F * var9)));
      Color var15 = var10 ? var13 : (var11 ? new Color(255, 255, 255, (int)(50.0F * var9)) : new Color(255, 255, 255, (int)(20.0F * var9)));
      RenderUtil.a(var3, var4, var5, var6, 4.0F, new Color[]{var14});
      RenderUtil.a(var3, var4, var5, var6, 4.0F, 0.5F, new Color[]{var15});
      float var16 = var3 + 5.0F;
      float var17 = var4 + 5.0F;
      float var18 = 20.0F;
      Color var19 = var10 ? var13 : new Color(255, 255, 255, (int)(22.0F * var9));
      RenderUtil.a(var16, var17, var18, var18, 3.0F, new Color[]{new Color(0, 0, 0, (int)(65.0F * var9))});
      RenderUtil.a(var16, var17, var18, var18, 3.0F, 0.5F, new Color[]{var19});
      CosmeticPreviewRenderer.a(var2, var16, var17, var18, var18, var9, var11);
      String var20 = LocalCosmetics.a(var2);
      if (var20.length() > 10) {
         var20 = var20.substring(0, 9) + "…";
      }

      RenderUtil.a(var1, MsdfManager.BOLD, var3 + 28.0F, var4 + 5.5F, var20, 4.85F, new Color(245, 245, 250, (int)(255.0F * var9)));
      String var21 = this.a(LocalCosmetics.b(var2));
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + 28.0F, var4 + 15.5F, var21, 4.25F, new Color(150, 155, 165, (int)(230.0F * var9)));
      float var22 = var3 + 5.0F;
      float var23 = var4 + 29.0F;
      float var24 = var5 - 10.0F;
      float var25 = 12.0F;
      boolean var26 = var7 >= var22 && var7 <= var22 + var24 && var8 >= var23 && var8 <= var23 + var25;
      String var27 = var10 ? "НАДЕТО" : "НАДЕТЬ";
      Color var28 = var10 ? var13 : (var26 ? new Color(255, 255, 255, (int)(40.0F * var9)) : new Color(255, 255, 255, (int)(16.0F * var9)));
      Color var29 = var10
         ? new Color(0, 0, 0, (int)(255.0F * var9))
         : (var26 ? new Color(255, 255, 255, (int)(255.0F * var9)) : new Color(220, 220, 230, (int)(255.0F * var9)));
      RenderUtil.a(var22, var23, var24, var25, 2.5F, new Color[]{var28});
      float var30 = Msdf.a(MsdfManager.BOLD, var27, 4.5F);
      float var31 = Msdf.a(MsdfManager.BOLD, 4.5F);
      RenderUtil.a(var1, MsdfManager.BOLD, var22 + (var24 - var30) / 2.0F, var23 + (var25 - var31) / 2.0F - 0.5F, var27, 4.5F, var29);
      this.clickBoxes.add(new float[]{var3, var4, var5, var6});
      this.clickActions.add(() -> LocalCosmetics.f(var2));
   }

   private List<Integer> a() {
      String var1 = TAB_TYPES[this.currentTab];
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < LocalCosmetics.a(); var3++) {
         if (var1.isEmpty() || var1.equals(LocalCosmetics.b(var3))) {
            var2.add(var3);
         }
      }

      return var2;
   }

   private String a(String var1) {
      return switch (var1) {
         case "cape" -> "Плащ";
         case "wings" -> "Крылья";
         case "pet" -> "Питомец";
         case "hat" -> "Шляпа";
         case "bodywear" -> "Тело";
         default -> var1;
      };
   }

   public boolean a(float var1, float var2, int var3, boolean var4, Runnable var5) {
      if (var4 && !(this.openAnim.getValue() < 0.5F)) {
         if (var3 == 0) {
            if (var1 >= this.closeBtnBox[0]
               && var1 <= this.closeBtnBox[0] + this.closeBtnBox[2]
               && var2 >= this.closeBtnBox[1]
               && var2 <= this.closeBtnBox[1] + this.closeBtnBox[3]) {
               if (var5 != null) {
                  var5.run();
               }

               return true;
            }

            for (int var6 = 0; var6 < this.clickBoxes.size(); var6++) {
               float[] var7 = this.clickBoxes.get(var6);
               if (var1 >= var7[0] && var1 <= var7[0] + var7[2] && var2 >= var7[1] && var2 <= var7[1] + var7[3]) {
                  if (var6 < this.clickActions.size()) {
                     this.clickActions.get(var6).run();
                  }

                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean a(double var1) {
      if (this.openAnim.getValue() < 0.2F) {
         return false;
      }

      this.scrollTarget += (float)(var1 * 20.0);
      if (this.scrollTarget < -this.maxScroll) {
         this.scrollTarget = -this.maxScroll;
      }

      if (this.scrollTarget > 0.0F) {
         this.scrollTarget = 0.0F;
      }

      return true;
   }
}
