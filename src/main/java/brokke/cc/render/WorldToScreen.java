package brokke.cc.render;

import brokke.cc.mixins.render.IGameRenderer;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

public final class WorldToScreen {
   private static final float NEAR_CLIP = 0.05F;
   private static final ThreadLocal<Vector4f> VEC = ThreadLocal.withInitial(Vector4f::new);

   private WorldToScreen() {
   }

   public static float[] a(Vec3d var0) {
      return a(var0.x, var0.y, var0.z);
   }

   public static float[] a(double var0, double var2, double var4) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      if (var6.player != null && var6.gameRenderer != null) {
         Camera var7 = var6.gameRenderer.getCamera();
         Vec3d var8 = var7.getCameraPos();
         if (RenderContext.WORLD_READY) {
            Vector4f var54 = VEC.get();
            var54.set((float)(var0 - var8.x), (float)(var2 - var8.y), (float)(var4 - var8.z), 1.0F);
            RenderContext.VIEW_PROJECTION_MATRIX.transform(var54);
            if (var54.w <= 0.05F) {
               return null;
            }

            float var10 = var54.x / var54.w;
            float var55 = var54.y / var54.w;
            float var12 = RenderUtil.a();
            float var56 = RenderUtil.b();
            float var14 = (var10 + 1.0F) * 0.5F * var12;
            float var57 = (1.0F - var55) * 0.5F * var56;
            return new float[]{var14, var57, 1.0F / var54.w};
         } else {
            double var9 = var0 - var8.x;
            double var11 = var2 - var8.y;
            double var13 = var4 - var8.z;
            double var15 = Math.toRadians(var7.getYaw());
            double var17 = Math.toRadians(var7.getPitch());
            double var19 = Math.cos(var15);
            double var21 = Math.sin(var15);
            double var23 = Math.cos(var17);
            double var25 = Math.sin(var17);
            double var27 = -var21 * var23;
            double var29 = -var25;
            double var31 = var19 * var23;
            double var33 = -var19;
            double var35 = -var21;
            double var37 = -var21 * var25;
            double var39 = var23;
            double var41 = var19 * var25;
            double var43 = var9 * var33 + var13 * var35;
            double var45 = var9 * var37 + var11 * var39 + var13 * var41;
            double var47 = var9 * var27 + var11 * var29 + var13 * var31;
            if (var47 < 0.05F) {
               return null;
            }

            float var49 = RenderUtil.a();
            float var50 = RenderUtil.b();
            float var51 = ((IGameRenderer)var6.gameRenderer).invokeGetFov(var7, var6.getRenderTickCounter().getTickProgress(true), true);
            double var52 = var50 / 2.0 / Math.tan(Math.toRadians(var51) / 2.0);
            return new float[]{var49 / 2.0F + (float)(var43 / var47 * var52), var50 / 2.0F - (float)(var45 / var47 * var52), (float)(var52 / var47)};
         }
      } else {
         return null;
      }
   }

   public static float[][] a(Vec3d var0, Vec3d var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.gameRenderer != null) {
         Camera var3 = var2.gameRenderer.getCamera();
         Vec3d var4 = var3.getCameraPos();
         float var5 = RenderUtil.a();
         float var6 = RenderUtil.b();
         if (RenderContext.WORLD_READY) {
            Vector4f var68 = new Vector4f((float)(var0.x - var4.x), (float)(var0.y - var4.y), (float)(var0.z - var4.z), 1.0F);
            Vector4f var8 = new Vector4f((float)(var1.x - var4.x), (float)(var1.y - var4.y), (float)(var1.z - var4.z), 1.0F);
            RenderContext.VIEW_PROJECTION_MATRIX.transform(var68);
            RenderContext.VIEW_PROJECTION_MATRIX.transform(var8);
            boolean var69 = var68.w >= 0.05F;
            boolean var10 = var8.w >= 0.05F;
            if (!var69 && !var10) {
               return null;
            }

            if (!var69) {
               float var70 = (0.05F - var68.w) / (var8.w - var68.w);
               var68.set(var68.x + var70 * (var8.x - var68.x), var68.y + var70 * (var8.y - var68.y), var68.z + var70 * (var8.z - var68.z), 0.05F);
            } else if (!var10) {
               float var71 = (0.05F - var68.w) / (var8.w - var68.w);
               var8.set(var68.x + var71 * (var8.x - var68.x), var68.y + var71 * (var8.y - var68.y), var68.z + var71 * (var8.z - var68.z), 0.05F);
            }

            float var72 = (var68.x / var68.w + 1.0F) * 0.5F * var5;
            float var12 = (1.0F - var68.y / var68.w) * 0.5F * var6;
            float var73 = (var8.x / var8.w + 1.0F) * 0.5F * var5;
            float var14 = (1.0F - var8.y / var8.w) * 0.5F * var6;
            return new float[][]{{var72, var12}, {var73, var14}};
         } else {
            double var7 = Math.toRadians(var3.getYaw());
            double var9 = Math.toRadians(var3.getPitch());
            double var11 = Math.cos(var7);
            double var13 = Math.sin(var7);
            double var15 = Math.cos(var9);
            double var17 = Math.sin(var9);
            double var19 = -var13 * var15;
            double var21 = -var17;
            double var23 = var11 * var15;
            double var25 = -var11;
            double var27 = -var13;
            double var29 = -var13 * var17;
            double var31 = var15;
            double var33 = var11 * var17;
            double var35 = var0.x - var4.x;
            double var37 = var0.y - var4.y;
            double var39 = var0.z - var4.z;
            double var41 = var35 * var25 + var39 * var27;
            double var43 = var35 * var29 + var37 * var31 + var39 * var33;
            double var45 = var35 * var19 + var37 * var21 + var39 * var23;
            double var47 = var1.x - var4.x;
            double var49 = var1.y - var4.y;
            double var51 = var1.z - var4.z;
            double var53 = var47 * var25 + var51 * var27;
            double var55 = var47 * var29 + var49 * var31 + var51 * var33;
            double var57 = var47 * var19 + var49 * var21 + var51 * var23;
            boolean var59 = var45 >= 0.05F;
            boolean var60 = var57 >= 0.05F;
            if (!var59 && !var60) {
               return null;
            }

            if (!var59) {
               double var61 = (0.05F - var45) / (var57 - var45);
               var41 += var61 * (var53 - var41);
               var43 += var61 * (var55 - var43);
               var45 = 0.05F;
            } else if (!var60) {
               double var74 = (0.05F - var45) / (var57 - var45);
               var53 = var41 + var74 * (var53 - var41);
               var55 = var43 + var74 * (var55 - var43);
               var57 = 0.05F;
            }

            float var75 = ((IGameRenderer)var2.gameRenderer).invokeGetFov(var3, var2.getRenderTickCounter().getTickProgress(true), true);
            double var62 = var6 / 2.0 / Math.tan(Math.toRadians(var75) / 2.0);
            float var64 = var5 / 2.0F + (float)(var41 / var45 * var62);
            float var65 = var6 / 2.0F - (float)(var43 / var45 * var62);
            float var66 = var5 / 2.0F + (float)(var53 / var57 * var62);
            float var67 = var6 / 2.0F - (float)(var55 / var57 * var62);
            return new float[][]{{var64, var65}, {var66, var67}};
         }
      } else {
         return null;
      }
   }

   public static float[][] a(Vec3d... var0) {
      if (var0 != null && var0.length >= 3) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         if (var1.player != null && var1.gameRenderer != null) {
            Camera var2 = var1.gameRenderer.getCamera();
            Vec3d var3 = var2.getCameraPos();
            float var4 = RenderUtil.a();
            float var5 = RenderUtil.b();
            if (RenderContext.WORLD_READY) {
               ArrayList var51 = new ArrayList(var0.length);

               for (Vec3d var57 : var0) {
                  Vector4f var11 = new Vector4f((float)(var57.x - var3.x), (float)(var57.y - var3.y), (float)(var57.z - var3.z), 1.0F);
                  RenderContext.VIEW_PROJECTION_MATRIX.transform(var11);
                  var51.add(var11);
               }

               ArrayList var52 = new ArrayList(var51.size() + 2);
               int var54 = var51.size();

               for (int var55 = 0; var55 < var54; var55++) {
                  Vector4f var58 = (Vector4f)var51.get(var55);
                  Vector4f var60 = (Vector4f)var51.get((var55 + 1) % var54);
                  boolean var62 = var58.w >= 0.05F;
                  boolean var13 = var60.w >= 0.05F;
                  if (var62 && var13) {
                     var52.add(var60);
                  } else if (var62 && !var13) {
                     float var64 = (0.05F - var58.w) / (var60.w - var58.w);
                     var52.add(
                        new Vector4f(var58.x + var64 * (var60.x - var58.x), var58.y + var64 * (var60.y - var58.y), var58.z + var64 * (var60.z - var58.z), 0.05F)
                     );
                  } else if (!var62 && var13) {
                     float var63 = (0.05F - var58.w) / (var60.w - var58.w);
                     var52.add(
                        new Vector4f(var58.x + var63 * (var60.x - var58.x), var58.y + var63 * (var60.y - var58.y), var58.z + var63 * (var60.z - var58.z), 0.05F)
                     );
                     var52.add(var60);
                  }
               }

               if (var52.size() < 3) {
                  return null;
               }

               float[][] var56 = new float[var52.size()][2];

               for (int var59 = 0; var59 < var52.size(); var59++) {
                  Vector4f var61 = (Vector4f)var52.get(var59);
                  var56[var59][0] = (var61.x / var61.w + 1.0F) * 0.5F * var4;
                  var56[var59][1] = (1.0F - var61.y / var61.w) * 0.5F * var5;
               }

               return var56;
            } else {
               double var6 = Math.toRadians(var2.getYaw());
               double var8 = Math.toRadians(var2.getPitch());
               double var10 = Math.cos(var6);
               double var12 = Math.sin(var6);
               double var14 = Math.cos(var8);
               double var16 = Math.sin(var8);
               double var18 = -var12 * var14;
               double var20 = -var16;
               double var22 = var10 * var14;
               double var24 = -var10;
               double var26 = -var12;
               double var28 = -var12 * var16;
               double var30 = var14;
               double var32 = var10 * var16;
               ArrayList var34 = new ArrayList(var0.length);

               for (Vec3d var38 : var0) {
                  double var39 = var38.x - var3.x;
                  double var41 = var38.y - var3.y;
                  double var43 = var38.z - var3.z;
                  double var45 = var39 * var24 + var43 * var26;
                  double var47 = var39 * var28 + var41 * var30 + var43 * var32;
                  double var49 = var39 * var18 + var41 * var20 + var43 * var22;
                  var34.add(new Vec3d(var45, var47, var49));
               }

               ArrayList var65 = new ArrayList(var34.size() + 2);
               int var66 = var34.size();

               for (int var67 = 0; var67 < var66; var67++) {
                  Vec3d var69 = (Vec3d)var34.get(var67);
                  Vec3d var71 = (Vec3d)var34.get((var67 + 1) % var66);
                  boolean var40 = var69.z >= 0.05F;
                  boolean var73 = var71.z >= 0.05F;
                  if (var40 && var73) {
                     var65.add(var71);
                  } else if (var40 && !var73) {
                     double var75 = (0.05F - var69.z) / (var71.z - var69.z);
                     var65.add(new Vec3d(var69.x + var75 * (var71.x - var69.x), var69.y + var75 * (var71.y - var69.y), 0.05F));
                  } else if (!var40 && var73) {
                     double var42 = (0.05F - var69.z) / (var71.z - var69.z);
                     var65.add(new Vec3d(var69.x + var42 * (var71.x - var69.x), var69.y + var42 * (var71.y - var69.y), 0.05F));
                     var65.add(var71);
                  }
               }

               if (var65.size() < 3) {
                  return null;
               }

               float var68 = ((IGameRenderer)var1.gameRenderer).invokeGetFov(var2, var1.getRenderTickCounter().getTickProgress(true), true);
               double var70 = var5 / 2.0 / Math.tan(Math.toRadians(var68) / 2.0);
               float[][] var72 = new float[var65.size()][2];

               for (int var74 = 0; var74 < var65.size(); var74++) {
                  Vec3d var76 = (Vec3d)var65.get(var74);
                  var72[var74][0] = var4 / 2.0F + (float)(var76.x / var76.z * var70);
                  var72[var74][1] = var5 / 2.0F - (float)(var76.y / var76.z * var70);
               }

               return var72;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean b(Vec3d var0) {
      return a(var0) != null;
   }

   public static float c(Vec3d var0) {
      float[] var1 = a(var0);
      return var1 != null ? MathHelper.clamp(var1[2] * 0.5F, 0.5F, 2.0F) : 1.0F;
   }
}
