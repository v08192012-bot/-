package brokke.cc.render;

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

public class CircleProgressPipeline {
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
               .withLocation(Identifier.of("brokke", "circle_progress"))
               .withVertexShader(Identifier.of("brokke", "circle_progress_vertex"))
               .withFragmentShader(Identifier.of("brokke", "circle_progress_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "CircleProgressPipeline Uniforms", 136, 16384L);
            scratchBuffer = MemoryUtil.memAlloc(128);
         } catch (Exception var1) {
         }
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8, float var9) {
      if (pipeline == null) {
         a();
      }

      if (pipeline != null && uniformBuffer != null && scratchBuffer != null) {
         float var10 = (var6 >> 16 & 0xFF) / 255.0F;
         float var11 = (var6 >> 8 & 0xFF) / 255.0F;
         float var12 = (var6 & 0xFF) / 255.0F;
         float var13 = (var6 >> 24 & 0xFF) / 255.0F;
         float var14 = (var7 >> 16 & 0xFF) / 255.0F;
         float var15 = (var7 >> 8 & 0xFF) / 255.0F;
         float var16 = (var7 & 0xFF) / 255.0F;
         float var17 = (var7 >> 24 & 0xFF) / 255.0F;
         ByteBuffer var18 = scratchBuffer;
         var18.clear();
         var18.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         var18.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         var18.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         var18.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         var18.position(64);
         var18.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var3);
         var18.putFloat(var4).putFloat(var5).putFloat(var8).putFloat(var9);
         var18.putFloat(var10).putFloat(var11).putFloat(var12).putFloat(var13);
         var18.putFloat(var14).putFloat(var15).putFloat(var16).putFloat(var17);
         var18.flip();
         GuiBatch.flush();
         CommandEncoder var19 = RenderSystem.getDevice().createCommandEncoder();
         int var20 = (writeOffset + 256 - 1) / 256 * 256;
         if (var20 + 128 > 16384) {
            var20 = 0;
         }

         GpuBufferSlice var21 = new GpuBufferSlice(uniformBuffer, var20, 128L);
         var19.writeToBuffer(var21, var18);
         writeOffset = var20 + 256;
         Framebuffer var22 = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass var23 = var19.createRenderPass(() -> "CircleProgressPipeline", var22.getColorAttachmentView(), OptionalInt.empty())) {
            var23.setPipeline(pipeline);
            Scissor.apply(var23);
            var23.setUniform("Uniforms", var21);
            var23.draw(0, 6);
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
      pipeline = null;
   }
}
