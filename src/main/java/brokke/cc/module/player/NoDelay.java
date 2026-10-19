package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.interfaces.ILivingEntity;
import brokke.cc.mixins.interfaces.IMinecraftClient;
import brokke.cc.setting.MultiSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.item.Items;

@NewFunction(name = "NoDelay", description = "Убирает задержку у выбранных элементов", category = Category.PLAYER)
public class NoDelay extends Function {
   final MultiSetting delay = new MultiSetting("Убрать задержку", "Прыжка", "ПКМ", "Пузырьки опыта");

   public NoDelay() {
      this.addSettings(this.delay);
   }

   @EventHandler
   public void onTick(UpdateEvent var1) {
      if (!this.nullCheck.a()) {
         if (this.delay.isOptionEnabled("Прыжка")) {
            ((ILivingEntity)mc.player).setJumpingCooldown(0);
         }

         if (this.delay.isOptionEnabled("ПКМ")) {
            ((IMinecraftClient)mc).setItemUseCooldown(0);
         } else if (this.delay.isOptionEnabled("Пузырьки опыта")
            && (mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE) || mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE))) {
            ((IMinecraftClient)mc).setItemUseCooldown(0);
         }
      }
   }
}
