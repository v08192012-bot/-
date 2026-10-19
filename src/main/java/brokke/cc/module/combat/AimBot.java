package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.rotation.RotationUtil;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "AimBot", description = "Автоматически наводится луком/арбалетом", category = Category.COMBAT)
public class AimBot extends Function {
   private final NumberSetting distance = new NumberSetting("Дистанция", 30.0, 0.0, 100.0, 1.0);
   private final NumberSetting fov = new NumberSetting("FOV", 90.0, 1.0, 180.0, 1.0);
   private final BooleanSetting prediction = new BooleanSetting("Предсказание", true);
   private final BooleanSetting silent = new BooleanSetting("Silent Aim", false);

   public AimBot() {
      this.addSettings(this.distance, this.fov, this.prediction, this.silent);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && this.k()) {
         Entity var2 = this.l();
         if (var2 != null) {
            this.a(var2);
            float[] var4 = RotationUtil.a(var2);
            float var5 = var4[0];
            float var6 = var4[1];
            if (!(Math.abs(RotationUtil.a(var5, mc.player.getYaw())) > this.fov.getValue())) {
               if (this.silent.isToggled()) {
                  mc.player.setYaw(var5);
                  mc.player.setPitch(var6);
                  mc.player.setHeadYaw(var5);
               } else {
                  mc.player.setYaw(var5);
                  mc.player.setPitch(var6);
                  mc.player.setHeadYaw(var5);
               }
            }
         }
      }
   }

   private boolean k() {
      if (mc.player == null) {
         return false;
      } else {
         Item var1 = mc.player.getMainHandStack().getItem();
         if (var1 == Items.BOW) {
            return mc.player.isUsingItem();
         } else if (var1 == Items.CROSSBOW) {
            return CrossbowItem.isCharged(mc.player.getMainHandStack());
         } else {
            return var1 == Items.TRIDENT ? mc.player.isUsingItem() : false;
         }
      }
   }

   private Entity l() {
      if (mc.world != null && mc.player != null) {
         Entity var1 = null;
         double var2 = this.distance.getValue();

         for (Entity var5 : mc.world.getEntities()) {
            double var6;
            if (var5 != mc.player
               && var5 instanceof LivingEntity
               && var5.isAlive()
               && !((var6 = mc.player.distanceTo(var5)) > this.distance.getValue())
               && var6 < var2) {
               var2 = var6;
               var1 = var5;
            }
         }

         return var1;
      } else {
         return null;
      }
   }

   private Vec3d a(Entity var1) {
      Vec3d var2 = var1.getEntityPos();
      if (!this.prediction.isToggled()) {
         return var2.add(0.0, var1.getHeight() / 2.0, 0.0);
      }

      Vec3d var3 = var1.getVelocity().multiply(10.0);
      return var2.add(var3).add(0.0, var1.getHeight() / 2.0, 0.0);
   }
}
