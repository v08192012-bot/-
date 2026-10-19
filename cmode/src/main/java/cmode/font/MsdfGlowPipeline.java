package cmode.font;

import cmode.render.GuiBatch;
import cmode.render.Scissor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class MsdfGlowPipeline {
   private static final int MAX_GLYPHS = 128;
   private static final int MSDF_DATA_SIZE = 128;
   private static final int GLYPH_DATA_SIZE = 4096;
   private static final int UBO_ALIGN = 256;
   private static final int SLOTS = 32;
   private static final int MSDF_CAPACITY = 8192;
   private static final int GLYPH_CAPACITY = 131072;
   private static RenderPipeline pipeline;
   private static GpuBuffer msdfBuffer;
   private static GpuBuffer glyphBuffer;
   private static ByteBuffer msdfData;
   private static ByteBuffer glyphData;
   private static int msdfWriteOffset = 0;
   private static int glyphWriteOffset = 0;
   private static boolean initialized = false;

   public static void a() {
      if (!initialized) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("cmode", "msdf_glow"))
               .withVertexShader(Identifier.of("cmode", "msdf_glow_vertex"))
               .withFragmentShader(Identifier.of("cmode", "msdf_glow_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("MsdfData", UniformType.UNIFORM_BUFFER)
               .withUniform("GlyphData", UniformType.UNIFORM_BUFFER)
               .withSampler("Sampler0")
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            msdfBuffer = RenderSystem.getDevice().createBuffer(() -> "MsdfGlowPipeline MsdfData", 136, 8192L);
            glyphBuffer = RenderSystem.getDevice().createBuffer(() -> "MsdfGlowPipeline GlyphData", 136, 131072L);
            msdfData = MemoryUtil.memAlloc(128);
            glyphData = MemoryUtil.memAlloc(4096);
            initialized = true;
         } catch (Exception var1) {
            System.err.println("[MsdfGlowPipeline] Failed to init: " + var1.getMessage());
         }
      }
   }

   public static void a(Matrix4f var0, MsdfFont var1, String var2, float var3, float var4, float var5, int var6, int var7, float var8, float var9) {
      if (initialized && var1 != null && var1.isLoaded() && !var2.isEmpty()) {
         float var10 = var5 / var1.getEmSize();
         float var11 = var3;
         float var12 = var4 + var1.getAscender() * var10;
         float var13 = (var6 >> 16 & 0xFF) / 255.0F;
         float var14 = (var6 >> 8 & 0xFF) / 255.0F;
         float var15 = (var6 & 0xFF) / 255.0F;
         float var16 = (var6 >> 24 & 0xFF) / 255.0F;
         float var17 = (var7 >> 16 & 0xFF) / 255.0F;
         float var18 = (var7 >> 8 & 0xFF) / 255.0F;
         float var19 = (var7 & 0xFF) / 255.0F;
         float var20 = (var7 >> 24 & 0xFF) / 255.0F;
         msdfData.clear();
         msdfData.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         msdfData.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         msdfData.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         msdfData.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         msdfData.putFloat(var13).putFloat(var14).putFloat(var15).putFloat(var16);
         msdfData.putFloat(var17).putFloat(var18).putFloat(var19).putFloat(var20);
         msdfData.putFloat(var1.getPxRange()).putFloat(var8).putFloat(0.0F).putFloat(0.0F);
         msdfData.putFloat(var9).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         msdfData.flip();
         glyphData.clear();
         int var21 = 0;

         for (int var22 = 0; var22 < var2.length() && var21 < 128; var22++) {
            char var23 = var2.charAt(var22);
            MsdfGlyph var24 = var1.a(var23);
            if (var24 != null) {
               if (var24.width > 0.0F && var24.height > 0.0F) {
                  float var25 = var11 + var24.bearingX * var10;
                  float var26 = var12 - var24.bearingY * var10;
                  float var27 = var24.width * var10;
                  float var28 = var24.height * var10;
                  glyphData.putFloat(var25).putFloat(var26).putFloat(var27).putFloat(var28);
                  glyphData.putFloat(var24.u0).putFloat(var24.v0);
                  glyphData.putFloat(var24.u1 - var24.u0).putFloat(var24.v1 - var24.v0);
                  var21++;
               }

               var11 += var24.advance * var10;
            }
         }

         if (var21 != 0) {
            glyphData.flip();
            a(var1, var21);
         }
      }
   }

   private static void a(MsdfFont var0, int var1) {
      GpuSampler var2 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      Framebuffer var3 = MinecraftClient.getInstance().getFramebuffer();
      GuiBatch.flush();
      CommandEncoder var4 = RenderSystem.getDevice().createCommandEncoder();
      int var5 = (msdfWriteOffset + 256 - 1) / 256 * 256;
      if (var5 + 128 > 8192) {
         var5 = 0;
      }

      GpuBufferSlice var6 = new GpuBufferSlice(msdfBuffer, var5, 128L);
      var4.writeToBuffer(var6, msdfData);
      msdfWriteOffset = var5 + 256;
      int var7 = (glyphWriteOffset + 256 - 1) / 256 * 256;
      if (var7 + 4096 > 131072) {
         var7 = 0;
      }

      GpuBufferSlice var8 = new GpuBufferSlice(glyphBuffer, var7, 4096L);
      var4.writeToBuffer(var8, glyphData);
      glyphWriteOffset = var7 + 4096;

      try (RenderPass var9 = var4.createRenderPass(() -> "MsdfGlowPipeline", var3.getColorAttachmentView(), OptionalInt.empty())) {
         var9.setPipeline(pipeline);
         Scissor.apply(var9);
         var9.setUniform("MsdfData", var6);
         var9.setUniform("GlyphData", var8);
         var9.bindTexture("Sampler0", var0.getTextureView(), var2);
         var9.draw(0, var1 * 6);
      }
   }

   public static void b() {
      if (msdfBuffer != null) {
         msdfBuffer.close();
         msdfBuffer = null;
      }

      if (glyphBuffer != null) {
         glyphBuffer.close();
         glyphBuffer = null;
      }

      if (msdfData != null) {
         MemoryUtil.memFree(msdfData);
         msdfData = null;
      }

      if (glyphData != null) {
         MemoryUtil.memFree(glyphData);
         glyphData = null;
      }

      msdfWriteOffset = 0;
      glyphWriteOffset = 0;
      initialized = false;
   }
}
