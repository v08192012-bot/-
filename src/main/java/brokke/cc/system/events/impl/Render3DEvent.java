package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent extends Event {
   private final MatrixStack matrices;
   private final float tickDelta;
   private final Camera camera;

   public Render3DEvent(MatrixStack var1, float var2, Camera var3) {
      this.matrices = var1;
      this.tickDelta = var2;
      this.camera = var3;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public Camera getCamera() {
      return this.camera;
   }
}
