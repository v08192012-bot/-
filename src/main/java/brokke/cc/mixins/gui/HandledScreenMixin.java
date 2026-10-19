package brokke.cc.mixins.gui;

import brokke.cc.core.AutoBuyOverlay;
import brokke.cc.core.Timer2;
import brokke.cc.module.player.AuctionHelper;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.player.ItemScroller;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.impl.EventClickSlot;
import brokke.cc.ui.InventoryButtonsOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
   @Unique
   private Timer2 itemScrollerTimer;
   @Unique
   private int lastScrolledSlotId = -1;
   @Shadow
   private int x;
   @Shadow
   private int y;
   @Shadow
   protected int backgroundWidth;
   @Shadow
   protected int backgroundHeight;

   @Unique
   private Timer2 getItemScrollerTimer() {
      if (this.itemScrollerTimer == null) {
         this.itemScrollerTimer = new Timer2();
      }

      return this.itemScrollerTimer;
   }

   protected HandledScreenMixin(Text var1) {
      super(var1);
   }

   @Shadow
   protected abstract void onMouseClick(Slot var1, int var2, int var3, SlotActionType var4);

   @Shadow
   @Override
   public abstract T getScreenHandler();

   @Shadow
   protected abstract void drawBackground(DrawContext var1, float var2, int var3, int var4);

   @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
   private void onSlotClick(Slot var1, int var2, int var3, SlotActionType var4, CallbackInfo var5) {
      if (var1 != null) {
         try {
            ScreenHandler var6 = this.getScreenHandler();
            int var7 = var6 != null ? var6.syncId : 0;
            EventClickSlot var8 = new EventClickSlot(var7, var2, var3, var4);
            EventBus.post(var8);
            if (var8.isCancelled()) {
               var5.cancel();
            }
         } catch (Exception var9) {
         }
      }
   }

   @Inject(method = "mouseDragged(Lnet/minecraft/client/gui/Click;DD)Z", at = @At("HEAD"), cancellable = true)
   private void onItemScrollerDrag(Click var1, double var2, double var4, CallbackInfoReturnable<Boolean> var6) {
      if (this.isItemScrollerActive() && ItemScroller.scrollEnabled.getValue() && var1.button() == 0 && this.isShiftKeyDown()) {
         Slot var7 = this.findSlotAt(var1.x(), var1.y());
         if (var7 != null && var7.id != this.lastScrolledSlotId && var7.hasStack()) {
            this.lastScrolledSlotId = var7.id;
            long var8 = ItemScroller.k();
            if (this.getItemScrollerTimer().a(var8)) {
               this.getItemScrollerTimer().a();
               this.onMouseClick(var7, var7.id, 0, SlotActionType.QUICK_MOVE);
            }
         }

         var6.setReturnValue(true);
      }
   }

   @Inject(method = "mouseReleased(Lnet/minecraft/client/gui/Click;)Z", at = @At("HEAD"))
   private void onItemScrollerRelease(Click var1, CallbackInfoReturnable<Boolean> var2) {
      this.lastScrolledSlotId = -1;
   }

   @Unique
   private boolean isItemScrollerActive() {
      ItemScroller var1 = FunctionManager.a(ItemScroller.class);
      return var1 != null && var1.isActive();
   }

   @Unique
   private boolean isShiftKeyDown() {
      Window var1 = this.client != null ? this.client.getWindow() : null;
      return var1 == null ? false : InputUtil.isKeyPressed(var1, 340) || InputUtil.isKeyPressed(var1, 344);
   }

   @Unique
   @Nullable
   private Slot findSlotAt(double var1, double var3) {
      ScreenHandler var5 = this.getScreenHandler();
      if (var5 == null) {
         return null;
      }

      for (Slot var7 : var5.slots) {
         int var8 = this.x + var7.x;
         int var9 = this.y + var7.y;
         if (var1 >= var8 && var1 < var8 + 16 && var3 >= var9 && var3 < var9 + 16) {
            return var7;
         }
      }

      return null;
   }

   @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true)
   private void onHandledMouseClicked(Click var1, boolean var2, CallbackInfoReturnable<Boolean> var3) {
      if (InventoryButtonsOverlay.a((HandledScreen<?>)(Object)this)
         && InventoryButtonsOverlay.a(
            (HandledScreen<?>)(Object)this, var1.x(), var1.y(), var1.button(), this.x, this.y, this.backgroundWidth, this.backgroundHeight
         )) {
         var3.setReturnValue(true);
      } else if (AutoBuyOverlay.a((HandledScreen<?>)(Object)this)
         && AutoBuyOverlay.a(var1.x(), var1.y(), var1.button(), this.x, this.y, this.backgroundWidth, this.backgroundHeight)) {
         var3.setReturnValue(true);
      }
   }

   @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
   private void onRenderBackground(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      this.drawBackground(var1, var4, var2, var3);
      var5.cancel();
   }

   @Inject(method = "renderMain(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"))
   private void onRenderAuctionHighlights(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      AuctionHelper var6 = FunctionManager.a(AuctionHelper.class);
      if (var6 != null && var6.isActive()) {
         var6.a(var1, (HandledScreen<?>)(Object)this, this.x, this.y);
      }

      if (AutoBuyOverlay.a((HandledScreen<?>)(Object)this)) {
         AutoBuyOverlay.a((HandledScreen<?>)(Object)this, this.x, this.y, this.backgroundWidth, this.backgroundHeight, var2, var3);
      }

      if (InventoryButtonsOverlay.a((HandledScreen<?>)(Object)this)) {
         InventoryButtonsOverlay.a((HandledScreen<?>)(Object)this, this.x, this.y, this.backgroundWidth, this.backgroundHeight, var2, var3);
      }
   }
}
