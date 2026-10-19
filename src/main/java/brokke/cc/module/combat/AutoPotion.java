package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.player.GuiMove;
import brokke.cc.core.HotbarSlot;
import brokke.cc.core.ItemSlot;
import brokke.cc.core.SlotGroup;
import brokke.cc.core.SlotGroups;
import brokke.cc.core.Timer2;
import brokke.cc.mixins.interfaces.IClientPlayerInteractionManager;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationMath;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.MultiSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.EntityUtility;
import brokke.cc.util.InventoryUtility;
import brokke.cc.util.PotionUtility;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "Auto Explosion", description = "Автоматически бросает зелья под себя", category = Category.COMBAT)
public class AutoPotion extends Function {
   private final MultiSetting potions = new MultiSetting("Зелья", "Сила", "Скорость", "Огнестойкость", "Исцеление", "Церка");
   private final NumberSetting healHealth = new NumberSetting("Здоровье исцеления", 6.0, 1.0, 19.0, 0.5);
   private final NumberSetting cerkaHealth = new NumberSetting("Здоровье церки", 6.0, 1.0, 19.0, 0.5);
   private final BooleanSetting throwUp = new BooleanSetting("Вверх", true);
   private final BooleanSetting throwWalls = new BooleanSetting("В стены", true);
   private final BooleanSetting donate = new BooleanSetting("Донат зелья", false);
   private final Timer2 staying = new Timer2();
   private final Timer2 ground = new Timer2();
   private final Timer2 wall = new Timer2();
   private final Timer2 roof = new Timer2();
   private final Timer2 strengthTimer = new Timer2();
   private final Timer2 speedTimer = new Timer2();
   private final Timer2 fireTimer = new Timer2();
   private final Timer2 healTimer = new Timer2();
   private final Timer2 cerkaTimer = new Timer2();
   private AutoPotion.a task;

   public AutoPotion() {
      this.potions.setOption("Сила", true);
      this.potions.setOption("Скорость", true);
      this.potions.setOption("Огнестойкость", true);
      this.potions.setOption("Исцеление", true);
      this.potions.setOption("Церка", true);
      this.addSettings(this.potions, this.healHealth, this.cerkaHealth, this.throwUp, this.throwWalls, this.donate);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         Rotation2 var2 = this.l();
         if (this.task != null) {
            boolean var3 = this.task.slot instanceof HotbarSlot;
            if (var3) {
               switch (this.task.stage) {
                  case 0:
                     InventoryUtility.e(this.task.slot.get45() - 36);
                     break;
                  case 1:
                     this.a(var2);
                     break;
                  case 2:
                     InventoryUtility.e(this.task.prevSlot);
               }
            } else {
               switch (this.task.stage) {
                  case 0:
                     InventoryUtility.d(this.task.slot.get45(), this.task.prevSlot);
                     InventoryUtility.e(this.task.prevSlot);
                     break;
                  case 1:
                     this.a(var2);
                     break;
                  case 2:
                     GuiMove.bypassQueue = true;

                     try {
                        InventoryUtility.d(this.task.slot.get45(), this.task.prevSlot);
                     } finally {
                        GuiMove.bypassQueue = false;
                     }
               }
            }

            if (this.task.stage < 2) {
               this.task.stage++;
               return;
            }

            this.task = null;
         }

         if (var2.getYaw() != -1.0F && var2.getPitch() != -1.0F) {
            this.a(StatusEffects.STRENGTH, "Сила", this.strengthTimer, () -> true, var2);
            this.a(StatusEffects.SPEED, "Скорость", this.speedTimer, () -> true, var2);
            this.a(StatusEffects.FIRE_RESISTANCE, "Огнестойкость", this.fireTimer, () -> true, var2);
            this.a(StatusEffects.INSTANT_HEALTH, "Исцеление", this.healTimer, () -> mc.player.getHealth() <= this.healHealth.getValue(), var2);
            this.a(StatusEffects.WEAKNESS, "Церка", this.cerkaTimer, this::k, var2);
         }
      }
   }

   private boolean k() {
      if (mc.player != null && mc.world != null) {
         SlotGroup<ItemSlot> var1 = SlotGroups.b().a(SlotGroups.a());
         ItemSlot var2 = var1.a(var0 -> PotionUtility.a(var0, StatusEffects.INSTANT_HEALTH) && !mc.player.getItemCooldownManager().isCoolingDown(var0));
         ItemSlot var3 = var1.a(var0 -> var0.getItem() == Items.ENCHANTED_GOLDEN_APPLE && !mc.player.getItemCooldownManager().isCoolingDown(var0));
         boolean var4 = false;

         for (AbstractClientPlayerEntity var6 : mc.world.getPlayers()) {
            if (mc.player.distanceTo(var6) < 5.0F && var6 != mc.player) {
               var4 = true;
               break;
            }
         }

         return mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.cerkaHealth.getValue() && var3 == null && var2 == null && var4;
      } else {
         return false;
      }
   }

   private void a(RegistryEntry<StatusEffect> var1, String var2, Timer2 var3, Supplier<Boolean> var4, Rotation2 var5) {
      if (this.task == null && mc.player != null && this.potions.isOptionEnabled(var2) && !mc.player.hasStatusEffect(var1) && var3.a(2000L) && var4.get()) {
         SlotGroup<ItemSlot> var6 = SlotGroups.b().a(SlotGroups.a());
         ItemSlot var7 = var6.a(this.a(var1));
         if (var7 != null) {
            this.task = new AutoPotion.a(var7, mc.player.getInventory().getSelectedSlot());
            var3.a();
         }
      }
   }

   private void a(Rotation2 var1) {
      if (mc.interactionManager != null && mc.world != null) {
         ((IClientPlayerInteractionManager)mc.interactionManager)
            .invokeSendSequencedPacket(mc.world, var1xx -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var1xx, var1.getYaw(), var1.getPitch()));
      }
   }

   private Rotation2 l() {
      if (mc.player != null && mc.world != null) {
         boolean var1 = false;
         float var2 = mc.player.getYaw();
         float var3 = 90.0F;
         if (this.potions.isOptionEnabled("Церка") && this.k()) {
            for (AbstractClientPlayerEntity var5 : mc.world.getPlayers()) {
               if (mc.player.distanceTo(var5) < 5.0F && var5 != mc.player) {
                  Vec3d var6 = mc.player.getCameraPosVec(1.0F);
                  double var7 = var5.getX() - var6.x;
                  double var9 = var5.getY() - var6.y;
                  double var11 = var5.getZ() - var6.z;
                  double var13 = Math.sqrt(var7 * var7 + var11 * var11);
                  return new Rotation2((float)(Math.toDegrees(Math.atan2(var11, var7)) - 90.0), (float)(-Math.toDegrees(Math.atan2(var9, var13))));
               }
            }
         }

         if (this.throwUp.isToggled()) {
            if (mc.player.getVelocity().x == 0.0
               && mc.player.getVelocity().z == 0.0
               && Math.abs(mc.player.getVelocity().y) < 0.1F
               && EntityUtility.a(mc.player) == Blocks.AIR) {
               if (this.staying.a(500L)) {
                  var3 = -90.0F;
                  return new Rotation2(var2, var3);
               }
            } else {
               this.staying.a();
            }
         }

         if (mc.player.isOnGround()) {
            if (this.ground.a(300L)) {
               BlockHitResult var16 = mc.world
                  .raycast(
                     new RaycastContext(
                        mc.player.getEyePos(),
                        mc.player.getEyePos().add(mc.player.getRotationVector(var3, var2).multiply(2.0)),
                        ShapeType.COLLIDER,
                        FluidHandling.NONE,
                        mc.player
                     )
                  );
               if (var16.getType() == Type.BLOCK) {
                  var1 = true;
               } else {
                  var3 = 76.0F;

                  for (int var18 = 0; var18 < 360; var18 += 45) {
                     BlockHitResult var21 = mc.world
                        .raycast(
                           new RaycastContext(
                              mc.player.getEyePos(),
                              mc.player.getEyePos().add(mc.player.getRotationVector(var3, var18).multiply(2.0)),
                              ShapeType.COLLIDER,
                              FluidHandling.NONE,
                              mc.player
                           )
                        );
                     if (var21.getType() == Type.BLOCK) {
                        var2 = RotationMath.b(mc.player.getYaw(), var18);
                        var1 = true;
                     }
                  }
               }
            }
         } else {
            this.ground.a();
            if (this.throwWalls.isToggled()) {
               boolean var17 = false;
               var3 = 5.0F;

               for (int var19 = 0; var19 < 360; var19 += 45) {
                  BlockHitResult var22 = mc.world
                     .raycast(
                        new RaycastContext(
                           mc.player.getEyePos(),
                           mc.player.getEyePos().add(mc.player.getRotationVector(var3, var19).multiply(0.5)),
                           ShapeType.COLLIDER,
                           FluidHandling.NONE,
                           mc.player
                        )
                     );
                  if (var22.getType() == Type.BLOCK) {
                     var2 = RotationMath.b(mc.player.getYaw(), var19);
                     var17 = true;
                  }
               }

               if (!var17) {
                  this.wall.a();
               } else if (this.wall.a(300L)) {
                  var1 = true;
               }

               if (!var1) {
                  boolean var20 = false;
                  var3 = -90.0F;
                  BlockHitResult var23 = mc.world
                     .raycast(
                        new RaycastContext(
                           mc.player.getEyePos(),
                           mc.player.getEyePos().add(mc.player.getRotationVector(var3, mc.player.getYaw()).multiply(0.5)),
                           ShapeType.COLLIDER,
                           FluidHandling.NONE,
                           mc.player
                        )
                     );
                  if (var23.getType() == Type.BLOCK) {
                     var2 = mc.player.getYaw();
                     var20 = true;
                  }

                  if (!var20) {
                     this.roof.a();
                  } else if (this.roof.a(300L)) {
                     var1 = true;
                  }
               }
            }
         }

         return var1 ? new Rotation2(var2, var3) : new Rotation2(-1.0F, -1.0F);
      } else {
         return new Rotation2(-1.0F, -1.0F);
      }
   }

   private Predicate<ItemStack> a(RegistryEntry<StatusEffect> var1) {
      return var2 -> {
         if (!var2.isEmpty() && var2.getItem() instanceof SplashPotionItem) {
            List<StatusEffectInstance> var3 = PotionUtility.a(var2);
            return !this.donate.isToggled() && var1 != StatusEffects.WEAKNESS
               ? var3.size() == 1 && var3.getFirst().getEffectType() == var1
               : var3.stream().anyMatch(var1xxx -> var1xxx.getEffectType() == var1);
         } else {
            return false;
         }
      };
   }

   static class a {
      int stage;
      final ItemSlot slot;
      final int prevSlot;

      public a(ItemSlot var1, int var2) {
         this.slot = var1;
         this.prevSlot = var2;
      }
   }
}
