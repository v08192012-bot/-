package brokke.cc.core;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public class User {
   private static final User instance = new User();
   private final Map<String, String> cache = new HashMap<>();
   private boolean nativeFailed;

   public static User getInstance() {
      return instance;
   }

   private User() {
      this.cache.put("username", "SOURSE by loverqq");
      this.cache.put("role", "local");
      this.cache.put("uid", "1");
      this.cache.put("subTime", "не используется");
   }

   public String getProfileValue(String var1) {
      return this.cache.getOrDefault(var1, "");
   }

   public String getUsername() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null) {
         if (var1.player != null && var1.player.getName() != null && !var1.player.getName().getString().isBlank()) {
            return var1.player.getName().getString();
         }

         if (var1.getSession() != null && var1.getSession().getUsername() != null && !var1.getSession().getUsername().isBlank()) {
            return var1.getSession().getUsername();
         }
      }

      return this.getProfileValue("username");
   }

   public String getRole() {
      return this.getProfileValue("role");
   }

   public String getUid() {
      return this.getProfileValue("uid");
   }

   public String getSubscriptionTime() {
      return this.getProfileValue("subTime");
   }

   public boolean isNativeFailed() {
      return this.nativeFailed;
   }
}
