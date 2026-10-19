package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;

public class ColorPipeline {
   public static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static RenderPipeline additivePipeline;
   private static RenderPipeline translucentPipeline;
   private static GpuBuffer vbo;
   private static final BlendFunction ADDITIVE_ALPHA = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);

   private static void b() {
      if (additivePipeline == null) {
         try {
            additivePipeline = c().withBlend(ADDITIVE_ALPHA).build();
            translucentPipeline = c().withBlend(BlendFunction.TRANSLUCENT).build();
         } catch (Exception var1) {
         }
      }
   }

   private static Builder c() {
      return RenderPipeline.builder()
         .withLocation(Identifier.of("brokke", "color"))
         .withVertexShader(Identifier.of("brokke", "color_vertex"))
         .withFragmentShader(Identifier.of("brokke", "color_fragment"))
         .withVertexFormat(FORMAT, DrawMode.TRIANGLES)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false);
   }

   private static RenderPipeline a(boolean var0) {
      b();
      return var0 ? additivePipeline : translucentPipeline;
   }

   public static BufferBuilder a() {
      b();
      return additivePipeline == null ? null : Tessellator.getInstance().begin(DrawMode.TRIANGLES, FORMAT);
   }

   public static void a(BufferBuilder var0, boolean var1) {
      if (var0 != null && a(var1) != null) {
         BuiltBuffer var2;
         try {
            var2 = var0.end();
         } catch (Exception var13) {
            return;
         }

         int var3 = var2.getDrawParameters().indexCount();
         if (var3 <= 0) {
            var2.close();
         } else {
            int var4 = var2.getBuffer().remaining();
            if (vbo == null || vbo.size() < var4) {
               if (vbo != null) {
                  vbo.close();
               }

               vbo = RenderSystem.getDevice().createBuffer(() -> "ColorPipeline VBO", 40, Math.max(var4, 4096));
            }

            GuiBatch.flush();
            CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
            var5.writeToBuffer(vbo.slice(0L, var4), var2.getBuffer());
            ShapeIndexBuffer var6 = RenderSystem.getSequentialBuffer(DrawMode.TRIANGLES);
            GpuBuffer var7 = var6.getIndexBuffer(var3);
            Framebuffer var8 = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass var9 = var5.createRenderPass(() -> "ColorPipeline", var8.getColorAttachmentView(), OptionalInt.empty())) {
               var9.setPipeline(a(var1));
               Scissor.apply(var9);
               var9.setVertexBuffer(0, vbo);
               var9.setIndexBuffer(var7, var6.getIndexType());
               var9.drawIndexed(0, 0, var3, 1);
            }

            var2.close();
         }
      }
   }

   public static void a(VertexConsumer var0, float var1, float var2, int var3) {
      float var4 = var1 / RenderUtil.a() * 2.0F - 1.0F;
      float var5 = 1.0F - var2 / RenderUtil.b() * 2.0F;
      var0.vertex(var4, var5, 0.0F).color(var3);
   }

   public static void a(BufferBuilder var0, float var1, float var2, float var3, float var4, int var5) {
      a(var0, var1, var2, var5);
      a(var0, var1, var2 + var4, var5);
      a(var0, var1 + var3, var2 + var4, var5);
      a(var0, var1, var2, var5);
      a(var0, var1 + var3, var2 + var4, var5);
      a(var0, var1 + var3, var2, var5);
   }

   public static void a(BufferBuilder var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      a(var0, var1, var2, var3, var4, var5, var5, var6, var6);
   }

   public static void a(BufferBuilder var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8) {
      float var9 = var3 - var1;
      float var10 = var4 - var2;
      float var11 = (float)Math.sqrt(var9 * var9 + var10 * var10);
      if (!(var11 < 1.0E-4F)) {
         float var12 = -var10 / var11;
         float var13 = var9 / var11;
         float var14 = 0.85F;
         float var15 = Math.max(0.05F, var5 * 0.5F - var14 * 0.5F);
         float var16 = Math.max(0.05F, var6 * 0.5F - var14 * 0.5F);
         float var17 = var15 + var14;
         float var18 = var16 + var14;
         int var19 = var7 & 16777215;
         int var20 = var8 & 16777215;
         float var21 = var1 - var12 * var17;
         float var22 = var2 - var13 * var17;
         float var23 = var1 - var12 * var15;
         float var24 = var2 - var13 * var15;
         float var25 = var1 + var12 * var15;
         float var26 = var2 + var13 * var15;
         float var27 = var1 + var12 * var17;
         float var28 = var2 + var13 * var17;
         float var29 = var3 - var12 * var18;
         float var30 = var4 - var13 * var18;
         float var31 = var3 - var12 * var16;
         float var32 = var4 - var13 * var16;
         float var33 = var3 + var12 * var16;
         float var34 = var4 + var13 * var16;
         float var35 = var3 + var12 * var18;
         float var36 = var4 + var13 * var18;
         a(var0, var21, var22, var19);
         a(var0, var29, var30, var20);
         a(var0, var31, var32, var8);
         a(var0, var21, var22, var19);
         a(var0, var31, var32, var8);
         a(var0, var23, var24, var7);
         a(var0, var23, var24, var7);
         a(var0, var31, var32, var8);
         a(var0, var33, var34, var8);
         a(var0, var23, var24, var7);
         a(var0, var33, var34, var8);
         a(var0, var25, var26, var7);
         a(var0, var25, var26, var7);
         a(var0, var33, var34, var8);
         a(var0, var35, var36, var20);
         a(var0, var25, var26, var7);
         a(var0, var35, var36, var20);
         a(var0, var27, var28, var19);
      }
   }

   public static void a(BufferBuilder var0, float[] var1, float[] var2, float[] var3, float[] var4, int var5, int var6, int var7, int var8) {
      if (var1 != null && var2 != null && var3 != null && var4 != null) {
         a(var0, var1[0], var1[1], var5);
         a(var0, var2[0], var2[1], var6);
         a(var0, var3[0], var3[1], var7);
         a(var0, var1[0], var1[1], var5);
         a(var0, var3[0], var3[1], var7);
         a(var0, var4[0], var4[1], var8);
      }
   }

   public static void a(BufferBuilder var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      a(var0, var1, var2, var7);
      a(var0, var3, var4, var7);
      a(var0, var5, var6, var7);
   }
}
