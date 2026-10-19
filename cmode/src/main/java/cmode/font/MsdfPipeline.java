package cmode.font;

import cmode.render.GuiBatch;
import cmode.render.Scissor;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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

public class MsdfPipeline {
   private static final int MAX_GLYPHS = 128;
   private static final int GLYPH_BYTES = 128;
   private static final int UNIFORM_BYTES = 16384;
   private static final int BUFFER_BYTES = 1048576;
   private static RenderPipeline pipeline;
   private static MappableRingBuffer glyphBuffer;
   private static ByteBuffer glyphData;
   private static GpuTextureView batchTexture;
   private static int glyphCount;
   private static int writeOffset;
   private static int batchDepth;

   public static void a() {
      if (pipeline == null) {
         RenderPipeline configured = RenderPipeline.builder()
            .withLocation(Identifier.of("cmode", "msdf"))
            .withVertexShader(Identifier.of("cmode", "msdf_vertex"))
            .withFragmentShader(Identifier.of("cmode", "msdf_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("GlyphData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         glyphBuffer = new MappableRingBuffer(() -> "cmode:msdf_glyph_batches", 138, 1048576);
         glyphData = MemoryUtil.memAlloc(16384);
         pipeline = configured;
      }
   }

   public static void beginFrame() {
      flush();
      if (glyphBuffer != null) {
         glyphBuffer.rotate();
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
         flush();
      }
   }

   private static boolean isBatching() {
      return batchDepth > 0 || GuiBatch.isActive();
   }

   public static void flush() {
      if (glyphCount != 0) {
         int count = glyphCount;

         try {
            if (writeOffset + 16384 > 1048576) {
               glyphBuffer.rotate();
               writeOffset = 0;
            }

            GpuBufferSlice slice = glyphBuffer.getBlocking().slice(writeOffset, 16384L);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            glyphData.flip();
            encoder.writeToBuffer(slice, glyphData);
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass pass = encoder.createRenderPass(() -> "cmode:msdf_batch", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
               pass.setPipeline(pipeline);
               pass.setUniform("GlyphData", slice);
               pass.bindTexture("Sampler0", batchTexture, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
               Scissor.apply(pass);
               pass.draw(0, count * 6);
            }

            writeOffset += 16384;
         } finally {
            glyphCount = 0;
            batchTexture = null;
            glyphData.clear();
         }
      }
   }

   private static void appendGlyph(Matrix4f matrix, MsdfFont font, MsdfGlyph glyph, float x, float y, float scale, int color, float rotation, float z) {
      GpuTextureView texture = font.getBatchTextureView();
      if (glyphCount == 128 || batchTexture != null && batchTexture != texture) {
         flush();
      }

      batchTexture = texture;
      int offset = glyphData.position();
      matrix.get(offset, glyphData);
      glyphData.position(offset + 64);
      glyphData.putFloat(x + glyph.bearingX * scale).putFloat(y - glyph.bearingY * scale).putFloat(glyph.width * scale).putFloat(glyph.height * scale);
      float u = font.batchU(glyph.u0);
      float v = font.batchV(glyph.v0);
      glyphData.putFloat(u).putFloat(v).putFloat(font.batchU(glyph.u1) - u).putFloat(font.batchV(glyph.v1) - v);
      glyphData.putFloat((color >> 16 & 0xFF) / 255.0F)
         .putFloat((color >> 8 & 0xFF) / 255.0F)
         .putFloat((color & 0xFF) / 255.0F)
         .putFloat((color >>> 24) / 255.0F);
      glyphData.putFloat(font.getPxRange()).putFloat((float)Math.toRadians(rotation)).putFloat(z).putFloat(0.0F);
      glyphCount++;
   }

   private static void drawText(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int color, int[] colors, float rotation, float z) {
      if (font != null && font.isLoaded() && text != null && !text.isEmpty()) {
         a();
         GuiBatch.select(GuiBatch.DrawType.TEXT);
         float scale = size / font.getEmSize();
         float cursor = x;
         float baseline = y + font.getAscender() * scale;

         for (int i = 0; i < text.length(); i++) {
            MsdfGlyph glyph = font.a(text.charAt(i));
            if (glyph != null) {
               if (glyph.width > 0.0F && glyph.height > 0.0F) {
                  appendGlyph(matrix, font, glyph, cursor, baseline, scale, colors == null ? color : colors[i % colors.length], rotation, z);
               }

               cursor += glyph.advance * scale;
            }
         }

         if (!isBatching()) {
            flush();
         }
      }
   }

   public static void a(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int color, float rotation, float z) {
      drawText(matrix, font, text, x, y, size, color, null, rotation, z);
   }

   public static void a(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int[] colors, float z) {
      if (colors != null && colors.length != 0) {
         drawText(matrix, font, text, x, y, size, 0, colors, 0.0F, z);
      }
   }

   public static float a(Matrix4f matrix, MsdfFont font, MsdfGlyph glyph, float x, float y, float size, int color, float rotation, float z) {
      if (font != null && font.isLoaded() && glyph != null) {
         a();
         GuiBatch.select(GuiBatch.DrawType.TEXT);
         if (glyph.width > 0.0F && glyph.height > 0.0F) {
            appendGlyph(matrix, font, glyph, x, y, size, color, rotation, z);
         }

         if (!isBatching()) {
            flush();
         }

         return glyph.advance * size;
      } else {
         return 0.0F;
      }
   }

   public static void d() {
      flush();
      if (glyphBuffer != null) {
         glyphBuffer.close();
         glyphBuffer = null;
      }

      if (glyphData != null) {
         MemoryUtil.memFree(glyphData);
         glyphData = null;
      }

      pipeline = null;
      batchTexture = null;
      writeOffset = 0;
      batchDepth = 0;
   }
}
