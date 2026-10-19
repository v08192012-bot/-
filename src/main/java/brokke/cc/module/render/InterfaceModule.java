package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.animation.NotificationManager;
import brokke.cc.core.HelperBinds;
import brokke.cc.hud.ArmorHud;
import brokke.cc.hud.ArrayListHud;
import brokke.cc.hud.CoolDownsHud;
import brokke.cc.hud.HotbarHud;
import brokke.cc.hud.InfoHud;
import brokke.cc.hud.InventoryHud;
import brokke.cc.hud.KeybindsHud;
import brokke.cc.hud.MusicPlayerHud;
import brokke.cc.hud.PotionsHud;
import brokke.cc.hud.ScoreboardHud;
import brokke.cc.hud.TargetHud;
import brokke.cc.hud.WatermarkHud;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ButtonSetting;
import brokke.cc.ui.ClickGui;
import brokke.cc.ui.WatermarkConfigModal;

@NewFunction(name = "Interface", description = "Настройки интерфейса клиента", category = Category.RENDER)
public class InterfaceModule extends Function {
   private static InterfaceModule INSTANCE;
   private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
   private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
   private final BooleanSetting info = new BooleanSetting("Info", true);
   private final BooleanSetting targetHud = new BooleanSetting("Target", true);
   private final BooleanSetting scoreboard = new BooleanSetting("Scoreboard", true);
   private final BooleanSetting potions = new BooleanSetting("Potions", true);
   private final BooleanSetting cooldowns = new BooleanSetting("CoolDowns", true);
   private final BooleanSetting keybinds = new BooleanSetting("Keybinds", true);
   private final BooleanSetting inventory = new BooleanSetting("Inventory", false);
   private final BooleanSetting hotbar = new BooleanSetting("Hotbar", false);
   private final BooleanSetting helperbinds = new BooleanSetting("HelperBinds", true);
   private final BooleanSetting armor = new BooleanSetting("Armor", true);
   private final BooleanSetting musicPlayer = new BooleanSetting("MusicPlayer (отключён)", false);
   private final BooleanSetting notifications = new BooleanSetting("Notifications", true);
   private final BooleanSetting shadows = new BooleanSetting("Тени", true);
   private final ButtonSetting themes = new ButtonSetting(() -> ClickGui.a() ? "Закрыть темы" : "Открыть темы", ClickGui::b);
   private final ButtonSetting watermarkConfig = new ButtonSetting(
      () -> WatermarkConfigModal.isOpen() ? "Закрыть Watermark" : "Настроить Watermark", WatermarkConfigModal::c
   );

   public InterfaceModule() {
      INSTANCE = this;
      this.addSettings(
         this.watermark,
         this.arrayList,
         this.info,
         this.targetHud,
         this.scoreboard,
         this.potions,
         this.cooldowns,
         this.keybinds,
         this.inventory,
         this.hotbar,
         this.helperbinds,
         this.armor,
         this.musicPlayer,
         this.notifications,
         this.shadows,
         this.themes,
         this.watermarkConfig
      );
      this.watermark.onChanged(() -> {
         if (this.isActive()) {
            WatermarkHud.setEnabled(this.watermark.isToggled());
         }
      });
      this.arrayList.onChanged(() -> {
         if (this.isActive()) {
            ArrayListHud.setEnabled(this.arrayList.isToggled());
         }
      });
      this.info.onChanged(() -> {
         if (this.isActive()) {
            InfoHud.setEnabled(this.info.isToggled());
         }
      });
      this.targetHud.onChanged(() -> {
         if (this.isActive()) {
            TargetHud.setEnabled(this.targetHud.isToggled());
         }
      });
      this.scoreboard.onChanged(() -> {
         if (this.isActive()) {
            ScoreboardHud.setEnabled(this.scoreboard.isToggled());
         }
      });
      this.potions.onChanged(() -> {
         if (this.isActive()) {
            PotionsHud.setEnabled(this.potions.isToggled());
         }
      });
      this.cooldowns.onChanged(() -> {
         if (this.isActive()) {
            CoolDownsHud.setEnabled(this.cooldowns.isToggled());
         }
      });
      this.keybinds.onChanged(() -> {
         if (this.isActive()) {
            KeybindsHud.setEnabled(this.keybinds.isToggled());
         }
      });
      this.inventory.onChanged(() -> {
         if (this.isActive()) {
            InventoryHud.setEnabled(this.inventory.isToggled());
         }
      });
      this.hotbar.onChanged(() -> {
         if (this.isActive()) {
            HotbarHud.setEnabled(this.hotbar.isToggled());
         }
      });
      this.helperbinds.onChanged(() -> {
         if (this.isActive()) {
            HelperBinds.setEnabled(this.helperbinds.isToggled());
         }
      });
      this.armor.onChanged(() -> {
         if (this.isActive()) {
            ArmorHud.setEnabled(this.armor.isToggled());
         }
      });
      this.musicPlayer.onChanged(() -> {
         if (this.isActive()) {
            MusicPlayerHud.a(this.musicPlayer.isToggled());
         }
      });
      this.notifications.onChanged(() -> {
         if (this.isActive()) {
            NotificationManager.setEnabled(this.notifications.isToggled());
         }
      });
      this.setEnabled(true);
   }

   @Override
   public void onEnable() {
      WatermarkHud.setEnabled(this.watermark.isToggled());
      ArrayListHud.setEnabled(this.arrayList.isToggled());
      InfoHud.setEnabled(this.info.isToggled());
      TargetHud.setEnabled(this.targetHud.isToggled());
      ScoreboardHud.setEnabled(this.scoreboard.isToggled());
      PotionsHud.setEnabled(this.potions.isToggled());
      CoolDownsHud.setEnabled(this.cooldowns.isToggled());
      KeybindsHud.setEnabled(this.keybinds.isToggled());
      InventoryHud.setEnabled(this.inventory.isToggled());
      HotbarHud.setEnabled(this.hotbar.isToggled());
      HelperBinds.setEnabled(this.helperbinds.isToggled());
      ArmorHud.setEnabled(this.armor.isToggled());
      MusicPlayerHud.a(this.musicPlayer.isToggled());
      NotificationManager.setEnabled(this.notifications.isToggled());
   }

   @Override
   public void onDisable() {
      WatermarkHud.setEnabled(false);
      ArrayListHud.setEnabled(false);
      InfoHud.setEnabled(false);
      TargetHud.setEnabled(false);
      ScoreboardHud.setEnabled(false);
      PotionsHud.setEnabled(false);
      CoolDownsHud.setEnabled(false);
      KeybindsHud.setEnabled(false);
      InventoryHud.setEnabled(false);
      HotbarHud.setEnabled(false);
      HelperBinds.setEnabled(false);
      ArmorHud.setEnabled(false);
      MusicPlayerHud.a(false);
      NotificationManager.setEnabled(false);
   }

   public static boolean k() {
      return true;
   }

   public static boolean l() {
      return INSTANCE == null || !INSTANCE.isActive() || INSTANCE.shadows.isToggled();
   }
}
