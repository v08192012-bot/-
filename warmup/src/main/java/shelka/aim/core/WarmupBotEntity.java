package shelka.aim.core;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Бот для разминки аима. Живёт только на клиенте: сервер о нём не знает,
 * поэтому он не может ни умереть по-настоящему, ни навредить.
 */
public class WarmupBotEntity extends PathAwareEntity {
   private static final Random RNG = new Random();

   private String skinName = "Target";
   private int totemPops;

   public int strafeDir = 1;
   public int strafeTicks;
   public int distTicks;
   public int jumpTicks;
   public double wantDist = 3.0;
   public double baseY;

   public WarmupBotEntity(EntityType<? extends PathAwareEntity> type, World world) {
      super(type, world);
   }

   public static Builder createBotAttributes() {
      return PathAwareEntity.createMobAttributes()
         .add(EntityAttributes.MAX_HEALTH, 20.0)
         .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
         .add(EntityAttributes.FOLLOW_RANGE, 0.0)
         .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.0);
   }

   public void setSkinName(String name) {
      this.skinName = name;
   }

   public String getSkinName() {
      return this.skinName;
   }

   public int getTotemPops() {
      return this.totemPops;
   }

   /** Локальное попадание: отлёт, партиклы, звук, hurt-анимация и тотем вместо смерти. */
   public void onHit(PlayerEntity attacker) {
      if (attacker == null || !this.isAlive()) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      float damage = (float)attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
      if (damage <= 0.0F) {
         damage = 8.0F;
      }

      boolean crit = attacker.fallDistance > 0.0
         && !attacker.isOnGround()
         && !attacker.isClimbing()
         && !attacker.isTouchingWater()
         && !attacker.hasStatusEffect(StatusEffects.BLINDNESS)
         && !attacker.hasVehicle();
      float dealt = crit ? damage * 1.5F : damage;

      double dx = this.getX() - attacker.getX();
      double dz = this.getZ() - attacker.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len > 0.001) {
         double push = crit ? 0.35 : 0.25;
         this.setVelocity(this.getVelocity().add(dx / len * push, 0.2, dz / len * push));
         this.velocityDirty = true;
      }

      if (client.world != null) {
         if (crit) {
            for (int i = 0; i < 15; i++) {
               client.world.addParticleClient(
                  ParticleTypes.CRIT,
                  this.getX() + (RNG.nextDouble() - 0.5) * 0.6,
                  this.getBodyY(0.5) + (RNG.nextDouble() - 0.5) * 0.6,
                  this.getZ() + (RNG.nextDouble() - 0.5) * 0.6,
                  (RNG.nextDouble() - 0.5) * 0.4,
                  RNG.nextDouble() * 0.2,
                  (RNG.nextDouble() - 0.5) * 0.4
               );
            }
         } else {
            for (int i = 0; i < 6; i++) {
               client.world.addParticleClient(
                  ParticleTypes.DAMAGE_INDICATOR,
                  this.getX() + (RNG.nextDouble() - 0.5) * 0.5,
                  this.getBodyY(0.5) + (RNG.nextDouble() - 0.5) * 0.5,
                  this.getZ() + (RNG.nextDouble() - 0.5) * 0.5,
                  0.1,
                  0.1,
                  0.1
               );
            }
         }
      }

      float left = this.getHealth() - dealt;
      if (left <= 0.0F) {
         this.totemPops++;
         this.setHealth(20.0F);
         if (client.world != null) {
            client.world.playSound(client.player, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

            for (int i = 0; i < 40; i++) {
               client.world.addParticleClient(
                  ParticleTypes.TOTEM_OF_UNDYING,
                  this.getX() + (RNG.nextDouble() - 0.5) * 0.8,
                  this.getBodyY(0.5) + (RNG.nextDouble() - 0.5) * 0.8,
                  this.getZ() + (RNG.nextDouble() - 0.5) * 0.8,
                  (RNG.nextDouble() - 0.5) * 0.6,
                  RNG.nextDouble() * 0.5,
                  (RNG.nextDouble() - 0.5) * 0.6
               );
            }
         }

         this.handleStatus((byte)35);
         this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
      } else {
         this.setHealth(left);
         this.hurtTime = 10;
         this.maxHurtTime = 10;
         this.handleStatus((byte)2);
         if (client.world != null) {
            client.world.playSound(
               client.player,
               this.getX(),
               this.getY(),
               this.getZ(),
               crit ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               (RNG.nextFloat() - RNG.nextFloat()) * 0.2F + 1.0F
            );
         }
      }
   }
}
