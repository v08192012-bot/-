package cmode.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class MsdfFont {
   private final String name;
   private final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
   private final Map<Integer, Map<String, Float>> widthCacheBySize = Collections.synchronizedMap(new LinkedHashMap<Integer, Map<String, Float>>(8, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Integer, Map<String, Float>> var1) {
         return this.size() > 8;
      }
   });
   private GpuTexture texture;
   private GpuTextureView textureView;
   private Identifier textureIdentifier;
   private GpuTextureView batchTextureView;
   private float batchUOffset;
   private float batchVOffset;
   private float batchUScale = 1.0F;
   private float batchVScale = 1.0F;
   private float lineHeight;
   private float ascender;
   private float descender;
   private float pxRange;
   private int atlasWidth;
   private int atlasHeight;
   private float emSize;
   private boolean loaded = false;

   public MsdfFont(String var1) {
      this.name = var1;
   }

   public void a(Identifier var1, Identifier var2) {
      if (!this.loaded) {
         try {
            this.a(var1);
            this.b(var2);
            this.loaded = true;
            System.out.println("[MSDF] Successfully loaded font: " + this.name);
         } catch (Exception var4) {
            System.err.println("[MSDF] Failed to load font '" + this.name + "':");
            var4.printStackTrace();
         }
      }
   }

   private void a(Identifier var1) throws Exception {
      Identifier var2 = Identifier.of(var1.getNamespace(), "font/" + var1.getPath());
      this.textureIdentifier = var2;

      try (InputStream var3 = MinecraftClient.getInstance().getResourceManager().getResource(var2).get().getInputStream()) {
         NativeImage var4 = NativeImage.read(var3);
         this.atlasWidth = var4.getWidth();
         this.atlasHeight = var4.getHeight();
         this.texture = RenderSystem.getDevice().createTexture(() -> "msdf:" + this.name, 5, TextureFormat.RGBA8, this.atlasWidth, this.atlasHeight, 1, 1);
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, var4);
         this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
         var4.close();
      }
   }

   private void b(Identifier var1) throws Exception {
      Identifier var2 = Identifier.of(var1.getNamespace(), "font/" + var1.getPath());

      try (InputStream var3 = MinecraftClient.getInstance().getResourceManager().getResource(var2).get().getInputStream()) {
         String var4 = new String(var3.readAllBytes());
         JsonObject var5 = JsonParser.parseString(var4).getAsJsonObject();
         JsonObject var6 = var5.getAsJsonObject("atlas");
         this.pxRange = var6.get("distanceRange").getAsFloat();
         boolean var7 = var6.has("yOrigin") && "bottom".equals(var6.get("yOrigin").getAsString());
         JsonObject var8 = var5.getAsJsonObject("metrics");
         this.emSize = var8.get("emSize").getAsFloat();
         this.lineHeight = var8.get("lineHeight").getAsFloat();
         this.ascender = var8.get("ascender").getAsFloat();
         this.descender = var8.get("descender").getAsFloat();

         for (JsonElement var10 : var5.getAsJsonArray("glyphs")) {
            JsonObject var11 = var10.getAsJsonObject();
            int var12 = var11.get("unicode").getAsInt();
            float var13 = var11.has("advance") ? var11.get("advance").getAsFloat() : 0.0F;
            float var14 = 0.0F;
            float var15 = 0.0F;
            float var16 = 0.0F;
            float var17 = 0.0F;
            float var18 = 0.0F;
            float var19 = 0.0F;
            float var20 = 0.0F;
            float var21 = 0.0F;
            if (var11.has("atlasBounds")) {
               JsonObject var22 = var11.getAsJsonObject("atlasBounds");
               float var23 = var22.get("left").getAsFloat();
               float var24 = var22.get("bottom").getAsFloat();
               float var25 = var22.get("right").getAsFloat();
               float var26 = var22.get("top").getAsFloat();
               var16 = var23 / this.atlasWidth;
               var18 = var25 / this.atlasWidth;
               if (var7) {
                  var17 = 1.0F - var26 / this.atlasHeight;
                  var19 = 1.0F - var24 / this.atlasHeight;
               } else {
                  var17 = var26 / this.atlasHeight;
                  var19 = var24 / this.atlasHeight;
               }
            }

            if (var11.has("planeBounds")) {
               JsonObject var29 = var11.getAsJsonObject("planeBounds");
               float var30 = var29.get("left").getAsFloat();
               float var31 = var29.get("bottom").getAsFloat();
               float var32 = var29.get("right").getAsFloat();
               float var33 = var29.get("top").getAsFloat();
               var20 = var30;
               var21 = var33;
               var14 = var32 - var30;
               var15 = var33 - var31;
            }

            this.glyphs.put(var12, new MsdfGlyph(var12, var13, 0.0F, 0.0F, var14, var15, var16, var17, var18, var19, var20, var21));
         }

         System.out.println("[MSDF] Loaded font '" + this.name + "' with " + this.glyphs.size() + " glyphs");
      }
   }

   public MsdfGlyph a(int var1) {
      if (var1 >= 57344 && var1 <= 63743) {
         return null;
      }

      MsdfGlyph var2 = this.glyphs.get(var1);
      if (var2 == null) {
         if (var1 == 171) {
            var2 = this.glyphs.get(60);
         } else if (var1 == 187) {
            var2 = this.glyphs.get(62);
         } else {
            int var3 = b(var1);
            if (var3 != var1) {
               var2 = this.glyphs.get(var3);
            }
         }
      }

      return var2;
   }

   private static int b(int var0) {
      if (var0 >= 119808 && var0 <= 120831) {
         if (var0 >= 119808 && var0 <= 119833) {
            return 65 + (var0 - 119808);
         }

         if (var0 >= 119834 && var0 <= 119859) {
            return 97 + (var0 - 119834);
         }

         if (var0 >= 119860 && var0 <= 119885) {
            return 65 + (var0 - 119860);
         }

         if (var0 >= 119886 && var0 <= 119911) {
            return 97 + (var0 - 119886);
         }

         if (var0 >= 119912 && var0 <= 119937) {
            return 65 + (var0 - 119912);
         }

         if (var0 >= 119938 && var0 <= 119963) {
            return 97 + (var0 - 119938);
         }

         if (var0 >= 119964 && var0 <= 119989) {
            return 65 + (var0 - 119964);
         }

         if (var0 >= 119990 && var0 <= 120015) {
            return 97 + (var0 - 119990);
         }

         if (var0 >= 120016 && var0 <= 120041) {
            return 65 + (var0 - 120016);
         }

         if (var0 >= 120042 && var0 <= 120067) {
            return 97 + (var0 - 120042);
         }

         if (var0 >= 120068 && var0 <= 120093) {
            return 65 + (var0 - 120068);
         }

         if (var0 >= 120094 && var0 <= 120119) {
            return 97 + (var0 - 120094);
         }

         if (var0 >= 120120 && var0 <= 120145) {
            return 65 + (var0 - 120120);
         }

         if (var0 >= 120146 && var0 <= 120171) {
            return 97 + (var0 - 120146);
         }

         if (var0 >= 120172 && var0 <= 120197) {
            return 65 + (var0 - 120172);
         }

         if (var0 >= 120198 && var0 <= 120223) {
            return 97 + (var0 - 120198);
         }

         if (var0 >= 120224 && var0 <= 120249) {
            return 65 + (var0 - 120224);
         }

         if (var0 >= 120250 && var0 <= 120275) {
            return 97 + (var0 - 120250);
         }

         if (var0 >= 120276 && var0 <= 120301) {
            return 65 + (var0 - 120276);
         }

         if (var0 >= 120302 && var0 <= 120327) {
            return 97 + (var0 - 120302);
         }

         if (var0 >= 120328 && var0 <= 120353) {
            return 65 + (var0 - 120328);
         }

         if (var0 >= 120354 && var0 <= 120379) {
            return 97 + (var0 - 120354);
         }

         if (var0 >= 120380 && var0 <= 120405) {
            return 65 + (var0 - 120380);
         }

         if (var0 >= 120406 && var0 <= 120431) {
            return 97 + (var0 - 120406);
         }

         if (var0 >= 120432 && var0 <= 120457) {
            return 65 + (var0 - 120432);
         }

         if (var0 >= 120458 && var0 <= 120483) {
            return 97 + (var0 - 120458);
         }

         if (var0 >= 120782 && var0 <= 120831) {
            return 48 + (var0 - 120782) % 10;
         }
      }
      return switch (var0) {
         case 166, 305, 448, 618, 1030, 1031, 1110, 1111, 7590, 8464, 8465, 8466 -> 73;
         case 216, 248, 7439, 7506 -> 79;
         case 319, 320, 321, 322, 671, 737 -> 76;
         case 350, 351, 738, 1029, 1109, 42801 -> 83;
         case 385, 386, 665, 7495 -> 66;
         case 393, 394, 1281, 7429, 7496 -> 68;
         case 398, 477, 7431, 7497, 8455, 8496 -> 69;
         case 401, 402, 988, 989, 1170, 1171, 7584, 8497, 8498, 42800, 42904, 42905, 43003 -> 70;
         case 403, 610, 1292, 1293, 7501 -> 71;
         case 491, 985, 8474 -> 81;
         case 592, 7424, 7491, 11375 -> 65;
         case 628, 7505, 8469 -> 78;
         case 640, 641, 691, 8475, 8476, 8477 -> 82;
         case 655, 696 -> 89;
         case 668, 1210, 1211, 8459, 8460, 8461 -> 72;
         case 690, 1032, 1112, 7434 -> 74;
         case 695, 7457 -> 87;
         case 739 -> 88;
         case 1178, 1179, 7435, 7503 -> 75;
         case 7428, 7580, 8450, 8493 -> 67;
         case 7437, 7504, 8499 -> 77;
         case 7448, 7510, 8473 -> 80;
         case 7449 -> 1071;
         case 7451, 7511 -> 84;
         case 7452, 7512 -> 85;
         case 7456, 7515 -> 86;
         case 7458, 8484, 8488 -> 90;
         case 7462 -> 1043;
         case 7463, 7467 -> 1051;
         case 7464 -> 1055;
         case 7465 -> 1056;
         default -> var0 >= 65313 && var0 <= 65338
            ? 65 + (var0 - 65313)
            : (var0 >= 65345 && var0 <= 65370 ? 97 + (var0 - 65345) : (var0 >= 65296 && var0 <= 65305 ? 48 + (var0 - 65296) : var0));
      };
   }

   public GpuTextureView getTextureView() {
      return this.textureView;
   }

   int getAtlasWidth() {
      return this.atlasWidth;
   }

   int getAtlasHeight() {
      return this.atlasHeight;
   }

   void copyToBatchImage(NativeImage atlas, int x, int y) throws IOException {
      try (
         InputStream input = MinecraftClient.getInstance().getResourceManager().getResource(this.textureIdentifier).orElseThrow().getInputStream();
         NativeImage image = NativeImage.read(input);
      ) {
         image.copyRect(atlas, 0, 0, x, y, this.atlasWidth, this.atlasHeight, false, false);
      }
   }

   void attachBatchAtlas(GpuTexture atlas, GpuTextureView view, int x, int y) {
      this.batchTextureView = view;
      this.batchUOffset = (float)x / atlas.getWidth(0);
      this.batchVOffset = (float)y / atlas.getHeight(0);
      this.batchUScale = (float)this.atlasWidth / atlas.getWidth(0);
      this.batchVScale = (float)this.atlasHeight / atlas.getHeight(0);
   }

   GpuTextureView getBatchTextureView() {
      return this.batchTextureView == null ? this.textureView : this.batchTextureView;
   }

   float batchU(float u) {
      return this.batchUOffset + u * this.batchUScale;
   }

   float batchV(float v) {
      return this.batchVOffset + v * this.batchVScale;
   }

   public float getLineHeight() {
      return this.lineHeight;
   }

   public float getAscender() {
      return this.ascender;
   }

   public float getDescender() {
      return this.descender;
   }

   public float getPxRange() {
      return this.pxRange;
   }

   public float getEmSize() {
      return this.emSize;
   }

   public String getName() {
      return this.name;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public float a(String var1, float var2) {
      if (var1 != null && !var1.isEmpty()) {
         Integer var3 = Float.floatToIntBits(var2);
         Map<String, Float> var4 = this.widthCacheBySize.get(var3);
         if (var4 == null) {
            var4 = Collections.synchronizedMap(new LinkedHashMap<String, Float>(1024, 0.75F, true) {
               @Override
               protected boolean removeEldestEntry(Map.Entry<String, Float> var1x) {
                  return this.size() > 1024;
               }
            });
            this.widthCacheBySize.put(var3, var4);
         }

         Float var5 = var4.get(var1);
         if (var5 != null) {
            return var5;
         }

         float var6 = var2 / this.emSize;
         float var7 = 0.0F;

         for (int var8 = 0; var8 < var1.length(); var8++) {
            char var9 = var1.charAt(var8);
            MsdfGlyph var10 = this.a(var9);
            if (var10 != null) {
               var7 += var10.advance * var6;
            }
         }

         var4.put(var1, var7);
         return var7;
      } else {
         return 0.0F;
      }
   }

   public void i() {
      this.batchTextureView = null;
      this.batchUOffset = 0.0F;
      this.batchVOffset = 0.0F;
      this.batchUScale = 1.0F;
      this.batchVScale = 1.0F;
      if (this.textureView != null) {
         this.textureView.close();
         this.textureView = null;
      }

      if (this.texture != null) {
         this.texture.close();
         this.texture = null;
      }

      this.glyphs.clear();
      this.widthCacheBySize.clear();
      this.loaded = false;
   }
}
