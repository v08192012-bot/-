package brokke.cc.irc;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.core.User;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.BufferBuilder;

public class IrcPanel {
   public static final float W = 450.0F;
   public static final float H = 250.0F;
   private static final float HEADER_H = 22.0F;
   private static final float SIDEBAR_W = 135.0F;
   private static final float INPUT_H = 20.0F;
   public final Animation openAnim = new Animation();
   private final Animation chatScrollAnim = new Animation();
   private final Animation sidebarScrollAnim = new Animation();
   private IrcPanel.a currentTab = IrcPanel.a.CHATS;
   private float chatScrollTarget = 0.0F;
   private float chatMaxScroll = 0.0F;
   private float sidebarScrollTarget = 0.0F;
   private float sidebarMaxScroll = 0.0F;
   private final StringBuilder inputBuffer = new StringBuilder();
   private boolean inputFocused = false;
   private boolean inputSelectAll = false;
   private final StringBuilder addFriendBuffer = new StringBuilder();
   private boolean addFriendFocused = false;
   private boolean addFriendSelectAll = false;
   private final List<float[]> clickBoxes = new ArrayList<>();
   private final List<Runnable> clickActions = new ArrayList<>();
   private final float[] closeBtnBox = new float[4];
   private String copiedCoordsToast = null;
   private long copiedCoordsTime = 0L;

   public boolean a() {
      return this.openAnim.getValue() > 0.05F && (this.inputFocused || this.addFriendFocused);
   }

   public IrcPanel() {
      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.chatScrollAnim.setEasing(Easing.SMOOTH_STEP);
      this.sidebarScrollAnim.setEasing(Easing.SMOOTH_STEP);
   }

   private static int a(int var0, float var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0 & 16777215;
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5, String var6, float var7, Color var8) {
      MinecraftClient var9 = MinecraftClient.getInstance();
      GpuTextureView var10 = var6 != null && !var6.isEmpty() ? SkinCache.a(var9, var6) : null;
      if (var10 != null) {
         int var11 = (int)Math.clamp(255.0F * var7, 0.0F, 255.0F) << 24 | 16777215;
         TexturePipeline.b(RenderUtil.c(), var2, var3, var4, var4, var10, var11, var5, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
         TexturePipeline.b(RenderUtil.c(), var2, var3, var4, var4, var10, var11, var5, 0.0F, 0.625F, 0.125F, 0.75F, 0.25F);
         RenderUtil.a(var2, var3, var4, var4, var5, 0.5F, a(-1, 35.0F * var7));
      } else {
         String var18 = var6 != null && !var6.isEmpty() ? var6 : "?";
         int var12 = Math.abs(var18.hashCode());
         Color var13 = new Color(40 + var12 % 110, 45 + var12 / 100 % 110, 85 + var12 / 1000 % 110);
         RenderUtil.a(var2, var3, var4, var4, var5, a(var13.getRGB(), 220.0F * var7));
         RenderUtil.a(var2, var3, var4, var4, var5, 0.5F, a(-1, 25.0F * var7));
         String var14 = var18.substring(0, 1).toUpperCase();
         float var15 = var4 * 0.48F;
         float var16 = Msdf.a(MsdfManager.BOLD, var14, var15);
         float var17 = Msdf.a(MsdfManager.BOLD, var15);
         RenderUtil.a(
            var1, MsdfManager.BOLD, var2 + (var4 - var16) / 2.0F, var3 + (var4 - var17) / 2.0F, var14, var15, new Color(245, 245, 250, (int)(255.0F * var7))
         );
      }
   }

   public void a(DrawContext var1, float var2, float var3, int var4, int var5, float var6, boolean var7) {
      this.openAnim.animate(var7 ? 1.0F : 0.0F, var6, 3.8F);
      float var8 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (!(var8 <= 0.005F)) {
         this.clickBoxes.clear();
         this.clickActions.clear();
         IrcManager var9 = IrcManager.getInstance();
         Color var10 = Theme.getInstance().getAccent();
         Color var11 = new Color(var10.getRed(), var10.getGreen(), var10.getBlue(), (int)(255.0F * var8));
         int var12 = var10.getRGB();
         HudConfig.a("irc_modal", var2, var3, 450.0F, 250.0F, 7.0F, new Color(0, 0, 0, (int)(245.0F * var8)));
         RenderUtil.a(var2, var3, 450.0F, 250.0F, 7.0F, 0.5F, a(-1, 35.0F * var8));
         float var13 = var2 + 8.0F;
         float var14 = var3 + 6.5F;
         RenderUtil.a(var1, MsdfManager.BOLD, var13, var14, "IRC", 7.5F, new Color(245, 245, 250, (int)(255.0F * var8)));
         float var15 = var13 + Msdf.a(MsdfManager.BOLD, "IRC", 7.5F) + 8.0F;
         float var16 = var3 + 9.5F;

         int var17 = switch (var9.getConnectionStatus()) {
            case CONNECTED -> a(-10822556, 255.0F * var8);
            case CONNECTING -> a(-997316, 255.0F * var8);
            case DISCONNECTED -> a(-1028016, 255.0F * var8);
         };
         RenderUtil.a(var15, var16 - 1.0F, 4.5F, 4.5F, 2.25F, var17);
         String var18 = "Онлайн: " + var9.getOnlineCount();
         RenderUtil.a(var1, MsdfManager.BOLD, var15 + 8.0F, var14 + 1.0F, var18, 5.5F, new Color(160, 165, 175, (int)(255.0F * var8)));
         float var19 = var2 + 450.0F - 11.0F;
         float var20 = var3 + 11.0F;
         this.closeBtnBox[0] = var19 - 6.0F;
         this.closeBtnBox[1] = var20 - 6.0F;
         this.closeBtnBox[2] = 12.0F;
         this.closeBtnBox[3] = 12.0F;
         boolean var21 = var4 >= this.closeBtnBox[0]
            && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var5 >= this.closeBtnBox[1]
            && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3];
         BufferBuilder var22 = ColorPipeline.a();
         if (var22 != null) {
            Color var23 = var21 ? Theme.getInstance().getAccent() : new Color(170, 175, 185);
            int var24 = (int)(255.0F * var8) << 24 | var23.getRGB() & 16777215;
            ColorPipeline.a(var22, var19 - 3.0F, var20 - 3.0F, var19 + 3.0F, var20 + 3.0F, 1.2F, var24);
            ColorPipeline.a(var22, var19 + 3.0F, var20 - 3.0F, var19 - 3.0F, var20 + 3.0F, 1.2F, var24);
            ColorPipeline.a(var22, false);
         }

         RenderUtil.a(var2 + 6.0F, var3 + 22.0F, 438.0F, 0.5F, 0.25F, a(-1, 20.0F * var8));
         this.a(var1, var2 + 6.0F, var3 + 22.0F + 4.0F, 135.0F, 218.0F, var4, var5, var8, var9, var11, var12, var6);
         float var27 = var2 + 6.0F + 135.0F + 3.0F;
         RenderUtil.a(var27, var3 + 22.0F + 4.0F, 0.5F, 218.0F, 0.25F, a(-1, 20.0F * var8));
         float var28 = var27 + 4.0F;
         float var25 = var2 + 450.0F - 6.0F - var28;
         float var26 = 218.0F;
         this.b(var1, var28, var3 + 22.0F + 4.0F, var25, var26, var4, var5, var8, var9, var11, var12, var6);
      }
   }

   private void a(
      DrawContext var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8, IrcManager var9, Color var10, int var11, float var12
   ) {
      float var13 = 16.0F;
      float var14 = (var4 - 2.0F) / 2.0F;
      float var15 = var2;
      float var16 = var2 + var14 + 2.0F;
      boolean var17 = this.currentTab == IrcPanel.a.CHATS;
      boolean var18 = this.currentTab == IrcPanel.a.FRIENDS;
      boolean var19 = var6 >= var15 && var6 <= var15 + var14 && var7 >= var3 && var7 <= var3 + var13;
      boolean var20 = var6 >= var16 && var6 <= var16 + var14 && var7 >= var3 && var7 <= var3 + var13;
      RenderUtil.a(var15, var3, var14, var13, 3.5F, var17 ? a(var11, 60.0F * var8) : (var19 ? a(-1, 15.0F * var8) : a(-1, 8.0F * var8)));
      RenderUtil.a(var15, var3, var14, var13, 3.5F, 0.5F, var17 ? a(var11, 200.0F * var8) : a(-1, 15.0F * var8));
      float var21 = Msdf.a(MsdfManager.BOLD, "Чаты", 5.5F);
      float var22 = Msdf.a(MsdfManager.BOLD, 5.5F);
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var15 + (var14 - var21) / 2.0F,
         var3 + (var13 - var22) / 2.0F,
         "Чаты",
         5.5F,
         var17 ? var10 : new Color(220, 225, 235, (int)(255.0F * var8))
      );
      this.clickBoxes.add(new float[]{var15, var3, var14, var13});
      this.clickActions.add(() -> {
         this.currentTab = IrcPanel.a.CHATS;
         this.sidebarScrollTarget = 0.0F;
      });
      RenderUtil.a(var16, var3, var14, var13, 3.5F, var18 ? a(var11, 60.0F * var8) : (var20 ? a(-1, 15.0F * var8) : a(-1, 8.0F * var8)));
      RenderUtil.a(var16, var3, var14, var13, 3.5F, 0.5F, var18 ? a(var11, 200.0F * var8) : a(-1, 15.0F * var8));
      float var23 = Msdf.a(MsdfManager.BOLD, "Друзья", 5.5F);
      float var24 = Msdf.a(MsdfManager.BOLD, 5.5F);
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var16 + (var14 - var23) / 2.0F,
         var3 + (var13 - var24) / 2.0F,
         "Друзья",
         5.5F,
         var18 ? var10 : new Color(220, 225, 235, (int)(255.0F * var8))
      );
      int var25 = var9.getIncomingRequests().size();
      if (var25 > 0) {
         String var26 = String.valueOf(var25);
         float var27 = Msdf.a(MsdfManager.BOLD, var26, 4.0F) + 5.0F;
         float var28 = 9.0F;
         float var29 = var16 + var14 - var27 - 2.0F;
         float var30 = var3 + (var13 - var28) / 2.0F;
         RenderUtil.a(var29, var30, var27, var28, 4.5F, a(-2080704, 255.0F * var8));
         float var31 = Msdf.a(MsdfManager.BOLD, var26, 4.0F);
         float var32 = Msdf.a(MsdfManager.BOLD, 4.0F);
         RenderUtil.a(
            var1, MsdfManager.BOLD, var29 + (var27 - var31) / 2.0F, var30 + (var28 - var32) / 2.0F, var26, 4.0F, new Color(255, 255, 255, (int)(255.0F * var8))
         );
      }

      this.clickBoxes.add(new float[]{var16, var3, var14, var13});
      this.clickActions.add(() -> {
         this.currentTab = IrcPanel.a.FRIENDS;
         this.sidebarScrollTarget = 0.0F;
      });
      float var35 = var3 + var13 + 4.0F;
      float var36 = 22.0F;
      float var37 = var5 - var13 - var36 - 8.0F;
      this.sidebarScrollAnim.animate(this.sidebarScrollTarget, var12, 8.0F);
      float var38 = this.sidebarScrollAnim.getValue();
      Scissor.b(var2, var35, var4, var37);
      if (this.currentTab == IrcPanel.a.CHATS) {
         this.a(var1, var2, var35, var4, var37, var38, var6, var7, var8, var9, var10, var11);
      } else {
         this.b(var1, var2, var35, var4, var37, var38, var6, var7, var8, var9, var10, var11);
      }

      Scissor.c();
      float var39 = var3 + var5 - var36;
      RenderUtil.a(var2, var39, var4, var36, 3.5F, a(-1, 8.0F * var8));
      String var40 = User.getInstance().getUsername();
      if (var40 == null || var40.isEmpty()) {
         var40 = "User";
      }

      this.a(var1, var2 + 3.5F, var39 + 3.5F, 15.0F, 3.0F, var40, var8, var10);
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 22.0F, var39 + 3.5F, var40, 5.5F, new Color(245, 245, 250, (int)(255.0F * var8)));
      String var41 = User.getInstance().getRole();
      if (var41 == null || var41.isEmpty()) {
         var41 = "User";
      }

      String var33 = User.getInstance().getUid();
      String var34 = var41 + (var33 != null && !var33.isEmpty() && !"0".equals(var33) ? " #" + var33 : "");
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 22.0F, var39 + 11.5F, var34, 4.0F, new Color(140, 145, 155, (int)(220.0F * var8)));
   }

   private void a(
      DrawContext var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8, float var9, IrcManager var10, Color var11, int var12
   ) {
      float var13 = var3 + var6;
      float var14 = 21.0F;
      boolean var15 = "global".equalsIgnoreCase(var10.getActiveChat());
      boolean var16 = var7 >= var2 && var7 <= var2 + var4 && var8 >= var13 && var8 <= var13 + var14 && var8 >= var3 && var8 <= var3 + var5;
      if (var15) {
         RenderUtil.a(var2, var13, var4, var14, 3.5F, a(var12, 50.0F * var9));
         RenderUtil.a(var2, var13 + 2.0F, 2.0F, var14 - 4.0F, 1.0F, var11.getRGB());
      } else if (var16) {
         RenderUtil.a(var2, var13, var4, var14, 3.5F, a(-1, 12.0F * var9));
      }

      RenderUtil.a(var2 + 4.0F, var13 + 3.0F, 15.0F, 15.0F, 3.0F, a(-14013131, 200.0F * var9));
      float var17 = Msdf.a(MsdfManager.BOLD, "#", 6.0F);
      float var18 = Msdf.a(MsdfManager.BOLD, 6.0F);
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 4.0F + (15.0F - var17) / 2.0F, var13 + 3.0F + (15.0F - var18) / 2.0F, "#", 6.0F, var11);
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 23.0F, var13 + 6.5F, "Главный IRC", 5.5F, var15 ? var11 : new Color(240, 240, 245, (int)(255.0F * var9)));
      int var19 = var10.getUnreadGlobal();
      if (var19 > 0) {
         String var20 = var19 > 99 ? "99+" : String.valueOf(var19);
         float var21 = Msdf.a(MsdfManager.BOLD, var20, 4.0F) + 5.0F;
         float var22 = 9.0F;
         float var23 = var2 + var4 - var21 - 4.0F;
         float var24 = var13 + (var14 - var22) / 2.0F;
         RenderUtil.a(var23, var24, var21, var22, 4.5F, a(-2080704, 255.0F * var9));
         float var25 = Msdf.a(MsdfManager.BOLD, var20, 4.0F);
         float var26 = Msdf.a(MsdfManager.BOLD, 4.0F);
         RenderUtil.a(
            var1, MsdfManager.BOLD, var23 + (var21 - var25) / 2.0F, var24 + (var22 - var26) / 2.0F, var20, 4.0F, new Color(255, 255, 255, (int)(255.0F * var9))
         );
      }

      float var36 = var13;
      this.clickBoxes.add(new float[]{var2, var36, var4, var14});
      this.clickActions.add(() -> {
         var10.b("global");
         this.chatScrollTarget = 0.0F;
      });
      float var35 = var13 + (var14 + 2.5F);

      for (IrcFriend var40 : var10.getFriends()) {
         boolean var41 = var40.getUsername().equalsIgnoreCase(var10.getActiveChat());
         boolean var42 = var7 >= var2 && var7 <= var2 + var4 && var8 >= var35 && var8 <= var35 + var14 && var8 >= var3 && var8 <= var3 + var5;
         if (var41) {
            RenderUtil.a(var2, var35, var4, var14, 3.5F, a(var12, 50.0F * var9));
            RenderUtil.a(var2, var35 + 2.0F, 2.0F, var14 - 4.0F, 1.0F, var11.getRGB());
         } else if (var42) {
            RenderUtil.a(var2, var35, var4, var14, 3.5F, a(-1, 12.0F * var9));
         }

         this.a(var1, var2 + 4.0F, var35 + 3.0F, 15.0F, 3.0F, var40.getUsername(), var9, var11);
         int var43 = var40.isOnline() ? -10822556 : -9407104;
         RenderUtil.a(var2 + 15.0F, var35 + 14.0F, 4.0F, 4.0F, 2.0F, a(var43, 255.0F * var9));
         RenderUtil.a(
            var1, MsdfManager.BOLD, var2 + 23.0F, var35 + 3.5F, var40.getUsername(), 5.5F, var41 ? var11 : new Color(240, 240, 245, (int)(255.0F * var9))
         );
         String var27 = var40.getLastMessage();
         if (var27 != null && !var27.isEmpty()) {
            if (var27.length() > 14) {
               var27 = var27.substring(0, 14) + "..";
            }

            RenderUtil.a(var1, MsdfManager.MONTSERRAT, var2 + 23.0F, var35 + 11.5F, var27, 4.0F, new Color(130, 135, 145, (int)(220.0F * var9)));
         }

         if (var40.getUnreadCount() > 0) {
            String var28 = var40.getUnreadCount() > 99 ? "99+" : String.valueOf(var40.getUnreadCount());
            float var29 = Msdf.a(MsdfManager.BOLD, var28, 4.0F) + 5.0F;
            float var30 = 9.0F;
            float var31 = var2 + var4 - var29 - 4.0F;
            float var32 = var35 + (var14 - var30) / 2.0F;
            RenderUtil.a(var31, var32, var29, var30, 4.5F, a(-2080704, 255.0F * var9));
            float var33 = Msdf.a(MsdfManager.BOLD, var28, 4.0F);
            float var34 = Msdf.a(MsdfManager.BOLD, 4.0F);
            RenderUtil.a(
               var1,
               MsdfManager.BOLD,
               var31 + (var29 - var33) / 2.0F,
               var32 + (var30 - var34) / 2.0F,
               var28,
               4.0F,
               new Color(255, 255, 255, (int)(255.0F * var9))
            );
         }

         float var44 = var35;
         String var45 = var40.getUsername();
         this.clickBoxes.add(new float[]{var2, var44, var4, var14});
         this.clickActions.add(() -> {
            var10.b(var45);
            this.chatScrollTarget = 0.0F;
         });
         var35 += var14 + 2.5F;
      }

      float var39 = var35 - (var3 + var6);
      this.sidebarMaxScroll = Math.max(0.0F, var39 - var5);
   }

   private void b(
      DrawContext var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8, float var9, IrcManager var10, Color var11, int var12
   ) {
      float var13 = var3 + var6;
      float var14 = 16.0F;
      float var15 = 16.0F;
      float var16 = var4 - var15 - 4.0F;
      RenderUtil.a(var2, var13, var16, var14, 3.5F, a(-1, 10.0F * var9));
      RenderUtil.a(var2, var13, var16, var14, 3.5F, 0.5F, this.addFriendFocused ? a(var12, 255.0F * var9) : a(-1, 20.0F * var9));
      String var17 = this.addFriendBuffer.toString();
      String var18 = var17.isEmpty() && !this.addFriendFocused ? "Никнейм..." : var17;
      Color var19 = var17.isEmpty() && !this.addFriendFocused ? new Color(130, 135, 145, (int)(200.0F * var9)) : new Color(240, 240, 245, (int)(255.0F * var9));
      if (this.addFriendFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
         var18 = var18 + "_";
      }

      float var20 = Msdf.a(MsdfManager.BOLD, 5.0F);
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 5.0F, var13 + (var14 - var20) / 2.0F, var18, 5.0F, var19);
      float var21 = var13;
      this.clickBoxes.add(new float[]{var2, var21, var16, var14});
      this.clickActions.add(() -> {
         this.addFriendFocused = true;
         this.inputFocused = false;
      });
      float var22 = var2 + var4 - var15;
      boolean var23 = var7 >= var22 && var7 <= var22 + var15 && var8 >= var13 && var8 <= var13 + var14 && var8 >= var3 && var8 <= var3 + var5;
      RenderUtil.a(var22, var13, var15, var14, 3.5F, var23 ? a(var12, 220.0F * var9) : a(var12, 110.0F * var9));
      BufferBuilder var24 = ColorPipeline.a();
      if (var24 != null) {
         int var25 = a(var23 ? -16777216 : -1, 255.0F * var9);
         float var26 = var22 + var15 / 2.0F;
         float var27 = var13 + var14 / 2.0F;
         ColorPipeline.a(var24, var26 - 3.5F, var27, var26 + 3.5F, var27, 1.3F, var25);
         ColorPipeline.a(var24, var26, var27 - 3.5F, var26, var27 + 3.5F, 1.3F, var25);
         ColorPipeline.a(var24, false);
      }

      this.clickBoxes.add(new float[]{var22, var21, var15, var14});
      this.clickActions.add(() -> {
         String var2x = this.addFriendBuffer.toString().trim();
         if (!var2x.isEmpty()) {
            var10.f(var2x);
            this.addFriendBuffer.setLength(0);
            this.addFriendFocused = false;
         } else {
            this.addFriendFocused = true;
            this.inputFocused = false;
         }
      });
      float var49 = var13 + (var14 + 6.0F);
      List var52 = var10.getIncomingRequests();
      List var53 = var10.getOutgoingRequests();
      if (!var52.isEmpty() || !var53.isEmpty()) {
         RenderUtil.a(var1, MsdfManager.BOLD, var2 + 2.0F, var49, "Заявки (" + var52.size() + ")", 5.0F, var11);
         float var50 = var49 + 9.0F;

         for (IrcFriendRequest var28 : (Iterable<IrcFriendRequest>)(Iterable<?>)(var52)) {
            float var29 = 22.0F;
            RenderUtil.a(var2, var50, var4, var29, 3.5F, a(-1, 10.0F * var9));
            RenderUtil.a(var2, var50, var4, var29, 3.5F, 0.5F, a(var12, 60.0F * var9));
            this.a(var1, var2 + 4.0F, var50 + 3.5F, 15.0F, 3.0F, var28.getUsername(), var9, var11);
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 23.0F, var50 + 3.5F, var28.getUsername(), 5.0F, new Color(245, 245, 250, (int)(255.0F * var9)));
            String var30 = "[" + var28.getRole() + "]";
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 23.0F, var50 + 11.5F, var30, 4.0F, new Color(140, 145, 155, (int)(220.0F * var9)));
            float var31 = 13.0F;
            float var32 = 13.0F;
            float var33 = var2 + var4 - var31 * 2.0F - 6.0F;
            float var34 = var50 + (var29 - var32) / 2.0F;
            boolean var35 = var7 >= var33 && var7 <= var33 + var31 && var8 >= var34 && var8 <= var34 + var32 && var8 >= var3 && var8 <= var3 + var5;
            RenderUtil.a(var33, var34, var31, var32, 3.0F, var35 ? a(-10822556, 255.0F * var9) : a(-10822556, 160.0F * var9));
            BufferBuilder var36 = ColorPipeline.a();
            if (var36 != null) {
               int var37 = var35 ? -16777216 : -1;
               int var38 = a(var37, 255.0F * var9);
               float var39 = var33 + var31 / 2.0F;
               float var40 = var34 + var32 / 2.0F;
               ColorPipeline.a(var36, var39 - 2.8F, var40 + 0.3F, var39 - 0.8F, var40 + 2.5F, 1.3F, var38);
               ColorPipeline.a(var36, var39 - 0.8F, var40 + 2.5F, var39 + 3.0F, var40 - 2.5F, 1.3F, var38);
               ColorPipeline.a(var36, false);
            }

            String var77 = var28.getUsername();
            this.clickBoxes.add(new float[]{var33, var34, var31, var32});
            this.clickActions.add(() -> var10.h(var77));
            float var80 = var2 + var4 - var31 - 3.0F;
            float var83 = var50 + (var29 - var32) / 2.0F;
            boolean var85 = var7 >= var80 && var7 <= var80 + var31 && var8 >= var83 && var8 <= var83 + var32 && var8 >= var3 && var8 <= var3 + var5;
            RenderUtil.a(var80, var83, var31, var32, 3.0F, var85 ? a(-2080704, 255.0F * var9) : a(-2080704, 160.0F * var9));
            BufferBuilder var41 = ColorPipeline.a();
            if (var41 != null) {
               int var42 = a(-1, 255.0F * var9);
               float var43 = var80 + var31 / 2.0F;
               float var44 = var83 + var32 / 2.0F;
               ColorPipeline.a(var41, var43 - 2.5F, var44 - 2.5F, var43 + 2.5F, var44 + 2.5F, 1.3F, var42);
               ColorPipeline.a(var41, var43 + 2.5F, var44 - 2.5F, var43 - 2.5F, var44 + 2.5F, 1.3F, var42);
               ColorPipeline.a(var41, false);
            }

            this.clickBoxes.add(new float[]{var80, var83, var31, var32});
            this.clickActions.add(() -> var10.i(var77));
            var50 += var29 + 3.0F;
         }

         for (IrcFriendRequest var57 : (Iterable<IrcFriendRequest>)(Iterable<?>)(var53)) {
            float var60 = 18.0F;
            RenderUtil.a(var2, var50, var4, var60, 3.5F, a(-1, 6.0F * var9));
            this.a(var1, var2 + 4.0F, var50 + 2.5F, 13.0F, 2.5F, var57.getUsername(), var9, var11);
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 21.0F, var50 + 2.5F, var57.getUsername(), 4.5F, new Color(200, 205, 215, (int)(255.0F * var9)));
            RenderUtil.a(var1, MsdfManager.MONTSERRAT, var2 + 21.0F, var50 + 9.5F, "Исходящая...", 4.0F, new Color(130, 135, 145, (int)(200.0F * var9)));
            float var62 = 12.0F;
            float var64 = 12.0F;
            float var66 = var2 + var4 - var62 - 3.0F;
            float var68 = var50 + (var60 - var64) / 2.0F;
            boolean var70 = var7 >= var66 && var7 <= var66 + var62 && var8 >= var68 && var8 <= var68 + var64 && var8 >= var3 && var8 <= var3 + var5;
            RenderUtil.a(var66, var68, var62, var64, 2.5F, var70 ? a(-2080704, 200.0F * var9) : a(-1, 15.0F * var9));
            BufferBuilder var72 = ColorPipeline.a();
            if (var72 != null) {
               int var74 = a(var70 ? -1 : -3090976, 255.0F * var9);
               float var78 = var66 + var62 / 2.0F;
               float var81 = var68 + var64 / 2.0F;
               ColorPipeline.a(var72, var78 - 2.2F, var81 - 2.2F, var78 + 2.2F, var81 + 2.2F, 1.2F, var74);
               ColorPipeline.a(var72, var78 + 2.2F, var81 - 2.2F, var78 - 2.2F, var81 + 2.2F, 1.2F, var74);
               ColorPipeline.a(var72, false);
            }

            String var75 = var57.getUsername();
            this.clickBoxes.add(new float[]{var66, var68, var62, var64});
            this.clickActions.add(() -> var10.i(var75));
            var50 += var60 + 2.5F;
         }

         var49 = var50 + 4.0F;
      }

      List var56 = var10.getFriends();
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 2.0F, var49, "Все друзья (" + var56.size() + ")", 5.0F, new Color(180, 185, 195, (int)(255.0F * var9)));
      float var51 = var49 + 9.0F;
      if (var56.isEmpty()) {
         RenderUtil.a(var1, MsdfManager.MONTSERRAT, var2 + 4.0F, var51 + 4.0F, "Список друзей пуст", 4.5F, new Color(120, 125, 135, (int)(200.0F * var9)));
         var51 += 16.0F;
      } else {
         for (IrcFriend var61 : (Iterable<IrcFriend>)(Iterable<?>)(var56)) {
            float var63 = 22.0F;
            RenderUtil.a(var2, var51, var4, var63, 3.5F, a(-1, 8.0F * var9));
            this.a(var1, var2 + 4.0F, var51 + 3.5F, 15.0F, 3.0F, var61.getUsername(), var9, var11);
            int var65 = var61.isOnline() ? -10822556 : -9407104;
            RenderUtil.a(var2 + 15.0F, var51 + 14.5F, 4.0F, 4.0F, 2.0F, a(var65, 255.0F * var9));
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 23.0F, var51 + 3.5F, var61.getUsername(), 5.5F, new Color(240, 240, 245, (int)(255.0F * var9)));
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 23.0F, var51 + 11.5F, "[" + var61.getRole() + "]", 4.0F, new Color(130, 135, 145, (int)(220.0F * var9)));
            float var67 = 24.0F;
            float var69 = 13.0F;
            float var71 = var2 + var4 - var67 - 17.0F;
            float var73 = var51 + (var63 - var69) / 2.0F;
            boolean var76 = var7 >= var71 && var7 <= var71 + var67 && var8 >= var73 && var8 <= var73 + var69 && var8 >= var3 && var8 <= var3 + var5;
            RenderUtil.a(var71, var73, var67, var69, 3.0F, var76 ? a(var12, 190.0F * var9) : a(var12, 85.0F * var9));
            float var79 = Msdf.a(MsdfManager.BOLD, "Чат", 4.5F);
            float var82 = Msdf.a(MsdfManager.BOLD, 4.5F);
            RenderUtil.a(
               var1,
               MsdfManager.BOLD,
               var71 + (var67 - var79) / 2.0F,
               var73 + (var69 - var82) / 2.0F,
               "Чат",
               4.5F,
               var76 ? new Color(0, 0, 0, (int)(255.0F * var9)) : new Color(245, 245, 250, (int)(255.0F * var9))
            );
            String var84 = var61.getUsername();
            this.clickBoxes.add(new float[]{var71, var73, var67, var69});
            this.clickActions.add(() -> {
               var10.b(var84);
               this.currentTab = IrcPanel.a.CHATS;
               this.chatScrollTarget = 0.0F;
            });
            float var86 = 13.0F;
            float var87 = 13.0F;
            float var88 = var2 + var4 - var86 - 3.0F;
            float var89 = var51 + (var63 - var87) / 2.0F;
            boolean var90 = var7 >= var88 && var7 <= var88 + var86 && var8 >= var89 && var8 <= var89 + var87 && var8 >= var3 && var8 <= var3 + var5;
            RenderUtil.a(var88, var89, var86, var87, 3.0F, var90 ? a(-2080704, 200.0F * var9) : a(-1, 12.0F * var9));
            BufferBuilder var45 = ColorPipeline.a();
            if (var45 != null) {
               int var46 = a(var90 ? -1 : -5196352, 255.0F * var9);
               float var47 = var88 + var86 / 2.0F;
               float var48 = var89 + var87 / 2.0F;
               ColorPipeline.a(var45, var47 - 2.5F, var48 - 2.5F, var47 + 2.5F, var48 + 2.5F, 1.2F, var46);
               ColorPipeline.a(var45, var47 + 2.5F, var48 - 2.5F, var47 - 2.5F, var48 + 2.5F, 1.2F, var46);
               ColorPipeline.a(var45, false);
            }

            this.clickBoxes.add(new float[]{var88, var89, var86, var87});
            this.clickActions.add(() -> var10.j(var84));
            var51 += var63 + 2.5F;
         }
      }

      float var59 = var51 - (var3 + var6);
      this.sidebarMaxScroll = Math.max(0.0F, var59 - var5);
   }

   private void b(
      DrawContext var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8, IrcManager var9, Color var10, int var11, float var12
   ) {
      String var13 = var9.getActiveChat();
      boolean var14 = "global".equalsIgnoreCase(var13);
      float var15 = 16.0F;
      RenderUtil.a(var2, var3, var4, var15, 3.5F, a(-1, 8.0F * var8));
      String var16 = var14 ? "Глобальный IRC чат" : "Чат с: " + var13;
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 6.0F, var3 + 4.0F, var16, 6.0F, new Color(240, 240, 245, (int)(255.0F * var8)));
      if (!var14) {
         float var17 = 48.0F;
         float var18 = 11.0F;
         float var19 = var2 + var4 - var17 - 4.0F;
         float var20 = var3 + 2.5F;
         boolean var21 = var6 >= var19 && var6 <= var19 + var17 && var7 >= var20 && var7 <= var20 + var18;
         RenderUtil.a(var19, var20, var17, var18, 3.0F, var21 ? a(-2080704, 180.0F * var8) : a(-1, 15.0F * var8));
         float var22 = Msdf.a(MsdfManager.BOLD, "Удалить", 4.5F);
         float var23 = Msdf.a(MsdfManager.BOLD, 4.5F);
         RenderUtil.a(
            var1,
            MsdfManager.BOLD,
            var19 + (var17 - var22) / 2.0F,
            var20 + (var18 - var23) / 2.0F,
            "Удалить",
            4.5F,
            var21 ? new Color(255, 255, 255, (int)(255.0F * var8)) : new Color(180, 185, 195, (int)(255.0F * var8))
         );
         this.clickBoxes.add(new float[]{var19, var20, var17, var18});
         this.clickActions.add(() -> {
            var9.j(var13);
            var9.b("global");
         });
      }

      float var46 = var3 + var15 + 3.0F;
      float var47 = var5 - var15 - 20.0F - 8.0F;
      List var48 = var14 ? var9.getGlobalMessages() : var9.c(var13);
      this.chatScrollAnim.animate(this.chatScrollTarget, var12, 8.0F);
      float var49 = this.chatScrollAnim.getValue();
      Scissor.b(var2, var46, var4, var47);
      float var50 = var46 + var49;
      String var51 = User.getInstance().getUsername();

      for (IrcMessage var24 : (Iterable<IrcMessage>)(Iterable<?>)(var48)) {
         boolean var25 = var24.isSelf() || var24.getSenderName() != null && var24.getSenderName().equalsIgnoreCase(var51);
         float var26 = var4 - 12.0F;
         float var27 = var24.h() ? 30.0F : 23.0F;
         if (var50 + var27 >= var46 && var50 <= var46 + var47) {
            float var28 = var2 + 6.0F;
            int var29 = var25 ? a(var11, 30.0F * var8) : a(-1, 10.0F * var8);
            RenderUtil.a(var28, var50, var26, var27, 4.0F, var29);
            RenderUtil.a(var28, var50, var26, var27, 4.0F, 0.5F, var25 ? a(var11, 80.0F * var8) : a(-1, 18.0F * var8));
            String var30 = var24.getSenderName() != null ? var24.getSenderName() : "Unknown";
            this.a(var1, var28 + 4.5F, var50 + 4.0F, 15.0F, 3.0F, var30, var8, var10);
            String var31 = "[" + (var24.getSenderRole() != null ? var24.getSenderRole() : "User") + "]";
            Color var32 = var25 ? var10 : new Color(215, 220, 230, (int)(255.0F * var8));
            RenderUtil.a(var1, MsdfManager.BOLD, var28 + 24.0F, var50 + 3.5F, var30, 5.5F, var32);
            float var33 = Msdf.a(MsdfManager.BOLD, var30, 5.5F);
            RenderUtil.a(var1, MsdfManager.BOLD, var28 + 26.0F + var33, var50 + 4.0F, var31, 4.0F, new Color(130, 135, 145, (int)(200.0F * var8)));
            String var34 = var24.k();
            float var35 = Msdf.a(MsdfManager.MONTSERRAT, var34, 4.0F);
            RenderUtil.a(var1, MsdfManager.MONTSERRAT, var28 + var26 - var35 - 5.0F, var50 + 4.0F, var34, 4.0F, new Color(110, 115, 125, (int)(200.0F * var8)));
            if (var24.h()) {
               IrcMessage.a var36 = var24.getCords();
               String var37 = String.format("XYZ: %d, %d, %d  [%s | %s]", var36.getX(), var36.getY(), var36.getZ(), var36.getDimension(), var36.getServer());
               RenderUtil.a(var1, MsdfManager.BOLD, var28 + 24.0F, var50 + 14.5F, var37, 5.0F, new Color(90, 220, 140, (int)(255.0F * var8)));
               float var38 = 44.0F;
               float var39 = 9.5F;
               float var40 = var28 + var26 - var38 - 4.0F;
               float var41 = var50 + 14.0F;
               boolean var42 = var6 >= var40 && var6 <= var40 + var38 && var7 >= var41 && var7 <= var41 + var39;
               RenderUtil.a(var40, var41, var38, var39, 2.5F, var42 ? a(var11, 200.0F * var8) : a(-1, 20.0F * var8));
               float var43 = Msdf.a(MsdfManager.BOLD, "Скопировать", 4.0F);
               float var44 = Msdf.a(MsdfManager.BOLD, 4.0F);
               RenderUtil.a(
                  var1,
                  MsdfManager.BOLD,
                  var40 + (var38 - var43) / 2.0F,
                  var41 + (var39 - var44) / 2.0F,
                  "Скопировать",
                  4.0F,
                  var42 ? new Color(0, 0, 0, (int)(255.0F * var8)) : new Color(220, 225, 230, (int)(255.0F * var8))
               );
               String var45 = String.format("%d %d %d", var36.getX(), var36.getY(), var36.getZ());
               this.clickBoxes.add(new float[]{var40, var41, var38, var39});
               this.clickActions.add(() -> {
                  MinecraftClient.getInstance().keyboard.setClipboard(var45);
                  this.copiedCoordsToast = "Координаты скопированы!";
                  this.copiedCoordsTime = System.currentTimeMillis();
               });
            } else {
               String var74 = var24.getText() != null ? var24.getText() : "";
               RenderUtil.a(var1, MsdfManager.MONTSERRAT, var28 + 24.0F, var50 + 12.0F, var74, 5.5F, new Color(235, 235, 240, (int)(255.0F * var8)));
            }
         }

         var50 += var27 + 2.5F;
      }

      float var53 = var50 - (var46 + var49);
      this.chatMaxScroll = Math.max(0.0F, var53 - var47);
      Scissor.c();
      if (this.copiedCoordsToast != null && System.currentTimeMillis() - this.copiedCoordsTime < 2000L) {
         float var54 = Msdf.a(MsdfManager.BOLD, this.copiedCoordsToast, 5.5F) + 14.0F;
         float var56 = var2 + (var4 - var54) / 2.0F;
         float var58 = var46 + 4.0F;
         RenderUtil.a(var56, var58, var54, 14.0F, 3.5F, a(-14802906, 240.0F * var8));
         RenderUtil.a(var56, var58, var54, 14.0F, 3.5F, 0.5F, a(var11, 200.0F * var8));
         float var60 = Msdf.a(MsdfManager.BOLD, this.copiedCoordsToast, 5.5F);
         float var62 = Msdf.a(MsdfManager.BOLD, 5.5F);
         RenderUtil.a(var1, MsdfManager.BOLD, var56 + (var54 - var60) / 2.0F, var58 + (14.0F - var62) / 2.0F, this.copiedCoordsToast, 5.5F, var10);
      }

      float var55 = var3 + var5 - 20.0F;
      float var57 = 18.0F;
      float var59 = 20.0F;
      boolean var61 = var6 >= var2 && var6 <= var2 + var57 && var7 >= var55 && var7 <= var55 + var59;
      RenderUtil.a(var2, var55, var57, var59, 3.5F, var61 ? a(var11, 140.0F * var8) : a(-1, 15.0F * var8));
      RenderUtil.a(var2, var55, var57, var59, 3.5F, 0.5F, var61 ? a(var11, 255.0F * var8) : a(-1, 25.0F * var8));
      BufferBuilder var63 = ColorPipeline.a();
      if (var63 != null) {
         int var64 = var61 ? var10.getRGB() : a(-2301461, 255.0F * var8);
         float var66 = var2 + var57 / 2.0F;
         float var68 = var55 + var59 / 2.0F;
         ColorPipeline.a(var63, var66 - 4.0F, var68, var66 + 4.0F, var68, 1.4F, var64);
         ColorPipeline.a(var63, var66, var68 - 4.0F, var66, var68 + 4.0F, 1.4F, var64);
         ColorPipeline.a(var63, false);
      }

      this.clickBoxes.add(new float[]{var2, var55, var57, var59});
      this.clickActions.add(() -> {
         var9.m();
         this.chatScrollTarget = 0.0F;
      });
      float var65 = var2 + var57 + 4.0F;
      float var67 = 24.0F;
      float var69 = var4 - var57 - var67 - 8.0F;
      float var70 = 20.0F;
      RenderUtil.a(var65, var55, var69, var70, 3.5F, a(-1, 10.0F * var8));
      RenderUtil.a(var65, var55, var69, var70, 3.5F, 0.5F, this.inputFocused ? a(var11, 255.0F * var8) : a(-1, 20.0F * var8));
      String var71 = this.inputBuffer.toString();
      String var72 = var71.isEmpty() && !this.inputFocused ? "Напишите сообщение..." : var71;
      Color var73 = var71.isEmpty() && !this.inputFocused ? new Color(130, 135, 145, (int)(200.0F * var8)) : new Color(240, 240, 245, (int)(255.0F * var8));
      if (this.inputFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
         var72 = var72 + "_";
      }

      float var75 = Msdf.a(MsdfManager.BOLD, 5.5F);
      RenderUtil.a(var1, MsdfManager.BOLD, var65 + 7.0F, var55 + (var70 - var75) / 2.0F, var72, 5.5F, var73);
      this.clickBoxes.add(new float[]{var65, var55, var69, var70});
      this.clickActions.add(() -> {
         this.inputFocused = true;
         this.addFriendFocused = false;
      });
      float var76 = var2 + var4 - var67;
      boolean var77 = var6 >= var76 && var6 <= var76 + var67 && var7 >= var55 && var7 <= var55 + var70;
      RenderUtil.a(var76, var55, var67, var70, 3.5F, var77 ? a(var11, 255.0F * var8) : a(var11, 190.0F * var8));
      BufferBuilder var78 = ColorPipeline.a();
      if (var78 != null) {
         int var79 = a(-16777216, 255.0F * var8);
         float var80 = var76 + var67 / 2.0F;
         float var81 = var55 + var70 / 2.0F;
         ColorPipeline.a(var78, var80 - 4.5F, var81 - 4.0F, var80 + 4.5F, var81, 1.3F, var79);
         ColorPipeline.a(var78, var80 - 4.5F, var81 + 4.0F, var80 + 4.5F, var81, 1.3F, var79);
         ColorPipeline.a(var78, var80 - 4.5F, var81 - 4.0F, var80 - 2.0F, var81, 1.3F, var79);
         ColorPipeline.a(var78, var80 - 4.5F, var81 + 4.0F, var80 - 2.0F, var81, 1.3F, var79);
         ColorPipeline.a(var78, var80 - 2.0F, var81, var80 + 4.5F, var81, 1.3F, var79);
         ColorPipeline.a(var78, false);
      }

      this.clickBoxes.add(new float[]{var76, var55, var67, var70});
      this.clickActions.add(() -> {
         String var2x = this.inputBuffer.toString().trim();
         if (!var2x.isEmpty()) {
            var9.e(var2x);
            this.inputBuffer.setLength(0);
            this.chatScrollTarget = 0.0F;
         }
      });
   }

   public boolean a(float var1, float var2, int var3, boolean var4, Runnable var5) {
      if (!(this.openAnim.getValue() < 0.2F) && var3 == 0) {
         if (var1 >= this.closeBtnBox[0]
            && var1 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var2 >= this.closeBtnBox[1]
            && var2 <= this.closeBtnBox[1] + this.closeBtnBox[3]) {
            if (var5 != null) {
               var5.run();
            }

            return true;
         } else {
            for (int var6 = 0; var6 < this.clickBoxes.size(); var6++) {
               float[] var7 = this.clickBoxes.get(var6);
               if (var1 >= var7[0] && var1 <= var7[0] + var7[2] && var2 >= var7[1] && var2 <= var7[1] + var7[3]) {
                  if (var6 < this.clickActions.size()) {
                     this.clickActions.get(var6).run();
                  }

                  return true;
               }
            }

            this.inputFocused = false;
            this.addFriendFocused = false;
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean a(double var1, double var3, double var5, double var7) {
      if (this.openAnim.getValue() < 0.5F) {
         return false;
      }

      if (var1 <= 155.0) {
         this.sidebarScrollTarget += (float)(var7 * 18.0);
         if (this.sidebarScrollTarget < -this.sidebarMaxScroll) {
            this.sidebarScrollTarget = -this.sidebarMaxScroll;
         }

         if (this.sidebarScrollTarget > 0.0F) {
            this.sidebarScrollTarget = 0.0F;
         }
      } else {
         this.chatScrollTarget += (float)(var7 * 18.0);
         if (this.chatScrollTarget < -this.chatMaxScroll) {
            this.chatScrollTarget = -this.chatMaxScroll;
         }

         if (this.chatScrollTarget > 0.0F) {
            this.chatScrollTarget = 0.0F;
         }
      }

      return true;
   }

   public boolean a(KeyInput var1) {
      if (this.openAnim.getValue() < 0.5F) {
         return false;
      }

      int var2 = var1.key();
      if (var2 == 256) {
         if (!this.inputFocused && !this.addFriendFocused) {
            return false;
         }

         this.inputFocused = false;
         this.addFriendFocused = false;
         return true;
      } else if (this.inputFocused) {
         if (var2 == 257 || var2 == 335) {
            String var6 = this.inputBuffer.toString().trim();
            if (!var6.isEmpty()) {
               IrcManager.getInstance().e(var6);
               this.inputBuffer.setLength(0);
               this.chatScrollTarget = 0.0F;
            }

            return true;
         } else if (var2 == 259) {
            if (this.inputSelectAll) {
               this.inputBuffer.setLength(0);
               this.inputSelectAll = false;
            } else if (!this.inputBuffer.isEmpty()) {
               this.inputBuffer.deleteCharAt(this.inputBuffer.length() - 1);
            }

            return true;
         } else {
            if (var2 == 65 && (var1.modifiers() & 2) != 0) {
               this.inputSelectAll = true;
               return true;
            }

            if (var2 == 86 && (var1.modifiers() & 2) != 0) {
               String var5 = MinecraftClient.getInstance().keyboard.getClipboard();
               if (var5 != null) {
                  if (this.inputSelectAll) {
                     this.inputBuffer.setLength(0);
                     this.inputSelectAll = false;
                  }

                  this.inputBuffer.append(var5);
               }

               return true;
            } else {
               return true;
            }
         }
      } else if (this.addFriendFocused) {
         if (var2 == 257 || var2 == 335) {
            String var4 = this.addFriendBuffer.toString().trim();
            if (!var4.isEmpty()) {
               IrcManager.getInstance().f(var4);
               this.addFriendBuffer.setLength(0);
               this.addFriendFocused = false;
            }

            return true;
         } else if (var2 == 259) {
            if (this.addFriendSelectAll) {
               this.addFriendBuffer.setLength(0);
               this.addFriendSelectAll = false;
            } else if (!this.addFriendBuffer.isEmpty()) {
               this.addFriendBuffer.deleteCharAt(this.addFriendBuffer.length() - 1);
            }

            return true;
         } else {
            if (var2 == 65 && (var1.modifiers() & 2) != 0) {
               this.addFriendSelectAll = true;
               return true;
            }

            if (var2 == 86 && (var1.modifiers() & 2) != 0) {
               String var3 = MinecraftClient.getInstance().keyboard.getClipboard();
               if (var3 != null) {
                  if (this.addFriendSelectAll) {
                     this.addFriendBuffer.setLength(0);
                     this.addFriendSelectAll = false;
                  }

                  this.addFriendBuffer.append(var3);
               }

               return true;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean a(CharInput var1) {
      if (!(this.openAnim.getValue() < 0.5F) && var1.isValidChar()) {
         String var2 = var1.asString();
         if (this.inputFocused) {
            if (this.inputSelectAll) {
               this.inputBuffer.setLength(0);
               this.inputSelectAll = false;
            }

            this.inputBuffer.append(var2);
            return true;
         } else if (this.addFriendFocused) {
            if (this.addFriendSelectAll) {
               this.addFriendBuffer.setLength(0);
               this.addFriendSelectAll = false;
            }

            this.addFriendBuffer.append(var2);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public enum a {
      CHATS,
      FRIENDS;
   }
}
