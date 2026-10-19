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
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public class NoiseShader implements AutoCloseable {
   private static final int UNIFORM_SIZE = 64;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer uniformData;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private int sceneWidth;
   private int sceneHeight;
   private boolean initialized;
   private boolean hasDepthSnapshot;

   public void a() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null) {
         Window var2 = var1.getWindow();
         if (var2 != null) {
            int var3 = var2.getFramebufferWidth();
            int var4 = var2.getFramebufferHeight();
            if (var3 > 1 && var4 > 1) {
               Framebuffer var5 = var1.getFramebuffer();
               if (var5 != null && var5.getDepthAttachment() != null) {
                  try {
                     this.b();
                     if (!this.a(var3, var4)) {
                        return;
                     }

                     GpuTexture var6 = var5.getDepthAttachment();
                     if ((var6.usage() & 2) != 0) {
                        GuiBatch.flush();
                        CommandEncoder var7 = RenderSystem.getDevice().createCommandEncoder();
                        var7.copyTextureToTexture(var6, this.depthTexture, 0, 0, 0, 0, 0, var3, var4);
                        this.hasDepthSnapshot = true;
                     }
                  } catch (Throwable var8) {
                  }
               }
            }
         }
      }
   }

   public void a(float[] var1) {
      this.a(var1, 0, 1.0F, 1.0F, false);
   }

   public void a(float[] var1, int var2, float var3, float var4, boolean var5) {
      if (this.hasDepthSnapshot) {
         this.hasDepthSnapshot = false;
         MinecraftClient var6 = MinecraftClient.getInstance();
         if (var6 != null) {
            Window var7 = var6.getWindow();
            if (var7 != null) {
               int var8 = var7.getFramebufferWidth();
               int var9 = var7.getFramebufferHeight();
               if (var8 > 1 && var9 > 1) {
                  Framebuffer var10 = var6.getFramebuffer();
                  if (var10 != null && var10.getColorAttachment() != null && var10.getDepthAttachment() != null) {
                     try {
                        this.b();
                        if (this.pipeline == null || this.depthTextureView == null) {
                           return;
                        }

                        float var11 = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
                        float var12 = (float)var8 / Math.max(1, var9);
                        float var13 = 1.0F / var8;
                        float var14 = 1.0F / var9;
                        this.uniformData.clear();
                        this.uniformData.putFloat(var1[0]).putFloat(var1[1]).putFloat(var1[2]).putFloat(var1[3]);
                        this.uniformData.putFloat(var11).putFloat(var1[3]).putFloat(var2).putFloat(var3);
                        this.uniformData.putFloat(var4).putFloat(var5 ? 1.0F : 0.0F).putFloat(var12).putFloat(var13);
                        this.uniformData.putFloat(var14).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
                        this.uniformData.flip();
                        GuiBatch.flush();
                        CommandEncoder var15 = RenderSystem.getDevice().createCommandEncoder();
                        var15.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                        GpuSampler var16 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                        GpuSampler var17 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                        try (RenderPass var18 = var15.createRenderPass(() -> "NoiseShader", var10.getColorAttachmentView(), OptionalInt.empty())) {
                           var18.setPipeline(this.pipeline);
                           var18.setUniform("NoiseData", this.uniformBuffer);
                           var18.bindTexture("Sampler0", var10.getColorAttachmentView(), var17);
                           var18.bindTexture("Sampler1", var10.getDepthAttachmentView(), var16);
                           var18.bindTexture("Sampler2", this.depthTextureView, var16);
                           var18.draw(0, 6);
                        }
                     } catch (Throwable var23) {
                     }
                  }
               }
            }
         }
      }
   }

   private void b() {
      if (!this.initialized) {
         this.pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "noise_hand"))
            .withVertexShader(Identifier.of("brokke", "noise_hand_vertex"))
            .withFragmentShader(Identifier.of("brokke", "noise_hand_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("NoiseData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "NoiseShader Uniforms", 136, 64L);
         this.uniformData = MemoryUtil.memAlloc(64);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.depthTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.c();
      this.depthTexture = RenderSystem.getDevice().createTexture(() -> "brokke:noise_depth", 13, TextureFormat.DEPTH32, var1, var2, 1, 1);
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void c() {
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
      this.c();
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }

      if (this.uniformData != null) {
         MemoryUtil.memFree(this.uniformData);
         this.uniformData = null;
      }

      this.initialized = false;
      this.hasDepthSnapshot = false;
   }
}
