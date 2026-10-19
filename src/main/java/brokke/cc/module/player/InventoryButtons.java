package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.ui.InventoryButtonsOverlay;

@NewFunction(name = "Inventory Buttons", description = "Кнопки над инвентарем для сброса вещей", category = Category.PLAYER)
public class InventoryButtons extends Function {
   public final NumberSetting delay = new NumberSetting("Задержка", 15.0, 0.0, 100.0, 1.0);
   public final BooleanSetting secondButton = new BooleanSetting("Доп. кнопка", true);
   public final BooleanSetting inContainers = new BooleanSetting("В сундуках", true);

   public InventoryButtons() {
      this.addSettings(this.delay, this.secondButton, this.inContainers);
      this.setEnabled(true);
   }

   @Override
   public void onDisable() {
      InventoryButtonsOverlay.a();
   }
}
