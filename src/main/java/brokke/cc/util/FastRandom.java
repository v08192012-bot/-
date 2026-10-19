package brokke.cc.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FastRandom extends Random {
   private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
   private Random random = null;
   private volatile boolean seedSet;
   private volatile boolean seedUpdated;
   private volatile long seed;

   private void a() {
      if (this.random == null) {
         this.random = new Random(this.seed);
         this.seedUpdated = false;
      } else if (this.seedUpdated) {
         this.random.setSeed(this.seed);
         this.seedUpdated = false;
      }
   }

   public static long a(long var0, long var2) {
      var0 *= var0 * 6364136223846793005L + 1442695040888963407L;
      return var0 + var2;
   }

   @Override
   public void setSeed(long var1) {
      this.seed = var1;
      this.seedSet = true;
      this.seedUpdated = true;
   }

   @Override
   public void nextBytes(byte[] var1) {
      if (this.seedSet) {
         this.a();
         this.random.nextBytes(var1);
      } else {
         this.threadLocalRandom.nextBytes(var1);
      }
   }

   @Override
   public int nextInt() {
      if (this.seedSet) {
         this.a();
         return this.random.nextInt();
      } else {
         return this.threadLocalRandom.nextInt();
      }
   }

   @Override
   public int nextInt(int var1) {
      if (this.seedSet) {
         this.a();
         return this.random.nextInt(var1);
      } else {
         return this.threadLocalRandom.nextInt(var1);
      }
   }

   @Override
   public long nextLong() {
      if (this.seedSet) {
         this.a();
         return this.random.nextLong();
      } else {
         return this.threadLocalRandom.nextLong();
      }
   }

   @Override
   public boolean nextBoolean() {
      if (this.seedSet) {
         this.a();
         return this.random.nextBoolean();
      } else {
         return this.threadLocalRandom.nextBoolean();
      }
   }

   @Override
   public float nextFloat() {
      if (this.seedSet) {
         this.a();
         return this.random.nextFloat();
      } else {
         return this.threadLocalRandom.nextFloat();
      }
   }

   @Override
   public double nextDouble() {
      if (this.seedSet) {
         this.a();
         return this.random.nextDouble();
      } else {
         return this.threadLocalRandom.nextDouble();
      }
   }

   @Override
   public double nextGaussian() {
      if (this.seedSet) {
         this.a();
         return this.random.nextGaussian();
      } else {
         return this.threadLocalRandom.nextGaussian();
      }
   }
}
