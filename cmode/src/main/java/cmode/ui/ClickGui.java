package cmode.ui;

import cmode.font.Msdf;
import cmode.font.MsdfManager;
import cmode.module.Function;
import cmode.module.FunctionManager;
import cmode.render.GuiBatch;
import cmode.render.RenderUtil;
import cmode.render.Theme;
import cmode.setting.BooleanSetting;
import cmode.setting.ModeSetting;
import cmode.setting.NumberSetting;
import cmode.setting.Setting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public final class ClickGui extends Screen {
   public static final ClickGui INSTANCE = new ClickGui();
   private static final float W = 125.0F;
   private static final float HEADER = 24.0F;
   private static final float PAD = 6.0F;
   private static final float INNER = 113.0F;
   private static final float ROW_H = 16.0F;
   private static final float GAP = 3.0F;
   private static final int KIND_MODULE = 0;
   private static final int KIND_BOOL = 1;
   private static final int KIND_NUMBER = 2;
   private static final int KIND_MODE = 3;
   private final List<ClickGui.Row> items = new ArrayList<>();
   private final List<ClickGui.Row> drawn = new ArrayList<>();
   private final Set<Function> expanded = new HashSet<>();
   private float panelX;
   private float panelY;
   private float panelH;
   private NumberSetting slider;

   private ClickGui() {
      super(Text.literal("cmode"));
   }

   private static final class Row {
      final int kind;
      final Function owner;
      final Setting setting;
      float x;
      float y;
      float w;
      float h;

      Row(int var1, Function var2, Setting var3) {
         this.kind = var1;
         this.owner = var2;
         this.setting = var3;
      }

      boolean contains(float var1, float var2) {
         return var1 >= this.x && var1 <= this.x + this.w && var2 >= this.y && var2 <= this.y + this.h;
      }
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      MsdfManager.a();
      GuiBatch.begin();

      try {
         this.draw(var2, var3);
      } finally {
         GuiBatch.end();
      }
   }

   private void draw(int var1, int var2) {
      float var3 = RenderUtil.a();
      float var4 = RenderUtil.b();
      float var5 = var1 * RenderUtil.e();
      float var6 = var2 * RenderUtil.e();
      RenderUtil.a(0.0F, 0.0F, var3, var4, 0.0F, 102 << 24);
      this.build();
      float var7 = 0.0F;

      for (ClickGui.Row var9 : this.items) {
         var7 += var9.h;
      }

      if (!this.items.isEmpty()) {
         var7 += GAP * (this.items.size() - 1);
      }

      this.panelH = Math.min(var4 - 30.0F, HEADER + 4.0F + var7 + PAD);
      this.panelX = (var3 - W) / 2.0F;
      this.panelY = (var4 - this.panelH) / 2.0F;
      RenderUtil.a(this.panelX, this.panelY, W, this.panelH, 8.0F, 0xF0141614);
      RenderUtil.a(this.panelX, this.panelY, W, this.panelH, 8.0F, 1.0F, 0x2EFFFFFF);
      Matrix4f var8 = RenderUtil.c();
      int var9 = Theme.getInstance().getAccent().getRGB();
      this.drawn.clear();
      Msdf.b();
      float var10 = Msdf.a(MsdfManager.BOLD, 9.0F);
      Msdf.a(var8, MsdfManager.BOLD, "cmode", this.panelX + 10.0F, this.panelY + (HEADER - var10) / 2.0F, 9.0F, var9);
      float var11 = this.panelY + HEADER + 2.0F;

      for (ClickGui.Row var13 : this.items) {
         if (var11 + var13.h <= this.panelY + this.panelH) {
            var13.x = this.panelX + PAD;
            var13.y = var11;
            var13.w = INNER;
            this.drawn.add(var13);
            boolean var14 = var13.contains(var5, var6);
            this.drawRow(var8, var13, var14, var9);
         }

         var11 += var13.h + GAP;
      }

      Msdf.c();
   }

   private void build() {
      this.items.clear();

      for (Function var2 : FunctionManager.getFunctions()) {
         ClickGui.Row var3 = new ClickGui.Row(KIND_MODULE, var2, null);
         var3.h = ROW_H;
         this.items.add(var3);
         if (this.expanded.contains(var2)) {
            for (Setting var5 : var2.getSettings()) {
               if (!var5.isVisible()) {
                  continue;
               }

               ClickGui.Row var6;
               if (var5 instanceof NumberSetting) {
                  var6 = new ClickGui.Row(KIND_NUMBER, var2, var5);
                  var6.h = 22.0F;
               } else if (var5 instanceof ModeSetting) {
                  var6 = new ClickGui.Row(KIND_MODE, var2, var5);
                  var6.h = 14.0F;
               } else if (var5 instanceof BooleanSetting) {
                  var6 = new ClickGui.Row(KIND_BOOL, var2, var5);
                  var6.h = 14.0F;
               } else {
                  continue;
               }

               this.items.add(var6);
            }
         }
      }
   }

   private void drawRow(Matrix4f var1, ClickGui.Row var2, boolean var3, int var4) {
      switch (var2.kind) {
         case KIND_MODULE:
            this.drawModule(var1, var2, var3, var4);
            break;
         case KIND_BOOL:
            this.drawBoolean(var1, var2, var3);
            break;
         case KIND_NUMBER:
            this.drawNumber(var1, var2, var3, var4);
            break;
         case KIND_MODE:
            this.drawMode(var1, var2, var3);
      }
   }

   private void drawModule(Matrix4f var1, ClickGui.Row var2, boolean var3, int var4) {
      Function var5 = var2.owner;
      boolean var6 = var5.isEnabled();
      int var7 = var6 ? 0x46000000 | var4 & 16777215 : 0x16FFFFFF;
      if (var3) {
         var7 = var6 ? 0x5A000000 | var4 & 16777215 : 0x24FFFFFF;
      }

      RenderUtil.a(var2.x, var2.y, var2.w, var2.h, 4.0F, var7);
      float var8 = 17.0F;
      float var9 = 10.0F;
      float var10 = var2.x + var2.w - var8 - 4.0F;
      float var11 = var2.y + 8.0F - var9 / 2.0F;
      boolean var12 = !var5.getSettings().isEmpty();
      float var13 = var10 - 8.0F;
      float var14 = 7.25F;
      float var15 = Msdf.a(MsdfManager.BOLD, var5.getName(), var14);
      float var16 = var13 - (var2.x + 8.0F) - (var12 ? 14.0F : 0.0F);
      if (var15 > var16 && var16 > 10.0F) {
         var14 = Math.max(4.75F, var14 * (var16 / var15));
         var15 = Msdf.a(MsdfManager.BOLD, var5.getName(), var14);
      }

      float var17 = Msdf.a(MsdfManager.BOLD, var14);
      int var18 = var6 ? -1 : 0xFFA8B2A8;
      Msdf.a(var1, MsdfManager.BOLD, var5.getName(), var2.x + 8.0F, var2.y + (ROW_H - var17) / 2.0F - 0.5F, var14, var18);
      if (var12) {
         String var19 = "...";
         float var20 = Msdf.a(MsdfManager.BOLD, var19, 10.0F);
         float var21 = Msdf.a(MsdfManager.BOLD, 10.0F);
         int var22 = this.expanded.contains(var5) ? -1 : 0xFF8A918A;
         Msdf.a(var1, MsdfManager.BOLD, var19, var13 - var20, var2.y + (ROW_H - var21) / 2.0F - 3.3F, 10.0F, var22);
      }

      RenderUtil.a(var10, var11, var8, var9, 5.0F, var6 ? var4 : 0x33FFFFFF);
      RenderUtil.a(var10, var11, var8, var9, 5.0F, 0.5F, 0x40FFFFFF);
      float var23 = 7.6F;
      float var24 = var10 + 1.2F + (var8 - 2.4F - var23) * (var6 ? 1.0F : 0.0F);
      RenderUtil.a(var24, var11 + 1.2F, var23, var23, 3.8F, -1);
   }

   private void drawBoolean(Matrix4f var1, ClickGui.Row var2, boolean var3) {
      BooleanSetting var4 = (BooleanSetting)var2.setting;
      boolean var5 = var4.isToggled();
      if (var3) {
         RenderUtil.a(var2.x, var2.y, var2.w, var2.h, 3.0F, 0x14FFFFFF);
      }

      float var6 = var2.x + 4.5F;
      float var7 = Msdf.a(MsdfManager.BOLD, 6.5F);
      Msdf.a(var1, MsdfManager.BOLD, var4.getName(), var6, var2.y + (11.0F - var7) / 2.0F - 0.5F, 6.5F, -1);
      float var8 = var2.x + var2.w - 4.5F - 11.0F;
      float var9 = var2.y + 1.5F;
      RenderUtil.a(var8, var9, 11.0F, 11.0F, 3.0F, var5 ? 0xFF9AA39A : 0x33FFFFFF);
      RenderUtil.a(var8, var9, 11.0F, 11.0F, 3.0F, 0.5F, var5 ? 0xBFFFFFFF : 0x40FFFFFF);
      if (var5) {
         String var10 = "m";
         float var11 = Msdf.a(MsdfManager.DICONS, var10, 7.5F);
         float var12 = Msdf.a(MsdfManager.DICONS, 7.5F);
         Msdf.a(var1, MsdfManager.DICONS, var10, var8 + (11.0F - var11) / 2.0F, var9 + (11.0F - var12) / 2.0F - 0.5F, 7.5F, -1);
      }
   }

   private void drawMode(Matrix4f var1, ClickGui.Row var2, boolean var3) {
      ModeSetting var4 = (ModeSetting)var2.setting;
      if (var3) {
         RenderUtil.a(var2.x, var2.y, var2.w, var2.h, 3.0F, 0x14FFFFFF);
      }

      float var5 = var2.x + 4.5F;
      float var6 = Msdf.a(MsdfManager.BOLD, 6.5F);
      Msdf.a(var1, MsdfManager.BOLD, var4.getName(), var5, var2.y + (11.0F - var6) / 2.0F - 0.5F, 6.5F, -1);
      String var7 = var4.getActiveMode();
      float var8 = Msdf.a(MsdfManager.BOLD, var7, 6.25F);
      float var9 = var5 + (var2.w - 9.0F) - var8;
      Msdf.a(var1, MsdfManager.BOLD, var7, var9, var2.y + (11.0F - Msdf.a(MsdfManager.BOLD, 6.25F)) / 2.0F - 0.5F, 6.25F, 0xFFB4BCB4);
   }

   private void drawNumber(Matrix4f var1, ClickGui.Row var2, boolean var3, int var4) {
      NumberSetting var5 = (NumberSetting)var2.setting;
      if (var3) {
         RenderUtil.a(var2.x, var2.y, var2.w, var2.h, 3.0F, 0x14FFFFFF);
      }

      float var6 = var2.x + 4.5F;
      float var7 = var2.w - 9.0F;
      float var8 = Msdf.a(MsdfManager.BOLD, 6.5F);
      Msdf.a(var1, MsdfManager.BOLD, var5.getName(), var6, var2.y + 0.5F, 6.5F, -1);
      String var9 = a(var5);
      float var10 = Msdf.a(MsdfManager.BOLD, var9, 6.25F);
      float var11 = Math.max(16.0F, var10 + 7.0F);
      float var12 = Msdf.a(MsdfManager.BOLD, 6.25F);
      float var13 = var6 + var7 - var11;
      float var14 = var2.y + 0.25F;
      float var15 = var12 + 3.0F;
      RenderUtil.a(var13, var14, var11, var15, 2.5F, 0x1FFFFFFF);
      RenderUtil.a(var13, var14, var11, var15, 2.5F, 0.5F, 0x2EFFFFFF);
      Msdf.a(var1, MsdfManager.BOLD, var9, var13 + (var11 - var10) / 2.0F, var14 + (var15 - var12) / 2.0F - 0.5F, 6.25F, -1);
      float var16 = (float)((var5.getValue() - var5.getMin()) / (var5.getMax() - var5.getMin()));
      float var17 = var2.y + var8 + 7.0F;
      float var18 = 2.5F;
      RenderUtil.a(var6, var17, var7, var18, 1.25F, 0x8CFFFFFF);
      if (var16 > 0.001F) {
         RenderUtil.a(var6, var17, Math.max(var18, var7 * var16), var18, 1.25F, 0xE6000000 | var4 & 16777215);
      }

      float var19 = 5.5F;
      float var20 = var6 + (var7 - var19) * var16;
      RenderUtil.a(var20, var17 + (var18 - var19) / 2.0F, var19, var19, 2.75F, -1);
   }

   private static String a(NumberSetting var0) {
      double var1 = var0.getValue();
      if (var0.getStep() >= 1.0) {
         return Long.toString(Math.round(var1));
      } else {
         return String.format(Locale.ROOT, var0.getStep() >= 0.1 ? "%.1f" : "%.2f", var1);
      }
   }

   @Override
   public boolean mouseClicked(Click var1, boolean var2) {
      float var3 = (float)(var1.x() * RenderUtil.e());
      float var4 = (float)(var1.y() * RenderUtil.e());
      if (var3 < this.panelX || var3 > this.panelX + W || var4 < this.panelY || var4 > this.panelY + this.panelH) {
         return true;
      }

      for (int var5 = this.drawn.size() - 1; var5 >= 0; var5--) {
         ClickGui.Row var6 = this.drawn.get(var5);
         if (!var6.contains(var3, var4)) {
            continue;
         }

         switch (var6.kind) {
            case KIND_MODULE:
               if (var1.button() == 0) {
                  var6.owner.toggle();
               } else if (var1.button() == 1 && !var6.owner.getSettings().isEmpty()) {
                  if (!this.expanded.remove(var6.owner)) {
                     this.expanded.add(var6.owner);
                  }
               }
               break;
            case KIND_BOOL:
               if (var1.button() == 0) {
                  BooleanSetting var7 = (BooleanSetting)var6.setting;
                  var7.applyValue(!var7.isToggled());
               }
               break;
            case KIND_NUMBER:
               if (var1.button() == 0) {
                  this.slider = (NumberSetting)var6.setting;
                  this.a(this.slider, var6, var3);
               }
               break;
            case KIND_MODE:
               if (var1.button() == 0) {
                  ModeSetting var8 = (ModeSetting)var6.setting;
                  List<String> var9 = var8.getModes();
                  int var10 = var9.indexOf(var8.getActiveMode());
                  var8.setMode(var9.get((var10 + 1) % var9.size()));
               }
         }

         return true;
      }

      return true;
   }

   private void a(NumberSetting var1, ClickGui.Row var2, float var3) {
      float var4 = var2.x + 4.5F;
      float var5 = var2.w - 9.0F;
      float var6 = Math.clamp((var3 - var4) / var5, 0.0F, 1.0F);
      double var7 = var1.getMin() + (var1.getMax() - var1.getMin()) * var6;
      double var9 = Math.round(var7 / var1.getStep()) * var1.getStep();
      var1.setValue(var9);
   }

   @Override
   public boolean mouseDragged(Click var1, double var2, double var4) {
      if (this.slider != null) {
         for (ClickGui.Row var6 : this.drawn) {
            if (var6.kind == KIND_NUMBER && var6.setting == this.slider) {
               this.a(this.slider, var6, (float)(var1.x() * RenderUtil.e()));
               break;
            }
         }
      }

      return true;
   }

   @Override
   public boolean mouseReleased(Click var1) {
      this.slider = null;
      return true;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext var1, int var2, int var3, float var4) {
   }
}
