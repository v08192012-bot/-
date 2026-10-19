package brokke.cc.core;

import brokke.cc.util.SwapUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public final class MovementStop {
   private static final MinecraftClient a = MinecraftClient.getInstance();

   private MovementStop() {
   }

   public static void a() {
      if (a.player != null && a.options != null) {
         SwapUtil.c().forEach(var0x -> var0x.setPressed(false));
         a.player.setSprinting(false);
         if (a.player.input != null) {
            a.player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
         }

         Vec3d var0 = a.player.getVelocity();
         a.player.setVelocity(0.0, var0.y, 0.0);
      }
   }
}
