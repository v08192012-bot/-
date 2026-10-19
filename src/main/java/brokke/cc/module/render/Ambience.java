package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.CustomSkyRenderer;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@NewFunction(name = "Ambience", description = "Изменяет окружение мира", category = Category.RENDER)
public class Ambience extends Function {
   public final ModeSetting skyMode = new ModeSetting(
      "Небо", "Обычное", "Туманность", "Закат", "Галактика", "Северное сияние", "Кровавая луна", "Космос", "Киберпанк", "Энд"
   );
   public final NumberSetting skySpeed = new NumberSetting("Скорость", 1.0, 0.1, 5.0, 0.1);
   public final NumberSetting skyBrightness = new NumberSetting("Яркость неба", 1.0, 0.2, 2.5, 0.1);
   private final BooleanSetting customTime = new BooleanSetting("Изменять время", false);
   private final NumberSetting time = new NumberSetting("Время", 12000.0, 0.0, 24000.0, 250.0);
   private long oldTime;

   public Ambience() {
      this.time.withFormatter(Ambience::a);
      this.addSettings(this.skyMode, this.skySpeed, this.skyBrightness, this.customTime, this.time);
      Runnable var1 = () -> {
         boolean var1x = this.k();
         this.skySpeed.setVisible(var1x);
         this.skyBrightness.setVisible(var1x);
         this.time.setVisible(this.customTime.isToggled());
      };
      this.skyMode.onChanged(var1);
      this.customTime.onChanged(var1);
   }

   public static String a(double var0) {
      long var2 = Math.round(var0) % 24000L;
      if (var2 < 0L) {
         var2 += 24000L;
      }

      long var4 = (var2 / 1000L + 6L) % 24L;
      long var6 = var2 % 1000L * 60L / 1000L;
      String var8;
      if (var4 >= 5L && var4 < 12L) {
         var8 = "утро";
      } else if (var4 >= 12L && var4 < 17L) {
         var8 = "день";
      } else if (var4 >= 17L && var4 < 23L) {
         var8 = "вечер";
      } else {
         var8 = "ночь";
      }

      return String.format("%02d:%02d (%s)", var4, var6, var8);
   }

   public boolean k() {
      return !this.skyMode.isSelected("Обычное") && !this.skyMode.isSelected("Энд");
   }

   public int l() {
      if (this.skyMode.isSelected("Туманность")) {
         return 0;
      } else if (this.skyMode.isSelected("Закат")) {
         return 1;
      } else if (this.skyMode.isSelected("Галактика")) {
         return 2;
      } else if (this.skyMode.isSelected("Северное сияние")) {
         return 3;
      } else if (this.skyMode.isSelected("Кровавая луна")) {
         return 4;
      } else if (this.skyMode.isSelected("Космос")) {
         return 5;
      } else {
         return this.skyMode.isSelected("Киберпанк") ? 6 : 0;
      }
   }

   public ModeSetting getSkyMode() {
      return this.skyMode;
   }

   public NumberSetting getSkySpeed() {
      return this.skySpeed;
   }

   public NumberSetting getSkyBrightness() {
      return this.skyBrightness;
   }

   public BooleanSetting getCustomTime() {
      return this.customTime;
   }

   public NumberSetting getTime() {
      return this.time;
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.isToggled()) {
         var1.setCancelled(true);
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.world != null && this.customTime.isToggled()) {
         mc.world.getLevelProperties().setTimeOfDay((long)this.time.getValue());
      }
   }

   @Override
   public void onEnable() {
      if (mc.world != null) {
         this.oldTime = mc.world.getTime();
      }

      CustomSkyRenderer.getInstance().b();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (mc.world != null) {
         mc.world.getLevelProperties().setTimeOfDay(this.oldTime);
      }

      CustomSkyRenderer.getInstance().b();
      super.onDisable();
   }
}
