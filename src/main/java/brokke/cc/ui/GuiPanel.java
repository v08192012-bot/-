package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ButtonSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.MultiSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.setting.Setting;
import brokke.cc.setting.TextSetting;
import brokke.cc.util.KeyHelper;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;

public class GuiPanel {
   public static final float W = 125.0F;
   public static final float H = 270.0F;
   public static final float MAX_H = 270.0F;
   private static final float HEADER = 24.0F;
   private static final float PAD = 6.0F;
   private static final float ROW_H = 16.0F;
   private static final float ROW_GAP = 3.0F;
   private static final float SETTING_GAP = 3.0F;
   private static final int TEXT_COLOR = -1;
   private static final int TEXT_DIM_COLOR = -5260861;
   public final Category category;
   /** Размеры панели: классическая гуи ставит 125x270, CS-гуи — размер контентной области. */
   public float panelW = W;
   public float panelH = H;
   /** Заголовок панели (в CS-гуи может отличаться от имени категории, напр. Visuals). */
   public String title;
   public final Animation open = new Animation();
   private final Animation scroll = new Animation();
   private float scrollTarget;
   float openDelay;
   private final Map<Function, Animation> toggles = new HashMap<>();
   private final Map<Function, Animation> hovers = new HashMap<>();
   private final Map<Function, Animation> expands = new HashMap<>();
   private final Map<Object, Map<String, Animation>> chipAnims = new HashMap<>();
   private final Map<Object, Animation> settingAnims = new HashMap<>();
   private final Map<Function, Animation> rowSlideAnims = new HashMap<>();
   private final Set<Function> expandedSet = new HashSet<>();
   private final List<GuiPanel.a> hitPool = new ArrayList<>();
   private int hitCount = 0;
   public Function hovered;
   public Function listening;
   private NumberSetting dragging;
   private float dragMin;
   private float dragMax;
   private boolean draggingScroll = false;
   private float dragScrollStartY;
   private float dragScrollStartTarget;
   private ColorSetting draggingColor;
   private int colorDragMode;
   private float[] colorBounds;
   public float x;
   public float y;
   public static NumberSetting typingNumber = null;
   public static final StringBuilder numberInputBuffer = new StringBuilder();
   private List<Function> cachedModules = null;
   private String lastSearchQuery = null;
   private static final Map<Function, Float> MODULE_NAME_WIDTHS = new HashMap<>();
   private static final Map<String, Float> CHIP_WIDTH_CACHE = new HashMap<>();
   private static float cachedDots10W = -1.0F;
   private static float cachedIconCW = -1.0F;
   private static float cachedDicons6H = -1.0F;
   private static float cachedBold6H = -1.0F;
   private static float cachedBold10H = -1.0F;
   private static float cachedBold65H = -1.0F;
   private static float cachedBold625H = -1.0F;
   private static float cachedBold725H = -1.0F;
   private static float cachedBold9H = -1.0F;
   private static float cachedDicons9H = -1.0F;
   private static float cachedDiconsM75W = -1.0F;
   private static float cachedDiconsM75H = -1.0F;

   private static int a(int var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0 & 16777215;
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
      return (int)(var3 + (var7 - var3) * var2) << 24 | var11 << 16 | var12 << 8 | var13;
   }

   public static void a() {
      if (typingNumber != null) {
         try {
            String var0 = numberInputBuffer.toString().replace(',', '.').trim();
            if (!var0.isEmpty()) {
               double var1 = Math.round(Math.clamp(Double.parseDouble(var0), typingNumber.getMin(), typingNumber.getMax()) / typingNumber.getStep())
                  * typingNumber.getStep();
               typingNumber.setValue(var1);
            }
         } catch (Exception var3) {
         }

         typingNumber = null;
         numberInputBuffer.setLength(0);
      }
   }

   public GuiPanel(Category var1) {
      this.category = var1;
      this.title = var1.getName();
      this.open.setEasing(Easing.SPRING_SOFT);
      this.scroll.setEasing(Easing.SMOOTH_STEP);
   }

   private void a(GuiPanel.b var1, float var2, float var3, float var4, float var5, Object var6, Object var7) {
      GuiPanel.a var8;
      if (this.hitCount < this.hitPool.size()) {
         var8 = this.hitPool.get(this.hitCount);
      } else {
         var8 = new GuiPanel.a();
         this.hitPool.add(var8);
      }

      var8.a(var1, var2, var3, var4, var5, var6, var7);
      this.hitCount++;
   }

   private static float c() {
      if (cachedDots10W < 0.0F && Msdf.a()) {
         cachedDots10W = Msdf.a(MsdfManager.BOLD, "...", 10.0F);
      }

      return cachedDots10W > 0.0F ? cachedDots10W : 12.0F;
   }

   private static float d() {
      if (cachedIconCW < 0.0F && Msdf.a()) {
         cachedIconCW = Msdf.a(MsdfManager.DICONS, "C", 6.0F);
      }

      return cachedIconCW > 0.0F ? cachedIconCW : 6.0F;
   }

   private static float e() {
      if (cachedDicons6H < 0.0F && Msdf.a()) {
         cachedDicons6H = Msdf.a(MsdfManager.DICONS, 6.0F);
      }

      return cachedDicons6H > 0.0F ? cachedDicons6H : 6.0F;
   }

   private static float f() {
      if (cachedBold6H < 0.0F && Msdf.a()) {
         cachedBold6H = Msdf.a(MsdfManager.BOLD, 6.0F);
      }

      return cachedBold6H > 0.0F ? cachedBold6H : 6.0F;
   }

   private static float g() {
      if (cachedBold10H < 0.0F && Msdf.a()) {
         cachedBold10H = Msdf.a(MsdfManager.BOLD, 10.0F);
      }

      return cachedBold10H > 0.0F ? cachedBold10H : 10.0F;
   }

   private static float h() {
      if (cachedBold65H < 0.0F && Msdf.a()) {
         cachedBold65H = Msdf.a(MsdfManager.BOLD, 6.5F);
      }

      return cachedBold65H > 0.0F ? cachedBold65H : 6.5F;
   }

   private static float i() {
      if (cachedBold625H < 0.0F && Msdf.a()) {
         cachedBold625H = Msdf.a(MsdfManager.BOLD, 6.25F);
      }

      return cachedBold625H > 0.0F ? cachedBold625H : 6.25F;
   }

   private static float j() {
      if (cachedBold725H < 0.0F && Msdf.a()) {
         cachedBold725H = Msdf.a(MsdfManager.BOLD, 7.25F);
      }

      return cachedBold725H > 0.0F ? cachedBold725H : 7.25F;
   }

   private static float k() {
      if (cachedBold9H < 0.0F && Msdf.a()) {
         cachedBold9H = Msdf.a(MsdfManager.BOLD, 9.0F);
      }

      return cachedBold9H > 0.0F ? cachedBold9H : 9.0F;
   }

   private static float l() {
      if (cachedDicons9H < 0.0F && Msdf.a()) {
         cachedDicons9H = Msdf.a(MsdfManager.DICONS, 9.0F);
      }

      return cachedDicons9H > 0.0F ? cachedDicons9H : 9.0F;
   }

   private static float m() {
      if (cachedDiconsM75W < 0.0F && Msdf.a()) {
         cachedDiconsM75W = Msdf.a(MsdfManager.DICONS, "m", 7.5F);
      }

      return cachedDiconsM75W > 0.0F ? cachedDiconsM75W : 7.5F;
   }

   private static float n() {
      if (cachedDiconsM75H < 0.0F && Msdf.a()) {
         cachedDiconsM75H = Msdf.a(MsdfManager.DICONS, 7.5F);
      }

      return cachedDiconsM75H > 0.0F ? cachedDiconsM75H : 7.5F;
   }

   private static float a(Function var0) {
      Float var1 = MODULE_NAME_WIDTHS.get(var0);
      if (var1 == null) {
         var1 = Msdf.a(MsdfManager.BOLD, var0.getName(), 7.25F);
         if (var1 > 0.0F) {
            MODULE_NAME_WIDTHS.put(var0, var1);
         }
      }

      return var1 != null ? var1 : 40.0F;
   }

   private static float b(String var0) {
      Float var1 = CHIP_WIDTH_CACHE.get(var0);
      if (var1 == null) {
         var1 = Msdf.a(MsdfManager.BOLD, var0, 6.25F) + 6.0F;
         if (var1 > 6.0F) {
            CHIP_WIDTH_CACHE.put(var0, var1);
         }
      }

      return var1 != null ? var1 : 20.0F;
   }

   public void a(DrawContext var1, int var2, int var3, float var4) {
      this.a(var1, var2, var3, var4, "", true);
   }

   public void a(DrawContext var1, int var2, int var3, float var4, String var5) {
      this.a(var1, var2, var3, var4, var5, true);
   }

   public void a(DrawContext var1, int var2, int var3, float var4, String var5, boolean var6) {
      long var7 = System.currentTimeMillis();
      int var9 = Theme.getInstance().getAccent().getRGB();
      if (!var6) {
         this.openDelay = 0.0F;
         this.open.animate(0.0F, var4, 5.5F, var7);
      } else if (this.openDelay > 0.0F) {
         this.openDelay -= var4;
      } else {
         this.open.animate(1.0F, var4, 5.5F, var7);
      }

      if (!(this.open.getValue() <= 0.001F)) {
         float var10 = Math.clamp(this.open.getValue(), 0.0F, 1.0F);
         float var11 = this.y + (1.0F - var10) * 14.0F;
         float var12 = var2 * RenderUtil.e();
         float var13 = var3 * RenderUtil.e();
         List var14 = this.a(var5);

         for (Function var16 : (Iterable<Function>)(Iterable<?>)(var14)) {
            Animation var17 = this.toggles.get(var16);
            if (var17 == null) {
               var17 = new Animation();
               var17.setEasing(Easing.SPRING_SOFT);
               this.toggles.put(var16, var17);
            }

            var17.animate(var16.isActive() ? 1.0F : 0.0F, var4, 6.5F, var7);
            Animation var18 = this.expands.get(var16);
            if (var18 == null) {
               var18 = new Animation();
               var18.setEasing(Easing.SPRING_SOFT);
               this.expands.put(var16, var18);
            }

            var18.animate(this.expandedSet.contains(var16) ? 1.0F : 0.0F, var4, 6.0F, var7);
            Animation var19 = this.hovers.get(var16);
            if (var19 == null) {
               var19 = new Animation();
               var19.setEasing(Easing.SMOOTH_STEP);
               this.hovers.put(var16, var19);
            }

            var19.animate(var16 == this.hovered ? 1.0F : 0.0F, var4, 8.0F, var7);
            Animation var20 = this.rowSlideAnims.get(var16);
            if (var20 == null) {
               var20 = new Animation();
               var20.setEasing(Easing.SPRING_SOFT);
               this.rowSlideAnims.put(var16, var20);
            }

            var20.animate(1.0F, var4, 6.0F, var7);
         }

         float var29 = this.a(var14);
         float var30 = this.panelH;
         float var31 = var11 + 24.0F + 2.0F;
         float var32 = var30 - 24.0F - 6.0F;
         this.scrollTarget = Math.clamp(this.scrollTarget, Math.min(0.0F, var32 - var29), 0.0F);
         this.scroll.animate(this.scrollTarget, var4, 7.0F, var7);
         HudConfig.a("panel", this.x, var11, this.panelW, var30, 8.0F, (int)(245.0F * var10) << 24);
         this.a(var1, var11, var10, var9);
         this.hitCount = 0;
         Scissor.a(this.x - 1.0F, var31, this.x + this.panelW + 1.0F, var31 + var32);
         Msdf.b();
         float var33 = var31 + this.scroll.getValue();

         for (Function var21 : (Iterable<Function>)(Iterable<?>)(var14)) {
            float var22 = this.b(var21);
            if (var33 + var22 >= var31 && var33 <= var31 + var32) {
               float var24 = Math.clamp((var31 + var32 - var33) / 16.0F, 0.0F, 1.0F);
               this.a(var1, var21, var12, var13, var33, var22, var24, var4, var10, var9, var7);
            }

            var33 += var22 + 3.0F;
         }

         Msdf.c();
         Scissor.a();
         if (var29 > var32) {
            float var35 = 2.0F;
            float var36 = this.x + this.panelW - 3.5F;
            float var37 = var31 + 2.0F;
            float var23 = var32 - 4.0F;
            float var38 = var32 / var29;
            float var25 = Math.max(16.0F, var23 * var38);
            float var26;
            float var27 = (var26 = var29 - var32) > 0.0F ? Math.clamp(-this.scroll.getValue() / var26, 0.0F, 1.0F) : 0.0F;
            float var28 = var37 + (var23 - var25) * var27;
            RenderUtil.a(var36, var37, var35, var23, 1.0F, a(-1, 18.0F * var10));
            RenderUtil.a(var36, var28, var35, var25, 1.0F, a(var9, (this.draggingScroll ? 230.0F : 160.0F) * var10));
         }
      }
   }

   private void a(DrawContext var1, float var2, float var3, int var4) {
      String var5 = this.title;
      float var6 = k();
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, var5, this.x + 10.0F, var2 + (24.0F - var6) / 2.0F, 9.0F, a(a(-1, var4, 0.25F), 255.0F * var3), false);
      String var7 = this.category.getIcon();
      float var8 = Msdf.a(MsdfManager.DICONS, var7, 9.0F);
      float var9 = l();
      Msdf.a(
         var1, MsdfManager.DICONS, var7, this.x + this.panelW - 10.0F - var8, var2 + (24.0F - var9) / 2.0F + 0.5F, 9.0F, a(a(-1, var4, 0.35F), 255.0F * var3), false
      );
      Msdf.c();
   }

   private void a(DrawContext var1, Function var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10, long var11) {
      float var13 = this.x + 6.0F;
      float var14 = this.panelW - 12.0F;
      float var15 = Math.clamp(this.toggles.get(var2).getValue(), 0.0F, 1.0F);
      float var16 = Math.clamp(this.hovers.get(var2).getValue(), 0.0F, 1.0F);
      float var17 = Math.clamp(this.expands.get(var2).getValue(), 0.0F, 1.0F);
      float var18 = var9;
      if (var15 > 0.01F) {
         RenderUtil.a(var13, var5, var14, var6, 4.0F, a(var10, 10.0F * var15 * var7 * var18));
      }

      if (var16 > 0.01F) {
         RenderUtil.a(var13, var5, var14, var6, 4.0F, a(-1, 6.0F * var16 * var7 * var18));
      }

      float var19 = var5 + 8.0F;
      float var20 = var13 + 4.0F;
      float var21 = 17.0F;
      float var22 = 10.0F;
      float var23 = 5.0F;
      float var24 = var13 + var14 - var21 - 4.0F;
      float var25 = var19 - var22 / 2.0F;
      int var26;
      float var27 = (var26 = !var2.getSettings().isEmpty() ? 1 : 0) != 0 ? c() : 0.0F;
      float var28 = var13 + var14 - var27 - 6.0F;
      float var29 = var24 - var27 - 4.0F;
      float var30 = var28 + (var29 - var28) * var15;
      float var31 = (var15 > 0.001F ? var21 + 6.0F : 0.0F) + (var26 != 0 ? var27 + 5.0F : 0.0F);
      float var32 = var13 + var14 - var31 - 2.0F;
      int var33 = var2.getKeybind();
      int var34 = this.listening == var2 || var33 != 0 && var33 != -1 ? 1 : 0;
      String var35 = this.listening == var2 ? "?" : KeyHelper.a(var33);
      float var36 = d();
      float var37 = Msdf.a(MsdfManager.BOLD, var35, 6.0F);
      float var38 = var34 != 0 ? 4.0F + var36 + 2.5F + var37 + 4.0F : 0.0F;
      float var39 = var32 - var20 - (var34 != 0 ? var38 + 4.0F : 0.0F);
      float var40 = 7.25F;
      float var41;
      if ((var41 = a(var2)) > var39 && var39 > 10.0F) {
         var40 = Math.max(4.75F, var40 * (var39 / var41));
      }

      float var42 = var40 == 7.25F ? var41 : Msdf.a(MsdfManager.BOLD, var2.getName(), var40);
      float var43 = var40 == 7.25F ? j() : Msdf.a(MsdfManager.BOLD, var40);
      int var44 = a(-5260861, -1, Math.max(var15, var16 * 0.5F));
      Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var20, var19 - var43 / 2.0F - 0.5F, var40, a(var44, 255.0F * var18 * var7), false);
      this.a(GuiPanel.b.ROW, var13, var5, var13 + var14, var5 + 16.0F, var2, null);
      if (var34 != 0) {
         float var45 = var20 + var42 + 3.0F;
         float var46 = var19 - 4.5F;
         RenderUtil.a(var45, var46, var38, 9.0F, 2.0F, a(var10, 40.0F * var18));
         RenderUtil.a(var45, var46, var38, 9.0F, 2.0F, 0.5F, a(var10, 80.0F * var18));
         Msdf.a(var1, MsdfManager.DICONS, "C", var45 + 3.5F, var19 - e() / 2.0F, 6.0F, a(var10, 255.0F * var7 * var18), false);
         Msdf.a(var1, MsdfManager.BOLD, var35, var45 + 3.5F + var36 + 2.5F, var19 - f() / 2.0F - 0.5F, 6.0F, a(-1, 255.0F * var18), false);
         this.a(GuiPanel.b.BIND, var45, var46, var45 + var38, var46 + 9.0F, var2, null);
      }

      if (var26 != 0) {
         int var50 = this.expandedSet.contains(var2) ? -1 : -8222058;
         float var53 = (140.0F + 80.0F * (this.expandedSet.contains(var2) ? 1.0F : 0.0F)) * var18 * var7;
         Msdf.a(var1, MsdfManager.BOLD, "...", var30, var19 - g() / 2.0F - 2.8F, 10.0F, a(var50, var53), false);
      }

      if (var15 > 0.001F) {
         int var51 = a(-13486527, var10, var15);
         RenderUtil.a(var24, var25, var21, var22, var23, a(var51, 220.0F * var15 * var7 * var18));
         RenderUtil.a(var24, var25, var21, var22, var23, 0.5F, a(var10, 120.0F * var15 * var7 * var18));
         float var54 = 7.6F;
         float var47 = 3.8F;
         float var48 = var24 + 1.2F + (var21 - 2.4F - var54) * var15;
         float var49 = var25 + 1.2F;
         RenderUtil.a(var48, var49, var54, var54, var47, a(-1, 255.0F * var15 * var7 * var18));
      }

      if (var17 > 0.001F) {
         Msdf.c();
         Scissor.a(var13, var5 + 16.0F, var13 + var14, var5 + var6);
         Msdf.b();
         float var52 = var5 + 16.0F + 2.0F;

         for (Setting var56 : var2.getSettings()) {
            if (var56.isVisible()) {
               var52 = this.a(var1, var56, var3, var4, var13, var14, var52, var17, var7, var18, var8, var10, var11);
            }
         }

         Msdf.c();
         Scissor.a();
         Msdf.b();
      }
   }

   private float a(
      DrawContext var1,
      Setting var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      int var12,
      long var13
   ) {
      float var15 = 255.0F * var8 * var9 * var10;
      float var16 = var5 + 4.5F;
      float var17 = var6 - 9.0F;
      if (var2 instanceof BooleanSetting var60) {
         float var66 = var7 + 5.5F;
         float var72 = var17 - 16.0F;
         float var77 = 6.5F;
         float var81;
         if ((var81 = Msdf.a(MsdfManager.BOLD, var2.getName(), var77)) > var72) {
            var77 = Math.max(4.5F, var77 * (var72 / var81));
         }

         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7 + (11.0F - Msdf.a(MsdfManager.BOLD, var77)) / 2.0F - 0.5F, var77, a(-1, var15), false);
         float var85 = var16 + var17 - 11.0F;
         float var90 = var66 - 5.5F;
         BooleanSetting var94 = var60;
         Animation var97 = this.settingAnims.get(var94);
         if (var97 == null) {
            var97 = new Animation();
            var97.setEasing(Easing.SPRING_SOFT);
            this.settingAnims.put(var94, var97);
         }

         var97.animate(var60.isToggled() ? 1.0F : 0.0F, var11, 6.5F, var13);
         float var100 = Math.clamp(var97.getValue(), 0.0F, 1.0F);
         int var103 = a(-14802904, var12, var100);
         RenderUtil.a(var85, var90, 11.0F, 11.0F, 3.0F, a(var103, (25.0F + 195.0F * var100) * (var15 / 255.0F)));
         int var105 = a(-12498859, var12, var100);
         RenderUtil.a(var85, var90, 11.0F, 11.0F, 3.0F, 0.5F, a(var105, var15 * 0.7F));
         if (var100 > 0.01F) {
            float var107 = m();
            float var109 = n();
            Msdf.a(var1, MsdfManager.DICONS, "m", var85 + (11.0F - var107) / 2.0F, var66 - var109 / 2.0F - 0.5F, 7.5F, a(-1, var100 * var15), false);
         }

         if (var8 > 0.3F) {
            this.a(GuiPanel.b.CHECK, var16, var7 - 1.0F, var16 + var17, var7 + 12.0F, var60, null);
         }

         return var7 + 11.0F + 3.0F;
      } else if (var2 instanceof ModeSetting var59) {
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7, 6.5F, a(-1, var15), false);
         return this.a(var1, var59.getModes(), var59::isMode, GuiPanel.b.MODE, var59, var16, var17, var7, var15, var8, var11, var12, var13);
      } else if (var2 instanceof ButtonSetting var58) {
         float var65 = var17;
         RenderUtil.a(var16, var7, var65, 11.0F, 2.5F, a(var12, 180.0F * (var15 / 255.0F)));
         float var71 = Msdf.a(MsdfManager.BOLD, var2.getName(), 6.5F);
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16 + (var65 - var71) / 2.0F, var7 + (11.0F - h()) / 2.0F - 0.5F, 6.5F, a(-1, var15), false);
         if (var8 > 0.3F) {
            this.a(GuiPanel.b.BUTTON, var16, var7, var16 + var65, var7 + 11.0F, var58, null);
         }

         return var7 + 11.0F + 3.0F;
      } else if (var2 instanceof NumberSetting var57) {
         int var64 = typingNumber == var57 ? 1 : 0;
         double var70 = var57.getMax() - var57.getMin();
         NumberSetting var80 = var57;
         Animation var84 = this.settingAnims.get(var80);
         if (var84 == null) {
            var84 = new Animation();
            var84.setEasing(Easing.SPRING_SOFT);
            this.settingAnims.put(var80, var84);
         }

         float var89 = (float)((var57.getValue() - var57.getMin()) / var70);
         if (this.dragging == var57) {
            var84.reset(var89);
         } else {
            var84.animate(var89, var11, 8.0F, var13);
         }

         float var93 = Math.clamp(var84.getValue(), 0.0F, 1.0F);
         String var96 = var57.getFormatter() != null ? var57.getFormatter().apply(var57.getMin() + var70 * var93) : a(var57.getMin() + var70 * var93);
         String var99 = var64 != 0 ? numberInputBuffer.toString() + (var13 / 500L % 2L == 0L ? "_" : "") : var96;
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7 + 0.5F, 6.5F, a(-1, var15), false);
         float var102 = Msdf.a(MsdfManager.BOLD, var64 != 0 ? numberInputBuffer.toString() : var99, 6.25F);
         float var104 = Math.max(16.0F, var102 + 7.0F);
         float var106 = i() + 2.5F;
         float var108 = var16 + var17 - var104;
         float var110 = var7;
         RenderUtil.a(var108, var110, var104, var106, 2.5F, var64 != 0 ? a(var12, 30.0F * (var15 / 255.0F)) : a(var12, 12.0F * (var15 / 255.0F)));
         RenderUtil.a(var108, var110, var104, var106, 2.5F, 0.5F, var64 != 0 ? a(var12, 200.0F * (var15 / 255.0F)) : a(var12, 60.0F * (var15 / 255.0F)));
         float var111 = var108
            + (var104 - Msdf.a(MsdfManager.BOLD, var64 != 0 ? (ClickGui.numberSelectedAll ? numberInputBuffer.toString() : var99) : var99, 6.25F)) / 2.0F;
         float var112 = var110 + (var106 - i()) / 2.0F - 0.5F;
         if (var64 != 0 && ClickGui.numberSelectedAll && !numberInputBuffer.isEmpty()) {
            float var113 = Msdf.a(MsdfManager.BOLD, numberInputBuffer.toString(), 6.25F);
            float var115 = i();
            RenderUtil.a(var111 - 1.0F, var112 - 0.5F, var113 + 2.0F, var115 + 1.0F, 1.5F, a(var12, 140.0F * (var15 / 255.0F)));
            Msdf.a(var1, MsdfManager.BOLD, numberInputBuffer.toString(), var111, var112, 6.25F, a(-1, var15), false);
         } else {
            Msdf.a(var1, MsdfManager.BOLD, var99, var111, var112, 6.25F, var64 != 0 ? a(-1, var15) : a(-1, var15), false);
         }

         float var114 = 2.5F;
         float var116 = var7 + h() + 7.0F;
         RenderUtil.a(var16, var116, var17, var114, 1.25F, a(-13815750, var15 * 0.55F));
         if (var93 > 0.001F) {
            RenderUtil.a(var16, var116, Math.max(var114, var17 * var93), var114, 1.25F, a(var12, var15));
         }

         float var117 = 5.5F;
         float var118 = 2.75F;
         float var119 = var16 + (var17 - var117) * var93;
         float var120 = var116 + (var114 - var117) / 2.0F;
         RenderUtil.a(var119, var120, var117, var117, var118, a(-1, var15));
         if (var8 > 0.3F) {
            this.a(GuiPanel.b.NUMBER_INPUT, var108 - 1.0F, var110 - 1.0F, var108 + var104 + 1.0F, var110 + var106 + 1.0F, var57, null);
            this.a(GuiPanel.b.SLIDER, var16, var116 - 3.0F, var16 + var17, var116 + var114 + 3.0F, var57, new float[]{var16, var17});
         }

         return var7 + 19.0F + 3.0F;
      } else if (var2 instanceof MultiSetting var56) {
         List var63 = var56.getEnabled();
         int var69 = 0;

         for (int var75 = 0; var75 < var63.size(); var75++) {
            if (var56.isOptionEnabled((String)var63.get(var75))) {
               var69++;
            }
         }

         String var76 = var69 + " из " + var63.size();
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7, 6.5F, a(-1, var15), false);
         Msdf.a(var1, MsdfManager.BOLD, var76, var16 + var17 - Msdf.a(MsdfManager.BOLD, var76, 6.5F), var7 + 0.25F, 6.5F, a(-5260861, var15), false);
         return this.a(var1, var63, var56::isOptionEnabled, GuiPanel.b.MULTI_OPT, var56, var16, var17, var7, var15, var8, var11, var12, var13);
      } else if (var2 instanceof BindSetting var55) {
         String var62 = KeyHelper.a(var55.getKey());
         float var68 = var17;
         String var74 = var55 == BindSetting.LISTENING ? "..." : var62;
         float var79;
         float var83 = (var79 = Msdf.a(MsdfManager.BOLD, var74, 6.25F)) + 6.0F;
         float var88 = var68 - var83 - 4.0F;
         float var92 = 6.5F;
         float var95;
         if ((var95 = Msdf.a(MsdfManager.BOLD, var2.getName(), var92)) > var88) {
            var92 = Math.max(4.5F, var92 * (var88 / var95));
         }

         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7 + (11.0F - Msdf.a(MsdfManager.BOLD, var92)) / 2.0F - 0.5F, var92, a(-1, var15), false);
         float var98 = var16 + var17 - var83;
         float var101 = var7 + 1.0F;
         RenderUtil.a(var98, var101, var83, 9.0F, 2.0F, a(var12, 40.0F * (var15 / 255.0F)));
         RenderUtil.a(var98, var101, var83, 9.0F, 2.0F, 0.5F, a(var12, 70.0F * (var15 / 255.0F)));
         Msdf.a(var1, MsdfManager.BOLD, var74, var98 + (var83 - var79) / 2.0F, var7 + (11.0F - i()) / 2.0F - 0.5F, 6.25F, a(-1, var15), false);
         if (var8 > 0.3F) {
            this.a(GuiPanel.b.BIND_SETTING, var98 - 2.0F, var101 - 1.0F, var98 + var83 + 2.0F, var101 + 10.0F, var55, null);
         }

         return var7 + 11.0F + 3.0F;
      } else if (var2 instanceof TextSetting var54) {
         int var61 = TextSetting.LISTENING == var54 ? 1 : 0;
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7 + 0.5F, 6.5F, a(-1, var15), false);
         float var67 = var7 + h() + 3.0F;
         RenderUtil.a(var16, var67, var17, 11.0F, 2.0F, a(-1, Math.round(var15 * 0.08F)));
         RenderUtil.a(var16, var67, var17, 11.0F, 2.0F, 0.5F, var61 != 0 ? a(var12, var15) : a(-1, Math.round(var15 * 0.22F)));
         String var73 = var54.getText();
         float var78 = var16 + 3.0F;
         float var82 = var67 + (11.0F - h()) / 2.0F - 0.5F;
         if (var61 != 0 && ClickGui.textSelectedAll && !var73.isEmpty()) {
            float var87 = Msdf.a(MsdfManager.BOLD, var73, 6.5F);
            float var91 = h();
            RenderUtil.a(var78 - 1.0F, var82 - 0.5F, var87 + 2.0F, var91 + 1.0F, 1.5F, a(var12, 140.0F * (var15 / 255.0F)));
            Msdf.a(var1, MsdfManager.BOLD, var73, var78, var82, 6.5F, a(-1, var15), false);
         } else {
            String var86 = var73 + (var61 != 0 && var13 / 500L % 2L == 0L ? "_" : "");
            Msdf.a(var1, MsdfManager.BOLD, var86, var78, var82, 6.5F, a(-1, var15), false);
         }

         if (var8 > 0.3F) {
            this.a(GuiPanel.b.TEXT_INPUT, var16, var67, var16 + var17, var67 + 11.0F, var54, null);
         }

         return var7 + h() + 14.0F + 3.0F;
      } else if (var2 instanceof ColorSetting var18) {
         float var19 = 6.5F;
         Msdf.a(var1, MsdfManager.BOLD, var2.getName(), var16, var7 + 0.5F, var19, a(-1, var15), false);
         Color var20 = var18.getColor();
         int var21 = var20 != null ? var20.getRGB() : -1;
         float var22 = 14.0F;
         float var23 = 8.0F;
         float var24 = var16 + var17 - var22;
         float var25 = var7;
         RenderUtil.a(var24, var25, var22, var23, 2.0F, a(var21, var15));
         RenderUtil.a(var24, var25, var22, var23, 2.0F, 0.5F, a(-1, 60.0F * (var15 / 255.0F)));
         float var26 = var7 + h() + 4.0F;
         float var27 = 8.0F;
         float var28 = var17 - var27 - 4.0F;
         float var29 = 40.0F;
         float var30 = var16;
         float var31 = var26;
         float var32 = var16 + var28 + 4.0F;
         float var33 = var26;
         float var34 = var29;
         float var35 = var18.getHue();
         float var36 = var18.getSaturation();
         float var37 = var18.getBrightness();
         int var38;
         int var39 = a(var38 = Color.HSBtoRGB(var35, 1.0F, 1.0F), var15);
         int var40 = a(a(var38, -1, 0.5F), var15);
         int var41 = a(-1, var15);
         int var42 = a(a(var38, -16777216, 0.5F), var15);
         int var43 = a(a(a(var38, -1, 0.5F), -16777216, 0.5F), var15);
         int var44 = a(a(-1, -16777216, 0.5F), var15);
         int var45 = a(-16777216, var15);
         int var46 = a(-16777216, var15);
         int var47 = a(-16777216, var15);
         RenderUtil.a(var30, var31, var28, var29, 3.0F, var39, var40, var41, var42, var43, var44, var45, var46, var47);
         RenderUtil.a(var30, var31, var28, var29, 3.0F, 0.5F, a(-1, 35.0F * (var15 / 255.0F)));
         float var48 = var30 + (1.0F - var36) * var28;
         float var49 = var31 + (1.0F - var37) * var29;
         RenderUtil.a(var48 - 2.5F, var49 - 2.5F, 5.0F, 5.0F, 2.5F, 1.0F, a(-1, var15));
         RenderUtil.a(var48 - 3.0F, var49 - 3.0F, 6.0F, 6.0F, 3.0F, 0.5F, a(-16777216, 100.0F * (var15 / 255.0F)));
         BufferBuilder var50 = ColorPipeline.a();
         if (var50 != null) {
            int var51 = (int)Math.clamp(var15, 0.0F, 255.0F);

            for (float var52 = 0.0F; var52 <= var34; var52++) {
               int var53 = Color.HSBtoRGB(var52 / var34, 1.0F, 1.0F) & 16777215 | var51 << 24;
               ColorPipeline.a(var50, var32, var33 + var52, var32 + var27, var33 + var52, 1.0F, var53);
            }

            ColorPipeline.a(var50, false);
         }

         RenderUtil.a(var32, var33, var27, var34, 2.0F, 0.5F, a(-1, 40.0F * (var15 / 255.0F)));
         float var121 = var33 + Math.clamp(var35, 0.0F, 1.0F) * var34;
         RenderUtil.a(var32 - 1.0F, var121 - 1.0F, var27 + 2.0F, 2.0F, 1.0F, a(-1, var15));
         RenderUtil.a(var32 - 1.0F, var121 - 1.0F, var27 + 2.0F, 2.0F, 1.0F, 0.5F, a(-16777216, 90.0F * (var15 / 255.0F)));
         if (var8 > 0.3F) {
            this.a(GuiPanel.b.COLOR_SV, var30, var31, var30 + var28, var31 + var29, var18, new float[]{var30, var31, var28, var29});
            this.a(GuiPanel.b.COLOR_HUE, var32 - 2.0F, var33, var32 + var27 + 2.0F, var33 + var34, var18, new float[]{var32, var33, var27, var34});
         }

         return var31 + var29 + 3.0F;
      } else {
         return var7;
      }
   }

   private float a(
      DrawContext var1,
      List<String> var2,
      Predicate<String> var3,
      GuiPanel.b var4,
      Object var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      int var12,
      long var13
   ) {
      float var15 = 0.0F;
      float var16 = var8 + h() + 5.0F;
      Map var17 = this.chipAnims.computeIfAbsent(var5, var0 -> new HashMap<>());

      for (String var19 : var2) {
         float var20 = b(var19);
         if (var15 + var20 > var7) {
            var15 = 0.0F;
            var16 += 12.0F;
         }

         float var21 = var6 + var15;
         Animation var22 = (Animation)var17.get(var19);
         if (var22 == null) {
            var22 = new Animation();
            var22.setEasing(Easing.SPRING);
            var17.put(var19, var22);
         }

         var22.animate(var3.test(var19) ? 1.0F : 0.0F, var11, 6.0F, var13);
         float var23 = var22.getValue();
         RenderUtil.a(var21, var16, var20, 9.0F, 2.0F, a(var12, (5.0F + 65.0F * var23) * var9 / 255.0F));
         RenderUtil.a(var21, var16, var20, 9.0F, 2.0F, 0.5F, a(-12827568, var9 * 0.5F));
         Msdf.a(var1, MsdfManager.BOLD, var19, var21 + 3.0F, var16 + (9.0F - i()) / 2.0F - 0.75F, 6.25F, a(a(-5260861, -1, var23), var9), false);
         if (var10 > 0.3F) {
            this.a(var4, var21, var16, var21 + var20, var16 + 9.0F, var5, var19);
         }

         var15 += var20 + 3.0F;
      }

      return var16 + 9.0F + 3.0F;
   }

   public boolean a(float var1, float var2, int var3, ClickGui var4) {
      if (this.open.getValue() < 0.2F) {
         return false;
      }

      List var5 = this.b();
      float var6 = this.a(var5);
      float var7 = this.panelH;
      float var8 = this.y + 24.0F + 2.0F;
      float var9 = var7 - 24.0F - 6.0F;
      if (var6 > var9) {
         float var11 = this.x + this.panelW - 3.5F;
         float var12 = var8 + 2.0F;
         float var13 = var9 - 4.0F;
         float var14 = var9 / var6;
         float var15 = Math.max(16.0F, var13 * var14);
         float var16;
         float var17 = (var16 = var6 - var9) > 0.0F ? Math.clamp(-this.scrollTarget / var16, 0.0F, 1.0F) : 0.0F;
         float var18 = var12 + (var13 - var15) * var17;
         if (var1 >= var11 - 6.0F && var1 <= this.x + this.panelW + 6.0F && var2 >= var12 && var2 <= var12 + var13) {
            if (var2 >= var18 && var2 <= var18 + var15) {
               this.draggingScroll = true;
               this.dragScrollStartY = var2;
               this.dragScrollStartTarget = this.scrollTarget;
               return true;
            }

            float var19 = Math.clamp((var2 - var12 - var15 / 2.0F) / (var13 - var15), 0.0F, 1.0F);
            this.scrollTarget = -var19 * var16;
            this.draggingScroll = true;
            this.dragScrollStartY = var2;
            this.dragScrollStartTarget = this.scrollTarget;
            return true;
         }
      }

      for (int var10 = this.hitCount - 1; var10 >= 0; var10--) {
         GuiPanel.a var21 = this.hitPool.get(var10);
         if (this.a(var1, var2, var21)) {
            switch (var21.getKind()) {
               case ROW:
                  a();
                  Function var26 = (Function)var21.getOwner();
                  switch (var3) {
                     case 0:
                        var26.toggle();
                        break;
                     case 1:
                        if (!this.expandedSet.remove(var26)) {
                           this.expandedSet.add(var26);
                        }
                        break;
                     case 2:
                        var4.a(this, var26);
                  }

                  return true;
               case CHECK:
                  Object var20;
                  ((BooleanSetting)(var20 = var21.getOwner())).applyValue(!((BooleanSetting)var20).isToggled());
                  return true;
               case MODE:
                  ((ModeSetting)var21.getOwner()).setMode((String)var21.getData());
                  return true;
               case MULTI_OPT:
                  ((MultiSetting)var21.getOwner()).toggleOption((String)var21.getData());
                  return true;
               case SLIDER:
                  a();
                  NumberSetting var25 = (NumberSetting)var21.getOwner();
                  if (var3 == 2) {
                     var25.resetToDefault();
                     return true;
                  }

                  if (var3 == 0) {
                     float[] var28 = (float[])var21.getData();
                     this.dragging = var25;
                     this.dragMin = var28[0];
                     this.dragMax = var28[0] + var28[1];
                     this.a(var1);
                     return true;
                  }

                  return true;
               case NUMBER_INPUT:
                  NumberSetting var24 = (NumberSetting)var21.getOwner();
                  if (var3 == 0) {
                     NumberSetting var27 = typingNumber;
                     a();
                     if (var27 != var24) {
                        typingNumber = var24;
                        numberInputBuffer.setLength(0);
                        numberInputBuffer.append(a(var24.getValue()));
                     }

                     return true;
                  }

                  return true;
               case BIND:
                  a();
                  var4.a(this, (Function)var21.getOwner());
                  return true;
               case BIND_SETTING:
                  a();
                  BindSetting var23 = BindSetting.LISTENING;
                  BindSetting.LISTENING = null;
                  if (var23 != var21.getOwner()) {
                     BindSetting.LISTENING = (BindSetting)var21.getOwner();
                  }

                  return true;
               case TEXT_INPUT:
                  a();
                  TextSetting var22 = TextSetting.LISTENING;
                  TextSetting.LISTENING = null;
                  if (var22 != var21.getOwner()) {
                     TextSetting.LISTENING = (TextSetting)var21.getOwner();
                  }

                  return true;
               case BUTTON:
                  ((ButtonSetting)var21.getOwner()).getAction().run();
                  return true;
               case COLOR_SV:
                  a();
                  if (var3 == 0) {
                     this.draggingColor = (ColorSetting)var21.getOwner();
                     this.colorDragMode = 1;
                     this.colorBounds = (float[])var21.getData();
                     this.c(var1, var2);
                     return true;
                  }

                  return true;
               case COLOR_HUE:
                  a();
                  if (var3 == 0) {
                     this.draggingColor = (ColorSetting)var21.getOwner();
                     this.colorDragMode = 2;
                     this.colorBounds = (float[])var21.getData();
                     this.c(var1, var2);
                     return true;
                  }

                  return true;
            }
         }
      }

      return false;
   }

   public boolean a(float var1, float var2, int var3) {
      if (this.open.getValue() < 0.2F) {
         return false;
      } else if (this.draggingScroll) {
         this.draggingScroll = false;
         return true;
      } else if (this.draggingColor != null) {
         this.c(var1, var2);
         this.draggingColor = null;
         return true;
      } else if (this.dragging != null) {
         this.a(var1);
         this.dragging = null;
         return true;
      } else {
         return false;
      }
   }

   public boolean b(float var1, float var2, int var3) {
      if (this.open.getValue() < 0.2F) {
         return false;
      }

      if (this.draggingScroll) {
         List var4 = this.b();
         float var5 = this.a(var4);
         float var6 = this.panelH;
         float var7 = this.panelH - 30.0F;
         float var8 = var7 - 4.0F;
         float var9 = var7 / var5;
         float var10 = Math.max(16.0F, var8 * var9);
         float var11 = var5 - var7;
         float var12 = var2 - this.dragScrollStartY;
         float var13;
         if ((var13 = var8 - var10) > 0.0F) {
            this.scrollTarget = Math.clamp(this.dragScrollStartTarget - var12 / var13 * var11, -var11, 0.0F);
         }

         return true;
      } else if (this.draggingColor != null) {
         this.c(var1, var2);
         return true;
      } else if (this.dragging != null) {
         this.a(var1);
         return true;
      } else {
         return false;
      }
   }

   private void c(float var1, float var2) {
      if (this.draggingColor != null && this.colorBounds != null) {
         if (this.colorDragMode == 1) {
            float var3 = this.colorBounds[0];
            float var4 = this.colorBounds[1];
            float var5 = this.colorBounds[2];
            float var6 = this.colorBounds[3];
            float var7 = Math.clamp(1.0F - (var1 - var3) / var5, 0.0F, 1.0F);
            float var8 = Math.clamp(1.0F - (var2 - var4) / var6, 0.0F, 1.0F);
            this.draggingColor.setSaturationAndBrightness(var7, var8);
         } else if (this.colorDragMode == 2) {
            float var9 = this.colorBounds[1];
            float var10 = this.colorBounds[3];
            float var11 = Math.clamp((var2 - var9) / var10, 0.0F, 1.0F);
            if (this.draggingColor.getSaturation() <= 0.001F) {
               this.draggingColor.setSaturation(1.0F);
            }

            if (this.draggingColor.getBrightness() <= 0.001F) {
               this.draggingColor.setBrightness(1.0F);
            }

            this.draggingColor.setHue(var11);
         }
      }
   }

   private void a(float var1) {
      float var2 = Math.clamp((var1 - this.dragMin) / (this.dragMax - this.dragMin), 0.0F, 1.0F);
      double var3 = Math.round((this.dragging.getMin() + (this.dragging.getMax() - this.dragging.getMin()) * var2) / this.dragging.getStep())
         * this.dragging.getStep();
      this.dragging.setValue(var3);
      Animation var5 = this.settingAnims.get(this.dragging);
      if (var5 != null) {
         var5.animate(1.0F, 0.016F, 10.0F);
      }
   }

   public boolean a(float var1, float var2, double var3) {
      if (this.open.getValue() < 0.2F) {
         return false;
      }

      if (!this.a(var1, var2)) {
         return false;
      }

      this.scrollTarget += (float)var3 * 35.0F;
      return true;
   }

   public boolean a(float var1, float var2) {
      if (this.open.getValue() < 0.2F) {
         return false;
      }

      List var3 = this.b();
      this.a(var3);
      float var5 = this.panelH;
      return var1 >= this.x && var1 <= this.x + this.panelW && var2 >= this.y && var2 <= this.y + var5;
   }

   public Function b(float var1, float var2) {
      for (int var3 = this.hitCount - 1; var3 >= 0; var3--) {
         GuiPanel.a var4 = this.hitPool.get(var3);
         if (var4.getKind() == GuiPanel.b.ROW && this.a(var1, var2, var4)) {
            return (Function)var4.getOwner();
         }
      }

      return null;
   }

   private boolean a(float var1, float var2, GuiPanel.a var3) {
      return var1 >= var3.getX1() && var1 <= var3.getX2() && var2 >= var3.getY1() && var2 <= var3.getY2();
   }

   public List<Function> b() {
      return this.a("");
   }

   public List<Function> a(String var1) {
      String var2 = var1 != null ? var1.trim().toLowerCase() : "";
      if (this.cachedModules != null && var2.equals(this.lastSearchQuery)) {
         return this.cachedModules;
      }

      List<Function> var3 = new ArrayList<>(FunctionManager.a(this.category));
      if (!var2.isEmpty()) {
         var3.removeIf(var1x -> !var1x.getName().toLowerCase().contains(var2));
      }

      var3.sort((var0, var1x) -> var0.getName().compareToIgnoreCase(var1x.getName()));
      this.lastSearchQuery = var2;
      this.cachedModules = var3;
      return this.cachedModules;
   }

   private float a(List<Function> var1) {
      if (var1.isEmpty()) {
         return 0.0F;
      }

      float var2 = 0.0F;

      for (Function var4 : var1) {
         var2 += this.b(var4) + 3.0F;
      }

      return Math.max(0.0F, var2 - 3.0F + 8.0F);
   }

   private float b(Function var1) {
      Animation var2 = this.expands.get(var1);
      float var3;
      if ((var3 = var2 != null ? var2.getValue() : (this.expandedSet.contains(var1) ? 1.0F : 0.0F)) <= 0.001F) {
         return 16.0F;
      }

      float var4 = 0.0F;
      byte var5 = 0;

      for (Setting var7 : var1.getSettings()) {
         if (var7.isVisible()) {
            var5 = 1;
            if (var7 instanceof ModeSetting var8) {
               var4 += this.b(var8.getModes());
            } else if (var7 instanceof MultiSetting var9) {
               var4 += this.b(var9.getEnabled());
            } else if (var7 instanceof NumberSetting) {
               var4 += 22.0F;
            } else if (var7 instanceof ButtonSetting) {
               var4 += 14.0F;
            } else if (var7 instanceof BindSetting) {
               var4 += 14.0F;
            } else if (var7 instanceof TextSetting) {
               var4 += h() + 14.0F + 3.0F;
            } else if (var7 instanceof ColorSetting) {
               var4 += h() + 44.0F + 3.0F;
            } else {
               var4 += 14.0F;
            }
         }
      }

      if (var5 != 0) {
         var4 += 4.0F;
      }

      return 16.0F + var4 * var3;
   }

   private float b(List<String> var1) {
      float var2 = 104.0F;
      float var3 = 0.0F;
      float var4 = h() + 5.0F;

      for (String var6 : var1) {
         float var7 = b(var6);
         if (var3 + var7 > var2) {
            var3 = 0.0F;
            var4 += 12.0F;
         }

         var3 += var7 + 3.0F;
      }

      return var4 + 9.0F + 3.0F;
   }

   private static String a(double var0) {
      return var0 == Math.floor(var0) ? String.valueOf((long)var0) : String.format("%.1f", var0);
   }

   static final class a {
      GuiPanel.b kind;
      float x1;
      float y1;
      float x2;
      float y2;
      Object owner;
      Object data;

      void a(GuiPanel.b var1, float var2, float var3, float var4, float var5, Object var6, Object var7) {
         this.kind = var1;
         this.x1 = var2;
         this.y1 = var3;
         this.x2 = var4;
         this.y2 = var5;
         this.owner = var6;
         this.data = var7;
      }

      GuiPanel.b getKind() {
         return this.kind;
      }

      float getX1() {
         return this.x1;
      }

      float getY1() {
         return this.y1;
      }

      float getX2() {
         return this.x2;
      }

      float getY2() {
         return this.y2;
      }

      Object getOwner() {
         return this.owner;
      }

      Object getData() {
         return this.data;
      }
   }

   enum b {
      ROW,
      CHECK,
      MODE,
      MULTI_OPT,
      SLIDER,
      NUMBER_INPUT,
      BIND,
      BIND_SETTING,
      TEXT_INPUT,
      BUTTON,
      COLOR_SV,
      COLOR_HUE;
   }
}
