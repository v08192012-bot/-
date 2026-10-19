package cmode.system.events;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
   private static final Map<Class<? extends Event>, List<EventBus.EventListener>> listeners = new ConcurrentHashMap<>();
   private static final Map<Class<? extends Event>, EventBus.EventListener[]> listenerCache = new ConcurrentHashMap<>();
   private static final Set<Object> registeredObjects = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Lookup LOOKUP = MethodHandles.lookup();

   public static void register(Object var0) {
      if (!registeredObjects.contains(var0)) {
         registeredObjects.add(var0);

         for (Method var4 : var0.getClass().getDeclaredMethods()) {
            if (var4.isAnnotationPresent(EventHandler.class) && var4.getParameterCount() == 1) {
               Class var5 = var4.getParameterTypes()[0];
               if (Event.class.isAssignableFrom(var5)) {
                  Class var6 = var5;
                  EventHandler var7 = var4.getAnnotation(EventHandler.class);
                  EventPriority var8 = var7.priority();
                  var4.setAccessible(true);

                  try {
                     MethodHandle var9 = LOOKUP.unreflect(var4).bindTo(var0);
                     listeners.computeIfAbsent(var6, var0x -> new CopyOnWriteArrayList<>()).add(new EventBus.EventListener(var0, var9, var8));
                     listeners.get(var6).sort(Comparator.comparingInt(var0x -> var0x.priority.getLevel()));
                     rebuildCache(var6);
                  } catch (IllegalAccessException var10) {
                     var10.printStackTrace();
                  }
               }
            }
         }
      }
   }

   public static void unregister(Object var0) {
      if (registeredObjects.contains(var0)) {
         registeredObjects.remove(var0);

         for (Entry<Class<? extends Event>, List<EventBus.EventListener>> var2 : listeners.entrySet()) {
            var2.getValue().removeIf(var1 -> var1.owner == var0);
            rebuildCache(var2.getKey());
         }
      }
   }

   private static void rebuildCache(Class<? extends Event> var0) {
      List<EventBus.EventListener> var1 = listeners.get(var0);
      if (var1 != null && !var1.isEmpty()) {
         listenerCache.put(var0, var1.toArray(new EventBus.EventListener[0]));
      } else {
         listenerCache.remove(var0);
      }
   }

   public static <T extends Event> T post(T var0) {
      EventBus.EventListener[] var1 = listenerCache.get(var0.getClass());
      if (var1 != null && var1.length != 0) {
         for (int var2 = 0; var2 < var1.length; var2++) {
            EventBus.EventListener var3 = var1[var2];
            if (!var0.isCancelled() || var3.priority.getLevel() >= EventPriority.HIGH.getLevel()) {
               try {
                  var3.handle.invoke((Event)var0);
               } catch (Throwable var5) {
                  var5.printStackTrace();
               }
            }
         }

         return var0;
      } else {
         return var0;
      }
   }

   public static boolean isRegistered(Object var0) {
      return registeredObjects.contains(var0);
   }

   public static void clear() {
      listeners.clear();
      listenerCache.clear();
      registeredObjects.clear();
   }

   public static int getListenerCount(Class<? extends Event> var0) {
      EventBus.EventListener[] var1 = listenerCache.get(var0);
      return var1 == null ? 0 : var1.length;
   }

   static class EventListener {
      final Object owner;
      final MethodHandle handle;
      final EventPriority priority;

      EventListener(Object var1, MethodHandle var2, EventPriority var3) {
         this.owner = var1;
         this.handle = var2;
         this.priority = var3;
      }
   }
}
