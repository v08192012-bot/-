package brokke.cc.rotation;

import brokke.cc.util.GCDUtil2;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SlothRotation extends RotationsSystem {
   private static final long MIN_UPDATE_NANOS = 500000L;
   private static final long STATE_GAP_NANOS = 200000000L;
   private static final long FOCUS_HOLD_NANOS = 120000000L;
   private static final long ACTION_DEBOUNCE_NANOS = 30000000L;
   private static final long PRECISION_RISE_GAP_NANOS = 150000000L;
   private static final double MAX_DELTA_TIME = 0.05;
   private static final int ROTATION_PRIORITY = 1;
   private static final int AIM_HISTORY_CAPACITY = 4;
   private static final int MOUSE_REPORT_CENTER_MICROS = 20150;
   private static final float PRECISION_HUMANIZATION_SCALE = 0.8F;
   private static final float PRECISION_RESPONSE_SCALE = 1.2F;
   private final Random random = new Random();
   private final double[] aimHistoryX = new double[4];
   private final double[] aimHistoryY = new double[4];
   private final double[] aimHistoryZ = new double[4];
   private final int[] aimHistoryZone = new int[4];
   private LivingEntity trackedTarget;
   private SlothRotation.a phase = SlothRotation.a.REACTION;
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
   private long lastPrecisionRiseNanos;
   private long lastActionPathNanos;
   private long nextMouseReportNanos;
   private long lastMouseReportNanos;
   private long lastRotationSampleNanos;
   private int previousMouseReportMicros = 20150;
   private int secondPreviousMouseReportMicros = 20150;
   private int cadenceAge = Integer.MIN_VALUE;
   private int cadenceTicksRemaining;
   private int cadenceRunLength;
   private int previousHoldRun;
   private int previousMoveRun;
   private int cadenceResumeAge = Integer.MIN_VALUE;
   private int precisionReportAge = Integer.MIN_VALUE;
   private boolean cadenceHold;
   private float cadenceResumeScale = 1.0F;
   private boolean precisionRequested;
   private boolean priorityBlocked;
   private boolean wasAirborne;
   private int aimHistorySize;
   private int aimHistoryCursor;
   private int lastAimZone = -1;
   private int previousAimZone = -1;
   private int repeatedPathModels;
   private SlothRotation.b pathModel = SlothRotation.b.MINIMUM_JERK;
   private SlothRotation.b previousPathModel = SlothRotation.b.MINIMUM_JERK;
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
   private int stallAge = Integer.MIN_VALUE;
   private int stallTicksRemaining;
   private boolean stallActive;
   private int burstAge = Integer.MIN_VALUE;
   private float burstGainYaw = 1.0F;
   private float burstGainPitch = 1.0F;
   private static final int MAX_FROZEN_TICK_STREAK = 6;
   private int frozenStreakAge = Integer.MIN_VALUE;
   private int frozenTickStreak;
   private Vec3d lastTurnVelocity = Vec3d.ZERO;
   private float turnBoost = 1.0F;
   private float strafeScore;
   private long lastTurnBoostNanos;
   private final SlothPatternEasing.b patternDriver = new SlothPatternEasing.b();
   private float externalSpeedBoost = 1.0F;

   public void a(float var1) {
      this.externalSpeedBoost = Math.max(0.1F, var1);
   }

   public void a() {
      this.trackedTarget = null;
      this.phase = SlothRotation.a.REACTION;
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
      this.lastPrecisionRiseNanos = 0L;
      this.lastActionPathNanos = 0L;
      this.nextMouseReportNanos = 0L;
      this.lastMouseReportNanos = 0L;
      this.lastRotationSampleNanos = 0L;
      this.previousMouseReportMicros = 20150;
      this.secondPreviousMouseReportMicros = 20150;
      this.cadenceAge = Integer.MIN_VALUE;
      this.cadenceTicksRemaining = 0;
      this.cadenceRunLength = 0;
      this.previousHoldRun = 0;
      this.previousMoveRun = 0;
      this.cadenceResumeAge = Integer.MIN_VALUE;
      this.precisionReportAge = Integer.MIN_VALUE;
      this.cadenceHold = false;
      this.cadenceResumeScale = 1.0F;
      this.precisionRequested = false;
      this.priorityBlocked = false;
      this.wasAirborne = mc.player != null && !mc.player.isOnGround();
      this.h();
      this.repeatedPathModels = 0;
      this.pathModel = SlothRotation.b.MINIMUM_JERK;
      this.previousPathModel = SlothRotation.b.MINIMUM_JERK;
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
      this.stallAge = Integer.MIN_VALUE;
      this.stallTicksRemaining = 0;
      this.stallActive = false;
      this.burstAge = Integer.MIN_VALUE;
      this.burstGainYaw = 1.0F;
      this.burstGainPitch = 1.0F;
      this.frozenStreakAge = Integer.MIN_VALUE;
      this.frozenTickStreak = 0;
      this.lastTurnVelocity = Vec3d.ZERO;
      this.turnBoost = 1.0F;
      this.strafeScore = 0.0F;
      this.lastTurnBoostNanos = 0L;
      this.patternDriver.a();
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
      if (var1 && !this.precisionRequested) {
         this.d(var2);
         if (this.trackedTarget != null && var2 - this.lastPrecisionRiseNanos >= 150000000L) {
            this.c(var2, true);
         }

         this.lastPrecisionRiseNanos = var2;
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
         if (this.random.nextDouble() < 0.32) {
            this.recoilYaw = this.c(1.5F, 4.5F);
         } else {
            this.recoilYaw = this.c(0.04F, 0.13F);
         }

         double var3 = this.random.nextDouble();
         this.recoilPitch = var3 < 0.25 ? this.d(0.8F, 2.2F) : (var3 < 0.63 ? this.d(0.12F, 0.42F) : (var3 < 0.83 ? -this.d(0.08F, 0.28F) : 0.0F));
         boolean var5 = this.pathModel == SlothRotation.b.EMPIRICAL
            && this.empiricalAction
            && var1 >= this.anchorPathStartNanos
            && var1 < this.anchorPathEndNanos;
         if (!var5) {
            this.c(var1, true);
         }

         this.phase = SlothRotation.a.RECOVER;
      }
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         long var2 = System.nanoTime();
         RotationStorage var4 = RotationStorage.instance;
         if (var4 != null && var4.getCurrentPriority() > 1) {
            this.b(var2, true);
            this.priorityBlocked = true;
         } else {
            if (this.trackedTarget != var1) {
               this.a(var1, var2);
            }

            if (this.priorityBlocked) {
               this.b(var2, true);
               this.priorityBlocked = false;
            } else {
               long var5 = this.lastUpdateNanos == 0L ? 4166667L : var2 - this.lastUpdateNanos;
               if (var5 >= 500000L) {
                  if (var5 > 200000000L) {
                     this.b(var2, true);
                     this.phase = SlothRotation.a.ACQUIRE;
                  } else {
                     this.lastUpdateNanos = var2;
                     double var7 = Math.min(var5 / 1.0E9, 0.05);
                     boolean var9 = this.precisionRequested || var2 < this.precisionUntilNanos;
                     boolean var10 = !mc.player.isOnGround();
                     if (var10 && !this.wasAirborne && mc.player.getVelocity().y > 0.035) {
                        this.d(var2, var9);
                     }

                     if (var10 && this.jumpCriticalPathPending && mc.player.getVelocity().y < -0.07) {
                        this.jumpCriticalPathPending = false;
                        this.c(var2, var9);
                     } else if (!var10) {
                        this.jumpCriticalPathPending = false;
                     }

                     this.wasAirborne = var10;
                     this.a(var2, var7, var9);
                     this.a(var1, var2, var7);
                     Vec3d var11 = this.a(var1, var7, var9);
                     Vec3d var12 = var11.subtract(mc.player.getEyePos());
                     float var13 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var12.z, var12.x)) - 90.0);
                     float var14 = (float)(-Math.toDegrees(Math.atan2(var12.y, var12.horizontalLength())));
                     float var15 = this.a(var1, var12.length());
                     float var16 = this.b(var1, var12.length());
                     float var17 = MathHelper.clamp(this.empiricalCurveYaw, -var15 * (var9 ? 0.272F : 0.85F), var15 * (var9 ? 0.272F : 0.85F));
                     float var18 = MathHelper.clamp(this.empiricalCurvePitch, -var16 * (var9 ? 0.224F : 0.72F), var16 * (var9 ? 0.224F : 0.72F));
                     float var19 = MathHelper.clamp(this.a(var2), -var16 * (var9 ? 0.176F : 0.78F), var16 * (var9 ? 0.176F : 0.78F));
                     this.a(var2, var9, var15, var16);
                     float var20 = this.e(var2);
                     float var21 = (var9 ? 0.176F : (this.phase == SlothRotation.a.ACQUIRE ? 0.35F : 1.0F)) * this.noiseMotionBlend;
                     float var22 = MathHelper.clamp(var15 * 0.35F, 0.12F, 1.05F) * var21;
                     float var23 = MathHelper.clamp(var15 * 0.22F, 0.09F, 0.75F) * var21;
                     this.recoilYaw = this.recoilYaw * (float)Math.exp(-var7 / 0.075);
                     this.recoilPitch = this.recoilPitch * (float)Math.exp(-var7 / 0.09);
                     float var24 = var9 ? 0.52F : 1.0F;
                     float var38 = MathHelper.wrapDegrees(
                        var13 + var17 + this.noiseYaw * var22 + this.tremorYaw * this.tremorBlend * var24 + this.lapseYaw * var20 + this.recoilYaw
                     );
                     float var39 = MathHelper.clamp(
                        var14
                           + var18
                           + var19
                           + this.noisePitch * var23
                           + this.tremorPitch * this.tremorBlend * var24
                           + this.lapsePitch * var20
                           + this.recoilPitch,
                        -89.0F,
                        89.0F
                     );
                     float var25 = MathHelper.wrapDegrees(var38 - this.requestedYaw);
                     float var26 = var39 - this.requestedPitch;
                     float var27 = (float)Math.hypot(var25, var26);
                     if (this.a(var27, var9, var2 < this.reactionUntilNanos)) {
                        this.b(var38, var39, var7);
                     } else if (this.a(var2, var9, var2 < this.reactionUntilNanos)) {
                        this.b(var38, var39, var7);
                     } else {
                        this.a(var38, var39, var7);
                        if (var2 < this.reactionUntilNanos && !var9) {
                           this.phase = SlothRotation.a.REACTION;
                           this.yawVelocity = this.b(this.yawVelocity, 0.0F, this.yawDeceleration * (float)var7);
                           this.pitchVelocity = this.b(this.pitchVelocity, 0.0F, this.pitchDeceleration * (float)var7);
                        } else {
                           if (!var9) {
                              this.phase = var27 > 10.0F ? SlothRotation.a.ACQUIRE : SlothRotation.a.TRACK;
                           } else {
                              this.phase = var2 < this.precisionUntilNanos && (Math.abs(this.recoilYaw) > 0.01F || Math.abs(this.recoilPitch) > 0.01F)
                                 ? SlothRotation.a.RECOVER
                                 : SlothRotation.a.FOCUS;
                           }

                           this.a(var25, var26, var7, var9, mc.player.isGliding());
                        }

                        float var28 = mc.player.age == this.cadenceResumeAge ? this.cadenceResumeScale : 1.0F;
                        this.c(var9);
                        float var29 = this.yawVelocity * (float)var7 * var28;
                        float var30 = this.pitchVelocity * (float)var7 * var28;
                        float var31 = var29 * this.burstGainYaw;
                        float var32 = var30 * this.burstGainPitch;
                        if (var31 * var25 > 0.0F && Math.abs(var31) > Math.abs(var25)) {
                           boolean var33 = Math.abs(var29) > Math.abs(var25);
                           if (Math.abs(this.wantedYawVelocity) > 8.0F) {
                              var31 = var25 + MathHelper.clamp(this.wantedYawVelocity * (float)var7 * 0.58F, -0.45F, 0.45F);
                              if (var33) {
                                 this.yawVelocity = this.wantedYawVelocity * 0.58F;
                                 this.yawMotorAcceleration = 0.0F;
                              }
                           } else {
                              var31 = var25 * this.d(0.82F, 0.98F);
                              if (var33) {
                                 this.yawVelocity = 0.0F;
                                 this.yawMotorAcceleration = 0.0F;
                              }
                           }
                        }

                        if (var32 * var26 > 0.0F && Math.abs(var32) > Math.abs(var26)) {
                           boolean var46 = Math.abs(var30) > Math.abs(var26);
                           if (Math.abs(this.wantedPitchVelocity) > 6.0F) {
                              var32 = var26 + MathHelper.clamp(this.wantedPitchVelocity * (float)var7 * 0.52F, -0.32F, 0.32F);
                              if (var46) {
                                 this.pitchVelocity = this.wantedPitchVelocity * 0.52F;
                                 this.pitchMotorAcceleration = 0.0F;
                              }
                           } else {
                              var32 = var26 * this.d(0.82F, 0.98F);
                              if (var46) {
                                 this.pitchVelocity = 0.0F;
                                 this.pitchMotorAcceleration = 0.0F;
                              }
                           }
                        }

                        float var47;
                        if (this.anchorPathEndNanos > this.anchorPathStartNanos) {
                           var47 = (float)this.d((double)(var2 - this.anchorPathStartNanos) / (this.anchorPathEndNanos - this.anchorPathStartNanos), 0.0, 1.0);
                        } else {
                           var47 = 0.5F;
                        }

                        SlothPatternEasing.c var34 = this.patternDriver.a(var7, var47);
                        var31 += var34.getYawOffset();
                        var32 += var34.getPitchOffset();
                        float var35 = MathHelper.clamp(0.85F + SlothPatternEasing.a(mc.player, var47) * 0.35F, 0.85F, 1.35F);
                        var31 = SlothPatternEasing.a(var31, 60.0F, var35);
                        var32 = SlothPatternEasing.a(var32, 24.0F, var35);
                        float var36 = this.d(62.0F, 70.0F);
                        float var37 = this.d(9.0F, 11.0F);
                        float var42 = MathHelper.clamp(var31, -var36, var36);
                        float var45 = MathHelper.clamp(var32, -var37, var37);
                        if (Math.abs(var42) + Math.abs(var45) >= GCDUtil2.a()) {
                           this.frozenTickStreak = 0;
                        }

                        this.requestedYaw = MathHelper.wrapDegrees(this.requestedYaw + var42);
                        this.requestedPitch = MathHelper.clamp(this.requestedPitch + var45, -89.0F, 89.0F);
                        if (Float.isFinite(this.requestedYaw) && Float.isFinite(this.requestedPitch)) {
                           this.a(var2, var9);
                        } else {
                           this.b(var2, true);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void a(LivingEntity var1, long var2) {
      this.trackedTarget = var1;
      this.b(var2, true);
      this.nextMouseReportNanos = var2;
      this.lastMouseReportNanos = 0L;
      this.lastRotationSampleNanos = 0L;
      this.previousMouseReportMicros = 20150;
      this.secondPreviousMouseReportMicros = 20150;
      this.cadenceAge = Integer.MIN_VALUE;
      this.cadenceRunLength = this.c();
      this.cadenceTicksRemaining = this.cadenceRunLength - 1;
      this.previousHoldRun = 0;
      this.previousMoveRun = 0;
      this.cadenceResumeAge = Integer.MIN_VALUE;
      this.precisionReportAge = Integer.MIN_VALUE;
      this.cadenceHold = false;
      this.cadenceResumeScale = 1.0F;
      this.stallAge = Integer.MIN_VALUE;
      this.stallTicksRemaining = 0;
      this.stallActive = false;
      this.burstAge = Integer.MIN_VALUE;
      this.frozenStreakAge = Integer.MIN_VALUE;
      this.frozenTickStreak = 0;
      this.lastTurnVelocity = var1.getVelocity();
      this.turnBoost = 1.0F;
      this.strafeScore = 0.0F;
      this.lastTurnBoostNanos = 0L;
      this.patternDriver.a();
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
      this.h();
      this.noiseYaw = this.noisePitch = 0.0F;
      this.noiseGoalYaw = this.noiseGoalPitch = 0.0F;
      this.recoilYaw = this.recoilPitch = 0.0F;
      this.tremorYaw = this.tremorPitch = 0.0F;
      this.tremorGoalYaw = this.tremorGoalPitch = 0.0F;
      this.tremorBlend = 0.0F;
      this.pathNoiseScale = SlothDatasetProfile.o(this.random);
      this.noiseMotionBlend = 0.1F;
      this.speedScale = this.speedGoal = SlothDatasetProfile.c(this.random);
      this.segmentSpeedScale = this.segmentSpeedGoal = 1.0F;
      this.perceptionLeadScale = this.perceptionLeadGoal = SlothDatasetProfile.m(this.random);
      this.perceptionLeadBias = this.perceptionLeadBiasGoal = SlothDatasetProfile.n(this.random);
      this.lapseYaw = this.lapsePitch = 0.0F;
      this.hesitationScale = this.hesitationGoal = 1.0F;
      this.lapsePeakProgress = 0.28F;
      this.lapseRiseExponent = 2.4F;
      this.anchorPathVelocityX = this.anchorPathVelocityY = this.anchorPathVelocityZ = 0.0;
      this.lastAnchorSampleNanos = var2;
      this.maximumYawSpeed = SlothDatasetProfile.e(this.random) * 2.0F;
      this.maximumPitchSpeed = SlothDatasetProfile.f(this.random);
      this.yawAcceleration = SlothDatasetProfile.g(this.random) * 1.35F;
      this.pitchAcceleration = SlothDatasetProfile.h(this.random);
      this.yawDeceleration = SlothDatasetProfile.i(this.random) * 1.35F;
      this.pitchDeceleration = SlothDatasetProfile.j(this.random);
      this.yawJerkLimit = SlothDatasetProfile.k(this.random) * 1.35F;
      this.pitchJerkLimit = SlothDatasetProfile.l(this.random);
      float var4 = this.d(var1);
      long var5 = SlothDatasetProfile.a(this.random, var4, this.precisionRequested);
      this.reactionUntilNanos = var2 + var5 * 1000000L;
      this.b(var2, this.precisionRequested, false);
      this.nextNoiseNanos = var2 + this.a(260L, 720L);
      this.nextTremorNanos = var2 + this.a(240L, 800L);
      this.nextSpeedNanos = var2 + this.a(110L, 340L);
      this.nextPerceptionNanos = var2 + this.a(320L, 920L);
      this.nextLapseNanos = var2 + this.a(500L, 1300L);
      this.nextHesitationNanos = var2 + this.a(1000L, 2800L);
      this.wasAirborne = !mc.player.isOnGround();
      this.phase = var5 > 0L ? SlothRotation.a.REACTION : SlothRotation.a.ACQUIRE;
   }

   private void a(long var1, double var3, boolean var5) {
      this.a(var3);
      this.b(var1);
      if (var1 >= this.nextAnchorNanos) {
         this.b(var1, var5, false);
         this.b(var1);
      }

      if (var1 >= this.nextNoiseNanos) {
         float var6 = this.i();
         this.noiseGoalYaw = MathHelper.clamp(this.i() * 0.78F + var6 * 0.22F, -1.0F, 1.0F);
         this.noiseGoalPitch = MathHelper.clamp(this.i() * 0.82F + var6 * 0.18F, -1.0F, 1.0F);
         this.nextNoiseNanos = var1 + this.a(260L, 760L);
      }

      double var18 = 1.0 - Math.exp(-var3 / 0.38);
      double var8 = 1.0 - Math.exp(-var3 / 0.48);
      this.noiseYaw = this.noiseYaw + (this.noiseGoalYaw - this.noiseYaw) * (float)var18;
      this.noisePitch = this.noisePitch + (this.noiseGoalPitch - this.noisePitch) * (float)var8;
      if (var1 >= this.nextSpeedNanos) {
         this.speedGoal = SlothDatasetProfile.d(this.random);
         this.nextSpeedNanos = var1 + this.a(110L, 360L);
      }

      double var10 = 1.0 - Math.exp(-var3 / 0.16);
      this.speedScale = this.speedScale + (this.speedGoal - this.speedScale) * (float)var10;
      double var12 = 1.0 - Math.exp(-var3 / 0.09);
      this.segmentSpeedScale = this.segmentSpeedScale + (this.segmentSpeedGoal - this.segmentSpeedScale) * (float)var12;
      if (var1 >= this.nextPerceptionNanos) {
         this.perceptionLeadGoal = SlothDatasetProfile.m(this.random);
         this.perceptionLeadBiasGoal = SlothDatasetProfile.n(this.random);
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
         this.hesitationGoal = this.d(0.78F, 0.94F);
         this.hesitationEndNanos = var1 + this.a(15L, 54L);
      }

      double var16 = 1.0 - Math.exp(-var3 / (this.hesitationGoal < this.hesitationScale ? 0.022 : 0.045));
      this.hesitationScale = this.hesitationScale + (this.hesitationGoal - this.hesitationScale) * (float)var16;
      this.a(var1, var3);
   }

   private void a(double var1) {
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

   private void a(LivingEntity var1, long var2, double var4) {
      Vec3d var6 = var1.getVelocity();
      double var7 = var6.horizontalLength();
      double var9 = this.lastTurnVelocity.horizontalLength();
      if (var7 > 0.08 && var9 > 0.08) {
         double var11 = (var6.x * this.lastTurnVelocity.x + var6.z * this.lastTurnVelocity.z) / (var7 * var9);
         long var13 = this.strafeScore > 0.6F ? 110000000L : 250000000L;
         if (var11 < 0.5 && var2 - this.lastTurnBoostNanos > var13) {
            this.lastTurnBoostNanos = var2;
            this.strafeScore = Math.min(1.0F, this.strafeScore + 0.45F);
            if (this.random.nextDouble() < 0.85) {
               this.turnBoost = Math.max(this.turnBoost, this.d(1.28F, 1.68F));
            }
         }
      }

      this.lastTurnVelocity = var6;
      float var15 = (float)(1.0 - Math.exp(-var4 / 0.3));
      this.turnBoost = this.turnBoost + (1.0F - this.turnBoost) * var15;
      float var12 = (float)(1.0 - Math.exp(-var4 / 1.1));
      this.strafeScore = this.strafeScore + (0.0F - this.strafeScore) * var12;
      if (this.strafeScore > 0.35F) {
         this.turnBoost = Math.max(this.turnBoost, 1.0F + 0.28F * this.strafeScore);
      }
   }

   private Vec3d a(LivingEntity var1, double var2, boolean var4) {
      Vec3d var5 = var1.getVelocity();
      if (var5.lengthSquared() > 2.25) {
         var5 = var5.normalize().multiply(1.5);
      }

      double var6 = var5.subtract(this.filteredVelocity).horizontalLength();
      double var8 = this.d(0.12 - var6 * 0.35, 0.045, 0.12);
      double var10 = 1.0 - Math.exp(-var2 / var8);
      this.filteredVelocity = this.filteredVelocity.lerp(var5, var10);
      Box var12 = var1.getBoundingBox();
      Vec3d var13 = new Vec3d(
         var12.minX + var12.getLengthX() * this.anchorX, var12.minY + var12.getLengthY() * this.anchorY, var12.minZ + var12.getLengthZ() * this.anchorZ
      );
      double var14 = mc.player.getEyePos().distanceTo(var13);
      double var16;
      if (var4) {
         var16 = this.d(
            (0.16 + var14 * 0.025 + this.filteredVelocity.horizontalLength() * 0.09) * this.perceptionLeadScale + this.perceptionLeadBias * 0.18, 0.1, 0.42
         );
      } else if (!mc.player.isGliding() && !var1.isGliding()) {
         var16 = this.d((0.52 + var14 * 0.09) * this.perceptionLeadScale + this.perceptionLeadBias, -0.2, 1.35);
      } else {
         var16 = this.d((0.95 + var14 * 0.08) * this.perceptionLeadScale + this.perceptionLeadBias, 0.15, 1.85);
      }

      if (this.strafeScore > 0.2F) {
         var16 *= 1.0 - 0.45 * this.strafeScore;
      }

      Vec3d var18 = this.filteredVelocity.multiply(var16);
      Vec3d var19 = var13.add(var18);
      if (!var4) {
         return var19;
      }

      double var20 = Math.min(var12.getLengthX(), var12.getLengthZ()) * 0.12;
      double var22 = var12.getLengthY() * 0.1;
      double var24 = Math.min(
         this.a(var13.x, var18.x, var12.minX + var20, var12.maxX - var20),
         Math.min(this.a(var13.y, var18.y, var12.minY + var22, var12.maxY - var22), this.a(var13.z, var18.z, var12.minZ + var20, var12.maxZ - var20))
      );
      return var13.add(var18.multiply(var24));
   }

   private double a(double var1, double var3, double var5, double var7) {
      if (Math.abs(var3) < 1.0E-8) {
         return 1.0;
      }

      double var9 = var3 > 0.0 ? (var7 - var1) / var3 : (var5 - var1) / var3;
      return this.d(var9, 0.0, 1.0);
   }

   private void a(float var1, float var2, double var3, boolean var5, boolean var6) {
      float var7 = this.speedScale * this.segmentSpeedScale * this.hesitationScale * this.airMotorScale * this.turnBoost * (var6 ? 1.12F : 1.0F);
      if (var5) {
         var7 *= 1.272F;
      }

      float var8 = Math.abs(var1);
      float var9 = Math.abs(var2);
      float var10 = var6 ? 1400.0F : (var8 < 18.0F ? 800.0F : (var8 < 65.0F ? 1150.0F : 1400.0F));
      float var11 = var6 ? 520.0F : (var9 < 8.0F ? 230.0F : (var9 < 24.0F ? 330.0F : 430.0F));
      float var12 = this.externalSpeedBoost;
      float var13 = Math.min(var10, this.maximumYawSpeed * var7 * var12);
      float var14 = Math.min(var11, this.maximumPitchSpeed * var7 * var12);
      float var15 = 0.22F + 0.78F * (float)Math.exp(-Math.abs(var1) / 28.0F);
      float var16 = 0.2F + 0.8F * (float)Math.exp(-Math.abs(var2) / 16.0F);
      float var17 = this.a(var1, this.yawDeceleration, var13) + this.wantedYawVelocity * (var5 ? 0.92F : 0.78F) * var15;
      float var18 = this.a(var2, this.pitchDeceleration, var14) + this.wantedPitchVelocity * (var5 ? 0.82F : 0.66F) * var16;
      float var31 = MathHelper.clamp(var17, -var13, var13);
      float var32 = MathHelper.clamp(var18, -var14, var14);
      float var19 = 1.0F + MathHelper.clamp((var8 - 30.0F) / 80.0F, 0.0F, 0.55F);
      float var20 = 1.0F + MathHelper.clamp((var9 - 18.0F) / 42.0F, 0.0F, 0.3F);
      boolean var21 = this.yawVelocity * var1 < 0.0F && Math.abs(this.yawVelocity) > 12.0F;
      float var22 = var21 ? 1.55F : 1.0F;
      float var23 = (this.b(this.yawVelocity, var31) ? this.yawDeceleration : this.yawAcceleration) * var19 * var22;
      float var24 = (this.b(this.pitchVelocity, var32) ? this.pitchDeceleration : this.pitchAcceleration) * var20;
      float var25 = var5 ? 1.3440001F : 1.0F;
      float var26 = var5 ? 1.32F : 1.0F;
      float var27 = MathHelper.clamp((var31 - this.yawVelocity) / (float)var3, -var23 * var25, var23 * var25);
      float var28 = MathHelper.clamp((var32 - this.pitchVelocity) / (float)var3, -var24 * var26, var24 * var26);
      this.yawMotorAcceleration = this.b(this.yawMotorAcceleration, var27, this.yawJerkLimit * var25 * var22 * (float)Math.sqrt(var19) * (float)var3);
      this.pitchMotorAcceleration = this.b(this.pitchMotorAcceleration, var28, this.pitchJerkLimit * var26 * (float)Math.sqrt(var20) * (float)var3);
      float var29 = this.yawVelocity + this.yawMotorAcceleration * (float)var3;
      float var30 = this.pitchVelocity + this.pitchMotorAcceleration * (float)var3;
      if ((var31 - this.yawVelocity) * (var31 - var29) <= 0.0F) {
         var29 = var31;
         this.yawMotorAcceleration *= 0.22F;
      }

      if ((var32 - this.pitchVelocity) * (var32 - var30) <= 0.0F) {
         var30 = var32;
         this.pitchMotorAcceleration *= 0.22F;
      }

      this.yawVelocity = MathHelper.clamp(var29, -var13, var13);
      this.pitchVelocity = MathHelper.clamp(var30, -var14, var14);
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

   private void a(long var1, boolean var3) {
      boolean var4 = var3 && mc.player.age != this.precisionReportAge;
      boolean var5 = var4 || mc.player.age == this.cadenceResumeAge;
      long var6 = this.lastRotationSampleNanos == 0L ? Long.MAX_VALUE : var1 - this.lastRotationSampleNanos;
      this.lastRotationSampleNanos = var1;
      long var8 = this.lastMouseReportNanos == 0L ? Long.MAX_VALUE : var1 - this.lastMouseReportNanos;
      if (var5 || var6 >= 12500000L || var1 >= this.nextMouseReportNanos) {
         if (this.lastMouseReportNanos != 0L) {
            int var10 = (int)this.d(var8 / 1000.0, 12500.0, 150000.0);
            this.secondPreviousMouseReportMicros = this.previousMouseReportMicros;
            this.previousMouseReportMicros = var10;
         }

         int var20 = SlothDatasetProfile.b(this.random);
         double var11 = 20150.0
            - (this.previousMouseReportMicros - 20150) * 0.19
            + (this.secondPreviousMouseReportMicros - 20150) * 0.275
            + (var20 - 20150) * 0.95
            + this.a(-260.0, 260.0);
         int var13 = (int)this.d(var11, 11500.0, 75000.0);
         double var14 = var6 < 12500000L ? var6 / 2000.0 : 0.0;
         long var16 = (long)Math.max(8000.0, var13 - var14);
         this.lastMouseReportNanos = var1;
         this.nextMouseReportNanos = var1 + var16 * 1000L;
         if (var3) {
            this.precisionReportAge = mc.player.age;
         }

         RotationStorage.a(new Rotation2(this.requestedYaw, this.requestedPitch), 360.0F, 360.0F, 30.0F, 22.0F, 2, 1, this.visibleAim);
         float var18 = Math.abs(MathHelper.wrapDegrees(this.requestedYaw - mc.player.getYaw()));
         float var19 = Math.abs(this.requestedPitch - mc.player.getPitch());
         if (var18 > 4.0F || var19 > 4.0F) {
            this.b(var1, true);
         }
      }
   }

   private boolean a(float var1, boolean var2, boolean var3) {
      int var4 = mc.player.age;
      if (var4 == this.cadenceAge) {
         return this.cadenceHold;
      }

      this.cadenceAge = var4;
      if (this.frozenTickStreak >= 6) {
         boolean var12 = this.cadenceHold;
         this.cadenceHold = false;
         this.cadenceRunLength = Math.max(3, this.c());
         this.cadenceTicksRemaining = this.cadenceRunLength - 1;
         this.cadenceResumeAge = var12 ? var4 : Integer.MIN_VALUE;
         this.cadenceResumeScale = 1.0F;
         if (var12) {
            this.e();
         }

         return false;
      } else {
         float var5 = (float)Math.hypot(this.wantedYawVelocity, this.wantedPitchVelocity);
         float var6 = (float)Math.hypot(this.yawVelocity, this.pitchVelocity);
         float var7 = (float)Math.hypot(MathHelper.wrapDegrees(this.requestedYaw - mc.player.getYaw()), this.requestedPitch - mc.player.getPitch());
         boolean var8 = !var2 && !var3 && var1 < 1.6F && var5 < 38.0F && var6 < 50.0F && var7 < 0.6F;
         boolean var9 = var2 && !var3 && var1 < 0.48F && var5 < 18.0F && var6 < 26.0F && var7 < 0.22F;
         boolean var10 = var8 || var9;
         if (!var10) {
            boolean var11 = this.cadenceHold;
            this.cadenceHold = false;
            this.cadenceRunLength = Math.max(3, this.c());
            this.cadenceTicksRemaining = this.cadenceRunLength - 1;
            this.cadenceResumeAge = var11 ? var4 : Integer.MIN_VALUE;
            this.cadenceResumeScale = var11 ? (var2 ? this.d(0.76F, 0.9F) : this.d(0.58F, 0.82F)) : 1.0F;
            if (var11) {
               this.e();
            }

            return false;
         } else {
            if (var2 && !this.cadenceHold && this.cadenceTicksRemaining > 1) {
               this.cadenceTicksRemaining = this.random.nextBoolean() ? 0 : 1;
            }

            if (this.cadenceTicksRemaining > 0) {
               this.cadenceTicksRemaining--;
               return this.cadenceHold;
            }

            if (this.cadenceHold) {
               this.previousHoldRun = this.cadenceRunLength;
               this.cadenceHold = false;
               this.cadenceRunLength = this.c();
               if (this.previousHoldRun == 1 && this.previousMoveRun == 1 && this.cadenceRunLength == 1) {
                  this.cadenceRunLength = 2;
               }

               this.cadenceTicksRemaining = this.cadenceRunLength - 1;
               this.cadenceResumeAge = var4;
               this.cadenceResumeScale = var2 ? this.d(0.82F, 0.95F) : this.d(0.58F, 0.82F);
               this.e();
               return false;
            } else {
               this.previousMoveRun = this.cadenceRunLength;
               this.cadenceHold = true;
               this.cadenceRunLength = var2 ? 1 : this.d();
               if (this.previousHoldRun == 1 && this.previousMoveRun == 1 && this.cadenceRunLength == 1) {
                  this.cadenceRunLength = 2;
               }

               this.cadenceTicksRemaining = this.cadenceRunLength - 1;
               return true;
            }
         }
      }
   }

   private int c() {
      int var1 = (int)Math.round(Math.exp(1.05 + this.random.nextGaussian() * 0.52));
      if (this.random.nextDouble() < 0.08) {
         var1 += this.random.nextInt(4, 13);
      }

      return MathHelper.clamp(var1, 2, 18);
   }

   private int d() {
      double var1 = this.random.nextDouble();
      return var1 < 0.55 ? 1 : (var1 < 0.9 ? 2 : 3);
   }

   private void e() {
      this.nextMouseReportNanos = 0L;
      this.lastMouseReportNanos = 0L;
      this.lastRotationSampleNanos = 0L;
   }

   private void b(float var1, float var2, double var3) {
      if (mc.player.age != this.frozenStreakAge) {
         this.frozenStreakAge = mc.player.age;
         this.frozenTickStreak++;
      }

      this.previousWantedYaw = var1;
      this.previousWantedPitch = var2;
      this.hasWantedRotation = true;
      this.yawVelocity = this.b(this.yawVelocity, 0.0F, this.yawDeceleration * (float)var3);
      this.pitchVelocity = this.b(this.pitchVelocity, 0.0F, this.pitchDeceleration * (float)var3);
      this.yawMotorAcceleration = this.b(this.yawMotorAcceleration, 0.0F, this.yawJerkLimit * (float)var3);
      this.pitchMotorAcceleration = this.b(this.pitchMotorAcceleration, 0.0F, this.pitchJerkLimit * (float)var3);
      this.f();
   }

   private boolean a(long var1, boolean var3, boolean var4) {
      int var5 = mc.player.age;
      if (var5 == this.stallAge) {
         return this.stallActive;
      } else {
         this.stallAge = var5;
         if (this.frozenTickStreak >= 6) {
            this.stallTicksRemaining = 0;
            this.stallActive = false;
            return false;
         } else if (this.stallTicksRemaining > 0) {
            this.stallTicksRemaining--;
            this.stallActive = true;
            return true;
         } else {
            boolean var6 = this.pathModel == SlothRotation.b.EMPIRICAL && this.empiricalJump
               || this.jumpPitchAmplitude != 0.0F && var1 >= this.jumpGestureStartNanos && var1 < this.jumpGestureEndNanos;
            if (!var3 && !var4 && !var6 && this.turnBoost < 1.15F && this.random.nextDouble() < 0.09) {
               this.stallTicksRemaining = this.random.nextDouble() < 0.75 ? 0 : 1;
               this.stallActive = true;
               return true;
            } else {
               this.stallActive = false;
               return false;
            }
         }
      }
   }

   private void c(boolean var1) {
      int var2 = mc.player.age;
      if (var2 != this.burstAge) {
         this.burstAge = var2;
         float var3 = var1 ? 0.16F : 0.55F;
         this.burstGainYaw = MathHelper.clamp((float)Math.exp(this.random.nextGaussian() * var3), 0.35F, 2.3F);
         this.burstGainPitch = MathHelper.clamp((float)Math.exp(this.random.nextGaussian() * var3 * 0.9F), 0.35F, 2.15F);
      }
   }

   private void f() {
      RotationStorage.a(new Rotation2(mc.player.getYaw(), mc.player.getPitch()), 360.0F, 360.0F, 30.0F, 22.0F, 2, 1, this.visibleAim);
   }

   private void b(long var1, boolean var3) {
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
         this.recoilYaw = 0.0F;
         this.recoilPitch = 0.0F;
      }

      this.lastUpdateNanos = var1;
   }

   private float d(LivingEntity var1) {
      Vec3d var2 = var1.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
      float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
      return Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw())) + Math.abs(var4 - mc.player.getPitch());
   }

   private void b(long var1, boolean var3, boolean var4) {
      boolean var5 = this.jumpPathRequested;
      this.jumpPathRequested = false;
      this.empiricalEntryYaw = this.empiricalCurveYaw;
      this.empiricalEntryPitch = this.empiricalCurvePitch;
      this.anchorStartX = this.anchorX;
      this.anchorStartY = this.anchorY;
      this.anchorStartZ = this.anchorZ;
      Vec3d var6 = this.e(var3);
      this.anchorGoalX = var6.x;
      this.anchorGoalY = var6.y;
      this.anchorGoalZ = var6.z;
      if (var5 && this.random.nextDouble() < 0.6) {
         this.pathModel = SlothRotation.b.EMPIRICAL;
         if (this.previousPathModel == SlothRotation.b.EMPIRICAL) {
            this.repeatedPathModels++;
         } else {
            this.previousPathModel = SlothRotation.b.EMPIRICAL;
            this.repeatedPathModels = 1;
         }
      } else {
         this.pathModel = this.d(var4);
      }

      if (this.pathModel == SlothRotation.b.EMPIRICAL) {
         this.empiricalAction = var4;
         this.empiricalJump = var5;
         this.empiricalPathIndex = var5 ? SlothDatasetProfile.a(this.random) : SlothDatasetProfile.a(this.random, var4);
      } else {
         this.empiricalAction = false;
         this.empiricalJump = false;
      }

      this.pathTimeWarp = this.a(-0.16, 0.2);
      this.pathEaseIn = this.a(1.82, 2.48);
      this.pathEaseOut = this.a(1.78, 2.42);
      this.pathFlickExponent = this.a(2.45, 3.25);
      this.pathNoiseScale = SlothDatasetProfile.o(this.random);
      double var7 = SlothDatasetProfile.b(this.random, var3);
      this.pathBendX = this.e(var7);
      this.pathBendY = this.e(var7 * 0.78);
      this.pathBendZ = this.e(var7);
      if (this.pathModel == SlothRotation.b.EMPIRICAL) {
         this.pathBendX = 0.0;
         this.pathBendY = 0.0;
         this.pathBendZ = 0.0;
         this.g();
      } else {
         this.empiricalPerpendicularYaw = 0.0F;
         this.empiricalPerpendicularPitch = 0.0F;
         this.empiricalAngularDistance = 0.0F;
      }

      this.anchorControlOneX = this.d(this.c(this.anchorStartX, this.anchorGoalX, 0.28) + this.e(var7), 0.14, 0.86);
      this.anchorControlOneY = this.d(this.c(this.anchorStartY, this.anchorGoalY, 0.28) + this.e(var7), 0.24, 0.9);
      this.anchorControlOneZ = this.d(this.c(this.anchorStartZ, this.anchorGoalZ, 0.28) + this.e(var7), 0.14, 0.86);
      double var9 = this.a(0.36, 0.67);
      this.anchorMidX = this.d(this.c(this.anchorStartX, this.anchorGoalX, var9) + this.pathBendX, 0.14, 0.86);
      this.anchorMidY = this.d(this.c(this.anchorStartY, this.anchorGoalY, var9) + this.pathBendY, 0.24, 0.9);
      this.anchorMidZ = this.d(this.c(this.anchorStartZ, this.anchorGoalZ, var9) + this.pathBendZ, 0.14, 0.86);
      this.anchorControlTwoX = this.d(this.anchorMidX + this.anchorMidX - this.anchorControlOneX, 0.14, 0.86);
      this.anchorControlTwoY = this.d(this.anchorMidY + this.anchorMidY - this.anchorControlOneY, 0.24, 0.9);
      this.anchorControlTwoZ = this.d(this.anchorMidZ + this.anchorMidZ - this.anchorControlOneZ, 0.14, 0.86);
      double var11 = this.pathModel == SlothRotation.b.FLICK_SETTLE ? this.a(0.025, 0.085) : this.a(0.008, 0.045);
      this.anchorOvershootX = this.d(this.anchorGoalX + (this.anchorGoalX - this.anchorStartX) * var11 + this.e(var7 * 0.32), 0.16, 0.84);
      this.anchorOvershootY = this.d(this.anchorGoalY + (this.anchorGoalY - this.anchorStartY) * var11 + this.e(var7 * 0.22), 0.27, 0.88);
      this.anchorOvershootZ = this.d(this.anchorGoalZ + (this.anchorGoalZ - this.anchorStartZ) * var11 + this.e(var7 * 0.32), 0.16, 0.84);
      if (this.pathModel == SlothRotation.b.EMPIRICAL) {
         this.pathPrimaryEnd = 0.75;
         this.pathCorrectionStart = 0.58;
         this.segmentSpeedGoal = this.d(0.9F, 1.12F);
      } else if (this.pathModel == SlothRotation.b.FLICK_SETTLE) {
         this.pathPrimaryEnd = this.a(0.42, 0.6);
         this.pathCorrectionStart = this.a(0.31, 0.48);
         this.segmentSpeedGoal = this.d(1.3F, 1.72F);
      } else if (this.pathModel == SlothRotation.b.CORRECTIVE) {
         this.pathPrimaryEnd = this.a(0.7, 0.86);
         this.pathCorrectionStart = this.a(0.52, 0.69);
         this.segmentSpeedGoal = this.d(0.78F, 1.24F);
      } else {
         this.pathPrimaryEnd = 0.75;
         this.pathCorrectionStart = 0.58;
         this.segmentSpeedGoal = this.d(0.74F, 1.3F);
      }

      double var13 = this.b(this.anchorStartX, this.anchorStartY, this.anchorStartZ, this.anchorGoalX, this.anchorGoalY, this.anchorGoalZ);
      double var15 = this.d(0.72 + var13 * 0.9, 0.78, 1.28);
      long var17;
      if (this.pathModel == SlothRotation.b.EMPIRICAL) {
         long var19 = this.empiricalJump
            ? SlothDatasetProfile.a(this.empiricalPathIndex)
            : SlothDatasetProfile.a(this.empiricalAction, this.empiricalPathIndex);
         var17 = (long)(var19 * var15 * this.a(0.9, 1.12));
      } else {
         long var25;
         long var21;
         if (this.pathModel == SlothRotation.b.FLICK_SETTLE) {
            var25 = 85L;
            var21 = 190L;
         } else if (this.pathModel == SlothRotation.b.RANDOM_SPLINE) {
            var25 = 170L;
            var21 = 390L;
         } else if (this.pathModel == SlothRotation.b.CORRECTIVE) {
            var25 = 145L;
            var21 = 350L;
         } else {
            var25 = 110L;
            var21 = 290L;
         }

         var17 = (long)(this.a((double)var25, (double)var21) * var15);
      }

      if (var3 && this.pathModel != SlothRotation.b.EMPIRICAL) {
         var17 = Math.min(var17, var4 ? 245L : 310L);
      }

      double var26 = var17 / 1000.0;
      this.pathEntryX = this.d(this.anchorPathVelocityX * var26 * 0.24, -0.12, 0.12);
      this.pathEntryY = this.d(this.anchorPathVelocityY * var26 * 0.24, -0.09, 0.09);
      this.pathEntryZ = this.d(this.anchorPathVelocityZ * var26 * 0.24, -0.12, 0.12);
      this.anchorPathStartNanos = var1;
      this.anchorPathEndNanos = var1 + var17 * 1000000L;
      double var27 = this.empiricalJump ? 1.0 : (var3 ? (var4 ? 0.9 : 0.78) : (var4 ? 0.93 : 0.84));
      if (this.random.nextDouble() < var27) {
         double var23 = this.pathModel == SlothRotation.b.EMPIRICAL
            ? (this.empiricalJump ? this.a(0.92, 0.985) : (var4 ? this.a(0.78, 0.94) : this.a(0.76, 0.93)))
            : (var4 ? this.a(0.55, 0.84) : this.a(0.62, 0.89));
         this.nextAnchorNanos = this.anchorPathStartNanos + (long)((this.anchorPathEndNanos - this.anchorPathStartNanos) * var23);
      } else {
         long var28 = this.random.nextDouble() < 0.08 ? this.a(140L, 620L) : (var3 ? this.a(17L, 88L) : this.a(20L, 125L));
         this.nextAnchorNanos = this.anchorPathEndNanos + var28;
      }
   }

   private void c(long var1, boolean var3) {
      if (var1 - this.lastActionPathNanos >= 30000000L) {
         this.lastActionPathNanos = var1;
         this.b(var1);
         this.b(var1, var3, true);
      }
   }

   private void d(long var1, boolean var3) {
      this.b(var1);
      this.jumpPathRequested = true;
      this.jumpCriticalPathPending = true;
      this.b(var1, var3, false);
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

   private float a(long var1) {
      if (this.jumpGestureEndNanos > this.jumpGestureStartNanos
         && var1 > this.jumpGestureStartNanos
         && var1 < this.jumpGestureEndNanos
         && this.jumpPitchAmplitude != 0.0F) {
         double var3 = (double)(var1 - this.jumpGestureStartNanos) / (this.jumpGestureEndNanos - this.jumpGestureStartNanos);
         double var5;
         if (var3 < 0.2) {
            var5 = this.d(var3 / 0.2) * 0.58;
         } else if (var3 < 0.44) {
            var5 = this.c(0.58, 1.0, this.d((var3 - 0.2) / 0.24));
         } else if (var3 < 0.63) {
            var5 = this.c(1.0, 0.84, this.d((var3 - 0.44) / 0.19));
         } else if (var3 < 0.92) {
            var5 = this.c(0.84, 0.0, this.d((var3 - 0.63) / 0.29));
         } else {
            var5 = 0.0;
         }

         return -this.jumpPitchAmplitude * (float)var5;
      } else {
         return 0.0F;
      }
   }

   private void g() {
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

   private void b(long var1) {
      if (this.anchorPathEndNanos > this.anchorPathStartNanos) {
         double var3 = this.d((double)(var1 - this.anchorPathStartNanos) / (this.anchorPathEndNanos - this.anchorPathStartNanos), 0.0, 1.0);
         double var5 = this.anchorX;
         double var7 = this.anchorY;
         double var9 = this.anchorZ;
         double var11 = this.d(var3 + this.pathTimeWarp * var3 * (1.0 - var3), 0.0, 1.0);
         if (this.pathModel == SlothRotation.b.EMPIRICAL) {
            double var13 = this.empiricalJump ? this.c(var3, var11, 0.38) : var11;
            double var15 = this.empiricalJump
               ? SlothDatasetProfile.a(this.empiricalPathIndex, var13)
               : SlothDatasetProfile.a(this.empiricalAction, this.empiricalPathIndex, var13);
            this.anchorX = this.c(this.anchorStartX, this.anchorGoalX, var15);
            this.anchorY = this.c(this.anchorStartY, this.anchorGoalY, var15);
            this.anchorZ = this.c(this.anchorStartZ, this.anchorGoalZ, var15);
            double var17 = this.empiricalJump
               ? SlothDatasetProfile.b(this.empiricalPathIndex, var13)
               : SlothDatasetProfile.b(this.empiricalAction, this.empiricalPathIndex, var13);
            float var19 = (float)(1.0 - this.d(var13));
            this.empiricalCurveYaw = this.empiricalEntryYaw * var19 + this.empiricalPerpendicularYaw * this.empiricalAngularDistance * (float)var17;
            this.empiricalCurvePitch = this.empiricalEntryPitch * var19 + this.empiricalPerpendicularPitch * this.empiricalAngularDistance * (float)var17;
         } else if (this.pathModel == SlothRotation.b.RANDOM_SPLINE) {
            double var20 = this.b(var11);
            if (var20 < 0.5) {
               double var24 = var20 * 2.0;
               this.anchorX = this.d(this.anchorStartX, this.anchorControlOneX, this.anchorMidX, var24);
               this.anchorY = this.d(this.anchorStartY, this.anchorControlOneY, this.anchorMidY, var24);
               this.anchorZ = this.d(this.anchorStartZ, this.anchorControlOneZ, this.anchorMidZ, var24);
            } else {
               double var25 = (var20 - 0.5) * 2.0;
               this.anchorX = this.d(this.anchorMidX, this.anchorControlTwoX, this.anchorGoalX, var25);
               this.anchorY = this.d(this.anchorMidY, this.anchorControlTwoY, this.anchorGoalY, var25);
               this.anchorZ = this.d(this.anchorMidZ, this.anchorControlTwoZ, this.anchorGoalZ, var25);
            }
         } else if (this.pathModel == SlothRotation.b.FLICK_SETTLE) {
            this.anchorX = this.c(this.anchorStartX, this.anchorOvershootX, this.anchorGoalX, var11);
            this.anchorY = this.c(this.anchorStartY, this.anchorOvershootY, this.anchorGoalY, var11);
            this.anchorZ = this.c(this.anchorStartZ, this.anchorOvershootZ, this.anchorGoalZ, var11);
         } else if (this.pathModel == SlothRotation.b.CORRECTIVE) {
            this.anchorX = this.b(this.anchorStartX, this.anchorOvershootX, this.anchorGoalX, var11);
            this.anchorY = this.b(this.anchorStartY, this.anchorOvershootY, this.anchorGoalY, var11);
            this.anchorZ = this.b(this.anchorStartZ, this.anchorOvershootZ, this.anchorGoalZ, var11);
         } else {
            double var21 = this.d(var11);
            double var26 = 16.0 * var21 * var21 * (1.0 - var21) * (1.0 - var21);
            this.anchorX = this.c(this.anchorStartX, this.anchorGoalX, var21) + this.pathBendX * var26;
            this.anchorY = this.c(this.anchorStartY, this.anchorGoalY, var21) + this.pathBendY * var26;
            this.anchorZ = this.c(this.anchorStartZ, this.anchorGoalZ, var21) + this.pathBendZ * var26;
         }

         if (this.pathModel != SlothRotation.b.EMPIRICAL) {
            float var22 = (float)(1.0 - this.d(var3));
            this.empiricalCurveYaw = this.empiricalEntryYaw * var22;
            this.empiricalCurvePitch = this.empiricalEntryPitch * var22;
         }

         double var23 = 4.0 * var3 * Math.pow(1.0 - var3, 3.0);
         this.anchorX = this.anchorX + this.pathEntryX * var23;
         this.anchorY = this.anchorY + this.pathEntryY * var23;
         this.anchorZ = this.anchorZ + this.pathEntryZ * var23;
         this.anchorX = this.d(this.anchorX, 0.12, 0.88);
         this.anchorY = this.d(this.anchorY, 0.22, 0.92);
         this.anchorZ = this.d(this.anchorZ, 0.12, 0.88);
         if (this.lastAnchorSampleNanos > 0L && var1 > this.lastAnchorSampleNanos) {
            double var27 = (var1 - this.lastAnchorSampleNanos) / 1.0E9;
            if (var27 <= 0.1) {
               double var28 = 1.0 - Math.exp(-var27 / 0.045);
               this.anchorPathVelocityX = this.anchorPathVelocityX + ((this.anchorX - var5) / var27 - this.anchorPathVelocityX) * var28;
               this.anchorPathVelocityY = this.anchorPathVelocityY + ((this.anchorY - var7) / var27 - this.anchorPathVelocityY) * var28;
               this.anchorPathVelocityZ = this.anchorPathVelocityZ + ((this.anchorZ - var9) / var27 - this.anchorPathVelocityZ) * var28;
            }
         }

         this.lastAnchorSampleNanos = var1;
      }
   }

   private void a(long var1, double var3) {
      Vec3d var5 = mc.player.getVelocity();
      boolean var6 = mc.player.isOnGround() && var5.x * var5.x + var5.z * var5.z < 0.0025;
      boolean var7 = this.c(var1);
      if (var6 && !var7) {
         if (this.tremorPulseEndNanos > 0L && var1 >= this.tremorPulseEndNanos) {
            this.tremorGoalYaw = 0.0F;
            this.tremorGoalPitch = 0.0F;
            this.tremorPulseEndNanos = 0L;
            this.nextTremorNanos = var1 + this.a(200L, 700L);
         }

         if (this.tremorPulseEndNanos == 0L && var1 >= this.nextTremorNanos) {
            float var8 = Math.max(0.001F, GCDUtil2.a());
            double var9 = this.random.nextDouble();
            int var11 = var9 < 0.72 ? 1 : (var9 < 0.96 ? 2 : 3);
            double var12 = this.random.nextDouble();
            int var14 = var12 < 0.6 ? 0 : (var12 < 0.92 ? 1 : 2);
            float var15 = this.random.nextBoolean() ? 1.0F : -1.0F;
            float var16 = this.random.nextBoolean() ? 1.0F : -1.0F;
            this.tremorGoalYaw = var11 * var8 * var15;
            this.tremorGoalPitch = var14 * var8 * var16;
            this.tremorPulseEndNanos = var1 + this.a(24L, 70L);
         }
      } else {
         this.tremorGoalYaw = 0.0F;
         this.tremorGoalPitch = 0.0F;
         this.tremorPulseEndNanos = 0L;
         if (var1 >= this.nextTremorNanos) {
            this.nextTremorNanos = var1 + this.a(240L, 800L);
         }
      }

      boolean var17 = this.tremorPulseEndNanos > var1;
      double var18 = 1.0 - Math.exp(-var3 / (var17 ? 0.018 : 0.035));
      this.tremorYaw = this.tremorYaw + (this.tremorGoalYaw - this.tremorYaw) * (float)var18;
      this.tremorPitch = this.tremorPitch + (this.tremorGoalPitch - this.tremorPitch) * (float)var18;
      float var19 = var6 && !var7 ? 1.0F : 0.0F;
      double var20 = 1.0 - Math.exp(-var3 / (var19 > this.tremorBlend ? 0.08 : 0.045));
      this.tremorBlend = this.tremorBlend + (var19 - this.tremorBlend) * (float)var20;
      double var21 = 1.0 - Math.exp(-var3 / (var7 ? 0.12 : 0.3));
      float var22 = var7 ? this.pathNoiseScale : 0.1F;
      this.noiseMotionBlend = this.noiseMotionBlend + (var22 - this.noiseMotionBlend) * (float)var21;
   }

   private boolean c(long var1) {
      double var3 = Math.sqrt(
         this.anchorPathVelocityX * this.anchorPathVelocityX
            + this.anchorPathVelocityY * this.anchorPathVelocityY
            + this.anchorPathVelocityZ * this.anchorPathVelocityZ
      );
      double var5 = Math.hypot(this.yawVelocity, this.pitchVelocity);
      return var1 < this.anchorPathEndNanos && var3 > 0.12 || var5 > 24.0;
   }

   private void a(long var1, boolean var3, float var4, float var5) {
      if (var1 >= this.nextLapseNanos && var1 >= this.lapseEndNanos) {
         if (var3) {
            this.nextLapseNanos = var1 + this.a(700L, 1800L);
         } else if (this.turnBoost > 1.2F) {
            this.nextLapseNanos = var1 + this.a(150L, 320L);
         } else {
            float var6 = Math.min(var4 + MathHelper.clamp(var4 * this.d(0.25F, 1.1F), 1.5F, 8.0F), 24.0F);
            float var7 = Math.min(var5 * 0.5F + MathHelper.clamp(var5 * this.d(0.35F, 0.8F), 1.2F, 6.0F), 14.0F);
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

            this.lapsePeakProgress = this.d(0.28F, 0.45F);
            this.lapseRiseExponent = this.d(2.0F, 2.8F);
            this.lapseStartNanos = var1;
            this.lapseEndNanos = var1 + this.a(240L, 480L);
            this.nextLapseNanos = this.lapseEndNanos + this.a(480L, 1500L);
         }
      }
   }

   private void d(long var1) {
      float var3 = this.e(var1);
      if (var3 <= 0.0F) {
         this.lapseStartNanos = 0L;
         this.lapseEndNanos = 0L;
         this.lapseYaw = 0.0F;
         this.lapsePitch = 0.0F;
      } else {
         this.lapseYaw *= var3;
         this.lapsePitch *= var3;
         this.lapsePeakProgress = 0.0F;
         this.lapseStartNanos = var1;
         this.lapseEndNanos = var1 + this.a(70L, 105L);
      }

      this.nextLapseNanos = var1 + this.a(900L, 2400L);
   }

   private float e(long var1) {
      if (this.lapseEndNanos > this.lapseStartNanos && var1 >= this.lapseStartNanos && var1 < this.lapseEndNanos) {
         double var3 = this.d((double)(var1 - this.lapseStartNanos) / (this.lapseEndNanos - this.lapseStartNanos), 0.0, 1.0);
         if (var3 < this.lapsePeakProgress) {
            double var7 = var3 / this.lapsePeakProgress;
            return (float)(1.0 - Math.pow(1.0 - var7, this.lapseRiseExponent));
         } else {
            double var5 = (var3 - this.lapsePeakProgress) / (1.0 - this.lapsePeakProgress);
            return (float)(1.0 - this.d(var5));
         }
      } else {
         return 0.0F;
      }
   }

   private SlothRotation.b d(boolean var1) {
      SlothRotation.b var2 = this.previousPathModel;

      for (int var3 = 0; var3 < 8; var3++) {
         double var4 = this.random.nextDouble();
         if (var1) {
            var2 = var4 < 0.5
               ? SlothRotation.b.EMPIRICAL
               : (var4 < 0.68 ? SlothRotation.b.MINIMUM_JERK : (var4 < 0.85 ? SlothRotation.b.CORRECTIVE : SlothRotation.b.RANDOM_SPLINE));
         } else {
            var2 = var4 < 0.48
               ? SlothRotation.b.EMPIRICAL
               : (var4 < 0.69 ? SlothRotation.b.MINIMUM_JERK : (var4 < 0.86 ? SlothRotation.b.CORRECTIVE : SlothRotation.b.RANDOM_SPLINE));
         }

         if ((var2 != SlothRotation.b.FLICK_SETTLE || this.previousPathModel != SlothRotation.b.FLICK_SETTLE)
            && (var2 != this.previousPathModel || this.repeatedPathModels < 2 || var2 == SlothRotation.b.EMPIRICAL)) {
            break;
         }
      }

      if (var2 == this.previousPathModel) {
         this.repeatedPathModels++;
      } else {
         this.previousPathModel = var2;
         this.repeatedPathModels = 1;
      }

      return var2;
   }

   private Vec3d e(boolean var1) {
      int var2 = var1 ? 24 : 28;
      Vec3d[] var3 = new Vec3d[var2];
      int[] var4 = new int[var2];
      double[] var5 = new double[var2];
      double[] var6 = new double[var2];
      double[] var7 = new double[var2];
      double var8 = var1 ? 0.05 : 0.07;
      double var10 = var1 ? 0.02 : 0.025;

      for (int var12 = 0; var12 < var2; var12++) {
         Vec3d var13 = this.f(var1);
         int var14 = this.a(var13);
         double var15 = this.aimHistorySize == 0 ? this.a(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ) : Double.MAX_VALUE;
         double var17 = this.aimHistorySize == 0 ? this.b(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ) : Double.MAX_VALUE;
         double var19 = 0.0;

         for (int var21 = 0; var21 < this.aimHistorySize; var21++) {
            int var22 = (this.aimHistoryCursor - 1 - var21 + 4) % 4;
            var15 = Math.min(var15, this.a(var13.x, var13.y, var13.z, this.aimHistoryX[var22], this.aimHistoryY[var22], this.aimHistoryZ[var22]));
            var17 = Math.min(var17, this.b(var13.x, var13.y, var13.z, this.aimHistoryX[var22], this.aimHistoryY[var22], this.aimHistoryZ[var22]));
            if (this.aimHistoryZone[var22] == var14) {
               var19 += 0.025 / (var21 + 1.0);
            }
         }

         double var27 = this.a(var13.x, var13.y, var13.z, this.anchorX, this.anchorY, this.anchorZ);
         double var23 = var15 * 0.22 + var17 * 0.35 + var27 * 0.06 + this.random.nextDouble() * 0.75 - var19;
         if (var14 == this.lastAimZone) {
            var23 -= 0.08;
         }

         if (var14 == this.previousAimZone) {
            var23 -= 0.035;
         }

         var3[var12] = var13;
         var4[var12] = var14;
         var5[var12] = var23;
         var6[var12] = var15;
         var7[var12] = var17;
      }

      int var25 = this.a(var5, var6, var7, var8, var10);
      if (var25 < 0) {
         var25 = this.a(var5, var6, var7, var8 * 0.55, var10 * 0.55);
      }

      if (var25 < 0) {
         var25 = this.a(var5, var6, var7, 0.018, 0.008);
      }

      if (var25 < 0) {
         return new Vec3d(this.anchorX, this.anchorY, this.anchorZ);
      }

      Vec3d var26 = var3[var25];
      this.a(var26, var4[var25]);
      return var26;
   }

   private int a(double[] var1, double[] var2, double[] var3, double var4, double var6) {
      double var8 = 2.0;
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
            var17 += 0.3 + Math.exp((var1[var14] - var10) * 0.6);
         }
      }

      if (var17 <= 0.0) {
         return -1;
      }

      double var18 = this.random.nextDouble() * var17;

      for (int var16 = 0; var16 < var1.length; var16++) {
         if (!(var2[var16] < var4) && !(var3[var16] < var6) && !(var1[var16] < var10 - var8)) {
            var18 -= 0.3 + Math.exp((var1[var16] - var10) * 0.6);
            if (var18 <= 0.0) {
               return var16;
            }
         }
      }

      return -1;
   }

   private Vec3d f(boolean var1) {
      double var2 = this.random.nextDouble();
      double var4;
      double var6;
      double var8;
      if (var2 < 0.7) {
         double var10 = this.random.nextGaussian();
         double var12 = this.random.nextGaussian();
         double var14 = this.anchorX * 0.68 + 0.16;
         double var16 = this.anchorY * 0.62 + 0.2622;
         double var18 = this.anchorZ * 0.68 + 0.16;
         double var20 = var1 ? 0.075 : 0.11;
         var4 = var14 + var10 * var20;
         var8 = var18 + var12 * var20;
         var6 = var16 + this.random.nextGaussian() * (var1 ? 0.075 : 0.11);
      } else if (var2 < 0.92) {
         double var25 = this.random.nextGaussian();
         double var27 = this.random.nextGaussian();
         var4 = 0.5 + var25 * (var1 ? 0.14 : 0.19);
         var8 = 0.5 + var27 * (var1 ? 0.14 : 0.19);
         var6 = 0.66 + this.random.nextGaussian() * (var1 ? 0.12 : 0.17);
      } else {
         double var26 = this.random.nextDouble() * Math.PI * 2.0;
         double var28 = var1 ? this.a(0.2, 0.34) : this.a(0.24, 0.4);
         var4 = 0.5 + Math.cos(var26) * var28;
         var8 = 0.5 + Math.sin(var26) * var28;
         var6 = this.a(var1 ? 0.42 : 0.32, var1 ? 0.88 : 0.92);
      }

      if (var1) {
         var4 = this.b(var4, 0.16, 0.84);
         var6 = this.b(var6, 0.36, 0.9);
         var8 = this.b(var8, 0.16, 0.84);
      } else {
         var4 = this.b(var4, 0.12, 0.88);
         var6 = this.b(var6, 0.29, 0.92);
         var8 = this.b(var8, 0.12, 0.88);
      }

      return new Vec3d(var4, var6, var8);
   }

   private double b(double var1, double var3, double var5) {
      double var7 = var1;

      for (int var9 = 0; var9 < 3 && (var7 < var3 || var7 > var5); var9++) {
         var7 = var7 < var3 ? var3 + (var3 - var7) : var5 - (var7 - var5);
      }

      return this.d(var7, var3, var5);
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
      this.aimHistoryCursor = (this.aimHistoryCursor + 1) % 4;
      this.aimHistorySize = Math.min(this.aimHistorySize + 1, 4);
      this.previousAimZone = this.lastAimZone;
      this.lastAimZone = var2;
   }

   private void h() {
      this.aimHistorySize = 0;
      this.aimHistoryCursor = 0;
      this.lastAimZone = -1;
      this.previousAimZone = -1;

      for (int var1 = 0; var1 < 4; var1++) {
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

   private double b(double var1, double var3, double var5, double var7) {
      double var9 = this.c(this.d(var7 / this.pathPrimaryEnd, 0.0, 1.0));
      double var11 = this.b(this.d((var7 - this.pathCorrectionStart) / (1.0 - this.pathCorrectionStart), 0.0, 1.0));
      return this.c(var1, var3, var9) + (var5 - var3) * var11;
   }

   private double c(double var1, double var3, double var5, double var7) {
      double var9 = this.d(var7 / this.pathPrimaryEnd, 0.0, 1.0);
      double var11 = 1.0 - Math.pow(1.0 - var9, this.pathFlickExponent);
      double var13 = this.d(this.d((var7 - this.pathCorrectionStart) / (1.0 - this.pathCorrectionStart), 0.0, 1.0));
      return this.c(var1, var3, var11) + (var5 - var3) * var13;
   }

   private double b(double var1) {
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

   private double c(double var1) {
      return var1 - Math.sin((Math.PI * 2) * var1) / (Math.PI * 2);
   }

   private double d(double var1) {
      double var3 = var1 * var1;
      double var5 = var3 * var1;
      return 10.0 * var5 - 15.0 * var5 * var1 + 6.0 * var5 * var3;
   }

   private double d(double var1, double var3, double var5, double var7) {
      double var9 = 1.0 - var7;
      return var9 * var9 * var1 + 2.0 * var9 * var7 * var3 + var7 * var7 * var5;
   }

   private double c(double var1, double var3, double var5) {
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

   private float i() {
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

   private double d(double var1, double var3, double var5) {
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
      EMPIRICAL,
      MINIMUM_JERK,
      RANDOM_SPLINE,
      FLICK_SETTLE,
      CORRECTIVE;
   }
}
