package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
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

public class LiquidGlassPipeline {
   private static final int UBO_ALIGN = 256;
   private static final int LIQUID_UBO_SIZE = 208;
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("brokke", "liquid_glass"))
         .withVertexShader(Identifier.of("brokke", "liquid_glass"))
         .withFragmentShader(Identifier.of("brokke", "liquid_glass"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("LiquidData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer dataBuffer;
   private static final int BUFFER_SIZE = 512;
   private static int writeOffset = 0;
   private static boolean initialized = false;

   public static void a() {
      if (!initialized) {
         dataBuffer = MemoryUtil.memAlloc(512);
         ByteBuffer var0 = MemoryUtil.memAlloc(4);
         var0.putInt(0);
         var0.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:liquid_dummy_vertex", 32, var0);
         MemoryUtil.memFree(var0);
         initialized = true;
      }
   }

   public static void b() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.getFramebuffer() != null && var0.getFramebuffer().getColorAttachment() != null) {
         a();
      }
   }

   public static void a(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = 10.0F;
      float var8 = 0.08F;
      float var9 = 1.5F;
      float var10 = var5 * var7 / 2.0F;
      float var11 = var4 == 240.0F ? 100.0F : 50.0F;
      a(
         var0,
         var1 - var9,
         var2 - var9,
         var3 + var9 * 2.0F,
         var4 + var9 * 2.0F,
         var10,
         var6,
         1.0F,
         var11,
         new Color(255, 255, 255, 255),
         1.0F,
         true,
         0.0F,
         var8,
         var7
      );
   }

   public static void b(Matrix4f var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = 10.0F;
      float var8 = 0.12F;
      float var9 = 1.5F;
      float var10 = var5 * var7 / 2.0F;
      a(
         var0,
         var1 - var9,
         var2 - var9,
         var3 + var9 * 2.0F,
         var4 + var9 * 2.0F,
         var10,
         var6,
         1.0F,
         80.0F,
         new Color(255, 255, 255, 255),
         1.0F,
         true,
         0.0F,
         var8,
         var7
      );
   }

   public static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      Color var9,
      float var10,
      boolean var11,
      float var12,
      float var13,
      float var14
   ) {
      MinecraftClient var15 = MinecraftClient.getInstance();
      if (var15.getFramebuffer() != null && var15.getFramebuffer().getColorAttachment() != null) {
         a();
         GuiBatch.flush();
         CommandEncoder var16 = RenderSystem.getDevice().createCommandEncoder();
         BlurPipeline.a(var16);
         GpuTextureView var17 = BlurPipeline.getCopyTextureView();
         if (var17 != null) {
            GpuSampler var18 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            GpuBufferSlice var19 = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
            float var20 = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
            int var21 = RenderUtil.a();
            int var22 = RenderUtil.b();
            a(
               var0,
               var1,
               var2,
               var3,
               var4,
               var21,
               var22,
               var5,
               var5,
               var5,
               var5,
               var20,
               var7,
               var8,
               var9,
               var10,
               var11,
               var12,
               var13,
               var14,
               new Color(255, 255, 255, 255),
               0.35F,
               0.25F
            );
            short var23 = 8192;
            if (uniformBuffer == null || uniformBuffer.size() < var23) {
               if (uniformBuffer != null) {
                  uniformBuffer.close();
               }

               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:liquid_uniform", 136, var23);
               writeOffset = 0;
            }

            int var24 = (writeOffset + 256 - 1) / 256 * 256;
            if (var24 + 208 > uniformBuffer.size()) {
               var24 = 0;
            }

            GpuBufferSlice var25 = new GpuBufferSlice(uniformBuffer, var24, 208L);
            var16.writeToBuffer(var25, dataBuffer);
            writeOffset = var24 + 256;

            try (RenderPass var26 = var16.createRenderPass(
                  () -> "brokke:liquid_glass",
                  var15.getFramebuffer().getColorAttachmentView(),
                  OptionalInt.empty(),
                  var15.getFramebuffer().getDepthAttachmentView(),
                  OptionalDouble.empty()
               )) {
               var26.setPipeline(PIPELINE);
               Scissor.apply(var26);
               var26.setVertexBuffer(0, dummyVertexBuffer);
               var26.bindTexture("Sampler0", var17, var18);
               RenderSystem.bindDefaultUniforms(var26);
               var26.setUniform("DynamicTransforms", var19);
               var26.setUniform("LiquidData", var25);
               var26.draw(0, 6);
            }
         }
      }
   }

   private static void a(
      Matrix4f var0,
      float var1,
      float var2,
      float var3,
      float var4,
      int var5,
      int var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      Color var14,
      float var15,
      boolean var16,
      float var17,
      float var18,
      float var19,
      Color var20,
      float var21,
      float var22
   ) {
      dataBuffer.clear();
      dataBuffer.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
      dataBuffer.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
      dataBuffer.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
      dataBuffer.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
      dataBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
      dataBuffer.putFloat(var5).putFloat(var6).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(var7).putFloat(var8).putFloat(var9).putFloat(var10);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(var11);
      dataBuffer.putFloat(var12).putFloat(var13).putFloat(var14.getAlpha() / 255.0F).putFloat(var15);
      dataBuffer.putFloat(var16 ? 1.0F : 0.0F).putFloat(var17).putFloat(var18).putFloat(var19);
      dataBuffer.putFloat(var21).putFloat(var22).putFloat(1.0F).putFloat(0.0F);
      dataBuffer.putFloat(var14.getRed() / 255.0F).putFloat(var14.getGreen() / 255.0F).putFloat(var14.getBlue() / 255.0F).putFloat(1.0F);
      dataBuffer.putFloat(var20.getRed() / 255.0F).putFloat(var20.getGreen() / 255.0F).putFloat(var20.getBlue() / 255.0F).putFloat(1.0F);
      dataBuffer.flip();
   }

   public static void c() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
         dummyVertexBuffer = null;
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
         dataBuffer = null;
      }

      writeOffset = 0;
      initialized = false;
   }
}
