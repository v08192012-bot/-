package brokke.cc.mixins.render;

import brokke.cc.cosmetic.CosmeticFeatureRenderer;
import brokke.cc.module.render.NameTags;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<PlayerLikeEntity, PlayerEntityRenderState, PlayerEntityModel> {
   public PlayerEntityRendererMixin(Context var1, PlayerEntityModel var2, float var3) {
      super(var1, var2, var3);
   }

   @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("RETURN"))
   private void onInit(Context var1, boolean var2, CallbackInfo var3) {
      this.addFeature(new CosmeticFeatureRenderer(this));
   }

   @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
   private void onHasLabel(PlayerLikeEntity var1, double var2, CallbackInfoReturnable<Boolean> var4) {
      if (NameTags.a(var1)) {
         var4.setReturnValue(false);
      }
   }
}
