package brokke.cc.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

public class FriendManager {
   private static final Set<String> FRIENDS = ConcurrentHashMap.newKeySet();
   private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("brokke_friends.txt");

   public static void a() {
      try {
         if (Files.exists(FILE)) {
            Files.readAllLines(FILE).stream().map(String::trim).filter(var0 -> !var0.isEmpty()).forEach(FRIENDS::add);
         }
      } catch (IOException var1) {
      }
   }

   public static boolean a(String var0) {
      return var0 != null && !var0.isEmpty() ? FRIENDS.contains(var0.toLowerCase()) : false;
   }

   public static void b(String var0) {
      if (var0 != null && !var0.isEmpty() && FRIENDS.add(var0.toLowerCase())) {
         d();
      }
   }

   public static void c(String var0) {
      if (var0 != null && FRIENDS.remove(var0.toLowerCase())) {
         d();
      }
   }

   public static Set<String> b() {
      return Collections.unmodifiableSet(FRIENDS);
   }

   public static void c() {
      FRIENDS.clear();
      d();
   }

   private static void d() {
      try {
         Files.write(FILE, FRIENDS);
      } catch (IOException var1) {
      }
   }
}
