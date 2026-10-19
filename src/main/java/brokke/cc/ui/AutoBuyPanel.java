package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.AutoBuyConfig;
import brokke.cc.config.HudConfig;
import brokke.cc.core.ItemEntry;
import brokke.cc.core.PurchaseRecord;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.misc.AutoBuy;
import brokke.cc.module.FunctionManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class AutoBuyPanel {
   public static final float W = 390.0F;
   public static final float H = 220.0F;
   private static final float HEADER_H = 19.0F;
   private static final float TABS_H = 14.0F;
   private static final String[] TAB_LABELS = new String[]{"Предметы", "Настройки", "История"};
   public final Animation openAnim = new Animation();
   private final Animation scrollAnim = new Animation();
   private float scrollTarget = 0.0F;
   private float maxScroll = 0.0F;
   private int currentTab = 0;
   private final List<float[]> clickBoxes = new ArrayList<>();
   private final List<Runnable> clickActions = new ArrayList<>();
   private final float[] closeBtnBox = new float[4];
   private String searchQuery = "";
   private boolean searchActive = false;
   private boolean searchSelectAll = false;
   private final StringBuilder searchBuffer = new StringBuilder();
   private ItemEntry editingEntry = null;
   private final StringBuilder editPriceBuffer = new StringBuilder();
   private boolean editingNick = false;
   private final StringBuilder editNickBuffer = new StringBuilder();
   private boolean editingMinPrice = false;
   private final StringBuilder editMinPriceBuffer = new StringBuilder();
   private ItemEntry editingDurEntry = null;
   private final StringBuilder editDurBuffer = new StringBuilder();
   private final Map<ItemEntry, Animation> abAnims = new HashMap<>();
   private final Map<ItemEntry, Animation> parseAnims = new HashMap<>();
   private final Map<ItemEntry, Animation> cardHoverAnims = new HashMap<>();
   private final Animation allSwitchAnim = new Animation();

   private static int a(int var0, float var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0 & 16777215;
   }

   private static int a(int var0, int var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      int var3 = var0 >>> 24 & 0xFF;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >>> 24 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      int var11 = (int)(var4 + (var8 - var4) * var2);
      int var12 = (int)(var5 + (var9 - var5) * var2);
      int var13 = (int)(var6 + (var10 - var6) * var2);
      int var14 = (int)(var3 + (var7 - var3) * var2);
      return var14 << 24 | var11 << 16 | var12 << 8 | var13;
   }

   public AutoBuyPanel() {
      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.scrollAnim.setEasing(Easing.SMOOTH_STEP);
      this.allSwitchAnim.setEasing(Easing.SPRING_SOFT);
   }

   public void a(DrawContext var1, float var2, float var3, int var4, int var5, float var6, boolean var7) {
      this.openAnim.animate(var7 ? 1.0F : 0.0F, var6, 3.8F);
      float var8 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (!(var8 <= 0.005F)) {
         this.clickBoxes.clear();
         this.clickActions.clear();
         float var9 = var2;
         float var10 = var3;
         HudConfig.a("autobuy_modal", var9, var10, 390.0F, 220.0F, 7.0F, new Color(0, 0, 0, (int)(245.0F * var8)));
         RenderUtil.a(var9, var10, 390.0F, 220.0F, 7.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var8))});
         float var11 = var9 + 8.0F;
         float var12 = var10 + 5.5F;
         RenderUtil.a(var1, MsdfManager.BOLD, var11 + 4.0F, var12 + 4.0F, "AutoBuy", 7.0F, new Color(245, 245, 250, (int)(255.0F * var8)));
         float var13 = var9 + 390.0F - 14.0F;
         float var14 = var10 + 12.5F;
         this.closeBtnBox[0] = var13 - 5.0F;
         this.closeBtnBox[1] = var14 - 5.0F;
         this.closeBtnBox[2] = 10.0F;
         this.closeBtnBox[3] = 10.0F;
         boolean var15 = var4 >= this.closeBtnBox[0]
            && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var5 >= this.closeBtnBox[1]
            && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3];
         AutoBuyConfig var16 = AutoBuyConfig.getInstance();
         long var17 = var16.entries.stream().filter(var0 -> var0.enabled).count();
         String var19 = "Активно: " + var17 + "/" + var16.entries.size();
         float var20 = Msdf.a(MsdfManager.BOLD, var19, 5.5F);
         float var21 = var13 - 2.5F - 8.0F - var20;
         float var22 = var14 - Msdf.a(MsdfManager.BOLD, 5.5F) / 2.0F;
         RenderUtil.a(var1, MsdfManager.BOLD, var21, var22, var19, 5.5F, Theme.getInstance().getAccent());
         BufferBuilder var23 = ColorPipeline.a();
         if (var23 != null) {
            Color var24 = var15 ? Theme.getInstance().getAccent() : new Color(170, 175, 185);
            int var25 = (int)(255.0F * var8) << 24 | var24.getRGB() & 16777215;
            ColorPipeline.a(var23, var13 - 2.5F, var14 - 2.5F, var13 + 2.5F, var14 + 2.5F, 1.2F, var25);
            ColorPipeline.a(var23, var13 + 2.5F, var14 - 2.5F, var13 - 2.5F, var14 + 2.5F, 1.2F, var25);
            ColorPipeline.a(var23, false);
         }

         float var39 = var10 + 19.0F;
         float var40 = 3.5F;
         float var26 = 0.0F;

         for (int var27 = 0; var27 < TAB_LABELS.length; var27++) {
            var26 += Msdf.a(MsdfManager.BOLD, TAB_LABELS[var27], 5.25F) + 12.0F;
            if (var27 < TAB_LABELS.length - 1) {
               var26 += var40;
            }
         }

         float var41 = var9 + (390.0F - var26) / 2.0F;

         for (int var28 = 0; var28 < TAB_LABELS.length; var28++) {
            String var29 = TAB_LABELS[var28];
            float var30 = Msdf.a(MsdfManager.BOLD, var29, 5.25F) + 12.0F;
            float var31 = 12.0F;
            boolean var32 = var28 == this.currentTab;
            boolean var33 = var4 >= var41 && var4 <= var41 + var30 && var5 >= var39 && var5 <= var39 + var31;
            Color var34 = var32
               ? Theme.getInstance().getAccent()
               : (var33 ? new Color(255, 255, 255, (int)(35.0F * var8)) : new Color(255, 255, 255, (int)(12.0F * var8)));
            Color var35 = var32 ? Color.BLACK : (var33 ? Color.WHITE : new Color(180, 185, 195, (int)(255.0F * var8)));
            RenderUtil.a(var41, var39, var30, var31, 3.0F, new Color[]{var34});
            float var36 = var41 + (var30 - Msdf.a(MsdfManager.BOLD, var29, 5.25F)) / 2.0F;
            float var37 = var39 + (var31 - Msdf.a(MsdfManager.BOLD, 5.25F)) / 2.0F - 0.5F;
            RenderUtil.a(var1, MsdfManager.BOLD, var36, var37, var29, 5.25F, var35);
            int var38 = var28;
            this.clickBoxes.add(new float[]{var41, var39, var30, var31});
            this.clickActions.add(() -> {
               if (this.currentTab != var38) {
                  this.a();
                  this.currentTab = var38;
                  this.scrollTarget = 0.0F;
                  this.searchSelectAll = false;
               }
            });
            var41 += var30 + var40;
         }

         float var42 = var39 + 14.0F + 5.0F;
         float var43 = 220.0F - (var42 - var10) - 6.0F;
         float var44 = 374.0F;
         this.scrollAnim.animate(this.scrollTarget, var6, 8.0F);
         float var45 = this.scrollAnim.getValue();
         MinecraftClient var46 = MinecraftClient.getInstance();
         float var47 = 2.0F / var46.getWindow().getScaleFactor();
         int var48 = Math.max(0, (int)Math.floor((var9 + 6.0F) * var47));
         int var49 = Math.max(0, (int)Math.floor(var42 * var47));
         int var50 = Math.max(var48, (int)Math.ceil((var9 + 6.0F + var44 + 4.0F) * var47));
         int var51 = Math.max(var49, (int)Math.ceil((var42 + var43) * var47));
         Scissor.b(var9 + 6.0F, var42, var44 + 4.0F, var43);
         var1.enableScissor(var48, var49, var50, var51);
         float var52 = 0.0F;
         if (this.currentTab == 0) {
            var52 = this.a(var1, var9 + 8.0F, var42 + var45, var44, var4, var5, var8, var6, var42, var43);
         } else if (this.currentTab == 1) {
            var52 = this.a(var1, var9 + 8.0F, var42 + var45, var44, var4, var5, var8, var42, var43);
         } else if (this.currentTab == 2) {
            var52 = this.b(var1, var9 + 8.0F, var42 + var45, var44, var4, var5, var8, var42, var43);
         }

         var1.disableScissor();
         Scissor.c();
         this.maxScroll = Math.max(0.0F, var52 - var43);
         if (this.scrollTarget < -this.maxScroll) {
            this.scrollTarget = -this.maxScroll;
         }

         if (this.scrollTarget > 0.0F) {
            this.scrollTarget = 0.0F;
         }
      }
   }

   private float a(DrawContext var1, float var2, float var3, float var4, int var5, int var6, float var7, float var8, float var9, float var10) {
      float var11 = var3;
      float var12 = var3;
      List<ItemEntry> var13 = AutoBuyConfig.getInstance().entries;
      boolean var14 = !var13.isEmpty() && var13.stream().allMatch(var0 -> var0.enabled);
      this.allSwitchAnim.animate(var14 ? 1.0F : 0.0F, var8, 7.0F);
      float var15 = Math.clamp(this.allSwitchAnim.getValue(), 0.0F, 1.0F);
      int var16 = Theme.getInstance().getAccent().getRGB();
      float var17 = 80.0F;
      float var18 = 13.0F;
      float var19 = var2 + var4 - var17;
      float var20 = 13.0F;
      float var21 = var4 - var17 - 3.5F;
      boolean var22 = var5 >= var2 && var5 <= var2 + var21 && var6 >= var12 && var6 <= var12 + var20;
      RenderUtil.a(var2, var12, var21, var20, 3.0F, new Color[]{new Color(255, 255, 255, (int)(12.0F * var7))});
      RenderUtil.a(
         var2,
         var12,
         var21,
         var20,
         3.0F,
         0.5F,
         new Color[]{
            this.searchActive
               ? Theme.getInstance().getAccent()
               : (var22 ? new Color(255, 255, 255, (int)(40.0F * var7)) : new Color(255, 255, 255, (int)(18.0F * var7)))
         }
      );
      if (this.searchActive && this.searchSelectAll && !this.searchBuffer.isEmpty()) {
         float var23 = Msdf.a(MsdfManager.BOLD, this.searchBuffer.toString(), 5.0F);
         Color var24 = Theme.getInstance().getAccent();
         RenderUtil.a(
            var2 + 4.5F,
            var12 + 2.0F,
            var23 + 2.0F,
            var20 - 4.0F,
            2.0F,
            new Color[]{new Color(var24.getRed(), var24.getGreen(), var24.getBlue(), (int)(90.0F * var7))}
         );
      }

      String var59 = this.searchBuffer.isEmpty() ? (this.searchActive ? "_" : "Поиск предметов...") : this.searchBuffer + (this.searchActive ? "_" : "");
      Color var60 = this.searchBuffer.isEmpty() && !this.searchActive
         ? new Color(130, 135, 145, (int)(200.0F * var7))
         : new Color(240, 240, 245, (int)(255.0F * var7));
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 5.0F, var12 + 3.5F, var59, 5.0F, var60);
      float var25 = var2;
      float var26 = var12;
      this.clickBoxes.add(new float[]{var25, var26, var21, var20});
      this.clickActions.add(() -> {
         this.searchActive = true;
         this.searchSelectAll = false;
         this.editingEntry = null;
         this.editingNick = false;
         this.editingMinPrice = false;
         this.editingDurEntry = null;
      });
      boolean var27 = var5 >= var19 && var5 <= var19 + var17 && var6 >= var12 && var6 <= var12 + var18;
      RenderUtil.a(var19, var12, var17, var18, 3.0F, new Color[]{new Color(255, 255, 255, (int)((var27 ? 18.0F : 12.0F) * var7))});
      int var28 = a(a(-1, (var27 ? 35.0F : 18.0F) * var7), var16, var15);
      RenderUtil.a(var19, var12, var17, var18, 3.0F, 0.5F, var28);
      String var29 = var14 ? "Выключить всё" : "Включить всё";
      float var30 = Msdf.a(MsdfManager.BOLD, var29, 4.3F);
      float var31 = 14.0F;
      float var32 = 8.0F;
      float var33 = var30 + 3.0F + var31;
      float var34 = var19 + (var17 - var33) / 2.0F;
      int var35 = a(-6249038, -1, var15);
      RenderUtil.a(
         var1, MsdfManager.BOLD, var34, var12 + 4.0F, var29, 4.3F, new Color(var35 >> 16 & 0xFF, var35 >> 8 & 0xFF, var35 & 0xFF, (int)(255.0F * var7))
      );
      float var36 = var34 + var30 + 3.0F;
      float var37 = var12 + (var18 - var32) / 2.0F;
      int var38 = a(a(-1, 14.0F * var7), var16, var15);
      RenderUtil.a(var36, var37, var31, var32, 4.0F, a(var38, (16.0F + 210.0F * var15) * var7));
      int var39 = a(a(-1, 28.0F * var7), var16, var15);
      RenderUtil.a(var36, var37, var31, var32, 4.0F, 0.5F, a(var39, (28.0F + 200.0F * var15) * var7));
      float var40 = 5.5F;
      float var41 = var36 + 1.25F + (var31 - 2.5F - var40) * var15;
      float var42 = var37 + (var32 - var40) / 2.0F;
      int var43 = a(-7827554, -1, var15);
      RenderUtil.a(var41, var42, var40, var40, 2.75F, a(var43, 255.0F * var7));
      float var44 = var19;
      float var45 = var12;
      this.clickBoxes.add(new float[]{var44, var45, var17, var18});
      this.clickActions.add(() -> {
         boolean var1x = var13.stream().anyMatch(var0x -> !var0x.enabled);

         for (ItemEntry var3x : var13) {
            var3x.enabled = var1x;
         }

         AutoBuyConfig.g();
      });
      float var57 = var12 + (var20 + 4.5F);
      ArrayList var46 = new ArrayList();

      for (ItemEntry var48 : var13) {
         String var49 = var48.displayName != null && !var48.displayName.isBlank() ? var48.displayName : var48.itemId;
         if (!this.searchQuery.isEmpty()) {
            String var50 = (var49 + " " + (var48.itemId != null ? var48.itemId : "") + " " + (var48.loreKeyword != null ? var48.loreKeyword : ""))
               .toLowerCase(Locale.ROOT);
            if (!var50.contains(this.searchQuery)) {
               continue;
            }
         }

         var46.add(var48);
      }

      if (var46.isEmpty()) {
         String var62 = var13.isEmpty() ? "Список предметов пуст" : "Ничего не найдено";
         float var64 = Msdf.a(MsdfManager.BOLD, var62, 5.5F);
         RenderUtil.a(var1, MsdfManager.BOLD, var2 + (var4 - var64) / 2.0F, var57 + 25.0F, var62, 5.5F, new Color(130, 135, 145, (int)(180.0F * var7)));
         return var57 + 50.0F - var11;
      }

      float var61 = 4.0F;
      float var63 = (var4 - var61) / 2.0F;
      float var65 = 34.0F;
      float var66 = 3.5F;

      for (int var51 = 0; var51 < var46.size(); var51++) {
         ItemEntry var52 = (ItemEntry)var46.get(var51);
         int var53 = var51 % 2;
         int var54 = var51 / 2;
         float var55 = var2 + var53 * (var63 + var61);
         float var56 = var57 + var54 * (var65 + var66);
         if (var56 + var65 >= var9 && var56 <= var9 + var10) {
            this.a(var1, var52, var55, var56, var63, var65, var5, var6, var7, var8, var9, var10);
         }
      }

      int var67 = (var46.size() + 1) / 2;
      var12 = var57 + var67 * (var65 + var66);
      return var12 - var11;
   }

   private void a(
      DrawContext var1, ItemEntry var2, float var3, float var4, float var5, float var6, int var7, int var8, float var9, float var10, float var11, float var12
   ) {
      int var13 = Theme.getInstance().getAccent().getRGB();
      Animation var14 = this.abAnims.computeIfAbsent(var2, var1x -> {
         Animation var2x = new Animation();
         var2x.setEasing(Easing.SPRING_SOFT);
         var2x.reset(var2.enabled ? 1.0F : 0.0F);
         return var2x;
      });
      var14.animate(var2.enabled ? 1.0F : 0.0F, var10, 7.0F);
      float var15 = Math.clamp(var14.getValue(), 0.0F, 1.0F);
      Animation var16 = this.parseAnims.computeIfAbsent(var2, var1x -> {
         Animation var2x = new Animation();
         var2x.setEasing(Easing.SPRING_SOFT);
         var2x.reset(var2.parseEnabled ? 1.0F : 0.0F);
         return var2x;
      });
      var16.animate(var2.parseEnabled ? 1.0F : 0.0F, var10, 7.0F);
      float var17 = Math.clamp(var16.getValue(), 0.0F, 1.0F);
      boolean var18 = var7 >= var3 && var7 <= var3 + var5 && var8 >= var4 && var8 <= var4 + var6;
      Animation var19 = this.cardHoverAnims.computeIfAbsent(var2, var0 -> {
         Animation var1x = new Animation();
         var1x.setEasing(Easing.SPRING_SOFT);
         return var1x;
      });
      var19.animate(var18 ? 1.0F : 0.0F, var10, 7.0F);
      float var20 = Math.clamp(var19.getValue(), 0.0F, 1.0F);
      RenderUtil.a(var3, var4, var5, var6, 3.5F, new Color[]{new Color(255, 255, 255, (int)((var18 ? 14.0F : 8.0F) * var9))});
      int var21 = a(-1, (var18 ? 32.0F : 16.0F) * var9);
      int var22 = a(var13, (160.0F + 60.0F * var20) * var9);
      RenderUtil.a(var3, var4, var5, var6, 3.5F, 0.5F, a(var21, var22, var15));
      float var23 = 18.0F;
      float var24 = var3 + 4.0F;
      float var25 = var4 + (var6 - var23) / 2.0F;
      RenderUtil.a(var24, var25, var23, var23, 3.0F, new Color[]{new Color(255, 255, 255, (int)(6.0F * var9))});
      RenderUtil.a(var24, var25, var23, var23, 3.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)((var18 ? 26.0F : 14.0F) * var9))});
      ItemStack var26 = a(var2);
      if (!var26.isEmpty() && var25 + var23 > var11 && var25 < var11 + var12) {
         this.a(var1, var26, var24 + 1.0F, var25 + 1.0F, 16.0F);
      }

      float var27 = var3 + 25.0F;
      float var28 = Msdf.a(MsdfManager.BOLD, "AB", 4.5F);
      float var29 = 16.0F;
      float var30 = 9.0F;
      float var31 = var28 + 2.5F + var29;
      float var32 = var3 + var5 - 4.5F - var31;
      float var33 = var4 + 3.5F;
      float var34 = Msdf.a(MsdfManager.BOLD, "Парс", 4.5F);
      float var35 = 9.5F;
      float var36 = var34 + 2.5F + var35;
      float var37 = var32 - 5.0F - var36;
      float var38 = var4 + 3.5F;
      String var39 = var2.displayName != null && !var2.displayName.isBlank() ? var2.displayName : var2.itemId;
      float var40 = var37 - var27 - 4.0F;
      String var41 = var39;
      if (Msdf.a(MsdfManager.BOLD, var41, 5.25F) > var40) {
         while (var41.length() > 3 && Msdf.a(MsdfManager.BOLD, var41 + "...", 5.25F) > var40) {
            var41 = var41.substring(0, var41.length() - 1);
         }

         var41 = var41 + "...";
      }

      RenderUtil.a(var1, MsdfManager.BOLD, var27, var4 + 4.5F, var41, 5.25F, new Color(245, 245, 250, (int)(255.0F * var9)));
      int var42 = a(-6249038, -1, var17);
      RenderUtil.a(
         var1, MsdfManager.BOLD, var37, var38 + 2.0F, "Парс", 4.5F, new Color(var42 >> 16 & 0xFF, var42 >> 8 & 0xFF, var42 & 0xFF, (int)(255.0F * var9))
      );
      float var43 = var37 + var34 + 2.5F;
      float var44 = var38;
      int var45 = a(a(-1, 12.0F * var9), var13, var17);
      RenderUtil.a(var43, var44, var35, var35, 2.5F, a(var45, (14.0F + 210.0F * var17) * var9));
      int var46 = a(a(-1, 25.0F * var9), var13, var17);
      RenderUtil.a(var43, var44, var35, var35, 2.5F, 0.5F, a(var46, (25.0F + 190.0F * var17) * var9));
      if (var17 > 0.01F) {
         float var47 = Msdf.a(MsdfManager.DICONS, "m", 6.5F);
         float var48 = Msdf.a(MsdfManager.DICONS, 6.5F);
         Msdf.a(
            var1, MsdfManager.DICONS, "m", var43 + (var35 - var47) / 2.0F, var44 + (var35 - var48) / 2.0F - 0.25F, 6.5F, a(-1, var17 * 255.0F * var9), false
         );
      }

      this.clickBoxes.add(new float[]{var37 - 1.0F, var38 - 1.0F, var36 + 2.0F, var35 + 2.0F});
      this.clickActions.add(() -> {
         var2.parseEnabled = !var2.parseEnabled;
         AutoBuyConfig.g();
      });
      int var77 = a(-6249038, -1, var15);
      RenderUtil.a(
         var1, MsdfManager.BOLD, var32, var33 + 2.0F, "AB", 4.5F, new Color(var77 >> 16 & 0xFF, var77 >> 8 & 0xFF, var77 & 0xFF, (int)(255.0F * var9))
      );
      float var78 = var32 + var28 + 2.5F;
      float var49 = var33;
      int var50 = a(a(-1, 14.0F * var9), var13, var15);
      RenderUtil.a(var78, var49, var29, var30, 4.5F, a(var50, (16.0F + 210.0F * var15) * var9));
      int var51 = a(a(-1, 28.0F * var9), var13, var15);
      RenderUtil.a(var78, var49, var29, var30, 4.5F, 0.5F, a(var51, (28.0F + 200.0F * var15) * var9));
      float var52 = 6.0F;
      float var53 = var78 + 1.5F + (var29 - 3.0F - var52) * var15;
      float var54 = var49 + (var30 - var52) / 2.0F;
      int var55 = a(-7827554, -1, var15);
      RenderUtil.a(var53, var54, var52, var52, 3.0F, a(var55, 255.0F * var9));
      this.clickBoxes.add(new float[]{var32 - 1.0F, var33 - 1.0F, var31 + 2.0F, var30 + 2.0F});
      this.clickActions.add(() -> {
         var2.enabled = !var2.enabled;
         AutoBuyConfig.g();
      });
      boolean var56 = this.editingEntry == var2;
      String var57;
      if (var56) {
         var57 = (this.editPriceBuffer.isEmpty() ? "0" : this.editPriceBuffer.toString()) + "_";
      } else if (var2.maxUnitPrice > 0L) {
         var57 = "Лимит: " + a(var2.maxUnitPrice) + "$";
      } else {
         var57 = "+ Лимит";
      }

      float var58 = Msdf.a(MsdfManager.BOLD, var57, 4.5F);
      float var59 = Math.max(34.0F, var58 + 8.0F);
      float var60 = 10.5F;
      float var61 = var3 + var5 - 4.5F - var59;
      float var62 = var4 + 18.5F;
      boolean var63 = var7 >= var61 && var7 <= var61 + var59 && var8 >= var62 && var8 <= var62 + var60;
      if (var56) {
         RenderUtil.a(var61, var62, var59, var60, 2.5F, a(var13, 45.0F * var9));
         RenderUtil.a(var61, var62, var59, var60, 2.5F, 0.5F, a(var13, 220.0F * var9));
      } else {
         RenderUtil.a(var61, var62, var59, var60, 2.5F, new Color[]{new Color(255, 255, 255, (int)((var63 ? 26.0F : 12.0F) * var9))});
         RenderUtil.a(var61, var62, var59, var60, 2.5F, 0.5F, new Color[]{new Color(255, 255, 255, (int)((var63 ? 45.0F : 20.0F) * var9))});
      }

      float var64 = var61 + (var59 - var58) / 2.0F;
      float var65 = var62 + 2.5F;
      Color var66 = var56
         ? Theme.getInstance().getAccent()
         : (var2.maxUnitPrice > 0L ? new Color(220, 225, 235, (int)(255.0F * var9)) : new Color(130, 135, 145, (int)(200.0F * var9)));
      RenderUtil.a(var1, MsdfManager.BOLD, var64, var65, var57, 4.5F, var66);
      this.clickBoxes.add(new float[]{var61, var62, var59, var60});
      this.clickActions.add(() -> {
         this.editingEntry = var2;
         this.editPriceBuffer.setLength(0);
         if (var2.maxUnitPrice > 0L) {
            this.editPriceBuffer.append(var2.maxUnitPrice);
         }

         this.searchActive = false;
         this.editingNick = false;
         this.editingMinPrice = false;
         this.editingDurEntry = null;
      });
      boolean var67 = var2.d();
      float var68 = var61;
      if (var67) {
         boolean var69 = this.editingDurEntry == var2;
         String var70;
         if (var69) {
            var70 = (this.editDurBuffer.isEmpty() ? "0" : this.editDurBuffer.toString()) + "%_";
         } else {
            int var71 = var2.minDurabilityPercent > 0 ? var2.minDurabilityPercent : 50;
            var70 = "Прочн: " + var71 + "%";
         }

         float var81 = Msdf.a(MsdfManager.BOLD, var70, 4.5F);
         float var72 = Math.max(30.0F, var81 + 7.0F);
         float var73 = 10.5F;
         var68 = var61 - var72 - 3.0F;
         float var74 = var4 + 18.5F;
         boolean var75 = var7 >= var68 && var7 <= var68 + var72 && var8 >= var74 && var8 <= var74 + var73;
         if (var69) {
            RenderUtil.a(var68, var74, var72, var73, 2.5F, a(var13, 45.0F * var9));
            RenderUtil.a(var68, var74, var72, var73, 2.5F, 0.5F, a(var13, 220.0F * var9));
            RenderUtil.a(var1, MsdfManager.BOLD, var68 + (var72 - var81) / 2.0F, var74 + 2.5F, var70, 4.5F, Theme.getInstance().getAccent());
         } else {
            RenderUtil.a(var68, var74, var72, var73, 2.5F, new Color[]{new Color(255, 255, 255, (int)((var75 ? 26.0F : 12.0F) * var9))});
            RenderUtil.a(var68, var74, var72, var73, 2.5F, 0.5F, new Color[]{new Color(255, 255, 255, (int)((var75 ? 45.0F : 20.0F) * var9))});
            Color var76 = var2.minDurabilityPercent > 0 ? new Color(220, 225, 235, (int)(255.0F * var9)) : new Color(150, 155, 165, (int)(200.0F * var9));
            RenderUtil.a(var1, MsdfManager.BOLD, var68 + (var72 - var81) / 2.0F, var74 + 2.5F, var70, 4.5F, var76);
         }

         this.clickBoxes.add(new float[]{var68, var74, var72, var73});
         this.clickActions.add(() -> {
            this.editingDurEntry = var2;
            this.editDurBuffer.setLength(0);
            int var2x = var2.minDurabilityPercent > 0 ? var2.minDurabilityPercent : 50;
            this.editDurBuffer.append(var2x);
            this.searchActive = false;
            this.editingEntry = null;
            this.editingNick = false;
            this.editingMinPrice = false;
         });
      }

      float var79 = (var67 ? var68 : var61) - var27 - 4.0F;
      String var80 = "Рынок: " + (var2.marketPrice > 0L ? a(var2.marketPrice) + "$" : "—");
      String var82 = var80;
      if (Msdf.a(MsdfManager.BOLD, var82, 4.5F) > var79) {
         while (var82.length() > 3 && Msdf.a(MsdfManager.BOLD, var82 + "...", 4.5F) > var79) {
            var82 = var82.substring(0, var82.length() - 1);
         }

         var82 = var82 + "...";
      }

      RenderUtil.a(var1, MsdfManager.BOLD, var27, var4 + 19.5F, var82, 4.5F, new Color(145, 150, 160, (int)(220.0F * var9)));
   }

   private float a(DrawContext var1, float var2, float var3, float var4, int var5, int var6, float var7, float var8, float var9) {
      float var10 = var3;
      float var11 = var3;
      AutoBuyConfig var12 = AutoBuyConfig.getInstance();
      float var13 = 17.0F;
      float var14 = 3.0F;
      var11 = this.a(var1, var2, var11, var4, var13, "Включить AutoBuy", var12.enabled, var5, var6, var7, () -> {
         var12.enabled = !var12.enabled;
         AutoBuy var1x = FunctionManager.a(AutoBuy.class);
         if (var1x != null && var1x.isActive() != var12.enabled) {
            var1x.setEnabled(var12.enabled);
         }

         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Авто-продажа (AutoSell)", var12.autoSell, var5, var6, var7, () -> {
         var12.autoSell = !var12.autoSell;
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Авто-релист", var12.autoRelist, var5, var6, var7, () -> {
         var12.autoRelist = !var12.autoRelist;
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Авто-парс цен", var12.autoParse, var5, var6, var7, () -> {
         var12.autoParse = !var12.autoParse;
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Авто-оплата (AutoPay)", var12.autoPay, var5, var6, var7, () -> {
         var12.autoPay = !var12.autoPay;
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Авто-обновление (AutoRefresh)", var12.autoRefresh, var5, var6, var7, () -> {
         var12.autoRefresh = !var12.autoRefresh;
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, var12, var5, var6, var7);
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Процент от рынка (парсинг)", var12.parsePercent + "%", var5, var6, var7, () -> {
         var12.parsePercent = Math.max(5, var12.parsePercent - 5);
         AutoBuyConfig.g();
      }, () -> {
         var12.parsePercent = Math.min(100, var12.parsePercent + 5);
         AutoBuyConfig.g();
      });
      var11 += var14;
      var11 = this.a(var1, var2, var11, var4, var13, "Интервал релиста", var12.relistIntervalSeconds + " с", var5, var6, var7, () -> {
         var12.relistIntervalSeconds = Math.max(5, var12.relistIntervalSeconds - 5);
         AutoBuyConfig.g();
      }, () -> {
         var12.relistIntervalSeconds = Math.min(300, var12.relistIntervalSeconds + 5);
         AutoBuyConfig.g();
      });
      var11 += var14;
      float var15 = 80.0F;
      float var16 = var2 + var4 - var15 - 4.0F;
      boolean var17 = var5 >= var16 && var5 <= var16 + var15 && var6 >= var11 + 2.0F && var6 <= var11 + var13 - 2.0F;
      RenderUtil.a(var2, var11, var4, var13, 3.5F, new Color[]{new Color(255, 255, 255, (int)(8.0F * var7))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var11 + 4.5F, "Ник для AutoPay", 5.5F, new Color(240, 240, 245, (int)(255.0F * var7)));
      RenderUtil.a(
         var16,
         var11 + 2.0F,
         var15,
         var13 - 4.0F,
         2.5F,
         new Color[]{
            this.editingNick
               ? Theme.getInstance().getAccent()
               : (var17 ? new Color(255, 255, 255, (int)(35.0F * var7)) : new Color(255, 255, 255, (int)(14.0F * var7)))
         }
      );
      String var18 = this.editingNick
         ? (this.editNickBuffer.isEmpty() ? "_" : this.editNickBuffer + "_")
         : (var12.autoPayNick.isEmpty() ? "(не задан)" : var12.autoPayNick);
      float var19 = Msdf.a(MsdfManager.BOLD, var18, 4.5F);
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var16 + (var15 - var19) / 2.0F,
         var11 + 4.5F,
         var18,
         4.5F,
         this.editingNick ? Color.BLACK : new Color(230, 230, 235, (int)(255.0F * var7))
      );
      this.clickBoxes.add(new float[]{var16, var11 + 2.0F, var15, var13 - 4.0F});
      this.clickActions.add(() -> {
         this.editingNick = true;
         this.editNickBuffer.setLength(0);
         this.editNickBuffer.append(var12.autoPayNick);
         this.searchActive = false;
         this.editingEntry = null;
         this.editingMinPrice = false;
         this.editingDurEntry = null;
      });
      var11 += var13 + var14 + 4.0F;
      float var20 = var4;
      boolean var21 = var5 >= var2 && var5 <= var2 + var20 && var6 >= var11 && var6 <= var11 + var13;
      RenderUtil.a(
         var2, var11, var20, var13, 3.5F, new Color[]{var21 ? new Color(220, 60, 60, (int)(200.0F * var7)) : new Color(255, 255, 255, (int)(12.0F * var7))}
      );
      RenderUtil.a(var2, var11, var20, var13, 3.5F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(25.0F * var7))});
      String var22 = "Сбросить настройки к стандартным";
      float var23 = Msdf.a(MsdfManager.BOLD, var22, 5.0F);
      RenderUtil.a(
         var1, MsdfManager.BOLD, var2 + (var20 - var23) / 2.0F, var11 + 5.0F, var22, 5.0F, var21 ? Color.WHITE : new Color(210, 210, 215, (int)(255.0F * var7))
      );
      this.clickBoxes.add(new float[]{var2, var11, var20, var13});
      this.clickActions.add(var12::b);
      var11 += var13 + var14;
      return var11 - var10;
   }

   private float a(DrawContext var1, float var2, float var3, float var4, float var5, AutoBuyConfig var6, int var7, int var8, float var9) {
      boolean var10 = var7 >= var2 && var7 <= var2 + var4 && var8 >= var3 && var8 <= var3 + var5;
      RenderUtil.a(var2, var3, var4, var5, 3.5F, new Color[]{new Color(255, 255, 255, (int)((var10 ? 14.0F : 8.0F) * var9))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var3 + 4.5F, "Мин. порог цены", 5.5F, new Color(240, 240, 245, (int)(255.0F * var9)));
      float var11 = 18.0F;
      float var12 = 10.0F;
      float var13 = 5.0F;
      float var14 = var2 + var4 - var11 - 5.0F;
      float var15 = var3 + (var5 - var12) / 2.0F;
      float var16 = 7.6F;
      float var17 = 3.8F;
      float var18 = var15 + 1.2F;
      boolean var19 = var7 >= var14 && var7 <= var14 + var11 && var8 >= var15 && var8 <= var15 + var12;
      int var20 = Theme.getInstance().getAccent().getRGB();
      if (var6.minPriceEnabled) {
         int var21 = a(-13486527, var20, 0.88F);
         RenderUtil.a(var14, var15, var11, var12, var13, a(var21, 220.0F * var9));
         RenderUtil.a(var14, var15, var11, var12, var13, 0.5F, a(var20, 150.0F * var9));
         float var22 = var14 + var11 - 1.2F - var16;
         RenderUtil.a(var22, var18, var16, var16, var17, a(-1, 255.0F * var9));
      } else {
         RenderUtil.a(var14, var15, var11, var12, var13, a(1159734059, 180.0F * var9));
         RenderUtil.a(var14, var15, var11, var12, var13, 0.5F, a(-1, (var19 ? 50.0F : 30.0F) * var9));
         float var28 = var14 + 1.2F;
         RenderUtil.a(var28, var18, var16, var16, var17, a(-2828062, 220.0F * var9));
      }

      String var29 = this.editingMinPrice ? (this.editMinPriceBuffer.isEmpty() ? "0" : this.editMinPriceBuffer.toString()) + "_" : a(var6.minPrice) + "$";
      float var30 = Msdf.a(MsdfManager.BOLD, var29, 4.5F);
      float var23 = Math.max(52.0F, var30 + 12.0F);
      float var24 = 11.0F;
      float var25 = var14 - var23 - 5.0F;
      float var26 = var3 + (var5 - var24) / 2.0F;
      boolean var27 = var7 >= var25 && var7 <= var25 + var23 && var8 >= var26 && var8 <= var26 + var24;
      if (this.editingMinPrice) {
         RenderUtil.a(var25, var26, var23, var24, 2.5F, a(Theme.getInstance().getAccent().getRGB(), 45.0F * var9));
         RenderUtil.a(var25, var26, var23, var24, 2.5F, 0.5F, a(Theme.getInstance().getAccent().getRGB(), 220.0F * var9));
         RenderUtil.a(var1, MsdfManager.BOLD, var25 + (var23 - var30) / 2.0F, var26 + 2.5F, var29, 4.5F, Theme.getInstance().getAccent());
      } else {
         RenderUtil.a(var25, var26, var23, var24, 2.5F, new Color[]{new Color(255, 255, 255, (int)((var27 ? 26.0F : 14.0F) * var9))});
         RenderUtil.a(var25, var26, var23, var24, 2.5F, 0.5F, new Color[]{new Color(255, 255, 255, (int)((var27 ? 45.0F : 20.0F) * var9))});
         RenderUtil.a(
            var1,
            MsdfManager.BOLD,
            var25 + (var23 - var30) / 2.0F,
            var26 + 2.5F,
            var29,
            4.5F,
            var6.minPriceEnabled ? new Color(235, 235, 240, (int)(255.0F * var9)) : new Color(150, 155, 165, (int)(200.0F * var9))
         );
      }

      this.clickBoxes.add(new float[]{var25, var26, var23, var24});
      this.clickActions.add(() -> {
         this.editingMinPrice = true;
         this.editMinPriceBuffer.setLength(0);
         if (var6.minPrice > 0L) {
            this.editMinPriceBuffer.append(var6.minPrice);
         }

         this.editingNick = false;
         this.editingEntry = null;
         this.searchActive = false;
         this.editingDurEntry = null;
      });
      this.clickBoxes.add(new float[]{var14 - 2.0F, var15 - 2.0F, var11 + 4.0F, var12 + 4.0F});
      this.clickActions.add(() -> {
         var6.minPriceEnabled = !var6.minPriceEnabled;
         AutoBuyConfig.g();
      });
      this.clickBoxes.add(new float[]{var2, var3, var25 - var2, var5});
      this.clickActions.add(() -> {
         var6.minPriceEnabled = !var6.minPriceEnabled;
         AutoBuyConfig.g();
      });
      return var3 + var5;
   }

   private float a(DrawContext var1, float var2, float var3, float var4, float var5, String var6, boolean var7, int var8, int var9, float var10, Runnable var11) {
      boolean var12 = var8 >= var2 && var8 <= var2 + var4 && var9 >= var3 && var9 <= var3 + var5;
      RenderUtil.a(var2, var3, var4, var5, 3.5F, new Color[]{new Color(255, 255, 255, (int)((var12 ? 14.0F : 8.0F) * var10))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var3 + 4.5F, var6, 5.5F, var7 ? Color.WHITE : new Color(210, 215, 225, (int)(255.0F * var10)));
      float var13 = 18.0F;
      float var14 = 10.0F;
      float var15 = 5.0F;
      float var16 = var2 + var4 - var13 - 5.0F;
      float var17 = var3 + (var5 - var14) / 2.0F;
      float var18 = 7.6F;
      float var19 = 3.8F;
      float var20 = var17 + 1.2F;
      int var21 = Theme.getInstance().getAccent().getRGB();
      if (var7) {
         int var22 = a(-13486527, var21, 0.88F);
         RenderUtil.a(var16, var17, var13, var14, var15, a(var22, 220.0F * var10));
         RenderUtil.a(var16, var17, var13, var14, var15, 0.5F, a(var21, 150.0F * var10));
         float var23 = var16 + var13 - 1.2F - var18;
         RenderUtil.a(var23, var20, var18, var18, var19, a(-1, 255.0F * var10));
      } else {
         RenderUtil.a(var16, var17, var13, var14, var15, a(1159734059, 180.0F * var10));
         RenderUtil.a(var16, var17, var13, var14, var15, 0.5F, a(-1, (var12 ? 50.0F : 30.0F) * var10));
         float var24 = var16 + 1.2F;
         RenderUtil.a(var24, var20, var18, var18, var19, a(-2828062, 220.0F * var10));
      }

      this.clickBoxes.add(new float[]{var2, var3, var4, var5});
      this.clickActions.add(var11);
      return var3 + var5;
   }

   private float a(
      DrawContext var1,
      float var2,
      float var3,
      float var4,
      float var5,
      String var6,
      String var7,
      int var8,
      int var9,
      float var10,
      Runnable var11,
      Runnable var12
   ) {
      RenderUtil.a(var2, var3, var4, var5, 3.5F, new Color[]{new Color(255, 255, 255, (int)(8.0F * var10))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var3 + 4.5F, var6, 5.5F, new Color(240, 240, 245, (int)(255.0F * var10)));
      float var13 = 11.0F;
      float var14 = 36.0F;
      float var15 = var2 + var4 - var13 - 4.0F;
      float var16 = var15 - var14 - 2.0F;
      float var17 = var16 - var13 - 2.0F;
      float var18 = var3 + (var5 - var13) / 2.0F;
      boolean var19 = var8 >= var17 && var8 <= var17 + var13 && var9 >= var18 && var9 <= var18 + var13;
      RenderUtil.a(var17, var18, var13, var13, 2.0F, new Color[]{var19 ? Theme.getInstance().getAccent() : new Color(255, 255, 255, (int)(20.0F * var10))});
      float var20 = Msdf.a(MsdfManager.BOLD, "-", 5.0F);
      float var21 = Msdf.a(MsdfManager.BOLD, 5.0F);
      RenderUtil.a(var1, MsdfManager.BOLD, var17 + (var13 - var20) / 2.0F, var18 + (var13 - var21) / 2.0F - 0.5F, "-", 5.0F, var19 ? Color.BLACK : Color.WHITE);
      this.clickBoxes.add(new float[]{var17, var18, var13, var13});
      this.clickActions.add(var11);
      float var22 = Msdf.a(MsdfManager.BOLD, var7, 4.75F);
      float var23 = Msdf.a(MsdfManager.BOLD, 4.75F);
      RenderUtil.a(var1, MsdfManager.BOLD, var16 + (var14 - var22) / 2.0F, var18 + (var13 - var23) / 2.0F - 0.5F, var7, 4.75F, Theme.getInstance().getAccent());
      boolean var24 = var8 >= var15 && var8 <= var15 + var13 && var9 >= var18 && var9 <= var18 + var13;
      RenderUtil.a(var15, var18, var13, var13, 2.0F, new Color[]{var24 ? Theme.getInstance().getAccent() : new Color(255, 255, 255, (int)(20.0F * var10))});
      float var25 = Msdf.a(MsdfManager.BOLD, "+", 5.0F);
      float var26 = Msdf.a(MsdfManager.BOLD, 5.0F);
      RenderUtil.a(var1, MsdfManager.BOLD, var15 + (var13 - var25) / 2.0F, var18 + (var13 - var26) / 2.0F - 0.5F, "+", 5.0F, var24 ? Color.BLACK : Color.WHITE);
      this.clickBoxes.add(new float[]{var15, var18, var13, var13});
      this.clickActions.add(var12);
      return var3 + var5;
   }

   private float b(DrawContext var1, float var2, float var3, float var4, int var5, int var6, float var7, float var8, float var9) {
      float var10 = var3;
      float var11 = var3;
      List var12 = AutoBuyConfig.getInstance().history;
      float var13 = 14.0F;
      RenderUtil.a(var2, var11, var4, var13, 3.0F, new Color[]{new Color(255, 255, 255, (int)(10.0F * var7))});
      String var14 = "Всего записей: " + var12.size();
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 5.0F, var11 + 3.5F, var14, 5.0F, new Color(200, 205, 215, (int)(255.0F * var7)));
      float var15 = 54.0F;
      float var16 = var2 + var4 - var15 - 3.0F;
      float var17 = var11 + 1.5F;
      float var18 = var13 - 3.0F;
      boolean var19 = var5 >= var16 && var5 <= var16 + var15 && var6 >= var17 && var6 <= var17 + var18;
      RenderUtil.a(
         var16, var17, var15, var18, 2.0F, new Color[]{var19 ? new Color(220, 60, 60, (int)(220.0F * var7)) : new Color(255, 255, 255, (int)(20.0F * var7))}
      );
      String var20 = "Очистить";
      float var21 = Msdf.a(MsdfManager.BOLD, var20, 4.5F);
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var16 + (var15 - var21) / 2.0F,
         var17 + 2.0F,
         var20,
         4.5F,
         var19 ? Color.WHITE : new Color(220, 220, 230, (int)(255.0F * var7))
      );
      this.clickBoxes.add(new float[]{var16, var17, var15, var18});
      this.clickActions.add(() -> {
         var12.clear();
         AutoBuyConfig.g();
      });
      float var35 = var11 + (var13 + 4.0F);
      if (var12.isEmpty()) {
         RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var35 + 6.0F, "История покупок пуста", 5.5F, new Color(140, 145, 155, (int)(220.0F * var7)));
         return var35 + 20.0F - var10;
      }

      float var22 = 22.0F;
      float var23 = 3.0F;

      for (PurchaseRecord var25 : (Iterable<PurchaseRecord>)(Iterable<?>)(var12)) {
         if (var35 + var22 >= var8 && var35 <= var8 + var9) {
            RenderUtil.a(var2, var35, var4, var22, 3.5F, new Color[]{new Color(255, 255, 255, (int)(8.0F * var7))});
            RenderUtil.a(var2, var35, var4, var22, 3.5F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(16.0F * var7))});
            ItemStack var26 = a(var25.itemId, var25.label);
            float var27 = var35 + 3.0F;
            if (!var26.isEmpty() && var27 + 16.0F > var8 && var27 < var8 + var9) {
               this.a(var1, var26, var2 + 3.5F, var27, 16.0F);
            }

            float var28 = var2 + 23.0F;
            Color var29 = var25.success ? new Color(90, 210, 110, (int)(255.0F * var7)) : new Color(220, 90, 90, (int)(255.0F * var7));
            String var30 = var25.success ? "✓ " : "✗ ";
            String var31 = var30 + (var25.label == null ? "?" : var25.label);
            RenderUtil.a(var1, MsdfManager.BOLD, var28, var35 + 3.0F, var31, 5.25F, var29);
            String var32 = "Продавец: " + (var25.seller != null ? var25.seller : "—");
            if (!var25.success && var25.failReason != null) {
               var32 = var32 + " • " + var25.failReason;
            }

            RenderUtil.a(var1, MsdfManager.BOLD, var28, var35 + 12.0F, var32, 4.5F, new Color(145, 150, 160, (int)(220.0F * var7)));
            String var33 = "×" + var25.count + "  " + a(var25.totalPrice) + "$";
            float var34 = Msdf.a(MsdfManager.BOLD, var33, 5.0F);
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + var4 - var34 - 6.0F, var35 + 7.5F, var33, 5.0F, Theme.getInstance().getAccent());
         }

         var35 += var22 + var23;
      }

      return var35 - var10;
   }

   public void a() {
      if (this.editingEntry != null) {
         try {
            String var1 = this.editPriceBuffer.toString().trim();
            this.editingEntry.maxUnitPrice = var1.isEmpty() ? 0L : Long.parseLong(var1);
            AutoBuyConfig.g();
         } catch (Exception var4) {
         }

         this.editingEntry = null;
      }

      if (this.editingNick) {
         AutoBuyConfig.getInstance().autoPayNick = this.editNickBuffer.toString().trim();
         AutoBuyConfig.g();
         this.editingNick = false;
      }

      if (this.editingMinPrice) {
         try {
            String var5 = this.editMinPriceBuffer.toString().trim();
            AutoBuyConfig.getInstance().minPrice = var5.isEmpty() ? 0L : Long.parseLong(var5);
            AutoBuyConfig.g();
         } catch (Exception var3) {
         }

         this.editingMinPrice = false;
      }

      if (this.editingDurEntry != null) {
         try {
            String var6 = this.editDurBuffer.toString().trim();
            this.editingDurEntry.minDurabilityPercent = var6.isEmpty() ? 0 : Math.clamp(Integer.parseInt(var6), 0, 100);
            AutoBuyConfig.g();
         } catch (Exception var2) {
         }

         this.editingDurEntry = null;
      }
   }

   public boolean a(float var1, float var2, int var3, boolean var4, Runnable var5) {
      if (var4 && var3 == 0) {
         if (var1 >= this.closeBtnBox[0]
            && var1 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var2 >= this.closeBtnBox[1]
            && var2 <= this.closeBtnBox[1] + this.closeBtnBox[3]) {
            this.a();
            if (var5 != null) {
               var5.run();
            }

            return true;
         } else {
            for (int var6 = 0; var6 < this.clickBoxes.size(); var6++) {
               float[] var7 = this.clickBoxes.get(var6);
               if (var1 >= var7[0] && var1 <= var7[0] + var7[2] && var2 >= var7[1] && var2 <= var7[1] + var7[3] && var6 < this.clickActions.size()) {
                  this.a();
                  this.clickActions.get(var6).run();
                  return true;
               }
            }

            this.searchActive = false;
            this.searchSelectAll = false;
            this.a();
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean a(double var1) {
      this.scrollTarget += (float)var1 * 20.0F;
      if (this.scrollTarget < -this.maxScroll) {
         this.scrollTarget = -this.maxScroll;
      }

      if (this.scrollTarget > 0.0F) {
         this.scrollTarget = 0.0F;
      }

      return true;
   }

   public boolean a(KeyInput var1) {
      boolean var2 = (var1.modifiers() & 2) != 0
         || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) == 1
         || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 345) == 1;
      if (var2 && var1.key() == 70) {
         this.a();
         this.currentTab = 0;
         this.searchActive = !this.searchActive;
         if (this.searchActive) {
            this.searchSelectAll = true;
         } else {
            this.searchSelectAll = false;
         }

         return true;
      } else if (this.editingEntry != null) {
         if (var1.key() == 257 || var1.key() == 335) {
            try {
               String var10 = this.editPriceBuffer.toString().trim();
               this.editingEntry.maxUnitPrice = var10.isEmpty() ? 0L : Long.parseLong(var10);
               AutoBuyConfig.g();
            } catch (Exception var5) {
            }

            this.editingEntry = null;
            return true;
         } else {
            if (var1.key() == 256) {
               this.editingEntry = null;
               return true;
            }

            if (var1.key() == 259) {
               if (this.editPriceBuffer.length() > 0) {
                  this.editPriceBuffer.deleteCharAt(this.editPriceBuffer.length() - 1);
               }

               return true;
            } else {
               return true;
            }
         }
      } else if (this.editingNick) {
         if (var1.key() == 257 || var1.key() == 335) {
            AutoBuyConfig.getInstance().autoPayNick = this.editNickBuffer.toString().trim();
            AutoBuyConfig.g();
            this.editingNick = false;
            return true;
         }

         if (var1.key() == 256) {
            this.editingNick = false;
            return true;
         }

         if (var1.key() == 259) {
            if (this.editNickBuffer.length() > 0) {
               this.editNickBuffer.deleteCharAt(this.editNickBuffer.length() - 1);
            }

            return true;
         } else {
            return true;
         }
      } else if (this.editingMinPrice) {
         if (var1.key() == 257 || var1.key() == 335) {
            try {
               String var9 = this.editMinPriceBuffer.toString().trim();
               AutoBuyConfig.getInstance().minPrice = var9.isEmpty() ? 0L : Long.parseLong(var9);
               AutoBuyConfig.g();
            } catch (Exception var6) {
            }

            this.editingMinPrice = false;
            return true;
         } else {
            if (var1.key() == 256) {
               this.editingMinPrice = false;
               return true;
            }

            if (var1.key() == 259) {
               if (this.editMinPriceBuffer.length() > 0) {
                  this.editMinPriceBuffer.deleteCharAt(this.editMinPriceBuffer.length() - 1);
               }

               return true;
            } else {
               return true;
            }
         }
      } else if (this.editingDurEntry != null) {
         if (var1.key() == 257 || var1.key() == 335) {
            try {
               String var8 = this.editDurBuffer.toString().trim();
               this.editingDurEntry.minDurabilityPercent = var8.isEmpty() ? 0 : Math.clamp(Integer.parseInt(var8), 0, 100);
               AutoBuyConfig.g();
            } catch (Exception var71) {
            }

            this.editingDurEntry = null;
            return true;
         } else {
            if (var1.key() == 256) {
               this.editingDurEntry = null;
               return true;
            }

            if (var1.key() == 259) {
               if (this.editDurBuffer.length() > 0) {
                  this.editDurBuffer.deleteCharAt(this.editDurBuffer.length() - 1);
               }

               return true;
            } else {
               return true;
            }
         }
      } else if (this.searchActive) {
         if (var2 && var1.key() == 65) {
            this.searchSelectAll = true;
            return true;
         }

         if (!var2 || var1.key() != 67 && var1.key() != 88) {
            if (var2 && var1.key() == 86) {
               String var3 = MinecraftClient.getInstance().keyboard.getClipboard();
               if (var3 != null) {
                  String var7 = var3.replace("\r", "").replace("\n", "");
                  if (this.searchSelectAll) {
                     this.searchBuffer.setLength(0);
                     this.searchSelectAll = false;
                  }

                  this.searchBuffer.append(var7);
                  this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
               }

               return true;
            } else {
               if (var1.key() == 257 || var1.key() == 335 || var1.key() == 256) {
                  this.searchActive = false;
                  this.searchSelectAll = false;
                  return true;
               }

               if (var1.key() != 259 && var1.key() != 261) {
                  return true;
               }

               if (this.searchSelectAll) {
                  this.searchBuffer.setLength(0);
                  this.searchQuery = "";
                  this.searchSelectAll = false;
               } else if (this.searchBuffer.length() > 0) {
                  this.searchBuffer.deleteCharAt(this.searchBuffer.length() - 1);
                  this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
               }

               return true;
            }
         } else {
            MinecraftClient.getInstance().keyboard.setClipboard(this.searchBuffer.toString());
            if (var1.key() == 88) {
               this.searchBuffer.setLength(0);
               this.searchQuery = "";
               this.searchSelectAll = false;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean a(CharInput var1) {
      if (this.editingEntry != null) {
         if (var1.isValidChar()) {
            String var8 = var1.asString();

            for (char var16 : var8.toCharArray()) {
               if (var16 >= '0' && var16 <= '9' && this.editPriceBuffer.length() < 12) {
                  this.editPriceBuffer.append(var16);
               }
            }
         }

         return true;
      } else if (this.editingNick) {
         if (var1.isValidChar()) {
            this.editNickBuffer.append(var1.asString());
         }

         return true;
      } else if (this.editingMinPrice) {
         if (var1.isValidChar()) {
            String var7 = var1.asString();

            for (char var15 : var7.toCharArray()) {
               if (var15 >= '0' && var15 <= '9' && this.editMinPriceBuffer.length() < 12) {
                  this.editMinPriceBuffer.append(var15);
               }
            }
         }

         return true;
      } else if (this.editingDurEntry == null) {
         if (this.searchActive) {
            if (var1.isValidChar()) {
               if (this.searchSelectAll) {
                  this.searchBuffer.setLength(0);
                  this.searchSelectAll = false;
               }

               this.searchBuffer.append(var1.asString());
               this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
            }

            return true;
         } else {
            return false;
         }
      } else {
         if (var1.isValidChar()) {
            String var2 = var1.asString();

            for (char var6 : var2.toCharArray()) {
               if (var6 >= '0' && var6 <= '9' && this.editDurBuffer.length() < 3) {
                  this.editDurBuffer.append(var6);
               }
            }
         }

         return true;
      }
   }

   private void a(DrawContext var1, ItemStack var2, float var3, float var4, float var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      float var7 = 2.0F / var6.getWindow().getScaleFactor();
      Matrix3x2fStack var8 = var1.getMatrices();
      var8.pushMatrix();
      var8.translate((var3 + var5 * 0.5F) * var7, (var4 + var5 * 0.5F) * var7);
      float var9 = var5 / 16.0F * var7;
      var8.scale(var9, var9);
      var8.translate(-8.0F, -8.0F);
      var1.drawItem(var2, 0, 0);
      var8.popMatrix();
   }

   private static ItemStack a(ItemEntry var0) {
      return var0 == null ? ItemStack.EMPTY : a(var0.itemId, var0.displayName);
   }

   private static ItemStack a(String var0, String var1) {
      if (var1 != null) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         if (var2.contains("трапка") && !var2.contains("взрывная")) {
            return new ItemStack(Items.POPPED_CHORUS_FRUIT);
         }

         if (var2.contains("скорост") && var2.contains("сфера")) {
            return AutoSborScreen.SPEED_SPHERE_STACK != null ? AutoSborScreen.SPEED_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
         }

         if (var2.contains("флеш") || var2.contains("flash")) {
            return AutoSborScreen.FLASH_SPHERE_STACK != null ? AutoSborScreen.FLASH_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
         }

         if (var2.contains("арморталити") || var2.contains("armortality")) {
            return AutoSborScreen.ARMORTALITY_SPHERE_STACK != null ? AutoSborScreen.ARMORTALITY_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
         }

         if (var2.contains("имморталити") || var2.contains("immortality")) {
            return AutoSborScreen.IMMORTALITY_SPHERE_STACK != null ? AutoSborScreen.IMMORTALITY_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
         }

         if (var2.contains("цербер")) {
            if (var2.contains("меч") || var2.contains("sword") || var0 != null && var0.endsWith("_sword")) {
               return AutoSborScreen.CERBERUS_SWORD_STACK != null ? AutoSborScreen.CERBERUS_SWORD_STACK : new ItemStack(Items.NETHERITE_SWORD);
            }

            return AutoSborScreen.CERBERUS_SPHERE_STACK != null ? AutoSborScreen.CERBERUS_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
         }

         if (var2.contains("мифическ") || var2.contains("mythic")) {
            if (!var2.contains("урон 3") && !var2.contains("урон iii") && !var2.contains("броня 2")) {
               return AutoSborScreen.ARMOR3_DAMAGE2_SPHERE_STACK != null ? AutoSborScreen.ARMOR3_DAMAGE2_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
            } else {
               return AutoSborScreen.ARMOR2_DAMAGE3_SPHERE_STACK != null ? AutoSborScreen.ARMOR2_DAMAGE3_SPHERE_STACK : new ItemStack(Items.PLAYER_HEAD);
            }
         }

         if (var2.contains("черепашь")) {
            return AutoSborScreen.TURTLE_MASTER_POTION_STACK != null ? AutoSborScreen.TURTLE_MASTER_POTION_STACK : new ItemStack(Items.POTION);
         }

         if (var2.contains("победител")) {
            return AutoSborScreen.WINNER_POTION_STACK != null ? AutoSborScreen.WINNER_POTION_STACK : new ItemStack(Items.POTION);
         }
      }

      if (var0 == null || var0.isBlank()) {
         return ItemStack.EMPTY;
      }

      if (!var0.contains("popped_winlend_fruit") && !var0.contains("popped_chorus_fruit")) {
         Identifier var4 = Identifier.tryParse(var0);
         if (var4 == null) {
            return ItemStack.EMPTY;
         }

         Item var3 = Registries.ITEM.get(var4);
         return var3 == Items.AIR ? ItemStack.EMPTY : new ItemStack(var3);
      } else {
         return new ItemStack(Items.POPPED_CHORUS_FRUIT);
      }
   }

   private static String a(long var0) {
      if (var0 <= 0L) {
         return "0";
      }

      StringBuilder var2 = new StringBuilder(Long.toString(var0));

      for (int var3 = var2.length() - 3; var3 > 0; var3 -= 3) {
         var2.insert(var3, ' ');
      }

      return var2.toString();
   }
}
