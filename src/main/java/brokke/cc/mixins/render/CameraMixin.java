package brokke.cc.mixins.render;

import brokke.cc.module.movement.FreeCam;
import brokke.cc.module.render.FreeLook;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.NoRender;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventRotation;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   private boolean thirdPerson;

   @Shadow
   protected abstract void setPos(double var1, double var3, double var5);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Inject(
      method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = Shift.AFTER),
      cancellable = true
   )
   public void onUpdate(World var1, Entity var2, boolean var3, boolean var4, float var5, CallbackInfo var6) {
      FreeCam var7 = FunctionManager.a(FreeCam.class);
      if (var7 != null && var7.isActive()) {
         this.setRotation(var7.k(), var7.l());
         this.setPos(var7.a(var5), var7.b(var5), var7.c(var5));
         this.thirdPerson = true;
         var6.cancel();
      } else {
         FreeLook var8 = FunctionManager.a(FreeLook.class);
         if (var8 != null && var8.k() && var2 instanceof ClientPlayerEntity var9 && !var9.isSleeping()) {
            float var10 = var4 ? -var8.getCameraPitch() : var8.getCameraPitch();
            float var11 = var8.getCameraYaw() - (var4 ? 180.0F : 0.0F);
            float var12 = var3 ? 4.0F : 0.0F;
            this.setRotation(var11, var10);
            this.moveBy(-this.clipToSpace(var12), 0.0F, 0.0F);
            var6.cancel();
         }
      }
   }

   @Redirect(
      method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V")
   )
   private void redirectSetRotation(Camera var1, float var2, float var3, World var4, Entity var5, boolean var6, boolean var7, float var8) {
      EventRotation var9 = new EventRotation(var2, var3, var8);
      EventBus.post(var9);
      float var10 = var9.getYaw();
      float var11 = var9.getPitch();
      if (var6 && var7) {
         var10 += 180.0F;
         var11 = -var11;
      }

      ((ICameraMixin)var1).setCustomRotation(var10, var11);
   }

   @Redirect(
      method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F")
   )
   private float redirectClipToSpace(Camera var1, float var2, World var3, Entity var4, boolean var5, boolean var6, float var7) {
      return (FunctionManager.a(FreeCam.class) == null || !FunctionManager.a(FreeCam.class).isActive()) && !NoRender.n()
         ? ((ICameraMixin)var1).setClipToSpace(var2)
         : var2;
   }

   @Redirect(
      method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V")
   )
   private void redirectMoveBy(Camera var1, float var2, float var3, float var4, World var5, Entity var6, boolean var7, boolean var8, float var9) {
      ((ICameraMixin)var1).setCustomMoveBy(var2, var3, var4);
   }
}
