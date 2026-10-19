package brokke.cc.core;

import brokke.cc.config.AutoBuyConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

public class AutoSeller {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int BLOCKED_RETRY_TICKS = 600;
   private AutoSeller.a state = AutoSeller.a.IDLE;
   private int delay = 0;
   private int waitTicks = 0;
   private int reopenTries = 0;
   private int blockedRetryTimer = 0;
   private String currentItemId = null;
   private ItemEntry currentEntry = null;
   private int currentCount = 1;
   private boolean relistMode = false;
   private final Set<String> attempted = new HashSet<>();
   private volatile boolean pending = false;
   private volatile boolean pendingRelist = false;
   private volatile boolean blocked = false;
   private volatile boolean listedOk = false;
   private volatile boolean fullSignal = false;
   private volatile boolean cooldownSignal = false;

   public void a() {
      this.state = AutoSeller.a.IDLE;
      this.delay = 0;
      this.waitTicks = 0;
      this.reopenTries = 0;
      this.blockedRetryTimer = 0;
      this.currentItemId = null;
      this.currentEntry = null;
      this.currentCount = 1;
      this.relistMode = false;
      this.attempted.clear();
      this.pending = false;
      this.pendingRelist = false;
      this.blocked = false;
      this.listedOk = false;
      this.fullSignal = false;
      this.cooldownSignal = false;
   }

   public void b() {
      this.pending = true;
      this.pendingRelist = false;
   }

   public void c() {
      this.pending = true;
      this.pendingRelist = true;
   }

   public boolean d() {
      return this.state != AutoSeller.a.IDLE;
   }

   public void e() {
      if (this.state == AutoSeller.a.WAIT) {
         this.listedOk = true;
      }
   }

   public void f() {
      if (this.state == AutoSeller.a.SELL_AUTO || this.state == AutoSeller.a.WAIT) {
         this.fullSignal = true;
      }
   }

   public void g() {
      if (this.blocked) {
         this.blocked = false;
         this.pending = true;
      }
   }

   public void h() {
      if (this.state == AutoSeller.a.REOPEN || this.state == AutoSeller.a.WAIT_REOPEN) {
         this.cooldownSignal = true;
      }
   }

   public synchronized void i() {
      if (this.mc.player != null) {
         if (this.delay > 0) {
            this.delay--;
         } else {
            switch (this.state) {
               case IDLE:
                  if (this.blocked) {
                     if (--this.blockedRetryTimer > 0) {
                        return;
                     }

                     this.blocked = false;
                     this.pending = true;
                     this.pendingRelist = this.relistMode;
                  }

                  if (!this.pending) {
                     return;
                  }

                  this.pending = false;
                  this.relistMode = this.pendingRelist;
                  this.pendingRelist = false;
                  this.attempted.clear();
                  this.state = AutoSeller.a.CLOSE;
                  break;
               case CLOSE:
                  if (this.mc.currentScreen != null) {
                     if (this.mc.player != null) {
                        this.mc.player.closeHandledScreen();
                     }

                     if (this.mc.currentScreen != null) {
                        this.mc.setScreen(null);
                     }

                     this.delay = 6;
                  } else {
                     this.state = AutoSeller.a.SWEEP;
                  }
                  break;
               case SWEEP:
                  int var1 = this.j();
                  if (var1 < 0) {
                     this.reopenTries = 0;
                     this.state = AutoSeller.a.REOPEN;
                  } else {
                     ItemStack var2 = this.mc.player.getInventory().getStack(var1);
                     this.currentItemId = Registries.ITEM.getId(var2.getItem()).toString();
                     this.currentEntry = this.b(var2);
                     this.currentCount = Math.max(1, var2.getCount());
                     this.a(var1);
                     this.state = AutoSeller.a.SELL_AUTO;
                     this.delay = 8;
                  }
                  break;
               case SELL_AUTO:
                  this.listedOk = false;
                  this.fullSignal = false;
                  this.mc.player.networkHandler.sendChatCommand(this.k());
                  this.state = AutoSeller.a.WAIT;
                  this.waitTicks = 100;
                  break;
               case WAIT:
                  if (this.fullSignal) {
                     this.fullSignal = false;
                     this.blocked = true;
                     this.blockedRetryTimer = 600;
                     this.reopenTries = 0;
                     this.state = AutoSeller.a.REOPEN;
                  } else if (this.listedOk) {
                     this.listedOk = false;
                     this.state = AutoSeller.a.SWEEP;
                     this.delay = 6;
                  } else {
                     this.waitTicks--;
                     if (this.waitTicks <= 0) {
                        if (this.currentItemId != null) {
                           this.attempted.add(this.currentItemId);
                        }

                        this.state = AutoSeller.a.SWEEP;
                        this.delay = 4;
                     }
                  }
                  break;
               case REOPEN:
                  this.cooldownSignal = false;
                  this.mc.player.networkHandler.sendChatCommand("ah");
                  this.reopenTries++;
                  this.state = AutoSeller.a.WAIT_REOPEN;
                  this.waitTicks = 40;
                  break;
               case WAIT_REOPEN:
                  if (this.l()) {
                     this.reopenTries = 0;
                     this.state = AutoSeller.a.IDLE;
                     this.delay = 10;
                  } else if (this.cooldownSignal) {
                     this.cooldownSignal = false;
                     this.state = AutoSeller.a.REOPEN;
                     this.delay = 25;
                  } else {
                     this.waitTicks--;
                     if (this.waitTicks <= 0) {
                        if (this.reopenTries < 6) {
                           this.state = AutoSeller.a.REOPEN;
                           this.delay = 10;
                        } else {
                           this.reopenTries = 0;
                           this.state = AutoSeller.a.IDLE;
                        }
                     }
                  }
                  break;
               default:
                  this.state = AutoSeller.a.IDLE;
            }
         }
      }
   }

   private int j() {
      if (this.mc.player == null) {
         return -1;
      }

      PlayerInventory var1 = this.mc.player.getInventory();

      for (int var2 = 0; var2 < 36; var2++) {
         ItemStack var3 = var1.getStack(var2);
         if (!var3.isEmpty()) {
            String var4 = Registries.ITEM.getId(var3.getItem()).toString();
            if (!this.attempted.contains(var4) && this.a(var3)) {
               return var2;
            }
         }
      }

      return -1;
   }

   private boolean a(ItemStack var1) {
      return this.b(var1) != null;
   }

   private ItemEntry b(ItemStack var1) {
      Lot var2 = PriceParser.a(var1);
      if (var2 == null) {
         return null;
      }

      for (ItemEntry var4 : AutoBuyConfig.getInstance().entries) {
         if (ItemMatcher.a(var4, var2)) {
            return var4;
         }
      }

      return null;
   }

   private String k() {
      long var1 = this.currentEntry != null ? this.currentEntry.marketPrice : 0L;
      if (var1 <= 0L) {
         return "ah sell auto";
      }

      AutoBuyConfig var3 = AutoBuyConfig.getInstance();
      int var4 = this.relistMode ? var3.relistDiscountPercent : var3.sellDiscountPercent;
      var4 = Math.max(0, Math.min(99, var4));
      long var5 = var1 * Math.max(1, this.currentCount) * (100L - var4) / 100L;
      if (var5 < 1L) {
         var5 = 1L;
      }

      return "ah sell " + var5;
   }

   private boolean l() {
      return this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1);
   }

   private void a(int var1) {
      if (var1 < 9) {
         this.b(var1);
      } else {
         int var2 = this.mc.player.getInventory().getSelectedSlot();
         if (this.mc.interactionManager != null) {
            this.mc.interactionManager.clickSlot(0, var1, var2, SlotActionType.SWAP, this.mc.player);
         }

         this.b(var2);
      }
   }

   private void b(int var1) {
      this.mc.player.getInventory().setSelectedSlot(var1);
      this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var1));
   }

   enum a {
      IDLE,
      CLOSE,
      SWEEP,
      SELL_AUTO,
      WAIT,
      REOPEN,
      WAIT_REOPEN;
   }
}
