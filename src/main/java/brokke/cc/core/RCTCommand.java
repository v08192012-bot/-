package brokke.cc.core;

import brokke.cc.mixins.interfaces.IBossBarHud;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.AnarchyUtil;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

public class RCTCommand {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final RCTCommand INSTANCE = new RCTCommand();
   private static final int MIN_ANARCHY = 1;
   private static final int MAX_ANARCHY = 36;
   private static final long DIRECT_JOIN_TIMEOUT_MS = 600L;
   private static final long LITE_RETRY_DELAY_MS = 250L;
   private static final long CLICK_DELAY_MS = 80L;
   private static final long TAB_RETRY_DELAY_MS = 400L;
   private static final long COMMAND_GAP_MS = 50L;
   private static final long SUCCESS_SETTLE_MS = 150L;
   private static final long TOTAL_TIMEOUT_MS = 10000L;
   private static final long HUB_RESEND_DELAY_MS = 2000L;
   private static final int MAX_ATTEMPTS = 3;
   public static volatile boolean RUNNING = false;
   private static int targetAnarchyNumber = -1;
   private static int initialAnarchy = -1;
   private static int lastAnarchyNumber = -1;
   private static RCTCommand.a stage = RCTCommand.a.IDLE;
   private static long startedAt = 0L;
   private static long stateSince = 0L;
   private static long lastAction = 0L;
   private static long lastTabClick = 0L;
   private static int attempts = 0;
   private static long lastHubSend = 0L;
   private static int lastMenuSync = -1;
   private static int lastMenuCount = -1;

   private static String c(int var0) {
      return StreamerMode.k() ? "ХЗ" : String.valueOf(var0);
   }

   public static void a() {
      EventBus.register(INSTANCE);
   }

   public static void b() {
      if (mc.player != null && mc.world != null) {
         int var0 = AnarchyUtil.a();
         int var1 = var0 > 0 ? var0 : (lastAnarchyNumber > 0 ? lastAnarchyNumber : -1);
         if (var1 >= 1 && var1 <= 36) {
            a(var1);
         } else {
            a("§cНе удалось определить текущую анархию. Используйте: §f.rct <1-36>");
         }
      } else {
         a("§cКоманду нельзя выполнить вне мира.");
      }
   }

   public static void a(int var0) {
      if (var0 < 1 || var0 > 36) {
         a("§cНомер анархии должен быть от 1 до 36: §f.rct <1-36>");
      } else if (mc.player == null || mc.world == null) {
         a("§cКоманду нельзя выполнить вне мира.");
      } else if (g()) {
         a("§cВы находитесь в режиме PvP.");
      } else {
         long var1 = System.currentTimeMillis();
         targetAnarchyNumber = var0;
         initialAnarchy = AnarchyUtil.a();
         lastAnarchyNumber = var0;
         startedAt = var1;
         stateSince = var1;
         lastAction = 0L;
         lastTabClick = 0L;
         attempts = 0;
         lastHubSend = 0L;
         lastMenuSync = -1;
         lastMenuCount = -1;
         stage = RCTCommand.a.IDLE;
         RUNNING = true;
         a("§7RCT запущен для анархии §f" + c(var0) + "§7.");
         if (h()) {
            a(var1);
         } else {
            b(var1);
         }
      }
   }

   public static void c() {
      a("hub", System.currentTimeMillis());
      a("§aОтправлена команда /hub");
   }

   public static void d() {
      i();
   }

   public static boolean isRunning() {
      return RUNNING;
   }

   public static int getLastAnarchyNumber() {
      return lastAnarchyNumber;
   }

   public static void setLastAnarchyNumber(int var0) {
      lastAnarchyNumber = var0;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (RUNNING && targetAnarchyNumber >= 1) {
         if (mc.player != null && mc.world != null) {
            long var2 = System.currentTimeMillis();
            if (var2 - startedAt > 10000L) {
               a("§cRCT не успел завершиться, отменяю попытку.");
               i();
            } else if (mc.currentScreen instanceof GenericContainerScreen var4 && a(var4)) {
               a(var4, var2);
            } else {
               int var6 = AnarchyUtil.a();
               if (a(var6, var2)) {
                  a("§aУспешно зашли на анархию " + c(targetAnarchyNumber));
                  i();
               } else if (stage == RCTCommand.a.DIRECT_JOIN) {
                  if (var2 - stateSince >= 600L) {
                     b(var2);
                  }
               } else if (stage == RCTCommand.a.FALLBACK) {
                  if (var2 - lastHubSend >= 2000L) {
                     if (attempts >= 3) {
                        a("§cНе удалось открыть меню выбора после 3 попыток.");
                        i();
                        return;
                     }

                     a("hub", var2);
                     lastHubSend = var2;
                     attempts++;
                     lastMenuSync = -1;
                     lastMenuCount = -1;
                     return;
                  }

                  if (var2 - lastAction >= 250L) {
                     a("lite", var2);
                  }
               }
            }
         } else {
            i();
         }
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (RUNNING && targetAnarchyNumber >= 1 && var1.getPacket() instanceof GameMessageS2CPacket var2) {
         String var4 = var2.content().getString().toLowerCase(Locale.ROOT);
         if (var4.contains("не удалось") || var4.contains("нельзя зайти") || var4.contains("already connecting")) {
            a("§cНа данную анархию не удалось зайти.");
            i();
         }
      }
   }

   private static void a(GenericContainerScreen var0, long var1) {
      if (mc.player != null && mc.interactionManager != null && var1 - lastAction >= 80L && b(var0)) {
         int var3 = d(targetAnarchyNumber);
         int var4 = e(targetAnarchyNumber);
         int var5 = 18 + targetAnarchyNumber - var4;
         DefaultedList var6 = var0.getScreenHandler().slots;
         int var7 = Math.min(var0.getScreenHandler().getInventory().size(), var6.size());
         int var8 = -1;
         if (var5 >= 18 && var5 < var7 && a(((Slot)var6.get(var5)).getStack(), targetAnarchyNumber)) {
            var8 = var5;
         } else {
            for (int var9 = 18; var9 <= 35 && var9 < var7; var9++) {
               if (a(((Slot)var6.get(var9)).getStack(), targetAnarchyNumber)) {
                  var8 = var9;
                  break;
               }
            }
         }

         if (var8 != -1) {
            mc.interactionManager.clickSlot(var0.getScreenHandler().syncId, var8, 0, SlotActionType.PICKUP, mc.player);
            lastAction = var1;
            stateSince = var1;
            mc.player.closeHandledScreen();
            i();
            a("§aУспешно отправлен запрос на вход в анархию " + c(targetAnarchyNumber));
         } else if (a(var0, var3) && var5 >= 18 && var5 < var7 && !((Slot)var6.get(var5)).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(var0.getScreenHandler().syncId, var5, 0, SlotActionType.PICKUP, mc.player);
            lastAction = var1;
            stateSince = var1;
            mc.player.closeHandledScreen();
            i();
            a("§aУспешно отправлен запрос на вход в анархию " + c(targetAnarchyNumber));
         } else if (var1 - lastTabClick >= 400L && var3 >= 0 && var3 < var7) {
            mc.interactionManager.clickSlot(var0.getScreenHandler().syncId, var3, 0, SlotActionType.PICKUP, mc.player);
            lastAction = var1;
            lastTabClick = var1;
            stateSince = var1;
            lastMenuSync = -1;
            lastMenuCount = -1;
         }
      }
   }

   private static boolean a(GenericContainerScreen var0, int var1) {
      if (var1 >= 0 && var1 < var0.getScreenHandler().slots.size()) {
         ItemStack var2 = var0.getScreenHandler().slots.get(var1).getStack();
         return !var2.isEmpty() && var2.hasGlint();
      } else {
         return false;
      }
   }

   private static boolean a(ItemStack var0, int var1) {
      if (var0 != null && !var0.isEmpty()) {
         if (var0.getCount() == var1) {
            return true;
         }

         String var2 = var0.getName().getString();
         int var3 = AnarchyUtil.a(var2);
         return var3 == var1;
      } else {
         return false;
      }
   }

   private static boolean h() {
      return initialAnarchy == -1 || initialAnarchy != targetAnarchyNumber;
   }

   private static boolean a(int var0, long var1) {
      if (var0 != targetAnarchyNumber) {
         return false;
      } else if (stage != RCTCommand.a.DIRECT_JOIN) {
         return false;
      } else {
         return initialAnarchy == targetAnarchyNumber ? false : var1 - lastAction >= 150L;
      }
   }

   private static void a(long var0) {
      AnarchyUtil.a(targetAnarchyNumber);
      stage = RCTCommand.a.DIRECT_JOIN;
      stateSince = var0;
      lastAction = var0;
   }

   private static void b(long var0) {
      stage = RCTCommand.a.FALLBACK;
      stateSince = var0;
      a("hub", var0);
      lastHubSend = var0;
      attempts = 1;
      lastMenuSync = -1;
      lastMenuCount = -1;
   }

   private static boolean a(GenericContainerScreen var0) {
      String var1 = var0.getTitle().getString().toLowerCase(Locale.ROOT);
      return var1.contains("лайт") || var1.contains("lite") || var1.contains("выбор сервера") || var1.contains("анарх");
   }

   private static int d(int var0) {
      if (var0 <= 9) {
         return 0;
      } else if (var0 <= 19) {
         return 1;
      } else {
         return var0 <= 29 ? 2 : 3;
      }
   }

   private static int e(int var0) {
      return switch (d(var0)) {
         case 1 -> 10;
         case 2 -> 20;
         case 3 -> 30;
         default -> 1;
      };
   }

   private static void a(String var0, long var1) {
      if (mc.player != null && mc.player.networkHandler != null && (lastAction == 0L || var1 - lastAction >= 50L)) {
         mc.player.networkHandler.sendChatCommand(var0);
         lastAction = var1;
         stateSince = var1;
      }
   }

   private static void i() {
      targetAnarchyNumber = -1;
      initialAnarchy = -1;
      stage = RCTCommand.a.IDLE;
      startedAt = 0L;
      stateSince = 0L;
      lastAction = 0L;
      lastTabClick = 0L;
      attempts = 0;
      lastHubSend = 0L;
      lastMenuSync = -1;
      lastMenuCount = -1;
      RUNNING = false;
   }

   private static boolean b(GenericContainerScreen var0) {
      int var1 = var0.getScreenHandler().syncId;
      int var2 = Math.min(var0.getScreenHandler().getInventory().size(), var0.getScreenHandler().slots.size());
      int var3 = 0;

      for (int var4 = 0; var4 < var2; var4++) {
         if (!var0.getScreenHandler().slots.get(var4).getStack().isEmpty()) {
            var3++;
         }
      }

      boolean var5 = var3 > 0 && var1 == lastMenuSync && var3 == lastMenuCount;
      lastMenuSync = var1;
      lastMenuCount = var3;
      return var5;
   }

   public static boolean g() {
      if (mc.inGameHud == null) {
         return false;
      }

      BossBarHud var0 = mc.inGameHud.getBossBarHud();
      if (var0 == null) {
         return false;
      }

      try {
         for (ClientBossBar var2 : ((IBossBarHud)var0).getBossBars().values()) {
            String var3 = var2.getName().getString().toLowerCase(Locale.ROOT);
            if (var3.contains("pvp") || var3.contains("пвп")) {
               return true;
            }
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private static void a(String var0) {
      mc.execute(() -> {
         if (mc.player != null) {
            mc.player.sendMessage(Text.literal(var0), false);
         }
      });
   }

   static {
      a();
   }

   enum a {
      IDLE,
      DIRECT_JOIN,
      FALLBACK;
   }
}
