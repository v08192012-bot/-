package brokke.cc.manager;

import brokke.cc.core.InvBypass;
import brokke.cc.core.MovementStop;
import brokke.cc.util.InventoryUtil2;
import brokke.cc.util.SwapUtil;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BooleanSupplier;

public final class SwapScriptManager {
   private static final LinkedList<SwapScriptManager.a> a = new LinkedList<>();

   private SwapScriptManager() {
   }

   public static void a() {
      SwapScriptManager.a var0 = a.peek();
      if (var0 != null && var0.a()) {
         a.poll();
      }
   }

   public static SwapScriptManager.a a(Class<?> var0) {
      for (SwapScriptManager.a var2 : a) {
         if (var2.a == var0) {
            return var2;
         }
      }

      return new SwapScriptManager.a(var0);
   }

   public static void a(SwapScriptManager.a var0, int var1) {
      if (var0 != null && !a.contains(var0)) {
         var0.c = var1;
         if (var1 > c()) {
            if (a.isEmpty()) {
               a.addFirst(var0);
            } else {
               a.add(1, var0);
            }
         } else {
            a.add(var0);
         }
      }
   }

   public static boolean b() {
      return a.isEmpty();
   }

   public static boolean b(Class<?> var0) {
      for (SwapScriptManager.a var2 : a) {
         if (var2.a == var0 && !var2.b()) {
            return false;
         }
      }

      return true;
   }

   private static int c() {
      int var0 = Integer.MIN_VALUE;

      for (SwapScriptManager.a var2 : a) {
         var0 = Math.max(var0, var2.c);
      }

      return var0 == Integer.MIN_VALUE ? 0 : var0;
   }

   public static final class a {
      final Class<?> a;
      private final Queue<BooleanSupplier> b = new LinkedList<>();
      int c;

      public a(Class<?> var1) {
         this.a = var1;
      }

      public SwapScriptManager.a a(BooleanSupplier var1) {
         this.b.add(var1);
         return this;
      }

      public boolean a() {
         BooleanSupplier var1 = this.b.peek();
         if (var1 == null) {
            return true;
         }

         if (var1.getAsBoolean()) {
            this.b.poll();
         }

         return this.b.isEmpty();
      }

      public boolean b() {
         return this.b.isEmpty();
      }
   }

   public static final class b implements BooleanSupplier {
      private final Runnable a;
      private final boolean b;
      private final int c;
      private int d;

      public b(Runnable var1, boolean var2, int var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
      }

      @Override
      public boolean getAsBoolean() {
         if (this.b) {
            MovementStop.a();
            if (this.d++ < this.c) {
               return false;
            }
         }

         this.a.run();
         InventoryUtil2.a();
         InvBypass.b(SwapUtil.c());
         return true;
      }
   }
}
