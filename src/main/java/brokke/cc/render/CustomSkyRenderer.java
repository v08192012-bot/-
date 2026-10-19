package brokke.cc.render;

import brokke.cc.module.render.Ambience;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class CustomSkyRenderer implements AutoCloseable {
   private static final CustomSkyRenderer INSTANCE = new CustomSkyRenderer();
   private static final int UNIFORM_SIZE = 256;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer scratchBuffer;
   private boolean initialized;
   private boolean disabled;

   private CustomSkyRenderer() {
   }

   public static CustomSkyRenderer getInstance() {
      return INSTANCE;
   }

   public void a(Ambience var1) {
      if (!this.disabled && var1 != null) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Framebuffer var3 = var2.getFramebuffer();
            if (var3 != null) {
               GpuTextureView var4 = var3.getColorAttachmentView();
               if (var4 != null) {
                  try {
                     this.c();
                     if (this.disabled || this.pipeline == null || this.uniformBuffer == null || this.scratchBuffer == null) {
                        return;
                     }

                     Matrix4f var5 = new Matrix4f(RenderContext.VIEW_MATRIX).setTranslation(0.0F, 0.0F, 0.0F);
                     Matrix4f var6 = new Matrix4f(RenderContext.PROJECTION_MATRIX).mul(var5).invert();
                     this.scratchBuffer.clear();
                     this.scratchBuffer.putFloat(var6.m00()).putFloat(var6.m01()).putFloat(var6.m02()).putFloat(var6.m03());
                     this.scratchBuffer.putFloat(var6.m10()).putFloat(var6.m11()).putFloat(var6.m12()).putFloat(var6.m13());
                     this.scratchBuffer.putFloat(var6.m20()).putFloat(var6.m21()).putFloat(var6.m22()).putFloat(var6.m23());
                     this.scratchBuffer.putFloat(var6.m30()).putFloat(var6.m31()).putFloat(var6.m32()).putFloat(var6.m33());
                     long var7 = System.currentTimeMillis();
                     float var9 = (float)(var7 % 10000000L) / 1000.0F;
                     this.scratchBuffer.putFloat(var9);
                     this.scratchBuffer.putFloat(var1.l());
                     this.scratchBuffer.putFloat((float)var1.getSkySpeed().getValue());
                     this.scratchBuffer.putFloat((float)var1.getSkyBrightness().getValue());
                     float var10 = 1.0F - var2.world.getRainGradient(1.0F);
                     this.scratchBuffer.putFloat(1.0F);
                     this.scratchBuffer.putFloat(var10);
                     this.scratchBuffer.putFloat(1.0F);
                     this.scratchBuffer.putFloat(0.0F);
                     this.scratchBuffer.flip();
                     GuiBatch.flush();
                     CommandEncoder var11 = RenderSystem.getDevice().createCommandEncoder();
                     var11.writeToBuffer(this.uniformBuffer.slice(), this.scratchBuffer);
                     GpuTextureView var12 = var3.getDepthAttachmentView();

                     try (RenderPass var13 = var12 != null
                           ? var11.createRenderPass(() -> "CustomSkyPass", var4, OptionalInt.empty(), var12, OptionalDouble.empty())
                           : var11.createRenderPass(() -> "CustomSkyPass", var4, OptionalInt.empty())) {
                        var13.setPipeline(this.pipeline);
                        var13.setUniform("SkyUniforms", this.uniformBuffer);
                        var13.draw(0, 6);
                     }
                  } catch (Throwable var18) {
                     this.disabled = true;
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
         try {
            this.pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("brokke", "custom_sky"))
               .withVertexShader(Identifier.of("brokke", "custom_sky_vertex"))
               .withFragmentShader(Identifier.of("brokke", "custom_sky_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("SkyUniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .build();
            this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "CustomSky Uniforms", 136, 256L);
            this.scratchBuffer = MemoryUtil.memAlloc(256);
            this.initialized = true;
         } catch (Throwable var2) {
            this.disabled = true;
         }
      }
   }

   @Override
   public void close() {
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }

      if (this.scratchBuffer != null) {
         MemoryUtil.memFree(this.scratchBuffer);
         this.scratchBuffer = null;
      }

      this.initialized = false;
      this.disabled = false;
   }
}
