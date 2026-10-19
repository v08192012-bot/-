package brokke.cc.util;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.system.events.impl.EventMoveInput;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MovingUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean a() {
      return mc.player == null
         ? false
         : mc.player.input.playerInput.forward()
            || mc.player.input.playerInput.backward()
            || mc.player.input.playerInput.left()
            || mc.player.input.playerInput.right();
   }

   public static double[] a(double var0) {
      if (mc.player == null) {
         return new double[]{0.0, 0.0};
      }

      float var2 = (mc.player.input.playerInput.forward() ? 1.0F : 0.0F) - (mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
      float var3 = (mc.player.input.playerInput.left() ? 1.0F : 0.0F) - (mc.player.input.playerInput.right() ? 1.0F : 0.0F);
      float var4 = mc.player.getYaw();
      if (var2 != 0.0F) {
         if (var3 > 0.0F) {
            var4 += var2 > 0.0F ? -45.0F : 45.0F;
         } else if (var3 < 0.0F) {
            var4 += var2 > 0.0F ? 45.0F : -45.0F;
         }

         var3 = 0.0F;
         var2 = var2 > 0.0F ? 1.0F : -1.0F;
      }

      double var5 = Math.sin(Math.toRadians(var4 + 90.0F));
      double var7 = Math.cos(Math.toRadians(var4 + 90.0F));
      double var9 = var2 * var0 * var7 + var3 * var0 * var5;
      double var11 = var2 * var0 * var5 - var3 * var0 * var7;
      return new double[]{var9, var11};
   }

   public static double a(Entity var0) {
      double var1 = var0.getX() - var0.lastRenderX;
      double var3 = var0.getY() - var0.lastRenderY;
      double var5 = var0.getZ() - var0.lastRenderZ;
      return Math.sqrt(var1 * var1 + var5 * var5 + var3 * var3);
   }

   public static void b(double var0) {
      double[] var2 = a(var0);
      Objects.requireNonNull(mc.player).setVelocity(var2[0], mc.player.getVelocity().getY(), var2[1]);
   }

   public static void a(double var0, double var2) {
      double[] var4 = a(var0);
      Objects.requireNonNull(mc.player).setVelocity(var4[0], var2, var4[1]);
   }

   public static double a(Vec3d var0, float var1) {
      float var2 = (float)Math.atan2(-var0.x, var0.z);
      double var3 = Math.toRadians(MathHelper.wrapDegrees(var1));
      return Math.toDegrees(MathHelper.wrapDegrees(var2 - var3));
   }

   public static PlayerInput a(PlayerInput var0, double var1, float var3) {
      boolean var4 = var0.forward();
      boolean var5 = var0.backward();
      boolean var6 = var0.left();
      boolean var7 = var0.right();
      if (var1 >= -90.0F + var3 && var1 <= 90.0F - var3) {
         var4 = true;
      } else if (var1 < -90.0F - var3 || var1 > 90.0F + var3) {
         var5 = true;
      }

      if (var1 >= 0.0F + var3 && var1 <= 180.0F - var3) {
         var7 = true;
      } else if (var1 >= -180.0F + var3 && var1 <= 0.0F - var3) {
         var6 = true;
      }

      return new PlayerInput(var4, var5, var6, var7, var0.jump(), var0.sneak(), var0.sprint());
   }

   public static void a(EventMoveInput var0, float var1) {
      float var2 = var0.getForward();
      float var3 = var0.getStrafe();
      if (var2 != 0.0F || var3 != 0.0F) {
         double var4 = MathHelper.wrapDegrees(Math.toDegrees(a(var1, var2, var3)));
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = Float.MAX_VALUE;

         for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
            for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
               if (var9 != 0.0F || var10 != 0.0F) {
                  double var11 = MathHelper.wrapDegrees(Math.toDegrees(a(var1, var9, var10)));
                  float var13 = Math.abs(MathHelper.wrapDegrees((float)(var4 - var11)));
                  if (var13 < var8) {
                     var8 = var13;
                     var6 = var9;
                     var7 = var10;
                  }
               }
            }
         }

         var0.setForward(var6);
         var0.setStrafe(var7);
      }
   }

   public static void a(EventMoveInput var0) {
      if (mc.player != null) {
         float var1 = var0.getForward();
         float var2 = var0.getStrafe();
         double var3 = MathHelper.wrapDegrees(Math.toDegrees(a(mc.player.isGliding() ? mc.player.getYaw() : FreeLookStorage.getFreeYaw(), var1, var2)));
         if (var1 != 0.0F || var2 != 0.0F) {
            float var5 = 0.0F;
            float var6 = 0.0F;
            float var7 = Float.MAX_VALUE;

            for (float var8 = -1.0F; var8 <= 1.0F; var8++) {
               for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
                  if (var9 != 0.0F || var8 != 0.0F) {
                     double var10 = MathHelper.wrapDegrees(Math.toDegrees(a(mc.player.getYaw(), var8, var9)));
                     double var12 = Math.abs(var3 - var10);
                     if (var12 < var7) {
                        var7 = (float)var12;
                        var5 = var8;
                        var6 = var9;
                     }
                  }
               }
            }

            var0.setForward(var5);
            var0.setStrafe(var6);
         }
      }
   }

   public static double a(float var0, float var1, float var2) {
      if (var1 < 0.0F) {
         var0 += 180.0F;
      }

      float var3 = 1.0F;
      if (var1 < 0.0F) {
         var3 = -0.5F;
      }

      if (var1 > 0.0F) {
         var3 = 0.5F;
      }

      if (var2 > 0.0F) {
         var0 -= 90.0F * var3;
      }

      if (var2 < 0.0F) {
         var0 += 90.0F * var3;
      }

      return Math.toRadians(var0);
   }

   public static PlayerInput a(PlayerInput var0, double var1) {
      return a(var0, var1, 20.0F);
   }

   private MovingUtil() {
   }
}
