package brokke.cc.mixins.input;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventMoveInput;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MovementInputMixin extends Input {
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void onTickTail(CallbackInfo var1) {
      float var2 = this.movementVector.y;
      float var3 = this.movementVector.x;
      boolean var4 = this.playerInput.jump();
      boolean var5 = this.playerInput.sneak();
      boolean var6 = this.playerInput.sprint();
      EventMoveInput var7 = new EventMoveInput(var2, var3, var4, var5, var6);
      EventBus.post(var7);
      this.movementVector = new Vec2f(var7.getStrafe(), var7.getForward());
      boolean var8 = var7.getForward() > 0.0F;
      boolean var9 = var7.getForward() < 0.0F;
      boolean var10 = var7.getStrafe() > 0.0F;
      boolean var11 = var7.getStrafe() < 0.0F;
      this.playerInput = new PlayerInput(var8, var9, var10, var11, var7.isJump(), var7.isSneak(), var7.isSprint());
   }
}
