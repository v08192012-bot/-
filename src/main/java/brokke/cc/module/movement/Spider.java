package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.InventoryUtility;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

@NewFunction(name = "Spider", description = "Автоматически ставит блоки для карабканья по стенам", category = Category.MOVEMENT)
public class Spider extends Function {
   private final BooleanSetting rotate = new BooleanSetting("Поворот", true);
   private final BooleanSetting swing = new BooleanSetting("Анимация руки", true);

   public Spider() {
      this.addSettings(this.rotate, this.swing);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && !mc.player.isSpectator()) {
         boolean var2 = mc.player.getOffHandStack().getItem() instanceof BlockItem;
         int var3 = this.k();
         if (var2 || var3 != -1) {
            BlockPos var4 = this.l();
            BlockPos var5 = this.b(var4);
            if (!var5.equals(BlockPos.ORIGIN)) {
               Direction var6 = this.a(var5, var4);
               if (var6 != null) {
                  ItemStack var7 = var2 ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(var3);
                  if (var7.getItem() instanceof BlockItem var8) {
                     VoxelShape var22 = var8.getBlock().getDefaultState().getCollisionShape(mc.world, var4);
                     if (!var22.isEmpty()) {
                        Box var10 = var22.getBoundingBox().offset(var4);
                        if (!var10.intersects(mc.player.getBoundingBox())) {
                           Vec3d var11 = new Vec3d(
                              var5.getX() + 0.5 + var6.getOffsetX() * 0.5,
                              var5.getY() + 0.5 + var6.getOffsetY() * 0.5,
                              var5.getZ() + 0.5 + var6.getOffsetZ() * 0.5
                           );
                           if (this.rotate.isToggled()) {
                              double var12 = var11.x - mc.player.getX();
                              double var14 = var11.y - mc.player.getEyePos().y;
                              double var16 = var11.z - mc.player.getZ();
                              double var18 = Math.hypot(var12, var16);
                              float var20 = (float)Math.toDegrees(Math.atan2(var16, var12)) - 90.0F;
                              float var21 = (float)(-Math.toDegrees(Math.atan2(var14, var18)));
                              RotationStorage.a(new Rotation2(var20, var21), 180.0F, 180.0F, 1, 2, false);
                           }

                           BlockHitResult var23 = new BlockHitResult(var11, var6, var5, false);
                           Hand var13 = var2 ? Hand.OFF_HAND : Hand.MAIN_HAND;
                           int var24 = mc.player.getInventory().getSelectedSlot();
                           if (!var2) {
                              InventoryUtility.e(var3);
                           }

                           mc.interactionManager.interactBlock(mc.player, var13, var23);
                           if (this.swing.isToggled()) {
                              mc.player.swingHand(var13);
                           }

                           if (!var2) {
                              InventoryUtility.e(var24);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Direction a(BlockPos var1, BlockPos var2) {
      if (var1.getX() < var2.getX()) {
         return Direction.EAST;
      } else if (var1.getX() > var2.getX()) {
         return Direction.WEST;
      } else if (var1.getZ() < var2.getZ()) {
         return Direction.SOUTH;
      } else if (var1.getZ() > var2.getZ()) {
         return Direction.NORTH;
      } else if (var1.getY() < var2.getY()) {
         return Direction.UP;
      } else {
         return var1.getY() > var2.getY() ? Direction.DOWN : null;
      }
   }

   private int k() {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (!var2.isEmpty() && var2.getItem() instanceof BlockItem) {
            return var1;
         }
      }

      return -1;
   }

   private BlockPos l() {
      Vec3d var1 = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      return BlockPos.ofFloored(var1.add(mc.player.getVelocity()).add(0.0, -0.001, 0.0));
   }

   private boolean a(BlockPos var1) {
      if (mc.world == null) {
         return false;
      }

      BlockState var2 = mc.world.getBlockState(var1);
      return !var2.isAir() && var2.isSolidBlock(mc.world, var1);
   }

   private BlockPos b(BlockPos var1) {
      if (mc.world == null) {
         return BlockPos.ORIGIN;
      } else {
         return this.a(var1)
            ? BlockPos.ORIGIN
            : Stream.of(var1.west(), var1.east(), var1.south(), var1.north()).filter(this::a).findFirst().orElse(BlockPos.ORIGIN);
      }
   }
}
