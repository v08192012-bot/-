package brokke.cc.mixins.interfaces;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface IClientPlayerInteractionManager {
   @Accessor("blockBreakingCooldown")
   void setBlockBreakingCooldown(int var1);

   @Invoker("sendSequencedPacket")
   void invokeSendSequencedPacket(ClientWorld var1, SequencedPacketCreator var2);
}
