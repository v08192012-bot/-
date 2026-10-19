package brokke.cc.mixins.render;

import brokke.cc.core.AutoBuyOverlay;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.render.Beautifully;
import brokke.cc.module.render.CameraTweaks;
import brokke.cc.module.render.CustomFog;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.NoRender;
import brokke.cc.module.render.WetWorld;
import brokke.cc.render.BlurPipeline;
import brokke.cc.render.ColorGradingRenderer;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.LiquidGlassPipeline;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventGameUpdate;
import brokke.cc.system.events.impl.HandEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.InventoryButtonsOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Shadow
   private MinecraftClient client;
   @Shadow
   private FogRenderer fogRenderer;
   @Shadow
   private GuiRenderer guiRenderer;
   @Shadow
   GuiRenderState guiState;
   @Unique
   private long lastHookTime = Util.getMeasuringTimeNano();
   @Unique
   private int accumulatedCalls = 0;
   @Unique
   private static CameraTweaks cachedCameraTweaks;

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.AFTER
      )
   )
   private void onAfterWorldRender(RenderTickCounter var1, CallbackInfo var2) {
      WetWorld.m();
      CustomFog.p();
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
         shift = Shift.AFTER
      )
   )
   private void onAfterHandDepthClear(RenderTickCounter var1, CallbackInfo var2) {
      HandEvent.PRE.setCancelled(false);
      EventBus.post(HandEvent.PRE);
   }

   @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V", at = @At("HEAD"))
   private void onRenderHead(RenderTickCounter var1, boolean var2, CallbackInfo var3) {
      GuiBatch.beginFrame();
      BlurPipeline.b();
      LiquidGlassPipeline.b();
      long var4 = Util.getMeasuringTimeNano();
      long var6 = var4 - this.lastHookTime;
      this.accumulatedCalls += (int)(var6 / 4166666L);
      this.lastHookTime = this.lastHookTime + this.accumulatedCalls * 4166666L;

      for (this.accumulatedCalls = Math.min(this.accumulatedCalls, 8); this.accumulatedCalls > 0; this.accumulatedCalls--) {
         EventGameUpdate.INSTANCE.setCancelled(false);
         EventBus.post(EventGameUpdate.INSTANCE);
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
   )
   private void onRenderBeforeGui(RenderTickCounter var1, boolean var2, CallbackInfo var3) {
      AutoBuyOverlay.a(this.client, this.guiState, this.guiRenderer, this.fogRenderer);
      InventoryButtonsOverlay.a(this.client);
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void onRenderWorld(RenderTickCounter var1, CallbackInfo var2) {
      if (Beautifully.l()) {
         ColorGradingRenderer.a(Beautifully.m(), Beautifully.n());
      }

      HandEvent.POST.setCancelled(false);
      EventBus.post(HandEvent.POST);
      MsdfManager.a();
      GuiBatch.begin();

      try {
         EventBus.post(Render2DEvent.get(var1));
      } finally {
         GuiBatch.end();
      }
   }

   @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
   private void onTiltViewWhenHurt(MatrixStack var1, float var2, CallbackInfo var3) {
      if (NoRender.v()) {
         var3.cancel();
      }
   }

   @Inject(method = "showFloatingItem(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private void onShowFloatingItem(ItemStack var1, CallbackInfo var2) {
      if (NoRender.u()) {
         var2.cancel();
      }
   }

   @Unique
   private static CameraTweaks getCameraTweaks() {
      if (cachedCameraTweaks == null) {
         cachedCameraTweaks = FunctionManager.a(CameraTweaks.class);
      }

      return cachedCameraTweaks;
   }

   @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
   private void onGetFov(Camera var1, float var2, boolean var3, CallbackInfoReturnable<Float> var4) {
      CameraTweaks var5 = getCameraTweaks();
      if (var5 != null && var5.isActive()) {
         Float var6 = var5.a((Float)var4.getReturnValue(), var2, var3);
         if (var6 != null) {
            var4.setReturnValue(var6);
         }
      }
   }

   @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("RETURN"), cancellable = true)
   private void onGetProjectionMatrix(float var1, CallbackInfoReturnable<Matrix4f> var2) {
      CameraTweaks var3 = getCameraTweaks();
      if (var3 != null && var3.isActive() && var3.k()) {
         float var4 = var3.l();
         var2.setReturnValue(new Matrix4f((Matrix4fc)var2.getReturnValue()).scale(var4, 1.0F, 1.0F));
      }
   }
}
