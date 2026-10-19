package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.ElytraChestplateSwapper;
import brokke.cc.manager.TimerController;
import brokke.cc.rotation.HolyWorldElytraPredictor;
import brokke.cc.rotation.RotationTask;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "Speed", description = "Elytra speed", category = Category.MOVEMENT)
public class Speed extends Function {
   private static final String ROTATION_TASK = "speed_elytra";
   private static final int ROTATION_PRIORITY = 35;
   public final ModeSetting mode = new ModeSetting("Mode", "HolyWorld", "ReallyWorld", "GrimElytra", "SpookyElytra");
   public final BooleanSetting swapElytra = new BooleanSetting("Свап элитры", true);
   private final HolyWorldElytraPredictor holyPredictor = new HolyWorldElytraPredictor();
   private int rwTicks;
   private int rwCollisionTicks;
   private boolean rwTimerActive;
   private int grimTicks;

   public Speed() {
      this.addSettings(this.mode, this.swapElytra);
   }

   @Override
   public void onEnable() {
      this.holyPredictor.e();
      this.rwTicks = 0;
      this.rwCollisionTicks = 0;
      this.rwTimerActive = false;
      this.grimTicks = 0;
      TimerController.b();
      super.onEnable();
      if (this.swapElytra.isToggled() && mc.player != null) {
         ElytraChestplateSwapper.b(mc);
      }
   }

   @Override
   public void onDisable() {
      TimerController.b();
      this.t();
      this.holyPredictor.e();
      if (this.swapElytra.isToggled() && mc.player != null) {
         ElytraChestplateSwapper.c(mc);
      }

      this.rwTicks = 0;
      this.rwCollisionTicks = 0;
      this.rwTimerActive = false;
      this.grimTicks = 0;
      super.onDisable();
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (mc.player != null) {
         if (this.mode.isSelected("HolyWorld")) {
            if (this.holyPredictor.isActive()) {
               if (this.holyPredictor.isJumpRequested()) {
                  var1.setJump(true);
               }

               if (this.holyPredictor.isSprintRequested()) {
                  var1.setSprint(true);
               }
            }
         } else if (this.mode.isSelected("ReallyWorld")) {
            if (mc.player.verticalCollision) {
               this.rwCollisionTicks++;
            } else {
               this.rwCollisionTicks = 0;
            }

            if (this.rwCollisionTicks >= 1) {
               var1.setJump(true);
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (this.mode.isSelected("HolyWorld")) {
            this.k();
         } else if (this.mode.isSelected("ReallyWorld")) {
            this.l();
         } else if (this.mode.isSelected("SpookyElytra")) {
            this.m();
         } else if (this.mode.isSelected("GrimElytra")) {
            this.n();
         }
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (this.mode.isSelected("ReallyWorld") && var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
         if (this.rwTicks % 2 == 1) {
            this.rwTicks++;
         }

         TimerController.a(1.0F);
      }
   }

   private void k() {
      this.holyPredictor.a(mc);
      if (this.holyPredictor.isActive() && (mc.player.isGliding() || !mc.player.isOnGround())) {
         RotationTask.a("speed_elytra", 35);
         RotationTask.a(mc.player.getYaw(), this.holyPredictor.getTargetPitch(), 360.0F, 360.0F, 360.0F, 360.0F, 0.0, 35);
      } else {
         this.t();
      }
   }

   private void l() {
      TimerController.a(1.7F);
      this.rwTimerActive = true;
      if (this.rwTicks > 3) {
         double var1 = 0.03;
         if (this.rwTicks % 2 == 0) {
            mc.player.addVelocity(0.0, 0.03, 0.0);
            var1 = mc.player.isOnGround() ? 0.085 : 0.03;
         }

         Vec3d var3 = this.q();
         mc.player.addVelocity(var3.x * var1, 0.0, var3.z * var1);
      }

      if (this.rwTicks % 2 == 0 && mc.player.networkHandler != null) {
         TimerController.a(0.3F);
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
      }

      this.rwTicks++;
   }

   private void m() {
      if (mc.player.fallDistance > 1.0 && !mc.player.isGliding() && mc.interactionManager != null && mc.player.networkHandler != null) {
         Slot var1 = this.p();
         if (var1 != null) {
            int var2 = mc.player.getInventory().getSelectedSlot();
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var1.id >= 36 && var1.id <= 44 ? var1.id - 36 : var2));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (mc.player.isSprinting() && this.r() && mc.player.checkGliding()) {
               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            }

            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var2));
         }
      }
   }

   private void n() {
      if (this.r()) {
         mc.player.setSprinting(true);
      }

      if (!mc.player.isOnGround() && !mc.player.isGliding() && this.o()) {
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         }

         mc.player.startGliding();
      }

      if (mc.player.isOnGround()) {
         this.grimTicks = 16;
      }

      if (this.r() && !mc.player.isOnGround() && this.o() && this.grimTicks > 0) {
         float var1 = this.s();
         RotationTask.a("speed_elytra", 35);
         RotationTask.a(var1, 75.0F, 360.0F, 360.0F, 360.0F, 360.0F, 0.0, 35);
      } else {
         this.t();
      }
   }

   private boolean o() {
      return mc.player != null && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   private Slot p() {
      return mc.player != null && mc.player.currentScreenHandler != null
         ? mc.player.currentScreenHandler.slots.stream().filter(var0 -> var0.getStack().isOf(Items.ELYTRA)).findFirst().orElse(null)
         : null;
   }

   private Vec3d q() {
      if (mc.player != null && mc.player.input != null) {
         float var1 = mc.player.input.getMovementInput().y;
         float var2 = mc.player.input.getMovementInput().x;
         if (var1 == 0.0F && var2 == 0.0F) {
            return Vec3d.ZERO;
         }

         float var3 = mc.player.getYaw();
         if (var1 != 0.0F) {
            if (var2 > 0.0F) {
               var3 += var1 > 0.0F ? -45.0F : 45.0F;
            } else if (var2 < 0.0F) {
               var3 += var1 > 0.0F ? 45.0F : -45.0F;
            }

            if (var1 < 0.0F) {
               var3 += 180.0F;
            }
         } else if (var2 != 0.0F) {
            var3 += var2 > 0.0F ? -90.0F : 90.0F;
         }

         double var4 = Math.toRadians(var3);
         return new Vec3d(-Math.sin(var4), 0.0, Math.cos(var4));
      } else {
         return Vec3d.ZERO;
      }
   }

   private boolean r() {
      return mc.player != null && mc.player.input != null && (mc.player.input.getMovementInput().y != 0.0F || mc.player.input.getMovementInput().x != 0.0F);
   }

   private float s() {
      float var1 = mc.player.input.getMovementInput().y;
      float var2 = mc.player.input.getMovementInput().x;
      float var3 = mc.player.getYaw();
      if (var1 < 0.0F) {
         var3 += 180.0F;
      }

      float var4 = var1 < 0.0F ? -0.5F : (var1 > 0.0F ? 0.5F : 1.0F);
      if (var2 > 0.0F) {
         var3 -= 90.0F * var4;
      } else if (var2 < 0.0F) {
         var3 += 90.0F * var4;
      }

      return var3;
   }

   private void t() {
      if (RotationTask.rotationPriority <= 35) {
         RotationTask.b("speed_elytra");
         RotationTask.rotationState = RotationTask.a.RESET;
         RotationTask.rotationPriority = 0;
      }
   }
}
