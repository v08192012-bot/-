package brokke.cc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

public class CursorHelper {
   private static long arrowCursor;
   private static boolean initialized = false;

   public static void a() {
      if (!initialized) {
         arrowCursor = GLFW.glfwCreateStandardCursor(221185);
         initialized = true;
      }
   }

   public static void b() {
      if (!initialized) {
         a();
      }

      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null) {
         Window var1 = var0.getWindow();
         if (var1 != null) {
            var0.mouse.unlockCursor();
            GLFW.glfwSetInputMode(var1.getHandle(), 208897, 212993);
            if (arrowCursor != 0L) {
               GLFW.glfwSetCursor(var1.getHandle(), arrowCursor);
            }
         }
      }
   }

   public static void c() {
      if (!initialized) {
         a();
      }

      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null) {
         Window var1 = var0.getWindow();
         if (var1 != null) {
            var0.mouse.unlockCursor();
            GLFW.glfwSetInputMode(var1.getHandle(), 208897, 212993);
         }
      }
   }
}
