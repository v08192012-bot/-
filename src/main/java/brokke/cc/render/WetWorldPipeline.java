package brokke.cc.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
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
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class WetWorldPipeline implements AutoCloseable {
   private static final WetWorldPipeline INSTANCE = new WetWorldPipeline();
   private static final int UNIFORM_SIZE = 288;
   private RenderPipeline pipeline;
   private GpuBuffer uniformBuffer;
   private ByteBuffer uniformData;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private int sceneWidth;
   private int sceneHeight;
   private boolean initialized;
   private boolean disabled;
   private boolean depthDirectWarned;

   private WetWorldPipeline() {
   }

   public static WetWorldPipeline getInstance() {
      return INSTANCE;
   }

   public static void b() {
      INSTANCE.disabled = false;
   }

   public static void a(
      Matrix4f var0,
      Matrix4f var1,
      Matrix4f var2,
      Vec3d var3,
      float var4,
      float var5,
      float var6,
      float var7,
      int var8,
      boolean var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      Color var15,
      float var16
   ) {
      INSTANCE.b(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14, var15, var16);
   }

   public void b(
      Matrix4f var1,
      Matrix4f var2,
      Matrix4f var3,
      Vec3d var4,
      float var5,
      float var6,
      float var7,
      float var8,
      int var9,
      boolean var10,
      float var11,
      float var12,
      float var13,
      float var14,
      float var15,
      Color var16,
      float var17
   ) {
      if (!this.disabled && var1 != null && var2 != null && var3 != null && var4 != null) {
         MinecraftClient var18 = MinecraftClient.getInstance();
         if (var18 != null && var18.world != null && var18.player != null && !(var6 <= 1.0E-4F)) {
            Window var19 = var18.getWindow();
            if (var19 != null) {
               int var20 = var19.getFramebufferWidth();
               int var21 = var19.getFramebufferHeight();
               if (var20 > 1 && var21 > 1) {
                  Framebuffer var22 = var18.getFramebuffer();
                  if (var22 != null && var22.getColorAttachment() != null && var22.getDepthAttachment() != null) {
                     try {
                        this.c();
                        if (this.disabled || !this.a(var20, var21)) {
                           return;
                        }

                        GuiBatch.flush();
                        CommandEncoder var23 = RenderSystem.getDevice().createCommandEncoder();
                        var23.copyTextureToTexture(var22.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var20, var21);
                        boolean var24 = false;
                        GpuTexture var25 = var22.getDepthAttachment();
                        if ((var25.usage() & 2) != 0) {
                           var23.copyTextureToTexture(var25, this.depthTexture, 0, 0, 0, 0, 0, var20, var21);
                           var24 = true;
                        } else {
                           if ((var25.usage() & 4) == 0) {
                              this.disabled = true;
                              return;
                           }

                           if (!this.depthDirectWarned) {
                              this.depthDirectWarned = true;
                           }
                        }

                        this.a(var20, var21, var2, var1, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17);
                        var23.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                        GpuSampler var26 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                        GpuSampler var27 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                        GpuTextureView var28 = var24 ? this.depthTextureView : var22.getDepthAttachmentView();

                        try (RenderPass var29 = var24
                              ? var23.createRenderPass(
                                 () -> "WetWorldPipeline",
                                 var22.getColorAttachmentView(),
                                 OptionalInt.empty(),
                                 var22.getDepthAttachmentView(),
                                 OptionalDouble.empty()
                              )
                              : var23.createRenderPass(() -> "WetWorldPipeline", var22.getColorAttachmentView(), OptionalInt.empty())) {
                           var29.setPipeline(this.pipeline);
                           var29.setUniform("Uniforms", this.uniformBuffer);
                           var29.bindTexture("Sampler0", this.sceneTextureView, var26);
                           var29.bindTexture("Sampler1", var28, var27);
                           var29.draw(0, 3);
                        }
                     } catch (Throwable var34) {
                        this.disabled = true;
                     }
                  }
               }
            }
         }
      }
   }

   private void c() {
      if (!this.initialized) {
         this.pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "wet_world"))
            .withVertexShader(Identifier.of("brokke", "wet_world_vertex"))
            .withFragmentShader(Identifier.of("brokke", "wet_world_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "WetWorldPipeline Uniforms", 136, 288L);
         this.uniformData = MemoryUtil.memAlloc(288);
         this.initialized = true;
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.depthTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.d();
      this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "brokke:wet_world_scene", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.depthTexture = RenderSystem.getDevice().createTexture(() -> "brokke:wet_world_depth", 13, TextureFormat.DEPTH32, var1, var2, 1, 1);
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.sceneWidth = var1;
      this.sceneHeight = var2;
      return true;
   }

   private void a(
      int var1,
      int var2,
      Matrix4f var3,
      Matrix4f var4,
      Matrix4f var5,
      Vec3d var6,
      float var7,
      float var8,
      float var9,
      float var10,
      int var11,
      boolean var12,
      float var13,
      float var14,
      float var15,
      float var16,
      float var17,
      Color var18,
      float var19
   ) {
      this.uniformData.clear();
      a(this.uniformData, var3);
      a(this.uniformData, var4);
      a(this.uniformData, var5);
      this.uniformData.putFloat((float)var6.x).putFloat((float)var6.y).putFloat((float)var6.z).putFloat(var7);
      this.uniformData.putFloat(var1).putFloat(var2).putFloat(0.0F).putFloat(0.0F);
      this.uniformData.putFloat(var8).putFloat(var9).putFloat(var10).putFloat(0.0F);
      this.uniformData.putFloat(var11).putFloat(var12 ? 1.0F : 0.0F).putFloat(var13).putFloat(var14);
      this.uniformData.putFloat(var15).putFloat(var16).putFloat(var17).putFloat(var19);
      if (var18 != null) {
         this.uniformData
            .putFloat(var18.getRed() / 255.0F)
            .putFloat(var18.getGreen() / 255.0F)
            .putFloat(var18.getBlue() / 255.0F)
            .putFloat(var18.getAlpha() / 255.0F);
      } else {
         this.uniformData.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      }

      this.uniformData.flip();
   }

   private static void a(ByteBuffer var0, Matrix4f var1) {
      var0.putFloat(var1.m00()).putFloat(var1.m01()).putFloat(var1.m02()).putFloat(var1.m03());
      var0.putFloat(var1.m10()).putFloat(var1.m11()).putFloat(var1.m12()).putFloat(var1.m13());
      var0.putFloat(var1.m20()).putFloat(var1.m21()).putFloat(var1.m22()).putFloat(var1.m23());
      var0.putFloat(var1.m30()).putFloat(var1.m31()).putFloat(var1.m32()).putFloat(var1.m33());
   }

   private void d() {
      if (this.sceneTextureView != null) {
         this.sceneTextureView.close();
         this.sceneTextureView = null;
      }

      if (this.sceneTexture != null) {
         this.sceneTexture.close();
         this.sceneTexture = null;
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }
   }

   @Override
   public void close() {
      this.d();
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }

      if (this.uniformData != null) {
         MemoryUtil.memFree(this.uniformData);
         this.uniformData = null;
      }

      this.initialized = false;
      this.disabled = false;
      this.depthDirectWarned = false;
      this.sceneWidth = 0;
      this.sceneHeight = 0;
   }
}
