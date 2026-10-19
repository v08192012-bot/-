package brokke.cc.rotation;

import brokke.cc.animation.Easing2;
import brokke.cc.animation.Easings2;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class SlothPatternEasing {
   static final SlothPatternEasing.c[] DEFAULT_PATTERN = a();

   private SlothPatternEasing() {
   }

   public static float a(float var0) {
      float var1 = MathHelper.clamp(var0, 0.0F, 1.0F);
      return (float)Easings2.QUART_OUT.ease(var1);
   }

   private static SlothPatternEasing.c[] a() {
      short var0 = 192;
      SlothPatternEasing.c[] var1 = new SlothPatternEasing.c[var0];
      Random var2 = new Random(12648430L);

      for (int var3 = 0; var3 < var0; var3++) {
         double var4 = (double)var3 / (var0 - 1);
         double var6 = Math.sin(var4 * Math.PI * 2.0) * 0.55 + Math.sin(var4 * Math.PI * 6.0 + 1.2) * 0.15 + Math.sin(var4 * Math.PI * 11.0 - 0.4) * 0.06;
         double var8 = Math.cos(var4 * Math.PI * 2.5) * 0.28 + Math.cos(var4 * Math.PI * 5.0 + 0.8) * 0.09 + Math.cos(var4 * Math.PI * 9.0) * 0.04;
         var6 += var2.nextGaussian() * 0.08;
         var8 += var2.nextGaussian() * 0.05;
         var1[var3] = new SlothPatternEasing.c((float)var6, (float)var8);
      }

      return var1;
   }

   public static float a(PlayerEntity var0, float var1) {
      float var2 = a(var1);
      if (var0 == null) {
         return var2;
      }

      double var3 = var0.getVelocity().horizontalLength();
      float var5 = (float)Math.min(1.5, 1.0 + var3 * 0.5);
      if (!var0.isOnGround()) {
         var5 = (float)(var5 * MathHelper.clamp(1.0 + var0.fallDistance * 0.015F, 1.0, 1.22F));
      }

      float var6 = var0.getHandSwingProgress(1.0F);
      var5 *= 1.0F + var6 * 0.08F;
      return var2 * MathHelper.clamp(var5, 0.55F, 1.6F);
   }

   public static float a(float var0, float var1, float var2) {
      return var0 == 0.0F ? 0.0F : Math.min(Math.abs(var0), var1) * Math.signum(var0) * var2;
   }

   public enum a {
      SINE_IN(Easings2.SINE_IN),
      SINE_OUT(Easings2.SINE_OUT),
      SINE_IN_OUT(Easings2.SINE_IN_OUT),
      QUAD_IN(Easings2.QUAD_IN),
      QUAD_OUT(Easings2.QUAD_OUT),
      QUAD_IN_OUT(Easings2.QUAD_IN_OUT),
      CUBIC_IN(Easings2.CUBIC_IN),
      CUBIC_OUT(Easings2.CUBIC_OUT),
      CUBIC_IN_OUT(Easings2.CUBIC_IN_OUT),
      QUART_IN(Easings2.QUART_IN),
      QUART_OUT(Easings2.QUART_OUT),
      QUART_IN_OUT(Easings2.QUART_IN_OUT),
      QUINT_IN(Easings2.QUINT_IN),
      QUINT_OUT(Easings2.QUINT_OUT),
      QUINT_IN_OUT(Easings2.QUINT_IN_OUT),
      EXPO_IN(Easings2.EXPO_IN),
      EXPO_OUT(Easings2.EXPO_OUT),
      EXPO_IN_OUT(Easings2.EXPO_IN_OUT),
      CIRC_IN(Easings2.CIRC_IN),
      CIRC_OUT(Easings2.CIRC_OUT),
      CIRC_IN_OUT(Easings2.CIRC_IN_OUT),
      BACK_IN(Easings2.BACK_IN),
      BACK_OUT(Easings2.BACK_OUT),
      BACK_IN_OUT(Easings2.BACK_IN_OUT),
      ELASTIC_IN(Easings2.ELASTIC_IN),
      ELASTIC_OUT(Easings2.ELASTIC_OUT),
      ELASTIC_IN_OUT(Easings2.ELASTIC_IN_OUT),
      BOUNCE_IN(Easings2.BOUNCE_IN),
      BOUNCE_OUT(Easings2.BOUNCE_OUT),
      BOUNCE_IN_OUT(Easings2.BOUNCE_IN_OUT);

      private final Easing2 easing;

      a(Easing2 nullxx) {
         this.easing = nullxx;
      }

      public double a(double var1) {
         return this.easing.ease(MathHelper.clamp(var1, 0.0, 1.0));
      }
   }

   public static final class b {
      private static final long RESEED_NANOS = 340000000L;
      private static final double SAMPLES_PER_SECOND = 26.0;
      private final Random random = new Random();
      private SlothPatternEasing.c[] pattern = SlothPatternEasing.DEFAULT_PATTERN;
      private int cursor;
      private double phase;
      private SlothPatternEasing.a activeCurve = SlothPatternEasing.a.QUART_OUT;
      private float amplitudeYaw = 0.55F;
      private float amplitudePitch = 0.28F;
      private long lastReseedNanos;

      public void a() {
         this.pattern = SlothPatternEasing.DEFAULT_PATTERN;
         this.cursor = this.pattern.length > 0 ? this.random.nextInt(this.pattern.length) : 0;
         this.phase = 0.0;
         this.activeCurve = a(this.random);
         this.amplitudeYaw = 0.45F + this.random.nextFloat() * 0.55F;
         this.amplitudePitch = 0.22F + this.random.nextFloat() * 0.28F;
         this.lastReseedNanos = 0L;
      }

      public void a(SlothPatternEasing.c[] var1) {
         if (var1 != null && var1.length >= 2) {
            this.pattern = var1;
            this.cursor = this.cursor % this.pattern.length;
         }
      }

      public SlothPatternEasing.c a(double var1, float var3) {
         long var4 = System.nanoTime();
         if (var4 - this.lastReseedNanos > 340000000L) {
            this.activeCurve = a(this.random);
            this.amplitudeYaw = 0.4F + this.random.nextFloat() * 0.6F;
            this.amplitudePitch = 0.2F + this.random.nextFloat() * 0.3F;
            this.lastReseedNanos = var4;
         }

         for (this.phase += var1 * 26.0; this.phase >= 1.0; this.phase--) {
            this.cursor = (this.cursor + 1) % this.pattern.length;
         }

         SlothPatternEasing.c var6 = this.pattern[this.cursor];
         SlothPatternEasing.c var7 = this.pattern[(this.cursor + 1) % this.pattern.length];
         float var8 = (float)this.phase;
         float var9 = (float)this.activeCurve.a(var8);
         float var10 = MathHelper.lerp(var9, var6.yawOffset, var7.yawOffset);
         float var11 = MathHelper.lerp(var9, var6.pitchOffset, var7.pitchOffset);
         float var12 = MathHelper.clamp(var3, 0.0F, 1.0F);
         float var13 = 4.0F * var12 * (1.0F - var12);
         return new SlothPatternEasing.c(var10 * this.amplitudeYaw * var13, var11 * this.amplitudePitch * var13);
      }

      private static SlothPatternEasing.a a(Random var0) {
         SlothPatternEasing.a[] var1 = new SlothPatternEasing.a[]{
            SlothPatternEasing.a.QUART_OUT,
            SlothPatternEasing.a.QUINT_OUT,
            SlothPatternEasing.a.CUBIC_OUT,
            SlothPatternEasing.a.SINE_IN_OUT,
            SlothPatternEasing.a.EXPO_OUT,
            SlothPatternEasing.a.CIRC_OUT,
            SlothPatternEasing.a.QUAD_IN_OUT
         };
         SlothPatternEasing.a[] var2 = new SlothPatternEasing.a[]{
            SlothPatternEasing.a.BACK_OUT, SlothPatternEasing.a.QUINT_IN_OUT, SlothPatternEasing.a.CUBIC_IN_OUT
         };
         return var0.nextInt(10) < 9 ? var1[var0.nextInt(var1.length)] : var2[var0.nextInt(var2.length)];
      }
   }

   public record c(float yawOffset, float pitchOffset) {
      public float getYawOffset() {
         return this.yawOffset;
      }

      public float getPitchOffset() {
         return this.pitchOffset;
      }
   }
}
