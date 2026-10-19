package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.client.render.RenderTickCounter;

public class Render2DEvent extends Event {
   public static final Render2DEvent INSTANCE = new Render2DEvent(null);
   private RenderTickCounter tickCounter;

   public Render2DEvent(RenderTickCounter var1) {
      this.tickCounter = var1;
   }

   public static Render2DEvent get(RenderTickCounter var0) {
      INSTANCE.setCancelled(false);
      INSTANCE.tickCounter = var0;
      return INSTANCE;
   }

   public RenderTickCounter getTickCounter() {
      return this.tickCounter;
   }

   public void setTickCounter(RenderTickCounter var1) {
      this.tickCounter = var1;
   }
}
