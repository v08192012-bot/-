package brokke.cc.config;

import brokke.cc.animation.NotificationManager;
import brokke.cc.core.HelperBinds;
import brokke.cc.cosmetic.LocalCosmetics;
import brokke.cc.hud.ArmorHud;
import brokke.cc.hud.ArrayListHud;
import brokke.cc.hud.CoolDownsHud;
import brokke.cc.hud.HotbarHud;
import brokke.cc.hud.InfoHud;
import brokke.cc.hud.InventoryHud;
import brokke.cc.hud.KeybindsHud;
import brokke.cc.hud.MusicPlayerHud;
import brokke.cc.hud.PotionsHud;
import brokke.cc.hud.ScoreboardHud;
import brokke.cc.hud.TargetHud;
import brokke.cc.hud.WatermarkHud;
import brokke.cc.module.misc.AutoSbor;
import brokke.cc.module.combat.AutoSwap;
import brokke.cc.module.combat.AutoTotem;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.misc.PvPSell;
import brokke.cc.render.Theme;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.KeybindSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.MultiSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.setting.Setting;
import brokke.cc.setting.TextSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

public class ConfigManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("brokke");
   private static final Path CONFIGS_FOLDER = CONFIG_DIR.resolve("configs");
   private static final Path DEFAULT_FILE = CONFIGS_FOLDER.resolve("default.json");
   private static String currentConfig = "default";
   private static String lastLoadedConfig = "default";
   private static boolean loading = false;
   private static boolean initialized = false;
   private static JsonObject defaultSnapshot = null;
   private static final AtomicBoolean saveScheduled = new AtomicBoolean(false);
   private static volatile long lastSaveRequestTime = 0L;

   public static boolean isLoading() {
      return loading;
   }

   public static void initialize() {
      try {
         Files.createDirectories(CONFIGS_FOLDER);
         defaultSnapshot = m();
         initialized = true;
         if (Files.exists(DEFAULT_FILE)) {
            b("default");
         } else {
            a("default");
         }
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   public static String getCurrentConfig() {
      return currentConfig;
   }

   public static String getLastLoadedConfig() {
      return lastLoadedConfig;
   }

   public static void onModuleChanged() {
      if (!loading && initialized) {
         scheduleSave();
      }
   }

   public static void onSettingChanged() {
      if (!loading && initialized) {
         scheduleSave();
      }
   }

   private static void scheduleSave() {
      lastSaveRequestTime = System.currentTimeMillis();
      if (saveScheduled.compareAndSet(false, true)) {
         CompletableFuture.runAsync(() -> {
            try {
               Thread.sleep(300L);

               while (System.currentTimeMillis() - lastSaveRequestTime < 250L) {
                  Thread.sleep(100L);
               }

               saveDefault();
            } catch (Exception var4) {
            } finally {
               saveScheduled.set(false);
            }
         });
      }
   }

   public static boolean saveCurrent() {
      return a(currentConfig);
   }

   public static boolean saveDefault() {
      try {
         Files.createDirectories(CONFIGS_FOLDER);
         JsonObject var0 = m();
         String var1 = GSON.toJson(var0);
         Files.writeString(DEFAULT_FILE, var1, StandardCharsets.UTF_8);
         return true;
      } catch (Exception var2) {
         var2.printStackTrace();
         return false;
      }
   }

   public static boolean a(String var0) {
      try {
         Files.createDirectories(CONFIGS_FOLDER);
         String var1 = var0 != null && !var0.trim().isEmpty() ? var0.trim() : "default";
         if (!var1.toLowerCase().endsWith(".json") && !var1.toLowerCase().endsWith(".cfg")) {
            var1 = var1 + ".json";
         }

         JsonObject var2 = m();
         String var3 = GSON.toJson(var2);
         Path var4 = CONFIGS_FOLDER.resolve(var1);
         Files.writeString(var4, var3, StandardCharsets.UTF_8);
         Files.writeString(DEFAULT_FILE, var3, StandardCharsets.UTF_8);
         String var5 = stripExtension(var1);
         currentConfig = var5;
         lastLoadedConfig = var5;
         return true;
      } catch (Exception var6) {
         var6.printStackTrace();
         return false;
      }
   }

   public static boolean b(String var0) {
      try {
         Files.createDirectories(CONFIGS_FOLDER);
         String var1 = var0 != null && !var0.trim().isEmpty() ? var0.trim() : "default";
         Path var2 = CONFIGS_FOLDER.resolve(var1);
         if (!Files.exists(var2) && !var1.toLowerCase().endsWith(".json") && !var1.toLowerCase().endsWith(".cfg")) {
            var2 = CONFIGS_FOLDER.resolve(var1 + ".json");
         }

         if (!Files.exists(var2)) {
            return false;
         }

         loading = true;

         try (FileReader var3 = new FileReader(var2.toFile(), StandardCharsets.UTF_8)) {
            JsonObject var4 = JsonParser.parseReader(var3).getAsJsonObject();
            if (var4 != null) {
               a(var4);
               String var5 = stripExtension(var2.getFileName().toString());
               currentConfig = var5;
               lastLoadedConfig = var5;
            }
         }

         saveDefault();
         return true;
      } catch (Exception var13) {
         var13.printStackTrace();
         return false;
      } finally {
         loading = false;
      }
   }

   public static boolean i() {
      try {
         loading = true;
         if (defaultSnapshot != null) {
            a(defaultSnapshot);
         } else {
            for (Function var1 : FunctionManager.getFunctions()) {
               var1.setEnabled(false);
               var1.setKeybind(0);

               for (Setting var3 : var1.getSettings()) {
                  if (var3 instanceof BooleanSetting var4) {
                     var4.applyValue(false);
                     var4.applyKeybind(0);
                  } else if (var3 instanceof NumberSetting var5) {
                     var5.resetToDefault();
                  } else if (var3 instanceof ModeSetting var6) {
                     if (!var6.getModes().isEmpty()) {
                        var6.setMode(var6.getModes().get(0));
                     }
                  } else if (var3 instanceof MultiSetting var7) {
                     for (String var11 : var7.getEnabled()) {
                        var7.setOption(var11, false);
                     }
                  } else if (var3 instanceof BindSetting var8) {
                     var8.setKey(-1);
                  } else if (var3 instanceof KeybindSetting var9) {
                     var9.setKeybind(0);
                  }
               }
            }

            KeybindsHud.a(7.0F, 7.0F);
            ArrayListHud.a(7.0F, 50.0F);
            PotionsHud.a(7.0F, 40.0F);
            CoolDownsHud.a(7.0F, 140.0F);
            TargetHud.a(7.0F, 105.0F);
            WatermarkHud.a(7.0F, 7.0F);
            HotbarHud.a(7.0F, 7.0F);
            ArmorHud.a(7.0F, 7.0F);
            InventoryHud.a(7.0F, 70.0F);
            ScoreboardHud.a(-1.0F, -1.0F);
            HelperBinds.a(7.0F, 7.0F);
            AutoTotem.a(-1.0F, -1.0F);
            PvPSell.a(7.0F, 100.0F);
            NotificationManager.setPosition(-1.0F, -1.0F);
            AutoSwap var17 = FunctionManager.a(AutoSwap.class);
            if (var17 != null) {
               var17.a((JsonArray)null);
            }
         }

         currentConfig = "default";
         lastLoadedConfig = "default";
         loading = false;
         saveDefault();
         return true;
      } catch (Exception var15) {
         var15.printStackTrace();
         return false;
      } finally {
         loading = false;
      }
   }

   public static boolean c(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim();
         Path var2 = CONFIGS_FOLDER.resolve(var1);
         if (!Files.exists(var2) && !var1.toLowerCase().endsWith(".json") && !var1.toLowerCase().endsWith(".cfg")) {
            var2 = CONFIGS_FOLDER.resolve(var1 + ".json");
         }

         try {
            if (Files.exists(var2)) {
               boolean var3 = Files.deleteIfExists(var2);
               if (var1.equalsIgnoreCase(currentConfig)) {
                  currentConfig = "default";
                  lastLoadedConfig = "default";
               }

               return var3;
            }
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         return false;
      } else {
         return false;
      }
   }

   public static List<String> j() {
      ArrayList var0 = new ArrayList();

      try {
         Files.createDirectories(CONFIGS_FOLDER);

         try (Stream<Path> var1 = Files.list(CONFIGS_FOLDER)) {
            var1.filter(var0x -> Files.isRegularFile(var0x))
               .filter(var0x -> var0x.toString().endsWith(".json") || var0x.toString().endsWith(".cfg"))
               .forEach(var1x -> {
                  String var2 = var1x.getFileName().toString();
                  String var3 = stripExtension(var2);
                  if (!var3.equalsIgnoreCase("default") && !var0.contains(var3)) {
                     var0.add(var3);
                  }
               });
         }
      } catch (Exception var6) {
      }

      var0.sort(String.CASE_INSENSITIVE_ORDER);
      var0.add(0, "default");
      return var0;
   }

   public static void openConfigFolder() {
      try {
         Files.createDirectories(CONFIGS_FOLDER);
         Util.getOperatingSystem().open(CONFIGS_FOLDER.toFile());
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   private static String stripExtension(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 != -1 ? var0.substring(0, var1) : var0;
   }

   private static JsonObject m() {
      JsonObject var0 = new JsonObject();
      JsonObject var1 = new JsonObject();

      for (Function var3 : FunctionManager.getFunctions()) {
         JsonObject var4 = new JsonObject();
         var4.addProperty("enabled", var3.isActive());
         var4.addProperty("keybind", var3.getKeybind());
         JsonObject var5 = new JsonObject();

         for (Setting var7 : var3.getSettings()) {
            JsonObject var8 = new JsonObject();
            if (var7 instanceof BooleanSetting var9) {
               var8.addProperty("type", "bool");
               var8.addProperty("value", var9.isToggled());
               var8.addProperty("keybind", var9.getKeybindId());
            } else if (var7 instanceof NumberSetting var10) {
               var8.addProperty("type", "number");
               var8.addProperty("value", var10.getValue());
            } else if (var7 instanceof ModeSetting var11) {
               var8.addProperty("type", "mode");
               var8.addProperty("value", var11.getSelection());
            } else if (!(var7 instanceof MultiSetting var12)) {
               if (var7 instanceof BindSetting var13) {
                  var8.addProperty("type", "bind");
                  var8.addProperty("value", var13.getKey());
               } else if (var7 instanceof KeybindSetting var14) {
                  var8.addProperty("type", "keybind");
                  var8.addProperty("value", var14.getKeybind());
               } else if (var7 instanceof ColorSetting var15) {
                  var8.addProperty("type", "color");
                  var8.addProperty("value", var15.getArgb());
               } else if (var7 instanceof TextSetting var16) {
                  var8.addProperty("type", "text");
                  var8.addProperty("value", var16.getText());
               }
            } else {
               var8.addProperty("type", "multi");
               JsonObject var35 = new JsonObject();

               for (String var19 : var12.getEnabled()) {
                  var35.addProperty(var19, var12.isOptionEnabled(var19));
               }

               var8.add("options", var35);
            }

            var5.add(var7.getName(), var8);
         }

         var4.add("settings", var5);
         var1.add(var3.getName(), var4);
      }

      var0.add("modules", var1);
      JsonObject var20 = new JsonObject();
      var20.add("keybindsHud", serializePosition(KeybindsHud.a()));
      var20.add("arrayListHud", serializePosition(ArrayListHud.a()));
      var20.add("potionsHud", serializePosition(PotionsHud.a()));
      var20.add("cooldownsHud", serializePosition(CoolDownsHud.a()));
      var20.add("targetHud", serializePosition(TargetHud.a()));
      var20.add("watermarkHud", serializePosition(WatermarkHud.e()));
      var20.add("infoHud", serializePosition(InfoHud.b()));
      var20.add("hotbarHud", serializePosition(HotbarHud.a()));
      var20.add("armorHud", serializePosition(ArmorHud.a()));
      var20.add("inventoryHud", serializePosition(InventoryHud.a()));
      var20.add("scoreboardHud", serializePosition(ScoreboardHud.a()));
      var20.add("helperbindsHud", serializePosition(HelperBinds.a()));
      var20.add("musicplayerHud", serializePosition(MusicPlayerHud.d()));
      var20.add("totemHud", serializePosition(AutoTotem.k()));
      var20.add("pvpSellHud", serializePosition(PvPSell.k()));
      var20.add("notificationsHud", serializePosition(NotificationManager.getPosition()));
      var20.addProperty("guiStyle", Theme.getInstance().getGuiStyle().name());
      var20.addProperty("accent", Theme.getInstance().getAccent().getRGB());
      JsonArray var21 = new JsonArray();

      for (Color var24 : Theme.getInstance().getPalette()) {
         var21.add(var24.getRGB());
      }

      var20.add("palette", var21);
      JsonObject var23 = new JsonObject();

      for (Entry var27 : HudConfig.getScales().entrySet()) {
         var23.addProperty((String)var27.getKey(), (Number)var27.getValue());
      }

      var20.add("scales", var23);
      JsonObject var26 = new JsonObject();
      WatermarkHud.c(var26);
      WatermarkHud.a(var26);
      var20.add("watermark", var26);
      JsonObject var28 = new JsonObject();
      InfoHud.c(var28);
      InfoHud.a(var28);
      var20.add("info", var28);
      JsonObject var29 = new JsonObject();
      TargetHud.a(var29);
      var20.add("target", var29);
      JsonObject var30 = new JsonObject();
      ScoreboardHud.a(var30);
      var20.add("scoreboard", var30);
      JsonObject var31 = new JsonObject();
      PotionsHud.a(var31);
      var20.add("potions", var31);
      JsonObject var32 = new JsonObject();
      CoolDownsHud.a(var32);
      var20.add("cooldowns", var32);
      JsonObject var33 = new JsonObject();
      KeybindsHud.a(var33);
      var20.add("keybinds", var33);
      JsonObject var34 = new JsonObject();
      ArrayListHud.c(var34);
      ArrayListHud.a(var34);
      var20.add("arrayList", var34);
      JsonObject var35 = new JsonObject();
      InventoryHud.a(var35);
      var20.add("inventory", var35);
      JsonObject var36 = new JsonObject();
      HotbarHud.a(var36);
      var20.add("hotbar", var36);
      JsonObject var37 = new JsonObject();
      HelperBinds.a(var37);
      var20.add("helperbinds", var37);
      JsonObject var38 = new JsonObject();
      MusicPlayerHud.a(var38);
      var20.add("musicplayer", var38);
      JsonObject var39 = new JsonObject();
      ArmorHud.a(var39);
      var20.add("armor", var39);
      JsonObject var40 = new JsonObject();
      NotificationManager.writeConfig(var40);
      var20.add("notifications", var40);
      var0.add("hud", var20);
      var0.add("cosmetics", LocalCosmetics.f());
      var0.add("autoSbor", AutoSbor.q());
      AutoSwap var41 = FunctionManager.a(AutoSwap.class);
      if (var41 != null) {
         var0.add("autoSwapWheel", var41.k());
      }

      var0.add("autoBuy", AutoBuyConfig.h());
      return var0;
   }

   private static void a(JsonObject var0) {
      if (var0.has("hud")) {
         JsonObject var1 = var0.getAsJsonObject("hud");
         if (var1.has("keybindsHud")) {
            float[] var2 = deserializePosition(var1.getAsJsonObject("keybindsHud"), 7.0F, 7.0F);
            KeybindsHud.a(var2[0], var2[1]);
         }

         if (var1.has("arrayListHud")) {
            float[] var17 = deserializePosition(var1.getAsJsonObject("arrayListHud"), 7.0F, 50.0F);
            ArrayListHud.a(var17[0], var17[1]);
         }

         if (var1.has("potionsHud")) {
            float[] var18 = deserializePosition(var1.getAsJsonObject("potionsHud"), 7.0F, 40.0F);
            PotionsHud.a(var18[0], var18[1]);
         }

         if (var1.has("cooldownsHud")) {
            float[] var19 = deserializePosition(var1.getAsJsonObject("cooldownsHud"), 7.0F, 140.0F);
            CoolDownsHud.a(var19[0], var19[1]);
         }

         if (var1.has("targetHud")) {
            float[] var20 = deserializePosition(var1.getAsJsonObject("targetHud"), 7.0F, 105.0F);
            TargetHud.a(var20[0], var20[1]);
         }

         if (var1.has("watermarkHud")) {
            float[] var21 = deserializePosition(var1.getAsJsonObject("watermarkHud"), 7.0F, 7.0F);
            WatermarkHud.a(var21[0], var21[1]);
         }

         if (var1.has("infoHud")) {
            float[] var22 = deserializePosition(var1.getAsJsonObject("infoHud"), 7.0F, 26.0F);
            InfoHud.a(var22[0], var22[1]);
         }

         if (var1.has("hotbarHud")) {
            float[] var23 = deserializePosition(var1.getAsJsonObject("hotbarHud"), 7.0F, 7.0F);
            HotbarHud.a(var23[0], var23[1]);
         }

         if (var1.has("armorHud")) {
            float[] var24 = deserializePosition(var1.getAsJsonObject("armorHud"), 7.0F, 7.0F);
            ArmorHud.a(var24[0], var24[1]);
         }

         if (var1.has("inventoryHud")) {
            float[] var25 = deserializePosition(var1.getAsJsonObject("inventoryHud"), 7.0F, 70.0F);
            InventoryHud.a(var25[0], var25[1]);
         }

         if (var1.has("scoreboardHud")) {
            float[] var26 = deserializePosition(var1.getAsJsonObject("scoreboardHud"), -1.0F, -1.0F);
            ScoreboardHud.a(var26[0], var26[1]);
         }

         if (var1.has("helperbindsHud")) {
            float[] var27 = deserializePosition(var1.getAsJsonObject("helperbindsHud"), 7.0F, 7.0F);
            HelperBinds.a(var27[0], var27[1]);
         }

         if (var1.has("musicplayerHud")) {
            float[] var28 = deserializePosition(var1.getAsJsonObject("musicplayerHud"), 7.0F, 46.0F);
            MusicPlayerHud.a(var28[0], var28[1]);
         }

         if (var1.has("totemHud")) {
            float[] var29 = deserializePosition(var1.getAsJsonObject("totemHud"), -1.0F, -1.0F);
            AutoTotem.a(var29[0], var29[1]);
         }

         if (var1.has("pvpSellHud")) {
            float[] var30 = deserializePosition(var1.getAsJsonObject("pvpSellHud"), 7.0F, 100.0F);
            PvPSell.a(var30[0], var30[1]);
         }

         if (var1.has("notificationsHud")) {
            float[] var31 = deserializePosition(var1.getAsJsonObject("notificationsHud"), -1.0F, -1.0F);
            NotificationManager.setPosition(var31[0], var31[1]);
         }

         if (var1.has("guiStyle")) {
            try {
               Theme.a var32 = Theme.a.valueOf(var1.get("guiStyle").getAsString());
               Theme.getInstance().a(var32);
            } catch (Exception var13) {
            }
         }

         if (var1.has("accent")) {
            try {
               int var33 = var1.get("accent").getAsInt();
               Theme.getInstance().a(new Color(var33, true));
            } catch (Exception var12) {
            }
         }

         if (var1.has("palette")) {
            try {
               JsonArray var34 = var1.getAsJsonArray("palette");
               ArrayList var3 = new ArrayList();

               for (JsonElement var5 : var34) {
                  var3.add(new Color(var5.getAsInt(), true));
               }

               Theme.getInstance().a(var3);
            } catch (Exception var14) {
            }
         }

         if (var1.has("scales")) {
            JsonObject var35 = var1.getAsJsonObject("scales");

            for (String var6 : new String[]{
               "keybinds",
               "potions",
               "cooldowns",
               "target",
               "watermark",
               "info",
               "hotbar",
               "armor",
               "inventory",
               "scoreboard",
               "helperbinds",
               "musicplayer",
               "pvpSell",
               "notifications"
            }) {
               if (var35.has(var6)) {
                  HudConfig.b(var6, var35.get(var6).getAsFloat());
               }
            }
         }

         if (var1.has("watermark")) {
            JsonObject var36 = var1.getAsJsonObject("watermark");
            WatermarkHud.d(var36);
            WatermarkHud.b(var36);
         }

         if (var1.has("info")) {
            JsonObject var37 = var1.getAsJsonObject("info");
            InfoHud.d(var37);
            InfoHud.b(var37);
         }

         if (var1.has("target")) {
            TargetHud.b(var1.getAsJsonObject("target"));
         }

         if (var1.has("scoreboard")) {
            ScoreboardHud.b(var1.getAsJsonObject("scoreboard"));
         }

         if (var1.has("potions")) {
            PotionsHud.b(var1.getAsJsonObject("potions"));
         }

         if (var1.has("cooldowns")) {
            CoolDownsHud.b(var1.getAsJsonObject("cooldowns"));
         }

         if (var1.has("keybinds")) {
            KeybindsHud.b(var1.getAsJsonObject("keybinds"));
         }

         if (var1.has("arrayList")) {
            ArrayListHud.d(var1.getAsJsonObject("arrayList"));
            ArrayListHud.b(var1.getAsJsonObject("arrayList"));
         }

         if (var1.has("inventory")) {
            InventoryHud.b(var1.getAsJsonObject("inventory"));
         }

         if (var1.has("hotbar")) {
            HotbarHud.b(var1.getAsJsonObject("hotbar"));
         }

         if (var1.has("helperbinds")) {
            HelperBinds.b(var1.getAsJsonObject("helperbinds"));
         }

         if (var1.has("musicplayer")) {
            MusicPlayerHud.b(var1.getAsJsonObject("musicplayer"));
         }

         if (var1.has("armor")) {
            ArmorHud.b(var1.getAsJsonObject("armor"));
         }

         if (var1.has("notifications")) {
            NotificationManager.readConfig(var1.getAsJsonObject("notifications"));
         }
      }

      if (var0.has("cosmetics")) {
         try {
            LocalCosmetics.a(var0.getAsJsonObject("cosmetics"));
         } catch (Exception var11) {
         }
      }

      if (var0.has("autoSbor")) {
         try {
            AutoSbor.a(var0.getAsJsonObject("autoSbor"));
         } catch (Exception var10) {
         }
      }

      if (var0.has("autoSwapWheel")) {
         AutoSwap var15 = FunctionManager.a(AutoSwap.class);
         if (var15 != null) {
            try {
               var15.a(var0.getAsJsonArray("autoSwapWheel"));
            } catch (Exception var9) {
            }
         }
      }

      if (var0.has("autoBuy")) {
         try {
            AutoBuyConfig.a(var0.getAsJsonObject("autoBuy"));
         } catch (Exception var8) {
         }
      }

      JsonObject var16 = var0.has("modules") ? var0.getAsJsonObject("modules") : var0;

      for (Function var40 : FunctionManager.getFunctions()) {
         try {
            a(var40, var16);
         } catch (Exception var7) {
            var7.printStackTrace();
         }
      }
   }

   private static JsonObject serializePosition(float[] var0) {
      JsonObject var1 = new JsonObject();
      var1.addProperty("x", var0[0]);
      var1.addProperty("y", var0[1]);
      return var1;
   }

   private static float[] deserializePosition(JsonObject var0, float var1, float var2) {
      try {
         return new float[]{var0.get("x").getAsFloat(), var0.get("y").getAsFloat()};
      } catch (Exception var4) {
         return new float[]{var1, var2};
      }
   }

   private static void a(Function var0, JsonObject var1) {
      if (var1.has(var0.getName())) {
         JsonObject var2 = var1.getAsJsonObject(var0.getName());
         if (var2.has("keybind")) {
            var0.setKeybind(var2.get("keybind").getAsInt());
         }

         if (var2.has("settings")) {
            JsonObject var3 = var2.getAsJsonObject("settings");

            for (Setting var5 : var0.getSettings()) {
               JsonObject var6 = null;
               if (var3.has(var5.getName())) {
                  var6 = var3.getAsJsonObject(var5.getName());
               } else if (var5.getName().equals("Не поворачивать камеру") && var3.has("Не двигать камеру")) {
                  var6 = var3.getAsJsonObject("Не двигать камеру");
               } else if (var5.getName().equals("Трапка хорус") && var3.has("Трапка")) {
                  var6 = var3.getAsJsonObject("Трапка");
               } else if (var5.getName().equals("Боссбар") && var3.has("Bossbar")) {
                  var6 = var3.getAsJsonObject("Bossbar");
               }

               if (var6 != null) {
                  String var7 = var6.has("type") ? var6.get("type").getAsString() : "";
                  if (var5 instanceof BooleanSetting var8 && var7.equals("bool")) {
                     var8.applyValue(var6.get("value").getAsBoolean());
                     if (var6.has("keybind")) {
                        var8.applyKeybind(var6.get("keybind").getAsInt());
                     }
                  } else if (var5 instanceof NumberSetting var9 && var7.equals("number")) {
                     var9.setValue(var6.get("value").getAsDouble());
                  } else if (var5 instanceof ModeSetting var10 && var7.equals("mode")) {
                     var10.setMode(var6.get("value").getAsString());
                  } else if (var5 instanceof MultiSetting var11 && var7.equals("multi")) {
                     JsonObject var16 = var6.getAsJsonObject("options");

                     for (String var18 : var16.keySet()) {
                        var11.setOption(var18, var16.get(var18).getAsBoolean());
                     }
                  } else if (var5 instanceof BindSetting var12 && var7.equals("bind")) {
                     var12.setKey(var6.get("value").getAsInt());
                  } else if (var5 instanceof KeybindSetting var13 && var7.equals("keybind")) {
                     var13.setKeybind(var6.get("value").getAsInt());
                  } else if (var5 instanceof ColorSetting var14 && var7.equals("color")) {
                     var14.setColor(new Color(var6.get("value").getAsInt(), true));
                  } else if (var5 instanceof TextSetting var15 && var7.equals("text")) {
                     var15.setValue(var6.get("value").getAsString());
                  }
               }
            }
         }

         if (var2.has("enabled")) {
            boolean var19 = var2.get("enabled").getAsBoolean();
            var0.setEnabled(var19);
         }
      }
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::saveDefault, "Brokke-Config-Shutdown"));
   }
}
