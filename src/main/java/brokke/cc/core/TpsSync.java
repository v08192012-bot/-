package brokke.cc.core;

import brokke.cc.manager.ServerTPSManager;
import net.minecraft.util.math.MathHelper;

public class TpsSync {
   public static final TpsSync INSTANCE = new TpsSync();

   public boolean a() {
      return true;
   }

   public float b() {
      return MathHelper.clamp(ServerTPSManager.getInstance().e(), 0.1F, 20.0F);
   }

   public long a(long var1) {
      float var3 = this.b();
      if (var3 >= 20.0F) {
         return var1;
      }

      float var4 = 20.0F / var3;
      float var5 = 1.0F + (20.0F - var3) * 0.05F;
      long var6 = (long)((float)var1 * var4 * var5);
      return Math.min(var6, 3000L);
   }
}
