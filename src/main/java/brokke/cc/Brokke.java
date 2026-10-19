package brokke.cc;

import brokke.cc.animation.NotificationManager;
import brokke.cc.config.AutoBuyConfig;
import brokke.cc.config.ConfigManager;
import brokke.cc.core.AutoBuyEngine;
import brokke.cc.core.FakePlayerRegistry;
import brokke.cc.core.FakePlayerTickHandler;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.core.HelperBinds;
import brokke.cc.core.ItemDatabase;
import brokke.cc.core.NeuroRecorder;
import brokke.cc.core.SilentView;
import brokke.cc.core.User;
import brokke.cc.font.MsdfManager;
import brokke.cc.font.MsdfPipeline;
import brokke.cc.hud.ArmorHud;
import brokke.cc.hud.ArrayListHud;
import brokke.cc.hud.CoolDownsHud;
import brokke.cc.hud.HotbarHud;
import brokke.cc.hud.InfoHud;
import brokke.cc.hud.InventoryHud;
import brokke.cc.hud.KeybindsHud;
import brokke.cc.hud.PotionsHud;
import brokke.cc.hud.ScoreboardHud;
import brokke.cc.hud.TargetHud;
import brokke.cc.hud.WatermarkHud;
import brokke.cc.irc.IrcManager;
import brokke.cc.irc.IrcNotificationManager;
import brokke.cc.manager.AccountManager;
import brokke.cc.manager.BindManager;
import brokke.cc.manager.FriendManager;
import brokke.cc.manager.NicknameManager;
import brokke.cc.module.FunctionManager;
import brokke.cc.nav.GPS;
import brokke.cc.nav.NavigationManager;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.OutlinePipeline;
import brokke.cc.render.RectPipeline;
import brokke.cc.render.TexturePipeline;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventManager;
import brokke.cc.util.SwapHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;

public class Brokke implements ClientModInitializer {
   private static final User USER = User.getInstance();

   public static User getUser() {
      return USER;
   }

   public void onInitializeClient() {
      FunctionManager.a();
      FriendManager.a();
      NicknameManager.a();
      AccountManager.a();
      BindManager.a();
      ItemDatabase.a();
      AutoBuyConfig.i();
      ConfigManager.initialize();
      KeybindsHud.c();
      ArrayListHud.c();
      PotionsHud.c();
      CoolDownsHud.c();
      TargetHud.c();
      WatermarkHud.f();
      InfoHud.c();
      HotbarHud.c();
      ArmorHud.c();
      InventoryHud.c();
      ScoreboardHud.c();
      HelperBinds.c();
      GPS.a();
      NavigationManager.b();
      NotificationManager.register();
      IrcNotificationManager.a();
      IrcManager.getInstance().c();
      FakePlayerRegistry.a();
      FakePlayerTickHandler.a();
      SwapHelper.a();
      HandlerProcessor.b();
      EventBus.register(SilentView.getInstance());
      EventBus.register(new EventManager());
      EventBus.register(AutoBuyEngine.INSTANCE);
      EventBus.register(NeuroRecorder.INSTANCE);
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)var0 -> {
         ConfigManager.saveDefault();
         AutoBuyConfig.g();
         GuiBatch.flush();
         MsdfPipeline.d();
         MsdfManager.c();
         RectPipeline.f();
         OutlinePipeline.f();
         TexturePipeline.b();
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(var0, var1) -> {
         ConfigManager.saveDefault();
         AutoBuyConfig.g();
      });
   }
}
