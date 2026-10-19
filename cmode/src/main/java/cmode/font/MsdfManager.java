package cmode.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class MsdfManager {
   private static final Map<String, MsdfFont> fonts = new HashMap<>();
   private static MsdfFont defaultFont;
   private static boolean initialized = false;
   private static GpuTexture batchAtlas;
   private static GpuTextureView batchAtlasView;
   public static MsdfFont MONTSERRAT;
   public static MsdfFont ICONS;
   public static MsdfFont BOLD;
   public static MsdfFont ICONS2;
   public static MsdfFont DICONS;

   public static void a() {
      if (!initialized) {
         System.out.println("[MSDF] Initializing MsdfManager...");
         Msdf.MONTSERRAT = MONTSERRAT = a("montserrat", "montserrat.png", "montserrat.json");
         Msdf.BOLD = BOLD = a("bold", "bold.png", "bold.json");
         Msdf.ICONS = ICONS = a("icons", "icons.png", "icons.json");
         Msdf.ICONS2 = ICONS2 = a("icons2", "icons2.png", "icons2.json");
         Msdf.DICONS = DICONS = a("dicons", "dicons.png", "dicons.json");
         createBatchAtlas();
         initialized = true;
      }
   }

   private static void createBatchAtlas() {
      List<MsdfFont> loaded = List.of(MONTSERRAT, BOLD, ICONS, ICONS2, DICONS).stream().filter(MsdfFont::isLoaded).toList();
      if (!loaded.isEmpty()) {
         int columns = (int)Math.ceil(Math.sqrt(loaded.size()));
         int rows = (loaded.size() + columns - 1) / columns;
         int tileWidth = loaded.stream().mapToInt(MsdfFont::getAtlasWidth).max().orElseThrow() + 4;
         int tileHeight = loaded.stream().mapToInt(MsdfFont::getAtlasHeight).max().orElseThrow() + 4;

         try (NativeImage image = new NativeImage(columns * tileWidth, rows * tileHeight, true)) {
            for (int i = 0; i < loaded.size(); i++) {
               loaded.get(i).copyToBatchImage(image, i % columns * tileWidth + 2, i / columns * tileHeight + 2);
            }

            batchAtlas = RenderSystem.getDevice()
               .createTexture(() -> "cmode:msdf_batch_atlas", 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(batchAtlas, image);
            batchAtlasView = RenderSystem.getDevice().createTextureView(batchAtlas);

            for (int i = 0; i < loaded.size(); i++) {
               loaded.get(i).attachBatchAtlas(batchAtlas, batchAtlasView, i % columns * tileWidth + 2, i / columns * tileHeight + 2);
            }
         } catch (IOException failure) {
            throw new IllegalStateException("Could not assemble the MSDF font atlas", failure);
         }
      }
   }

   public static MsdfFont a(String var0, String var1, String var2) {
      MsdfFont var3 = new MsdfFont(var0);
      var3.a(Identifier.of("cmode", var1), Identifier.of("cmode", var2));
      fonts.put(var0, var3);
      if (defaultFont == null) {
         defaultFont = var3;
      }

      return var3;
   }

   public static MsdfFont a(String var0) {
      if (!initialized) {
         a();
      }

      return fonts.get(var0);
   }

   public static MsdfFont getDefaultFont() {
      return defaultFont;
   }

   public static void b(String var0) {
      MsdfFont var1 = fonts.get(var0);
      if (var1 != null) {
         defaultFont = var1;
      }
   }

   public static void c() {
      MsdfPipeline.flush();
      if (batchAtlasView != null) {
         batchAtlasView.close();
         batchAtlasView = null;
      }

      if (batchAtlas != null) {
         batchAtlas.close();
         batchAtlas = null;
      }

      fonts.values().forEach(MsdfFont::i);
      fonts.clear();
      defaultFont = null;
      initialized = false;
   }
}
