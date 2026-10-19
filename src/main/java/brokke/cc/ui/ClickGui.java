package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.cosmetic.CosmeticsPanel;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.irc.IrcPanel;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.TextSetting;
import brokke.cc.system.events.EventManager;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class ClickGui extends Screen {
   private static final float GAP = 8.0F;
   private static final float THEME_W = 125.0F;
   private static final float THEME_H = 140.0F;
   private final List<GuiPanel> panels = new ArrayList<>();
   private GuiPanel listeningPanel;
   private final Animation tooltip = new Animation();
   private String lastTooltip;
   boolean themeOpen;
   private final Animation themeAnim = new Animation();
   private final List<ClickGui.a> themeHitPool = new ArrayList<>();
   private int themeHitCount = 0;
   private final int[] eventsColors = new int[6];
   private float themeX;
   private float themeY;
   private float colorHue = 0.72F;
   private float colorSat = 0.64F;
   private float colorVal = 1.0F;
   private int colorDrag = 0;
   private boolean eyedropperActive = false;
   public boolean swingOpen;
   private final Animation swingAnim = new Animation();
   public Screen parentScreen;
   public boolean eventsOpen;
   private final EventsPanel eventsPanel = new EventsPanel();
   private float eventsBtnX;
   private float eventsBtnY;
   private static final float EVENTS_BTN_W = 58.0F;
   private static final float EVENTS_BTN_H = 18.0F;
   public boolean cosmeticsOpen;
   private final CosmeticsPanel cosmeticsPanel = new CosmeticsPanel();
   private float cosmeticsBtnX;
   private float cosmeticsBtnY;
   private static final float COSMETICS_BTN_W = 58.0F;
   private static final float COSMETICS_BTN_H = 18.0F;
   private final int[] cosmeticsColors = new int[9];
   public boolean autoBuyOpen;
   private final AutoBuyPanel autoBuyPanel = new AutoBuyPanel();
   public static float externalSlideProg = 0.0F;
   public boolean ircOpen;
   private final IrcPanel ircPanel = new IrcPanel();
   private final StringBuilder searchBuffer = new StringBuilder();
   private boolean searchFocused = false;
   private float searchX;
   private float searchY;
   private static final float SEARCH_W = 120.0F;
   private static final float SEARCH_H = 18.0F;
   public boolean closing = false;
   public static boolean searchSelectedAll = false;
   public static boolean numberSelectedAll = false;
   public static boolean textSelectedAll = false;

   private void a(float var1, float var2, float var3, float var4, int var5, Object var6) {
      ClickGui.a var7;
      if (this.themeHitCount < this.themeHitPool.size()) {
         var7 = this.themeHitPool.get(this.themeHitCount);
      } else {
         var7 = new ClickGui.a();
         this.themeHitPool.add(var7);
      }

      var7.a(var1, var2, var3, var4, var5, var6);
      this.themeHitCount++;
   }

   public ClickGui() {
      super(Text.of("ClickGui"));
      int var1 = 0;

      for (Category var5 : Category.values()) {
         GuiPanel var6 = new GuiPanel(var5);
         var6.openDelay = var1 * 0.045F;
         this.panels.add(var6);
         var1++;
      }

      this.tooltip.setEasing(Easing.SPRING_SOFT);
      this.themeAnim.setEasing(Easing.SPRING_SOFT);
   }

   @Override
   protected void init() {
      if (this.parentScreen != null) {
         this.parentScreen.init(this.width, this.height);
      }

      super.init();
   }

   @Override
   public void resize(int width, int height) {
      if (this.parentScreen != null) {
         this.parentScreen.resize(width, height);
      }

      super.resize(width, height);
   }

   public static boolean a() {
      return EventManager.clickGui != null && EventManager.clickGui.themeOpen;
   }

   public static void b() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.themeOpen = !EventManager.clickGui.themeOpen;
         if (EventManager.clickGui.themeOpen) {
            EventManager.clickGui.eventsOpen = false;
            EventManager.clickGui.cosmeticsOpen = false;
            EventManager.clickGui.autoBuyOpen = false;
            EventManager.clickGui.ircOpen = false;
         }
      }
   }

   public static void c() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.themeOpen = true;
         EventManager.clickGui.eventsOpen = false;
         EventManager.clickGui.cosmeticsOpen = false;
         EventManager.clickGui.autoBuyOpen = false;
         EventManager.clickGui.ircOpen = false;
      }
   }

   public static boolean d() {
      return EventManager.clickGui != null && EventManager.clickGui.eventsOpen;
   }

   public static void e() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.eventsOpen = !EventManager.clickGui.eventsOpen;
         if (EventManager.clickGui.eventsOpen) {
            EventManager.clickGui.cosmeticsOpen = false;
            EventManager.clickGui.themeOpen = false;
            EventManager.clickGui.autoBuyOpen = false;
            EventManager.clickGui.ircOpen = false;
            EventManager.clickGui.searchFocused = false;
            GuiPanel.a();
         }
      }
   }

   public static boolean f() {
      return EventManager.clickGui != null && EventManager.clickGui.cosmeticsOpen;
   }

   public static void g() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.cosmeticsOpen = !EventManager.clickGui.cosmeticsOpen;
         if (EventManager.clickGui.cosmeticsOpen) {
            EventManager.clickGui.eventsOpen = false;
            EventManager.clickGui.themeOpen = false;
            EventManager.clickGui.autoBuyOpen = false;
            EventManager.clickGui.ircOpen = false;
            EventManager.clickGui.searchFocused = false;
            GuiPanel.a();
         }
      }
   }

   public static boolean h() {
      return EventManager.clickGui != null && EventManager.clickGui.autoBuyOpen;
   }

   public static void i() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.autoBuyOpen = !EventManager.clickGui.autoBuyOpen;
         if (EventManager.clickGui.autoBuyOpen) {
            EventManager.clickGui.eventsOpen = false;
            EventManager.clickGui.cosmeticsOpen = false;
            EventManager.clickGui.themeOpen = false;
            EventManager.clickGui.ircOpen = false;
            EventManager.clickGui.searchFocused = false;
            GuiPanel.a();
         }
      }
   }

   public static void j() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (EventManager.clickGui == null) {
         EventManager.clickGui = new ClickGui();
      }

      if (var0.currentScreen != EventManager.clickGui) {
         var0.setScreen(EventManager.clickGui);
      }

      EventManager.clickGui.autoBuyOpen = true;
      EventManager.clickGui.eventsOpen = false;
      EventManager.clickGui.cosmeticsOpen = false;
      EventManager.clickGui.themeOpen = false;
      EventManager.clickGui.ircOpen = false;
      EventManager.clickGui.searchFocused = false;
      GuiPanel.a();
   }

   public static boolean k() {
      return EventManager.clickGui != null && EventManager.clickGui.ircOpen;
   }

   public static void l() {
      if (EventManager.clickGui != null) {
         EventManager.clickGui.ircOpen = !EventManager.clickGui.ircOpen;
         if (EventManager.clickGui.ircOpen) {
            EventManager.clickGui.eventsOpen = false;
            EventManager.clickGui.cosmeticsOpen = false;
            EventManager.clickGui.themeOpen = false;
            EventManager.clickGui.autoBuyOpen = false;
            EventManager.clickGui.searchFocused = false;
            GuiPanel.a();
         }
      }
   }

   public static void m() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (EventManager.clickGui == null) {
         EventManager.clickGui = new ClickGui();
      }

      if (var0.currentScreen != EventManager.clickGui) {
         var0.setScreen(EventManager.clickGui);
      }

      EventManager.clickGui.ircOpen = true;
      EventManager.clickGui.eventsOpen = false;
      EventManager.clickGui.cosmeticsOpen = false;
      EventManager.clickGui.themeOpen = false;
      EventManager.clickGui.autoBuyOpen = false;
      EventManager.clickGui.searchFocused = false;
      GuiPanel.a();
   }

   public void a(GuiPanel var1, Function var2) {
      for (GuiPanel var4 : this.panels) {
         var4.listening = null;
      }

      this.listeningPanel = var1;
      var1.listening = var2;
   }

   public void n() {
      int var1 = 0;

      for (GuiPanel var3 : this.panels) {
         var3.open.reset(0.0F);
         var3.openDelay = var1 * 0.045F;
         var3.listening = null;
         var1++;
      }

      this.listeningPanel = null;
      this.tooltip.reset(0.0F);
      this.themeOpen = false;
      this.themeAnim.reset(0.0F);
      this.eventsOpen = false;
      this.eventsPanel.openAnim.reset(0.0F);
      this.cosmeticsOpen = false;
      this.cosmeticsPanel.openAnim.reset(0.0F);
      this.autoBuyOpen = false;
      this.autoBuyPanel.openAnim.reset(0.0F);
      this.colorDrag = 0;
      this.eyedropperActive = false;
      this.searchBuffer.setLength(0);
      this.searchFocused = false;
      GuiPanel.a();
      this.closing = false;
      externalSlideProg = 0.0F;
   }

   public boolean isClosing() {
      return this.closing;
   }

   public void p() {
      this.closing = false;
   }

   public void q() {
      this.closing = false;
      externalSlideProg = 0.0F;
      this.s();
      Screen var1 = this.parentScreen;
      this.parentScreen = null;
      MinecraftClient.getInstance().setScreen(var1);
   }

   @Override
   public void close() {
      if (!this.closing) {
         this.closing = true;
         externalSlideProg = 0.0F;
         this.themeOpen = false;
         this.eventsOpen = false;
         this.cosmeticsOpen = false;
         this.searchFocused = false;
         this.searchBuffer.setLength(0);
         GuiPanel.a();
         TextSetting.LISTENING = null;
         BindSetting.LISTENING = null;
         if (this.listeningPanel != null) {
            this.listeningPanel.listening = null;
            this.listeningPanel = null;
         }

         this.s();
         float var1 = 0.0F;

         for (GuiPanel var3 : this.panels) {
            var1 = Math.max(var1, var3.open.getValue());
         }

         if (var1 <= 0.005F) {
            this.q();
         }
      }
   }

   @Override
   public void removed() {
      this.s();
      this.closing = false;
      externalSlideProg = 0.0F;
      super.removed();
   }

   public boolean r() {
      return this.closing
         ? false
         : this.searchFocused
            || GuiPanel.typingNumber != null
            || TextSetting.LISTENING != null
            || BindSetting.LISTENING != null
            || this.listeningPanel != null && this.listeningPanel.listening != null
            || this.ircOpen && this.ircPanel != null && this.ircPanel.a();
   }

   private void s() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.options != null) {
         var1.options.forwardKey.setPressed(false);
         var1.options.backKey.setPressed(false);
         var1.options.leftKey.setPressed(false);
         var1.options.rightKey.setPressed(false);
         var1.options.jumpKey.setPressed(false);
         var1.options.sneakKey.setPressed(false);
         var1.options.sprintKey.setPressed(false);
      }
   }

   private void t() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.player != null && var1.getWindow() != null && var1.options != null) {
         if (this.r()) {
            this.s();
         } else {
            Window var2 = var1.getWindow();
            KeyBinding[] var3 = new KeyBinding[]{
               var1.options.forwardKey,
               var1.options.backKey,
               var1.options.leftKey,
               var1.options.rightKey,
               var1.options.jumpKey,
               var1.options.sneakKey,
               var1.options.sprintKey
            };

            for (KeyBinding var7 : var3) {
               Key var8 = InputUtil.fromTranslationKey(var7.getBoundKeyTranslationKey());
               if (var8.getCategory() == Type.KEYSYM) {
                  var7.setPressed(InputUtil.isKeyPressed(var2, var8.getCode()));
               } else if (var8.getCategory() == Type.MOUSE) {
                  var7.setPressed(GLFW.glfwGetMouseButton(var2.getHandle(), var8.getCode()) == 1);
               }
            }
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      MsdfManager.a();
      GuiBatch.begin();

      try {
         this.renderContent(context, mouseX, mouseY, deltaTicks);
      } finally {
         GuiBatch.end();
      }
   }

   private void renderContent(DrawContext var1, int var2, int var3, float var4) {
      this.t();
      float var5 = 0.0F;

      for (GuiPanel var7 : this.panels) {
         var5 = Math.max(var5, var7.open.getValue());
      }

      Math.clamp(this.eventsPanel.openAnim.getValue(), 0.0F, 1.0F);
      Math.clamp(this.cosmeticsPanel.openAnim.getValue(), 0.0F, 1.0F);
      Math.clamp(this.autoBuyPanel.openAnim.getValue(), 0.0F, 1.0F);
      Math.clamp(this.ircPanel.openAnim.getValue(), 0.0F, 1.0F);
      this.eventsPanel.openAnim.animate(this.eventsOpen ? 1.0F : 0.0F, var4, this.closing ? 5.5F : 3.8F);
      this.cosmeticsPanel.openAnim.animate(this.cosmeticsOpen ? 1.0F : 0.0F, var4, this.closing ? 5.5F : 3.8F);
      this.autoBuyPanel.openAnim.animate(this.autoBuyOpen ? 1.0F : 0.0F, var4, this.closing ? 5.5F : 3.8F);
      this.ircPanel.openAnim.animate(this.ircOpen ? 1.0F : 0.0F, var4, this.closing ? 5.5F : 3.8F);
      float var69 = Math.clamp(this.eventsPanel.openAnim.getValue(), 0.0F, 1.0F);
      float var70 = Math.clamp(this.cosmeticsPanel.openAnim.getValue(), 0.0F, 1.0F);
      float var8 = Math.clamp(this.autoBuyPanel.openAnim.getValue(), 0.0F, 1.0F);
      float var9 = Math.clamp(this.ircPanel.openAnim.getValue(), 0.0F, 1.0F);
      float var10 = Math.max(Math.max(Math.max(Math.max(var69, var70), var8), var9), externalSlideProg);
      float var11 = Math.max(var5, var10);
      if (this.parentScreen != null) {
         this.parentScreen.render(var1, -1, -1, var4);
         int var12;
         if ((var12 = (int)(96.0F * Math.clamp(var11, 0.0F, 1.0F))) > 0) {
            var1.fill(0, 0, this.width, this.height, var12 << 24);
         }
      } else if (MinecraftClient.getInstance().world == null) {
         var1.fill(0, 0, this.width, this.height, -16118766);
      } else {
         int var71;
         if ((var71 = (int)(48.0F * Math.clamp(var11, 0.0F, 1.0F))) > 0) {
            var1.fill(0, 0, this.width, this.height, var71 << 24);
         }
      }

      float var72 = var2 * RenderUtil.e();
      float var13 = var3 * RenderUtil.e();
      float var14 = 125.0F * this.panels.size() + 8.0F * (this.panels.size() - 1);
      float var15 = (RenderUtil.a() - var14) / 2.0F;
      float var16 = (RenderUtil.b() - 270.0F) / 2.0F;
      this.themeAnim
         .animate(
            this.themeOpen && !this.eventsOpen && !this.cosmeticsOpen && !this.autoBuyOpen && !this.ircOpen && externalSlideProg < 0.05F ? 1.0F : 0.0F,
            var4,
            this.closing ? 5.5F : 3.6F
         );
      float var17;
      float var18 = (var17 = Math.clamp(this.themeAnim.getValue(), 0.0F, 1.0F)) * 133.0F;
      float var19 = var10 * (var15 + var14 + 40.0F);
      Function var20 = null;

      for (int var21 = 0; var21 < this.panels.size(); var21++) {
         GuiPanel var22 = this.panels.get(var21);
         var22.x = var15 + var21 * 133.0F - var18 - var19;
         var22.y = var16;
         var22.hovered = null;
         if (!this.closing
            && var10 < 0.05F
            && !this.eventsOpen
            && !this.cosmeticsOpen
            && !this.autoBuyOpen
            && !this.ircOpen
            && var17 < 0.3F
            && var22.open.getValue() > 0.5F
            && var22.a(var72, var13)) {
            Function var23 = var22.b(var72, var13);
            var22.hovered = var23;
            if (var23 != null && !var23.getDescription().isEmpty()) {
               var20 = var23;
            }
         }
      }

      String var73 = this.searchBuffer.toString();

      for (GuiPanel var76 : this.panels) {
         var76.a(var1, var2, var3, var4, var73, !this.closing);
      }

      if (var17 > 0.01F) {
         this.a(var1, var17, var15 - var19, var14, var16);
      }

      this.searchX = (RenderUtil.a() - 120.0F) / 2.0F - var18 / 2.0F - var19;
      this.searchY = var16 + 270.0F + 10.0F;
      float var75 = var5;
      if (var5 > 0.01F && this.searchX + 120.0F > 0.0F) {
         HudConfig.a("search", this.searchX, this.searchY, 120.0F, 18.0F, 5.0F, (int)(245.0F * var75) << 24);
         int var77 = Theme.getInstance().getAccent().getRGB();
         if (this.searchBuffer.isEmpty() && !this.searchFocused) {
            float var79 = Msdf.a(MsdfManager.BOLD, "Поиск по модулям", 6.5F);
            Msdf.b();
            Msdf.a(
               var1,
               MsdfManager.BOLD,
               "Поиск по модулям",
               this.searchX + (120.0F - var79) / 2.0F,
               this.searchY + (18.0F - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F - 0.5F,
               6.5F,
               a(-7892581, 180.0F * var75),
               false
            );
            Msdf.c();
         } else {
            float var24 = this.searchX + 6.0F;
            float var25 = this.searchY + (18.0F - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F - 0.5F;
            String var26 = this.searchBuffer.toString();
            if (this.searchFocused && searchSelectedAll && !var26.isEmpty()) {
               float var95 = Msdf.a(MsdfManager.BOLD, var26, 6.5F);
               float var28 = Msdf.a(MsdfManager.BOLD, 6.5F);
               RenderUtil.a(var24 - 1.5F, var25 - 1.0F, var95 + 3.0F, var28 + 2.0F, 2.0F, a(var77, 130.0F * var75));
               Msdf.b();
               Msdf.a(var1, MsdfManager.BOLD, var26, var24, var25, 6.5F, a(-1, 255.0F * var75), false);
               Msdf.c();
            } else {
               String var27 = var26 + (this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "");
               Msdf.b();
               Msdf.a(var1, MsdfManager.BOLD, var27, var24, var25, 6.5F, a(-1, 255.0F * var75), false);
               Msdf.c();
            }
         }
      }

      float var78 = 6.0F;
      this.eventsBtnX = this.searchX - var78 - 58.0F;
      this.eventsBtnY = this.searchY;
      this.cosmeticsBtnX = this.searchX + 120.0F + var78;
      this.cosmeticsBtnY = this.searchY;
      float var80 = var5;
      if (var5 > 0.01F && (this.cosmeticsBtnX + 58.0F > 0.0F || this.eventsBtnX + 58.0F > 0.0F)) {
         int var81 = var10 < 0.05F
               && var72 >= this.eventsBtnX
               && var72 <= this.eventsBtnX + 58.0F
               && var13 >= this.eventsBtnY
               && var13 <= this.eventsBtnY + 18.0F
            ? 1
            : 0;
         HudConfig.a("events_btn", this.eventsBtnX, this.eventsBtnY, 58.0F, 18.0F, 5.0F, (int)(245.0F * var80) << 24);
         int var88 = Theme.getInstance().getAccent().getRGB();
         float var96 = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
         float var103 = (float)(0.5 + 0.5 * Math.sin(var96 * 2.8));
         int var29 = a(a(-1, (var81 != 0 ? 90.0F : 40.0F) * var80), a(var88, (var81 != 0 ? 230.0F : 150.0F) * var80), var81 != 0 ? 0.9F : 0.25F + 0.5F * var103);
         RenderUtil.a(this.eventsBtnX, this.eventsBtnY, 58.0F, 18.0F, 5.0F, 0.1F, var29);
         String var30 = "Events";
         float var31 = 6.0F;
         float var32 = 6.5F;
         float var33 = 3.0F;
         String var34 = "q";
         float var35 = Msdf.a(MsdfManager.ICONS2, var34, var32);
         float var36 = Msdf.a(MsdfManager.ICONS2, var32);
         float var37 = Msdf.a(MsdfManager.BOLD, var30, var31);
         float var38 = Msdf.a(MsdfManager.BOLD, var31);
         float var39 = var35 + var33 + var37;
         float var40;
         float var41 = var40 = this.eventsBtnX + (58.0F - var39) / 2.0F;
         float var42 = this.eventsBtnY + (18.0F - var36) / 2.0F;
         float var43 = var40 + var35 + var33;
         float var44 = this.eventsBtnY + (18.0F - var38) / 2.0F - 0.5F;
         float var45 = (float)(0.5 + 0.5 * Math.sin(var96 * (var81 != 0 ? 4.5 : 3.2) + 0.75));
         int var46 = a(a(var88, -1, 0.15F + 0.7F * var45), 255.0F * var80);
         Msdf.a(var1, MsdfManager.ICONS2, var34, var41, var42, var32, var46, false);
         int[] var47 = this.eventsColors;

         for (int var48 = 0; var48 < var30.length(); var48++) {
            float var49 = (float)(0.5 + 0.5 * Math.sin(var96 * (var81 != 0 ? 4.5 : 3.2) - var48 * 0.75));
            int var50 = a(var88, -1, 0.15F + 0.7F * var49);
            var47[var48] = a(var50, 255.0F * var80);
         }

         Msdf.a(var1, MsdfManager.BOLD, var30, var43, var44, var31, false, var47);
         int var111 = var10 < 0.05F
               && var72 >= this.cosmeticsBtnX
               && var72 <= this.cosmeticsBtnX + 58.0F
               && var13 >= this.cosmeticsBtnY
               && var13 <= this.cosmeticsBtnY + 18.0F
            ? 1
            : 0;
         HudConfig.a("cosmetics_btn", this.cosmeticsBtnX, this.cosmeticsBtnY, 58.0F, 18.0F, 5.0F, (int)(245.0F * var80) << 24);
         float var112 = (float)(0.5 + 0.5 * Math.sin(var96 * 2.8 + 1.2));
         int var113 = a(
            a(-1, (var111 != 0 ? 90.0F : 40.0F) * var80), a(var88, (var111 != 0 ? 230.0F : 150.0F) * var80), var111 != 0 ? 0.9F : 0.25F + 0.5F * var112
         );
         RenderUtil.a(this.cosmeticsBtnX, this.cosmeticsBtnY, 58.0F, 18.0F, 5.0F, 0.1F, var113);
         String var51 = "Cosmetics";
         String var52 = "t";
         float var53 = Msdf.a(MsdfManager.ICONS2, var52, var32);
         float var54 = Msdf.a(MsdfManager.ICONS2, var32);
         float var55 = Msdf.a(MsdfManager.BOLD, var51, var31);
         float var56 = Msdf.a(MsdfManager.BOLD, var31);
         float var57 = var53 + var33 + var55;
         float var58;
         float var59 = var58 = this.cosmeticsBtnX + (58.0F - var57) / 2.0F;
         float var60 = this.cosmeticsBtnY + (18.0F - var54) / 2.0F;
         float var61 = var58 + var53 + var33;
         float var62 = this.cosmeticsBtnY + (18.0F - var56) / 2.0F - 0.5F;
         float var63 = (float)(0.5 + 0.5 * Math.sin(var96 * (var111 != 0 ? 4.5 : 3.2) + 1.5 + 0.65));
         int var64 = a(a(var88, -1, 0.15F + 0.7F * var63), 255.0F * var80);
         Msdf.a(var1, MsdfManager.ICONS2, var52, var59, var60, var32, var64, false);
         int[] var65 = this.cosmeticsColors;

         for (int var66 = 0; var66 < var51.length(); var66++) {
            float var67 = (float)(0.5 + 0.5 * Math.sin(var96 * (var111 != 0 ? 4.5 : 3.2) - var66 * 0.65 + 1.5));
            int var68 = a(var88, -1, 0.15F + 0.7F * var67);
            var65[var66] = a(var68, 255.0F * var80);
         }

         Msdf.a(var1, MsdfManager.BOLD, var51, var61, var62, var31, false, var65);
      }

      if (this.eventsOpen || var69 > 0.005F) {
         float var82 = (RenderUtil.a() - 360.0F) / 2.0F;
         float var89 = (RenderUtil.b() - 195.0F) / 2.0F;
         float var97 = RenderUtil.a() - var82 + 40.0F;
         float var104 = var82 + (1.0F - var69) * var97;
         this.eventsPanel.a(var1, var104, var89, (int)var72, (int)var13, var4, this.eventsOpen);
      }

      if (this.cosmeticsOpen || var70 > 0.005F) {
         float var83 = (RenderUtil.a() - 380.0F) / 2.0F;
         float var90 = (RenderUtil.b() - 210.0F) / 2.0F;
         float var98 = RenderUtil.a() - var83 + 40.0F;
         float var105 = var83 + (1.0F - var70) * var98;
         this.cosmeticsPanel.a(var1, var105, var90, (int)var72, (int)var13, var4, this.cosmeticsOpen);
      }

      if (this.autoBuyOpen || var8 > 0.005F) {
         float var84 = (RenderUtil.a() - 390.0F) / 2.0F;
         float var91 = (RenderUtil.b() - 220.0F) / 2.0F;
         float var99 = RenderUtil.a() - var84 + 40.0F;
         float var106 = var84 + (1.0F - var8) * var99;
         this.autoBuyPanel.a(var1, var106, var91, (int)var72, (int)var13, var4, this.autoBuyOpen);
      }

      if (this.ircOpen || var9 > 0.005F) {
         float var85 = (RenderUtil.a() - 450.0F) / 2.0F;
         float var92 = (RenderUtil.b() - 250.0F) / 2.0F;
         float var100 = RenderUtil.a() - var85 + 40.0F;
         float var107 = var85 + (1.0F - var9) * var100;
         this.ircPanel.a(var1, var107, var92, (int)var72, (int)var13, var4, this.ircOpen);
      }

      if (var20 != null) {
         this.lastTooltip = var20.getDescription();
      }

      this.tooltip
         .animate(
            var20 != null && var17 < 0.3F && !this.eventsOpen && !this.cosmeticsOpen && !this.autoBuyOpen && !this.ircOpen && var10 < 0.05F ? 1.0F : 0.0F,
            var4,
            12.0F
         );
      if (this.lastTooltip != null && !this.lastTooltip.isEmpty() && this.tooltip.getValue() > 0.01F) {
         float var86 = Math.clamp(this.tooltip.getValue(), 0.0F, 1.0F);
         String var93 = this.lastTooltip;
         float var101 = 7.5F;
         float var108 = Msdf.a(MsdfManager.BOLD, var93, var101);
         float var109 = RenderUtil.a() / 2.0F - var18 / 2.0F - var19;
         float var110 = var16 - 20.0F;
         Msdf.b();
         Msdf.a(var1, MsdfManager.BOLD, var93, var109 - var108 / 2.0F + 0.5F, var110 + 0.5F, var101, a(0, 120.0F * var86), false);
         Msdf.a(var1, MsdfManager.BOLD, var93, var109 - var108 / 2.0F, var110, var101, a(-985608, 255.0F * var86), false);
         Msdf.c();
      }

      if (WatermarkConfigModal.isOpen()) {
         WatermarkConfigModal.a(var72, var13, var4);
      }

      if (this.closing) {
         float var87 = 0.0F;

         for (GuiPanel var102 : this.panels) {
            var87 = Math.max(var87, var102.open.getValue());
         }

         if (var87 <= 0.005F && var17 <= 0.01F && var10 <= 0.01F) {
            this.q();
         }
      }
   }

   private void u() {
      if (this.colorDrag == 0) {
         Color var1 = Theme.getInstance().getAccent();
         float[] var2 = Color.RGBtoHSB(var1.getRed(), var1.getGreen(), var1.getBlue(), null);
         if (var1.getRed() != var1.getGreen() || var1.getGreen() != var1.getBlue()) {
            this.colorHue = var2[0];
         }

         this.colorSat = var2[1];
         this.colorVal = var2[2];
      }
   }

   private void v() {
      int var1;
      int var2 = (var1 = Color.HSBtoRGB(this.colorHue, this.colorSat, this.colorVal)) >> 16 & 0xFF;
      int var3 = var1 >> 8 & 0xFF;
      int var4 = var1 & 0xFF;
      Theme.getInstance().a(new Color(var2, var3, var4));
   }

   private void a(Color var1) {
      if (var1 != null) {
         float[] var2 = Color.RGBtoHSB(var1.getRed(), var1.getGreen(), var1.getBlue(), null);
         if (var1.getRed() != var1.getGreen() || var1.getGreen() != var1.getBlue()) {
            this.colorHue = var2[0];
         }

         this.colorSat = var2[1];
         this.colorVal = var2[2];
         Theme.getInstance().a(new Color(var1.getRed(), var1.getGreen(), var1.getBlue()));
      }
   }

   private void a(float var1, float var2) {
      float var3 = this.themeX + 8.0F;
      float var4 = this.themeY + 22.0F;
      float var5 = 90.0F;
      float var6 = 54.0F;
      this.colorSat = Math.clamp(1.0F - (var1 - var3) / var5, 0.0F, 1.0F);
      this.colorVal = Math.clamp(1.0F - (var2 - var4) / var6, 0.0F, 1.0F);
      this.v();
   }

   private void a(float var1) {
      float var2 = this.themeY + 22.0F;
      float var3 = 54.0F;
      this.colorHue = Math.clamp((var1 - var2) / var3, 0.0F, 1.0F);
      this.v();
   }

   private boolean b(float var1, float var2) {
      return var1 >= this.themeX && var1 <= this.themeX + 125.0F && var2 >= this.themeY && var2 <= this.themeY + 140.0F;
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5) {
      this.u();
      float var6 = var3 + var4 + 8.0F;
      this.themeX = var6 + (1.0F - var2) * (RenderUtil.a() - var6);
      this.themeY = var5;
      float var7 = 125.0F;
      float var8 = 140.0F;
      float var9 = var2;
      int var10 = Theme.getInstance().getAccent().getRGB();
      HudConfig.a("theme", this.themeX, this.themeY, var7, var8, 8.0F, (int)(245.0F * var9) << 24);
      this.themeHitCount = 0;
      float var11 = this.themeX + 11.0F;
      float var12 = this.themeY + 11.0F;
      BufferBuilder var13 = ColorPipeline.a();
      if (var13 != null) {
         int var14 = a(this.eyedropperActive ? var10 : -2302746, 255.0F * var9);
         ColorPipeline.a(var13, var11 - 2.5F, var12 + 2.5F, var11 + 1.5F, var12 - 1.5F, 1.6F, var14);
         ColorPipeline.a(var13, var11 + 1.0F, var12 - 1.0F, var11 + 3.0F, var12 - 3.0F, 2.6F, var14);
         ColorPipeline.a(var13, var11 - 3.2F, var12 + 3.2F, var11 - 2.2F, var12 + 2.2F, 0.8F, var14);
         ColorPipeline.a(var13, false);
      }

      this.a(var11 - 6.0F, var12 - 6.0F, var11 + 6.0F, var12 + 6.0F, 10, null);
      float var65 = Msdf.a(MsdfManager.BOLD, "Цвет", 8.0F);
      Msdf.a(var1, MsdfManager.BOLD, "Цвет", this.themeX + (var7 - var65) / 2.0F, this.themeY + 7.0F, 8.0F, a(-986891, 255.0F * var9), false);
      float var15 = this.themeX + var7 - 12.0F;
      float var16 = this.themeY + 11.0F;
      BufferBuilder var17 = ColorPipeline.a();
      if (var17 != null) {
         int var18 = a(-5592396, 255.0F * var9);
         ColorPipeline.a(var17, var15 - 2.5F, var16 - 2.5F, var15 + 2.5F, var16 + 2.5F, 1.2F, var18);
         ColorPipeline.a(var17, var15 + 2.5F, var16 - 2.5F, var15 - 2.5F, var16 + 2.5F, 1.2F, var18);
         ColorPipeline.a(var17, false);
      }

      this.a(var15 - 5.0F, var16 - 5.0F, var15 + 5.0F, var16 + 5.0F, 11, null);
      float var66 = this.themeX + 8.0F;
      float var19 = this.themeY + 22.0F;
      float var20 = 90.0F;
      float var21 = 54.0F;
      int var22;
      int var23 = a(var22 = Color.HSBtoRGB(this.colorHue, 1.0F, 1.0F), 255.0F * var9);
      int var24 = a(a(var22, -1, 0.5F), 255.0F * var9);
      int var25 = a(-1, 255.0F * var9);
      int var26 = a(a(var22, -16777216, 0.5F), 255.0F * var9);
      int var27 = a(a(a(var22, -1, 0.5F), -16777216, 0.5F), 255.0F * var9);
      int var28 = a(a(-1, -16777216, 0.5F), 255.0F * var9);
      int var29 = a(-16777216, 255.0F * var9);
      int var30 = a(-16777216, 255.0F * var9);
      int var31 = a(-16777216, 255.0F * var9);
      RenderUtil.a(var66, var19, var20, var21, 4.0F, var23, var24, var25, var26, var27, var28, var29, var30, var31);
      RenderUtil.a(var66, var19, var20, var21, 4.0F, 0.5F, a(-1, 35.0F * var9));
      float var32 = var66 + (1.0F - this.colorSat) * var20;
      float var33 = var19 + (1.0F - this.colorVal) * var21;
      RenderUtil.a(var32 - 3.0F, var33 - 3.0F, 6.0F, 6.0F, 3.0F, 1.25F, a(-1, 255.0F * var9));
      RenderUtil.a(var32 - 3.5F, var33 - 3.5F, 7.0F, 7.0F, 3.5F, 0.5F, a(-16777216, 100.0F * var9));
      this.a(var66, var19, var66 + var20, var19 + var21, 1, null);
      float var34 = var66 + var20 + 6.0F;
      float var35 = var19;
      float var36 = 9.0F;
      float var37 = var21;
      float var38 = var36 / 2.0F;
      BufferBuilder var39 = ColorPipeline.a();
      if (var39 != null) {
         int var40 = (int)Math.clamp(255.0F * var9, 0.0F, 255.0F);

         for (float var41 = 0.0F; var41 <= var37; var41 += 0.5F) {
            float var42;
            int var43 = Color.HSBtoRGB(var42 = var41 / var37, 1.0F, 1.0F) & 16777215 | var40 << 24;
            float var44 = 0.0F;
            if (var41 < var38) {
               float var45 = var38 - var41;
               var44 = var38 - (float)Math.sqrt(Math.max(0.0F, var38 * var38 - var45 * var45));
            } else if (var41 > var37 - var38) {
               float var72 = var41 - (var37 - var38);
               var44 = var38 - (float)Math.sqrt(Math.max(0.0F, var38 * var38 - var72 * var72));
            }

            ColorPipeline.a(var39, var34 + var44, var35 + var41, var34 + var36 - var44, var35 + var41, 0.65F, var43);
         }

         ColorPipeline.a(var39, false);
      }

      RenderUtil.a(var34, var35, var36, var37, var38, 0.5F, a(-1, 45.0F * var9));
      float var67 = var35 + Math.clamp(this.colorHue, 0.0F, 1.0F) * var37;
      RenderUtil.a(var34 - 1.0F, var67 - 1.5F, var36 + 2.0F, 3.0F, 1.5F, a(-1, 255.0F * var9));
      RenderUtil.a(var34 - 1.0F, var67 - 1.5F, var36 + 2.0F, 3.0F, 1.5F, 0.5F, a(-16777216, 90.0F * var9));
      this.a(var34 - 2.0F, var35, var34 + var36 + 2.0F, var35 + var37, 2, null);
      float var68 = var19 + var21 + 8.0F;
      float var69 = this.themeX + 8.0F;
      float var70 = var68;
      float var71 = 20.0F;
      float var73 = 20.0F;
      RenderUtil.a(var69, var70, var71, var73, 5.0F, a(var10, 255.0F * var9));
      RenderUtil.a(var69, var70, var71, var73, 5.0F, 0.5F, a(-1, 45.0F * var9));
      float var46 = var69 + var71 + 6.0F;
      float var47 = var68 + 1.5F;
      float var48 = 4.5F;
      float var49 = 3.5F;
      List var50 = Theme.getInstance().getPalette();
      int var51 = Math.min(var50.size(), 9);

      for (int var52 = 0; var52 < var51; var52++) {
         Color var53 = (Color)var50.get(var52);
         int var54 = var52 % 5;
         int var55 = var52 / 5;
         float var56 = var46 + var54 * (var48 * 2.0F + var49);
         float var57 = var47 + var55 * (var48 * 2.0F + 2.5F);
         int var58 = (var53.getRGB() & 16777215) == (var10 & 16777215) ? 1 : 0;
         RenderUtil.a(var56, var57, var48 * 2.0F, var48 * 2.0F, var48, a(var53.getRGB(), 255.0F * var9));
         if (var58 != 0) {
            RenderUtil.a(var56 - 1.5F, var57 - 1.5F, var48 * 2.0F + 3.0F, var48 * 2.0F + 3.0F, var48 + 1.5F, 1.0F, a(-1, 230.0F * var9));
         }

         this.a(var56 - 1.0F, var57 - 1.0F, var56 + var48 * 2.0F + 1.0F, var57 + var48 * 2.0F + 1.0F, 4, var53);
      }

      if (var51 < 10) {
         int var74 = var51 % 5;
         int var76 = var51 / 5;
         float var78 = var46 + var74 * (var48 * 2.0F + var49);
         float var80 = var47 + var76 * (var48 * 2.0F + 2.5F);
         float var82 = var48 * 2.0F;
         RenderUtil.a(var78, var80, var82, var82, var82 / 2.0F, a(-14145488, 255.0F * var9));
         Msdf.a(
            var1,
            MsdfManager.BOLD,
            "+",
            var78 + (var82 - Msdf.a(MsdfManager.BOLD, "+", 6.0F)) / 2.0F,
            var80 + (var82 - Msdf.a(MsdfManager.BOLD, 6.0F)) / 2.0F - 0.5F,
            6.0F,
            a(-2302746, 255.0F * var9),
            false
         );
         this.a(var78 - 1.0F, var80 - 1.0F, var78 + var82 + 1.0F, var80 + var82 + 1.0F, 5, null);
      }

      float var75 = var68 + var73 + 6.0F;
      Msdf.a(var1, MsdfManager.BOLD, "Стиль GUI", this.themeX + 9.0F, var75, 6.0F, a(-8224116, 255.0F * var9), false);
      float var77 = this.themeX + 8.0F;
      float var79 = var75 + 8.0F + 2.0F;
      float var81 = 11.0F;
      float var83 = 3.0F;
      Theme.a var84 = Theme.getInstance().getGuiStyle();

      for (Theme.a var61 : Theme.a.values()) {
         float var62 = Msdf.a(MsdfManager.BOLD, var61.getLabel(), 6.25F) + 7.0F;
         int var63 = var61 == var84 ? 1 : 0;
         RenderUtil.a(var77, var79, var62, var81, 3.0F, a(var10, (var63 != 0 ? 110.0F : 28.0F) * var9));
         String var64;
         Msdf.a(
            var1,
            MsdfManager.BOLD,
            var64 = var61.getLabel(),
            var77 + (var62 - Msdf.a(MsdfManager.BOLD, var64, 6.25F)) / 2.0F,
            var79 + (var81 - Msdf.a(MsdfManager.BOLD, 6.25F)) / 2.0F - 0.5F,
            6.25F,
            a(var63 != 0 ? -1315861 : -6908258, 255.0F * var9),
            false
         );
         this.a(var77, var79, var77 + var62, var79 + var81, 6, var61);
         var77 += var62 + var83;
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.closing) {
         return true;
      }

      if (BindSetting.LISTENING != null) {
         BindSetting.LISTENING.setKey(-(click.button() + 100));
         BindSetting.LISTENING = null;
         return true;
      }

      float var3 = (float)(click.x() * RenderUtil.e());
      float var4 = (float)(click.y() * RenderUtil.e());
      if (WatermarkConfigModal.isOpen() && WatermarkConfigModal.a((double)var3, (double)var4, click.button())) {
         return true;
      }

      if (this.themeAnim.getValue() > 0.9F) {
         if (this.eyedropperActive) {
            try {
               MinecraftClient var18 = MinecraftClient.getInstance();
               int var24 = (int)(click.x() * var18.getWindow().getScaleFactor());
               int var29 = (int)(var18.getWindow().getHeight() - click.y() * var18.getWindow().getScaleFactor());
               ByteBuffer var33 = MemoryUtil.memAlloc(4);
               GL11.glReadPixels(var24, var29, 1, 1, 6408, 5121, var33);
               int var9 = var33.get(0) & 255;
               int var10 = var33.get(1) & 255;
               int var11 = var33.get(2) & 255;
               MemoryUtil.memFree(var33);
               this.a(new Color(var9, var10, var11));
            } catch (Exception var12) {
            }

            this.eyedropperActive = false;
            return true;
         }

         if (click.button() == 0 || click.button() == 1) {
            for (int var5 = 0; var5 < this.themeHitCount; var5++) {
               ClickGui.a var6 = this.themeHitPool.get(var5);
               if (var3 >= var6.x1 && var3 <= var6.x2 && var4 >= var6.y1 && var4 <= var6.y2) {
                  int var7 = var6.type;
                  if (var6.type == 1 && click.button() == 0) {
                     this.colorDrag = 1;
                     this.a(var3, var4);
                     return true;
                  }

                  if (var7 == 2 && click.button() == 0) {
                     this.colorDrag = 2;
                     this.a(var4);
                     return true;
                  }

                  if (var7 == 4) {
                     if (click.button() == 0) {
                        this.a((Color)var6.data);
                     } else if (click.button() == 1) {
                        Theme.getInstance().d((Color)var6.data);
                     }

                     return true;
                  }

                  if (var7 == 5 && click.button() == 0) {
                     Theme.getInstance().c(Theme.getInstance().getAccent());
                     return true;
                  }

                  if (var7 == 6 && click.button() == 0) {
                     Theme.getInstance().a((Theme.a)var6.data);
                     ConfigManager.onSettingChanged();
                     return true;
                  }

                  if (var7 == 10 && click.button() == 0) {
                     this.eyedropperActive = !this.eyedropperActive;
                     return true;
                  }

                  if (var7 == 11 && click.button() == 0) {
                     this.themeOpen = false;
                     return true;
                  }
               }
            }
         }

         if (this.b(var3, var4)) {
            return true;
         }
      }

      if (this.eventsOpen) {
         if (this.eventsPanel.a(var3, var4, click.button(), this.eventsOpen, () -> this.eventsOpen = false)) {
            return true;
         }

         float var17 = (RenderUtil.a() - 360.0F) / 2.0F;
         float var23 = (RenderUtil.b() - 195.0F) / 2.0F;
         float var28 = RenderUtil.a() - var17 + 40.0F;
         float var32 = var17 + (1.0F - Math.clamp(this.eventsPanel.openAnim.getValue(), 0.0F, 1.0F)) * var28;
         if (var3 >= var32 && var3 <= var32 + 360.0F && var4 >= var23 && var4 <= var23 + 195.0F) {
            return true;
         }

         this.eventsOpen = false;
         return true;
      } else if (this.cosmeticsOpen) {
         if (this.cosmeticsPanel.a(var3, var4, click.button(), this.cosmeticsOpen, () -> this.cosmeticsOpen = false)) {
            return true;
         }

         float var16 = (RenderUtil.a() - 380.0F) / 2.0F;
         float var22 = (RenderUtil.b() - 210.0F) / 2.0F;
         float var27 = RenderUtil.a() - var16 + 40.0F;
         float var31 = var16 + (1.0F - Math.clamp(this.cosmeticsPanel.openAnim.getValue(), 0.0F, 1.0F)) * var27;
         if (var3 >= var31 && var3 <= var31 + 380.0F && var4 >= var22 && var4 <= var22 + 210.0F) {
            return true;
         }

         this.cosmeticsOpen = false;
         return true;
      } else if (this.autoBuyOpen) {
         if (this.autoBuyPanel.a(var3, var4, click.button(), this.autoBuyOpen, () -> this.autoBuyOpen = false)) {
            return true;
         }

         float var15 = (RenderUtil.a() - 390.0F) / 2.0F;
         float var21 = (RenderUtil.b() - 220.0F) / 2.0F;
         float var26 = RenderUtil.a() - var15 + 40.0F;
         float var30 = var15 + (1.0F - Math.clamp(this.autoBuyPanel.openAnim.getValue(), 0.0F, 1.0F)) * var26;
         if (var3 >= var30 && var3 <= var30 + 390.0F && var4 >= var21 && var4 <= var21 + 220.0F) {
            return true;
         }

         this.autoBuyOpen = false;
         return true;
      } else if (this.ircOpen) {
         if (this.ircPanel.a(var3, var4, click.button(), this.ircOpen, () -> this.ircOpen = false)) {
            return true;
         }

         float var14 = (RenderUtil.a() - 450.0F) / 2.0F;
         float var20 = (RenderUtil.b() - 250.0F) / 2.0F;
         float var25 = RenderUtil.a() - var14 + 40.0F;
         float var8 = var14 + (1.0F - Math.clamp(this.ircPanel.openAnim.getValue(), 0.0F, 1.0F)) * var25;
         if (var3 >= var8 && var3 <= var8 + 450.0F && var4 >= var20 && var4 <= var20 + 250.0F) {
            return true;
         }

         this.ircOpen = false;
         return true;
      } else if (!(this.eventsPanel.openAnim.getValue() > 0.05F)
         && !(this.cosmeticsPanel.openAnim.getValue() > 0.05F)
         && !(this.autoBuyPanel.openAnim.getValue() > 0.05F)
         && !(this.ircPanel.openAnim.getValue() > 0.05F)) {
         if (var3 >= this.eventsBtnX && var3 <= this.eventsBtnX + 58.0F && var4 >= this.eventsBtnY && var4 <= this.eventsBtnY + 18.0F && click.button() == 0) {
            this.eventsOpen = !this.eventsOpen;
            if (this.eventsOpen) {
               this.cosmeticsOpen = false;
               this.themeOpen = false;
               this.autoBuyOpen = false;
               this.ircOpen = false;
               this.searchFocused = false;
               GuiPanel.a();
            }

            return true;
         } else if (var3 >= this.cosmeticsBtnX
            && var3 <= this.cosmeticsBtnX + 58.0F
            && var4 >= this.cosmeticsBtnY
            && var4 <= this.cosmeticsBtnY + 18.0F
            && click.button() == 0) {
            this.cosmeticsOpen = !this.cosmeticsOpen;
            if (this.cosmeticsOpen) {
               this.eventsOpen = false;
               this.themeOpen = false;
               this.autoBuyOpen = false;
               this.ircOpen = false;
               this.searchFocused = false;
               GuiPanel.a();
            }

            return true;
         } else {
            if (var3 >= this.searchX && var3 <= this.searchX + 120.0F && var4 >= this.searchY && var4 <= this.searchY + 18.0F) {
               this.searchFocused = true;
               searchSelectedAll = false;
               return true;
            }

            this.searchFocused = false;
            searchSelectedAll = false;

            for (GuiPanel var19 : this.panels) {
               if (var19.a(var3, var4, click.button(), this)) {
                  return true;
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      if (this.closing) {
         return true;
      }

      float var2 = (float)(click.x() * RenderUtil.e());
      float var3 = (float)(click.y() * RenderUtil.e());
      if (WatermarkConfigModal.isOpen() && WatermarkConfigModal.b(var2, var3, click.button())) {
         return true;
      }

      if (!this.eventsOpen
         && !this.cosmeticsOpen
         && !this.autoBuyOpen
         && !this.ircOpen
         && !(this.eventsPanel.openAnim.getValue() > 0.05F)
         && !(this.cosmeticsPanel.openAnim.getValue() > 0.05F)
         && !(this.autoBuyPanel.openAnim.getValue() > 0.05F)
         && !(this.ircPanel.openAnim.getValue() > 0.05F)) {
         if (this.colorDrag != 0) {
            this.colorDrag = 0;
            return true;
         }

         for (GuiPanel var5 : this.panels) {
            if (var5.a(var2, var3, click.button())) {
               return true;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (this.closing) {
         return true;
      }

      float var6 = (float)(click.x() * RenderUtil.e());
      float var7 = (float)(click.y() * RenderUtil.e());
      if (WatermarkConfigModal.isOpen() && WatermarkConfigModal.a(var6, var7, click.button(), offsetX, offsetY)) {
         return true;
      }

      if (!this.eventsOpen
         && !this.cosmeticsOpen
         && !this.autoBuyOpen
         && !this.ircOpen
         && !(this.eventsPanel.openAnim.getValue() > 0.05F)
         && !(this.cosmeticsPanel.openAnim.getValue() > 0.05F)
         && !(this.autoBuyPanel.openAnim.getValue() > 0.05F)
         && !(this.ircPanel.openAnim.getValue() > 0.05F)) {
         if (this.themeAnim.getValue() > 0.1F && this.colorDrag != 0) {
            if (this.colorDrag == 1) {
               this.a(var6, var7);
               return true;
            }

            if (this.colorDrag == 2) {
               this.a(var7);
               return true;
            }
         }

         for (GuiPanel var9 : this.panels) {
            if (var9.b(var6, var7, click.button())) {
               return true;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.closing) {
         return true;
      }

      float var9 = (float)(mouseX * RenderUtil.e());
      float var10 = (float)(mouseY * RenderUtil.e());
      if (WatermarkConfigModal.isOpen() && WatermarkConfigModal.a(var9, var10, verticalAmount)) {
         return true;
      }

      if (this.eventsOpen) {
         return this.eventsPanel.a(verticalAmount) ? true : true;
      }

      if (this.cosmeticsOpen) {
         return this.cosmeticsPanel.a(verticalAmount) ? true : true;
      }

      if (this.autoBuyOpen) {
         return this.autoBuyPanel.a(verticalAmount) ? true : true;
      }

      if (this.ircOpen) {
         return this.ircPanel.a(var9, var10, horizontalAmount, verticalAmount) ? true : true;
      }

      if (!(this.eventsPanel.openAnim.getValue() > 0.05F)
         && !(this.cosmeticsPanel.openAnim.getValue() > 0.05F)
         && !(this.autoBuyPanel.openAnim.getValue() > 0.05F)
         && !(this.ircPanel.openAnim.getValue() > 0.05F)) {
         for (GuiPanel var12 : this.panels) {
            if (var12.a(var9, var10, verticalAmount)) {
               return true;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static String w() {
      try {
         return MinecraftClient.getInstance().keyboard.getClipboard();
      } catch (Exception var3) {
         try {
            return GLFW.glfwGetClipboardString(MinecraftClient.getInstance().getWindow().getHandle());
         } catch (Exception var2) {
            return "";
         }
      }
   }

   private static void a(String var0) {
      if (var0 != null) {
         try {
            MinecraftClient.getInstance().keyboard.setClipboard(var0);
         } catch (Exception var4) {
            try {
               GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), var0);
            } catch (Exception var3) {
            }
         }
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.closing) {
         if (input.key() == 256) {
            this.q();
         }

         return true;
      } else {
         if (this.autoBuyOpen && this.autoBuyPanel.a(input)) {
            return true;
         }

         if (this.ircOpen && this.ircPanel.a(input)) {
            return true;
         }

         int var2;
         if ((
                  var2 = (input.modifiers() & 2) == 0
                        && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) != 1
                        && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 345) != 1
                     ? 0
                     : 1
               )
               != 0
            && input.key() == 70) {
            this.searchFocused = !this.searchFocused;
            searchSelectedAll = false;
            return true;
         }

         if (this.searchFocused) {
            if (var2 != 0 && input.key() == 65) {
               searchSelectedAll = true;
               return true;
            }

            if (var2 == 0 || input.key() != 67 && input.key() != 88) {
               if (var2 != 0 && input.key() == 86) {
                  String var14 = w();
                  if (var14 != null) {
                     String var15 = var14.replace("\r", "").replace("\n", "");
                     if (searchSelectedAll) {
                        this.searchBuffer.setLength(0);
                        searchSelectedAll = false;
                     }

                     this.searchBuffer.append(var15);
                  }

                  return true;
               } else {
                  if (input.key() == 257 || input.key() == 335 || input.key() == 256) {
                     this.searchFocused = false;
                     searchSelectedAll = false;
                  } else if (input.key() == 259 || input.key() == 261) {
                     if (searchSelectedAll) {
                        this.searchBuffer.setLength(0);
                        searchSelectedAll = false;
                     } else if (this.searchBuffer.length() > 0) {
                        this.searchBuffer.deleteCharAt(this.searchBuffer.length() - 1);
                     }
                  }

                  return true;
               }
            } else {
               a(this.searchBuffer.toString());
               if (input.key() == 88) {
                  this.searchBuffer.setLength(0);
                  searchSelectedAll = false;
               }

               return true;
            }
         } else if (GuiPanel.typingNumber != null) {
            if (var2 != 0 && input.key() == 65) {
               numberSelectedAll = true;
               return true;
            }

            if (var2 == 0 || input.key() != 67 && input.key() != 88) {
               if (var2 != 0 && input.key() == 86) {
                  String var12 = w();
                  if (var12 != null) {
                     String var13 = var12.replace("\r", "").replace("\n", "");
                     if (numberSelectedAll) {
                        GuiPanel.numberInputBuffer.setLength(0);
                        numberSelectedAll = false;
                     }

                     char[] var4 = var13.toCharArray();
                     int var5 = var4.length;

                     for (int var6 = 0; var6 < var5; var6++) {
                        char var7;
                        if ((var7 = var4[var6]) >= '0' && var7 <= '9' || var7 == '.' || var7 == ',' || var7 == '-') {
                           GuiPanel.numberInputBuffer.append(var7);
                        }
                     }
                  }

                  return true;
               } else {
                  if (input.key() == 257 || input.key() == 335 || input.key() == 256) {
                     GuiPanel.a();
                     numberSelectedAll = false;
                  } else if (input.key() == 259 || input.key() == 261) {
                     if (numberSelectedAll) {
                        GuiPanel.numberInputBuffer.setLength(0);
                        numberSelectedAll = false;
                     } else if (GuiPanel.numberInputBuffer.length() > 0) {
                        GuiPanel.numberInputBuffer.deleteCharAt(GuiPanel.numberInputBuffer.length() - 1);
                     }
                  }

                  return true;
               }
            } else {
               a(GuiPanel.numberInputBuffer.toString());
               if (input.key() == 88) {
                  GuiPanel.numberInputBuffer.setLength(0);
                  numberSelectedAll = false;
               }

               return true;
            }
         } else if (TextSetting.LISTENING != null) {
            if (var2 != 0 && input.key() == 65) {
               textSelectedAll = true;
               return true;
            }

            if (var2 == 0 || input.key() != 67 && input.key() != 88) {
               if (var2 != 0 && input.key() == 86) {
                  String var10 = w();
                  if (var10 != null) {
                     String var11 = var10.replace("\r", "").replace("\n", "");
                     if (textSelectedAll) {
                        TextSetting.LISTENING.setValue(var11);
                        textSelectedAll = false;
                     } else {
                        TextSetting.LISTENING.setValue(TextSetting.LISTENING.getText() + var11);
                     }
                  }

                  return true;
               } else {
                  if (input.key() == 257 || input.key() == 335 || input.key() == 256) {
                     TextSetting.LISTENING = null;
                     textSelectedAll = false;
                  } else if (input.key() == 259 || input.key() == 261) {
                     if (textSelectedAll) {
                        TextSetting.LISTENING.setValue("");
                        textSelectedAll = false;
                     } else {
                        String var9 = TextSetting.LISTENING.getText();
                        if (!var9.isEmpty()) {
                           TextSetting.LISTENING.setValue(var9.substring(0, var9.length() - 1));
                        }
                     }
                  }

                  return true;
               }
            } else {
               a(TextSetting.LISTENING.getText());
               if (input.key() == 88) {
                  TextSetting.LISTENING.setValue("");
                  textSelectedAll = false;
               }

               return true;
            }
         } else {
            if (BindSetting.LISTENING != null) {
               int var8 = input.key() != 256 && input.key() != 261 && input.key() != 259 ? 0 : 1;
               BindSetting.LISTENING.setKey(var8 != 0 ? -1 : input.key());
               BindSetting.LISTENING = null;
               return true;
            }

            if (this.listeningPanel != null && this.listeningPanel.listening != null) {
               int var3 = input.key() != 256 && input.key() != 261 && input.key() != 259 ? 0 : 1;
               this.listeningPanel.listening.setKeybind(var3 != 0 ? 0 : input.key());
               this.listeningPanel.listening = null;
               this.listeningPanel = null;
               return true;
            }

            if (input.key() == 256) {
               if (this.autoBuyOpen) {
                  this.autoBuyOpen = false;
               } else if (this.eventsOpen) {
                  this.eventsOpen = false;
               } else if (this.cosmeticsOpen) {
                  this.cosmeticsOpen = false;
               } else if (this.ircOpen) {
                  this.ircOpen = false;
               } else if (this.themeOpen) {
                  this.themeOpen = false;
               } else {
                  this.close();
               }

               return true;
            } else {
               return super.keyPressed(input);
            }
         }
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.closing) {
         return true;
      }

      if (this.autoBuyOpen) {
         return this.autoBuyPanel.a(input) ? true : true;
      }

      if (this.ircOpen) {
         return this.ircPanel.a(input) ? true : true;
      }

      if (this.searchFocused) {
         if (input.isValidChar()) {
            if (searchSelectedAll) {
               this.searchBuffer.setLength(0);
               searchSelectedAll = false;
            }

            this.searchBuffer.append(input.asString());
         }

         return true;
      } else if (GuiPanel.typingNumber == null) {
         if (TextSetting.LISTENING != null) {
            if (input.isValidChar()) {
               if (textSelectedAll) {
                  TextSetting.LISTENING.setValue("");
                  textSelectedAll = false;
               }

               TextSetting.LISTENING.setValue(TextSetting.LISTENING.getText() + input.asString());
            }

            return true;
         } else {
            return super.charTyped(input);
         }
      } else {
         if (input.isValidChar()) {
            if (numberSelectedAll) {
               GuiPanel.numberInputBuffer.setLength(0);
               numberSelectedAll = false;
            }

            String var2 = input.asString();
            char[] var3 = var2.toCharArray();
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; var5++) {
               char var6;
               if ((var6 = var3[var5]) >= '0' && var6 <= '9' || var6 == '.' || var6 == ',' || var6 == '-') {
                  GuiPanel.numberInputBuffer.append(var6);
               }
            }
         }

         return true;
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   private static int a(int var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0 & 16777215;
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
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

   private static Color a(Color var0, Color var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      return new Color(
         (int)(var0.getRed() + (var1.getRed() - var0.getRed()) * var2),
         (int)(var0.getGreen() + (var1.getGreen() - var0.getGreen()) * var2),
         (int)(var0.getBlue() + (var1.getBlue() - var0.getBlue()) * var2),
         (int)(var0.getAlpha() + (var1.getAlpha() - var0.getAlpha()) * var2)
      );
   }

   public static boolean a(float var0, float var1, float var2, float var3) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      return EventManager.clickGui != null && var4.currentScreen == EventManager.clickGui && !EventManager.clickGui.closing
         ? EventManager.clickGui.b(var0, var1, var2, var3)
         : false;
   }

   public boolean b(float var1, float var2, float var3, float var4) {
      for (GuiPanel var6 : this.panels) {
         if (var6.open.getValue() > 0.01F) {
            float var16 = 270.0F;
            float var15 = 125.0F;
            float var14 = var6.y;
            float var13 = var6.x;
            float var12 = var4;
            float var11 = var3;
            float var10 = var2;
            float var9 = var1;
            if (var1 < var13 + var15 && var9 + var11 > var13 && var10 < var14 + var16 && var10 + var12 > var14) {
               return true;
            }
         }
      }

      if (this.themeOpen || this.themeAnim.getValue() > 0.01F) {
         float var76 = 140.0F;
         float var69 = 125.0F;
         float var62 = this.themeY;
         float var55 = this.themeX;
         float var48 = var4;
         float var41 = var3;
         float var34 = var2;
         float var27 = var1;
         if (var1 < var55 + var69 && var27 + var41 > var55 && var34 < var62 + var76 && var34 + var48 > var62) {
            return true;
         }
      }

      float var77 = 18.0F;
      float var70 = 120.0F;
      float var63 = this.searchY;
      float var56 = this.searchX;
      float var49 = var4;
      float var42 = var3;
      float var35 = var2;
      float var28 = var1;
      if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
         return true;
      }

      if (this.eventsOpen || this.eventsPanel.openAnim.getValue() > 0.01F) {
         float var17 = (RenderUtil.a() - 360.0F) / 2.0F;
         float var20 = (RenderUtil.b() - 195.0F) / 2.0F;
         float var7 = RenderUtil.a() - var17 + 40.0F;
         float var8 = var17 + (1.0F - this.eventsPanel.openAnim.getValue()) * var7;
         var77 = 195.0F;
         var70 = 360.0F;
         var63 = var20;
         var56 = var8;
         var49 = var4;
         var42 = var3;
         var35 = var2;
         var28 = var1;
         if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
            return true;
         }
      }

      if (this.cosmeticsOpen || this.cosmeticsPanel.openAnim.getValue() > 0.01F) {
         float var18 = (RenderUtil.a() - 380.0F) / 2.0F;
         float var21 = (RenderUtil.b() - 210.0F) / 2.0F;
         float var23 = RenderUtil.a() - var18 + 40.0F;
         float var25 = var18 + (1.0F - this.cosmeticsPanel.openAnim.getValue()) * var23;
         var77 = 210.0F;
         var70 = 380.0F;
         var63 = var21;
         var56 = var25;
         var49 = var4;
         var42 = var3;
         var35 = var2;
         var28 = var1;
         if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
            return true;
         }
      }

      if (this.autoBuyOpen || this.autoBuyPanel.openAnim.getValue() > 0.01F) {
         float var19 = (RenderUtil.a() - 390.0F) / 2.0F;
         float var22 = (RenderUtil.b() - 220.0F) / 2.0F;
         float var24 = RenderUtil.a() - var19 + 40.0F;
         float var26 = var19 + (1.0F - this.autoBuyPanel.openAnim.getValue()) * var24;
         var77 = 220.0F;
         var70 = 390.0F;
         var63 = var22;
         var56 = var26;
         var49 = var4;
         var42 = var3;
         var35 = var2;
         var28 = var1;
         if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
            return true;
         }
      }

      if (!this.eventsOpen) {
         var77 = 18.0F;
         var70 = 58.0F;
         var63 = this.eventsBtnY;
         var56 = this.eventsBtnX;
         var49 = var4;
         var42 = var3;
         var35 = var2;
         var28 = var1;
         if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
            return true;
         }
      }

      if (!this.cosmeticsOpen) {
         var77 = 18.0F;
         var70 = 58.0F;
         var63 = this.cosmeticsBtnY;
         var56 = this.cosmeticsBtnX;
         var49 = var4;
         var42 = var3;
         var35 = var2;
         var28 = var1;
         if (var1 < var56 + var70 && var28 + var42 > var56 && var35 < var63 + var77 && var35 + var49 > var63) {
            return true;
         }
      }

      return false;
   }

   private static boolean a(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return var0 < var4 + var6 && var0 + var2 > var4 && var1 < var5 + var7 && var1 + var3 > var5;
   }

   static final class a {
      float x1;
      float y1;
      float x2;
      float y2;
      int type;
      Object data;

      void a(float var1, float var2, float var3, float var4, int var5, Object var6) {
         this.x1 = var1;
         this.y1 = var2;
         this.x2 = var3;
         this.y2 = var4;
         this.type = var5;
         this.data = var6;
      }
   }
}
