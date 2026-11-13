package brokke.cc.module.movement;

import brokke.cc.manager.TaskScheduler;
import brokke.cc.mixins.interfaces.IClientPlayerInteractionManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Aura;
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
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

@NewFunction(name = "WindHop", description = "Кидает заряд ветра под себя и прыгает в тайминг", category = Category.MOVEMENT)
public class WindHop extends Function {
   private static final int PHASE_IDLE = 0;
   private static final int PHASE_AIM = 1;
   private static final int PHASE_RETURN = 2;
   /** Страховка наводки: даже если взгляд не дошёл вниз, бросаем через столько тиков. */
   private static final int MAX_AIM_TICKS = 10;
   /** Страховка возврата взгляда. */
   private static final int MAX_RETURN_TICKS = 10;
   /** Сколько тиков держим ввод прыжка: переживает задержку планировщика задач. */
   private static final int JUMP_INPUT_TICKS = 3;
   /** Приоритет выше ауры (1) и ReallyWorld (5): наводка вниз не должна перебиваться. */
   private static final int AIM_ROTATION_PRIORITY = 6;
   private static final int RETURN_ROTATION_PRIORITY = 6;
   private static final float AIM_TOLERANCE = 2.0F;

   private final BindSetting bind = new BindSetting("Бинд заряда ветра", -1);
   private final BooleanSetting autoMace = new BooleanSetting("Автоматически брать в руки булаву", true);
   private final BooleanSetting onlyGround = new BooleanSetting("Только на земле", true);
   private final FloatSetting aimAngle = new FloatSetting("Смотреть вниз, градусов", 89.0F, 75.0F, 89.5F, 0.5F);
   private final FloatSetting aimSpeed = new FloatSetting("Скорость наводки", 30.0F, 6.0F, 32.0F, 1.0F);
   private final FloatSetting returnSpeed = new FloatSetting("Скорость возврата", 26.0F, 6.0F, 32.0F, 1.0F);
   private final FloatSetting jumpDelay = new FloatSetting("Прыжок через, тиков", 0.0F, 0.0F, 4.0F, 1.0F);

   /** Связка выполняется: аура в это время не бьёт и не прыгает под крит. */
   private static boolean busy;

   private int phase = PHASE_IDLE;
   private int phaseTicks;
   private int settleTicks;
   private int previousSlot = -1;
   private int jumpTicks;
   private boolean jumpSent;
   /** Держим наводку за собой, чтобы авто-отпускание камеры не вмешалось в связку. */
   private boolean holdingAim;
   private float returnYaw;
   private float returnPitch;

   public WindHop() {
      this.addSettings(
         this.bind, this.autoMace, this.onlyGround, this.aimAngle, this.aimSpeed, this.returnSpeed, this.jumpDelay
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

   /** Бинд: старт связки в том же тике, без ожидания цели ауры. */
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

      // Планировщик задач на HIGHEST обнуляет ввод в этом же событии: пока он занят,
      // прыжок молча потеряется, и связка «кинул и не прыгнул» только портит тайминг.
      if (!TaskScheduler.e()) {
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
      // Наводка вниз уходит ТОЛЬКО пакетами: камера остаётся за мышью, руки не дёргаются,
      // а игрок продолжает свободно двигаться и смотреть куда хочет.
      this.phase = PHASE_AIM;
      this.phaseTicks = 0;
      this.settleTicks = 0;
      this.jumpTicks = 0;
      this.jumpSent = false;
      RotationStorage.holdAim();
      this.holdingAim = true;
      busy = true;
   }

   @EventHandler
   public void onTick(UpdateEvent event) {
      if (this.jumpTicks > 0) {
         this.jumpTicks--;
      }

      if (this.phase == PHASE_IDLE) {
         return;
      }

      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.abort();
         return;
      }

      this.phaseTicks++;
      switch (this.phase) {
         case PHASE_AIM:
            this.tickAim();
            break;
         case PHASE_RETURN:
            this.tickReturn();
            break;
         default:
            this.abort();
      }
   }

   /** Быстрый, но плавный доворот вниз: 3-4 тика — и заряд уже летит. */
   private void tickAim() {
      this.applyRotation(this.returnYaw, this.aimAngle.getFloatValue(), this.aimSpeed.getFloatValue(), AIM_ROTATION_PRIORITY);
      boolean aimed = mc.player.getPitch() >= this.aimAngle.getFloatValue() - AIM_TOLERANCE;
      this.settleTicks = aimed ? this.settleTicks + 1 : 0;
      // Бросаем на СЛЕДУЮЩЕМ тике после готовности прицела: пакет поворота уже ушёл,
      // сервер видит ту же ротацию, что и мы, и бросок не выглядит «телепортом прицела».
      if (this.settleTicks >= 2 || this.phaseTicks >= MAX_AIM_TICKS) {
         // Кривой бросок хуже пропущенного: заряд улетит вперёд и толкнёт игрока назад.
         if (mc.player.getPitch() < 70.0F) {
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

      // Пакет отправляем сами, с явной ротацией: так бросок не зависит от того,
      // перехватил ли interactItem другой модуль (AutoCrystal), и сервер получает
      // ровно тот взгляд вниз, который мы показали пакетом поворота.
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      ((IClientPlayerInteractionManager)mc.interactionManager)
         .invokeSendSequencedPacket(mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch));
      mc.player.swingHand(Hand.MAIN_HAND);

      this.phase = PHASE_RETURN;
      this.phaseTicks = 0;
      this.restoreOrMace();
      // Прыжок сразу по бинду: взрыв заряда приходит на 1-2 тика позже, и если
      // прыгнуть после него, игрока уже отрывает от земли и прыжок пропадает.
      this.jump();
   }

   /** Взгляд возвращаем на прежний уровень: камера при этом не двигается. */
   private void tickReturn() {
      this.applyRotation(this.returnYaw, this.returnPitch, this.returnSpeed.getFloatValue(), RETURN_ROTATION_PRIORITY);
      boolean returned = Math.abs(mc.player.getPitch() - this.returnPitch) <= 1.5F;
      if (returned || this.phaseTicks >= MAX_RETURN_TICKS) {
         this.finish();
      }
   }

   private void jump() {
      if (this.jumpSent) {
         return;
      }

      this.jumpSent = true;
      this.jumpTicks = JUMP_INPUT_TICKS + (int)this.jumpDelay.getFloatValue();
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onMoveInput(EventMoveInput event) {
      if (this.jumpTicks > 0) {
         event.setJump(true);
      }
   }

   private void applyRotation(float yaw, float pitch, float speed, int priority) {
      // false: наводка идёт пакетами, камера не трогается.
      RotationStorage.a(new Rotation2(yaw, pitch), speed, speed, speed, speed, 2, priority, false);
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

   /** Отмена до броска: заряд остался в руке — возвращаем то, что было. */
   private void cancel() {
      this.restorePreviousSlot();
      if (mc.player != null && this.phase != PHASE_IDLE && Math.abs(mc.player.getPitch() - this.returnPitch) > 3.0F) {
         // Прицел уже ушёл вниз — не оставляем игрока смотреть в пол, а плавно возвращаем.
         this.phase = PHASE_RETURN;
         this.phaseTicks = 0;
      } else {
         this.finish();
      }
   }

   /** Модуль выключили или мир пропал: вернуть предмет и снять свою наводку. */
   private void abort() {
      this.restorePreviousSlot();
      this.finish();
   }

   private void restorePreviousSlot() {
      if (mc.player != null
         && this.previousSlot >= 0
         && this.previousSlot <= 8
         && mc.player.getInventory().getSelectedSlot() != this.previousSlot) {
         this.selectSlot(this.previousSlot);
      }

      this.previousSlot = -1;
   }

   private void finish() {
      if (this.phase != PHASE_IDLE) {
         Aura aura = Aura.INSTANCE;
         if (aura != null && aura.l() && aura.getTarget() != null) {
            // Аура ведёт бой — просто уступаем ей наводку, цель и камеру не трогаем.
            RotationStorage.a().resetPriority();
         } else {
            // Боя нет: отпускаем «тихую» наводку, камера плавно возвращается к игроку.
            RotationStorage.releaseAim();
         }
      }

      if (this.holdingAim) {
         this.holdingAim = false;
         RotationStorage.releaseHold();
      }

      this.previousSlot = -1;
      this.jumpSent = false;
      this.jumpTicks = 0;
      this.phase = PHASE_IDLE;
      this.phaseTicks = 0;
      this.settleTicks = 0;
      busy = false;
   }
}
