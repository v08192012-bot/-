package cmode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cmode.module.Function;
import cmode.module.FunctionManager;
import cmode.setting.BooleanSetting;
import cmode.setting.ColorSetting;
import cmode.setting.ModeSetting;
import cmode.setting.NumberSetting;
import cmode.setting.Setting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("cmode");
   private static final Path FILE = DIR.resolve("state.json");
   private static boolean loading;
   private static boolean initialized;

   private ConfigManager() {
   }

   public static void initialize() {
      if (initialized) {
         return;
      }

      initialized = true;
      if (Files.exists(FILE)) {
         loading = true;

         try {
            load(JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject());
         } catch (Exception var1) {
            var1.printStackTrace();
         } finally {
            loading = false;
         }
      } else {
         save();
      }
   }

   public static boolean isLoading() {
      return loading;
   }

   public static void onModuleChanged() {
      if (initialized && !loading) {
         save();
      }
   }

   public static void onSettingChanged() {
      if (initialized && !loading) {
         save();
      }
   }

   public static void saveDefault() {
      save();
   }

   private static void save() {
      try {
         Files.createDirectories(DIR);
         JsonObject var0 = new JsonObject();

         for (Function var2 : FunctionManager.getFunctions()) {
            JsonObject var3 = new JsonObject();
            var3.addProperty("enabled", var2.isEnabled());
            var3.addProperty("keybind", var2.getKeybind());
            JsonObject var4 = new JsonObject();

            for (Setting var6 : var2.getSettings()) {
               if (var6 instanceof BooleanSetting var7) {
                  var4.addProperty(var7.getName(), var7.isToggled());
               } else if (var6 instanceof NumberSetting var8) {
                  var4.addProperty(var8.getName(), var8.getValue());
               } else if (var6 instanceof ModeSetting var9) {
                  var4.addProperty(var9.getName(), var9.getActiveMode());
               } else if (var6 instanceof ColorSetting var10) {
                  var4.addProperty(var10.getName(), var10.getArgb());
               }
            }

            var3.add("settings", var4);
            var0.add(var2.getName(), var3);
         }

         Files.writeString(FILE, GSON.toJson(var0), StandardCharsets.UTF_8);
      } catch (IOException var11) {
      }
   }

   private static void load(JsonObject var0) {
      for (Function var2 : FunctionManager.getFunctions()) {
         JsonElement var3 = var0.get(var2.getName());
         if (var3 != null && var3.isJsonObject()) {
            JsonObject var4 = var3.getAsJsonObject();
            JsonElement var5 = var4.get("keybind");
            if (var5 != null && var5.isJsonPrimitive()) {
               var2.setKeybind(var5.getAsInt());
            }

            JsonElement var9 = var4.get("settings");
            if (var9 != null && var9.isJsonObject()) {
               JsonObject var6 = var9.getAsJsonObject();

               for (Setting var8 : var2.getSettings()) {
                  JsonElement var7 = var6.get(var8.getName());
                  if (var7 == null || !var7.isJsonPrimitive()) {
                     continue;
                  }

                  if (var8 instanceof BooleanSetting var10) {
                     var10.applyValue(var7.getAsBoolean());
                  } else if (var8 instanceof NumberSetting var11) {
                     var11.setValue(var7.getAsDouble());
                  } else if (var8 instanceof ModeSetting var12) {
                     var12.setMode(var7.getAsString());
                  } else if (var8 instanceof ColorSetting var13) {
                     var13.setColor(new java.awt.Color(var7.getAsInt(), true));
                  }
               }
            }

            JsonElement var14 = var4.get("enabled");
            if (var14 != null && var14.getAsBoolean()) {
               var2.setEnabled(true);
            }
         }
      }
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::saveDefault, "cmode-config-shutdown"));
   }
}
