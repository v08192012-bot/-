package brokke.cc.mixins.gui;

import brokke.cc.ui.InventoryButtonsOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin<T extends AbstractRecipeScreenHandler> extends HandledScreen<T> {
   protected RecipeBookScreenMixin(T var1, PlayerInventory var2, Text var3) {
      super(var1, var2, var3);
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"))
   private void onRecipeBookRender(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      if (InventoryButtonsOverlay.a(this)) {
         InventoryButtonsOverlay.a(this, this.x, this.y, this.backgroundWidth, this.backgroundHeight, var2, var3);
      }
   }

   @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true)
   private void onRecipeBookMouseClicked(Click var1, boolean var2, CallbackInfoReturnable<Boolean> var3) {
      if (InventoryButtonsOverlay.a(this)
         && InventoryButtonsOverlay.a(this, var1.x(), var1.y(), var1.button(), this.x, this.y, this.backgroundWidth, this.backgroundHeight)) {
         var3.setReturnValue(true);
      }
   }
}
