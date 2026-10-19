package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.client.gui.DrawContext;

public class HudRenderEvent extends Event {
   private final DrawContext context;

   public HudRenderEvent(DrawContext var1) {
      this.context = var1;
   }

   public DrawContext getContext() {
      return this.context;
   }
}
