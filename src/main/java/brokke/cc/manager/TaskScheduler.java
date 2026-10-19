package brokke.cc.manager;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.EventMoveInput;
import java.util.LinkedList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;

public final class TaskScheduler {
   static final MinecraftClient mc = MinecraftClient.getInstance();
   static final LinkedList<TaskScheduler.a> taskQueue = new LinkedList<>();
   private static boolean registered = false;

   public static void a() {
      if (!registered) {
         EventBus.register(new TaskScheduler.b());
         registered = true;
      }
   }

   public static void a(Class<?> var0, Runnable var1) {
      a(var0, var1, 0);
   }

   public static void a(Class<?> var0, Runnable var1, int var2) {
      a();
      TaskScheduler.a var3 = new TaskScheduler.a(var0, var1, var2);
      synchronized (taskQueue) {
         if (var2 > d()) {
            if (taskQueue.isEmpty()) {
               taskQueue.addFirst(var3);
            } else {
               taskQueue.add(1, var3);
            }
         } else {
            taskQueue.add(var3);
         }
      }
   }

   public static boolean a(Class<?> var0) {
      synchronized (taskQueue) {
         for (TaskScheduler.a var3 : taskQueue) {
            if (var3.provider == var0) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean b(Class<?> var0) {
      return a(var0);
   }

   public static void b() {
      synchronized (taskQueue) {
         taskQueue.clear();
      }
   }

   private static int d() {
      int var0 = Integer.MIN_VALUE;

      for (TaskScheduler.a var2 : taskQueue) {
         if (var2.priority > var0) {
            var0 = var2.priority;
         }
      }

      return var0 == Integer.MIN_VALUE ? 0 : var0;
   }

   public static void c() {
      if (mc.player != null) {
         if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            if (mc.player.networkHandler != null) {
               mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            }
         } else {
            mc.player.closeHandledScreen();
         }
      }
   }

   public static void a(int var0, int var1, SlotActionType var2) {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, var1, var2, mc.player);
      }
   }

   public static void a(Slot var0) {
      if (var0 != null && var0.id != -1 && mc.player != null) {
         a(var0.id, 40, SlotActionType.SWAP);
      }
   }

   public static void a(int var0) {
      if (var0 >= 0 && mc.player != null) {
         a(var0, 40, SlotActionType.SWAP);
      }
   }

   static {
      a();
   }

   static class a {
      final Class<?> provider;
      final Runnable task;
      final int priority;

      a(Class<?> var1, Runnable var2, int var3) {
         this.provider = var1;
         this.task = var2;
         this.priority = var3;
      }
   }

   public static class b {
      @EventHandler(priority = EventPriority.HIGHEST)
      public void onMoveInput(EventMoveInput var1) {
         if (TaskScheduler.mc.player != null) {
            TaskScheduler.a var2;
            synchronized (TaskScheduler.taskQueue) {
               var2 = TaskScheduler.taskQueue.peek();
            }

            if (var2 != null) {
               var1.setForward(0.0F);
               var1.setStrafe(0.0F);
               var1.setJump(false);
               var1.setSprint(false);
               var1.setSneak(false);
               PlayerInput var9 = TaskScheduler.mc.player.getLastPlayerInput();
               if (var9 == null
                  || !var9.jump() && !TaskScheduler.mc.player.isSprinting() && !var9.forward() && !var9.backward() && !var9.left() && !var9.right()) {
                  try {
                     var2.task.run();
                     TaskScheduler.c();
                  } catch (Exception var7) {
                     var7.printStackTrace();
                  }

                  synchronized (TaskScheduler.taskQueue) {
                     TaskScheduler.taskQueue.poll();
                  }
               }
            }
         }
      }
   }
}
