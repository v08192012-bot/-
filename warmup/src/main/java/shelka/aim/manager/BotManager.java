package shelka.aim.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import shelka.aim.core.WarmupBotEntity;
import shelka.aim.core.WarmupBotRegistry;
import shelka.aim.core.WarmupBotTicker;

public final class BotManager {
   private static final List<WarmupBotEntity> BOTS = new ArrayList<>();
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("shelka-aim.properties");

   private BotManager() {
   }

   public static List<WarmupBotEntity> getBots() {
      return BOTS;
   }

   public static void spawn(String name) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null || client.player == null) {
         return;
      }

      Vec3d pos = client.player.getEntityPos().add(client.player.getRotationVector().multiply(3.0));
      WarmupBotEntity bot = new WarmupBotEntity(WarmupBotRegistry.BOT, client.world);
      bot.setPosition(pos);
      bot.setCustomName(Text.literal(name));
      bot.setCustomNameVisible(true);
      bot.setSkinName(name);
      bot.setHealth(20.0F);
      bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
      bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
      bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
      bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
      bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
      bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
      client.world.addEntity(bot);
      BOTS.add(bot);

      if (WarmupBotTicker.isMoving()) {
         bot.setNoGravity(true);
         bot.baseY = bot.getY();
      }

      send("§aБот §f" + name + " §aпоставлен §7(всего: " + BOTS.size() + ")");
   }

   public static void removeNearest() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null || client.player == null) {
         return;
      }

      WarmupBotEntity nearest = null;
      double best = Double.MAX_VALUE;
      for (WarmupBotEntity bot : BOTS) {
         if (bot != null && bot.isAlive()) {
            double dist = client.player.squaredDistanceTo(bot);
            if (dist < best) {
               best = dist;
               nearest = bot;
            }
         }
      }

      if (nearest == null) {
         send("§cБотов рядом нет");
         return;
      }

      nearest.discard();
      BOTS.remove(nearest);
      send("§cБот убран §7(осталось: " + BOTS.size() + ")");
   }

   public static void clear() {
      int count = 0;
      for (WarmupBotEntity bot : new ArrayList<>(BOTS)) {
         if (bot != null && bot.isAlive()) {
            bot.discard();
            count++;
         }
      }

      BOTS.clear();
      send(count == 0 ? "§7Ботов и так не было" : "§cУбрано ботов: §f" + count);
   }

   public static void prune() {
      BOTS.removeIf(bot -> bot == null || !bot.isAlive());
   }

   public static void send(String message) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         client.player.sendMessage(Text.literal(message), false);
      }
   }

   public static void loadConfig() {
      Properties props = new Properties();
      if (Files.exists(CONFIG_PATH)) {
         try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
         } catch (IOException ignored) {
         }
      }

      try {
         WarmupBotTicker.setSpeed(Double.parseDouble(props.getProperty("speed", "1.0")));
      } catch (NumberFormatException ignored) {
      }

      if ("true".equalsIgnoreCase(props.getProperty("moving", "true"))) {
         WarmupBotTicker.setMoving(true);
      }
   }

   public static void saveConfig() {
      Properties props = new Properties();
      props.setProperty("speed", Double.toString(WarmupBotTicker.getSpeed()));
      props.setProperty("moving", Boolean.toString(WarmupBotTicker.isMoving()));

      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Shelka Aim warmup bot");
         }
      } catch (IOException ignored) {
      }
   }
}
