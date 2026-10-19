package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.HudConfig;
import brokke.cc.core.RCTCommand;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;

public class EventsPanel {
   private static final int MAX_PER_REQ = 30;
   private static final long REFRESH_MS = 10000L;
   public static final float W = 360.0F;
   public static final float H = 195.0F;
   private static final float HEADER_H = 19.0F;
   private static final float CARD_H = 34.0F;
   private static final float CARD_GAP = 3.5F;
   private static final int[] FT_SERVERS_121 = new int[]{
      100, 101, 102, 103, 104, 105, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 300, 301, 302, 303, 400, 401, 500, 501, 600, 700, 800, 900, 904
   };
   private static final int[] FT_SERVERS_1165 = new int[]{
      1001, 1002, 1003, 1004, 1005, 2001, 2002, 2003, 2004, 2005, 3001, 3002, 3003, 4001, 4002, 5001, 5002, 6001, 6002, 6003, 6004
   };
   private static final Map<String, String> HW_SERVER_NAMES = c();
   private static final Map<String, String> NAME_SHORTENINGS = new HashMap<>();
   private final List<EventsPanel.a> ftCards = new CopyOnWriteArrayList<>();
   private final List<EventsPanel.a> hwCards = new CopyOnWriteArrayList<>();
   private volatile long fetchTimestamp = 0L;
   private volatile boolean fetching = false;
   private volatile String ftStatus = "Загрузка...";
   private volatile String hwStatus = "Загрузка...";
   private long lastFetch = 0L;
   public final Animation openAnim = new Animation();
   private final Animation scrollAnim = new Animation();
   private float scrollTarget = 0.0F;
   private float maxScroll = 0.0F;
   private final List<float[]> buttonBoxes = new ArrayList<>();
   private final List<Runnable> buttonActions = new ArrayList<>();
   private final float[] closeBtnBox = new float[4];

   public EventsPanel() {
      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.scrollAnim.setEasing(Easing.SMOOTH_STEP);
   }

   public void a(boolean var1) {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   public void a() {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   private void b() {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   private void a(int[] var1, String var2, List<EventsPanel.a> var3) {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   private void a(String var1, String var2, List<EventsPanel.a> var3) throws Exception {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   private void a(List<EventsPanel.a> var1) {
      this.ftStatus = "внешние сервисы отключены";
      this.hwStatus = "внешние сервисы отключены";
   }

   private static boolean a(String var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.toLowerCase();
      return var1.contains("airdrop") || var1.contains("myst_beacon") || var1.contains("myst");
   }

   public void a(DrawContext var1, float var2, float var3, int var4, int var5, float var6, boolean var7) {
      this.a(var7);
      this.openAnim.animate(var7 ? 1.0F : 0.0F, var6, 3.8F);
      float var8 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (!(var8 <= 0.005F)) {
         this.buttonBoxes.clear();
         this.buttonActions.clear();
         float var9 = 360.0F;
         float var10 = 195.0F;
         float var11 = var2;
         float var12 = var3;
         HudConfig.a("events_modal", var11, var12, var9, var10, 7.0F, new Color(0, 0, 0, (int)(245.0F * var8)));
         RenderUtil.a(var11, var12, var9, var10, 7.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var8))});
         float var13 = var11 + 8.0F;
         float var14 = var12 + 5.5F;
         RenderUtil.a(var1, MsdfManager.BOLD, var13, var14, "События", 7.0F, new Color(245, 245, 250, (int)(255.0F * var8)));
         float var15 = var11 + var9 - 10.0F;
         float var16 = var12 + 8.5F;
         this.closeBtnBox[0] = var15 - 5.0F;
         this.closeBtnBox[1] = var16 - 5.0F;
         this.closeBtnBox[2] = 10.0F;
         this.closeBtnBox[3] = 10.0F;
         boolean var17 = var4 >= this.closeBtnBox[0]
            && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var5 >= this.closeBtnBox[1]
            && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3];
         BufferBuilder var18 = ColorPipeline.a();
         if (var18 != null) {
            Color var19 = var17 ? Theme.getInstance().getAccent() : new Color(170, 175, 185);
            int var20 = (int)(255.0F * var8) << 24 | var19.getRGB() & 16777215;
            ColorPipeline.a(var18, var15 - 2.5F, var16 - 2.5F, var15 + 2.5F, var16 + 2.5F, 1.2F, var20);
            ColorPipeline.a(var18, var15 + 2.5F, var16 - 2.5F, var15 - 2.5F, var16 + 2.5F, 1.2F, var20);
            ColorPipeline.a(var18, false);
         }

         float var34 = (var9 - 18.0F) / 2.0F;
         float var35 = var11 + 6.0F;
         float var21 = var11 + 12.0F + var34;
         float var22 = var12 + 19.0F;
         Color var23 = Theme.getInstance().getAccent();
         Color var24 = new Color(var23.getRed(), var23.getGreen(), var23.getBlue(), (int)(255.0F * var8));
         RenderUtil.a(var1, MsdfManager.BOLD, var35 + 2.0F, var22, "FunTime", 6.0F, var24);
         RenderUtil.a(var1, MsdfManager.BOLD, var21 + 2.0F, var22, "HolyWorld", 6.0F, var24);
         RenderUtil.a(var11 + var9 / 2.0F - 0.5F, var22 + 10.0F, 1.0F, var10 - 19.0F - 14.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(14.0F * var8))});
         float var25 = var22 + 10.0F;
         float var26 = var10 - (var25 - var12) - 6.0F;
         this.scrollAnim.animate(this.scrollTarget, var6, 8.0F);
         float var27 = this.scrollAnim.getValue();
         long var28 = this.fetchTimestamp == 0L ? 0L : (System.currentTimeMillis() - this.fetchTimestamp) / 1000L;
         Scissor.b(var11, var25, var9, var26);
         float var30 = var25 + var27;
         if (this.ftCards.isEmpty()) {
            RenderUtil.a(var1, MsdfManager.BOLD, var35 + 2.0F, var30 + 6.0F, this.ftStatus, 5.5F, new Color(140, 145, 155, (int)(220.0F * var8)));
         } else {
            for (EventsPanel.a var32 : this.ftCards) {
               if (var30 + 34.0F >= var25 && var30 <= var25 + var26) {
                  this.a(var1, var32, var35, var30, var34, var4, var5, var8, var28);
               }

               var30 += 37.5F;
            }
         }

         float var36 = var25 + var27;
         if (this.hwCards.isEmpty()) {
            RenderUtil.a(var1, MsdfManager.BOLD, var21 + 2.0F, var36 + 6.0F, this.hwStatus, 5.5F, new Color(140, 145, 155, (int)(220.0F * var8)));
         } else {
            for (EventsPanel.a var33 : this.hwCards) {
               if (var36 + 34.0F >= var25 && var36 <= var25 + var26) {
                  this.a(var1, var33, var21, var36, var34, var4, var5, var8);
               }

               var36 += 37.5F;
            }
         }

         Scissor.c();
         float var38 = Math.max(var30 - (var25 + var27), var36 - (var25 + var27));
         this.maxScroll = Math.max(0.0F, var38 - var26);
         if (this.scrollTarget < -this.maxScroll) {
            this.scrollTarget = -this.maxScroll;
         }

         if (this.scrollTarget > 0.0F) {
            this.scrollTarget = 0.0F;
         }
      }
   }

   private void a(DrawContext var1, EventsPanel.a var2, float var3, float var4, float var5, int var6, int var7, float var8, long var9) {
      RenderUtil.a(var3, var4, var5, 34.0F, 4.0F, new Color[]{new Color(255, 255, 255, (int)(10.0F * var8))});
      RenderUtil.a(var3, var4, var5, 34.0F, 4.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(18.0F * var8))});
      float var11 = 4.5F;
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + var11, var4 + 4.0F, var2.a(), 5.5F, new Color(245, 245, 250, (int)(255.0F * var8)));
      String var12 = "/an" + var2.serverNum;
      float var13 = Msdf.a(MsdfManager.BOLD, var12, 4.5F) + 6.0F;
      float var14 = 10.0F;
      float var15 = var3 + var5 - var13 - var11;
      float var16 = var4 + 3.5F;
      boolean var17 = var6 >= var15 && var6 <= var15 + var13 && var7 >= var16 && var7 <= var16 + var14;
      Color var18 = Theme.getInstance().getAccent();
      Color var19 = new Color(var18.getRed(), var18.getGreen(), var18.getBlue(), (int)(255.0F * var8));
      RenderUtil.a(var15, var16, var13, var14, 2.5F, new Color[]{var17 ? var19 : new Color(255, 255, 255, (int)(20.0F * var8))});
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var15 + 3.0F,
         var16 + 2.5F,
         var12,
         4.5F,
         var17 ? new Color(0, 0, 0, (int)(255.0F * var8)) : new Color(240, 240, 245, (int)(255.0F * var8))
      );
      this.buttonBoxes.add(new float[]{var15, var16, var13, var14});
      this.buttonActions.add(() -> {
         MinecraftClient var1x = MinecraftClient.getInstance();
         if (var1x.player != null && var1x.player.networkHandler != null) {
            var1x.player.networkHandler.sendChatCommand("an" + var2.serverNum);
         }

         if (var1x.currentScreen != null) {
            var1x.currentScreen.close();
         }
      });
      String var20 = "Анархия " + var2.serverNum + " • " + var2.version;
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + var11, var4 + 17.0F, var20, 4.75F, new Color(155, 160, 170, (int)(255.0F * var8)));
      String var21 = var2.a(var9);
      float var22 = Msdf.a(MsdfManager.BOLD, var21, 4.75F);
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + var5 - var22 - var11, var4 + 17.0F, var21, 4.75F, var19);
   }

   private void a(DrawContext var1, EventsPanel.a var2, float var3, float var4, float var5, int var6, int var7, float var8) {
      RenderUtil.a(var3, var4, var5, 34.0F, 4.0F, new Color[]{new Color(255, 255, 255, (int)(10.0F * var8))});
      RenderUtil.a(var3, var4, var5, 34.0F, 4.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(18.0F * var8))});
      float var9 = 4.5F;
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + var9, var4 + 4.0F, var2.a(), 5.5F, new Color(245, 245, 250, (int)(255.0F * var8)));
      String var10 = var2.serverNum > 0 ? ".rct " + (StreamerMode.k() ? "██" : var2.serverNum) : ".rct";
      float var11 = Msdf.a(MsdfManager.BOLD, var10, 4.5F) + 6.0F;
      float var12 = 10.0F;
      float var13 = var3 + var5 - var11 - var9;
      float var14 = var4 + 3.5F;
      boolean var15 = var6 >= var13 && var6 <= var13 + var11 && var7 >= var14 && var7 <= var14 + var12;
      Color var16 = Theme.getInstance().getAccent();
      Color var17 = new Color(var16.getRed(), var16.getGreen(), var16.getBlue(), (int)(255.0F * var8));
      RenderUtil.a(var13, var14, var11, var12, 2.5F, new Color[]{var15 ? var17 : new Color(255, 255, 255, (int)(20.0F * var8))});
      RenderUtil.a(
         var1,
         MsdfManager.BOLD,
         var13 + 3.0F,
         var14 + 2.5F,
         var10,
         4.5F,
         var15 ? new Color(0, 0, 0, (int)(255.0F * var8)) : new Color(240, 240, 245, (int)(255.0F * var8))
      );
      this.buttonBoxes.add(new float[]{var13, var14, var11, var12});
      this.buttonActions.add(() -> {
         MinecraftClient var1x = MinecraftClient.getInstance();
         if (var2.serverNum > 0) {
            RCTCommand.a(var2.serverNum);
         } else {
            RCTCommand.b();
         }

         if (var1x.currentScreen != null) {
            var1x.currentScreen.close();
         }
      });
      RenderUtil.a(var1, MsdfManager.BOLD, var3 + var9, var4 + 17.0F, StreamerMode.a(var2.getServer()), 4.75F, new Color(155, 160, 170, (int)(255.0F * var8)));
      if (var2.phase != null && var2.phase.startsWith("HOLYWORLD_ACTIVE:")) {
         String var21 = d(var2.phase.substring("HOLYWORLD_ACTIVE:".length()));
         Color var19 = e(var21);
         float var20 = Msdf.a(MsdfManager.BOLD, var21, 4.75F);
         RenderUtil.a(
            var1,
            MsdfManager.BOLD,
            var3 + var5 - var20 - var9,
            var4 + 17.0F,
            var21,
            4.75F,
            new Color(var19.getRed(), var19.getGreen(), var19.getBlue(), (int)(255.0F * var8))
         );
      } else {
         float var18 = Msdf.a(MsdfManager.BOLD, "Активен", 4.75F);
         RenderUtil.a(var1, MsdfManager.BOLD, var3 + var5 - var18 - var9, var4 + 17.0F, "Активен", 4.75F, new Color(87, 227, 137, (int)(255.0F * var8)));
      }
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

            for (int var6 = 0; var6 < this.buttonBoxes.size(); var6++) {
               float[] var7 = this.buttonBoxes.get(var6);
               if (var1 >= var7[0] && var1 <= var7[0] + var7[2] && var2 >= var7[1] && var2 <= var7[1] + var7[3]) {
                  if (var6 < this.buttonActions.size()) {
                     this.buttonActions.get(var6).run();
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

   private static String b(String var0) {
      return var0 != null && !var0.isEmpty() ? Character.toUpperCase(var0.charAt(0)) + var0.substring(1) : var0;
   }

   private static String c(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         String var2 = NAME_SHORTENINGS.get(var1);
         if (var2 != null) {
            return var2;
         }

         int var3 = var1.indexOf(32);
         return var3 > 0 ? var1.substring(var3 + 1) : var1;
      } else {
         return var0;
      }
   }

   private static String d(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase();
         if (var1.contains("legendary") || var1.contains("легендар")) {
            return "Легендарный";
         } else if (var1.contains("mythic")) {
            return "Мифический";
         } else if (var1.contains("epic")) {
            return "Эпический";
         } else if (var1.contains("explosive") || var1.contains("взрыв")) {
            return "Взрывной";
         } else if (var1.contains("deadly") || var1.contains("смертельн")) {
            return "Смертельный";
         } else if (var1.contains("peaceful") || var1.contains("мирн")) {
            return "Мирный";
         } else if (var1.contains("roskoshni") || var1.contains("роскошн")) {
            return "Роскошный";
         } else if (!var1.contains("rare") && !var1.contains("редк")) {
            return !var1.contains("normal") && !var1.contains("common") && !var1.contains("default") && !var1.contains("обычн") ? var0 : "Обычный";
         } else {
            return "Редкий";
         }
      } else {
         return "";
      }
   }

   private static Color e(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase();
         if (var1.contains("legendary") || var1.contains("легендар") || var1.contains("roskoshni") || var1.contains("роскошн")) {
            return new Color(255, 215, 0);
         } else if (var1.contains("mythic") || var1.contains("мифич")) {
            return new Color(198, 120, 221);
         } else if (var1.contains("epic") || var1.contains("эпич")) {
            return new Color(178, 58, 238);
         } else if (var1.contains("explosive") || var1.contains("взрыв")) {
            return new Color(255, 140, 66);
         } else if (var1.contains("deadly") || var1.contains("смертельн")) {
            return new Color(255, 85, 85);
         } else if (!var1.contains("peaceful") && !var1.contains("мирн")) {
            return !var1.contains("rare") && !var1.contains("редк") ? new Color(160, 165, 175) : new Color(64, 166, 255);
         } else {
            return new Color(87, 227, 137);
         }
      } else {
         return new Color(180, 185, 195);
      }
   }

   private static String f(String var0) {
      if (var0 == null || var0.isBlank()) {
         return var0 == null ? "?" : var0;
      }

      if (var0.startsWith("LITE_NEW_ANARCHY_")) {
         return "Лайт (1.20) #" + var0.substring("LITE_NEW_ANARCHY_".length());
      }

      if (var0.startsWith("LITE_ANARCHY_")) {
         String var1 = var0.substring("LITE_ANARCHY_".length());

         try {
            int var2 = Integer.parseInt(var1);
            String var3;
            if (var2 <= 9) {
               var3 = "СолоЛайт";
            } else if (var2 <= 19) {
               var3 = "ДуоЛайт";
            } else if (var2 <= 29) {
               var3 = "ТриоЛайт";
            } else {
               var3 = "КланЛайт";
            }

            return var3 + " #" + var2;
         } catch (NumberFormatException var4) {
            return "Анархия #" + var1;
         }
      } else {
         return var0;
      }
   }

   private static Map<String, String> c() {
      HashMap var0 = new HashMap();

      for (int var1 = 1; var1 <= 9; var1++) {
         var0.put("LITE_ANARCHY_" + var1, "СолоЛайт #" + var1);
      }

      for (int var2 = 10; var2 <= 19; var2++) {
         var0.put("LITE_ANARCHY_" + var2, "ДуоЛайт #" + var2);
      }

      for (int var3 = 20; var3 <= 29; var3++) {
         var0.put("LITE_ANARCHY_" + var3, "ТриоЛайт #" + var3);
      }

      for (int var4 = 30; var4 <= 36; var4++) {
         var0.put("LITE_ANARCHY_" + var4, "КланЛайт #" + var4);
      }

      var0.put("LITE_NEW_ANARCHY_1", "Лайт (1.20) #1");
      var0.put("LITE_NEW_ANARCHY_2", "Лайт (1.20) #2");
      var0.put("LITE_NEW_ANARCHY_3", "Лайт (1.20) #3");
      return var0;
   }

   static {
      NAME_SHORTENINGS.put("Ценный груз", "Цн груз");
   }

   public record a(String server, int serverNum, String version, String eventId, String phase, int secondsLeft, String loot, String eventType) {
      String a() {
         return switch (this.eventId) {
            case "vulkan" -> "Вулкан";
            case "beacon" -> "Маяк";
            case "hellm" -> "Адский маяк";
            case "airdrop" -> "Аирдроп";
            case "myst_beacon" -> "Мист. маяк";
            case "meteor_rain" -> "Метеорит";
            case "ожидание" -> "Ожидание";
            case "supply_drop" -> "Сброс припасов";
            case "boss" -> "Босс";
            case "treasure" -> "Сокровище";
            case "raid" -> "Рейд";
            default -> this.eventId;
         };
      }

      String a(long var1) {
         if (this.phase != null && this.phase.startsWith("HOLYWORLD_ACTIVE")) {
            return "Активен";
         }

         int var3 = "LOOTING".equals(this.phase) ? Math.max(0, 60 - (int)var1) : Math.max(0, this.secondsLeft - (int)var1);
         String var4 = a(var3);
         if (this.phase == null) {
            return "Спавн: " + var4;
         }

         return switch (this.phase) {
            case "ACTIVATING" -> "Через: " + var4;
            case "RUNNING" -> "Акт: " + var4;
            case "OPENED" -> "Откр: " + var4;
            case "LOOTING" -> "Закр: " + var4;
            case "WAITING" -> "Спавн: " + var4;
            default -> var4;
         };
      }

      private static String a(int var0) {
         int var1 = var0 / 60;
         int var2 = var0 % 60;
         return var1 > 0 ? var1 + "м " + var2 + "с" : var2 + "с";
      }

      public String getServer() {
         return this.server;
      }

      public int getServerNum() {
         return this.serverNum;
      }

      public String getVersion() {
         return this.version;
      }

      public String getEventId() {
         return this.eventId;
      }

      public String getPhase() {
         return this.phase;
      }

      public int getSecondsLeft() {
         return this.secondsLeft;
      }

      public String getLoot() {
         return this.loot;
      }

      public String getEventType() {
         return this.eventType;
      }
   }
}
