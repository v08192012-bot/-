package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@NewFunction(
   name = "ExstraOptimizer",
   description = "Максимальная оптимизация рендера: отсечение далёких сущностей, блок-энтити и частиц + быстрые настройки для сильного буста FPS",
   category = Category.RENDER
)
public class ExstraOptimizer extends Function {
   private static ExstraOptimizer INSTANCE;
   private final BooleanSetting fastSettings = new BooleanSetting("Быстрые настройки", true);
   private final BooleanSetting cullEntities = new BooleanSetting("Отсечение сущностей", true);
   private final NumberSetting entityDistance = new NumberSetting("Дистанция сущностей", 96.0, 16.0, 512.0, 8.0);
   private final BooleanSetting cullBlockEntities = new BooleanSetting("Отсечение блок-энтити", true);
   private final NumberSetting blockEntityDistance = new NumberSetting("Дистанция блок-энтити", 48.0, 8.0, 256.0, 8.0);
   private final BooleanSetting cullParticles = new BooleanSetting("Отсечение частиц", true);
   private final NumberSetting particleDistance = new NumberSetting("Дистанция частиц", 32.0, 8.0, 128.0, 4.0);
   private final BooleanSetting noCelestial = new BooleanSetting("Без солнца и луны", false);
   private static double camX;
   private static double camY;
   private static double camZ;
   private static boolean cameraValid = false;
   private boolean captured = false;
   private boolean prevShadows = true;
   private boolean prevAo = true;
   private int prevBiomeBlend = 0;
   private ParticlesMode prevParticles = ParticlesMode.ALL;
   private CloudRenderMode prevClouds = CloudRenderMode.FANCY;

   public ExstraOptimizer() {
      INSTANCE = this;
      this.addSettings(
         this.fastSettings,
         this.cullEntities,
         this.entityDistance,
         this.cullBlockEntities,
         this.blockEntityDistance,
         this.cullParticles,
         this.particleDistance,
         this.noCelestial
      );
      this.entityDistance.visibleWhen(this.cullEntities::getValue);
      this.blockEntityDistance.visibleWhen(this.cullBlockEntities::getValue);
      this.particleDistance.visibleWhen(this.cullParticles::getValue);
   }

   public static ExstraOptimizer k() {
      if (INSTANCE == null) {
         INSTANCE = FunctionManager.a(ExstraOptimizer.class);
      }

      return INSTANCE;
   }

   public static boolean sky() {
      ExstraOptimizer var0 = k();
      return var0 != null && var0.isActive() && var0.noCelestial.isToggled();
   }

   public static boolean cullEntity(Entity var0, double var1, double var3, double var5) {
      ExstraOptimizer var7 = k();
      if (var7 != null && var7.isActive() && var7.cullEntities.isToggled() && var0 != null) {
         if (var0 == net.minecraft.client.MinecraftClient.getInstance().player) {
            return false;
         } else {
            double var8 = var0.getX() - var1;
            double var10 = var0.getY() - var3;
            double var12 = var0.getZ() - var5;
            return a(var8, var10, var12, Math.max(var0.getWidth(), var0.getHeight()), var7.entityDistance.getValue());
         }
      } else {
         return false;
      }
   }

   public static boolean cullEntityState(EntityRenderState var0, double var1, double var3, double var5) {
      ExstraOptimizer var7 = k();
      return var7 != null && var7.isActive() && var7.cullEntities.isToggled() && var0 != null
         ? a(var1, var3, var5, Math.max(var0.width, var0.height), var7.entityDistance.getValue())
         : false;
   }

   public static boolean cullBlockEntity(BlockEntityRenderState var0) {
      ExstraOptimizer var1 = k();
      if (var1 != null && var1.isActive() && var1.cullBlockEntities.isToggled() && var0 != null) {
         BlockPos var2 = var0.pos;
         if (var2 == null) {
            return false;
         } else {
            p();
            if (!cameraValid) {
               return false;
            } else {
               double var3 = var2.getX() + 0.5 - camX;
               double var5 = var2.getY() + 0.5 - camY;
               double var7 = var2.getZ() + 0.5 - camZ;
               return a(var3, var5, var7, 0.0, var1.blockEntityDistance.getValue());
            }
         }
      } else {
         return false;
      }
   }

   public static boolean cullParticle(double var0, double var2, double var4) {
      ExstraOptimizer var6 = k();
      if (var6 != null && var6.isActive() && var6.cullParticles.isToggled()) {
         p();
         if (!cameraValid) {
            return false;
         } else {
            return a(var0 - camX, var2 - camY, var4 - camZ, 0.0, var6.particleDistance.getValue());
         }
      } else {
         return false;
      }
   }

   private static boolean a(double var0, double var2, double var4, double var6, double var8) {
      double var10 = var8 + var6;
      return var0 * var0 + var2 * var2 + var4 * var4 > var10 * var10;
   }

   private static void p() {
      if (!cameraValid) {
         net.minecraft.client.MinecraftClient var0 = net.minecraft.client.MinecraftClient.getInstance();
         if (var0.gameRenderer != null && var0.gameRenderer.getCamera() != null) {
            a(var0.gameRenderer.getCamera());
         }
      }
   }

   public static void a(Camera var0) {
      if (var0 == null) {
         cameraValid = false;
      } else {
         Vec3d var1 = var0.getCameraPos();
         camX = var1.x;
         camY = var1.y;
         camZ = var1.z;
         cameraValid = true;
      }
   }

   @Override
   public void onEnable() {
      this.captured = false;
      this.q();
   }

   @Override
   public void onDisable() {
      this.r();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.q();
   }

   private void q() {
      if (this.fastSettings.isToggled() && mc.options != null) {
         if (!this.captured) {
            this.prevShadows = mc.options.getEntityShadows().getValue();
            this.prevAo = mc.options.getAo().getValue();
            this.prevBiomeBlend = mc.options.getBiomeBlendRadius().getValue();
            this.prevParticles = mc.options.getParticles().getValue();
            this.prevClouds = mc.options.getCloudRenderMode().getValue();
            this.captured = true;
         }

         mc.options.getEntityShadows().setValue(false);
         mc.options.getAo().setValue(false);
         mc.options.getBiomeBlendRadius().setValue(0);
         mc.options.getParticles().setValue(ParticlesMode.DECREASED);
         mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
      }
   }

   private void r() {
      if (this.captured && mc.options != null) {
         mc.options.getEntityShadows().setValue(this.prevShadows);
         mc.options.getAo().setValue(this.prevAo);
         mc.options.getBiomeBlendRadius().setValue(this.prevBiomeBlend);
         mc.options.getParticles().setValue(this.prevParticles);
         mc.options.getCloudRenderMode().setValue(this.prevClouds);
      }

      this.captured = false;
   }
}
