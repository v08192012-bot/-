package brokke.cc.core;

import brokke.cc.config.AutoBuyConfig;
import brokke.cc.manager.RelistManager;
import brokke.cc.nav.Marker;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.TextNormalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoBuyEngine {
   public static final AutoBuyEngine INSTANCE = new AutoBuyEngine();
   private static final Random RANDOM = ThreadLocalRandom.current();
   public final PriceAutoParser parser = new PriceAutoParser();
   public final AutoSeller seller = new AutoSeller();
   public final RelistManager relist = new RelistManager();
   public final AutoPay autoPay = new AutoPay();
   private int relistTicks = -1;
   private int parseTicks = -1;
   private boolean resumeAfterParse = false;
   private static final Pattern BALANCE = Pattern.compile(
      "(?iu)(?:баланс|на\\s+счету|на\\s+счёте|на\\s+счете|ваш\\s+счёт|ваш\\s+счет)[^\\d\\-]*([\\d\\s\\u00A0.,]+)"
   );
   private int clickCooldown = 0;
   private long lastHash = 0L;
   private int stallCounter = 0;
   private int refreshCooldown = 0;
   private int openCooldown = 0;
   private int noScreenTicks = 0;
   private volatile String targetItemId = null;
   private volatile String targetName = null;
   private volatile String targetKeyword = null;
   private volatile long targetMaxPrice = 0L;
   private volatile boolean targetRequireCustom = false;
   private volatile List<Marker> targetMarkers = null;
   private int actionCooldown = 0;
   private boolean watching = false;
   private String watchItemId = null;
   private int preBuyCount = 0;
   private int watchdog = 0;
   private PurchaseRecord pendingRecord = null;
   private static final int WATCHDOG_TICKS = 20;
   private static final int SORT_CLICK_DELAY = 6;
   private float targetYaw = Float.NaN;
   private float targetPitch = Float.NaN;
   private int headTurnTimer = 0;
   private int nextHeadTurnDelay = 0;
   private boolean turningHead = false;
   private boolean wasEnabled = false;
   private static final float TURN_SPEED_BASE = 8.0F;
   private float currentTurnSpeed = 8.0F;
   private int randomActionDelay = 0;
   private int focusItemSlot = -1;
   private int mouseHoverTicks = 0;
   private int blinkTimer = 0;
   private int randomPauseTimer = 0;
   private float randomSpeedMultiplier = 1.0F;
   private int randomTickCounter = 0;
   private int refreshCounter = 0;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int AUTOBUY_SPEEDUP = 100;

   private AutoBuyEngine() {
   }

   public static boolean a() {
      return AutoBuyConfig.getInstance().enabled || INSTANCE.parser.c() || INSTANCE.seller.d() || INSTANCE.relist.b();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (this.mc.player != null && this.mc.interactionManager != null) {
         AutoBuyConfig var2 = AutoBuyConfig.getInstance();
         this.randomTickCounter++;
         if (this.randomTickCounter >= 3) {
            this.randomTickCounter = 0;
            this.randomSpeedMultiplier = 0.9F + RANDOM.nextFloat() * 0.3F;
         }

         if (this.randomPauseTimer > 0) {
            this.randomPauseTimer--;
         } else {
            try {
               this.parser.e();
            } catch (Throwable var11) {
            }

            if (!this.parser.c()) {
               if (var2.enabled) {
                  if (!this.wasEnabled) {
                     this.nextHeadTurnDelay = b(2, 20);
                     this.currentTurnSpeed = 8.0F * (0.9F + RANDOM.nextFloat() * 0.2F) * this.randomSpeedMultiplier;
                     this.wasEnabled = true;
                  }

                  this.f();
               } else if (this.wasEnabled) {
                  this.turningHead = false;
                  this.targetYaw = Float.NaN;
                  this.targetPitch = Float.NaN;
                  this.wasEnabled = false;
               }

               if (var2.enabled) {
                  if (this.randomActionDelay > 0) {
                     this.randomActionDelay--;
                  } else {
                     if (var2.autoPay && RANDOM.nextInt(2) == 0) {
                        try {
                           this.autoPay.a();
                        } catch (Throwable var10) {
                        }
                     }

                     try {
                        if (RANDOM.nextInt(2) == 0) {
                           this.relist.d();
                        }
                     } catch (Throwable var9) {
                     }

                     if (!this.relist.b()) {
                        try {
                           if (RANDOM.nextInt(2) == 0) {
                              this.seller.i();
                           }
                        } catch (Throwable var8) {
                        }

                        if (!this.seller.d()) {
                           try {
                              this.h();
                           } catch (Throwable var7) {
                           }

                           try {
                              this.i();
                           } catch (Throwable var6) {
                           }

                           try {
                              this.k();
                           } catch (Throwable var5) {
                           }

                           try {
                              this.d();
                           } catch (Throwable var4) {
                           }

                           this.e();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void d() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      if (var1.autoRefresh && this.mc.currentScreen instanceof GenericContainerScreen var3 && a(var3)) {
         GenericContainerScreenHandler var4 = var3.getScreenHandler();
         int var5 = (var4.getRows() - 1) * 9;
         long var6 = this.b(var4, var5);
         this.refreshCounter++;
         if (this.refreshCounter >= -1) {
            this.refreshCounter = 0;
            if (var6 != this.lastHash) {
               this.lastHash = var6;
               this.stallCounter = 0;
               if (this.refreshCooldown <= 0) {
                  this.a(var4, var4.syncId, var4.getRows());
                  this.refreshCooldown = this.b(AutoBuyConfig.getInstance());
               }
            } else {
               this.stallCounter++;
               if (this.stallCounter >= 1 + RANDOM.nextInt(2) && this.refreshCooldown <= 0) {
                  this.a(var4, var4.syncId, var4.getRows());
                  this.refreshCooldown = this.b(AutoBuyConfig.getInstance());
                  this.stallCounter = 0;
               }
            }
         }
      }
   }

   private void e() {
      if (this.blinkTimer > 0) {
         this.blinkTimer--;
         if (this.blinkTimer == 0) {
            this.focusItemSlot = -1;
            this.mouseHoverTicks = 0;
         }
      } else if (RANDOM.nextInt(40) == 0) {
         this.blinkTimer = b(1, 2);
      }
   }

   private void f() {
      if (this.mc.player != null) {
         if (RANDOM.nextInt(4) == 0) {
            this.currentTurnSpeed = 8.0F * (0.8F + RANDOM.nextFloat() * 0.4F) * this.randomSpeedMultiplier;
         }

         if (this.turningHead) {
            if (this.headTurnTimer > 0) {
               this.headTurnTimer--;
               float var1 = this.mc.player.getYaw();
               float var2 = this.mc.player.getPitch();
               float var3 = MathHelper.wrapDegrees(this.targetYaw - var1);
               float var4 = MathHelper.wrapDegrees(this.targetPitch - var2);
               float var5 = 0.9F + RANDOM.nextFloat() * 0.2F;
               float var6 = Math.min(Math.abs(var3), this.currentTurnSpeed * var5) * Math.signum(var3);
               float var7 = Math.min(Math.abs(var4), this.currentTurnSpeed * 0.9F * var5) * Math.signum(var4);
               if (Math.abs(var3) < 0.5F && Math.abs(var4) < 0.5F) {
                  this.turningHead = false;
                  this.nextHeadTurnDelay = b(2, 30);
               } else {
                  this.mc.player.setYaw(var1 + var6);
                  this.mc.player.setPitch(var2 + var7);
               }
            } else {
               this.turningHead = false;
               this.nextHeadTurnDelay = b(2, 30);
            }
         } else if (this.nextHeadTurnDelay > 0) {
            this.nextHeadTurnDelay--;
         } else {
            this.g();
         }
      }
   }

   private void g() {
      if (this.mc.player != null) {
         float var1 = this.mc.player.getYaw();
         float var2 = this.mc.player.getPitch();
         this.targetYaw = var1 + (RANDOM.nextFloat() * 160.0F - 80.0F);
         this.targetPitch = MathHelper.clamp(var2 + (RANDOM.nextFloat() * 60.0F - 30.0F), -80.0F, 80.0F);
         this.headTurnTimer = b(1, 10);
         this.turningHead = true;
         this.currentTurnSpeed = 8.0F * (0.8F + RANDOM.nextFloat() * 0.4F) * this.randomSpeedMultiplier;
      }
   }

   private void h() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      if (this.resumeAfterParse) {
         this.resumeAfterParse = false;
         var1.enabled = true;
      }

      if (!var1.enabled) {
         this.parseTicks = -1;
         this.relistTicks = -1;
      } else {
         if (var1.autoRelist) {
            if (this.relistTicks < 0) {
               this.relistTicks = Math.max(5, var1.relistIntervalSeconds * 10) + b(-20, 50);
            }

            if (--this.relistTicks <= 0) {
               int var2 = Math.max(5, var1.relistIntervalSeconds * 10);
               this.relistTicks = var2 + b(-var2 / 4, var2 / 3);
               if (RANDOM.nextInt(3) == 0) {
                  this.relistTicks = this.relistTicks + b(5, 20);
               } else {
                  this.relist.a();
               }

               return;
            }
         } else {
            this.relistTicks = -1;
         }

         if (var1.autoParse) {
            if (this.parseTicks < 0) {
               this.parseTicks = Math.max(20, var1.parseIntervalSeconds * 20) + b(-20, 50);
            }

            if (--this.parseTicks <= 0) {
               int var3 = Math.max(20, var1.parseIntervalSeconds * 20);
               this.parseTicks = var3 + b(-var3 / 4, var3 / 3);
               this.resumeAfterParse = true;
               this.parser.a();
               return;
            }
         } else {
            this.parseTicks = -1;
         }

         this.randomActionDelay = Math.max(0, this.c(var1) - 1);
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof GameMessageS2CPacket var3) {
         AutoBuyConfig var4 = AutoBuyConfig.getInstance();
         String var5 = var3.content().getString().toLowerCase(Locale.ROOT);
         if (var5.contains("недостаточно средств")) {
            this.b();
         }

         if (var5.contains("не можете больше выставлять")) {
            this.seller.f();
         }

         if (var5.contains("купил у вас")) {
            this.seller.g();
         }

         if (var5.contains("успешно выставили")) {
            this.seller.e();
         }

         if (var5.contains("подождите") && var5.contains("для использования этой команды")) {
            this.seller.h();
            this.openCooldown = Math.max(this.openCooldown, this.d(var4));
         }

         if (!var5.contains("пополнен") && !var5.contains("купил у вас")) {
            Long var6 = c(var5);
            if (var6 != null) {
               this.autoPay.a(var6);
            }
         }
      }
   }

   private void i() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      if (this.clickCooldown > 0) {
         this.clickCooldown--;
      }

      if (this.refreshCooldown > 0) {
         this.refreshCooldown--;
      }

      if (this.openCooldown > 0) {
         this.openCooldown--;
      }

      if (RANDOM.nextInt(20) == 0) {
         this.randomPauseTimer = b(1, 3);
      } else {
         Screen var2 = this.mc.currentScreen;
         if (var2 instanceof GenericContainerScreen var3) {
            this.noScreenTicks = 0;
            if (!a(var3)) {
               this.stallCounter = 0;
            } else if (this.clickCooldown <= 0) {
               GenericContainerScreenHandler var4 = var3.getScreenHandler();
               int var5 = var4.syncId;
               int var6 = var4.getRows();
               int var7 = (var6 - 1) * 9;
               if (!this.a(var4, var5)) {
                  long var8 = this.b(var4, var7);
                  boolean var10 = var8 != this.lastHash;
                  if (var10) {
                     this.stallCounter = 0;
                     this.lastHash = var8;
                  } else {
                     this.stallCounter++;
                  }

                  if (RANDOM.nextInt(8) == 0) {
                     this.focusItemSlot = RANDOM.nextInt(var7);
                     this.mouseHoverTicks = b(1, 2);
                  }

                  if (this.mouseHoverTicks > 0) {
                     this.mouseHoverTicks--;
                  }

                  ArrayList var11 = new ArrayList();

                  for (int var12 = 0; var12 < var7; var12++) {
                     ItemStack var13 = var4.getSlot(var12).getStack();
                     Lot var14 = PriceParser.a(var12, var13);
                     if (var14 != null && !var14.barrier) {
                        var11.add(var14);
                     }
                  }

                  Lot var17 = null;
                  ItemEntry var18 = null;

                  for (Lot var15 : (Iterable<Lot>)(Iterable<?>)(var11)) {
                     ItemEntry var16 = this.a(var1, var15);
                     if (var16 != null) {
                        var17 = var15;
                        var18 = var16;
                        break;
                     }
                  }

                  if (var17 != null && var18 != null) {
                     this.targetItemId = var17.itemId;
                     this.targetName = var18.displayName;
                     this.targetKeyword = var18.loreKeyword;
                     this.targetMaxPrice = var18.maxUnitPrice;
                     this.targetRequireCustom = var18.requireCustom;
                     this.targetMarkers = var18.c();
                     this.clickCooldown = this.a(var1);
                     if (RANDOM.nextInt(8) == 0) {
                        this.clickCooldown = this.clickCooldown + Math.max(1, this.c(var1));
                        this.targetItemId = null;
                        return;
                     }

                     this.a(var5, var17.slot);
                  }
               }
            }
         } else {
            this.stallCounter = 0;
            if (var2 == null) {
               this.noScreenTicks++;
               if (this.noScreenTicks >= Math.max(1, this.d(var1) / 2)) {
                  this.j();
               }
            } else if (var2 instanceof GameMenuScreen) {
               this.mc.setScreen(null);
               this.noScreenTicks = 0;
            } else {
               this.noScreenTicks = 0;
            }
         }
      }
   }

   private boolean a(GenericContainerScreenHandler var1, int var2) {
      int var3 = AuctionSortMode.a(var1);
      if (var3 < 0) {
         return false;
      } else {
         AuctionSortMode var4 = AuctionSortMode.a(var1.getSlot(var3).getStack());
         if (var4 != null && var4 != AuctionSortMode.NEWEST) {
            this.a(var2, var3);
            this.clickCooldown = Math.max(this.clickCooldown, 6);
            this.lastHash = 0L;
            this.stallCounter = 0;
            return true;
         } else {
            return false;
         }
      }
   }

   private void j() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      if (this.openCooldown <= 0 && this.mc.player != null && this.mc.player.networkHandler != null) {
         this.mc.player.networkHandler.sendChatCommand("ah");
         this.openCooldown = this.d(var1);
      }
   }

   private ItemEntry a(AutoBuyConfig var1, Lot var2) {
      String var3 = this.mc.player.getGameProfile() != null ? this.mc.player.getGameProfile().name() : null;
      if (var3 != null && var2.seller != null && var3.equalsIgnoreCase(var2.seller)) {
         return null;
      }

      if (var1.b(var2.seller)) {
         return null;
      }

      if (var1.minPriceEnabled && var1.minPrice > 0L && var2.unitPrice < var1.minPrice) {
         return null;
      }

      ItemEntry var4 = null;
      int var5 = -1;
      ArrayList var6 = new ArrayList<>(var1.entries);
      if (RANDOM.nextInt(2) == 0) {
         Collections.shuffle(var6, RANDOM);
      }

      for (ItemEntry var8 : (Iterable<ItemEntry>)(Iterable<?>)(var6)) {
         if (var8.enabled && var8.maxUnitPrice > 0L && ItemMatcher.a(var8, var2) && var2.unitPrice <= var8.maxUnitPrice) {
            int var9 = (var8.displayName != null ? var8.displayName.length() * 2 : 0)
               + (var8.loreKeyword != null && !var8.loreKeyword.isBlank() ? b(15, 50) : 0)
               + (var8.matchAnySword ? -b(10, 30) : 0)
               + (var8.requireCustom ? b(10, 30) : 0)
               + (RANDOM.nextInt(20) - 10);
            if (var9 > var5) {
               var5 = var9;
               var4 = var8;
            }
         }
      }

      return var4;
   }

   public static boolean a(GenericContainerScreen var0) {
      GenericContainerScreenHandler var1 = var0.getScreenHandler();
      if (var1.getRows() != 6) {
         return false;
      }

      String var2 = var0.getTitle().getString();
      return var2.startsWith("Аукцион");
   }

   private void a(GenericContainerScreenHandler var1, int var2, int var3) {
      if (this.refreshCooldown <= 0) {
         int var4 = var3 * 9;
         int var5 = -1;
         int var6 = AutoBuyConfig.getInstance().refreshSlot;
         if (var6 >= 0 && var6 < var4) {
            ItemStack var7 = var1.getSlot(var6).getStack();
            if (!var7.isEmpty()) {
               String var8 = Registries.ITEM.getId(var7.getItem()).toString();
               if (var8.equals("minecraft:emerald")) {
                  var5 = var6;
               }
            }
         }

         if (var5 < 0) {
            var5 = this.a(var1, (var3 - 1) * 9, var4, "minecraft:emerald");
         }

         if (var5 >= 0) {
            this.a(var2, var5);
            this.refreshCooldown = this.b(AutoBuyConfig.getInstance());
            this.lastHash = 0L;
            this.stallCounter = 0;
         }
      }
   }

   private int a(GenericContainerScreenHandler var1, int var2, int var3, String var4) {
      ArrayList var5 = new ArrayList();

      for (int var6 = var2; var6 < var3; var6++) {
         var5.add(var6);
      }

      if (RANDOM.nextInt(2) == 0) {
         Collections.shuffle(var5, RANDOM);
      }

      for (int var7 : (Iterable<Integer>)(Iterable<?>)(var5)) {
         ItemStack var8 = var1.getSlot(var7).getStack();
         if (!var8.isEmpty()) {
            String var9 = Registries.ITEM.getId(var8.getItem()).toString();
            if (var9.equals(var4)) {
               return var7;
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

   private void k() {
      AutoBuyConfig var1 = AutoBuyConfig.getInstance();
      if (this.watching) {
         this.watchdog--;
         if (this.watchItemId != null && this.a(this.watchItemId) > this.preBuyCount) {
            String var2 = this.watchItemId;
            if (this.pendingRecord != null) {
               this.pendingRecord.success = true;
               AutoBuyConfig.getInstance().a(this.pendingRecord);
            }

            this.m();
            boolean var3 = AutoBuyConfig.getInstance().autoSell;
            this.l();
            if (var3 && var2 != null) {
               this.seller.b();
            }
         } else if (this.watchdog <= 0) {
            this.l();
         }
      }

      if (this.actionCooldown > 0) {
         this.actionCooldown--;
      } else if (this.mc.currentScreen instanceof GenericContainerScreen var18 && b(var18) && var1.enabled) {
         GenericContainerScreenHandler var4 = var18.getScreenHandler();
         int var5 = var4.syncId;
         Lot var6 = this.a(var4);
         String var7 = this.targetItemId;
         long var8 = this.targetMaxPrice;
         if (var7 == null || var8 <= 0L) {
            this.c(var4, var5);
         } else if (var6 != null && var7.equals(var6.itemId)) {
            String var10 = this.targetKeyword;
            String var11 = this.targetName;
            if (var10 != null && !var10.isBlank()) {
               boolean var12 = TextNormalizer.b(var6.lore, var10);
               boolean var13 = TextNormalizer.b(var6.name, var10);
               if (!var12 && !var13) {
                  this.c(var4, var5);
                  this.targetItemId = null;
                  return;
               }

               if (this.targetRequireCustom && var6.name != null && var6.name.toLowerCase(Locale.ROOT).contains(var10.toLowerCase(Locale.ROOT))) {
                  this.c(var4, var5);
                  this.targetItemId = null;
                  return;
               }
            } else if (var11 != null && !var11.isBlank() && !TextNormalizer.a(var11, var6.name)) {
               this.c(var4, var5);
               this.targetItemId = null;
               return;
            }

            if (this.targetRequireCustom && !var6.hasMarkers) {
               this.c(var4, var5);
               this.targetItemId = null;
            } else {
               List<Marker> var19 = this.targetMarkers;
               if (var19 != null && !var19.isEmpty()) {
                  boolean var20 = var19.stream().anyMatch(var1x -> ItemMatcher.a(var1x, var6));
                  if (!var20) {
                     this.c(var4, var5);
                     this.targetItemId = null;
                     this.targetMarkers = null;
                     return;
                  }
               } else if (this.targetRequireCustom) {
                  this.c(var4, var5);
                  this.targetItemId = null;
                  this.targetMarkers = null;
                  return;
               }

               if (var6.unitPrice > var8) {
                  this.c(var4, var5);
                  this.targetItemId = null;
               } else {
                  int var21 = this.b(var4);
                  if (var21 >= 0) {
                     this.actionCooldown = this.c(var1);
                     if (RANDOM.nextInt(10) == 0) {
                        this.c(var4, var5);
                        this.targetItemId = null;
                        return;
                     }

                     this.a(var5, var21);
                     this.watching = true;
                     this.watchItemId = var6.itemId;
                     this.preBuyCount = this.a(var6.itemId);
                     this.watchdog = 20 + RANDOM.nextInt(10);
                     long var14 = var6.totalPrice > 0L ? var6.totalPrice : var6.unitPrice * Math.max(1, var6.count);
                     String var16 = var6.name != null && !var6.name.isBlank() ? var6.name : var6.itemId;
                     this.pendingRecord = new PurchaseRecord(System.currentTimeMillis(), var6.itemId, var16, var6.seller, var6.unitPrice, var14, var6.count);
                     this.targetItemId = null;
                     this.targetMaxPrice = 0L;
                     this.targetMarkers = null;
                  }
               }
            }
         } else {
            this.c(var4, var5);
            this.targetItemId = null;
         }
      }
   }

   private void c(GenericContainerScreenHandler var1, int var2) {
      this.actionCooldown = this.c(AutoBuyConfig.getInstance());
      this.d(var1, var2);
   }

   public static boolean b(GenericContainerScreen var0) {
      String var1 = var0.getTitle().getString();
      return var1.contains("Покупка предмета") || var1.contains("Подтверждение");
   }

   private Lot a(GenericContainerScreenHandler var1) {
      int var2 = var1.getRows() * 9;
      int var3 = RANDOM.nextInt(Math.max(1, var2 / 2));

      for (int var4 = var3; var4 < var2; var4++) {
         ItemStack var5 = var1.getSlot(var4).getStack();
         if (!var5.isEmpty()) {
            String var6 = Registries.ITEM.getId(var5.getItem()).toString();
            if (!var6.contains("stained_glass") && !var6.contains("concrete") && !var6.contains("wool")) {
               Lot var7 = PriceParser.a(var4, var5);
               if (var7 != null && !var7.barrier) {
                  return var7;
               }
            }
         }
      }

      for (int var8 = 0; var8 < var3; var8++) {
         ItemStack var9 = var1.getSlot(var8).getStack();
         if (!var9.isEmpty()) {
            String var10 = Registries.ITEM.getId(var9.getItem()).toString();
            if (!var10.contains("stained_glass") && !var10.contains("concrete") && !var10.contains("wool")) {
               Lot var11 = PriceParser.a(var8, var9);
               if (var11 != null && !var11.barrier) {
                  return var11;
               }
            }
         }
      }

      return null;
   }

   private int b(GenericContainerScreenHandler var1) {
      int var2 = var1.getRows() * 9;
      int var3 = RANDOM.nextInt(Math.max(1, var2));

      for (int var4 = var3; var4 < var2; var4++) {
         String var5 = Registries.ITEM.getId(var1.getSlot(var4).getStack().getItem()).toString();
         if (var5.contains("lime_stained_glass")
            || var5.contains("green_stained_glass")
            || var5.contains("lime_concrete")
            || var5.contains("green_wool")
            || var5.contains("lime_wool")) {
            return var4;
         }
      }

      for (int var6 = 0; var6 < var3; var6++) {
         String var7 = Registries.ITEM.getId(var1.getSlot(var6).getStack().getItem()).toString();
         if (var7.contains("lime_stained_glass")
            || var7.contains("green_stained_glass")
            || var7.contains("lime_concrete")
            || var7.contains("green_wool")
            || var7.contains("lime_wool")) {
            return var6;
         }
      }

      return -1;
   }

   private int c(GenericContainerScreenHandler var1) {
      int var2 = var1.getRows() * 9;

      for (int var3 = 0; var3 < var2; var3++) {
         String var4 = Registries.ITEM.getId(var1.getSlot(var3).getStack().getItem()).toString();
         if (var4.contains("red_stained_glass") || var4.contains("red_concrete") || var4.contains("red_wool")) {
            return var3;
         }
      }

      return -1;
   }

   private void d(GenericContainerScreenHandler var1, int var2) {
      int var3 = this.c(var1);
      if (var3 < 0) {
         this.mc.player.closeHandledScreen();
      } else {
         this.a(var2, var3);
      }
   }

   public synchronized void b() {
      if (this.watching) {
         if (this.pendingRecord != null) {
            this.pendingRecord.success = false;
            this.pendingRecord.failReason = "недостаточно средств";
            AutoBuyConfig.getInstance().a(this.pendingRecord);
         }

         this.b("§c[AutoBuy] Недостаточно средств — покупка отменена");
         this.l();
      }
   }

   private int a(String var1) {
      if (this.mc.player != null && var1 != null) {
         PlayerInventory var2 = this.mc.player.getInventory();
         int var3 = 0;
         int var4 = var2.size();
         int var5 = RANDOM.nextInt(Math.max(1, var4 / 2));

         for (int var6 = var5; var6 < var4; var6++) {
            ItemStack var7 = var2.getStack(var6);
            if (!var7.isEmpty() && Registries.ITEM.getId(var7.getItem()).toString().equals(var1)) {
               var3 += var7.getCount();
            }
         }

         for (int var8 = 0; var8 < var5; var8++) {
            ItemStack var9 = var2.getStack(var8);
            if (!var9.isEmpty() && Registries.ITEM.getId(var9.getItem()).toString().equals(var1)) {
               var3 += var9.getCount();
            }
         }

         return var3;
      } else {
         return 0;
      }
   }

   private void a(int var1, int var2) {
      if (this.mc.interactionManager != null && this.mc.player != null) {
         this.mc.interactionManager.clickSlot(var1, var2, 0, SlotActionType.PICKUP, this.mc.player);
      }
   }

   private void l() {
      this.watching = false;
      this.watchItemId = null;
      this.preBuyCount = 0;
      this.watchdog = 0;
      this.pendingRecord = null;
   }

   private void m() {
      String var1 = this.pendingRecord != null ? this.pendingRecord.label : (this.watchItemId == null ? "?" : this.watchItemId);
      this.b("§a[AutoBuy] Куплено: §f" + var1);
   }

   private void b(String var1) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal(var1), false);
      }
   }

   private static Long c(String var0) {
      Matcher var1 = BALANCE.matcher(var0);
      if (!var1.find()) {
         return null;
      }

      String var2 = var1.group(1).replaceAll("[\\s\\u00A0]+", "");
      int var3 = var2.indexOf(46);
      int var4 = var2.indexOf(44);
      int var5 = var3 < 0 ? var4 : (var4 < 0 ? var3 : Math.min(var3, var4));
      if (var5 >= 0) {
         var2 = var2.substring(0, var5);
      }

      String var8 = var2.replaceAll("\\D", "");
      if (var8.isEmpty()) {
         return null;
      }

      try {
         return Long.parseLong(var8);
      } catch (NumberFormatException var7) {
         return null;
      }
   }

   public void c() {
      this.refreshCooldown = 0;
      this.lastHash = 0L;
      this.stallCounter = 999;
      this.refreshCounter = 999;
      if (this.mc.currentScreen instanceof GenericContainerScreen var2 && a(var2)) {
         GenericContainerScreenHandler var3 = var2.getScreenHandler();
         this.a(var3, var3.syncId, var3.getRows());
      }
   }

   private int a(AutoBuyConfig var1) {
      int var2 = var1.a(0.18F, 28, 210);
      int var3 = Math.max(0, var1.clickIntervalTicks) * 16;
      return Math.max(1, this.a((var2 + var3 + RANDOM.nextInt(28)) / 100));
   }

   private int b(AutoBuyConfig var1) {
      if (var1.refreshIntervalTicks > 0) {
         return Math.max(1, var1.refreshIntervalTicks / 100);
      }

      int var2 = var1.a(0.45F, 18, 160);
      return Math.max(0, this.a((var2 + RANDOM.nextInt(20)) / 100));
   }

   private int c(AutoBuyConfig var1) {
      int var2 = var1.a(0.7F, 10, 130);
      return Math.max(0, this.a((var2 + RANDOM.nextInt(18)) / 100));
   }

   private int d(AutoBuyConfig var1) {
      int var2 = var1.a(1.0F, 200, 1150);
      return Math.max(1, this.a(var2 + RANDOM.nextInt(60)));
   }

   private int a(int var1) {
      return (int)Math.ceil(Math.max(0, var1) / 50.0);
   }

   private static int b(int var0, int var1) {
      return var0 >= var1 ? var0 : var0 + RANDOM.nextInt(var1 - var0 + 1);
   }
}
