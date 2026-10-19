package brokke.cc.mixins.input;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.combat.Aura;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventMoveInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
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
      float strafe = var7.getStrafe();
      float forward = var7.getForward();

      // Свободная камера (аура крутит игрока серверно): WASD должны идти относительно
      // того, куда смотрит камера, а не относительно серверной ротации.
      // Ваниль повернёт ввод на yaw игрока — значит, заранее доворачиваем на разницу.
      MinecraftClient var12 = MinecraftClient.getInstance();
      Aura var13 = Aura.INSTANCE;
      if (var12.player != null && var13 != null && var13.l() && FreeLookStorage.isActive()) {
         float delta = MathHelper.wrapDegrees(FreeLookStorage.getFreeYaw() - var12.player.getYaw());
         if (Math.abs(delta) > 0.01F) {
            float rad = delta * (float) (Math.PI / 180.0);
            float sin = MathHelper.sin(rad);
            float cos = MathHelper.cos(rad);
            float x = strafe * cos - forward * sin;
            float z = forward * cos + strafe * sin;
            strafe = x;
            forward = z;
         }
      }

      this.movementVector = new Vec2f(strafe, forward);
      boolean var8 = forward > 0.0F;
      boolean var9 = forward < 0.0F;
      boolean var10 = strafe > 0.0F;
      boolean var11 = strafe < 0.0F;
      this.playerInput = new PlayerInput(var8, var9, var10, var11, var7.isJump(), var7.isSneak(), var7.isSprint());
   }
}
