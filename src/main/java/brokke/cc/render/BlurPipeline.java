package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
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
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class BlurPipeline {
   private static final int NUM_LEVELS = 4;
   private static final int BUFFER_SIZE = 4096;
   private static final int UBO_ALIGN = 256;
   private static final int BLUR_UBO_SIZE = 160;
   private static final int MAX_BATCH_RECTS = 64;
   private static final int FINAL_UBO_SIZE = 2144;
   private static final RenderPipeline PIPELINE_BLUR = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("brokke", "blur_pass"))
         .withVertexShader(Identifier.of("brokke", "blur_pass_vertex"))
         .withFragmentShader(Identifier.of("brokke", "blur_pass_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final RenderPipeline PIPELINE_FINAL = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("brokke", "blur_final"))
         .withVertexShader(Identifier.of("brokke", "blur_final_vertex"))
         .withFragmentShader(Identifier.of("brokke", "blur_final_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final ThreadLocal<float[]> ROUND_CACHE = ThreadLocal.withInitial(() -> new float[4]);
   private static GpuBuffer uniformBuffer;
   private static int finalWriteOffset = 0;
   private static GpuBuffer blurUniformBuffer;
   private static int blurWriteOffset;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer dataBuffer;
   private static GpuTexture copyTexture;
   private static GpuTextureView copyTextureView;
   private static final GpuTexture[] blurTextures = new GpuTexture[4];
   private static final GpuTextureView[] blurViews = new GpuTextureView[4];
   private static final int[] levelWidths = new int[4];
   private static final int[] levelHeights = new int[4];
   private static long lastBlurFrame = -1L;
   private static final float[] batchRectData = new float[512];
   private static int batchRectCount = 0;
   private static int batchDepth;
   private static Matrix4f batchMatrix = null;
   private static float batchZ = 0.0F;
   private static int lastWidth = 0;
   private static int lastHeight = 0;
   private static boolean initialized = false;
   private static long frameCounter = 0L;
   private static long lastSnapshotTick = -1L;

   public static void a() {
      if (!initialized) {
         dataBuffer = MemoryUtil.memAlloc(4096);
         ByteBuffer var0 = MemoryUtil.memAlloc(4);
         var0.putInt(0);
         var0.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:blur_dummy_vertex", 32, var0);
         MemoryUtil.memFree(var0);
         initialized = true;
      }
   }

   private static void a(int var0, int var1) {
      if (copyTexture == null || var0 != lastWidth || var1 != lastHeight) {
         if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
         }

         if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
         }

         copyTexture = RenderSystem.getDevice().createTexture(() -> "brokke:blur_copy", 13, TextureFormat.RGBA8, var0, var1, 1, 1);
         copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);
         lastWidth = var0;
         lastHeight = var1;
      }
   }

   private static void b(int var0, int var1) {
      if (blurTextures[0] == null || var0 != lastWidth || var1 != lastHeight) {
         for (int var2 = 0; var2 < 4; var2++) {
            if (blurViews[var2] != null) {
               blurViews[var2].close();
               blurViews[var2] = null;
            }

            if (blurTextures[var2] != null) {
               blurTextures[var2].close();
               blurTextures[var2] = null;
            }
         }

         int var6 = var0;
         int var3 = var1;

         for (int var4 = 0; var4 < 4; var4++) {
            var6 = Math.max(1, var6 / 2);
            var3 = Math.max(1, var3 / 2);
            levelWidths[var4] = var6;
            levelHeights[var4] = var3;
            int var5 = var4;
            blurTextures[var4] = RenderSystem.getDevice().createTexture(() -> "brokke:blur_level_" + var5, 13, TextureFormat.RGBA8, var6, var3, 1, 1);
            blurViews[var4] = RenderSystem.getDevice().createTextureView(blurTextures[var4]);
         }
      }
   }

   private static GpuBufferSlice a(CommandEncoder var0, int var1, int var2, int var3, int var4, float var5, float var6) {
      a(var1, var2, var3, var4, var5, var6);
      int var7 = (blurWriteOffset + 256 - 1) / 256 * 256;
      GpuBufferSlice var8 = new GpuBufferSlice(blurUniformBuffer, var7, 160L);
      var0.writeToBuffer(var8, dataBuffer);
      blurWriteOffset = var7 + 256;
      return var8;
   }

   public static void a(CommandEncoder var0, GpuSampler var1) {
      if (lastBlurFrame != frameCounter || blurViews[0] == null) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2.getFramebuffer() != null && var2.getFramebuffer().getColorAttachment() != null) {
            int var3 = var2.getFramebuffer().textureWidth;
            int var4 = var2.getFramebuffer().textureHeight;
            b(var3, var4);
            lastWidth = var3;
            lastHeight = var4;
            short var5 = 2048;
            if (blurUniformBuffer == null || blurUniformBuffer.size() < var5) {
               if (blurUniformBuffer != null) {
                  blurUniformBuffer.close();
               }

               blurUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:blur_ubo", 136, var5);
            }

            blurWriteOffset = 0;
            GpuBufferSlice var6 = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
            GpuBufferSlice var7 = a(var0, lastWidth, lastHeight, levelWidths[0], levelHeights[0], 0.5F, 0.0F);

            try (RenderPass var8 = var0.createRenderPass(() -> "brokke:blur_down_0", blurViews[0], OptionalInt.empty(), null, OptionalDouble.empty())) {
               var8.setPipeline(PIPELINE_BLUR);
               var8.setVertexBuffer(0, dummyVertexBuffer);
               var8.bindTexture("Sampler0", var2.getFramebuffer().getColorAttachmentView(), var1);
               RenderSystem.bindDefaultUniforms(var8);
               var8.setUniform("DynamicTransforms", var6);
               var8.setUniform("BlurData", var7);
               var8.draw(0, 6);
            }

            for (int var22 = 1; var22 < 4; var22++) {
               int var9 = var22 - 1;
               int var10 = var22;
               int var11 = var22;
               GpuBufferSlice var12 = a(var0, levelWidths[var9], levelHeights[var9], levelWidths[var10], levelHeights[var10], 0.5F, 0.0F);

               try (RenderPass var13 = var0.createRenderPass(
                     () -> "brokke:blur_down_" + var11, blurViews[var10], OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  var13.setPipeline(PIPELINE_BLUR);
                  var13.setVertexBuffer(0, dummyVertexBuffer);
                  var13.bindTexture("Sampler0", blurViews[var9], var1);
                  RenderSystem.bindDefaultUniforms(var13);
                  var13.setUniform("DynamicTransforms", var6);
                  var13.setUniform("BlurData", var12);
                  var13.draw(0, 6);
               }
            }

            for (int var23 = 2; var23 >= 0; var23--) {
               int var24 = var23 + 1;
               int var25 = var23;
               int var26 = var23;
               GpuBufferSlice var27 = a(var0, levelWidths[var24], levelHeights[var24], levelWidths[var25], levelHeights[var25], 1.2F, 1.0F);

               try (RenderPass var28 = var0.createRenderPass(
                     () -> "brokke:blur_up_" + var26, blurViews[var25], OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  var28.setPipeline(PIPELINE_BLUR);
                  var28.setVertexBuffer(0, dummyVertexBuffer);
                  var28.bindTexture("Sampler0", blurViews[var24], var1);
                  RenderSystem.bindDefaultUniforms(var28);
                  var28.setUniform("DynamicTransforms", var6);
                  var28.setUniform("BlurData", var27);
                  var28.draw(0, 6);
               }
            }

            lastBlurFrame = frameCounter;
            GlStateManager._viewport(0, 0, var3, var4);
         }
      }
   }

   public static void b() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.getFramebuffer() != null && var0.getFramebuffer().getColorAttachment() != null) {
         a();
         frameCounter++;
         finalWriteOffset = 0;
      }
   }

   public static void a(CommandEncoder var0) {
      if (copyTexture == null || lastSnapshotTick != frameCounter) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         if (var1.getFramebuffer() != null && var1.getFramebuffer().getColorAttachment() != null) {
            int var2 = var1.getFramebuffer().textureWidth;
            int var3 = var1.getFramebuffer().textureHeight;
            a(var2, var3);
            var0.copyTextureToTexture(var1.getFramebuffer().getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, var2, var3);
            lastSnapshotTick = frameCounter;
         }
      }
   }

   public static GpuTextureView getCopyTextureView() {
      return copyTextureView;
   }

   public static GpuTextureView d() {
      return blurViews[0];
   }

   public static void e() {
      batchDepth++;
   }

   public static void f() {
      if (batchDepth > 0) {
         batchDepth--;
      }

      if (batchDepth == 0 && !GuiBatch.isActive()) {
         h();
      }
   }

   public static boolean isBatching() {
      return batchDepth > 0 || GuiBatch.isActive();
   }

   public static void h() {
      if (batchRectCount > 0 && batchMatrix != null) {
         a(batchMatrix, batchRectCount, batchZ);
         batchRectCount = 0;
         batchMatrix = null;
      }
   }

   private static boolean a(Matrix4f var0, Matrix4f var1) {
      if (var0 == var1) {
         return true;
      } else {
         return var0 != null && var1 != null ? var0.equals(var1) : false;
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      a(var0, var1, var2, var3, var4, var5, var5, var5, var5, var6);
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      GuiBatch.select(GuiBatch.DrawType.BLUR);
      if (!isBatching()) {
         batchRectData[0] = var1;
         batchRectData[1] = var2;
         batchRectData[2] = var3;
         batchRectData[3] = var4;
         batchRectData[4] = var5;
         batchRectData[5] = var6;
         batchRectData[6] = var7;
         batchRectData[7] = var8;
         a(var0, 1, var9);
      } else {
         if (batchMatrix != null && (!a(batchMatrix, var0) || Math.abs(batchZ - var9) > 0.001F || batchRectCount >= 64)) {
            h();
         }

         if (batchMatrix == null) {
            batchMatrix = new Matrix4f(var0);
            batchZ = var9;
         }

         int var10 = batchRectCount * 8;
         batchRectData[var10] = var1;
         batchRectData[var10 + 1] = var2;
         batchRectData[var10 + 2] = var3;
         batchRectData[var10 + 3] = var4;
         batchRectData[var10 + 4] = var5;
         batchRectData[var10 + 5] = var6;
         batchRectData[var10 + 6] = var7;
         batchRectData[var10 + 7] = var8;
         batchRectCount++;
      }
   }

   private static void a(Matrix4f var0, int var1, float var2) {
      if (var1 > 0) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.getFramebuffer() != null && var3.getFramebuffer().getColorAttachment() != null) {
            a();
            GpuSampler var4 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
            a(var5, var4);
            if (blurViews[0] != null) {
               GpuBufferSlice var6 = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
               int var7 = RenderUtil.a();
               int var8 = RenderUtil.b();
               a(var0, var7, var8, var2, var1);
               short var9 = 2304;
               int var10 = 16 * var9;
               if (uniformBuffer == null || uniformBuffer.size() < var10) {
                  if (uniformBuffer != null) {
                     uniformBuffer.close();
                  }

                  uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:blur_final_uniform", 136, var10);
                  finalWriteOffset = 0;
               }

               int var11 = (finalWriteOffset + 256 - 1) / 256 * 256;
               if (var11 + 2144 > uniformBuffer.size()) {
                  var11 = 0;
               }

               GpuBufferSlice var12 = new GpuBufferSlice(uniformBuffer, var11, 2144L);
               var5.writeToBuffer(var12, dataBuffer);
               finalWriteOffset = var11 + var9;

               try (RenderPass var13 = var5.createRenderPass(
                     () -> "brokke:blur_final", var3.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  var13.setPipeline(PIPELINE_FINAL);
                  Scissor.apply(var13);
                  var13.setVertexBuffer(0, dummyVertexBuffer);
                  var13.bindTexture("Sampler0", blurViews[0], var4);
                  RenderSystem.bindDefaultUniforms(var13);
                  var13.setUniform("DynamicTransforms", var6);
                  var13.setUniform("BlurData", var12);
                  var13.draw(0, var1 * 6);
               }
            }
         }
      }
   }

   private static void a(int var0, int var1, int var2, int var3, float var4, float var5) {
      dataBuffer.clear();

      for (int var6 = 0; var6 < 16; var6++) {
         dataBuffer.putFloat(0.0F);
      }

      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(var0).putFloat(var1);
      dataBuffer.putFloat(var0).putFloat(var1).putFloat(var5).putFloat(var4);
      dataBuffer.putFloat(var2).putFloat(var3).putFloat(1.0F).putFloat(0.0F);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.flip();
   }

   private static void a(Matrix4f var0, int var1, int var2, float var3, int var4) {
      dataBuffer.clear();
      dataBuffer.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
      dataBuffer.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
      dataBuffer.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
      dataBuffer.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
      dataBuffer.putFloat(var1).putFloat(var2).putFloat(1.0F).putFloat(0.0F);
      dataBuffer.putFloat(var3).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);

      for (int var5 = 0; var5 < var4; var5++) {
         int var6 = var5 * 8;
         dataBuffer.putFloat(batchRectData[var6]).putFloat(batchRectData[var6 + 1]).putFloat(batchRectData[var6 + 2]).putFloat(batchRectData[var6 + 3]);
         dataBuffer.putFloat(batchRectData[var6 + 4]).putFloat(batchRectData[var6 + 5]).putFloat(batchRectData[var6 + 6]).putFloat(batchRectData[var6 + 7]);
      }

      dataBuffer.flip();
   }

   public static void i() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (blurUniformBuffer != null) {
         blurUniformBuffer.close();
         blurUniformBuffer = null;
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
         dummyVertexBuffer = null;
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
         dataBuffer = null;
      }

      if (copyTextureView != null) {
         copyTextureView.close();
         copyTextureView = null;
      }

      if (copyTexture != null) {
         copyTexture.close();
         copyTexture = null;
      }

      for (int var0 = 0; var0 < 4; var0++) {
         if (blurViews[var0] != null) {
            blurViews[var0].close();
            blurViews[var0] = null;
         }

         if (blurTextures[var0] != null) {
            blurTextures[var0].close();
            blurTextures[var0] = null;
         }
      }

      lastWidth = 0;
      lastHeight = 0;
      lastBlurFrame = -1L;
      lastSnapshotTick = -1L;
      batchRectCount = 0;
      batchDepth = 0;
      batchMatrix = null;
      initialized = false;
   }
}
