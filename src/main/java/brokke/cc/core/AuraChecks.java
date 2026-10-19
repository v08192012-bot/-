package brokke.cc.core;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class AuraChecks {
   private AuraChecks() {
   }

   public static boolean a(MinecraftClient var0) {
      if (var0.player == null) {
         return false;
      }

      ItemStack var1 = var0.player.getMainHandStack();
      return var1.isEmpty()
         ? false
         : var1.isOf(Items.WOODEN_SWORD)
            || var1.isOf(Items.STONE_SWORD)
            || var1.isOf(Items.IRON_SWORD)
            || var1.isOf(Items.GOLDEN_SWORD)
            || var1.isOf(Items.DIAMOND_SWORD)
            || var1.isOf(Items.NETHERITE_SWORD)
            || var1.isOf(Items.WOODEN_AXE)
            || var1.isOf(Items.STONE_AXE)
            || var1.isOf(Items.IRON_AXE)
            || var1.isOf(Items.GOLDEN_AXE)
            || var1.isOf(Items.DIAMOND_AXE)
            || var1.isOf(Items.NETHERITE_AXE)
            || var1.isOf(Items.TRIDENT)
            || var1.isOf(Items.MACE);
   }

   public static boolean b(MinecraftClient var0) {
      if (var0.player != null && var0.world != null) {
         Box var1 = var0.player.getBoundingBox();
         Box var2 = new Box(var1.minX - 0.06, var1.minY + 0.05, var1.minZ - 0.06, var1.maxX + 0.06, var1.maxY - 0.05, var1.maxZ + 0.06);
         return BlockPos.stream(var2)
            .anyMatch(
               var2xx -> {
                  BlockState var3 = var0.world.getBlockState(var2xx);
                  return !var3.getCollisionShape(var0.world, var2xx).isEmpty()
                     && var3.getCollisionShape(var0.world, var2xx)
                        .getBoundingBoxes()
                        .stream()
                        .map(var1xx -> var1xx.offset(var2xx.getX(), var2xx.getY(), var2xx.getZ()))
                        .anyMatch(var2::intersects);
               }
            );
      } else {
         return false;
      }
   }

   public static boolean a(MinecraftClient var0, Entity var1) {
      if (var1 != null && var0.player != null && var0.world != null) {
         Vec3d var2 = var0.player.getEyePos();
         if (a(var0, var2, var1.getEyePos())) {
            return true;
         } else if (a(var0, var2, var1.getBoundingBox().getCenter())) {
            return true;
         } else {
            Box var3 = var1.getBoundingBox();
            if (a(var0, var2, new Vec3d(var3.getCenter().x, var3.minY + var3.getLengthY() * 0.75, var3.getCenter().z))) {
               return true;
            } else {
               return a(var0, var2, new Vec3d(var3.getCenter().x, var3.minY + var3.getLengthY() * 0.25, var3.getCenter().z))
                  ? true
                  : a(var0, var2, new Vec3d(var3.getCenter().x, var3.minY + 0.1, var3.getCenter().z));
            }
         }
      } else {
         return false;
      }
   }

   private static boolean a(MinecraftClient var0, Vec3d var1, Vec3d var2) {
      return var0.world.raycast(new RaycastContext(var1, var2, ShapeType.COLLIDER, FluidHandling.NONE, var0.player)).getType() == Type.MISS;
   }

   public static boolean b(MinecraftClient var0, Entity var1) {
      if (var1 != null && var0.player != null && var0.world != null) {
         BlockHitResult var2 = var0.world
            .raycast(new RaycastContext(var0.player.getEyePos(), var1.getEyePos(), ShapeType.OUTLINE, FluidHandling.NONE, var0.player));
         if (var2.getType() != Type.BLOCK) {
            return false;
         }

         BlockHitResult var3 = var2;
         BlockState var4 = var0.world.getBlockState(var3.getBlockPos());
         return var4.isOf(Blocks.SHORT_GRASS)
            || var4.isOf(Blocks.TALL_GRASS)
            || var4.isOf(Blocks.FERN)
            || var4.isOf(Blocks.LARGE_FERN)
            || var4.isOf(Blocks.DEAD_BUSH)
            || var4.isOf(Blocks.SEAGRASS)
            || var4.isOf(Blocks.TALL_SEAGRASS)
            || var4.isOf(Blocks.KELP)
            || var4.isOf(Blocks.KELP_PLANT)
            || var4.isOf(Blocks.OAK_FENCE)
            || var4.isOf(Blocks.SPRUCE_FENCE)
            || var4.isOf(Blocks.BIRCH_FENCE)
            || var4.isOf(Blocks.JUNGLE_FENCE)
            || var4.isOf(Blocks.ACACIA_FENCE)
            || var4.isOf(Blocks.CHERRY_FENCE)
            || var4.isOf(Blocks.DARK_OAK_FENCE)
            || var4.isOf(Blocks.PALE_OAK_FENCE)
            || var4.isOf(Blocks.MANGROVE_FENCE)
            || var4.isOf(Blocks.BAMBOO_FENCE)
            || var4.isOf(Blocks.CRIMSON_FENCE)
            || var4.isOf(Blocks.WARPED_FENCE)
            || var4.isOf(Blocks.NETHER_BRICK_FENCE);
      } else {
         return false;
      }
   }

   public static boolean c(MinecraftClient var0) {
      if (var0.player != null && var0.world != null) {
         Box var1 = var0.player.getBoundingBox().expand(0.001);
         int var2 = MathHelper.floor(var1.minX);
         int var3 = MathHelper.floor(var1.minY);
         int var4 = MathHelper.floor(var1.minZ);
         int var5 = MathHelper.floor(var1.maxX);
         int var6 = MathHelper.floor(var1.maxY);
         int var7 = MathHelper.floor(var1.maxZ);

         for (BlockPos var9 : BlockPos.iterate(var2, var3, var4, var5, var6, var7)) {
            if (var0.world.getBlockState(var9).isOf(Blocks.COBWEB)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean d(MinecraftClient var0) {
      if (var0.player != null && var0.world != null) {
         Box var1 = var0.player.getBoundingBox().contract(0.05);
         int var2 = MathHelper.floor(var1.minX);
         int var3 = MathHelper.floor(var1.minY);
         int var4 = MathHelper.floor(var1.minZ);
         int var5 = MathHelper.floor(var1.maxX);
         int var6 = MathHelper.floor(var1.maxY);
         int var7 = MathHelper.floor(var1.maxZ);

         for (BlockPos var9 : BlockPos.iterate(var2, var3, var4, var5, var6, var7)) {
            if (!var0.world.getBlockState(var9).isAir()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean c(MinecraftClient var0, Entity var1) {
      return var1 != null && var0.player != null && var0.world != null ? !a(var0, var1) : false;
   }
}
