package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.AutoBuyEngine;
import brokke.cc.mixins.interfaces.ISimpleOption;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventGameUpdate;
import net.minecraft.client.option.SimpleOption;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

@NewFunction(
   name = "FrameSync",
   description = "Синхронизирует FPS с герцовкой экрана для плавной картинки и высокой производительности",
   category = Category.MISC
)
public class FrameSync extends Function {
   public final ModeSetting mode = new ModeSetting("Режим", "1x Hz", "1x Hz", "2x Hz", "Hz + 1", "Hz - 1", "Кастомный");
   public final NumberSetting customFps = new NumberSetting("Кастомный FPS", 144.0, 30.0, 360.0, 1.0);
   public final BooleanSetting unfocusedLimit = new BooleanSetting("Ограничивать в фоне", true);
   public final NumberSetting unfocusedFps = new NumberSetting("FPS в фоне", 30.0, 10.0, 60.0, 5.0);
   public final BooleanSetting menuLimit = new BooleanSetting("Ограничивать в меню", false);
   public final NumberSetting menuFps = new NumberSetting("FPS в меню", 60.0, 30.0, 144.0, 5.0);
   public final BooleanSetting vsync = new BooleanSetting("VSync", false);
   private int prevFps = -1;
   private boolean prevVsync = false;
   private boolean applied = false;
   private int cachedHz = 60;
   private long lastHzCheck = 0L;

   public FrameSync() {
      this.addSettings(this.mode, this.customFps, this.unfocusedLimit, this.unfocusedFps, this.menuLimit, this.menuFps, this.vsync);
      this.customFps.visibleWhen(() -> this.mode.isSelected("Кастомный"));
      this.unfocusedFps.visibleWhen(this.unfocusedLimit::getValue);
      this.menuFps.visibleWhen(this.menuLimit::getValue);
      this.mode.onChanged(this::m);
      this.customFps.onChanged(this::m);
      this.unfocusedLimit.onChanged(this::m);
      this.unfocusedFps.onChanged(this::m);
      this.menuLimit.onChanged(this::m);
      this.menuFps.onChanged(this::m);
      this.vsync.onChanged(this::m);
   }

   private int l() {
      if (mc.getWindow() == null) {
         return 60;
      }

      try {
         long var1 = mc.getWindow().getHandle();
         long var3 = GLFW.glfwGetWindowMonitor(var1);
         if (var3 == 0L) {
            int[] var5 = new int[1];
            int[] var6 = new int[1];
            int[] var7 = new int[1];
            int[] var8 = new int[1];
            GLFW.glfwGetWindowPos(var1, var5, var6);
            GLFW.glfwGetWindowSize(var1, var7, var8);
            int var9 = var5[0] + var7[0] / 2;
            int var10 = var6[0] + var8[0] / 2;
            PointerBuffer var11 = GLFW.glfwGetMonitors();
            if (var11 != null) {
               for (int var12 = 0; var12 < var11.limit(); var12++) {
                  long var13 = var11.get(var12);
                  int[] var15 = new int[1];
                  int[] var16 = new int[1];
                  GLFW.glfwGetMonitorPos(var13, var15, var16);
                  GLFWVidMode var17 = GLFW.glfwGetVideoMode(var13);
                  if (var17 != null && var9 >= var15[0] && var9 < var15[0] + var17.width() && var10 >= var16[0] && var10 < var16[0] + var17.height()) {
                     var3 = var13;
                     break;
                  }
               }
            }
         }

         if (var3 == 0L) {
            var3 = GLFW.glfwGetPrimaryMonitor();
         }

         if (var3 != 0L) {
            GLFWVidMode var19 = GLFW.glfwGetVideoMode(var3);
            if (var19 != null) {
               int var20 = var19.refreshRate();
               if (var20 > 0) {
                  return var20;
               }
            }
         }
      } catch (Exception var18) {
      }

      return 60;
   }

   public int k() {
      if (AutoBuyEngine.a()) {
         long var5 = System.currentTimeMillis();
         if (var5 - this.lastHzCheck > 1000L) {
            this.cachedHz = this.l();
            this.lastHzCheck = var5;
         }
         return switch (this.mode.getActiveMode()) {
            case "2x Hz" -> this.cachedHz * 2;
            case "Hz + 1" -> this.cachedHz + 1;
            case "Hz - 1" -> Math.max(30, this.cachedHz - 1);
            case "Кастомный" -> (int)this.customFps.getValue();
            default -> this.cachedHz;
         };
      } else {
         if (this.unfocusedLimit.getValue() && mc.getWindow() != null && !mc.isWindowFocused()) {
            return (int)this.unfocusedFps.getValue();
         }

         if (this.menuLimit.getValue() && mc.currentScreen != null) {
            return (int)this.menuFps.getValue();
         }

         long var1 = System.currentTimeMillis();
         if (var1 - this.lastHzCheck > 1000L) {
            this.cachedHz = this.l();
            this.lastHzCheck = var1;
         }
         return switch (this.mode.getActiveMode()) {
            case "2x Hz" -> this.cachedHz * 2;
            case "Hz + 1" -> this.cachedHz + 1;
            case "Hz - 1" -> Math.max(30, this.cachedHz - 1);
            case "Кастомный" -> (int)this.customFps.getValue();
            default -> this.cachedHz;
         };
      }
   }

   private void m() {
      if (mc.options != null) {
         SimpleOption var1 = mc.options.getMaxFps();
         SimpleOption var2 = mc.options.getEnableVsync();
         if (!this.applied) {
            this.prevFps = (Integer)var1.getValue();
            this.prevVsync = (Boolean)var2.getValue();
            this.applied = true;
         }

         int var3 = this.k();
         if ((Integer)var1.getValue() != var3) {
            ((ISimpleOption)(Object)var1).setValueRaw(var3);
         }

         boolean var4 = this.vsync.getValue();
         if ((Boolean)var2.getValue() != var4) {
            ((ISimpleOption)(Object)var2).setValueRaw(var4);
         }
      }
   }

   private void n() {
      if (this.applied && mc.options != null) {
         SimpleOption var1 = mc.options.getMaxFps();
         SimpleOption var2 = mc.options.getEnableVsync();
         int var3 = this.prevFps > 0 ? this.prevFps : 260;
         ((ISimpleOption)(Object)var1).setValueRaw(var3);
         ((ISimpleOption)(Object)var2).setValueRaw(this.prevVsync);
         this.applied = false;
      } else {
         this.applied = false;
      }
   }

   @Override
   public void onEnable() {
      this.cachedHz = this.l();
      this.lastHzCheck = System.currentTimeMillis();
      this.m();
   }

   @Override
   public void onDisable() {
      this.n();
   }

   @EventHandler
   public void onGameUpdate(EventGameUpdate var1) {
      this.m();
   }
}
