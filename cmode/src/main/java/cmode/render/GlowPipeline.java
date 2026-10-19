package cmode.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class GlowPipeline {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer scratchBuffer;
   private static final int UNIFORM_SIZE = 128;
   private static final int UBO_ALIGN = 256;
   private static final int BUFFER_CAPACITY = 16384;
   private static int writeOffset = 0;

   public static void a() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("cmode", "glow"))
               .withVertexShader(Identifier.of("cmode", "glow_vertex"))
               .withFragmentShader(Identifier.of("cmode", "glow_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "GlowPipeline Uniforms", 136, 16384L);
            scratchBuffer = MemoryUtil.memAlloc(128);
         } catch (Exception var1) {
         }
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, int var4, float var5) {
      if (pipeline == null) {
         a();
      }

      if (pipeline != null && uniformBuffer != null && scratchBuffer != null) {
         float var6 = (var4 >> 16 & 0xFF) / 255.0F;
         float var7 = (var4 >> 8 & 0xFF) / 255.0F;
         float var8 = (var4 & 0xFF) / 255.0F;
         float var9 = (var4 >> 24 & 0xFF) / 255.0F;
         ByteBuffer var10 = scratchBuffer;
         var10.clear();
         var10.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         var10.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         var10.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         var10.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         var10.position(64);
         var10.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var5);
         var10.putFloat(var6).putFloat(var7).putFloat(var8).putFloat(var9);
         var10.flip();
         GuiBatch.flush();
         CommandEncoder var11 = RenderSystem.getDevice().createCommandEncoder();
         int var12 = (writeOffset + 256 - 1) / 256 * 256;
         if (var12 + 128 > 16384) {
            var12 = 0;
         }

         GpuBufferSlice var13 = new GpuBufferSlice(uniformBuffer, var12, 128L);
         var11.writeToBuffer(var13, var10);
         writeOffset = var12 + 256;
         Framebuffer var14 = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass var15 = var11.createRenderPass(() -> "GlowPipeline", var14.getColorAttachmentView(), OptionalInt.empty())) {
            var15.setPipeline(pipeline);
            Scissor.apply(var15);
            var15.setUniform("Uniforms", var13);
            var15.draw(0, 6);
         }
      }
   }

   public static void b() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (scratchBuffer != null) {
         MemoryUtil.memFree(scratchBuffer);
         scratchBuffer = null;
      }

      writeOffset = 0;
   }
}
