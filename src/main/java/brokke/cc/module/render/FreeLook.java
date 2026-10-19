package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.FreeLookStorage;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "FreeLook", description = "Вращай камерой, не меняя направление персонажа", category = Category.RENDER)
public class FreeLook extends Function {
   public final BindSetting key = new BindSetting("Клавиша", 342);
   public final BooleanSetting thirdPerson = new BooleanSetting("От третьего лица", true);
   private boolean active;
   private float cameraYaw;
   private float cameraPitch;
   private Perspective prevPerspective = Perspective.FIRST_PERSON;

   public FreeLook() {
      this.addSettings(this.key, this.thirdPerson);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.getWindow() != null) {
         if (mc.currentScreen != null) {
            if (this.active) {
               this.n();
            }
         } else {
            int var2 = this.key.getKey();
            if (var2 == -1) {
               if (this.active) {
                  this.n();
               }
            } else {
               boolean var3;
               if (var2 < 0) {
                  var3 = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), -(var2 + 100)) == 1;
               } else {
                  var3 = GLFW.glfwGetKey(mc.getWindow().getHandle(), var2) == 1;
               }

               if (var3 && !this.active) {
                  this.active = true;
                  this.cameraYaw = mc.player.getYaw();
                  this.cameraPitch = mc.player.getPitch();
                  FreeLookStorage.a(true);
                  FreeLookStorage.setFreeYaw(this.cameraYaw);
                  FreeLookStorage.setFreePitch(this.cameraPitch);
                  if (this.thirdPerson.isToggled() && mc.options != null) {
                     this.prevPerspective = mc.options.getPerspective();
                     mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                  }
               } else if (!var3 && this.active) {
                  this.n();
               }
            }
         }
      } else if (this.active) {
         this.n();
      }
   }

   public boolean k() {
      return this.isActive() && this.active && mc.currentScreen == null;
   }

   public float getCameraYaw() {
      return this.cameraYaw;
   }

   public float getCameraPitch() {
      return this.cameraPitch;
   }

   public void a(float var1, float var2) {
      float var3 = GCDUtil.a();
      this.cameraYaw += var1 * var3;
      this.cameraPitch = MathHelper.clamp(this.cameraPitch + var2 * var3, -90.0F, 90.0F);
      FreeLookStorage.setFreeYaw(this.cameraYaw);
      FreeLookStorage.setFreePitch(this.cameraPitch);
   }

   private void n() {
      this.active = false;
      FreeLookStorage.a(false);
      if (this.thirdPerson.isToggled() && mc.options != null) {
         mc.options.setPerspective(this.prevPerspective);
      }
   }

   @Override
   public void onDisable() {
      if (this.active) {
         this.n();
      }

      super.onDisable();
   }
}
