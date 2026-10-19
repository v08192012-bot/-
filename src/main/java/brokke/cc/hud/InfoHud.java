package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.manager.ServerTPSManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.util.MathUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class InfoHud {
   private static final InfoHud INSTANCE = new InfoHud();
   private static final String ID = "info";
   private static boolean enabled = false;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private final Animation chatAnim = new Animation();
   private float x = 7.0F;
   private float y = 26.0F;
   private float targetX = 7.0F;
   private float targetY = 26.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private String draggedItem = null;
   private float draggedItemOffset = 0.0F;
   private float dragItemStartX = 0.0F;
   private float dragItemStartY = 0.0F;
   private boolean itemDragActive = false;
   private final List<String> order = new ArrayList<>(List.of("coords", "bps", "tps"));
   private final Map<String, Float> itemAnimRelX = new HashMap<>();
   private final Map<String, Float> itemWidths = new LinkedHashMap<>();
   private final List<String> visibleOrder = new ArrayList<>();
   private final Map<String, Float> targetRelX = new HashMap<>();
   private int lastX = Integer.MIN_VALUE;
   private int lastY = Integer.MIN_VALUE;
   private int lastZ = Integer.MIN_VALUE;
   private String cachedCoordsValue = "";
   private float lastBpsVal = -1.0F;
   private String cachedBpsFormatted = "";
   private float lastTpsVal = -1.0F;
   private String cachedTpsFormatted = "";
   private float animTotalW = 0.0F;
   private long lastAnimTime = System.nanoTime();
   private static float cachedSepW = -1.0F;
   private static float cachedCoordsIconW = -1.0F;
   private static float cachedCoordsIconH = -1.0F;
   private static float cachedBpsIconW = -1.0F;
   private static float cachedBpsIconH = -1.0F;
   private static float cachedTpsIconW = -1.0F;
   private static float cachedTpsIconH = -1.0F;
   private static String lastCachedCoordsStr = null;
   private static float cachedCoordsTextW = 0.0F;
   private static String lastCachedBpsStr = null;
   private static float cachedBpsTextW = 0.0F;
   private static String lastCachedTpsStr = null;
   private static float cachedTpsTextW = 0.0F;
   static boolean showCoords = true;
   static boolean showBps = true;
   static boolean showTps = true;

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      enabled = var0;
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
   }

   public static void b(JsonObject var0) {
      if (var0.has("enabled")) {
         enabled = var0.get("enabled").getAsBoolean();
      }
   }

   public static float[] b() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.targetX = var0;
      INSTANCE.targetY = var1;
      INSTANCE.initPos = true;
   }

   public static void c(JsonObject var0) {
      var0.addProperty("coords", showCoords);
      var0.addProperty("bps", showBps);
      var0.addProperty("tps", showTps);
      JsonArray var1 = new JsonArray();

      for (String var3 : INSTANCE.order) {
         var1.add(var3);
      }

      var0.add("order", var1);
   }

   public static void d(JsonObject var0) {
      if (var0.has("coords")) {
         showCoords = var0.get("coords").getAsBoolean();
      }

      if (var0.has("bps")) {
         showBps = var0.get("bps").getAsBoolean();
      }

      if (var0.has("tps")) {
         showTps = var0.get("tps").getAsBoolean();
      }

      if (var0.has("order")) {
         INSTANCE.order.clear();

         for (JsonElement var2 : var0.getAsJsonArray("order")) {
            INSTANCE.order.add(var2.getAsString());
         }

         for (String var4 : List.of("coords", "bps", "tps")) {
            if (!INSTANCE.order.contains(var4)) {
               INSTANCE.order.add(var4);
            }
         }
      }
   }

   public static void c() {
      EventBus.register(INSTANCE);
      ArrayList var0 = new ArrayList();
      var0.add(new HudConfig.b() {
         @Override
         public String a() {
            return "Координаты";
         }

         @Override
         public boolean b() {
            return InfoHud.showCoords;
         }

         @Override
         public void a(boolean var1) {
            InfoHud.showCoords = var1;
            ConfigManager.onSettingChanged();
         }
      });
      var0.add(new HudConfig.b() {
         @Override
         public String a() {
            return "BPS";
         }

         @Override
         public boolean b() {
            return InfoHud.showBps;
         }

         @Override
         public void a(boolean var1) {
            InfoHud.showBps = var1;
            ConfigManager.onSettingChanged();
         }
      });
      var0.add(new HudConfig.b() {
         @Override
         public String a() {
            return "TPS";
         }

         @Override
         public boolean b() {
            return InfoHud.showTps;
         }

         @Override
         public void a(boolean var1) {
            InfoHud.showTps = var1;
            ConfigManager.onSettingChanged();
         }
      });
      HudConfig.a("info", var0);
   }

   private void a(MinecraftClient var1, float var2, float var3, Map<String, Float> var4, List<String> var5, Map<String, Float> var6, float var7, float var8) {
      if (!(var1.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
         this.draggedItem = null;
         this.itemDragActive = false;
      } else {
         double var9 = var1.getWindow().getScaleFactor();
         float var11 = (float)(var1.mouse.getX() / var9) * RenderUtil.e();
         float var12 = (float)(var1.mouse.getY() / var9) * RenderUtil.e();
         int var13;
         if ((var13 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1 ? 1 : 0) != 0
            && !this.wasPressed
            && !HudConfig.b(var11, var12)
            && var11 >= this.x - 2.0F
            && var11 <= this.x + var2 + 2.0F
            && var12 >= var8 - 2.0F
            && var12 <= var8 + var3 + 2.0F) {
            float var14 = (var11 - this.x) / var7;
            String var15 = null;
            float var16 = 0.0F;

            for (String var18 : var5) {
               float var19 = this.itemAnimRelX.getOrDefault(var18, var6.getOrDefault(var18, 0.0F));
               float var20 = var4.getOrDefault(var18, 0.0F);
               if (var14 >= var19 - 3.5F && var14 <= var19 + var20 + 3.5F) {
                  var15 = var18;
                  var16 = var14 - var19;
                  break;
               }
            }

            if (var15 != null) {
               this.draggedItem = var15;
               this.draggedItemOffset = var16;
               this.dragItemStartX = var11;
               this.dragItemStartY = var12;
               this.itemDragActive = false;
               this.dragging = false;
            } else {
               this.draggedItem = null;
               this.itemDragActive = false;
               this.dragging = true;
            }

            this.dragDX = var11 - this.targetX;
            this.dragDY = var12 - this.targetY;
         }

         if (var13 != 0) {
            if (this.draggedItem != null) {
               if (!this.itemDragActive && (Math.abs(var11 - this.dragItemStartX) > 2.0F || Math.abs(var12 - this.dragItemStartY) > 2.0F)) {
                  if (Math.abs(var12 - this.dragItemStartY) > Math.abs(var11 - this.dragItemStartX) * 1.5F && Math.abs(var12 - this.dragItemStartY) > 5.0F) {
                     this.dragging = true;
                     this.draggedItem = null;
                     this.itemDragActive = false;
                  } else {
                     this.itemDragActive = true;
                  }
               }

               if (this.itemDragActive && this.draggedItem != null) {
                  float var24 = (var11 - this.x) / var7 - this.draggedItemOffset;
                  this.itemAnimRelX.put(this.draggedItem, var24);
                  float var25 = var24 + var4.getOrDefault(this.draggedItem, 0.0F) * 0.5F;
                  int var26;
                  if ((var26 = var5.indexOf(this.draggedItem)) > 0) {
                     String var27 = var5.get(var26 - 1);
                     float var29 = var6.get(var27) + var4.get(var27) * 0.5F;
                     if (var25 < var29) {
                        int var31 = this.order.indexOf(this.draggedItem);
                        int var21 = this.order.indexOf(var27);
                        if (var31 != -1 && var21 != -1) {
                           this.order.set(var31, var27);
                           this.order.set(var21, this.draggedItem);
                           ConfigManager.onSettingChanged();
                        }
                     }
                  }

                  if (var26 >= 0 && var26 < var5.size() - 1) {
                     String var28 = var5.get(var26 + 1);
                     float var30 = var6.get(var28) + var4.get(var28) * 0.5F;
                     if (var25 > var30) {
                        int var32 = this.order.indexOf(this.draggedItem);
                        int var33 = this.order.indexOf(var28);
                        if (var32 != -1 && var33 != -1) {
                           this.order.set(var32, var28);
                           this.order.set(var33, this.draggedItem);
                           ConfigManager.onSettingChanged();
                        }
                     }
                  }
               }
            }

            if (this.dragging) {
               float[] var22 = HudConfig.b("info", var11 - this.dragDX, var12 - this.dragDY, var2, var3);
               this.targetX = var22[0];
               this.targetY = var22[1];
            }
         }

         if (var13 == 0) {
            if (this.draggedItem != null || this.dragging) {
               if (this.dragging) {
                  HudConfig.b();
               }

               ConfigManager.onSettingChanged();
            }

            this.dragging = false;
            this.draggedItem = null;
            this.itemDragActive = false;
         }

         this.wasPressed = var13 != 0;
         if (this.dragging || Math.abs(this.targetX - this.x) > 0.05F || Math.abs(this.targetY - this.y) > 0.05F) {
            float[] var23 = HudConfig.a("info", this.x, this.y, this.targetX, this.targetY);
            this.x = var23[0];
            this.y = var23[1];
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.player != null && !var2.options.hudHidden) {
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(1.0F, 0.0F, 5.5F);
         float var3 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F);
         float var4 = (1.0F - var3) * 4.0F;
         Color var5 = Theme.getInstance().getAccent();
         int var6 = (int)Math.floor(var2.player.getX());
         int var7 = (int)Math.floor(var2.player.getY());
         int var8 = (int)Math.floor(var2.player.getZ());
         if (var6 != this.lastX || var7 != this.lastY || var8 != this.lastZ) {
            this.lastX = var6;
            this.lastY = var7;
            this.lastZ = var8;
            this.cachedCoordsValue = var6 + " " + var7 + " " + var8;
         }

         String var9 = this.cachedCoordsValue;
         float var10;
         if (Math.abs((var10 = MathUtils.a()) - this.lastBpsVal) >= 0.05F || this.cachedBpsFormatted.isEmpty()) {
            this.lastBpsVal = var10;
            this.cachedBpsFormatted = String.format(Locale.ROOT, "%.1f BPS", var10);
         }

         String var11 = this.cachedBpsFormatted;
         float var12;
         if (Math.abs((var12 = ServerTPSManager.getInstance().b()) - this.lastTpsVal) >= 0.05F || this.cachedTpsFormatted.isEmpty()) {
            this.lastTpsVal = var12;
            this.cachedTpsFormatted = String.format(Locale.ROOT, "%.1f TPS", var12);
         }

         String var13 = this.cachedTpsFormatted;
         float var14 = 6.0F;
         float var15 = 16.0F;
         float var16 = 5.5F;
         float var17 = 3.5F;
         float var18 = 6.75F;
         float var19 = 7.0F;
         if (cachedSepW < 0.0F) {
            cachedSepW = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
            cachedCoordsIconW = Msdf.a(MsdfManager.ICONS2, "g", var19);
            cachedCoordsIconH = Msdf.a(MsdfManager.ICONS2, var19);
            cachedBpsIconW = Msdf.a(MsdfManager.ICONS, "C", var19);
            cachedBpsIconH = Msdf.a(MsdfManager.ICONS, var19);
            cachedTpsIconW = Msdf.a(MsdfManager.ICONS2, "x", var19);
            cachedTpsIconH = Msdf.a(MsdfManager.ICONS2, var19);
         }

         float var20 = cachedSepW;
         float var21 = 3.5F;
         if (!var9.equals(lastCachedCoordsStr)) {
            lastCachedCoordsStr = var9;
            cachedCoordsTextW = Msdf.a(MsdfManager.BOLD, var9, var18);
         }

         float var22 = cachedCoordsIconW;
         float var23 = cachedCoordsIconH;
         float var24 = cachedCoordsTextW;
         float var25 = var22 + var17 + var24;
         if (!var11.equals(lastCachedBpsStr)) {
            lastCachedBpsStr = var11;
            cachedBpsTextW = Msdf.a(MsdfManager.BOLD, var11, var18);
         }

         float var26 = cachedBpsIconW;
         float var27 = cachedBpsIconH;
         float var28 = cachedBpsTextW;
         float var29 = var26 + var17 + var28;
         if (!var13.equals(lastCachedTpsStr)) {
            lastCachedTpsStr = var13;
            cachedTpsTextW = Msdf.a(MsdfManager.BOLD, var13, var18);
         }

         float var30 = cachedTpsIconW;
         float var32 = cachedTpsTextW;
         float var33 = var30 + var17 + var32;
         this.itemWidths.clear();
         if (showCoords) {
            this.itemWidths.put("coords", var25);
         }

         if (showBps) {
            this.itemWidths.put("bps", var29);
         }

         if (showTps) {
            this.itemWidths.put("tps", var33);
         }

         this.visibleOrder.clear();
         this.targetRelX.clear();
         float var34 = var14;
         byte var35 = 1;

         for (String var37 : this.order) {
            if (this.itemWidths.containsKey(var37)) {
               this.visibleOrder.add(var37);
               if (var35 == 0) {
                  var34 += var21 + var20 + var21;
               }

               this.targetRelX.put(var37, var34);
               var34 += this.itemWidths.get(var37);
               var35 = 0;
            }
         }

         float var63 = var34 + var14;
         float var64 = var15;
         long var38;
         float var40 = Math.clamp((float)((var38 = System.nanoTime()) - this.lastAnimTime) / 1.0E9F, 0.001F, 0.05F);
         this.lastAnimTime = var38;
         if (this.animTotalW <= 0.0F) {
            this.animTotalW = var63;
         } else {
            float var41 = 1.0F - (float)Math.exp(-22.0 * var40);
            this.animTotalW = this.animTotalW + (var63 - this.animTotalW) * var41;
            if (Math.abs(var63 - this.animTotalW) < 0.05F) {
               this.animTotalW = var63;
            }
         }

         for (String var42 : this.visibleOrder) {
            if (!this.itemAnimRelX.containsKey(var42)) {
               this.itemAnimRelX.put(var42, this.targetRelX.get(var42));
            }
         }

         this.itemAnimRelX.keySet().removeIf(var1x -> !this.itemWidths.containsKey(var1x));
         float var66 = HudConfig.a("info");
         float var67 = this.animTotalW;
         float var43 = this.animTotalW * var66;
         float var44 = var64 * var66;
         if (!this.initPos) {
            this.x = 7.0F;
            this.y = 26.0F;
            this.targetX = 7.0F;
            this.targetY = 26.0F;
            this.initPos = true;
         }

         boolean var45 = var2.currentScreen instanceof ChatScreen;
         int var46 = this.y > RenderUtil.b() - 50.0F ? 1 : 0;
         float var47 = var45 && var46 != 0 && !this.dragging ? -15.0F : 0.0F;
         this.chatAnim.setEasing(Easing.SPRING_SOFT);
         this.chatAnim.animate(var47, 0.0F, 6.0F);
         float var48 = this.chatAnim.getValue();
         float var49 = this.y + var48;
         this.a(var2, var43, var44, this.itemWidths, this.visibleOrder, this.targetRelX, var66, var49);
         HudConfig.c("info", this.x, var49, var43, var44);
         float var50 = 1.0F - (float)Math.exp(-22.0 * var40);

         for (String var52 : this.visibleOrder) {
            if (!var52.equals(this.draggedItem) || !this.itemDragActive) {
               float var53 = this.itemAnimRelX.getOrDefault(var52, this.targetRelX.get(var52));
               float var54 = this.targetRelX.get(var52);
               float var70 = var53 + (var54 - var53) * var50;
               if (Math.abs(var54 - var70) < 0.05F) {
                  var70 = var54;
               }

               this.itemAnimRelX.put(var52, var70);
            }
         }

         float var68 = var49 + var4;
         Matrix4f var69 = RenderUtil.d();
         var69.translate(this.x, var68, 0.0F);
         var69.scale(var66, var66, 1.0F);
         var69.translate(-this.x, -var68, 0.0F);
         HudConfig.a(var69, "info", this.x, var68, var67, var64, var16, (int)(180.0F * var3) << 24);
         float var71 = var68 + (var64 - Msdf.a(MsdfManager.MONTSERRAT, var18)) / 2.0F;
         float var72 = var68 + (var64 - var23) / 2.0F;
         float var55 = var68 + (var64 - var27) / 2.0F;
         float var56 = var68 + (var64 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
         Msdf.b();

         for (int var57 = 0; var57 < this.visibleOrder.size() - 1; var57++) {
            String var58 = this.visibleOrder.get(var57);
            String var59 = this.visibleOrder.get(var57 + 1);
            float var60 = this.x + this.itemAnimRelX.getOrDefault(var58, this.targetRelX.getOrDefault(var58, 0.0F)) + this.itemWidths.getOrDefault(var58, 0.0F);
            float var61 = this.x + this.itemAnimRelX.getOrDefault(var59, this.targetRelX.getOrDefault(var59, 0.0F));
            float var62 = (var60 + var61 - var20) * 0.5F;
            Msdf.a(var69, MsdfManager.MONTSERRAT, "|", var62, var56, 6.5F, b(new Color(110, 115, 130), 160.0F * var3));
         }

         for (String var74 : this.visibleOrder) {
            float var75 = this.x + this.itemAnimRelX.getOrDefault(var74, this.targetRelX.getOrDefault(var74, 0.0F));
            if ("coords".equals(var74)) {
               Msdf.a(var69, MsdfManager.ICONS2, "g", var75, var72, var19, b(var5, 255.0F * var3));
               var75 += var22 + var17;
               Msdf.a(var69, MsdfManager.BOLD, var9, var75, var71, var18, b(Color.WHITE, 255.0F * var3));
            } else if ("bps".equals(var74)) {
               Msdf.a(var69, MsdfManager.ICONS, "C", var75, var55, var19, b(var5, 255.0F * var3));
               var75 += var26 + var17;
               Msdf.a(var69, MsdfManager.BOLD, var11, var75, var71, var18, b(Color.WHITE, 255.0F * var3));
            } else if ("tps".equals(var74)) {
               Color var79 = var12 < 15.0F ? new Color(235, 87, 87) : (var12 < 18.5F ? new Color(242, 153, 74) : var5);
               Color var80 = var12 < 15.0F ? new Color(235, 87, 87) : (var12 < 18.5F ? new Color(242, 153, 74) : Color.WHITE);
               Msdf.a(var69, MsdfManager.ICONS2, "x", var75, var72, var19, b(var79, 255.0F * var3));
               var75 += var30 + var17;
               Msdf.a(var69, MsdfManager.BOLD, var13, var75, var71, var18, b(var80, 255.0F * var3));
            }
         }

         Msdf.c();
         HudConfig.c("info");
      }
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
   }

   private static int b(Color var0, float var1) {
      return (int)Math.clamp(var1, 0.0F, 255.0F) << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }
}
