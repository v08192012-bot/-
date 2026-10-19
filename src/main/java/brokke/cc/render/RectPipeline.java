package brokke.cc.render;

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

public class RectPipeline {
   private static final int MAX_BATCH_ITEMS = 64;
   private static final int ITEM_FLOATS = 24;
   private static final int ITEM_BYTES = 176;
   private static final int UNIFORM_SIZE = 11264;
   private static final int BUFFER_CAPACITY = 720896;
   private static RenderPipeline pipeline;
   private static MappableRingBuffer uniformBuffer;
   private static ByteBuffer scratchBuffer;
   private static int writeOffset;
   private static final float[] batchData = new float[1536];
   private static final Matrix4f[] batchMatrices = new Matrix4f[64];
   private static final float[] batchZ = new float[64];
   private static int batchCount;
   private static int batchDepth;

   public static void a() {
      if (pipeline == null) {
         RenderPipeline configured = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "rectangle"))
            .withVertexShader(Identifier.of("brokke", "rectangle_vertex"))
            .withFragmentShader(Identifier.of("brokke", "rectangle_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         uniformBuffer = new MappableRingBuffer(() -> "RectanglePipeline batches", 138, 720896);
         scratchBuffer = MemoryUtil.memAlloc(11264);
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

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int... var10) {
      GuiBatch.select(GuiBatch.DrawType.RECTANGLE);
      if (batchCount == 64) {
         e();
      }

      if (batchMatrices[batchCount] == null) {
         batchMatrices[batchCount] = new Matrix4f();
      }

      batchMatrices[batchCount].set(var0);
      batchZ[batchCount] = var9;
      a(batchCount, var1, var2, var3, var4, var6, var7, var5, var8, var10);
      batchCount++;
      if (!isBatching()) {
         e();
      }
   }

   private static void a(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int... var9) {
      int var10 = var0 * 24;
      batchData[var10] = var1;
      batchData[var10 + 1] = var2;
      batchData[var10 + 2] = var3;
      batchData[var10 + 3] = var4;
      batchData[var10 + 4] = var5;
      batchData[var10 + 5] = var6;
      batchData[var10 + 6] = var7;
      batchData[var10 + 7] = var8;
      int var11 = var9 != null ? var9.length : 0;
      if (var11 <= 1) {
         int var12 = var11 == 1 ? var9[0] : -1;
         float var13 = (var12 >> 16 & 0xFF) / 255.0F;
         float var14 = (var12 >> 8 & 0xFF) / 255.0F;
         float var15 = (var12 & 0xFF) / 255.0F;
         float var16 = (var12 >> 24 & 0xFF) / 255.0F;

         for (int var17 = 0; var17 < 4; var17++) {
            int var18 = var10 + 8 + var17 * 4;
            batchData[var18] = var13;
            batchData[var18 + 1] = var14;
            batchData[var18 + 2] = var15;
            batchData[var18 + 3] = var16;
         }
      } else if (var11 == 2) {
         int var22 = var9[0];
         int var24 = var9[1];
         float var26 = (var22 >> 16 & 0xFF) / 255.0F;
         float var28 = (var22 >> 8 & 0xFF) / 255.0F;
         float var29 = (var22 & 0xFF) / 255.0F;
         float var30 = (var22 >> 24 & 0xFF) / 255.0F;
         float var31 = (var24 >> 16 & 0xFF) / 255.0F;
         float var19 = (var24 >> 8 & 0xFF) / 255.0F;
         float var20 = (var24 & 0xFF) / 255.0F;
         float var21 = (var24 >> 24 & 0xFF) / 255.0F;
         batchData[var10 + 8] = var26;
         batchData[var10 + 9] = var28;
         batchData[var10 + 10] = var29;
         batchData[var10 + 11] = var30;
         batchData[var10 + 12] = var26;
         batchData[var10 + 13] = var28;
         batchData[var10 + 14] = var29;
         batchData[var10 + 15] = var30;
         batchData[var10 + 16] = var31;
         batchData[var10 + 17] = var19;
         batchData[var10 + 18] = var20;
         batchData[var10 + 19] = var21;
         batchData[var10 + 20] = var31;
         batchData[var10 + 21] = var19;
         batchData[var10 + 22] = var20;
         batchData[var10 + 23] = var21;
      } else {
         for (int var23 = 0; var23 < 4; var23++) {
            int var25 = var9[Math.min(var23, var11 - 1)];
            int var27 = var10 + 8 + var23 * 4;
            batchData[var27] = (var25 >> 16 & 0xFF) / 255.0F;
            batchData[var27 + 1] = (var25 >> 8 & 0xFF) / 255.0F;
            batchData[var27 + 2] = (var25 & 0xFF) / 255.0F;
            batchData[var27 + 3] = (var25 >> 24 & 0xFF) / 255.0F;
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

         for (int component = 0; component < 24; component++) {
            scratchBuffer.putFloat(batchData[i * 24 + component]);
         }
      }

      scratchBuffer.flip();
      if (writeOffset + 11264 > 720896) {
         uniformBuffer.rotate();
         writeOffset = 0;
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuBufferSlice slice = uniformBuffer.getBlocking().slice(writeOffset, 11264L);
      encoder.writeToBuffer(slice, scratchBuffer);
      Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

      try (RenderPass pass = encoder.createRenderPass(() -> "RectanglePipeline batch", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         pass.setPipeline(pipeline);
         pass.setUniform("Uniforms", slice);
         Scissor.apply(pass);
         pass.draw(0, count * 6);
      }

      writeOffset += 11264;
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
