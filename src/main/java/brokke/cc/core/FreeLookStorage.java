package brokke.cc.core;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventRotation;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
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
    * Возврат камеры игроку после «тихой» наводки. Камера всё ещё показывает freeYaw
    * (её можно свободно крутить мышью), а игрок плавно доворачивается туда же.
    * Как только углы совпали — камера снова настоящая, и рывка не видно.
    */
   private static boolean releasing;
   private static long releaseStartMs;
   /** Страховка от вечного возврата (игрок умер, мир сменился и т.п.). */
   private static final long RELEASE_TIMEOUT_MS = 700L;
   /** Максимальный шаг доворота игрока за тик — как у обычного человеческого взгляда. */
   private static final float RELEASE_YAW_STEP = 24.0F;
   private static final float RELEASE_PITCH_STEP = 18.0F;

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
            // Вход в «тихий» режим. Если камера ещё возвращается после прошлой наводки,
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
    * Вернуть камеру игроку после «тихой» наводки. Камера остаётся на месте и продолжает
    * слушаться мыши, а игрок доворачивается к ней сам — так камера снова становится
    * настоящей, но на экране ничего не дёргается.
    */
   public static void restore() {
      a();
      if (mc.player == null) {
         active = false;
         silent = false;
         releasing = false;
         return;
      }

      silent = false;
      tracked = true;
      if (active) {
         if (!releasing) {
            releasing = true;
            releaseStartMs = System.currentTimeMillis();
         }
      } else {
         releasing = false;
      }
   }

   /** Камера ещё доезжает до игрока: её нельзя перезаписывать взглядом сущности. */
   public static boolean isReleasing() {
      return releasing;
   }

   public static void a(boolean var0) {
      a();
      if (!var0) {
         silent = false;
      }

      // Любое прямое включение/выключение свободной камеры отменяет плавный возврат:
      // им управляет тот модуль, который сейчас ведёт камеру (FreeLook, AutoExplosion).
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

   /** Доворот игрока к свободному взгляду: пакеты уходят обычным движением, камера стоит. */
   @EventHandler
   public void onTick(UpdateEvent var1) {
      if (!releasing) {
         return;
      }

      if (mc.player == null || mc.world == null) {
         releasing = false;
         active = false;
         return;
      }

      if (mc.player.isRemoved() || mc.player.isDead()) {
         releasing = false;
         active = false;
         return;
      }

      if (mc.currentScreen != null) {
         // В открытом GUI игрок не крутит прицел — доворот продолжим после закрытия.
         return;
      }

      float yawDelta = MathHelper.wrapDegrees(freeYaw - mc.player.getYaw());
      float pitchDelta = freePitch - mc.player.getPitch();
      boolean arrived = Math.abs(yawDelta) <= 0.4F && Math.abs(pitchDelta) <= 0.4F;
      if (arrived || System.currentTimeMillis() - releaseStartMs > RELEASE_TIMEOUT_MS) {
         // Углы совпали — отдаём камеру игроку. Рывка нет: смотрим туда же.
         mc.player.setYaw(freeYaw);
         mc.player.setPitch(MathHelper.clamp(freePitch, -90.0F, 90.0F));
         active = false;
         releasing = false;
         tracked = true;
         return;
      }

      float stepYaw = MathHelper.clamp(yawDelta, -RELEASE_YAW_STEP, RELEASE_YAW_STEP);
      float stepPitch = MathHelper.clamp(pitchDelta, -RELEASE_PITCH_STEP, RELEASE_PITCH_STEP);
      float quantum = GCDUtil.a();
      if (quantum > 1.0E-5F) {
         // Шаг кладём на сетку сенсы: иначе античиты видят «кривой» инкремент поворота.
         stepYaw = Math.round(stepYaw / quantum) * quantum;
         stepPitch = Math.round(stepPitch / quantum) * quantum;
      }

      mc.player.setYaw(mc.player.getYaw() + stepYaw);
      mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + stepPitch, -90.0F, 90.0F));
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
