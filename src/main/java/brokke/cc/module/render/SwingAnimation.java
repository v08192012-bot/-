package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Aura;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@NewFunction(name = "SwingAnimation", description = "Изменяет анимации рук при взмахе", category = Category.RENDER)
public class SwingAnimation extends Function {
   private final ModeSetting mode = new ModeSetting("Анимация", "1.7", "1.8", "Бонк", "Вращение", "Комбо", "Слэш", "Взмах");
   private final NumberSetting power = new NumberSetting("Сила удара", 1.0, 0.1, 3.0, 0.1);
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.5, 3.0, 0.1);
   private final BooleanSetting onlySword = new BooleanSetting("Только мечи", false);
   private final BooleanSetting onlyAura = new BooleanSetting("Только с Aura", false);
   private final BooleanSetting leftHand = new BooleanSetting("Левая рука", true);
   private int comboIndex = 0;
   private float prevProgress = 0.0F;

   public SwingAnimation() {
      this.addSettings(this.mode, this.power, this.speed, this.onlySword, this.onlyAura, this.leftHand);
   }

   public ModeSetting getMode() {
      return this.mode;
   }

   public NumberSetting getPower() {
      return this.power;
   }

   public NumberSetting getSpeed() {
      return this.speed;
   }

   public BooleanSetting getOnlySword() {
      return this.onlySword;
   }

   public BooleanSetting getOnlyAura() {
      return this.onlyAura;
   }

   public BooleanSetting getLeftHand() {
      return this.leftHand;
   }

   public boolean q() {
      if (!this.onlyAura.isOn()) {
         return true;
      }

      Aura var1 = FunctionManager.a(Aura.class);
      return var1 != null && var1.isActive();
   }

   public double r() {
      return 1.0 / Math.max(0.1, this.speed.getValue());
   }

   public int s() {
      return this.isActive() && this.q() ? Math.max(1, (int)(6.0 / this.speed.getValue())) : -1;
   }

   public boolean a(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (this.onlySword.isOn() && !var1.isIn(ItemTags.SWORDS)) {
         return false;
      }

      Item var2 = var1.getItem();
      return var2 != Items.AIR
         && var2 != Items.FILLED_MAP
         && var2 != Items.CROSSBOW
         && var2 != Items.BOW
         && var2 != Items.TRIDENT
         && var2.getUseAction(var1) != UseAction.DRINK
         && var2.getUseAction(var1) != UseAction.EAT;
   }

   public boolean a(MatrixStack var1, float var2, Arm var3, int var4) {
      if (!this.isActive() || !this.q()) {
         return false;
      }

      if (!this.leftHand.isOn() && var3 == Arm.LEFT) {
         return false;
      }

      MinecraftClient var5 = MinecraftClient.getInstance();
      if (var5.player == null) {
         return false;
      }

      ItemStack var6 = var3 == var5.player.getMainArm() ? var5.player.getMainHandStack() : var5.player.getOffHandStack();
      if (!this.a(var6)) {
         return false;
      }

      if (var2 <= 0.0F) {
         return false;
      }

      this.a(var1, var2, var3, (float)var4);
      return true;
   }

   private void a(MatrixStack var1, float var2, Arm var3, float var4) {
      float var5 = (float)this.power.getValue();
      String var6 = this.mode.getSelection();
      boolean var7 = true;
      switch (var6) {
         case "1.7":
            float var21 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var26 = MathHelper.sin(var2 * var2 * (float) Math.PI);
            var1.translate(var4 * -0.15F * var21 * var5, 0.08F * var21 * var5, -0.1F * var21 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * (45.0F + var26 * -20.0F * var5)));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * var21 * -20.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var21 * -70.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         case "1.8":
            float var20 = -0.35F * MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var25 = var7 ? 0.0F : 0.18F * MathHelper.sin(MathHelper.sqrt(var2) * (float) (Math.PI * 2));
            float var12 = -0.18F * MathHelper.sin(var2 * (float) Math.PI);
            var1.translate(var4 * var20 * var5, var25 * var5, var12 * var5);
            float var13 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var14 = MathHelper.sin(var2 * var2 * (float) Math.PI);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * (45.0F + var14 * -20.0F * var5)));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * var13 * -25.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var13 * -80.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         case "Бонк":
            float var19 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var24 = MathHelper.sin(var2 * (float) Math.PI);
            var1.translate(var4 * -0.06F * var19 * var5, var7 ? 0.0F : -0.14F * var19 * var5, -0.12F * var24 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-65.0F * var19 * var5));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * 18.0F * var19 * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         case "Вращение":
            float var18 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            var1.translate(var4 * -0.1F * var18 * var5, 0.06F * var18 * var5, -0.08F * var18 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-360.0F * var2 * var5));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * var18 * -20.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         case "Комбо":
            if (var2 < this.prevProgress && this.prevProgress > 0.6F) {
               this.comboIndex = (this.comboIndex + 1) % 5;
            }

            this.prevProgress = var2;
            float var17 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var23 = MathHelper.sin(var2 * (float) Math.PI);
            if (this.comboIndex == 0) {
               var1.translate(var4 * -0.12F * var17 * var5, var7 ? 0.0F : -0.08F * var17 * var5, -0.1F * var23 * var5);
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-65.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -30.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * 35.0F * var23 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            } else if (this.comboIndex == 1) {
               var1.translate(var4 * 0.12F * var17 * var5, var7 ? 0.0F : -0.08F * var17 * var5, -0.1F * var23 * var5);
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 35.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * -35.0F * var23 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            } else if (this.comboIndex == 2) {
               var1.translate(var4 * -0.08F * var17 * var5, 0.05F * var17 * var5, -0.4F * var17 * var5);
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * 10.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            } else if (this.comboIndex == 3) {
               var1.translate(var4 * -0.1F * var17 * var5, 0.15F * var17 * var5, -0.1F * var17 * var5);
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * -40.0F * var17 * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            } else {
               var1.translate(var4 * -0.1F * var17 * var5, 0.06F * var17 * var5, -0.08F * var17 * var5);
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-360.0F * var2 * var5));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * var17 * -20.0F * var5));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            }
            break;
         case "Слэш":
            float var16 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var22 = MathHelper.sin(var2 * (float) Math.PI);
            var1.translate(var4 * (0.22F * var16 - 0.1F * var22) * var5, var7 ? 0.0F : -0.06F * var16 * var5, -0.14F * var22 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -80.0F * var16 * var5));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * var16 * var5));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * -35.0F * var16 * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         case "Взмах":
            float var15 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            var1.translate(var4 * -0.12F * var15 * var5, 0.18F * var15 * var5, -0.12F * var15 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * 45.0F));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(42.0F * var15 * var5));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * -40.0F * var15 * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
            break;
         default:
            float var10 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
            float var11 = MathHelper.sin(var2 * var2 * (float) Math.PI);
            var1.translate(var4 * -0.15F * var10 * var5, 0.08F * var10 * var5, -0.1F * var10 * var5);
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * (45.0F + var11 * -20.0F * var5)));
            var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var4 * var10 * -20.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var10 * -70.0F * var5));
            var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4 * -45.0F));
      }
   }
}
