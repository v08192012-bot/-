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
         if (!silent && mc.player != null) {
            // Вход в «тихий» режим: сейчас камера и игрок смотрят в одну сторону,
            // поэтому фиксируем взгляд как стартовую точку свободной камеры.
            freeYaw = mc.player.getYaw();
            freePitch = mc.player.getPitch();
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

   public static void a(boolean var0) {
      a();
      if (!var0) {
         silent = false;
      }

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
      } else if (mc.player != null && !silent) {
         freeYaw = var1.getYaw();
         freePitch = var1.getPitch();
         tracked = true;
      }
   }
}
