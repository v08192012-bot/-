package brokke.cc.core;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity.RemovalReason;

public class FakePlayerEntity2 extends OtherClientPlayerEntity {
   public FakePlayerEntity2(ClientWorld var1, GameProfile var2) {
      super(var1, var2);
   }

   public void a() {
      this.unsetRemoved();
      ((ClientWorld)this.getEntityWorld()).addEntity(this);
   }

   public void b() {
      ((ClientWorld)this.getEntityWorld()).removeEntity(this.getId(), RemovalReason.DISCARDED);
      this.onRemoved();
   }

   @Override
   public void takeKnockback(double strength, double x, double z) {
   }
}
