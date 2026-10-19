package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class WetSurfaceRenderer implements AutoCloseable {
   private static final WetSurfaceRenderer INSTANCE = new WetSurfaceRenderer();
   private static final float EPSILON = 1.0E-4F;
   private static final int UNIFORM_SIZE = 320;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer uniformData;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private int sceneWidth;
   private int sceneHeight;
   private boolean initialized;
   private boolean disabled;
   private boolean depthDirectWarned;

   private WetSurfaceRenderer() {
   }

   public static WetSurfaceRenderer getInstance() {
      return INSTANCE;
   }

   public void a(MinecraftClient var1, Camera var2, Matrix4f var3, Matrix4f var4, WetSurfaceRenderer.a var5) {
      if (!this.disabled
         && var1 != null
         && var2 != null
         && var4 != null
         && var5 != null
         && var1.world != null
         && var1.player != null
         && !(var5.wetness <= 1.0E-4F)
         && !(var5.reflectionStrength <= 1.0E-4F)) {
         Window var6 = var1.getWindow();
         int var7 = var6.getFramebufferWidth();
         int var8 = var6.getFramebufferHeight();
         if (var7 > 1 && var8 > 1) {
            Framebuffer var9 = var1.getFramebuffer();
            if (var9 != null && var9.getColorAttachment() != null && var9.getDepthAttachment() != null) {
               try {
                  this.c();
                  if (this.disabled || !this.a(var7, var8)) {
                     return;
                  }

                  GuiBatch.flush();
                  CommandEncoder var10 = RenderSystem.getDevice().createCommandEncoder();
                  var10.copyTextureToTexture(var9.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var7, var8);
                  boolean var11 = false;
                  GpuTexture var12 = var9.getDepthAttachment();
                  if ((var12.usage() & 2) != 0) {
                     var10.copyTextureToTexture(var12, this.depthTexture, 0, 0, 0, 0, 0, var7, var8);
                     var11 = true;
                  } else {
                     if ((var12.usage() & 4) == 0) {
                        this.disabled = true;
                        System.err.println("[WetSurface] depth attachment is neither copyable nor bindable, disabling");
                        return;
                     }

                     if (!this.depthDirectWarned) {
                        this.depthDirectWarned = true;
                        System.out.println("[WetSurface] depth attachment not copyable, sampling it directly");
                     }
                  }

                  Matrix4f var13 = var3 != null ? new Matrix4f(var3) : a(var2);
                  Matrix4f var14 = new Matrix4f(var4);
                  Matrix4f var15 = new Matrix4f(var14).invert();
                  Vec3d var16 = var2.getCameraPos();
                  Matrix4f var17 = new Matrix4f().translation((float)var16.x, (float)var16.y, (float)var16.z).mul(new Matrix4f(var13).invert());
                  this.a(var7, var8, var13, var14, var15, var17, var5);
                  var10.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                  GpuSampler var18 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                  GpuSampler var19 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                  GpuTextureView var20 = var11 ? this.depthTextureView : var9.getDepthAttachmentView();

                  try (RenderPass var21 = var11
                        ? var10.createRenderPass(
                           () -> "WetSurfaceRenderer",
                           var9.getColorAttachmentView(),
                           OptionalInt.empty(),
                           var9.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )
                        : var10.createRenderPass(() -> "WetSurfaceRenderer", var9.getColorAttachmentView(), OptionalInt.empty())) {
                     var21.setPipeline(this.pipeline);
                     var21.setUniform("WetData", this.uniformBuffer);
                     var21.bindTexture("Sampler0", this.sceneTextureView, var18);
                     var21.bindTexture("Sampler1", var20, var19);
                     var21.draw(0, 6);
                  }
               } catch (Throwable var26) {
                  this.disabled = true;
                  System.err.println("[WetSurface] renderer disabled: " + var26.getMessage());
               }
            }
         }
      }
   }

   public void b() {
      this.disabled = false;
   }

   private void c() {
      if (!this.initialized) {
         this.pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "wet_surface"))
            .withVertexShader(Identifier.of("brokke", "wet_surface_vertex"))
            .withFragmentShader(Identifier.of("brokke", "wet_surface_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("WetData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "WetSurfaceRenderer Uniforms", 136, 320L);
         this.uniformData = MemoryUtil.memAlloc(320);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.depthTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      if (this.sceneTextureView != null) {
         this.sceneTextureView.close();
         this.sceneTextureView = null;
      }

      if (this.sceneTexture != null) {
         this.sceneTexture.close();
         this.sceneTexture = null;
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }

      this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "brokke:wet_surface_scene", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.depthTexture = RenderSystem.getDevice().createTexture(() -> "brokke:wet_surface_depth", 13, TextureFormat.DEPTH32, var1, var2, 1, 1);
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void a(int var1, int var2, Matrix4f var3, Matrix4f var4, Matrix4f var5, Matrix4f var6, WetSurfaceRenderer.a var7) {
      this.uniformData.clear();
      float var8 = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
      a(this.uniformData, var3);
      a(this.uniformData, var4);
      a(this.uniformData, var5);
      a(this.uniformData, var6);
      this.uniformData.putFloat(var1).putFloat(var2).putFloat(var8).putFloat(0.0F);
      this.uniformData.putFloat(a(var7.wetness, 0.0F, 2.0F));
      this.uniformData.putFloat(a(var7.puddleCoverage, 0.0F, 1.0F));
      this.uniformData.putFloat(a(var7.reflectionStrength, 0.0F, 2.5F));
      this.uniformData.putFloat(a(var7.rippleStrength, 0.0F, 2.0F));
      this.uniformData.putFloat(a(var7.maxDistance, 4.0F, 128.0F));
      this.uniformData.putFloat(a(var7.rainAmount, 0.0F, 2.0F));
      this.uniformData.putFloat(Math.max(4, Math.min(16, var7.reflectionSteps)));
      this.uniformData.putFloat(var7.ripples ? 1.0F : 0.0F);
      this.uniformData.putFloat(a(var7.skyR, 0.0F, 4.0F)).putFloat(a(var7.skyG, 0.0F, 4.0F)).putFloat(a(var7.skyB, 0.0F, 4.0F)).putFloat(0.0F);
      this.uniformData.flip();
   }

   private static void a(ByteBuffer var0, Matrix4f var1) {
      var0.putFloat(var1.m00()).putFloat(var1.m01()).putFloat(var1.m02()).putFloat(var1.m03());
      var0.putFloat(var1.m10()).putFloat(var1.m11()).putFloat(var1.m12()).putFloat(var1.m13());
      var0.putFloat(var1.m20()).putFloat(var1.m21()).putFloat(var1.m22()).putFloat(var1.m23());
      var0.putFloat(var1.m30()).putFloat(var1.m31()).putFloat(var1.m32()).putFloat(var1.m33());
   }

   private static Matrix4f a(Camera var0) {
      Vec3d var1 = var0.getCameraPos();
      double var2 = Math.toRadians(var0.getYaw());
      double var4 = Math.toRadians(var0.getPitch());
      double var6 = Math.cos(var2);
      double var8 = Math.sin(var2);
      double var10 = Math.cos(var4);
      double var12 = Math.sin(var4);
      Vec3d var14 = new Vec3d(-var8 * var10, -var12, var6 * var10);
      Vec3d var15 = var1.add(var14);
      return new Matrix4f().lookAt((float)var1.x, (float)var1.y, (float)var1.z, (float)var15.x, (float)var15.y, (float)var15.z, 0.0F, 1.0F, 0.0F);
   }

   private static float a(float var0, float var1, float var2) {
      return !Float.isFinite(var0) ? var1 : Math.max(var1, Math.min(var2, var0));
   }

   @Override
   public void close() {
      if (this.sceneTextureView != null) {
         this.sceneTextureView.close();
         this.sceneTextureView = null;
      }

      if (this.sceneTexture != null) {
         this.sceneTexture.close();
         this.sceneTexture = null;
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }

      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }

      if (this.uniformData != null) {
         MemoryUtil.memFree(this.uniformData);
         this.uniformData = null;
      }

      this.initialized = false;
      this.disabled = false;
      this.depthDirectWarned = false;
      this.sceneWidth = 0;
      this.sceneHeight = 0;
   }

   public static final class a {
      public float wetness = 1.0F;
      public float puddleCoverage = 0.82F;
      public float reflectionStrength = 1.25F;
      public float maxDistance = 56.0F;
      public float rippleStrength = 0.8F;
      public float rainAmount = 1.0F;
      public int reflectionSteps = 10;
      public boolean ripples = true;
      public float skyR = 0.6F;
      public float skyG = 0.7F;
      public float skyB = 0.85F;
   }
}
