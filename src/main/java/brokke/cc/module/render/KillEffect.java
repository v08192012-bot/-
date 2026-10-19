package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.RenderContext;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

@NewFunction(name = "KillEffect", description = "Торнадо на месте убийства", category = Category.RENDER)
public class KillEffect extends Function {
   private static KillEffect instance;
   private static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static RenderPipeline tornadoPipeline;
   private static GpuBuffer matrixUniformBuffer;
   private static GpuBuffer beamUniformBuffer;
   private static GpuBuffer tornadoVbo;
   private static ByteBuffer scratchMatrix;
   private static ByteBuffer scratchBeam;
   private static final int MATRIX_SIZE = 64;
   private static final int BEAM_SIZE = 48;
   private static final int MAX_EFFECTS = 16;
   private final BooleanSetting syncTheme = new BooleanSetting("Цвет темы", true);
   private final ColorSetting customColor = new ColorSetting("Цвет", new Color(130, 80, 255));
   private final NumberSetting size = new NumberSetting("Размер", 2.5, 0.5, 6.0, 0.1);
   private final NumberSetting intensity = new NumberSetting("Интенсивность", 1.0, 0.1, 3.0, 0.1);
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.2, 3.0, 0.1);
   private final NumberSetting lifetime = new NumberSetting("Длительность", 2.0, 0.5, 5.0, 0.1);
   private final List<KillEffect.a> activeEffects = new CopyOnWriteArrayList<>();
   private final Set<Integer> knownAlive = new HashSet<>();

   public KillEffect() {
      instance = this;
      this.addSettings(this.syncTheme, this.customColor, this.size, this.intensity, this.speed, this.lifetime);
      this.syncTheme.onChanged(() -> this.customColor.setVisible(!this.syncTheme.isToggled()));
   }

   public static KillEffect k() {
      if (instance == null) {
         instance = FunctionManager.a(KillEffect.class);
      }

      return instance;
   }

   private static void a(WorldRenderContext var0) {
      KillEffect var1 = k();
      if (var1 != null && var1.isActive()) {
         var1.b(var0);
      }
   }

   public static void a(Vec3d var0) {
      KillEffect var1 = k();
      if (var1 != null && var1.isActive()) {
         var1.b(var0);
      }
   }

   public void b(Vec3d var1) {
      if (var1 != null) {
         Color var2 = this.syncTheme.isToggled() ? Theme.getInstance().getAccent() : this.customColor.getColor();
         this.activeEffects
            .add(
               new KillEffect.a(
                  var1,
                  System.currentTimeMillis(),
                  var2,
                  (float)this.size.getValue(),
                  (float)this.intensity.getValue(),
                  (float)this.speed.getValue(),
                  (float)this.lifetime.getValue()
               )
            );

         while (this.activeEffects.size() > 16) {
            this.activeEffects.remove(0);
         }
      }
   }

   @Override
   public void onDisable() {
      this.activeEffects.clear();
      this.knownAlive.clear();
      super.onDisable();
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.world != null && mc.player != null && var1.getPacket() instanceof EntityStatusS2CPacket var2 && var2.getStatus() == 3) {
         Entity var4 = var2.getEntity(mc.world);
         if (var4 instanceof LivingEntity && var4 != mc.player) {
            this.b(var4.getEntityPos());
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.world != null && mc.player != null) {
         for (Entity var3 : mc.world.getEntities()) {
            if (var3 instanceof LivingEntity var4 && var3 != mc.player) {
               if ((var4.isDead() || var4.getHealth() <= 0.0F) && this.knownAlive.remove(var3.getId())) {
                  this.b(var3.getEntityPos());
               } else if (!var4.isDead() && var4.getHealth() > 0.0F) {
                  this.knownAlive.add(var3.getId());
               }
            }
         }

         long var5 = System.currentTimeMillis();
         this.activeEffects.removeIf(var2 -> (float)(var5 - var2.startTime) > var2.lifetimeSec * 1000.0F);
      } else {
         this.knownAlive.clear();
      }
   }

   private void b(WorldRenderContext var1) {
      if (!this.activeEffects.isEmpty() && mc.player != null && mc.world != null) {
         l();
         if (tornadoPipeline != null && matrixUniformBuffer != null && beamUniformBuffer != null) {
            Matrix4f var2 = this.m();
            Vec3d var3 = mc.gameRenderer.getCamera().getCameraPos();
            Camera var4 = mc.gameRenderer.getCamera();
            long var5 = System.currentTimeMillis();
            if (scratchMatrix == null) {
               scratchMatrix = MemoryUtil.memAlloc(64);
            }

            scratchMatrix.clear();
            scratchMatrix.putFloat(var2.m00()).putFloat(var2.m01()).putFloat(var2.m02()).putFloat(var2.m03());
            scratchMatrix.putFloat(var2.m10()).putFloat(var2.m11()).putFloat(var2.m12()).putFloat(var2.m13());
            scratchMatrix.putFloat(var2.m20()).putFloat(var2.m21()).putFloat(var2.m22()).putFloat(var2.m23());
            scratchMatrix.putFloat(var2.m30()).putFloat(var2.m31()).putFloat(var2.m32()).putFloat(var2.m33());
            scratchMatrix.flip();
            CommandEncoder var7 = RenderSystem.getDevice().createCommandEncoder();
            var7.writeToBuffer(matrixUniformBuffer.slice(), scratchMatrix);
            Framebuffer var8 = mc.getFramebuffer();

            for (KillEffect.a var10 : this.activeEffects) {
               float var11 = (float)(var5 - var10.startTime) / 1000.0F;
               float var12 = var11 / Math.max(0.1F, var10.lifetimeSec);
               if (!(var12 >= 1.0F)) {
                  float var13 = var11 * var10.speed;
                  Color var14 = this.syncTheme.isToggled() ? Theme.getInstance().getAccent() : var10.color;
                  float var15 = var14.getRed() / 255.0F;
                  float var16 = var14.getGreen() / 255.0F;
                  float var17 = var14.getBlue() / 255.0F;
                  float var18 = var14.getAlpha() / 255.0F;
                  if (scratchBeam == null) {
                     scratchBeam = MemoryUtil.memAlloc(48);
                  }

                  scratchBeam.clear();
                  scratchBeam.putFloat(var13);
                  scratchBeam.putFloat(var12);
                  scratchBeam.putFloat(0.0F);
                  scratchBeam.putFloat(0.0F);
                  scratchBeam.putFloat(var15);
                  scratchBeam.putFloat(var16);
                  scratchBeam.putFloat(var17);
                  scratchBeam.putFloat(var18);
                  scratchBeam.putFloat(var10.intensity);
                  scratchBeam.putFloat(var10.size);
                  scratchBeam.putFloat(0.0F);
                  scratchBeam.putFloat(0.0F);
                  scratchBeam.flip();
                  var7.writeToBuffer(beamUniformBuffer.slice(), scratchBeam);
                  BufferBuilder var19 = Tessellator.getInstance().begin(DrawMode.TRIANGLES, FORMAT);
                  this.a(var19, var3, var4, var10.pos, var10.size);
                  BuiltBuffer var20 = null;

                  try {
                     var20 = var19.end();
                     int var21 = var20.getDrawParameters().indexCount();
                     if (var21 > 0) {
                        int var22 = var20.getBuffer().remaining();
                        if (tornadoVbo == null || tornadoVbo.size() < var22) {
                           if (tornadoVbo != null) {
                              tornadoVbo.close();
                           }

                           tornadoVbo = RenderSystem.getDevice().createBuffer(() -> "KillEffect VBO", 40, Math.max(var22, 4096));
                        }

                        var7.writeToBuffer(tornadoVbo.slice(0L, var22), var20.getBuffer());
                        ShapeIndexBuffer var23 = RenderSystem.getSequentialBuffer(DrawMode.TRIANGLES);
                        GpuBuffer var24 = var23.getIndexBuffer(var21);

                        try (RenderPass var25 = var7.createRenderPass(
                              () -> "KillEffect", var8.getColorAttachmentView(), OptionalInt.empty(), var8.getDepthAttachmentView(), OptionalDouble.empty()
                           )) {
                           var25.setPipeline(tornadoPipeline);
                           var25.setVertexBuffer(0, tornadoVbo);
                           var25.setIndexBuffer(var24, var23.getIndexType());
                           var25.setUniform("MatrixData", matrixUniformBuffer);
                           var25.setUniform("BeamData", beamUniformBuffer);
                           var25.drawIndexed(0, 0, var21, 1);
                        }
                     }
                  } catch (Exception var35) {
                  } finally {
                     if (var20 != null) {
                        var20.close();
                     }
                  }
               }
            }
         }
      }
   }

   private void a(BufferBuilder var1, Vec3d var2, Camera var3, Vec3d var4, float var5) {
      float var6 = var5 * 0.9F;
      float var7 = var5 * 2.6F;
      double var8 = var4.x;
      double var10 = var4.y;
      double var12 = var4.z;
      Vector3f var14 = new Vector3f(0.0F, 1.0F, 0.0F);
      Vector3f var15 = new Vector3f(1.0F, 0.0F, 0.0F).rotate(var3.getRotation());
      var15.y = 0.0F;
      if (var15.lengthSquared() > 1.0E-4F) {
         var15.normalize();
      } else {
         var15.set(1.0F, 0.0F, 0.0F);
      }

      this.a(var1, var2, var8, var10, var12, var15, var14, var6, var7, -1);
      Vector3f var16 = new Vector3f(-var15.z, 0.0F, var15.x);
      this.a(var1, var2, var8, var10, var12, var16, var14, var6, var7, -1);
   }

   private void a(BufferBuilder var1, Vec3d var2, double var3, double var5, double var7, Vector3f var9, Vector3f var10, float var11, float var12, int var13) {
      double var14 = var9.x * (var11 * 0.5F);
      double var16 = var9.z * (var11 * 0.5F);
      double var18 = var3 - var14;
      double var20 = var5;
      double var22 = var7 - var16;
      double var24 = var3 + var14;
      double var26 = var5;
      double var28 = var7 + var16;
      double var30 = var3 + var14;
      double var32 = var5 + var12;
      double var34 = var7 + var16;
      double var36 = var3 - var14;
      double var38 = var5 + var12;
      double var40 = var7 - var16;
      this.a(var1, var2, var18, var20, var22, 0.0F, 0.0F, var13);
      this.a(var1, var2, var24, var26, var28, 1.0F, 0.0F, var13);
      this.a(var1, var2, var30, var32, var34, 1.0F, 1.0F, var13);
      this.a(var1, var2, var18, var20, var22, 0.0F, 0.0F, var13);
      this.a(var1, var2, var30, var32, var34, 1.0F, 1.0F, var13);
      this.a(var1, var2, var36, var38, var40, 0.0F, 1.0F, var13);
   }

   private void a(BufferBuilder var1, Vec3d var2, double var3, double var5, double var7, float var9, float var10, int var11) {
      var1.vertex((float)(var3 - var2.x), (float)(var5 - var2.y), (float)(var7 - var2.z))
         .texture(var9, var10)
         .color(var11 >> 16 & 0xFF, var11 >> 8 & 0xFF, var11 & 0xFF, var11 >> 24 & 0xFF);
   }

   private static void l() {
      if (tornadoPipeline == null) {
         tornadoPipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "kill_effect_tornado"))
            .withVertexShader(Identifier.of("brokke", "kill_effect_vertex"))
            .withFragmentShader(Identifier.of("brokke", "kill_effect_tornado"))
            .withVertexFormat(FORMAT, DrawMode.TRIANGLES)
            .withUniform("MatrixData", UniformType.UNIFORM_BUFFER)
            .withUniform("BeamData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withDepthBias(-1.0F, -10.0F)
            .withCull(false)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
            .build();
         matrixUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "KillEffect Matrix Uniforms", 136, 64L);
         beamUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "KillEffect Beam Uniforms", 136, 48L);
      }
   }

   private Matrix4f m() {
      if (RenderContext.WORLD_READY) {
         return new Matrix4f(RenderContext.PROJECTION_MATRIX).mul(RenderContext.VIEW_MATRIX);
      }

      Camera var1 = mc.gameRenderer.getCamera();
      float var2 = mc.getRenderTickCounter().getTickProgress(true);
      Matrix4f var3 = new Matrix4f().rotation(var1.getRotation());
      float var4 = (float)this.n();
      float var5 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(var1, var2, true);
      Matrix4f var6 = new Matrix4f().perspective((float)Math.toRadians(var5), var4, 0.05F, 1000.0F);
      return var6.mul(var3);
   }

   private double n() {
      Window var1 = mc.getWindow();
      return (double)var1.getFramebufferWidth() / Math.max(1, var1.getFramebufferHeight());
   }

   static {
      WorldRenderEvents.AFTER_ENTITIES.register(KillEffect::a);
   }

   record a(Vec3d pos, long startTime, Color color, float size, float intensity, float speed, float lifetimeSec) {
      public Vec3d getPos() {
         return this.pos;
      }

      public long getStartTime() {
         return this.startTime;
      }

      public Color getColor() {
         return this.color;
      }

      public float getSize() {
         return this.size;
      }

      public float getIntensity() {
         return this.intensity;
      }

      public float getSpeed() {
         return this.speed;
      }

      public float getLifetimeSec() {
         return this.lifetimeSec;
      }
   }
}
