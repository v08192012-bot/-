package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventScroll;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "CameraTweaks", description = "Приближение и смена соотношения сторон", category = Category.RENDER)
public class CameraTweaks extends Function {
   public static boolean skipFovOverrideForSensitivity = false;
   private static final int DEFAULT_ZOOM_DIVISOR_STEP = 10;
   private static final int SCROLL_BASE = 2;
   private static final int SCROLL_RESOLUTION = 5;
   private static final int SCROLL_STEP_LIMIT = 30;
   private boolean zooming = false;
   private double zoomDivisor = 4.0;
   private float targetZoomStep = 10.0F;
   private float currentZoomStep = 10.0F;
   private float currentZoomMultiplier = 1.0F;
   private float targetZoomMultiplier = 1.0F;
   private boolean zoomSensitivityApplied = false;
   private double cachedSensitivity = 0.5;
   final BooleanSetting aspectRatio = new BooleanSetting("Aspect ratio", false);
   final ModeSetting aspectRatioMode = new ModeSetting("Пресет", "Custom", "16:9", "16:10", "4:3", "21:9", "1:1");
   final NumberSetting aspectRatioValue = new NumberSetting("Множитель", 1.0, 0.5, 2.5, 0.01);
   final BindSetting zoomSetting = new BindSetting("Приближение", 67);

   public CameraTweaks() {
      this.addSettings(this.aspectRatio, this.aspectRatioMode, this.aspectRatioValue, this.zoomSetting);
      this.aspectRatioMode.visibleWhen(this.aspectRatio::isToggled);
      this.aspectRatioValue.visibleWhen(() -> this.aspectRatio.isToggled() && this.aspectRatioMode.isMode("Custom"));
      this.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.r();
      this.zooming = false;
      this.currentZoomMultiplier = 1.0F;
      this.targetZoomMultiplier = 1.0F;
      super.onDisable();
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (var1.getAction() == 1 || var1.getAction() == 2) {
         int var2 = var1.getKey();
         if (this.zooming) {
            if (var2 == 61 || var2 == 334) {
               this.a(true, 1.0);
            } else if (var2 == 45 || var2 == 333) {
               this.a(false, 1.0);
            } else if (var2 == 48 || var2 == 320) {
               this.p();
            }
         }
      }
   }

   @EventHandler
   public void onScroll(EventScroll var1) {
      if (mc.currentScreen == null && this.zooming) {
         double var2 = var1.getVertical();
         if (var2 != 0.0) {
            this.a(var2 > 0.0, Math.abs(var2));
            var1.cancel();
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.s();
      this.n();
      this.q();
   }

   public Float a(float var1, float var2, boolean var3) {
      if (skipFovOverrideForSensitivity) {
         return null;
      }

      if (mc.currentScreen != null) {
         return null;
      }

      if (!var3) {
         return null;
      }

      this.n();
      if (!this.zooming && !(Math.abs(this.currentZoomMultiplier - 1.0F) > 0.005F)) {
         return null;
      }

      float var4 = this.a(var1);
      float var5 = var4 * this.currentZoomMultiplier;
      return MathHelper.clamp(var5, 10.0F, 170.0F);
   }

   public boolean k() {
      return this.aspectRatio.isToggled();
   }

   public float l() {
      if (!this.aspectRatio.isToggled()) {
         return 1.0F;
      }

      float var1 = 1.0F;
      if (mc != null && mc.getWindow() != null && mc.getWindow().getFramebufferHeight() > 0) {
         var1 = (float)((double)mc.getWindow().getFramebufferWidth() / mc.getWindow().getFramebufferHeight());
      }

      float var10000;
      if (var1 > 0.0F) {
         var10000 = var1 / switch (this.aspectRatioMode.getSelection().toLowerCase()) {
            case "16:9" -> 1.7777778F;
            case "16:10" -> 1.6F;
            case "4:3" -> 1.3333334F;
            case "21:9" -> 2.3333333F;
            case "1:1" -> 1.0F;
            default -> var1 / (float)this.aspectRatioValue.getValue();
         };
      } else {
         var10000 = 1.0F;
      }

      return var10000;
   }

   private void n() {
      this.o();
      float var1 = this.zooming ? (float)(1.0 / this.zoomDivisor) : 1.0F;
      this.targetZoomMultiplier = var1;
      if (this.zooming) {
         this.currentZoomMultiplier = MathHelper.lerp(0.25F, this.currentZoomMultiplier, this.targetZoomMultiplier);
      } else {
         this.currentZoomMultiplier = MathHelper.lerp(0.25F, this.currentZoomMultiplier, 1.0F);
         if (Math.abs(this.currentZoomMultiplier - 1.0F) < 0.002F) {
            this.currentZoomMultiplier = 1.0F;
         }
      }
   }

   private void o() {
      this.currentZoomStep = this.currentZoomStep + (this.targetZoomStep - this.currentZoomStep) * 0.5F;
      if (Math.abs(this.currentZoomStep - this.targetZoomStep) < 0.01F) {
         this.currentZoomStep = this.targetZoomStep;
      }

      this.zoomDivisor = this.currentZoomStep != 0.0F ? Math.pow(2.0, this.currentZoomStep / 5.0F) : 1.0;
   }

   private void a(boolean var1, double var2) {
      if (var1) {
         this.targetZoomStep = Math.min(this.targetZoomStep + (float)var2, 30.0F);
      } else {
         this.targetZoomStep = Math.max(this.targetZoomStep - (float)var2, 0.0F);
      }
   }

   private void p() {
      this.targetZoomStep = 10.0F;
      this.currentZoomStep = this.targetZoomStep;
      this.zoomDivisor = Math.pow(2.0, this.currentZoomStep / 5.0F);
   }

   public boolean isZooming() {
      return this.zooming;
   }

   private void q() {
      if (mc != null && mc.options != null && mc.options.getMouseSensitivity() != null) {
         if (!this.zooming) {
            this.r();
         } else {
            if (!this.zoomSensitivityApplied) {
               this.cachedSensitivity = mc.options.getMouseSensitivity().getValue();
               this.zoomSensitivityApplied = true;
            }

            double var1 = this.cachedSensitivity * this.currentZoomMultiplier;
            mc.options.getMouseSensitivity().setValue(Math.max(0.01, var1));
         }
      }
   }

   private void r() {
      if (this.zoomSensitivityApplied && mc != null && mc.options != null && mc.options.getMouseSensitivity() != null) {
         mc.options.getMouseSensitivity().setValue(this.cachedSensitivity);
         this.zoomSensitivityApplied = false;
      }
   }

   private void s() {
      boolean var1 = mc.currentScreen == null && this.t();
      if (var1 != this.zooming) {
         this.zooming = var1;
      }
   }

   private boolean t() {
      if (mc != null && mc.getWindow() != null) {
         int var1 = this.zoomSetting.getKey();
         if (var1 != -1 && var1 != 0) {
            long var2 = mc.getWindow().getHandle();
            if (var2 == 0L) {
               return false;
            }

            if (var1 < 0) {
               int var4 = -(var1 + 100);
               if (var4 >= 0) {
                  return GLFW.glfwGetMouseButton(var2, var4) == 1;
               }
            }

            return var1 >= 1000 ? GLFW.glfwGetMouseButton(var2, var1 - 1000) == 1 : GLFW.glfwGetKey(var2, var1) == 1;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private float a(float var1) {
      return mc != null && mc.options != null && mc.options.getFov() != null ? mc.options.getFov().getValue().intValue() : var1;
   }
}
