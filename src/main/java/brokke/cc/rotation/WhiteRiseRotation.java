package brokke.cc.rotation;

import brokke.cc.module.combat.Aura;
import brokke.cc.nav.BestPoint;
import brokke.cc.util.GCDUtil2;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class WhiteRiseRotation extends RotationsSystem {
   private final Aura aura;
   private LivingEntity trackedTarget;
   private float lastYaw;
   private float lastPitch;
   private float speedAcceleration;
   private boolean back;
   private boolean initialized;
   private float jitterOffset;
   private int tickCounter;

   public WhiteRiseRotation(Aura var1) {
      this.aura = var1;
   }

   public void a() {
      this.trackedTarget = null;
      this.speedAcceleration = 0.0F;
      this.back = false;
      this.jitterOffset = 0.0F;
      this.tickCounter = 0;
      this.initialized = mc.player != null;
      if (mc.player != null) {
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
      } else {
         this.lastYaw = 0.0F;
         this.lastPitch = 0.0F;
      }
   }

   public void b() {
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         if (mc.player.isBlocking()) {
            this.rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            this.lastYaw = this.rotate.x;
            this.lastPitch = this.rotate.y;
         } else {
            if (!this.initialized) {
               this.lastYaw = mc.player.getYaw();
               this.lastPitch = mc.player.getPitch();
               this.initialized = true;
            }

            if (this.trackedTarget != var1) {
               this.trackedTarget = var1;
               this.speedAcceleration = 0.0F;
               this.back = false;
               this.tickCounter = 0;
            }

            this.tickCounter++;
            this.jitterOffset = (float)((Math.sin(this.tickCounter * 0.17) * 0.12 + (Math.random() * 0.08 - 0.04)) * 0.7F);
            Vec3d var2 = BestPoint.a(var1, 128.0);
            Vec2f var3 = RotationUtils.b(var2);
            float var4 = var3.x;
            float var5 = var3.y;
            float var6 = Math.abs(MathHelper.wrapDegrees(var4 - this.lastYaw));
            boolean var7 = mc.player.getAttackCooldownProgress(1.0F) > 0.9F && this.aura.getTicksToAttack() <= 1;
            if (!this.back) {
               float var8 = 0.0055F;
               if (var6 > 60.0F) {
                  var8 += 0.028800001F;
               } else if (var6 > 30.0F) {
                  var8 += 0.014400001F;
               } else {
                  var8 += 0.0072000003F;
               }

               if (var7) {
                  var8 += 0.012857143F;
               }

               this.speedAcceleration = this.speedAcceleration + var8 * (1.6F + this.jitterOffset);
               if (this.speedAcceleration >= 0.22F) {
                  this.back = true;
               }
            } else {
               float var21 = var7 ? 0.045F : 0.008F;
               this.speedAcceleration = this.speedAcceleration - var21 * (2.1F + this.jitterOffset);
               if (this.speedAcceleration <= -0.04F) {
                  this.back = false;
               }
            }

            float var22 = MathHelper.clamp(this.speedAcceleration, 0.0F, mc.player.isGliding() ? 0.38F : 0.26F);
            if (var7) {
               var22 = Math.min(var22 + 0.1F, mc.player.isGliding() ? 0.46F : 0.34F);
            }

            float var23 = var22 + this.jitterOffset * 0.5F;
            if (this.tickCounter % 7 == 0) {
               var23 += 0.03F;
            }

            float var9 = MathHelper.wrapDegrees(var4 - this.lastYaw);
            float var10 = var5 - this.lastPitch;
            float var11 = mc.player.isGliding() ? 42.0F : (var7 ? 28.0F : 20.0F);
            float var12 = mc.player.isGliding() ? 12.0F : (var7 ? 4.5F : 2.8F);
            var9 = MathHelper.clamp(var9, -var11, var11);
            var10 = MathHelper.clamp(var10, -var12, var12);
            float var13 = var23 * 0.28F;
            float var14 = var23 * (0.85F + this.jitterOffset * 0.4F);
            float var15 = this.lastYaw + var9 * var14;
            float var16 = this.lastPitch + var10 * var13;
            float var17 = GCDUtil2.a();
            if (var17 > 0.0F) {
               var15 = this.lastYaw + Math.round((var15 - this.lastYaw) / var17) * var17;
               var16 = this.lastPitch + Math.round((var16 - this.lastPitch) / var17) * var17;
            }

            var16 = MathHelper.clamp(var16, -89.0F, 89.0F);
            Rotation2 var18 = new Rotation2(var15, var16);
            float var19 = mc.player.isGliding() && var1.isGliding() ? 360.0F : 45.0F;
            RotationStorage.a(var18, var19, var19, var19, var19, 0, 1, Aura.clientLook.isEnabled());
            this.rotate = new Vec2f(var18.getYaw(), var18.getPitch());
            this.lastYaw = var18.getYaw();
            this.lastPitch = var18.getPitch();
         }
      }
   }

   private Vec3d d(LivingEntity var1) {
      Vec3d var2 = BestPoint.b(var1);
      if (var2 == null) {
         var2 = var1.getBoundingBox().getCenter();
      }

      return this.b(var1) ? this.a(var1, var2) : var2;
   }
}
