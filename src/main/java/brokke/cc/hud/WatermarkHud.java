package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.core.User;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.mixins.interfaces.IBossBarHud;
import brokke.cc.module.render.NoRender;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.WatermarkConfigModal;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class WatermarkHud {
   private static final String ID = "watermark";
   private static final Identifier ICON_TEX = Identifier.of("brokke", "textures/watermark_icon.png");
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static boolean enabled = false;
   private static WatermarkHud.a positionMode = WatermarkHud.a.CENTER;
   private static boolean indicatorColors = false;
   private static final Map<String, Boolean> enabledElements = new LinkedHashMap<>();
   public static final List<String> ALL_ELEMENTS = List.of(
      "logo",
      "avatar",
      "user",
      "fps",
      "time",
      "ping",
      "jitter",
      "ip",
      "cpu",
      "ram",
      "gpu",
      "kd",
      "cpu_freq",
      "cpu_cores",
      "var",
      "loss",
      "net_problem",
      "preset",
      "pvp_timer"
   );
   private static final WatermarkHud INSTANCE = new WatermarkHud();
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private float x = 7.0F;
   private float y = 7.0F;
   private float targetX = 7.0F;
   private float targetY = 7.0F;
   private float baseTargetY = 7.0F;
   private boolean dragging;
   private boolean wasPressed;
   private boolean wasRmb;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private String draggedItem = null;
   private float draggedItemOffset = 0.0F;
   private float dragItemStartX = 0.0F;
   private float dragItemStartY = 0.0F;
   private boolean itemDragActive = false;
   private final List<String> order = ALL_ELEMENTS != null ? new ArrayList<>(ALL_ELEMENTS) : new ArrayList<>();
   private final Map<String, Float> itemAnimRelX = new HashMap<>();
   private final Map<String, Float> itemWidths = new LinkedHashMap<>();
   private final List<String> visibleOrder = new ArrayList<>();
   private final Map<String, Float> targetRelX = new HashMap<>();
   private long lastTimeSecond = -1L;
   private String cachedTimeFormatted = "";
   private int lastFps = -1;
   private String cachedFpsStrVal = "";
   private int lastPing = -1;
   private String cachedPingStrVal = "";
   private float animTotalW = 0.0F;
   private long lastAnimTime = System.nanoTime();
   private static float cachedSepW = -1.0F;
   private static float cachedFpsIconW = -1.0F;
   private static float cachedFpsIconH = -1.0F;
   private static float cachedPingIconW = -1.0F;
   private static float cachedPingIconH = -1.0F;
   private static float cachedUserIconW = -1.0F;
   private static float cachedUserIconH = -1.0F;
   private static float cachedTimeIconW = -1.0F;
   private static float cachedTimeIconH = -1.0F;
   private static float cachedJitterIconW = -1.0F;
   private static float cachedIpIconW = -1.0F;
   private static float cachedCpuIconW = -1.0F;
   private static float cachedRamIconW = -1.0F;
   private static float cachedGpuIconW = -1.0F;
   private static float cachedKdIconW = -1.0F;
   private static float cachedCpuFreqIconW = -1.0F;
   private static float cachedCpuCoresIconW = -1.0F;
   private static float cachedVarIconW = -1.0F;
   private static float cachedLossIconW = -1.0F;
   private static float cachedNetIconW = -1.0F;
   private static float cachedPresetIconW = -1.0F;
   private static float cachedPvpIconW = -1.0F;
   private static String lastCachedUser = null;
   private static float cachedUserTextW = 0.0F;
   private static String lastCachedFpsStr = null;
   private static float cachedFpsTextW = 0.0F;
   private static String lastCachedPingStr = null;
   private static float cachedPingTextW = 0.0F;
   private static String lastCachedTimeStr = null;
   private static float cachedTimeTextW = 0.0F;
   private static OperatingSystemMXBean cachedOsBean = null;
   private static Method cachedCpuMethod = null;
   private static boolean cpuMethodLookupAttempted = false;
   private String cachedCpuStr = "14% CPU";
   private float cachedCpuTextW = 0.0F;
   private String cachedRamStr = "0 MB";
   private float cachedRamTextW = 0.0F;
   private int lastJitter = -1;
   private String cachedJitterStrVal = "0 MS";
   private float cachedJitterTextW = 0.0F;
   private String lastCachedIp = null;
   private float cachedIpTextW = 0.0F;
   private String lastCachedPreset = null;
   private float cachedPresetTextW = 0.0F;
   private static float cachedGpuTextW = -1.0F;
   private static float cachedKdTextW = -1.0F;
   private static float cachedCpuFreqTextW = -1.0F;
   private static float cachedCpuCoresTextW = -1.0F;
   private static float cachedVarTextW = -1.0F;
   private static float cachedLossTextW = -1.0F;
   private static float cachedNetTextW = -1.0F;
   private static float cachedPvpTextW = -1.0F;

   private static int g() {
      if (!cpuMethodLookupAttempted) {
         cpuMethodLookupAttempted = true;

         try {
            cachedOsBean = ManagementFactory.getOperatingSystemMXBean();
            cachedCpuMethod = cachedOsBean.getClass().getMethod("getCpuLoad");
            cachedCpuMethod.setAccessible(true);
         } catch (Exception var4) {
         }
      }

      if (cachedCpuMethod != null && cachedOsBean != null) {
         try {
            if (cachedCpuMethod.invoke(cachedOsBean) instanceof Number var1) {
               double var2 = var1.doubleValue();
               if (var2 >= 0.0) {
                  return (int)Math.round(var2 * 100.0);
               }
            }
         } catch (Exception var3) {
         }
      }

      return 14;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      enabled = var0;
   }

   public static boolean a(String var0) {
      return enabledElements.getOrDefault(var0, false);
   }

   public static void a(String var0, boolean var1) {
      enabledElements.put(var0, var1);
   }

   public static void b(String var0) {
      a(var0, !a(var0));
   }

   public static WatermarkHud.a getPositionMode() {
      return positionMode;
   }

   public static void a(WatermarkHud.a var0) {
      positionMode = var0;
      d();
   }

   public static boolean isIndicatorColors() {
      return indicatorColors;
   }

   public static void setIndicatorColors(boolean var0) {
      indicatorColors = var0;
   }

   public static void d() {
      float var0 = RenderUtil.a();
      float var1 = INSTANCE.animTotalW > 0.0F ? INSTANCE.animTotalW : 100.0F;
      float var2 = HudConfig.a("watermark");
      float var3 = var1 * var2;
      if (positionMode == WatermarkHud.a.CENTER) {
         INSTANCE.targetX = (var0 - var3) / 2.0F;
      } else if (positionMode == WatermarkHud.a.LEFT) {
         INSTANCE.targetX = 7.0F;
      } else if (positionMode == WatermarkHud.a.RIGHT) {
         INSTANCE.targetX = var0 - var3 - 7.0F;
      }
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
   }

   public static void b(JsonObject var0) {
      if (var0.has("enabled")) {
         enabled = var0.get("enabled").getAsBoolean();
      }
   }

   public static float[] e() {
      return new float[]{INSTANCE.x, INSTANCE.y};
   }

   public static void a(float var0, float var1) {
      INSTANCE.x = var0;
      INSTANCE.y = var1;
      INSTANCE.targetX = var0;
      INSTANCE.targetY = var1;
      INSTANCE.baseTargetY = var1;
      INSTANCE.initPos = true;
   }

   public static void c(JsonObject var0) {
      for (Entry var2 : enabledElements.entrySet()) {
         var0.addProperty((String)var2.getKey(), (Boolean)var2.getValue());
      }

      var0.addProperty("positionMode", positionMode.name());
      JsonArray var4 = new JsonArray();

      for (String var3 : INSTANCE.order) {
         var4.add(var3);
      }

      var0.add("order", var4);
   }

   public static void d(JsonObject var0) {
      for (String var2 : ALL_ELEMENTS) {
         if (var0.has(var2)) {
            enabledElements.put(var2, var0.get(var2).getAsBoolean());
         }
      }

      if (var0.has("positionMode")) {
         try {
            positionMode = WatermarkHud.a.valueOf(var0.get("positionMode").getAsString());
         } catch (Exception var4) {
         }
      }

      if (var0.has("order")) {
         INSTANCE.order.clear();

         for (JsonElement var7 : var0.getAsJsonArray("order")) {
            String var3 = var7.getAsString();
            if (!INSTANCE.order.contains(var3) && ALL_ELEMENTS.contains(var3)) {
               INSTANCE.order.add(var3);
            }
         }

         for (String var8 : ALL_ELEMENTS) {
            if (!INSTANCE.order.contains(var8)) {
               INSTANCE.order.add(var8);
            }
         }
      }
   }

   public static void f() {
      EventBus.register(INSTANCE);
      ArrayList var0 = new ArrayList();

      for (final String var2 : ALL_ELEMENTS) {
         var0.add(new HudConfig.b() {
            @Override
            public String a() {
               for (WatermarkConfigModal.a var2x : WatermarkConfigModal.getItems()) {
                  if (var2x.getId().equals(var2)) {
                     return var2x.getLabel();
                  }
               }

               return var2;
            }

            @Override
            public boolean b() {
               return WatermarkHud.a(var2);
            }

            @Override
            public void a(boolean var1) {
               WatermarkHud.a(var2, var1);
               ConfigManager.onSettingChanged();
            }
         });
      }

      HudConfig.a("watermark", var0);
   }

   private float a(MinecraftClient var1, float var2, float var3, float var4) {
      if (var1 == null || var1.inGameHud == null) {
         return 0.0F;
      } else if (NoRender.w()) {
         return 0.0F;
      } else {
         BossBarHud var5 = var1.inGameHud.getBossBarHud();
         if (var5 == null) {
            return 0.0F;
         } else {
            Map var6 = ((IBossBarHud)var5).getBossBars();
            if (var6 != null && !var6.isEmpty()) {
               float var7 = RenderUtil.e();
               int var8 = (int)(RenderUtil.b() / 3.0F / (19.0F * var7));
               int var9 = Math.min(var6.size(), Math.max(1, var8));
               float var10 = RenderUtil.a();
               float var11 = 91.0F * var7 + 8.0F;
               float var12 = var10 * 0.5F - var11;
               float var13 = var10 * 0.5F + var11;
               float var14 = (12.0F + var9 * 19.0F) * var7;
               boolean var15 = var2 < var13 && var2 + var3 > var12;
               return var15 && var4 < var14 ? var14 + 3.0F - var4 : 0.0F;
            } else {
               return 0.0F;
            }
         }
      }
   }

   private void a(MinecraftClient var1, float var2, float var3, Map<String, Float> var4, List<String> var5, Map<String, Float> var6, float var7) {
      if (var1.currentScreen instanceof ChatScreen) {
         double var8 = var1.getWindow().getScaleFactor();
         float var10 = (float)(var1.mouse.getX() / var8) * RenderUtil.e();
         float var11 = (float)(var1.mouse.getY() / var8) * RenderUtil.e();
         boolean var12 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1;
         boolean var13 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 1) == 1;
         boolean var14 = GLFW.glfwGetKey(var1.getWindow().getHandle(), 340) == 1 || GLFW.glfwGetKey(var1.getWindow().getHandle(), 344) == 1;
         boolean var15 = var13 && !this.wasRmb;
         this.wasRmb = var13;
         float var16 = 6.0F;
         boolean var17 = var10 >= this.x - var16 && var10 <= this.x + var2 + var16 && var11 >= this.y - var16 && var11 <= this.y + var3 + var16;
         if (var15 && !HudConfig.b(var10, var11) && var17) {
            WatermarkConfigModal.c();
            if (WatermarkConfigModal.isOpen()) {
               WatermarkConfigModal.a(this.x + var2 + 5.0F, this.y);
            }
         }

         boolean var18 = var12;
         if (var18 && !this.wasPressed && !HudConfig.b(var10, var11) && var17) {
            if (var14) {
               this.dragging = true;
               this.draggedItem = null;
               this.itemDragActive = false;
            } else {
               float var19 = (var10 - this.x) / var7;
               String var20 = null;
               float var21 = 0.0F;

               for (String var23 : var5) {
                  float var24 = this.itemAnimRelX.getOrDefault(var23, var6.getOrDefault(var23, 0.0F));
                  float var25 = var4.getOrDefault(var23, 0.0F);
                  if (var19 >= var24 - 3.5F && var19 <= var24 + var25 + 3.5F) {
                     var20 = var23;
                     var21 = var19 - var24;
                     break;
                  }
               }

               if (var20 != null) {
                  this.draggedItem = var20;
                  this.draggedItemOffset = var21;
                  this.dragItemStartX = var10;
                  this.dragItemStartY = var11;
                  this.itemDragActive = false;
                  this.dragging = false;
               } else {
                  this.draggedItem = null;
                  this.itemDragActive = false;
                  this.dragging = true;
               }
            }

            this.dragDX = var10 - this.x;
            this.dragDY = var11 - this.y;
         }

         if (var18) {
            if (this.draggedItem != null && !this.dragging) {
               float var35 = var10 - this.dragItemStartX;
               float var37 = var11 - this.dragItemStartY;
               if (Math.abs(var37) > 3.5F && Math.abs(var37) > Math.abs(var35) * 0.8F) {
                  this.dragging = true;
                  this.draggedItem = null;
                  this.itemDragActive = false;
                  this.dragDX = var10 - this.x;
                  this.dragDY = var11 - this.y;
               } else if (!this.itemDragActive && Math.abs(var35) > 1.5F) {
                  this.itemDragActive = true;
               } else if (this.itemDragActive && Math.abs(var37) > 7.0F) {
                  this.dragging = true;
                  this.draggedItem = null;
                  this.itemDragActive = false;
                  this.dragDX = var10 - this.x;
                  this.dragDY = var11 - this.y;
               }

               if (this.itemDragActive && this.draggedItem != null) {
                  float var39 = (var10 - this.x) / var7;
                  float var41 = var39 - this.draggedItemOffset;
                  this.itemAnimRelX.put(this.draggedItem, var41);
                  float var43 = var41 + var4.getOrDefault(this.draggedItem, 0.0F) * 0.5F;
                  int var44 = var5.indexOf(this.draggedItem);
                  boolean var45 = false;
                  if (var44 > 0) {
                     String var26 = var5.get(var44 - 1);
                     float var27 = var6.get(var26) + var4.get(var26) * 0.5F;
                     float var28 = var6.get(this.draggedItem) + var4.get(this.draggedItem) * 0.5F;
                     float var29 = (var27 + var28) * 0.5F;
                     if (var43 < var29) {
                        int var30 = this.order.indexOf(this.draggedItem);
                        int var31 = this.order.indexOf(var26);
                        if (var30 != -1 && var31 != -1) {
                           this.order.set(var30, var26);
                           this.order.set(var31, this.draggedItem);
                           ConfigManager.onSettingChanged();
                           var45 = true;
                        }
                     }
                  }

                  if (!var45 && var44 >= 0 && var44 < var5.size() - 1) {
                     String var46 = var5.get(var44 + 1);
                     float var47 = var6.get(var46) + var4.get(var46) * 0.5F;
                     float var48 = var6.get(this.draggedItem) + var4.get(this.draggedItem) * 0.5F;
                     float var49 = (var48 + var47) * 0.5F;
                     if (var43 > var49) {
                        int var50 = this.order.indexOf(this.draggedItem);
                        int var51 = this.order.indexOf(var46);
                        if (var50 != -1 && var51 != -1) {
                           this.order.set(var50, var46);
                           this.order.set(var51, this.draggedItem);
                           ConfigManager.onSettingChanged();
                        }
                     }
                  }
               }
            } else if (this.dragging) {
               float[] var34 = HudConfig.b("watermark", var10 - this.dragDX, var11 - this.dragDY, var2, var3);
               float var36 = RenderUtil.a();
               float var38 = RenderUtil.b();
               float var40 = Math.clamp(var34[0], 2.0F, Math.max(2.0F, var36 - var2 - 2.0F));
               float var42 = Math.clamp(var34[1], 2.0F, Math.max(2.0F, var38 - var3 - 2.0F));
               this.targetX = var40;
               this.targetY = var42;
               this.baseTargetY = var42;
               this.x = var40;
               this.y = var42;
            }
         }

         if (!var18) {
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

         this.wasPressed = var18;
      } else {
         this.dragging = false;
         this.wasPressed = false;
         this.wasRmb = false;
         this.draggedItem = null;
         this.itemDragActive = false;
      }

      if (!this.dragging) {
         float var32 = this.a(var1, this.x, var2, this.baseTargetY);
         this.targetY = this.baseTargetY + var32;
      }

      if (!this.dragging && (Math.abs(this.targetX - this.x) > 0.05F || Math.abs(this.targetY - this.y) > 0.05F)) {
         float[] var33 = HudConfig.a("watermark", this.x, this.y, this.targetX, this.targetY);
         this.x = var33[0];
         this.y = var33[1];
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
         float var6 = 6.0F;
         float var7 = 16.0F;
         float var8 = 5.5F;
         float var9 = 3.5F;
         float var10 = 10.0F;
         float var11 = 6.75F;
         float var12 = 7.0F;
         float var13 = 3.5F;
         int var14 = var2.getCurrentFps();
         if (var14 != this.lastFps || this.cachedFpsStrVal.isEmpty()) {
            this.lastFps = var14;
            this.cachedFpsStrVal = var14 + " FPS";
            cachedFpsTextW = Msdf.a(MsdfManager.BOLD, this.cachedFpsStrVal, var11);
         }

         String var15 = this.cachedFpsStrVal;
         int var16 = 0;
         if (var2.getNetworkHandler() != null && var2.player != null) {
            PlayerListEntry var17 = var2.getNetworkHandler().getPlayerListEntry(var2.player.getUuid());
            if (var17 != null) {
               var16 = var17.getLatency();
            }
         }

         if (var16 != this.lastPing || this.cachedPingStrVal.isEmpty()) {
            int var61 = this.lastPing >= 0 ? Math.abs(var16 - this.lastPing) : 0;
            this.lastPing = var16;
            this.cachedPingStrVal = var16 + " MS";
            cachedPingTextW = Msdf.a(MsdfManager.BOLD, this.cachedPingStrVal, var11);
            if (var61 != this.lastJitter) {
               this.lastJitter = var61;
               this.cachedJitterStrVal = var61 + " MS";
               this.cachedJitterTextW = Msdf.a(MsdfManager.BOLD, this.cachedJitterStrVal, var11);
            }
         }

         String var62 = this.cachedPingStrVal;
         String var18 = this.cachedJitterStrVal;
         String var19 = User.getInstance().getUsername();
         if (!var19.equals(lastCachedUser)) {
            lastCachedUser = var19;
            cachedUserTextW = Msdf.a(MsdfManager.BOLD, var19, var11);
         }

         long var20 = System.currentTimeMillis() / 1000L;
         if (var20 != this.lastTimeSecond) {
            this.lastTimeSecond = var20;
            this.cachedTimeFormatted = LocalTime.now().format(TIME_FORMATTER);
            cachedTimeTextW = Msdf.a(MsdfManager.BOLD, this.cachedTimeFormatted, var11);
            int var22 = g();
            this.cachedCpuStr = var22 + "% CPU";
            this.cachedCpuTextW = Msdf.a(MsdfManager.BOLD, this.cachedCpuStr, var11);
            long var23 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
            this.cachedRamStr = var23 + " MB";
            this.cachedRamTextW = Msdf.a(MsdfManager.BOLD, this.cachedRamStr, var11);
         }

         String var63 = this.cachedTimeFormatted;
         String var64 = this.cachedCpuStr;
         String var24 = this.cachedRamStr;
         String var25 = var2.getCurrentServerEntry() != null ? var2.getCurrentServerEntry().address : "Локальный";
         if (!var25.equals(this.lastCachedIp)) {
            this.lastCachedIp = var25;
            this.cachedIpTextW = Msdf.a(MsdfManager.BOLD, var25, var11);
         }

         String var26 = "GPU";
         String var27 = "1.00 K/D";
         String var28 = "3.6 GHz";
         String var29 = Runtime.getRuntime().availableProcessors() + " Cores";
         String var30 = "0.2 VAR";
         String var31 = "0% LOSS";
         String var32 = "OK";
         String var33 = ConfigManager.getCurrentConfig();
         if (var33 == null || var33.isEmpty()) {
            var33 = "default";
         }

         if (!var33.equals(this.lastCachedPreset)) {
            this.lastCachedPreset = var33;
            this.cachedPresetTextW = Msdf.a(MsdfManager.BOLD, var33, var11);
         }

         String var34 = "15s";
         if (cachedSepW < 0.0F) {
            cachedSepW = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
            cachedFpsIconW = Msdf.a(MsdfManager.ICONS2, "f", var12);
            cachedFpsIconH = Msdf.a(MsdfManager.ICONS2, var12);
            cachedPingIconW = Msdf.a(MsdfManager.DICONS, "p", var12);
            cachedPingIconH = Msdf.a(MsdfManager.DICONS, var12);
            cachedUserIconW = Msdf.a(MsdfManager.ICONS2, "e", var12);
            cachedUserIconH = Msdf.a(MsdfManager.ICONS2, var12);
            cachedTimeIconW = Msdf.a(MsdfManager.ICONS2, "x", var12);
            cachedTimeIconH = Msdf.a(MsdfManager.ICONS2, var12);
            cachedJitterIconW = Msdf.a(MsdfManager.DICONS, "w", var12);
            cachedIpIconW = Msdf.a(MsdfManager.DICONS, "@", var12);
            cachedCpuIconW = Msdf.a(MsdfManager.DICONS, "c", var12);
            cachedRamIconW = Msdf.a(MsdfManager.DICONS, "r", var12);
            cachedGpuIconW = Msdf.a(MsdfManager.DICONS, "g", var12);
            cachedKdIconW = Msdf.a(MsdfManager.DICONS, "%", var12);
            cachedCpuFreqIconW = Msdf.a(MsdfManager.DICONS, "^", var12);
            cachedCpuCoresIconW = Msdf.a(MsdfManager.DICONS, "#", var12);
            cachedVarIconW = Msdf.a(MsdfManager.DICONS, "v", var12);
            cachedLossIconW = Msdf.a(MsdfManager.DICONS, "!", var12);
            cachedNetIconW = Msdf.a(MsdfManager.DICONS, "?", var12);
            cachedPresetIconW = Msdf.a(MsdfManager.DICONS, "*", var12);
            cachedPvpIconW = Msdf.a(MsdfManager.DICONS, "s", var12);
         }

         if (cachedGpuTextW < 0.0F) {
            cachedGpuTextW = Msdf.a(MsdfManager.BOLD, var26, var11);
            cachedKdTextW = Msdf.a(MsdfManager.BOLD, var27, var11);
            cachedCpuFreqTextW = Msdf.a(MsdfManager.BOLD, var28, var11);
            cachedCpuCoresTextW = Msdf.a(MsdfManager.BOLD, var29, var11);
            cachedVarTextW = Msdf.a(MsdfManager.BOLD, var30, var11);
            cachedLossTextW = Msdf.a(MsdfManager.BOLD, var31, var11);
            cachedNetTextW = Msdf.a(MsdfManager.BOLD, var32, var11);
            cachedPvpTextW = Msdf.a(MsdfManager.BOLD, var34, var11);
         }

         this.itemWidths.clear();
         if (a("logo")) {
            this.itemWidths.put("logo", var10);
         }

         if (a("avatar")) {
            this.itemWidths.put("avatar", var10);
         }

         if (a("user")) {
            this.itemWidths.put("user", cachedUserIconW + var9 + cachedUserTextW);
         }

         if (a("fps")) {
            this.itemWidths.put("fps", cachedFpsIconW + var9 + cachedFpsTextW);
         }

         if (a("time")) {
            this.itemWidths.put("time", cachedTimeIconW + var9 + cachedTimeTextW);
         }

         if (a("ping")) {
            this.itemWidths.put("ping", cachedPingIconW + var9 + cachedPingTextW);
         }

         if (a("jitter")) {
            this.itemWidths.put("jitter", cachedJitterIconW + var9 + this.cachedJitterTextW);
         }

         if (a("ip")) {
            this.itemWidths.put("ip", cachedIpIconW + var9 + this.cachedIpTextW);
         }

         if (a("cpu")) {
            this.itemWidths.put("cpu", cachedCpuIconW + var9 + this.cachedCpuTextW);
         }

         if (a("ram")) {
            this.itemWidths.put("ram", cachedRamIconW + var9 + this.cachedRamTextW);
         }

         if (a("gpu")) {
            this.itemWidths.put("gpu", cachedGpuIconW + var9 + cachedGpuTextW);
         }

         if (a("kd")) {
            this.itemWidths.put("kd", cachedKdIconW + var9 + cachedKdTextW);
         }

         if (a("cpu_freq")) {
            this.itemWidths.put("cpu_freq", cachedCpuFreqIconW + var9 + cachedCpuFreqTextW);
         }

         if (a("cpu_cores")) {
            this.itemWidths.put("cpu_cores", cachedCpuCoresIconW + var9 + cachedCpuCoresTextW);
         }

         if (a("var")) {
            this.itemWidths.put("var", cachedVarIconW + var9 + cachedVarTextW);
         }

         if (a("loss")) {
            this.itemWidths.put("loss", cachedLossIconW + var9 + cachedLossTextW);
         }

         if (a("net_problem")) {
            this.itemWidths.put("net_problem", cachedNetIconW + var9 + cachedNetTextW);
         }

         if (a("preset")) {
            this.itemWidths.put("preset", cachedPresetIconW + var9 + this.cachedPresetTextW);
         }

         if (a("pvp_timer")) {
            this.itemWidths.put("pvp_timer", cachedPvpIconW + var9 + cachedPvpTextW);
         }

         for (String var36 : ALL_ELEMENTS) {
            if (!this.order.contains(var36)) {
               this.order.add(var36);
            }
         }

         this.visibleOrder.clear();
         this.targetRelX.clear();
         float var65 = var6;
         boolean var66 = true;

         for (String var38 : this.order) {
            if (this.itemWidths.containsKey(var38)) {
               this.visibleOrder.add(var38);
               if (!var66) {
                  var65 += var13 + cachedSepW + var13;
               }

               this.targetRelX.put(var38, var65);
               var65 += this.itemWidths.get(var38);
               var66 = false;
            }
         }

         float var67 = var65 + var6;
         float var68 = var7;
         long var39 = System.nanoTime();
         float var41 = (float)(var39 - this.lastAnimTime) / 1.0E9F;
         float var69 = Math.clamp(var41, 0.001F, 0.05F);
         this.lastAnimTime = var39;
         if (this.animTotalW <= 0.0F) {
            this.animTotalW = var67;
         } else {
            float var42 = 1.0F - (float)Math.exp(-22.0 * var69);
            this.animTotalW = this.animTotalW + (var67 - this.animTotalW) * var42;
            if (Math.abs(var67 - this.animTotalW) < 0.05F) {
               this.animTotalW = var67;
            }
         }

         for (String var43 : this.visibleOrder) {
            if (!this.itemAnimRelX.containsKey(var43)) {
               this.itemAnimRelX.put(var43, this.targetRelX.get(var43));
            }
         }

         this.itemAnimRelX.keySet().removeIf(var1x -> !this.itemWidths.containsKey(var1x));
         float var71 = HudConfig.a("watermark");
         float var72 = this.animTotalW;
         float var44 = var72 * var71;
         float var45 = var68 * var71;
         if (!this.initPos) {
            this.x = 7.0F;
            this.y = 7.0F;
            this.targetX = 7.0F;
            this.targetY = 7.0F;
            this.baseTargetY = 7.0F;
            this.initPos = true;
         }

         this.a(var2, var44, var45, this.itemWidths, this.visibleOrder, this.targetRelX, var71);
         HudConfig.c("watermark", this.x, this.y, var44, var45);
         float var46 = 1.0F - (float)Math.exp(-22.0 * var69);

         for (String var48 : this.visibleOrder) {
            if (!var48.equals(this.draggedItem) || !this.itemDragActive) {
               float var49 = this.itemAnimRelX.getOrDefault(var48, this.targetRelX.get(var48));
               float var50 = this.targetRelX.get(var48);
               float var75 = var49 + (var50 - var49) * var46;
               if (Math.abs(var50 - var75) < 0.05F) {
                  var75 = var50;
               }

               this.itemAnimRelX.put(var48, var75);
            }
         }

         float var73 = this.y + var4;
         Matrix4f var74 = RenderUtil.d();
         var74.translate(this.x, var73, 0.0F);
         var74.scale(var71, var71, 1.0F);
         var74.translate(-this.x, -var73, 0.0F);
         HudConfig.a(var74, "watermark", this.x, var73, var72, var68, var8, (int)(180.0F * var3) << 24);
         float var76 = var73 + (var68 - var10) / 2.0F;
         float var77 = var73 + (var68 - Msdf.a(MsdfManager.MONTSERRAT, var11)) / 2.0F;
         float var51 = var73 + (var68 - cachedFpsIconH) / 2.0F;
         float var52 = var73 + (var68 - cachedPingIconH) / 2.0F;
         float var53 = var73 + (var68 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
         Msdf.b();

         for (int var54 = 0; var54 < this.visibleOrder.size() - 1; var54++) {
            String var55 = this.visibleOrder.get(var54);
            String var56 = this.visibleOrder.get(var54 + 1);
            float var57 = this.x + this.itemAnimRelX.getOrDefault(var55, this.targetRelX.getOrDefault(var55, 0.0F)) + this.itemWidths.getOrDefault(var55, 0.0F);
            float var58 = this.x + this.itemAnimRelX.getOrDefault(var56, this.targetRelX.getOrDefault(var56, 0.0F));
            float var59 = (var57 + var58 - cachedSepW) * 0.5F;
            Msdf.a(var74, MsdfManager.MONTSERRAT, "|", var59, var53, 6.5F, b(new Color(110, 115, 130), 160.0F * var3));
         }

         for (String var80 : this.visibleOrder) {
            float var81 = this.x + this.itemAnimRelX.getOrDefault(var80, this.targetRelX.get(var80));
            Color var83 = var5;
            Color var85 = Color.WHITE;
            int var86 = b(var83, 255.0F * var3);
            int var60 = b(var85, 255.0F * var3);
            if ("logo".equals(var80)) {
               RenderUtil.a(
                  var74,
                  var81 - 2.5F,
                  var76 - 2.5F,
                  var10 + 5.0F,
                  ICON_TEX,
                  0.0F,
                  (int)Math.clamp(255.0F * var3, 0.0F, 255.0F) << 24 | var83.getRGB() & 16777215
               );
            } else if ("avatar".equals(var80)) {
               this.a(var74, var2, var81, var76, var10, var3);
            } else {
               this.a(
                  var74,
                  var80,
                  var81,
                  var77,
                  var51,
                  var52,
                  var12,
                  var11,
                  var9,
                  var86,
                  var60,
                  var15,
                  var62,
                  var19,
                  var63,
                  var18,
                  var25,
                  var64,
                  var24,
                  var26,
                  var27,
                  var28,
                  var29,
                  var30,
                  var31,
                  var32,
                  var33,
                  var34
               );
            }
         }

         Msdf.c();
         HudConfig.c("watermark");
         if (WatermarkConfigModal.isOpen()) {
            double var79 = var2.getWindow().getScaleFactor();
            float var82 = (float)(var2.mouse.getX() / var79) * RenderUtil.e();
            float var84 = (float)(var2.mouse.getY() / var79) * RenderUtil.e();
            WatermarkConfigModal.a(var82, var84, var69);
         }
      }
   }

   private void a(
      Matrix4f var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      int var10,
      int var11,
      String var12,
      String var13,
      String var14,
      String var15,
      String var16,
      String var17,
      String var18,
      String var19,
      String var20,
      String var21,
      String var22,
      String var23,
      String var24,
      String var25,
      String var26,
      String var27,
      String var28
   ) {
      switch (var2) {
         case "fps":
            Msdf.a(var1, MsdfManager.ICONS2, "f", var3, var5, var7, var10);
            var3 += cachedFpsIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var12, var3, var4, var8, var11);
            break;
         case "ping":
            Msdf.a(var1, MsdfManager.DICONS, "p", var3, var6, var7, var10);
            var3 += cachedPingIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var13, var3, var4, var8, var11);
            break;
         case "user":
            Msdf.a(var1, MsdfManager.ICONS2, "e", var3, var5, var7, var10);
            var3 += cachedUserIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var14, var3, var4, var8, var11);
            break;
         case "time":
            Msdf.a(var1, MsdfManager.ICONS2, "x", var3, var5, var7, var10);
            var3 += cachedTimeIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var15, var3, var4, var8, var11);
            break;
         case "jitter":
            Msdf.a(var1, MsdfManager.DICONS, "w", var3, var6, var7, var10);
            var3 += cachedJitterIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var16, var3, var4, var8, var11);
            break;
         case "ip":
            Msdf.a(var1, MsdfManager.DICONS, "@", var3, var6, var7, var10);
            var3 += cachedIpIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var17, var3, var4, var8, var11);
            break;
         case "cpu":
            Msdf.a(var1, MsdfManager.DICONS, "c", var3, var6, var7, var10);
            var3 += cachedCpuIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var18, var3, var4, var8, var11);
            break;
         case "ram":
            Msdf.a(var1, MsdfManager.DICONS, "r", var3, var6, var7, var10);
            var3 += cachedRamIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var19, var3, var4, var8, var11);
            break;
         case "gpu":
            Msdf.a(var1, MsdfManager.DICONS, "g", var3, var6, var7, var10);
            var3 += cachedGpuIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var20, var3, var4, var8, var11);
            break;
         case "kd":
            Msdf.a(var1, MsdfManager.DICONS, "%", var3, var6, var7, var10);
            var3 += cachedKdIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var21, var3, var4, var8, var11);
            break;
         case "cpu_freq":
            Msdf.a(var1, MsdfManager.DICONS, "^", var3, var6, var7, var10);
            var3 += cachedCpuFreqIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var22, var3, var4, var8, var11);
            break;
         case "cpu_cores":
            Msdf.a(var1, MsdfManager.DICONS, "#", var3, var6, var7, var10);
            var3 += cachedCpuCoresIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var23, var3, var4, var8, var11);
            break;
         case "var":
            Msdf.a(var1, MsdfManager.DICONS, "v", var3, var6, var7, var10);
            var3 += cachedVarIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var24, var3, var4, var8, var11);
            break;
         case "loss":
            Msdf.a(var1, MsdfManager.DICONS, "!", var3, var6, var7, var10);
            var3 += cachedLossIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var25, var3, var4, var8, var11);
            break;
         case "net_problem":
            Msdf.a(var1, MsdfManager.DICONS, "?", var3, var6, var7, var10);
            var3 += cachedNetIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var26, var3, var4, var8, var11);
            break;
         case "preset":
            Msdf.a(var1, MsdfManager.DICONS, "*", var3, var6, var7, var10);
            var3 += cachedPresetIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var27, var3, var4, var8, var11);
            break;
         case "pvp_timer":
            Msdf.a(var1, MsdfManager.DICONS, "s", var3, var6, var7, var10);
            var3 += cachedPvpIconW + var9;
            Msdf.a(var1, MsdfManager.BOLD, var28, var3, var4, var8, var11);
      }
   }

   private void a(Matrix4f var1, MinecraftClient var2, float var3, float var4, float var5, float var6) {
      if (var2.player != null) {
         Identifier var7 = SkinCache.a(var2, var2.player);
         if (var7 != null) {
            try {
               AbstractTexture var8 = var2.getTextureManager().getTexture(var7);
               if (var8 != null) {
                  GpuTextureView var9 = var8.getGlTextureView();
                  if (var9 != null) {
                     int var10 = (int)Math.clamp(255.0F * var6, 0.0F, 255.0F) << 24 | 16777215;
                     TexturePipeline.b(var1, var3, var4, var5, var5, var9, var10, 2.5F, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
                     TexturePipeline.b(var1, var3, var4, var5, var5, var9, var10, 2.5F, 0.0F, 0.625F, 0.125F, 0.75F, 0.25F);
                  }
               }
            } catch (Exception var11) {
            }
         }
      }
   }

   private static Color a(Color var0, float var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)Math.clamp(var1, 0.0F, 255.0F));
   }

   private static int b(Color var0, float var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }

   static {
      enabledElements.put("logo", true);
      enabledElements.put("avatar", false);
      enabledElements.put("user", true);
      enabledElements.put("fps", true);
      enabledElements.put("time", false);
      enabledElements.put("ping", false);
      enabledElements.put("jitter", false);
      enabledElements.put("ip", false);
      enabledElements.put("cpu", false);
      enabledElements.put("ram", false);
      enabledElements.put("gpu", false);
      enabledElements.put("kd", false);
      enabledElements.put("cpu_freq", false);
      enabledElements.put("cpu_cores", false);
      enabledElements.put("var", false);
      enabledElements.put("loss", false);
      enabledElements.put("net_problem", false);
      enabledElements.put("preset", false);
      enabledElements.put("pvp_timer", true);
   }

   public enum a {
      CENTER("По центру"),
      LEFT("Слева"),
      RIGHT("Справа");

      private final String label;

      a(String nullxx) {
         this.label = nullxx;
      }

      public String getLabel() {
         return this.label;
      }
   }
}
