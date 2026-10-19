package brokke.cc.nav;

import brokke.cc.util.InventoryUtility;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MineBlockTask implements NavTask {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final BlockPos targetBlock;
   private final NavGoal goal;
   private List<PathNode> path = Collections.emptyList();
   private boolean finished;
   private int miningTicks;
   private int recalculateCooldown;

   public MineBlockTask(BlockPos var1) {
      this.targetBlock = var1;
      this.goal = new GoalNear(var1, 3.2);
   }

   @Override
   public void a() {
      this.finished = false;
      this.miningTicks = 0;
      this.recalculateCooldown = 0;
      this.j();
   }

   @Override
   public void b() {
      if (this.mc.player != null && this.mc.world != null && !this.finished) {
         BlockState var1 = this.mc.world.getBlockState(this.targetBlock);
         if (var1.isAir()) {
            this.finished = true;
         } else {
            Vec3d var2 = this.mc.player.getEyePos();
            Vec3d var3 = new Vec3d(this.targetBlock.getX() + 0.5, this.targetBlock.getY() + 0.5, this.targetBlock.getZ() + 0.5);
            double var4 = var2.squaredDistanceTo(var3);
            if (var4 <= 18.0) {
               NavigationManager.getInstance().getMovementController().a();
               NavRotationController var8 = NavigationManager.getInstance().getRotationController();
               var8.a(var3);
               var8.a(45.0F);
               this.a(var1);
               this.miningTicks++;
               if (this.mc.interactionManager != null) {
                  Direction var7 = Direction.getFacing(var3.x - var2.x, var3.y - var2.y, var3.z - var2.z);
                  this.mc.interactionManager.updateBlockBreakingProgress(this.targetBlock, var7);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
               }
            } else {
               if (this.recalculateCooldown > 0) {
                  this.recalculateCooldown--;
               }

               NavMovementController var6 = NavigationManager.getInstance().getMovementController();
               if ((var6.isStuck() || !var6.isActive()) && this.recalculateCooldown <= 0) {
                  this.recalculateCooldown = 20;
                  this.j();
               }
            }
         }
      }
   }

   private void a(BlockState var1) {
      if (this.mc.player != null) {
         int var2 = -1;
         float var3 = 1.0F;

         for (int var4 = 0; var4 < 9; var4++) {
            ItemStack var5 = this.mc.player.getInventory().getStack(var4);
            float var6 = var5.getMiningSpeedMultiplier(var1);
            if (var6 > var3) {
               var3 = var6;
               var2 = var4;
            }
         }

         if (var2 != -1 && this.mc.player.getInventory().getSelectedSlot() != var2) {
            InventoryUtility.e(var2);
         }
      }
   }

   private void j() {
      if (this.mc.player != null && this.mc.world != null) {
         BlockPos var1 = this.mc.player.getBlockPos();
         List var2 = PathFinder.a(this.mc.world, var1, this.goal);
         if (var2 != null && !var2.isEmpty()) {
            this.path = var2;
            NavigationManager.getInstance().getMovementController().a(var2);
         }
      }
   }

   @Override
   public boolean isFinished() {
      return this.finished;
   }

   @Override
   public void d() {
      NavigationManager.getInstance().getMovementController().a();
      if (this.mc.interactionManager != null) {
         this.mc.interactionManager.cancelBlockBreaking();
      }
   }

   @Override
   public String getName() {
      return "Mine " + this.targetBlock.getX() + ", " + this.targetBlock.getY() + ", " + this.targetBlock.getZ();
   }

   @Override
   public String f() {
      if (this.finished) {
         return "Вскопано";
      }

      if (this.mc.player != null
         && this.mc
               .player
               .getEyePos()
               .squaredDistanceTo(new Vec3d(this.targetBlock.getX() + 0.5, this.targetBlock.getY() + 0.5, this.targetBlock.getZ() + 0.5))
            <= 18.0) {
         return "Копает (" + this.miningTicks + " тиков)";
      }

      NavMovementController var1 = NavigationManager.getInstance().getMovementController();
      int var2 = this.path.isEmpty() ? 0 : Math.max(0, this.path.size() - var1.getCurrentIndex());
      return "Идёт к блоку, осталось точек: " + var2;
   }

   @Override
   public NavGoal getGoal() {
      return this.goal;
   }

   @Override
   public List<PathNode> getPath() {
      return this.path;
   }

   public BlockPos getTargetBlock() {
      return this.targetBlock;
   }
}
