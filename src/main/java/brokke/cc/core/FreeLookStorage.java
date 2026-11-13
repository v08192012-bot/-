package brokke.cc.core;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventRotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class FreeLookStorage {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final FreeLookStorage INSTANCE = new FreeLookStorage();
   private static boolean active;
   private static float freeYaw;
   private static float freePitch;
   private static boolean registered;
   /** Свободная камера уже отслеживается onRotation (то есть freeYaw — это реальный взгляд). */
   private static boolean tracked;
   /**
    * «Тихий» режим: аура крутит игрока для сервера, а камера полностью наша.
    * В этом режиме onRotation НЕ перезаписывает freeYaw (иначе камеру утаскивает
    * за серверной ротацией, как только режим на тик выключился).
    */
   private static boolean silent;
   /**
    * Затухание картинки при выходе из «тихой» наводки. Это ЧИСТО РЕНДЕР: игрок,
    * его поворот и движение не трогаются вообще — WASD продолжает работать как обычно.
    * Нужно лишь чтобы камера не щёлкнула из свободного взгляда в настоящий.
    */
   private static boolean releasing;
   private static long releaseStartMs;
   private static float releaseFromYaw;
   private static float releaseFromPitch;
   private static final long RELEASE_MS = 130L;

   public static FreeLookStorage a() {
      if (!registered) {
         EventBus.register(INSTANCE);
         registered = true;
      }

      return INSTANCE;
   }

   public static boolean isActive() {
      return active;
   }

   /** Включить/выключить «тихую» наводку: камера живёт отдельно от игрока. */
   public static void silent(boolean var0) {
      a();
      if (var0) {
         if (!silent) {
            // Вход в «тихий» режим. Если камера ещё доезжает после прошлой наводки,
            // НЕ переставляем её на взгляд игрока: взгляд игрока — это прицел ауры,
            // и камера прыгнула бы туда рывком.
            if (!releasing && mc.player != null) {
               freeYaw = mc.player.getYaw();
               freePitch = mc.player.getPitch();
            }

            releasing = false;
            tracked = true;
         }

         silent = true;
         active = true;
      } else {
         silent = false;
      }
   }

   public static boolean isSilent() {
      return silent;
   }

   /**
    * Закончить «тихую» наводку. Камера мгновенно снова настоящая (мышь, WASD, прицел —
    * всё как обычно), но первые ~130 мс картинка плавно доезжает до настоящего взгляда,
    * чтобы не было резкого щелчка. Игрок при этом не поворачивается ни на градус.
    */
   public static void restore() {
      a();
      if (!silent) {
         return;
      }

      silent = false;
      if (active && mc.player != null) {
         releaseFromYaw = freeYaw;
         releaseFromPitch = freePitch;
         releaseStartMs = System.currentTimeMillis();
         releasing = true;
         active = false;
         tracked = false;
      }
   }

   /** Камера ещё доезжает до настоящего взгляда (только картинка). */
   public static boolean isReleasing() {
      return releasing;
   }

   public static void a(boolean var0) {
      a();
      if (!var0) {
         silent = false;
      }

      // Любое прямое включение свободной камеры отменяет затухание:
      // камерой управляет тот модуль, который её включил (FreeLook, AutoExplosion).
      releasing = false;
      if (var0 && !active) {
         // Важно: при включении НЕ берём yaw игрока — в этот момент он уже может быть
         // повёрнут серверной (silent) ротацией ауры, и камеру утащило бы за ней.
         // freeYaw/freePitch и так следят за камерой в onRotation, когда режим выключен.
         if (!tracked && mc.player != null) {
            freeYaw = mc.player.getYaw();
            freePitch = mc.player.getPitch();
            tracked = true;
         }
      }

      active = var0;
   }

   public static float getFreeYaw() {
      return freeYaw;
   }

   public static void setFreeYaw(float var0) {
      freeYaw = var0;
   }

   public static float getFreePitch() {
      return freePitch;
   }

   public static void setFreePitch(float var0) {
      freePitch = var0;
   }

   public static void a(double var0, double var2) {
      freeYaw += (float)(var0 * 0.15);
      freePitch = MathHelper.clamp(freePitch + (float)(var2 * 0.15), -90.0F, 90.0F);
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onRotation(EventRotation var1) {
      if (active) {
         var1.setYaw(freeYaw);
         var1.setPitch(freePitch);
         return;
      }

      if (releasing && mc.player != null) {
         float var2 = MathHelper.clamp((float)(System.currentTimeMillis() - releaseStartMs) / RELEASE_MS, 0.0F, 1.0F);
         float var3 = var2 * var2 * (3.0F - 2.0F * var2);
         float var4 = releaseFromYaw + MathHelper.wrapDegrees(var1.getYaw() - releaseFromYaw) * var3;
         float var5 = releaseFromPitch + (var1.getPitch() - releaseFromPitch) * var3;
         var1.setYaw(var4);
         var1.setPitch(var5);
         // Руки рисуются по этим углам: держим их ровно там же, где камера.
         freeYaw = var4;
         freePitch = var5;
         tracked = true;
         if (var2 >= 1.0F) {
            releasing = false;
         }

         return;
      }

      if (mc.player != null && !silent) {
         freeYaw = var1.getYaw();
         freePitch = var1.getPitch();
         tracked = true;
      }
   }
}
