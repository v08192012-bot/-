package brokke.cc.module.combat;

import brokke.cc.core.AntiBotSystem;
import brokke.cc.core.AuraChecks;
import brokke.cc.core.SprintReset;
import brokke.cc.core.TpsSync;
import brokke.cc.manager.FriendManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.rotation.IdealHitUtils;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventLook;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "TriggerBot", description = "Бьёт цель под прицелом с умными критами", category = Category.COMBAT)
public class TriggerBot extends Function {
   private static final long MIN_ATTACK_INTERVAL = 50L;
   private final ModeSetting attackMode = new ModeSetting("attack_mode", "Обычный", new String[]{"Обычный"});
   private final ModeSetting targetMode = new ModeSetting("target_mode", "Невидимых", new String[]{"Невидимых", "Только игроки"});
   private final ModeSetting hitConditions = new ModeSetting("hit_conditions", "Умные криты", new String[]{"Умные криты", "Только криты", "Через траву"});
   private final BooleanSetting weaponOnly = new BooleanSetting("weapon_only", true);
   private final FloatSetting reach = new FloatSetting("reach", 3.0F, 1.0F, 6.0F, 0.1F);
   private final BooleanSetting throughEntities = new BooleanSetting("through_entities", false);
   private final BooleanSetting customCooldown = new BooleanSetting("custom_cooldown", false);
   private final FloatSetting customCooldownPercent = new FloatSetting("custom_cooldown_percent", 100.0F, 0.0F, 100.0F, 1.0F)
      .showWhen(this.customCooldown::isToggled);
   private final BooleanSetting tpsSync = new BooleanSetting("tps_sync", false);
   private final BooleanSetting comboLunge = new BooleanSetting("combo_lunge", false);
   private LivingEntity target;
   private long lastAttack;

   public TriggerBot() {
      this.addSettings(
         this.attackMode,
         this.targetMode,
         this.hitConditions,
         this.weaponOnly,
         this.reach,
         this.throughEntities,
         this.customCooldown,
         this.customCooldownPercent,
         this.tpsSync,
         this.comboLunge
      );
   }

   public LivingEntity k() {
      return this.target != null && this.target.isAlive() && !this.target.isRemoved() && System.currentTimeMillis() - this.lastAttack < 1000L
         ? this.target
         : null;
   }

   public LivingEntity l() {
      if (mc.player == null || mc.world == null) {
         return null;
      } else {
         double var1 = this.reach.getValue();
         Vec3d var3 = mc.player.getEyePos();
         Vec3d var4 = mc.player.getRotationVec(1.0F);
         Vec3d var5 = var3.add(var4.multiply(var1));
         ShapeType var6 = this.hitConditions.isMode("Через траву") ? ShapeType.COLLIDER : ShapeType.OUTLINE;
         BlockHitResult var7 = mc.world.raycast(new RaycastContext(var3, var5, var6, FluidHandling.NONE, mc.player));
         Vec3d var8 = var7.getType() == Type.BLOCK ? var7.getPos() : var5;
         Box var9 = mc.player.getBoundingBox().stretch(var4.multiply(var1)).expand(1.0);
         Predicate<Entity> var10 = this.throughEntities.isToggled() ? this::a : (var1x -> var1x instanceof LivingEntity);
         EntityHitResult var11 = ProjectileUtil.raycast(mc.player, var3, var8, var9, var10, var1 * var1);
         return var11 != null && var11.getEntity() instanceof LivingEntity var12 && this.a(var12) ? var12 : null;
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.o();
   }

   @Override
   public void onDisable() {
      this.o();
      super.onDisable();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player == null || mc.world == null) {
         this.o();
      } else {
         IdealHitUtils.b();
         this.p();
      }
   }

   @EventHandler
   public void onLook(EventLook var1) {
      this.p();
   }

   private void p() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.o();
      } else if (mc.currentScreen == null && !mc.interactionManager.isBreakingBlock()) {
         Aura var1 = Aura.INSTANCE;
         boolean auraSkyTime = var1 != null && var1.l() && var1.rotationType.isSelected("SkyTime");
         if (var1 == null || !var1.l() || auraSkyTime) {
            if (this.weaponOnly.isToggled() && !AuraChecks.a(mc)) {
               this.target = null;
            } else {
               LivingEntity var2 = this.l();
               if (var2 != null) {
                  long var3 = System.currentTimeMillis();
                  if (var3 - this.lastAttack >= MIN_ATTACK_INTERVAL) {
                     float var5 = this.customCooldown.isToggled() ? (float)(this.customCooldownPercent.getValue() / 100.0) : IdealHitUtils.a();
                     if (this.b(var5)
                        && this.c(var2)
                        && this.a(mc.player.getMainHandStack().getItem() == Items.MACE, var2)
                        && (!auraSkyTime || var1.skyTimeTriggerAllow())) {
                        boolean var6 = mc.player.isSprinting();
                        this.target = var2;
                        this.lastAttack = var3;
                        mc.interactionManager.attackEntity(mc.player, var2);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        if (this.comboLunge.isToggled()) {
                           SprintReset.a("W-Tap", var6);
                        }
                        if (auraSkyTime) {
                           var1.skyTimeTriggerAttacked();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean c(LivingEntity var1) {
      if (this.hitConditions.isMode("Через траву")) {
         return true;
      } else if (this.hitConditions.isMode("Только криты")) {
         return !mc.player.isOnGround() && IdealHitUtils.a(var1, true);
      } else {
         return mc.player.isOnGround() || IdealHitUtils.a(var1, true);
      }
   }

   private boolean a(boolean var1, LivingEntity var2) {
      if (!var1) {
         return true;
      } else {
         AutoMace var3 = FunctionManager.a(AutoMace.class);
         return var3 == null || !var3.isActive() || var3.a(var2, this.reach.getValue());
      }
   }

   private boolean a(Entity var1) {
      if (!(var1 instanceof LivingEntity var2)) {
         return false;
      } else if (var2 == mc.player || !var2.isAlive() || var2.isRemoved() || var2.getHealth() <= 0.0F) {
         return false;
      } else if (var2 instanceof ArmorStandEntity) {
         return false;
      } else if (var2 instanceof PlayerEntity var3) {
         return !FriendManager.a(var3.getName().getString()) && !AntiBotSystem.b(var3) && !var3.isSpectator();
      } else {
         return !this.targetMode.isMode("Только игроки");
      }
   }

   private boolean b(float var1) {
      if (this.tpsSync.isToggled()) {
         float var2 = (float)mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
         if (var2 <= 0.0F) {
            return false;
         } else {
            long var3 = (long)(1000.0F / var2 * var1);
            return System.currentTimeMillis() - this.lastAttack >= TpsSync.INSTANCE.a(var3);
         }
      } else {
         return mc.player.getAttackCooldownProgress(0.0F) >= var1;
      }
   }

   private void o() {
      this.target = null;
      this.lastAttack = 0L;
   }
}
