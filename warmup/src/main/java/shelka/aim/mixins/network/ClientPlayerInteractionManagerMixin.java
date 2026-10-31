package shelka.aim.mixins.network;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shelka.aim.core.WarmupBotEntity;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
   @Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void shelkaAimOnAttackEntity(PlayerEntity attacker, Entity target, CallbackInfo ci) {
      if (target instanceof WarmupBotEntity bot) {
         bot.onHit(attacker);
         // Бот существует только на клиенте: на сервере его entity id нет. Если пакет
         // атаки уйдёт, сервер получит удар по несуществующей сущности — это BadPackets,
         // то есть флаг на КАЖДОМ ударе по боту. Гасим пакет: удар остаётся локальным.
         ci.cancel();
      }
   }
}
