package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.combat.Aura;
import brokke.cc.nav.BestPoint;
import brokke.cc.util.GCDUtil2;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class ReallyWorldRotation extends RotationsSystem {
   private LivingEntity trackedTarget;
   private float currentYaw;
   private float currentPitch;
   private boolean initialized;
   private int ticks;

   public void a() {
      this.trackedTarget = null;
      this.ticks = 0;
      this.initialized = false;
      if (mc.player != null) {
         this.currentYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
         this.currentPitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : mc.player.getPitch();
      }
   }

   public void b() {
      this.ticks = 0;
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         if (!this.initialized) {
            this.currentYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
            this.currentPitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : mc.player.getPitch();
            this.initialized = true;
         }

         if (this.trackedTarget != var1) {
            this.trackedTarget = var1;
            this.ticks = 0;
         }

         this.ticks++;
         Vec3d var2 = BestPoint.a(var1, 128.0);
         if (var2 == null) {
            var2 = var1.getEntityPos().add(0.0, var1.getHeight() * 0.5, 0.0);
         }

         var2 = this.a(var1, var2);
         Vec2f var3 = RotationUtils.b(var2);
         float var4 = var3.x;
         float var5 = var3.y;
         float var6 = MathHelper.wrapDegrees(var4 - this.currentYaw);
         float var7 = var5 - this.currentPitch;
         float var8 = (float)Math.hypot(Math.abs(var6), Math.abs(var7));
         ThreadLocalRandom var9 = ThreadLocalRandom.current();
         float var10 = (float)(Math.sin(this.ticks * 0.35) * 0.15 + (var9.nextDouble() - 0.5) * 0.12);
         float var11 = (float)(Math.cos(this.ticks * 0.28) * 0.12 + (var9.nextDouble() - 0.5) * 0.08);
         float var12 = var8 > 60.0F ? 180.0F : (var8 > 25.0F ? 130.0F : 90.0F);
         float var13 = (float)(0.85 + var9.nextDouble() * 0.3);
         float var14 = Math.min(var8, var12 * var13);
         float var15 = var8 > 0.001F ? var6 / var8 * var14 : 0.0F;
         float var16 = var8 > 0.001F ? var7 / var8 * var14 : 0.0F;
         this.currentYaw = this.currentYaw + GCDUtil2.a(var15 + var10);
         this.currentPitch = MathHelper.clamp(this.currentPitch + GCDUtil2.a(var16 + var11), -90.0F, 90.0F);
         this.rotate = new Vec2f(this.currentYaw, this.currentPitch);
         RotationStorage.a(new Rotation2(this.currentYaw, this.currentPitch), 360.0F, 360.0F, 360.0F, 360.0F, 1, 5, Aura.clientLook.isEnabled());
      } else {
         this.a();
      }
   }
}
