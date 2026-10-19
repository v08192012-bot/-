package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.SprintReset;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;

@NewFunction(name = "AutoSprint", description = "Автоматически врубает режим бега", category = Category.MOVEMENT)
public class AutoSprint extends Function {
   private static AutoSprint instance;
   private volatile long sprintBlockedUntil = 0L;
   private static int pauseDepth = 0;

   public AutoSprint() {
      instance = this;
   }

   public static AutoSprint getInstance() {
      return instance;
   }

   public boolean l() {
      return pauseDepth > 0 || System.currentTimeMillis() < this.sprintBlockedUntil || SprintReset.a();
   }

   public static void a(long var0) {
      if (instance != null) {
         instance.sprintBlockedUntil = Math.max(instance.sprintBlockedUntil, System.currentTimeMillis() + var0);
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.options != null) {
         if (this.l()) {
            mc.options.sprintKey.setPressed(false);
         } else {
            mc.options.sprintKey.setPressed(true);
         }
      }
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(false);
      }

      super.onDisable();
   }
}
