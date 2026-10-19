package brokke.cc.mixins.render;

import brokke.cc.module.misc.Optimizer;
import brokke.cc.module.render.ExstraOptimizer;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BillboardParticle.class)
public class BillboardParticleMixin {
   @Inject(method = "render(Lnet/minecraft/client/particle/BillboardParticleSubmittable;Lnet/minecraft/client/render/Camera;F)V", at = @At("HEAD"), cancellable = true)
   private void onRender(BillboardParticleSubmittable var1, Camera var2, float var3, CallbackInfo var4) {
      Particle var5 = (Particle)(Object)this;
      Box var6 = var5.getBoundingBox();
      double var7 = (var6.minX + var6.maxX) * 0.5;
      double var9 = (var6.minY + var6.maxY) * 0.5;
      double var11 = (var6.minZ + var6.maxZ) * 0.5;
      if (Optimizer.a(var7, var9, var11) || ExstraOptimizer.cullParticle(var7, var9, var11)) {
         var4.cancel();
      }
   }
}
