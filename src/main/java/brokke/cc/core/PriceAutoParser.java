package brokke.cc.core;

import brokke.cc.config.AutoBuyConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class PriceAutoParser {
   private static final int MAX_PAGES = 25;
   private static final int SORT_CLICK_DELAY = 6;
   private static final int PRE_SORT_CLICKS = 4;
   private static final int POST_SORT_CLICKS = 2;
   private static final int SCAN_MAX_TICKS = 14;
   private static final int STABLE_TICKS = 2;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private PriceAutoParser.a state = PriceAutoParser.a.IDLE;
   private List<ItemEntry> queue = new ArrayList<>();
   private int index = 0;
   private int delay = 0;
   private long bestPrice = -1L;
   private int page = 0;
   private long pageHash = 0L;
   private int counter = 0;
   private int sortClicksLeft = 0;
   private String label = "";
   private long lastScanHash = 0L;
   private int stableTicks = 0;
   private AuctionSortMode sortTarget = null;

   public synchronized void a() {
      if (this.state != PriceAutoParser.a.IDLE) {
         this.b();
      } else if (this.mc.player != null) {
         this.queue = new ArrayList<>();

         for (ItemEntry var2 : AutoBuyConfig.getInstance().entries) {
            if (var2.enabled && var2.parseEnabled) {
               this.queue.add(var2);
            }
         }

         if (this.queue.isEmpty()) {
            this.a("§e[AutoBuy] Нет включённых предметов для парсинга");
         } else {
            this.index = 0;
            this.delay = 0;
            this.counter = 0;
            this.sortClicksLeft = 0;
            this.label = "сортировка";
            this.state = PriceAutoParser.a.PRE_OPEN;
            this.a("§e[AutoBuy] Парсинг цен: " + this.queue.size() + " предмет(ов)");
         }
      }
   }

   public synchronized void b() {
      this.state = PriceAutoParser.a.IDLE;
      this.queue.clear();
      this.index = 0;
      this.label = "";
      this.sortTarget = null;
   }

   public boolean c() {
      return this.state != PriceAutoParser.a.IDLE;
   }

   public String d() {
      if (this.state == PriceAutoParser.a.IDLE) {
         return "Парсер цен: ожидание";
      }

      int var1 = Math.min(this.index + 1, Math.max(1, this.queue.size()));
      return "Парсер: " + var1 + "/" + this.queue.size() + " — " + this.label;
   }

   public synchronized void e() {
      if (this.state != PriceAutoParser.a.IDLE) {
         if (this.mc.player == null) {
            this.b();
         } else if (this.delay > 0) {
            this.delay--;
         } else {
            switch (this.state) {
               case PRE_OPEN:
                  this.g();
                  break;
               case PRE_WAIT:
                  this.h();
                  break;
               case PRE_SORT:
                  this.i();
                  break;
               case NEXT:
                  this.n();
                  break;
               case WAIT_SCREEN:
                  this.o();
                  break;
               case SCAN:
                  this.q();
                  break;
               case WAIT_PAGE:
                  this.r();
                  break;
               case ADVANCE:
                  this.s();
                  break;
               case POST_OPEN:
                  this.j();
                  break;
               case POST_WAIT:
                  this.k();
                  break;
               case POST_SORT:
                  this.l();
            }
         }
      }
   }

   private void f() {
      if (this.mc.player != null) {
         this.mc.player.closeHandledScreen();
      }

      if (this.mc.currentScreen != null) {
         this.mc.setScreen(null);
      }
   }

   private void g() {
      if (this.u()) {
         this.a(AuctionSortMode.CHEAPEST_PER_UNIT, 4, PriceAutoParser.a.PRE_SORT);
      } else if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
         this.delay = 4;
      } else if (this.mc.currentScreen != null) {
         this.f();
         this.delay = 6;
      } else {
         this.mc.player.networkHandler.sendChatCommand("ah");
         this.state = PriceAutoParser.a.PRE_WAIT;
         this.counter = 60;
      }
   }

   private void h() {
      if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
      }

      if (this.u()) {
         this.a(AuctionSortMode.CHEAPEST_PER_UNIT, 4, PriceAutoParser.a.PRE_SORT);
      } else if (--this.counter <= 0) {
         this.state = PriceAutoParser.a.NEXT;
         this.delay = 8;
      }
   }

   private void i() {
      if (!this.u()) {
         this.state = PriceAutoParser.a.NEXT;
         this.delay = 8;
      } else {
         GenericContainerScreenHandler var1 = this.v();
         int var2 = AuctionSortMode.a(var1);
         if (var2 < 0) {
            this.a("§e[AutoBuy] Воронка «Сортировка» не найдена");
            this.state = PriceAutoParser.a.NEXT;
            this.delay = 8;
         } else {
            AuctionSortMode var3 = AuctionSortMode.a(var1.getSlot(var2).getStack());
            if (this.sortTarget != null && var3 == this.sortTarget) {
               this.state = PriceAutoParser.a.NEXT;
               this.delay = 8;
            } else if (this.sortClicksLeft <= 0) {
               this.state = PriceAutoParser.a.NEXT;
               this.delay = 8;
            } else {
               this.a(var1.syncId, var2);
               this.sortClicksLeft--;
               this.delay = 6;
            }
         }
      }
   }

   private void j() {
      this.label = "сортировка";
      if (this.u()) {
         this.a(AuctionSortMode.NEWEST, 2, PriceAutoParser.a.POST_SORT);
      } else if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
         this.delay = 4;
      } else if (this.mc.currentScreen != null) {
         this.f();
         this.delay = 6;
      } else {
         this.mc.player.networkHandler.sendChatCommand("ah");
         this.state = PriceAutoParser.a.POST_WAIT;
         this.counter = 60;
      }
   }

   private void k() {
      if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
      }

      if (this.u()) {
         this.a(AuctionSortMode.NEWEST, 2, PriceAutoParser.a.POST_SORT);
      } else if (--this.counter <= 0) {
         this.m();
      }
   }

   private void l() {
      if (!this.u()) {
         this.m();
      } else {
         GenericContainerScreenHandler var1 = this.v();
         int var2 = AuctionSortMode.a(var1);
         if (var2 < 0) {
            this.m();
         } else {
            AuctionSortMode var3 = AuctionSortMode.a(var1.getSlot(var2).getStack());
            if (this.sortTarget != null && var3 == this.sortTarget) {
               this.m();
            } else if (this.sortClicksLeft <= 0) {
               this.m();
            } else {
               this.a(var1.syncId, var2);
               this.sortClicksLeft--;
               this.delay = 6;
            }
         }
      }
   }

   private void m() {
      this.a("§a[AutoBuy] Парсинг окончен");
      AutoBuyConfig.g();
      this.b();
   }

   private void n() {
      if (this.index >= this.queue.size()) {
         this.state = PriceAutoParser.a.POST_OPEN;
         this.counter = 60;
         this.delay = 4;
      } else {
         ItemEntry var1 = this.queue.get(this.index);
         String var2 = var1.displayName != null && !var1.displayName.isBlank() ? var1.displayName : var1.itemId;
         if (var1.loreKeyword != null && !var1.loreKeyword.isBlank()) {
            var2 = var2 + " [" + var1.loreKeyword + "]";
         }

         this.label = var2;
         this.bestPrice = -1L;
         this.page = 0;
         this.pageHash = 0L;
         if (this.mc.currentScreen instanceof GameMenuScreen) {
            this.mc.setScreen(null);
            this.delay = 4;
         } else if (this.mc.currentScreen != null) {
            this.f();
            this.delay = 4;
         } else {
            String var3 = var1.b().replaceAll("[«»\"']", "").trim();
            this.mc.player.networkHandler.sendChatCommand("ah search " + var3);
            this.state = PriceAutoParser.a.WAIT_SCREEN;
            this.delay = 0;
            this.counter = 60;
         }
      }
   }

   private void o() {
      this.counter--;
      if (this.counter <= 0) {
         this.t();
      } else {
         if (this.mc.currentScreen instanceof GameMenuScreen) {
            this.mc.setScreen(null);
         }

         if (this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1)) {
            this.p();
         }
      }
   }

   private void p() {
      this.state = PriceAutoParser.a.SCAN;
      this.counter = 14;
      this.lastScanHash = 0L;
      this.stableTicks = 0;
   }

   private void q() {
      if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
      } else if (this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1)) {
         GenericContainerScreenHandler var12 = var1.getScreenHandler();
         int var3 = var12.getRows();
         int var4 = (var3 - 1) * 9;
         ItemEntry var5 = this.queue.get(this.index);
         int var6 = 0;

         for (int var7 = 0; var7 < var4; var7++) {
            ItemStack var8 = var12.getSlot(var7).getStack();
            if (!var8.isEmpty()) {
               var6++;
            }

            Lot var9 = PriceParser.a(var7, var8);
            if (var9 != null && !var9.barrier && ItemMatcher.a(var5, var9) && (this.bestPrice < 0L || var9.unitPrice < this.bestPrice)) {
               this.bestPrice = var9.unitPrice;
            }
         }

         long var13 = this.b(var12, var4);
         if (var13 == this.lastScanHash) {
            this.stableTicks++;
         } else {
            this.stableTicks = 0;
            this.lastScanHash = var13;
         }

         this.counter--;
         boolean var14 = var6 > 0 && this.stableTicks >= 2;
         if (var14 || this.counter <= 0) {
            boolean var10 = this.bestPrice < 0L && this.page + 1 < 25;
            if (!var10) {
               this.t();
            } else {
               int var11 = this.a(var12, var3);
               if (var11 < 0) {
                  this.t();
               } else {
                  this.pageHash = this.b(var12, var4);
                  this.a(var12.syncId, var11);
                  this.state = PriceAutoParser.a.WAIT_PAGE;
                  this.delay = 3;
                  this.counter = 30;
                  this.page++;
               }
            }
         }
      } else if (--this.counter <= 0) {
         this.t();
      }
   }

   private void r() {
      if (this.mc.currentScreen instanceof GameMenuScreen) {
         this.mc.setScreen(null);
      } else if (this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1)) {
         GenericContainerScreenHandler var5 = var1.getScreenHandler();
         int var3 = var5.getRows();
         int var4 = (var3 - 1) * 9;
         if (this.b(var5, var4) != this.pageHash) {
            this.p();
         } else {
            this.counter--;
            if (this.counter <= 0) {
               this.t();
            }
         }
      } else if (--this.counter <= 0) {
         this.t();
      }
   }

   private void s() {
      if (this.mc.currentScreen != null) {
         this.f();
         this.delay = 4;
      } else {
         this.index++;
         this.state = PriceAutoParser.a.NEXT;
         this.delay = 4;
      }
   }

   private void t() {
      ItemEntry var1 = this.queue.get(this.index);
      AutoBuyConfig var2 = AutoBuyConfig.getInstance();
      int var3 = Math.max(1, Math.min(100, var2.parsePercent));
      long var4 = this.bestPrice > 0L ? this.bestPrice * var3 / 100L : 0L;
      var1.maxUnitPrice = var4;
      if (this.bestPrice > 0L) {
         var1.marketPrice = this.bestPrice;
      }

      AutoBuyConfig.g();
      this.state = PriceAutoParser.a.ADVANCE;
      this.delay = 2;
   }

   private void a(AuctionSortMode var1, int var2, PriceAutoParser.a var3) {
      this.sortTarget = var1;
      this.sortClicksLeft = var2;
      AuctionSortMode var4 = AuctionSortMode.b(this.v());
      if (var4 != null) {
         this.sortClicksLeft = var4.a(var1);
      }

      this.state = var3;
      this.delay = 6;
   }

   private boolean u() {
      return this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1);
   }

   private GenericContainerScreenHandler v() {
      return ((GenericContainerScreen)this.mc.currentScreen).getScreenHandler();
   }

   private boolean a(ItemEntry var1) {
      if (var1 != null && var1.itemId != null) {
         String var2 = var1.itemId;
         return var2.endsWith("_helmet") || var2.endsWith("_chestplate") || var2.endsWith("_leggings") || var2.endsWith("_boots");
      } else {
         return false;
      }
   }

   private int a(GenericContainerScreenHandler var1, int var2) {
      int var3 = (var2 - 1) * 9;
      int var4 = var2 * 9;

      for (int var5 = var3; var5 < var4; var5++) {
         ItemStack var6 = var1.getSlot(var5).getStack();
         if (!var6.isEmpty()) {
            String var7 = var6.getName().getString().toLowerCase(Locale.ROOT);
            if (!var7.contains("назад")
               && !var7.contains("prev")
               && !var7.contains("предыдущ")
               && (
                  var7.contains("след")
                     || var7.contains("вперед")
                     || var7.contains("вперёд")
                     || var7.contains("далее")
                     || var7.contains("next")
                     || var7.contains("▶")
                     || var7.contains("►")
                     || var7.contains("→")
                     || var7.contains("»")
               )) {
               return var5;
            }
         }
      }

      for (int var8 = var3; var8 < var4; var8++) {
         ItemStack var10 = var1.getSlot(var8).getStack();
         if (!var10.isEmpty()) {
            String var12 = Registries.ITEM.getId(var10.getItem()).toString();
            if (var12.equals("minecraft:lime_dye") || var12.equals("minecraft:green_dye")) {
               return var8;
            }
         }
      }

      int var9 = var4 - 1;
      if (var9 >= var3 && var9 < var1.slots.size()) {
         ItemStack var11 = var1.getSlot(var9).getStack();
         if (!var11.isEmpty() && var11.isOf(Items.ARROW)) {
            String var13 = var11.getName().getString().toLowerCase(Locale.ROOT);
            if (!var13.contains("назад") && !var13.contains("prev") && !var13.contains("предыдущ")) {
               return var9;
            }
         }
      }

      return -1;
   }

   private long b(GenericContainerScreenHandler var1, int var2) {
      long var3 = 1L;

      for (int var5 = 0; var5 < var2; var5++) {
         ItemStack var6 = var1.getSlot(var5).getStack();
         var3 = var3 * 31L + (var6.isEmpty() ? 0L : Registries.ITEM.getRawId(var6.getItem()) * 113L + var6.getCount());
      }

      return var3;
   }

   private void a(int var1, int var2) {
      if (this.mc.interactionManager != null && this.mc.player != null) {
         this.mc.interactionManager.clickSlot(var1, var2, 0, SlotActionType.PICKUP, this.mc.player);
      }
   }

   private void a(String var1) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal(var1), false);
      }
   }

   enum a {
      IDLE,
      PRE_OPEN,
      PRE_WAIT,
      PRE_SORT,
      NEXT,
      WAIT_SCREEN,
      SCAN,
      WAIT_PAGE,
      ADVANCE,
      POST_OPEN,
      POST_WAIT,
      POST_SORT;
   }
}
