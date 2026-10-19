package brokke.cc.mixins.render;

import brokke.cc.hud.HotbarHud;
import brokke.cc.hud.ScoreboardHud;
import brokke.cc.module.render.Beautifully;
import brokke.cc.module.render.NoRender;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.HudRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
   private void onRender(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      EventBus.post(new HudRenderEvent(var1));
   }

   @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void renderCrosshair(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (MinecraftClient.getInstance().currentScreen != null) {
         var3.cancel();
      }
   }

   @Inject(method = "renderVignetteOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderVignetteOverlay(DrawContext var1, Entity var2, CallbackInfo var3) {
      if (NoRender.o()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (NoRender.p()) {
         MinecraftClient var4 = MinecraftClient.getInstance();
         if (var4.player != null) {
            for (StatusEffectInstance var6 : var4.player.getStatusEffects()) {
               if (!var6.getEffectType().value().isBeneficial()) {
                  var3.cancel();
                  return;
               }
            }
         }
      }
   }

   @Inject(method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void cancelVanillaHotbar(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (HotbarHud.isEnabled()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
   private void cancelVanillaScoreboard(DrawContext var1, ScoreboardObjective var2, CallbackInfo var3) {
      if (ScoreboardHud.isEnabled()) {
         var3.cancel();
      }
   }

   @Inject(method = "renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V", at = @At("TAIL"))
   private void renderFoodSaturation(DrawContext var1, PlayerEntity var2, int var3, int var4, CallbackInfo var5) {
      if (Beautifully.k()) {
         HungerManager var6 = var2.getHungerManager();
         float var7 = var6.getSaturationLevel();
         int var8 = var3 - 10;
         boolean var9 = var2.hasStatusEffect(StatusEffects.HUNGER);
         Identifier var10 = Identifier.ofVanilla(var9 ? "hud/food_empty_hunger" : "hud/food_empty");
         Identifier var11 = Identifier.ofVanilla(var9 ? "hud/food_half_hunger" : "hud/food_half");
         Identifier var12 = Identifier.ofVanilla(var9 ? "hud/food_full_hunger" : "hud/food_full");

         for (int var13 = 0; var13 < 10; var13++) {
            int var14 = var4 - var13 * 8 - 9;
            var1.drawGuiTexture(RenderPipelines.GUI_TEXTURED, var10, var14, var8, 9, 9);
            float var15 = var7 - var13 * 2;
            if (var15 >= 2.0F) {
               var1.drawGuiTexture(RenderPipelines.GUI_TEXTURED, var12, var14, var8, 9, 9);
            } else if (var15 >= 1.0F) {
               var1.drawGuiTexture(RenderPipelines.GUI_TEXTURED, var11, var14, var8, 9, 9);
            }
         }
      }
   }
}
