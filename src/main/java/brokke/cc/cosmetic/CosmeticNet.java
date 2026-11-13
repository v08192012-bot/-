package brokke.cc.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Транспорт обмена косметикой через публичный релей (по умолчанию ntfy.sh).
 *
 * Почему не чат: на сервере кулдаун 120 секунд, а маячков нужно несколько за сессию.
 * Релей по HTTPS не требует ни аккаунта, ни своего хостинга: клиент подписывается
 * на один топик и публикует туда короткий JSON. Ничего, кроме набора индексов
 * косметики и ника, наружу не уходит.
 *
 * Топик — это просто имя канала: кто знает ключ, тот и читает. Ключ общий на сборку,
 * поэтому у всех друзей он совпадает, а снаружи канал не найти.
 */
public final class CosmeticNet {
   /** Сколько тиков ждём при первом подключении: свежие сообщения за это окно. */
   private static final String FIRST_WINDOW = "30m";
   private static final int MAX_BACKOFF_MS = 30000;
   /** Минимальный интервал между публикациями: защита от залпа при смене косметики. */
   private static final long MIN_PUBLISH_INTERVAL_MS = 20000L;

   private static final AtomicBoolean RUNNING = new AtomicBoolean();
   private static volatile Thread worker;
   private static volatile HttpClient http;
   private static volatile String baseUrl = "https://ntfy.sh";
   private static volatile String topic = "";
   private static volatile String lastId = "";
   private static volatile Consumer<String> debug;
   private static final java.util.concurrent.ConcurrentLinkedQueue<String> LOG_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();
   private static volatile long lastPublishAt;
   private static volatile long lastReceiveAt;
   private static volatile String lastReceiveName = "";
   private static volatile UUID selfUuid;
   private static ExecutorService publisher;

   private CosmeticNet() {
   }

   public static boolean isRunning() {
      return RUNNING.get();
   }

   /** Строка для .cos: жив ли канал и когда был последний чужой маячок. */
   public static String describe() {
      if (!RUNNING.get()) {
         return "выключен";
      }

      long var0 = lastReceiveAt;
      if (var0 == 0L) {
         return "подключён к " + baseUrl + " / " + topic + ", пока ничего не принял";
      }

      long var1 = Math.max(0L, (System.currentTimeMillis() - var0) / 1000L);
      return "подключён к " + baseUrl + " / " + topic + ", последний маячок " + lastReceiveName + " " + var1 + " с назад";
   }

   public static synchronized void start(String var0, String var1, Consumer<String> var2) {
      stop();
      baseUrl = normalizeBase(var0);
      topic = sanitizeTopic(var1);
      debug = var2;
      lastId = "";
      if (topic.isEmpty()) {
         return;
      }

      RUNNING.set(true);
      http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
      publisher = Executors.newSingleThreadExecutor(var0x -> {
         Thread var1x = new Thread(var0x, "CosmeticSync-Pub");
         var1x.setDaemon(true);
         return var1x;
      });
      worker = new Thread(CosmeticNet::subscribeLoop, "CosmeticSync-Sub");
      worker.setDaemon(true);
      worker.start();
      log("подключились к " + baseUrl + " / " + topic);
   }

   public static synchronized void stop() {
      RUNNING.set(false);
      Thread var0 = worker;
      worker = null;
      if (var0 != null) {
         var0.interrupt();
      }

      ExecutorService var1 = publisher;
      publisher = null;
      if (var1 != null) {
         var1.shutdownNow();
      }

      debug = null;
   }

   public static void setSelf(UUID var0) {
      selfUuid = var0;
   }

   /** Публикация строки вида {@code v1|ник|uuid|размер каталога|индексы}. */
   public static void publish(String var0, UUID var1, int var2, int[] var3) {
      publish(var0, var1, var2, var3, false);
   }

   public static void publish(String var0, UUID var1, int var2, int[] var3, boolean var4) {
      if (!RUNNING.get() || var0 == null || var0.isEmpty()) {
         return;
      }

      String var5 = baseUrl;
      String var6 = topic;
      ExecutorService var7 = publisher;
      HttpClient var8 = http;
      if (var5 == null || var6.isEmpty() || var7 == null || var8 == null) {
         return;
      }

      long var9 = System.currentTimeMillis();
      if (!var4 && var9 - lastPublishAt < MIN_PUBLISH_INTERVAL_MS) {
         return;
      }

      lastPublishAt = var9;
      StringBuilder var10 = new StringBuilder();
      for (int var11 = 0; var11 < var3.length; var11++) {
         if (var11 > 0) {
            var10.append(',');
         }

         var10.append(var3[var11]);
      }

      String var12 = "v1|" + var0 + "|" + (var1 == null ? "-" : var1.toString()) + "|" + var2 + "|" + var10;
      String var13 = var5 + "/" + var6;
      String var14 = var12;

      try {
         var7.execute(() -> {
            try {
               HttpRequest var4x = HttpRequest.newBuilder(URI.create(var13))
                  .header("Content-Type", "text/plain; charset=utf-8")
                  .timeout(Duration.ofSeconds(15))
                  .POST(HttpRequest.BodyPublishers.ofString(var14, StandardCharsets.UTF_8))
                  .build();
               HttpResponse<String> var5x = var8.send(var4x, HttpResponse.BodyHandlers.ofString());
               if (var5x.statusCode() >= 300) {
                  log("публикация не прошла: HTTP " + var5x.statusCode());
               }
            } catch (Exception var6x) {
               log("публикация не прошла: " + var6x.getClass().getSimpleName());
            }
         });
      } catch (Exception var15) {
      }
   }

   private static void subscribeLoop() {
      int var0 = 1000;

      while (RUNNING.get()) {
         String var1 = lastId;
         String var2 = baseUrl;
         String var3 = topic;
         HttpClient var4 = http;
         if (var2 == null || var3 == null || var4 == null) {
            return;
         }

         String var5 = var2
            + "/"
            + var3
            + "/json?since="
            + (var1 == null || var1.isEmpty() ? FIRST_WINDOW : var1)
            + "&poll=0";
         boolean var6 = false;

         try {
            HttpRequest var7 = HttpRequest.newBuilder(URI.create(var5)).header("Accept", "application/x-ndjson").GET().build();
            HttpResponse<Stream<String>> var8 = var4.send(var7, HttpResponse.BodyHandlers.ofLines());
            if (var8.statusCode() != 200) {
               log("подписка не прошла: HTTP " + var8.statusCode());
               var6 = true;
               if (var8.statusCode() == 404 || var8.statusCode() == 400 || var8.statusCode() == 416) {
                  // Идентификатор сообщения уже выпал из кеша релея: начинаем окно заново,
                  // иначе подписка зациклится на несуществующем since.
                  lastId = "";
               }
            } else {
               var0 = 1000;
               try (Stream<String> var9 = var8.body()) {
                  var9.forEach(CosmeticNet::acceptLine);
               }
            }
         } catch (InterruptedException var10) {
            Thread.currentThread().interrupt();
            return;
         } catch (Exception var11) {
            if (RUNNING.get()) {
               log("подписка оборвалась: " + var11.getClass().getSimpleName());
            }
         }

         if (!RUNNING.get()) {
            return;
         }

         try {
            Thread.sleep(var6 ? 15000L : var0);
         } catch (InterruptedException var12) {
            Thread.currentThread().interrupt();
            return;
         }

         var0 = Math.min(MAX_BACKOFF_MS, var0 * 2);
      }
   }

   private static void acceptLine(String var0) {
      if (var0 == null || var0.isEmpty() || var0.charAt(0) != '{') {
         return;
      }

      try {
         JsonObject var1 = JsonParser.parseString(var0).getAsJsonObject();
         if (var1.has("id")) {
            String var2 = var1.get("id").getAsString();
            if (var2 != null && !var2.isEmpty()) {
               lastId = var2;
            }
         }

         if (!var1.has("event") || !"message".equals(var1.get("event").getAsString())) {
            return;
         }

         if (!var1.has("message")) {
            return;
         }

         parsePayload(var1.get("message").getAsString());
      } catch (Exception var3) {
      }
   }

   private static void parsePayload(String var0) {
      try {
         if (var0 == null || !var0.startsWith("v1|")) {
            return;
         }

         String[] var1 = var0.split("\\|", 5);
         if (var1.length < 5) {
            return;
         }

         String var2 = var1[1];
         if (var2.isEmpty() || var2.length() > 32) {
            return;
         }

         UUID var3 = null;
         if (!"-".equals(var1[2])) {
            try {
               var3 = UUID.fromString(var1[2]);
            } catch (Exception var4) {
            }
         }

         if (var3 != null && selfUuid != null && selfUuid.equals(var3)) {
            return;
         }

         int var8;
         try {
            var8 = Integer.parseInt(var1[3]);
         } catch (NumberFormatException var5) {
            return;
         }

         int var9 = Math.min(Math.max(var8, 0), LocalCosmetics.a());
         int[] var10 = new int[0];
         if (!var1[4].isEmpty()) {
            String[] var11 = var1[4].split(",");
            int[] var12 = new int[var11.length];
            int var13 = 0;

            for (String var14 : var11) {
               try {
                  int var15 = Integer.parseInt(var14);
                  if (var15 >= 0 && var15 < var9) {
                     var12[var13++] = var15;
                  }
               } catch (NumberFormatException var16) {
               }
            }

            if (var13 > 0) {
               var10 = new int[var13];
               System.arraycopy(var12, 0, var10, 0, var13);
            }
         }

         RemoteCosmetics.apply(var3, var2, var10);
         lastReceiveAt = System.currentTimeMillis();
         lastReceiveName = var2;
         log("получили косметику: " + var2 + " → " + var10.length);
      } catch (Exception var17) {
      }
   }

   private static String normalizeBase(String var0) {
      String var1 = var0 == null ? "" : var0.trim();
      if (var1.isEmpty()) {
         return "https://ntfy.sh";
      }

      while (var1.endsWith("/")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      if (!var1.startsWith("http://") && !var1.startsWith("https://")) {
         var1 = "https://" + var1;
      }

      return var1;
   }

   private static String sanitizeTopic(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1.length() && var2.length() < 64; var3++) {
         char var4 = var1.charAt(var3);
         if (var4 >= 'a' && var4 <= 'z' || var4 >= '0' && var4 <= '9' || var4 == '-' || var4 == '_') {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   private static String escape(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '"' || var3 == '\\') {
            var1.append('\\').append(var3);
         } else if (var3 >= ' ') {
            var1.append(var3);
         }
      }

      return var1.toString();
   }

   /** Отладочные строки копим здесь: сетевой поток не имеет права трогать чат. */
   public static String pollLog() {
      return LOG_QUEUE.poll();
   }

   private static void log(String var0) {
      if (debug != null) {
         LOG_QUEUE.add(var0);
      }
   }
}
