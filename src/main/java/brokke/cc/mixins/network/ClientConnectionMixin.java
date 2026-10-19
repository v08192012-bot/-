package brokke.cc.mixins.network;

import brokke.cc.core.SlotTracker;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.SendPacketEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   @Unique
   private static boolean stackOverflowFix;

   @Shadow
   public abstract boolean isLocal();

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   public void triggerSendPacketEvent(Packet<?> var1, CallbackInfo var2) {
      if (var1 instanceof UpdateSelectedSlotC2SPacket var3) {
         int var4 = var3.getSelectedSlot();
         if (var4 < 0 || var4 > 8 || var4 == SlotTracker.serverHeldItemSlot) {
            var2.cancel();
            return;
         }

         SlotTracker.serverHeldItemSlot = var4;
      }

      SendPacketEvent var5 = new SendPacketEvent(var1);
      if (!stackOverflowFix) {
         EventBus.post(var5);
         if (var5.isCancelled()) {
            var2.cancel();
            return;
         }

         Packet var6 = var5.getPacket();
         if (var6 != var1) {
            var2.cancel();
            stackOverflowFix = true;
            if (mc.getNetworkHandler() != null) {
               mc.getNetworkHandler().sendPacket(var6);
            }

            stackOverflowFix = false;
         }
      }
   }
}
