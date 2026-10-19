package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.Timer2;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveVelocity;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@NewFunction(name = "GrimGlide", description = "Полёты на элитре", category = Category.MOVEMENT)
public class GrimGlide extends Function {
   public final ModeSetting mode = new ModeSetting("Режим", "Бесконечный RW");
   private int flagTicks = 0;
   private final Timer2 timer = new Timer2();

   public GrimGlide() {
      this.addSettings();
   }

   @Override
   public void onDisable() {
      this.flagTicks = 0;
      this.timer.a();
      super.onDisable();
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
      }

      this.flagTicks = 10;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
      }

      if (mc.world == null) {
      }
   }

   @EventHandler
   public void onMoveVelocity(EventMoveVelocity var1) {
      if (mc.player != null) {
      }

      if (mc.world != null) {
      }

      if (!mc.player.isGliding()) {
      }
   }

   private void k() {
      if (mc.player == null) {
      }
   }
}
