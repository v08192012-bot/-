package brokke.cc.util;

import net.minecraft.client.MinecraftClient;

public class MoveUtil {
   public static boolean a() {
      return MinecraftClient.getInstance().player.input.getMovementInput().lengthSquared() > 1.0E-7F;
   }
}
