package brokke.cc.mixins.gui;

import brokke.cc.config.HudConfig;
import brokke.cc.module.misc.ChatHelper;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.ViewModel;
import brokke.cc.render.RenderUtil;
import brokke.cc.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Arm;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
   @Shadow
   protected TextFieldWidget chatField;
   @Unique
   private boolean vmDragging = false;
   @Unique
   private boolean vmWasLmbPressed = false;
   @Unique
   private Arm vmArm = Arm.RIGHT;
   @Unique
   private int vmLastMouseX = 0;
   @Unique
   private int vmLastMouseY = 0;

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
   private void onRender(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      HudConfig.a(var1);
      ChatHelper.a(var1, this.chatField);
      MinecraftClient var6 = MinecraftClient.getInstance();
      ViewModel var7 = FunctionManager.a(ViewModel.class);
      if (var7 != null && var7.isActive()) {
         boolean var8 = var6.player == null || var6.player.getMainArm() == Arm.RIGHT;
         NumberSetting var9 = var8 ? var7.getMainX() : var7.getOffX();
         NumberSetting var10 = var8 ? var7.getMainY() : var7.getOffY();
         NumberSetting var11 = var8 ? var7.getOffX() : var7.getMainX();
         NumberSetting var12 = var8 ? var7.getOffY() : var7.getMainY();
         int var13 = var6.getWindow().getScaledWidth();
         int var14 = var6.getWindow().getScaledHeight();
         long var15 = var6.getWindow().getHandle();
         boolean var17 = GLFW.glfwGetMouseButton(var15, 0) == 1;
         boolean var18 = var17 && !this.vmWasLmbPressed;
         this.vmWasLmbPressed = var17;
         float var19 = var2 * RenderUtil.e();
         float var20 = var3 * RenderUtil.e();
         boolean var21 = HudConfig.b(var19, var20) || HudConfig.c(var19, var20);
         float var22 = var13 * 0.35F;
         float var23 = var14 * 0.35F;
         float var24 = var13 * 0.35F;
         float var25 = var14 * 0.35F;
         float var26 = (float)var7.getRightScale().getValue();
         float var27 = var13 * 0.7F + (float)var9.getValue() * var22;
         float var28 = var14 * 0.58F - (float)var10.getValue() * var23;
         float var29 = var13 * 0.3F * var26;
         float var30 = var14 * 0.48F * var26;
         boolean var31 = Math.abs(var2 - var27) <= var29 * 0.5F && Math.abs(var3 - var28) <= var30 * 0.5F;
         float var32 = (float)var7.getLeftScale().getValue();
         float var33 = var13 * 0.3F + (float)var11.getValue() * var24;
         float var34 = var14 * 0.58F - (float)var12.getValue() * var25;
         float var35 = var13 * 0.3F * var32;
         float var36 = var14 * 0.48F * var32;
         boolean var37 = Math.abs(var2 - var33) <= var35 * 0.5F && Math.abs(var3 - var34) <= var36 * 0.5F;
         if (var18) {
            if (!this.chatField.isMouseOver(var2, var3) && !var21) {
               if (var31 && var37) {
                  float var44 = (var2 - var27) * (var2 - var27) + (var3 - var28) * (var3 - var28);
                  float var46 = (var2 - var33) * (var2 - var33) + (var3 - var34) * (var3 - var34);
                  if (var44 <= var46) {
                     this.vmDragging = true;
                     HudConfig.isViewModelDragging = true;
                     this.vmArm = Arm.RIGHT;
                     this.vmLastMouseX = var2;
                     this.vmLastMouseY = var3;
                  } else {
                     this.vmDragging = true;
                     HudConfig.isViewModelDragging = true;
                     this.vmArm = Arm.LEFT;
                     this.vmLastMouseX = var2;
                     this.vmLastMouseY = var3;
                  }
               } else if (var31) {
                  this.vmDragging = true;
                  HudConfig.isViewModelDragging = true;
                  this.vmArm = Arm.RIGHT;
                  this.vmLastMouseX = var2;
                  this.vmLastMouseY = var3;
               } else if (var37) {
                  this.vmDragging = true;
                  HudConfig.isViewModelDragging = true;
                  this.vmArm = Arm.LEFT;
                  this.vmLastMouseX = var2;
                  this.vmLastMouseY = var3;
               }
            }
         } else if (var17) {
            if (this.vmDragging) {
               float var38 = var2 - this.vmLastMouseX;
               float var39 = var3 - this.vmLastMouseY;
               this.vmLastMouseX = var2;
               this.vmLastMouseY = var3;
               if (var38 != 0.0F || var39 != 0.0F) {
                  float var40 = 1.0F / (this.vmArm == Arm.RIGHT ? var22 : var24);
                  float var41 = 1.0F / (this.vmArm == Arm.RIGHT ? var23 : var25);
                  NumberSetting var42 = this.vmArm == Arm.RIGHT ? var9 : var11;
                  NumberSetting var43 = this.vmArm == Arm.RIGHT ? var10 : var12;
                  var42.setValue(var42.getValue() + var38 * var40);
                  var43.setValue(var43.getValue() - var39 * var41);
               }
            }
         } else if (this.vmDragging) {
            this.vmDragging = false;
            HudConfig.isViewModelDragging = false;
         }

         boolean var45 = this.vmDragging && this.vmArm == Arm.RIGHT;
         boolean var47 = this.vmDragging && this.vmArm == Arm.LEFT;
         float var48 = 0.35F;
         ViewModel.rightHoldAnim = ViewModel.rightHoldAnim + ((var45 ? 1.0F : 0.0F) - ViewModel.rightHoldAnim) * var48;
         ViewModel.leftHoldAnim = ViewModel.leftHoldAnim + ((var47 ? 1.0F : 0.0F) - ViewModel.leftHoldAnim) * var48;
      }
   }

   @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
   private void onMouseScrolled(double var1, double var3, double var5, double var7, CallbackInfoReturnable<Boolean> var9) {
      ViewModel var10 = FunctionManager.a(ViewModel.class);
      if (var10 != null && var10.isActive()) {
         MinecraftClient var11 = MinecraftClient.getInstance();
         float var12 = (float)var1 * RenderUtil.e();
         float var13 = (float)var3 * RenderUtil.e();
         if (HudConfig.c(var12, var13)) {
            return;
         }

         if (this.chatField != null && this.chatField.isMouseOver(var1, var3)) {
            return;
         }

         boolean var14 = var11.player == null || var11.player.getMainArm() == Arm.RIGHT;
         NumberSetting var15 = var14 ? var10.getMainX() : var10.getOffX();
         NumberSetting var16 = var14 ? var10.getMainY() : var10.getOffY();
         NumberSetting var17 = var14 ? var10.getMainZ() : var10.getOffZ();
         NumberSetting var18 = var14 ? var10.getOffX() : var10.getMainX();
         NumberSetting var19 = var14 ? var10.getOffY() : var10.getMainY();
         NumberSetting var20 = var14 ? var10.getOffZ() : var10.getMainZ();
         int var21 = var11.getWindow().getScaledWidth();
         int var22 = var11.getWindow().getScaledHeight();
         float var23 = var21 * 0.35F;
         float var24 = var22 * 0.35F;
         float var25 = var21 * 0.35F;
         float var26 = var22 * 0.35F;
         float var27 = (float)var10.getRightScale().getValue();
         float var28 = var21 * 0.7F + (float)var15.getValue() * var23;
         float var29 = var22 * 0.58F - (float)var16.getValue() * var24;
         float var30 = var21 * 0.3F * var27;
         float var31 = var22 * 0.48F * var27;
         boolean var32 = Math.abs(var1 - var28) <= var30 * 0.5F && Math.abs(var3 - var29) <= var31 * 0.5F;
         float var33 = (float)var10.getLeftScale().getValue();
         float var34 = var21 * 0.3F + (float)var18.getValue() * var25;
         float var35 = var22 * 0.58F - (float)var19.getValue() * var26;
         float var36 = var21 * 0.3F * var33;
         float var37 = var22 * 0.48F * var33;
         boolean var38 = Math.abs(var1 - var34) <= var36 * 0.5F && Math.abs(var3 - var35) <= var37 * 0.5F;
         if (this.vmDragging || var32 || var38) {
            Arm var39;
            if (this.vmDragging) {
               var39 = this.vmArm;
            } else if (var32 && var38) {
               float var40 = ((float)var1 - var28) * ((float)var1 - var28) + ((float)var3 - var29) * ((float)var3 - var29);
               float var41 = ((float)var1 - var34) * ((float)var1 - var34) + ((float)var3 - var35) * ((float)var3 - var35);
               var39 = var40 <= var41 ? Arm.RIGHT : Arm.LEFT;
            } else {
               var39 = var32 ? Arm.RIGHT : Arm.LEFT;
            }

            NumberSetting var42 = var39 == Arm.RIGHT ? var17 : var20;
            float var43 = (float)var7 * 0.03F;
            var42.setValue(var42.getValue() + var43);
            var9.setReturnValue(true);
         }
      }
   }

   @Inject(method = "removed()V", at = @At("TAIL"))
   private void onRemoved(CallbackInfo var1) {
      this.vmDragging = false;
      HudConfig.isViewModelDragging = false;
      ViewModel.rightHoldAnim = 0.0F;
      ViewModel.leftHoldAnim = 0.0F;
   }
}
