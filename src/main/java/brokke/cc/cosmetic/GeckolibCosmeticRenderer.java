package brokke.cc.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GeckolibCosmeticRenderer {
   private static GeckolibCosmeticRenderer instance;
   private final Map<Integer, GeoModel> modelCache = new ConcurrentHashMap<>();
   private final Map<Integer, GeckolibCosmeticRenderer.b> animationCache = new ConcurrentHashMap<>();
   private final Set<Integer> noAnimationSet = ConcurrentHashMap.newKeySet();
   private final Map<Integer, Long> animationStartTime = new ConcurrentHashMap<>();
   private final Map<Integer, Map<String, float[]>> initialBoneTransforms = new ConcurrentHashMap<>();
   private final GeckolibModelParser modelParser = new GeckolibModelParser();

   public static GeckolibCosmeticRenderer a() {
      if (instance == null) {
         instance = new GeckolibCosmeticRenderer();
      }

      return instance;
   }

   public void a(CosmeticModel var1, MatrixStack var2, VertexConsumerProvider var3, int var4) {
      if (var1 != null && var1.getTextureId() != null) {
         GeoModel var5 = this.a(var1);
         if (var5 != null) {
            GeckolibCosmeticRenderer.b var6 = this.b(var1);
            if (var6 != null) {
               this.a(var5, var6, var1.getId());
            }

            Identifier var7 = var1.getTextureId();
            RenderLayer var8 = RenderLayers.entityCutoutNoCull(var7);
            VertexConsumer var9 = var3.getBuffer(var8);

            for (GeoBone var11 : var5.topLevelBones) {
               this.a(var11, var2, var9, var4, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   public void a(CosmeticModel var1, MatrixStack var2, VertexConsumer var3, int var4) {
      if (var1 != null && var1.getTextureId() != null && var3 != null) {
         GeoModel var5 = this.a(var1);
         if (var5 != null) {
            GeckolibCosmeticRenderer.b var6 = this.b(var1);
            if (var6 != null) {
               this.a(var5, var6, var1.getId());
            }

            for (GeoBone var8 : var5.topLevelBones) {
               this.a(var8, var2, var3, var4, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   public GeoModel a(CosmeticModel var1) {
      int var2 = var1.getId();
      if (this.modelCache.containsKey(var2)) {
         return this.modelCache.get(var2);
      }

      GeoModel var3 = this.modelParser.a(var1);
      if (var3 != null) {
         this.modelCache.put(var2, var3);
         this.a(var2, var3);
      }

      return var3;
   }

   private void a(int var1, GeoModel var2) {
      HashMap var3 = new HashMap();

      for (GeoBone var5 : var2.topLevelBones) {
         this.a(var5, var3);
      }

      this.initialBoneTransforms.put(var1, var3);
   }

   private void a(GeoBone var1, Map<String, float[]> var2) {
      var2.put(
         var1.name,
         new float[]{
            var1.getRotateX(),
            var1.getRotateY(),
            var1.getRotateZ(),
            var1.getPositionX(),
            var1.getPositionY(),
            var1.getPositionZ(),
            var1.getScaleX(),
            var1.getScaleY(),
            var1.getScaleZ()
         }
      );

      for (GeoBone var4 : var1.childBones) {
         this.a(var4, var2);
      }
   }

   private void a(GeoBone var1, MatrixStack var2, VertexConsumer var3, int var4, int var5, float var6, float var7, float var8, float var9) {
      if (!var1.isHidden) {
         var2.push();
         GeckoRenderHelper.a(var1, var2);
         GeckoRenderHelper.b(var1, var2);
         GeckoRenderHelper.d(var1, var2);
         GeckoRenderHelper.e(var1, var2);
         GeckoRenderHelper.c(var1, var2);

         for (GeoCube var11 : var1.childCubes) {
            this.a(var11, var2, var3, var4, var5, var6, var7, var8, var9);
         }

         for (GeoBone var13 : var1.childBones) {
            this.a(var13, var2, var3, var4, var5, var6, var7, var8, var9);
         }

         var2.pop();
      }
   }

   private void a(GeoCube var1, MatrixStack var2, VertexConsumer var3, int var4, int var5, float var6, float var7, float var8, float var9) {
      var2.push();
      GeckoRenderHelper.a(var1, var2);
      GeckoRenderHelper.c(var1, var2);
      GeckoRenderHelper.b(var1, var2);
      Matrix4f var10 = var2.peek().getPositionMatrix();
      Matrix3f var11 = var2.peek().getNormalMatrix();

      for (GeoQuad var15 : var1.quads) {
         if (var15 != null) {
            Vector3f var16 = new Vector3f(var15.normal.getX(), var15.normal.getY(), var15.normal.getZ());
            var11.transform(var16);
            float var17 = var16.x();
            float var18 = var16.y();
            float var19 = var16.z();
            if ((var1.size.getY() == 0.0F || var1.size.getZ() == 0.0F) && var17 < 0.0F) {
               var17 = -var17;
            }

            if ((var1.size.getX() == 0.0F || var1.size.getZ() == 0.0F) && var18 < 0.0F) {
               var18 = -var18;
            }

            if ((var1.size.getX() == 0.0F || var1.size.getY() == 0.0F) && var19 < 0.0F) {
               var19 = -var19;
            }

            for (GeoVertex var23 : var15.vertices) {
               var3.vertex(var10, var23.position.getX(), var23.position.getY(), var23.position.getZ())
                  .color(var6, var7, var8, var9)
                  .texture(var23.textureU, var23.textureV)
                  .overlay(var5)
                  .light(var4)
                  .normal(var17, var18, var19);
            }
         }
      }

      var2.pop();
   }

   private GeckolibCosmeticRenderer.b b(CosmeticModel var1) {
      int var2 = var1.getId();
      if (this.animationCache.containsKey(var2)) {
         return this.animationCache.get(var2);
      }

      if (this.noAnimationSet.contains(var2)) {
         return null;
      }

      JsonObject var3 = var1.getAnimationJson();
      if (var3 == null) {
         this.noAnimationSet.add(var2);
         return null;
      }

      try {
         GeckolibCosmeticRenderer.b var4 = this.a(var3);
         if (var4 != null) {
            this.animationCache.put(var2, var4);
         } else {
            this.noAnimationSet.add(var2);
         }

         return var4;
      } catch (Exception var5) {
         this.noAnimationSet.add(var2);
         return null;
      }
   }

   private GeckolibCosmeticRenderer.b a(JsonObject var1) {
      GeckolibCosmeticRenderer.b var2 = new GeckolibCosmeticRenderer.b();
      if (!var1.has("animations")) {
         return null;
      }

      JsonObject var3 = var1.getAsJsonObject("animations");
      Iterator var4 = var3.entrySet().iterator();
      if (var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         String var6 = (String)var5.getKey();
         JsonObject var7 = ((JsonElement)var5.getValue()).getAsJsonObject();
         var2.animationName = var6;
         var2.loop = var7.has("loop") && var7.get("loop").getAsBoolean();
         var2.length = var7.has("animation_length") ? var7.get("animation_length").getAsFloat() : 1.0F;
         if (var7.has("bones")) {
            JsonObject var8 = var7.getAsJsonObject("bones");

            for (Entry var10 : var8.entrySet()) {
               String var11 = (String)var10.getKey();
               JsonObject var12 = ((JsonElement)var10.getValue()).getAsJsonObject();
               GeckolibCosmeticRenderer.a var13 = new GeckolibCosmeticRenderer.a();
               if (var12.has("rotation")) {
                  var13.rotationKeyframes = this.a(var12.get("rotation"));
               }

               if (var12.has("position")) {
                  var13.positionKeyframes = this.a(var12.get("position"));
               }

               if (var12.has("scale")) {
                  var13.scaleKeyframes = this.a(var12.get("scale"));
               }

               var2.boneAnimations.put(var11, var13);
            }
         }
      }

      return var2;
   }

   private Map<Float, float[]> a(JsonElement var1) {
      HashMap var2 = new HashMap();
      if (var1.isJsonObject()) {
         JsonObject var3 = var1.getAsJsonObject();

         for (Entry var5 : var3.entrySet()) {
            try {
               float var6 = Float.parseFloat((String)var5.getKey());
               JsonElement var7 = (JsonElement)var5.getValue();
               float[] var8 = new float[3];
               if (var7.isJsonObject()) {
                  JsonObject var9 = var7.getAsJsonObject();
                  if (var9.has("vector")) {
                     JsonArray var10 = var9.getAsJsonArray("vector");
                     var8[0] = var10.get(0).getAsFloat();
                     var8[1] = var10.get(1).getAsFloat();
                     var8[2] = var10.get(2).getAsFloat();
                  }
               } else if (var7.isJsonArray()) {
                  JsonArray var12 = var7.getAsJsonArray();
                  var8[0] = var12.get(0).getAsFloat();
                  var8[1] = var12.get(1).getAsFloat();
                  var8[2] = var12.get(2).getAsFloat();
               }

               var2.put(var6, var8);
            } catch (NumberFormatException var11) {
            }
         }
      }

      return var2;
   }

   private void a(GeoModel var1, GeckolibCosmeticRenderer.b var2, int var3) {
      Map<String, float[]> var4 = this.initialBoneTransforms.get(var3);
      if (var4 != null) {
         long var5 = this.animationStartTime.computeIfAbsent(var3, var0 -> System.currentTimeMillis());
         float var7 = (float)(System.currentTimeMillis() - var5) / 1000.0F;
         float var8 = var2.loop && var2.length > 0.0F ? var7 % var2.length : Math.min(var7, var2.length);

         for (GeoBone var10 : var1.topLevelBones) {
            this.b(var10, var4);
         }

         for (Entry var17 : var2.boneAnimations.entrySet()) {
            String var11 = (String)var17.getKey();
            GeckolibCosmeticRenderer.a var12 = (GeckolibCosmeticRenderer.a)var17.getValue();
            GeoBone var13 = this.a(var1, var11);
            if (var13 != null) {
               float[] var14 = var4.getOrDefault(var11, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F});
               if (!var12.rotationKeyframes.isEmpty()) {
                  float[] var15 = this.a(var12.rotationKeyframes, var8);
                  var13.setRotateX(var14[0] + (float)Math.toRadians(-var15[0]));
                  var13.setRotateY(var14[1] + (float)Math.toRadians(-var15[1]));
                  var13.setRotateZ(var14[2] + (float)Math.toRadians(var15[2]));
               }

               if (!var12.positionKeyframes.isEmpty()) {
                  float[] var18 = this.a(var12.positionKeyframes, var8);
                  var13.setPositionX(var14[3] + var18[0]);
                  var13.setPositionY(var14[4] + var18[1]);
                  var13.setPositionZ(var14[5] + var18[2]);
               }

               if (!var12.scaleKeyframes.isEmpty()) {
                  float[] var19 = this.a(var12.scaleKeyframes, var8);
                  var13.setScaleX(var14[6] * var19[0]);
                  var13.setScaleY(var14[7] * var19[1]);
                  var13.setScaleZ(var14[8] * var19[2]);
               }
            }
         }
      }
   }

   private void b(GeoBone var1, Map<String, float[]> var2) {
      float[] var3 = var2.get(var1.name);
      if (var3 != null) {
         var1.setRotateX(var3[0]);
         var1.setRotateY(var3[1]);
         var1.setRotateZ(var3[2]);
         var1.setPositionX(var3[3]);
         var1.setPositionY(var3[4]);
         var1.setPositionZ(var3[5]);
         var1.setScaleX(var3[6]);
         var1.setScaleY(var3[7]);
         var1.setScaleZ(var3[8]);
      }

      for (GeoBone var5 : var1.childBones) {
         this.b(var5, var2);
      }
   }

   private GeoBone a(GeoModel var1, String var2) {
      for (GeoBone var4 : var1.topLevelBones) {
         GeoBone var5 = this.a(var4, var2);
         if (var5 != null) {
            return var5;
         }
      }

      return null;
   }

   private GeoBone a(GeoBone var1, String var2) {
      if (var1.name.equals(var2)) {
         return var1;
      }

      for (GeoBone var4 : var1.childBones) {
         GeoBone var5 = this.a(var4, var2);
         if (var5 != null) {
            return var5;
         }
      }

      return null;
   }

   private float[] a(Map<Float, float[]> var1, float var2) {
      if (var1.isEmpty()) {
         return new float[]{0.0F, 0.0F, 0.0F};
      }

      Float var3 = null;
      Float var4 = null;
      float[] var5 = null;
      float[] var6 = null;

      for (Entry var8 : var1.entrySet()) {
         float var9 = (Float)var8.getKey();
         if (var9 <= var2 && (var3 == null || var9 > var3)) {
            var3 = var9;
            var5 = (float[])var8.getValue();
         }

         if (var9 >= var2 && (var4 == null || var9 < var4)) {
            var4 = var9;
            var6 = (float[])var8.getValue();
         }
      }

      if (var5 == null && var6 == null) {
         return new float[]{0.0F, 0.0F, 0.0F};
      }

      if (var5 == null) {
         return var6;
      }

      if (var6 == null) {
         return var5;
      }

      if (var3.equals(var4)) {
         return var5;
      }

      float var10 = (var2 - var3) / (var4 - var3);
      return new float[]{var5[0] + var10 * (var6[0] - var5[0]), var5[1] + var10 * (var6[1] - var5[1]), var5[2] + var10 * (var6[2] - var5[2])};
   }

   static class a {
      Map<Float, float[]> rotationKeyframes = new HashMap<>();
      Map<Float, float[]> positionKeyframes = new HashMap<>();
      Map<Float, float[]> scaleKeyframes = new HashMap<>();
   }

   static class b {
      String animationName;
      boolean loop;
      float length;
      Map<String, GeckolibCosmeticRenderer.a> boneAnimations = new HashMap<>();
   }
}
