package brokke.cc.mixins.render;

import brokke.cc.module.render.NameTags;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
   @Inject(method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
   private void onHasLabel(Entity var1, double var2, CallbackInfoReturnable<Boolean> var4) {
      if (NameTags.a(var1)) {
         var4.setReturnValue(false);
      }
   }
}
