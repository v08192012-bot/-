package brokke.cc.core;

import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventRotation;
import brokke.cc.system.events.impl.Render2DEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class SilentView {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final SilentView INSTANCE = new SilentView();
   private boolean enabled;
   private boolean active;
   private float viewYaw;
   private float viewPitch;
   private long lastSteerNs;

   public static SilentView getInstance() {
      return INSTANCE;
   }

   public void a(boolean var1) {
      this.enabled = var1;
      if (!var1 && this.active) {
         this.d();
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean c() {
      return this.active && this.enabled;
   }

   public void d() {
      if (this.active && mc.player != null) {
         mc.player.setYaw(this.viewYaw);
         mc.player.setPitch(this.viewPitch);
      }

      this.active = false;
      this.lastSteerNs = 0L;
   }

   public float e() {
      if (this.active && this.enabled) {
         return this.viewYaw;
      } else {
         return mc.player == null ? 0.0F : mc.player.getYaw();
      }
   }

   public float f() {
      if (this.active && this.enabled) {
         return this.viewPitch;
      } else {
         return mc.player == null ? 0.0F : mc.player.getPitch();
      }
   }

   public boolean a(double var1, double var3) {
      if (this.active && this.enabled && mc.player != null) {
         this.viewYaw += (float)var1;
         this.viewPitch = MathHelper.clamp(this.viewPitch + (float)var3, -90.0F, 90.0F);
         return true;
      } else {
         return false;
      }
   }

   public void g() {
      if (mc.player != null && this.enabled) {
         if (!this.active) {
            this.viewYaw = mc.player.getYaw();
            this.viewPitch = mc.player.getPitch();
            this.active = true;
         }

         this.lastSteerNs = System.nanoTime();
      }
   }

   public void a(float var1, float var2) {
      if (mc.player != null) {
         if (!this.enabled) {
            if (this.active) {
               this.d();
            }

            mc.player.setYaw(var1);
            mc.player.setPitch(var2);
         } else {
            this.g();
            mc.player.setYaw(var1);
            mc.player.setPitch(var2);
         }
      }
   }

   @EventHandler
   public void onRotation(EventRotation var1) {
      if (this.active && this.enabled) {
         if (mc.player != null && mc.getCameraEntity() == mc.player) {
            var1.setYaw(this.viewYaw);
            var1.setPitch(this.viewPitch);
         }
      } else if (mc.player != null) {
         this.viewYaw = var1.getYaw();
         this.viewPitch = var1.getPitch();
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      if (this.active) {
         if (mc.player != null && this.enabled) {
            long var2 = System.nanoTime();
            if (var2 - this.lastSteerNs > 150000000L) {
               this.d();
            }
         } else {
            this.d();
         }
      }
   }
}
