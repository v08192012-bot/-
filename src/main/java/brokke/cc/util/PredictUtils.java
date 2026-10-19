package brokke.cc.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PredictUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<UUID, PredictUtils.a> positionCache = new ConcurrentHashMap<>();

   public static void a(LivingEntity var0) {
      PredictUtils.a var1 = positionCache.computeIfAbsent(var0.getUuid(), var0x -> new PredictUtils.a());
      var1.a(var0.getX(), var0.getY(), var0.getZ());
   }

   public static PredictUtils.a b(LivingEntity var0) {
      return positionCache.get(var0.getUuid());
   }

   public static Vec3d a(LivingEntity var0, int var1, float var2, boolean var3) {
      PredictUtils.a var4 = b(var0);
      Vec3d var5 = new Vec3d(var0.getX(), var0.getY() + var0.getStandingEyeHeight() / 2.0F, var0.getZ());
      if (var4 == null) {
         return b(var0, var5, var1);
      }

      Vec3d var6 = var4.n();
      double var7 = var4.getLastSpeed();
      boolean var9 = var4.o();
      if (var0.isGliding()) {
         double var10 = Math.hypot(var6.x, var6.z) * 20.0;
         double var12 = Math.abs(var6.y) * 20.0;
         if (var10 <= 5.0 && var12 <= 5.0) {
            return var5;
         }

         boolean var14 = var3 && var0.isGliding() && var9;
         float var15 = var14 ? var1 + 2 + var2 : var1;
         Vec3d var16 = var5.add(var6.multiply(var15, var15, var15));
         Vec3d var17 = b(var0, var5, var1);
         double var18 = MathHelper.clamp(var7 / 50.0, 0.3, 0.9);
         return new Vec3d(MathHelper.lerp(var18, var17.x, var16.x), MathHelper.lerp(var18, var17.y, var16.y), MathHelper.lerp(var18, var17.z, var16.z));
      } else {
         return var7 > 1.0 ? var5.add(var6.multiply(var1, var1, var1)) : var5;
      }
   }

   public static Vec3d a(LivingEntity var0, Vec3d var1, int var2) {
      PredictUtils.a var3 = b(var0);
      if (var3 != null && var0.isGliding()) {
         Vec3d var4 = var3.n();
         double var5 = Math.hypot(var4.x, var4.z) * 20.0;
         double var7 = Math.abs(var4.y) * 20.0;
         return var5 <= 5.0 && var7 <= 5.0 ? var1 : var1.add(var4.multiply(var2, var2, var2));
      } else {
         return b(var0, var1, var2);
      }
   }

   public static Vec3d b(LivingEntity var0, Vec3d var1, int var2) {
      Vec3d var3 = var0.getVelocity();
      if (!var0.isGliding()) {
         return var1.add(var3.multiply(var2, var2, var2));
      }

      double var4 = Math.hypot(var0.lastRenderX - var0.getX(), var0.lastRenderZ - var0.getZ()) * 20.0;
      double var6 = Math.abs(var0.getY() - var0.lastRenderY) * 20.0;
      if (var4 <= 5.0 && var6 <= 5.0) {
         return var1;
      }

      for (int var8 = 0; var8 < var2; var8++) {
         Vec3d var9 = var0.getRotationVector();
         float var10 = (float)Math.toRadians(var0.getPitch());
         double var11 = Math.sqrt(var3.x * var3.x + var3.z * var3.z);
         double var13 = var3.length();
         float var15 = MathHelper.cos(var10);
         float var19 = (float)(var15 * var15 * Math.min(1.0, var9.length() / 0.4));
         Vec3d var18 = var3.add(0.0, -0.08 * (-1.0 + var19 * 0.75), 0.0);
         if (var18.y < 0.0 && var11 > 0.0) {
            double var16 = var18.y * -0.1 * var19;
            var18 = var18.add(var9.x * var16 / var11, var16, var9.z * var16 / var11);
         }

         if (var10 < 0.0F && var11 > 0.0) {
            double var20 = var13 * -MathHelper.sin(var10) * 0.04;
            var18 = var18.add(-var9.x * var20 / var11, var20 * 3.2, -var9.z * var20 / var11);
         }

         if (var11 > 0.0) {
            var18 = var18.add((var9.x / var11 * var13 - var18.x) * 0.1, 0.0, (var9.z / var11 * var13 - var18.z) * 0.1);
         }

         var3 = var18.multiply(0.99, 0.98, 0.99);
         var1 = var1.add(var3);
      }

      return var1;
   }

   public static Vec3d c(LivingEntity var0) {
      if (var0 == null) {
         return Vec3d.ZERO;
      } else {
         Vec3d var1 = var0.getEntityPos().add(0.0, var0.getHeight() * 0.5, 0.0);
         if (var0.isGliding()) {
            Vec3d var2 = var0.getVelocity();
            return var1.add(var2.x * 2.0, var2.y * 2.0, var2.z * 2.0);
         } else {
            return var1;
         }
      }
   }

   public static void a() {
      long var0 = System.currentTimeMillis();
      positionCache.entrySet().removeIf(var2 -> var0 - var2.getValue().getLastUpdate() > 10000L);
   }

   public static void clearPositionCache() {
      positionCache.clear();
   }

   public static class a {
      private double serverX;
      private double serverY;
      private double serverZ;
      private double prevServerX;
      private double prevServerY;
      private double prevServerZ;
      private double backUpX;
      private double backUpY;
      private double backUpZ;
      private double lastSpeed;
      private double prevSpeed;
      private long lastUpdate;

      public double getServerX() {
         return this.serverX;
      }

      public double getServerY() {
         return this.serverY;
      }

      public double getServerZ() {
         return this.serverZ;
      }

      public double getPrevServerX() {
         return this.prevServerX;
      }

      public double getPrevServerY() {
         return this.prevServerY;
      }

      public double getPrevServerZ() {
         return this.prevServerZ;
      }

      public double getBackUpX() {
         return this.backUpX;
      }

      public double getBackUpY() {
         return this.backUpY;
      }

      public double getBackUpZ() {
         return this.backUpZ;
      }

      public double getLastSpeed() {
         return this.lastSpeed;
      }

      public double getPrevSpeed() {
         return this.prevSpeed;
      }

      public long getLastUpdate() {
         return this.lastUpdate;
      }

      public Vec3d m() {
         return new Vec3d(this.serverX, this.serverY, this.serverZ);
      }

      public Vec3d n() {
         return new Vec3d(this.serverX - this.prevServerX, this.serverY - this.prevServerY, this.serverZ - this.prevServerZ);
      }

      public void a(double var1, double var3, double var5) {
         this.backUpX = this.prevServerX;
         this.backUpY = this.prevServerY;
         this.backUpZ = this.prevServerZ;
         this.prevServerX = this.serverX;
         this.prevServerY = this.serverY;
         this.prevServerZ = this.serverZ;
         this.serverX = var1;
         this.serverY = var3;
         this.serverZ = var5;
         this.prevSpeed = this.lastSpeed;
         this.lastSpeed = this.n().length() * 20.0;
         this.lastUpdate = System.currentTimeMillis();
      }

      public boolean o() {
         return this.lastSpeed >= 20.0 || this.lastSpeed != this.prevSpeed && this.lastSpeed == 0.0;
      }
   }
}
