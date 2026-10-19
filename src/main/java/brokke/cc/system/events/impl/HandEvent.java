package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class HandEvent extends Event {
   public static final HandEvent PRE = new HandEvent(HandEvent.Phase.PRE);
   public static final HandEvent POST = new HandEvent(HandEvent.Phase.POST);
   private final HandEvent.Phase phase;

   public HandEvent(HandEvent.Phase var1) {
      this.phase = var1;
   }

   public boolean isPre() {
      return this.phase == HandEvent.Phase.PRE;
   }

   public boolean isPost() {
      return this.phase == HandEvent.Phase.POST;
   }

   public HandEvent.Phase getPhase() {
      return this.phase;
   }

   public enum Phase {
      PRE,
      POST;
   }
}
