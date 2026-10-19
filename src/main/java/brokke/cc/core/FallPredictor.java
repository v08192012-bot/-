package brokke.cc.core;

import brokke.cc.util.EnchantmentUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FallPredictor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static float a(PlayerEntity var0) {
      if (var0 != null && mc.world != null) {
         Vec3d var1 = var0.getVelocity();
         double var2 = var0.getY();
         float var4 = (float)var0.fallDistance;

         while (var2 > mc.world.getBottomY()) {
            BlockPos var5 = BlockPos.ofFloored(var0.getX(), var2, var0.getZ());
            if (!mc.world.isAir(var5)) {
               break;
            }

            var4 = (float)(var4 + (var1.y < 0.0 ? -var1.y : 0.0));
            var1 = new Vec3d(var1.x, (var1.y - 0.08) * 0.98, var1.z);
            var2 += var1.y;
         }

         float var8 = var4 - 3.0F;
         if (var8 <= 0.0F) {
            return 0.0F;
         }

         ItemStack var6 = var0.getEquippedStack(EquipmentSlot.FEET);
         if (!var6.isEmpty()) {
            int var7 = EnchantmentUtility.a(var6, Enchantments.FEATHER_FALLING);
            if (var7 > 0) {
               var8 *= 1.0F - var7 * 0.12F;
            }
         }

         return MathHelper.clamp(var8, 0.0F, Float.MAX_VALUE);
      } else {
         return 0.0F;
      }
   }

   private FallPredictor() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
