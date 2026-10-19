package brokke.cc.core;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class InvBypass {
   private static final MinecraftClient a = MinecraftClient.getInstance();

   private InvBypass() {
   }

   public static void a(List<KeyBinding> var0) {
      if (a.options != null && a.options.sprintKey != null) {
         a.options.sprintKey.setPressed(false);
      }

      if (a.player != null) {
         a.player.setSprinting(false);
      }

      if (var0 != null) {
         var0.forEach(var0x -> {
            if (var0x != null) {
               var0x.setPressed(false);
            }
         });
      }
   }

   public static void b(List<KeyBinding> var0) {
      if (a.getWindow() != null && a.options != null && var0 != null) {
         for (KeyBinding var2 : var0) {
            if (var2 != null) {
               int var3 = var2.getDefaultKey().getCode();
               var2.setPressed(InputUtil.isKeyPressed(a.getWindow(), var3));
            }
         }
      }
   }
}
