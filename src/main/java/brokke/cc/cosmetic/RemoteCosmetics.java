package brokke.cc.cosmetic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Косметика, которую прислали другие юзеры чита.
 *
 * Храним по UUID, а имя — как запасной ключ: на серверах с плагинами ников
 * настоящее имя игрока и подпись в чате могут не совпадать, и тогда рендер
 * находит запись по нику из game profile.
 */
public final class RemoteCosmetics {
   private static final long EXPIRE_MS = 20L * 60L * 1000L;
   private static final Map<UUID, RemoteCosmetics.Entry> BY_UUID = new ConcurrentHashMap<>();
   private static final Map<String, RemoteCosmetics.Entry> BY_NAME = new ConcurrentHashMap<>();

   private RemoteCosmetics() {
   }

   public static void apply(UUID var0, String var1, int[] var2) {
      RemoteCosmetics.Entry var3 = new RemoteCosmetics.Entry(var0, var1, var2, System.currentTimeMillis());
      BY_NAME.put(var1.toLowerCase(), var3);
      if (var0 != null) {
         BY_UUID.put(var0, var3);
      }
   }

   /** Индексы косметики игрока или null, если он ничего не присылал. */
   public static int[] idsFor(UUID var0, String var1) {
      RemoteCosmetics.Entry var2 = var0 == null ? null : BY_UUID.get(var0);
      if (var2 == null && var1 != null) {
         var2 = BY_NAME.get(var1.toLowerCase());
      }

      if (var2 == null) {
         return null;
      }

      if (System.currentTimeMillis() - var2.receivedAt > EXPIRE_MS) {
         return null;
      }

      return var2.ids;
   }

   public static RemoteCosmetics.Entry byName(String var0) {
      return var0 == null ? null : BY_NAME.get(var0.toLowerCase());
   }

   public static void clear() {
      BY_UUID.clear();
      BY_NAME.clear();
   }

   public static final class Entry {
      private final UUID uuid;
      private final String name;
      private final int[] ids;
      private final long receivedAt;

      private Entry(UUID var1, String var2, int[] var3, long var4) {
         this.uuid = var1;
         this.name = var2;
         this.ids = var3;
         this.receivedAt = var4;
      }

      public UUID uuid() {
         return this.uuid;
      }

      public String name() {
         return this.name;
      }

      public int[] ids() {
         return this.ids;
      }

      public long receivedAt() {
         return this.receivedAt;
      }
   }
}
