package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "Optimizer", description = "Не рендерит объекты позади камеры для повышения FPS", category = Category.MISC)
public class Optimizer extends Function {
   public final BooleanSetting cullEntities = new BooleanSetting("Сущности", true);
   public final BooleanSetting cullBlockEntities = new BooleanSetting("Блок-сущности", true);
   public final BooleanSetting cullParticles = new BooleanSetting("Частицы", true);
   public final NumberSetting safetyMargin = new NumberSetting("Отступ (блоки)", 1.0, 0.0, 4.0, 0.5);
   private static double camX;
   private static double camY;
   private static double camZ;
   private static double lookX;
   private static double lookY;
   private static double lookZ;
   private static boolean cameraValid = false;
   private static Optimizer INSTANCE;

   public Optimizer() {
      INSTANCE = this;
      this.addSettings(this.cullEntities, this.cullBlockEntities, this.cullParticles, this.safetyMargin);
      this.setEnabled(true);
   }

   public static Optimizer k() {
      if (INSTANCE == null) {
         INSTANCE = FunctionManager.a(Optimizer.class);
      }

      return INSTANCE;
   }

   public static boolean l() {
      Optimizer var0 = k();
      return var0 != null && var0.isActive() && var0.cullEntities.getValue();
   }

   public static boolean m() {
      Optimizer var0 = k();
      return var0 != null && var0.isActive() && var0.cullBlockEntities.getValue();
   }

   public static boolean n() {
      Optimizer var0 = k();
      return var0 != null && var0.isActive() && var0.cullParticles.getValue();
   }

   public static double o() {
      Optimizer var0 = k();
      return var0 != null ? var0.safetyMargin.getValue() : 1.0;
   }

   public static void a(Camera var0) {
      if (var0 == null) {
         cameraValid = false;
      } else {
         Vec3d var1 = var0.getCameraPos();
         camX = var1.x;
         camY = var1.y;
         camZ = var1.z;
         float var2 = var0.getYaw();
         float var3 = var0.getPitch();
         double var4 = Math.toRadians(var2);
         double var6 = Math.toRadians(var3);
         lookX = -Math.sin(var4) * Math.cos(var6);
         lookY = -Math.sin(var6);
         lookZ = Math.cos(var4) * Math.cos(var6);
         cameraValid = true;
      }
   }

   private static void p() {
      if (!cameraValid) {
         MinecraftClient var0 = MinecraftClient.getInstance();
         if (var0.gameRenderer != null && var0.gameRenderer.getCamera() != null) {
            a(var0.gameRenderer.getCamera());
         }
      }
   }

   public static boolean a(double var0, double var2, double var4, double var6) {
      p();
      if (!cameraValid) {
         return false;
      }

      double var8 = var0 * lookX + var2 * lookY + var4 * lookZ;
      return var8 < -var6;
   }

   public static boolean a(Entity var0, double var1, double var3, double var5) {
      if (l() && var0 != null) {
         MinecraftClient var7 = MinecraftClient.getInstance();
         if (var0 == var7.player) {
            return false;
         }

         double var8 = var0.getX() - var1;
         double var10 = var0.getY() - var3;
         double var12 = var0.getZ() - var5;
         double var14 = Math.max(var0.getWidth(), var0.getHeight()) * 0.75;
         return a(var8, var10, var12, var14 + o());
      } else {
         return false;
      }
   }

   public static boolean a(EntityRenderState var0, double var1, double var3, double var5) {
      if (l() && var0 != null) {
         double var7 = Math.max(var0.width, var0.height) * 0.75;
         return a(var1, var3, var5, var7 + o());
      } else {
         return false;
      }
   }

   public static boolean a(BlockEntityRenderState var0) {
      if (m() && var0 != null) {
         BlockPos var1 = var0.pos;
         if (var1 == null) {
            return false;
         }

         p();
         if (!cameraValid) {
            return false;
         }

         double var2 = var1.getX() + 0.5 - camX;
         double var4 = var1.getY() + 0.5 - camY;
         double var6 = var1.getZ() + 0.5 - camZ;
         return a(var2, var4, var6, 1.0 + o());
      } else {
         return false;
      }
   }

   public static boolean a(double var0, double var2, double var4) {
      if (!n()) {
         return false;
      }

      p();
      if (!cameraValid) {
         return false;
      }

      double var6 = var0 - camX;
      double var8 = var2 - camY;
      double var10 = var4 - camZ;
      return a(var6, var8, var10, o());
   }
}
