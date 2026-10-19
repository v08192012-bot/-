package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class HolyWorldElytraPredictor {
   private static final int HORIZON_STEPS = 11;
   private static final float MAX_PITCH_DELTA = 90.0F;
   private static final int SAMPLE_COUNT = 24;
   private static final double SAMPLE_STEP = 2.0;
   private final HolyWorldTrajectoryPlanner planner = new HolyWorldTrajectoryPlanner(11);
   private final HolyWorldTerrainProfile profile = new HolyWorldTerrainProfile(24, 2.0);
   private final HolyWorldElytraPhysics.a state = new HolyWorldElytraPhysics.a();
   private boolean jumpRequested;
   private boolean sprintRequested;
   private boolean active;
   private boolean prevJump;
   private boolean inputJump;
   private int glideCooldown;
   private float targetPitch = 0.0F;

   public boolean isActive() {
      return this.active;
   }

   public boolean isJumpRequested() {
      return this.jumpRequested;
   }

   public boolean isSprintRequested() {
      return this.sprintRequested;
   }

   public float getTargetPitch() {
      return this.targetPitch;
   }

   public void e() {
      this.active = false;
      this.jumpRequested = false;
      this.sprintRequested = false;
      this.glideCooldown = 0;
      this.prevJump = false;
      this.inputJump = false;
   }

   public void a(MinecraftClient var1) {
      this.active = false;
      this.jumpRequested = false;
      this.sprintRequested = false;
      ClientPlayerEntity var2 = var1.player;
      if (var2 != null && var1.world != null && var2.networkHandler != null) {
         if (!this.a(var1, var2)) {
            this.prevJump = false;
            this.inputJump = false;
         } else {
            boolean var3 = var2.isGliding();
            boolean var4 = var2.isOnGround();
            if (var3) {
               this.glideCooldown = 10;
            } else if (this.glideCooldown > 0) {
               this.glideCooldown--;
            }

            if (!var2.input.hasForwardMovement()) {
               this.prevJump = false;
               this.inputJump = false;
            } else {
               boolean var5 = var4 || !var3 && this.a(var2);
               boolean var6 = this.inputJump ? this.prevJump : var1.options.jumpKey.isPressed();
               this.active = true;
               this.inputJump = true;
               this.jumpRequested = var5 && !var6;
               this.sprintRequested = true;
               this.prevJump = this.jumpRequested;
               if (var3 || this.glideCooldown > 0) {
                  this.a(var1, var2, var3, var4);
               }
            }
         }
      }
   }

   private void a(MinecraftClient var1, ClientPlayerEntity var2, boolean var3, boolean var4) {
      double var5 = Math.toRadians(var2.getYaw());
      double var7 = -Math.sin(var5);
      double var9 = Math.cos(var5);
      this.a(var1, var2, var7, var9);
      this.a(var2, var3, var4, var7, var9);
      float var11 = var2.getPitch();
      int var12 = this.b(var1);
      float var13 = this.planner.a(this.state, var11, 90.0F, this.profile, var12);
      this.targetPitch = MathHelper.clamp(var13, -90.0F, 90.0F);
   }

   private void a(ClientPlayerEntity var1, boolean var2, boolean var3, double var4, double var6) {
      Vec3d var8 = var1.getVelocity();
      this.state.posX = 0.0;
      this.state.posY = var1.getY();
      this.state.motionX = Math.max(0.0, var8.x * var4 + var8.z * var6);
      this.state.motionY = var8.y;
      this.state.isGliding = var2;
      this.state.onGround = var3;
      this.state.hasJumped = this.prevJump;
      this.state.fallCooldown = -1;
   }

   private void a(MinecraftClient var1, ClientPlayerEntity var2, double var3, double var5) {
      this.profile.a();
      Vec3d var7 = var2.getEntityPos();

      for (int var8 = 0; var8 < this.profile.b(); var8++) {
         double var9 = var8 * this.profile.getStepSize();
         double var11 = var7.x + var3 * var9;
         double var13 = var7.z + var5 * var9;
         this.profile.a(this.a(var1, var2, var11, var7.y, var13));
      }
   }

   private double a(MinecraftClient var1, ClientPlayerEntity var2, double var3, double var5, double var7) {
      double var9 = this.a(var1, var2, var3, var5 + 2.5, var7, 32.0);
      if (!Double.isNaN(var9)) {
         return var9;
      }

      if (var1.world.getBlockState(BlockPos.ofFloored(var3, var5 + 2.5, var7)).isAir()) {
         return -4096.0;
      }

      double var11 = this.a(var1, var2, var3, var5 + 24.0, var7, 21.5);
      return Double.isNaN(var11) ? var5 + 24.0 : var11;
   }

   private double a(MinecraftClient var1, ClientPlayerEntity var2, double var3, double var5, double var7, double var9) {
      Vec3d var11 = new Vec3d(var3, var5, var7);
      Vec3d var12 = var11.add(0.0, -var9, 0.0);
      BlockHitResult var13 = var1.world.raycast(new RaycastContext(var11, var12, ShapeType.COLLIDER, FluidHandling.ANY, var2));
      return var13.getType() == Type.MISS ? Double.NaN : var13.getPos().y;
   }

   private int b(MinecraftClient var1) {
      if (var1.getNetworkHandler() != null && var1.player != null) {
         PlayerListEntry var2 = var1.getNetworkHandler().getPlayerListEntry(var1.player.getUuid());
         if (var2 != null) {
            return MathHelper.clamp(1 + var2.getLatency() / 50, 1, 6);
         }
      }

      return 2;
   }

   private boolean a(MinecraftClient var1, ClientPlayerEntity var2) {
      return var1.currentScreen == null
         && !var2.isSpectator()
         && !var2.hasVehicle()
         && !var2.isClimbing()
         && !var2.isTouchingWater()
         && !var2.isInLava()
         && !var2.getAbilities().flying
         && this.b(var2);
   }

   private boolean a(ClientPlayerEntity var1) {
      return !var1.isOnGround() && !var1.isTouchingWater() && this.b(var1);
   }

   private boolean b(ClientPlayerEntity var1) {
      for (EquipmentSlot var3 : EquipmentSlot.VALUES) {
         ItemStack var4 = var1.getEquippedStack(var3);
         if (LivingEntity.canGlideWith(var4, var3)) {
            return true;
         }
      }

      return false;
   }
}
