package brokke.cc.rotation;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.module.combat.Aura;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationStorage {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   /** Максимальный поворот за один тик (пакет): больше — античиты видят телепорт прицела. */
   private static final float MAX_YAW_STEP = 55.0F;
   private static final float MAX_PITCH_STEP = 40.0F;
   private static final float PITCH_LIMIT = 89.5F;
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

   private void l() {
      Rotation2 var1 = new Rotation2(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch());
      Aura var2 = Aura.INSTANCE;
      if (var2 != null && var2.l() && var2.rotationType.isSelected("AimAssist")) {
         var2.getAimAssistRotation().b(var1.getYaw(), var1.getPitch());
         if (var2.getAimAssistRotation().c(var1.getYaw(), var1.getPitch())) {
            this.j();
         }
      } else {
         ThreadLocalRandom var3 = ThreadLocalRandom.current();
         float var4 = this.currentYawReturnSpeed * (0.6F + var3.nextFloat() * 0.7F);
         float var5 = this.currentPitchReturnSpeed * (0.55F + var3.nextFloat() * 0.75F);
         if (this.a(var1, var4, var5)) {
            this.j();
         }
      }
   }

   @EventHandler
   public void onEventTick(UpdateEvent var1) {
      if (this.currentTask.equals(RotationStorage.a.AIM) && this.idleTicks > this.currentTimeout) {
         this.currentTask = RotationStorage.a.RESET;
      }

      if (this.currentTask.equals(RotationStorage.a.RESET)) {
         this.l();
      }

      this.idleTicks++;
   }

   public static void a(Rotation2 var0, float var1, float var2, float var3, float var4, int var5, int var6, boolean var7) {
      RotationStorage var8 = a();
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
      float var6 = Math.min(Math.abs(var4), Math.min(var2, MAX_YAW_STEP));
      float var7 = Math.min(Math.abs(var5), Math.min(var3, MAX_PITCH_STEP));
      float var8 = mc.player.getYaw();
      var8 += GCDUtil.a(MathHelper.clamp(var4, -var6, var6));
      mc.player.setYaw(var8);
      mc.player.setPitch(
         MathHelper.clamp(mc.player.getPitch() + GCDUtil.a(MathHelper.clamp(var5, -var7, var7)), -PITCH_LIMIT, PITCH_LIMIT)
      );
      this.idleTicks = 0;
      float var9 = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - var1.getYaw()));
      float var10 = Math.abs(mc.player.getPitch() - var1.getPitch());
      return var9 < 1.0F && var10 < 1.0F;
   }

   public void j() {
      boolean var0 = FreeLookStorage.isSilent();
      if (FreeLookStorage.isActive() && mc.player != null) {
         float var1 = Math.abs(MathHelper.wrapDegrees(FreeLookStorage.getFreeYaw() - mc.player.getYaw()));
         float var2 = Math.abs(FreeLookStorage.getFreePitch() - mc.player.getPitch());
         boolean var3 = var0 && this.currentYawReturnSpeed > 0.0F && this.currentPitchReturnSpeed > 0.0F;
         if (var3 && (var1 > 8.0F || var2 > 6.0F)) {
            // Не телепортируем прицел назад (сервер видит мгновенный разворот — badpackets):
            // доворачиваем плавно, как обычный возврат взгляда.
            this.currentTask = RotationStorage.a.RESET;
            this.currentPriority = 0;
            this.targetRotation = new Rotation2(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch());
            this.idleTicks = 0;
            return;
         }

         mc.player.setYaw(FreeLookStorage.getFreeYaw());
         mc.player.setPitch(MathHelper.clamp(FreeLookStorage.getFreePitch(), -PITCH_LIMIT, PITCH_LIMIT));
      }

      this.currentTask = RotationStorage.a.IDLE;
      this.currentPriority = 0;
      this.targetRotation = null;
      this.clientLook = true;
      FreeLookStorage.a(false);
   }

   public boolean k() {
      return !this.currentTask.equals(RotationStorage.a.IDLE);
   }

   public enum a {
      AIM,
      RESET,
      IDLE;
   }
}
