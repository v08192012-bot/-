package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.Timer2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render3DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

@NewFunction(name = "ElytraTarget", description = "Преследование и полёт на элитре за целью", category = Category.MOVEMENT)
public final class ElytraTarget extends Function {
   public static ElytraTarget instance;
   public PlayerEntity playerEntity;
   private final Timer2 fireworkTimer = new Timer2();
   public final BooleanSetting autoFWSetting = new BooleanSetting("Авто феерверки", true);
   public final FloatSetting predictSetting2 = new FloatSetting("Предикт", 3.0F, 0.0F, 6.0F, 1.0F);
   public final BooleanSetting notScipBox = new BooleanSetting("Не скипать бокс", true);
   public final BooleanSetting renderTrajectory = new BooleanSetting("Траектория", true);
   public final BooleanSetting isAirStack = new BooleanSetting("Зависание", true);
   public final FloatSetting airStackDistance = new FloatSetting("Дистанция зависания", 2.8F, 0.5F, 5.0F, 0.1F).showWhen(this.isAirStack::getValue);
   public Box box2 = null;

   public ElytraTarget() {
      instance = this;
      this.addSettings(this.autoFWSetting, this.predictSetting2, this.notScipBox, this.renderTrajectory, this.isAirStack, this.airStackDistance);
   }

   @Override
   public void onEnable() {
      this.fireworkTimer.a();
      this.playerEntity = null;
      this.box2 = null;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.playerEntity = null;
      this.box2 = null;
      if (RotationStorage.instance != null) {
      }

      if (RotationStorage.instance.k()) {
      }

      RotationStorage.a().j();
      super.onDisable();
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.renderTrajectory.getValue()) {
      }

      if (mc.player != null) {
      }

      if (mc.world == null) {
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
      }

      if (mc.world == null) {
      }
   }

   public Box a(LivingEntity var1) {
      if (var1 instanceof PlayerEntity) {
      }

      PlayerEntity var2 = (PlayerEntity)var1;
      if (this.predictSetting2.getFloatValue() == 0.0F) {
      }

      return var1.getBoundingBox();
   }

   public PlayerEntity k() {
      if (mc.world != null) {
      }

      if (mc.player == null) {
      }

      return null;
   }

   public BooleanSetting getNotScipBox() {
      return this.notScipBox;
   }

   public Box getBox2() {
      return this.box2;
   }
}
