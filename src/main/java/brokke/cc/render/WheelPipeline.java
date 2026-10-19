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

public class WheelPipeline {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer scratchBuffer;
   private static final int UNIFORM_SIZE = 256;
   private static final int UBO_ALIGN = 256;
   private static final int BUFFER_CAPACITY = 16384;
   private static int writeOffset = 0;
   private static boolean initAttempted = false;

   public static void a() {
      if (pipeline == null && !initAttempted) {
         initAttempted = true;

         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("brokke", "wheel"))
               .withVertexShader(Identifier.of("brokke", "wheel_vertex"))
               .withFragmentShader(Identifier.of("brokke", "wheel_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "WheelPipeline Uniforms", 136, 16384L);
            scratchBuffer = MemoryUtil.memAlloc(256);
         } catch (Exception var1) {
            pipeline = null;
         }
      }
   }

   public static boolean a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, int[] var8, float var9, float var10) {
      if (pipeline == null && !initAttempted) {
         a();
      }

      if (pipeline != null && uniformBuffer != null && scratchBuffer != null) {
         float var11 = Math.max(var6, 3.0F) + 2.0F;
         float var12 = var4 + var11;
         float var13 = var12 * 2.0F;
         float var14 = var1 - var12;
         float var15 = var2 - var12;
         ByteBuffer var16 = scratchBuffer;
         var16.clear();
         var16.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         var16.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         var16.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         var16.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         var16.position(64);
         var16.putFloat(var14).putFloat(var15).putFloat(var13).putFloat(var13);
         var16.putFloat(var3).putFloat(var4).putFloat(var5).putFloat(var6);
         var16.putFloat(var7).putFloat(var9).putFloat(var10).putFloat(0.0F);

         for (int var17 = 0; var17 < 8; var17++) {
            int var18 = var17 < var7 && var17 < var8.length ? var8[var17] : 0;
            float var19 = (var18 >> 16 & 0xFF) / 255.0F;
            float var20 = (var18 >> 8 & 0xFF) / 255.0F;
            float var21 = (var18 & 0xFF) / 255.0F;
            float var22 = (var18 >> 24 & 0xFF) / 255.0F;
            var16.putFloat(var19).putFloat(var20).putFloat(var21).putFloat(var22);
         }

         var16.position(256);
         var16.flip();
         GuiBatch.flush();
         CommandEncoder var26 = RenderSystem.getDevice().createCommandEncoder();
         int var27 = (writeOffset + 256 - 1) / 256 * 256;
         if (var27 + 256 > 16384) {
            var27 = 0;
         }

         GpuBufferSlice var28 = new GpuBufferSlice(uniformBuffer, var27, 256L);
         var26.writeToBuffer(var28, var16);
         writeOffset = var27 + 256;
         Framebuffer var29 = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass var30 = var26.createRenderPass(() -> "WheelPipeline", var29.getColorAttachmentView(), OptionalInt.empty())) {
            var30.setPipeline(pipeline);
            Scissor.apply(var30);
            var30.setUniform("Uniforms", var28);
            var30.draw(0, 6);
         }

         return true;
      } else {
         return false;
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
      initAttempted = false;
   }
}
