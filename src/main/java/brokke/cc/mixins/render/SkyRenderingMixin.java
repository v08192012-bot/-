package brokke.cc.mixins.render;

import brokke.cc.module.render.Ambience;
import brokke.cc.module.render.ExstraOptimizer;
import brokke.cc.module.FunctionManager;
import brokke.cc.render.CustomSkyRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
   @Inject(method = "updateRenderState(Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/state/SkyRenderState;)V", at = @At("TAIL"))
   private void onUpdateRenderState(ClientWorld var1, float var2, Camera var3, SkyRenderState var4, CallbackInfo var5) {
      Ambience var6 = FunctionManager.a(Ambience.class);
      if (var6 != null && var6.isActive()) {
         if (var6.getSkyMode().isSelected("Энд")) {
            var4.skybox = Skybox.END;
         } else if (var6.k() && var4.skybox == Skybox.NONE) {
            var4.skybox = Skybox.OVERWORLD;
         }
      }
   }

   @Inject(method = "renderTopSky(I)V", at = @At("HEAD"), cancellable = true)
   private void onRenderTopSky(int var1, CallbackInfo var2) {
      Ambience var3 = FunctionManager.a(Ambience.class);
      if (var3 != null && var3.isActive() && var3.k()) {
         CustomSkyRenderer.getInstance().a(var3);
         var2.cancel();
      }
   }

   @Inject(method = "renderEndSky()V", at = @At("HEAD"), cancellable = true)
   private void onRenderEndSky(CallbackInfo var1) {
      Ambience var2 = FunctionManager.a(Ambience.class);
      if (var2 != null && var2.isActive() && var2.k()) {
         CustomSkyRenderer.getInstance().a(var2);
         var1.cancel();
      }
   }

   @Inject(method = "renderSkyDark()V", at = @At("HEAD"), cancellable = true)
   private void onRenderSkyDark(CallbackInfo var1) {
      Ambience var2 = FunctionManager.a(Ambience.class);
      if (var2 != null && var2.isActive() && var2.k()) {
         var1.cancel();
      }
   }

   @Inject(method = "renderGlowingSky(Lnet/minecraft/client/util/math/MatrixStack;FI)V", at = @At("HEAD"), cancellable = true)
   private void onRenderGlowingSky(MatrixStack var1, float var2, int var3, CallbackInfo var4) {
      Ambience var5 = FunctionManager.a(Ambience.class);
      if (var5 != null && var5.isActive() && var5.k()) {
         var4.cancel();
      }
   }

   @Inject(method = "renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;FFFLnet/minecraft/world/MoonPhase;FF)V", at = @At("HEAD"), cancellable = true)
   private void onRenderCelestialBodies(MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7, CallbackInfo var8) {
      if (ExstraOptimizer.sky()) {
         var8.cancel();
         return;
      }

      Ambience var9 = FunctionManager.a(Ambience.class);
      if (var9 != null && var9.isActive() && var9.k()) {
         var8.cancel();
      }
   }
}
