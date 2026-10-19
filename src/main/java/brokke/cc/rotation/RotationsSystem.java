package brokke.cc.rotation;

import brokke.cc.util.GCDUtil2;
import brokke.cc.util.PredictUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public abstract class RotationsSystem {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   public Vec2f rotate = Vec2f.ZERO;

   public abstract void a(LivingEntity var1);

   public static Vec2f a(float var0, float var1) {
      if (mc.player != null && (var0 == -90.0F && var1 == 90.0F || var0 == -180.0F)) {
         return new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      }

      float var2 = GCDUtil2.b();
      var0 -= var0 % var2;
      var1 -= var1 % var2;
      return new Vec2f(var0, var1);
   }

   protected boolean b(LivingEntity var1) {
      return mc.player != null && var1 != null && mc.player.isGliding() && var1.isGliding();
   }

   protected Vec3d a(LivingEntity var1, Vec3d var2) {
      return !this.b(var1) ? var2 : PredictUtils.c(var1);
   }

   protected Box c(LivingEntity var1) {
      Box var2 = var1.getBoundingBox();
      if (!this.b(var1)) {
         return var2;
      }

      Vec3d var3 = var2.getCenter();
      Vec3d var4 = this.a(var1, var3);
      return var2.offset(var4.subtract(var3));
   }
}
