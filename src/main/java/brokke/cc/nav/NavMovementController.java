package brokke.cc.nav;

import brokke.cc.core.MovementType;
import brokke.cc.module.movement.Baritone;
import brokke.cc.system.events.impl.EventMoveInput;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class NavMovementController {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private List<PathNode> currentPath;
   private int currentIndex;
   private boolean active;
   private boolean shouldForward;
   private boolean shouldJump;
   private boolean shouldSprint;
   private float strafe;
   private Vec3d lastPos = Vec3d.ZERO;
   private int stuckTicks;
   private boolean stuck;
   private boolean surfacingForAir;
   private int lastIndex = -1;
   private int nodeTicks;
   private long pathSeed;
   private int sprintJumpCount;
   private int sprintJumpCooldown;
   private int microStrafeTimer;
   private int alignTicks;

   public void a(List<PathNode> var1) {
      this.currentPath = var1;
      this.currentIndex = 0;
      this.active = var1 != null && !var1.isEmpty();
      this.shouldForward = false;
      this.shouldJump = false;
      this.shouldSprint = false;
      this.strafe = 0.0F;
      this.alignTicks = 0;
      this.stuckTicks = 0;
      this.stuck = false;
      this.surfacingForAir = false;
      this.lastIndex = -1;
      this.nodeTicks = 0;
      if (!this.active || this.pathSeed == 0L) {
         this.pathSeed = System.nanoTime();
      }

      this.sprintJumpCount = 0;
      this.sprintJumpCooldown = 0;
      this.microStrafeTimer = 0;
      if (this.mc.player != null) {
         this.lastPos = this.mc.player.getEntityPos();
      }
   }

   public void a() {
      this.active = false;
      this.currentPath = null;
      this.currentIndex = 0;
      this.shouldForward = false;
      this.shouldJump = false;
      this.shouldSprint = false;
      this.strafe = 0.0F;
      this.alignTicks = 0;
      this.stuckTicks = 0;
      this.stuck = false;
      this.surfacingForAir = false;
      this.lastIndex = -1;
      this.nodeTicks = 0;
      this.sprintJumpCount = 0;
      this.sprintJumpCooldown = 0;
      this.microStrafeTimer = 0;
   }

   public void a(NavRotationController var1) {
      if (this.active && this.mc.player != null && this.currentPath != null && this.currentIndex < this.currentPath.size()) {
         boolean var2 = this.mc.player.isTouchingWater();
         boolean var3 = this.c();
         boolean var4 = this.d();
         Vec3d var5 = this.mc.player.getEntityPos();
         double var6 = Math.hypot(var5.x - this.lastPos.x, var5.z - this.lastPos.z);
         double var8 = Math.abs(var5.y - this.lastPos.y);
         double var10 = !var2 && !var3 ? var6 : Math.hypot(var6, var8);
         if (this.shouldForward && var10 < 0.04) {
            this.stuckTicks++;
            if (this.mc.player.horizontalCollision && !var3 && !var4) {
               this.stuckTicks += 2;
            }

            if (this.stuckTicks > 35) {
               this.stuck = true;
            }
         } else {
            this.stuckTicks = 0;
            this.stuck = false;
         }

         this.lastPos = var5;
         if (this.currentIndex == this.lastIndex) {
            this.nodeTicks++;
            if (this.nodeTicks > 100 && var10 < 0.04) {
               this.stuck = true;
            }
         } else {
            this.nodeTicks = 0;
            this.lastIndex = this.currentIndex;
         }

         if (this.currentIndex < this.currentPath.size()) {
            PathNode var12 = this.currentPath.get(this.currentIndex);
            double var13 = var12.getPos().getY() + 0.1 - var5.y;
            if (var13 > 1.25 && (this.mc.player.isOnGround() || var2) && this.stuckTicks > 10) {
               this.stuck = true;
            }
         }

         boolean var55 = this.mc.player.isSubmergedInWater();
         int var56 = this.mc.player.getAir();
         int var14 = Math.min(this.currentIndex + 8, this.currentPath.size());

         for (int var15 = var14 - 1; var15 > this.currentIndex; var15--) {
            PathNode var16 = this.currentPath.get(var15);
            BlockPos var17 = var16.getPos();
            double var18 = var17.getX() + 0.5 - var5.x;
            double var20 = var17.getZ() + 0.5 - var5.z;
            double var22 = Math.hypot(var18, var20);
            double var24 = Math.abs(var17.getY() - var5.y);
            Vec3d var26 = new Vec3d(var17.getX() + 0.5, var17.getY() + 0.1, var17.getZ() + 0.5);
            if (var22 < 1.35 && var24 < 1.35) {
               this.currentIndex = var15;
               break;
            }

            if (var22 < 2.4 && var24 < 1.35 && this.b(var5, var26)) {
               this.currentIndex = var15;
               break;
            }
         }

         while (this.currentIndex < this.currentPath.size()) {
            PathNode var57 = this.currentPath.get(this.currentIndex);
            BlockPos var59 = var57.getPos();
            boolean var60 = PathFinder.b(this.mc.world, var59);
            double var62 = var59.getX() + 0.5 - var5.x;
            double var66 = var59.getZ() + 0.5 - var5.z;
            double var69 = Math.hypot(var62, var66);
            double var71 = Math.abs(var59.getY() - var5.y);
            boolean var72 = this.currentIndex == this.currentPath.size() - 1;
            boolean var27 = false;
            if (var2 && !var60) {
               if (!this.mc.player.isTouchingWater() || this.mc.player.isOnGround() && var5.y >= var59.getY() - 0.4) {
                  var27 = true;
               }
            } else if (var57.getType() == MovementType.JUMP_UP) {
               boolean var28 = PathFinder.g(this.mc.world, var59.down()) || PathFinder.g(this.mc.world, var59);
               double var29 = var28 ? 0.65 : 0.2;
               if (var5.y >= var59.getY() - var29 && var69 < (var72 ? 0.35 : 1.4)) {
                  var27 = true;
               }
            } else if (var57.getType() == MovementType.FALL_DOWN) {
               if (var71 < 1.35 && var69 < (var72 ? 0.35 : 1.5) && this.mc.player.isOnGround()) {
                  var27 = true;
               }
            } else {
               boolean var74 = this.a(var59);
               if (var72) {
                  BlockPos var78 = BlockPos.ofFloored(var5.x, var5.y, var5.z);
                  if (var59.equals(var78) || var69 < 0.45 && var71 < 1.2) {
                     var27 = true;
                  }
               } else {
                  double var79 = var2 && var60 ? 1.5 : (var74 ? 0.85 : 1.35);
                  if (var69 < var79 && var71 < 1.5) {
                     var27 = true;
                  } else if (this.currentIndex + 1 < this.currentPath.size()) {
                     PathNode var31 = this.currentPath.get(this.currentIndex + 1);
                     double var32 = var31.getPos().getX() + 0.5 - var5.x;
                     double var34 = var31.getPos().getZ() + 0.5 - var5.z;
                     double var36 = Math.hypot(var32, var34);
                     double var38 = Math.abs(var31.getPos().getY() - var5.y);
                     double var40 = var31.getPos().getX() + 0.5 - (var59.getX() + 0.5);
                     double var42 = var31.getPos().getZ() + 0.5 - (var59.getZ() + 0.5);
                     double var44 = Math.hypot(var40, var42);
                     double var46 = var5.x - (var59.getX() + 0.5);
                     double var48 = var5.z - (var59.getZ() + 0.5);
                     double var50 = var46 * var40 + var48 * var42;
                     double var52 = var44 > 0.01 ? Math.abs(var46 * var42 - var48 * var40) / var44 : 0.0;
                     Vec3d var54 = new Vec3d(var31.getPos().getX() + 0.5, var31.getPos().getY() + 0.1, var31.getPos().getZ() + 0.5);
                     if ((var57.getType() != MovementType.JUMP_UP || var5.y >= var59.getY() - 0.2)
                        && (var50 > 0.0 && var52 < 0.85 || var36 < var69)
                        && var69 < 2.5
                        && var71 < 1.5
                        && var38 < 1.5
                        && this.b(var5, var54)) {
                        var27 = true;
                     }
                  }
               }
            }

            if (!var27) {
               break;
            }

            this.currentIndex++;
         }

         if (this.currentIndex >= this.currentPath.size()) {
            this.active = false;
            this.shouldForward = false;
            this.shouldJump = false;
            this.shouldSprint = false;
            if (var1 != null) {
               var1.b();
            }
         } else {
            double var58 = Double.MAX_VALUE;
            int var61 = Math.min(this.currentIndex + 6, this.currentPath.size());

            for (int var63 = this.currentIndex; var63 < var61; var63++) {
               PathNode var19 = this.currentPath.get(var63);
               double var67 = Math.hypot(var19.getPos().getX() + 0.5 - var5.x, var19.getPos().getZ() + 0.5 - var5.z);
               double var70 = Math.abs(var19.getPos().getY() - var5.y);
               if (var70 < 1.5 && var67 < var58) {
                  var58 = var67;
               }
            }

            if (var58 > 2.8 && !var3 && !var2 && this.mc.player.isOnGround()) {
               this.stuck = true;
            }

            PathNode var64 = this.currentPath.get(this.currentIndex);
            BlockPos var65 = var64.getPos();
            Vec3d var68 = new Vec3d(var65.getX() + 0.5, var65.getY(), var65.getZ() + 0.5);
            double var21 = var68.x - var5.x;
            double var23 = var68.z - var5.z;
            double var25 = Math.hypot(var21, var23);
            if (var2 && var55 && var56 <= 110) {
               this.surfacingForAir = true;
            } else if (!var2 || var56 >= 270) {
               this.surfacingForAir = false;
            }

            float var73 = 0.0F;
            if (var1 != null) {
               Vec3d var75 = this.a(var5);
               var1.a(var75);
               if (this.surfacingForAir && var55) {
                  float var80 = var1.getTargetPitch();
                  if (var80 > -18.0F) {
                     var1.a(var1.getTargetYaw(), -18.0F);
                  }
               }

               var1.a();
               float var81 = var1.getTargetYaw();
               float var30 = var1.f();
               var73 = Math.abs(MathHelper.wrapDegrees(var81 - var30));
               if (var73 > 105.0F) {
                  this.shouldForward = false;
                  this.shouldSprint = false;
                  this.alignTicks = 0;
               } else {
                  this.shouldForward = true;
                  this.alignTicks++;
                  boolean var84 = Baritone.INSTANCE == null || Baritone.INSTANCE.sprint.isEnabled();
                  boolean var87 = this.mc.player.getHungerManager().getFoodLevel() > 6;
                  this.shouldSprint = var84 && var87 && !var3 && var73 < 65.0F;
               }
            } else {
               this.shouldForward = true;
               boolean var76 = Baritone.INSTANCE == null || Baritone.INSTANCE.sprint.isEnabled();
               boolean var82 = this.mc.player.getHungerManager().getFoodLevel() > 6;
               this.shouldSprint = var76 && var82 && !var3;
            }

            if (var3) {
               this.shouldSprint = false;
               if (this.mc.player.isSprinting()) {
                  this.mc.player.setSprinting(false);
               }
            }

            this.microStrafeTimer++;
            this.strafe = 0.0F;
            boolean var77 = Baritone.INSTANCE == null || Baritone.INSTANCE.autoJump.isEnabled();
            this.shouldJump = false;
            double var83 = var68.y - var5.y;
            if (!var4 && !var3 && var77 && this.mc.player.isOnGround()) {
               if (var64.getType() == MovementType.JUMP_UP && var25 < 1.65 && var83 > 0.1 && var83 <= 1.35 && var73 < 65.0F) {
                  this.shouldJump = true;
                  this.shouldForward = true;
               } else if (this.currentIndex + 1 < this.currentPath.size() && this.currentPath.get(this.currentIndex + 1).getType() == MovementType.JUMP_UP) {
                  PathNode var85 = this.currentPath.get(this.currentIndex + 1);
                  double var88 = Math.hypot(var85.getPos().getX() + 0.5 - var5.x, var85.getPos().getZ() + 0.5 - var5.z);
                  double var93 = var85.getPos().getY() - var5.y;
                  if (var88 < 1.5 && var93 > 0.1 && var93 <= 1.35 && var73 < 65.0F) {
                     this.shouldJump = true;
                     this.shouldForward = true;
                  }
               }
            }

            if (!var2 && !this.mc.player.isOnGround() && (var64.getType() == MovementType.JUMP_UP || var83 > 0.15)) {
               this.shouldForward = true;
            }

            boolean var86 = PathFinder.b(this.mc.world, var64.getPos());
            if (var2) {
               this.shouldForward = true;
               double var89 = var68.y - var5.y;
               BlockPos var94 = this.mc.player.getBlockPos();
               int var35 = Integer.compare(var65.getX(), var94.getX());
               int var99 = Integer.compare(var65.getZ(), var94.getZ());
               BlockPos var37 = var35 == 0 && var99 == 0 ? var94.offset(this.mc.player.getHorizontalFacing()) : var94.add(var35, 0, var99);
               boolean var104 = PathFinder.d(this.mc.world, var37.up(1))
                  && PathFinder.d(this.mc.world, var37.up(2))
                  && PathFinder.d(this.mc.world, var94.up(2));
               if (var35 != 0 && var99 != 0) {
                  var104 = var104 && PathFinder.d(this.mc.world, var94.add(var35, 1, 0)) && PathFinder.d(this.mc.world, var94.add(0, 1, var99));
               }

               if (!var86) {
                  this.shouldSprint = false;
                  this.mc.player.setSprinting(false);
                  if (var89 > 0.1 && var89 <= 1.25 && var25 < 1.35 && var104 && var77) {
                     this.shouldJump = true;
                  }
               } else {
                  if (Baritone.INSTANCE == null || Baritone.INSTANCE.sprint.isEnabled()) {
                     this.shouldSprint = true;
                  }

                  if (this.surfacingForAir && var55) {
                     if (var77) {
                        this.shouldJump = true;
                     }
                  } else if (var89 > 0.4 && var89 <= 1.25 && var104) {
                     if (var77) {
                        this.shouldJump = true;
                     }
                  } else if (this.mc.player.horizontalCollision && var104) {
                     if (var77) {
                        this.shouldJump = true;
                     }
                  } else if (this.mc.player.isOnGround() && var89 > 0.0 && var89 <= 1.25 && var104 && var77) {
                     this.shouldJump = true;
                  }
               }
            }

            if (!var2 && !var4 && !var3 && this.mc.player.horizontalCollision && var77 && this.mc.player.isOnGround()) {
               BlockPos var90 = this.mc.player.getBlockPos();
               int var33 = Integer.compare(var65.getX(), var90.getX());
               int var95 = Integer.compare(var65.getZ(), var90.getZ());
               Direction var97 = this.mc.player.getHorizontalFacing();
               BlockPos[] var100 = new BlockPos[]{
                  var33 == 0 && var95 == 0 ? var90.offset(var97) : var90.add(var33, 0, var95),
                  var33 != 0 ? var90.add(var33, 0, 0) : var90.offset(var97),
                  var95 != 0 ? var90.add(0, 0, var95) : var90.offset(var97),
                  var90.offset(var97)
               };

               for (BlockPos var107 : var100) {
                  if (!PathFinder.g(this.mc.world, var107)
                     && !PathFinder.g(this.mc.world, var107.down())
                     && !PathFinder.h(this.mc.world, var107)
                     && !PathFinder.h(this.mc.world, var107.down())
                     && !PathFinder.d(this.mc.world, var107)
                     && PathFinder.d(this.mc.world, var107.up(1))
                     && PathFinder.d(this.mc.world, var107.up(2))
                     && PathFinder.d(this.mc.world, var90.up(2))) {
                     this.shouldJump = true;
                     this.shouldForward = true;
                     break;
                  }
               }
            }

            if (this.stuckTicks > 8 && var2 && var77) {
               double var91 = var68.y - var5.y;
               BlockPos var96 = this.mc.player.getBlockPos();
               int var98 = Integer.compare(var65.getX(), var96.getX());
               int var101 = Integer.compare(var65.getZ(), var96.getZ());
               BlockPos var103 = var98 == 0 && var101 == 0 ? var96.offset(this.mc.player.getHorizontalFacing()) : var96.add(var98, 0, var101);
               boolean var106 = PathFinder.d(this.mc.world, var103.up(1))
                  && PathFinder.d(this.mc.world, var103.up(2))
                  && PathFinder.d(this.mc.world, var96.up(2));
               if (var98 != 0 && var101 != 0) {
                  var106 = var106 && PathFinder.d(this.mc.world, var96.add(var98, 1, 0)) && PathFinder.d(this.mc.world, var96.add(0, 1, var101));
               }

               if (var91 <= 1.25 && var106) {
                  this.shouldJump = true;
                  if (!var86) {
                     this.shouldSprint = false;
                     this.mc.player.setSprinting(false);
                     this.shouldForward = true;
                  }
               }
            }

            boolean var92 = this.mc.player.hasStatusEffect(StatusEffects.SPEED);
            if (!var92
               && var77
               && !var2
               && !var4
               && !var3
               && this.shouldForward
               && this.shouldSprint
               && this.mc.player.isOnGround()
               && var64.getType() != MovementType.FALL_DOWN) {
               this.shouldJump = true;
            }

            if (var4 || var3) {
               this.shouldJump = false;
            }
         }
      } else {
         this.shouldForward = false;
         this.shouldJump = false;
         this.shouldSprint = false;
         if (var1 != null) {
            var1.b();
         }
      }
   }

   private boolean a(Vec3d var1, Vec3d var2) {
      if (this.mc.world == null) {
         return false;
      }

      BlockHitResult var3 = this.mc.world.raycast(new RaycastContext(var1, var2, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
      return var3 == null || var3.getType() == Type.MISS;
   }

   private boolean a(BlockPos var1) {
      return this.mc.world != null && var1 != null ? PathFinder.f(this.mc.world, var1) : false;
   }

   private boolean b(Vec3d var1, Vec3d var2) {
      if (this.mc.world == null) {
         return false;
      }

      double var3 = var2.x - var1.x;
      double var5 = var2.z - var1.z;
      double var7 = Math.hypot(var3, var5);
      if (var7 < 0.5) {
         return true;
      }

      int var9 = (int)Math.ceil(var7 / 0.4);

      for (int var10 = 1; var10 <= var9; var10++) {
         double var11 = (double)var10 / var9;
         double var13 = var1.x + var3 * var11;
         double var15 = var1.z + var5 * var11;
         double var17 = var1.y + (var2.y - var1.y) * var11;
         BlockPos var19 = BlockPos.ofFloored(var13, var17, var15);
         if (!PathFinder.d(this.mc.world, var19)) {
            if (!PathFinder.g(this.mc.world, var19) || !PathFinder.d(this.mc.world, var19.up(1)) || !PathFinder.d(this.mc.world, var19.up(2))) {
               return false;
            }
         } else if (!PathFinder.d(this.mc.world, var19.up())) {
            return false;
         }

         if (!PathFinder.e(this.mc.world, var19.down(1))
            && !PathFinder.e(this.mc.world, var19.down(2))
            && !PathFinder.e(this.mc.world, var19.down(3))
            && !PathFinder.h(this.mc.world, var19)
            && !PathFinder.h(this.mc.world, var19.down(1))
            && !PathFinder.b(this.mc.world, var19)
            && !PathFinder.b(this.mc.world, var19.down(1))) {
            return false;
         }
      }

      return true;
   }

   private Vec3d a(Vec3d var1) {
      if (this.currentPath != null && !this.currentPath.isEmpty() && this.currentIndex < this.currentPath.size()) {
         PathNode var2 = this.currentPath.get(this.currentIndex);
         BlockPos var3 = var2.getPos();
         boolean var4 = this.mc.player != null && this.mc.player.isTouchingWater();
         double var5 = var4 ? 0.5 : 1.5;
         if (!this.c() && !PathFinder.h(this.mc.world, var3)) {
            if (this.currentIndex == this.currentPath.size() - 1) {
               return new Vec3d(var3.getX() + 0.5, var3.getY() + var5, var3.getZ() + 0.5);
            }

            if (var2.getType() == MovementType.JUMP_UP) {
               return new Vec3d(var3.getX() + 0.5, var3.getY() + 1.2, var3.getZ() + 0.5);
            }

            if (this.a(var3)) {
               if (this.currentIndex + 1 < this.currentPath.size()) {
                  PathNode var26 = this.currentPath.get(this.currentIndex + 1);
                  double var8 = var3.getX() + 0.5 - var1.x;
                  double var28 = var3.getZ() + 0.5 - var1.z;
                  double var30 = Math.hypot(var8, var28);
                  if (var30 < 1.0) {
                     double var31 = MathHelper.clamp(1.0 - var30, 0.0, 1.0);
                     double var34 = (var3.getX() + 0.5) * (1.0 - var31) + (var26.getPos().getX() + 0.5) * var31;
                     double var36 = (var3.getY() + var5) * (1.0 - var31) + (var26.getPos().getY() + var5) * var31;
                     double var38 = (var3.getZ() + 0.5) * (1.0 - var31) + (var26.getPos().getZ() + 0.5) * var31;
                     return new Vec3d(var34, var36, var38);
                  }
               }

               return new Vec3d(var3.getX() + 0.5, var3.getY() + var5, var3.getZ() + 0.5);
            } else {
               double var7 = 0.0;
               if (this.currentIndex + 1 < this.currentPath.size()) {
                  PathNode var9 = this.currentPath.get(this.currentIndex + 1);
                  double var10 = var9.getPos().getX() + 0.5 - (var3.getX() + 0.5);
                  double var12 = var9.getPos().getZ() + 0.5 - (var3.getZ() + 0.5);
                  double var14 = var10 * var10 + var12 * var12;
                  if (var14 > 0.01) {
                     double var16 = var1.x - (var3.getX() + 0.5);
                     double var18 = var1.z - (var3.getZ() + 0.5);
                     var7 = MathHelper.clamp((var16 * var10 + var18 * var12) / var14, 0.0, 1.0);
                  }
               }

               double var27 = this.currentIndex + var7;
               Vec3d var11 = this.mc.player != null ? this.mc.player.getEyePos() : var1.add(0.0, 1.6, 0.0);
               Vec3d var29 = var1.add(0.0, 0.8, 0.0);
               if (var4 && !PathFinder.b(this.mc.world, var3)) {
                  return new Vec3d(var3.getX() + 0.5, var3.getY() + 0.8, var3.getZ() + 0.5);
               }

               double var13 = 2.2;

               for (double var15 = var13; var15 >= 0.5; var15 -= 0.5) {
                  double var35 = Math.min(var27 + var15, this.currentPath.size() - 1);
                  Vec3d var37 = PathCurve.a(this.currentPath, var35, this.mc.world, this.pathSeed);
                  Vec3d var20 = var37.add(0.0, var5, 0.0);
                  Vec3d var40 = var37.add(0.0, var4 ? 0.3 : 0.8, 0.0);
                  if (this.a(var11, var20) && this.a(var29, var40) && (var4 || this.b(var1, var37))) {
                     return var20;
                  }
               }

               int var32 = Math.min(this.currentIndex + 1, this.currentPath.size() - 1);
               BlockPos var33 = this.currentPath.get(var32).getPos();
               double var17 = var33.getX() + 0.5 - var1.x;
               double var19 = var33.getZ() + 0.5 - var1.z;
               if (var32 + 1 < this.currentPath.size()) {
                  PathNode var21 = this.currentPath.get(var32 + 1);
                  double var22 = var21.getPos().getX() + 0.5 - (var33.getX() + 0.5);
                  double var24 = var21.getPos().getZ() + 0.5 - (var33.getZ() + 0.5);
                  if (var17 * var22 + var19 * var24 < 0.0) {
                     var33 = this.currentPath.get(++var32).getPos();
                  }
               }

               double var39 = var4 && PathFinder.b(this.mc.world, var33) ? 0.5 : 1.5;
               return PathCurve.a(this.currentPath, var32, this.mc.world, this.pathSeed).add(0.0, var39, 0.0);
            }
         } else {
            return new Vec3d(var3.getX() + 0.5, var3.getY() + 1.2, var3.getZ() + 0.5);
         }
      } else {
         return var1.add(0.0, 1.5, 0.0);
      }
   }

   public BlockPos b() {
      if (this.mc.player == null) {
         return null;
      }

      BlockPos var1 = this.mc.player.getBlockPos();
      if (this.currentPath != null && this.currentIndex < this.currentPath.size()) {
         BlockPos var2 = this.currentPath.get(this.currentIndex).getPos();
         int var3 = Integer.compare(var2.getX(), var1.getX());
         int var4 = Integer.compare(var2.getZ(), var1.getZ());
         if (var3 != 0 || var4 != 0) {
            BlockPos var5 = var1.add(var3, 0, var4);
            if (this.mc.world != null && !PathFinder.d(this.mc.world, var5)) {
               return var5;
            }

            BlockPos var6 = var5.up();
            if (this.mc.world != null && !PathFinder.d(this.mc.world, var6)) {
               return var6;
            }

            BlockPos var7 = var1.add(var3, 0, 0);
            if (this.mc.world != null && !PathFinder.d(this.mc.world, var7)) {
               return var7;
            }

            BlockPos var8 = var1.add(0, 0, var4);
            return this.mc.world != null && !PathFinder.d(this.mc.world, var8) ? var8 : var5;
         }
      }

      return this.mc.player.horizontalCollision ? var1.offset(this.mc.player.getHorizontalFacing()) : var1;
   }

   public void a(EventMoveInput var1) {
      if (this.active && this.mc.player != null && this.mc.world != null) {
         if (this.c()) {
            var1.setSprint(false);
            var1.setJump(false);
            var1.setSneak(false);
            var1.setForward(1.0F);
            var1.setStrafe(0.0F);
         } else {
            if (this.shouldForward) {
               var1.setForward(1.0F);
            } else {
               var1.setForward(0.0F);
            }

            if (this.strafe != 0.0F) {
               var1.setStrafe(this.strafe);
            }

            boolean var2 = this.d();
            if (this.shouldJump && !var2) {
               var1.setJump(true);
            }

            if (this.shouldSprint) {
               var1.setSprint(true);
            } else {
               var1.setSprint(false);
            }

            if (!var2
               && this.mc.player.isOnGround()
               && !this.mc.player.isTouchingWater()
               && this.currentPath != null
               && this.currentIndex < this.currentPath.size()) {
               PathNode var3 = this.currentPath.get(this.currentIndex);
               if (var3.getType() != MovementType.FALL_DOWN && var3.getType() != MovementType.JUMP_UP) {
                  float var4 = this.mc.player.getYaw();
                  NavRotationController var5 = NavigationManager.getInstance().getRotationController();
                  if (var5 != null && var5.isActive()) {
                     var4 = var5.f();
                  }

                  if (var1.getForward() != 0.0F || var1.getStrafe() != 0.0F) {
                     if (var1.getForward() == 0.0F) {
                        var4 += var1.getStrafe() > 0.0F ? -90.0F : 90.0F;
                     } else if (var1.getForward() < 0.0F) {
                        var4 += 180.0F;
                        if (var1.getStrafe() != 0.0F) {
                           var4 += var1.getStrafe() > 0.0F ? 45.0F : -45.0F;
                        }
                     } else if (var1.getStrafe() != 0.0F) {
                        var4 += var1.getStrafe() > 0.0F ? -45.0F : 45.0F;
                     }

                     double var6 = Math.toRadians(var4);
                     double var8 = -Math.sin(var6);
                     double var10 = Math.cos(var6);
                     Vec3d var12 = this.mc.player.getEntityPos();
                     BlockPos var13 = BlockPos.ofFloored(var12.x + var8 * 0.5, var12.y, var12.z + var10 * 0.5);
                     boolean var14 = PathFinder.e(this.mc.world, var13)
                        || PathFinder.e(this.mc.world, var13.down(1))
                        || PathFinder.e(this.mc.world, var13.down(2))
                        || PathFinder.e(this.mc.world, var13.down(3))
                        || PathFinder.g(this.mc.world, var13)
                        || PathFinder.g(this.mc.world, var13.down(1))
                        || PathFinder.h(this.mc.world, var13)
                        || PathFinder.h(this.mc.world, var13.down(1))
                        || PathFinder.b(this.mc.world, var13)
                        || PathFinder.b(this.mc.world, var13.down(1))
                        || PathFinder.b(this.mc.world, var13.down(2))
                        || PathFinder.b(this.mc.world, var13.down(3));
                     if (!var14) {
                        var1.setSneak(true);
                        var1.setSprint(false);
                        var1.setJump(false);
                        var1.setForward(0.0F);
                        if (this.mc.player.isSprinting()) {
                           this.mc.player.setSprinting(false);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean c() {
      if (this.mc.player == null || this.mc.world == null) {
         return false;
      }

      if (this.mc.player.isClimbing()) {
         return true;
      }

      BlockPos var1 = this.mc.player.getBlockPos();
      BlockPos var2 = BlockPos.ofFloored(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      return PathFinder.h(this.mc.world, var1)
         || PathFinder.h(this.mc.world, var1.up())
         || PathFinder.h(this.mc.world, var2)
         || PathFinder.h(this.mc.world, var2.up());
   }

   public boolean d() {
      if (this.mc.player != null && this.mc.world != null) {
         Box var1 = this.mc.player.getBoundingBox().expand(0.2, 0.4, 0.2);
         int var2 = MathHelper.floor(var1.minX);
         int var3 = MathHelper.floor(var1.maxX);
         int var4 = MathHelper.floor(var1.minY - 0.6);
         int var5 = MathHelper.floor(var1.maxY);
         int var6 = MathHelper.floor(var1.minZ);
         int var7 = MathHelper.floor(var1.maxZ);

         for (int var8 = var2; var8 <= var3; var8++) {
            for (int var9 = var4; var9 <= var5; var9++) {
               for (int var10 = var6; var10 <= var7; var10++) {
                  BlockPos var11 = new BlockPos(var8, var9, var10);
                  if (PathFinder.g(this.mc.world, var11)) {
                     return true;
                  }
               }
            }
         }

         float var20 = this.mc.player.getYaw();
         NavRotationController var21 = NavigationManager.getInstance().getRotationController();
         if (var21 != null && var21.isActive()) {
            var20 = var21.f();
         }

         double var22 = Math.toRadians(var20);
         double var12 = -Math.sin(var22) * 0.7;
         double var14 = Math.cos(var22) * 0.7;
         Vec3d var16 = this.mc.player.getEntityPos();

         for (double var17 = -0.5; var17 <= 1.0; var17 += 0.5) {
            BlockPos var19 = BlockPos.ofFloored(var16.x + var12, var16.y + var17, var16.z + var14);
            if (PathFinder.g(this.mc.world, var19)) {
               return true;
            }
         }

         if (this.currentPath != null && this.currentIndex < this.currentPath.size()) {
            BlockPos var23 = this.currentPath.get(this.currentIndex).getPos();
            if (PathFinder.g(this.mc.world, var23) || PathFinder.g(this.mc.world, var23.down())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isStuck() {
      return this.stuck;
   }

   public void g() {
      this.stuck = false;
      this.stuckTicks = 0;
      this.nodeTicks = 0;
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }

   public PathNode i() {
      return this.currentPath != null && this.currentIndex >= 0 && this.currentIndex < this.currentPath.size() ? this.currentPath.get(this.currentIndex) : null;
   }

   public List<PathNode> getCurrentPath() {
      return this.currentPath;
   }
}
