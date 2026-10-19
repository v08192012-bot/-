package brokke.cc.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

public class NicknameManager {
   private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
   private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("brokke_nicknames.txt");

   public static void a() {
      try {
         if (Files.exists(FILE)) {
            for (String var1 : Files.readAllLines(FILE)) {
               int var2 = var1.indexOf(61);
               if (var2 > 0) {
                  String var3 = var1.substring(0, var2).trim();
                  String var4 = var1.substring(var2 + 1).trim();
                  if (!var3.isEmpty() && !var4.isEmpty()) {
                     ALIASES.put(var3.toLowerCase(), var4);
                  }
               }
            }
         }
      } catch (IOException var5) {
      }
   }

   public static String a(String var0) {
      return var0 == null ? null : ALIASES.get(var0.toLowerCase());
   }

   public static void a(String var0, String var1) {
      if (var0 != null && !var0.isEmpty()) {
         if (var1 != null && !var1.isEmpty()) {
            ALIASES.put(var0.toLowerCase(), var1);
            c();
         } else {
            b(var0);
         }
      }
   }

   public static void b(String var0) {
      if (var0 != null && ALIASES.remove(var0.toLowerCase()) != null) {
         c();
      }
   }

   public static Map<String, String> b() {
      return Collections.unmodifiableMap(ALIASES);
   }

   private static void c() {
      try {
         StringBuilder var0 = new StringBuilder();

         for (Entry var2 : ALIASES.entrySet()) {
            var0.append((String)var2.getKey()).append('=').append((String)var2.getValue()).append('\n');
         }

         Files.writeString(FILE, var0.toString());
      } catch (IOException var3) {
      }
   }
}
