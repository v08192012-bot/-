package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;

public class KeyEvent extends Event {
   int action;
   int key;

   public int getAction() {
      return this.action;
   }

   public int getKey() {
      return this.key;
   }

   public KeyEvent(int var1, int var2) {
      this.action = var1;
      this.key = var2;
   }
}
