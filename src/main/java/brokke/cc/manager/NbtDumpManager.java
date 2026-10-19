package brokke.cc.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class NbtDumpManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("nbt_dumps.json");
   private static final List<NbtDumpManager.a> DUMPS = new ArrayList<>();
   private static boolean loaded = false;

   private static synchronized void b() {
      if (!loaded) {
         loaded = true;
         DUMPS.clear();

         try {
            if (Files.exists(FILE)) {
               List var0 = (List)GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), (new TypeToken<List<NbtDumpManager.a>>() {}).getType());
               if (var0 != null) {
                  DUMPS.addAll(var0);
               }
            }
         } catch (Exception var1) {
         }
      }
   }

   public static synchronized void a() {
      loaded = false;
      b();
   }

   public static String a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         b();
         if (DUMPS.isEmpty()) {
            return null;
         }

         String var1 = Registries.ITEM.getId(var0.getItem()).toString();
         String var2 = b(var0);
         String var3 = var0.getName().getString();
         String var4 = c(var0);

         for (NbtDumpManager.a var6 : DUMPS) {
            if (var6.itemId != null && var6.itemId.equalsIgnoreCase(var1) && var6.customDumpName != null && !var6.customDumpName.isEmpty()) {
               if (var6.customDataNbt != null && !var6.customDataNbt.isEmpty() && var2 != null && var2.toLowerCase().contains(var6.customDataNbt.toLowerCase())
                  )
                {
                  return var6.customDumpName;
               }

               if (var6.lore != null && !var6.lore.isEmpty() && var4 != null && var4.toLowerCase().contains(var6.lore.toLowerCase())) {
                  return var6.customDumpName;
               }

               if (var6.displayName != null && !var6.displayName.isEmpty() && var3.equalsIgnoreCase(var6.displayName)) {
                  return var6.customDumpName;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String b(ItemStack var0) {
      NbtComponent var1 = var0.get(DataComponentTypes.CUSTOM_DATA);
      return var1 != null ? var1.copyNbt().toString() : null;
   }

   private static String c(ItemStack var0) {
      LoreComponent var1 = var0.get(DataComponentTypes.LORE);
      if (var1 == null) {
         return null;
      }

      StringBuilder var2 = new StringBuilder();

      for (Text var4 : var1.lines()) {
         var2.append(var4.getString()).append(' ');
      }

      String var5 = var2.toString().trim();
      return var5.isEmpty() ? null : var5;
   }

   public static class a {
      public String itemId;
      public String customDumpName;
      public String customDataNbt;
      public String lore;
      public String displayName;
   }
}
