package brokke.cc.rotation;

import brokke.cc.module.combat.Aura;
import brokke.cc.module.ModuleClass;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public final class IdealHitUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int WATER_CRIT_INTENT_TICKS = 8;
   private static final int WATER_CRIT_CONTACT_TICKS = 10;
   private static final double WATER_CRIT_MIN_UPWARD_VELOCITY = 0.05;
   private static int lastWaterContactAge = Integer.MIN_VALUE;
   private static int lastWaterCritIntentAge = Integer.MIN_VALUE;
   private static int lastAirAge = Integer.MIN_VALUE;
   private static int lastGroundAge = Integer.MIN_VALUE;

   private IdealHitUtils() {
   }

   public static float a() {
      if (mc.player == null) {
         return 1.0F;
      } else if (mc.player.getMainHandStack().getItem() == Items.AIR) {
         return 1.0F;
      } else {
         return !(mc.player.getMainHandStack().getItem() instanceof AxeItem) && !(mc.player.getMainHandStack().getItem() instanceof ShovelItem) ? 0.98F : 1.0F;
      }
   }

   public static boolean a(LivingEntity var0) {
      Aura var1 = Aura.INSTANCE;
      boolean var2 = var1 != null && var1.smartCrit.isEnabled();
      return a(var0, var2);
   }

   public static boolean a(LivingEntity var0, boolean var1) {
      if (mc.player == null) {
         return false;
      } else if (!var1) {
         return true;
      } else {
         i();
         b();
         boolean var2 = ModuleClass.packetCriticals != null && ModuleClass.packetCriticals.a();
         boolean var3 = h();
         boolean var4 = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
         if (var2 && var3) {
            return true;
         } else if (var2 && var4) {
            return mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0;
         } else if (d()) {
            return true;
         } else if (k()) {
            return l();
         } else if (mc.player.isGliding() || mc.player.getAbilities().flying) {
            return true;
         } else if (g()) {
            return true;
         } else {
            return c() ? e() : mc.player.isOnGround();
         }
      }
   }

   public static void b() {
      if (mc.player == null) {
         lastAirAge = Integer.MIN_VALUE;
         lastGroundAge = Integer.MIN_VALUE;
      } else {
         if (lastAirAge > mc.player.age || lastGroundAge > mc.player.age) {
            lastAirAge = Integer.MIN_VALUE;
            lastGroundAge = Integer.MIN_VALUE;
         }

         if (mc.player.isOnGround()) {
            lastGroundAge = mc.player.age;
         } else {
            lastAirAge = mc.player.age;
         }
      }
   }

   public static boolean c() {
      if (mc.player == null) {
         return false;
      } else if (!mc.player.isOnGround()) {
         return true;
      } else {
         return mc.options != null && mc.options.jumpKey.isPressed() ? true : lastAirAge != Integer.MIN_VALUE && mc.player.age - lastAirAge < 3;
      }
   }

   public static boolean d() {
      if (mc.player == null) {
         return false;
      }

      if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
         return true;
      }

      if (mc.world == null) {
         return false;
      }

      BlockPos var0 = BlockPos.ofFloored(mc.player.getEyePos());
      BlockPos var1 = BlockPos.ofFloored(mc.player.getEntityPos());
      BlockPos var2 = BlockPos.ofFloored(mc.player.getEntityPos().add(0.0, -0.4, 0.0));
      return mc.world.getBlockState(var1).isOf(Blocks.WATER)
         || mc.world.getBlockState(var0).isOf(Blocks.WATER)
         || mc.world.getBlockState(var2).isOf(Blocks.WATER);
   }

   public static boolean e() {
      if (mc.player == null) {
         return false;
      } else if (mc.player.isOnGround()) {
         return false;
      } else if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
         return false;
      } else if (mc.player.isInLava() || mc.player.isClimbing() || mc.player.hasVehicle() || mc.player.getAbilities().flying) {
         return false;
      } else if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
         return false;
      } else if (mc.player.isGliding()) {
         return false;
      } else {
         return h() ? false : mc.player.fallDistance > 0.0 && mc.player.getVelocity().y < 0.0;
      }
   }

   public static boolean f() {
      if (mc.player != null && mc.world != null) {
         Box var0 = mc.player.getBoundingBox().contract(0.03);
         Box var1 = new Box(var0.minX, var0.maxY, var0.minZ, var0.maxX, var0.maxY + 0.5, var0.maxZ);

         for (BlockPos var3 : BlockPos.iterate(
            MathHelper.floor(var1.minX),
            MathHelper.floor(var1.minY),
            MathHelper.floor(var1.minZ),
            MathHelper.floor(var1.maxX),
            MathHelper.floor(var1.maxY),
            MathHelper.floor(var1.maxZ)
         )) {
            BlockState var4 = mc.world.getBlockState(var3);
            if (!var4.isAir() && !var4.getCollisionShape(mc.world, var3).isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void i() {
      if (mc.player != null && mc.world != null) {
         boolean var0 = m();
         if (var0) {
            lastWaterContactAge = mc.player.age;
            if (j()) {
               lastWaterCritIntentAge = mc.player.age;
            }
         }
      } else {
         lastWaterContactAge = Integer.MIN_VALUE;
         lastWaterCritIntentAge = Integer.MIN_VALUE;
      }
   }

   private static boolean j() {
      return mc.player != null && mc.options != null
         ? mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() && mc.player.getVelocity().y > 0.05
         : false;
   }

   private static boolean k() {
      return mc.player != null && mc.options != null && mc.options.jumpKey.isPressed()
         ? mc.player.age - lastWaterCritIntentAge <= 8 && mc.player.age - lastWaterContactAge <= 10
         : false;
   }

   private static boolean l() {
      return mc.player != null
         && !mc.player.isOnGround()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && mc.player.fallDistance > 0.0
         && mc.player.getVelocity().y < 0.0;
   }

   private static boolean m() {
      if (mc.player != null && mc.world != null) {
         BlockPos var0 = BlockPos.ofFloored(mc.player.getEntityPos().add(0.0, -0.4, 0.0));
         return mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.world.getBlockState(var0).isOf(Blocks.WATER);
      } else {
         return false;
      }
   }

   public static boolean g() {
      if (mc.player != null && mc.world != null) {
         BlockPos var0 = BlockPos.ofFloored(mc.player.getEntityPos().add(0.0, mc.player.getHeight() / 2.0F, 0.0));
         return mc.player.isInLava()
            || mc.player.isClimbing()
            || mc.world.getBlockState(var0).isOf(Blocks.WATER)
            || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
            || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            || h()
            || mc.player.isGliding()
            || mc.player.hasVehicle()
            || mc.player.getAbilities().flying
            || mc.player.isTouchingWater() && !k()
            || f();
      } else {
         return true;
      }
   }

   public static boolean h() {
      if (mc.player != null && mc.world != null) {
         Box var0 = mc.player.getBoundingBox();

         for (BlockPos var2 : BlockPos.iterate(
            MathHelper.floor(var0.minX),
            MathHelper.floor(var0.minY),
            MathHelper.floor(var0.minZ),
            MathHelper.floor(var0.maxX),
            MathHelper.floor(var0.maxY),
            MathHelper.floor(var0.maxZ)
         )) {
            if (mc.world.getBlockState(var2).isOf(Blocks.COBWEB)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
