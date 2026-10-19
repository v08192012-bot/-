package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.config.AutoBuyConfig;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ButtonSetting;
import brokke.cc.ui.ClickGui;

@NewFunction(name = "AutoBuy", description = "Автоматическая покупка и парсинг аукциона", category = Category.MISC)
public class AutoBuy extends Function {
   private final ButtonSetting openMenu = new ButtonSetting("Открыть меню", ClickGui::j);
   private final BooleanSetting auctionButtons = new BooleanSetting("Кнопки на аукционе", true);
   private final BooleanSetting autoSell = new BooleanSetting("AutoSell", false);
   private final BooleanSetting autoRelist = new BooleanSetting("AutoRelist", false);
   private final BooleanSetting autoParse = new BooleanSetting("AutoParse", true);
   private final BooleanSetting autoPay = new BooleanSetting("AutoPay", false);

   public AutoBuy() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      this.autoSell.applyValue(var1.autoSell);
      this.autoRelist.applyValue(var1.autoRelist);
      this.autoParse.applyValue(var1.autoParse);
      this.autoPay.applyValue(var1.autoPay);
      this.addSettings(this.openMenu, this.auctionButtons, this.autoSell, this.autoRelist, this.autoParse, this.autoPay);
   }

   public boolean k() {
      return this.auctionButtons.getValue();
   }

   @Override
   public void onEnable() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      var1.enabled = true;
      var1.autoSell = this.autoSell.getValue();
      var1.autoRelist = this.autoRelist.getValue();
      var1.autoParse = this.autoParse.getValue();
      var1.autoPay = this.autoPay.getValue();
      AutoBuyConfig.g();
   }

   @Override
   public void onDisable() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      var1.enabled = false;
      AutoBuyConfig.g();
   }
}
