package brokke.cc.module.render;

import brokke.cc.cosmetic.CosmeticNet;
import brokke.cc.cosmetic.LocalCosmetics;
import brokke.cc.cosmetic.RemoteCosmetics;
import brokke.cc.manager.CommandManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.TextSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

/**
 * Обмен косметикой между юзерами чита.
 *
 * Основной транспорт — HTTPS-релей (ntfy.sh по умолчанию): подписка на общий канал,
 * публикация короткого JSON с индексами косметики. Ни аккаунта, ни своего хостинга
 * не нужно, и в чат сервера не уходит ничего — важно, потому что там кулдаун 120 с.
 *
 * Чат остался фолбэком (по умолчанию выключен): на серверах без кулдауна или когда
 * релей недоступен.
 */
@NewFunction(name = "CosmeticSync", description = "Показывает косметику других юзеров чита", category = Category.RENDER)
public class CosmeticSync extends Function {
   private static final String PREFIX = "[bk1]";
   private static final int PROTOCOL = 1;
   private static final int JOIN_DELAY_TICKS = 40;
   private static final long REPLY_THROTTLE_MS = 15000L;
   private static final String DEFAULT_SERVER = "https://ntfy.sh";
   private static final String DEFAULT_CHANNEL = "shelka-cos-beta-2026";

   private static CosmeticSync INSTANCE;

   private final BindSetting sendBind = new BindSetting("Отправить косметику", -1);
   private final TextSetting server = new TextSetting("Сервер синка", DEFAULT_SERVER);
   private final TextSetting channel = new TextSetting("Ключ канала", DEFAULT_CHANNEL);
   private final FloatSetting heartbeat = new FloatSetting("Повторять, сек", 240.0F, 0.0F, 900.0F, 30.0F);
   private final BooleanSetting showOthers = new BooleanSetting("Косметика других юзеров", true);
   private final BooleanSetting viaChat = new BooleanSetting("Фолбэк через чат (кулдаун сервера)", false);
   private final BooleanSetting hideBeacons = new BooleanSetting("Скрывать служебные сообщения", true);
   private final BooleanSetting debug = new BooleanSetting("Отладка", false);

   private final Map<String, Long> lastReplyAt = new ConcurrentHashMap<>();
   private final Map<String, Long> replyDueAt = new ConcurrentHashMap<>();

   private int joinTicks = -1;
   private Object lastWorld;
   private String lastSignature = "";
   private String lastServer = "";
   private String lastChannel = "";
   private long lastPublishAt;
   /** Ждём задержку входа, чтобы отправить чат-маячок ровно один раз. */
   private boolean chatBeaconPending;

   public CosmeticSync() {
      this.addSettings(
         this.sendBind,
         this.server,
         this.channel,
         this.heartbeat,
         this.showOthers,
         this.viaChat,
         this.hideBeacons,
         this.debug
      );
   }

   public static CosmeticSync k() {
      if (INSTANCE == null) {
         INSTANCE = FunctionManager.a(CosmeticSync.class);
      }

      return INSTANCE;
   }

   /** Косметику другого игрока показываем, только пока модуль включён. */
   public static boolean showRemote() {
      CosmeticSync var0 = k();
      return var0 != null && var0.isActive() && var0.showOthers.isToggled();
   }

   @Override
   public void onEnable() {
      this.joinTicks = JOIN_DELAY_TICKS;
      this.lastPublishAt = 0L;
      this.lastReplyAt.clear();
      this.replyDueAt.clear();
      CosmeticNet.setSelf(mc.player == null ? null : mc.player.getUuid());
      this.restartNet();
      this.publishNow();
      this.chatBeaconPending = true;
      this.lastWorld = mc.world;
      this.lastSignature = signature();
   }

   @Override
   public void onDisable() {
      CosmeticNet.stop();
      RemoteCosmetics.clear();
      this.lastReplyAt.clear();
      this.replyDueAt.clear();
      this.joinTicks = -1;
      this.lastWorld = null;
      super.onDisable();
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (var1.getAction() == 1 && mc.currentScreen == null && this.sendBind.matchesKey(var1.getKey())) {
         this.publishNow();
         if (this.viaChat.isToggled()) {
            this.broadcastChat("вручную");
         }
      }
   }

   private void restartNet() {
      this.lastServer = this.server.getValue();
      this.lastChannel = this.channel.getValue();
      java.util.function.Consumer<String> var1 = this.debug.isToggled() ? ignored -> { } : null;
      CosmeticNet.start(this.lastServer, this.lastChannel, var1);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player == null || mc.world == null) {
         this.lastWorld = null;
         return;
      }

      CosmeticNet.setSelf(mc.player.getUuid());
      if (!this.lastServer.equals(this.server.getValue()) || !this.lastChannel.equals(this.channel.getValue())) {
         this.restartNet();
      }

      if (this.debug.isToggled()) {
         // Сетевые события копятся в очереди; в чат пишем только из тика.
         String var20;
         while ((var20 = CosmeticNet.pollLog()) != null) {
            CommandManager.d("§7[CosmeticSync] §f" + var20);
         }
      }

      if (this.lastWorld != mc.world) {
         // Сменили мир/сервер: чужие записи невалидны, а новый сервер нас ещё не видел.
         this.lastWorld = mc.world;
         RemoteCosmetics.clear();
         this.replyDueAt.clear();
         this.lastReplyAt.clear();
         this.lastSignature = signature();
         this.joinTicks = JOIN_DELAY_TICKS;
         this.publishNow();
         this.chatBeaconPending = true;
         return;
      }

      if (this.joinTicks > 0) {
         this.joinTicks--;
         if (this.joinTicks == 0 && this.chatBeaconPending && this.viaChat.isToggled()) {
            this.chatBeaconPending = false;
            this.broadcastChat("вход в мир");
         }
      }

      String var2 = signature();
      if (!var2.equals(this.lastSignature)) {
         this.lastSignature = var2;
         this.publishNow();
         if (this.viaChat.isToggled()) {
            this.broadcastChat("смена косметики");
         }
      }

      long var3 = System.currentTimeMillis();
      float var5 = this.heartbeat.getFloatValue();
      if (var5 > 0.0F && var3 - this.lastPublishAt >= (long)(var5 * 1000.0F)) {
         this.publishNow();
      }

      if (!this.viaChat.isToggled() || var3 - this.lastPublishAt < 1500L) {
         return;
      }

      for (Map.Entry<String, Long> var6 : this.replyDueAt.entrySet()) {
         if (var3 >= var6.getValue()) {
            this.replyDueAt.remove(var6.getKey());
            this.broadcastChat("ответ " + var6.getKey());
            break;
         }
      }
   }

   private void publishNow() {
      if (mc.player == null || mc.player.getGameProfile() == null) {
         return;
      }

      String var1 = mc.player.getGameProfile().name();
      if (var1 == null || var1.isEmpty()) {
         return;
      }

      List<Integer> var2 = LocalCosmetics.e();
      int[] var3 = new int[var2.size()];

      for (int var4 = 0; var4 < var2.size(); var4++) {
         var3[var4] = var2.get(var4);
      }

      CosmeticNet.publish(var1, mc.player.getUuid(), LocalCosmetics.a(), var3, true);
      this.lastPublishAt = System.currentTimeMillis();
      this.lastSignature = signature();
   }

   /**
    * Разбор входящего сообщения чата (фолбэк-маячок). Возвращает true, если строку
    * надо скрыть — это наш служебный маячок, юзеру он в чате не нужен.
    */
   public static boolean handleChatMessage(Text var0) {
      CosmeticSync var1 = k();
      if (var1 == null || !var1.isActive() || var0 == null) {
         return false;
      }

      String var2 = var0.getString();
      int var3 = var2.indexOf(PREFIX);
      if (var3 < 0) {
         return false;
      }

      String var4 = var2.substring(var3 + PREFIX.length());
      int var5 = 0;
      while (var5 < var4.length() && isBase64(var4.charAt(var5))) {
         var5++;
      }

      if (var5 == 0) {
         return false;
      }

      boolean var6 = var1.acceptChat(var4.substring(0, var5));
      return var6 && var1.hideBeacons.isToggled();
   }

   private boolean acceptChat(String var1) {
      try {
         String var2 = new String(Base64.getUrlDecoder().decode(var1), StandardCharsets.UTF_8);
         String[] var3 = var2.split("\\|", -1);
         if (var3.length < 4 || Integer.parseInt(var3[0]) != PROTOCOL) {
            return false;
         }

         int var4 = Integer.parseInt(var3[1]);
         String var5 = var3[2];
         if (var5.isEmpty() || var5.length() > 32) {
            return false;
         }

         int var6 = Math.min(var4, LocalCosmetics.a());
         int[] var7 = var3[3].isEmpty() ? new int[0] : parseIds(var3[3], var6);
         UUID var8 = this.resolveUuid(var5);
         if (mc.player != null && var5.equalsIgnoreCase(mc.player.getGameProfile().name())) {
            return true;
         }

         RemoteCosmetics.apply(var8, var5, var7);
         this.scheduleReply(var5);
         return true;
      } catch (Exception var9) {
         return false;
      }
   }

   private static int[] parseIds(String var0, int var1) {
      String[] var2 = var0.split(",");
      int[] var3 = new int[var2.length];
      int var4 = 0;

      for (String var5 : var2) {
         try {
            int var6 = Integer.parseInt(var5);
            if (var6 >= 0 && var6 < var1) {
               var3[var4++] = var6;
            }
         } catch (NumberFormatException var8) {
         }
      }

      int[] var9 = new int[var4];
      System.arraycopy(var3, 0, var9, 0, var4);
      return var9;
   }

   private UUID resolveUuid(String var1) {
      if (mc.getNetworkHandler() == null) {
         return null;
      }

      PlayerListEntry var2 = mc.getNetworkHandler().getPlayerListEntry(var1);
      if (var2 != null && var2.getProfile() != null) {
         return var2.getProfile().id();
      }

      return null;
   }

   /** Новый игрок прислал маячок в чате — отвечаем один раз, иначе обмена не будет. */
   private void scheduleReply(String var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = this.lastReplyAt.get(var1);
      if (var4 != null && var2 - var4 < REPLY_THROTTLE_MS) {
         return;
      }

      if (!this.replyDueAt.containsKey(var1)) {
         this.replyDueAt.put(var1, var2 + 2000L + ThreadLocalRandom.current().nextLong(4000L));
      }
   }

   private void broadcastChat(String var1) {
      if (mc.player == null || mc.getNetworkHandler() == null || mc.player.getGameProfile() == null) {
         return;
      }

      String var2 = mc.player.getGameProfile().name();
      if (var2 == null || var2.isEmpty()) {
         return;
      }

      List<Integer> var3 = LocalCosmetics.e();
      StringBuilder var4 = new StringBuilder();

      for (Integer var6 : var3) {
         if (var4.length() > 0) {
            var4.append(',');
         }

         var4.append(var6);
      }

      String var7 = PROTOCOL + "|" + LocalCosmetics.a() + "|" + clean(var2) + "|" + var4;
      String var8 = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(var7.getBytes(StandardCharsets.UTF_8));
      mc.getNetworkHandler().sendChatMessage(var8);
      this.lastPublishAt = System.currentTimeMillis();
      if (var1.startsWith("ответ ")) {
         this.lastReplyAt.put(var1.substring(6), this.lastPublishAt);
      }

      if (this.debug.isToggled()) {
         CommandManager.d("§7[CosmeticSync] чат-маячок (" + var1 + "): §f" + var3.size());
      }
   }

   private static String clean(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 != '|' && var3 >= ' ' && var3 != 127) {
            var1.append(var3);
         }
      }

      return var1.toString();
   }

   private static boolean isBase64(char var0) {
      return var0 >= 'A' && var0 <= 'Z'
         || var0 >= 'a' && var0 <= 'z'
         || var0 >= '0' && var0 <= '9'
         || var0 == '-'
         || var0 == '_'
         || var0 == '=';
   }

   private static String signature() {
      StringBuilder var0 = new StringBuilder();

      for (Integer var2 : LocalCosmetics.e()) {
         var0.append(var2).append(';');
      }

      return var0.toString();
   }
}
