package cmode;

import cmode.config.ConfigManager;
import cmode.font.MsdfManager;
import cmode.font.MsdfPipeline;
import cmode.manager.FriendManager;
import cmode.module.FunctionManager;
import cmode.render.GuiBatch;
import cmode.render.OutlinePipeline;
import cmode.render.RectPipeline;
import cmode.render.TexturePipeline;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class Cmode implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      FunctionManager.a();
      FriendManager.a();
      ConfigManager.initialize();

      ClientLifecycleEvents.CLIENT_STOPPING.register(var0 -> {
         ConfigManager.saveDefault();
         GuiBatch.flush();
         MsdfPipeline.d();
         MsdfManager.c();
         RectPipeline.f();
         OutlinePipeline.f();
         TexturePipeline.b();
      });

      ClientPlayConnectionEvents.DISCONNECT.register((var0, var1) -> ConfigManager.saveDefault());
   }
}
