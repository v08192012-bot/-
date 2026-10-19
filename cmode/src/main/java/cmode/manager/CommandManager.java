package cmode.manager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class CommandManager {
   public static final String PREFIX = ".";
   private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

   static {
      DESCRIPTIONS.put("help", "список команд");
      DESCRIPTIONS.put("friend", "управление друзьями");
   }

   private CommandManager() {
   }

   public static boolean handle(String var0) {
      if (var0 == null || !var0.startsWith(PREFIX)) {
         return false;
      }

      String[] var1 = var0.substring(PREFIX.length()).trim().split("\\s+");
      if (var1.length == 0 || var1[0].isEmpty()) {
         return false;
      }

      try {
         switch (var1[0].toLowerCase(Locale.ROOT)) {
            case "help":
               message("§b§lcmode §7» §fСписок команд:");
               message("§7.friend add|remove|list|clear [ник]");
               break;
            case "friend":
               friend(var1);
               break;
            default:
               message("§cНеизвестная команда. §f.help§c — список команд");
         }
      } catch (Exception var2) {
         message("§cОшибка: §f" + var2.getMessage());
      }

      return true;
   }

   private static void friend(String[] var0) {
      if (var0.length < 2) {
         message("§cИспользование: §f.friend add|remove|list|clear [ник]");
         return;
      }

      switch (var0[1].toLowerCase(Locale.ROOT)) {
         case "add":
            if (var0.length < 3) {
               message("§cИспользование: §f.friend add <ник>");
               return;
            }

            FriendManager.b(var0[2]);
            message("§aДруг добавлен: §f" + var0[2]);
            break;
         case "remove":
         case "del":
         case "delete":
            if (var0.length < 3) {
               message("§cИспользование: §f.friend remove <ник>");
               return;
            }

            FriendManager.c(var0[2]);
            message("§aДруг удалён: §f" + var0[2]);
            break;
         case "clear":
            FriendManager.c();
            message("§aСписок друзей очищен");
            break;
         case "list":
            List<String> var2 = new ArrayList<>(FriendManager.b());
            message(var2.isEmpty() ? "§7Список друзей пуст" : "§bДрузья (" + var2.size() + "): §f" + String.join(", ", var2));
            break;
         default:
            message("§cИспользование: §f.friend add|remove|list|clear [ник]");
      }
   }

   public static void message(String var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      var1.execute(() -> {
         if (var1.player != null) {
            var1.player.sendMessage(Text.literal(var0), false);
         }
      });
   }
}
