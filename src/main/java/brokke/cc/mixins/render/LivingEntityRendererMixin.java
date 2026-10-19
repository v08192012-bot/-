package brokke.cc.mixins.render;

import brokke.cc.module.render.NameTags;
import brokke.cc.module.render.SeeInvisibles;
import brokke.cc.render.Theme;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private boolean brokke$seeInvisiblesRendering;

   @Shadow
   public abstract Identifier getTexture(S var1);

   @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
   private void onHasLabel(LivingEntity var1, double var2, CallbackInfoReturnable<Boolean> var4) {
      if (NameTags.a(var1)) {
         var4.setReturnValue(false);
      }
   }

   @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
   private void onUpdateRenderStateSeeInvisibles(T var1, S var2, float var3, CallbackInfo var4) {
      SeeInvisibles var5 = SeeInvisibles.k();
      if (var5 != null && var5.isActive() && var2.invisible && var1 instanceof PlayerEntity && (Object)this instanceof PlayerEntityRenderer) {
         var2.invisibleToPlayer = false;
      }
   }

   @Inject(method = "isVisible(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)Z", at = @At("RETURN"), cancellable = true)
   private void modifyVisibility(S var1, CallbackInfoReturnable<Boolean> var2) {
      SeeInvisibles var3 = SeeInvisibles.k();
      if (var3 != null && var3.isActive() && var1.invisible && (Object)this instanceof PlayerEntityRenderer) {
         var2.setReturnValue(true);
      }
   }

   @Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;", at = @At("RETURN"), cancellable = true)
   private void modifyRenderLayer(S var1, boolean var2, boolean var3, boolean var4, CallbackInfoReturnable<RenderLayer> var5) {
      SeeInvisibles var6 = SeeInvisibles.k();
      if (var6 != null && var6.isActive() && var1.invisible && (Object)this instanceof PlayerEntityRenderer) {
         Identifier var7 = this.getTexture(var1);
         var5.setReturnValue(RenderLayers.entityTranslucent(var7, true));
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD")
   )
   private void onRenderHead(S var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5) {
      SeeInvisibles var6 = SeeInvisibles.k();
      this.brokke$seeInvisiblesRendering = var6 != null && var6.isActive() && var1.invisible && (Object)this instanceof PlayerEntityRenderer;
   }

   @Redirect(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ColorHelper;mix(II)I")
   )
   private int brokke$modifySeeInvisiblesAlpha(int var1, int var2) {
      int var3 = ColorHelper.mix(var1, var2);
      if (this.brokke$seeInvisiblesRendering) {
         SeeInvisibles var4 = SeeInvisibles.k();
         if (var4 != null && var4.isActive()) {
            float var5 = (float)var4.getOpacity().getValue();
            int var6;
            if (var4.getColorMode().isMode("От темы")) {
               var6 = Theme.getInstance().getAccent().getRGB();
            } else {
               var6 = var4.getColor().getArgb();
            }

            int var7 = var6 >> 24 & 0xFF;
            if (var7 == 0) {
               var7 = 255;
            }

            int var8 = Math.round(var7 * var5) << 24;
            return var6 & 16777215 | var8;
         } else {
            return var3;
         }
      } else {
         return var3;
      }
   }
}
