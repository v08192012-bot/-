package brokke.cc.core;

import brokke.cc.system.events.EventBus;
import net.minecraft.client.MinecraftClient;

public class BaseHandler {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();

   public BaseHandler() {
      EventBus.register(this);
   }
}
