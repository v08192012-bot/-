package brokke.cc.cosmetic;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public class SkinCache {
   private static final Map<UUID, Identifier> CACHE = new ConcurrentHashMap<>();
   private static final Map<UUID, GpuTextureView> VIEW_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Supplier<SkinTextures>> USER_SUPPLIERS = new ConcurrentHashMap<>();
   private static final Map<Identifier, GpuTextureView> VIEW_BY_ID = new ConcurrentHashMap<>();

   public static Identifier a(MinecraftClient var0, PlayerEntity var1) {
      UUID var2 = var1.getUuid();
      Identifier var3 = CACHE.get(var2);
      if (var3 != null) {
         return var3;
      }

      if (var0.getNetworkHandler() != null) {
         PlayerListEntry var4 = var0.getNetworkHandler().getPlayerListEntry(var2);
         if (var4 != null) {
            try {
               SkinTextures var5 = var4.getSkinTextures();
               if (var5 != null) {
                  Identifier var6 = var5.body().texturePath();
                  CACHE.put(var2, var6);
                  return var6;
               }
            } catch (Exception var7) {
            }
         }
      }

      return null;
   }

   public static GpuTextureView b(MinecraftClient var0, PlayerEntity var1) {
      UUID var2 = var1.getUuid();
      GpuTextureView var3 = VIEW_CACHE.get(var2);
      if (var3 != null) {
         return var3;
      }

      Identifier var4 = a(var0, var1);
      if (var4 != null && var0.getTextureManager() != null) {
         try {
            AbstractTexture var5 = var0.getTextureManager().getTexture(var4);
            if (var5 != null) {
               GpuTextureView var6 = var5.getGlTextureView();
               if (var6 != null) {
                  VIEW_CACHE.put(var2, var6);
                  return var6;
               }
            }
         } catch (Exception var7) {
         }
      }

      return null;
   }

   public static GpuTextureView a(MinecraftClient var0, String var1) {
      if (var1 != null && !var1.trim().isEmpty() && var0 != null) {
         String var2 = var1.trim();
         Supplier var3 = USER_SUPPLIERS.computeIfAbsent(var2, var1x -> {
            try {
               UUID var2x = Uuids.getOfflinePlayerUuid(var1x);
               GameProfile var3x = new GameProfile(var2x, var1x);
               if (var0.getSkinProvider() != null) {
                  return var0.getSkinProvider().supplySkinTextures(var3x, false);
               }
            } catch (Exception var4x) {
            }

            return null;
         });
         Identifier var4 = null;
         if (var3 != null) {
            try {
               SkinTextures var5 = (SkinTextures)var3.get();
               if (var5 != null && var5.body() != null) {
                  var4 = var5.body().texturePath();
               }
            } catch (Exception var10) {
            }
         }

         if (var4 == null) {
            try {
               UUID var11 = Uuids.getOfflinePlayerUuid(var2);
               SkinTextures var6 = DefaultSkinHelper.getSkinTextures(var11);
               if (var6 != null && var6.body() != null) {
                  var4 = var6.body().texturePath();
               }
            } catch (Exception var9) {
            }
         }

         if (var4 != null && var0.getTextureManager() != null) {
            GpuTextureView var12 = VIEW_BY_ID.get(var4);
            if (var12 != null) {
               return var12;
            }

            try {
               AbstractTexture var13 = var0.getTextureManager().getTexture(var4);
               if (var13 != null) {
                  GpuTextureView var7 = var13.getGlTextureView();
                  if (var7 != null) {
                     VIEW_BY_ID.put(var4, var7);
                     return var7;
                  }
               }
            } catch (Exception var8) {
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void a() {
      CACHE.clear();
      VIEW_CACHE.clear();
      USER_SUPPLIERS.clear();
      VIEW_BY_ID.clear();
   }
}
