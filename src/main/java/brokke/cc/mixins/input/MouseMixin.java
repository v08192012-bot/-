package brokke.cc.mixins.input;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.core.SilentView;
import brokke.cc.module.combat.Aura;
import brokke.cc.module.render.FreeLook;
import brokke.cc.module.FunctionManager;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventLook;
import brokke.cc.system.events.impl.EventScroll;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.util.GCDUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
   @Shadow
   private double cursorDeltaX;
   @Shadow
   private double cursorDeltaY;
   @Unique
   private static final double NO_POS = Double.MIN_VALUE;
   @Unique
   private double lastLookX = Double.MIN_VALUE;
   @Unique
   private double lastLookY;

   @Inject(method = "updateMouse(D)V", at = @At("HEAD"))
   private void onUpdateMouseHead(double var1, CallbackInfo var3) {
      FreeLook var4 = FunctionManager.a(FreeLook.class);
      if (var4 != null && var4.k()) {
         var4.a((float)this.cursorDeltaX, (float)this.cursorDeltaY);
         this.cursorDeltaX = 0.0;
         this.cursorDeltaY = 0.0;
      } else {
         MinecraftClient var5 = MinecraftClient.getInstance();
         Aura var6 = Aura.INSTANCE;
         // «Наводка от первого лица» выключена (или SkyTime со свободной камерой):
         // аура крутит игрока для сервера, а мышь двигает свободную клиентскую камеру.
         if (var6 != null && var6.l() && FreeLookStorage.isActive() && var5.currentScreen == null) {
            float var7 = GCDUtil.a();
            FreeLookStorage.setFreeYaw(FreeLookStorage.getFreeYaw() + (float)this.cursorDeltaX * var7);
            FreeLookStorage.setFreePitch(MathHelper.clamp(FreeLookStorage.getFreePitch() + (float)this.cursorDeltaY * var7, -90.0F, 90.0F));
            this.cursorDeltaX = 0.0;
            this.cursorDeltaY = 0.0;
         }
      }
   }

   @Inject(method = "onCursorPos(JDD)V", at = @At("TAIL"))
   private void onCursorPosTail(long var1, double var3, double var5, CallbackInfo var7) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      if (var8.getWindow().getHandle() != var1 || !var8.isWindowFocused()) {
         this.lastLookX = Double.MIN_VALUE;
      } else if (this.lastLookX == Double.MIN_VALUE) {
         this.lastLookX = var3;
         this.lastLookY = var5;
      } else {
         this.lastLookX = var3;
         this.lastLookY = var5;
      }
   }

   @Redirect(method = "updateMouse(D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
   private void redirectLookDirection(ClientPlayerEntity var1, double var2, double var4) {
      EventLook var6 = new EventLook(var2, var4);
      EventBus.post(var6);
      if (!var6.isCancelled()) {
         var2 = var6.getYaw();
         var4 = var6.getPitch();
         Aura var7 = Aura.INSTANCE;
         if (var7 != null && var7.l() && var7.rotationType.isSelected("Legit")) {
            double[] var8 = var7.getLegitRotation().a(var1, var2, var4);
            var1.changeLookDirection(var8[0], var8[1]);
         } else if (!SilentView.getInstance().a(var2, var4)) {
            var1.changeLookDirection(var2, var4);
         }
      }
   }

   @Inject(method = "onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V", at = @At("HEAD"))
   private void onMouseButton(long var1, MouseInput var3, int var4, CallbackInfo var5) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      if (var6.getWindow().getHandle() == var1) {
         EventBus.post(new KeyEvent(var4, -(var3.button() + 100)));
      }
   }

   @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
   private void onMouseScroll(long var1, double var3, double var5, CallbackInfo var7) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      if (var8.getWindow().getHandle() == var1) {
         EventScroll var9 = new EventScroll(var3, var5);
         EventBus.post(var9);
         if (var9.isCancelled()) {
            var7.cancel();
         }
      }
   }
}
