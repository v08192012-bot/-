package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.misc.Optimizer;
import brokke.cc.core.FreeLookStorage;
import brokke.cc.manager.FriendManager;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.SendPacketEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "AutoExplosion", description = "Автоматически ставит и взрывает кристалл", category = Category.COMBAT)
public class AutoExplosion extends Function {
   private final BooleanSetting safeYourSelf = new BooleanSetting("Не взрывать себя", false);
   private final BooleanSetting safeFriend = new BooleanSetting("Не взрывать друзей", true);
   private final NumberSetting radius = new NumberSetting("Радиус", 5.0, 1.0, 10.0, 0.1);
   private final BooleanSetting comboRotate = new BooleanSetting("Ротация", true);
   private final BooleanSetting crystalSpam = new BooleanSetting("Спам кристаллами", false);
   private final NumberSetting spamRadius = new NumberSetting("Спам радиус", 5.0, 1.0, 7.0, 0.5).visibleWhen(this.crystalSpam::isToggled);
   private final NumberSetting spamDelay = new NumberSetting("Спам задержка", 1.0, 0.0, 10.0, 1.0).visibleWhen(this.crystalSpam::isToggled);
   private final NumberSetting spamMin = new NumberSetting("Спам мин урон", 4.0, 0.0, 36.0, 0.5).visibleWhen(this.crystalSpam::isToggled);
   private final NumberSetting spamSelf = new NumberSetting("Спам макс себе", 10.0, 0.0, 36.0, 0.5).visibleWhen(this.crystalSpam::isToggled);
   private final BooleanSetting spamRotate = new BooleanSetting("Спам ротация", true).visibleWhen(this.crystalSpam::isToggled);
   private final BooleanSetting spamOptimize = new BooleanSetting("Оптимизация рендера", true).visibleWhen(this.crystalSpam::isToggled);
   private static final double DIAMETER = 12.0;
   private BlockPos pos;
   private int ticksAction;
   private boolean crystalPlaced;
   private int hotSlot;
   private int spamCooldown;
   private int slotToRestore = -1;
   private int restoreTicks = 0;
   private int placementStage = 0;
   private int placementTicks = 0;
   private BlockHitResult pendingHit;
   private boolean packetPlace;

   public AutoExplosion() {
      this.addSettings(
         this.safeYourSelf,
         this.safeFriend,
         this.radius,
         this.comboRotate,
         this.crystalSpam,
         this.spamRadius,
         this.spamDelay,
         this.spamMin,
         this.spamSelf,
         this.spamRotate,
         this.spamOptimize
      );
   }

   @Override
   public void onDisable() {
      this.k();
      this.spamCooldown = 0;
      this.packetPlace = false;
      this.placementStage = 0;
      this.placementTicks = 0;
      this.pendingHit = null;
      if (mc.player != null && this.slotToRestore != -1) {
         mc.player.getInventory().setSelectedSlot(this.slotToRestore);
         if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.slotToRestore));
         }

         this.slotToRestore = -1;
         this.restoreTicks = 0;
      }

      this.l();
      super.onDisable();
   }

   @EventHandler
   public void onPacketSend(SendPacketEvent var1) {
      if (!this.packetPlace && var1.getPacket() instanceof PlayerInteractBlockC2SPacket var2 && mc.player != null && mc.world != null) {
         ItemStack var8 = mc.player.getMainHandStack();
         ItemStack var4 = mc.player.getOffHandStack();
         if ((var8.isOf(Items.OBSIDIAN) || var8.isOf(Items.CRYING_OBSIDIAN) || var4.isOf(Items.OBSIDIAN) || var4.isOf(Items.CRYING_OBSIDIAN))
            && this.pos == null) {
            this.hotSlot = this.b(mc.player);
            if (this.hotSlot != -1) {
               BlockHitResult var5 = var2.getBlockHitResult();
               BlockPos var6 = var5.getBlockPos();
               Direction var7 = var5.getSide();
               this.pos = var6.offset(var7);
               this.ticksAction = 0;
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      ClientPlayerEntity var2 = mc.player;
      ClientWorld var3 = mc.world;
      if (var2 != null && var3 != null) {
         if (this.restoreTicks > 0) {
            this.restoreTicks--;
            if (this.restoreTicks == 0 && this.slotToRestore != -1) {
               var2.getInventory().setSelectedSlot(this.slotToRestore);
               if (var2.networkHandler != null) {
                  var2.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.slotToRestore));
               }

               this.slotToRestore = -1;
            }
         }

         if (this.placementStage == 1) {
            this.placementTicks--;
            if (this.placementTicks <= 0 && this.pendingHit != null && mc.interactionManager != null) {
               this.placementStage = 0;
               BlockHitResult var4 = this.pendingHit;
               this.pendingHit = null;
               this.packetPlace = true;

               try {
                  mc.interactionManager.interactBlock(var2, Hand.MAIN_HAND, var4);
                  var2.swingHand(Hand.MAIN_HAND);
               } finally {
                  this.packetPlace = false;
               }

               this.restoreTicks = 2;
            }
         }

         if (this.pos == null) {
            if (this.crystalSpam.getValue()) {
               if (this.spamCooldown > 0) {
                  this.spamCooldown--;
               } else {
                  this.spamCooldown = (int)this.spamDelay.getValue();
                  this.a(var2, var3);
               }
            }
         } else {
            if (!this.crystalPlaced) {
               Block var9 = var3.getBlockState(this.pos).getBlock();
               boolean var5 = var9 == Blocks.OBSIDIAN || var9 == Blocks.BEDROCK || var9 == Blocks.CRYING_OBSIDIAN;
               if (!var5) {
                  this.ticksAction++;
                  if (this.ticksAction > 20) {
                     this.k();
                  }

                  return;
               }

               this.crystalPlaced = true;
               this.ticksAction = 0;
               if (this.placementStage != 0 || this.pendingHit != null || this.restoreTicks > 0 || this.slotToRestore != -1) {
                  return;
               }

               if (this.comboRotate.getValue()) {
                  this.a(var2, Vec3d.ofCenter(this.pos.up()));
               }

               Vec3d var6 = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 1.0, this.pos.getZ() + 0.5);
               this.a(var2, new BlockHitResult(var6, Direction.UP, this.pos, false));
            }

            if (this.a(var2, this.pos.up())) {
               this.k();
            } else {
               this.ticksAction++;
               if (this.ticksAction > 30) {
                  this.k();
               }
            }
         }
      }
   }

   private void k() {
      this.pos = null;
      this.crystalPlaced = false;
      this.ticksAction = 0;
      this.hotSlot = 0;
      if (this.comboRotate.getValue()) {
         this.l();
      }
   }

   private void a(ClientPlayerEntity var1, ClientWorld var2) {
      if (!var1.isUsingItem() && mc.interactionManager != null) {
         if (this.placementStage == 0 && this.pendingHit == null && this.restoreTicks <= 0 && this.slotToRestore == -1) {
            PlayerEntity var3 = this.b(var1, var2);
            if (var3 == null) {
               if (this.spamRotate.getValue()) {
                  this.l();
               }
            } else {
               boolean var4 = false;
               EndCrystalEntity var5 = null;
               double var6 = 0.0;

               for (EndCrystalEntity var9 : var2.getEntitiesByClass(EndCrystalEntity.class, var1.getBoundingBox().expand(7.0), var0 -> true)) {
                  if (var9.isAlive() && !(var1.getEyePos().distanceTo(var9.getEntityPos().add(0.0, 0.5, 0.0)) > 5.5)) {
                     double var10 = this.a(var9.getEntityPos().add(0.0, 0.5, 0.0), var3, var2, var1);
                     if (!(var10 < this.spamMin.getValue())) {
                        double var12 = this.a(var9.getEntityPos().add(0.0, 0.5, 0.0), var1, var2, var1);
                        if (!(var12 > this.spamSelf.getValue()) && var10 > var6) {
                           var6 = var10;
                           var5 = var9;
                        }
                     }
                  }
               }

               if (var5 != null && this.b(var1, var5.getBlockPos())) {
                  if (this.spamRotate.getValue()) {
                     this.a(var1, var5.getEntityPos().add(0.0, 0.5, 0.0));
                  }

                  mc.interactionManager.attackEntity(var1, var5);
                  var1.swingHand(Hand.MAIN_HAND);
                  this.m();
                  var4 = true;
               }

               BlockPos var31 = var1.getBlockPos();
               int var32 = (int)Math.ceil(this.spamRadius.getValue()) + 1;
               Vec3d var33 = var1.getEyePos();
               BlockPos var11 = null;
               Direction var34 = Direction.UP;
               double var13 = 0.0;

               for (int var15 = -var32; var15 <= var32; var15++) {
                  for (int var16 = -var32; var16 <= var32; var16++) {
                     for (int var17 = -var32; var17 <= var32; var17++) {
                        BlockPos var18 = var31.add(var15, var16, var17);
                        double var19 = this.spamRadius.getValue();
                        if (!(var15 * var15 + var16 * var16 + var17 * var17 > var19 * var19 + 2.0)) {
                           Block var21 = var2.getBlockState(var18).getBlock();
                           if (var21 == Blocks.OBSIDIAN || var21 == Blocks.BEDROCK) {
                              BlockPos var22 = var18.up();
                              if (var2.getBlockState(var22).isAir() && var2.getBlockState(var22.up()).isAir()) {
                                 Vec3d var23 = Vec3d.ofCenter(var22);
                                 if (!(var33.distanceTo(var23) > 5.5)) {
                                    Box var24 = new Box(var22.getX(), var22.getY(), var22.getZ(), var22.getX() + 1, var22.getY() + 2, var22.getZ() + 1);
                                    boolean var25 = false;

                                    for (Entity var27 : var2.getEntitiesByClass(Entity.class, var24, var0 -> true)) {
                                       if (var27 != var1 && var27.isAlive() && !var27.isSpectator()) {
                                          var25 = true;
                                          break;
                                       }
                                    }

                                    if (!var25) {
                                       Direction var38 = this.a(var2, var1, var33, var18);
                                       if (var38 != null) {
                                          double var39 = this.a(var23, var3, var2, var1);
                                          if (!(var39 < this.spamMin.getValue()) && !(var39 <= var13)) {
                                             double var29 = this.a(var23, var1, var2, var1);
                                             if (!(var29 > this.spamSelf.getValue()) && (!this.safeFriend.getValue() || !this.a(var22))) {
                                                var13 = var39;
                                                var11 = var18;
                                                var34 = var38;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (var11 == null) {
                  if (var4 && this.spamRotate.getValue()) {
                     this.l();
                  }
               } else {
                  int var35 = this.b(var1);
                  if (var35 == -1) {
                     if (var4 && this.spamRotate.getValue()) {
                        this.l();
                     }
                  } else {
                     Vec3d var36 = Vec3d.ofCenter(var11.up());
                     if (this.spamRotate.getValue()) {
                        this.a(var1, var36);
                     }

                     Vec3d var37 = new Vec3d(
                        var11.getX() + 0.5 + var34.getOffsetX() * 0.5,
                        var11.getY() + 0.5 + var34.getOffsetY() * 0.5,
                        var11.getZ() + 0.5 + var34.getOffsetZ() * 0.5
                     );
                     this.a(var1, new BlockHitResult(var37, var34, var11, false));
                     this.m();
                  }
               }
            }
         } else if (this.spamCooldown <= 0) {
            this.spamCooldown = (int)this.spamDelay.getValue();
         }
      }
   }

   private void a(ClientPlayerEntity var1, Vec3d var2) {
      if (!this.a(var1)) {
         Vec3d var3 = var1.getEyePos();
         Vec3d var4 = var2.subtract(var3);
         double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
         float var7 = (float)Math.toDegrees(Math.atan2(var4.z, var4.x)) - 90.0F;
         float var8 = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(var4.y, var5))), -90.0F, 90.0F);
         this.a(var7, var8);
      }
   }

   private void a(float var1, float var2) {
      if (mc.player != null) {
         if (!FreeLookStorage.isActive()) {
            FreeLookStorage.a(true);
         }

         float var3 = MathHelper.clamp(var2, -90.0F, 90.0F);
         float var4 = MathHelper.wrapDegrees(var1 - mc.player.getYaw());
         float var5 = var3 - mc.player.getPitch();
         float var6 = GCDUtil.a();
         if (var6 >= 1.0E-4F) {
            var4 = Math.round(var4 / var6) * var6;
            var5 = Math.round(var5 / var6) * var6;
         }

         float var7 = mc.player.getYaw() + var4;
         float var8 = MathHelper.clamp(mc.player.getPitch() + var5, -90.0F, 90.0F);
         mc.player.setYaw(var7);
         mc.player.setPitch(var8);
         mc.player.headYaw = var7;
         mc.player.bodyYaw = var7;
      }
   }

   private void l() {
      if (mc.player != null && FreeLookStorage.isActive()) {
         mc.player.setYaw(FreeLookStorage.getFreeYaw());
         mc.player.setPitch(FreeLookStorage.getFreePitch());
         mc.player.headYaw = FreeLookStorage.getFreeYaw();
         mc.player.bodyYaw = FreeLookStorage.getFreeYaw();
      }

      FreeLookStorage.a(false);
   }

   private boolean a(ClientPlayerEntity var1) {
      return var1.getMainHandStack().isOf(Items.END_CRYSTAL) || var1.getOffHandStack().isOf(Items.END_CRYSTAL);
   }

   private void m() {
      if (this.spamOptimize.getValue()) {
         try {
            Optimizer var1 = FunctionManager.a(Optimizer.class);
            if (var1 != null && !var1.isActive()) {
               var1.setEnabled(true);
            }
         } catch (Throwable var2) {
         }
      }
   }

   private PlayerEntity b(ClientPlayerEntity var1, ClientWorld var2) {
      PlayerEntity var3 = null;
      double var4 = 64.0;

      for (PlayerEntity var7 : var2.getPlayers()) {
         if (var7 != var1 && var7.isAlive() && !var7.isRemoved() && !var7.isSpectator() && !var7.isCreative() && !FriendManager.a(var7.getName().getString())) {
            double var8 = var1.squaredDistanceTo(var7);
            if (!(var8 > var4)) {
               var4 = var8;
               var3 = var7;
            }
         }
      }

      return var3;
   }

   private Direction a(ClientWorld var1, ClientPlayerEntity var2, Vec3d var3, BlockPos var4) {
      Direction var5 = null;
      double var6 = Double.MAX_VALUE;

      for (Direction var11 : Direction.values()) {
         Vec3d var12 = new Vec3d(
            var4.getX() + 0.5 + var11.getOffsetX() * 0.5, var4.getY() + 0.5 + var11.getOffsetY() * 0.5, var4.getZ() + 0.5 + var11.getOffsetZ() * 0.5
         );
         float[] var13 = this.a(var3, var12);
         if (this.a(var13, var4, var2)) {
            double var14 = var3.squaredDistanceTo(var12);
            if (var14 < var6) {
               var6 = var14;
               var5 = var11;
            }
         }
      }

      return var5;
   }

   private float[] a(Vec3d var1, Vec3d var2) {
      Vec3d var3 = var2.subtract(var1);
      double var4 = Math.sqrt(var3.x * var3.x + var3.z * var3.z);
      float var6 = (float)Math.toDegrees(Math.atan2(var3.z, var3.x)) - 90.0F;
      float var7 = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(var3.y, var4))), -90.0F, 90.0F);
      return new float[]{var6, var7};
   }

   private boolean a(float[] var1, BlockPos var2, ClientPlayerEntity var3) {
      Vec3d var4 = var3.getEyePos();
      Vec3d var5 = this.b(var1[0], var1[1]);
      Vec3d var6 = var4.add(var5.multiply(4.5));
      return new Box(var2).raycast(var4, var6).isPresent();
   }

   private Vec3d b(float var1, float var2) {
      float var3 = var2 * (float) (Math.PI / 180.0);
      float var4 = -var1 * (float) (Math.PI / 180.0);
      float var5 = MathHelper.cos(var4);
      float var6 = MathHelper.sin(var4);
      float var7 = MathHelper.cos(var3);
      float var8 = MathHelper.sin(var3);
      return new Vec3d(var6 * var7, -var8, var5 * var7);
   }

   private double a(Vec3d var1, LivingEntity var2, ClientWorld var3, ClientPlayerEntity var4) {
      if (var2.getEntityPos().distanceTo(var1) > 12.0) {
         return 0.0;
      }

      double var5 = this.a(var1, var2.getBoundingBox(), var3, var4);
      if (var5 <= 0.0) {
         return 0.0;
      }

      double var7 = (1.0 - var2.getEntityPos().distanceTo(var1) / 12.0) * var5;
      if (var7 <= 0.0) {
         return 0.0;
      }

      float var9 = (float)((var7 * var7 + var7) / 2.0 * 7.0 * 12.0 + 1.0);
      float var10 = var2.getArmor();
      float var11 = 0.0F;

      try {
         var11 = (float)var2.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      } catch (Throwable var161) {
      }

      float var12 = 2.0F + var11 / 4.0F;
      float var13 = MathHelper.clamp(var10 - var9 / var12, var10 * 0.2F, 20.0F);
      float var16 = var9 * (1.0F - var13 / 25.0F);
      StatusEffectInstance var14 = var2.getStatusEffect(StatusEffects.RESISTANCE);
      if (var14 != null) {
         var16 *= 1.0F - 0.2F * (var14.getAmplifier() + 1);
      }

      return Math.max(var16, 0.0F);
   }

   private double a(Vec3d var1, Box var2, ClientWorld var3, ClientPlayerEntity var4) {
      double var5 = 1.0 / ((var2.maxX - var2.minX) * 2.0 + 1.0);
      double var7 = 1.0 / ((var2.maxY - var2.minY) * 2.0 + 1.0);
      double var9 = 1.0 / ((var2.maxZ - var2.minZ) * 2.0 + 1.0);
      if (!(var5 < 0.0) && !(var7 < 0.0) && !(var9 < 0.0)) {
         double var11 = (1.0 - Math.floor(1.0 / var5) * var5) * 0.5;
         double var13 = (1.0 - Math.floor(1.0 / var9) * var9) * 0.5;
         int var15 = 0;
         int var16 = 0;

         for (double var17 = 0.0; var17 <= 1.0; var17 += var5) {
            for (double var19 = 0.0; var19 <= 1.0; var19 += var7) {
               for (double var21 = 0.0; var21 <= 1.0; var21 += var9) {
                  Vec3d var23 = new Vec3d(
                     MathHelper.lerp(var17, var2.minX, var2.maxX) + var11,
                     MathHelper.lerp(var19, var2.minY, var2.maxY),
                     MathHelper.lerp(var21, var2.minZ, var2.maxZ) + var13
                  );
                  BlockHitResult var24 = var3.raycast(new RaycastContext(var23, var1, ShapeType.COLLIDER, FluidHandling.NONE, var4));
                  if (var24.getType() == Type.MISS) {
                     var15++;
                  }

                  var16++;
               }
            }
         }

         return var16 == 0 ? 0.0 : (double)var15 / var16;
      } else {
         return 0.0;
      }
   }

   private void a(ClientPlayerEntity var1, BlockHitResult var2) {
      if (this.placementStage == 0 && this.pendingHit == null) {
         int var3 = this.b(var1);
         if (var3 != -1 && mc.interactionManager != null) {
            if (this.slotToRestore == -1) {
               this.slotToRestore = var1.getInventory().getSelectedSlot();
            }

            var1.getInventory().setSelectedSlot(var3);
            if (var1.networkHandler != null) {
               var1.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var3));
            }

            this.pendingHit = var2;
            this.placementStage = 1;
            this.placementTicks = 1;
         }
      }
   }

   private boolean a(ClientPlayerEntity var1, BlockPos var2) {
      if (mc.world != null && mc.interactionManager != null) {
         for (EndCrystalEntity var4 : mc.world.getEntitiesByClass(EndCrystalEntity.class, var1.getBoundingBox().expand(6.0), var0 -> true)) {
            if (var4.getBlockPos().equals(var2)) {
               if (this.b(var1, var2)) {
                  if (this.comboRotate.getValue()) {
                     this.a(var1, var4.getEntityPos().add(0.0, 0.5, 0.0));
                  }

                  mc.interactionManager.attackEntity(var1, var4);
                  var1.swingHand(Hand.MAIN_HAND);
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean b(ClientPlayerEntity var1, BlockPos var2) {
      if (this.safeYourSelf.getValue() && mc.world != null) {
         double var3 = this.a(Vec3d.ofBottomCenter(var2).add(0.0, 0.5, 0.0), var1, mc.world, var1);
         if (var3 > 2.0) {
            return false;
         }
      }

      return !this.safeFriend.getValue() || !this.a(var2);
   }

   private boolean a(BlockPos var1) {
      if (mc.world == null) {
         return false;
      }

      double var2 = this.radius.getValue();
      Box var4 = new Box(var1.getX() - var2, var1.getY() - var2, var1.getZ() - var2, var1.getX() + var2, var1.getY() + var2, var1.getZ() + var2);

      for (Entity var6 : mc.world.getEntitiesByClass(Entity.class, var4, var0 -> true)) {
         if (var6 instanceof PlayerEntity var7 && FriendManager.a(var7.getName().getString())) {
            return true;
         }
      }

      return false;
   }

   private int b(ClientPlayerEntity var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (var1.getInventory().getStack(var2).isOf(Items.END_CRYSTAL)) {
            return var2;
         }
      }

      return -1;
   }
}
