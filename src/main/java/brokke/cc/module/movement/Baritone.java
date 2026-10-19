package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.nav.NavigationManager;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;

@NewFunction(name = "Baritone", description = "Система навигации и выполнения задач", category = Category.MOVEMENT)
public class Baritone extends Function {
   public static Baritone INSTANCE;
   public final BooleanSetting renderPath = new BooleanSetting("Отрисовка пути", true);
   public final BooleanSetting sprint = new BooleanSetting("Спринт", true);
   public final BooleanSetting autoJump = new BooleanSetting("Автопрыжок", true);

   public Baritone() {
      INSTANCE = this;
      this.addSettings(this.renderPath, this.sprint, this.autoJump);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      NavigationManager.getInstance().getPathRenderer().setEnabled(this.renderPath.isEnabled());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      NavigationManager.getInstance().c();
   }
}
