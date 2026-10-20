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
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventLook;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.SendPacketEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(
   name = "TriggerBot",
   description = "Бьёт цель под прицелом с умными критами; режим «Легит» — реакция, промахи и сброс спринта как у человека",
   category = Category.COMBAT
)
public class TriggerBot extends Function {
   private static final long MIN_ATTACK_INTERVAL = 50L;
   private static final double LEGIT_REACH = 3.0;
   private static final float LEGIT_YAW_SLACK = 8.0F;
   private static final float LEGIT_PITCH_SLACK = 6.0F;
   private final ModeSetting attackMode = new ModeSetting("attack_mode", "Легит", new String[]{"Обычный", "Легит"});
   private final ModeSetting targetMode = new ModeSetting("target_mode", "Невидимых", new String[]{"Невидимых", "Только игроки"});
   private final ModeSetting hitConditions = new ModeSetting("hit_conditions", "Умные криты", new String[]{"Умные криты", "Только криты", "Через траву"});
   private final BooleanSetting weaponOnly = new BooleanSetting("weapon_only", true);
   private final FloatSetting reach = new FloatSetting("reach", 3.0F, 1.0F, 6.0F, 0.1F);
   private final BooleanSetting throughEntities = new BooleanSetting("through_entities", false);
   private final BooleanSetting customCooldown = new BooleanSetting("custom_cooldown", false);
   private final FloatSetting customCooldownPercent = new FloatSetting("custom_cooldown_percent", 100.0F, 0.0F, 100.0F, 1.0F)
      .showWhen(this.customCooldown::isToggled);
   private final BooleanSetting tpsSync = new BooleanSetting("tps_sync", false);
   private final BooleanSetting comboLunge = new BooleanSetting("combo_lunge", false).showWhen(() -> !this.isLegit());
   private final FloatSetting legitReaction = new FloatSetting("Реакция, мс", 180.0F, 60.0F, 450.0F, 5.0F)
      .showWhen(this::isLegit);
   private final FloatSetting legitHitChance = new FloatSetting("Шанс удара", 90.0F, 40.0F, 100.0F, 1.0F)
      .showWhen(this::isLegit);
   private final BooleanSetting legitSprintReset = new BooleanSetting("Легитный сброс спринта", true)
      .showWhen(this::isLegit);
   private final BooleanSetting legitMistakes = new BooleanSetting("Человеческие ошибки", true)
      .showWhen(this::isLegit);
   private final BooleanSetting legitVanillaReach = new BooleanSetting("Ванильная дистанция", true)
      .showWhen(this::isLegit);
   private LivingEntity target;
   private long lastAttack;
   private int legitTargetId = -1;
   private long legitReactAt;
   private long legitReadyAt;
   private long legitPauseUntil;
   private float legitThreshold = 0.98F;
   private int legitLastAttackAge = Integer.MIN_VALUE;
   private float lastSentYaw;
   private float lastSentPitch;
   private boolean hasSentRotation;

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
         this.comboLunge,
         this.legitReaction,
         this.legitHitChance,
         this.legitSprintReset,
         this.legitMistakes,
         this.legitVanillaReach
      );
   }

   public boolean isLegit() {
      return this.attackMode.isMode("Легит");
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
         if (this.isLegit() && this.legitVanillaReach.isToggled()) {
            // за ванильные 3 блока человек дотянуться не может
            var1 = Math.min(var1, LEGIT_REACH);
         }

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
      this.legitThreshold = this.legitRandomThreshold();
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
         // сброс спринта живёт тиками: без этого вызова клавиша осталась бы отжатой
         SprintReset.b();
         this.p(true);
      }
   }

   @EventHandler
   public void onLook(EventLook var1) {
      this.p(false);
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (this.legitSprintResetActive()) {
         SprintReset.a(var1);
      }
   }

   /**
    * Запоминаем поворот, который уже отправлен серверу. Легит-режим бьёт только
    * тогда, когда текущий взгляд совпадает с отправленным — как у человека,
    * который сначала доводит прицел и лишь потом кликает.
    */
   @EventHandler(priority = EventPriority.LOWEST)
   public void onPacket(SendPacketEvent var1) {
      if (!var1.isCancelled() && mc.player != null && var1.getPacket() instanceof PlayerMoveC2SPacket var2 && var2.changesLook()) {
         this.lastSentYaw = var2.getYaw(mc.player.getYaw());
         this.lastSentPitch = var2.getPitch(mc.player.getPitch());
         this.hasSentRotation = true;
      }
   }

   private boolean legitSprintResetActive() {
      return this.isLegit() && this.legitSprintReset.isToggled();
   }

   private void p(boolean var1) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.o();
         return;
      }

      if (mc.currentScreen != null || mc.interactionManager.isBreakingBlock()) {
         return;
      }

      Aura var2 = Aura.INSTANCE;
      boolean auraSkyTime = var2 != null && var2.l() && var2.rotationType.isSelected("SkyTime");
      if (var2 != null && var2.l() && !auraSkyTime) {
         return;
      }

      if (this.weaponOnly.isToggled() && !AuraChecks.a(mc)) {
         this.target = null;
         return;
      }

      boolean legit = this.isLegit() && !auraSkyTime;
      if (legit && !var1) {
         // легит-режим бьёт только из тика: максимум один удар за тик, ровно как у человека
         return;
      }

      LivingEntity var3 = this.l();
      if (legit) {
         var3 = this.legitSelection(var3, var1);
      }

      if (var3 == null) {
         return;
      }

      long var4 = System.currentTimeMillis();
      if (var4 - this.lastAttack < MIN_ATTACK_INTERVAL) {
         return;
      }

      float var6 = this.customCooldown.isToggled()
         ? (float)(this.customCooldownPercent.getValue() / 100.0)
         : (legit ? this.legitThreshold : IdealHitUtils.a());
      boolean var7 = this.b(var6)
         && this.c(var3)
         && this.a(mc.player.getMainHandStack().getItem() == Items.MACE, var3)
         && (!auraSkyTime || var2.skyTimeTriggerAllow());
      if (var7 && legit && !this.legitReady(var4)) {
         var7 = false;
      }

      if (!var7) {
         return;
      }

      boolean var8 = mc.player.isSprinting();
      this.target = var3;
      this.lastAttack = var4;
      this.legitLastAttackAge = mc.player.age;
      // Без повторного swingHand: LivingEntity.attack() уже махнул рукой и отправил
      // HandSwingC2SPacket — второй мах на удар считается лишним пакетом.
      mc.interactionManager.attackEntity(mc.player, var3);
      if (legit) {
         if (this.legitSprintReset.isToggled()) {
            // мягкий W-тап: на один тик отпускаем спринт, вперёд продолжаем идти
            SprintReset.a("Legit", var8);
         }

         this.legitThreshold = this.legitRandomThreshold();
         this.legitReadyAt = var4;
      } else if (this.comboLunge.isToggled()) {
         SprintReset.a("W-Tap", var8);
      }

      if (auraSkyTime) {
         var2.skyTimeTriggerAttacked();
      }
   }

   /**
    * Человеческая выборка цели: новая цель под прицелом требует времени на реакцию,
    * изредка случаются короткие паузы, а удар может не пройти по «человеческой» причине.
    */
   private LivingEntity legitSelection(LivingEntity var1, boolean var2) {
      long var3 = System.currentTimeMillis();
      if (var1 == null) {
         this.legitTargetId = -1;
         this.legitReactAt = 0L;
         return null;
      }

      int var5 = var1.getId();
      if (var5 != this.legitTargetId) {
         this.legitTargetId = var5;
         this.legitReactAt = var3 + this.legitReactionDelay();
         this.legitReadyAt = var3;
         this.legitThreshold = this.legitRandomThreshold();
         return null;
      }

      if (var3 < this.legitReactAt || var3 < this.legitReadyAt || var3 < this.legitPauseUntil) {
         return null;
      }

      if (var2 && this.legitMistakes.isToggled() && ThreadLocalRandom.current().nextInt(500) == 0) {
         // «моргнул»: короткая пауза, будто взгляд ушёл или рука дрогнула
         this.legitPauseUntil = var3 + 140L + ThreadLocalRandom.current().nextLong(260L);
         return null;
      }

      return var1;
   }

   private boolean legitReady(long var1) {
      if (mc.player == null) {
         return false;
      }

      if (!this.rotationSettled()) {
         // сервер ещё не знает текущий поворот — удар выглядел бы «из ниоткуда»
         return false;
      }

      if (mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD)) {
         // едим, пьём или тянем лук — человек сначала доест, а со щитом бить можно
         return false;
      }

      if (this.legitLastAttackAge == mc.player.age) {
         return false;
      }

      float var3 = this.legitHitChance.getFloatValue();
      if (var3 < 100.0F && ThreadLocalRandom.current().nextFloat() * 100.0F >= var3) {
         // промах по таймингу: кликнул чуть раньше/позже окна и переждал
         this.legitReadyAt = var1 + 80L + ThreadLocalRandom.current().nextLong(140L);
         this.legitThreshold = this.legitRandomThreshold();
         return false;
      }

      return true;
   }

   private boolean rotationSettled() {
      if (mc.player == null) {
         return false;
      }

      if (!this.hasSentRotation) {
         // первый удар после включения: считаем, что сервер видит текущий поворот
         this.lastSentYaw = mc.player.getYaw();
         this.lastSentPitch = mc.player.getPitch();
         this.hasSentRotation = true;
         return true;
      }

      float var1 = mc.player.getYaw() - this.lastSentYaw;
      var1 %= 360.0F;
      if (var1 >= 180.0F) {
         var1 -= 360.0F;
      } else if (var1 < -180.0F) {
         var1 += 360.0F;
      }

      float var2 = mc.player.getPitch() - this.lastSentPitch;
      return Math.abs(var1) <= LEGIT_YAW_SLACK && Math.abs(var2) <= LEGIT_PITCH_SLACK;
   }

   private long legitReactionDelay() {
      float var1 = ThreadLocalRandom.current().nextFloat();
      double var2 = this.legitReaction.getValue();
      // чаще реагируем около базового значения, изредка — заметно медленнее
      return (long)(var2 * (0.65F + var1 * var1 * 0.9F)) + ThreadLocalRandom.current().nextLong(25L);
   }

   private float legitRandomThreshold() {
      // человек не выжимает кулдаун идеально: 93–100%
      return 0.93F + ThreadLocalRandom.current().nextFloat() * 0.07F;
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
      this.legitTargetId = -1;
      this.legitReactAt = 0L;
      this.legitReadyAt = 0L;
      this.legitPauseUntil = 0L;
      this.legitLastAttackAge = Integer.MIN_VALUE;
      this.hasSentRotation = false;
   }
}
