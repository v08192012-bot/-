package brokke.cc.util;

import net.minecraft.client.MinecraftClient;

public class NullCheck {
   MinecraftClient mc = MinecraftClient.getInstance();

   public boolean a() {
      return this.mc.player == null;
   }

   public boolean b() {
      return this.mc.player == null || this.mc.world == null;
   }

   public boolean c() {
      return this.mc.world == null;
   }
}
