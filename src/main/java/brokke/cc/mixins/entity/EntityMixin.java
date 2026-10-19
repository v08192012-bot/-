package brokke.cc.mixins.entity;

import brokke.cc.module.movement.NoPush;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
   private static final double PLAYER_RENDER_DISTANCE = 192.0;

   @Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true)
   private void onShouldRender(double var1, CallbackInfoReturnable<Boolean> var3) {
      if ((Object)this instanceof PlayerEntity && var1 < PLAYER_RENDER_DISTANCE * PLAYER_RENDER_DISTANCE) {
         var3.setReturnValue(true);
      }
   }

   @Redirect(
      method = "updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"
      )
   )
   private Vec3d stopWaterVelocity(FluidState var1, BlockView var2, BlockPos var3) {
      return NoPush.Instance.isActive() && NoPush.water.isToggled() ? Vec3d.ZERO : var1.getVelocity(var2, var3);
   }
}
