package brokke.cc.cosmetic;

import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class CosmeticPreviewRenderer {
   private static final int TEXTURE_SIZE = 96;
   private static GpuTexture colorTexture;
   private static GpuTextureView colorTextureView;
   private static GpuTexture depthTexture;
   private static GpuTextureView depthTextureView;
   private static ProjectionMatrix2 projectionMatrix;
   private static final Map<Integer, float[]> BOUNDS_CACHE = new ConcurrentHashMap<>();

   private static void b() {
      if (colorTexture == null || colorTexture.isClosed()) {
         BOUNDS_CACHE.clear();
         GpuDevice var0 = RenderSystem.getDevice();
         colorTexture = var0.createTexture(() -> "CosmeticPreview Color", 12, TextureFormat.RGBA8, 96, 96, 1, 1);
         colorTextureView = var0.createTextureView(colorTexture);
         depthTexture = var0.createTexture(() -> "CosmeticPreview Depth", 8, TextureFormat.DEPTH32, 96, 96, 1, 1);
         depthTextureView = var0.createTextureView(depthTexture);
         projectionMatrix = new ProjectionMatrix2("CosmeticPreview Projection", -1000.0F, 1000.0F, true);
      }
   }

   public static void a() {
      BOUNDS_CACHE.clear();
      if (colorTexture != null) {
         colorTexture.close();
         colorTexture = null;
      }

      if (colorTextureView != null) {
         colorTextureView.close();
         colorTextureView = null;
      }

      if (depthTexture != null) {
         depthTexture.close();
         depthTexture = null;
      }

      if (depthTextureView != null) {
         depthTextureView.close();
         depthTextureView = null;
      }

      if (projectionMatrix != null) {
         projectionMatrix.close();
         projectionMatrix = null;
      }
   }

   private static float[] a(int var0, CosmeticModel var1, GeoModel var2) {
      return BOUNDS_CACHE.computeIfAbsent(var0, var1x -> {
         float[] var2x = new float[]{Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
         float[] var3 = new float[]{Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
         boolean[] var4 = new boolean[]{false};
         if (var2 != null && var2.topLevelBones != null) {
            MatrixStack var5 = new MatrixStack();

            for (GeoBone var7 : var2.topLevelBones) {
               a(var7, var5, var2x, var3, var4);
            }
         }

         if (!var4[0]) {
            return new float[]{0.0F, 0.0F, 0.0F, 1.0F};
         }

         float var12 = (var2x[0] + var3[0]) / 2.0F;
         float var13 = (var2x[1] + var3[1]) / 2.0F;
         float var14 = (var2x[2] + var3[2]) / 2.0F;
         float var8 = var3[0] - var2x[0];
         float var9 = var3[1] - var2x[1];
         float var10 = var3[2] - var2x[2];
         float var11 = (float)Math.sqrt(var8 * var8 + var9 * var9 + var10 * var10);
         if (var11 < 0.1F) {
            var11 = 1.0F;
         }

         return new float[]{var12, var13, var14, var11};
      });
   }

   private static void a(GeoBone var0, MatrixStack var1, float[] var2, float[] var3, boolean[] var4) {
      if (var0 != null && !var0.isHidden) {
         var1.push();
         GeckoRenderHelper.a(var0, var1);
         GeckoRenderHelper.b(var0, var1);
         GeckoRenderHelper.d(var0, var1);
         GeckoRenderHelper.e(var0, var1);
         GeckoRenderHelper.c(var0, var1);
         if (var0.childCubes != null) {
            for (GeoCube var6 : var0.childCubes) {
               if (var6 != null) {
                  var1.push();
                  GeckoRenderHelper.a(var6, var1);
                  GeckoRenderHelper.c(var6, var1);
                  GeckoRenderHelper.b(var6, var1);
                  Matrix4f var7 = var1.peek().getPositionMatrix();
                  if (var6.quads != null) {
                     for (GeoQuad var11 : var6.quads) {
                        if (var11 != null && var11.vertices != null) {
                           for (GeoVertex var15 : var11.vertices) {
                              if (var15 != null && var15.position != null) {
                                 Vector4f var16 = new Vector4f(var15.position.getX(), var15.position.getY(), var15.position.getZ(), 1.0F);
                                 var7.transform(var16);
                                 if (var16.x < var2[0]) {
                                    var2[0] = var16.x;
                                 }

                                 if (var16.x > var3[0]) {
                                    var3[0] = var16.x;
                                 }

                                 if (var16.y < var2[1]) {
                                    var2[1] = var16.y;
                                 }

                                 if (var16.y > var3[1]) {
                                    var3[1] = var16.y;
                                 }

                                 if (var16.z < var2[2]) {
                                    var2[2] = var16.z;
                                 }

                                 if (var16.z > var3[2]) {
                                    var3[2] = var16.z;
                                 }

                                 var4[0] = true;
                              }
                           }
                        }
                     }
                  }

                  var1.pop();
               }
            }
         }

         if (var0.childBones != null) {
            for (GeoBone var18 : var0.childBones) {
               a(var18, var1, var2, var3, var4);
            }
         }

         var1.pop();
      }
   }

   public static void a(int var0, float var1, float var2, float var3, float var4, float var5, boolean var6) {
      if (!(var5 <= 0.01F)) {
         CosmeticModel var7 = LocalCosmetics.g(var0);
         if (var7 == null) {
            a(var0, var1, var2, var3, var4);
         } else {
            Identifier var8 = LocalCosmetics.d(var0);
            if (var8 != null) {
               var7.setTextureId(var8);
            }

            GeoModel var9 = GeckolibCosmeticRenderer.a().a(var7);
            if (var9 == null) {
               a(var0, var1, var2, var3, var4);
            } else {
               boolean var10 = false;
               boolean var11 = false;
               boolean var12 = false;
               Matrix4fStack var13 = null;

               try {
                  b();
                  MinecraftClient var14 = MinecraftClient.getInstance();
                  Immediate var15 = var14.getBufferBuilders().getEntityVertexConsumers();
                  var15.draw();
                  var11 = GL11.glIsEnabled(3089);
                  if (var11) {
                     GL11.glDisable(3089);
                  }

                  CommandEncoder var16 = RenderSystem.getDevice().createCommandEncoder();
                  var16.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);
                  RenderSystem.backupProjectionMatrix();
                  var12 = true;
                  GpuBufferSlice var17 = projectionMatrix.set(96.0F, 96.0F);
                  RenderSystem.setProjectionMatrix(var17, ProjectionType.ORTHOGRAPHIC);
                  RenderSystem.outputColorTextureOverride = colorTextureView;
                  RenderSystem.outputDepthTextureOverride = depthTextureView;
                  DiffuseLighting var18 = var14.gameRenderer.getDiffuseLighting();
                  var18.setShaderLights(Type.ENTITY_IN_UI);
                  var13 = RenderSystem.getModelViewStack();
                  var13.pushMatrix();
                  var13.identity();
                  MatrixStack var19 = new MatrixStack();
                  var19.push();
                  var19.translate(48.0F, 48.0F, 0.0F);
                  var19.multiply(RotationAxis.POSITIVE_X.rotationDegrees(16.0F));
                  float var20 = var6 ? 2400.0F : 4000.0F;
                  float var21 = (float)((System.currentTimeMillis() + var0 * 260L) % (long)var20) / var20 * 360.0F;
                  var19.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var21));
                  float[] var22 = a(var0, var7, var9);
                  float var23 = var22[0];
                  float var24 = var22[1];
                  float var25 = var22[2];
                  float var26 = var22[3];
                  float var27 = 1.0F;
                  if (var7.getPreviewScale() > 0.1F && var7.getPreviewScale() != 1.0F) {
                     var27 = Math.clamp(var7.getPreviewScale(), 0.8F, 1.15F);
                  }

                  float var28 = 68.0F * var27;
                  float var29 = var28 / var26;
                  var19.scale(var29, var29, -var29);
                  var19.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                  var19.translate(-var23, -var24, -var25);
                  GeckolibCosmeticRenderer.a().a(var7, var19, var15, 15728880);
                  RenderLayer var30 = RenderLayers.entityCutoutNoCull(var7.getTextureId());
                  var15.draw(var30);
                  var15.draw();
                  var19.pop();
                  var13.popMatrix();
                  var13 = null;
                  RenderSystem.outputColorTextureOverride = null;
                  RenderSystem.outputDepthTextureOverride = null;
                  RenderSystem.restoreProjectionMatrix();
                  var12 = false;
                  var10 = true;
               } catch (Exception var38) {
                  if (var13 != null) {
                     try {
                        var13.popMatrix();
                     } catch (Exception var37) {
                     }
                  }

                  if (var12) {
                     RenderSystem.outputColorTextureOverride = null;
                     RenderSystem.outputDepthTextureOverride = null;

                     try {
                        RenderSystem.restoreProjectionMatrix();
                     } catch (Exception var36) {
                     }
                  }
               } finally {
                  if (var11) {
                     GL11.glEnable(3089);
                  }
               }

               if (var10) {
                  int var40 = (int)(255.0F * var5) << 24 | 16777215;
                  TexturePipeline.a(RenderUtil.c(), var1, var2, var3, var4, colorTextureView, var40, 3.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
               } else {
                  a(var0, var1, var2, var3, var4);
               }
            }
         }
      }
   }

   private static void a(int var0, float var1, float var2, float var3, float var4) {
      try {
         RenderUtil.a(var1, var2, var3, var4, LocalCosmetics.d(var0), 3.0F, Color.WHITE);
      } catch (Exception var6) {
      }
   }
}
