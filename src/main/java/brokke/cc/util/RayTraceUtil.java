package brokke.cc.util;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector3f;

public class RayTraceUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static HitResult a(double var0, float var2, float var3, Entity var4) {
      Vec3d var5 = mc.player.getEyePos();
      Vec3d var6 = b(var3, var2);
      Vec3d var7 = var5.add(var6.x * var0, var6.y * var0, var6.z * var0);
      return mc.world.raycast(new RaycastContext(var5, var7, ShapeType.OUTLINE, FluidHandling.NONE, var4));
   }

   public static BlockHitResult a(Vec3d var0, Vec3d var1, ShapeType var2) {
      return a(var0, var1, var2, mc.player);
   }

   public static BlockHitResult a(Vec3d var0, Vec3d var1, ShapeType var2, Entity var3) {
      return mc.world.raycast(new RaycastContext(var0, var1, var2, FluidHandling.NONE, var3));
   }

   public static boolean a(Vec3d var0, double var1, Box var3) {
      Vec3d var4 = Objects.requireNonNull(mc.player).getEyePos();
      return var3.contains(var4) || var3.raycast(var4, var4.add(var0.multiply(var1))).isPresent();
   }

   public static boolean a(LivingEntity var0, float var1, float var2, float var3, boolean var4) {
      Entity var5 = mc.getCameraEntity();
      if (var5 != null && mc.world != null) {
         double var6 = var3 * var3;
         Vec3d var8 = var5.getEyePos();
         Vector3f var9 = a(var1, var2);
         var9.mul(var3, var3, var3);
         Vec3d var10 = var8.add(var9.x, var9.y, var9.z);
         Box var11 = var0.getBoundingBox();
         EntityHitResult var12 = ProjectileUtil.raycast(var5, var8, var10, var11, var1x -> !var1x.isSpectator() && var1x.isAlive() && var1x == var0, var6);
         return var12 != null;
      } else {
         return false;
      }
   }

   public static Vector3f a(float var0, float var1) {
      float var2 = var1 * (float) (Math.PI / 180.0);
      float var3 = -var0 * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var3);
      float var5 = MathHelper.sin(var3);
      float var6 = MathHelper.cos(var2);
      float var7 = MathHelper.sin(var2);
      return new Vector3f(var5 * var6, -var7, var4 * var6);
   }

   public static Vec3d b(float var0, float var1) {
      float var2 = -var1 * (float) (Math.PI / 180.0) - (float) Math.PI;
      float var3 = -var0 * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var2);
      float var5 = MathHelper.sin(var2);
      float var6 = -MathHelper.cos(var3);
      float var7 = MathHelper.sin(var3);
      return new Vec3d(var5 * var6, var7, var4 * var6);
   }

   public static boolean a(float var0, float var1, double var2, Entity var4) {
      Vec3d var5 = mc.player.getEyePos();
      Vec3d var6 = mc.player.getRotationVector(var1, var0);
      Vec3d var7 = var5.add(var6.multiply(var2));
      Box var8 = var4.getBoundingBox();
      return var8.contains(var5) || var8.raycast(var5, var7).isPresent();
   }
}
