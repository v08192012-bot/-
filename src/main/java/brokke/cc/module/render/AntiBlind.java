package brokke.cc.module.render;

import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.effect.StatusEffects;

@NewFunction(name = "AntiBlind", description = "Убирает слепоту и тьму — экран не темнеет", category = Category.RENDER)
public class AntiBlind extends Function {
   private final BooleanSetting blindness = new BooleanSetting("Слепота", true);
   private final BooleanSetting darkness = new BooleanSetting("Тьма", true);

   public AntiBlind() {
      this.addSettings(this.blindness, this.darkness);
   }

   public static AntiBlind k() {
      return FunctionManager.a(AntiBlind.class);
   }

   /**
    * Слепоту гасим ещё и в рендере: эффект мог прийти в этом же тике, уже после
    * нашей чистки, и туман успел бы отрисоваться до следующего тика.
    */
   public static boolean blindnessHidden() {
      AntiBlind var0 = k();
      return var0 != null && var0.isActive() && var0.blindness.isToggled();
   }

   public static boolean darknessHidden() {
      AntiBlind var0 = k();
      return var0 != null && var0.isActive() && var0.darkness.isToggled();
   }

   @Override
   public void onEnable() {
      this.purge();
   }

   /**
    * Эффект снимается только на клиенте: сервер о нём не знает и ничего не проверяет.
    * Поэтому слепота не темнит экран, не сужает обзор и не запрещает спринт — ровно
    * то, что мешает в ПвП. Пакетов не отправляется, флагов это дать не может.
    */
   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.purge();
   }

   private void purge() {
      if (mc.player == null) {
         return;
      }

      if (this.blindness.isToggled()) {
         mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
      }

      if (this.darkness.isToggled()) {
         mc.player.removeStatusEffect(StatusEffects.DARKNESS);
      }
   }
}
