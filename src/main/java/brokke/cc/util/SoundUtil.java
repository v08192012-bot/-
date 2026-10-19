package brokke.cc.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;

public class SoundUtil {
   private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(var0 -> {
      Thread var1 = new Thread(var0, "Brokke-Sound-Thread");
      var1.setDaemon(true);
      return var1;
   });
   private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();

   public static byte[] a(String var0) {
      return CACHE.computeIfAbsent(var0, var0x -> {
         try (InputStream var1 = SoundUtil.class.getResourceAsStream(var0x)) {
            return var1 == null ? null : var1.readAllBytes();
         } catch (Exception var6) {
            return null;
         }
      });
   }

   public static void a(String var0, float var1, float var2) {
      EXECUTOR.execute(() -> {
         try {
            byte[] var3 = a(var0);
            if (var3 == null || var3.length == 0) {
               return;
            }

            AudioInputStream var4 = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(var3)));
            AudioFormat var5 = var4.getFormat();
            byte[] var6 = var4.readAllBytes();
            if (Math.abs(var2 - 1.0F) > 0.01F && var5.getSampleSizeInBits() == 16) {
               var6 = a(var6, var5.getChannels(), var2);
            }

            AudioInputStream var7 = new AudioInputStream(new ByteArrayInputStream(var6), var5, var6.length / var5.getFrameSize());
            Clip var8 = AudioSystem.getClip();
            var8.open(var7);
            if (var8.isControlSupported(Type.MASTER_GAIN)) {
               FloatControl var9 = (FloatControl)var8.getControl(Type.MASTER_GAIN);
               float var10 = Math.clamp(var1, 0.0F, 1.0F);
               float var11 = var9.getMinimum();
               float var12 = var9.getMaximum();
               float var13 = var10 <= 1.0E-4F ? var11 : (float)(Math.log10(var10) * 20.0);
               var9.setValue(Math.clamp(var13, var11, var12));
            }

            var8.addLineListener(var1xx -> {
               if (var1xx.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                  var8.close();
               }
            });
            var8.start();
         } catch (Exception var14) {
         }
      });
   }

   private static byte[] a(byte[] var0, int var1, float var2) {
      byte var3 = 2;
      int var4 = var1 * var3;
      int var5 = var0.length / var4;
      int var6 = (int)(var5 / var2);
      if (var6 <= 0) {
         return var0;
      }

      byte[] var7 = new byte[var6 * var4];
      ByteBuffer var8 = ByteBuffer.wrap(var0).order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer var9 = ByteBuffer.wrap(var7).order(ByteOrder.LITTLE_ENDIAN);
      short[] var10 = new short[var1];
      short[] var11 = new short[var1];

      for (int var12 = 0; var12 < var6; var12++) {
         float var13 = var12 * var2;
         int var14 = (int)var13;
         int var15 = Math.min(var14 + 1, var5 - 1);
         float var16 = var13 - var14;

         for (int var17 = 0; var17 < var1; var17++) {
            var10[var17] = var8.getShort((var14 * var1 + var17) * 2);
            var11[var17] = var8.getShort((var15 * var1 + var17) * 2);
            short var18 = (short)Math.round((1.0F - var16) * var10[var17] + var16 * var11[var17]);
            var9.putShort((var12 * var1 + var17) * 2, var18);
         }
      }

      return var7;
   }
}
