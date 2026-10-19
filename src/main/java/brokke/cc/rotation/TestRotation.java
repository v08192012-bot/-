package brokke.cc.rotation;

import brokke.cc.module.combat.Aura;
import brokke.cc.util.GCDUtil2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class TestRotation extends RotationsSystem {
   private static final Path DATASET_PATH = Path.of(System.getProperty("user.home"), "Desktop", "data.json");
   private final List<TestRotation.a> frames = new ArrayList<>();
   private LivingEntity trackedTarget;
   private LivingEntity trackedRotationTarget;
   private Vec3d currentAimPoint;
   private Vec3d targetAimPoint;
   private long lastModified = Long.MIN_VALUE;
   private long lastLoadAttempt;
   private boolean datasetReady;
   private int playbackIndex;
   private int aimPointTicks;
   private int aimPointRefreshTicks;
   private int smoothProfileTicks;
   private float smoothYawStep;
   private float smoothPitchStep;
   private float smoothYaw;
   private float smoothPitch;
   private float yawSmoothFactor = 1.0F;
   private float pitchSmoothFactor = 1.0F;
   private boolean hasRotationState;

   public void a() {
      this.trackedTarget = null;
      this.trackedRotationTarget = null;
      this.currentAimPoint = null;
      this.targetAimPoint = null;
      this.playbackIndex = 0;
      this.aimPointTicks = 0;
      this.aimPointRefreshTicks = 0;
      this.smoothProfileTicks = 0;
      this.smoothYawStep = 0.0F;
      this.smoothPitchStep = 0.0F;
      this.smoothYaw = 0.0F;
      this.smoothPitch = 0.0F;
      this.yawSmoothFactor = 1.0F;
      this.pitchSmoothFactor = 1.0F;
      this.hasRotationState = false;
   }

   @Override
   public void a(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         boolean var2 = this.b();
         this.c();
         Vec3d var3 = this.a(var1, var2);
         Vec2f var4 = RotationUtils.b(var3);
         if (this.datasetReady && !this.frames.isEmpty()) {
            float var5 = mc.player.getYaw();
            float var6 = mc.player.getPitch();
            this.a(var1, var5, var6);
            float var7 = MathHelper.wrapDegrees(var4.x - this.smoothYaw);
            float var8 = var4.y - this.smoothPitch;
            TestRotation.a var9 = this.a(var7, var8, var2);
            this.a(var9, var7, var8, var2);
            float var10 = Math.max(GCDUtil2.a(), 1.0E-4F);
            float var11 = this.a(var7, var9, true, var2);
            float var12 = this.a(var8, var9, false, var2);
            var11 += this.a(var9, var7, true, var10);
            var12 += this.a(var9, var8, false, var10);
            this.smoothYawStep = this.a(this.smoothYawStep, var11, var7, true, var2);
            this.smoothPitchStep = this.a(this.smoothPitchStep, var12, var8, false, var2);
            float var13 = this.a(this.smoothYawStep, var7, var10, true);
            float var14 = this.a(this.smoothPitchStep, var8, var10, false);
            this.smoothYaw = MathHelper.wrapDegrees(this.smoothYaw + var13);
            this.smoothPitch = MathHelper.clamp(this.smoothPitch + var14, -89.0F, 89.0F);
            RotationStorage.a(new Rotation2(this.smoothYaw, this.smoothPitch), 360.0F, 360.0F, 45.0F, 45.0F, 0, 1, Aura.clientLook.isEnabled());
         } else {
            RotationStorage.a(new Rotation2(var4.x, MathHelper.clamp(var4.y, -89.0F, 89.0F)), 360.0F, 360.0F, 45.0F, 45.0F, 0, 1, Aura.clientLook.isEnabled());
         }
      }
   }

   private boolean b() {
      float var1 = mc.player.getAttackCooldownProgress(1.5F);
      boolean var2 = !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0;
      return var1 >= 0.88F || var2;
   }

   private void c() {
      long var1 = System.currentTimeMillis();
      if (this.a(var1)) {
         this.lastLoadAttempt = var1;
         long var3 = this.d();
         if (!this.datasetReady || var3 != this.lastModified) {
            this.frames.clear();
            this.datasetReady = false;
            if (!Files.exists(DATASET_PATH)) {
               this.lastModified = Long.MIN_VALUE;
            } else {
               try (BufferedReader var5 = Files.newBufferedReader(DATASET_PATH)) {
                  for (JsonElement var8 : JsonParser.parseReader(var5).getAsJsonArray()) {
                     if (var8.isJsonObject()) {
                        TestRotation.a var9 = this.a(var8.getAsJsonObject());
                        if (var9 != null) {
                           this.frames.add(var9);
                        }
                     }
                  }

                  this.datasetReady = !this.frames.isEmpty();
                  this.lastModified = var3;
                  this.a();
               } catch (IOException | IllegalStateException var12) {
                  this.datasetReady = false;
                  this.lastModified = Long.MIN_VALUE;
                  this.a();
               }
            }
         }
      }
   }

   private boolean a(long var1) {
      return this.datasetReady && !this.frames.isEmpty() ? var1 - this.lastLoadAttempt >= 3000L : var1 - this.lastLoadAttempt >= 1500L;
   }

   private long d() {
      try {
         return Files.exists(DATASET_PATH) ? Files.getLastModifiedTime(DATASET_PATH).toMillis() : Long.MIN_VALUE;
      } catch (IOException var2) {
         return Long.MIN_VALUE;
      }
   }

   private TestRotation.a a(JsonObject var1) {
      float var2 = this.a(var1, "fromYaw");
      float var3 = this.a(var1, "toYaw");
      float var4 = this.a(var1, "fromPitch");
      float var5 = this.a(var1, "toPitch");
      float var6 = MathHelper.wrapDegrees(var3 - var2);
      float var7 = var5 - var4;
      float var8 = Math.abs(var6);
      float var9 = Math.abs(var7);
      float var10 = Math.max(this.a(var1, "deltaYaw"), var8);
      float var11 = Math.max(this.a(var1, "deltaPitch"), var9);
      if (var10 <= 0.0F && var11 <= 0.0F) {
         return null;
      }

      TestRotation.a var12 = new TestRotation.a();
      var12.deltaYaw = var10;
      var12.deltaPitch = var11;
      var12.signedYaw = var6 != 0.0F ? var6 : Math.signum(this.a(var1, "jitterYawDir")) * var10;
      var12.signedPitch = var7 != 0.0F ? var7 : Math.signum(this.a(var1, "jitterPitchDir")) * var11;
      var12.rotationSpeed = Math.max(this.a(var1, "rotationSpeed"), 0.0F);
      var12.jitterScore = Math.max(this.a(var1, "jitterScore"), 0.0F);
      var12.jitterYawSpeed = Math.max(this.a(var1, "jitterYawSpeed"), 0.0F);
      var12.jitterPitchSpeed = Math.max(this.a(var1, "jitterPitchSpeed"), 0.0F);
      var12.isJittering = this.b(var1, "isJittering");
      var12.attacking = this.b(var1, "attacking");
      var12.combatFrame = this.b(var1, "isCombatFrame");
      var12.instantSnap = this.b(var1, "isInstantSnap");
      var12.timeDeltaMs = Math.max(1L, var1.has("timeDeltaMs") ? var1.get("timeDeltaMs").getAsLong() : 50L);
      return var12;
   }

   private TestRotation.a a(float var1, float var2, boolean var3) {
      float var4 = Math.abs(var1) + Math.abs(var2) * 0.82F;
      int var5 = this.frames.size();
      int var6 = Math.min(var5, var3 ? 78 : 56);
      int var7 = this.playbackIndex % var5;
      float var8 = Float.MAX_VALUE;

      for (int var9 = 0; var9 < var6; var9++) {
         int var10 = (this.playbackIndex + var9) % var5;
         TestRotation.a var11 = this.frames.get(var10);
         float var12 = var11.deltaYaw + var11.deltaPitch * 0.82F;
         float var13 = Math.abs(var12 - var4);
         if (var3) {
            if (!var11.a()) {
               var13 += 3.0F;
            }

            if (var11.instantSnap) {
               var13 -= 0.5F;
            }
         } else if (var11.a()) {
            var13 += 1.6F;
         }

         if (var4 < 10.0F && var11.isJittering) {
            var13 -= Math.min(var11.jitterScore, 2.6F) * 0.2F;
         }

         float var14 = var13 + var9 * 0.032F;
         if (var14 < var8) {
            var8 = var14;
            var7 = var10;
         }
      }

      this.playbackIndex = (var7 + 1) % var5;
      return this.frames.get(var7);
   }

   private void a(TestRotation.a var1, float var2, float var3, boolean var4) {
      if (this.smoothProfileTicks > 0) {
         this.smoothProfileTicks--;
      } else {
         ThreadLocalRandom var5 = ThreadLocalRandom.current();
         float var6 = MathHelper.clamp((Math.abs(var2) + Math.abs(var3)) / 32.0F, 0.0F, 1.0F);
         float var7 = MathHelper.clamp((float)var1.timeDeltaMs / 120.0F, 0.0F, 1.0F);
         float var8 = var4 ? 0.94F : 0.86F;
         float var9 = var4 ? 1.12F : 1.04F;
         float var10 = var4 ? 0.92F : 0.84F;
         float var11 = var4 ? 1.08F : 1.0F;
         this.yawSmoothFactor = var5.nextFloat(var8, var9 + var6 * 0.08F + var7 * 0.04F);
         this.pitchSmoothFactor = var5.nextFloat(var10, var11 + var6 * 0.06F + var7 * 0.03F);
         if (var1.a()) {
            this.yawSmoothFactor *= 1.02F;
            this.pitchSmoothFactor *= 1.015F;
         }

         this.smoothProfileTicks = var5.nextInt(var4 ? 2 : 3, var4 ? 6 : 8);
      }
   }

   private float a(float var1, TestRotation.a var2, boolean var3, boolean var4) {
      float var5 = Math.abs(var1);
      if (var5 <= 1.0E-4F) {
         return 0.0F;
      }

      float var6 = var3 ? var2.deltaYaw : var2.deltaPitch;
      float var7 = 0.3F + MathHelper.clamp(var2.rotationSpeed * (var3 ? 3.4F : 2.8F), 0.0F, var3 ? 0.2F : 0.16F);
      float var8 = MathHelper.clamp(var5 / (var3 ? 105.0F : 82.0F), 0.09F, var3 ? 0.52F : 0.46F);
      float var9 = Math.max(var6 * Math.max(var7, var8), var3 ? 0.03F : 0.024F);
      if (var2.instantSnap) {
         var9 = Math.max(var9, var5 * (var3 ? 0.085F : 0.065F));
      }

      if (var2.attacking || var2.combatFrame) {
         var9 *= var3 ? 1.02F : 1.015F;
      }

      float var12 = var9 * (var3 ? 0.5F : 0.46F);
      float var10 = var3 ? 6.0F : 4.0F;
      if (var5 < var10) {
         float var11 = 1.0F + (var10 - var5) / var10 * 0.18F;
         var12 *= var11;
      }

      float var15 = var3 ? Math.max(0.48F, var5 * (var2.instantSnap ? 0.11F : 0.065F)) : Math.max(0.34F, var5 * (var2.instantSnap ? 0.09F : 0.058F));
      var9 = Math.min(var12, var15);
      var9 = Math.min(var9, var5);
      return Math.signum(var1) * var9;
   }

   private float a(TestRotation.a var1, float var2, boolean var3, float var4) {
      float var5 = Math.abs(var2);
      if (var5 > (var3 ? 6.5F : 4.8F)) {
         return 0.0F;
      }

      float var6 = var3 ? var1.jitterYawSpeed : var1.jitterPitchSpeed;
      float var7 = var4 * MathHelper.clamp(var1.jitterScore * 0.01F, 0.0F, var3 ? 0.15F : 0.11F);
      float var10 = var7 + var4 * MathHelper.clamp(var6 * (var3 ? 1.3F : 1.0F), 0.0F, var3 ? 0.1F : 0.07F);
      if (var1.isJittering) {
         var10 *= 1.05F;
      }

      if (var10 <= 0.0F) {
         return 0.0F;
      }

      float var8 = ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F;
      float var9 = var10 * ThreadLocalRandom.current().nextFloat(0.3F, 0.95F) * var8;
      if (Math.abs(var9) > var5 && Math.signum(var9) == Math.signum(var2)) {
         var9 = var2;
      }

      return var9;
   }

   private void a(LivingEntity var1, float var2, float var3) {
      if (!this.hasRotationState || this.trackedRotationTarget != var1) {
         this.trackedRotationTarget = var1;
         this.smoothYaw = var2;
         this.smoothPitch = var3;
         this.smoothYawStep = 0.0F;
         this.smoothPitchStep = 0.0F;
         this.yawSmoothFactor = 1.0F;
         this.pitchSmoothFactor = 1.0F;
         this.smoothProfileTicks = 0;
         this.hasRotationState = true;
      }
   }

   private float a(float var1, float var2, float var3, boolean var4, boolean var5) {
      float var6 = Math.abs(var3);
      if (var6 <= 1.0E-4F) {
         return 0.0F;
      }

      float var7 = var4 ? (var5 ? 0.092F : 0.06F) : (var5 ? 0.082F : 0.055F);
      float var8 = var7 * (var4 ? this.yawSmoothFactor : this.pitchSmoothFactor);
      float var9 = var1 + (var2 - var1) * MathHelper.clamp(var8, 0.025F, 0.16F);
      float var10 = var4 ? 0.13F : 0.1F;
      float var11 = var4 ? (var5 ? 0.056F : 0.036F) : (var5 ? 0.046F : 0.032F);
      float var12 = var4 ? this.yawSmoothFactor : this.pitchSmoothFactor;
      float var13 = var10 + var6 * var11 * MathHelper.clamp(var12, 0.88F, 1.18F);
      float var14 = var4 ? 5.5F : 3.8F;
      if (var6 < var14) {
         var13 *= 1.12F;
      }

      float var15 = MathHelper.clamp(var9, -var13, var13);
      if (Math.abs(var3) < Math.abs(var15) && Math.signum(var3) == Math.signum(var15)) {
         var15 = var3;
      }

      return var15;
   }

   private float a(float var1, float var2, float var3, boolean var4) {
      float var5 = var1;
      if (Math.abs(var2) < Math.abs(var5) && Math.signum(var2) == Math.signum(var5)) {
         var5 = var2;
      }

      float var6 = Math.round(var5 / var3) * var3;
      if (var6 == 0.0F && Math.abs(var5) >= var3 * 0.2F) {
         var6 = Math.signum(var5) * var3;
      }

      if (Math.abs(var2) < Math.abs(var6) && Math.signum(var2) == Math.signum(var6)) {
         var6 = var2;
      }

      if (!var4) {
         var6 = MathHelper.clamp(var6, -89.0F, 89.0F);
      }

      return var6;
   }

   private Vec3d a(LivingEntity var1, boolean var2) {
      if (this.trackedTarget == var1 && this.currentAimPoint != null && this.targetAimPoint != null) {
         if (this.aimPointTicks++ >= this.aimPointRefreshTicks) {
            this.targetAimPoint = this.b(var1, var2);
            this.aimPointTicks = 0;
            this.aimPointRefreshTicks = this.a(var2);
         }

         float var3 = var2 ? 0.06F : 0.04F;
         this.currentAimPoint = new Vec3d(
            MathHelper.lerp(var3, this.currentAimPoint.x, this.targetAimPoint.x),
            MathHelper.lerp(var3, this.currentAimPoint.y, this.targetAimPoint.y),
            MathHelper.lerp(var3, this.currentAimPoint.z, this.targetAimPoint.z)
         );
         return this.currentAimPoint;
      } else {
         this.trackedTarget = var1;
         this.targetAimPoint = this.b(var1, var2);
         this.currentAimPoint = this.targetAimPoint;
         this.aimPointTicks = 0;
         this.aimPointRefreshTicks = this.a(var2);
         return this.currentAimPoint;
      }
   }

   private int a(boolean var1) {
      return ThreadLocalRandom.current().nextInt(var1 ? 7 : 10, var1 ? 13 : 18);
   }

   private Vec3d b(LivingEntity var1, boolean var2) {
      Box var3 = this.c(var1);
      ThreadLocalRandom var4 = ThreadLocalRandom.current();
      double var5 = MathHelper.lerp(var4.nextDouble(0.45, 0.55), var3.minX, var3.maxX);
      double var7 = MathHelper.lerp(var4.nextDouble(var2 ? 0.53 : 0.49, var2 ? 0.7 : 0.76), var3.minY, var3.maxY);
      double var9 = MathHelper.lerp(var4.nextDouble(0.45, 0.55), var3.minZ, var3.maxZ);
      return new Vec3d(var5, var7, var9);
   }

   private float a(JsonObject var1, String var2) {
      return var1.has(var2) ? var1.get(var2).getAsFloat() : 0.0F;
   }

   private boolean b(JsonObject var1, String var2) {
      return var1.has(var2) && var1.get(var2).getAsBoolean();
   }

   static class a {
      float deltaYaw;
      float deltaPitch;
      float signedYaw;
      float signedPitch;
      float rotationSpeed;
      float jitterScore;
      float jitterYawSpeed;
      float jitterPitchSpeed;
      long timeDeltaMs;
      boolean isJittering;
      boolean attacking;
      boolean combatFrame;
      boolean instantSnap;

      boolean a() {
         return this.attacking || this.combatFrame || this.instantSnap;
      }
   }
}
