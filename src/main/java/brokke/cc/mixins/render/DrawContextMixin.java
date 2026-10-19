package brokke.cc.mixins.render;

import brokke.cc.module.player.AuctionHelper;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.player.StreamerMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextMixin {
   @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private Text light$protectDrawText(Text var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private String light$protectDrawTextString(String var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private Text light$protectDrawTextShadow(Text var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private String light$protectDrawTextShadowString(String var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private Text light$protectCenteredText(Text var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private String light$protectCenteredTextString(String var1) {
      return StreamerMode.a(var1);
   }

   @ModifyVariable(
      method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private String light$modifyStackOverlayCount(String var1, TextRenderer var2, ItemStack var3, int var4, int var5) {
      if (var1 != null) {
         return var1;
      }

      AuctionHelper var6 = FunctionManager.a(AuctionHelper.class);
      return var6 != null ? var6.a(var3) : null;
   }
}
