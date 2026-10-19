package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.nav.BestPoint;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Holy2Rotation extends RotationsSystem {
   private static final float ROTATION_FOV = 180.0F;
   private static final float YAW_SPEED = 1080.0F;
   private static final float PITCH_SPEED = 1080.0F;
   private static final float RETURN_SPEED = 1080.0F;
   private static final float IDLE_REPATH_THRESHOLD = 0.05F;
   private static final float ACTIVE_REPATH_THRESHOLD = 0.16F;
   private static final float FAST_SIDE_MOTION_THRESHOLD = 0.07F;
   private static final float EARLY_REPATH_PROGRESS = 0.015F;
   private static final float LATE_REPATH_PROGRESS = 0.09F;
   private static final float MAX_LATERAL_DISTANCE = 54.0F;
   private static final float LATERAL_PITCH_SCALE = 0.68F;
   private static final float MOVEMENT_THRESHOLD = 0.08F;
   private static final float VELOCITY_THRESHOLD = 0.06F;
   private static final int STYLE_ORBIT = 0;
   private static final int STYLE_WHIP = 1;
   private static final int STYLE_ZIGZAG = 2;
   private static final int STYLE_RUN_SLIDE = 3;
   private static final int STYLE_CROSS_CUT = 4;
   private static final int STYLE_SPIRAL = 5;
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
   private int spinTurns;
   private float spinDirection;
   private float yawWaveAmplitude;
   private float pitchWaveAmplitude;
   private float wavePhase;
   private float curveExponent;
   private float spinExponent;
   private float lateralScale;
   private float yawWaveFrequency;
   private float pitchWaveFrequency;
   private int pathStyle;
   private boolean pathMoving;
   private float pathMovementYaw;
   private float pathMovementBlend;
   private float pathMovementPitchBlend;
   private float microYawAmplitude;
   private float microPitchAmplitude;
   private float pitchDiveAmplitude;
   private float endWhipAmplitude;

   public float getCurrentFov() {
      return this.currentFov;
   }

   public void b() {
      this.currentFov = 180.0F;
      this.f();
      this.j();
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
            this.j();
         } else {
            Vec3d var2 = this.g(var1);
            if (var2 == null) {
               this.f();
               this.j();
            } else {
               Vec2f var3 = RotationUtils.b(var2);
               float var4 = MathHelper.wrapDegrees(var3.x);
               float var5 = MathHelper.clamp(var3.y, -89.0F, 89.0F);
               if (!this.a(var4)) {
                  this.f();
                  this.j();
               } else {
                  long var6 = System.nanoTime();
                  this.a(var1, var6, var4, var5);
                  Rotation2 var8 = this.pathActive ? this.a(var6) : new Rotation2(var4, var5);
                  RotationStorage.a(var8, 1080.0F, 1080.0F, 1080.0F, 1080.0F, 0, 1, false);
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
         float var5 = MathHelper.clamp(var3.y, -89.0F, 89.0F);
         RotationStorage.a(new Rotation2(var4, var5), 1080.0F, 1080.0F, 1080.0F, 1080.0F, 0, 1, false);
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
      this.spinTurns = 0;
      this.pathStyle = 0;
      this.pathMoving = false;
      this.pathMovementYaw = 0.0F;
      this.pathMovementBlend = 0.0F;
      this.pathMovementPitchBlend = 0.0F;
      this.microYawAmplitude = 0.0F;
      this.microPitchAmplitude = 0.0F;
      this.pitchDiveAmplitude = 0.0F;
      this.endWhipAmplitude = 0.0F;
   }

   private void a(LivingEntity var1, long var2, float var4, float var5) {
      Rotation2 var6 = this.pathActive ? this.a(var2) : this.g();
      float var7 = var6.getYaw();
      float var8 = var6.getPitch();
      float var9 = (float)Math.hypot(MathHelper.wrapDegrees(var4 - var7), var5 - var8);
      float var10 = this.trackedTarget == var1 ? (float)Math.hypot(MathHelper.wrapDegrees(var4 - this.pathGoalYaw), var5 - this.pathGoalPitch) : var9;
      float var11 = this.hasPreviousTargetYaw ? Math.abs(MathHelper.wrapDegrees(var4 - this.previousTargetYaw)) : 0.0F;
      double var12 = this.b(var2);
      boolean var14 = this.trackedTarget != var1;
      boolean var15 = !this.pathActive && (var14 || var9 > 0.05F || var10 > 0.05F);
      boolean var16 = this.pathActive
         && (var12 >= 0.999 || var10 > 0.16F && var12 >= 0.015F || var9 > 0.19999999F && var12 >= 0.09F || var11 > 0.07F && var12 >= 0.015F);
      if (var15 || var16) {
         boolean var17 = var9 > 2.0F || var11 > 0.07F;
         this.a(var1, var2, var6.getYaw(), var6.getPitch(), var4, var5, var17);
      }

      this.previousTargetYaw = var4;
      this.hasPreviousTargetYaw = true;
   }

   private void a(LivingEntity var1, long var2, float var4, float var5, float var6, float var7, boolean var8) {
      this.trackedTarget = var1;
      this.pathActive = true;
      this.pathAction = var8;
      this.pathIndex = SlothDatasetProfile.a(this.random, var8);
      float var9 = this.h();
      this.pathMoving = var9 > 0.08F;
      this.pathStyle = this.a(var8, var9);
      this.pathMovementYaw = this.i();
      this.pathMovementBlend = this.b(this.pathMoving ? 0.34F : 0.08F, this.pathMoving ? 0.82F : 0.24F) * Math.max(0.25F, var9);
      this.pathMovementPitchBlend = this.b(this.pathMoving ? 0.24F : 0.06F, this.pathMoving ? 0.58F : 0.16F) * Math.max(0.2F, var9);
      this.pathStartNanos = var2;
      this.pathStartYaw = MathHelper.wrapDegrees(var4);
      this.pathStartPitch = MathHelper.clamp(var5, -89.0F, 89.0F);
      this.pathGoalYaw = MathHelper.wrapDegrees(var6);
      this.pathGoalPitch = MathHelper.clamp(var7, -89.0F, 89.0F);
      float var10 = Math.abs(MathHelper.wrapDegrees(this.pathGoalYaw - this.pathStartYaw));
      float var11 = Math.abs(this.pathGoalPitch - this.pathStartPitch);
      if (this.pathMoving) {
         this.spinTurns = var8 ? this.random.nextInt(3) + 1 : this.random.nextInt(2) + 1;
      } else {
         this.spinTurns = var8 ? this.random.nextInt(5) + 2 : this.random.nextInt(4) + 1;
      }

      if (this.pathStyle == 5) {
         this.spinTurns = this.spinTurns + (this.pathMoving ? 1 : 2);
      } else if (this.pathStyle == 1 && this.random.nextFloat() < 0.55F) {
         this.spinTurns++;
      }

      if (var10 > 120.0F && this.random.nextFloat() < 0.78F) {
         this.spinTurns++;
      }

      float var12 = Math.signum(MathHelper.wrapDegrees(this.pathGoalYaw - this.pathStartYaw));
      if (var12 == 0.0F) {
         var12 = this.random.nextBoolean() ? 1.0F : -1.0F;
      }

      this.spinDirection = this.random.nextFloat() < 0.42F ? -var12 : var12;
      this.yawWaveAmplitude = this.b(var8 ? 42.0F : 28.0F, var8 ? 140.0F : 104.0F);
      this.pitchWaveAmplitude = this.b(var8 ? 14.0F : 9.0F, var8 ? 56.0F : 34.0F);
      this.microYawAmplitude = this.b(this.pathMoving ? 14.0F : 8.0F, this.pathMoving ? 42.0F : 30.0F);
      this.microPitchAmplitude = this.b(this.pathMoving ? 6.0F : 4.0F, this.pathMoving ? 24.0F : 16.0F);
      this.pitchDiveAmplitude = this.b(var8 ? 8.0F : 4.0F, var8 ? 28.0F : 16.0F);
      this.endWhipAmplitude = this.b(var8 ? 10.0F : 5.0F, var8 ? 38.0F : 20.0F);
      this.wavePhase = this.random.nextFloat() * (float) (Math.PI * 2);
      this.curveExponent = this.b(var8 ? 3.2F : 2.5F, var8 ? 6.6F : 4.7F);
      this.spinExponent = this.b(var8 ? 1.7F : 1.3F, var8 ? 3.2F : 2.5F);
      this.lateralScale = this.b(this.pathMoving ? 0.82F : 1.28F, this.pathMoving ? 1.62F : 2.8F);
      this.yawWaveFrequency = this.b(this.pathMoving ? 4.2F : 3.0F, this.pathMoving ? 9.8F : 7.2F);
      this.pitchWaveFrequency = this.b(this.pathMoving ? 2.5F : 1.8F, this.pathMoving ? 6.4F : 4.9F);
      float var13 = MathHelper.clamp(0.38F + var10 / 115.0F + var11 / 80.0F, 0.42F, 1.18F);
      float var14 = this.b(var8 ? 18.0F : 24.0F, var8 ? 56.0F : 74.0F);
      if (this.pathMoving) {
         var14 *= this.b(0.58F, 0.84F);
      }

      if (this.pathStyle == 3) {
         var14 *= 0.76F;
      } else if (this.pathStyle == 5) {
         var14 *= 1.08F;
      }

      long var15 = (long)(var14 * var13);
      if (this.spinTurns >= 4) {
         var15 += this.pathMoving ? 4L : 8L;
      }

      long var17 = this.pathMoving ? 84L : 108L;
      this.pathDurationNanos = Math.max(12L, Math.min(var15, var17)) * 1000000L;
   }

   private Rotation2 a(long var1) {
      if (this.pathActive && this.pathDurationNanos > 0L) {
         double var3 = this.b(var1);
         double var5 = SlothDatasetProfile.a(this.pathAction, this.pathIndex, var3);
         double var7 = this.b((float)var5, (float)this.a(var3, this.curveExponent), 0.58F);
         double var9 = this.a(var3, this.spinExponent);
         double var11 = SlothDatasetProfile.b(this.pathAction, this.pathIndex, var3);
         float var13 = MathHelper.wrapDegrees(this.pathGoalYaw - this.pathStartYaw);
         float var14 = this.pathGoalPitch - this.pathStartPitch;
         float var15 = MathHelper.wrapDegrees(this.pathStartYaw + var13 * (float)var7);
         float var16 = MathHelper.clamp(this.pathStartPitch + var14 * (float)var7, -89.0F, 89.0F);
         float var17 = Math.min((float)Math.hypot(var13, var14), 54.0F);
         float var18 = var13 == 0.0F ? this.spinDirection : Math.signum(var13);
         float var19 = var18 * var17 * (float)(var11 * this.lateralScale);
         float var20 = (float)(-var18 * var17 * var11 * 0.68F);
         float var21 = this.spinDirection * this.spinTurns * 360.0F * (float)var9;
         float var22 = (float)Math.pow(Math.max(0.0, 1.0 - Math.abs(var3 * 2.0 - 1.0)), 0.3);
         float var23 = (float)Math.pow(Math.max(0.0, 1.0 - var3), 0.18);
         float var24 = this.yawWaveAmplitude * var22 * (float)Math.sin(var3 * Math.PI * this.yawWaveFrequency + this.wavePhase);
         float var25 = this.pitchWaveAmplitude * var22 * (float)Math.cos(var3 * Math.PI * this.pitchWaveFrequency + this.wavePhase * 0.71F);
         float var26 = this.microYawAmplitude * var23 * this.a(var3 * Math.PI * (this.yawWaveFrequency * 1.65F + 1.2F) + this.wavePhase * 1.4F);
         float var27 = this.microPitchAmplitude * var23 * (float)Math.sin(var3 * Math.PI * (this.pitchWaveFrequency * 1.85F + 0.9F) + this.wavePhase * 1.1F);
         float var28;
         float var29;
         switch (this.pathStyle) {
            case 1:
               float var41 = (float)Math.pow(Math.sin(var3 * Math.PI), 0.72);
               float var43 = -this.spinDirection * this.endWhipAmplitude * var41;
               var28 = var21 * 0.82F + var19 * 1.34F + var24 * 0.72F + var43 + var26;
               var29 = var20 * 0.48F + var25 * 0.86F + var27;
               break;
            case 2:
               float var40 = this.a(var3 * Math.PI * (this.yawWaveFrequency * 0.78F) + this.wavePhase);
               var28 = var21 * 0.46F + var19 * 1.18F + var40 * this.yawWaveAmplitude + var26 * 0.9F;
               var29 = var20 * 0.82F + var40 * this.pitchWaveAmplitude * 0.62F + var25 * 0.38F + var27;
               break;
            case 3:
               float var39 = (float)Math.sin(var3 * Math.PI * (this.yawWaveFrequency * 0.58F) + this.wavePhase);
               var28 = var21 * 0.32F + var19 * 0.92F + var39 * this.yawWaveAmplitude * 0.68F + var26 * 0.75F;
               var29 = var20 * 0.34F + var25 * 0.26F + var27 * 0.45F;
               break;
            case 4:
               float var38 = (float)Math.sin(var3 * Math.PI * (this.yawWaveFrequency * 1.35F) + this.wavePhase * 1.4F);
               var28 = var21 * 0.74F + var19 * 1.52F + var24 * 0.85F + var38 * this.microYawAmplitude * 1.45F;
               var29 = var20 + var25 * 0.55F - var38 * this.pitchWaveAmplitude * 0.7F + var27;
               break;
            case 5:
               float var30 = (float)Math.sin(var3 * Math.PI * (this.yawWaveFrequency * 0.92F) + this.wavePhase);
               float var31 = (float)(1.0 - Math.pow(1.0 - var3, 2.4));
               var28 = var21 * 1.18F + var19 * 1.12F + var24 * 0.58F + var26 * 1.1F + var30 * this.yawWaveAmplitude * 0.42F;
               var29 = var20 + var25 + var27 + (var31 - 0.35F) * this.pitchDiveAmplitude;
               break;
            default:
               var28 = var21 + var19 + var24 + var26 * 0.55F;
               var29 = var20 + var25 + var27 * 0.65F;
         }

         float var42 = this.h();
         float var44 = this.b(this.pathMovementYaw, this.i(), 0.42F + var42 * 0.38F);
         float var32 = (float)Math.pow(Math.max(0.0, 1.0 - var3), this.pathMoving ? 0.42F : 0.9F);
         float var33 = MathHelper.wrapDegrees(var44 - var15) * this.pathMovementBlend * var32 * (0.35F + var42 * 0.75F);
         float var34 = (this.e() - var16) * this.pathMovementPitchBlend * var32 * (0.28F + var42 * 0.6F);
         float var35 = -var18 * this.endWhipAmplitude * (float)Math.pow(var3, 1.65F) * (float)Math.pow(Math.max(0.0, 1.0 - var3), 0.18F);
         float var36 = MathHelper.wrapDegrees(var15 + var28 + var33 - var35);
         float var37 = MathHelper.clamp(var16 + var29 + var34, -89.0F, 89.0F);
         if (var3 >= 0.999) {
            this.pathActive = false;
            var36 = this.pathGoalYaw;
            var37 = this.pathGoalPitch;
         }

         return new Rotation2(var36, var37);
      } else {
         return new Rotation2(this.pathGoalYaw, this.pathGoalPitch);
      }
   }

   private int a(boolean var1, float var2) {
      double var3 = this.random.nextDouble();
      if (var2 > 0.45F) {
         if (var3 < 0.3) {
            return 3;
         } else if (var3 < 0.54) {
            return 2;
         } else if (var3 < 0.74) {
            return 1;
         } else {
            return var3 < 0.9 ? 4 : 5;
         }
      } else if (var1) {
         if (var3 < 0.22) {
            return 5;
         } else if (var3 < 0.42) {
            return 1;
         } else if (var3 < 0.6) {
            return 4;
         } else {
            return var3 < 0.8 ? 0 : 2;
         }
      } else if (var3 < 0.18) {
         return 0;
      } else if (var3 < 0.38) {
         return 2;
      } else if (var3 < 0.56) {
         return 3;
      } else if (var3 < 0.74) {
         return 1;
      } else {
         return var3 < 0.88 ? 4 : 5;
      }
   }

   private Rotation2 g() {
      return new Rotation2(MathHelper.wrapDegrees(mc.player.getYaw()), MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F));
   }

   private float h() {
      if (mc.player != null && mc.player.input != null) {
         float var1 = (mc.player.input.playerInput.forward() ? 1.0F : 0.0F) - (mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
         float var2 = (mc.player.input.playerInput.left() ? 1.0F : 0.0F) - (mc.player.input.playerInput.right() ? 1.0F : 0.0F);
         float var3 = MathHelper.clamp(Math.abs(var1) + Math.abs(var2) * 0.85F, 0.0F, 1.0F);
         Vec3d var4 = mc.player.getVelocity();
         double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
         float var7 = MathHelper.clamp((float)(var5 / 0.23), 0.0F, 1.0F);
         float var8 = mc.player.isSprinting() ? 0.14F : 0.0F;
         return MathHelper.clamp(Math.max(var3, var7) + var8, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   private float i() {
      float var1 = this.d();
      if (mc.player != null && mc.player.input != null) {
         float var2 = (mc.player.input.playerInput.forward() ? 1.0F : 0.0F) - (mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
         float var3 = (mc.player.input.playerInput.left() ? 1.0F : 0.0F) - (mc.player.input.playerInput.right() ? 1.0F : 0.0F);
         if (var2 == 0.0F && var3 == 0.0F) {
            Vec3d var4 = mc.player.getVelocity();
            double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
            return var5 > 0.06F ? (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var4.z, var4.x)) - 90.0) : var1;
         } else {
            return MathHelper.wrapDegrees((float)Math.toDegrees(this.a(var1, var2, var3)));
         }
      } else {
         return var1;
      }
   }

   private double b(long var1) {
      return this.pathActive && this.pathDurationNanos > 0L ? Math.max(0.0, Math.min(1.0, (double)(var1 - this.pathStartNanos) / this.pathDurationNanos)) : 1.0;
   }

   private double a(float var1, float var2, float var3) {
      if (var2 < 0.0F) {
         var1 += 180.0F;
      }

      float var4 = 1.0F;
      if (var2 < 0.0F) {
         var4 = -0.5F;
      } else if (var2 > 0.0F) {
         var4 = 0.5F;
      }

      if (var3 > 0.0F) {
         var1 -= 90.0F * var4;
      } else if (var3 < 0.0F) {
         var1 += 90.0F * var4;
      }

      return Math.toRadians(var1);
   }

   private double a(double var1, float var3) {
      return 1.0 - Math.pow(1.0 - var1, var3);
   }

   private float a(double var1) {
      return Math.signum((float)Math.sin(var1));
   }

   private float b(float var1, float var2, float var3) {
      return var1 + (var2 - var1) * MathHelper.clamp(var3, 0.0F, 1.0F);
   }

   private float b(float var1, float var2) {
      return var1 + (var2 - var1) * this.random.nextFloat();
   }

   private void j() {
      if (RotationStorage.instance != null && RotationStorage.instance.k()) {
         RotationStorage.instance.j();
      }
   }
}
