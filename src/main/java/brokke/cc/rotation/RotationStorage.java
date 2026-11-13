package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.combat.Aura;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationStorage {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   /**
    * Максимальный поворот за один тик (пакет). Больше — античиты (Shard/Sloth) видят
    * «телепорт прицела» и кикают BadPackets. 32°/тик ≈ 640°/с — верх человеческого флика.
    */
   private static final float MAX_YAW_STEP = 32.0F;
   private static final float MAX_PITCH_STEP = 26.0F;
   private static final float PITCH_LIMIT = 89.5F;
   /** Внешний модуль (WindHop) ведёт наводку сам: авто-отпускание камеры не срабатывает. */
   private static int aimHold;

   public static void holdAim() {
      aimHold++;
   }

   public static void releaseHold() {
      if (aimHold > 0) {
         aimHold--;
      }
   }

   public static boolean isAimHeld() {
      return aimHold > 0;
   }

   public static RotationStorage instance = new RotationStorage();
   private static boolean registered;
   private RotationStorage.a currentTask = RotationStorage.a.IDLE;
   private float currentYawSpeed;
   private float currentPitchSpeed;
   private float currentYawReturnSpeed;
   private float currentPitchReturnSpeed;
   private int currentPriority;
   private int currentTimeout;
   private int idleTicks;
   private Rotation2 targetRotation;
   private boolean clientLook;
   /** Бюджет поворота на текущий тик: несколько модулей за тик не должны в сумме дать больше лимита. */
   private int applyAge = Integer.MIN_VALUE;
   private float appliedYaw;
   private float appliedPitch;

   public RotationStorage() {
      instance = this;
      if (!registered) {
         EventBus.register(this);
         registered = true;
      }
   }

   public static RotationStorage a() {
      if (instance == null) {
         instance = new RotationStorage();
      }

      return instance;
   }

   public boolean isClientLook() {
      return this.clientLook;
   }

   public int getCurrentPriority() {
      return this.currentPriority;
   }

   /**
    * Полный сброс наводки: задача, приоритет, цель и тихий режим камеры.
    * Нужен, когда аура отпускает цель или выключается: иначе «тихая» камера
    * оставалась висеть, и взгляд переставал реагировать на мышь.
    */
   public static void releaseAim() {
      RotationStorage var0 = a();
      var0.currentTask = RotationStorage.a.IDLE;
      var0.currentPriority = 0;
      var0.targetRotation = null;
      var0.clientLook = true;
      var0.idleTicks = 0;
      FreeLookStorage.restore();
   }

   /**
    * Сбросить приоритет текущей задачи, не трогая цель и камеру.
    * Нужно модулям, которые на время забирают наводку выше ауры: иначе после них
    * высокий приоритет остался бы висеть и блокировал ауру.
    */
   public void resetPriority() {
      this.currentPriority = 0;
   }

   public Rotation2 getTargetRotation() {
      return this.targetRotation;
   }

   public RotationStorage.a getCurrentTask() {
      return this.currentTask;
   }

   public int getIdleTicks() {
      return this.idleTicks;
   }

   public int getCurrentTimeout() {
      return this.currentTimeout;
   }

   public float getCurrentYawReturnSpeed() {
      return this.currentYawReturnSpeed;
   }

   public float getCurrentPitchReturnSpeed() {
      return this.currentPitchReturnSpeed;
   }

   @EventHandler
   public void onEventTick(UpdateEvent var1) {
      if (aimHold == 0 && this.currentTask.equals(RotationStorage.a.AIM) && this.idleTicks > this.currentTimeout) {
         // Наводка закончилась: снимаем задачу и приоритет, но камеру не отпускаем
         // сразу — пока аура держит цель, «тихий» взгляд остаётся свободным.
         this.currentTask = RotationStorage.a.IDLE;
         this.currentPriority = 0;
         this.targetRotation = null;
      }

      // Никто больше не ведёт наводку, а камера всё ещё «чужая»: возвращаем её игроку
      // (только картинку, без поворота игрока). Это страховка от зависшей камеры.
      if (aimHold == 0 && FreeLookStorage.isSilent() && !auraAiming()) {
         releaseAim();
      }

      this.idleTicks++;
   }

   /** Аура включена и прямо сейчас ведёт цель — её «тихую» камеру трогать нельзя. */
   private static boolean auraAiming() {
      Aura var0 = Aura.INSTANCE;
      return var0 != null && var0.l() && var0.getTarget() != null;
   }

   public static void a(Rotation2 var0, float var1, float var2, float var3, float var4, int var5, int var6, boolean var7) {
      RotationStorage var8 = a();
      // В открытом GUI человек мышью прицел не крутит: ротация приходит только пакетами,
      // и для античитов (Shard/Sloth) это отдельный «BadPackets». Просто не применяем.
      if (mc.currentScreen != null) {
         return;
      }

      if (mc.player != null && var8.currentPriority <= var6) {
         var8.clientLook = var7;
         if (var7) {
            FreeLookStorage.a(false);
         } else {
            // наводка от первого лица выключена: камера отдельно, игрок (и пакеты) — отдельно
            FreeLookStorage.silent(true);
         }
         var8.currentYawSpeed = var1;
         var8.currentPitchSpeed = var2;
         var8.currentYawReturnSpeed = var3;
         var8.currentPitchReturnSpeed = var4;
         var8.currentTimeout = var5;
         var8.currentPriority = var6;
         var8.currentTask = RotationStorage.a.AIM;
         var8.targetRotation = var0;
         var8.a(var0, var1, var2);
      }
   }

   public static void a(Rotation2 var0, float var1, float var2, int var3, int var4, boolean var5) {
      a(var0, var1, var1, var2, var2, var3, var4, var5);
   }

   private boolean a(Rotation2 var1, float var2, float var3) {
      if (mc.player == null) {
         return false;
      }

      float var4 = MathHelper.wrapDegrees(var1.getYaw() - mc.player.getYaw());
      float var5 = var1.getPitch() - mc.player.getPitch();

      // Суммарный поворот за тик ограничен: RotationsSystem.a() зовут несколько модулей,
      // и без бюджета за один тик в пакет уходило 2-3 «человеческих» флика подряд.
      int age = mc.player.age;
      if (age != this.applyAge) {
         this.applyAge = age;
         this.appliedYaw = 0.0F;
         this.appliedPitch = 0.0F;
      }

      float budgetYaw = Math.max(0.0F, MAX_YAW_STEP - this.appliedYaw);
      float budgetPitch = Math.max(0.0F, MAX_PITCH_STEP - this.appliedPitch);
      float var6 = Math.min(Math.abs(var4), Math.min(var2, budgetYaw));
      float var7 = Math.min(Math.abs(var5), Math.min(var3, budgetPitch));
      float yawDelta = GCDUtil.a(MathHelper.clamp(var4, -var6, var6));
      float pitchDelta = GCDUtil.a(MathHelper.clamp(var5, -var7, var7));

      // Суб-квантовый остаток: иначе цель в 1.5 квантах от прицела «висит» навсегда
      float g = GCDUtil.a();
      if (g > 1.0E-5F) {
         if (yawDelta == 0.0F && Math.abs(var4) >= g) {
            yawDelta = Math.copySign(g, var4);
         }

         if (pitchDelta == 0.0F && Math.abs(var5) >= g) {
            pitchDelta = Math.copySign(g, var5);
         }
      }

      float newYaw = mc.player.getYaw() + yawDelta;
      float newPitch = MathHelper.clamp(mc.player.getPitch() + pitchDelta, -pitchLimit(), pitchLimit());

      // Всё, что уходит в пакет, обязано быть конечным числом (иначе цикл ниже повиснет)
      if (!Float.isFinite(newYaw) || !Float.isFinite(newPitch)) {
         return false;
      }

      // Yaw держим в [-180, 180), но сдвигаем на целое число «оборотов в квантах»,
      // чтобы значение осталось на сетке GCD (иначе проверка сенсы ловит разрыв).
      if (g > 1.0E-5F) {
         float turn = Math.round(360.0F / g) * g;
         if (turn > 1.0E-3F) {
            int guard = 0;
            while (newYaw >= 180.0F && guard++ < 8) {
               newYaw -= turn;
            }

            guard = 0;
            while (newYaw < -180.0F && guard++ < 8) {
               newYaw += turn;
            }
         }
      }

      this.appliedYaw += Math.abs(yawDelta);
      this.appliedPitch += Math.abs(pitchDelta);
      mc.player.setYaw(newYaw);
      mc.player.setPitch(newPitch);
      this.idleTicks = 0;
      float var9 = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - var1.getYaw()));
      float var10 = Math.abs(mc.player.getPitch() - var1.getPitch());
      return var9 < 1.0F && var10 < 1.0F;
   }

   /** Максимум pitch с учётом сетки GCD: обрезка на «кривом» значении ломает проверку сенсы. */
   private static float pitchLimit() {
      float g = GCDUtil.a();
      if (g < 1.0E-5F) {
         return PITCH_LIMIT;
      }

      float lim = (float)Math.floor(PITCH_LIMIT / g) * g;
      return lim > 45.0F ? lim : PITCH_LIMIT;
   }

   public void j() {
      if (aimHold > 0) {
         // Наводку держит внешний модуль (WindHop): камеру не трогаем.
         return;
      }

      this.currentTask = RotationStorage.a.IDLE;
      this.currentPriority = 0;
      this.targetRotation = null;
      this.clientLook = true;
      this.idleTicks = 0;
      // Камера возвращается к игроку плавно и без рывка: молчаливая наводка
      // больше не «зависает» после боя.
      FreeLookStorage.restore();
   }

   public boolean k() {
      return !this.currentTask.equals(RotationStorage.a.IDLE);
   }

   public enum a {
      AIM,
      IDLE;
   }
}
