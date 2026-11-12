package brokke.cc.core;

import brokke.cc.module.movement.AutoSprint;
import brokke.cc.system.events.impl.EventMoveInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import org.lwjgl.glfw.GLFW;

public final class SprintReset {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static int resetTicks = 0;
   private static String activeMode = "Нет";
   private static int lastTickAge = Integer.MIN_VALUE;
   /** Спринт снят именно нами, под крит (а не режимом «Сброс спринта»). */
   private static boolean critRelease;
   /** Сколько тиков держим спринт снятым, если удар так и не случился. */
   private static int critHoldTicks;

   private SprintReset() {
   }

   public static boolean a() {
      // Пока спринт снят под крит, AutoSprint не должен жать клавишу обратно.
      return resetTicks > 0 || critRelease;
   }

   public static void a(String var0) {
      boolean var1 = mc.player != null && mc.player.isSprinting();
      a(var0, var1);
   }

   public static void a(String var0, boolean var1) {
      if (mc.player != null
         && var0 != null
         && !var0.equals("Нет")
         && mc.player.isOnGround()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isClimbing()) {
         boolean var2 = mc.options != null && mc.options.sprintKey.isPressed();
         AutoSprint var3 = AutoSprint.getInstance();
         boolean var4 = var3 != null && var3.isActive();
         if (var1 || var2 || var4) {
            activeMode = var0;
            critRelease = false;
            resetTicks = 1;
            if (mc.options != null) {
               mc.options.sprintKey.setPressed(false);
            }
         }
      }
   }

   /**
    * Отпускание спринта под крит в воздухе (W-тап на падении).
    * Обычный a(mode, bool) работает только на земле — там crit и так невозможен,
    * а вот в падении спринт нужно снять самим флагом: это тот же ванильный путь,
    * по которому уходит ClientCommandC2SPacket(STOP_SPRINTING).
    *
    * @return true, если спринт действительно отпущен
    */
   public static boolean d() {
      if (mc.player == null || mc.options == null || !mc.player.isSprinting()) {
         return false;
      }

      activeMode = "Legit";
      critRelease = true;
      // Спринт держим снятым до удара (restoreCritSprint вернёт его сразу после),
      // но не дольше critHoldTicks — иначе можно застрять без спринта.
      critHoldTicks = 12;
      resetTicks = 2;
      mc.options.sprintKey.setPressed(false);
      mc.player.setSprinting(false);
      return true;
   }

   /** Спринт снят под крит и ещё не восстановлен. */
   public static boolean isCritRelease() {
      return critRelease;
   }

   /**
    * Мгновенно вернуть спринт после крита.
    *
    * Раньше клавиша ждала два тика: если удар приходился в самом конце падения,
    * аура приземлялась всё ещё без спринта и следующий удар уходил сплэшем.
    * Возврат сразу после удара ускоряет следующий «перекрит» и убирает такие сплэши.
    */
   public static void restoreCritSprint() {
      if (!critRelease) {
         return;
      }

      critRelease = false;
      critHoldTicks = 0;
      resetTicks = 0;
      c();
   }

   public static void a(EventMoveInput var0) {
      if (mc.player != null && a()) {
         if (activeMode.equals("W-Tap")) {
            var0.setForward(0.0F);
            var0.setSprint(false);
         } else if (activeMode.equals("S-Tap")) {
            var0.setForward(-1.0F);
            var0.setSprint(false);
         } else if (activeMode.equals("Legit")) {
            var0.setSprint(false);
         }
      }
   }

   public static void b() {
      if (mc.player == null || mc.world == null) {
         resetTicks = 0;
         critRelease = false;
         critHoldTicks = 0;
         lastTickAge = Integer.MIN_VALUE;
      } else if (lastTickAge != mc.player.age) {
         lastTickAge = mc.player.age;
         if (resetTicks > 0) {
            resetTicks--;
            if (resetTicks == 0 && !critRelease) {
               c();
            }
         }

         if (critRelease && --critHoldTicks <= 0) {
            restoreCritSprint();
         }
      }
   }

   private static void c() {
      if (mc.options != null) {
         AutoSprint var0 = AutoSprint.getInstance();
         if (var0 != null && var0.isActive() && !var0.l()) {
            mc.options.sprintKey.setPressed(true);
         } else if (a(mc.options.sprintKey) || mc.options.getSprintToggled() != null && mc.options.getSprintToggled().getValue()) {
            mc.options.sprintKey.setPressed(true);
         }
      }
   }

   private static boolean a(KeyBinding var0) {
      if (var0 != null && mc.getWindow() != null) {
         Window var1 = mc.getWindow();
         Key var2 = InputUtil.fromTranslationKey(var0.getBoundKeyTranslationKey());
         if (var2.getCategory() == Type.KEYSYM) {
            return InputUtil.isKeyPressed(var1, var2.getCode());
         } else {
            return var2.getCategory() == Type.MOUSE ? GLFW.glfwGetMouseButton(var1.getHandle(), var2.getCode()) == 1 : false;
         }
      } else {
         return false;
      }
   }
}
