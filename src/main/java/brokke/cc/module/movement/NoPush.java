package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;

@NewFunction(name = "No Push", description = "Отключает отталкивание от сущностей, воды и блоков", category = Category.MOVEMENT)
public class NoPush extends Function {
   public static NoPush Instance;
   public static BooleanSetting players = new BooleanSetting("Сущности", true);
   public static BooleanSetting water = new BooleanSetting("Вода", true);
   public static BooleanSetting blocks = new BooleanSetting("Блоки", true);

   public NoPush() {
      Instance = this;
      this.addSettings(players, water, blocks);
   }
}
