package brokke.cc.mixins.render;

import brokke.cc.module.render.Atmosphere;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {
   @Inject(
      method = "getColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;I)I",
      at = @At("RETURN"),
      cancellable = true
   )
   private void onGetColor(BlockState state, BlockRenderView world, BlockPos pos, int tintIndex, CallbackInfoReturnable<Integer> cir) {
      int original = cir.getReturnValue();
      int tinted = Atmosphere.tintBlock(state, pos, original);
      if (tinted != original) {
         cir.setReturnValue(tinted);
      }
   }

   @Inject(
      method = "getParticleColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I",
      at = @At("RETURN"),
      cancellable = true
   )
   private void onGetParticleColor(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
      int original = cir.getReturnValue();
      int tinted = Atmosphere.tintBlock(state, pos, original);
      if (tinted != original) {
         cir.setReturnValue(tinted);
      }
   }
}
