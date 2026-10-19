package brokke.cc.rotation;

import brokke.cc.module.combat.AimAssist;
import brokke.cc.module.combat.Aura;
import brokke.cc.module.FunctionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

public class LegitRotation extends RotationsSystem {
   private LivingEntity target;
   private long lastTargetStamp;

   @Override
   public void a(LivingEntity var1) {
      this.target = var1;
      this.lastTargetStamp = System.nanoTime();
   }

   public void a() {
      this.target = null;
   }

   public LivingEntity b() {
      return this.c();
   }

   private LivingEntity c() {
      if (this.target != null && this.target.isAlive() && !this.target.isRemoved()) {
         return System.nanoTime() - this.lastTargetStamp > 400000000L ? null : this.target;
      } else {
         return null;
      }
   }

   public double[] a(ClientPlayerEntity var1, double var2, double var4) {
      Aura var6 = Aura.INSTANCE;
      if (var6 != null && var6.l() && var6.rotationType.isSelected("Legit")) {
         LivingEntity var7 = var6.getTarget();
         if (var7 == null || !var7.isAlive() || var7.isRemoved()) {
            var7 = this.c();
         }

         AimAssist var8 = FunctionManager.a(AimAssist.class);
         if ((var7 == null || !var7.isAlive() || var7.isRemoved()) && var8 != null) {
            var7 = var8.l();
         }

         if (var7 == null || !var7.isAlive() || var7.isRemoved()) {
            return new double[]{var2, var4};
         } else if (var8 != null) {
            float var9 = 0.6F;
            return var8.a(var1, var2, var4, var7, var9);
         } else {
            return new double[]{var2, var4};
         }
      } else {
         return new double[]{var2, var4};
      }
   }

   public double a(ClientPlayerEntity var1, double var2) {
      return this.a(var1, var2, 0.0)[0];
   }
}
