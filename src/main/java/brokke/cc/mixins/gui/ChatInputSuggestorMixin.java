package brokke.cc.mixins.gui;

import brokke.cc.manager.CommandManager;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   boolean completingSuggestions;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionWindow window;
   @Shadow
   @Final
   private List<OrderedText> messages;

   @Shadow
   public abstract void show(boolean var1);

   @Inject(method = "refresh()V", at = @At("HEAD"), cancellable = true)
   private void onRefresh(CallbackInfo var1) {
      String var2 = this.textField.getText();
      if (var2.startsWith(".")) {
         if (!this.completingSuggestions) {
            this.textField.setSuggestion(null);
            this.window = null;
         }

         this.messages.clear();
         int var3 = this.textField.getCursor();
         if (var3 >= 1 && (this.window == null || !this.completingSuggestions)) {
            CommandManager.a var4 = CommandManager.a(var2, var3);
            if (var4.getCandidates().isEmpty()) {
               this.pendingSuggestions = null;
            } else {
               SuggestionsBuilder var5 = new SuggestionsBuilder(var2.substring(0, var3), var4.getStart());

               for (String var7 : var4.getCandidates()) {
                  var5.suggest(var7);
               }

               this.pendingSuggestions = var5.buildFuture();
               this.pendingSuggestions.thenRun(() -> {
                  if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
                     this.show(false);
                  }
               });
            }
         } else if (var3 < 1) {
            this.pendingSuggestions = null;
         }

         var1.cancel();
      }
   }
}
