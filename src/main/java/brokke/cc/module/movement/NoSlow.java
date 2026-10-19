package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.NoSlowEvent;
import brokke.cc.system.events.impl.UpdateEvent;

@NewFunction(name = "No Slow Down", description = "Убирает замедления от использования предметов", category = Category.MOVEMENT)
public class NoSlow extends Function {
   private static NoSlow instance;
   public static final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Grim");
   private int ticks = 0;
   private boolean cancel = false;

   public NoSlow() {
      this.addSettings(mode);
      instance = this;
   }

   @EventHandler
   public void onNoSlow(NoSlowEvent var1) {
      if (!this.nullCheck.b()) {
         switch (mode.getSelection()) {
            case "Vanilla":
               var1.setCancelled(true);
               break;
            case "Grim":
               if (this.cancel) {
                  var1.setCancelled(true);
               }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (!this.nullCheck.b()) {
         if (!mc.player.isUsingItem()) {
            this.ticks = 0;
            this.cancel = false;
         } else {
            this.ticks++;
            this.cancel = this.ticks % 2 == 0;
         }
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.ticks = 0;
      this.cancel = false;
   }

   public static NoSlow getInstance() {
      return instance;
   }
}
