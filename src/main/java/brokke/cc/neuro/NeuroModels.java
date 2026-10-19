package brokke.cc.neuro;

import brokke.cc.rotation.SpaceTimeModel;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

/**
 * Откуда брать веса нейромодели.
 *
 * Порядок: сначала обученная в клиенте модель в run/brokke/neuro/spacetime.bin
 * (её пишет .neuro train), если её нет — встроенная из джарки
 * assets/brokke/rotation/spacetime.bin.
 */
public final class NeuroModels {
   public static final String RESOURCE = "assets/brokke/rotation/spacetime.bin";
   private static SpaceTimeModel bundled;
   private static boolean bundledTried;
   private static SpaceTimeModel active;
   private static boolean fromFile;
   private static int revision;

   private NeuroModels() {
   }

   public static Path dir() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      Path var1 = var0 != null && var0.runDirectory != null ? var0.runDirectory.toPath() : Path.of("run");
      return var1.resolve("brokke").resolve("neuro");
   }

   public static Path file() {
      return dir().resolve("spacetime.bin");
   }

   public static synchronized SpaceTimeModel get() {
      if (active == null) {
         reload();
      }

      return active;
   }

   public static synchronized int revision() {
      return revision;
   }

   public static synchronized boolean isTrained() {
      return fromFile;
   }

   public static synchronized void reload() {
      Path var0 = file();
      SpaceTimeModel var1 = Files.isRegularFile(var0) ? SpaceTimeModel.load(var0) : null;
      if (var1 != null) {
         active = var1;
         fromFile = true;
      } else {
         if (!bundledTried) {
            bundledTried = true;
            bundled = SpaceTimeModel.load(RESOURCE);
         }

         active = bundled;
         fromFile = false;
      }

      revision++;
   }

   public static synchronized String describe() {
      return fromFile ? "§aобученная: §f" + file() : "§7встроенная §8(обученную пишет .neuro train в " + file() + ")";
   }
}
