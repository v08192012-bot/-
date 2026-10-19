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
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class CustomFogRenderer implements AutoCloseable {
   private static final CustomFogRenderer INSTANCE = new CustomFogRenderer();
   private static final int UNIFORM_SIZE = 128;
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

   private CustomFogRenderer() {
   }

   public static CustomFogRenderer getInstance() {
      return INSTANCE;
   }

   public void a(MinecraftClient var1, Matrix4f var2, CustomFogRenderer.a var3) {
      if (!this.disabled && var1 != null && var2 != null && var3 != null && var1.world != null && var1.player != null && !(var3.strength <= 1.0E-4F)) {
         Window var4 = var1.getWindow();
         if (var4 != null) {
            int var5 = var4.getFramebufferWidth();
            int var6 = var4.getFramebufferHeight();
            if (var5 > 1 && var6 > 1) {
               Framebuffer var7 = var1.getFramebuffer();
               if (var7 != null && var7.getColorAttachment() != null && var7.getDepthAttachment() != null) {
                  try {
                     this.c();
                     if (this.disabled || !this.a(var5, var6)) {
                        return;
                     }

                     GuiBatch.flush();
                     CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
                     var8.copyTextureToTexture(var7.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var5, var6);
                     boolean var9 = false;
                     GpuTexture var10 = var7.getDepthAttachment();
                     if ((var10.usage() & 2) != 0) {
                        var8.copyTextureToTexture(var10, this.depthTexture, 0, 0, 0, 0, 0, var5, var6);
                        var9 = true;
                     } else {
                        if ((var10.usage() & 4) == 0) {
                           this.disabled = true;
                           System.err.println("[CustomFog] depth attachment is neither copyable nor bindable, disabling");
                           return;
                        }

                        if (!this.depthDirectWarned) {
                           this.depthDirectWarned = true;
                           System.out.println("[CustomFog] depth attachment not copyable, sampling it directly");
                        }
                     }

                     this.a(var5, var6, var2, var3);
                     var8.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                     GpuSampler var11 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                     GpuSampler var12 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                     GpuTextureView var13 = var9 ? this.depthTextureView : var7.getDepthAttachmentView();

                     try (RenderPass var14 = var9
                           ? var8.createRenderPass(
                              () -> "CustomFogRenderer",
                              var7.getColorAttachmentView(),
                              OptionalInt.empty(),
                              var7.getDepthAttachmentView(),
                              OptionalDouble.empty()
                           )
                           : var8.createRenderPass(() -> "CustomFogRenderer", var7.getColorAttachmentView(), OptionalInt.empty())) {
                        var14.setPipeline(this.pipeline);
                        var14.setUniform("FogData", this.uniformBuffer);
                        var14.bindTexture("Sampler0", this.sceneTextureView, var11);
                        var14.bindTexture("Sampler1", var13, var12);
                        var14.draw(0, 6);
                     }
                  } catch (Throwable var19) {
                     this.disabled = true;
                     System.err.println("[CustomFog] renderer disabled: " + var19.getMessage());
                  }
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
            .withLocation(Identifier.of("brokke", "custom_fog"))
            .withVertexShader(Identifier.of("brokke", "custom_fog_vertex"))
            .withFragmentShader(Identifier.of("brokke", "custom_fog_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("FogData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "CustomFogRenderer Uniforms", 136, 128L);
         this.uniformData = MemoryUtil.memAlloc(128);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.depthTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.d();
      this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "brokke:custom_fog_scene", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.depthTexture = RenderSystem.getDevice().createTexture(() -> "brokke:custom_fog_depth", 13, TextureFormat.DEPTH32, var1, var2, 1, 1);
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void a(int var1, int var2, Matrix4f var3, CustomFogRenderer.a var4) {
      Matrix4f var5 = new Matrix4f(var3).invert();
      this.uniformData.clear();
      this.uniformData.putFloat(var5.m00()).putFloat(var5.m01()).putFloat(var5.m02()).putFloat(var5.m03());
      this.uniformData.putFloat(var5.m10()).putFloat(var5.m11()).putFloat(var5.m12()).putFloat(var5.m13());
      this.uniformData.putFloat(var5.m20()).putFloat(var5.m21()).putFloat(var5.m22()).putFloat(var5.m23());
      this.uniformData.putFloat(var5.m30()).putFloat(var5.m31()).putFloat(var5.m32()).putFloat(var5.m33());
      this.uniformData.putFloat(var1).putFloat(var2).putFloat(0.0F).putFloat(0.0F);
      this.uniformData.putFloat(a(var4.red, 0.0F, 1.0F)).putFloat(a(var4.green, 0.0F, 1.0F)).putFloat(a(var4.blue, 0.0F, 1.0F)).putFloat(1.0F);
      this.uniformData
         .putFloat(a(var4.start, 0.0F, 512.0F))
         .putFloat(a(var4.dispersion, 0.01F, 512.0F))
         .putFloat(a(var4.strength, 0.0F, 1.0F))
         .putFloat(a(var4.blurRadius, 0.0F, 64.0F));
      this.uniformData.putFloat(var4.colorMode ? 1.0F : 0.0F).putFloat(var4.fogSky ? 1.0F : 0.0F).putFloat(0.0F).putFloat(0.0F);
      this.uniformData.flip();
   }

   private static float a(float var0, float var1, float var2) {
      return !Float.isFinite(var0) ? var1 : Math.max(var1, Math.min(var2, var0));
   }

   private void d() {
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

      this.sceneWidth = 0;
      this.sceneHeight = 0;
   }

   @Override
   public void close() {
      this.d();
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
   }

   public static final class a {
      public boolean colorMode;
      public boolean fogSky = true;
      public float start = 16.0F;
      public float dispersion = 64.0F;
      public float strength = 0.9F;
      public float blurRadius = 10.0F;
      public float red = 0.49F;
      public float green = 0.36F;
      public float blue = 1.0F;
   }
}
