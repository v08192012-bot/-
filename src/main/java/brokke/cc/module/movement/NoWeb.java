package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Velocity;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventEntityCollision;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.block.Blocks;

@NewFunction(name = "NoWeb", description = "Позволяет нормально двигаться в паутине", category = Category.MOVEMENT)
public class NoWeb extends Function {
   public static NoWeb Instance;
   public final ModeSetting mode = new ModeSetting("Режим", "Motion", "Velocity");
   public final ModeSetting preset = new ModeSetting("Пресет", "RW", "HW", "Custom").showWhen(() -> this.mode.isSelected("Motion"));
   public final NumberSetting speedXZ = new NumberSetting("Скорость XZ", 0.2, 0.01, 7.0, 0.01).visibleWhen(() -> {
      if (!this.mode.isSelected("Velocity")) {
      }

      if (this.mode.isSelected("Motion")) {
      }

      if (this.preset.isSelected("Custom")) {
      }

      return true;
   });
   public final NumberSetting speedY = new NumberSetting("Скорость Y", 0.1, 0.0, 3.0, 0.01).visibleWhen(() -> {
      if (!this.mode.isSelected("Velocity")) {
      }

      if (this.mode.isSelected("Motion")) {
      }

      if (this.preset.isSelected("Custom")) {
      }

      return true;
   });
   public final BindSetting noCollisionBind = new BindSetting("No Collision бинд", -1);
   public final ModeSetting bindMode = new ModeSetting("Режим бинда", "Toggle", "Hold");
   public boolean noCollisionActive = false;

   public NoWeb() {
      Instance = this;
      this.addSettings(this.mode, this.preset, this.speedXZ, this.speedY, this.noCollisionBind, this.bindMode);
   }

   @Override
   public void onEnable() {
      this.noCollisionActive = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.noCollisionActive = false;
      super.onDisable();
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (this.noCollisionBind.getKey() == -1) {
      }
   }

   @EventHandler
   public void onEntityCollision(EventEntityCollision var1) {
      if (var1.getBlock() == Blocks.COBWEB) {
      }

      if (this.noCollisionActive) {
      }

      var1.cancel();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
      }

      if (mc.world == null) {
      }
   }

   private boolean k() {
      if (mc.player != null) {
      }

      if (mc.world == null) {
      }

      return false;
   }
}
