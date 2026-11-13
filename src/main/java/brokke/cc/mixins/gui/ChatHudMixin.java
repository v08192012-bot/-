package brokke.cc.mixins.gui;

import brokke.cc.manager.NicknameManager;
import brokke.cc.module.render.CosmeticSync;
import brokke.cc.module.misc.ChatHelper;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
   @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), argsOnly = true)
   private Text brokkeModifyAddMessage(Text var1) {
      ChatHelper.a(var1);
      return applyNicknames(var1);
   }

   @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
   private Text brokkeModifyAddMessageSimple(Text var1) {
      ChatHelper.a(var1);
      return applyNicknames(var1);
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void brokkeCosmeticBeacon(Text var1, CallbackInfo var2) {
      if (CosmeticSync.handleChatMessage(var1)) {
         var2.cancel();
      }
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void brokkeCosmeticBeaconSigned(Text var1, net.minecraft.network.message.MessageSignatureData var2, net.minecraft.client.gui.hud.MessageIndicator var3, CallbackInfo var4) {
      if (CosmeticSync.handleChatMessage(var1)) {
         var4.cancel();
      }
   }

   private static Text applyNicknames(Text var0) {
      Map var1 = NicknameManager.b();
      if (var1.isEmpty()) {
         return var0;
      }

      MutableText var2 = Text.empty();
      var0.visit((var2xx, var3) -> {
         String var4 = var3;

         for (Entry var6 : (Iterable<Entry>)(Iterable<?>)(var1.entrySet())) {
            var4 = var4.replaceAll("(?i)\\b" + Pattern.quote((String)var6.getKey()) + "\\b", (String)var6.getValue());
         }

         var2.append(Text.literal(var4).setStyle(var2xx));
         return Optional.empty();
      }, Style.EMPTY);
      return var2;
   }
}
