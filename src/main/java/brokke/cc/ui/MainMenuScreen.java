package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.core.User;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfFont;
import brokke.cc.font.MsdfManager;
import brokke.cc.manager.AccountManager;
import brokke.cc.manager.CommandManager;
import brokke.cc.render.BlurPipeline;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MainMenuScreen extends Screen {
   private static final Identifier LOGO_TEX = Identifier.of("brokke", "textures/watermark_icon.png");
   private static final Identifier DISCORD_TEX = Identifier.of("brokke", "textures/icons/discord.png");
   private static final Identifier TELEGRAM_TEX = Identifier.of("brokke", "textures/icons/telegram.png");
   private static final Identifier YOUTUBE_TEX = Identifier.of("brokke", "textures/icons/youtube.png");
   private static final Identifier[] WALLPAPERS = new Identifier[]{
      Identifier.of("brokke", "textures/wallpapers/wp_1.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_2.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_3.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_4.png")
   };
   private static final Identifier[] WALLPAPERS_BLUR = new Identifier[]{
      Identifier.of("brokke", "textures/wallpapers/wp_1_blur.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_2_blur.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_3_blur.png"),
      Identifier.of("brokke", "textures/wallpapers/wp_4_blur.png")
   };
   private static final Path WP_CONFIG = FabricLoader.getInstance().getConfigDir().resolve("brokke").resolve("wallpaper.txt");
   private static int selectedWallpaperIndex = 0;
   private static final String NICK_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
   private final Animation openAnim = new Animation();
   private final Animation singleplayerHover = new Animation();
   private final Animation multiplayerHover = new Animation();
   private final Animation accountsHover = new Animation();
   private final Animation optionsHover = new Animation();
   private final Animation quitHover = new Animation();
   private final Animation discordHover = new Animation();
   private final Animation telegramHover = new Animation();
   private final Animation youtubeHover = new Animation();
   private final Animation wallpaperBtnHover = new Animation();
   private boolean accountModalOpen = false;
   private final Animation accountModalAnim = new Animation();
   private String accountInput = "";
   private String searchQuery = "";
   private int focusedInput = 1;
   private boolean selectAll = false;
   private int selectedTab = 0;
   private final Animation tabIndicatorAnim = new Animation();
   private final Animation scrollAnim = new Animation();
   private float scrollTarget = 0.0F;
   private boolean wallpaperModalOpen = false;
   private final Animation wallpaperModalAnim = new Animation();
   private final Animation carouselIndexAnim = new Animation();
   private int carouselTargetIndex = 0;
   private final Animation selectBtnHover = new Animation();
   private int lastRenderedWpIndex = -1;
   private int prevWallpaperIndex = 0;
   private final Animation wpTransitionAnim = new Animation();
   private final Random random = new Random();
   private boolean initialized = false;

   private static Color a() {
      return Theme.getInstance().getAccent();
   }

   public MainMenuScreen() {
      super(Text.literal("Main Menu"));
      this.openAnim.setEasing(Easing.CUBIC_OUT);
      this.accountModalAnim.setEasing(Easing.EXPO_OUT);
      this.wallpaperModalAnim.setEasing(Easing.EXPO_OUT);
      this.carouselIndexAnim.setEasing(Easing.EXPO_OUT);
      this.singleplayerHover.setEasing(Easing.SMOOTH_STEP);
      this.multiplayerHover.setEasing(Easing.SMOOTH_STEP);
      this.accountsHover.setEasing(Easing.SMOOTH_STEP);
      this.optionsHover.setEasing(Easing.SMOOTH_STEP);
      this.quitHover.setEasing(Easing.SMOOTH_STEP);
      this.discordHover.setEasing(Easing.SMOOTH_STEP);
      this.telegramHover.setEasing(Easing.SMOOTH_STEP);
      this.youtubeHover.setEasing(Easing.SMOOTH_STEP);
      this.wallpaperBtnHover.setEasing(Easing.SMOOTH_STEP);
      this.tabIndicatorAnim.setEasing(Easing.SPRING_SOFT);
      this.scrollAnim.setEasing(Easing.SMOOTH_STEP);
      this.selectBtnHover.setEasing(Easing.SMOOTH_STEP);
      this.wpTransitionAnim.setEasing(Easing.CUBIC_OUT);
      this.wpTransitionAnim.reset(1.0F);
   }

   @Override
   protected void init() {
      if (!this.initialized) {
         this.openAnim.reset(0.0F);
         AccountManager.e();
         b();
         this.carouselTargetIndex = selectedWallpaperIndex;
         this.carouselIndexAnim.reset(selectedWallpaperIndex);
         this.lastRenderedWpIndex = selectedWallpaperIndex;
         this.prevWallpaperIndex = selectedWallpaperIndex;
         this.wpTransitionAnim.reset(1.0F);
         this.initialized = true;
      }

      BlurPipeline.i();
   }

   private static void b() {
      try {
         if (Files.exists(WP_CONFIG)) {
            String var0 = Files.readString(WP_CONFIG).trim();
            selectedWallpaperIndex = Math.clamp(Integer.parseInt(var0), 0, WALLPAPERS.length - 1);
         }
      } catch (Exception var1) {
      }
   }

   private static void c() {
      try {
         if (WP_CONFIG.getParent() != null) {
            Files.createDirectories(WP_CONFIG.getParent());
         }

         Files.writeString(WP_CONFIG, String.valueOf(selectedWallpaperIndex));
      } catch (Exception var1) {
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
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
      float var5 = var2 * RenderUtil.e();
      float var6 = var3 * RenderUtil.e();
      float var7 = RenderUtil.a();
      float var8 = RenderUtil.b();
      if (this.lastRenderedWpIndex == -1) {
         this.lastRenderedWpIndex = selectedWallpaperIndex;
         this.prevWallpaperIndex = selectedWallpaperIndex;
         this.wpTransitionAnim.reset(1.0F);
      } else if (this.lastRenderedWpIndex != selectedWallpaperIndex) {
         this.prevWallpaperIndex = this.lastRenderedWpIndex;
         this.lastRenderedWpIndex = selectedWallpaperIndex;
         this.wpTransitionAnim.reset(0.0F);
      }

      this.wpTransitionAnim.animate(1.0F, var4, 7.0F);
      float var9 = Math.clamp(this.wpTransitionAnim.getValue(), 0.0F, 1.0F);
      RenderUtil.a(0.0F, 0.0F, var7, var8, 0.0F, new Color[]{new Color(10, 12, 18, 255)});
      int var10 = Math.clamp(this.prevWallpaperIndex, 0, WALLPAPERS_BLUR.length - 1);
      int var11 = Math.clamp(selectedWallpaperIndex, 0, WALLPAPERS_BLUR.length - 1);
      if (var9 < 1.0F && var10 != var11) {
         RenderUtil.a(0.0F, 0.0F, var7, var8, WALLPAPERS_BLUR[var10], 0.0F, Color.WHITE);
         RenderUtil.a(0.0F, 0.0F, var7, var8, WALLPAPERS_BLUR[var11], 0.0F, a(Color.WHITE, 255.0F * var9));
      } else {
         RenderUtil.a(0.0F, 0.0F, var7, var8, WALLPAPERS_BLUR[var11], 0.0F, Color.WHITE);
      }

      this.openAnim.animate(1.0F, var4, 4.0F);
      float var12 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      this.accountModalAnim.animate(this.accountModalOpen ? 1.0F : 0.0F, var4, this.accountModalOpen ? 6.0F : 7.0F, Easing.EXPO_OUT);
      float var13 = this.accountModalAnim.getValue();
      this.wallpaperModalAnim.animate(this.wallpaperModalOpen ? 1.0F : 0.0F, var4, this.wallpaperModalOpen ? 6.0F : 7.0F, Easing.EXPO_OUT);
      float var14 = this.wallpaperModalAnim.getValue();
      float var15 = Math.max(var13, var14);
      float var16 = var12 * Math.clamp(1.0F - var15 * 1.6F, 0.0F, 1.0F);
      int var17 = !this.accountModalOpen && !this.wallpaperModalOpen && !(var15 > 0.05F) ? 0 : 1;
      float var18 = 168.0F;
      float var19 = 25.0F;
      float var20 = 9.0F;
      float var21 = 5.5F;
      float var22 = 53.0F + (var19 * 3.0F + var21 * 2.0F) + var21 + 23.0F;
      float var23 = (var7 - var18) / 2.0F;
      float var24 = (var8 - var22) / 2.0F + (1.0F - var12) * 12.0F;
      if (var16 > 0.005F) {
         float var49;
         byte var10000;
         label290: {
            String var25 = "Shelka new";
            float var26 = Msdf.a(MsdfManager.BOLD, var25, 6.5F);
            float var27 = 8.0F;
            float var28 = 8.5F;
            float var29 = 4.5F;
            float var31 = var28 + var29 + var26 + var27 * 2.0F;
            float var32 = 15.5F;
            float var33 = 7.75F;
            float var34 = (var7 - var31) / 2.0F;
            float var35 = var24;
            RenderUtil.a(var34, var35, var31, var32, var33, new Color[]{new Color(20, 24, 36, (int)(115.0F * var16))});
            RenderUtil.a(var34, var35, var31, var32, var33, 0.75F, new Color[]{new Color(255, 255, 255, (int)(48.0F * var16))});
            float var36;
            float var37 = var36 = var34 + var27;
            float var38 = var35 + (var32 - var28) / 2.0F;
            RenderUtil.a(var37, var38, var28, LOGO_TEX, 0.0F, a(a(), 255.0F * var16));
            float var39 = var37 + var28 + var29;
            float var40 = var35 + (var32 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
            a(var1, MsdfManager.BOLD, var39, var40, var25, 6.5F, var16, 2.5F, 0.4F, 0.0F);
            float var41 = var35 + var32 + 10.0F;
            String var42 = User.getInstance().getUsername();
            String var43 = "Welcome, ";
            float var44 = Msdf.a(MsdfManager.BOLD, var43, 12.5F);
            float var45 = Msdf.a(MsdfManager.BOLD, var42, 12.5F);
            float var46 = var44 + var45;
            float var47 = (var7 - var46) / 2.0F;
            a(var1, MsdfManager.BOLD, var47, var41, var43, 12.5F, var16, 2.2F, 0.2F, 0.0F);
            a(var1, MsdfManager.BOLD, var47 + var44, var41, var42, 12.5F, var16, 2.2F, 0.2F, -var43.length() * 0.2F);
            float var48;
            var49 = var48 = var41 + 22.0F;
            if (var17 == 0) {
               float var96 = var19;
               float var95 = var18;
               float var94 = var49;
               float var93 = var23;
               float var92 = var6;
               float var91 = var5;
               if (var5 >= var93 && var91 <= var93 + var95 && var92 >= var94 && var92 <= var94 + var96) {
                  var10000 = 1;
                  break label290;
               }
            }

            var10000 = 0;
         }

         float var51;
         label277: {
            byte var50 = var10000;
            this.singleplayerHover.animate(var50 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var23, var49, var18, var19, var20, MsdfManager.ICONS2, "e", "Singleplayer", this.singleplayerHover.getValue(), var16);
            var51 = var49 + var19 + var21;
            if (var17 == 0) {
               float var137 = var19;
               float var129 = var18;
               float var121 = var51;
               float var113 = var23;
               float var105 = var6;
               float var97 = var5;
               if (var5 >= var113 && var97 <= var113 + var129 && var105 >= var121 && var105 <= var121 + var137) {
                  var10000 = 1;
                  break label277;
               }
            }

            var10000 = 0;
         }

         float var53;
         label264: {
            byte var52 = var10000;
            this.multiplayerHover.animate(var52 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var23, var51, var18, var19, var20, MsdfManager.ICONS2, "g", "Multiplayer", this.multiplayerHover.getValue(), var16);
            var53 = var51 + var19 + var21;
            if (var17 == 0) {
               float var138 = var19;
               float var130 = var18;
               float var122 = var53;
               float var114 = var23;
               float var106 = var6;
               float var98 = var5;
               if (var5 >= var114 && var98 <= var114 + var130 && var106 >= var122 && var106 <= var122 + var138) {
                  var10000 = 1;
                  break label264;
               }
            }

            var10000 = 0;
         }

         float var55;
         float var56;
         float var57;
         float var58;
         float var59;
         label251: {
            byte var54 = var10000;
            this.accountsHover.animate(var54 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var23, var53, var18, var19, var20, MsdfManager.ICONS2, "d", "Accounts", this.accountsHover.getValue(), var16);
            var55 = var53 + var19 + var21;
            var56 = (var18 - var21) / 2.0F;
            var57 = 23.0F;
            var58 = 8.5F;
            var59 = var23;
            if (var17 == 0) {
               float var139 = var57;
               float var131 = var56;
               float var123 = var55;
               float var115 = var59;
               float var107 = var6;
               float var99 = var5;
               if (var5 >= var115 && var99 <= var115 + var131 && var107 >= var123 && var107 <= var123 + var139) {
                  var10000 = 1;
                  break label251;
               }
            }

            var10000 = 0;
         }

         float var61;
         label238: {
            byte var60 = var10000;
            this.optionsHover.animate(var60 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var59, var55, var56, var57, var58, "Options", this.optionsHover.getValue(), var16);
            var61 = var23 + var56 + var21;
            if (var17 == 0) {
               float var140 = var57;
               float var132 = var56;
               float var124 = var55;
               float var116 = var61;
               float var108 = var6;
               float var100 = var5;
               if (var5 >= var116 && var100 <= var116 + var132 && var108 >= var124 && var108 <= var124 + var140) {
                  var10000 = 1;
                  break label238;
               }
            }

            var10000 = 0;
         }

         float var63;
         float var64;
         float var65;
         float var67;
         float var68;
         float var69;
         label225: {
            byte var62 = var10000;
            this.quitHover.animate(var62 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var61, var55, var56, var57, var58, "Exit", this.quitHover.getValue(), var16);
            var63 = 20.0F;
            var64 = 7.5F;
            var65 = 5.0F;
            float var66 = var63 * 3.0F + var65 * 2.0F;
            var67 = var7 - var66 - 14.0F;
            var68 = 14.0F;
            var69 = var67;
            if (var17 == 0) {
               float var141 = var63;
               float var133 = var63;
               float var125 = var68;
               float var117 = var69;
               float var109 = var6;
               float var101 = var5;
               if (var5 >= var117 && var101 <= var117 + var133 && var109 >= var125 && var109 <= var125 + var141) {
                  var10000 = 1;
                  break label225;
               }
            }

            var10000 = 0;
         }

         float var71;
         label212: {
            byte var70 = var10000;
            this.discordHover.animate(var70 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var69, var68, var63, var63, var64, DISCORD_TEX, this.discordHover.getValue(), var16, new Color(88, 101, 242));
            var71 = var67 + var63 + var65;
            if (var17 == 0) {
               float var142 = var63;
               float var134 = var63;
               float var126 = var68;
               float var118 = var71;
               float var110 = var6;
               float var102 = var5;
               if (var5 >= var118 && var102 <= var118 + var134 && var110 >= var126 && var110 <= var126 + var142) {
                  var10000 = 1;
                  break label212;
               }
            }

            var10000 = 0;
         }

         float var73;
         label199: {
            byte var72 = var10000;
            this.telegramHover.animate(var72 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var71, var68, var63, var63, var64, TELEGRAM_TEX, this.telegramHover.getValue(), var16, new Color(36, 161, 222));
            var73 = var67 + (var63 + var65) * 2.0F;
            if (var17 == 0) {
               float var143 = var63;
               float var135 = var63;
               float var127 = var68;
               float var119 = var73;
               float var111 = var6;
               float var103 = var5;
               if (var5 >= var119 && var103 <= var119 + var135 && var111 >= var127 && var111 <= var127 + var143) {
                  var10000 = 1;
                  break label199;
               }
            }

            var10000 = 0;
         }

         float var75;
         float var76;
         float var77;
         float var78;
         float var79;
         label186: {
            byte var74 = var10000;
            this.youtubeHover.animate(var74 != 0 ? 1.0F : 0.0F, var4, 12.0F);
            this.a(var1, var73, var68, var63, var63, var64, YOUTUBE_TEX, this.youtubeHover.getValue(), var16, new Color(255, 68, 68));
            var75 = 78.0F;
            var76 = 20.0F;
            var77 = 7.5F;
            var78 = var7 - var75 - 14.0F;
            var79 = var8 - var76 - 14.0F;
            if (var17 == 0) {
               float var144 = var76;
               float var136 = var75;
               float var128 = var79;
               float var120 = var78;
               float var112 = var6;
               float var104 = var5;
               if (var5 >= var120 && var104 <= var120 + var136 && var112 >= var128 && var112 <= var128 + var144) {
                  var10000 = 1;
                  break label186;
               }
            }

            var10000 = 0;
         }

         byte var80 = var10000;
         this.wallpaperBtnHover.animate(var80 != 0 ? 1.0F : 0.0F, var4, 12.0F);
         float var81 = this.wallpaperBtnHover.getValue();
         Color var82 = a(new Color(20, 24, 36, (int)(115.0F * var16)), new Color(34, 42, 60, (int)(155.0F * var16)), var81);
         Color var83 = a(new Color(255, 255, 255, (int)(38.0F * var16)), new Color(255, 255, 255, (int)(75.0F * var16)), var81);
         RenderUtil.a(var78, var79, var75, var76, var77, new Color[]{var82});
         RenderUtil.a(var78, var79, var75, var76, var77, 0.75F, new Color[]{var83});
         float var84 = 7.5F;
         float var85 = var78 + 7.5F;
         float var86 = var79 + (var76 - Msdf.a(MsdfManager.ICONS2, var84)) / 2.0F;
         Color var87 = a(new Color(185, 190, 205), a(), var81);
         RenderUtil.a(var1, MsdfManager.ICONS2, var85, var86, "f", var84, a(var87, 255.0F * var16));
         float var88 = var85 + 11.5F;
         float var89 = var79 + (var76 - Msdf.a(MsdfManager.MONTSERRAT, 6.75F)) / 2.0F;
         Color var90 = a(new Color(210, 215, 228), Color.WHITE, var81);
         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var88, var89, "Смена обоев", 6.75F, a(var90, 255.0F * var16));
      }

      if (var13 > 0.005F) {
         this.a(var1, var7, var8, var5, var6, var13, var4);
      }

      if (var14 > 0.005F) {
         this.b(var1, var7, var8, var5, var6, var14, var4);
      }

      super.render(var1, var2, var3, var4);
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5, float var6, Identifier var7, float var8, float var9, Color var10) {
      Color var11 = new Color(20, 24, 36, (int)(115.0F * var9));
      Color var12 = a(new Color(34, 42, 60, (int)(155.0F * var9)), a(var10, 55.0F * var9), 0.35F);
      Color var13 = a(var11, var12, var8);
      Color var14 = new Color(255, 255, 255, (int)(38.0F * var9));
      Color var15 = a(new Color(255, 255, 255, (int)(75.0F * var9)), a(var10, 180.0F * var9), 0.6F);
      Color var16 = a(var14, var15, var8);
      RenderUtil.a(var2, var3, var4, var5, var6, new Color[]{var13});
      RenderUtil.a(var2, var3, var4, var5, var6, 0.75F, new Color[]{var16});
      float var17 = 9.5F;
      float var18 = var2 + (var4 - var17) / 2.0F;
      float var19 = var3 + (var5 - var17) / 2.0F;
      Color var20 = a(new Color(195, 200, 215), var10, var8);
      RenderUtil.a(var18, var19, var17, var7, 0.0F, a(var20, 255.0F * var9));
   }

   private void a(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   private void a(
      DrawContext var1, float var2, float var3, float var4, float var5, float var6, MsdfFont var7, String var8, String var9, float var10, float var11
   ) {
      Color var12 = new Color(20, 24, 36, (int)(115.0F * var11));
      Color var13 = new Color(34, 42, 60, (int)(155.0F * var11));
      Color var14 = a(var12, var13, var10);
      Color var15 = new Color(255, 255, 255, (int)(38.0F * var11));
      Color var16 = new Color(255, 255, 255, (int)(75.0F * var11));
      Color var17 = a(var15, var16, var10);
      RenderUtil.a(var2, var3, var4, var5, var6, new Color[]{var14});
      RenderUtil.a(var2, var3, var4, var5, var6, 0.75F, new Color[]{var17});
      float var18 = 8.5F;
      float var19 = var2 + 13.0F;
      float var20 = var3 + (var5 - Msdf.a(var7, var18)) / 2.0F;
      Color var21 = a(new Color(185, 190, 205), a(), var10);
      RenderUtil.a(var1, var7, var19, var20, var8, var18, a(var21, 255.0F * var11));
      float var22 = 7.5F;
      float var23 = Msdf.a(MsdfManager.MONTSERRAT, var9, var22);
      float var24 = var2 + (var4 - var23) / 2.0F;
      float var25 = var3 + (var5 - Msdf.a(MsdfManager.MONTSERRAT, var22)) / 2.0F;
      Color var26 = a(new Color(210, 215, 228), Color.WHITE, var10);
      RenderUtil.a(var1, MsdfManager.MONTSERRAT, var24, var25, var9, var22, a(var26, 255.0F * var11));
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5, float var6, String var7, float var8, float var9) {
      Color var10 = new Color(20, 24, 36, (int)(115.0F * var9));
      Color var11 = new Color(34, 42, 60, (int)(155.0F * var9));
      Color var12 = a(var10, var11, var8);
      Color var13 = new Color(255, 255, 255, (int)(38.0F * var9));
      Color var14 = new Color(255, 255, 255, (int)(75.0F * var9));
      Color var15 = a(var13, var14, var8);
      RenderUtil.a(var2, var3, var4, var5, var6, new Color[]{var12});
      RenderUtil.a(var2, var3, var4, var5, var6, 0.75F, new Color[]{var15});
      float var16 = 7.25F;
      float var17 = Msdf.a(MsdfManager.MONTSERRAT, var7, var16);
      float var18 = var2 + (var4 - var17) / 2.0F;
      float var19 = var3 + (var5 - Msdf.a(MsdfManager.MONTSERRAT, var16)) / 2.0F;
      Color var20 = a(new Color(205, 210, 225), Color.WHITE, var8);
      RenderUtil.a(var1, MsdfManager.MONTSERRAT, var18, var19, var7, var16, a(var20, 255.0F * var9));
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = Math.clamp(var6, 0.0F, 1.0F);
      RenderUtil.a(0.0F, 0.0F, var2, var3, 0.0F, new Color[]{a(0, 0, 0, 130.0F * var8)});
      float var9 = 210.0F;
      float var10 = 265.0F;
      float var11 = 12.0F;
      float var12 = (var2 - var9) / 2.0F;
      float var13 = (var3 - var10) / 2.0F + (1.0F - var8) * 6.0F;
      if (Theme.getInstance().getGuiStyle() == Theme.a.BLUR) {
         RenderUtil.a(var12, var13, var9, var10, var11, 12.0F);
      }

      RenderUtil.a(var12, var13, var9, var10, var11, new Color[]{a(18, 22, 34, 135.0F * var8)});
      RenderUtil.a(var12, var13, var9, var10, var11, 0.75F, new Color[]{a(255, 255, 255, 50.0F * var8)});
      float var14 = 12.0F;
      float var15 = var13 + 12.0F;
      RenderUtil.a(var1, MsdfManager.BOLD, var12 + var14, var15, "ДОБАВИТЬ АККАУНТ", 6.25F, a(new Color(155, 160, 175), 255.0F * var8));
      float var16 = 14.0F;
      float var17 = var12 + var9 - var14 - var16;
      float var18 = var15 - 3.0F;
      float var109 = var16;
      float var108 = var16;
      float var107 = var18;
      float var106 = var17;
      float var105 = var5;
      float var104 = var4;
      int var19;
      if ((var19 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0) != 0) {
         RenderUtil.a(var17, var18, var16, var16, 4.0F, new Color[]{a(255, 255, 255, 20.0F * var8)});
         RenderUtil.a(var17, var18, var16, var16, 4.0F, 0.75F, new Color[]{a(255, 255, 255, 40.0F * var8)});
      }

      a(var17 + var16 / 2.0F, var18 + var16 / 2.0F, 2.75F, 1.15F, a(var19 != 0 ? Color.WHITE : new Color(130, 135, 150), 255.0F * var8));
      float var20 = 3.5F;
      float var21 = 2.0F;
      float var22 = var17 - 18.0F;
      float var23 = var18 + (var16 - (var20 * 2.0F + var21)) / 2.0F;
      RenderUtil.a(var22, var23, var20, var20, 1.0F, new Color[]{a(new Color(242, 80, 34), 255.0F * var8)});
      RenderUtil.a(var22 + var20 + var21, var23, var20, var20, 1.0F, new Color[]{a(new Color(127, 186, 0), 255.0F * var8)});
      RenderUtil.a(var22, var23 + var20 + var21, var20, var20, 1.0F, new Color[]{a(new Color(0, 164, 239), 255.0F * var8)});
      RenderUtil.a(var22 + var20 + var21, var23 + var20 + var21, var20, var20, 1.0F, new Color[]{a(new Color(255, 185, 0), 255.0F * var8)});
      float var24 = var13 + 28.0F;
      float var25 = 20.0F;
      float var26 = 4.5F;
      float var27 = var9 - var14 * 2.0F - (var25 * 2.0F + var26 * 2.0F);
      float var28 = 20.0F;
      float var29 = 6.0F;
      Color var30 = this.focusedInput == 1 ? a(a(30, 36, 52, 160.0F * var8), a(a(), 25.0F * var8), 0.25F) : a(22, 26, 38, 115.0F * var8);
      Color var31 = this.focusedInput == 1 ? a(a(), 190.0F * var8) : a(255, 255, 255, 38.0F * var8);
      RenderUtil.a(var12 + var14, var24, var27, var28, var29, new Color[]{var30});
      RenderUtil.a(var12 + var14, var24, var27, var28, var29, 0.75F, new Color[]{var31});
      int var32 = System.currentTimeMillis() / 450L % 2L == 0L ? 1 : 0;
      String var33 = this.accountInput + (this.focusedInput == 1 && var32 != 0 && !this.selectAll ? "|" : "");
      float var34 = var24 + var28 / 2.0F;
      float var35 = var12 + var14 + 8.0F;
      float var36 = var34 - Msdf.a(MsdfManager.MONTSERRAT, 7.0F) / 2.0F + 0.3F;
      if (this.accountInput.isEmpty() && this.focusedInput != 1) {
         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var35, var36, "Никнейм...", 7.0F, a(new Color(110, 115, 130), 255.0F * var8));
      } else {
         if (this.focusedInput == 1 && this.selectAll && !this.accountInput.isEmpty()) {
            float var37 = Msdf.a(MsdfManager.MONTSERRAT, this.accountInput, 7.0F);
            RenderUtil.a(var35 - 1.5F, var34 - 5.0F, var37 + 3.0F, 10.0F, 2.0F, new Color[]{a(a(), 150.0F * var8)});
         }

         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var35, var36, var33, 7.0F, a(Color.WHITE, 255.0F * var8));
      }

      float var110 = var12 + var14 + var27 + var26;
      var109 = var25;
      var108 = var25;
      var107 = var24;
      var106 = var110;
      var105 = var5;
      var104 = var4;
      int var38;
      Color var39 = (var38 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0) != 0
         ? a(a(), Color.WHITE, 0.18F)
         : a();
      RenderUtil.a(var110, var24, var25, var25, var29, new Color[]{a(var39, 235.0F * var8)});
      RenderUtil.a(var110, var24, var25, var25, var29, 0.75F, new Color[]{a(var38 != 0 ? Color.WHITE : a(), (var38 != 0 ? 140.0F : 80.0F) * var8)});
      if (var38 != 0) {
         RenderUtil.a(var110 + 1.5F, var24 + 1.0F, var25 - 3.0F, 2.0F, 1.0F, new Color[]{a(255, 255, 255, 45.0F * var8)});
      }

      float var40 = var110 + var25 / 2.0F;
      float var41 = var24 + var25 / 2.0F;
      RenderUtil.a(var40 - 4.0F, var41 - 0.75F, 8.0F, 1.5F, 0.75F, new Color[]{a(Color.WHITE, 255.0F * var8)});
      RenderUtil.a(var40 - 0.75F, var41 - 4.0F, 1.5F, 8.0F, 0.75F, new Color[]{a(Color.WHITE, 255.0F * var8)});
      float var42 = var110 + var25 + var26;
      var109 = var25;
      var108 = var25;
      var107 = var24;
      var106 = var42;
      var105 = var5;
      var104 = var4;
      int var43;
      Color var44 = (var43 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0) != 0
         ? a(a(38, 46, 66, 175.0F * var8), a(a(), 40.0F * var8), 0.35F)
         : a(24, 28, 42, 130.0F * var8);
      RenderUtil.a(var42, var24, var25, var25, var29, new Color[]{var44});
      RenderUtil.a(var42, var24, var25, var25, var29, 0.75F, new Color[]{var43 != 0 ? a(a(), 190.0F * var8) : a(255, 255, 255, 38.0F * var8)});
      if (var43 != 0) {
         RenderUtil.a(var42 + 1.5F, var24 + 1.0F, var25 - 3.0F, 2.0F, 1.0F, new Color[]{a(255, 255, 255, 25.0F * var8)});
      }

      float var45 = var42 + var25 / 2.0F;
      float var46 = var24 + var25 / 2.0F;
      Color var47 = var43 != 0 ? a() : new Color(190, 195, 210);
      RenderUtil.a(var45 - 4.5F, var46 - 4.5F, 9.0F, 9.0F, 2.2F, 1.0F, new Color[]{a(var47, 255.0F * var8)});
      RenderUtil.a(var45 - 0.65F, var46 - 0.65F, 1.3F, 1.3F, 0.65F, new Color[]{a(var47, 255.0F * var8)});
      RenderUtil.a(var45 - 2.5F, var46 - 2.5F, 1.2F, 1.2F, 0.6F, new Color[]{a(var47, 255.0F * var8)});
      RenderUtil.a(var45 + 1.3F, var46 + 1.3F, 1.2F, 1.2F, 0.6F, new Color[]{a(var47, 255.0F * var8)});
      RenderUtil.a(var45 + 1.3F, var46 - 2.5F, 1.2F, 1.2F, 0.6F, new Color[]{a(var47, 255.0F * var8)});
      RenderUtil.a(var45 - 2.5F, var46 + 1.3F, 1.2F, 1.2F, 0.6F, new Color[]{a(var47, 255.0F * var8)});
      float var48 = var24 + var28 + 5.0F;
      float var49 = var9 - var14 * 2.0F;
      float var50 = 18.0F;
      float var51 = var48 + var50 / 2.0F;
      Color var52 = this.focusedInput == 2 ? a(a(30, 36, 52, 160.0F * var8), a(a(), 25.0F * var8), 0.25F) : a(22, 26, 38, 110.0F * var8);
      Color var53 = this.focusedInput == 2 ? a(a(), 190.0F * var8) : a(255, 255, 255, 35.0F * var8);
      RenderUtil.a(var12 + var14, var48, var49, var50, 5.5F, new Color[]{var52});
      RenderUtil.a(var12 + var14, var48, var49, var50, 5.5F, 0.75F, new Color[]{var53});
      float var54 = var12 + var14 + 8.0F;
      float var55 = var51;
      Color var56 = this.focusedInput == 2 ? a() : new Color(125, 130, 145);
      RenderUtil.a(var54 - 2.75F, var55 - 3.25F, 5.5F, 5.5F, 2.75F, 0.9F, new Color[]{a(var56, 220.0F * var8)});
      RenderUtil.a(var54 + 1.5F, var55 + 1.0F, 1.1F, 1.1F, 0.55F, new Color[]{a(var56, 220.0F * var8)});
      RenderUtil.a(var54 + 2.2F, var55 + 1.7F, 1.1F, 1.1F, 0.55F, new Color[]{a(var56, 220.0F * var8)});
      RenderUtil.a(var54 + 2.9F, var55 + 2.4F, 1.1F, 1.1F, 0.55F, new Color[]{a(var56, 220.0F * var8)});
      float var57 = var54 + 8.5F;
      float var58 = var51 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F) / 2.0F + 0.25F;
      String var59 = this.searchQuery + (this.focusedInput == 2 && var32 != 0 && !this.selectAll ? "|" : "");
      if (this.searchQuery.isEmpty() && this.focusedInput != 2) {
         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var57, var58, "Поиск...", 6.5F, a(new Color(110, 115, 130), 255.0F * var8));
      } else {
         if (this.focusedInput == 2 && this.selectAll && !this.searchQuery.isEmpty()) {
            float var60 = Msdf.a(MsdfManager.MONTSERRAT, this.searchQuery, 6.5F);
            RenderUtil.a(var57 - 1.5F, var51 - 4.5F, var60 + 3.0F, 9.0F, 2.0F, new Color[]{a(a(), 150.0F * var8)});
         }

         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var57, var58, var59, 6.5F, a(Color.WHITE, 255.0F * var8));
      }

      float var111 = var48 + var50 + 8.0F;
      float var61 = Msdf.a(MsdfManager.BOLD, "Все", 6.75F);
      float var62 = Msdf.a(MsdfManager.BOLD, "Избранные", 6.75F);
      float var63 = 16.0F;
      float var64;
      float var65 = (var64 = var12 + var14 + 4.0F) + var61 + var63;
      float var10002 = var64 - 3.0F;
      float var10003 = var111 - 2.0F;
      float var10004 = var61 + 6.0F;
      var109 = 13.0F;
      var108 = var10004;
      var107 = var10003;
      var106 = var10002;
      var105 = var5;
      var104 = var4;
      int var66 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0;
      var10002 = var65 - 3.0F;
      var10003 = var111 - 2.0F;
      var10004 = var62 + 6.0F;
      var109 = 13.0F;
      var108 = var10004;
      var107 = var10003;
      var106 = var10002;
      var105 = var5;
      var104 = var4;
      int var67 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0;
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var64,
         var111,
         "Все",
         6.75F,
         a(this.selectedTab == 0 ? a() : (var66 != 0 ? Color.WHITE : new Color(130, 135, 150)), 255.0F * var8)
      );
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var65,
         var111,
         "Избранные",
         6.75F,
         a(this.selectedTab == 1 ? a() : (var67 != 0 ? Color.WHITE : new Color(130, 135, 150)), 255.0F * var8)
      );
      this.tabIndicatorAnim.animate(this.selectedTab == 0 ? 0.0F : 1.0F, var7, 14.0F);
      float var68 = this.tabIndicatorAnim.getValue();
      float var69 = var64 + (var65 - var64) * var68;
      float var70 = var61 + (var62 - var61) * var68;
      RenderUtil.a(var69, var111 + 9.5F, var70, 1.5F, 0.75F, new Color[]{a(a(), 255.0F * var8)});
      float var71 = var111 + 16.0F;
      float var72 = var13 + var10 - var71 - 10.0F;
      float var73 = var9 - var14 * 2.0F;
      ArrayList var74 = new ArrayList();
      String var75 = AccountManager.getActiveUsername();
      if (var75 == null || var75.isEmpty()) {
         var75 = this.client != null && this.client.getSession() != null ? this.client.getSession().getUsername() : "";
      }

      for (AccountManager.a var77 : AccountManager.getAccounts()) {
         if ((this.selectedTab != 1 || var77.isFavorite())
            && (this.searchQuery.isEmpty() || var77.getUsername().toLowerCase().contains(this.searchQuery.toLowerCase()))) {
            var74.add(var77);
         }
      }

      float var112 = 26.0F;
      float var113 = 4.0F;
      float var78 = var74.size() * (var112 + var113);
      float var79 = Math.max(0.0F, var78 - var72);
      this.scrollTarget = Math.clamp(this.scrollTarget, -var79, 0.0F);
      this.scrollAnim.animate(this.scrollTarget, var7, 12.0F);
      Scissor.b(var12 + var14 - 1.0F, var71, var73 + 2.0F, var72);
      float var80 = var71 + this.scrollAnim.getValue();

      for (AccountManager.a var82 : (Iterable<AccountManager.a>)(Iterable<?>)(var74)) {
         if (var80 + var112 >= var71 && var80 <= var71 + var72) {
            boolean var83 = var82.getUsername().equalsIgnoreCase(var75);
            var10002 = var12 + var14;
            var109 = var112;
            var108 = var73;
            var107 = var80;
            var106 = var10002;
            var105 = var5;
            var104 = var4;
            int var84 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0;
            Color var85 = var83
               ? a(a(28, 34, 50, 180.0F * var8), a(a(), 40.0F * var8), 0.35F)
               : (var84 != 0 ? a(a(30, 36, 52, 140.0F * var8), a(a(), 20.0F * var8), 0.2F) : a(22, 26, 38, 100.0F * var8));
            Color var86 = var83
               ? a(a(), 210.0F * var8)
               : (var84 != 0 ? a(a(255, 255, 255, 50.0F * var8), a(a(), 80.0F * var8), 0.35F) : a(255, 255, 255, 28.0F * var8));
            RenderUtil.a(var12 + var14, var80, var73, var112, 7.0F, new Color[]{var85});
            RenderUtil.a(var12 + var14, var80, var73, var112, 7.0F, 0.75F, new Color[]{var86});
            float var87 = 16.0F;
            float var88 = var12 + var14 + 5.0F;
            float var89 = var80 + (var112 - var87) / 2.0F;
            float var90 = 4.0F;
            MinecraftClient var91 = this.client != null ? this.client : MinecraftClient.getInstance();
            GpuTextureView var92 = SkinCache.a(var91, var82.getUsername());
            if (var92 != null) {
               int var93 = (int)Math.clamp(255.0F * var8, 0.0F, 255.0F) << 24 | 16777215;
               TexturePipeline.b(RenderUtil.c(), var88, var89, var87, var87, var92, var93, var90, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
               TexturePipeline.b(RenderUtil.c(), var88, var89, var87, var87, var92, var93, var90, 0.0F, 0.625F, 0.125F, 0.75F, 0.25F);
               RenderUtil.a(var88, var89, var87, var87, var90, 0.75F, new Color[]{var83 ? a(a(), 255.0F * var8) : a(255, 255, 255, 30.0F * var8)});
            } else {
               int var119 = Math.abs(var82.getUsername().hashCode());
               Color var94 = new Color(50 + var119 % 140, 50 + var119 / 100 % 140, 120 + var119 / 1000 % 120);
               Color var95 = var83 ? a(var94, a(), 0.5F) : var94;
               RenderUtil.a(var88, var89, var87, var87, var90, new Color[]{a(var95, 230.0F * var8)});
               RenderUtil.a(var88, var89, var87, var87, var90, 0.75F, new Color[]{var83 ? a(a(), 255.0F * var8) : a(255, 255, 255, 30.0F * var8)});
               String var96 = var82.getUsername().substring(0, Math.min(1, var82.getUsername().length())).toUpperCase();
               float var97 = Msdf.a(MsdfManager.BOLD, var96, 7.5F);
               float var98 = var89 + (var87 - Msdf.a(MsdfManager.BOLD, 7.5F)) / 2.0F - 0.75F;
               RenderUtil.a(var1, MsdfManager.BOLD, var88 + (var87 - var97) / 2.0F, var98, var96, 7.5F, a(Color.WHITE, 255.0F * var8));
            }

            float var120 = var88 + var87 + 6.0F;
            float var121 = var80 + 5.25F;
            RenderUtil.a(var1, MsdfManager.BOLD, var120, var121, var82.getUsername(), 6.75F, a(Color.WHITE, 255.0F * var8));
            float var122 = var121 + 8.0F;
            if (var83) {
               RenderUtil.a(var1, MsdfManager.MONTSERRAT, var120, var122, "Активен", 5.25F, a(a(), 255.0F * var8));
            } else {
               RenderUtil.a(var1, MsdfManager.MONTSERRAT, var120, var122, "Оффлайн", 5.25F, a(new Color(135, 140, 155), 255.0F * var8));
            }

            float var123 = 16.0F;
            float var124;
            float var125 = (var124 = var12 + var14 + var73 - var123 - 4.0F) - var123 - 3.0F;
            float var99 = var80 + (var112 - var123) / 2.0F;
            var109 = var123;
            var108 = var123;
            var107 = var99;
            var106 = var125;
            var105 = var5;
            var104 = var4;
            int var100;
            if ((var100 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0) != 0) {
               RenderUtil.a(var125, var99, var123, var123, 4.5F, new Color[]{a(255, 215, 50, 26.0F * var8)});
               RenderUtil.a(var125, var99, var123, var123, 4.5F, 0.75F, new Color[]{a(255, 215, 50, 95.0F * var8)});
            } else if (var82.isFavorite()) {
               RenderUtil.a(var125, var99, var123, var123, 4.5F, new Color[]{a(255, 215, 50, 18.0F * var8)});
               RenderUtil.a(var125, var99, var123, var123, 4.5F, 0.75F, new Color[]{a(255, 215, 50, 55.0F * var8)});
            } else {
               RenderUtil.a(var125, var99, var123, var123, 4.5F, new Color[]{a(255, 255, 255, 8.0F * var8)});
               RenderUtil.a(var125, var99, var123, var123, 4.5F, 0.75F, new Color[]{a(255, 255, 255, 18.0F * var8)});
            }

            Color var101 = var82.isFavorite() ? new Color(255, 205, 45) : (var100 != 0 ? new Color(255, 225, 110) : new Color(125, 130, 145));
            a(var125 + var123 / 2.0F, var99 + var123 / 2.0F, 4.0F, a(var101, 255.0F * var8));
            var109 = var123;
            var108 = var123;
            var107 = var99;
            var106 = var124;
            var105 = var5;
            var104 = var4;
            int var102;
            if ((var102 = var4 >= var106 && var104 <= var106 + var108 && var105 >= var107 && var105 <= var107 + var109 ? 1 : 0) != 0) {
               RenderUtil.a(var124, var99, var123, var123, 4.5F, new Color[]{a(255, 65, 65, 32.0F * var8)});
               RenderUtil.a(var124, var99, var123, var123, 4.5F, 0.75F, new Color[]{a(255, 75, 75, 110.0F * var8)});
            } else {
               RenderUtil.a(var124, var99, var123, var123, 4.5F, new Color[]{a(255, 255, 255, 8.0F * var8)});
               RenderUtil.a(var124, var99, var123, var123, 4.5F, 0.75F, new Color[]{a(255, 255, 255, 18.0F * var8)});
            }

            Color var103 = var102 != 0 ? new Color(255, 80, 80) : new Color(125, 130, 145);
            a(var124 + var123 / 2.0F, var99 + var123 / 2.0F, 3.0F, 1.15F, a(var103, 255.0F * var8));
         }

         var80 += var112 + var113;
      }

      Scissor.c();
      if (var79 > 0.0F) {
         float var114 = 2.0F;
         float var115 = var12 + var9 - var14 + 3.0F;
         float var116 = Math.max(14.0F, var72 * (var72 / var78));
         float var117 = -this.scrollAnim.getValue() / var79;
         float var118 = var71 + var117 * (var72 - var116);
         RenderUtil.a(var115, var71, var114, var72, 1.0F, new Color[]{a(255, 255, 255, 20.0F * var8)});
         RenderUtil.a(var115, var118, var114, var116, 1.0F, new Color[]{a(a(), 190.0F * var8)});
      }
   }

   private void b(DrawContext var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = Math.clamp(var6, 0.0F, 1.0F);
      RenderUtil.a(0.0F, 0.0F, var2, var3, 0.0F, new Color[]{a(0, 0, 0, 125.0F * var8)});
      float var9 = 22.0F;
      float var10 = Msdf.a(MsdfManager.BOLD, "ВЫБОР ОБОЕВ", 7.0F);
      RenderUtil.a(var1, MsdfManager.BOLD, (var2 - var10) / 2.0F, var9, "ВЫБОР ОБОЕВ", 7.0F, a(new Color(210, 215, 230), 230.0F * var8));
      float var11 = var2 - 24.0F;
      float var12 = 20.0F;
      float var10002 = var11 - 6.0F;
      float var10003 = var12 - 6.0F;
      float var43 = 18.0F;
      float var42 = 18.0F;
      float var41 = var10003;
      float var40 = var10002;
      float var39 = var5;
      float var38 = var4;
      int var13 = var4 >= var40 && var38 <= var40 + var42 && var39 >= var41 && var39 <= var41 + var43 ? 1 : 0;
      a(var11, var12, 3.5F, 1.25F, a(var13 != 0 ? Color.WHITE : new Color(140, 145, 160), 255.0F * var8));
      this.carouselIndexAnim.animate(this.carouselTargetIndex, var7, 10.0F, Easing.EXPO_OUT);
      float var14 = this.carouselIndexAnim.getValue();
      float var15 = 180.0F;
      float var16 = 102.0F;
      float var17 = 10.0F;
      float var18 = 196.0F;
      float var19 = (var3 - 20.0F) / 2.0F;

      for (int var20 = 0; var20 < WALLPAPERS.length; var20++) {
         float var21;
         float var22;
         if (!((var22 = Math.abs(var21 = var20 - var14)) > 2.5F)) {
            float var23 = var2 / 2.0F + var21 * var18 - var15 / 2.0F;
            float var24 = var19 - var16 / 2.0F;
            float var25 = Math.clamp(1.0F - var22 * 0.1F, 0.78F, 1.0F);
            float var26 = var15 * var25;
            float var27 = var16 * var25;
            float var28 = var23 + (var15 - var26) / 2.0F;
            float var29 = var24 + (var16 - var27) / 2.0F;
            float var30 = var8 * Math.clamp(1.0F - var22 * 0.4F, 0.35F, 1.0F);
            RenderUtil.a(var28, var29, var26, var27, WALLPAPERS[var20], var17 * var25, a(Color.WHITE, 255.0F * var30));
            var43 = var27;
            var42 = var26;
            var41 = var29;
            var40 = var28;
            var39 = var5;
            var38 = var4;
            int var31 = var4 >= var40 && var38 <= var40 + var42 && var39 >= var41 && var39 <= var41 + var43 ? 1 : 0;
            if (var20 == selectedWallpaperIndex) {
               RenderUtil.a(var28, var29, var26, var27, var17 * var25, 1.5F, new Color[]{a(a(), 255.0F * var30)});
            } else if (var31 != 0) {
               RenderUtil.a(var28, var29, var26, var27, var17 * var25, 1.0F, new Color[]{a(Color.WHITE, 160.0F * var30)});
            } else {
               RenderUtil.a(var28, var29, var26, var27, var17 * var25, 0.75F, new Color[]{a(255, 255, 255, 45.0F * var30)});
            }
         }
      }

      float var44 = var19 + var16 / 2.0F + 16.0F;
      float var45 = WALLPAPERS.length * 5.5F + (WALLPAPERS.length - 1) * 6.0F;
      float var46 = (var2 - var45) / 2.0F;
      int var47 = Math.clamp(Math.round(var14), 0, WALLPAPERS.length - 1);

      for (int var48 = 0; var48 < WALLPAPERS.length; var48++) {
         float var50 = var46 + var48 * 11.5F;
         float var52 = var48 == var47 ? 5.5F : 4.0F;
         float var54 = var44 + (5.5F - var52) / 2.0F;
         Color var56 = var48 == var47 ? Color.WHITE : a(255, 255, 255, 90.0F);
         RenderUtil.a(var50, var54, var52, var52, var52 / 2.0F, new Color[]{a(var56, 255.0F * var8)});
      }

      float var49 = 95.0F;
      float var51 = 22.0F;
      float var53 = 11.0F;
      float var55 = (var2 - var49) / 2.0F;
      float var57 = var44 + 14.0F;
      int var58 = var47 == selectedWallpaperIndex ? 1 : 0;
      var43 = var51;
      var42 = var49;
      var41 = var57;
      var40 = var55;
      var39 = var5;
      var38 = var4;
      int var59 = var4 >= var40 && var38 <= var40 + var42 && var39 >= var41 && var39 <= var41 + var43 ? 1 : 0;
      this.selectBtnHover.animate(var59 != 0 ? 1.0F : 0.0F, var7, 12.0F);
      float var60 = this.selectBtnHover.getValue();
      Color var32 = var58 != 0 ? new Color(24, 28, 38, (int)(220.0F * var8)) : new Color(38, 42, 56, (int)(220.0F * var8));
      Color var33 = var58 != 0 ? new Color(30, 36, 48, (int)(240.0F * var8)) : new Color(52, 58, 76, (int)(240.0F * var8));
      a(var32, var33, var60);
      RenderUtil.a(var55, var57, var49, var51, var53, 5.0F);
      RenderUtil.a(
         var55, var57, var49, var51, var53, 0.75F, new Color[]{a(var58 != 0 ? a() : Color.WHITE, (var58 != 0 ? 160.0F : 50.0F + 60.0F * var60) * var8)}
      );
      String var35 = var58 != 0 ? "Установлено" : "Выбрать";
      Color var36 = var58 != 0 ? a() : Color.WHITE;
      float var37 = Msdf.a(MsdfManager.BOLD, var35, 6.75F);
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var55 + (var49 - var37) / 2.0F,
         var57 + (var51 - Msdf.a(MsdfManager.BOLD, 6.75F)) / 2.0F,
         var35,
         6.75F,
         a(var36, 255.0F * var8)
      );
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int var3 = click.button() == 0 ? 1 : 0;
      int var4 = click.button() == 1 ? 1 : 0;
      if (var3 == 0 && var4 == 0) {
         return super.mouseClicked(click, doubled);
      }

      float var5 = (float)(click.x() * RenderUtil.e());
      float var6 = (float)(click.y() * RenderUtil.e());
      float var7 = RenderUtil.a();
      float var8 = RenderUtil.b();
      if (this.wallpaperModalOpen) {
         if (var3 == 0) {
            return true;
         }

         float var43 = var7 - 24.0F;
         float var45 = 20.0F;
         float var254 = var43 - 8.0F;
         float var257 = var45 - 8.0F;
         float var245 = 20.0F;
         float var221 = 20.0F;
         float var197 = var257;
         float var173 = var254;
         float var149 = var6;
         float var125 = var5;
         if (var5 >= var173 && var125 <= var173 + var221 && var149 >= var197 && var149 <= var197 + var245) {
            this.wallpaperModalOpen = false;
            return true;
         }

         float var47 = 180.0F;
         float var49 = 102.0F;
         float var51 = 196.0F;
         float var53 = (var8 - 20.0F) / 2.0F;

         for (int var56 = 0; var56 < WALLPAPERS.length; var56++) {
            float var60 = var56 - this.carouselIndexAnim.getValue();
            float var64 = var7 / 2.0F + var60 * var51 - var47 / 2.0F;
            float var68 = var53 - var49 / 2.0F;
            float var74 = Math.abs(var60);
            float var80 = Math.clamp(1.0F - var74 * 0.1F, 0.78F, 1.0F);
            float var84 = var47 * var80;
            float var88 = var49 * var80;
            float var91 = var64 + (var47 - var84) / 2.0F;
            float var94 = var68 + (var49 - var88) / 2.0F;
            var245 = var88;
            var221 = var84;
            var197 = var94;
            var173 = var91;
            var149 = var6;
            var125 = var5;
            if (var5 >= var173 && var125 <= var173 + var221 && var149 >= var197 && var149 <= var197 + var245) {
               this.carouselTargetIndex = var56;
               selectedWallpaperIndex = var56;
               c();
               return true;
            }
         }

         float var57 = var53 + var49 / 2.0F + 16.0F;
         float var61 = WALLPAPERS.length * 5.5F + (WALLPAPERS.length - 1) * 6.0F;
         float var65 = (var7 - var61) / 2.0F;

         for (int var69 = 0; var69 < WALLPAPERS.length; var69++) {
            float var75 = var65 + var69 * 11.5F;
            var254 = var75 - 4.0F;
            var257 = var57 - 4.0F;
            var245 = 13.0F;
            var221 = 13.0F;
            var197 = var257;
            var173 = var254;
            var149 = var6;
            var125 = var5;
            if (var5 >= var173 && var125 <= var173 + var221 && var149 >= var197 && var149 <= var197 + var245) {
               this.carouselTargetIndex = var69;
               selectedWallpaperIndex = var69;
               c();
               return true;
            }
         }

         float var70 = 95.0F;
         float var76 = 22.0F;
         float var81 = (var7 - var70) / 2.0F;
         float var85 = var57 + 14.0F;
         var245 = var76;
         var221 = var70;
         var197 = var85;
         var173 = var81;
         var149 = var6;
         var125 = var5;
         if (var5 >= var173 && var125 <= var173 + var221 && var149 >= var197 && var149 <= var197 + var245) {
            selectedWallpaperIndex = Math.clamp(this.carouselTargetIndex, 0, WALLPAPERS.length - 1);
            c();
            this.wallpaperModalOpen = false;
            return true;
         } else {
            this.wallpaperModalOpen = false;
            return true;
         }
      } else if (this.accountModalOpen) {
         float var42 = 210.0F;
         float var44 = 265.0F;
         float var46 = (var7 - var42) / 2.0F;
         float var48 = (var8 - var44) / 2.0F;
         float var50 = 12.0F;
         float var233 = var44;
         float var209 = var42;
         float var185 = var48;
         float var161 = var46;
         float var137 = var6;
         float var113 = var5;
         if (!(var5 >= var161) || !(var113 <= var161 + var209) || !(var137 >= var185) || !(var137 <= var185 + var233)) {
            this.accountModalOpen = false;
            return true;
         }

         if (var3 != 0) {
            this.selectAll = false;
            float var52 = var48 + 12.0F;
            float var54 = 14.0F;
            float var58 = var46 + var42 - var50 - var54;
            float var62 = var52 - 3.0F;
            var233 = var54;
            var209 = var54;
            var185 = var62;
            var161 = var58;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.accountModalOpen = false;
               return true;
            }

            float var66 = var48 + 28.0F;
            float var71 = 20.0F;
            float var77 = 4.5F;
            float var82 = var42 - var50 * 2.0F - (var71 * 2.0F + var77 * 2.0F);
            float var86 = 20.0F;
            float var10002 = var46 + var50;
            var233 = var86;
            var209 = var82;
            var185 = var66;
            var161 = var10002;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.focusedInput = 1;
               return true;
            }

            float var89 = var46 + var50 + var82 + var77;
            var233 = var71;
            var209 = var71;
            var185 = var66;
            var161 = var89;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.e();
               return true;
            }

            float var92 = var89 + var71 + var77;
            var233 = var71;
            var209 = var71;
            var185 = var66;
            var161 = var92;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.d();
               this.focusedInput = 1;
               return true;
            }

            float var95 = var66 + var86 + 5.0F;
            float var26 = var42 - var50 * 2.0F;
            float var27 = 18.0F;
            var10002 = var46 + var50;
            var233 = var27;
            var209 = var26;
            var185 = var95;
            var161 = var10002;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.focusedInput = 2;
               return true;
            }

            float var99 = var95 + var27 + 8.0F;
            float var100 = Msdf.a(MsdfManager.BOLD, "Все", 6.75F);
            float var101 = Msdf.a(MsdfManager.BOLD, "Избранные", 6.75F);
            float var102 = 16.0F;
            float var103;
            float var104 = (var103 = var46 + var50 + 4.0F) + var100 + var102;
            var10002 = var103 - 4.0F;
            float var10003 = var99 - 4.0F;
            float var10004 = var100 + 8.0F;
            var233 = 16.0F;
            var209 = var10004;
            var185 = var10003;
            var161 = var10002;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.selectedTab = 0;
               this.scrollTarget = 0.0F;
               return true;
            }

            var10002 = var104 - 4.0F;
            var10003 = var99 - 4.0F;
            var10004 = var101 + 8.0F;
            var233 = 16.0F;
            var209 = var10004;
            var185 = var10003;
            var161 = var10002;
            var137 = var6;
            var113 = var5;
            if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
               this.selectedTab = 1;
               this.scrollTarget = 0.0F;
               return true;
            }
         }

         float var55 = var48 + 28.0F + 20.0F + 5.0F + 18.0F + 8.0F + 16.0F;
         float var59 = var48 + var44 - var55 - 10.0F;
         float var63 = var42 - var50 * 2.0F;
         float var252 = var46 + var50;
         var233 = var59;
         var209 = var63;
         var185 = var55;
         var161 = var252;
         var137 = var6;
         var113 = var5;
         if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
            ArrayList var67 = new ArrayList();

            for (AccountManager.a var78 : AccountManager.getAccounts()) {
               if ((this.selectedTab != 1 || var78.isFavorite())
                  && (this.searchQuery.isEmpty() || var78.getUsername().toLowerCase().contains(this.searchQuery.toLowerCase()))) {
                  var67.add(var78);
               }
            }

            float var73 = 26.0F;
            float var79 = 4.0F;
            float var83 = var55 + this.scrollAnim.getValue();

            for (AccountManager.a var90 : (Iterable<AccountManager.a>)(Iterable<?>)(var67)) {
               if (var83 + var73 >= var55 && var83 <= var55 + var59) {
                  var252 = var46 + var50;
                  var233 = var73;
                  var209 = var63;
                  var185 = var83;
                  var161 = var252;
                  var137 = var6;
                  var113 = var5;
                  if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
                     if (var4 != 0) {
                        if (this.client != null) {
                           this.client.keyboard.setClipboard(var90.getUsername());
                        }

                        return true;
                     }

                     float var93 = 16.0F;
                     float var96;
                     float var97 = (var96 = var46 + var50 + var63 - var93 - 4.0F) - var93 - 3.0F;
                     float var98 = var83 + (var73 - var93) / 2.0F;
                     var233 = var93;
                     var209 = var93;
                     var185 = var98;
                     var161 = var96;
                     var137 = var6;
                     var113 = var5;
                     if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
                        AccountManager.a(var90);
                        return true;
                     }

                     var233 = var93;
                     var209 = var93;
                     var185 = var98;
                     var161 = var97;
                     var137 = var6;
                     var113 = var5;
                     if (var5 >= var161 && var113 <= var161 + var209 && var137 >= var185 && var137 <= var185 + var233) {
                        AccountManager.b(var90);
                        return true;
                     }

                     AccountManager.d(var90.getUsername());
                     return true;
                  }
               }

               var83 += var73 + var79;
            }
         }

         this.focusedInput = 0;
         this.selectAll = false;
         return true;
      } else {
         if (var3 == 0) {
            return false;
         }

         float var9 = 78.0F;
         float var10 = 20.0F;
         float var11 = var7 - var9 - 14.0F;
         float var12 = var8 - var10 - 14.0F;
         float var41 = var10;
         float var40 = var9;
         float var39 = var12;
         float var38 = var11;
         float var37 = var6;
         float var36 = var5;
         if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
            this.wallpaperModalOpen = true;
            this.carouselTargetIndex = selectedWallpaperIndex;
            this.carouselIndexAnim.reset(selectedWallpaperIndex);
            return true;
         }

         float var13 = 20.0F;
         float var14 = 5.0F;
         float var15 = var13 * 3.0F + var14 * 2.0F;
         float var16 = var7 - var15 - 14.0F;
         float var17 = 14.0F;
         float var18 = var16;
         var41 = var13;
         var40 = var13;
         var39 = var17;
         var38 = var18;
         var37 = var6;
         var36 = var5;
         if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
            this.a("https://discord.gg/ZSeAA6NNaB");
            return true;
         }

         float var19 = var16 + var13 + var14;
         var41 = var13;
         var40 = var13;
         var39 = var17;
         var38 = var19;
         var37 = var6;
         var36 = var5;
         if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
            this.a("https://t.me/Brokkecc");
            return true;
         }

         float var20 = var16 + (var13 + var14) * 2.0F;
         var41 = var13;
         var40 = var13;
         var39 = var17;
         var38 = var20;
         var37 = var6;
         var36 = var5;
         if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
            this.a("https://www.youtube.com/@Brokannn");
            return true;
         }

         float var21 = 168.0F;
         float var22 = 25.0F;
         float var23 = 5.5F;
         float var24 = 53.0F + (var22 * 3.0F + var23 * 2.0F) + var23 + 23.0F;
         float var25 = (var7 - var21) / 2.0F;
         float var28 = (var8 - var24) / 2.0F + 15.5F + 10.0F + 22.0F;
         var41 = var22;
         var40 = var21;
         var39 = var28;
         var38 = var25;
         var37 = var6;
         var36 = var5;
         if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
            if (this.client != null) {
               this.client.setScreen(new SelectWorldScreen(this));
            }

            return true;
         } else {
            float var29 = var28 + var22 + var23;
            var41 = var22;
            var40 = var21;
            var39 = var29;
            var38 = var25;
            var37 = var6;
            var36 = var5;
            if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
               if (this.client != null) {
                  this.client.setScreen(new MultiplayerScreen(this));
               }

               return true;
            } else {
               float var30 = var29 + var22 + var23;
               var41 = var22;
               var40 = var21;
               var39 = var30;
               var38 = var25;
               var37 = var6;
               var36 = var5;
               if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
                  this.accountModalOpen = true;
                  this.focusedInput = 1;
                  this.scrollTarget = 0.0F;
                  return true;
               }

               float var31 = var30 + var22 + var23;
               float var32 = (var21 - var23) / 2.0F;
               float var33 = 23.0F;
               float var34 = var25;
               var41 = var33;
               var40 = var32;
               var39 = var31;
               var38 = var34;
               var37 = var6;
               var36 = var5;
               if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
                  if (this.client != null) {
                     this.client.setScreen(new OptionsScreen(this, this.client.options));
                  }

                  return true;
               } else {
                  float var35 = var25 + var32 + var23;
                  var41 = var33;
                  var40 = var32;
                  var39 = var31;
                  var38 = var35;
                  var37 = var6;
                  var36 = var5;
                  if (var5 >= var38 && var36 <= var38 + var40 && var37 >= var39 && var37 <= var39 + var41) {
                     if (this.client != null) {
                        this.client.scheduleStop();
                     }

                     return true;
                  } else {
                     return super.mouseClicked(click, doubled);
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.wallpaperModalOpen) {
         if (verticalAmount != 0.0) {
            this.carouselTargetIndex = Math.clamp(this.carouselTargetIndex - (int)Math.signum(verticalAmount), 0, WALLPAPERS.length - 1);
         }

         return true;
      } else if (this.accountModalOpen) {
         this.scrollTarget += (float)(verticalAmount * 18.0);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.wallpaperModalOpen) {
         if (input.key() == 256) {
            this.wallpaperModalOpen = false;
            return true;
         }

         if (input.key() == 263) {
            this.carouselTargetIndex = Math.clamp(this.carouselTargetIndex - 1, 0, WALLPAPERS.length - 1);
            return true;
         }

         if (input.key() == 262) {
            this.carouselTargetIndex = Math.clamp(this.carouselTargetIndex + 1, 0, WALLPAPERS.length - 1);
            return true;
         }

         if (input.key() != 257 && input.key() != 335 && input.key() != 32) {
            return true;
         }

         selectedWallpaperIndex = Math.clamp(this.carouselTargetIndex, 0, WALLPAPERS.length - 1);
         c();
         this.wallpaperModalOpen = false;
         return true;
      } else if (this.accountModalOpen) {
         boolean var2;
         if ((var2 = this.f()) && input.key() == 70) {
            this.focusedInput = 2;
            this.selectAll = false;
            return true;
         }

         if (var2 && input.key() == 65) {
            if (this.focusedInput == 1 && !this.accountInput.isEmpty()) {
               this.selectAll = true;
            } else if (this.focusedInput == 2 && !this.searchQuery.isEmpty()) {
               this.selectAll = true;
            }

            return true;
         } else if (var2 && input.key() == 67) {
            String var5 = this.focusedInput == 1 ? this.accountInput : (this.focusedInput == 2 ? this.searchQuery : "");
            if (!var5.isEmpty() && this.client != null) {
               this.client.keyboard.setClipboard(var5);
            }

            return true;
         } else if (var2 && input.key() == 86) {
            if (this.client != null) {
               String var3 = this.client.keyboard.getClipboard();
               if (var3 != null && !var3.isEmpty()) {
                  String var4 = var3.replaceAll("[\\r\\n\\t]", "").trim();
                  if (this.focusedInput == 1) {
                     if (this.selectAll) {
                        this.accountInput = var4.length() > 16 ? var4.substring(0, 16) : var4;
                        this.selectAll = false;
                     } else if (this.accountInput.length() + var4.length() <= 16) {
                        this.accountInput = this.accountInput + var4;
                     }
                  } else if (this.focusedInput == 2) {
                     if (this.selectAll) {
                        this.searchQuery = var4.length() > 16 ? var4.substring(0, 16) : var4;
                        this.selectAll = false;
                     } else if (this.searchQuery.length() + var4.length() <= 16) {
                        this.searchQuery = this.searchQuery + var4;
                     }
                  }
               }
            }

            return true;
         } else if (input.key() == 256) {
            if (this.selectAll) {
               this.selectAll = false;
               return true;
            } else {
               this.accountModalOpen = false;
               return true;
            }
         } else if (input.key() != 257 && input.key() != 335) {
            if (input.key() == 259) {
               if (this.focusedInput == 1) {
                  if (this.selectAll) {
                     this.accountInput = "";
                     this.selectAll = false;
                  } else if (!this.accountInput.isEmpty()) {
                     this.accountInput = this.accountInput.substring(0, this.accountInput.length() - 1);
                  }
               } else if (this.focusedInput == 2) {
                  if (this.selectAll) {
                     this.searchQuery = "";
                     this.selectAll = false;
                  } else if (!this.searchQuery.isEmpty()) {
                     this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                  }
               }

               return true;
            } else {
               if (input.key() != 263 && input.key() != 262 && input.key() != 268 && input.key() != 269) {
                  return true;
               }

               this.selectAll = false;
               return true;
            }
         } else {
            if (this.focusedInput == 1) {
               this.e();
            }

            this.selectAll = false;
            return true;
         }
      } else {
         return super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.accountModalOpen && input.isValidChar()) {
         if (this.focusedInput == 1) {
            if (this.selectAll) {
               this.accountInput = input.asString();
               this.selectAll = false;
            } else if (this.accountInput.length() < 16) {
               this.accountInput = this.accountInput + input.asString();
            }
         } else if (this.focusedInput == 2) {
            if (this.selectAll) {
               this.searchQuery = input.asString();
               this.selectAll = false;
            } else if (this.searchQuery.length() < 16) {
               this.searchQuery = this.searchQuery + input.asString();
            }
         }

         return true;
      } else {
         return super.charTyped(input);
      }
   }

   private void d() {
      this.accountInput = AccountManager.c();
      this.selectAll = false;
   }

   private void e() {
      if (this.accountInput != null && !this.accountInput.trim().isEmpty()) {
         AccountManager.c(this.accountInput.trim());
         this.accountInput = "";
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static boolean a(float var0, float var1, float var2, float var3, float var4, float var5) {
      return var0 >= var2 && var0 <= var2 + var4 && var1 >= var3 && var1 <= var3 + var5;
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
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

   private boolean f() {
      long var1;
      return this.client != null && this.client.getWindow() != null
         ? GLFW.glfwGetKey(var1 = this.client.getWindow().getHandle(), 341) == 1 || GLFW.glfwGetKey(var1, 345) == 1
         : false;
   }

   private boolean g() {
      long var1;
      return this.client != null && this.client.getWindow() != null
         ? GLFW.glfwGetKey(var1 = this.client.getWindow().getHandle(), 340) == 1 || GLFW.glfwGetKey(var1, 344) == 1
         : false;
   }

   private static void a(float var0, float var1, float var2, float var3, Color var4) {
      float var5 = 0.35F;

      int var6;
      for (int var7 = -(var6 = Math.max(1, Math.round(var2 / var5))); var7 <= var6; var7++) {
         float var8 = var7 * (var2 / var6);
         RenderUtil.a(var0 + var8 - var3 / 2.0F, var1 + var8 - var3 / 2.0F, var3, var3, var3 / 2.0F, new Color[]{var4});
         RenderUtil.a(var0 + var8 - var3 / 2.0F, var1 - var8 - var3 / 2.0F, var3, var3, var3 / 2.0F, new Color[]{var4});
      }
   }

   private static void a(float var0, float var1, float var2, Color var3) {
      float var4 = var2 * 0.45F;
      float[] var5 = new float[10];
      float[] var6 = new float[10];

      for (int var7 = 0; var7 < 10; var7++) {
         double var8 = (-Math.PI / 2) + var7 * (Math.PI / 5);
         float var10 = var7 % 2 == 0 ? var2 : var4;
         var5[var7] = var0 + (float)(Math.cos(var8) * var10);
         var6[var7] = var1 + (float)(Math.sin(var8) * var10);
      }

      for (int var17 = 0; var17 < 5; var17++) {
         float var19 = var5[var17 * 2];
         float var9 = var6[var17 * 2];
         float var22 = var5[(var17 * 2 + 2) % 10];
         float var11 = var6[(var17 * 2 + 2) % 10];

         for (float var12 = 0.0F; var12 <= 1.0F; var12 += 0.08F) {
            float var13 = var19 + (var0 - var19) * var12;
            float var14 = var9 + (var1 - var9) * var12;
            RenderUtil.a(var13 - 0.6F, var14 - 0.6F, 1.2F, 1.2F, 0.6F, new Color[]{var3});
            float var15 = var22 + (var0 - var22) * var12;
            float var16 = var11 + (var1 - var11) * var12;
            RenderUtil.a(var15 - 0.6F, var16 - 0.6F, 1.2F, 1.2F, 0.6F, new Color[]{var3});
         }
      }

      for (int var18 = 0; var18 < 10; var18++) {
         int var20 = (var18 + 1) % 10;
         float var21 = var5[var18];
         float var23 = var6[var18];
         float var24 = var5[var20];
         float var25 = var6[var20];

         for (float var26 = 0.0F; var26 <= 1.0F; var26 += 0.05F) {
            float var27 = var21 + (var24 - var21) * var26;
            float var28 = var23 + (var25 - var23) * var26;
            RenderUtil.a(var27 - 0.65F, var28 - 0.65F, 1.3F, 1.3F, 0.65F, new Color[]{var3});
         }
      }
   }

   private static void a(DrawContext var0, MsdfFont var1, float var2, float var3, String var4, float var5, float var6, float var7, float var8, float var9) {
      if (var4 != null && !var4.isEmpty() && !(var6 <= 0.005F)) {
         int[] var10 = new int[var4.length()];
         long var11 = System.currentTimeMillis();
         Color var13 = a();
         float[] var14 = Color.RGBtoHSB(var13.getRed(), var13.getGreen(), var13.getBlue(), null);

         for (int var15 = 0; var15 < var4.length(); var15++) {
            float var16;
            float var17 = (float)((Math.sin(var16 = (float)var11 * var7 * 0.001F - var15 * var8 + var9) + 1.0) * 0.5);
            float var18;
            if ((var18 = (var14[0] + (float)Math.sin(var16 * 0.6F) * 0.09F) % 1.0F) < 0.0F) {
               var18++;
            }

            Color var19 = Color.getHSBColor(var18, Math.max(0.4F, var14[1]), Math.min(1.0F, var14[2] + 0.1F));
            Color var20 = a(Color.WHITE, var19, 0.4F + var17 * 0.5F);
            float var21 = (float)Math.pow((Math.sin(var16 + 0.9F) + 1.0) * 0.5, 4.0);
            Color var22 = a(var20, Color.WHITE, var21 * 0.8F);
            var10[var15] = a(var22, 255.0F * var6).getRGB();
         }

         Msdf.a(var0, var1, var4, var2, var3, var5, false, var10);
      }
   }

   private static Color a(int var0, int var1, int var2, float var3) {
      return new Color(var0, var1, var2, (int)Math.clamp(var3, 0.0F, 255.0F));
   }
}
