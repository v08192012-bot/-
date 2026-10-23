package shelka.aim;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import shelka.aim.core.WarmupBotRegistry;
import shelka.aim.core.WarmupBotTicker;
import shelka.aim.manager.BotManager;

public final class ShelkaAim implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      WarmupBotRegistry.register();
      WarmupBotTicker.register();
      BotManager.loadConfig();

      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BotManager.saveConfig());
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BotManager.prune());
   }
}
