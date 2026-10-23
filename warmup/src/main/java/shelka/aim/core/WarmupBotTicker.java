package shelka.aim.core;

import java.util.ArrayList;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import shelka.aim.manager.BotManager;

/**
 * Движение ботов: стрейфы вокруг игрока, смена дистанции, редкие прыжки.
 * Скорость — множитель к базовым константам (1.0 = как в оригинале).
 */
public final class WarmupBotTicker {
   private static final double BASE_STRAFE_SPEED = 0.17;
   private static final double BASE_RADIAL_SPEED = 0.12;
   private static final double MIN_DIST = 2.2;
   private static final double MAX_DIST = 4.0;
   private static final int JUMP_LENGTH = 12;
   private static final double JUMP_HEIGHT = 1.15;
   private static final double MIN_SPEED = 0.05;
   private static final double MAX_SPEED = 5.0;

   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Random RNG = new Random();

   private static boolean registered;
   private static boolean moving;
   private static double speed = 1.0;

   private WarmupBotTicker() {
   }

   public static void register() {
      if (!registered) {
         ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
         registered = true;
      }
   }

   public static boolean isMoving() {
      return moving;
   }

   public static double getSpeed() {
      return speed;
   }

   public static void setSpeed(double value) {
      speed = MathHelper.clamp(value, MIN_SPEED, MAX_SPEED);
   }

   public static void setMoving(boolean value) {
      moving = value;

      for (WarmupBotEntity bot : BotManager.getBots()) {
         if (bot == null) {
            continue;
         }

         bot.setNoGravity(value);
         if (value) {
            bot.baseY = bot.getY();
            bot.strafeTicks = 0;
            bot.distTicks = 0;
            bot.jumpTicks = 0;
         }
      }
   }

   private static void tick() {
      for (WarmupBotEntity bot : new ArrayList<>(BotManager.getBots())) {
         if (bot == null || !bot.isAlive()) {
            BotManager.getBots().remove(bot);
            continue;
         }

         if (!moving || mc.player == null) {
            continue;
         }

         if (!bot.isNoGravity()) {
            bot.setNoGravity(true);
            bot.baseY = bot.getY();
            bot.strafeTicks = 0;
            bot.distTicks = 0;
            bot.jumpTicks = 0;
         }

         step(bot);
      }
   }

   private static void step(WarmupBotEntity bot) {
      Vec3d eye = mc.player.getEntityPos();
      double dx = bot.getX() - eye.x;
      double dz = bot.getZ() - eye.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 0.05) {
         dx = 1.0;
         dz = 0.0;
         dist = 1.0;
      }

      double nx = dx / dist;
      double nz = dz / dist;

      if (--bot.strafeTicks <= 0) {
         bot.strafeDir = RNG.nextBoolean() ? 1 : -1;
         bot.strafeTicks = 6 + RNG.nextInt(19);
      }

      if (--bot.distTicks <= 0) {
         bot.wantDist = MIN_DIST + RNG.nextDouble() * (MAX_DIST - MIN_DIST);
         bot.distTicks = 40 + RNG.nextInt(41);
      }

      double jumpChance = Math.min(0.25, speed / 60.0);
      if (bot.jumpTicks <= 0 && RNG.nextDouble() < jumpChance) {
         bot.jumpTicks = JUMP_LENGTH;
      }

      double strafeSpeed = BASE_STRAFE_SPEED * speed;
      double radialSpeed = BASE_RADIAL_SPEED * speed;

      double strafeX = -nz * strafeSpeed * bot.strafeDir;
      double strafeZ = nx * strafeSpeed * bot.strafeDir;
      double radial = MathHelper.clamp((bot.wantDist - dist) * speed, -radialSpeed, radialSpeed);
      double moveX = strafeX + nx * radial;
      double moveZ = strafeZ + nz * radial;

      double y = bot.baseY;
      if (bot.jumpTicks > 0) {
         double progress = 1.0 - bot.jumpTicks / (double)JUMP_LENGTH;
         y += Math.sin(progress * Math.PI) * JUMP_HEIGHT;
         bot.jumpTicks--;
      }

      bot.lastX = bot.getX();
      bot.lastY = bot.getY();
      bot.lastZ = bot.getZ();
      bot.lastYaw = bot.getYaw();
      bot.lastHeadYaw = bot.headYaw;
      bot.lastBodyYaw = bot.bodyYaw;
      bot.setPosition(bot.getX() + moveX, y, bot.getZ() + moveZ);

      float yaw = (float)Math.toDegrees(Math.atan2(-(bot.getX() - eye.x), bot.getZ() - eye.z));
      bot.setYaw(yaw);
      bot.setHeadYaw(yaw);
      bot.setBodyYaw(yaw);
   }
}
