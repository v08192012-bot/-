package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.nav.BestPoint;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class HolyWorldRotation extends RotationsSystem {
   private static final float ROTATION_FOV = 180.0F;
   private static final float YAW_SPEED = 360.0F;
   private static final float PITCH_SPEED = 360.0F;
   private static final float RETURN_SPEED = 360.0F;
   private static final float IDLE_REPATH_THRESHOLD = 0.35F;
   private static final float ACTIVE_REPATH_THRESHOLD = 0.9F;
   private static final float FAST_SIDE_MOTION_THRESHOLD = 0.7F;
   private static final float EARLY_REPATH_PROGRESS = 0.18F;
   private static final float LATE_REPATH_PROGRESS = 0.52F;
   private static final float MAX_LATERAL_DISTANCE = 18.0F;
   private static final float LATERAL_PITCH_SCALE = 0.14F;
   private final Random random = new Random();
   private float currentFov = 180.0F;
   private LivingEntity trackedTarget;
   private boolean pathActive;
   private boolean pathAction;
   private int pathIndex;
   private long pathStartNanos;
   private long pathDurationNanos;
   private float pathStartYaw;
   private float pathGoalYaw;
   private float pathStartPitch;
   private float pathGoalPitch;
   private float previousTargetYaw;
   private boolean hasPreviousTargetYaw;

   public float getCurrentFov() {
      return this.currentFov;
   }

   public void b() {
      this.currentFov = 180.0F;
      this.f();
      this.h();
   }

   public void c() {
      this.currentFov = 180.0F;
   }

   public boolean d(LivingEntity var1) {
      return !this.e(var1) ? false : this.f(var1);
   }

   public boolean e(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         Vec3d var2 = this.g(var1);
         return var2 == null ? false : this.a(MathHelper.wrapDegrees(RotationUtils.b(var2).x));
      } else {
         return false;
      }
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null) {
         if (var1 == null) {
            this.currentFov = 180.0F;
            this.f();
            this.h();
         } else {
            Vec3d var2 = this.g(var1);
            if (var2 == null) {
               this.f();
               this.h();
            } else {
               Vec2f var3 = RotationUtils.b(var2);
               float var4 = MathHelper.wrapDegrees(var3.x);
               if (!this.a(var4)) {
                  this.f();
                  this.h();
               } else {
                  long var5 = System.nanoTime();
                  float var7 = this.e();
                  this.a(var1, var5, var4, var7);
                  Rotation2 var8 = this.pathActive ? this.a(var5) : new Rotation2(var4, var7);
                  RotationStorage.a(var8, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
               }
            }
         }
      }
   }

   private boolean f(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         Vec3d var2 = this.g(var1);
         if (var2 == null) {
            return false;
         }

         Vec2f var3 = RotationUtils.b(var2);
         float var4 = MathHelper.wrapDegrees(var3.x);
         float var5 = this.e();
         RotationStorage.a(new Rotation2(var4, var5), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
         this.a(var1, var4, var5);
         this.currentFov = 180.0F;
         return true;
      } else {
         return false;
      }
   }

   private Vec3d g(LivingEntity var1) {
      Vec3d var2 = BestPoint.a(var1);
      if (var2 == null) {
         var2 = MultipointUtils.a(var1);
      }

      if (var2 == null) {
         var2 = this.c(var1).getCenter();
      }

      return var2;
   }

   private float d() {
      return FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
   }

   private float e() {
      return FreeLookStorage.isActive()
         ? MathHelper.clamp(FreeLookStorage.getFreePitch(), -89.0F, 89.0F)
         : MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F);
   }

   private boolean a(float var1) {
      return true;
   }

   private void a(LivingEntity var1, float var2, float var3) {
      this.trackedTarget = var1;
      this.pathActive = false;
      this.pathGoalYaw = MathHelper.wrapDegrees(var2);
      this.pathGoalPitch = MathHelper.clamp(var3, -89.0F, 89.0F);
      this.previousTargetYaw = this.pathGoalYaw;
      this.hasPreviousTargetYaw = true;
   }

   private void f() {
      this.trackedTarget = null;
      this.pathActive = false;
      this.hasPreviousTargetYaw = false;
   }

   private void a(LivingEntity var1, long var2, float var4, float var5) {
      Rotation2 var6 = this.pathActive ? this.a(var2) : this.g();
      float var7 = var6.getYaw();
      float var8 = Math.abs(MathHelper.wrapDegrees(var4 - var7));
      float var9 = this.trackedTarget == var1 ? Math.abs(MathHelper.wrapDegrees(var4 - this.pathGoalYaw)) : var8;
      float var10 = this.hasPreviousTargetYaw ? Math.abs(MathHelper.wrapDegrees(var4 - this.previousTargetYaw)) : 0.0F;
      double var11 = this.b(var2);
      boolean var13 = this.trackedTarget != var1;
      boolean var14 = !this.pathActive && (var13 || var8 > 0.35F || var9 > 0.35F);
      boolean var15 = this.pathActive
         && (var11 >= 0.999 || var9 > 0.9F && var11 >= 0.18F || var8 > 1.3499999F && var11 >= 0.52F || var10 > 0.7F && var11 >= 0.18F);
      if (var14 || var15) {
         boolean var16 = var8 > 5.5F || var10 > 0.7F;
         this.a(var1, var2, var6.getYaw(), var6.getPitch(), var4, var5, var16);
      }

      this.previousTargetYaw = var4;
      this.hasPreviousTargetYaw = true;
   }

   private void a(LivingEntity var1, long var2, float var4, float var5, float var6, float var7, boolean var8) {
      this.trackedTarget = var1;
      this.pathActive = true;
      this.pathAction = var8;
      this.pathIndex = SlothDatasetProfile.a(this.random, var8);
      this.pathStartNanos = var2;
      this.pathStartYaw = MathHelper.wrapDegrees(var4);
      this.pathStartPitch = MathHelper.clamp(var5, -89.0F, 89.0F);
      this.pathGoalYaw = MathHelper.wrapDegrees(var6);
      this.pathGoalPitch = MathHelper.clamp(var7, -89.0F, 89.0F);
      float var9 = Math.abs(MathHelper.wrapDegrees(this.pathGoalYaw - this.pathStartYaw));
      float var10 = MathHelper.clamp(0.72F + var9 / 28.0F, 0.8F, 1.24F);
      long var11 = (long)((float)SlothDatasetProfile.a(var8, this.pathIndex) * var10 * this.b(0.92F, 1.08F));
      long var13 = var8 ? 260L : 320L;
      var11 = Math.max(75L, Math.min(var11, var13));
      this.pathDurationNanos = var11 * 1000000L;
   }

   private Rotation2 a(long var1) {
      if (this.pathActive && this.pathDurationNanos > 0L) {
         double var3 = this.b(var1);
         double var5 = SlothDatasetProfile.a(this.pathAction, this.pathIndex, var3);
         double var7 = SlothDatasetProfile.b(this.pathAction, this.pathIndex, var3);
         float var9 = MathHelper.wrapDegrees(this.pathGoalYaw - this.pathStartYaw);
         float var10 = this.pathGoalPitch - this.pathStartPitch;
         float var11 = MathHelper.wrapDegrees(this.pathStartYaw + var9 * (float)var5);
         float var12 = MathHelper.clamp(this.pathStartPitch + var10 * (float)var5, -89.0F, 89.0F);
         float var13 = Math.min((float)Math.hypot(var9, var10), 18.0F);
         float var14 = var9 == 0.0F ? 1.0F : Math.signum(var9);
         float var15 = (float)(var14 * var13 * var7);
         float var16 = (float)(-var14 * var13 * var7 * 0.14F);
         float var17 = MathHelper.wrapDegrees(var11 + var15);
         float var18 = MathHelper.clamp(var12 + var16, -89.0F, 89.0F);
         if (var3 >= 0.999) {
            this.pathActive = false;
            var17 = this.pathGoalYaw;
            var18 = this.pathGoalPitch;
         }

         return new Rotation2(var17, var18);
      } else {
         return new Rotation2(this.pathGoalYaw, this.pathGoalPitch);
      }
   }

   private Rotation2 g() {
      return new Rotation2(MathHelper.wrapDegrees(mc.player.getYaw()), MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F));
   }

   private double b(long var1) {
      return this.pathActive && this.pathDurationNanos > 0L ? Math.max(0.0, Math.min(1.0, (double)(var1 - this.pathStartNanos) / this.pathDurationNanos)) : 1.0;
   }

   private float b(float var1, float var2) {
      return var1 + (var2 - var1) * this.random.nextFloat();
   }

   private void h() {
      if (RotationStorage.instance != null && RotationStorage.instance.k()) {
         RotationStorage.instance.j();
      }
   }
}
