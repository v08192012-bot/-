package brokke.cc.core;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

public final class AntiBotSystem {
   private static final Set<UUID> BOTS = ConcurrentHashMap.newKeySet();

   private AntiBotSystem() {
   }

   public static void a(ClientWorld var0) {
      if (var0 == null) {
         BOTS.clear();
      } else {
         for (PlayerEntity var2 : var0.getPlayers()) {
            a(var2);
         }
      }
   }

   public static boolean a(PlayerEntity var0) {
      if (var0 == null) {
         return false;
      } else if (c(var0)) {
         BOTS.add(var0.getUuid());
         return true;
      } else {
         return BOTS.contains(var0.getUuid());
      }
   }

   public static boolean b(PlayerEntity var0) {
      return var0 != null && BOTS.contains(var0.getUuid());
   }

   private static boolean c(PlayerEntity var0) {
      UUID var1 = UUID.nameUUIDFromBytes(("OfflinePlayer:" + var0.getName().getString()).getBytes(StandardCharsets.UTF_8));
      return !var0.getUuid().equals(var1) && var0 instanceof OtherClientPlayerEntity && !var0.getName().getString().contains("-");
   }
}
