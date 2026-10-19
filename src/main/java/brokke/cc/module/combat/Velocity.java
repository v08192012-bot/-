package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "Velocity", description = "Изменение отбрасывания", category = Category.COMBAT)
public class Velocity extends Function {
   private final ModeSetting mode = new ModeSetting("Режим", "Отмена", "Компенсация", "Изменение");
   private final NumberSetting modifierX = new NumberSetting("Отбрасывание X", 50.0, 0.0, 100.0, 1.0);
   private final NumberSetting modifierY = new NumberSetting("Отбрасывание Y", 50.0, 0.0, 100.0, 1.0);
   private final NumberSetting modifierZ = new NumberSetting("Отбрасывание Z", 50.0, 0.0, 100.0, 1.0);
   private Vec3d lastMotion = Vec3d.ZERO;
   private boolean gotVelocity;
   private boolean wasHurt;
   private boolean jumped;

   public Velocity() {
      this.addSettings(this.mode, this.modifierX, this.modifierY, this.modifierZ);
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.player != null && !mc.player.isDead() && var1.getPacket() instanceof EntityVelocityUpdateS2CPacket var3 && var3.getEntityId() == mc.player.getId()
         )
       {
         if (this.mode.isMode("Отмена")) {
            var1.setCancelled(true);
         } else if (this.mode.isMode("Изменение")) {
            var1.setCancelled(true);
            Vec3d var4 = var3.getVelocity();
            mc.player
               .setVelocity(var4.x * this.modifierX.getValue() / 100.0, var4.y * this.modifierY.getValue() / 100.0, var4.z * this.modifierZ.getValue() / 100.0);
         } else if (this.mode.isMode("Компенсация")) {
            this.lastMotion = var3.getVelocity();
            this.gotVelocity = true;
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && this.mode.isMode("Компенсация") && this.gotVelocity) {
         if (mc.player.hurtTime > 0) {
            this.wasHurt = true;
         }

         if (this.wasHurt && mc.player.hurtTime == 0) {
            if (mc.player.isOnGround()) {
               mc.player.jump();
               this.jumped = true;
            }

            Vec3d var2 = this.lastMotion.normalize();
            Vec3d var3 = var2.multiply(-0.2);
            mc.player.setVelocity(var3.x, var3.y, var3.z);
            this.wasHurt = false;
            this.gotVelocity = false;
         }

         if (this.jumped && mc.player.isOnGround()) {
            this.jumped = false;
         }
      }
   }
}
