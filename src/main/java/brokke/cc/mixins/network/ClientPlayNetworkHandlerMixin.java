package brokke.cc.mixins.network;

import brokke.cc.manager.CommandManager;
import brokke.cc.module.misc.ChatHelper;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.EventPopTotem;
import brokke.cc.system.events.impl.PotionEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   @ModifyVariable(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
   private String onModifySendChatMessage(String var1) {
      return ChatHelper.a(var1);
   }

   @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
   private void onSendChatMessage(String var1, CallbackInfo var2) {
      if (var1 != null && CommandManager.c(var1)) {
         var2.cancel();
      }
   }

   @ModifyVariable(method = "sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
   private String onModifySendChatCommand(String var1) {
      return ChatHelper.b(var1);
   }

   @Inject(method = "onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V", at = @At("HEAD"))
   private void onEntityStatus(EntityStatusS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V", at = @At("TAIL"))
   private void onEntityStatusTail(EntityStatusS2CPacket var1, CallbackInfo var2) {
      if (var1.getStatus() == 35 && mc.world != null && var1.getEntity(mc.world) instanceof PlayerEntity var3) {
         EventBus.post(new EventPopTotem(var3));
      }
   }

   @Inject(method = "onGameMessage(Lnet/minecraft/network/packet/s2c/play/GameMessageS2CPacket;)V", at = @At("HEAD"))
   private void onGameMessagePacket(GameMessageS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onChunkDeltaUpdate(Lnet/minecraft/network/packet/s2c/play/ChunkDeltaUpdateS2CPacket;)V", at = @At("HEAD"))
   private void onChunkDeltaUpdatePacket(ChunkDeltaUpdateS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onBlockUpdate(Lnet/minecraft/network/packet/s2c/play/BlockUpdateS2CPacket;)V", at = @At("HEAD"))
   private void onBlockUpdatePacket(BlockUpdateS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onPlaySound(Lnet/minecraft/network/packet/s2c/play/PlaySoundS2CPacket;)V", at = @At("HEAD"))
   private void onPlaySoundPacket(PlaySoundS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onParticle(Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;)V", at = @At("HEAD"))
   private void onParticlePacket(ParticleS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onEntityTrackerUpdate(Lnet/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket;)V", at = @At("HEAD"))
   private void onEntityTrackerUpdatePacket(EntityTrackerUpdateS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onEntityAttributes(Lnet/minecraft/network/packet/s2c/play/EntityAttributesS2CPacket;)V", at = @At("HEAD"))
   private void onEntityAttributesPacket(EntityAttributesS2CPacket var1, CallbackInfo var2) {
      EventBus.post(new EventPacket(var1));
   }

   @Inject(method = "onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V", at = @At("HEAD"), cancellable = true)
   private void onPlayerPositionLookPacket(PlayerPositionLookS2CPacket var1, CallbackInfo var2) {
      EventPacket var3 = new EventPacket(var1);
      EventBus.post(var3);
      if (var3.isCancelled()) {
         var2.cancel();
      }
   }

   @Inject(method = "onEntityVelocityUpdate(Lnet/minecraft/network/packet/s2c/play/EntityVelocityUpdateS2CPacket;)V", at = @At("HEAD"), cancellable = true)
   private void onEntityVelocityUpdatePacket(EntityVelocityUpdateS2CPacket var1, CallbackInfo var2) {
      if (mc.world != null
         && mc.world.getEntityById(var1.getEntityId()) instanceof ItemEntity var4
         && (
            var4.isOnGround()
               || var4.verticalCollision
               || var4.getEntityWorld() != null && !var4.getEntityWorld().isSpaceEmpty(var4, var4.getBoundingBox().offset(0.0, -0.2, 0.0))
         )) {
         Vec3d var5 = var1.getVelocity();
         if (Math.abs(var5.y) < 0.25) {
            var2.cancel();
            return;
         }
      }

      EventPacket var6 = new EventPacket(var1);
      EventBus.post(var6);
      if (var6.isCancelled()) {
         var2.cancel();
      }
   }

   @Inject(method = "onExplosion(Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;)V", at = @At("HEAD"), cancellable = true)
   private void onExplosionPacket(ExplosionS2CPacket var1, CallbackInfo var2) {
      EventPacket var3 = new EventPacket(var1);
      EventBus.post(var3);
      if (var3.isCancelled()) {
         var2.cancel();
      }
   }

   @Inject(method = "onWorldEvent(Lnet/minecraft/network/packet/s2c/play/WorldEventS2CPacket;)V", at = @At("HEAD"))
   private void onWorldEventPacket(WorldEventS2CPacket var1, CallbackInfo var2) {
      if (var1.getEventId() == 2002) {
         EventBus.post(new PotionEvent(PotionEvent.Type.PARTICLES, var1.getData(), var1.getPos()));
      }
   }
}
