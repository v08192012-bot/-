package brokke.cc.nav;

import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PartyMarker {
   private final String id;
   private final String senderName;
   private final boolean isPlayerTarget;
   private final UUID targetPlayerUuid;
   private final String label;
   private final Vec3d staticPos;
   private final long createdAt;

   public PartyMarker(String var1, String var2, boolean var3, UUID var4, String var5, Vec3d var6) {
      this(var1, var2, var3, var4, var5, var6, System.currentTimeMillis());
   }

   public PartyMarker(String var1, String var2, boolean var3, UUID var4, String var5, Vec3d var6, long var7) {
      this.id = var1;
      this.senderName = var2;
      this.isPlayerTarget = var3;
      this.targetPlayerUuid = var4;
      this.label = var5;
      this.staticPos = var6;
      this.createdAt = var7;
   }

   public String getId() {
      return this.id;
   }

   public String getSenderName() {
      return this.senderName;
   }

   public boolean isPlayerTarget() {
      return this.isPlayerTarget;
   }

   public UUID getTargetPlayerUuid() {
      return this.targetPlayerUuid;
   }

   public String getLabel() {
      return this.label;
   }

   public Vec3d getStaticPos() {
      return this.staticPos;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public Vec3d a(World var1) {
      if (this.isPlayerTarget && this.targetPlayerUuid != null && var1 != null) {
         PlayerEntity var2 = var1.getPlayerByUuid(this.targetPlayerUuid);
         if (var2 != null && var2.isAlive()) {
            return var2.getEyePos().add(0.0, 0.4, 0.0);
         }
      }

      return this.staticPos;
   }

   public boolean b(World var1) {
      if (this.isPlayerTarget && this.targetPlayerUuid != null && var1 != null) {
         PlayerEntity var2 = var1.getPlayerByUuid(this.targetPlayerUuid);
         return var2 != null && var2.isAlive();
      } else {
         return this.staticPos != null;
      }
   }
}
