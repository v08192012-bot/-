package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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

public class TexturePipeline {
   public static final BlendFunction ADDITIVE_ALPHA = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);
   private static RenderPipeline pipeline;
   private static RenderPipeline additivePipeline;
   private static MappableRingBuffer uniformBuffer;
   private static ByteBuffer scratchBuffer;
   private static final int MAX_BATCH_TEXTURES = 64;
   private static final int TEXTURE_BYTES = 144;
   private static final int UNIFORM_SIZE = 9216;
   private static final int BUFFER_CAPACITY = 589824;
   private static int writeOffset;
   private static int batchCount;
   private static GpuTextureView batchTexture;
   private static FilterMode batchFilter;
   private static boolean batchAdditive;

   public static void beginFrame() {
      flush();
      if (uniformBuffer != null) {
         uniformBuffer.rotate();
      }

      writeOffset = 0;
   }

   public static void a() {
      if (pipeline == null) {
         pipeline = c().withBlend(BlendFunction.TRANSLUCENT).build();

         try {
            additivePipeline = c().withBlend(ADDITIVE_ALPHA).build();
         } catch (Exception var1) {
            additivePipeline = pipeline;
         }

         uniformBuffer = new MappableRingBuffer(() -> "TexturePipeline batches", 138, 589824);
         scratchBuffer = MemoryUtil.memAlloc(9216);
      }
   }

   private static Builder c() {
      return RenderPipeline.builder()
         .withLocation(Identifier.of("brokke", "texture"))
         .withVertexShader(Identifier.of("brokke", "texture_vertex"))
         .withFragmentShader(Identifier.of("brokke", "texture_fragment"))
         .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
         .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, GpuTextureView var4, int var5, float var6, float var7) {
      a(var0, var1, var2, var3, var3, var4, var5, var6, var7, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   public static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, FilterMode.LINEAR, 0.0F, false);
   }

   public static void b(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, FilterMode.NEAREST, 0.0F, false);
   }

   public static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, 0.0F, var7, var8, var9, var10, var11, FilterMode.LINEAR, 0.0F, true);
   }

   public static void c(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, 0.0F, var7, var8, var9, var10, var11, FilterMode.LINEAR, var12, false);
   }

   public static void d(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      a(var0, var1, var2, var3, var4, var5, var6, 0.0F, var7, var8, var9, var10, var11, FilterMode.LINEAR, var12, true);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, GpuTextureView var5, int var6, float var7) {
      d(var0, var1, var2, var3, var4, var5, var6, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var7);
   }

   private static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      GpuTextureView var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      FilterMode var13,
      float var14,
      boolean var15
   ) {
      if (pipeline == null) {
         a();
      }

      if (var5 != null) {
         GuiBatch.select(GuiBatch.DrawType.TEXTURE);
         if (batchCount == 64 || batchCount > 0 && (batchTexture != var5 || batchFilter != var13 || batchAdditive != var15)) {
            flush();
         }

         batchTexture = var5;
         batchFilter = var13;
         batchAdditive = var15;
         int offset = scratchBuffer.position();
         var0.get(offset, scratchBuffer);
         scratchBuffer.position(offset + 64);
         scratchBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
         scratchBuffer.putFloat((var6 >> 16 & 0xFF) / 255.0F)
            .putFloat((var6 >> 8 & 0xFF) / 255.0F)
            .putFloat((var6 & 0xFF) / 255.0F)
            .putFloat((var6 >>> 24) / 255.0F);
         scratchBuffer.putFloat(var7).putFloat(var14).putFloat(0.0F).putFloat(0.0F);
         scratchBuffer.putFloat(var8).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         scratchBuffer.putFloat(var9).putFloat(var10).putFloat(var11 - var9).putFloat(var12 - var10);
         batchCount++;
         if (!GuiBatch.isActive()) {
            flush();
         }
      }
   }

   public static void flush() {
      if (batchCount != 0) {
         try {
            if (writeOffset + 9216 > 589824) {
               uniformBuffer.rotate();
               writeOffset = 0;
            }

            GpuBufferSlice slice = uniformBuffer.getBlocking().slice(writeOffset, 9216L);
            scratchBuffer.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(slice, scratchBuffer);
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass pass = encoder.createRenderPass(() -> "TexturePipeline batch", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
               pass.setPipeline(batchAdditive ? additivePipeline : pipeline);
               pass.setUniform("Uniforms", slice);
               pass.bindTexture("Sampler0", batchTexture, RenderSystem.getSamplerCache().get(batchFilter));
               Scissor.apply(pass);
               pass.draw(0, batchCount * 6);
            }

            writeOffset += 9216;
         } finally {
            batchCount = 0;
            batchTexture = null;
            batchFilter = null;
            scratchBuffer.clear();
         }
      }
   }

   public static void b() {
      flush();
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
      additivePipeline = null;
   }
}
