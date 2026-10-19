package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.Theme;
import brokke.cc.render.WetWorldPipeline;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import java.awt.Color;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@NewFunction(name = "WetWorld", description = "Делает блоки мокрыми с отражениями и рябью", category = Category.RENDER)
public class WetWorld extends Function {
   public final ModeSetting quality = new ModeSetting("Качество", "Низкое", "Среднее", "Высокое", "Ультра").withMode("Высокое");
   public final NumberSetting reflectionStrength = new NumberSetting("Сила отражений", 0.75, 0.1, 1.5, 0.05);
   public final NumberSetting darkening = new NumberSetting("Затемнение", 0.3, 0.0, 0.8, 0.05);
   public final BooleanSetting ripples = new BooleanSetting("Рябь", true);
   public final NumberSetting rippleSpeed = new NumberSetting("Скорость ряби", 1.0, 0.1, 3.0, 0.1);
   public final BooleanSetting rainOnly = new BooleanSetting("Только в дождь", false);
   private static WetWorld instance;

   public static WetWorld k() {
      if (instance == null) {
         instance = FunctionManager.a(WetWorld.class);
      }

      return instance;
   }

   public WetWorld() {
      this.addSettings(this.quality, this.reflectionStrength, this.darkening, this.ripples, this.rippleSpeed, this.rainOnly);
      this.ripples.onChanged(() -> this.rippleSpeed.setVisible(this.ripples.isToggled()));
   }

   public int l() {
      if (this.quality.isMode("Низкое") || this.quality.isMode("Low")) {
         return 0;
      } else if (this.quality.isMode("Среднее") || this.quality.isMode("Medium")) {
         return 1;
      } else if (!this.quality.isMode("Высокое") && !this.quality.isMode("High")) {
         return !this.quality.isMode("Ультра") && !this.quality.isMode("Ultra") ? 2 : 3;
      } else {
         return 2;
      }
   }

   @Override
   public void onEnable() {
      WetWorldPipeline.b();
   }

   @Override
   public void onDisable() {
      WetWorldPipeline.b();
   }

   public static void m() {
      WetWorld var0 = k();
      if (var0 != null && var0.isActive()) {
         var0.n();
      }
   }

   private void n() {
      if (mc.world != null && mc.player != null && mc.gameRenderer != null) {
         float var1 = mc.getRenderTickCounter().getTickProgress(true);
         float var2 = mc.world.getRainGradient(var1);
         if (!this.rainOnly.isToggled() || !(var2 <= 0.01F)) {
            Camera var3 = mc.gameRenderer.getCamera();
            if (var3 != null) {
               Vec3d var4 = var3.getCameraPos();
               float var5 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(var3, var1, true);
               Matrix4f var6 = mc.gameRenderer.getBasicProjectionMatrix(var5);
               if (var6 != null) {
                  Matrix4f var7 = new Matrix4f(var6).invert();
                  Matrix4f var8 = new Matrix4f().rotation(var3.getRotation());
                  float var9 = (float)this.reflectionStrength.getValue();
                  if (this.rainOnly.isToggled()) {
                     var9 *= Math.clamp(var2, 0.0F, 1.0F);
                  }

                  float var10 = (float)(System.currentTimeMillis() % 120000L / 1000.0);
                  CustomFog var11 = FunctionManager.a(CustomFog.class);
                  float var12 = 0.0F;
                  float var13 = 10000.0F;
                  float var14 = 0.0F;
                  Color var15 = new Color(0, 0, 0, 0);
                  float var16 = 1.0F;
                  if (var11 != null && var11.isActive()) {
                     var12 = (float)var11.getStart().getValue();
                     var13 = (float)(var11.getStart().getValue() + var11.getDispersion().getValue());
                     var15 = Theme.getInstance().getAccent();
                     var16 = (float)var11.getStrength().getValue();
                     var14 = var11.o() ? 1.0F : 0.0F;
                  } else if (mc.options != null) {
                     int var17 = mc.options.getViewDistance().getValue();
                     var13 = var17 * 16.0F;
                     var12 = var13 * 0.75F;
                  }

                  WetWorldPipeline.a(
                     var6,
                     var7,
                     var8,
                     var4,
                     var5,
                     var9,
                     (float)this.darkening.getValue(),
                     var10,
                     this.l(),
                     this.ripples.isToggled(),
                     (float)this.rippleSpeed.getValue(),
                     var2,
                     var12,
                     var13,
                     var14,
                     var15,
                     var16
                  );
               }
            }
         }
      }
   }
}
