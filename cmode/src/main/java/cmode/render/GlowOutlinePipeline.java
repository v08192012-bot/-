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

public class GlowOutlinePipeline {
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
               .withLocation(Identifier.of("cmode", "glow_outline"))
               .withVertexShader(Identifier.of("cmode", "glow_outline_vertex"))
               .withFragmentShader(Identifier.of("cmode", "glow_outline_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "GlowOutlinePipeline Uniforms", 136, 16384L);
            scratchBuffer = MemoryUtil.memAlloc(128);
         } catch (Exception var1) {
         }
      }
   }

   public static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      float var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      if ((var6 >> 24 & 0xFF) != 0 && !(var3 <= 0.0F) && !(var4 <= 0.0F) && (!(var9 <= 0.0F) || !(var11 <= 0.0F))) {
         if (pipeline == null) {
            a();
         }

         if (pipeline != null && uniformBuffer != null && scratchBuffer != null) {
            float var13 = (var6 >> 16 & 0xFF) / 255.0F;
            float var14 = (var6 >> 8 & 0xFF) / 255.0F;
            float var15 = (var6 & 0xFF) / 255.0F;
            float var16 = (var6 >> 24 & 0xFF) / 255.0F;
            ByteBuffer var17 = scratchBuffer;
            var17.clear();
            var17.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
            var17.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
            var17.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
            var17.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
            var17.position(64);
            var17.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
            var17.putFloat(var7).putFloat(var5).putFloat(var9).putFloat(var11);
            var17.putFloat(var13).putFloat(var14).putFloat(var15).putFloat(var16);
            var17.putFloat(var12).putFloat(var8).putFloat(var10).putFloat(0.0F);
            var17.flip();
            GuiBatch.flush();
            CommandEncoder var18 = RenderSystem.getDevice().createCommandEncoder();
            int var19 = (writeOffset + 256 - 1) / 256 * 256;
            if (var19 + 128 > 16384) {
               var19 = 0;
            }

            GpuBufferSlice var20 = new GpuBufferSlice(uniformBuffer, var19, 128L);
            var18.writeToBuffer(var20, var17);
            writeOffset = var19 + 256;
            Framebuffer var21 = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass var22 = var18.createRenderPass(() -> "GlowOutlinePipeline", var21.getColorAttachmentView(), OptionalInt.empty())) {
               var22.setPipeline(pipeline);
               Scissor.apply(var22);
               var22.setUniform("Uniforms", var20);
               var22.draw(0, 6);
            }
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
