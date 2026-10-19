package brokke.cc.irc;

import brokke.cc.animation.Easing;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.util.SoundUtil;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public class IrcNotificationManager {
   private static final IrcNotificationManager INSTANCE = new IrcNotificationManager();
   private static final List<IrcNotification> notifications = new CopyOnWriteArrayList<>();
   private static final int MAX_NOTIFS = 4;
   private static final float CARD_H = 32.0F;
   private static final float CARD_GAP = 5.0F;
   private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

   public static void a() {
      EventBus.register(INSTANCE);
   }

   public static void a(IrcMessage var0) {
      if (var0 != null) {
         String var1 = var0.getSenderName() != null ? var0.getSenderName() : "Unknown";
         String var2 = var0.h() ? "Отправил координаты" : (var0.getText() != null ? var0.getText() : "");
         String var3 = var0.k() != null ? var0.k() : LocalTime.now().format(TIME_FMT);
         a("Messages", var1, var2, var3, 4000L);
      }
   }

   public static void a(String var0) {
      if (var0 != null) {
         String var1 = LocalTime.now().format(TIME_FMT);
         a("Messages", var0, "Отправил заявку в друзья", var1, 4000L);
      }
   }

   public static synchronized void a(String var0, String var1, String var2, String var3, long var4) {
      while (notifications.size() >= 4) {
         notifications.remove(0);
      }

      notifications.add(new IrcNotification(var0, var1, var2, var3, var4));

      try {
         SoundUtil.a("/assets/brokke/sounds/enable1.wav", 1.0F, 1.2F);
      } catch (Exception var7) {
      }
   }

   private static int a(int var0, float var1) {
      int var2 = Math.clamp(Math.round(var1), 0, 255);
      return var0 & 16777215 | var2 << 24;
   }

   private void a(float var1, float var2, float var3, float var4, String var5, float var6) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      GpuTextureView var8 = var5 != null && !var5.isEmpty() ? SkinCache.a(var7, var5) : null;
      if (var8 != null) {
         int var9 = (int)Math.clamp(255.0F * var6, 0.0F, 255.0F) << 24 | 16777215;
         TexturePipeline.b(RenderUtil.c(), var1, var2, var3, var3, var8, var9, var4, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
         TexturePipeline.b(RenderUtil.c(), var1, var2, var3, var3, var8, var9, var4, 0.0F, 0.625F, 0.125F, 0.75F, 0.25F);
         RenderUtil.a(var1, var2, var3, var3, var4, 0.5F, a(-1, 35.0F * var6));
      } else {
         String var17 = var5 != null && !var5.isEmpty() ? var5 : "?";
         int var10 = Math.abs(var17.hashCode());
         Color var11 = new Color(40 + var10 % 110, 45 + var10 / 100 % 110, 85 + var10 / 1000 % 110);
         RenderUtil.a(var1, var2, var3, var3, var4, a(var11.getRGB(), 220.0F * var6));
         RenderUtil.a(var1, var2, var3, var3, var4, 0.5F, a(-1, 25.0F * var6));
         String var12 = var17.substring(0, 1).toUpperCase();
         float var13 = var3 * 0.48F;
         float var14 = Msdf.a(MsdfManager.BOLD, var12, var13);
         float var15 = Msdf.a(MsdfManager.BOLD, var13);
         Matrix4f var16 = RenderUtil.d();
         Msdf.b();
         Msdf.a(var16, MsdfManager.BOLD, var12, var1 + (var3 - var14) / 2.0F, var2 + (var3 - var15) / 2.0F, var13, a(16119290, 255.0F * var6));
         Msdf.c();
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (!var2.options.hudHidden && !notifications.isEmpty()) {
         float var3 = RenderUtil.a();
         float var4 = RenderUtil.b();

         for (int var5 = notifications.size() - 1; var5 >= 0; var5--) {
            IrcNotification var6 = notifications.get(var5);
            boolean var7 = var6.j();
            var6.getAnim().animate(var7 ? 0.0F : 1.0F, 0.0F, 6.0F, Easing.SPRING_SOFT);
            float var8 = Math.clamp(var6.getAnim().getValue(), 0.0F, 1.0F);
            if (var7 && var8 <= 0.01F) {
               notifications.remove(var5);
            }
         }

         if (!notifications.isEmpty()) {
            Matrix4f var33 = RenderUtil.d();
            Color var34 = Theme.getInstance().getAccent();
            int var35 = var34 != null ? var34.getRGB() & 16777215 : 5164408;
            float var36 = var3 - 14.0F;
            float var9 = var4 - 18.0F;
            float var10 = 0.0F;

            for (int var11 = 0; var11 < notifications.size(); var11++) {
               IrcNotification var12 = notifications.get(var11);
               if (!var12.isYInitialized()) {
                  var12.getYAnim().reset(var10);
                  var12.setYInitialized(true);
               }

               var12.getYAnim().animate(var10, 0.0F, 6.0F, Easing.SPRING_SOFT);
               float var13 = var12.getYAnim().getValue();
               float var14 = Math.clamp(var12.getAnim().getValue(), 0.0F, 1.0F);
               if (var14 > 0.005F) {
                  String var15 = var12.getSender() != null ? var12.getSender() : "";
                  String var16 = var12.getMessage() != null ? var12.getMessage() : "";
                  String var17 = var12.getTitle() != null ? var12.getTitle() : "Messages";
                  String var18 = var12.getTime() != null ? var12.getTime() : "";
                  float var19 = Msdf.a(MsdfManager.BOLD, var15 + ": ", 5.5F);
                  float var20 = Msdf.a(MsdfManager.MONTSERRAT, var16, 5.5F);
                  float var21 = Msdf.a(MsdfManager.BOLD, var17, 5.0F);
                  float var22 = Msdf.a(MsdfManager.MONTSERRAT, var18, 4.0F);
                  float var23 = Math.max(var21 + var22 + 36.0F, var19 + var20 + 36.0F);
                  float var24 = Math.clamp(var23, 160.0F, 230.0F);
                  float var25 = var36 - var24 + (1.0F - var14) * (var24 + 15.0F);
                  float var26 = var9 - 32.0F - var13;
                  RenderUtil.a(var33, var25, var26, var24, 32.0F, 4.0F, 6.0F);
                  RenderUtil.a(var33, var25, var26, var24, 32.0F, 4.0F, a(1053206, 230.0F * var14));
                  RenderUtil.a(var33, var25, var26, var24, 32.0F, 4.0F, 0.5F, a(16777215, 25.0F * var14));
                  RenderUtil.a(var33, var25, var26 + 3.5F, 2.0F, 25.0F, 1.0F, a(var35, 255.0F * var14));
                  this.a(var25 + 6.0F, var26 + 5.0F, 21.0F, 3.5F, var15, var14);
                  float var27 = var26 + 4.0F;
                  Msdf.b();
                  Msdf.a(var33, MsdfManager.BOLD, var17, var25 + 31.0F, var27, 5.0F, a(var35, 255.0F * var14));
                  if (!var18.isEmpty()) {
                     Msdf.a(var33, MsdfManager.MONTSERRAT, var18, var25 + var24 - var22 - 6.0F, var27 + 0.5F, 4.0F, a(9080990, 200.0F * var14));
                  }

                  Msdf.c();
                  float var28 = var26 + 16.0F;
                  float var29 = var24 - var19 - 35.0F;
                  String var30 = var16;
                  if (var29 > 10.0F && Msdf.a(MsdfManager.MONTSERRAT, var30, 5.5F) > var29) {
                     while (var30.length() > 3 && Msdf.a(MsdfManager.MONTSERRAT, var30 + "..", 5.5F) > var29) {
                        var30 = var30.substring(0, var30.length() - 1);
                     }

                     var30 = var30 + "..";
                  }

                  Msdf.b();
                  Msdf.a(var33, MsdfManager.BOLD, var15 + ": ", var25 + 31.0F, var28, 5.5F, a(16777215, 255.0F * var14));
                  Msdf.a(var33, MsdfManager.MONTSERRAT, var30, var25 + 31.0F + var19, var28, 5.5F, a(13686240, 240.0F * var14));
                  Msdf.c();
                  float var31 = var12.k();
                  float var32 = (var24 - 12.0F) * var31;
                  if (var32 > 0.5F) {
                     RenderUtil.a(var33, var25 + 6.0F, var26 + 32.0F - 1.5F, var32, 1.0F, 0.5F, a(var35, 200.0F * var14));
                  }
               }

               var10 += 37.0F;
            }
         }
      }
   }
}
