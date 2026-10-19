package brokke.cc.rotation;

import brokke.cc.animation.Easings2;
import brokke.cc.util.GCDUtil2;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FuntimeRotation extends RotationsSystem {
   private static final long MIN_UPDATE_NANOS = 500000L;
   private static final long STATE_GAP_NANOS = 200000000L;
   private static final long FOCUS_HOLD_NANOS = 120000000L;
   private static final long ACTION_DEBOUNCE_NANOS = 30000000L;
   private static final double MAX_DELTA_TIME = 0.05;
   private static final int ROTATION_PRIORITY = 1;
   private static final int AIM_HISTORY_CAPACITY = 12;
   private static final int MOUSE_REPORT_CENTER_MICROS = 20150;
   private static final float PRECISION_HUMANIZATION_SCALE = 0.8F;
   private static final float PRECISION_RESPONSE_SCALE = 1.2F;
   private static final float PLAYER_SPEED_FACTOR_CAP = 1.5F;
   private static final float MAX_YAW_DELTA = 60.0F;
   private static final float MAX_PITCH_DELTA = 24.0F;
   private static final float SPEED_ENVELOPE_MIN = 0.55F;
   private static final float SPEED_ENVELOPE_MAX = 1.25F;
   private final Random random = new Random();
   private final double[] aimHistoryX = new double[12];
   private final double[] aimHistoryY = new double[12];
   private final double[] aimHistoryZ = new double[12];
   private final int[] aimHistoryZone = new int[12];
   private LivingEntity trackedTarget;
   private FuntimeRotation.a phase = FuntimeRotation.a.REACTION;
   private Vec3d filteredVelocity = Vec3d.ZERO;
   private float requestedYaw;
   private float requestedPitch;
   private float yawVelocity;
   private float pitchVelocity;
   private float yawMotorAcceleration;
   private float pitchMotorAcceleration;
   private float wantedYawVelocity;
   private float wantedPitchVelocity;
   private float previousWantedYaw;
   private float previousWantedPitch;
   private boolean hasWantedRotation;
   private double anchorX = 0.5;
   private double anchorY = 0.62;
   private double anchorZ = 0.5;
   private double anchorGoalX = 0.5;
   private double anchorGoalY = 0.62;
   private double anchorGoalZ = 0.5;
   private double anchorStartX = 0.5;
   private double anchorStartY = 0.62;
   private double anchorStartZ = 0.5;
   private double anchorControlOneX = 0.5;
   private double anchorControlOneY = 0.62;
   private double anchorControlOneZ = 0.5;
   private double anchorControlTwoX = 0.5;
   private double anchorControlTwoY = 0.62;
   private double anchorControlTwoZ = 0.5;
   private double anchorMidX = 0.5;
   private double anchorMidY = 0.62;
   private double anchorMidZ = 0.5;
   private double anchorOvershootX = 0.5;
   private double anchorOvershootY = 0.62;
   private double anchorOvershootZ = 0.5;
   private double pathBendX;
   private double pathBendY;
   private double pathBendZ;
   private double pathTimeWarp;
   private double pathEaseIn = 2.0;
   private double pathEaseOut = 2.0;
   private double pathFlickExponent = 2.8;
   private double pathPrimaryEnd = 0.75;
   private double pathCorrectionStart = 0.58;
   private double pathEntryX;
   private double pathEntryY;
   private double pathEntryZ;
   private double anchorPathVelocityX;
   private double anchorPathVelocityY;
   private double anchorPathVelocityZ;
   private float noiseYaw;
   private float noisePitch;
   private float noiseGoalYaw;
   private float noiseGoalPitch;
   private float recoilYaw;
   private float recoilPitch;
   private float tremorYaw;
   private float tremorPitch;
   private float tremorGoalYaw;
   private float tremorGoalPitch;
   private float tremorBlend;
   private float pathNoiseScale = 0.06F;
   private float noiseMotionBlend = 0.1F;
   private float speedScale = 1.0F;
   private float speedGoal = 1.0F;
   private float segmentSpeedScale = 1.0F;
   private float segmentSpeedGoal = 1.0F;
   private float perceptionLeadScale = 1.0F;
   private float perceptionLeadGoal = 1.0F;
   private float perceptionLeadBias;
   private float perceptionLeadBiasGoal;
   private float lapseYaw;
   private float lapsePitch;
   private float hesitationScale = 1.0F;
   private float hesitationGoal = 1.0F;
   private float lapsePeakProgress = 0.28F;
   private float lapseRiseExponent = 2.4F;
   private float maximumYawSpeed;
   private float maximumPitchSpeed;
   private float yawAcceleration;
   private float pitchAcceleration;
   private float yawDeceleration;
   private float pitchDeceleration;
   private float yawJerkLimit;
   private float pitchJerkLimit;
   private long lastUpdateNanos;
   private long reactionUntilNanos;
   private long nextAnchorNanos;
   private long anchorPathStartNanos;
   private long anchorPathEndNanos;
   private long lastAnchorSampleNanos;
   private long nextNoiseNanos;
   private long nextTremorNanos;
   private long tremorPulseEndNanos;
   private long nextSpeedNanos;
   private long nextPerceptionNanos;
   private long nextLapseNanos;
   private long lapseStartNanos;
   private long lapseEndNanos;
   private long nextHesitationNanos;
   private long hesitationEndNanos;
   private long precisionUntilNanos;
   private long lastActionPathNanos;
   private long nextMotorPauseNanos;
   private long motorPauseUntilNanos;
   private long nextMouseReportNanos;
   private long lastMouseReportNanos;
   private long lastRotationSampleNanos;
   private int previousMouseReportMicros = 20150;
   private int secondPreviousMouseReportMicros = 20150;
   private boolean precisionRequested;
   private boolean priorityBlocked;
   private boolean wasAirborne;
   private int aimHistorySize;
   private int aimHistoryCursor;
   private int lastAimZone = -1;
   private int previousAimZone = -1;
   private int repeatedPathModels;
   private FuntimeRotation.c pathModel = FuntimeRotation.c.MINIMUM_JERK;
   private FuntimeRotation.c previousPathModel = FuntimeRotation.c.MINIMUM_JERK;
   private FuntimeRotation.b pathEase = FuntimeRotation.b.MINIMUM_JERK;
   private FuntimeRotation.b previousPathEase = FuntimeRotation.b.MINIMUM_JERK;
   private int empiricalPathIndex;
   private boolean empiricalAction;
   private boolean empiricalJump;
   private boolean jumpPathRequested;
   private boolean jumpCriticalPathPending;
   private float empiricalCurveYaw;
   private float empiricalCurvePitch;
   private float empiricalEntryYaw;
   private float empiricalEntryPitch;
   private float empiricalPerpendicularYaw;
   private float empiricalPerpendicularPitch;
   private float empiricalAngularDistance;
   private long jumpGestureStartNanos;
   private long jumpGestureEndNanos;
   private float jumpPitchAmplitude;
   private float airMotorScale = 1.0F;
   private boolean visibleAim;

   public void a() {
      this.trackedTarget = null;
      this.phase = FuntimeRotation.a.REACTION;
      this.filteredVelocity = Vec3d.ZERO;
      this.yawVelocity = 0.0F;
      this.pitchVelocity = 0.0F;
      this.yawMotorAcceleration = 0.0F;
      this.pitchMotorAcceleration = 0.0F;
      this.wantedYawVelocity = 0.0F;
      this.wantedPitchVelocity = 0.0F;
      this.previousWantedYaw = 0.0F;
      this.previousWantedPitch = 0.0F;
      this.hasWantedRotation = false;
      this.anchorX = this.anchorGoalX = 0.5;
      this.anchorY = this.anchorGoalY = 0.62;
      this.anchorZ = this.anchorGoalZ = 0.5;
      this.anchorStartX = this.anchorControlOneX = this.anchorControlTwoX = 0.5;
      this.anchorStartY = this.anchorControlOneY = this.anchorControlTwoY = 0.62;
      this.anchorStartZ = this.anchorControlOneZ = this.anchorControlTwoZ = 0.5;
      this.anchorMidX = this.anchorOvershootX = 0.5;
      this.anchorMidY = this.anchorOvershootY = 0.62;
      this.anchorMidZ = this.anchorOvershootZ = 0.5;
      this.pathBendX = this.pathBendY = this.pathBendZ = 0.0;
      this.pathTimeWarp = 0.0;
      this.pathEaseIn = this.pathEaseOut = 2.0;
      this.pathFlickExponent = 2.8;
      this.pathPrimaryEnd = 0.75;
      this.pathCorrectionStart = 0.58;
      this.pathEntryX = this.pathEntryY = this.pathEntryZ = 0.0;
      this.anchorPathVelocityX = this.anchorPathVelocityY = this.anchorPathVelocityZ = 0.0;
      this.noiseYaw = this.noisePitch = 0.0F;
      this.noiseGoalYaw = this.noiseGoalPitch = 0.0F;
      this.recoilYaw = this.recoilPitch = 0.0F;
      this.tremorYaw = this.tremorPitch = 0.0F;
      this.tremorGoalYaw = this.tremorGoalPitch = 0.0F;
      this.tremorBlend = 0.0F;
      this.pathNoiseScale = 0.06F;
      this.noiseMotionBlend = 0.1F;
      this.speedScale = this.speedGoal = 1.0F;
      this.segmentSpeedScale = this.segmentSpeedGoal = 1.0F;
      this.perceptionLeadScale = this.perceptionLeadGoal = 1.0F;
      this.perceptionLeadBias = this.perceptionLeadBiasGoal = 0.0F;
      this.lapseYaw = this.lapsePitch = 0.0F;
      this.hesitationScale = this.hesitationGoal = 1.0F;
      this.lapsePeakProgress = 0.28F;
      this.lapseRiseExponent = 2.4F;
      this.lastUpdateNanos = 0L;
      this.reactionUntilNanos = 0L;
      this.nextAnchorNanos = 0L;
      this.anchorPathStartNanos = 0L;
      this.anchorPathEndNanos = 0L;
      this.lastAnchorSampleNanos = 0L;
      this.nextNoiseNanos = 0L;
      this.nextTremorNanos = 0L;
      this.tremorPulseEndNanos = 0L;
      this.nextSpeedNanos = 0L;
      this.nextPerceptionNanos = 0L;
      this.nextLapseNanos = 0L;
      this.lapseStartNanos = 0L;
      this.lapseEndNanos = 0L;
      this.nextHesitationNanos = 0L;
      this.hesitationEndNanos = 0L;
      this.precisionUntilNanos = 0L;
      this.lastActionPathNanos = 0L;
      this.nextMotorPauseNanos = 0L;
      this.motorPauseUntilNanos = 0L;
      this.nextMouseReportNanos = 0L;
      this.lastMouseReportNanos = 0L;
      this.lastRotationSampleNanos = 0L;
      this.previousMouseReportMicros = 20150;
      this.secondPreviousMouseReportMicros = 20150;
      this.precisionRequested = false;
      this.priorityBlocked = false;
      this.wasAirborne = mc.player != null && !mc.player.isOnGround();
      this.e();
      this.repeatedPathModels = 0;
      this.pathModel = FuntimeRotation.c.MINIMUM_JERK;
      this.previousPathModel = FuntimeRotation.c.MINIMUM_JERK;
      this.pathEase = FuntimeRotation.b.MINIMUM_JERK;
      this.previousPathEase = FuntimeRotation.b.MINIMUM_JERK;
      this.empiricalPathIndex = 0;
      this.empiricalAction = false;
      this.empiricalJump = false;
      this.jumpPathRequested = false;
      this.jumpCriticalPathPending = false;
      this.empiricalCurveYaw = 0.0F;
      this.empiricalCurvePitch = 0.0F;
      this.empiricalEntryYaw = 0.0F;
      this.empiricalEntryPitch = 0.0F;
      this.empiricalPerpendicularYaw = 0.0F;
      this.empiricalPerpendicularPitch = 0.0F;
      this.empiricalAngularDistance = 0.0F;
      this.jumpGestureStartNanos = 0L;
      this.jumpGestureEndNanos = 0L;
      this.jumpPitchAmplitude = 0.0F;
      this.airMotorScale = 1.0F;
      if (mc.player != null) {
         this.requestedYaw = mc.player.getYaw();
         this.requestedPitch = mc.player.getPitch();
      } else {
         this.requestedYaw = 0.0F;
         this.requestedPitch = 0.0F;
      }
   }

   public void a(boolean var1) {
      long var2 = System.nanoTime();
      if (var1 && !this.precisionRequested && this.trackedTarget != null) {
         this.b(var2, true);
      }

      this.precisionRequested = var1;
      if (var1) {
         this.precisionUntilNanos = Math.max(this.precisionUntilNanos, var2 + 120000000L);
      }
   }

   public void setVisibleAim(boolean var1) {
      this.visibleAim = var1;
   }

   public void b() {
      if (mc.player != null && this.trackedTarget != null) {
         long var1 = System.nanoTime();
         this.precisionUntilNanos = Math.max(this.precisionUntilNanos, var1 + 100000000L);
         this.recoilYaw = this.c(0.04F, 0.13F);
         double var3 = this.random.nextDouble();
         this.recoilPitch = var3 < 0.63 ? this.d(0.12F, 0.42F) : (var3 < 0.83 ? -this.d(0.08F, 0.28F) : 0.0F);
         boolean var5 = this.pathModel == FuntimeRotation.c.EMPIRICAL
            && this.empiricalAction
            && var1 >= this.anchorPathStartNanos
            && var1 < this.anchorPathEndNanos;
         if (!var5) {
            this.b(var1, true);
         }

         this.phase = FuntimeRotation.a.RECOVER;
      }
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         long var2 = System.nanoTime();
         RotationStorage var4 = RotationStorage.instance;
         if (var4 != null && var4.getCurrentPriority() > 1) {
            this.a(var2, true);
            this.priorityBlocked = true;
         } else {
            if (this.trackedTarget != var1) {
               this.a(var1, var2);
            }

            if (this.priorityBlocked) {
               this.a(var2, true);
               this.priorityBlocked = false;
            } else {
               long var5 = this.lastUpdateNanos == 0L ? 4166667L : var2 - this.lastUpdateNanos;
               if (var5 >= 500000L) {
                  if (var5 > 200000000L) {
                     this.a(var2, true);
                     this.phase = FuntimeRotation.a.ACQUIRE;
                  } else {
                     this.lastUpdateNanos = var2;
                     double var7 = Math.min(var5 / 1.0E9, 0.05);
                     boolean var9 = this.precisionRequested || var2 < this.precisionUntilNanos;
                     boolean var10 = this.d(var2, var9 || var2 < this.reactionUntilNanos);
                     boolean var11 = !mc.player.isOnGround();
                     if (var11 && !this.wasAirborne && mc.player.getVelocity().y > 0.035) {
                        this.c(var2, var9);
                     }

                     if (var11 && this.jumpCriticalPathPending && mc.player.getVelocity().y < -0.07) {
                        this.jumpCriticalPathPending = false;
                        this.b(var2, var9);
                     } else if (!var11) {
                        this.jumpCriticalPathPending = false;
                     }

                     this.wasAirborne = var11;
                     this.a(var2, var7, var9);
                     Vec3d var12 = this.a(var1, var7, var9);
                     Vec3d var13 = var12.subtract(mc.player.getEyePos());
                     float var14 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var13.z, var13.x)) - 90.0);
                     float var15 = (float)(-Math.toDegrees(Math.atan2(var13.y, var13.horizontalLength())));
                     float var16 = this.a(var1, var13.length());
                     float var17 = this.b(var1, var13.length());
                     float var18 = MathHelper.clamp(this.empiricalCurveYaw, -var16 * (var9 ? 0.272F : 0.85F), var16 * (var9 ? 0.272F : 0.85F));
                     float var19 = MathHelper.clamp(this.empiricalCurvePitch, -var17 * (var9 ? 0.224F : 0.72F), var17 * (var9 ? 0.224F : 0.72F));
                     float var20 = MathHelper.clamp(this.b(var2), -var17 * (var9 ? 0.176F : 0.78F), var17 * (var9 ? 0.176F : 0.78F));
                     this.a(var2, var9, var16, var17);
                     float var21 = this.e(var2) * (var9 ? 0.14400001F : 1.0F);
                     float var22 = (var9 ? 0.176F : (this.phase == FuntimeRotation.a.ACQUIRE ? 0.35F : 1.0F)) * this.noiseMotionBlend;
                     float var23 = MathHelper.clamp(var16 * 0.14F, 0.035F, 0.28F) * var22;
                     float var24 = MathHelper.clamp(var16 * 0.08F, 0.025F, 0.18F) * var22;
                     this.recoilYaw = this.recoilYaw * (float)Math.exp(-var7 / 0.075);
                     this.recoilPitch = this.recoilPitch * (float)Math.exp(-var7 / 0.09);
                     float var25 = var9 ? 0.52F : 1.0F;
                     var14 = MathHelper.wrapDegrees(
                        var14 + var18 + this.noiseYaw * var23 + this.tremorYaw * this.tremorBlend * var25 + this.lapseYaw * var21 + this.recoilYaw
                     );
                     var15 = MathHelper.clamp(
                        var15
                           + var19
                           + var20
                           + this.noisePitch * var24
                           + this.tremorPitch * this.tremorBlend * var25
                           + this.lapsePitch * var21
                           + this.recoilPitch,
                        -89.0F,
                        89.0F
                     );
                     this.a(var14, var15, var7);
                     float var26 = MathHelper.wrapDegrees(var14 - this.requestedYaw);
                     float var27 = var15 - this.requestedPitch;
                     float var28 = (float)Math.hypot(var26, var27);
                     if (var2 < this.reactionUntilNanos && !var9) {
                        this.phase = FuntimeRotation.a.REACTION;
                        this.yawVelocity = this.b(this.yawVelocity, 0.0F, this.yawDeceleration * (float)var7);
                        this.pitchVelocity = this.b(this.pitchVelocity, 0.0F, this.pitchDeceleration * (float)var7);
                     } else {
                        if (!var9) {
                           this.phase = var28 > 10.0F ? FuntimeRotation.a.ACQUIRE : FuntimeRotation.a.TRACK;
                        } else {
                           this.phase = var2 < this.precisionUntilNanos && (Math.abs(this.recoilYaw) > 0.01F || Math.abs(this.recoilPitch) > 0.01F)
                              ? FuntimeRotation.a.RECOVER
                              : FuntimeRotation.a.FOCUS;
                        }

                        this.a(var26, var27, var7, var9, mc.player.isGliding());
                     }

                     if (var10) {
                        this.yawVelocity = 0.0F;
                        this.pitchVelocity = 0.0F;
                        this.yawMotorAcceleration = 0.0F;
                        this.pitchMotorAcceleration = 0.0F;
                     }

                     float var29 = (float)Math.min(1.5, 1.0 + mc.player.getVelocity().horizontalLength() * 0.5);
                     if (!mc.player.isOnGround()) {
                        var29 = (float)(var29 * MathHelper.clamp(1.0 + mc.player.fallDistance * 0.015F, 1.0, 1.25));
                     }

                     double var30 = this.anchorPathEndNanos > this.anchorPathStartNanos
                        ? this.c((double)(var2 - this.anchorPathStartNanos) / (this.anchorPathEndNanos - this.anchorPathStartNanos), 0.0, 1.0)
                        : 0.5;
                     float var32 = (float)this.b(0.55F, 1.25, this.pathEase.a(var30, this)) * var29;
                     float var33 = var10 ? 0.0F : this.yawVelocity * (float)var7 * var32;
                     float var34 = var10 ? 0.0F : this.pitchVelocity * (float)var7 * var32;
                     if (var33 * var26 > 0.0F && Math.abs(var33) > Math.abs(var26)) {
                        if (Math.abs(this.wantedYawVelocity) > 8.0F) {
                           var33 = var26 + MathHelper.clamp(this.wantedYawVelocity * (float)var7 * 0.58F, -0.45F, 0.45F);
                           this.yawVelocity = this.wantedYawVelocity * 0.58F;
                           this.yawMotorAcceleration = 0.0F;
                        } else {
                           var33 = var26;
                           this.yawVelocity = 0.0F;
                           this.yawMotorAcceleration = 0.0F;
                        }
                     }

                     if (var34 * var27 > 0.0F && Math.abs(var34) > Math.abs(var27)) {
                        if (Math.abs(this.wantedPitchVelocity) > 6.0F) {
                           var34 = var27 + MathHelper.clamp(this.wantedPitchVelocity * (float)var7 * 0.52F, -0.32F, 0.32F);
                           this.pitchVelocity = this.wantedPitchVelocity * 0.52F;
                           this.pitchMotorAcceleration = 0.0F;
                        } else {
                           var34 = var27;
                           this.pitchVelocity = 0.0F;
                           this.pitchMotorAcceleration = 0.0F;
                        }
                     }

                     var33 = Math.min(Math.abs(var33), 60.0F) * Math.signum(var33);
                     var34 = Math.min(Math.abs(var34), 24.0F) * Math.signum(var34);
                     this.requestedYaw = MathHelper.wrapDegrees(this.requestedYaw + var33);
                     this.requestedPitch = MathHelper.clamp(this.requestedPitch + var34, -89.0F, 89.0F);
                     if (Float.isFinite(this.requestedYaw) && Float.isFinite(this.requestedPitch)) {
                        this.a(var2);
                     } else {
                        this.a(var2, true);
                     }
                  }
               }
            }
         }
      }
   }

   private void a(LivingEntity var1, long var2) {
      this.trackedTarget = var1;
      this.a(var2, true);
      this.nextMouseReportNanos = var2;
      this.lastMouseReportNanos = 0L;
      this.lastRotationSampleNanos = 0L;
      this.previousMouseReportMicros = 20150;
      this.secondPreviousMouseReportMicros = 20150;
      this.filteredVelocity = var1.getVelocity();
      this.anchorX = this.anchorGoalX = this.a(0.4, 0.6);
      this.anchorY = this.anchorGoalY = this.a(0.64, 0.82);
      this.anchorZ = this.anchorGoalZ = this.a(0.4, 0.6);
      this.anchorStartX = this.anchorControlOneX = this.anchorControlTwoX = this.anchorX;
      this.anchorStartY = this.anchorControlOneY = this.anchorControlTwoY = this.anchorY;
      this.anchorStartZ = this.anchorControlOneZ = this.anchorControlTwoZ = this.anchorZ;
      this.anchorMidX = this.anchorOvershootX = this.anchorX;
      this.anchorMidY = this.anchorOvershootY = this.anchorY;
      this.anchorMidZ = this.anchorOvershootZ = this.anchorZ;
      this.e();
      this.noiseYaw = this.noisePitch = 0.0F;
      this.noiseGoalYaw = this.noiseGoalPitch = 0.0F;
      this.recoilYaw = this.recoilPitch = 0.0F;
      this.tremorYaw = this.tremorPitch = 0.0F;
      this.tremorGoalYaw = this.tremorGoalPitch = 0.0F;
      this.tremorBlend = 0.0F;
      this.pathNoiseScale = this.d(0.03F, 0.09F);
      this.noiseMotionBlend = 0.1F;
      this.speedScale = this.speedGoal = this.d(0.86F, 1.16F);
      this.segmentSpeedScale = this.segmentSpeedGoal = 1.0F;
      this.perceptionLeadScale = this.perceptionLeadGoal = this.d(0.72F, 1.12F);
      this.perceptionLeadBias = this.perceptionLeadBiasGoal = this.d(-0.12F, 0.08F);
      this.lapseYaw = this.lapsePitch = 0.0F;
      this.hesitationScale = this.hesitationGoal = 1.0F;
      this.lapsePeakProgress = 0.28F;
      this.lapseRiseExponent = 2.4F;
      this.anchorPathVelocityX = this.anchorPathVelocityY = this.anchorPathVelocityZ = 0.0;
      this.lastAnchorSampleNanos = var2;
      this.maximumYawSpeed = this.d(610.0F, 830.0F);
      this.maximumPitchSpeed = this.d(280.0F, 410.0F);
      this.yawAcceleration = this.d(2800.0F, 5200.0F);
      this.pitchAcceleration = this.d(1200.0F, 2400.0F);
      this.yawDeceleration = this.d(3600.0F, 6500.0F);
      this.pitchDeceleration = this.d(1800.0F, 3400.0F);
      this.yawJerkLimit = this.d(36000.0F, 72000.0F);
      this.pitchJerkLimit = this.d(16000.0F, 36000.0F);
      float var4 = this.d(var1);
      long var5;
      if (!this.precisionRequested && !(var4 < 6.0F)) {
         var5 = Math.round(MathHelper.clamp(18.0F + var4 * 0.43F + (float)this.random.nextGaussian() * 7.0F, 16.0F, 105.0F));
      } else {
         var5 = Math.round(this.d(0.0F, 12.0F));
      }

      this.reactionUntilNanos = var2 + var5 * 1000000L;
      this.a(var2, this.precisionRequested, false);
      this.nextNoiseNanos = var2 + this.a(260L, 720L);
      this.nextTremorNanos = var2 + this.a(480L, 1200L);
      this.nextSpeedNanos = var2 + this.a(150L, 480L);
      this.nextPerceptionNanos = var2 + this.a(320L, 920L);
      this.nextLapseNanos = var2 + this.a(650L, 1900L);
      this.nextHesitationNanos = var2 + this.a(650L, 1900L);
      this.nextMotorPauseNanos = var2 + this.a(850L, 2600L);
      this.motorPauseUntilNanos = 0L;
      this.wasAirborne = !mc.player.isOnGround();
      this.phase = var5 > 0L ? FuntimeRotation.a.REACTION : FuntimeRotation.a.ACQUIRE;
   }

   private void a(long var1, double var3, boolean var5) {
      this.b(var3);
      this.c(var1);
      if (var1 >= this.nextAnchorNanos) {
         this.a(var1, var5, false);
         this.c(var1);
      }

      if (var1 >= this.nextNoiseNanos) {
         float var6 = this.f();
         this.noiseGoalYaw = MathHelper.clamp(this.f() * 0.78F + var6 * 0.22F, -1.0F, 1.0F);
         this.noiseGoalPitch = MathHelper.clamp(this.f() * 0.82F + var6 * 0.18F, -1.0F, 1.0F);
         this.nextNoiseNanos = var1 + this.a(260L, 760L);
      }

      double var18 = 1.0 - Math.exp(-var3 / 0.38);
      double var8 = 1.0 - Math.exp(-var3 / 0.48);
      this.noiseYaw = this.noiseYaw + (this.noiseGoalYaw - this.noiseYaw) * (float)var18;
      this.noisePitch = this.noisePitch + (this.noiseGoalPitch - this.noisePitch) * (float)var8;
      if (var1 >= this.nextSpeedNanos) {
         this.speedGoal = this.d(0.68F, 1.36F);
         this.nextSpeedNanos = var1 + this.a(150L, 520L);
      }

      double var10 = 1.0 - Math.exp(-var3 / 0.13);
      this.speedScale = this.speedScale + (this.speedGoal - this.speedScale) * (float)var10;
      double var12 = 1.0 - Math.exp(-var3 / 0.09);
      this.segmentSpeedScale = this.segmentSpeedScale + (this.segmentSpeedGoal - this.segmentSpeedScale) * (float)var12;
      if (var1 >= this.nextPerceptionNanos) {
         this.perceptionLeadGoal = this.d(0.48F, 1.24F);
         this.perceptionLeadBiasGoal = this.d(-0.22F, 0.13F);
         this.nextPerceptionNanos = var1 + this.a(340L, 1150L);
      }

      double var14 = 1.0 - Math.exp(-var3 / 0.22);
      this.perceptionLeadScale = this.perceptionLeadScale + (this.perceptionLeadGoal - this.perceptionLeadScale) * (float)var14;
      this.perceptionLeadBias = this.perceptionLeadBias + (this.perceptionLeadBiasGoal - this.perceptionLeadBias) * (float)var14;
      if (var5) {
         this.hesitationGoal = 1.0F;
         if (this.hesitationEndNanos > 0L) {
            this.hesitationEndNanos = 0L;
            this.nextHesitationNanos = var1 + this.a(700L, 2400L);
         }
      } else if (this.hesitationEndNanos > 0L && var1 >= this.hesitationEndNanos) {
         this.hesitationGoal = 1.0F;
         this.hesitationEndNanos = 0L;
         this.nextHesitationNanos = var1 + this.a(700L, 2400L);
      } else if (this.hesitationEndNanos == 0L && var1 >= this.nextHesitationNanos) {
         this.hesitationGoal = this.d(0.62F, 0.9F);
         this.hesitationEndNanos = var1 + this.a(20L, 85L);
      }

      double var16 = 1.0 - Math.exp(-var3 / (this.hesitationGoal < this.hesitationScale ? 0.022 : 0.045));
      this.hesitationScale = this.hesitationScale + (this.hesitationGoal - this.hesitationScale) * (float)var16;
      this.a(var1, var3);
   }

   private void b(double var1) {
      float var3;
      if (mc.player.isOnGround()) {
         var3 = 1.0F;
      } else {
         double var4 = mc.player.getVelocity().y;
         if (var4 > 0.04) {
            var3 = 0.82F;
         } else if (var4 > -0.1) {
            var3 = 0.74F;
         } else {
            var3 = MathHelper.clamp(1.0F + (float)Math.abs(var4) * 0.34F, 1.04F, 1.18F);
         }
      }

      float var6 = (float)(1.0 - Math.exp(-var1 / 0.065));
      this.airMotorScale = this.airMotorScale + (var3 - this.airMotorScale) * var6;
   }

   private Vec3d a(LivingEntity var1, double var2, boolean var4) {
      Vec3d var5 = var1.getVelocity();
      if (var5.lengthSquared() > 2.25) {
         var5 = var5.normalize().multiply(1.5);
      }

      double var6 = 1.0 - Math.exp(-var2 / 0.12);
      this.filteredVelocity = this.filteredVelocity.lerp(var5, var6);
      Box var8 = var1.getBoundingBox();
      Vec3d var9 = new Vec3d(
         var8.minX + var8.getLengthX() * this.anchorX, var8.minY + var8.getLengthY() * this.anchorY, var8.minZ + var8.getLengthZ() * this.anchorZ
      );
      double var10 = mc.player.getEyePos().distanceTo(var9);
      double var12;
      if (var4) {
         var12 = 0.05;
      } else if (!mc.player.isGliding() && !var1.isGliding()) {
         var12 = this.c((0.52 + var10 * 0.09) * this.perceptionLeadScale + this.perceptionLeadBias, -0.2, 1.35);
      } else {
         var12 = this.c((0.95 + var10 * 0.08) * this.perceptionLeadScale + this.perceptionLeadBias, 0.15, 1.85);
      }

      Vec3d var14 = var9.add(this.filteredVelocity.multiply(var12));
      if (!var4) {
         return var14;
      }

      double var15 = Math.min(var8.getLengthX(), var8.getLengthZ()) * 0.12;
      double var17 = var8.getLengthY() * 0.1;
      return new Vec3d(
         MathHelper.clamp(var14.x, var8.minX + var15, var8.maxX - var15),
         MathHelper.clamp(var14.y, var8.minY + var17, var8.maxY - var17),
         MathHelper.clamp(var14.z, var8.minZ + var15, var8.maxZ - var15)
      );
   }

   private void a(float var1, float var2, double var3, boolean var5, boolean var6) {
      float var7 = this.speedScale * this.segmentSpeedScale * this.hesitationScale * this.airMotorScale * (var6 ? 1.12F : 1.0F);
      if (var5) {
         var7 *= 1.272F;
      }

      float var8 = Math.abs(var1);
      float var9 = Math.abs(var2);
      float var10 = var6 ? 1100.0F : (var8 < 18.0F ? 520.0F : (var8 < 65.0F ? 720.0F : 980.0F));
      float var11 = var6 ? 520.0F : (var9 < 8.0F ? 230.0F : (var9 < 24.0F ? 330.0F : 430.0F));
      float var12 = Math.min(var10, this.maximumYawSpeed * var7);
      float var13 = Math.min(var11, this.maximumPitchSpeed * var7);
      float var14 = (float)Math.exp(-Math.abs(var1) / 18.0F);
      float var15 = (float)Math.exp(-Math.abs(var2) / 13.0F);
      float var16 = this.a(var1, this.yawDeceleration, var12) + this.wantedYawVelocity * (var5 ? 0.86F : 0.7F) * var14;
      float var17 = this.a(var2, this.pitchDeceleration, var13) + this.wantedPitchVelocity * (var5 ? 0.82F : 0.66F) * var15;
      float var28 = MathHelper.clamp(var16, -var12, var12);
      float var29 = MathHelper.clamp(var17, -var13, var13);
      float var18 = 1.0F + MathHelper.clamp((var8 - 35.0F) / 90.0F, 0.0F, 0.45F);
      float var19 = 1.0F + MathHelper.clamp((var9 - 18.0F) / 42.0F, 0.0F, 0.3F);
      float var20 = (this.b(this.yawVelocity, var28) ? this.yawDeceleration : this.yawAcceleration) * var18;
      float var21 = (this.b(this.pitchVelocity, var29) ? this.pitchDeceleration : this.pitchAcceleration) * var19;
      float var22 = var5 ? 1.3440001F : 1.0F;
      float var23 = var5 ? 1.32F : 1.0F;
      float var24 = MathHelper.clamp((var28 - this.yawVelocity) / (float)var3, -var20 * var22, var20 * var22);
      float var25 = MathHelper.clamp((var29 - this.pitchVelocity) / (float)var3, -var21 * var23, var21 * var23);
      this.yawMotorAcceleration = this.b(this.yawMotorAcceleration, var24, this.yawJerkLimit * var22 * (float)Math.sqrt(var18) * (float)var3);
      this.pitchMotorAcceleration = this.b(this.pitchMotorAcceleration, var25, this.pitchJerkLimit * var23 * (float)Math.sqrt(var19) * (float)var3);
      float var26 = this.yawVelocity + this.yawMotorAcceleration * (float)var3;
      float var27 = this.pitchVelocity + this.pitchMotorAcceleration * (float)var3;
      if ((var28 - this.yawVelocity) * (var28 - var26) <= 0.0F) {
         var26 = var28;
         this.yawMotorAcceleration *= 0.22F;
      }

      if ((var29 - this.pitchVelocity) * (var29 - var27) <= 0.0F) {
         var27 = var29;
         this.pitchMotorAcceleration *= 0.22F;
      }

      this.yawVelocity = MathHelper.clamp(var26, -var12, var12);
      this.pitchVelocity = MathHelper.clamp(var27, -var13, var13);
   }

   private void a(float var1, float var2, double var3) {
      if (!this.hasWantedRotation) {
         this.previousWantedYaw = var1;
         this.previousWantedPitch = var2;
         this.wantedYawVelocity = 0.0F;
         this.wantedPitchVelocity = 0.0F;
         this.hasWantedRotation = true;
      } else {
         float var5 = MathHelper.wrapDegrees(var1 - this.previousWantedYaw) / (float)var3;
         float var6 = (var2 - this.previousWantedPitch) / (float)var3;
         var5 = MathHelper.clamp(var5, -1100.0F, 1100.0F);
         var6 = MathHelper.clamp(var6, -620.0F, 620.0F);
         float var7 = (float)(1.0 - Math.exp(-var3 / 0.055));
         this.wantedYawVelocity = this.wantedYawVelocity + (var5 - this.wantedYawVelocity) * var7;
         this.wantedPitchVelocity = this.wantedPitchVelocity + (var6 - this.wantedPitchVelocity) * var7;
         this.previousWantedYaw = var1;
         this.previousWantedPitch = var2;
      }
   }

   private boolean b(float var1, float var2) {
      return Math.abs(var1) < 0.001F ? false : var1 * var2 <= 0.0F || Math.abs(var2) < Math.abs(var1);
   }

   private float a(float var1, float var2, float var3) {
      if (Math.abs(var1) < 0.001F) {
         return 0.0F;
      }

      float var4 = (float)Math.sqrt(2.0F * var2 * Math.abs(var1));
      return Math.copySign(Math.min(var3, var4), var1);
   }

   private void a(long var1) {
      long var3 = this.lastRotationSampleNanos == 0L ? Long.MAX_VALUE : var1 - this.lastRotationSampleNanos;
      this.lastRotationSampleNanos = var1;
      long var5 = this.lastMouseReportNanos == 0L ? Long.MAX_VALUE : var1 - this.lastMouseReportNanos;
      if (var3 >= 12500000L || var1 >= this.nextMouseReportNanos) {
         if (this.lastMouseReportNanos != 0L) {
            int var7 = (int)this.c(var5 / 1000.0, 12500.0, 35000.0);
            this.secondPreviousMouseReportMicros = this.previousMouseReportMicros;
            this.previousMouseReportMicros = var7;
         }

         int var17 = FuntimeMotorProfile.b(this.random);
         double var8 = 20150.0
            - (this.previousMouseReportMicros - 20150) * 0.19
            + (this.secondPreviousMouseReportMicros - 20150) * 0.275
            + (var17 - 20150) * 0.95
            + this.a(-260.0, 260.0);
         int var10 = (int)this.c(var8, 11500.0, 35000.0);
         double var11 = var3 < 12500000L ? var3 / 2000.0 : 0.0;
         long var13 = (long)Math.max(8000.0, var10 - var11);
         this.lastMouseReportNanos = var1;
         this.nextMouseReportNanos = var1 + var13 * 1000L;
         RotationStorage.a(new Rotation2(this.requestedYaw, this.requestedPitch), 360.0F, 360.0F, 45.0F, 45.0F, 0, 1, this.visibleAim);
         float var15 = Math.abs(MathHelper.wrapDegrees(this.requestedYaw - mc.player.getYaw()));
         float var16 = Math.abs(this.requestedPitch - mc.player.getPitch());
         if (var15 > 4.0F || var16 > 4.0F) {
            this.a(var1, true);
         }
      }
   }

   private void a(long var1, boolean var3) {
      if (mc.player != null) {
         this.requestedYaw = MathHelper.wrapDegrees(mc.player.getYaw());
         this.requestedPitch = MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F);
      }

      if (var3) {
         this.yawVelocity = 0.0F;
         this.pitchVelocity = 0.0F;
         this.yawMotorAcceleration = 0.0F;
         this.pitchMotorAcceleration = 0.0F;
         this.wantedYawVelocity = 0.0F;
         this.wantedPitchVelocity = 0.0F;
         this.hasWantedRotation = false;
         this.empiricalCurveYaw = 0.0F;
         this.empiricalCurvePitch = 0.0F;
         this.empiricalEntryYaw = 0.0F;
         this.empiricalEntryPitch = 0.0F;
      }

      this.lastUpdateNanos = var1;
   }

   private float d(LivingEntity var1) {
      Vec3d var2 = var1.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
      float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
      return Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw())) + Math.abs(var4 - mc.player.getPitch());
   }

   private void a(long var1, boolean var3, boolean var4) {
      boolean var5 = this.jumpPathRequested;
      this.jumpPathRequested = false;
      this.empiricalEntryYaw = this.empiricalCurveYaw;
      this.empiricalEntryPitch = this.empiricalCurvePitch;
      this.anchorStartX = this.anchorX;
      this.anchorStartY = this.anchorY;
      this.anchorStartZ = this.anchorZ;
      Vec3d var6 = this.c(var3);
      this.anchorGoalX = var6.x;
      this.anchorGoalY = var6.y;
      this.anchorGoalZ = var6.z;
      double var7 = this.a(this.anchorStartX, this.anchorStartY, this.anchorStartZ, this.anchorGoalX, this.anchorGoalY, this.anchorGoalZ);
      if (var5 && this.random.nextDouble() < 0.96) {
         this.pathModel = FuntimeRotation.c.EMPIRICAL;
         if (this.previousPathModel == FuntimeRotation.c.EMPIRICAL) {
            this.repeatedPathModels++;
         } else {
            this.previousPathModel = FuntimeRotation.c.EMPIRICAL;
            this.repeatedPathModels = 1;
         }
      } else {
         this.pathModel = this.a(var4, var7);
      }

      if (this.pathModel == FuntimeRotation.c.EMPIRICAL) {
         this.empiricalAction = var4;
         this.empiricalJump = var5;
         this.empiricalPathIndex = var5 ? FuntimeMotorProfile.a(this.random) : FuntimeMotorProfile.a(this.random, var4);
      } else {
         this.empiricalAction = false;
         this.empiricalJump = false;
      }

      this.pathTimeWarp = this.a(-0.28, 0.34);
      this.pathEase = this.d();
      this.pathEaseIn = this.a(1.65, 2.9);
      this.pathEaseOut = this.a(1.65, 2.9);
      this.pathFlickExponent = this.a(2.15, 3.75);
      this.pathNoiseScale = this.d(0.03F, 0.09F);
      double var9 = var3 ? this.a(0.025, 0.075) : this.a(0.045, 0.14);
      this.pathBendX = this.e(var9);
      this.pathBendY = this.e(var9 * 0.78);
      this.pathBendZ = this.e(var9);
      if (this.pathModel == FuntimeRotation.c.EMPIRICAL) {
         this.pathBendX = 0.0;
         this.pathBendY = 0.0;
         this.pathBendZ = 0.0;
         this.c();
      } else {
         this.empiricalPerpendicularYaw = 0.0F;
         this.empiricalPerpendicularPitch = 0.0F;
         this.empiricalAngularDistance = 0.0F;
      }

      this.anchorControlOneX = this.c(this.b(this.anchorStartX, this.anchorGoalX, 0.28) + this.e(var9), 0.14, 0.86);
      this.anchorControlOneY = this.c(this.b(this.anchorStartY, this.anchorGoalY, 0.28) + this.e(var9), 0.24, 0.9);
      this.anchorControlOneZ = this.c(this.b(this.anchorStartZ, this.anchorGoalZ, 0.28) + this.e(var9), 0.14, 0.86);
      double var11 = this.a(0.36, 0.67);
      this.anchorMidX = this.c(this.b(this.anchorStartX, this.anchorGoalX, var11) + this.pathBendX, 0.14, 0.86);
      this.anchorMidY = this.c(this.b(this.anchorStartY, this.anchorGoalY, var11) + this.pathBendY, 0.24, 0.9);
      this.anchorMidZ = this.c(this.b(this.anchorStartZ, this.anchorGoalZ, var11) + this.pathBendZ, 0.14, 0.86);
      this.anchorControlTwoX = this.c(this.anchorMidX + this.anchorMidX - this.anchorControlOneX, 0.14, 0.86);
      this.anchorControlTwoY = this.c(this.anchorMidY + this.anchorMidY - this.anchorControlOneY, 0.24, 0.9);
      this.anchorControlTwoZ = this.c(this.anchorMidZ + this.anchorMidZ - this.anchorControlOneZ, 0.14, 0.86);
      double var13 = this.pathModel == FuntimeRotation.c.FLICK_SETTLE ? this.a(0.025, 0.085) : this.a(0.008, 0.045);
      this.anchorOvershootX = this.c(this.anchorGoalX + (this.anchorGoalX - this.anchorStartX) * var13 + this.e(var9 * 0.32), 0.16, 0.84);
      this.anchorOvershootY = this.c(this.anchorGoalY + (this.anchorGoalY - this.anchorStartY) * var13 + this.e(var9 * 0.22), 0.27, 0.88);
      this.anchorOvershootZ = this.c(this.anchorGoalZ + (this.anchorGoalZ - this.anchorStartZ) * var13 + this.e(var9 * 0.32), 0.16, 0.84);
      if (this.pathModel == FuntimeRotation.c.EMPIRICAL) {
         this.pathPrimaryEnd = 0.75;
         this.pathCorrectionStart = 0.58;
         this.segmentSpeedGoal = this.d(0.9F, 1.12F);
      } else if (this.pathModel == FuntimeRotation.c.FLICK_SETTLE) {
         this.pathPrimaryEnd = this.a(0.42, 0.6);
         this.pathCorrectionStart = this.a(0.31, 0.48);
         this.segmentSpeedGoal = this.d(1.3F, 1.72F);
      } else if (this.pathModel == FuntimeRotation.c.CORRECTIVE) {
         this.pathPrimaryEnd = this.a(0.7, 0.86);
         this.pathCorrectionStart = this.a(0.52, 0.69);
         this.segmentSpeedGoal = this.d(0.78F, 1.24F);
      } else {
         this.pathPrimaryEnd = 0.75;
         this.pathCorrectionStart = 0.58;
         this.segmentSpeedGoal = this.d(0.74F, 1.3F);
      }

      double var15 = this.b(this.anchorStartX, this.anchorStartY, this.anchorStartZ, this.anchorGoalX, this.anchorGoalY, this.anchorGoalZ);
      double var17 = this.c(0.72 + var15 * 0.9, 0.78, 1.28);
      long var19;
      if (this.pathModel == FuntimeRotation.c.EMPIRICAL) {
         long var21 = this.empiricalJump
            ? FuntimeMotorProfile.a(this.empiricalPathIndex)
            : FuntimeMotorProfile.a(this.empiricalAction, this.empiricalPathIndex);
         var19 = (long)(var21 * var17 * this.a(0.92, 1.08));
      } else {
         long var27;
         long var23;
         if (this.pathModel == FuntimeRotation.c.FLICK_SETTLE) {
            var27 = 85L;
            var23 = 190L;
         } else if (this.pathModel == FuntimeRotation.c.RANDOM_SPLINE) {
            var27 = 170L;
            var23 = 390L;
         } else if (this.pathModel == FuntimeRotation.c.CORRECTIVE) {
            var27 = 145L;
            var23 = 350L;
         } else {
            var27 = 110L;
            var23 = 290L;
         }

         var19 = (long)(this.a((double)var27, (double)var23) * var17);
      }

      if (var3 && this.pathModel != FuntimeRotation.c.EMPIRICAL) {
         var19 = Math.min(var19, var4 ? 245L : 310L);
      }

      double var28 = var19 / 1000.0;
      this.pathEntryX = this.c(this.anchorPathVelocityX * var28 * 0.24, -0.12, 0.12);
      this.pathEntryY = this.c(this.anchorPathVelocityY * var28 * 0.24, -0.09, 0.09);
      this.pathEntryZ = this.c(this.anchorPathVelocityZ * var28 * 0.24, -0.12, 0.12);
      this.anchorPathStartNanos = var1;
      this.anchorPathEndNanos = var1 + var19 * 1000000L;
      double var29 = this.empiricalJump ? 1.0 : (var3 ? (var4 ? 0.9 : 0.78) : (var4 ? 0.93 : 0.84));
      if (this.random.nextDouble() < var29) {
         double var25 = this.pathModel == FuntimeRotation.c.EMPIRICAL
            ? (this.empiricalJump ? this.a(0.92, 0.985) : (var4 ? this.a(0.78, 0.94) : this.a(0.76, 0.93)))
            : (var4 ? this.a(0.55, 0.84) : this.a(0.62, 0.89));
         this.nextAnchorNanos = this.anchorPathStartNanos + (long)((this.anchorPathEndNanos - this.anchorPathStartNanos) * var25);
      } else {
         long var30 = this.random.nextDouble() < 0.08 ? this.a(140L, 620L) : (var3 ? this.a(17L, 88L) : this.a(20L, 125L));
         this.nextAnchorNanos = this.anchorPathEndNanos + var30;
      }
   }

   private void b(long var1, boolean var3) {
      if (var1 - this.lastActionPathNanos >= 30000000L) {
         this.lastActionPathNanos = var1;
         this.c(var1);
         this.a(var1, var3, true);
      }
   }

   private void c(long var1, boolean var3) {
      this.c(var1);
      this.jumpPathRequested = true;
      this.jumpCriticalPathPending = true;
      this.a(var1, var3, false);
      this.jumpGestureStartNanos = var1;
      this.jumpGestureEndNanos = var1 + this.a(435L, 525L);
      double var4 = this.random.nextDouble();
      if (var4 < 0.52) {
         this.jumpPitchAmplitude = this.d(6.0F, 18.0F);
      } else if (var4 < 0.75) {
         this.jumpPitchAmplitude = this.d(1.5F, 6.0F);
      } else if (var4 < 0.9) {
         this.jumpPitchAmplitude = 0.0F;
      } else {
         this.jumpPitchAmplitude = -this.d(1.5F, 6.0F);
      }
   }

   private float b(long var1) {
      if (this.jumpGestureEndNanos > this.jumpGestureStartNanos
         && var1 > this.jumpGestureStartNanos
         && var1 < this.jumpGestureEndNanos
         && this.jumpPitchAmplitude != 0.0F) {
         double var3 = (double)(var1 - this.jumpGestureStartNanos) / (this.jumpGestureEndNanos - this.jumpGestureStartNanos);
         double var5;
         if (var3 < 0.2) {
            var5 = this.a(var3 / 0.2) * 0.58;
         } else if (var3 < 0.44) {
            var5 = this.b(0.58, 1.0, this.a((var3 - 0.2) / 0.24));
         } else if (var3 < 0.63) {
            var5 = this.b(1.0, 0.84, this.a((var3 - 0.44) / 0.19));
         } else if (var3 < 0.92) {
            var5 = this.b(0.84, 0.0, this.a((var3 - 0.63) / 0.29));
         } else {
            var5 = 0.0;
         }

         return -this.jumpPitchAmplitude * (float)var5;
      } else {
         return 0.0F;
      }
   }

   private void c() {
      Vec3d var1 = this.a(this.anchorStartX, this.anchorStartY, this.anchorStartZ);
      Vec3d var2 = this.a(this.anchorGoalX, this.anchorGoalY, this.anchorGoalZ);
      float var3 = MathHelper.wrapDegrees((float)(var2.x - var1.x));
      float var4 = (float)(var2.y - var1.y);
      float var5 = (float)Math.hypot(var3, var4);
      if (var5 < 0.001F) {
         this.empiricalPerpendicularYaw = 0.0F;
         this.empiricalPerpendicularPitch = 0.0F;
         this.empiricalAngularDistance = 0.0F;
      } else {
         this.empiricalPerpendicularYaw = -var4 / var5;
         this.empiricalPerpendicularPitch = var3 / var5;
         this.empiricalAngularDistance = Math.min(var5, 18.0F);
      }
   }

   private Vec3d a(double var1, double var3, double var5) {
      if (this.trackedTarget != null && mc.player != null) {
         Box var7 = this.trackedTarget.getBoundingBox();
         Vec3d var8 = new Vec3d(var7.minX + var7.getLengthX() * var1, var7.minY + var7.getLengthY() * var3, var7.minZ + var7.getLengthZ() * var5);
         Vec3d var9 = var8.subtract(mc.player.getEyePos());
         double var10 = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var9.z, var9.x)) - 90.0);
         double var12 = -Math.toDegrees(Math.atan2(var9.y, var9.horizontalLength()));
         return new Vec3d(var10, var12, 0.0);
      } else {
         return Vec3d.ZERO;
      }
   }

   private void c(long var1) {
      if (this.anchorPathEndNanos > this.anchorPathStartNanos) {
         double var3 = this.c((double)(var1 - this.anchorPathStartNanos) / (this.anchorPathEndNanos - this.anchorPathStartNanos), 0.0, 1.0);
         double var5 = this.anchorX;
         double var7 = this.anchorY;
         double var9 = this.anchorZ;
         double var11 = this.c(var3 + this.pathTimeWarp * var3 * (1.0 - var3), 0.0, 1.0);
         if (this.pathModel == FuntimeRotation.c.EMPIRICAL) {
            double var13 = this.pathEase.a(var11, this);
            double var15 = this.empiricalJump ? this.b(var3, var13, 0.38) : var13;
            double var17 = this.empiricalJump
               ? FuntimeMotorProfile.a(this.empiricalPathIndex, var15)
               : FuntimeMotorProfile.a(this.empiricalAction, this.empiricalPathIndex, var15);
            this.anchorX = this.b(this.anchorStartX, this.anchorGoalX, var17);
            this.anchorY = this.b(this.anchorStartY, this.anchorGoalY, var17);
            this.anchorZ = this.b(this.anchorStartZ, this.anchorGoalZ, var17);
            double var19 = this.empiricalJump
               ? FuntimeMotorProfile.b(this.empiricalPathIndex, var15)
               : FuntimeMotorProfile.b(this.empiricalAction, this.empiricalPathIndex, var15);
            float var21 = (float)(1.0 - this.a(var15));
            this.empiricalCurveYaw = this.empiricalEntryYaw * var21 + this.empiricalPerpendicularYaw * this.empiricalAngularDistance * (float)var19;
            this.empiricalCurvePitch = this.empiricalEntryPitch * var21 + this.empiricalPerpendicularPitch * this.empiricalAngularDistance * (float)var19;
         } else if (this.pathModel == FuntimeRotation.c.RANDOM_SPLINE) {
            double var22 = this.pathEase.a(var11, this);
            if (var22 < 0.5) {
               double var26 = var22 * 2.0;
               this.anchorX = this.c(this.anchorStartX, this.anchorControlOneX, this.anchorMidX, var26);
               this.anchorY = this.c(this.anchorStartY, this.anchorControlOneY, this.anchorMidY, var26);
               this.anchorZ = this.c(this.anchorStartZ, this.anchorControlOneZ, this.anchorMidZ, var26);
            } else {
               double var27 = (var22 - 0.5) * 2.0;
               this.anchorX = this.c(this.anchorMidX, this.anchorControlTwoX, this.anchorGoalX, var27);
               this.anchorY = this.c(this.anchorMidY, this.anchorControlTwoY, this.anchorGoalY, var27);
               this.anchorZ = this.c(this.anchorMidZ, this.anchorControlTwoZ, this.anchorGoalZ, var27);
            }
         } else if (this.pathModel == FuntimeRotation.c.FLICK_SETTLE) {
            this.anchorX = this.b(this.anchorStartX, this.anchorOvershootX, this.anchorGoalX, var11);
            this.anchorY = this.b(this.anchorStartY, this.anchorOvershootY, this.anchorGoalY, var11);
            this.anchorZ = this.b(this.anchorStartZ, this.anchorOvershootZ, this.anchorGoalZ, var11);
         } else if (this.pathModel == FuntimeRotation.c.CORRECTIVE) {
            this.anchorX = this.a(this.anchorStartX, this.anchorOvershootX, this.anchorGoalX, var11);
            this.anchorY = this.a(this.anchorStartY, this.anchorOvershootY, this.anchorGoalY, var11);
            this.anchorZ = this.a(this.anchorStartZ, this.anchorOvershootZ, this.anchorGoalZ, var11);
         } else {
            double var23 = this.pathEase.a(var11, this);
            double var28 = 16.0 * var23 * var23 * (1.0 - var23) * (1.0 - var23);
            this.anchorX = this.b(this.anchorStartX, this.anchorGoalX, var23) + this.pathBendX * var28;
            this.anchorY = this.b(this.anchorStartY, this.anchorGoalY, var23) + this.pathBendY * var28;
            this.anchorZ = this.b(this.anchorStartZ, this.anchorGoalZ, var23) + this.pathBendZ * var28;
         }

         if (this.pathModel != FuntimeRotation.c.EMPIRICAL) {
            float var24 = (float)(1.0 - this.a(var3));
            this.empiricalCurveYaw = this.empiricalEntryYaw * var24;
            this.empiricalCurvePitch = this.empiricalEntryPitch * var24;
         }

         double var25 = 4.0 * var3 * Math.pow(1.0 - var3, 3.0);
         this.anchorX = this.anchorX + this.pathEntryX * var25;
         this.anchorY = this.anchorY + this.pathEntryY * var25;
         this.anchorZ = this.anchorZ + this.pathEntryZ * var25;
         this.anchorX = this.c(this.anchorX, 0.12, 0.88);
         this.anchorY = this.c(this.anchorY, 0.22, 0.92);
         this.anchorZ = this.c(this.anchorZ, 0.12, 0.88);
         if (this.lastAnchorSampleNanos > 0L && var1 > this.lastAnchorSampleNanos) {
            double var29 = (var1 - this.lastAnchorSampleNanos) / 1.0E9;
            if (var29 <= 0.1) {
               double var30 = 1.0 - Math.exp(-var29 / 0.045);
               this.anchorPathVelocityX = this.anchorPathVelocityX + ((this.anchorX - var5) / var29 - this.anchorPathVelocityX) * var30;
               this.anchorPathVelocityY = this.anchorPathVelocityY + ((this.anchorY - var7) / var29 - this.anchorPathVelocityY) * var30;
               this.anchorPathVelocityZ = this.anchorPathVelocityZ + ((this.anchorZ - var9) / var29 - this.anchorPathVelocityZ) * var30;
            }
         }

         this.lastAnchorSampleNanos = var1;
      }
   }

   private void a(long var1, double var3) {
      Vec3d var5 = mc.player.getVelocity();
      boolean var6 = mc.player.isOnGround() && var5.x * var5.x + var5.z * var5.z < 0.0025;
      boolean var7 = this.d(var1);
      if (var6 && !var7) {
         if (this.tremorPulseEndNanos > 0L && var1 >= this.tremorPulseEndNanos) {
            this.tremorGoalYaw = 0.0F;
            this.tremorGoalPitch = 0.0F;
            this.tremorPulseEndNanos = 0L;
            this.nextTremorNanos = var1 + this.a(480L, 1400L);
         }

         if (this.tremorPulseEndNanos == 0L && var1 >= this.nextTremorNanos) {
            float var8 = Math.max(0.001F, GCDUtil2.b());
            int var9 = this.random.nextDouble() < 0.94 ? 1 : 2;
            int var10 = this.random.nextDouble() < 0.78 ? 0 : 1;
            float var11 = this.random.nextBoolean() ? 1.0F : -1.0F;
            float var12 = this.random.nextBoolean() ? 1.0F : -1.0F;
            this.tremorGoalYaw = var9 * var8 * var11;
            this.tremorGoalPitch = var10 * var8 * var12;
            this.tremorPulseEndNanos = var1 + this.a(18L, 44L);
         }
      } else {
         this.tremorGoalYaw = 0.0F;
         this.tremorGoalPitch = 0.0F;
         this.tremorPulseEndNanos = 0L;
         if (var1 >= this.nextTremorNanos) {
            this.nextTremorNanos = var1 + this.a(480L, 1200L);
         }
      }

      boolean var17 = this.tremorPulseEndNanos > var1;
      double var18 = 1.0 - Math.exp(-var3 / (var17 ? 0.018 : 0.035));
      this.tremorYaw = this.tremorYaw + (this.tremorGoalYaw - this.tremorYaw) * (float)var18;
      this.tremorPitch = this.tremorPitch + (this.tremorGoalPitch - this.tremorPitch) * (float)var18;
      float var19 = var6 && !var7 ? 1.0F : 0.0F;
      double var20 = 1.0 - Math.exp(-var3 / (var19 > this.tremorBlend ? 0.08 : 0.045));
      this.tremorBlend = this.tremorBlend + (var19 - this.tremorBlend) * (float)var20;
      double var14 = 1.0 - Math.exp(-var3 / (var7 ? 0.12 : 0.3));
      float var16 = var7 ? this.pathNoiseScale : 0.1F;
      this.noiseMotionBlend = this.noiseMotionBlend + (var16 - this.noiseMotionBlend) * (float)var14;
   }

   private boolean d(long var1) {
      double var3 = Math.sqrt(
         this.anchorPathVelocityX * this.anchorPathVelocityX
            + this.anchorPathVelocityY * this.anchorPathVelocityY
            + this.anchorPathVelocityZ * this.anchorPathVelocityZ
      );
      double var5 = Math.hypot(this.yawVelocity, this.pitchVelocity);
      return var1 < this.anchorPathEndNanos && var3 > 0.12 || var5 > 24.0;
   }

   private boolean d(long var1, boolean var3) {
      if (var3) {
         this.motorPauseUntilNanos = 0L;
         if (var1 >= this.nextMotorPauseNanos) {
            this.nextMotorPauseNanos = var1 + this.a(650L, 1800L);
         }

         return false;
      } else {
         if (var1 < this.motorPauseUntilNanos) {
            return true;
         }

         if (var1 < this.nextMotorPauseNanos) {
            return false;
         }

         double var4 = this.random.nextDouble();
         long var6 = var4 < 0.65 ? this.a(17L, 43L) : (var4 < 0.92 ? this.a(44L, 90L) : (var4 < 0.985 ? this.a(91L, 190L) : this.a(220L, 620L)));
         this.motorPauseUntilNanos = var1 + var6;
         this.nextMotorPauseNanos = this.motorPauseUntilNanos + this.a(750L, 2400L);
         return true;
      }
   }

   private void a(long var1, boolean var3, float var4, float var5) {
      if (var1 >= this.nextLapseNanos && var1 >= this.lapseEndNanos) {
         if (var3) {
            this.nextLapseNanos = var1 + this.a(260L, 720L);
         } else {
            float var6 = MathHelper.clamp(var4 * this.d(0.78F, 1.24F), 0.85F, 12.0F);
            float var7 = MathHelper.clamp(var5 * this.d(0.58F, 0.96F), 1.2F, 16.0F);
            float var8 = this.random.nextBoolean() ? 1.0F : -1.0F;
            float var9 = this.random.nextBoolean() ? 1.0F : -1.0F;
            if (this.trackedTarget != null && mc.player != null) {
               Vec3d var10 = this.trackedTarget.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
               float var11 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0);
               float var12 = (float)(-Math.toDegrees(Math.atan2(var10.y, var10.horizontalLength())));
               float var13 = MathHelper.wrapDegrees(this.requestedYaw - var11);
               float var14 = this.requestedPitch - var12;
               if (Math.abs(var13) > 0.15F) {
                  var8 = Math.signum(var13);
               }

               if (Math.abs(var14) > 0.15F) {
                  var9 = Math.signum(var14);
               }
            }

            double var15 = this.random.nextDouble();
            if (var15 < 0.46) {
               this.lapseYaw = var8 * var6;
               this.lapsePitch = this.c(0.08F, 0.24F) * var7;
            } else if (var15 < 0.72) {
               this.lapseYaw = this.c(0.12F, 0.34F) * var6;
               this.lapsePitch = var9 * var7;
            } else {
               this.lapseYaw = var8 * var6 * this.d(0.66F, 0.96F);
               this.lapsePitch = var9 * var7 * this.d(0.42F, 0.76F);
            }

            this.lapsePeakProgress = this.d(0.18F, 0.38F);
            this.lapseRiseExponent = this.d(2.0F, 2.8F);
            this.lapseStartNanos = var1;
            this.lapseEndNanos = var1 + this.a(125L, 340L);
            this.nextLapseNanos = this.lapseEndNanos + this.a(900L, 3400L);
         }
      }
   }

   private float e(long var1) {
      if (this.lapseEndNanos > this.lapseStartNanos && var1 >= this.lapseStartNanos && var1 < this.lapseEndNanos) {
         double var3 = this.c((double)(var1 - this.lapseStartNanos) / (this.lapseEndNanos - this.lapseStartNanos), 0.0, 1.0);
         if (var3 < this.lapsePeakProgress) {
            double var7 = var3 / this.lapsePeakProgress;
            return (float)(1.0 - Math.pow(1.0 - var7, this.lapseRiseExponent));
         } else {
            double var5 = (var3 - this.lapsePeakProgress) / (1.0 - this.lapsePeakProgress);
            return (float)(1.0 - this.a(var5));
         }
      } else {
         return 0.0F;
      }
   }

   private FuntimeRotation.c a(boolean var1, double var2) {
      FuntimeRotation.c var4 = this.previousPathModel;

      for (int var5 = 0; var5 < 8; var5++) {
         double var6 = this.random.nextDouble();
         if (var1) {
            var4 = var6 < 0.9
               ? FuntimeRotation.c.EMPIRICAL
               : (
                  var6 < 0.93
                     ? FuntimeRotation.c.MINIMUM_JERK
                     : (
                        var6 < 0.97
                           ? FuntimeRotation.c.CORRECTIVE
                           : (var6 < 0.99 ? FuntimeRotation.c.RANDOM_SPLINE : (var2 >= 2.8 ? FuntimeRotation.c.FLICK_SETTLE : FuntimeRotation.c.CORRECTIVE))
                     )
               );
         } else {
            var4 = var6 < 0.92
               ? FuntimeRotation.c.EMPIRICAL
               : (var6 < 0.95 ? FuntimeRotation.c.MINIMUM_JERK : (var6 < 0.99 ? FuntimeRotation.c.CORRECTIVE : FuntimeRotation.c.RANDOM_SPLINE));
         }

         if ((var4 != FuntimeRotation.c.FLICK_SETTLE || this.previousPathModel != FuntimeRotation.c.FLICK_SETTLE)
            && (var4 != this.previousPathModel || this.repeatedPathModels < 2 || var4 == FuntimeRotation.c.EMPIRICAL)) {
            break;
         }
      }

      if (var4 == this.previousPathModel) {
         this.repeatedPathModels++;
      } else {
         this.previousPathModel = var4;
         this.repeatedPathModels = 1;
      }

      return var4;
   }

   private FuntimeRotation.b d() {
      FuntimeRotation.b[] var1 = FuntimeRotation.b.values();
      FuntimeRotation.b var2 = this.previousPathEase;

      for (int var3 = 0; var3 < 6 && var2 == this.previousPathEase; var3++) {
         var2 = var1[this.random.nextInt(var1.length)];
      }

      this.previousPathEase = var2;
      return var2;
   }

   private Vec3d c(boolean var1) {
      int var2 = var1 ? 44 : 56;
      Vec3d[] var3 = new Vec3d[var2];
      int[] var4 = new int[var2];
      double[] var5 = new double[var2];
      double[] var6 = new double[var2];
      double[] var7 = new double[var2];
      double var8 = var1 ? 0.36 : 0.48;
      double var10 = var1 ? 0.135 : 0.19;

      for (int var12 = 0; var12 < var2; var12++) {
         Vec3d var13 = this.d(var1);
         int var14 = this.a(var13);
         double var15 = this.aimHistorySize == 0 ? this.a(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ) : Double.MAX_VALUE;
         double var17 = this.aimHistorySize == 0 ? this.b(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ) : Double.MAX_VALUE;
         double var19 = 0.0;

         for (int var21 = 0; var21 < this.aimHistorySize; var21++) {
            int var22 = (this.aimHistoryCursor - 1 - var21 + 12) % 12;
            var15 = Math.min(var15, this.a(var13.x, var13.y, var13.z, this.aimHistoryX[var22], this.aimHistoryY[var22], this.aimHistoryZ[var22]));
            var17 = Math.min(var17, this.b(var13.x, var13.y, var13.z, this.aimHistoryX[var22], this.aimHistoryY[var22], this.aimHistoryZ[var22]));
            if (this.aimHistoryZone[var22] == var14 && var21 < 6) {
               var19 += 0.08 - var21 * 0.01;
            }
         }

         double var27 = this.a(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ);
         double var23 = var15 * 1.35 + var17 * 1.1 + var27 * 0.42 + this.random.nextDouble() * 0.08 - var19;
         if (var14 == this.lastAimZone) {
            var23 -= 0.28;
         }

         if (var14 == this.previousAimZone) {
            var23 -= 0.12;
         }

         var3[var12] = var13;
         var4[var12] = var14;
         var5[var12] = var23;
         var6[var12] = var15;
         var7[var12] = var17;
      }

      int var25 = this.a(var5, var6, var7, var8, var10);
      if (var25 < 0) {
         var25 = this.a(var5, var6, var7, var8 * 0.68, var10 * 0.7);
      }

      if (var25 < 0) {
         var25 = this.a(var5, var6, var7, 0.24, 0.075);
      }

      if (var25 < 0) {
         return new Vec3d(this.anchorX, this.anchorY, this.anchorZ);
      }

      Vec3d var26 = var3[var25];
      this.a(var26, var4[var25]);
      return var26;
   }

   private int a(double[] var1, double[] var2, double[] var3, double var4, double var6) {
      double var8 = 1.1;
      double var10 = -Double.MAX_VALUE;

      for (int var12 = 0; var12 < var1.length; var12++) {
         if (!(var2[var12] < var4) && !(var3[var12] < var6)) {
            var10 = Math.max(var10, var1[var12]);
         }
      }

      if (var10 == -Double.MAX_VALUE) {
         return -1;
      }

      double var17 = 0.0;

      for (int var14 = 0; var14 < var1.length; var14++) {
         if (!(var2[var14] < var4) && !(var3[var14] < var6) && !(var1[var14] < var10 - var8)) {
            var17 += 0.16 + Math.exp((var1[var14] - var10) * 1.15);
         }
      }

      if (var17 <= 0.0) {
         return -1;
      }

      double var18 = this.random.nextDouble() * var17;

      for (int var16 = 0; var16 < var1.length; var16++) {
         if (!(var2[var16] < var4) && !(var3[var16] < var6) && !(var1[var16] < var10 - var8)) {
            var18 -= 0.16 + Math.exp((var1[var16] - var10) * 1.15);
            if (var18 <= 0.0) {
               return var16;
            }
         }
      }

      return -1;
   }

   private Vec3d d(boolean var1) {
      int var2 = this.random.nextInt(8);
      double var3 = (var2 + this.a(0.1, 0.9)) * Math.PI / 4.0;
      double var5 = var1 ? this.a(0.16, 0.34) : this.a(0.18, 0.39);
      double var7 = this.random.nextDouble();
      double var9;
      double var11;
      if (var1) {
         if (var7 < 0.1) {
            var9 = 0.36;
            var11 = 0.47;
         } else if (var7 < 0.37) {
            var9 = 0.47;
            var11 = 0.68;
         } else if (var7 < 0.89) {
            var9 = 0.68;
            var11 = 0.86;
         } else {
            var9 = 0.86;
            var11 = 0.9;
         }
      } else if (var7 < 0.15) {
         var9 = 0.29;
         var11 = 0.47;
      } else if (var7 < 0.43) {
         var9 = 0.47;
         var11 = 0.67;
      } else if (var7 < 0.89) {
         var9 = 0.67;
         var11 = 0.86;
      } else {
         var9 = 0.86;
         var11 = 0.92;
      }

      double var13 = 0.5 + Math.cos(var3) * var5;
      double var15 = this.a(var9, var11);
      double var17 = 0.5 + Math.sin(var3) * var5;
      if (var1) {
         var13 = this.c(var13, 0.16, 0.84);
         var15 = this.c(var15, 0.36, 0.9);
         var17 = this.c(var17, 0.16, 0.84);
      } else {
         var13 = this.c(var13, 0.12, 0.88);
         var15 = this.c(var15, 0.29, 0.92);
         var17 = this.c(var17, 0.12, 0.88);
      }

      return new Vec3d(var13, var15, var17);
   }

   private int a(Vec3d var1) {
      int var2 = var1.y < 0.49 ? 0 : (var1.y < 0.68 ? 1 : 2);
      double var3 = Math.atan2(var1.z - 0.5, var1.x - 0.5);
      if (var3 < 0.0) {
         var3 += Math.PI * 2;
      }

      int var5 = Math.min(7, (int)(var3 / (Math.PI / 4)));
      return var2 * 8 + var5;
   }

   private void a(Vec3d var1, int var2) {
      this.aimHistoryX[this.aimHistoryCursor] = var1.x;
      this.aimHistoryY[this.aimHistoryCursor] = var1.y;
      this.aimHistoryZ[this.aimHistoryCursor] = var1.z;
      this.aimHistoryZone[this.aimHistoryCursor] = var2;
      this.aimHistoryCursor = (this.aimHistoryCursor + 1) % 12;
      this.aimHistorySize = Math.min(this.aimHistorySize + 1, 12);
      this.previousAimZone = this.lastAimZone;
      this.lastAimZone = var2;
   }

   private void e() {
      this.aimHistorySize = 0;
      this.aimHistoryCursor = 0;
      this.lastAimZone = -1;
      this.previousAimZone = -1;

      for (int var1 = 0; var1 < 12; var1++) {
         this.aimHistoryX[var1] = 0.0;
         this.aimHistoryY[var1] = 0.0;
         this.aimHistoryZ[var1] = 0.0;
         this.aimHistoryZone[var1] = -1;
      }
   }

   private double a(double var1, double var3, double var5, double var7, double var9, double var11) {
      if (this.trackedTarget != null && mc.player != null) {
         Box var13 = this.trackedTarget.getBoundingBox();
         Vec3d var14 = mc.player.getEyePos();
         double var15 = var13.minX + var13.getLengthX() * var1 - var14.x;
         double var17 = var13.minY + var13.getLengthY() * var3 - var14.y;
         double var19 = var13.minZ + var13.getLengthZ() * var5 - var14.z;
         double var21 = var13.minX + var13.getLengthX() * var7 - var14.x;
         double var23 = var13.minY + var13.getLengthY() * var9 - var14.y;
         double var25 = var13.minZ + var13.getLengthZ() * var11 - var14.z;
         float var27 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var19, var15)) - 90.0);
         float var28 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var25, var21)) - 90.0);
         float var29 = (float)(-Math.toDegrees(Math.atan2(var17, Math.hypot(var15, var19))));
         float var30 = (float)(-Math.toDegrees(Math.atan2(var23, Math.hypot(var21, var25))));
         double var31 = MathHelper.wrapDegrees(var27 - var28);
         double var33 = var29 - var30;
         return Math.hypot(var31, var33);
      } else {
         return this.b(var1, var3, var5, var7, var9, var11);
      }
   }

   private double b(double var1, double var3, double var5, double var7, double var9, double var11) {
      double var13 = var1 - var7;
      double var15 = (var3 - var9) * 1.25;
      double var17 = var5 - var11;
      return Math.sqrt(var13 * var13 + var15 * var15 + var17 * var17);
   }

   private double a(double var1, double var3, double var5, double var7) {
      double var9 = this.d(this.c(var7 / this.pathPrimaryEnd, 0.0, 1.0));
      double var11 = this.c(this.c((var7 - this.pathCorrectionStart) / (1.0 - this.pathCorrectionStart), 0.0, 1.0));
      return this.b(var1, var3, var9) + (var5 - var3) * var11;
   }

   private double b(double var1, double var3, double var5, double var7) {
      double var9 = this.c(var7 / this.pathPrimaryEnd, 0.0, 1.0);
      double var11 = 1.0 - Math.pow(1.0 - var9, this.pathFlickExponent);
      double var13 = this.a(this.c((var7 - this.pathCorrectionStart) / (1.0 - this.pathCorrectionStart), 0.0, 1.0));
      return this.b(var1, var3, var11) + (var5 - var3) * var13;
   }

   private double c(double var1) {
      if (var1 <= 0.0) {
         return 0.0;
      }

      if (var1 >= 1.0) {
         return 1.0;
      }

      double var3 = Math.pow(var1, this.pathEaseIn);
      double var5 = Math.pow(1.0 - var1, this.pathEaseOut);
      return var3 / (var3 + var5);
   }

   private double d(double var1) {
      return var1 - Math.sin((Math.PI * 2) * var1) / (Math.PI * 2);
   }

   public double a(double var1) {
      double var3 = var1 * var1;
      double var5 = var3 * var1;
      return 10.0 * var5 - 15.0 * var5 * var1 + 6.0 * var5 * var3;
   }

   private double c(double var1, double var3, double var5, double var7) {
      double var9 = 1.0 - var7;
      return var9 * var9 * var1 + 2.0 * var9 * var7 * var3 + var7 * var7 * var5;
   }

   private double b(double var1, double var3, double var5) {
      return var1 + (var3 - var1) * var5;
   }

   private double e(double var1) {
      return this.a(-var1, var1);
   }

   private float a(LivingEntity var1, double var2) {
      Box var4 = var1.getBoundingBox();
      double var5 = Math.max(var4.getLengthX(), var4.getLengthZ()) * 0.5;
      return (float)Math.toDegrees(Math.atan2(var5, Math.max(0.35, var2)));
   }

   private float b(LivingEntity var1, double var2) {
      double var4 = var1.getBoundingBox().getLengthY() * 0.5;
      return (float)Math.toDegrees(Math.atan2(var4, Math.max(0.35, var2)));
   }

   private float f() {
      return MathHelper.clamp((float)this.random.nextGaussian() * 0.48F, -1.0F, 1.0F);
   }

   private float c(float var1, float var2) {
      return (this.random.nextBoolean() ? 1.0F : -1.0F) * this.d(var1, var2);
   }

   private float d(float var1, float var2) {
      return var1 + this.random.nextFloat() * (var2 - var1);
   }

   private double a(double var1, double var3) {
      return var1 + this.random.nextDouble() * (var3 - var1);
   }

   private long a(long var1, long var3) {
      long var5 = var1 + (long)(this.random.nextDouble() * (var3 - var1));
      return var5 * 1000000L;
   }

   private float b(float var1, float var2, float var3) {
      return var1 < var2 ? Math.min(var1 + var3, var2) : Math.max(var1 - var3, var2);
   }

   private double c(double var1, double var3, double var5) {
      return Math.max(var3, Math.min(var5, var1));
   }

   enum a {
      REACTION,
      ACQUIRE,
      TRACK,
      FOCUS,
      RECOVER;
   }

   enum b {
      MINIMUM_JERK,
      EASE_OUT_QUAD,
      EASE_OUT_CUBIC,
      EASE_OUT_QUART,
      EASE_IN_OUT_SINE,
      EASE_IN_OUT_CIRC,
      EASE_IN_OUT_CUBIC,
      EASE_IN_OUT_QUINT,
      EASE_IN_OUT_EXPO;

      double a(double var1, FuntimeRotation var3) {
         switch (this) {
            case EASE_OUT_QUAD:
               return Easings2.QUAD_OUT.ease(var1);
            case EASE_OUT_CUBIC:
               return Easings2.CUBIC_OUT.ease(var1);
            case EASE_OUT_QUART:
               return Easings2.QUART_OUT.ease(var1);
            case EASE_IN_OUT_SINE:
               return Easings2.SINE_IN_OUT.ease(var1);
            case EASE_IN_OUT_CIRC:
               return Easings2.CIRC_IN_OUT.ease(var1);
            case EASE_IN_OUT_CUBIC:
               return Easings2.CUBIC_IN_OUT.ease(var1);
            case EASE_IN_OUT_QUINT:
               return Easings2.QUINT_IN_OUT.ease(var1);
            case EASE_IN_OUT_EXPO:
               return Easings2.EXPO_IN_OUT.ease(var1);
            default:
               return var3.a(var1);
         }
      }
   }

   enum c {
      EMPIRICAL,
      MINIMUM_JERK,
      RANDOM_SPLINE,
      FLICK_SETTLE,
      CORRECTIVE;
   }
}
