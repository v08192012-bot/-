package brokke.cc.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public final class EntityUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static float timer = 1.0F;

   public static void a() {
      timer = 1.0F;
   }

   public static void setTimer(float var0) {
      timer = var0;
   }

   public static float getTimer() {
      return timer;
   }

   public static Block c() {
      return a(0.0, 0.0, 0.0);
   }

   public static Block a(double var0, double var2, double var4) {
      return mc.player != null && mc.world != null
         ? mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX() + var0, mc.player.getY() + var2, mc.player.getZ() + var4)).getBlock()
         : Blocks.AIR;
   }

   public static Block a(Entity var0) {
      return var0 != null && mc.world != null
         ? mc.world.getBlockState(BlockPos.ofFloored(var0.getX(), var0.getY() + var0.getHeight() + 0.5, var0.getZ())).getBlock()
         : Blocks.AIR;
   }

   private EntityUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
