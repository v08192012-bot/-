package brokke.cc.rotation;

import brokke.cc.rotation.testnerv.TestNervCamera;
import net.minecraft.entity.LivingEntity;

/**
 * Режим TestNerv: обёртка над точным переносом камеры Opus.
 *
 * Сама камера работает на своём обработчике тика (как BcCamera в Opus): строит признаки,
 * считает шаг сети и сразу применяет его к прицелу. Этот класс нужен только для общей
 * инфраструктуры ауры (выбор режима, сброс состояния, статус в {@code .neuro status}).
 */
public class TestNervRotation extends RotationsSystem {
   private final TestNervCamera camera = TestNervCamera.INSTANCE;

   @Override
   public void a(LivingEntity target) {
      // Камера уже отработала в этом тике; здесь повторно ничего не считаем и не применяем.
   }

   /** Полный сброс: выключение ауры, смена режима, смерть игрока. */
   public void a() {
      this.camera.reset();
   }

   public String status() {
      return this.camera.status();
   }

   public boolean isReady() {
      return this.camera.isReady();
   }
}
