package brokke.cc.rotation;

import brokke.cc.core.IMinecraft;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPostSync;
import brokke.cc.system.events.impl.EventSync;
import brokke.cc.system.events.impl.TravelRotationEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class RotationTask implements IMinecraft {
   private static final String BARITONE_TASK_NAME = "baritone_global";
   public static final Map<String, RotationTask> tasks = new ConcurrentHashMap<>();
   private final String name;
   private int priority = 1;
   private static float spoofYaw;
   private static float spoofPitch;
   private static boolean spoofingThisTick;
   private static float packetYaw;
   private static boolean hasPacketYaw;
   public static float visualHeadYaw;
   public static float visualHeadPitch;
   public static float visualPrevHeadYaw;
   public static float visualPrevHeadPitch;
   private static float visualBodyYaw;
   private static float visualPrevBodyYaw;
   public static volatile float renderHeadYaw;
   public static volatile float renderHeadPitch;
   public static volatile float renderPrevHeadYaw;
   public static volatile float renderPrevHeadPitch;
   private static volatile boolean renderRotationActive;
   public static long returnStartTime = 0L;
   private static long returnDurationMs = 800L;
   public static long resetDelayMs = -1L;
   private static boolean instantResetOnReturn = false;
   public static RotationTask.a rotationState = RotationTask.a.IDLE;
   public static float auraSpinYaw = 0.0F;
   private static float auraPrevSpinYaw = 0.0F;
   private static float auraSpinBaseYaw = 0.0F;
   private static float auraSpinProgress = 0.0F;
   private static int auraSpinDir = 1;
   private static long auraSpinLastTime = 0L;
   public static boolean auraSpinActive = false;
   public static boolean enableVisualSpin = false;
   private static RotateVector desiredRotation;
   public static float targetYaw;
   public static float targetPitch;
   public static float yawSpeed = Float.MAX_VALUE;
   public static float pitchSpeed = Float.MAX_VALUE;
   private static float returnYawSpeed = 220.0F;
   private static float returnPitchSpeed = 180.0F;
   public static int rotationPriority = 0;
   private static long rotationTimeoutMs = 0L;
   public static long inactiveMs = 0L;
   private static long lastInactiveUpdateMs = 0L;
   public static boolean needSmoothReset = false;
   public static boolean smoothReturn = false;
   private static float movementTargetYaw = Float.NaN;
   private static Runnable pendingAfterAimAction;

   private RotationTask(String var1) {
      this.name = var1;
   }

   public static float getSpoofYaw() {
      return spoofYaw;
   }

   public static float getSpoofPitch() {
      return spoofPitch;
   }

   public static boolean c() {
      return rotationState != RotationTask.a.IDLE;
   }

   public static boolean d() {
      return c();
   }

   public static float e() {
      return q() && mc.player != null ? mc.player.getYaw() : renderHeadYaw;
   }

   public static float f() {
      return q() && mc.player != null ? mc.player.getPitch() : renderHeadPitch;
   }

   public static float g() {
      return q() && mc.player != null ? mc.player.lastYaw : renderPrevHeadYaw;
   }

   public static float h() {
      return q() && mc.player != null ? mc.player.lastPitch : renderPrevHeadPitch;
   }

   public static float i() {
      return q() && mc.player != null ? mc.player.bodyYaw : visualBodyYaw;
   }

   public static float j() {
      return q() && mc.player != null ? mc.player.lastBodyYaw : visualPrevBodyYaw;
   }

   public static boolean k() {
      return renderRotationActive && !q();
   }

   public static void l() {
      renderRotationActive = false;
   }

   private static boolean q() {
      return !renderRotationActive;
   }

   public static boolean a(String var0) {
      return tasks.containsKey(var0);
   }

   public static boolean m() {
      return a("baritone_global");
   }

   public static boolean n() {
      if (!c()) {
         return false;
      } else {
         return rotationState == RotationTask.a.RESET ? false : m();
      }
   }

   public static void o() {
      b("baritone_global");
   }

   public static RotationTask a(String var0, int var1) {
      RotationTask var2 = tasks.computeIfAbsent(var0, RotationTask::new);
      var2.priority = var1;
      return var2;
   }

   public static void b(String var0) {
      RotationTask var1 = tasks.remove(var0);
      if (var1 != null && tasks.isEmpty()) {
         s();
      }
   }

   public static void c(String var0) {
      tasks.remove(var0);
      if (tasks.isEmpty()) {
         s();
      }
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, double var6, int var8, long var9) {
      if (var8 >= rotationPriority || rotationState != RotationTask.a.AIM) {
         if (rotationState == RotationTask.a.IDLE && mc.player != null) {
            visualHeadYaw = mc.player.getYaw();
            visualHeadPitch = mc.player.getPitch();
            visualPrevHeadYaw = mc.player.lastYaw;
            visualPrevHeadPitch = mc.player.lastPitch;
            visualBodyYaw = mc.player.bodyYaw;
            visualPrevBodyYaw = mc.player.lastBodyYaw;
            r();
         }

         targetYaw = var0;
         targetPitch = MathHelper.clamp(var1, -90.0F, 90.0F);
         yawSpeed = var2;
         pitchSpeed = var3;
         returnYawSpeed = var4;
         returnPitchSpeed = var5;
         rotationPriority = var8;
         rotationTimeoutMs = a(var6);
         inactiveMs = 0L;
         lastInactiveUpdateMs = System.currentTimeMillis();
         returnStartTime = 0L;
         resetDelayMs = var9;
         instantResetOnReturn = false;
         needSmoothReset = false;
         smoothReturn = false;
         pendingAfterAimAction = null;
         desiredRotation = new RotateVector(targetYaw, targetPitch);
         rotationState = RotationTask.a.AIM;
         renderRotationActive = true;
      }
   }

   public static void a(float var0, float var1, float var2, float var3, float var4, float var5, double var6, int var8) {
      a(var0, var1, var2, var3, var4, var5, var6, var8, -1L);
   }

   public static void a(float var0, float var1, float var2, int var3) {
      a(var0, var1, Float.MAX_VALUE, Float.MAX_VALUE, var2, var2, 0.0, var3, -1L);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
         visualPrevHeadYaw = visualHeadYaw;
         visualPrevHeadPitch = visualHeadPitch;
         visualPrevBodyYaw = visualBodyYaw;
         if (!c()) {
            visualHeadYaw = mc.player.getYaw();
            visualHeadPitch = mc.player.getPitch();
            visualBodyYaw = mc.player.bodyYaw;
            renderPrevHeadYaw = visualHeadYaw;
            renderPrevHeadPitch = visualHeadPitch;
            renderHeadYaw = visualHeadYaw;
            renderHeadPitch = visualHeadPitch;
         } else if (rotationState == RotationTask.a.AIM) {
            visualHeadYaw = targetYaw;
            visualHeadPitch = targetPitch;
            renderHeadYaw = targetYaw;
            renderHeadPitch = targetPitch;
         } else if (rotationState == RotationTask.a.RESET) {
            s();
         }
      }
   }

   @EventHandler
   public void onSync(EventSync var1) {
      if (mc.player == null) {
         spoofingThisTick = false;
      } else {
         spoofYaw = mc.player.getYaw();
         spoofPitch = mc.player.getPitch();
         if (c()) {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(MathHelper.clamp(targetPitch, -90.0F, 90.0F));
            spoofingThisTick = true;
            if (pendingAfterAimAction != null) {
               Runnable var2 = pendingAfterAimAction;
               pendingAfterAimAction = null;
               var2.run();
            }
         }
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync var1) {
      if (mc.player != null && spoofingThisTick) {
         mc.player.setYaw(spoofYaw);
         mc.player.setPitch(spoofPitch);
         spoofingThisTick = false;
      }
   }

   @EventHandler
   public void onTravel(TravelRotationEvent var1) {
      if (c()) {
         var1.setYaw(targetYaw);
         var1.setPitch(targetPitch);
         var1.cancel();
      }
   }

   private static void r() {
      renderHeadYaw = visualHeadYaw;
      renderHeadPitch = visualHeadPitch;
      renderPrevHeadYaw = visualPrevHeadYaw;
      renderPrevHeadPitch = visualPrevHeadPitch;
   }

   private static void s() {
      rotationState = RotationTask.a.IDLE;
      rotationPriority = 0;
      rotationTimeoutMs = 0L;
      inactiveMs = 0L;
      lastInactiveUpdateMs = 0L;
      returnStartTime = 0L;
      resetDelayMs = -1L;
      instantResetOnReturn = false;
      needSmoothReset = false;
      smoothReturn = false;
      desiredRotation = null;
      movementTargetYaw = Float.NaN;
      pendingAfterAimAction = null;
      l();
   }

   public static float a(LivingEntity var0) {
      if (var0 != null && mc.player != null) {
         double var1 = var0.getX() - var0.lastX;
         double var3 = var0.getZ() - var0.lastZ;
         double var5 = Math.sqrt(var1 * var1 + var3 * var3);
         double var7 = var0.getX();
         double var9 = var0.getZ();
         if (var5 > 0.001) {
            var7 += var1 / var5 * 0.87;
            var9 += var3 / var5 * 0.87;
         }

         float var11 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var9 - mc.player.getZ(), var7 - mc.player.getX())) - 90.0);
         return MathHelper.wrapDegrees(var11);
      } else {
         return mc.player != null ? mc.player.getYaw() : 0.0F;
      }
   }

   public static void p() {
      renderPrevHeadYaw = renderHeadYaw;
      renderPrevHeadPitch = renderHeadPitch;
   }

   public static float a(float var0) {
      return var0 == -0.0F ? 0.0F : var0;
   }

   private static long a(double var0) {
      return Math.max(0L, Math.round(var0 * 50.0));
   }

   static {
      EventBus.register(new RotationTask("global"));
   }

   public enum a {
      IDLE,
      AIM,
      RESET;
   }
}
