package brokke.cc.core;

import brokke.cc.manager.TaskScheduler;

public final class InventoryTasks {
   private InventoryTasks() {
   }

   public static void a(Class<?> var0, Runnable var1) {
      TaskScheduler.a(var0, var1, 0);
   }

   public static void a(Class<?> var0, Runnable var1, int var2) {
      TaskScheduler.a(var0, var1, var2);
   }

   public static boolean a(Class<?> var0) {
      return TaskScheduler.a(var0);
   }

   public static boolean b(Class<?> var0) {
      return TaskScheduler.a(var0);
   }
}
