package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
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
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class ColorGradingRenderer implements AutoCloseable {
   private static final ColorGradingRenderer INSTANCE = new ColorGradingRenderer();
   private static final int UNIFORM_SIZE = 16;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer uniformData;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private int sceneWidth;
   private int sceneHeight;
   private boolean initialized;
   private boolean disabled;

   private ColorGradingRenderer() {
   }

   public static ColorGradingRenderer getInstance() {
      return INSTANCE;
   }

   public static void a(float var0, float var1) {
      INSTANCE.a(MinecraftClient.getInstance(), var0, var1);
   }

   public void b() {
      this.disabled = false;
   }

   public void a(MinecraftClient var1, float var2, float var3) {
      if (!this.disabled && var1 != null && var1.world != null && var1.player != null) {
         Window var4 = var1.getWindow();
         if (var4 != null) {
            int var5 = var4.getFramebufferWidth();
            int var6 = var4.getFramebufferHeight();
            if (var5 > 1 && var6 > 1) {
               Framebuffer var7 = var1.getFramebuffer();
               if (var7 != null && var7.getColorAttachment() != null) {
                  try {
                     this.c();
                     if (this.disabled || !this.a(var5, var6)) {
                        return;
                     }

                     GuiBatch.flush();
                     CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
                     var8.copyTextureToTexture(var7.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var5, var6);
                     this.a(var5, var6, var2, var3);
                     var8.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                     GpuSampler var9 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                     try (RenderPass var10 = var8.createRenderPass(() -> "ColorGradingRenderer", var7.getColorAttachmentView(), OptionalInt.empty())) {
                        var10.setPipeline(this.pipeline);
                        var10.setUniform("ColorGradingData", this.uniformBuffer);
                        var10.bindTexture("Sampler0", this.sceneTextureView, var9);
                        var10.draw(0, 6);
                     }
                  } catch (Throwable var15) {
                     this.disabled = true;
                     System.err.println("[ColorGrading] renderer disabled: " + var15.getMessage());
                  }
               }
            }
         }
      }
   }

   private void c() {
      if (!this.initialized) {
         this.pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "colorgrading"))
            .withVertexShader(Identifier.of("brokke", "colorgrading_vertex"))
            .withFragmentShader(Identifier.of("brokke", "colorgrading_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("ColorGradingData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withoutBlend()
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ColorGradingRenderer Uniforms", 136, 16L);
         this.uniformData = MemoryUtil.memAlloc(16);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.d();
      this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "brokke:colorgrading_scene", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void a(int var1, int var2, float var3, float var4) {
      this.uniformData.clear();
      this.uniformData.putFloat(var3).putFloat(var4).putFloat(var1).putFloat(var2);
      this.uniformData.flip();
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
   }
}
