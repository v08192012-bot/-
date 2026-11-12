package brokke.cc.rotation.testnerv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Проверка переноса без Minecraft: грузит тот же файл модели, что лежит в jar, и прогоняет
 * встроенные эталоны Opus (признаки, логиты, пределы, дискриминатор). Запускается в CI.
 */
public final class TestNervSelfCheck {
   private TestNervSelfCheck() {
   }

   public static void main(String[] arguments) throws Exception {
      if (arguments.length == 0) {
         throw new IllegalArgumentException("укажи путь к bc_camera_testnerv.json(.gz)");
      }

      try (InputStream stream = open(Path.of(arguments[0]))) {
         long started = System.nanoTime();
         TestNervNet network = TestNervNet.load(stream);
         long elapsed = (System.nanoTime() - started) / 1_000_000L;
         System.out.println("OK: " + network.status);
         System.out.printf("загрузка и эталон: %d мс%n", elapsed);
         System.out.printf(
            "сеть: %d признаков, %d слоёв по %d, %d классов, окно %d, температура %.1f%n",
            network.featureCount,
            network.layers,
            network.hiddenSize,
            network.classCount,
            network.windowSize,
            network.temperature
         );
      }

      if (arguments.length > 1) {
         try (InputStream stream = open(Path.of(arguments[1]))) {
            TestNervKeyFix keyFix = TestNervKeyFix.load(stream);
            System.out.println("OK: " + keyFix.status);
         }
      }
   }

   private static InputStream open(Path path) throws IOException {
      InputStream stream = Files.newInputStream(path);
      return path.toString().endsWith(".gz") ? new GZIPInputStream(stream) : stream;
   }
}
