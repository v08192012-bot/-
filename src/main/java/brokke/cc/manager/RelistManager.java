package brokke.cc.manager;

import brokke.cc.config.AutoBuyConfig;
import brokke.cc.core.AutoBuyEngine;
import brokke.cc.core.ItemEntry;
import brokke.cc.core.ItemMatcher;
import brokke.cc.core.Lot;
import brokke.cc.core.PriceParser;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class RelistManager {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private static final String BUTTON_KEYWORD = "товары на продаже";
   private static final String LISTINGS_TITLE = "продаж";
   private static final int COLLECT_DELAY = 10;
   private static final int MAX_COLLECT = 300;
   private RelistManager.a state = RelistManager.a.IDLE;
   private int delay = 0;
   private int waitTicks = 0;
   private int collected = 0;
   private int emptyScans = 0;
   private volatile boolean pending = false;

   public void a() {
      this.pending = true;
   }

   public boolean b() {
      return this.state != RelistManager.a.IDLE;
   }

   public String c() {
      return this.state == RelistManager.a.IDLE ? "Релист: ожидание" : "Релист: снято " + this.collected;
   }

   public synchronized void d() {
      if (this.mc.player == null || this.mc.interactionManager == null) {
         this.state = RelistManager.a.IDLE;
      } else if (this.delay > 0) {
         this.delay--;
      } else {
         switch (this.state) {
            case IDLE:
               if (!this.pending) {
                  return;
               }

               this.pending = false;
               this.collected = 0;
               this.emptyScans = 0;
               this.state = this.f() ? RelistManager.a.OPEN_LISTINGS : RelistManager.a.OPEN_AUCTION;
               break;
            case OPEN_AUCTION:
               if (this.mc.currentScreen != null) {
                  if (this.mc.player != null) {
                     this.mc.player.closeHandledScreen();
                  }

                  if (this.mc.currentScreen != null) {
                     this.mc.setScreen(null);
                  }

                  this.delay = 6;
               } else {
                  this.mc.player.networkHandler.sendChatCommand("ah");
                  this.state = RelistManager.a.WAIT_AUCTION;
                  this.waitTicks = 60;
               }
               break;
            case WAIT_AUCTION:
               if (this.f()) {
                  this.state = RelistManager.a.OPEN_LISTINGS;
                  this.delay = 4;
               } else if (--this.waitTicks <= 0) {
                  this.e();
               }
               break;
            case OPEN_LISTINGS:
               if (!this.f()) {
                  this.e();
                  return;
               }

               GenericContainerScreenHandler var3 = ((GenericContainerScreen)this.mc.currentScreen).getScreenHandler();
               int var4 = this.a(var3);
               if (var4 < 0) {
                  this.a("§e[Релист] Эндер-сундук «Товары на продаже» не найден");
                  this.e();
                  return;
               }

               this.a(var3.syncId, var4, 0);
               this.state = RelistManager.a.WAIT_LISTINGS;
               this.waitTicks = 60;
               break;
            case WAIT_LISTINGS:
               if (this.g()) {
                  this.state = RelistManager.a.COLLECT;
                  this.delay = 6;
                  this.emptyScans = 0;
               } else if (--this.waitTicks <= 0) {
                  this.e();
               }
               break;
            case COLLECT:
               if (!this.g()) {
                  this.e();
                  return;
               }

               GenericContainerScreenHandler var1 = ((GenericContainerScreen)this.mc.currentScreen).getScreenHandler();
               int var2 = this.b(var1);
               if (var2 < 0) {
                  if (++this.emptyScans >= 3) {
                     this.e();
                  } else {
                     this.delay = 4;
                  }
               } else {
                  this.emptyScans = 0;
                  this.a(var1.syncId, var2, 1);
                  this.collected++;
                  this.delay = 10;
                  if (this.collected >= 300) {
                     this.e();
                  }
               }
               break;
            case FINISH:
               if (this.mc.currentScreen != null) {
                  if (this.mc.player != null) {
                     this.mc.player.closeHandledScreen();
                  }

                  if (this.mc.currentScreen != null) {
                     this.mc.setScreen(null);
                  }

                  this.delay = 6;
               } else {
                  if (this.collected > 0) {
                     this.a("§a[Релист] Снято лотов: §f" + this.collected + "§a — перевыставляю");
                     AutoBuyEngine.INSTANCE.seller.c();
                  } else {
                     this.a("§7[Релист] Своих лотов из списка нет");
                  }

                  this.state = RelistManager.a.IDLE;
               }
               break;
            default:
               this.state = RelistManager.a.IDLE;
         }
      }
   }

   private void e() {
      this.state = RelistManager.a.FINISH;
      this.delay = 4;
   }

   private boolean f() {
      return this.mc.currentScreen instanceof GenericContainerScreen var1 && AutoBuyEngine.a(var1);
   }

   private boolean g() {
      if (this.mc.currentScreen instanceof GenericContainerScreen var1) {
         String var3 = var1.getTitle().getString().toLowerCase(Locale.ROOT);
         return var3.contains("продаж");
      } else {
         return false;
      }
   }

   private int a(GenericContainerScreenHandler var1) {
      int var2 = var1.getRows() * 9;

      for (int var3 = 0; var3 < var2; var3++) {
         ItemStack var4 = var1.getSlot(var3).getStack();
         if (!var4.isEmpty()) {
            String var5 = Registries.ITEM.getId(var4.getItem()).toString();
            if (var5.equals("minecraft:ender_chest") && this.a(var4, "товары на продаже")) {
               return var3;
            }
         }
      }

      for (int var6 = 0; var6 < var2; var6++) {
         ItemStack var7 = var1.getSlot(var6).getStack();
         if (!var7.isEmpty() && this.a(var7, "товары на продаже")) {
            return var6;
         }
      }

      return -1;
   }

   private int b(GenericContainerScreenHandler var1) {
      int var2 = var1.getRows();
      int var3 = (var2 - 1) * 9;

      for (int var4 = 0; var4 < var3; var4++) {
         ItemStack var5 = var1.getSlot(var4).getStack();
         if (!var5.isEmpty()) {
            Lot var6 = PriceParser.a(var5);
            if (var6 != null) {
               for (ItemEntry var8 : AutoBuyConfig.getInstance().entries) {
                  if (ItemMatcher.a(var8, var6)) {
                     return var4;
                  }
               }
            }
         }
      }

      return -1;
   }

   private boolean a(ItemStack var1, String var2) {
      String var3 = var1.getName().getString().toLowerCase(Locale.ROOT);
      if (var3.contains(var2)) {
         return true;
      }

      LoreComponent var4 = var1.get(DataComponentTypes.LORE);
      if (var4 != null) {
         for (Text var6 : var4.lines()) {
            if (var6.getString().toLowerCase(Locale.ROOT).contains(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private void a(int var1, int var2, int var3) {
      if (this.mc.interactionManager != null && this.mc.player != null) {
         this.mc.interactionManager.clickSlot(var1, var2, var3, SlotActionType.PICKUP, this.mc.player);
      }
   }

   private void a(String var1) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal(var1), false);
      }
   }

   enum a {
      IDLE,
      OPEN_AUCTION,
      WAIT_AUCTION,
      OPEN_LISTINGS,
      WAIT_LISTINGS,
      COLLECT,
      FINISH;
   }
}
