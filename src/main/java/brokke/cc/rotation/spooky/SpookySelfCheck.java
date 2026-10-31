package brokke.cc.rotation.spooky;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Автономная проверка переноса: гоняет эталонные тики из motor.bin через forward-pass
 * и сравнивает с записанными ответами. Никакого Minecraft в зависимостях нет, поэтому
 * это же прогоняется в CI отдельной командой javac/java.
 *
 * <pre>
 *   java -cp out brokke.cc.rotation.spooky.SpookySelfCheck model/motor.bin
 * </pre>
 */
public final class SpookySelfCheck {
   private SpookySelfCheck() {
   }

   public static void main(String[] args) throws Exception {
      Path path = Paths.get(args.length > 0 ? args[0] : "spooky_motor.bin");
      try (InputStream in = Files.newInputStream(path)) {
         SpookyModel model = SpookyModel.load(in);
         System.out.printf(
            "модель: %d признаков, MLP %d, GRU %dx%d, смесь %d, latency %d%n",
            model.nFeatures,
            model.mlpHidden,
            model.gruLayers,
            model.gruHidden,
            model.mix,
            model.latency
         );
         System.out.printf(
            "огибающая: %s%n",
            model.hasEnvelope() ? model.envelopeCapsYaw.length + "/" + model.envelopeCapsPitch.length + " корзин" : "выключена"
         );

         if (model.goldenCount() == 0) {
            System.out.println("ОШИБКА: в модели нет эталонных тиков");
            System.exit(2);
         }

         double worst = model.selfCheck();
         System.out.printf("эталонных тиков: %d, максимальное расхождение: %.3e%n", model.goldenCount(), worst);
         if (!(worst <= 1.0E-9)) {
            System.out.println("ОШИБКА: forward-pass не совпал с эталоном");
            System.exit(1);
         }

         // быстрый прогон рантайма: цель перед игроком, ротация должна двигаться
         SpookyRotation rotation = new SpookyRotation(model, SpookyMath.pixelStep(0.5), new java.util.Random(1));
         AimRotation current = new AimRotation(0.0F, 0.0F);
         SpookyRotation.Target target = new SpookyRotation.Target(42, true, 3.0, 0.0, 3.0, 3.6, 1.8, 3.6);
         SpookyRotation.PlayerState player = new SpookyRotation.PlayerState(
            0.0, 1.62, 0.0, true, false, false, false, false, true, false, false, 1.0, 20
         );

         double total = 0.0;
         for (int tick = 0; tick < 40; tick++) {
            AimRotation next = rotation.tick(current, target, player, tick);
            total += next.distanceTo(current);
            current = next;
         }
         System.out.printf("рантайм: 40 тиков, суммарный поворот %.1f град, итог %s%n", total, current);
         if (!(total > 0.0)) {
            System.out.println("ОШИБКА: ротация не двигается");
            System.exit(1);
         }
         System.out.println("OK");
      }
   }
}
