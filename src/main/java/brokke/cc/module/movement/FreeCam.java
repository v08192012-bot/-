package brokke.cc.module.movement;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.SendPacketEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.world.World;

@NewFunction(name = "Free Camera", description = "Свободная камера", category = Category.MOVEMENT)
public class FreeCam extends Function {
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 5.0, 0.1);
   private float savedYaw;
   private float savedPitch;
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;
   private World lastWorld;

   public FreeCam() {
      this.addSettings(this.speed);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         mc.chunkCullingEnabled = false;
         this.savedYaw = mc.player.getYaw();
         this.savedPitch = mc.player.getPitch();
         this.fakeX = this.prevFakeX = mc.player.getX();
         this.fakeY = this.prevFakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
         this.fakeZ = this.prevFakeZ = mc.player.getZ();
         this.lastWorld = mc.world;
         super.onEnable();
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDisable() {
      mc.chunkCullingEnabled = true;
      this.lastWorld = null;
      if (mc.player != null) {
         mc.player.setYaw(this.savedYaw);
         mc.player.setPitch(this.savedPitch);
      }

      super.onDisable();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && mc.world == this.lastWorld) {
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         float var2 = 0.0F;
         float var3 = 0.0F;
         if (mc.options.forwardKey.isPressed()) {
            var2++;
         }

         if (mc.options.backKey.isPressed()) {
            var2--;
         }

         if (mc.options.leftKey.isPressed()) {
            var3++;
         }

         if (mc.options.rightKey.isPressed()) {
            var3--;
         }

         double var4 = this.speed.getValue();
         if (mc.options.sprintKey.isPressed()) {
            var4 *= 1.5;
         }

         double[] var6 = this.a(var2, var3, var4);
         this.fakeX = this.fakeX + var6[0];
         this.fakeZ = this.fakeZ + var6[1];
         if (mc.options.jumpKey.isPressed()) {
            this.fakeY += var4;
         }

         if (mc.options.sneakKey.isPressed()) {
            this.fakeY -= var4;
         }
      } else {
         this.toggle();
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      var1.setForward(0.0F);
      var1.setStrafe(0.0F);
      var1.setJump(false);
      var1.setSneak(false);
      var1.setSprint(false);
   }

   @EventHandler
   public void onSendPacket(SendPacketEvent var1) {
      if (var1.getPacket() instanceof PlayerMoveC2SPacket) {
         var1.cancel();
      }
   }

   private double[] a(float var1, float var2, double var3) {
      if (mc.player == null) {
         return new double[]{0.0, 0.0};
      }

      float var5 = mc.player.getYaw();
      if (var1 != 0.0F) {
         if (var2 > 0.0F) {
            var5 += var1 > 0.0F ? -45.0F : 45.0F;
         } else if (var2 < 0.0F) {
            var5 += var1 > 0.0F ? 45.0F : -45.0F;
         }

         var2 = 0.0F;
         var1 = var1 > 0.0F ? 1.0F : -1.0F;
      }

      double var6 = Math.sin(Math.toRadians(var5 + 90.0F));
      double var8 = Math.cos(Math.toRadians(var5 + 90.0F));
      return new double[]{var1 * var3 * var8 + var2 * var3 * var6, var1 * var3 * var6 - var2 * var3 * var8};
   }

   public float k() {
      return mc.player != null ? mc.player.getYaw() : this.savedYaw;
   }

   public float l() {
      return mc.player != null ? mc.player.getPitch() : this.savedPitch;
   }

   public double a(float var1) {
      return this.prevFakeX + (this.fakeX - this.prevFakeX) * var1;
   }

   public double b(float var1) {
      return this.prevFakeY + (this.fakeY - this.prevFakeY) * var1;
   }

   public double c(float var1) {
      return this.prevFakeZ + (this.fakeZ - this.prevFakeZ) * var1;
   }
}
