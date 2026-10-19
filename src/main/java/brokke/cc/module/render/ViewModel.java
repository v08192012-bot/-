package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.ViewModelHandOutlineRenderer;
import brokke.cc.setting.ButtonSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.HandEvent;
import brokke.cc.system.events.impl.HandRenderEvent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@NewFunction(
   name = "ViewModel",
   description = "Изменяет положение и размер предметов в руке (ЛКМ в чате для перемещения, Колёсико мыши для Z)",
   category = Category.RENDER
)
public class ViewModel extends Function {
   private final NumberSetting mainX = new NumberSetting("Основная рука X", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting mainY = new NumberSetting("Основная рука Y", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting mainZ = new NumberSetting("Основная рука Z", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting offX = new NumberSetting("Вторая рука X", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting offY = new NumberSetting("Вторая рука Y", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting offZ = new NumberSetting("Вторая рука Z", 0.0, -2.0, 2.0, 0.1);
   private final NumberSetting rightScale = new NumberSetting("Размер правой руки", 1.0, 0.1, 3.0, 0.05);
   private final NumberSetting leftScale = new NumberSetting("Размер левой руки", 1.0, 0.1, 3.0, 0.05);
   public static float rightHoverAnim = 0.0F;
   public static float leftHoverAnim = 0.0F;
   public static float rightHoldAnim = 0.0F;
   public static float leftHoldAnim = 0.0F;

   public ViewModel() {
      ButtonSetting var1 = new ButtonSetting("Сбросить позиции", () -> this.getSettings().forEach(var0 -> {
         if (var0 instanceof NumberSetting var1x) {
            var1x.resetToDefault();
         }
      }));
      this.addSettings(this.mainX, this.mainY, this.mainZ, this.offX, this.offY, this.offZ, this.rightScale, this.leftScale, var1);
   }

   @Override
   public void onDisable() {
      rightHoverAnim = 0.0F;
      leftHoverAnim = 0.0F;
      rightHoldAnim = 0.0F;
      leftHoldAnim = 0.0F;
   }

   @EventHandler
   public void onHand(HandEvent var1) {
      if (var1.isPre()) {
         ViewModelHandOutlineRenderer.a();
      } else if (var1.isPost()) {
         ViewModelHandOutlineRenderer.b();
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onHandRender(HandRenderEvent var1) {
      MatrixStack var2 = var1.getMatrices();
      boolean var3 = var1.getArm() == (mc.player != null ? mc.player.getMainArm() : Arm.RIGHT);
      boolean var4 = var1.getArm() == Arm.RIGHT;
      if (var3) {
         var2.translate((float)this.mainX.getValue(), (float)this.mainY.getValue(), (float)this.mainZ.getValue());
      } else {
         var2.translate((float)this.offX.getValue(), (float)this.offY.getValue(), (float)this.offZ.getValue());
      }

      float var5 = var4 ? (float)this.rightScale.getValue() : (float)this.leftScale.getValue();
      if (var5 != 1.0F) {
         var2.scale(var5, var5, var5);
      }
   }

   public NumberSetting getMainX() {
      return this.mainX;
   }

   public NumberSetting getMainY() {
      return this.mainY;
   }

   public NumberSetting getMainZ() {
      return this.mainZ;
   }

   public NumberSetting getOffX() {
      return this.offX;
   }

   public NumberSetting getOffY() {
      return this.offY;
   }

   public NumberSetting getOffZ() {
      return this.offZ;
   }

   public NumberSetting getRightScale() {
      return this.rightScale;
   }

   public NumberSetting getLeftScale() {
      return this.leftScale;
   }
}
