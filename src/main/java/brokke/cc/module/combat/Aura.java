package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.ModuleClass;
import brokke.cc.module.NewFunction;
import brokke.cc.module.player.AutoEat;
import brokke.cc.module.player.NoRotate;
import brokke.cc.core.AuraChecks;
import brokke.cc.core.FakePlayerEntity;
import brokke.cc.core.FreeLookStorage;
import brokke.cc.core.SprintReset;
import brokke.cc.core.TpsSync;
import brokke.cc.manager.FriendManager;
import brokke.cc.nav.BestPoint;
import brokke.cc.rotation.AimAssistRotation;
import brokke.cc.rotation.FuntimeRotation;
import brokke.cc.rotation.Holy2Rotation;
import brokke.cc.rotation.HolyWorldRotation;
import brokke.cc.rotation.IdealHitUtils;
import brokke.cc.rotation.LegitRotation;
import brokke.cc.rotation.MultipointUtils;
import brokke.cc.rotation.NeuroRotation;
import brokke.cc.rotation.ReallyWorldRotation;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.rotation.RotationUtils;
import brokke.cc.rotation.RotationsSystem;
import brokke.cc.rotation.SkyTimeRotation;
import brokke.cc.rotation.SlothRotation;
import brokke.cc.rotation.SpookyTimeRotation;
import brokke.cc.rotation.TestRotation;
import brokke.cc.rotation.WhiteRiseRotation;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.ListSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventGameUpdate;
import brokke.cc.system.events.impl.EventKeyboardInput;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.SendPacketEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.HotbarUtil;
import brokke.cc.util.MathUtils;
import brokke.cc.util.MovingUtil;
import brokke.cc.util.PredictUtils;
import brokke.cc.util.TimerUtils;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@NewFunction(name = "Aura", description = "Автоматически наводиться и бьёт таргета", category = Category.COMBAT)
public class Aura extends Function {
   public static Aura INSTANCE;
   public final ModeSetting rotationType = new ModeSetting(
      "Ротация", "Legit", new String[]{"Legit", "ReallyWorld", "SkyTime", "Нейро", "SpookyTime"}
   );
   private final ListSetting targets = new ListSetting(
      "Таргеты", new BooleanSetting("Игроки", true), new BooleanSetting("Голые", true), new BooleanSetting("Мирные", false), new BooleanSetting("Мобы", true)
   );
   private final ModeSetting moveFix = new ModeSetting("Коррекция", "Свободная", new String[]{"Нет", "Свободная", "Сфокусированная", "Полная"});
   private final ModeSetting priority = new ModeSetting("Приоритет", "Дистанция", new String[]{"Дистанция", "Здоровье", "Угол", "Никакой"});
   private final FloatSetting range = new FloatSetting("Дистанция атаки", 3.0F, 0.0F, 6.0F, 0.05F);
   public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", false);
   /** Сколько тиков аура может ждать крит, прежде чем ударить обычным ударом. 0 — не ждать. */
   private final FloatSetting critWait = new FloatSetting("Ждать крит, тиков", 3.0F, 0.0F, 10.0F, 1.0F)
      .showWhen(() -> this.smartCrit.isEnabled());
   public final BooleanSetting onlyCrits = new BooleanSetting("Только критами", false);
   public final ModeSetting sprintReset = new ModeSetting("Сброс спринта", "Legit", new String[]{"Нет", "Legit", "W-Tap", "S-Tap"});
   private final BooleanSetting throughWalls = new BooleanSetting("Через блоки", false);
   private final BooleanSetting unpressShield = new BooleanSetting("Отжимать щит", false);
   private final BooleanSetting breakShield = new BooleanSetting("Ломать щит", true);
   private final BooleanSetting attackOnEating = new BooleanSetting("Не бить когда ешь", true);
   public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", true);
   private final FloatSetting skyTimeFov = new FloatSetting("Поле зрения", 120.0F, 30.0F, 180.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   private final FloatSetting skyTimeSpeed = new FloatSetting("Скорость ротации", 70.0F, 50.0F, 100.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   private final FloatSetting skyTimeHitChance = new FloatSetting("Шанс удара", 90.0F, 0.0F, 100.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   private final BooleanSetting skyTimeTurnAway = new BooleanSetting("Отведение после удара", true)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   private final BooleanSetting skyTimeJitter = new BooleanSetting("Джитер хитбокса", false)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   public final BooleanSetting skyTimeFreeCam = new BooleanSetting("Свободная камера", true)
      .showWhen(() -> this.rotationType.isSelected("SkyTime"));
   private final FloatSetting neuroSpeed = new FloatSetting("Скорость нейро", 70.0F, 50.0F, 100.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("Нейро"));
   private final FloatSetting neuroFov = new FloatSetting("Поле зрения нейро", 120.0F, 30.0F, 180.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("Нейро"));
   private final FloatSetting neuroSmooth = new FloatSetting("Плавность", 70.0F, 0.0F, 100.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("Нейро"));
   private final FloatSetting spookySpeed = new FloatSetting("Скорость SpookyTime", 70.0F, 50.0F, 100.0F, 1.0F)
      .showWhen(() -> this.rotationType.isSelected("SpookyTime"));
   private final BooleanSetting spookyMicro = new BooleanSetting("Микродрожь", true)
      .showWhen(() -> this.rotationType.isSelected("SpookyTime"));
   private LivingEntity target;
   Vec2f currentRotations = new Vec2f(0.0F, 0.0F);
   Vec2f targetRotations = new Vec2f(0.0F, 0.0F);
   private final TimerUtils attackTimer = new TimerUtils();
   private final TestRotation testRotation = new TestRotation();
   private final FuntimeRotation funtimeRotation = new FuntimeRotation();
   private final SlothRotation slothRotation = new SlothRotation();
   private final HolyWorldRotation holyWorldRotation = new HolyWorldRotation();
   private final Holy2Rotation holy2Rotation = new Holy2Rotation();
   private final WhiteRiseRotation whiteRiseRotation = new WhiteRiseRotation(this);
   private final LegitRotation legitRotation = new LegitRotation();
   private final ReallyWorldRotation reallyWorldRotation = new ReallyWorldRotation();
   private final AimAssistRotation aimAssistRotation = new AimAssistRotation();
   private final SkyTimeRotation skyTimeRotation = new SkyTimeRotation();
   private final NeuroRotation neuroRotation = new NeuroRotation();
   private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
   private boolean funtimeModeActive = false;
   private boolean slothModeActive = false;
   private boolean skyTimeModeActive = false;
   private boolean neuroModeActive = false;
   private boolean spookyTimeModeActive = false;
   private boolean holyWorldModeActive = false;
   private float lastServerYaw;
   private float lastServerPitch;
   private boolean hasServerRotation;
   /** Разброс момента удара: античиты (AutoClicker) ловят строго одинаковые интервалы. */
   private float attackJitter;
   /** Тик, на котором отпустили спринт ради крита (защита от повторного отпускания). */
   private int critReleaseAge = Integer.MIN_VALUE;
   /** Тик последнего удара: не больше одного attack-пакета за тик. */
   private int attackAge = Integer.MIN_VALUE;
   /** Сколько тиков удар уже готов, но ждёт крит (см. critWait). */
   private int critWaitTicks;
   /** Сколько тиков ждём спада hurtTime у цели, чтобы удар не ушёл в иммунитет. */
   private int hurtWaitTicks;
   /** Диагностика: удар отправлен, ждём подтверждения урона. */
   private int attackSentAge = Integer.MIN_VALUE;
   private LivingEntity attackTrackedTarget;
   private int attackTargetHurtAtSend;
   /** Удары и удары без урона — для .neuro status. */
   private static int dbgAttacks;
   private static int dbgNoDamage;
   private LivingEntity holyWorldServerTarget;
   private boolean auraAttackInProgress;
   private long holyWorldHiddenSinceNanos;
   private final TimerUtils backTimer = new TimerUtils();
   private TpsSync tpsSync = TpsSync.INSTANCE;
   /** Разброс после удара: только он, темп задаёт кулдаун (P()). */
   private static final int ATTACK_DELAY_MIN_TICKS = 0;
   private static final int ATTACK_DELAY_MAX_TICKS = 2;
   private int attackDelayTicks = 0;
   private int ticksToAttack = 0;
   private LivingEntity lastDataTarget = null;
   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;
   public static float adjYaw;
   public static float adjPitch;
   public static float otvodkaYaw;
   public static float otvodkaPitch;
   public boolean isRotated;
   private boolean returning = false;
   private boolean hadTarget = false;
   private float naturalYaw;
   private float naturalPitch;
   private float prevAuraPlayerYaw = Float.NaN;
   private float prevAuraPlayerPitch = 0.0F;
   private float lastAppliedAuraYawDelta = 0.0F;
   private float lastAppliedAuraPitchDelta = 0.0F;
   private long lastTargetScanNanos = 0L;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

   public void setTarget(LivingEntity var1) {
      if (this.target != var1) {
         this.critWaitTicks = 0;
         this.hurtWaitTicks = 0;
      }

      this.target = var1;
   }

   public void a(float var1, float var2) {
      this.lastAppliedAuraYawDelta += var1;
      this.lastAppliedAuraPitchDelta += var2;
   }

   public AimAssistRotation getAimAssistRotation() {
      return this.aimAssistRotation;
   }

   private void B() {
      this.returning = false;
      this.hadTarget = false;
      this.aimAssistRotation.b();
      RotationStorage.a().j();
   }

   public Aura() {
      INSTANCE = this;
      this.addSettings(
         this.rotationType,
         this.targets,
         this.range,
         this.smartCrit,
         this.critWait,
         this.onlyCrits,
         this.sprintReset,
         this.attackOnEating,
         this.throughWalls,
         this.unpressShield,
         this.breakShield,
         clientLook,
         this.moveFix,
         this.priority,
         this.skyTimeFov,
         this.skyTimeSpeed,
         this.skyTimeHitChance,
         this.skyTimeTurnAway,
         this.skyTimeJitter,
         this.skyTimeFreeCam,
         this.neuroSpeed,
         this.neuroFov,
         this.neuroSmooth,
         this.spookySpeed,
         this.spookyMicro
      );
   }

   public boolean l() {
      return this.isActive();
   }

   /** SkyTime делегирует атаки встроенному TriggerBot (см. TriggerBot.p). */
   public boolean skyTimeTriggerBotActive() {
      if (!this.l() || !this.rotationType.isSelected("SkyTime")) {
         return false;
      }

      TriggerBot var1 = FunctionManager.a(TriggerBot.class);
      return var1 != null && var1.isActive();
   }

   /** Разрешение удара для TriggerBot: еда, задержка ауры и шанс удара SkyTime. */
   public boolean skyTimeTriggerAllow() {
      if (!this.skyTimeTriggerBotActive()) {
         return true;
      } else if (this.attackOnEating.isEnabled() && (this.N() || AutoEat.l())) {
         return false;
      } else if (this.attackDelayTicks > 0) {
         return false;
      } else {
         if (this.skyTimeHitChance.getFloatValue() < 100.0F
            && ThreadLocalRandom.current().nextFloat() * 100.0F >= this.skyTimeHitChance.getFloatValue()) {
            this.attackDelayTicks = 1;
            return false;
         }

         return true;
      }
   }

   /** TriggerBot нанёс удар в SkyTime: отведение после удара + учёт кулдауна ауры. */
   public void skyTimeTriggerAttacked() {
      if (this.skyTimeTriggerBotActive()) {
         this.skyTimeRotation.b();
         this.attackDelayTicks = this.L();
         this.ticksToAttack = this.attackDelayTicks;
         this.attackTimer.a();
      }
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public Vec2f getCurrentRotations() {
      return this.currentRotations;
   }

   public Vec2f getTargetRotations() {
      return this.targetRotations;
   }

   public TimerUtils getAttackTimer() {
      return this.attackTimer;
   }

   @EventHandler
   public void onAttackEntity(AttackEntityEvent var1) {
      if (mc.player != null && mc.world != null && var1.getPlayer() == mc.player && var1.getTarget() instanceof LivingEntity var2 && this.e(var2)) {
         this.target = var2;
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      this.a(var1);
      SprintReset.a(var1);
      // Спринт больше не выключаем «на всякий случай» каждый воздушный тик:
      // это и давало рывки в движении. Под крит спринт отпускается точечно,
      // за тик до удара (scheduleCritRelease).
   }

   private void a(EventMoveInput var1) {
      if (mc.player == null || this.rotationType.isSelected("NoRotate") || this.rotationType.isSelected("Legit")) {
         return;
      }

      if (this.moveFix.getSelectedIndex() == 0) {
         return;
      }

      if (this.moveFix.getSelectedIndex() == 1) {
         // «Свободная»: ввод всегда доворачивается на разницу между камерой и игроком,
         // непрерывно, без подбора из 8 направлений. Старый MovingUtil.a() округлял ввод
         // до ближайшей диагонали, и при плавной наводке персонаж «залипал» и дёргался.
         // Делаем это и без цели: пока камера возвращается, ввод тоже должен быть согласован.
         float var2 = FreeLookStorage.isActive()
            ? MathHelper.wrapDegrees(FreeLookStorage.getFreeYaw() - mc.player.getYaw())
            : 0.0F;
         if (Math.abs(var2) > 0.01F) {
            float var3 = (float)Math.toRadians(var2);
            float var4 = MathHelper.sin(var3);
            float var5 = MathHelper.cos(var3);
            float var6 = var1.getStrafe();
            float var7 = var1.getForward();
            var1.setStrafe(var6 * var5 - var7 * var4);
            var1.setForward(var6 * var4 + var7 * var5);
         }
      } else if (this.target != null && this.moveFix.getSelectedIndex() == 2) {
         MovingUtil.a(var1, this.C());
      } else if (this.target != null && this.moveFix.getSelectedIndex() == 3) {
         this.a(var1, this.C(), this.D());
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput var1) {
      if (mc.player != null && mc.world != null && this.target != null && !this.rotationType.isSelected("NoRotate") && !this.rotationType.isSelected("Legit")) {
         float var2 = this.C();
         if (this.moveFix.getSelectedIndex() == 2) {
            var1.setYaw(var2, mc.player.getYaw());
         } else if (this.moveFix.getSelectedIndex() == 3) {
            var1.setYaw(var2, this.D());
         }
      }
   }

   private float C() {
      return RotationStorage.instance != null && RotationStorage.instance.getTargetRotation() != null
         ? RotationStorage.instance.getTargetRotation().getYaw()
         : mc.player.getYaw();
   }

   private float D() {
      return RotationUtils.b(this.target.getBoundingBox().getCenter()).x;
   }

   private void a(EventMoveInput var1, float var2, float var3) {
      float var4 = var1.getForward();
      float var5 = var1.getStrafe();
      if (var4 != 0.0F || var5 != 0.0F) {
         double var6 = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.a(var3, var4, var5)));
         float var8 = 0.0F;
         float var9 = 0.0F;
         float var10 = Float.MAX_VALUE;

         for (float var11 = -1.0F; var11 <= 1.0F; var11++) {
            for (float var12 = -1.0F; var12 <= 1.0F; var12++) {
               if (var11 != 0.0F || var12 != 0.0F) {
                  double var13 = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.a(var2, var11, var12)));
                  double var15;
                  if ((var15 = Math.abs(MathHelper.wrapDegrees(var6 - var13))) < var10) {
                     var10 = (float)var15;
                     var8 = var11;
                     var9 = var12;
                  }
               }
            }
         }

         var1.setForward(var8);
         var1.setStrafe(var9);
      }
   }

   @EventHandler
   private void onGameUpdate(EventGameUpdate var1) {
      if (mc.player != null && mc.world != null) {
         if (this.rotationType.isSelected("Legit")) {
            if (this.target != null) {
               if (!this.d(this.target)) {
                  this.I();
               }
            } else {
               long var2;
               if ((var2 = System.nanoTime()) - this.lastTargetScanNanos > 50000000L) {
                  this.lastTargetScanNanos = var2;
                  this.I();
               }
            }
         }

         if (this.target != null) {
            this.F();
         } else if (this.returning) {
            float var5 = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : this.naturalYaw;
            float var3 = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : this.naturalPitch;
            boolean var4;
            if (this.rotationType.isSelected("AimAssist")) {
               var4 = this.aimAssistRotation.c(var5, var3);
            } else {
               var4 = true;
            }

            if (var4) {
               this.B();
            }
         }
      }
   }

   @EventHandler
   public void onTick(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         this.lastYaw++;
         if (!FreeLookStorage.isActive() && !Float.isNaN(this.prevAuraPlayerYaw)) {
            float var2 = MathHelper.wrapDegrees(mc.player.getYaw() - this.prevAuraPlayerYaw) - this.lastAppliedAuraYawDelta;
            float var3 = mc.player.getPitch() - this.prevAuraPlayerPitch - this.lastAppliedAuraPitchDelta;
            if (Math.abs(var2) > 0.01F || Math.abs(var3) > 0.01F) {
               this.naturalYaw = MathHelper.wrapDegrees(this.naturalYaw + var2);
               this.naturalPitch = MathHelper.clamp(this.naturalPitch + var3, -90.0F, 90.0F);
               FreeLookStorage.setFreeYaw(this.naturalYaw);
               FreeLookStorage.setFreePitch(this.naturalPitch);
            }
         }

         this.prevAuraPlayerYaw = mc.player.getYaw();
         this.prevAuraPlayerPitch = mc.player.getPitch();
         this.lastAppliedAuraYawDelta = 0.0F;
         if (this.rotationType.isSelected("Legit")) {
            if (!clientLook.isEnabled()) {
               clientLook.setValue(true);
            }

            if (FreeLookStorage.isActive()) {
               FreeLookStorage.a(false);
            }
         }

         IdealHitUtils.b();
         SprintReset.b();
         if (this.target != null) {
            if (this.rotationType.isSelected("AimAssist")) {
               this.aimAssistRotation.d(this.target);
            }
         } else if (this.returning) {
            float var4 = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : this.naturalYaw;
            float var5 = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : this.naturalPitch;
            if (this.rotationType.isSelected("AimAssist")) {
               this.aimAssistRotation.b(var4, var5);
            }
         }

         if (this.ticksToAttack > 0) {
            this.ticksToAttack--;
         }

         if (this.attackDelayTicks > 0) {
            this.attackDelayTicks--;
         }

         this.verifyAttack();
         if (!ModuleClass.packetCriticals.a() || !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            this.scheduleCritRelease();
            this.E();
         }
      }
   }

   /** Диагностика для античитов: реальные дельты поворота между отправленными пакетами. */
   public static float dbgMaxYawStep;
   public static float dbgMaxPitchStep;
   public static int dbgBigSteps;

   /**
    * Через 2 тика после удара проверяем, вырос ли hurtTime у цели: если нет, сервер
    * удар не засчитал (иммунитет/щит/дистанция). Счётчик нужен, чтобы такие удары
    * было видно в .neuro status как «без урона», а не гадать по ощущениям.
    */
   private void verifyAttack() {
      if (this.attackSentAge == Integer.MIN_VALUE || mc.player == null) {
         this.attackSentAge = Integer.MIN_VALUE;
         this.attackTrackedTarget = null;
         return;
      }

      if (mc.player.age - this.attackSentAge < 2) {
         return;
      }

      LivingEntity var1 = this.attackTrackedTarget;
      boolean var2 = var1 == null || var1.isRemoved() || var1.isDead() || var1.hurtTime > this.attackTargetHurtAtSend;
      if (!var2) {
         dbgNoDamage++;
      }

      this.attackSentAge = Integer.MIN_VALUE;
      this.attackTrackedTarget = null;
   }

   public static int dbgAttacks() {
      return dbgAttacks;
   }

   public static int dbgNoDamage() {
      return dbgNoDamage;
   }

   public static void dbgReset() {
      dbgMaxYawStep = 0.0F;
      dbgMaxPitchStep = 0.0F;
      dbgBigSteps = 0;
      dbgAttacks = 0;
      dbgNoDamage = 0;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPacket(SendPacketEvent var1) {
      if (!var1.isCancelled() && var1.getPacket() instanceof PlayerMoveC2SPacket var2 && var2.changesLook() && mc.player != null) {
         float newYaw = var2.getYaw(mc.player.getYaw());
         float newPitch = var2.getPitch(mc.player.getPitch());
         if (this.hasServerRotation) {
            float dYaw = Math.abs(MathHelper.wrapDegrees(newYaw - this.lastServerYaw));
            float dPitch = Math.abs(newPitch - this.lastServerPitch);
            if (dYaw > dbgMaxYawStep) {
               dbgMaxYawStep = dYaw;
            }

            if (dPitch > dbgMaxPitchStep) {
               dbgMaxPitchStep = dPitch;
            }

            if (dYaw > 20.0F || dPitch > 15.0F) {
               dbgBigSteps++;
            }
         }

         this.lastServerYaw = newYaw;
         this.lastServerPitch = newPitch;
         this.hasServerRotation = true;
      }
   }

   /**
    * Крит требует, чтобы сервер видел нас БЕЗ спринта в момент удара.
    *
    * Раньше спринт отпускался прямо в тик удара и удар сдвигался на следующий — из-за
    * этого на прыжках терялось 2-3 удара и разваливалось комбо. Теперь спринт отпускается
    * ЗА ТИК до готовности удара: пакет STOP_SPRINTING уходит обычным путём, а удар
    * случается ровно в свой тик и уже критом.
    */
   private void scheduleCritRelease() {
      if (mc.player == null || this.target == null) {
         return;
      }

      if (!this.smartCrit.isEnabled() && !this.onlyCrits.isOn()) {
         return;
      }

      if (this.attackDelayTicks > 1 || this.critReleaseAge == mc.player.age) {
         return;
      }

      // Крит возможен только в падении, на земле и на подъёме отпускать нечего.
      if (!mc.player.isSprinting() || !IdealHitUtils.e()) {
         return;
      }

      float var1 = Math.min(0.995F, IdealHitUtils.a() + this.attackJitter);
      if (mc.player.getAttackCooldownProgress(0.5F) < var1 - 0.09F) {
         return;
      }

      if (SprintReset.d()) {
         this.critReleaseAge = mc.player.age;
      }
   }

   private void E() {
      this.I();
      if (this.target != null) {
         int var1 = this.attackOnEating.isEnabled() && (this.N() || AutoEat.l()) ? 1 : 0;
         if (this.S() && this.attackDelayTicks <= 0 && var1 == 0) {
            this.k(this.target);
         }

         if (this.P() && this.attackDelayTicks <= 0) {
            if (var1 != 0) {
               return;
            }

            if (this.skyTimeTriggerBotActive()) {
               return;
            }

            if (this.rotationType.isSelected("SkyTime")
               && this.skyTimeHitChance.getFloatValue() < 100.0F
               && ThreadLocalRandom.current().nextFloat() * 100.0F >= this.skyTimeHitChance.getFloatValue()) {
               this.attackDelayTicks = 1;
               return;
            }

            this.K();
            if (this.S()) {
               this.U();
            }
         }
      } else {
         this.attackDelayTicks = 0;
         this.backTimer.a();
         adjPitch = 0.0F;
         adjYaw = 0.0F;
         this.testRotation.a();
         this.W();
         this.R();
         this.X();
         this.Y();
         this.Z();
         this.AA();
         if (!this.returning) {
            this.aimAssistRotation.b();
         }

         this.whiteRiseRotation.a();
         this.legitRotation.a();
         this.reallyWorldRotation.a();
         this.lastDataTarget = null;
         this.ticksToAttack = 0;
      }
   }

   public void q() {
      this.F();
   }

   public void b(LivingEntity var1) {
      if (var1 != null && mc.player != null) {
         LivingEntity var2 = this.target;
         this.target = var1;

         try {
            this.F();
         } finally {
            if (var2 != null && var2.isAlive()) {
               this.target = var2;
            }
         }
      }
   }

   public void r() {
      if (this.rotationType.isSelected("Funtime")) {
         this.funtimeRotation.b();
      }

      if (this.rotationType.isSelected("WhiteRise")) {
         this.whiteRiseRotation.b();
      }

      if (this.rotationType.isSelected("ReallyWorld")) {
         this.reallyWorldRotation.b();
      }
   }

   private void F() {
      if (this.target != null) {
         if (!this.rotationType.isSelected("Funtime")) {
            this.R();
         }

         if (!this.rotationType.isSelected("SkyTime")) {
            this.Y();
         }

         if (!this.rotationType.isSelected("Нейро")) {
            this.Z();
         }

         if (!this.rotationType.isSelected("SpookyTime")) {
            this.AA();
         }

         if (!this.S()) {
            this.W();
         }

         if (!this.rotationType.isSelected("Sloth")) {
            this.X();
         }

         RotationsSystem var1;
         if (this.rotationType.isSelected("Smooth")) {
            var1 = new RotationsSystem() {
               @Override
               public void a(LivingEntity var1) {
                  if (!mc.player.isGliding()) {
                     Vec3d var2 = var1.getEntityPos().add(0.0, var1.getHeight() * 0.6F, 0.0).subtract(mc.player.getEyePos());
                     float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
                     float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, Math.hypot(var2.x, var2.z))));
                     RotationStorage.a(new Rotation2(var3, var4), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isEnabled());
                  } else {
                     Vec3d var8 = Vec3d.fromPolar(var1.getPitch(), var1.getYaw());
                     Vec3d var9 = var1.getRotationVector();
                     Vec3d var10 = var1.getEntityPos().add(0.0, var1.getHeight() * 0.6F, 0.0).subtract(mc.player.getEyePos());
                     Vec3d var5 = var8.normalize().lerp(var9, var8.length());
                     float var6 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0);
                     float var7 = (float)(-Math.toDegrees(Math.atan2(var10.y, Math.hypot(var10.x, var10.z))));
                     RotationStorage.a(new Rotation2(var6, var7), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isEnabled());
                  }
               }
            };
         } else if (this.rotationType.isSelected("Legit")) {
            var1 = this.legitRotation;
            Vec3d var2 = mc.player.getEyePos();
            Vec3d var3 = this.target.getBoundingBox().getCenter();
            float var4 = (float)(Math.toDegrees(Math.atan2(var3.z - var2.z, var3.x - var2.x)) - 90.0);
            this.targetRotations = new Vec2f(var4, mc.player.getPitch());
            this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
         } else if (this.rotationType.isSelected("ReallyWorld")) {
            var1 = this.reallyWorldRotation;
         } else if (this.S()) {
            if (!this.holyWorldModeActive) {
               this.V();
               this.holyWorldModeActive = true;
            }

            if (this.holyWorldServerTarget != this.target) {
               this.holyWorldServerTarget = this.target;
               this.hasServerRotation = false;
               this.holyWorldHiddenSinceNanos = 0L;
            }

            var1 = this.T();
         } else if (this.rotationType.isSelected("Test")) {
            var1 = this.testRotation;
         } else if (this.rotationType.isSelected("Funtime")) {
            if (!this.funtimeModeActive) {
               this.funtimeRotation.a();
               this.funtimeModeActive = true;
            }

            this.funtimeRotation.setVisibleAim(clientLook.isEnabled());
            this.funtimeRotation.a(this.j(this.target));
            var1 = this.funtimeRotation;
         } else if (this.rotationType.isSelected("Sloth")) {
            if (!this.slothModeActive) {
               this.slothRotation.a();
               this.slothModeActive = true;
            }

            if (this.holyWorldServerTarget != this.target) {
               this.holyWorldServerTarget = this.target;
               this.hasServerRotation = false;
               this.holyWorldHiddenSinceNanos = 0L;
            }

            this.slothRotation.setVisibleAim(false);
            this.slothRotation.a(this.j(this.target));
            var1 = this.slothRotation;
         } else if (this.rotationType.isSelected("Нейро")) {
            if (!this.neuroModeActive) {
               this.neuroRotation.a();
               this.neuroModeActive = true;
            }

            this.neuroRotation.setSpeed(this.neuroSpeed.getFloatValue());
            this.neuroRotation.setFov(this.neuroFov.getFloatValue());
            this.neuroRotation.setSmooth(this.neuroSmooth.getFloatValue());
            this.neuroRotation.setVisibleAim(clientLook.isEnabled());
            var1 = this.neuroRotation;
         } else if (this.rotationType.isSelected("SpookyTime")) {
            if (!this.spookyTimeModeActive) {
               this.spookyTimeRotation.a();
               this.spookyTimeModeActive = true;
            }

            this.spookyTimeRotation.setSpeed(this.spookySpeed.getFloatValue());
            this.spookyTimeRotation.setMicro(this.spookyMicro.isEnabled());
            this.spookyTimeRotation.setVisibleAim(clientLook.isEnabled());
            var1 = this.spookyTimeRotation;
         } else if (this.rotationType.isSelected("SkyTime")) {
            if (!this.skyTimeModeActive) {
               this.skyTimeRotation.a();
               this.skyTimeModeActive = true;
            }

            this.skyTimeRotation.setVisibleAim(!this.skyTimeFreeCam.isEnabled() && clientLook.isEnabled());
            this.skyTimeRotation.setFov(this.skyTimeFov.getFloatValue());
            this.skyTimeRotation.setSpeed(this.skyTimeSpeed.getFloatValue());
            this.skyTimeRotation.setJitter(this.skyTimeJitter.isEnabled());
            this.skyTimeRotation.setTurnAway(this.skyTimeTurnAway.isEnabled());
            var1 = this.skyTimeRotation;
         } else if (this.rotationType.isSelected("AimAssist")) {
            var1 = this.aimAssistRotation;
         } else if (this.rotationType.isSelected("NoRotate")) {
            var1 = new RotationsSystem() {
               @Override
               public void a(LivingEntity var1) {
                  RotationStorage.a(
                     new Rotation2(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()),
                     MathUtils.a(100.0F, 170.0F),
                     MathUtils.a(100.0F, 170.0F),
                     MathUtils.a(100.0F, 170.0F),
                     MathUtils.a(100.0F, 170.0F),
                     1,
                     6,
                     false
                  );
               }
            };
         } else if (this.rotationType.isSelected("Data")) {
            var1 = new RotationsSystem() {
               @Override
               public void a(LivingEntity var1) {
                  boolean var2 = Aura.this.J();
                  Vec3d var3 = Aura.this.g(var1);
                  Vec2f var4 = RotationUtils.b(var3);
                  float var5 = mc.player.getYaw();
                  float var6 = mc.player.getPitch();
                  float var7 = Math.abs(MathHelper.wrapDegrees(var4.x - var5));
                  float var8 = Math.abs(var4.y - var6);
                  boolean var9 = var7 > 70.0F || var7 > 42.0F && mc.player.squaredDistanceTo(var1) < 9.0;
                  Rotation2 var10 = new Rotation2(var4.x, var4.y);
                  Aura.this.targetRotations = new Vec2f(var10.getYaw(), var10.getPitch());
                  Aura.this.currentRotations = new Vec2f(var5, var6);
                  float var11 = var9 ? Math.max(95.0F, Math.min(180.0F, var7 * 1.45F)) : (var2 ? 24.0F : 11.5F);
                  float var12 = var9 ? Math.max(55.0F, Math.min(110.0F, Math.max(18.0F, var8 * 1.35F))) : (var2 ? 18.0F : 9.0F);
                  float var13 = var9 ? var11 : (var2 ? 18.0F : 9.0F);
                  float var14 = var9 ? var12 : (var2 ? 14.0F : 7.0F);
                  RotationStorage.a(var10, var11, var12, var13, var14, 1, 1, Aura.clientLook.isEnabled());
               }
            };
         } else {
            final Vec2f var5 = RotationUtils.b(this.a(this.target, this.target.getLeashPos(1.0F)));
            var1 = new RotationsSystem() {
               @Override
               public void a(LivingEntity var1) {
                  Aura.this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                  RotationStorage.a(new Rotation2(var5.x, var5.y), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isEnabled());
               }
            };
         }

         var1.a(this.target);
      }
   }

   public EntityHitResult a(float var1) {
      float var2 = mc.player.getYaw();
      float var3 = mc.player.getPitch();
      return this.a(var2, var3, var1);
   }

   public boolean a(LivingEntity var1, float var2) {
      if (var1 != null && mc.player != null) {
         EntityHitResult var3 = this.a(var2);
         return var3 != null && var3.getEntity() == var1;
      } else {
         return false;
      }
   }

   private EntityHitResult G() {
      return this.a(this.range.getFloatValue());
   }

   private EntityHitResult b(float var1, float var2) {
      return this.a(var1, var2, this.range.getFloatValue());
   }

   private EntityHitResult a(float var1, float var2, float var3) {
      if (mc.player != null && mc.world != null) {
         Vec3d var4 = mc.player.getCameraPosVec(1.0F);
         Vec3d var5 = Vec3d.fromPolar(var2, var1);
         Vec3d var6 = var4.add(var5.multiply(var3));
         double var7 = var3 * var3;
         if (!this.throughWalls.isEnabled()) {
            BlockHitResult var9 = mc.world.raycast(new RaycastContext(var4, var6, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (var9 != null && var9.getType() != Type.MISS) {
               var6 = var9.getPos();
               var7 = var4.squaredDistanceTo(var6);
            }
         }

         Box var11 = mc.player.getBoundingBox().stretch(var5.multiply(var3)).expand(1.0, 1.0, 1.0);
         EntityHitResult var10 = ProjectileUtil.raycast(mc.player, var4, var6, var11, var0 -> var0 != mc.player && var0.isAlive() && !var0.isSpectator(), var7);
         return var10 != null && var4.distanceTo(var10.getPos()) <= var3 ? var10 : null;
      } else {
         return null;
      }
   }

   public boolean a(float var1, float var2, LivingEntity var3, float var4) {
      return this.b(var1, var2, var3, var4);
   }

   public boolean c(LivingEntity var1) {
      return AuraChecks.a(mc, var1);
   }

   private Vec3d a(LivingEntity var1, Vec3d var2) {
      return mc.player != null && var1 != null && mc.player.isGliding() && var1.isGliding() ? PredictUtils.c(var1) : var2;
   }

   Vec3d g(LivingEntity var1) {
      if (this.h(var1)) {
         return this.a(var1, this.i(var1));
      }

      Vec3d var2 = BestPoint.a(var1);
      if (var2 == null) {
         var2 = MultipointUtils.a(var1);
      }

      if (var2 == null) {
         var2 = this.i(var1);
      }

      return this.a(var1, var2);
   }

   private boolean h(LivingEntity var1) {
      if (mc.player != null && var1 != null) {
         Vec3d var2 = mc.player.getEyePos();
         Box var3 = var1.getBoundingBox();
         if (var3.expand(0.12).contains(var2)) {
            return true;
         }

         Vec3d var4 = this.i(var1);
         return var2.squaredDistanceTo(var4) <= 2.25;
      } else {
         return false;
      }
   }

   private Vec3d i(LivingEntity var1) {
      Box var2 = var1.getBoundingBox();
      return new Vec3d(var2.getCenter().x, var2.minY + var2.getLengthY() * 0.72, var2.getCenter().z);
   }

   private LivingEntity H() {
      if (this.l() && mc.world != null && mc.player != null) {
         LivingEntity var1 = null;
         double var2 = Double.MAX_VALUE;
         Vec3d var4 = mc.player.getEyePos();
         float var5 = mc.player.getYaw();
         float var6 = mc.player.getPitch();
         String var7 = this.priority.getActiveMode();

         for (Entity var9 : mc.world.getEntities()) {
            if (var9 instanceof LivingEntity var10 && this.e(var10)) {
               double var11 = switch (var7) {
                  case "Здоровье" -> var10.getHealth();
                  case "Угол" -> {
                     Vec2f var15 = RotationUtils.b(var10.getBoundingBox().getCenter());
                     double var16 = Math.abs(MathHelper.wrapDegrees(var15.x - var5));
                     double var18 = Math.abs(MathHelper.wrapDegrees(var15.y - var6));
                     yield var16 + var18;
                  }
                  default -> var10.getBoundingBox().getCenter().squaredDistanceTo(var4);
               };
               if (var11 < var2) {
                  var2 = var11;
                  var1 = var10;
               }
            }
         }

         return var1;
      } else {
         return null;
      }
   }

   private void I() {
      this.lastTargetScanNanos = System.nanoTime();
      if (!this.l()) {
         this.target = null;
         if (this.returning) {
            this.B();
         }
      } else if (this.target == null || !this.e(this.target)) {
         LivingEntity var1 = this.target;
         this.target = this.H();
         this.critWaitTicks = 0;
         this.hurtWaitTicks = 0;
         if (this.target != null) {
            if (!this.hadTarget) {
               this.hadTarget = true;
               this.returning = false;
               if (mc.player != null && !FreeLookStorage.isActive()) {
                  this.naturalYaw = mc.player.getYaw();
                  this.naturalPitch = mc.player.getPitch();
                  FreeLookStorage.setFreeYaw(this.naturalYaw);
                  FreeLookStorage.setFreePitch(this.naturalPitch);
               }
            }
         } else if (var1 != null || this.hadTarget) {
            this.hadTarget = false;
            if (this.rotationType.isSelected("AimAssist")) {
               this.returning = true;
            } else {
               this.B();
            }
         }
      }
   }

   public LivingEntity a(double var1) {
      if (this.target != null && this.e(this.target)) {
         return this.target;
      }

      if (mc.world != null && mc.player != null) {
         LivingEntity var3 = null;
         double var4 = var1 * var1;

         for (Entity var7 : mc.world.getEntities()) {
            double var9;
            if (var7 instanceof LivingEntity var8 && this.e(var8) && (var9 = mc.player.squaredDistanceTo(var8)) < var4) {
               var4 = var9;
               var3 = var8;
            }
         }

         return var3;
      } else {
         return null;
      }
   }

   boolean J() {
      float var1 = mc.player.getAttackCooldownProgress(1.5F);
      float var2 = Math.max(0.82F, IdealHitUtils.a() - 0.08F);
      int var3 = var1 >= var2 ? 1 : 0;
      int var4 = !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0 ? 1 : 0;
      return var3 != 0 || var4 != 0;
   }

   private void K() {
      if (mc.currentScreen != null || mc.player == null || mc.getNetworkHandler() == null) {
         return;
      }

      // Удар по сущности, которой на сервере уже нет (деспавн бота, смерть в том же тике),
      // — это невалидный PlayerInteractEntityC2SPacket, то есть BadPackets.
      if (this.target == null || this.target.isRemoved() || !this.target.isAlive()) {
         return;
      }

      if (mc.player.isDead() || mc.player.isSpectator()) {
         return;
      }

      // Один удар за тик — как у ванили. Два attack-пакета в одном тике
      // античиты считают MorePackets/BadPackets.
      if (this.attackAge == mc.player.age) {
         return;
      }

      this.attackAge = mc.player.age;
      this.critWaitTicks = 0;
      this.hurtWaitTicks = 0;
      this.attackSentAge = mc.player.age;
      this.attackTrackedTarget = this.target;
      this.attackTargetHurtAtSend = this.target.hurtTime;
      dbgAttacks++;
      this.attackJitter = ThreadLocalRandom.current().nextFloat() * 0.03F;
      Hand var1 = null;
      if (this.unpressShield.isEnabled()) {
         var1 = this.M();
         if (var1 != null) {
            mc.interactionManager.stopUsingItem(mc.player);
         }
      }

      this.auraAttackInProgress = true;

      try {
         boolean var16 = mc.player.isSprinting();

         try {
            // Удар всегда текущим слотом. Никаких silent-switch (UpdateSelectedSlot туда-обратно
            // в одном тике) и никаких ручных START/STOP_SPRINTING вокруг удара — Shard/Sloth
            // ловят именно такие «невозможные» комбинации как BadPackets.
            mc.interactionManager.attackEntity(mc.player, this.target);
         } finally {
            if (mc.player != null && mc.player.isOnGround()) {
               SprintReset.a(this.sprintReset.getActiveMode(), var16);
            }
         }
      } finally {
         this.auraAttackInProgress = false;
      }

      // Взмах ОБЯЗАТЕЛЕН и ровно один: attackEntity в 1.21.11 рукой не машет,
      // ваниль делает swingHand из MinecraftClient.doAttack. Без HandSwingC2SPacket
      // удар выглядит как «удар без анимации» — это ровно то, что ловят Shard/Sloth.
      mc.player.swingHand(Hand.MAIN_HAND);
      if (var1 != null) {
         this.a(var1);
      }

      if (this.rotationType.isSelected("Funtime")) {
         this.funtimeRotation.b();
      }

      if (this.rotationType.isSelected("WhiteRise")) {
         this.whiteRiseRotation.b();
      }

      if (this.rotationType.isSelected("SkyTime")) {
         this.skyTimeRotation.b();
      }

      if (this.rotationType.isSelected("Нейро")) {
         this.neuroRotation.b();
      }

      if (this.rotationType.isSelected("SpookyTime")) {
         this.spookyTimeRotation.b();
      }

      this.attackDelayTicks = this.L();
      this.ticksToAttack = this.attackDelayTicks;
      this.attackTimer.a();
   }

   /**
    * Пауза после удара. Раньше тут был случайный разброс 3..14 тиков: он складывался
    * с кулдауном и заметно растягивал серию ударов. Теперь темп задаёт только кулдаун
    * (проверка в P()), а этот таймер добавляет 0..2 тика разброса, чтобы интервалы
    * не были строго одинаковыми (по ним ловят AutoClicker).
    */
   private int L() {
      if (this.rotationType.isSelected("Legit")) {
         return ThreadLocalRandom.current().nextInt(0, 2);
      }

      return ThreadLocalRandom.current().nextInt(ATTACK_DELAY_MIN_TICKS, ATTACK_DELAY_MAX_TICKS + 1);
   }

   private Hand M() {
      if (mc.player != null && mc.player.isBlocking()) {
         Hand var1 = mc.player.getActiveHand();
         if (var1 == null) {
            return null;
         } else {
            return this.a(mc.player.getStackInHand(var1)) ? var1 : null;
         }
      } else {
         return null;
      }
   }

   private boolean N() {
      return mc.player != null && mc.player.isUsingItem() ? !this.unpressShield.isEnabled() || this.M() == null : false;
   }

   private void a(Hand var1) {
      if (mc.player != null && mc.interactionManager != null && var1 != null && this.a(mc.player.getStackInHand(var1)) && !mc.player.isUsingItem()) {
         mc.interactionManager.interactItem(mc.player, var1);
      }
   }

   private boolean a(ItemStack var1) {
      return !var1.isEmpty() && var1.getItem() instanceof ShieldItem;
   }

   private boolean O() {
      if (mc.player == null) {
         return false;
      }

      Item var1 = mc.player.getMainHandStack().getItem();
      return var1 != Items.AIR
         && (
            HotbarUtil.c(var1)
               || HotbarUtil.b(var1)
               || HotbarUtil.a(var1)
               || HotbarUtil.d(var1)
               || var1 instanceof HoeItem
               || var1 instanceof MaceItem
               || var1 == Items.MACE
         );
   }

   public boolean d(LivingEntity var1) {
      if (var1 != null && var1 != mc.player) {
         if (!var1.isAlive() || var1.getHealth() <= 0.0F || var1.isRemoved()) {
            return false;
         } else if (var1 instanceof ArmorStandEntity) {
            return false;
         } else if (var1 instanceof PlayerEntity var2 && this.b(var2)) {
            return false;
         } else {
            double var4 = this.z() + 2.0;
            return mc.player != null && mc.player.squaredDistanceTo(var1) <= var4 * var4;
         }
      } else {
         return false;
      }
   }

   public boolean e(LivingEntity var1) {
      if (var1 != null && var1 != mc.player) {
         if (!var1.isAlive() || var1.getHealth() <= 0.0F) {
            return false;
         }

         if (var1 instanceof ArmorStandEntity) {
            return false;
         }

         if (!(var1 instanceof IronGolemEntity) && !(var1 instanceof BatEntity)) {
            if (var1 instanceof FakePlayerEntity) {
               if (!this.targets.isSelected("Игроки")) {
                  return false;
               }
            } else if (var1 instanceof PlayerEntity var2) {
               if (!this.targets.isSelected("Игроки")) {
                  return false;
               }

               if (this.b(var2)) {
                  return false;
               }

               if (this.c(var2) && !this.targets.isSelected("Голые")) {
                  return false;
               }
            } else if (!(var1 instanceof PassiveEntity) && !(var1 instanceof CodEntity)) {
               if (!(var1 instanceof HostileEntity)) {
                  return false;
               }

               if (!this.targets.isSelected("Мобы")) {
                  return false;
               }
            } else if (!this.targets.isSelected("Мирные")) {
               return false;
            }

            double var20;
            double var4 = (var20 = this.z()) + 2.0;
            if (mc.player.squaredDistanceTo(var1) > var4 * var4) {
               return false;
            }

            Box var6 = var1.getBoundingBox();
            Vec3d var7 = mc.player.getEyePos();
            double var8 = MathHelper.clamp(var7.x, var6.minX, var6.maxX);
            double var10 = MathHelper.clamp(var7.y, var6.minY, var6.maxY);
            double var12 = MathHelper.clamp(var7.z, var6.minZ, var6.maxZ);
            double var14 = var7.x - var8;
            double var16 = var7.y - var10;
            double var18 = var7.z - var12;
            if (var14 * var14 + var16 * var16 + var18 * var18 > var20 * var20) {
               return false;
            }

            if (!this.throughWalls.isEnabled() && !AuraChecks.a(mc, var1)) {
               return false;
            }

            if (this.S()) {
               if (!this.l(var1)) {
                  return false;
               }

               if (!this.throughWalls.isEnabled() && !AuraChecks.a(mc, var1)) {
                  return false;
               }
            }

            if (this.rotationType.isSelected("SkyTime") && !this.skyTimeRotation.e(var1)) {
               return false;
            }

            if (this.rotationType.isSelected("Нейро") && !this.neuroRotation.e(var1)) {
               return false;
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean b(PlayerEntity var1) {
      return var1 != null && var1.getName() != null ? FriendManager.a(var1.getName().getString()) : false;
   }

   private boolean c(PlayerEntity var1) {
      for (EquipmentSlot var5 : ARMOR_SLOTS) {
         if (!var1.getEquippedStack(var5).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public boolean f(LivingEntity var1) {
      if (mc.player != null && var1 != null && var1.isAlive()) {
         float var2 = this.rotationType.isSelected("Legit") ? Math.min(this.range.getFloatValue(), 3.0F) : this.range.getFloatValue();
         if (this.rotationType.isSelected("NoRotate")) {
            return this.throughWalls.isEnabled() || this.c(var1);
         } else if (!this.rotationType.isSelected("Legit") && !clientLook.isEnabled()) {
            return this.hasServerRotation
               ? this.p(this.lastServerYaw, this.lastServerPitch, var1, var2)
               : this.p(mc.player.getYaw(), mc.player.getPitch(), var1, var2);
         } else {
            return !this.throughWalls.isEnabled() && mc.crosshairTarget instanceof EntityHitResult var3 && var3.getEntity() == var1
               ? mc.player.getEyePos().distanceTo(var3.getPos()) <= var2
               : this.p(mc.player.getYaw(), mc.player.getPitch(), var1, var2);
         }
      } else {
         return false;
      }
   }

   /**
    * Мягкая проверка наводки для режима «Нейро».
    *
    * Жёсткий рейкаст по текущей позиции промахивается, когда цель бежит или когда
    * плавная наводка ещё догоняет кадр — из-за этого аура секундами «наводится,
    * но не бьёт». Здесь считаем расстояние от луча до хитбокса: пока луч проходит
    * в 0.4 блока от бокса, серверный луч тоже его заденет (бокс едет вместе с целью).
    */
   private boolean p(float var1, float var2, LivingEntity var3, float var4) {
      if (mc.player == null || mc.world == null || var3 == null) {
         return false;
      }

      Vec3d var5 = mc.player.getEyePos();
      Box var6 = var3.getBoundingBox().expand(var3.getTargetingMargin() + 0.3);
      if (var6.squaredMagnitude(var5) > (var4 + 0.5) * (var4 + 0.5)) {
         return false;
      }

      if (!this.throughWalls.isEnabled()) {
         Vec3d var7 = var6.getCenter();
         BlockHitResult var8 = mc.world.raycast(new RaycastContext(var5, var7, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (var8 != null && var8.getType() != Type.MISS && var5.squaredDistanceTo(var8.getPos()) < var5.squaredDistanceTo(var7) - 1.0E-4) {
            return false;
         }
      }

      Vec3d var9 = Vec3d.fromPolar(var2, var1).normalize();
      double var10 = MathHelper.clamp(var5.distanceTo(var6.getCenter()) + 0.6, 0.5, var4 + 0.6);
      Vec3d var11 = var5.add(var9.multiply(var10));
      double var12 = MathHelper.clamp(var11.x, var6.minX, var6.maxX) - var11.x;
      double var13 = MathHelper.clamp(var11.y, var6.minY, var6.maxY) - var11.y;
      double var14 = MathHelper.clamp(var11.z, var6.minZ, var6.maxZ) - var11.z;
      return var12 * var12 + var13 * var13 + var14 * var14 <= 0.16;
   }

   public boolean b(float var1, float var2, LivingEntity var3, float var4) {
      if (mc.player != null && mc.world != null && var3 != null && var3.isAlive()) {
         Vec3d var5 = mc.player.getEyePos();
         Vec3d var6 = Vec3d.fromPolar(var2, var1);
         Vec3d var7 = var5.add(var6.multiply(var4));
         double var8 = var4 * var4;
         if (!this.throughWalls.isEnabled()) {
            BlockHitResult var10 = mc.world.raycast(new RaycastContext(var5, var7, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (var10 != null && var10.getType() != Type.MISS) {
               var7 = var10.getPos();
               var8 = var5.squaredDistanceTo(var7);
            }
         }

         Box var13 = var3.getBoundingBox().expand(var3.getTargetingMargin());
         if (var13.contains(var5)) {
            Vec3d var14 = var3.getBoundingBox().getCenter().subtract(var5);
            return var14.lengthSquared() < 1.0E-6 ? true : var6.dotProduct(var14.normalize()) > 0.3;
         } else {
            Optional var11 = var13.raycast(var5, var7);
            if (var11.isPresent()) {
               Vec3d var12 = (Vec3d)var11.get();
               return var5.squaredDistanceTo(var12) <= var8;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean s() {
      return this.throughWalls.isEnabled();
   }

   private boolean P() {
      if (this.target != null && mc.player != null) {
         // Пакет атаки уходит из тика РАНЬШЕ пакета поворота этого же тика, поэтому
         // сервер проверяет попадание по нашей ПРОШЛОЙ наводке. Если она ещё не лежит
         // в хитбоксе — удар уходит «в молоко»: пакет есть, урона нет. Ждём тик, пока
         // наводка (и reach-чек сервера) не сойдётся. Раньше это было только в «Нейро»,
         // из-за чего в остальных режимах удары иногда не регали.
         if (this.hasServerRotation && !this.p(this.lastServerYaw, this.lastServerPitch, this.target, this.range.getFloatValue())) {
            return false;
         }

         // Джиттер ТОЛЬКО вверх: ударить раньше полного кулдауна = потерять урон,
         // а вот «всегда ровно на 0.98» — это ровный таймер, который ловят AutoClicker-чеки
         float var1 = Math.min(0.995F, IdealHitUtils.a() + this.attackJitter);
         if (mc.player.getAttackCooldownProgress(0.5F) < var1 - 0.001F) {
            return false;
         }

         // Цель ещё в иммунитете от предыдущего урона: пакет уйдёт, а урона не будет
         // (ровно это ощущается как «бьёт, но не регает»). Ждём, пока hurtTime спадёт.
         if (this.target.hurtTime > 0) {
            if (++this.hurtWaitTicks <= 3) {
               return false;
            }
         } else {
            this.hurtWaitTicks = 0;
         }

         if (!this.f(this.target)) {
            return false;
         }

         if (!this.throughWalls.isEnabled() && this.rotationType.isSelected("Data") && !this.a(this.G())) {
            return false;
         }

         if (mc.player.isGliding() && this.target.isGliding()) {
            Vec3d var7 = this.target.getEntityPos().add(0.0, this.target.getHeight() / 2.0, 0.0);
            byte var4 = 0;
            Vec3d var8 = PredictUtils.a(this.target, var7, var4);
            if (mc.player.getEyePos().distanceTo(var8) > 5.0) {
               return false;
            }
         } else {
            double var3 = this.target.getBoundingBox().squaredMagnitude(mc.player.getEyePos());
            // минус 8 см: цель за время пинга успевает отойти, и серверный Reach-чек
            // (он считает по своей, уже устаревшей позиции) ловит «удар с 3.01»
            double var5 = this.rotationType.isSelected("Legit") && !this.throughWalls.isEnabled()
               ? Math.min(this.range.getFloatValue(), 3.0F)
               : this.range.getFloatValue() - 0.08;
            if (var3 > var5 * var5) {
               return false;
            }
         }

         if (this.onlyCrits.isOn()) {
            return IdealHitUtils.d() || IdealHitUtils.e();
         }

         if (!this.smartCrit.isEnabled()) {
            return true;
         }

         // Крит возможен только в падении. На земле ждать нечего, а на подъёме
         // ожидание съедало 2-3 удара подряд (прыжок с ~1 блоком над головой) —
         // именно из-за этого разваливалось комбо. Ждём максимум 2 тика и только
         // сразу после верхней точки, когда fallDistance ещё нулевой.
         if (IdealHitUtils.e()) {
            this.critWaitTicks = 0;
            return true;
         }

         if (!mc.player.isOnGround()
            && mc.player.getVelocity().y < 0.0
            && this.critWaitTicks < Math.min(2, (int)this.critWait.getFloatValue())) {
            this.critWaitTicks++;
            return false;
         }

         this.critWaitTicks = 0;
         return true;
      } else {
         return false;
      }
   }

   private boolean Q() {
      EntityHitResult var1 = this.b(this.lastServerYaw, this.lastServerPitch);
      if (var1 != null && var1.getEntity() == this.target) {
         Vec3d var2 = mc.player.getEyePos();
         Vec3d var3 = this.target.getBoundingBox().getCenter().subtract(var2);
         if (var3.lengthSquared() < 1.0E-8) {
            return false;
         }

         Vec3d var4 = Vec3d.fromPolar(this.lastServerPitch, this.lastServerYaw);
         return Math.toDegrees(Math.acos(MathHelper.clamp(var4.normalize().dotProduct(var3.normalize()), -1.0, 1.0))) <= 22.0;
      } else {
         return false;
      }
   }

   private boolean j(LivingEntity var1) {
      if (var1 != null && var1 == this.target && mc.player != null) {
         float var2 = Math.max(0.62F, IdealHitUtils.a() - 0.18F);
         if (this.attackDelayTicks > 3 && mc.player.getAttackCooldownProgress(1.5F) < var2) {
            return false;
         }

         Box var3 = var1.getBoundingBox();
         Vec3d var4 = mc.player.getEyePos();
         double var5 = MathHelper.clamp(var4.x, var3.minX, var3.maxX);
         double var7 = MathHelper.clamp(var4.y, var3.minY, var3.maxY);
         double var9 = MathHelper.clamp(var4.z, var3.minZ, var3.maxZ);
         double var11 = var4.x - var5;
         double var13 = var4.y - var7;
         double var15 = var4.z - var9;
         double var17 = this.range.getFloatValue() + 0.4F;
         return var11 * var11 + var13 * var13 + var15 * var15 <= var17 * var17;
      } else {
         return false;
      }
   }

   private void R() {
      if (this.funtimeModeActive) {
         this.funtimeModeActive = false;
         this.funtimeRotation.a();
      }
   }

   private void Y() {
      if (this.skyTimeModeActive) {
         this.skyTimeModeActive = false;
         this.skyTimeRotation.a();
      }
   }

   private void Z() {
      if (this.neuroModeActive) {
         this.neuroModeActive = false;
         this.neuroRotation.a();
      }
   }

   private void AA() {
      if (this.spookyTimeModeActive) {
         this.spookyTimeModeActive = false;
         this.spookyTimeRotation.a();
      }
   }

   private boolean S() {
      return this.rotationType.isSelected("HolyWorld") || this.rotationType.isSelected("Holy2");
   }

   private RotationsSystem T() {
      return this.rotationType.isSelected("Holy2") ? this.holy2Rotation : this.holyWorldRotation;
   }

   private void k(LivingEntity var1) {
      if (this.rotationType.isSelected("Holy2")) {
         this.holy2Rotation.d(var1);
      } else {
         this.holyWorldRotation.d(var1);
      }
   }

   private void U() {
      if (this.rotationType.isSelected("Holy2")) {
         this.holy2Rotation.c();
      } else {
         this.holyWorldRotation.c();
      }
   }

   private boolean l(LivingEntity var1) {
      return true;
   }

   private void V() {
      this.holyWorldRotation.b();
      this.holy2Rotation.b();
   }

   private void W() {
      if (this.holyWorldModeActive) {
         this.holyWorldModeActive = false;
         this.V();
         this.holyWorldServerTarget = null;
         this.hasServerRotation = false;
         this.holyWorldHiddenSinceNanos = 0L;
         this.auraAttackInProgress = false;
      }
   }

   private void X() {
      if (this.slothModeActive) {
         this.slothModeActive = false;
         this.slothRotation.a();
      }

      this.holyWorldServerTarget = null;
      this.hasServerRotation = false;
      this.holyWorldHiddenSinceNanos = 0L;
   }

   public boolean t() {
      return this.l() && this.S();
   }

   public boolean u() {
      return this.t() && this.auraAttackInProgress;
   }

   public int getTicksToAttack() {
      return this.ticksToAttack;
   }

   private boolean a(EntityHitResult var1) {
      float var2 = Math.abs(MathHelper.wrapDegrees(this.targetRotations.x - mc.player.getYaw()));
      float var3 = Math.abs(this.targetRotations.y - mc.player.getPitch());
      int var4 = var2 <= 1.15F && var3 <= 0.9F ? 1 : 0;
      int var5 = var1 != null && var1.getEntity() == this.target ? 1 : 0;
      return var4 != 0 && var5 != 0;
   }

   public boolean w() {
      BlockPos var1 = BlockPos.ofFloored(mc.player.getEntityPos().add(0.0, -0.4, 0.0));
      return !mc.player.isSubmergedInWater() && mc.world.getBlockState(var1).isOf(Blocks.WATER);
   }

   public float x() {
      return mc.player == null ? 0.0F : mc.player.getAttackCooldownProgress(0.0F);
   }

   public float y() {
      return (float)(1.0 / mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20.0);
   }

   public float z() {
      return mc.player.isGliding()
         ? 0.0F
         : (this.rotationType.isSelected("Legit") ? Math.max(this.range.getFloatValue() + 1.5F, 4.5F) : this.range.getFloatValue());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.returning = false;
      this.hadTarget = false;
      this.prevAuraPlayerYaw = Float.NaN;
      this.B();
      if (this.target != null) {
         this.backTimer.a();
      }

      this.target = null;
      this.testRotation.a();
      this.V();
      this.holyWorldModeActive = false;
      this.funtimeRotation.a();
      this.funtimeModeActive = false;
      this.slothRotation.a();
      this.slothModeActive = false;
      this.skyTimeRotation.a();
      this.skyTimeModeActive = false;
      this.neuroRotation.a();
      this.neuroModeActive = false;
      this.hasServerRotation = false;
      this.holyWorldServerTarget = null;
      this.auraAttackInProgress = false;
      this.holyWorldHiddenSinceNanos = 0L;
      this.whiteRiseRotation.a();
      this.legitRotation.a();
      this.reallyWorldRotation.a();
      this.aimAssistRotation.b();
      this.lastDataTarget = null;
      this.ticksToAttack = 0;
      this.attackDelayTicks = 0;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.returning = false;
      this.hadTarget = false;
      this.prevAuraPlayerYaw = Float.NaN;
      if (mc.player != null) {
         this.naturalYaw = mc.player.getYaw();
         this.naturalPitch = mc.player.getPitch();
         FreeLookStorage.setFreeYaw(this.naturalYaw);
         FreeLookStorage.setFreePitch(this.naturalPitch);
      }

      if (this.rotationType.isSelected("Legit")) {
         clientLook.setValue(true);
         FreeLookStorage.a(false);
      }

      this.testRotation.a();
      this.V();
      this.holyWorldModeActive = false;
      this.funtimeRotation.a();
      this.funtimeModeActive = false;
      this.slothRotation.a();
      this.slothModeActive = false;
      this.skyTimeRotation.a();
      this.skyTimeModeActive = false;
      this.neuroRotation.a();
      this.neuroModeActive = false;
      this.hasServerRotation = false;
      this.holyWorldServerTarget = null;
      this.auraAttackInProgress = false;
      this.holyWorldHiddenSinceNanos = 0L;
      this.whiteRiseRotation.a();
      this.legitRotation.a();
      this.reallyWorldRotation.a();
      this.aimAssistRotation.b();
      this.lastDataTarget = null;
      this.ticksToAttack = 0;
      this.attackDelayTicks = 0;
      if (mc.player != null) {
         this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   public LegitRotation getLegitRotation() {
      return this.legitRotation;
   }
}
