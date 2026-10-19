package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MultipointUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Vec3d a(Entity var0) {
      if (mc.player != null && var0 != null) {
         Box var1 = var0.getBoundingBox();
         Vec3d var2 = mc.player.getEyePos();
         return new Vec3d(
            MathHelper.clamp(var2.x, var1.minX, var1.maxX), MathHelper.clamp(var2.y, var1.minY, var1.maxY), MathHelper.clamp(var2.z, var1.minZ, var1.maxZ)
         );
      } else {
         return var0 != null ? new Vec3d(var0.getX(), var0.getY() + var0.getHeight() * 0.5, var0.getZ()) : Vec3d.ZERO;
      }
   }
}
