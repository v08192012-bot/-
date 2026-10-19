package brokke.cc.manager;

import brokke.cc.core.IMinecraft;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.util.MathUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Arrays;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public final class ServerTPSManager implements IMinecraft {
   private static final ServerTPSManager INSTANCE = new ServerTPSManager();
   private static final int MIN_SAMPLES = 8;
   private static final float MIN_TPS = 10.0F;
   private static final float MAX_TPS = 20.0F;
   private final ArrayDeque<Float> samples = new ArrayDeque<>(20);
   private long lastTimeMs;
   private long tickTimeMs;
   private float smoothedTickTimeMs = 50.0F;
   private float tps = 20.0F;

   private ServerTPSManager() {
      EventBus.register(this);
   }

   public static ServerTPSManager getInstance() {
      return INSTANCE;
   }

   public float b() {
      return a(this.tps);
   }

   public float c() {
      if (this.samples.size() < 8) {
         return 1.0F;
      }

      float var1 = MathUtil.clamp(this.tps, 10.0F, 20.0F);
      return 20.0F / var1;
   }

   public float d() {
      if (this.samples.isEmpty()) {
         return 1.0F;
      }

      float var1 = this.e();
      return var1 >= 19.8F ? 1.0F : 20.0F / MathUtil.clamp(var1, 10.0F, 20.0F);
   }

   public float e() {
      if (this.samples.isEmpty()) {
         return 20.0F;
      }

      Float[] var1 = this.samples.toArray(new Float[0]);
      Arrays.sort(var1);
      int var2 = var1.length / 2;
      float var3 = var1.length % 2 == 0 ? (var1[var2 - 1] + var1[var2]) * 0.5F : var1[var2];
      return MathUtil.clamp(var3, 10.0F, 20.0F);
   }

   public long getTickTimeMs() {
      return this.tickTimeMs;
   }

   @EventHandler
   public void onPacketReceive(EventPacket var1) {
      if (var1.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         long var2 = System.currentTimeMillis();
         if (this.lastTimeMs != 0L) {
            this.tickTimeMs = var2 - this.lastTimeMs;
            if (this.tickTimeMs <= 0L) {
               this.lastTimeMs = var2;
               return;
            }

            this.smoothedTickTimeMs = (float)(this.smoothedTickTimeMs * 0.9 + this.tickTimeMs * 0.1);
            if (this.samples.size() > 20) {
               this.samples.poll();
            }

            float var4 = 20.0F * (1000.0F / (float)this.tickTimeMs);
            this.samples.add(var4);
            float var5 = 0.0F;

            for (Float var7 : this.samples) {
               var5 += MathUtil.clamp(var7, 10.0F, 20.0F);
            }

            if (!this.samples.isEmpty()) {
               this.tps = var5 / this.samples.size();
            }
         }

         this.lastTimeMs = var2;
      }
   }

   private static float a(double var0) {
      BigDecimal var2 = new BigDecimal(var0);
      var2 = var2.setScale(2, RoundingMode.HALF_UP);
      return var2.floatValue();
   }
}
