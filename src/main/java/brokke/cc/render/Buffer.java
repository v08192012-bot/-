package brokke.cc.render;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Buffer {
   private static final Map<String, Integer> dynamicIdCounters = Maps.newHashMap();

   public static NativeImageBackedTexture a(String var0) throws IOException {
      throw new IOException("Network image loading removed");
   }

   public static NativeImage a(NativeImage var0) {
      if (var0 == null) {
         return null;
      }

      byte var1 = 22;
      byte var2 = 22;
      int var3 = var0.getWidth();
      int var4 = var0.getHeight();

      for (int var5 = var0.getHeight(); var1 < var3 || var2 < var5; var2 = (byte)(var2 * 2)) {
         var1 = (byte)(var1 * 2);
      }

      NativeImage var8 = new NativeImage(var1, var2, true);

      for (int var6 = 0; var6 < var3; var6++) {
         for (int var7 = 0; var7 < var4; var7++) {
            var8.setColorArgb(var6, var7, var0.getColorArgb(var6, var7));
         }
      }

      var0.close();
      return var8;
   }

   public void a(InputStream var1, InputStream var2) {
      try {
         MemoryStack var3 = MemoryStack.stackPush();

         try {
            org.lwjgl.glfw.GLFWImage.Buffer var4 = GLFWImage.malloc(2, var3);
            List var5 = List.of(var1, var2);
            ArrayList<ByteBuffer> var6 = new ArrayList<>();

            for (int var7 = 0; var7 < var5.size(); var7++) {
               NativeImage var8 = NativeImage.read((InputStream)var5.get(var7));
               ByteBuffer var9 = MemoryUtil.memAlloc(var8.getWidth() * var8.getHeight() * 4);
               var9.asIntBuffer().put(var8.copyPixelsAbgr());
               var4.position(var7);
               var4.width(var8.getWidth());
               var4.height(var8.getHeight());
               var4.pixels(var9);
               var6.add(var9);
            }

            if (GLFW.glfwGetPlatform() != 393219) {
               GLFW.glfwSetWindowIcon(MinecraftClient.getInstance().getWindow().getHandle(), var4);
            }

            var6.forEach(MemoryUtil::memFree);
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Exception var12) {
      }
   }

   public static Identifier a(String var0, NativeImageBackedTexture var1) {
      Integer var2 = dynamicIdCounters.get(var0);
      if (var2 == null) {
         var2 = 1;
      } else {
         var2 = var2 + 1;
      }

      dynamicIdCounters.put(var0, var2);
      Identifier var3 = Identifier.of("brokke", String.format(Locale.ROOT, "dynamic/%s_%d", var0, var2));
      MinecraftClient.getInstance().getTextureManager().registerTexture(var3, var1);
      return var3;
   }
}
