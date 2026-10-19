package brokke.cc.core;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class FakePlayerEntity extends PathAwareEntity {
   private String skinName = "Steve";
   private int totemPops = 0;
   public int strafeDir = 1;
   public int strafeTicks;
   public int distTicks;
   public int jumpTicks;
   public double wantDist = 3.0;
   public double baseY;

   public FakePlayerEntity(EntityType<? extends PathAwareEntity> var1, World var2) {
      super(var1, var2);
   }

   public static Builder a() {
      return PathAwareEntity.createMobAttributes()
         .add(EntityAttributes.MAX_HEALTH, 20.0)
         .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
         .add(EntityAttributes.FOLLOW_RANGE, 0.0)
         .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.0);
   }

   public void setSkinName(String var1) {
      this.skinName = var1;
   }

   public String getSkinName() {
      return this.skinName;
   }

   public int getTotemPops() {
      return this.totemPops;
   }

   public void a(PlayerEntity var1) {
      if (var1 != null && this.isAlive()) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         Random var3 = new Random();
         float var4 = (float)var1.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
         if (var4 <= 0.0F) {
            var4 = 8.0F;
         }

         boolean var5 = var1.fallDistance > 0.0
            && !var1.isOnGround()
            && !var1.isClimbing()
            && !var1.isTouchingWater()
            && !var1.hasStatusEffect(StatusEffects.BLINDNESS)
            && !var1.hasVehicle();
         float var6 = var5 ? var4 * 1.5F : var4;
         double var7 = this.getX() - var1.getX();
         double var9 = this.getZ() - var1.getZ();
         double var11 = Math.sqrt(var7 * var7 + var9 * var9);
         if (var11 > 0.001) {
            double var13 = var5 ? 0.35 : 0.25;
            this.setVelocity(this.getVelocity().add(var7 / var11 * var13, 0.2, var9 / var11 * var13));
            this.velocityDirty = true;
         }

         if (var2.world != null) {
            if (var5) {
               for (int var16 = 0; var16 < 15; var16++) {
                  var2.world
                     .addParticleClient(
                        ParticleTypes.CRIT,
                        this.getX() + (var3.nextDouble() - 0.5) * 0.6,
                        this.getBodyY(0.5) + (var3.nextDouble() - 0.5) * 0.6,
                        this.getZ() + (var3.nextDouble() - 0.5) * 0.6,
                        (var3.nextDouble() - 0.5) * 0.4,
                        var3.nextDouble() * 0.2,
                        (var3.nextDouble() - 0.5) * 0.4
                     );
               }
            } else {
               for (int var17 = 0; var17 < 6; var17++) {
                  var2.world
                     .addParticleClient(
                        ParticleTypes.DAMAGE_INDICATOR,
                        this.getX() + (var3.nextDouble() - 0.5) * 0.5,
                        this.getBodyY(0.5) + (var3.nextDouble() - 0.5) * 0.5,
                        this.getZ() + (var3.nextDouble() - 0.5) * 0.5,
                        0.1,
                        0.1,
                        0.1
                     );
               }
            }
         }

         float var18 = this.getHealth();
         float var14 = var18 - var6;
         if (var14 <= 0.0F) {
            this.totemPops++;
            this.setHealth(20.0F);
            if (var2.world != null) {
               var2.world.playSound(var2.player, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

               for (int var15 = 0; var15 < 40; var15++) {
                  var2.world
                     .addParticleClient(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        this.getX() + (var3.nextDouble() - 0.5) * 0.8,
                        this.getBodyY(0.5) + (var3.nextDouble() - 0.5) * 0.8,
                        this.getZ() + (var3.nextDouble() - 0.5) * 0.8,
                        (var3.nextDouble() - 0.5) * 0.6,
                        var3.nextDouble() * 0.5,
                        (var3.nextDouble() - 0.5) * 0.6
                     );
               }
            }

            this.handleStatus((byte)35);
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         } else {
            this.setHealth(var14);
            this.hurtTime = 10;
            this.maxHurtTime = 10;
            this.handleStatus((byte)2);
            if (var2.world != null) {
               var2.world
                  .playSound(
                     var2.player,
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     var5 ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_HURT,
                     SoundCategory.PLAYERS,
                     1.0F,
                     (var3.nextFloat() - var3.nextFloat()) * 0.2F + 1.0F
                  );
            }
         }
      }
   }
}
