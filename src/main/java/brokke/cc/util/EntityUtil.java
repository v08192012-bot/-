package brokke.cc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class EntityUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static void a(float var0) {
      mc.player.age = (int)(mc.player.age * var0);
   }

   public static void a() {
   }

   public static boolean b() {
      return mc.player != null && mc.world != null && mc.getNetworkHandler() != null;
   }

   public static BlockPos a(double var0, double var2, double var4) {
      if (mc.player != null && mc.world != null) {
         Vec3d var6 = mc.player.getEntityPos().add(var0, var2, var4);
         return BlockPos.ofFloored(var6);
      } else {
         return null;
      }
   }

   public static BlockPos a(Entity var0) {
      return mc.world == null ? null : BlockPos.ofFloored(var0.getX(), var0.getY() + 1.0, var0.getZ());
   }

   public static double[] c() {
      if (mc.player == null) {
         return new double[]{0.0, 0.0};
      }

      Vec2f var0 = mc.player.input.getMovementInput();
      return new double[]{var0.x, var0.y};
   }
}
