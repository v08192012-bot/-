package brokke.cc.core;

import brokke.cc.manager.SwapScriptManager;

public final class SwapLocks {
   private SwapLocks() {
   }

   public static boolean a() {
      return !SwapScriptManager.b();
   }
}
