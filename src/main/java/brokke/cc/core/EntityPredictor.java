package brokke.cc.core;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class EntityPredictor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static float a(LivingEntity var0, double var1, boolean var3, boolean var4) {
      if (mc.world != null && var0 != null) {
         float var5 = 0.0F;
         Box var6 = var0.getBoundingBox().expand(var1);

         for (Entity var8 : mc.world.getOtherEntities(var0, var6)) {
            if (var3 && var8 instanceof EndCrystalEntity) {
               float var10 = a(var8.getEntityPos(), var0);
               if (var10 > var5) {
                  var5 = var10;
               }
            } else if (var4 && var8 instanceof TntEntity) {
               float var9 = a(var8.getEntityPos(), var0);
               if (var9 > var5) {
                  var5 = var9;
               }
            }
         }

         return var5;
      } else {
         return 0.0F;
      }
   }

   public static float a(Vec3d var0, LivingEntity var1) {
      if (mc.world != null && var1 != null) {
         double var2 = 12.0;
         double var4 = var0.distanceTo(var1.getEntityPos()) / var2;
         if (var4 > 1.0) {
            return 0.0F;
         }

         double var6 = a(var0, (Entity)var1);
         double var8 = (1.0 - var4) * var6;
         float var10 = (float)((var8 * var8 + var8) / 2.0 * 7.0 * var2 + 1.0);
         DamageSource var11 = mc.world.getDamageSources().explosion(null, null);
         float var12 = a(var10);
         float var13 = a(var1, var12);
         return b(var1, var13);
      } else {
         return 0.0F;
      }
   }

   private static double a(Vec3d var0, Entity var1) {
      Box var2 = var1.getBoundingBox();
      double var3 = 1.0 / (var2.getLengthX() * 2.0 + 1.0);
      double var5 = 1.0 / (var2.getLengthY() * 2.0 + 1.0);
      double var7 = 1.0 / (var2.getLengthZ() * 2.0 + 1.0);
      double var9 = (1.0 - Math.floor(1.0 / var3) * var3) / 2.0;
      double var11 = (1.0 - Math.floor(1.0 / var7) * var7) / 2.0;
      if (!(var3 < 0.0) && !(var5 < 0.0) && !(var7 < 0.0)) {
         int var13 = 0;
         int var14 = 0;

         for (double var15 = 0.0; var15 <= 1.0; var15 += var3) {
            for (double var17 = 0.0; var17 <= 1.0; var17 += var5) {
               for (double var19 = 0.0; var19 <= 1.0; var19 += var7) {
                  double var21 = MathHelper.lerp(var15, var2.minX, var2.maxX);
                  double var23 = MathHelper.lerp(var17, var2.minY, var2.maxY);
                  double var25 = MathHelper.lerp(var19, var2.minZ, var2.maxZ);
                  Vec3d var27 = new Vec3d(var21 + var9, var23, var25 + var11);
                  BlockHitResult var28 = mc.world.raycast(new RaycastContext(var0, var27, ShapeType.COLLIDER, FluidHandling.NONE, var1));
                  if (var28.getType() == Type.MISS) {
                     var14++;
                  } else {
                     BlockPos var29 = var28.getBlockPos();
                     BlockState var30 = mc.world.getBlockState(var29);
                     if (var30.getBlock() != Blocks.OBSIDIAN && var30.getBlock() != Blocks.BEDROCK && var30.getBlock() != Blocks.RESPAWN_ANCHOR) {
                        var14++;
                     }
                  }

                  var13++;
               }
            }
         }

         return var13 == 0 ? 0.0 : (double)var14 / var13;
      } else {
         return 0.0;
      }
   }

   private static float a(float var0) {
      if (mc.world == null) {
         return var0;
      }

      return switch (mc.world.getDifficulty()) {
         case PEACEFUL -> 0.0F;
         case EASY -> Math.min(var0 / 2.0F + 1.0F, var0);
         case HARD -> var0 * 1.5F;
         default -> var0;
      };
   }

   private static float a(LivingEntity var0, float var1) {
      int var2 = var0.getArmor();
      float var3 = (float)var0.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      float var4 = 2.0F + var3 / 4.0F;
      float var5 = MathHelper.clamp(var2 - var1 / var4, var2 * 0.2F, 20.0F);
      return var1 * (1.0F - var5 / 25.0F);
   }

   private static float b(LivingEntity var0, float var1) {
      return var1;
   }

   private EntityPredictor() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
