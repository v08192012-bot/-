package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.screen.ShulkerBoxScreenHandler;

@NewFunction(name = "ShulkerJump", description = "Подлетаешь вверх при взаимодействии с шалкером", category = Category.MOVEMENT)
public class ShulkerJump extends Function {
   private final NumberSetting velocity = new NumberSetting("Сила подброса", 2.4, 0.5, 5.0, 0.1);
   private int ticks = 0;
   private boolean jumping = false;

   public ShulkerJump() {
      this.addSettings(this.velocity);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler && !this.jumping) {
            mc.options.jumpKey.setPressed(true);
            mc.player.closeHandledScreen();
            mc.player.addVelocity(0.0, this.velocity.getValue(), 0.0);
            this.jumping = true;
            this.ticks = 0;
         }

         if (this.jumping) {
            if (this.ticks >= 5) {
               mc.player.closeHandledScreen();
               mc.options.jumpKey.setPressed(false);
               this.jumping = false;
               this.ticks = 0;
            } else {
               this.ticks++;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.jumping = false;
      this.ticks = 0;
      if (mc.options != null && mc.options.jumpKey != null) {
         mc.options.jumpKey.setPressed(false);
      }

      super.onDisable();
   }
}
