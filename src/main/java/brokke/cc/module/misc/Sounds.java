package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.util.SoundUtil;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;

@NewFunction(name = "Sounds", description = "Добавляет звуки клиента", category = Category.MISC)
public class Sounds extends Function {
   private static Sounds instance;
   private final ModeSetting soundType = new ModeSetting("Тип", "Тип 1", "Тип 2", "Тип 3", "Тип 4");
   private final NumberSetting volume = new NumberSetting("Громкость звука", 1.0, 0.1, 1.0, 0.1);

   public static Sounds k() {
      if (instance == null) {
         instance = FunctionManager.a(Sounds.class);
      }

      return instance;
   }

   public Sounds() {
      instance = this;
      this.addSettings(this.soundType, this.volume);
   }

   public void b(boolean var1) {
      if (this.isActive()) {
         String var2 = switch (this.soundType.getActiveMode()) {
            case "Тип 2" -> "enable1.wav";
            case "Тип 3" -> "enable2.wav";
            case "Тип 4" -> "enable3.wav";
            default -> "enable.wav";
         };
         float var5 = (float)this.volume.getValue();
         float var6 = var1 ? 1.0F : 0.8F;
         SoundUtil.a("/assets/brokke/sounds/" + var2, var5, var6);
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof GameMessageS2CPacket var2 && mc.player != null) {
         String var4 = var2.content().getString();
         if (var4.contains("Вы успешно купили") || var4.contains("отправлено игроку")) {
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, (float)this.volume.getValue(), 1.0F);
         }
      }
   }

   public ModeSetting getSoundType() {
      return this.soundType;
   }

   public NumberSetting getVolume() {
      return this.volume;
   }
}
