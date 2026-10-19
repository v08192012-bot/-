package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

@NewFunction(name = "ElytraFly", description = "Взлетает вверх с элитрой (HW)", category = Category.MOVEMENT)
public class ElytraFly extends Function {
   private boolean val121 = false;

   @Override
   public void onDisable() {
      this.val121 = false;
      super.onDisable();
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (mc.player != null) {
         ItemStack var2 = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         if (var2.isOf(Items.ELYTRA)) {
            if (mc.player.isOnGround()) {
               var1.setJump(true);
               this.val121 = true;
            } else if (!mc.player.isGliding()) {
               if (!this.val121) {
                  if (mc.player.networkHandler != null) {
                     mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
                  }

                  mc.player.startGliding();
               } else {
                  this.val121 = false;
               }
            } else {
               this.val121 = false;
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         ItemStack var2 = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         if (var2.isOf(Items.ELYTRA) && mc.player.isGliding()) {
            mc.player.setPitch(0.0F);
            mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y + 0.0305, mc.player.getVelocity().z);
         }
      }
   }
}
