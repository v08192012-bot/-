package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.TextSetting;
import brokke.cc.system.events.EventManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Клик-гуи в стиле CS: одно окно, слева логотип, название клиента и список категорий,
 * сверху профиль и поиск, в контенте — модули текущей категории, снизу — ник сессии.
 *
 * Модули и настройки рисует обычный {@link GuiPanel} (у него теперь настраиваемые
 * размеры), поэтому виджеты, драг слайдеров, пикеры цвета и бинды работают так же,
 * как в классической гуи — эта гуи только раскладывает их внутри окна.
 *
 * Категории: Main (домашняя страница), Combat, Movement, Visuals, Player, Misc,
 * плюс Theme (акцент, режим клик-гуи, стиль фона).
 */
public class CsClickGui extends Screen {
   private static final float WIN_W = 560.0F;
   private static final float WIN_H = 360.0F;
   private static final float SIDE_W = 148.0F;
   private static final float TOP_H = 44.0F;
   private static final float PAD = 14.0F;
   private static final float ITEM_H = 26.0F;
   private static final float ITEM_GAP = 2.0F;
   private static final float SEARCH_W = 150.0F;
   private static final float SEARCH_H = 22.0F;
   private static final Identifier LOGO_TEX = Identifier.of("brokke", "textures/watermark_icon.png");

   private static final int PAGE_MAIN = 0;
   private static final int PAGE_THEME = 6;
   private static final int PAGE_COUNT = 7;
   private static final String[] PAGE_NAMES = {"Main", "Combat", "Movement", "Visuals", "Player", "Misc", "Theme"};
   private static final Category[] PAGE_CATEGORIES = {
      null, Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC, null
   };

   private final List<GuiPanel> panels = new ArrayList<>();
   private final Animation openAnim = new Animation();
   private final Animation[] hovers = new Animation[PAGE_COUNT];
   private final StringBuilder searchBuffer = new StringBuilder();
   private int page = PAGE_MAIN;
   private boolean closing;
   private boolean searchFocused;
   public Screen parentScreen;

   private float winX;
   private float winY;
   private float winW = WIN_W;
   private float winH = WIN_H;
   private float contentX;
   private float contentY;
   private float contentW;
   private float contentH;

   public CsClickGui() {
      super(Text.of("Shelka new"));
      for (Category var2 : Category.values()) {
         GuiPanel var3 = new GuiPanel(var2);
         if (var2 == Category.RENDER) {
            var3.title = "Visuals";
         }

         this.panels.add(var3);
      }

      this.openAnim.setEasing(Easing.SPRING_SOFT);
      for (int var4 = 0; var4 < PAGE_COUNT; var4++) {
         this.hovers[var4] = new Animation();
         this.hovers[var4].setEasing(Easing.SMOOTH_STEP);
      }
   }

   // ---------------------------------------------------------------- lifecycle

   /** Вызывается при открытии: сбрасываем анимации и состояние. */
   public void n() {
      this.closing = false;
      this.openAnim.reset(0.0F);
      this.searchBuffer.setLength(0);
      this.searchFocused = false;
      this.page = PAGE_MAIN;

      for (GuiPanel var2 : this.panels) {
         var2.open.reset(0.0F);
         var2.listening = null;
      }

      GuiPanel.a();
      TextSetting.LISTENING = null;
      BindSetting.LISTENING = null;
   }

   public boolean isClosing() {
      return this.closing;
   }

   @Override
   public void close() {
      if (this.closing) {
         this.q();
      } else {
         this.closing = true;
         this.searchFocused = false;
         this.searchBuffer.setLength(0);
         for (GuiPanel var2 : this.panels) {
            var2.listening = null;
         }

         GuiPanel.a();
         TextSetting.LISTENING = null;
         BindSetting.LISTENING = null;
      }
   }

   /** Завершает закрытие: возвращает родительский экран (или в игру). */
   public void q() {
      this.closing = false;
      Screen var1 = this.parentScreen;
      this.parentScreen = null;
      MinecraftClient.getInstance().setScreen(var1);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   // ---------------------------------------------------------------- layout

   private void layout() {
      float var1 = RenderUtil.a();
      float var2 = RenderUtil.b();
      this.winW = Math.min(WIN_W, Math.max(380.0F, var1 - 24.0F));
      this.winH = Math.min(WIN_H, Math.max(240.0F, var2 - 24.0F));
      this.winX = (var1 - this.winW) / 2.0F;
      this.winY = (var2 - this.winH) / 2.0F;
      this.contentX = this.winX + SIDE_W;
      this.contentY = this.winY + TOP_H;
      this.contentW = this.winW - SIDE_W;
      this.contentH = this.winH - TOP_H;
   }

   private float itemY(int var1) {
      return this.winY + 62.0F + var1 * (ITEM_H + ITEM_GAP) + (var1 >= 6 ? 16.0F : 0.0F);
   }

   private float searchX() {
      return this.winX + this.winW - PAD - SEARCH_W;
   }

   private float searchY() {
      return this.winY + (TOP_H - SEARCH_H) / 2.0F;
   }

   private GuiPanel activePanel() {
      Category var1 = PAGE_CATEGORIES[this.page];
      if (var1 == null) {
         return null;
      }

      for (GuiPanel var3 : this.panels) {
         if (var3.category == var1) {
            return var3;
         }
      }

      return null;
   }

   // ---------------------------------------------------------------- render

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.layout();
      this.openAnim.animate(this.closing ? 0.0F : 1.0F, deltaTicks, 6.0F);
      float var5 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (this.closing && var5 <= 0.01F) {
         this.q();
         return;
      }

      MsdfManager.a();
      GuiBatch.begin();
      try {
         this.renderContent(context, mouseX, mouseY, deltaTicks, var5);
      } finally {
         GuiBatch.end();
      }
   }

   private void renderContent(DrawContext var1, int var2, int var3, float var4, float var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      int var7 = var6.world != null ? (int)(140.0F * var5) << 24 : -16118766;
      var1.fill(0, 0, this.width, this.height, var7);
      if (var5 <= 0.001F) {
         return;
      }

      float var8 = var2 * RenderUtil.e();
      float var9 = var3 * RenderUtil.e();
      float var10 = var5;
      int var12 = Theme.getInstance().getAccent().getRGB();

      // Окно целиком: блюр/фон + скруглённые углы.
      HudConfig.a("cs_gui", this.winX, this.winY, this.winW, this.winH, 10.0F, (int)(245.0F * var10) << 24);
      // Сайдбар темнее контента, между ними тонкая линия.
      RenderUtil.a(this.winX, this.winY, SIDE_W, this.winH, 10.0F, a(-1, 8.0F * var10));
      RenderUtil.a(this.winX + SIDE_W, this.winY, this.winW - SIDE_W, this.winH, 10.0F, a(0, 46.0F * var10));
      RenderUtil.a(this.winX + SIDE_W, this.winY + 10.0F, 0.7F, this.winH - 20.0F, 0.35F, a(-1, 14.0F * var10));

      this.drawSidebar(var1, this.winY, var8, var9, var10, var12);
      this.drawTopbar(var1, var8, var9, var10, var12);
      this.drawPage(var1, var2, var3, var4, var10, var12);
      this.drawFooter(var1, var10);
   }

   private void drawSidebar(DrawContext var1, float var2, float var3, float var4, float var5, int var6) {
      // Логотип и название клиента.
      RenderUtil.a(RenderUtil.d(), this.winX + PAD, var2 + 13.0F, 24.0F, 24.0F, LOGO_TEX, 0.0F, a(-1, 255.0F * var5));
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, "Shelka new", this.winX + PAD + 31.0F, var2 + 15.0F, 10.0F, a(-1, 255.0F * var5), false);
      Msdf.a(var1, MsdfManager.BOLD, "1.0.0", this.winX + PAD + 31.0F, var2 + 27.0F, 6.5F, a(var6, 190.0F * var5), false);
      Msdf.c();

      for (int var7 = 0; var7 < PAGE_COUNT; var7++) {
         if (var7 == 6) {
            Msdf.b();
            Msdf.a(var1, MsdfManager.BOLD, "Other", this.winX + PAD, this.itemY(6) - 10.0F, 6.0F, a(-1, 90.0F * var5), false);
            Msdf.c();
         }

         boolean var8 = this.page == var7;
         boolean var9 = !var8 && var3 >= this.winX + 8.0F && var3 <= this.winX + SIDE_W - 8.0F && var4 >= this.itemY(var7) && var4 <= this.itemY(var7) + ITEM_H;
         this.hovers[var7].animate(var9 ? 1.0F : 0.0F, 0.016F, 10.0F);
         float var10 = Math.clamp(this.hovers[var7].getValue(), 0.0F, 1.0F);
         float var11 = this.itemY(var7);
         if (var8) {
            RenderUtil.a(this.winX + 8.0F, var11, SIDE_W - 16.0F, ITEM_H, 6.0F, a(var6, 38.0F * var5));
            RenderUtil.a(this.winX + 8.0F, var11 + 6.0F, 1.6F, ITEM_H - 12.0F, 0.8F, a(var6, 255.0F * var5));
         } else if (var10 > 0.01F) {
            RenderUtil.a(this.winX + 8.0F, var11, SIDE_W - 16.0F, ITEM_H, 6.0F, a(-1, 7.0F * var10 * var5));
         }

         int var12 = var8 ? -1 : a(-1, (int)(150.0F + 60.0F * var10) * var5);
         float var13 = this.winX + PAD + 6.0F;
         float var14 = var11 + ITEM_H / 2.0F;
         Msdf.b();
         if (var7 == PAGE_THEME) {
            // У темы вместо глифа — точка акцентного цвета.
            RenderUtil.a(var13 - 1.0F, var14 - 4.0F, 8.0F, 8.0F, 4.0F, a(var6, 255.0F * var5));
         } else {
            Msdf.a(var1, MsdfManager.DICONS, this.pageIcon(var7), var13 - 3.0F, var14 - 4.5F, 8.5F, var12, false);
         }

         Msdf.a(var1, MsdfManager.BOLD, PAGE_NAMES[var7], var13 + 15.0F, var14 - 3.0F, 7.5F, var12, false);
         Msdf.c();
      }
   }

   private String pageIcon(int var1) {
      Category var2 = PAGE_CATEGORIES[var1];
      if (var2 == null) {
         return var1 == PAGE_MAIN ? "S" : "D";
      } else {
         return var2.getIcon();
      }
   }

   private void drawTopbar(DrawContext var1, float var3, float var4, float var5, int var6) {
      float var7 = this.winX + SIDE_W + PAD;
      float var8 = this.winY + (TOP_H - 22.0F) / 2.0F;
      RenderUtil.a(var7, var8, 84.0F, 22.0F, 6.0F, a(-1, 8.0F * var5));
      RenderUtil.a(var7, var8, 84.0F, 22.0F, 6.0F, 0.55F, a(-1, 12.0F * var5));
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, "Default", var7 + 10.0F, var8 + 8.0F, 7.0F, a(-1, 235.0F * var5), false);
      Msdf.a(var1, MsdfManager.BOLD, "v", var7 + 66.0F, var8 + 8.0F, 6.5F, a(-1, 90.0F * var5), false);
      Msdf.c();

      float var9 = this.searchX();
      float var10 = this.searchY();
      boolean var11 = this.searchFocused;
      RenderUtil.a(var9, var10, SEARCH_W, SEARCH_H, 6.0F, a(-1, (var11 ? 14.0F : 8.0F) * var5));
      if (var11) {
         RenderUtil.a(var9, var10, SEARCH_W, SEARCH_H, 6.0F, 0.6F, a(var6, 90.0F * var5));
      }

      Msdf.b();
      String var12 = this.searchBuffer.toString();
      if (var12.isEmpty()) {
         Msdf.a(var1, MsdfManager.BOLD, "Поиск", var9 + 10.0F, var10 + 8.0F, 6.5F, a(-1, 90.0F * var5), false);
      } else {
         String var13 = var12 + (var11 && System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "");
         Msdf.a(var1, MsdfManager.BOLD, var13, var9 + 10.0F, var10 + 8.0F, 6.5F, a(-1, 235.0F * var5), false);
      }

      Msdf.c();
      RenderUtil.a(this.contentX, this.winY + TOP_H - 1.0F, this.contentW, 0.6F, 0.3F, a(0, 40.0F * var5));
   }

   private void drawPage(DrawContext var1, int var3, int var4, float var5, float var6, int var7) {
      float var8 = this.contentX + PAD;
      float var9 = this.contentY + 10.0F;
      float var10 = this.contentW - PAD * 2.0F;
      float var11 = this.contentH - 20.0F;
      if (this.page == PAGE_MAIN) {
         this.drawHome(var1, var8, var9, var10, var11, var6, var7);
      } else if (this.page == PAGE_THEME) {
         this.drawTheme(var1, var8, var9, var10, var11, var6, var7);
      } else {
         GuiPanel var12 = this.activePanel();
         if (var12 != null) {
            var12.panelW = var10;
            var12.panelH = var11;
            var12.x = var8;
            var12.y = var9;
            float var13 = var3 * RenderUtil.e();
            float var14 = var4 * RenderUtil.e();
            boolean var15 = !this.closing && var13 >= var12.x && var13 <= var12.x + var12.panelW && var14 >= var12.y && var14 <= var12.y + var12.panelH;
            var12.hovered = var15 ? var12.b(var13, var14) : null;
            var12.a(var1, var3, var4, var5, this.searchBuffer.toString(), !this.closing);
            Function var16 = var12.hovered;
            if (var16 != null && !var16.getDescription().isEmpty()) {
               Msdf.b();
               Msdf.a(var1, MsdfManager.BOLD, var16.getDescription(), var8 + 10.0F, var9 + var11 - 14.0F, 6.0F, a(-1, 110.0F * var6), false);
               Msdf.c();
            }
         }
      }
   }

   private void drawHome(DrawContext var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      float var8 = var2 + var4 / 2.0F;
      float var9 = var3 + var5 / 2.0F - 40.0F;
      RenderUtil.a(RenderUtil.d(), var8 - 26.0F, var9 - 26.0F, 52.0F, 52.0F, LOGO_TEX, 0.0F, a(-1, 255.0F * var6));
      Msdf.b();
      float var10 = Msdf.a(MsdfManager.BOLD, "Shelka new", 17.0F);
      Msdf.a(var1, MsdfManager.BOLD, "Shelka new", var8 - var10 / 2.0F, var9 + 34.0F, 17.0F, a(-1, 255.0F * var6), false);
      float var11 = Msdf.a(MsdfManager.BOLD, "version 1.0.0", 7.5F);
      Msdf.a(var1, MsdfManager.BOLD, "version 1.0.0", var8 - var11 / 2.0F, var9 + 55.0F, 7.5F, a(var7, 200.0F * var6), false);
      String[] var12 = {
         "ЛКМ — включить модуль",
         "ПКМ — раскрыть настройки",
         "СКМ — назначить клавишу",
         "ПКМ по настройке — сброс"
      };
      float var13 = var9 + 80.0F;

      for (String var17 : var12) {
         float var18 = Msdf.a(MsdfManager.BOLD, var17, 6.5F);
         Msdf.a(var1, MsdfManager.BOLD, var17, var8 - var18 / 2.0F, var13, 6.5F, a(-1, 120.0F * var6), false);
         var13 += 12.0F;
      }

      Msdf.c();
   }

   private void drawTheme(DrawContext var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, "Акцент", var2, var3 + 2.0F, 8.0F, a(-1, 235.0F * var6), false);
      Msdf.c();
      List<Color> var8 = Theme.getInstance().getPalette();
      float var9 = var3 + 18.0F;

      for (int var10 = 0; var10 < var8.size(); var10++) {
         float var11 = var2 + var10 * 32.0F;
         boolean var12 = (var8.get(var10).getRGB() & 16777215) == (Theme.getInstance().getAccent().getRGB() & 16777215);
         RenderUtil.a(var11, var9, 26.0F, 26.0F, 7.0F, a(var8.get(var10).getRGB(), 255.0F * var6));
         if (var12) {
            RenderUtil.a(var11 - 2.0F, var9 - 2.0F, 30.0F, 30.0F, 8.0F, 1.2F, a(-1, 220.0F * var6));
         }
      }

      float var23 = var9 + 46.0F;
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, "Режим клик-гуи", var2, var23, 8.0F, a(-1, 235.0F * var6), false);
      Msdf.c();
      Theme.b var24 = Theme.getInstance().getGuiMode();
      this.drawChip(var1, var2, var23 + 16.0F, 74.0F, "Панели", var24 == Theme.b.PANELS, var6, var7);
      this.drawChip(var1, var2 + 80.0F, var23 + 16.0F, 50.0F, "CS", var24 == Theme.b.CS, var6, var7);
      float var25 = var23 + 60.0F;
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, "Стиль фона", var2, var25, 8.0F, a(-1, 235.0F * var6), false);
      Msdf.c();
      Theme.a var26 = Theme.getInstance().getGuiStyle();
      this.drawChip(var1, var2, var25 + 16.0F, 74.0F, Theme.a.BLUR.getLabel(), var26 == Theme.a.BLUR, var6, var7);
      this.drawChip(var1, var2 + 80.0F, var25 + 16.0F, 74.0F, Theme.a.NORMAL.getLabel(), var26 == Theme.a.NORMAL, var6, var7);
   }

   private void drawChip(DrawContext var1, float var2, float var3, float var4, String var5, boolean var6, float var7, int var8) {
      RenderUtil.a(var2, var3, var4, 20.0F, 6.0F, var6 ? a(var8, 190.0F * var7) : a(-1, 8.0F * var7));
      if (!var6) {
         RenderUtil.a(var2, var3, var4, 20.0F, 6.0F, 0.5F, a(-1, 14.0F * var7));
      }

      Msdf.b();
      float var9 = Msdf.a(MsdfManager.BOLD, var5, 6.5F);
      Msdf.a(var1, MsdfManager.BOLD, var5, var2 + (var4 - var9) / 2.0F, var3 + 7.5F, 6.5F, a(var6 ? -16777216 : -1, 235.0F * var7), false);
      Msdf.c();
   }

   private void drawFooter(DrawContext var1, float var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      String var7 = var6.getSession() != null ? var6.getSession().getUsername() : "";
      if (var7 == null || var7.isEmpty()) {
         return;
      }

      float var8 = this.winY + this.winH - 30.0F;
      RenderUtil.a(this.winX + PAD, var8, SIDE_W - PAD * 2.0F, 0.5F, 0.25F, a(-1, 14.0F * var5));
      RenderUtil.a(this.winX + PAD, var8 + 12.0F, 5.0F, 5.0F, 2.5F, a(Theme.getInstance().getAccent().getRGB(), 230.0F * var5));
      Msdf.b();
      Msdf.a(var1, MsdfManager.BOLD, var7, this.winX + PAD + 11.0F, var8 + 9.0F, 7.0F, a(-1, 200.0F * var5), false);
      Msdf.c();
   }

   // ---------------------------------------------------------------- input

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.closing) {
         return true;
      }

      this.layout();
      if (BindSetting.LISTENING != null) {
         BindSetting.LISTENING.setKey(-(click.button() + 100));
         BindSetting.LISTENING = null;
         return true;
      }

      float var3 = (float)(click.x() * RenderUtil.e());
      float var4 = (float)(click.y() * RenderUtil.e());
      GuiPanel.a();

      if (this.page == PAGE_THEME && this.themeClick(var3, var4)) {
         return true;
      }

      // Поиск.
      float var5 = this.searchX();
      float var6 = this.searchY();
      if (var3 >= var5 && var3 <= var5 + SEARCH_W && var4 >= var6 && var4 <= var6 + SEARCH_H) {
         this.searchFocused = true;
         return true;
      }

      this.searchFocused = false;
      // Сайдбар.
      if (var3 >= this.winX + 8.0F && var3 <= this.winX + SIDE_W - 8.0F) {
         for (int var7 = 0; var7 < PAGE_COUNT; var7++) {
            float var8 = this.itemY(var7);
            if (var4 >= var8 && var4 <= var8 + ITEM_H) {
               this.setPage(var7);
               return true;
            }
         }
      }

      GuiPanel var9 = this.activePanel();
      if (var9 != null) {
         return var9.a(var3, var4, click.button(), EventManager.clickGui);
      }

      return super.mouseClicked(click, doubled);
   }

   private boolean themeClick(float var1, float var2) {
      float var3 = this.contentX + PAD;
      float var4 = this.contentY + 10.0F;
      float var5 = var3 + 18.0F;
      List<Color> var6 = Theme.getInstance().getPalette();

      for (int var7 = 0; var7 < var6.size(); var7++) {
         float var8 = var3 + var7 * 32.0F;
         if (var1 >= var8 && var1 <= var8 + 26.0F && var2 >= var5 && var2 <= var5 + 26.0F) {
            Theme.getInstance().a(var6.get(var7));
            return true;
         }
      }

      float var13 = var5 + 46.0F + 16.0F;
      if (var2 >= var13 && var2 <= var13 + 20.0F) {
         if (var1 >= var3 && var1 <= var3 + 74.0F) {
            Theme.getInstance().a(Theme.b.PANELS);
            return true;
         }

         if (var1 >= var3 + 80.0F && var1 <= var3 + 130.0F) {
            Theme.getInstance().a(Theme.b.CS);
            return true;
         }
      }

      float var14 = var13 + 60.0F;
      if (var2 >= var14 && var2 <= var14 + 20.0F) {
         if (var1 >= var3 && var1 <= var3 + 74.0F) {
            Theme.getInstance().a(Theme.a.BLUR);
            return true;
         }

         if (var1 >= var3 + 80.0F && var1 <= var3 + 154.0F) {
            Theme.getInstance().a(Theme.a.NORMAL);
            return true;
         }
      }

      return false;
   }

   private void setPage(int var1) {
      if (this.page != var1) {
         this.page = var1;
         GuiPanel var2 = this.activePanel();
         if (var2 != null) {
            var2.open.reset(0.0F);
         }
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      float var2 = (float)(click.x() * RenderUtil.e());
      float var3 = (float)(click.y() * RenderUtil.e());
      GuiPanel var4 = this.activePanel();
      if (var4 != null) {
         var4.a(var2, var3, click.button());
      }

      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      float var6 = (float)(click.x() * RenderUtil.e());
      float var7 = (float)(click.y() * RenderUtil.e());
      GuiPanel var8 = this.activePanel();
      if (var8 != null) {
         var8.b(var6, var7, click.button());
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float var7 = (float)(mouseX * RenderUtil.e());
      float var8 = (float)(mouseY * RenderUtil.e());
      GuiPanel var9 = this.activePanel();
      return var9 != null && var9.a(var7, var8, verticalAmount) ? true : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.closing) {
         return true;
      }

      if (this.searchFocused) {
         if (input.key() == 256 || input.key() == 257) {
            this.searchFocused = false;
            return true;
         }

         if (input.key() == 259 || input.key() == 261) {
            if (this.searchBuffer.length() > 0) {
               this.searchBuffer.deleteCharAt(this.searchBuffer.length() - 1);
            }

            return true;
         }

         if (input.key() == 70 && (input.modifiers() & 2) != 0) {
            this.searchFocused = false;
            return true;
         }
      } else {
         if (GuiPanel.typingNumber != null) {
            if (input.key() == 256 || input.key() == 257 || input.key() == 335) {
               if (input.key() != 256) {
                  GuiPanel.a();
               } else {
                  GuiPanel.typingNumber = null;
                  GuiPanel.numberInputBuffer.setLength(0);
               }

               return true;
            }

            if (input.key() == 259 || input.key() == 261) {
               if (GuiPanel.numberInputBuffer.length() > 0) {
                  GuiPanel.numberInputBuffer.deleteCharAt(GuiPanel.numberInputBuffer.length() - 1);
               }

               return true;
            }
         } else if (TextSetting.LISTENING != null) {
            if (input.key() == 256 || input.key() == 257 || input.key() == 335) {
               TextSetting.LISTENING = null;
               return true;
            }

            if (input.key() == 259 || input.key() == 261) {
               String var2 = TextSetting.LISTENING.getText();
               if (!var2.isEmpty()) {
                  TextSetting.LISTENING.setValue(var2.substring(0, var2.length() - 1));
               }

               return true;
            }
         } else {
            if (BindSetting.LISTENING != null) {
               int var6 = input.key() != 256 && input.key() != 261 && input.key() != 259 ? 0 : 1;
               BindSetting.LISTENING.setKey(var6 != 0 ? -1 : input.key());
               BindSetting.LISTENING = null;
               return true;
            }

            GuiPanel var7 = this.activePanel();
            if (var7 != null && var7.listening != null) {
               int var3 = input.key() != 256 && input.key() != 261 && input.key() != 259 ? 0 : 1;
               var7.listening.setKeybind(var3 != 0 ? 0 : input.key());
               var7.listening = null;
               return true;
            }

            if (input.key() == 256) {
               this.close();
               return true;
            }
         }
      }

      return super.keyPressed(input);
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.closing) {
         return true;
      }

      if (this.searchFocused) {
         if (input.isValidChar()) {
            this.searchBuffer.append(input.asString());
         }

         return true;
      }

      if (GuiPanel.typingNumber != null) {
         if (input.isValidChar()) {
            String var2 = input.asString();
            if (!var2.isEmpty()) {
               char var3 = var2.charAt(0);
               if (var3 >= '0' && var3 <= '9' || var3 == '.' || var3 == ',' || var3 == '-') {
                  GuiPanel.numberInputBuffer.append(var3);
               }
            }
         }

         return true;
      }

      if (TextSetting.LISTENING != null) {
         if (input.isValidChar()) {
            TextSetting.LISTENING.setValue(TextSetting.LISTENING.getText() + input.asString());
         }

         return true;
      }

      return super.charTyped(input);
   }

   // ---------------------------------------------------------------- colors

   private static int a(int var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0 & 16777215;
   }
}
