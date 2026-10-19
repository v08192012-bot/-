package cmode.render;

import cmode.core.LegacyRenderCompat;
import cmode.manager.FriendManager;
import cmode.module.render.FriendsMarkers;
import cmode.system.events.impl.Render3DEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryUtil;

public final class ShaderEspRenderer {
   private static SimpleFramebuffer silhouetteFb;
   private static GpuTexture blurTextureA;
   private static GpuTextureView blurTextureAView;
   private static GpuTexture blurTextureB;
   private static GpuTextureView blurTextureBView;
   private static RenderPipeline blurHPipeline;
   private static RenderPipeline blurVPipeline;
   private static RenderPipeline compositePipeline;
   private static GpuBuffer blurUniformBuffer;
   private static ByteBuffer blurUniformData;
   private static GpuBuffer compositeUniformBuffer;
   private static ByteBuffer compositeUniformData;
   private static int lastWidth = 0;
   private static int lastHeight = 0;
   private static int lastBlurWidth = 0;
   private static int lastBlurHeight = 0;
   private static boolean initialized = false;
   private static final int BLUR_UBO_SIZE = 16;
   private static final int COMPOSITE_UBO_SIZE = 64;

   private ShaderEspRenderer() {
   }

   public static void a(Render3DEvent var0, FriendsMarkers var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2 != null && var2.world != null && var2.player != null) {
         Camera var3 = var0.getCamera();
         if (var3 != null) {
            Window var4 = var2.getWindow();
            if (var4 != null) {
               int var5 = var4.getFramebufferWidth();
               int var6 = var4.getFramebufferHeight();
               if (var5 > 1 && var6 > 1) {
                  ArrayList var7 = new ArrayList();
                  a(var2, var7);
                  if (!var7.isEmpty()) {
                     b();
                     a(var5, var6);
                     float var9 = var0.getTickDelta();
                     float var10 = (float)var1.getGlowRadius().getValue();
                     boolean var11 = var1.getThroughWalls().isToggled();
                     a(var2, var7, var0.getMatrices(), var3, var9, var11);
                     a(var5, var6, var10);
                     a(var2, var5, var6, var1);
                  }
               }
            }
         }
      }
   }

   private static void a(MinecraftClient var0, List<Entity> var1) {
      for (PlayerEntity var2 : var0.world.getPlayers()) {
         if (var2 != null
            && var2 != var0.player
            && var2.isAlive()
            && !var2.isRemoved()
            && FriendManager.a(var2.getName().getString())) {
            var1.add(var2);
         }
      }
   }

   private static void a(MinecraftClient var0, List<Entity> var1, MatrixStack var2, Camera var3, float var4, boolean var5) {
      GuiBatch.flush();
      CommandEncoder var6 = RenderSystem.getDevice().createCommandEncoder();
      var6.clearColorAndDepthTextures(silhouetteFb.getColorAttachment(), 0, silhouetteFb.getDepthAttachment(), 1.0);
      RenderSystem.outputColorTextureOverride = silhouetteFb.getColorAttachmentView();
      RenderSystem.outputDepthTextureOverride = silhouetteFb.getDepthAttachmentView();
      EntityRenderManager var7 = var0.getEntityRenderDispatcher();
      Immediate var8 = var0.getBufferBuilders().getEntityVertexConsumers();
      Vec3d var9 = var3.getCameraPos();

      for (Entity var11 : var1) {
         Vec3d var12 = var11.getLerpedPos(var4);
         var2.push();
         var2.translate(var12.x - var9.x, var12.y - var9.y, var12.z - var9.z);

         try {
            LegacyRenderCompat.a(var7, var11, var4, 0.0, 0.0, 0.0, var2, var8);
         } catch (Exception var15) {
         }

         var2.pop();
      }

      try {
         var8.draw();
      } catch (Exception var14) {
      }

      RenderSystem.outputColorTextureOverride = null;
      RenderSystem.outputDepthTextureOverride = null;
   }

   private static void a(int var0, int var1, float var2) {
      GuiBatch.flush();
      CommandEncoder var3 = RenderSystem.getDevice().createCommandEncoder();
      GpuSampler var4 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      float var5 = Math.max(1.0F, var2 / 3.0F);
      blurUniformData.clear();
      blurUniformData.putFloat(1.0F / lastBlurWidth);
      blurUniformData.putFloat(1.0F / lastBlurHeight);
      blurUniformData.putFloat(var5);
      blurUniformData.putFloat(0.0F);
      blurUniformData.flip();
      var3.writeToBuffer(blurUniformBuffer.slice(), blurUniformData);

      try (RenderPass var6 = var3.createRenderPass(() -> "ShaderEsp Blur H 1", blurTextureAView, OptionalInt.empty())) {
         var6.setPipeline(blurHPipeline);
         var6.setUniform("BlurUniforms", blurUniformBuffer);
         var6.bindTexture("Sampler0", silhouetteFb.getColorAttachmentView(), var4);
         var6.draw(0, 6);
      }

      try (RenderPass var17 = var3.createRenderPass(() -> "ShaderEsp Blur V 1", blurTextureBView, OptionalInt.empty())) {
         var17.setPipeline(blurVPipeline);
         var17.setUniform("BlurUniforms", blurUniformBuffer);
         var17.bindTexture("Sampler0", blurTextureAView, var4);
         var17.draw(0, 6);
      }

      blurUniformData.clear();
      blurUniformData.putFloat(1.0F / lastBlurWidth);
      blurUniformData.putFloat(1.0F / lastBlurHeight);
      blurUniformData.putFloat(var5 * 2.0F);
      blurUniformData.putFloat(0.0F);
      blurUniformData.flip();
      var3.writeToBuffer(blurUniformBuffer.slice(), blurUniformData);

      try (RenderPass var18 = var3.createRenderPass(() -> "ShaderEsp Blur H 2", blurTextureAView, OptionalInt.empty())) {
         var18.setPipeline(blurHPipeline);
         var18.setUniform("BlurUniforms", blurUniformBuffer);
         var18.bindTexture("Sampler0", blurTextureBView, var4);
         var18.draw(0, 6);
      }

      try (RenderPass var19 = var3.createRenderPass(() -> "ShaderEsp Blur V 2", blurTextureBView, OptionalInt.empty())) {
         var19.setPipeline(blurVPipeline);
         var19.setUniform("BlurUniforms", blurUniformBuffer);
         var19.bindTexture("Sampler0", blurTextureAView, var4);
         var19.draw(0, 6);
      }
   }

   private static void a(MinecraftClient var0, int var1, int var2, FriendsMarkers var3) {
      GuiBatch.flush();
      CommandEncoder var4 = RenderSystem.getDevice().createCommandEncoder();
      GpuSampler var5 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      Color var6 = var3.getColor();
      compositeUniformData.clear();
      compositeUniformData.putFloat(1.0F / var1);
      compositeUniformData.putFloat(1.0F / var2);
      compositeUniformData.putFloat(0.0F);
      compositeUniformData.putFloat(0.0F);
      compositeUniformData.putFloat((float)var3.getOutlineThickness().getValue());
      compositeUniformData.putFloat((float)var3.getOutlineStrength().getValue());
      compositeUniformData.putFloat((float)var3.getGlowIntensity().getValue());
      compositeUniformData.putFloat(var3.getFill().isToggled() ? (float)var3.getFillOpacity().getValue() : 0.0F);
      compositeUniformData.putFloat(var6.getRed() / 255.0F);
      compositeUniformData.putFloat(var6.getGreen() / 255.0F);
      compositeUniformData.putFloat(var6.getBlue() / 255.0F);
      compositeUniformData.putFloat(var6.getAlpha() / 255.0F);
      compositeUniformData.putFloat(var6.getRed() / 255.0F);
      compositeUniformData.putFloat(var6.getGreen() / 255.0F);
      compositeUniformData.putFloat(var6.getBlue() / 255.0F);
      compositeUniformData.putFloat(var6.getAlpha() / 255.0F);
      compositeUniformData.flip();
      var4.writeToBuffer(compositeUniformBuffer.slice(), compositeUniformData);

      try (RenderPass var7 = var4.createRenderPass(() -> "ShaderEsp Composite Friends", var0.getFramebuffer().getColorAttachmentView(), OptionalInt.empty())) {
         var7.setPipeline(compositePipeline);
         var7.setUniform("CompositeUniforms", compositeUniformBuffer);
         var7.bindTexture("Sampler0", silhouetteFb.getColorAttachmentView(), var5);
         var7.bindTexture("Sampler1", blurTextureBView, var5);
         var7.draw(0, 6);
      }
   }

   private static void b() {
      if (!initialized) {
         blurHPipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("cmode", "shader_esp_blur_h"))
            .withVertexShader(Identifier.of("cmode", "shader_esp_vertex"))
            .withFragmentShader(Identifier.of("cmode", "shader_esp_blur_h"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("BlurUniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         blurVPipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("cmode", "shader_esp_blur_v"))
            .withVertexShader(Identifier.of("cmode", "shader_esp_vertex"))
            .withFragmentShader(Identifier.of("cmode", "shader_esp_blur_v"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("BlurUniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         compositePipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("cmode", "shader_esp_composite"))
            .withVertexShader(Identifier.of("cmode", "shader_esp_vertex"))
            .withFragmentShader(Identifier.of("cmode", "shader_esp_composite"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("CompositeUniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         blurUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ShaderEsp Blur Uniforms", 136, 16L);
         blurUniformData = MemoryUtil.memAlloc(16);
         compositeUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ShaderEsp Composite Uniforms", 136, 64L);
         compositeUniformData = MemoryUtil.memAlloc(64);
         initialized = true;
      }
   }

   private static void a(int var0, int var1) {
      int var2 = Math.max(1, var0 / 2);
      int var3 = Math.max(1, var1 / 2);
      if (silhouetteFb == null
         || blurTextureA == null
         || blurTextureB == null
         || lastWidth != var0
         || lastHeight != var1
         || lastBlurWidth != var2
         || lastBlurHeight != var3) {
         c();
         silhouetteFb = new SimpleFramebuffer("ShaderESP Silhouette", var0, var1, true);
         blurTextureA = RenderSystem.getDevice().createTexture(() -> "cmode:shader_esp_blur_a", 13, TextureFormat.RGBA8, var2, var3, 1, 1);
         blurTextureAView = RenderSystem.getDevice().createTextureView(blurTextureA);
         blurTextureB = RenderSystem.getDevice().createTexture(() -> "cmode:shader_esp_blur_b", 13, TextureFormat.RGBA8, var2, var3, 1, 1);
         blurTextureBView = RenderSystem.getDevice().createTextureView(blurTextureB);
         lastWidth = var0;
         lastHeight = var1;
         lastBlurWidth = var2;
         lastBlurHeight = var3;
      }
   }

   private static void c() {
      if (silhouetteFb != null) {
         silhouetteFb.delete();
         silhouetteFb = null;
      }

      if (blurTextureAView != null) {
         blurTextureAView.close();
         blurTextureAView = null;
      }

      if (blurTextureA != null) {
         blurTextureA.close();
         blurTextureA = null;
      }

      if (blurTextureBView != null) {
         blurTextureBView.close();
         blurTextureBView = null;
      }

      if (blurTextureB != null) {
         blurTextureB.close();
         blurTextureB = null;
      }

      lastWidth = 0;
      lastHeight = 0;
      lastBlurWidth = 0;
      lastBlurHeight = 0;
   }

   public static void a() {
      c();
      if (blurUniformBuffer != null) {
         blurUniformBuffer.close();
         blurUniformBuffer = null;
      }

      if (blurUniformData != null) {
         MemoryUtil.memFree(blurUniformData);
         blurUniformData = null;
      }

      if (compositeUniformBuffer != null) {
         compositeUniformBuffer.close();
         compositeUniformBuffer = null;
      }

      if (compositeUniformData != null) {
         MemoryUtil.memFree(compositeUniformData);
         compositeUniformData = null;
      }

      initialized = false;
   }
}
