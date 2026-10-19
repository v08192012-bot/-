package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class EventScroll extends Event {
   private final double horizontal;
   private final double vertical;

   public double getHorizontal() {
      return this.horizontal;
   }

   public double getVertical() {
      return this.vertical;
   }

   public EventScroll(double var1, double var3) {
      this.horizontal = var1;
      this.vertical = var3;
   }
}
