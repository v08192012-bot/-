package shelka.aim.manager;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import shelka.aim.core.WarmupBotTicker;

public final class CommandManager {
   public static final String PREFIX = ".";

   private CommandManager() {
   }

   public static boolean handle(String message) {
      if (message == null || !message.startsWith(PREFIX)) {
         return false;
      }

      String[] args = message.substring(PREFIX.length()).trim().split("\\s+");
      if (args.length == 0 || args[0].isEmpty()) {
         return false;
      }

      String command = args[0].toLowerCase(Locale.ROOT);
      if (!command.equals("bot") && !command.equals("warmup") && !command.equals("aim")) {
         return false;
      }

      try {
         run(args);
      } catch (Exception error) {
         send("§cОшибка: §f" + error.getMessage());
      }

      return true;
   }

   private static void run(String[] args) {
      if (args.length < 2) {
         usage();
         return;
      }

      switch (args[1].toLowerCase(Locale.ROOT)) {
         case "spawn":
         case "add": {
            String name = args.length >= 3 ? args[2] : "Target";
            BotManager.spawn(name);
            break;
         }
         case "remove":
         case "del":
         case "delete":
            BotManager.removeNearest();
            break;
         case "clear":
            BotManager.clear();
            break;
         case "move":
         case "spar": {
            boolean on = args.length < 3 || isOn(args[2]);
            WarmupBotTicker.setMoving(on);
            BotManager.saveConfig();
            send(on ? "§aБот двигается §7(стрейфы, дистанция, прыжки)" : "§7Бот стоит на месте");
            break;
         }
         case "speed": {
            if (args.length < 3) {
               send("§7Скорость: §f" + fmt(WarmupBotTicker.getSpeed()));
               send("§cИспользование: §f.bot speed <0.05-5.0>");
               return;
            }

            double value;
            try {
               value = Double.parseDouble(args[2].replace(',', '.'));
            } catch (NumberFormatException error) {
               send("§cНужно число, например: §f.bot speed 1.5");
               return;
            }

            WarmupBotTicker.setSpeed(value);
            BotManager.saveConfig();
            send("§aСкорость бота: §f" + fmt(WarmupBotTicker.getSpeed()) + "§7x");
            break;
         }
         case "help":
            usage();
            break;
         default:
            send("§cНеизвестная подкоманда");
            usage();
      }
   }

   private static boolean isOn(String value) {
      String v = value.toLowerCase(Locale.ROOT);
      return v.equals("on") || v.equals("true") || v.equals("1") || v.equals("вкл");
   }

   private static String fmt(double value) {
      return String.format(Locale.ROOT, "%.2f", value);
   }

   private static void usage() {
      send("§b§lShelka Aim §7» §fразминка аима");
      send("§f.bot spawn [ник] §7— поставить бота");
      send("§f.bot remove §7— убрать ближайшего");
      send("§f.bot clear §7— убрать всех");
      send("§f.bot move [on|off] §7— движение");
      send("§f.bot speed <0.05-5.0> §7— скорость движения");
   }

   public static void send(String message) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         client.player.sendMessage(Text.literal(message), false);
      }
   }
}
