package brokke.cc.module.combat;

import brokke.cc.manager.FriendManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "AutoMace", description = "Берёт булаву в руку, когда под вами игрок", category = Category.COMBAT)
public class AutoMace extends Function {
   private final FloatSetting radius = new FloatSetting("Радиус", 4.0F, 1.0F, 8.0F, 0.5F);
   private final FloatSetting minHeight = new FloatSetting("Мин. высота над целью", 2.0F, 0.5F, 8.0F, 0.5F);
   private final FloatSetting maxDrop = new FloatSetting("Макс. высота над целью", 8.0F, 1.0F, 24.0F, 1.0F);
   private final BooleanSetting onlyFalling = new BooleanSetting("Только при падении", true);
   private final BooleanSetting onlyInAir = new BooleanSetting("Только в воздухе", true);
   private final BooleanSetting friends = new BooleanSetting("Учитывать друзей", false);
   private final BooleanSetting restore = new BooleanSetting("Возвращать предмет", true);
   private final BooleanSetting onlyTarget = new BooleanSetting("Только с целью (TriggerBot/AimAssist)", false);
   private int slotToRestore = -1;
   private int ourMaceSlot = -1;

   public AutoMace() {
      this.addSettings(
         this.radius,
         this.minHeight,
         this.maxDrop,
         this.onlyFalling,
         this.onlyInAir,
         this.friends,
         this.restore,
         this.onlyTarget
      );
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.o();
   }

   @Override
   public void onDisable() {
      this.o();
      super.onDisable();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player == null || mc.world == null) {
         this.o();
      } else if (!mc.player.isUsingItem()) {
         int var2 = mc.player.getInventory().getSelectedSlot();
         if (this.ourMaceSlot != -1 && var2 != this.ourMaceSlot) {
            this.slotToRestore = -1;
            this.ourMaceSlot = -1;
         }

         boolean var3 = this.d(this.ourMaceSlot != -1);
         if (var3) {
            if (this.ourMaceSlot == -1) {
               int var4 = this.g();
               if (var4 != -1 && var4 != var2) {
                  this.a(var4);
                  this.ourMaceSlot = var4;
                  this.slotToRestore = this.restore.isToggled() ? var2 : -1;
               }
            }
         } else if (this.ourMaceSlot != -1) {
            if (this.slotToRestore != -1) {
               this.a(this.slotToRestore);
            }

            this.slotToRestore = -1;
            this.ourMaceSlot = -1;
         }
      }
   }

   public boolean b(LivingEntity var1) {
      if (mc.player == null || var1 == null || !var1.isAlive() || var1.isRemoved()) {
         return false;
      } else if (mc.player.getMainHandStack().getItem() != Items.MACE) {
         return false;
      } else if (mc.player.isOnGround() || mc.player.isGliding() || mc.player.getVelocity().y >= -0.01) {
         return false;
      } else {
         return mc.player.fallDistance > 0.15F;
      }
   }

   public boolean a(LivingEntity var1, double var2) {
      if (!this.b(var1) || !this.c(var1)) {
         return false;
      } else if (mc.player.isUsingItem() || mc.player.getAttackCooldownProgress(0.5F) < 0.9F) {
         return false;
      } else if (this.a(mc.player.getEyePos(), var1.getBoundingBox()) > var2) {
         return false;
      } else {
         Vec3d var4 = this.f();
         if (var4 == null) {
            return false;
         } else {
            Vec3d var5 = var4.add(0.0, mc.player.getStandingEyeHeight(), 0.0);
            return this.a(var5, var1.getBoundingBox()) <= var2;
         }
      }
   }

   private boolean d(boolean var1) {
      if (this.onlyInAir.isToggled() && mc.player.isOnGround()) {
         return false;
      } else if (this.onlyTarget.isToggled()) {
         LivingEntity var2 = this.i();
         if (var2 == null || !this.c(var2)) {
            return false;
         } else {
            return this.b(var2, var1);
         }
      } else {
         if (!var1 && this.onlyFalling.isToggled() && mc.player.getVelocity().y >= 0.0) {
            return false;
         }

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            if (var6 != mc.player
               && var6.isAlive()
               && !var6.isSpectator()
               && !var6.isCreative()
               && (this.friends.isToggled() || !FriendManager.a(var6.getName().getString()))
               && this.b(var6, var1)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean b(LivingEntity var1, boolean var2) {
      double var3 = var2 ? 0.0 : this.minHeight.getValue();
      double var5 = Math.max(this.maxDrop.getValue(), this.minHeight.getValue());
      double var7 = mc.player.getY() - var1.getY();
      if (var7 <= 0.0 || var7 < var3 || var7 > var5) {
         return false;
      } else {
         double var9 = var1.getX() - mc.player.getX();
         double var11 = var1.getZ() - mc.player.getZ();
         double var13 = this.radius.getValue();
         return var9 * var9 + var11 * var11 <= var13 * var13;
      }
   }

   private boolean c(LivingEntity var1) {
      if (var1 == null || var1 == mc.player || !var1.isAlive() || var1.isSpectator()) {
         return false;
      } else {
         return !(var1 instanceof PlayerEntity var2)
            || this.friends.isToggled()
            || !FriendManager.a(var2.getName().getString());
      }
   }

   private LivingEntity i() {
      TriggerBot var1 = FunctionManager.a(TriggerBot.class);
      if (var1 != null && var1.isActive()) {
         LivingEntity var2 = var1.l();
         if (var2 != null) {
            return var2;
         }

         var2 = var1.k();
         if (var2 != null) {
            return var2;
         }
      }

      AimAssist var3 = FunctionManager.a(AimAssist.class);
      return var3 != null && var3.isActive() ? var3.k() : null;
   }

   private Vec3d f() {
      if (mc.player == null || mc.world == null || mc.player.isGliding()) {
         return null;
      } else {
         double var1 = Math.max(0.0, mc.player.getY() - mc.world.getBottomY());
         double var2 = Math.min(20.0, Math.max(1.0, Math.sqrt(var1 / 0.04)));
         Vec3d var3 = mc.player.getVelocity();
         Vec3d var4 = mc.player.getEntityPos().add(var3.x * var2, 0.0, var3.z * var2);
         Vec3d var5 = var4.add(0.0, -Math.max(4.0, this.maxDrop.getValue() + 3.0), 0.0);
         HitResult var6 = mc.world.raycast(new RaycastContext(var4, var5, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         return var6.getType() == Type.BLOCK ? var6.getPos() : null;
      }
   }

   private double a(Vec3d var1, Box var2) {
      double var3 = Math.max(var2.minX, Math.min(var1.x, var2.maxX));
      double var5 = Math.max(var2.minY, Math.min(var1.y, var2.maxY));
      double var7 = Math.max(var2.minZ, Math.min(var1.z, var2.maxZ));
      return var1.distanceTo(new Vec3d(var3, var5, var7));
   }

   private int g() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int var1 = 0; var1 < 9; var1++) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (var2.getItem() == Items.MACE) {
               return var1;
            }
         }

         return -1;
      }
   }

   private void a(int var1) {
      mc.player.getInventory().setSelectedSlot(var1);
      if (mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var1));
      }
   }

   private void o() {
      if (this.slotToRestore != -1 && mc.player != null && mc.player.getInventory().getSelectedSlot() == this.ourMaceSlot) {
         this.a(this.slotToRestore);
      }

      this.slotToRestore = -1;
      this.ourMaceSlot = -1;
   }
}
