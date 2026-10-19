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

public final class GhostDistortionRenderer implements AutoCloseable {
   public static final int MAX_CENTERS = 8;
   private static final GhostDistortionRenderer INSTANCE = new GhostDistortionRenderer();
   private static final int UNIFORM_SIZE = 160;
   private static final float FREQUENCY = 1.0F;
   private static final float SPEED = 3.5F;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer uniformData;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private int sceneWidth;
   private int sceneHeight;
   private boolean initialized;
   private boolean disabled;

   private GhostDistortionRenderer() {
   }

   public static GhostDistortionRenderer getInstance() {
      return INSTANCE;
   }

   public void b() {
      this.disabled = false;
   }

   public static float a(float var0, float var1) {
      float var2 = Math.max(Math.max(-var0, var0 - 1.0F), 0.0F);
      float var3 = Math.max(Math.max(-var1, var1 - 1.0F), 0.0F);
      return 1.0F - Math.clamp(Math.max(var2, var3) / 0.35F, 0.0F, 1.0F);
   }

   public void a(MinecraftClient var1, float[] var2, int var3, float var4) {
      if (!this.disabled && var1 != null && var2 != null && var3 > 0 && !(var4 <= 1.0E-6F) && var1.world != null && var1.player != null) {
         int var5 = Math.min(var3, 8);
         if (var2.length >= var5 * 4) {
            Window var6 = var1.getWindow();
            if (var6 != null) {
               int var7 = var6.getFramebufferWidth();
               int var8 = var6.getFramebufferHeight();
               if (var7 > 1 && var8 > 1) {
                  Framebuffer var9 = var1.getFramebuffer();
                  if (var9 != null && var9.getColorAttachment() != null) {
                     try {
                        this.c();
                        if (this.disabled || !this.a(var7, var8)) {
                           return;
                        }

                        GuiBatch.flush();
                        CommandEncoder var10 = RenderSystem.getDevice().createCommandEncoder();
                        var10.copyTextureToTexture(var9.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var7, var8);
                        this.a(var7, var8, var2, var5, var4);
                        var10.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                        GpuSampler var11 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                        try (RenderPass var12 = var10.createRenderPass(() -> "GhostDistortionRenderer", var9.getColorAttachmentView(), OptionalInt.empty())) {
                           var12.setPipeline(this.pipeline);
                           var12.setUniform("DistortData", this.uniformBuffer);
                           var12.bindTexture("Sampler0", this.sceneTextureView, var11);
                           var12.draw(0, 6);
                        }
                     } catch (Throwable var17) {
                        this.disabled = true;
                        System.err.println("[GhostDistortion] renderer disabled: " + var17.getMessage());
                     }
                  }
               }
            }
         }
      }
   }

   private void c() {
      if (!this.initialized) {
         this.pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "ghost_distort"))
            .withVertexShader(Identifier.of("brokke", "ghost_distort_vertex"))
            .withFragmentShader(Identifier.of("brokke", "ghost_distort_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("DistortData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "GhostDistortionRenderer Uniforms", 136, 160L);
         this.uniformData = MemoryUtil.memAlloc(160);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.d();
      this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "brokke:ghost_distort_scene", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void a(int var1, int var2, float[] var3, int var4, float var5) {
      float var6 = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
      this.uniformData.clear();
      this.uniformData.putFloat(var1).putFloat(var2).putFloat(var6).putFloat(var4);
      this.uniformData.putFloat(var5).putFloat(1.0F).putFloat(3.5F).putFloat(0.0F);

      for (int var7 = 0; var7 < 8; var7++) {
         if (var7 < var4) {
            this.uniformData.putFloat(var3[var7 * 4]).putFloat(var3[var7 * 4 + 1]).putFloat(var3[var7 * 4 + 2]).putFloat(var3[var7 * 4 + 3]);
         } else {
            this.uniformData.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         }
      }

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
