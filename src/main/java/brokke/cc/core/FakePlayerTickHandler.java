package brokke.cc.core;

import brokke.cc.manager.FakePlayerManager;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FakePlayerTickHandler {
   private static final double STRAFE_SPEED = 0.17;
   private static final double RADIAL_SPEED = 0.12;
   private static final double MIN_DIST = 2.2;
   private static final double MAX_DIST = 4.0;
   private static final int JUMP_LENGTH = 12;
   private static final double JUMP_HEIGHT = 1.15;
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Random RNG = new Random();
   private static boolean registered = false;
   private static boolean moving = false;

   public static void a() {
      if (!registered) {
         EventBus.register(new FakePlayerTickHandler());
         registered = true;
      }
   }

   public static boolean isMoving() {
      return moving;
   }

   public static void a(boolean var0) {
      moving = var0;

      for (FakePlayerEntity var2 : FakePlayerManager.getFakePlayers()) {
         if (var2 != null) {
            var2.setNoGravity(var0);
            if (var0) {
               var2.baseY = var2.getY();
               var2.strafeTicks = 0;
               var2.distTicks = 0;
               var2.jumpTicks = 0;
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      for (FakePlayerEntity var3 : new ArrayList<>(FakePlayerManager.getFakePlayers())) {
         if (var3 == null || !var3.isAlive()) {
            FakePlayerManager.getFakePlayers().remove(var3);
         } else if (moving && mc.player != null) {
            this.a(var3);
         }
      }
   }

   private void a(FakePlayerEntity var1) {
      Vec3d var2 = mc.player.getEntityPos();
      double var3 = var1.getX() - var2.x;
      double var5 = var1.getZ() - var2.z;
      double var7 = Math.sqrt(var3 * var3 + var5 * var5);
      if (var7 < 0.05) {
         var3 = 1.0;
         var5 = 0.0;
         var7 = 1.0;
      }

      double var9 = var3 / var7;
      double var11 = var5 / var7;
      if (--var1.strafeTicks <= 0) {
         var1.strafeDir = RNG.nextBoolean() ? 1 : -1;
         var1.strafeTicks = 6 + RNG.nextInt(19);
      }

      if (--var1.distTicks <= 0) {
         var1.wantDist = 2.2 + RNG.nextDouble() * 1.7999999999999998;
         var1.distTicks = 40 + RNG.nextInt(41);
      }

      if (var1.jumpTicks <= 0 && RNG.nextInt(60) == 0) {
         var1.jumpTicks = 12;
      }

      double var13 = -var11 * 0.17 * var1.strafeDir;
      double var15 = var9 * 0.17 * var1.strafeDir;
      double var17 = MathHelper.clamp(var1.wantDist - var7, -0.12, 0.12);
      double var19 = var13 + var9 * var17;
      double var21 = var15 + var11 * var17;
      double var23 = var1.baseY;
      if (var1.jumpTicks > 0) {
         double var25 = 1.0 - var1.jumpTicks / 12.0;
         var23 += Math.sin(var25 * Math.PI) * 1.15;
         var1.jumpTicks--;
      }

      var1.lastX = var1.getX();
      var1.lastY = var1.getY();
      var1.lastZ = var1.getZ();
      var1.lastYaw = var1.getYaw();
      var1.lastHeadYaw = var1.headYaw;
      var1.lastBodyYaw = var1.bodyYaw;
      var1.setPosition(var1.getX() + var19, var23, var1.getZ() + var21);
      float var27 = (float)Math.toDegrees(Math.atan2(-(var1.getX() - var2.x), var1.getZ() - var2.z));
      var1.setYaw(var27);
      var1.setHeadYaw(var27);
      var1.setBodyYaw(var27);
   }
}
