package brokke.cc.module.combat;

import brokke.cc.core.AntiBotSystem;
import brokke.cc.core.AuraChecks;
import brokke.cc.manager.FriendManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.render.FreeLook;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventLook;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "AimAssist", description = "Плавная помощь в наводке на цель", category = Category.COMBAT)
public class AimAssist extends Function {
   private static final double DEG_PER_DELTA = 0.15;
   private static final double TICK_NANOS = 5.0E7;
   private static final long SCAN_INTERVAL_NANOS = 50000000L;
   private final ModeSetting rotation = new ModeSetting("rotation", "Linear", new String[]{"Linear", "Smooth", "Instant"});
   private final FloatSetting speed = new FloatSetting("speed", 3.0F, 1.0F, 10.0F, 0.1F);
   private final FloatSetting strength = new FloatSetting("strength", 100.0F, 0.0F, 100.0F, 1.0F);
   private final FloatSetting verticalFreedom = new FloatSetting("vertical_freedom", 0.0F, -90.0F, 90.0F, 1.0F);
   private final ModeSetting behavior = new ModeSetting("behavior", "Legit", new String[]{"Legit", "Rage"});
   private final FloatSetting slowThreshold = new FloatSetting("slow_threshold", 0.4F, 0.0F, 3.0F, 0.05F);
   private final FloatSetting legitSpeed = new FloatSetting("legit_speed", 1.0F, 0.1F, 5.0F, 0.1F);
   private final BooleanSetting randomize = new BooleanSetting("randomize", false);
   private final ModeSetting priority = new ModeSetting("priority", "Distance", new String[]{"Distance", "Health", "Angle"});
   private final FloatSetting distance = new FloatSetting("distance", 4.5F, 1.0F, 8.0F, 0.1F);
   private final FloatSetting fov = new FloatSetting("fov", 90.0F, 1.0F, 360.0F, 1.0F);
   private final BooleanSetting sticky = new BooleanSetting("sticky", false);
   private final BooleanSetting lastHitOnly = new BooleanSetting("last_hit_only", false);
   private final ModeSetting targets = new ModeSetting("targets", "Players", new String[]{"Players", "Mobs", "All"});
   private final ModeSetting conditions = new ModeSetting("conditions", "None", new String[]{"None", "Visible", "Weapon"});
   private LivingEntity target;
   private LivingEntity lastHit;
   private long acquiredAt;
   private long lastScan;
   private long lastFrame;

   public AimAssist() {
      this.addSettings(
         this.rotation,
         this.speed,
         this.strength,
         this.verticalFreedom,
         this.behavior,
         this.slowThreshold,
         this.legitSpeed,
         this.randomize,
         this.priority,
         this.distance,
         this.fov,
         this.sticky,
         this.lastHitOnly,
         this.targets,
         this.conditions
      );
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.acquiredAt = 0L;
      this.lastScan = 0L;
      this.lastFrame = 0L;
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.lastFrame = 0L;
   }

   public LivingEntity k() {
      return this.isActive() && this.target != null && this.target.isAlive() && !this.target.isRemoved() ? this.target : null;
   }

   @EventHandler
   public void onAttack(AttackEntityEvent var1) {
      if (var1.getPlayer() == mc.player && var1.getTarget() instanceof LivingEntity var2) {
         this.lastHit = var2;
      }
   }

   @EventHandler
   public void onLook(EventLook var1) {
      if (mc.player == null || mc.world == null) {
         this.target = null;
      } else if (mc.currentScreen == null) {
         FreeLook var2 = FunctionManager.a(FreeLook.class);
         if (var2 == null || !var2.k()) {
            Aura var3 = Aura.INSTANCE;
            if (var3 == null || !var3.l() || !var3.rotationType.isSelected("Legit")) {
               LivingEntity var4 = this.target;
               if (!this.b(var4)) {
                  long var5 = System.nanoTime();
                  if (var5 - this.lastScan < SCAN_INTERVAL_NANOS) {
                     return;
                  }

                  this.lastScan = var5;
                  LivingEntity var7 = this.l();
                  if (var7 != this.target) {
                     this.target = var7;
                     this.acquiredAt = var5;
                  }

                  var4 = this.target;
               }

               if (var4 != null) {
                  double[] var8 = this.a(mc.player, var1.getYaw(), var1.getPitch(), var4, this.strength.getValue() / 100.0);
                  var1.setYaw(var8[0]);
                  var1.setPitch(var8[1]);
               }
            }
         }
      }
   }

   public double[] a(ClientPlayerEntity var1, double var2, double var4, LivingEntity var6) {
      return this.a(var1, var2, var4, var6, this.strength.getValue() / 100.0);
   }

   public double[] a(ClientPlayerEntity var1, double var2, double var4, LivingEntity var6, double var7) {
      if (var1 == null || var6 == null || mc.world == null || !var6.isAlive() || var6.isRemoved()) {
         return new double[]{var2, var4};
      } else {
         long var9 = System.nanoTime();
         if (var6 != this.target) {
            this.target = var6;
            this.acquiredAt = var9;
         }

         if (var9 - this.acquiredAt < (long)(this.slowThreshold.getValue() * 1.0E9)) {
            return new double[]{var2, var4};
         } else {
            Vec3d var11 = var6.getBoundingBox().getCenter().subtract(var1.getEyePos());
            if (var11.lengthSquared() < 1.0E-8) {
               return new double[]{var2, var4};
            }

            double var12 = Math.toDegrees(Math.atan2(-var11.x, var11.z));
            double var14 = -Math.toDegrees(Math.atan2(var11.y, Math.hypot(var11.x, var11.z)));
            double var16 = MathHelper.wrapDegrees(var12 - var1.getYaw());
            double var18 = var14 - var1.getPitch();
            if (!this.behavior.isMode("Rage") && Math.hypot(var16, var18) > this.fov.getValue() * 0.5) {
               return new double[]{var2, var4};
            }

            double var20 = this.b(var9);
            double var22 = this.a(var16, var20, var7, var2 * DEG_PER_DELTA);
            double var24 = 0.0;
            if (Math.abs(var18) <= Math.abs(this.verticalFreedom.getFloatValue())) {
               var24 = this.a(var18, var20, var7, var4 * DEG_PER_DELTA);
            }

            return new double[]{var2 + var22 / DEG_PER_DELTA, var4 + var24 / DEG_PER_DELTA};
         }
      }
   }

   public LivingEntity l() {
      if (mc.player == null || mc.world == null) {
         return null;
      } else {
         LivingEntity var1 = null;
         double var2 = Double.MAX_VALUE;

         for (Entity var5 : mc.world.getEntities()) {
            if (var5 instanceof LivingEntity var6 && this.a(var6, true)) {
               double var7 = this.c(var6);
               if (var7 < var2) {
                  var2 = var7;
                  var1 = var6;
               }
            }
         }

         return var1;
      }
   }

   private double a(double var1, double var3, double var5, double var7) {
      if (Math.abs(var1) < 1.0E-4) {
         return 0.0;
      }

      double var9 = Math.clamp(var5, 0.0, 1.0);
      if (var9 <= 0.0) {
         return 0.0;
      }

      boolean var11 = this.behavior.isMode("Rage");
      if (!var11 && (var7 == 0.0 || Math.signum(var7) != Math.signum(var1))) {
         return 0.0;
      }

      double var12 = this.speed.getValue() * var9 * var3;
      if (var11) {
         var12 *= 1.5;
      } else {
         var12 /= Math.max(0.1, this.legitSpeed.getValue());
      }

      if (this.randomize.isToggled()) {
         var12 *= ThreadLocalRandom.current().nextDouble(0.85, 1.15);
      }

      String var14 = this.rotation.getActiveMode();
      if ("Instant".equals(var14)) {
         return var1 * var9;
      }

      double var15 = var12;
      if ("Smooth".equals(var14)) {
         var15 = Math.max(var12 * 0.5, Math.abs(var1) * (0.06 + 0.01 * this.speed.getValue()) * var3 * var9);
         if (!var11) {
            var15 /= Math.max(0.1, this.legitSpeed.getValue());
         }
      }

      return MathHelper.clamp(var1, -var15, var15);
   }

   /**
    * Доводка остаточной ошибки для режима SkyTime (использует настройки модуля, внутреннее состояние не трогает).
    *
    * @param residual остаток до цели, градусы (со знаком)
    * @param velocity дельта за тик (шаг нейросети): в Legit коррекция работает только при совпадении
    *                 знака — не спорит с нейросетью, не двигает прицел на freeze-паузах и сохраняет шум
    * @param dt       дельта времени тика, секунды
    * @param vertical true для pitch (учитывает vertical_freedom; 0 = без ограничения)
    * @return коррекция в градусах
    */
   public double correction(double residual, double velocity, float dt, boolean vertical) {
      if (residual == 0.0) {
         return 0.0;
      } else {
         double var5 = Math.clamp(this.strength.getValue() / 100.0, 0.0, 1.0);
         if (var5 <= 0.0) {
            return 0.0;
         }

         boolean var7 = this.behavior.isMode("Rage");
         if (!var7 && (velocity == 0.0 || Math.signum(velocity) != Math.signum(residual))) {
            return 0.0;
         }

         if (vertical) {
            double var8 = this.verticalFreedom.getFloatValue();
            if (var8 != 0.0 && Math.abs(residual) > Math.abs(var8)) {
               return 0.0;
            }
         }

         double var9 = Math.clamp(dt / 0.05, 0.25, 4.0);
         double var11 = this.speed.getValue() * var5 * var9 * (var7 ? 1.5 : 1.0) / Math.max(0.1, this.legitSpeed.getValue());
         if (this.randomize.isToggled() && var11 > 0.0) {
            var11 *= ThreadLocalRandom.current().nextDouble(0.85, 1.15);
         }

         String var13 = this.rotation.getActiveMode();
         if ("Instant".equals(var13)) {
            return Math.clamp(residual * var5, -Math.abs(residual), Math.abs(residual));
         }

         double var14 = var11;
         if ("Smooth".equals(var13)) {
            var14 = Math.max(var11 * 0.5, Math.abs(residual) * (0.06 + 0.01 * this.speed.getValue()) * var5 * var9);
         }

         return MathHelper.clamp(residual, -var14, var14);
      }
   }

   private double b(long var1) {
      double var3 = this.lastFrame == 0L ? 1.0 : (var1 - this.lastFrame) / TICK_NANOS;
      this.lastFrame = var1;
      return Math.clamp(var3, 0.25, 4.0);
   }

   private boolean b(LivingEntity var1) {
      return var1 != null && var1.isAlive() && !var1.isRemoved() && this.a(var1, false);
   }

   private boolean a(LivingEntity var1, boolean var2) {
      if (var1 == null || var1 == mc.player || mc.player == null) {
         return false;
      } else if (!var1.isAlive() || var1.isRemoved() || var1.getHealth() <= 0.0F) {
         return false;
      } else if (var1 instanceof ArmorStandEntity) {
         return false;
      } else {
         if (var1 instanceof PlayerEntity var3) {
            if (FriendManager.a(var3.getName().getString()) || AntiBotSystem.b(var3)) {
               return false;
            }

            if (this.targets.isMode("Mobs")) {
               return false;
            }
         } else if (this.targets.isMode("Players")) {
            return false;
         }

         if (this.lastHitOnly.isToggled() && var1 != this.lastHit) {
            return false;
         } else if (this.conditions.isMode("Weapon") && !AuraChecks.a(mc)) {
            return false;
         } else {
            double var6 = this.distance.getValue();
            Vec3d var4 = mc.player.getEyePos();
            if (var4.squaredDistanceTo(var1.getBoundingBox().getCenter()) > var6 * var6
               && mc.player.squaredDistanceTo(var1) > var6 * var6) {
               return false;
            } else {
               boolean var8 = this.conditions.isMode("Visible") || !this.sticky.isToggled();
               if (var8 && !AuraChecks.a(mc, var1)) {
                  return false;
               } else {
                  return !var2 || this.behavior.isMode("Rage") || this.d(var1) <= this.fov.getValue() * 0.5;
               }
            }
         }
      }
   }

   private double c(LivingEntity var1) {
      String var2 = this.priority.getActiveMode();
      if ("Health".equals(var2)) {
         return var1.getHealth();
      } else {
         return "Angle".equals(var2) ? this.d(var1) : mc.player.getEyePos().squaredDistanceTo(var1.getBoundingBox().getCenter());
      }
   }

   private double d(LivingEntity var1) {
      Vec3d var2 = var1.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      double var3 = Math.toDegrees(Math.atan2(-var2.x, var2.z));
      double var5 = -Math.toDegrees(Math.atan2(var2.y, Math.hypot(var2.x, var2.z)));
      return Math.hypot(MathHelper.wrapDegrees(var3 - mc.player.getYaw()), var5 - mc.player.getPitch());
   }
}
