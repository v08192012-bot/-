package brokke.cc.util;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public final class SwapUtil {
   private static final MinecraftClient a = MinecraftClient.getInstance();

   private SwapUtil() {
   }

   public static void a() {
   }

   public static void b() {
      if (a.player != null) {
         if (a.player.currentScreenHandler != null && !(a.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            a.player.closeHandledScreen();
         } else if (a.currentScreen != null) {
            a.setScreen(null);
         }
      }
   }

   public static void a(int var0, int var1) {
      if (a.interactionManager != null && a.player != null && var0 >= 0) {
         int var2 = var0 < 9 ? var0 + 36 : var0;
         if (var2 != 45 || var1 != 40) {
            a.interactionManager.clickSlot(a.player.playerScreenHandler.syncId, var2, var1, SlotActionType.SWAP, a.player);
         }
      }
   }

   public static List<KeyBinding> c() {
      return List.of(a.options.forwardKey, a.options.backKey, a.options.leftKey, a.options.rightKey, a.options.jumpKey, a.options.sprintKey);
   }

   public static boolean a(int var0) {
      if (var0 != -1 && var0 >= 0) {
         long var1 = a.getWindow().getHandle();
         return var0 >= 0 && var0 <= 7 ? GLFW.glfwGetMouseButton(var1, var0) == 1 : GLFW.glfwGetKey(var1, var0) == 1;
      } else {
         return false;
      }
   }

   public static Key b(int var0) {
      if (var0 < 0) {
         return InputUtil.UNKNOWN_KEY;
      } else {
         return var0 >= 0 && var0 <= 7 ? Type.MOUSE.createFromCode(var0) : Type.KEYSYM.createFromCode(var0);
      }
   }

   public static int a(Key var0) {
      return var0 != null && var0 != InputUtil.UNKNOWN_KEY ? var0.getCode() : -1;
   }
}
