package cmode.render;

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
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class OutlinePipeline {
   private static final int MAX_BATCH_ITEMS = 64;
   private static final int ITEM_FLOATS = 28;
   private static final int ITEM_BYTES = 192;
   private static final int UNIFORM_SIZE = 12288;
   private static final int BUFFER_CAPACITY = 786432;
   private static RenderPipeline pipeline;
   private static MappableRingBuffer uniformBuffer;
   private static ByteBuffer scratchBuffer;
   private static int writeOffset;
   private static final float[] batchData = new float[1792];
   private static final Matrix4f[] batchMatrices = new Matrix4f[64];
   private static final float[] batchZ = new float[64];
   private static int batchCount;
   private static int batchDepth;

   public static void a() {
      if (pipeline == null) {
         RenderPipeline configured = RenderPipeline.builder()
            .withLocation(Identifier.of("cmode", "outline"))
            .withVertexShader(Identifier.of("cmode", "outline_vertex"))
            .withFragmentShader(Identifier.of("cmode", "outline_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         uniformBuffer = new MappableRingBuffer(() -> "OutlinePipeline batches", 138, 786432);
         scratchBuffer = MemoryUtil.memAlloc(12288);
         pipeline = configured;
      }
   }

   public static void beginFrame() {
      e();
      if (uniformBuffer != null) {
         uniformBuffer.rotate();
      }

      writeOffset = 0;
   }

   public static void b() {
      batchDepth++;
   }

   public static void c() {
      if (batchDepth > 0) {
         batchDepth--;
      }

      if (batchDepth == 0 && !GuiBatch.isActive()) {
         e();
      }
   }

   public static boolean isBatching() {
      return batchDepth > 0 || GuiBatch.isActive();
   }

   public static void e() {
      if (batchCount != 0) {
         try {
            drawBatch(batchCount);
         } finally {
            batchCount = 0;
         }
      }
   }

   public static void a(
      Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int... var11
   ) {
      GuiBatch.select(GuiBatch.DrawType.OUTLINE);
      if (batchCount == 64) {
         e();
      }

      if (batchMatrices[batchCount] == null) {
         batchMatrices[batchCount] = new Matrix4f();
      }

      batchMatrices[batchCount].set(var0);
      batchZ[batchCount] = var10;
      a(batchCount, var1, var2, var3, var4, var6, var7, var5, var8, var9, var11);
      batchCount++;
      if (!isBatching()) {
         e();
      }
   }

   private static void a(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int... var10) {
      int var11 = var0 * 28;
      batchData[var11] = var1;
      batchData[var11 + 1] = var2;
      batchData[var11 + 2] = var3;
      batchData[var11 + 3] = var4;
      batchData[var11 + 4] = var5;
      batchData[var11 + 5] = var6;
      batchData[var11 + 6] = var7;
      batchData[var11 + 7] = var8;
      batchData[var11 + 8] = var9;
      batchData[var11 + 9] = 0.0F;
      batchData[var11 + 10] = 0.0F;
      batchData[var11 + 11] = 0.0F;
      int var12 = var10 != null ? var10.length : 0;
      if (var12 <= 1) {
         int var13 = var12 == 1 ? var10[0] : -1;
         float var14 = (var13 >> 16 & 0xFF) / 255.0F;
         float var15 = (var13 >> 8 & 0xFF) / 255.0F;
         float var16 = (var13 & 0xFF) / 255.0F;
         float var17 = (var13 >> 24 & 0xFF) / 255.0F;

         for (int var18 = 0; var18 < 4; var18++) {
            int var19 = var11 + 12 + var18 * 4;
            batchData[var19] = var14;
            batchData[var19 + 1] = var15;
            batchData[var19 + 2] = var16;
            batchData[var19 + 3] = var17;
         }
      } else if (var12 == 2) {
         int var23 = var10[0];
         int var25 = var10[1];
         float var27 = (var23 >> 16 & 0xFF) / 255.0F;
         float var29 = (var23 >> 8 & 0xFF) / 255.0F;
         float var30 = (var23 & 0xFF) / 255.0F;
         float var31 = (var23 >> 24 & 0xFF) / 255.0F;
         float var32 = (var25 >> 16 & 0xFF) / 255.0F;
         float var20 = (var25 >> 8 & 0xFF) / 255.0F;
         float var21 = (var25 & 0xFF) / 255.0F;
         float var22 = (var25 >> 24 & 0xFF) / 255.0F;
         batchData[var11 + 12] = var27;
         batchData[var11 + 13] = var29;
         batchData[var11 + 14] = var30;
         batchData[var11 + 15] = var31;
         batchData[var11 + 16] = var27;
         batchData[var11 + 17] = var29;
         batchData[var11 + 18] = var30;
         batchData[var11 + 19] = var31;
         batchData[var11 + 20] = var32;
         batchData[var11 + 21] = var20;
         batchData[var11 + 22] = var21;
         batchData[var11 + 23] = var22;
         batchData[var11 + 24] = var32;
         batchData[var11 + 25] = var20;
         batchData[var11 + 26] = var21;
         batchData[var11 + 27] = var22;
      } else {
         for (int var24 = 0; var24 < 4; var24++) {
            int var26 = var10[Math.min(var24, var12 - 1)];
            int var28 = var11 + 12 + var24 * 4;
            batchData[var28] = (var26 >> 16 & 0xFF) / 255.0F;
            batchData[var28 + 1] = (var26 >> 8 & 0xFF) / 255.0F;
            batchData[var28 + 2] = (var26 & 0xFF) / 255.0F;
            batchData[var28 + 3] = (var26 >> 24 & 0xFF) / 255.0F;
         }
      }
   }

   private static void drawBatch(int count) {
      a();
      scratchBuffer.clear();

      for (int i = 0; i < count; i++) {
         int offset = scratchBuffer.position();
         batchMatrices[i].get(offset, scratchBuffer);
         scratchBuffer.position(offset + 64);
         scratchBuffer.putFloat(batchZ[i]).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);

         for (int component = 0; component < 28; component++) {
            scratchBuffer.putFloat(batchData[i * 28 + component]);
         }
      }

      scratchBuffer.flip();
      if (writeOffset + 12288 > 786432) {
         uniformBuffer.rotate();
         writeOffset = 0;
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuBufferSlice slice = uniformBuffer.getBlocking().slice(writeOffset, 12288L);
      encoder.writeToBuffer(slice, scratchBuffer);
      Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

      try (RenderPass pass = encoder.createRenderPass(() -> "OutlinePipeline batch", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         pass.setPipeline(pipeline);
         pass.setUniform("Uniforms", slice);
         Scissor.apply(pass);
         pass.draw(0, count * 6);
      }

      writeOffset += 12288;
   }

   public static void f() {
      e();
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (scratchBuffer != null) {
         MemoryUtil.memFree(scratchBuffer);
         scratchBuffer = null;
      }

      batchCount = 0;
      batchDepth = 0;
      pipeline = null;
      writeOffset = 0;
   }
}
