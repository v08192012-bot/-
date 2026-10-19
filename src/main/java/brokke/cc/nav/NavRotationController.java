package brokke.cc.nav;

import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.util.GCDUtil2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NavRotationController {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private float targetYaw;
   private float targetPitch;
   private float currentYaw;
   private float currentPitch;
   private float velYaw;
   private float velPitch;
   private boolean active;
   private boolean initialized;
   private int rotTicks;

   public void a(Vec3d var1) {
      if (this.mc.player != null && var1 != null) {
         Vec3d var2 = this.mc.player.getEyePos();
         double var3 = var1.x - var2.x;
         double var5 = var1.y - var2.y;
         double var7 = var1.z - var2.z;
         double var9 = Math.hypot(var3, var7);
         float var11 = var9 > 0.15 ? (float)Math.toDegrees(Math.atan2(var7, var3)) - 90.0F : (this.initialized ? this.currentYaw : this.targetYaw);
         float var12 = (float)(-Math.toDegrees(Math.atan2(var5, Math.max(var9, 0.001))));
         this.a(var11, var12);
      }
   }

   public void a(float var1, float var2) {
      this.targetYaw = MathHelper.wrapDegrees(var1);
      this.targetPitch = MathHelper.clamp(var2, -89.0F, 89.0F);
      this.active = true;
   }

   public void a() {
      if (this.active && this.mc.player != null) {
         this.rotTicks++;
         if (!this.initialized) {
            this.currentYaw = this.mc.player.getYaw();
            this.currentPitch = this.mc.player.getPitch();
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.initialized = true;
         }

         float var1 = MathHelper.wrapDegrees(this.targetYaw - this.currentYaw);
         float var2 = this.targetPitch - this.currentPitch;
         float var3 = (float)Math.hypot(var1, var2);
         if (var3 < 0.1F) {
            this.currentYaw = this.targetYaw;
            this.currentPitch = this.targetPitch;
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.mc.player.setYaw(this.currentYaw);
            this.mc.player.setPitch(this.currentPitch);
         } else {
            float var4 = var1 / var3;
            float var5 = var2 / var3;
            float var6;
            if (var3 > 80.0F) {
               var6 = Math.min(var3 * 0.9F, 105.0F);
            } else if (var3 > 35.0F) {
               var6 = Math.min(var3 * 0.8F, 75.0F);
            } else if (var3 > 12.0F) {
               var6 = var3 * 0.65F;
            } else {
               var6 = Math.max(2.5F, var3 * 0.5F);
            }

            float var7 = var4 * Math.min(var6, Math.abs(var1));
            float var8 = var5 * Math.min(var6, Math.abs(var2));
            float var9 = GCDUtil2.a(var7);
            float var10 = GCDUtil2.a(var8);
            if (Math.abs(var9) > Math.abs(var1)) {
               var9 = GCDUtil2.a(var1);
            }

            if (Math.abs(var10) > Math.abs(var2)) {
               var10 = GCDUtil2.a(var2);
            }

            this.currentYaw += var9;
            this.currentPitch = MathHelper.clamp(this.currentPitch + var10, -89.0F, 89.0F);
            this.mc.player.setYaw(this.currentYaw);
            this.mc.player.setPitch(this.currentPitch);
            RotationStorage.a(new Rotation2(this.currentYaw, this.currentPitch), 360.0F, 360.0F, 360.0F, 360.0F, 2, 1, true);
         }
      }
   }

   public void a(float var1) {
      this.a();
   }

   public void b() {
      this.active = false;
      this.initialized = false;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.rotTicks = 0;
      if (RotationStorage.instance != null && RotationStorage.instance.k()) {
         RotationStorage.a().j();
      }
   }

   public boolean isActive() {
      return this.active;
   }

   public float getTargetYaw() {
      return this.targetYaw;
   }

   public float getTargetPitch() {
      return this.targetPitch;
   }

   public float f() {
      return this.initialized ? this.currentYaw : (this.mc.player != null ? this.mc.player.getYaw() : this.targetYaw);
   }

   public float g() {
      return this.initialized ? this.currentPitch : (this.mc.player != null ? this.mc.player.getPitch() : this.targetPitch);
   }
}
