package brokke.cc.module.movement;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.NewFunction;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

@NewFunction(name = "WindHop", description = "Кидает заряд ветра под себя и прыгает в тайминг", category = Category.MOVEMENT)
public class WindHop extends Function {
   private static final int PHASE_IDLE = 0;
   private static final int PHASE_AIM = 1;
   private static final int PHASE_THROW = 2;
   private static final int PHASE_RETURN = 3;
   /** Сколько тиков максимум доворачиваем взгляд вниз. */
   private static final int MAX_AIM_TICKS = 24;
   /** Страховка: вся связка не должна длиться дольше этого. */
   private static final int MAX_TOTAL_TICKS = 60;
   /** Приоритет выше ауры (1) и ReallyWorld (5): наводка вниз не должна перебиваться. */
   private static final int AIM_ROTATION_PRIORITY = 6;
   private static final int RETURN_ROTATION_PRIORITY = 0;

   private final BindSetting bind = new BindSetting("Бинд заряда ветра", -1);
   private final BooleanSetting autoMace = new BooleanSetting("Автоматически брать в руки булаву", true);
   private final BooleanSetting firstPerson = new BooleanSetting("Наводка от первого лица", true);
   private final BooleanSetting onlyGround = new BooleanSetting("Только на земле", true);
   private final FloatSetting aimAngle = new FloatSetting("Смотреть вниз, градусов", 88.0F, 60.0F, 89.5F, 0.5F);
   private final FloatSetting aimSpeed = new FloatSetting("Скорость наводки", 20.0F, 6.0F, 32.0F, 1.0F);
   private final FloatSetting returnSpeed = new FloatSetting("Скорость возврата", 14.0F, 6.0F, 32.0F, 1.0F);
   private final FloatSetting jumpDelay = new FloatSetting("Прыжок через, тиков", 0.0F, 0.0F, 4.0F, 1.0F);

   /** Связка выполняется: аура в это время не бьёт и не прыгает под крит. */
   private static boolean busy;

   private int phase = PHASE_IDLE;
   private int phaseTicks;
   private int totalTicks;
   private int throwTries;
   private int settleTicks;
   private int previousSlot = -1;
   private int jumpTicks;
   private boolean jumpSent;
   private boolean jumpPressedByUs;
   private boolean visibleRotation;
   private float returnYaw;
   private float returnPitch;

   public WindHop() {
      this.addSettings(
         this.bind, this.autoMace, this.firstPerson, this.onlyGround, this.aimAngle, this.aimSpeed, this.returnSpeed, this.jumpDelay
      );
   }

   public static boolean isBusy() {
      return busy;
   }

   @Override
   public void onDisable() {
      this.abort();
      super.onDisable();
   }

   /** Бинд: старт связки. */
   @EventHandler
   public void onKey(KeyEvent event) {
      if (event.getAction() != 1 || mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      if (busy || !this.bind.matchesKey(event.getKey())) {
         return;
      }

      this.start();
   }

   private void start() {
      if (mc.player.isUsingItem() || mc.player.isSpectator() || mc.player.isDead()) {
         return;
      }

      if (mc.player.hasVehicle() || mc.player.isGliding() || mc.player.getAbilities().flying || mc.player.isClimbing()) {
         return;
      }

      if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
         return;
      }

      if (this.onlyGround.isEnabled() && !mc.player.isOnGround()) {
         return;
      }

      int chargeSlot = this.findHotbar(Items.WIND_CHARGE);
      if (chargeSlot == -1) {
         return;
      }

      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      if (chargeSlot != this.previousSlot) {
         this.selectSlot(chargeSlot);
      }

      this.returnYaw = mc.player.getYaw();
      this.returnPitch = mc.player.getPitch();
      // Камеру ведём сами только если её не забрала аура («тихая» наводка) или FreeLook.
      this.visibleRotation = this.firstPerson.isEnabled() && !FreeLookStorage.isActive();
      this.phase = PHASE_AIM;
      this.phaseTicks = 0;
      this.totalTicks = 0;
      this.throwTries = 0;
      this.settleTicks = 0;
      this.jumpTicks = 0;
      this.jumpSent = false;
      this.jumpPressedByUs = false;
      busy = true;
   }

   @EventHandler
   public void onTick(UpdateEvent event) {
      this.releaseJumpKey();

      if (this.phase == PHASE_IDLE) {
         return;
      }

      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.abort();
         return;
      }

      if (++this.totalTicks > MAX_TOTAL_TICKS) {
         this.cancel();
         return;
      }

      this.phaseTicks++;
      switch (this.phase) {
         case PHASE_AIM:
            this.tickAim();
            break;
         case PHASE_THROW:
            this.tickThrow();
            break;
         case PHASE_RETURN:
            this.tickReturn();
            break;
         default:
            this.cancel();
      }
   }

   /** Плавно доворачиваем взгляд вниз — сервер видит обычный человеческий поворот. */
   private void tickAim() {
      this.applyRotation(this.returnYaw, this.aimAngle.getFloatValue(), this.aimSpeed.getFloatValue(), AIM_ROTATION_PRIORITY);
      boolean aimed = Math.abs(mc.player.getPitch() - this.aimAngle.getFloatValue()) <= 1.0F;
      // Бросок на втором подряд тике с готовым прицелом: движение уже ушло пакетом,
      // и пакет использования видит у сервера ту же ротацию, что и мы, а не прошлую.
      this.settleTicks = aimed ? this.settleTicks + 1 : 0;
      if (this.settleTicks >= 2) {
         this.throwCharge();
      } else if (this.phaseTicks >= MAX_AIM_TICKS) {
         if (mc.player.getPitch() < 45.0F) {
            this.cancel();
         } else {
            this.throwCharge();
         }
      }
   }

   private void throwCharge() {
      if (mc.player.getMainHandStack().getItem() != Items.WIND_CHARGE) {
         int chargeSlot = this.findHotbar(Items.WIND_CHARGE);
         if (chargeSlot == -1) {
            this.cancel();
            return;
         }

         this.selectSlot(chargeSlot);
         if (mc.player.getMainHandStack().getItem() != Items.WIND_CHARGE) {
            this.cancel();
            return;
         }
      }

      ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      if (!result.isAccepted() && ++this.throwTries > 3) {
         this.cancel();
         return;
      }

      if (!result.isAccepted()) {
         return;
      }

      // Наводка вниз больше не нужна: снимаем свой высокий приоритет, чтобы возврат
      // взгляда и аура снова могли вести камеру.
      RotationStorage.a().resetPriority();
      this.phase = PHASE_THROW;
      this.phaseTicks = 0;
      // Нулевая задержка — прыгаем сразу: если серверный взрыв придёт раньше прыжка,
      // он оторвёт нас от земли и прыгнуть будет уже нельзя.
      if (this.jumpDelay.getFloatValue() <= 0.0F) {
         this.jump();
      }
   }

   /** Сразу после броска возвращаем предмет из бинда (или берём булаву) и ждём прыжок. */
   private void tickThrow() {
      if (this.phaseTicks == 1) {
         this.restoreOrMace();
      }

      this.applyRotation(this.returnYaw, this.returnPitch, this.returnSpeed.getFloatValue(), RETURN_ROTATION_PRIORITY);
      if (this.phaseTicks >= this.jumpDelay.getFloatValue() + 1.0F) {
         this.jump();
      }

      if (this.jumpSent) {
         this.phase = PHASE_RETURN;
         this.phaseTicks = 0;
      }
   }

   private void jump() {
      if (this.jumpSent) {
         return;
      }

      this.jumpSent = true;
      if (mc.player.isOnGround()) {
         // Клавишу жмём сами: обработчик ввода ниже продублирует прыжок в событии,
         // чтобы его не съел планировщик задач, обнуляющий ввод на HIGHEST.
         this.jumpTicks = 2;
         if (mc.options != null && !mc.options.jumpKey.isPressed()) {
            mc.options.jumpKey.setPressed(true);
            this.jumpPressedByUs = true;
         }
      }
   }

   /** Возвращаем прицел на прежний взгляд; аура, если у неё есть цель, забирает наводку себе. */
   private void tickReturn() {
      if (RotationStorage.a().getCurrentPriority() > RETURN_ROTATION_PRIORITY) {
         this.finish();
         return;
      }

      this.applyRotation(this.returnYaw, this.returnPitch, this.returnSpeed.getFloatValue(), RETURN_ROTATION_PRIORITY);
      boolean returned = Math.abs(mc.player.getPitch() - this.returnPitch) <= 1.5F
         && Math.abs(mc.player.getYaw() - this.returnYaw) <= 2.0F;
      if (returned || this.phaseTicks > 12) {
         RotationStorage.a().j();
         this.finish();
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onMoveInput(EventMoveInput event) {
      if (this.jumpTicks > 0) {
         event.setJump(true);
      }
   }

   private void applyRotation(float yaw, float pitch, float speed, int priority) {
      RotationStorage.a(new Rotation2(yaw, pitch), speed, speed, speed, speed, 2, priority, this.visibleRotation);
   }

   private void restoreOrMace() {
      if (mc.player == null) {
         return;
      }

      int target = this.previousSlot;
      if (this.autoMace.isEnabled()) {
         int maceSlot = this.findHotbar(Items.MACE);
         if (maceSlot != -1) {
            target = maceSlot;
         }
      }

      if (target >= 0 && target <= 8 && target != mc.player.getInventory().getSelectedSlot()) {
         this.selectSlot(target);
      }
   }

   private int findHotbar(Item item) {
      if (mc.player == null) {
         return -1;
      }

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return slot;
         }
      }

      return -1;
   }

   private void selectSlot(int slot) {
      if (mc.player == null) {
         return;
      }

      mc.player.getInventory().setSelectedSlot(slot);
      if (mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private void releaseJumpKey() {
      if (this.jumpTicks > 0 && --this.jumpTicks == 0 && this.jumpPressedByUs) {
         if (mc.options != null) {
            mc.options.jumpKey.setPressed(false);
         }

         this.jumpPressedByUs = false;
      }
   }

   /** Отмена до броска: заряд остался в руке — возвращаем то, что было. */
   private void cancel() {
      if (mc.player != null
         && this.previousSlot >= 0
         && this.previousSlot <= 8
         && mc.player.getInventory().getSelectedSlot() != this.previousSlot) {
         this.selectSlot(this.previousSlot);
      }

      this.previousSlot = -1;
      if (mc.player != null && this.phase != PHASE_IDLE && Math.abs(mc.player.getPitch() - this.returnPitch) > 3.0F) {
         // Прицел уже ушёл вниз — не оставляем игрока смотреть в пол, а плавно возвращаем.
         this.phase = PHASE_RETURN;
         this.phaseTicks = 0;
         this.totalTicks = 0;
      } else {
         this.finish();
      }
   }

   /** Модуль выключили или мир пропал: вернуть предмет и снять свой приоритет ротации. */
   private void abort() {
      if (mc.player != null
         && this.previousSlot >= 0
         && this.previousSlot <= 8
         && mc.player.getInventory().getSelectedSlot() != this.previousSlot) {
         this.selectSlot(this.previousSlot);
      }

      this.previousSlot = -1;
      this.finish();
   }

   private void finish() {
      if (this.phase != PHASE_IDLE) {
         // Сбрасываем приоритет: иначе наш высокий приоритет остался бы висеть
         // и блокировал наводку ауры. Цель и камеру не трогаем.
         RotationStorage.a().resetPriority();
      }

      if (mc.options != null && this.jumpPressedByUs) {
         mc.options.jumpKey.setPressed(false);
      }

      this.jumpPressedByUs = false;
      this.jumpSent = false;
      this.jumpTicks = 0;
      this.phase = PHASE_IDLE;
      this.phaseTicks = 0;
      this.totalTicks = 0;
      this.throwTries = 0;
      this.previousSlot = -1;
      busy = false;
   }
}
