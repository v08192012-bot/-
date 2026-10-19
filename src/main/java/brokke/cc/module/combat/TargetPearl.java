package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "TargetPearl", description = "Автоматически кидает пёрл за выбранной целью", category = Category.COMBAT)
public class TargetPearl extends Function {
   private final Set<UUID> seenPearls = new HashSet<>();
   private final NumberSetting radius = new NumberSetting("Радиус", 30.0, 5.0, 100.0, 1.0);
   private final NumberSetting delay = new NumberSetting("Тики", 10.0, 1.0, 40.0, 1.0);
   private final BooleanSetting pauseAimAssist = new BooleanSetting("Пауза AimAssist", true);
   private int lastThrowTick = 0;
   private boolean aimAssistWasEnabled = false;
   private boolean aimBotWasEnabled = false;
   private int throwStage = 0;
   private int pearlSlot = -1;
   private int oldSlot = -1;
   private float targetYaw = 0.0F;
   private float targetPitch = 0.0F;

   public TargetPearl() {
      this.addSettings(this.radius, this.delay, this.pauseAimAssist);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.seenPearls.clear();
      this.lastThrowTick = 0;
      this.n();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.seenPearls.clear();
      this.lastThrowTick = 0;
      this.n();
      this.m();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         this.lastThrowTick++;
         if (this.throwStage > 0) {
            this.k();
         } else {
            for (Entity var3 : mc.world.getEntities()) {
               if (var3 instanceof EnderPearlEntity var4) {
                  UUID var5 = var4.getUuid();
                  int var8;
                  if (!this.seenPearls.contains(var5)
                     && var4.age <= 2
                     && var4.getOwner() instanceof PlayerEntity var7
                     && var7 != mc.player
                     && !(mc.player.distanceTo(var7) > this.radius.getValue())
                     && this.lastThrowTick >= (int)this.delay.getValue()
                     && (var8 = this.l()) != -1) {
                     Vec3d var9 = this.a(var4);
                     float[] var10 = this.a(var9);
                     this.pearlSlot = var8;
                     this.oldSlot = mc.player.getInventory().getSelectedSlot();
                     this.targetYaw = var10[0];
                     this.targetPitch = var10[1];
                     if (this.pauseAimAssist.isToggled()) {
                        this.b(true);
                     }

                     this.throwStage = 1;
                     this.lastThrowTick = 0;
                     this.seenPearls.add(var5);
                     break;
                  }
               }
            }
         }
      }
   }

   private void k() {
      switch (this.throwStage) {
         case 1:
            mc.player.getInventory().setSelectedSlot(this.pearlSlot);
            if (mc.player.networkHandler != null) {
               mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.pearlSlot));
            }

            mc.player.setYaw(this.targetYaw);
            mc.player.setPitch(this.targetPitch);
            this.throwStage = 2;
            break;
         case 2:
            ItemStack var1 = mc.player.getMainHandStack();
            if (var1.isOf(Items.ENDER_PEARL) && mc.interactionManager != null) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }

            this.throwStage = 3;
            break;
         case 3:
            mc.player.getInventory().setSelectedSlot(this.oldSlot);
            if (mc.player.networkHandler != null) {
               mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.oldSlot));
            }

            if (this.pauseAimAssist.isToggled()) {
               this.b(false);
            }

            this.n();
            break;
         default:
            this.n();
      }
   }

   private Vec3d a(EnderPearlEntity var1) {
      Vec3d var2 = new Vec3d(var1.getX(), var1.getY(), var1.getZ());
      Vec3d var3 = var1.getVelocity();

      for (int var4 = 0; var4 < 200; var4++) {
         Vec3d var5 = var2.add(var3);
         BlockHitResult var6 = mc.world.raycast(new RaycastContext(var2, var5, ShapeType.COLLIDER, FluidHandling.NONE, var1));
         if (var6.getType() != Type.MISS) {
            return var6.getPos();
         }

         var2 = var5;
         var3 = new Vec3d(var3.x * 0.99, var3.y * 0.99 - 0.03, var3.z * 0.99);
      }

      return var2;
   }

   private float[] a(Vec3d var1) {
      Vec3d var2 = mc.player.getEyePos();
      double var3 = var1.x - var2.x;
      double var5 = var1.z - var2.z;
      double var7 = var1.y - var2.y;
      double var9 = Math.sqrt(var3 * var3 + var5 * var5);
      float var11 = (float)(Math.toDegrees(Math.atan2(var5, var3)) - 90.0);
      float var12;
      float var13 = var12 = (float)(-Math.toDegrees(Math.atan2(var7, var9)));
      double var14 = Double.MAX_VALUE;

      for (float var16 = var12; var16 >= -85.0F && var16 >= var12 - 50.0F; var16 -= 0.5F) {
         Vec3d var17 = this.a(var2, var11, var16);
         double var18;
         if ((var18 = var17.distanceTo(var1)) < var14) {
            var14 = var18;
            var13 = var16;
         }
      }

      return new float[]{var11, var13};
   }

   private Vec3d a(Vec3d var1, float var2, float var3) {
      double var4 = 1.5;
      double var6 = -Math.sin(Math.toRadians(var2)) * Math.cos(Math.toRadians(var3)) * var4;
      double var8 = -Math.sin(Math.toRadians(var3)) * var4;
      double var10 = Math.cos(Math.toRadians(var2)) * Math.cos(Math.toRadians(var3)) * var4;
      Vec3d var12 = new Vec3d(var6, var8, var10);
      Vec3d var13 = var1;

      for (int var14 = 0; var14 < 200; var14++) {
         Vec3d var15 = var13.add(var12);
         BlockHitResult var16 = mc.world.raycast(new RaycastContext(var13, var15, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (var16.getType() != Type.MISS) {
            return var16.getPos();
         }

         var13 = var15;
         var12 = new Vec3d(var12.x * 0.99, var12.y * 0.99 - 0.03, var12.z * 0.99);
      }

      return var13;
   }

   private int l() {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.isOf(Items.ENDER_PEARL)) {
            return var1;
         }
      }

      return -1;
   }

   private void b(boolean var1) {
      AimBot var2 = FunctionManager.a(AimBot.class);
      AimAssist var3 = FunctionManager.a(AimAssist.class);
      if (var1) {
         if (var2 != null && var2.isActive()) {
            this.aimBotWasEnabled = true;
            var2.setEnabled(false);
         }

         if (var3 != null && var3.isActive()) {
            this.aimAssistWasEnabled = true;
            var3.setEnabled(false);
         }
      } else {
         if (this.aimBotWasEnabled && var2 != null && !var2.isActive()) {
            var2.setEnabled(true);
            this.aimBotWasEnabled = false;
         }

         if (this.aimAssistWasEnabled && var3 != null && !var3.isActive()) {
            var3.setEnabled(true);
            this.aimAssistWasEnabled = false;
         }
      }
   }

   private void m() {
      AimBot var1 = FunctionManager.a(AimBot.class);
      if (this.aimBotWasEnabled && var1 != null && !var1.isActive()) {
         var1.setEnabled(true);
         this.aimBotWasEnabled = false;
      }
   }

   private void n() {
      this.throwStage = 0;
      this.pearlSlot = -1;
      this.oldSlot = -1;
   }
}
