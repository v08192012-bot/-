package brokke.cc.render;

import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.ViewModel;
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
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class ViewModelHandOutlineRenderer {
   private static final int UNIFORM_SIZE = 64;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformData;
   private static GpuTexture depthTexture;
   private static GpuTextureView depthTextureView;
   private static int sceneWidth = 0;
   private static int sceneHeight = 0;
   private static boolean initialized = false;
   private static boolean hasDepthSnapshot = false;

   public static void a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null && var0.world != null && var0.player != null) {
         ViewModel var1 = FunctionManager.a(ViewModel.class);
         if (var1 != null && var1.isActive() && var0.currentScreen instanceof ChatScreen) {
            float var2 = Math.max(ViewModel.rightHoldAnim, ViewModel.leftHoldAnim);
            if (var2 <= 0.01F) {
               hasDepthSnapshot = false;
            } else {
               Window var3 = var0.getWindow();
               if (var3 != null) {
                  int var4 = var3.getFramebufferWidth();
                  int var5 = var3.getFramebufferHeight();
                  if (var4 > 1 && var5 > 1) {
                     Framebuffer var6 = var0.getFramebuffer();
                     if (var6 != null && var6.getDepthAttachment() != null) {
                        try {
                           c();
                           if (!a(var4, var5)) {
                              return;
                           }

                           GpuTexture var7 = var6.getDepthAttachment();
                           if ((var7.usage() & 2) != 0) {
                              GuiBatch.flush();
                              CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
                              var8.copyTextureToTexture(var7, depthTexture, 0, 0, 0, 0, 0, var4, var5);
                              hasDepthSnapshot = true;
                           }
                        } catch (Throwable var9) {
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void b() {
      if (hasDepthSnapshot) {
         hasDepthSnapshot = false;
         MinecraftClient var0 = MinecraftClient.getInstance();
         if (var0 != null && var0.world != null && var0.player != null) {
            ViewModel var1 = FunctionManager.a(ViewModel.class);
            if (var1 != null && var1.isActive() && var0.currentScreen instanceof ChatScreen) {
               float var2 = Math.max(ViewModel.rightHoldAnim, ViewModel.leftHoldAnim);
               if (!(var2 <= 0.01F)) {
                  Window var3 = var0.getWindow();
                  if (var3 != null) {
                     int var4 = var3.getFramebufferWidth();
                     int var5 = var3.getFramebufferHeight();
                     if (var4 > 1 && var5 > 1) {
                        Framebuffer var6 = var0.getFramebuffer();
                        if (var6 != null && var6.getColorAttachment() != null && var6.getDepthAttachment() != null) {
                           try {
                              c();
                              if (pipeline == null || depthTextureView == null) {
                                 return;
                              }

                              float var7 = ViewModel.rightHoldAnim;
                              float var8 = ViewModel.leftHoldAnim;
                              boolean var9 = var7 > 0.01F;
                              boolean var10 = var8 > 0.01F;
                              float var11;
                              if (var9 && !var10) {
                                 var11 = 1.0F;
                              } else if (var10 && !var9) {
                                 var11 = 0.0F;
                              } else {
                                 var11 = -1.0F;
                              }

                              boolean var12 = var0.player == null || var0.player.getMainArm() == Arm.RIGHT;
                              float var13 = var12 ? (float)var1.getMainX().getValue() : (float)var1.getOffX().getValue();
                              float var14 = var12 ? (float)var1.getMainY().getValue() : (float)var1.getOffY().getValue();
                              float var15 = var12 ? (float)var1.getOffX().getValue() : (float)var1.getMainX().getValue();
                              float var16 = var12 ? (float)var1.getOffY().getValue() : (float)var1.getMainY().getValue();
                              float var17 = 0.7F + var13 * 0.35F;
                              float var18 = 0.58F - var14 * 0.35F;
                              float var19 = 0.3F + var15 * 0.35F;
                              float var20 = 0.58F - var16 * 0.35F;
                              float var21 = 1.0F / var4;
                              float var22 = 1.0F / var5;
                              float var23 = (float)var4 / Math.max(1, var5);
                              uniformData.clear();
                              uniformData.putFloat(var4).putFloat(var5).putFloat(var21).putFloat(var22);
                              uniformData.putFloat(var2).putFloat(2.0F).putFloat(var11).putFloat(var23);
                              uniformData.putFloat(1.0F).putFloat(1.0F).putFloat(1.0F).putFloat(var2);
                              uniformData.putFloat(var17).putFloat(var18).putFloat(var19).putFloat(var20);
                              uniformData.flip();
                              GuiBatch.flush();
                              CommandEncoder var24 = RenderSystem.getDevice().createCommandEncoder();
                              var24.writeToBuffer(uniformBuffer.slice(), uniformData);
                              GpuSampler var25 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                              GpuSampler var26 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                              try (RenderPass var27 = var24.createRenderPass(
                                    () -> "ViewModelHandOutlineRenderer", var6.getColorAttachmentView(), OptionalInt.empty()
                                 )) {
                                 var27.setPipeline(pipeline);
                                 var27.setUniform("Uniforms", uniformBuffer);
                                 var27.bindTexture("Sampler0", var6.getColorAttachmentView(), var26);
                                 var27.bindTexture("Sampler1", var6.getDepthAttachmentView(), var25);
                                 var27.bindTexture("Sampler2", depthTextureView, var25);
                                 var27.draw(0, 6);
                              }
                           } catch (Throwable var32) {
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void c() {
      if (!initialized) {
         pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "viewmodel_outline"))
            .withVertexShader(Identifier.of("brokke", "viewmodel_outline_vertex"))
            .withFragmentShader(Identifier.of("brokke", "viewmodel_outline_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ViewModelHandOutlineRenderer Uniforms", 136, 64L);
         uniformData = MemoryUtil.memAlloc(64);
         initialized = true;
      }
   }

   private static boolean a(int var0, int var1) {
      if (depthTexture != null && sceneWidth == var0 && sceneHeight == var1) {
         return true;
      }

      d();
      depthTexture = RenderSystem.getDevice().createTexture(() -> "brokke:viewmodel_depth", 13, TextureFormat.DEPTH32, var0, var1, 1, 1);
      depthTextureView = RenderSystem.getDevice().createTextureView(depthTexture);
      sceneWidth = var0;
      sceneHeight = var1;
      return true;
   }

   private static void d() {
      if (depthTextureView != null) {
         depthTextureView.close();
         depthTextureView = null;
      }

      if (depthTexture != null) {
         depthTexture.close();
         depthTexture = null;
      }

      sceneWidth = 0;
      sceneHeight = 0;
   }
}
