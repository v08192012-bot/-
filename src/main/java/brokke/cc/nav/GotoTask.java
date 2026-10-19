package brokke.cc.nav;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class GotoTask implements NavTask {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final NavGoal goal;
   private final String name;
   private List<PathNode> path = Collections.emptyList();
   private boolean finished;
   private int recalculateCooldown;
   private int failedAttempts;
   private BlockPos originStart;

   public GotoTask(BlockPos var1) {
      this.goal = new GoalBlock(var1);
      this.name = "Goto " + var1.getX() + ", " + var1.getY() + ", " + var1.getZ();
   }

   public GotoTask(int var1, int var2) {
      this.goal = new GoalXZ(var1, var2);
      this.name = "Goto " + var1 + ", " + var2;
   }

   public GotoTask(NavGoal var1, String var2) {
      this.goal = var1;
      this.name = var2;
   }

   @Override
   public void a() {
      this.finished = false;
      this.failedAttempts = 0;
      this.recalculateCooldown = 0;
      if (this.mc.player != null) {
         this.originStart = this.mc.player.getBlockPos();
      }

      this.i();
   }

   @Override
   public void b() {
      if (this.mc.player != null && this.mc.world != null && !this.finished) {
         BlockPos var1 = this.mc.player.getBlockPos();
         Vec3d var2 = this.mc.player.getEntityPos();
         BlockPos var3 = BlockPos.ofFloored(var2.x, var2.y, var2.z);
         if (!this.goal.a(var1) && !this.goal.a(var3) && !this.goal.a(var1.down()) && !this.goal.a(var1.up())) {
            if (this.goal instanceof GoalBlock var4) {
               BlockPos var10 = var4.getTarget();
               if (var1.getX() == var10.getX() && var1.getZ() == var10.getZ() && Math.abs(var1.getY() - var10.getY()) <= 3
                  || var3.getX() == var10.getX() && var3.getZ() == var10.getZ() && Math.abs(var3.getY() - var10.getY()) <= 3) {
                  this.finished = true;
                  return;
               }
            }

            if (!(
               this.goal instanceof GoalXZ var8
                  && (var1.getX() == var8.getX() && var1.getZ() == var8.getZ() || var3.getX() == var8.getX() && var3.getZ() == var8.getZ())
            )) {
               if (this.recalculateCooldown > 0) {
                  this.recalculateCooldown--;
               }

               NavMovementController var9 = NavigationManager.getInstance().getMovementController();
               boolean var12 = false;
               boolean var6 = this.path != null && !this.path.isEmpty() && this.goal.a(this.path.get(this.path.size() - 1).getPos());
               if (!var9.isActive()) {
                  var12 = this.recalculateCooldown <= 0;
               } else if (var9.isStuck()) {
                  var12 = true;
               } else if (!var6 && this.recalculateCooldown <= 0 && this.path != null && !this.path.isEmpty()) {
                  int var7 = this.path.size() - var9.getCurrentIndex();
                  if (var7 <= 3) {
                     var12 = true;
                  }
               }

               if (var12) {
                  this.recalculateCooldown = 20;
                  BlockPos var13 = var9.isStuck() ? var9.b() : null;
                  var9.g();
                  this.a(var13);
               }
            } else {
               this.finished = true;
            }
         } else {
            this.finished = true;
         }
      }
   }

   private void i() {
      this.a(null);
   }

   private void a(BlockPos var1) {
      if (this.mc.player != null && this.mc.world != null) {
         BlockPos var2 = this.mc.player.getBlockPos();
         if (this.originStart == null || var2.getSquaredDistance(this.originStart) > 2500.0) {
            this.originStart = var2;
         }

         List var3 = PathFinder.a(this.mc.world, var2, this.goal, this.originStart, var1);
         if (var3 != null && !var3.isEmpty()) {
            this.path = var3;
            this.failedAttempts = 0;
            NavigationManager.getInstance().getMovementController().a(var3);
         } else {
            if (var1 != null) {
               var3 = PathFinder.a(this.mc.world, var2, this.goal, this.originStart, null);
               if (var3 != null && !var3.isEmpty()) {
                  this.path = var3;
                  this.failedAttempts = 0;
                  NavigationManager.getInstance().getMovementController().a(var3);
                  return;
               }
            }

            this.failedAttempts++;
            this.recalculateCooldown = Math.min(60, 20 + this.failedAttempts * 3);
            if (this.failedAttempts >= 35) {
               NavigationManager.getInstance().a("Не удалось построить путь");
            }
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
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String f() {
      if (this.finished) {
         return "Завершено";
      }

      NavMovementController var1 = NavigationManager.getInstance().getMovementController();
      int var2 = this.path.isEmpty() ? 0 : Math.max(0, this.path.size() - var1.getCurrentIndex());
      return "В пути, осталось точек: " + var2;
   }

   @Override
   public NavGoal getGoal() {
      return this.goal;
   }

   @Override
   public List<PathNode> getPath() {
      return this.path;
   }
}
