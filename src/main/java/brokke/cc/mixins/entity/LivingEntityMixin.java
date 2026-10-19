package brokke.cc.mixins.entity;

import brokke.cc.module.movement.NoPush;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EntityJumpEvent;
import brokke.cc.system.events.impl.EventMoveVelocity;
import brokke.cc.system.events.impl.TravelRotationEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @Inject(method = "isPushable()Z", at = @At("HEAD"), cancellable = true)
   public void isPushable(CallbackInfoReturnable<Boolean> var1) {
      if (NoPush.Instance.isActive() && NoPush.players.isToggled()) {
         var1.setReturnValue(false);
      }
   }

   @Inject(method = "jump()V", at = @At("HEAD"))
   public void onJump(CallbackInfo var1) {
      EventBus.post(new EntityJumpEvent((LivingEntity)(Object)this));
   }

   @Redirect(
      method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")
   )
   private Vec3d redirectGetRotationVector(LivingEntity var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player == null) {
         return var1.getRotationVector();
      }

      if ((Object)this instanceof ClientPlayerEntity) {
         TravelRotationEvent var3 = new TravelRotationEvent((LivingEntity)(Object)this, var1.getPitch(), var1.getYaw());
         EventBus.post(var3);
         if (var3.isCancelled()) {
            float var4 = var3.getPitch() * (float) (Math.PI / 180.0);
            float var5 = -var3.getYaw() * (float) (Math.PI / 180.0);
            float var6 = MathHelper.cos(var5);
            float var7 = MathHelper.sin(var5);
            float var8 = MathHelper.cos(var4);
            float var9 = MathHelper.sin(var4);
            return new Vec3d(var7 * var8, -var9, var6 * var8);
         }
      }

      return var1.getRotationVector();
   }

   @Redirect(
      method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F")
   )
   private float redirectGetPitch(LivingEntity var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player == null) {
         return var1.getPitch();
      }

      if (!((Object)this instanceof ClientPlayerEntity)) {
         return var1.getPitch();
      }

      TravelRotationEvent var3 = TravelRotationEvent.getLastRotationEvent();
      return var3 != null && var3.isCancelled() ? var3.getPitch() : var1.getPitch();
   }

   @Inject(method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At("RETURN"), cancellable = true)
   private void onCalcGlidingVelocity(Vec3d var1, CallbackInfoReturnable<Vec3d> var2) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player != null && (Object)this instanceof ClientPlayerEntity) {
         EventMoveVelocity var4 = new EventMoveVelocity((Vec3d)var2.getReturnValue());
         EventBus.post(var4);
         var2.setReturnValue(var4.getMovement());
      }
   }
}
