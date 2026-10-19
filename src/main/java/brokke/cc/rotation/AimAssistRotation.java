package brokke.cc.rotation;

import brokke.cc.module.combat.Aura;
import brokke.cc.util.GCDUtil2;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistRotation extends RotationsSystem {
   private final AimAssistPredictor predictor = new AimAssistPredictor();
   private final AimAssistMultipoint multipointHelper = new AimAssistMultipoint();
   private final float[] targetAnglesDelta = new float[2];
   private final float[] motorNoiseVec = new float[2];
   private final double[] knockbackDelta = new double[3];
   private double smoothTargetX;
   private double smoothTargetY;
   private double smoothTargetZ;
   private boolean pointInitialized;
   private float repitPhase;
   private float repitThreshold;
   private boolean repitActive = true;
   private boolean repitTriggered = false;
   private float repitProgress;
   private float smoothedYaw;
   private float smoothedPitch;
   private float remainderYaw;
   private float remainderPitch;
   private long lastRenderTime;
   private long lastMultipointTime;
   private float noiseMinBurst = 0.1F;
   private float noiseMaxBurst = 0.6F;
   private float noiseMinPause = 0.5F;
   private float noiseMaxPause = 3.25F;
   private float noiseBurstIntensity = 0.5F;
   private float noisePauseIntensity = 0.08F;
   private float noiseRampRate = 0.018F;
   private float noiseMaxRamp = 0.45F;
   private float noiseNearDist = 2.0F;
   private float noiseFarDist = 8.0F;
   private float noiseNearFactor = 0.15F;
   private float currentNoisePeriod = 0.5F;
   private float currentNoiseIntensity = 0.0F;
   private float noiseRamp = 0.0F;
   private float noiseIntervalTimer = 0.0F;
   private boolean noiseBurstActive = false;
   private float motorNoiseMag = 0.0F;
   private float driftPhase1 = 0.0F;
   private float driftPhase2 = 0.0F;
   private float driftFreq1 = 0.3F;
   private float driftFreq2 = 0.2F;
   private float driftAmplitude = 0.04F;
   private boolean hasGaussianSpare = false;
   private float gaussianSpare = 0.0F;
   private boolean overshootActive = false;
   private float overshootDuration = 0.0F;
   private float overshootElapsed = 0.0F;
   private float overshootYaw = 0.0F;
   private float overshootPitch = 0.0F;
   private int lastTargetId = -1;

   public AimAssistRotation() {
      this.a();
   }

   public void a() {
      ThreadLocalRandom var1 = ThreadLocalRandom.current();
      this.noiseMinBurst = 0.1F + var1.nextFloat() * 0.5F;
      this.noiseMaxBurst = (0.5F + var1.nextFloat() * 0.75F) * 2.0F;
      this.noiseMinPause = 0.5F + var1.nextFloat() * 0.5F;
      this.noiseMaxPause = 3.0F + var1.nextFloat() * 0.25F;
      this.noiseBurstIntensity = 0.5F + var1.nextFloat() * 0.7F;
      this.noisePauseIntensity = 0.08F + var1.nextFloat() * 0.12F;
      this.noiseRampRate = 0.018F + var1.nextFloat() * 0.005F;
      this.noiseMaxRamp = 0.45F + var1.nextFloat() * 0.05F;
      this.noiseNearDist = 2.0F + var1.nextFloat();
      this.noiseFarDist = 8.0F + var1.nextFloat() * 3.0F;
      this.noiseNearFactor = 0.15F + var1.nextFloat() * 0.05F;
      this.currentNoisePeriod = this.noiseMinPause + var1.nextFloat() * (this.noiseMaxPause - this.noiseMinPause);
      this.driftFreq1 = 0.3F + var1.nextFloat() * 0.4F;
      this.driftFreq2 = 0.2F + var1.nextFloat() * 0.3F;
      this.driftAmplitude = 0.04F + var1.nextFloat() * 0.06F;
      this.multipointHelper.a(0.05F + var1.nextFloat() * 0.15F);
   }

   public void d(LivingEntity var1) {
      if (var1 != null && var1.isAlive()) {
         if (var1.getId() != this.lastTargetId) {
            this.e(var1);
         }

         Vec3d var2 = var1.getVelocity();
         if (var2.lengthSquared() > 1.0E-6) {
            this.predictor.a(var1, this.d(), 100);
         }
      }
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null && var1.isAlive()) {
         if (var1.getId() != this.lastTargetId) {
            this.e(var1);
         }

         float var2 = 6.0F;
         if (this.a(var1, var2)) {
            this.smoothedYaw = 0.0F;
            this.smoothedPitch = 0.0F;
            RotationStorage.a(new Rotation2(mc.player.getYaw(), mc.player.getPitch()), 360.0F, 360.0F, 360.0F, 360.0F, 10, 1, false);
         } else {
            long var3 = System.nanoTime();
            float var5 = this.lastRenderTime == 0L ? 0.016666668F : Math.min((float)(var3 - this.lastRenderTime) / 1.0E9F, 0.1F);
            this.lastRenderTime = var3;
            if (var5 < 1.0E-4F) {
               var5 = 0.016666668F;
            }

            float var6 = 18.0F;
            float var7 = 1.0F;
            boolean var8 = true;
            float var9 = 0.4F;
            float var10 = 0.15F;
            byte var11 = 2;
            byte var12 = 1;
            float var13 = 65.0F;
            boolean var14 = true;
            float var15 = 3.0F;
            boolean var16 = false;
            boolean var17 = false;
            Vec3d var18 = mc.player.getEyePos();
            float var19 = this.d();
            this.a(var1, var19, var5, var18, var11, var13, var12, var16);
            float var20 = this.targetAnglesDelta[0];
            float var21 = this.targetAnglesDelta[1];
            if (Float.isFinite(var20) && Float.isFinite(var21)) {
               float var22 = var20 * var20 + var21 * var21;
               if (!(var22 < 0.01F)) {
                  float var23 = MathHelper.sqrt(var22);
                  if (var8) {
                     var21 += -MathHelper.clamp(mc.player.distanceTo(var1) * 0.12F, 0.0F, 1.5F);
                  }

                  float var24 = this.b(var1, var18);
                  float var25;
                  float var26;
                  if (var17) {
                     float var27 = this.e(var23, var24);
                     float var28 = Math.max(var27 / var6, 0.01F);
                     this.a(var23, var24, var5, var28);
                     float var29 = this.repitActive ? this.a(this.repitProgress) : this.d(var20, var21);
                     float var30 = var23 / var28 * var29 * var7;
                     float var31 = var23 > 1.0E-4F ? 1.0F / var23 : 0.0F;
                     var25 = MathHelper.clamp(var20 * var31 * var30 * var5, -20.0F, 20.0F);
                     var26 = var8 ? this.a(var21, var23, var24, var5, var6, var7, var9) : 0.0F;
                  } else {
                     float var36 = var6 * var7 * 10.0F * var5;
                     float var38 = Math.min(var23 / 10.0F, 1.0F);
                     float var41 = var23 > 1.0E-4F ? 1.0F / var23 : 0.0F;
                     var25 = var20 * var41 * var36 * var38;
                     var26 = var8 ? var21 * var41 * var36 * var38 * var9 : 0.0F;
                  }

                  this.a(var23, var5, var6);
                  float var37 = var10 * this.motorNoiseMag;
                  if (var37 > 1.0E-4F) {
                     this.b(var25, var26, var37, var5);
                     float var39 = this.b(var5);
                     var25 += this.motorNoiseVec[0] + var39 * 0.5F;
                     var26 += this.motorNoiseVec[1] + var39 * 0.25F;
                  }

                  if (var14) {
                     this.b(var23, var5, var15);
                     var25 += this.overshootYaw;
                     var26 += this.overshootPitch;
                  }

                  float var40 = MathHelper.clamp(1.0F - (float)Math.exp(-20.0F * var5), 0.05F, 0.95F);
                  this.smoothedYaw = this.smoothedYaw + (var25 - this.smoothedYaw) * var40;
                  this.smoothedPitch = this.smoothedPitch + (var26 - this.smoothedPitch) * (var40 * 0.75F);
                  float var42 = Math.abs(var20) * 1.5F + 0.5F;
                  float var43 = Math.abs(var21) * 1.5F + 0.5F;
                  this.smoothedYaw = MathHelper.clamp(this.smoothedYaw, -var42, var42);
                  this.smoothedPitch = MathHelper.clamp(this.smoothedPitch, -var43, var43);
                  float var44 = GCDUtil2.a();
                  float var32;
                  float var33;
                  if (var44 < 1.0E-4F) {
                     var32 = this.smoothedYaw;
                     var33 = this.smoothedPitch;
                  } else {
                     this.remainderYaw = this.remainderYaw + this.smoothedYaw;
                     this.remainderPitch = this.remainderPitch + this.smoothedPitch;
                     var32 = Math.round(this.remainderYaw / var44) * var44;
                     var33 = Math.round(this.remainderPitch / var44) * var44;
                     this.remainderYaw = MathHelper.clamp(this.remainderYaw - var32, -var44 * 2.0F, var44 * 2.0F);
                     this.remainderPitch = MathHelper.clamp(this.remainderPitch - var33, -var44 * 2.0F, var44 * 2.0F);
                  }

                  float var34 = mc.player.getYaw() + var32;
                  float var35 = MathHelper.clamp(mc.player.getPitch() + var33, -90.0F, 90.0F);
                  if (Aura.INSTANCE != null) {
                     Aura.INSTANCE.a(var32, var33);
                  }

                  RotationStorage.a(new Rotation2(var34, var35), 360.0F, 360.0F, 360.0F, 360.0F, 10, 1, false);
               }
            }
         }
      } else {
         this.b();
      }
   }

   public void b(float var1, float var2) {
   }

   public boolean c(float var1, float var2) {
      if (mc.player == null) {
         this.b();
         return true;
      }

      float var3 = MathHelper.wrapDegrees(var1 - mc.player.getYaw());
      float var4 = var2 - mc.player.getPitch();
      float var5 = var3 * var3 + var4 * var4;
      float var6 = GCDUtil2.a();
      float var7 = Math.max(0.5F, var6);
      if (Math.abs(var3) <= var7 && Math.abs(var4) <= var7) {
         this.b();
         return true;
      }

      long var8 = System.nanoTime();
      float var10 = this.lastRenderTime == 0L ? 0.016666668F : Math.min((float)(var8 - this.lastRenderTime) / 1.0E9F, 0.1F);
      this.lastRenderTime = var8;
      if (var10 < 1.0E-4F) {
         var10 = 0.016666668F;
      }

      float var11 = 18.0F;
      float var12 = 1.0F;
      float var13 = MathHelper.sqrt(var5);
      float var14 = var11 * var12 * 10.0F * var10;
      float var15 = Math.min(var13 / 10.0F, 1.0F);
      float var16 = var13 > 1.0E-4F ? 1.0F / var13 : 0.0F;
      float var17 = var3 * var16 * var14 * var15;
      float var18 = var4 * var16 * var14 * var15 * 0.4F;
      float var19 = MathHelper.clamp(1.0F - (float)Math.exp(-20.0F * var10), 0.05F, 0.95F);
      this.smoothedYaw = this.smoothedYaw + (var17 - this.smoothedYaw) * var19;
      this.smoothedPitch = this.smoothedPitch + (var18 - this.smoothedPitch) * (var19 * 0.75F);
      float var20 = Math.abs(var3) * 1.5F + 0.5F;
      float var21 = Math.abs(var4) * 1.5F + 0.5F;
      this.smoothedYaw = MathHelper.clamp(this.smoothedYaw, -var20, var20);
      this.smoothedPitch = MathHelper.clamp(this.smoothedPitch, -var21, var21);
      float var22;
      float var23;
      if (var6 < 1.0E-4F) {
         var22 = this.smoothedYaw;
         var23 = this.smoothedPitch;
      } else {
         this.remainderYaw = this.remainderYaw + this.smoothedYaw;
         this.remainderPitch = this.remainderPitch + this.smoothedPitch;
         var22 = Math.round(this.remainderYaw / var6) * var6;
         var23 = Math.round(this.remainderPitch / var6) * var6;
         this.remainderYaw = MathHelper.clamp(this.remainderYaw - var22, -var6 * 2.0F, var6 * 2.0F);
         this.remainderPitch = MathHelper.clamp(this.remainderPitch - var23, -var6 * 2.0F, var6 * 2.0F);
      }

      float var24 = mc.player.getYaw() + var22;
      float var25 = MathHelper.clamp(mc.player.getPitch() + var23, -90.0F, 90.0F);
      if (Aura.INSTANCE != null) {
         Aura.INSTANCE.a(var22, var23);
      }

      RotationStorage.a(new Rotation2(var24, var25), 360.0F, 360.0F, 360.0F, 360.0F, 10, 1, false);
      return false;
   }

   private boolean a(LivingEntity var1, float var2) {
      if (mc.player != null && var1 != null) {
         Vec3d var3 = mc.player.getEyePos();
         Vec3d var4 = mc.player.getRotationVector();
         Vec3d var5 = var3.add(var4.multiply(var2));
         Box var6 = var1.getBoundingBox().expand(0.08);
         return var6.contains(var3) || var6.raycast(var3, var5).isPresent();
      } else {
         return false;
      }
   }

   private void a(LivingEntity var1, float var2, float var3, Vec3d var4, int var5, float var6, int var7, boolean var8) {
      if (var1 != null && mc.player != null) {
         double var9 = MathHelper.lerp(var2, var1.lastRenderX, var1.getX());
         double var11 = MathHelper.lerp(var2, var1.lastRenderY, var1.getY());
         double var13 = MathHelper.lerp(var2, var1.lastRenderZ, var1.getZ());
         if (var5 > 0 && ThreadLocalRandom.current().nextFloat() < var6 / 100.0F) {
            Vec3d var15 = this.predictor.getLastVelocity();
            if (var15 != null && var15.lengthSquared() > 1.0E-6) {
               var9 += var15.x * var5;
               var11 += var15.y * var5;
               var13 += var15.z * var5;
            }
         }

         float var27 = mc.player.getAttackCooldownProgress(1.5F);
         if (var27 > 0.85F && !mc.player.isUsingItem()) {
            double var16 = var1.getX() - var4.x;
            double var18 = var1.getZ() - var4.z;
            double var20 = var16 * var16 + var18 * var18;
            if (var20 < 12.25 && var20 > 2.25) {
               float var22 = (var27 - 0.85F) / 0.15F;
               var22 *= var22;
               AimAssistPredictor.a(
                  this.knockbackDelta, var4.x, var4.z, mc.player.getYaw(), var1, mc.player.isSprinting(), AimAssistPredictor.a(mc.player), var7
               );
               var9 -= this.knockbackDelta[0] * var22;
               var11 -= this.knockbackDelta[1] * var22;
               var13 -= this.knockbackDelta[2] * var22;
            }
         }

         if (var8) {
            this.a(var1, var9, var11, var13);
            double[] var28 = this.multipointHelper.a();
            if (var28 != null) {
               var9 = var28[0];
               var11 = var28[1];
               var13 = var28[2];
            } else {
               double var17 = var11 + var1.getHeight() * 0.35;
               double var19 = var11 + var1.getHeight() * 0.85;
               var11 = MathHelper.clamp(var4.y, var17, var19);
            }
         } else {
            double var29 = var11 + var1.getHeight() * 0.35;
            double var32 = var11 + var1.getHeight() * 0.85;
            var11 = MathHelper.clamp(var4.y, var29, var32);
         }

         if (!this.pointInitialized) {
            this.smoothTargetX = var9;
            this.smoothTargetY = var11;
            this.smoothTargetZ = var13;
            this.pointInitialized = true;
         } else {
            double var30 = MathHelper.clamp(1.0 - Math.exp(-25.0 * var3), 0.05, 0.95);
            this.smoothTargetX = this.smoothTargetX + (var9 - this.smoothTargetX) * var30;
            this.smoothTargetY = this.smoothTargetY + (var11 - this.smoothTargetY) * var30;
            this.smoothTargetZ = this.smoothTargetZ + (var13 - this.smoothTargetZ) * var30;
         }

         double var31 = this.smoothTargetX - var4.x;
         double var33 = this.smoothTargetY - var4.y;
         double var34 = this.smoothTargetZ - var4.z;
         double var36 = Math.sqrt(var31 * var31 + var34 * var34);
         if (var36 < 1.0E-4 && Math.abs(var33) < 1.0E-4) {
            this.targetAnglesDelta[0] = 0.0F;
            this.targetAnglesDelta[1] = 0.0F;
         } else {
            float var24 = (float)Math.toDegrees(Math.atan2(var34, var31)) - 90.0F;
            float var25 = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(var33, var36))), -90.0F, 90.0F);
            this.targetAnglesDelta[0] = MathHelper.wrapDegrees(var24 - mc.player.getYaw());
            this.targetAnglesDelta[1] = var25 - mc.player.getPitch();
         }
      } else {
         this.targetAnglesDelta[0] = 0.0F;
         this.targetAnglesDelta[1] = 0.0F;
      }
   }

   private void a(LivingEntity var1, double var2, double var4, double var6) {
      if (mc.world != null) {
         long var8 = mc.world.getTime();
         long var10 = Math.max(1L, 0L);
         if (var8 - this.lastMultipointTime >= var10) {
            this.lastMultipointTime = var8;
            this.multipointHelper.a(8, 0.7F, true);
            this.multipointHelper.a(new Vec3d(var2, var4, var6), var1, mc.player.getEntityPos(), mc.player.distanceTo(var1));
         }
      }
   }

   private void a(float var1, float var2, float var3, float var4) {
      if (var1 < 0.1F) {
         this.repitActive = false;
         this.repitProgress = 1.0F;
      } else if (this.repitActive) {
         this.repitProgress = this.repitProgress + var3 / Math.max(var4 * 0.4F, 0.01F);
         if (this.repitProgress >= 1.0F || var1 < var2 * 2.0F) {
            this.repitActive = false;
            this.repitProgress = 1.0F;
         }
      }
   }

   private float a(float var1) {
      float var2 = MathHelper.clamp(var1, 0.0F, 1.0F);
      return 24.0F * var2 * var2 * (1.0F - var2) * (1.0F - var2);
   }

   private float d(float var1, float var2) {
      float var3 = MathHelper.sqrt(var1 * var1 + var2 * var2);
      return var3 < 1.0E-4F ? 0.1F : 0.4F + 0.3F * Math.min(var3 / 5.0F, 1.0F);
   }

   private float a(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = Math.abs(var1);
      float var9 = var1 > 0.0F ? 0.4F : 0.8F;
      if (var8 > 7.0F) {
         float var10 = this.e(var8, var3);
         float var11 = Math.max(var10 / (var5 * 0.4F), 0.02F);
         return MathHelper.clamp(Math.signum(var1) * (var8 / var11) * 0.5F * var4 * var6 * var9 * var7, -20.0F, 20.0F);
      } else {
         return var8 > 0.3F ? MathHelper.clamp(var1 * 0.6F * var9 * var7 * var4 * 10.0F, -20.0F, 20.0F) : var1 * 2.0F * 0.4F * var9 * var7 * var4 * 10.0F;
      }
   }

   private float e(float var1, float var2) {
      return var1 < 1.0E-4F ? 0.0F : (float)(Math.log(var1 / Math.max(var2, 0.1F) + 1.0) / Math.log(2.0));
   }

   private float b(LivingEntity var1, Vec3d var2) {
      if (var1 == null) {
         return 1.0F;
      }

      double var3 = var1.getX() - var2.x;
      double var5 = var1.getY() + var1.getHeight() * 0.5 - var2.y;
      double var7 = var1.getZ() - var2.z;
      double var9 = var3 * var3 + var5 * var5 + var7 * var7;
      double var11 = Math.max(Math.sqrt(var9), 0.5);
      return (float)Math.toDegrees(Math.atan2(var1.getWidth() * 0.5, var11));
   }

   private void a(float var1, float var2, float var3) {
      this.noiseIntervalTimer += var2;
      if (this.noiseIntervalTimer >= this.currentNoisePeriod) {
         this.noiseBurstActive = !this.noiseBurstActive;
         this.noiseIntervalTimer = 0.0F;
         ThreadLocalRandom var4 = ThreadLocalRandom.current();
         this.currentNoisePeriod = this.noiseBurstActive
            ? this.noiseMinBurst + var4.nextFloat() * (this.noiseMaxBurst - this.noiseMinBurst)
            : this.noiseMinPause + var4.nextFloat() * (this.noiseMaxPause - this.noiseMinPause);
      }

      float var9 = this.noiseBurstActive ? 7.0F : 2.5F;
      float var5 = this.noiseBurstActive ? this.noiseBurstIntensity : this.noisePauseIntensity;
      this.currentNoiseIntensity = this.currentNoiseIntensity + (var5 - this.currentNoiseIntensity) * (1.0F - (float)Math.exp(-var9 * var2));
      float var6 = this.currentNoiseIntensity;
      float var7 = this.noiseRampRate * (7.0F / Math.max(var3, 1.0F));
      this.noiseRamp = MathHelper.clamp(this.noiseRamp + var2 * var7, 0.0F, this.noiseMaxRamp);
      float var10 = var6 + this.noiseRamp;
      if (var1 < this.noiseNearDist) {
         var10 *= this.noiseNearFactor;
      } else if (var1 < this.noiseFarDist) {
         float var8 = (var1 - this.noiseNearDist) / (this.noiseFarDist - this.noiseNearDist);
         var10 *= this.noiseNearFactor + (1.0F - this.noiseNearFactor) * var8;
      }

      this.motorNoiseMag = Math.max(var10, 0.0F);
   }

   private void b(float var1, float var2, float var3, float var4) {
      float var5 = (float)Math.exp(-var4 / 0.04F);
      float var6 = this.c();
      float var7 = this.c();
      float var8 = MathHelper.sqrt(var1 * var1 + var2 * var2) + 1.0E-4F;
      float var9 = var3 * var8 * (1.0F - var5);
      float var10 = var1 / var8;
      float var11 = var2 / var8;
      float var12 = -var11;
      float var13 = var10;
      float var14 = var6 * var9;
      float var15 = var7 * var9 * 0.4F;
      this.motorNoiseVec[0] = this.motorNoiseVec[0] * var5 + var14 * var10 + var15 * var12;
      this.motorNoiseVec[1] = this.motorNoiseVec[1] * var5 + var14 * var11 + var15 * var13;
   }

   private float c() {
      if (this.hasGaussianSpare) {
         this.hasGaussianSpare = false;
         return this.gaussianSpare;
      } else {
         ThreadLocalRandom var1 = ThreadLocalRandom.current();
         double var2 = Math.max(1.0E-10, var1.nextDouble());
         double var4 = var1.nextDouble();
         double var6 = Math.sqrt(-2.0 * Math.log(var2));
         this.gaussianSpare = (float)(var6 * Math.sin((Math.PI * 2) * var4));
         this.hasGaussianSpare = true;
         return (float)(var6 * Math.cos((Math.PI * 2) * var4));
      }
   }

   private float b(float var1) {
      this.driftPhase1 = this.driftPhase1 + this.driftFreq1 * var1;
      this.driftPhase2 = this.driftPhase2 + this.driftFreq2 * var1;
      if (this.driftPhase1 > Math.PI * 2) {
         this.driftPhase1 -= (float) (Math.PI * 2);
      }

      if (this.driftPhase2 > Math.PI * 2) {
         this.driftPhase2 -= (float) (Math.PI * 2);
      }

      return this.driftAmplitude * (0.6F * (float)Math.sin(this.driftPhase1) + 0.4F * (float)Math.sin(this.driftPhase2 * 1.618F));
   }

   private void b(float var1, float var2, float var3) {
      if (this.overshootActive) {
         this.overshootElapsed += var2;
         if (this.overshootElapsed >= this.overshootDuration) {
            this.overshootActive = false;
            this.overshootYaw = 0.0F;
            this.overshootPitch = 0.0F;
         }
      } else if (var1 < 3.0F && var1 > 0.5F) {
         if (ThreadLocalRandom.current().nextFloat() < var3 / 100.0F * var2 * 20.0F) {
            this.overshootActive = true;
            this.overshootElapsed = 0.0F;
            this.overshootDuration = 0.08F + ThreadLocalRandom.current().nextFloat() * 0.12F;
            float var4 = 1.0F + ThreadLocalRandom.current().nextFloat() * 2.0F;
            float var5 = ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F;
            this.overshootYaw = var5 * var4 * var2;
            this.overshootPitch = (ThreadLocalRandom.current().nextFloat() - 0.5F) * var4 * var2 * 0.3F;
         }
      } else {
         this.overshootYaw = 0.0F;
         this.overshootPitch = 0.0F;
      }
   }

   private void e(LivingEntity var1) {
      this.lastTargetId = var1 != null ? var1.getId() : -1;
      this.pointInitialized = false;
      this.smoothTargetX = 0.0;
      this.smoothTargetY = 0.0;
      this.smoothTargetZ = 0.0;
      this.repitPhase = 0.0F;
      this.repitTriggered = false;
      this.repitThreshold = var1 != null ? this.f(var1) : 0.0F;
      this.repitActive = true;
      this.repitProgress = 0.0F;
      this.lastMultipointTime = 0L;
      this.multipointHelper.b();
      this.predictor.c();
      this.overshootActive = false;
      this.overshootYaw = 0.0F;
      this.overshootPitch = 0.0F;
      this.smoothedYaw = 0.0F;
      this.smoothedPitch = 0.0F;
      this.motorNoiseVec[0] = 0.0F;
      this.motorNoiseVec[1] = 0.0F;
   }

   private float f(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         double var2 = mc.player.distanceTo(var1);
         float var4 = 0.5F + (float)(var2 / 8.0) * 0.5F;
         return MathHelper.clamp(var4 + (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.2F, 0.3F, 1.0F);
      } else {
         return 0.5F;
      }
   }

   public void b() {
      this.lastTargetId = -1;
      this.pointInitialized = false;
      this.smoothTargetX = 0.0;
      this.smoothTargetY = 0.0;
      this.smoothTargetZ = 0.0;
      this.repitPhase = 0.0F;
      this.repitTriggered = false;
      this.repitThreshold = 0.0F;
      this.repitActive = true;
      this.repitProgress = 0.0F;
      this.smoothedYaw = 0.0F;
      this.smoothedPitch = 0.0F;
      this.remainderYaw = 0.0F;
      this.remainderPitch = 0.0F;
      this.lastMultipointTime = 0L;
      this.lastRenderTime = 0L;
      this.overshootActive = false;
      this.overshootYaw = 0.0F;
      this.overshootPitch = 0.0F;
      this.motorNoiseVec[0] = 0.0F;
      this.motorNoiseVec[1] = 0.0F;
      this.predictor.c();
      this.multipointHelper.b();
   }

   private float d() {
      try {
         return mc.getRenderTickCounter().getTickProgress(false);
      } catch (Throwable var2) {
         return 1.0F;
      }
   }
}
