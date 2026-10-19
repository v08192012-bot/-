package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.config.ConfigManager;
import brokke.cc.core.Timer2;
import brokke.cc.setting.ButtonSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.ui.AutoSborScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@NewFunction(name = "AutoSbor", description = "Автоматический сбор предметов", category = Category.MISC)
public class AutoSbor extends Function {
   public static final ItemStack[] TOP_SLOTS = new ItemStack[8];
   public static final ItemStack[] INVENTORY_SLOTS = new ItemStack[27];
   public static final int[] TOP_MIN_DURABILITY = new int[8];
   public static final int[] INVENTORY_MIN_DURABILITY = new int[27];
   public static final int[] TOP_SHARPNESS = new int[8];
   public static final int[] INVENTORY_SHARPNESS = new int[27];
   public static final boolean[] TOP_CRIT_2 = new boolean[8];
   public static final boolean[] INVENTORY_CRIT_2 = new boolean[27];
   public static final int[] TOP_EFFICIENCY = new int[8];
   public static final int[] INVENTORY_EFFICIENCY = new int[27];
   public static final int[] TOP_PROTECTION = new int[8];
   public static final int[] INVENTORY_PROTECTION = new int[27];
   public static final int[] TOP_UNBREAKING = new int[8];
   public static final int[] INVENTORY_UNBREAKING = new int[27];
   public static final int[] TOP_RESPIRATION = new int[8];
   public static final int[] INVENTORY_RESPIRATION = new int[27];
   public static final int[] TOP_DEPTH_STRIDER = new int[8];
   public static final int[] INVENTORY_DEPTH_STRIDER = new int[27];
   private static final Pattern HW_UNIT_PRICE_PATTERN = Pattern.compile("(?i)Цена за 1 ед\\.?:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern HW_TOTAL_PRICE_PATTERN = Pattern.compile("(?i)(?:^|\\n|\\r|[^а-яa-z])Цена:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern COUNT_X = Pattern.compile("(?i)\\bx\\s*(\\d{1,4})\\b");
   private static final Pattern COUNT_PCS = Pattern.compile("(?i)\\b(\\d{1,4})\\s*(?:шт|штук|pcs)\\b");
   private static final Pattern COUNT_LABEL = Pattern.compile("(?i)(?:Количество|Count)[:\\s]+(\\d+)");
   private static final Pattern NUM_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._]\\d{3})+|\\d+)");
   private final Timer2 actionTimer = new Timer2();
   private final Timer2 timeoutTimer = new Timer2();
   private AutoSbor.a state = AutoSbor.a.IDLE;
   private boolean isPriceCheckMode = false;
   private long totalCalculatedCost = 0L;
   private final List<String> priceCheckResults = new ArrayList<>();
   private int currentItemIndex = 0;
   private final Set<Integer> skippedItems = new HashSet<>();
   private int lastAdjustCount = -1;
   private int adjustStuckCount = 0;
   private int auctionRefreshCount = 0;
   private int auctionPageScanned = 0;
   private int filterNotFoundCount = 0;
   private int insufficientFundsCount = 0;
   private final ButtonSetting openGui = new ButtonSetting("Открыть Гуи", () -> {
      MinecraftClient var0 = MinecraftClient.getInstance();
      var0.setScreen(new AutoSborScreen(var0.currentScreen));
   });
   private final ButtonSetting startSbor = new ButtonSetting("Запустить сбор", this::k);
   private final ButtonSetting checkPrice = new ButtonSetting("Проверить стоимость", this::l);
   private static final Pattern DURABILITY_FRACTION = Pattern.compile("(?i)(?:прочность|durability)[:\\s]*(\\d+)\\s*[/\\\\|]\\s*(\\d+)");
   private static final Pattern DURABILITY_PERCENT = Pattern.compile("(?i)(?:прочность|durability)[:\\s]*(\\d+)\\s*%");
   private static final Path AUTOSBOR_CONFIGS_DIR = FabricLoader.getInstance().getConfigDir().resolve("brokke").resolve("autosbor_configs");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public static int a(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_MIN_DURABILITY.length) {
         return TOP_MIN_DURABILITY[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_MIN_DURABILITY.length ? INVENTORY_MIN_DURABILITY[var1] : 0;
      }
   }

   public static void a(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 100);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_MIN_DURABILITY.length) {
         TOP_MIN_DURABILITY[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_MIN_DURABILITY.length) {
         INVENTORY_MIN_DURABILITY[var1] = var3;
      }
   }

   public static int b(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_SHARPNESS.length) {
         return TOP_SHARPNESS[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_SHARPNESS.length ? INVENTORY_SHARPNESS[var1] : 0;
      }
   }

   public static void b(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 9);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_SHARPNESS.length) {
         TOP_SHARPNESS[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_SHARPNESS.length) {
         INVENTORY_SHARPNESS[var1] = var3;
      }
   }

   public static boolean c(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_CRIT_2.length) {
         return TOP_CRIT_2[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_CRIT_2.length ? INVENTORY_CRIT_2[var1] : false;
      }
   }

   public static void a(int var0, int var1, boolean var2) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_CRIT_2.length) {
         TOP_CRIT_2[var1] = var2;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_CRIT_2.length) {
         INVENTORY_CRIT_2[var1] = var2;
      }
   }

   public static int d(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_EFFICIENCY.length) {
         return TOP_EFFICIENCY[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_EFFICIENCY.length ? INVENTORY_EFFICIENCY[var1] : 0;
      }
   }

   public static void c(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 10);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_EFFICIENCY.length) {
         TOP_EFFICIENCY[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_EFFICIENCY.length) {
         INVENTORY_EFFICIENCY[var1] = var3;
      }
   }

   public static int e(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_PROTECTION.length) {
         return TOP_PROTECTION[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_PROTECTION.length ? INVENTORY_PROTECTION[var1] : 0;
      }
   }

   public static void d(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 5);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_PROTECTION.length) {
         TOP_PROTECTION[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_PROTECTION.length) {
         INVENTORY_PROTECTION[var1] = var3;
      }
   }

   public static int f(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_UNBREAKING.length) {
         return TOP_UNBREAKING[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_UNBREAKING.length ? INVENTORY_UNBREAKING[var1] : 0;
      }
   }

   public static void e(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 5);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_UNBREAKING.length) {
         TOP_UNBREAKING[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_UNBREAKING.length) {
         INVENTORY_UNBREAKING[var1] = var3;
      }
   }

   public static int g(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_RESPIRATION.length) {
         return TOP_RESPIRATION[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_RESPIRATION.length ? INVENTORY_RESPIRATION[var1] : 0;
      }
   }

   public static void f(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 3);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_RESPIRATION.length) {
         TOP_RESPIRATION[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_RESPIRATION.length) {
         INVENTORY_RESPIRATION[var1] = var3;
      }
   }

   public static int h(int var0, int var1) {
      if (var0 == 0 && var1 >= 0 && var1 < TOP_DEPTH_STRIDER.length) {
         return TOP_DEPTH_STRIDER[var1];
      } else {
         return var0 == 1 && var1 >= 0 && var1 < INVENTORY_DEPTH_STRIDER.length ? INVENTORY_DEPTH_STRIDER[var1] : 0;
      }
   }

   public static void g(int var0, int var1, int var2) {
      int var3 = Math.clamp(var2, 0, 3);
      if (var0 == 0 && var1 >= 0 && var1 < TOP_DEPTH_STRIDER.length) {
         TOP_DEPTH_STRIDER[var1] = var3;
      } else if (var0 == 1 && var1 >= 0 && var1 < INVENTORY_DEPTH_STRIDER.length) {
         INVENTORY_DEPTH_STRIDER[var1] = var3;
      }
   }

   public static int a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("protection") || var4.contains("защита")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("защита") || var5.contains("protection")) {
               if (!var5.contains("v") && !var5.contains("5")) {
                  if (!var5.contains("iv") && !var5.contains("4")) {
                     if (!var5.contains("iii") && !var5.contains("3")) {
                        if (!var5.contains("ii") && !var5.contains("2")) {
                           if (!var5.contains("i") && !var5.contains("1")) {
                              continue;
                           }

                           return 1;
                        }

                        return 2;
                     }

                     return 3;
                  }

                  return 4;
               }

               return 5;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static int b(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("unbreaking") || var4.contains("прочность")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("прочность") || var5.contains("unbreaking")) {
               if (!var5.contains("v") && !var5.contains("5")) {
                  if (!var5.contains("iv") && !var5.contains("4")) {
                     if (!var5.contains("iii") && !var5.contains("3")) {
                        if (!var5.contains("ii") && !var5.contains("2")) {
                           if (!var5.contains("i") && !var5.contains("1")) {
                              continue;
                           }

                           return 1;
                        }

                        return 2;
                     }

                     return 3;
                  }

                  return 4;
               }

               return 5;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static int c(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("respiration") || var4.contains("дыхание")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("дыхание") || var5.contains("respiration")) {
               if (!var5.contains("iii") && !var5.contains("3")) {
                  if (!var5.contains("ii") && !var5.contains("2")) {
                     if (!var5.contains("i") && !var5.contains("1")) {
                        continue;
                     }

                     return 1;
                  }

                  return 2;
               }

               return 3;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static int d(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("depth_strider") || var4.contains("ходьба") || var4.contains("шагоход")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("ходьба") || var5.contains("шагоход") || var5.contains("strider")) {
               if (!var5.contains("iii") && !var5.contains("3")) {
                  if (!var5.contains("ii") && !var5.contains("2")) {
                     if (!var5.contains("i") && !var5.contains("1")) {
                        continue;
                     }

                     return 1;
                  }

                  return 2;
               }

               return 3;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public AutoSbor() {
      this.addSettings(this.openGui, this.startSbor, this.checkPrice);
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.m();
   }

   public void k() {
      if (!this.isActive()) {
         this.toggle();
      }

      this.isPriceCheckMode = false;
      this.state = AutoSbor.a.SEARCHING;
      this.currentItemIndex = 0;
      this.skippedItems.clear();
      this.lastAdjustCount = -1;
      this.adjustStuckCount = 0;
      this.auctionRefreshCount = 0;
      this.auctionPageScanned = 0;
      this.filterNotFoundCount = 0;
      this.insufficientFundsCount = 0;
      this.actionTimer.a();
      this.timeoutTimer.a();
      this.b("§aAutoSbor запущен!§r");
   }

   public void l() {
      if (!this.isActive()) {
         this.toggle();
      }

      this.isPriceCheckMode = true;
      this.state = AutoSbor.a.SEARCHING;
      this.currentItemIndex = 0;
      this.skippedItems.clear();
      this.totalCalculatedCost = 0L;
      this.priceCheckResults.clear();
      this.lastAdjustCount = -1;
      this.adjustStuckCount = 0;
      this.auctionRefreshCount = 0;
      this.auctionPageScanned = 0;
      this.filterNotFoundCount = 0;
      this.insufficientFundsCount = 0;
      this.actionTimer.a();
      this.timeoutTimer.a();
      this.b("§6[AutoSbor] §fПроверка стоимости сета запущена...§r");
   }

   public void m() {
      this.state = AutoSbor.a.IDLE;
      this.isPriceCheckMode = false;
      this.currentItemIndex = 0;
      this.skippedItems.clear();
      this.totalCalculatedCost = 0L;
      this.priceCheckResults.clear();
      this.lastAdjustCount = -1;
      this.adjustStuckCount = 0;
      this.auctionRefreshCount = 0;
      this.auctionPageScanned = 0;
      this.filterNotFoundCount = 0;
      this.insufficientFundsCount = 0;
      this.actionTimer.a();
      this.timeoutTimer.a();
   }

   public boolean n() {
      return this.state != AutoSbor.a.IDLE;
   }

   public boolean isPriceCheckMode() {
      return this.isPriceCheckMode;
   }

   public static String a(long var0) {
      return String.format(Locale.US, "%,d", var0).replace(',', ' ');
   }

   public String p() {
      if (!this.n()) {
         return "";
      }

      List var1 = this.w();
      int var2 = var1.size();
      int var3 = Math.min(var2, this.currentItemIndex + 1);
      String var4 = this.currentItemIndex >= 0 && this.currentItemIndex < var2 ? Z(((AutoSbor.b)var1.get(this.currentItemIndex)).getStack()) : "";
      return this.isPriceCheckMode ? "Чек: " + var4 + " (" + var3 + "/" + var2 + ")" : "Сбор: " + var4 + " (" + var3 + "/" + var2 + ")";
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (this.isActive() && this.state != AutoSbor.a.IDLE && var1.getPacket() instanceof GameMessageS2CPacket var2) {
         String var8 = var2.content().getString();
         String var4 = h(var8).toLowerCase(Locale.ROOT);
         if (var4.contains("по вашему запросу не было найдено ни одного фильтра") || var4.contains("не было найдено ни одного фильтра")) {
            this.filterNotFoundCount++;
            if (this.filterNotFoundCount >= 3) {
               this.filterNotFoundCount = 0;
               this.skippedItems.add(this.currentItemIndex);
               List var5 = this.w();
               String var6 = this.currentItemIndex >= 0 && this.currentItemIndex < var5.size()
                  ? Z(((AutoSbor.b)var5.get(this.currentItemIndex)).getStack())
                  : "предмет";
               if (this.isPriceCheckMode) {
                  String var7 = "§7- §f" + var6 + ": §cфильтр не найден на аукционе§r";
                  this.priceCheckResults.add(var7);
                  this.b(var7);
                  this.currentItemIndex++;
               } else {
                  this.b("§cФильтр для §f" + var6 + " §cне найден (3/3), пропускаем!§r");
               }

               if (mc.player != null && mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               this.state = AutoSbor.a.SEARCHING;
               this.actionTimer.a();
            }
         }

         if (var4.contains("у вас недостаточно средств для совершения покупки") || var4.contains("недостаточно средств") && var4.contains("покупк")) {
            this.insufficientFundsCount++;
            if (this.insufficientFundsCount >= 2) {
               this.insufficientFundsCount = 0;
               this.b("§cНедостаточно средств для совершения покупки! Авто-Сбор остановлен.§r");
               if (mc.player != null && mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               this.m();
               if (this.isActive()) {
                  this.toggle();
               }
            } else {
               this.b("§eПредупреждение: недостаточно средств для совершения покупки (1/2)!§r");
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && this.isActive() && this.state != AutoSbor.a.IDLE) {
         switch (this.state) {
            case SEARCHING:
               this.s();
               break;
            case WAIT_AUCTION_SCREEN:
               this.t();
               break;
            case WAIT_PURCHASE_DIALOG:
               this.u();
               break;
            case WAIT_PURCHASE_COMPLETE:
               this.v();
         }
      }
   }

   private void s() {
      if (this.actionTimer.a(600L)) {
         if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         List var1 = this.w();
         if (var1.isEmpty()) {
            this.b("§cВ AutoSbor нет настроенных предметов!§r");
            this.m();
         } else if (this.isPriceCheckMode) {
            if (this.currentItemIndex < var1.size()) {
               AutoSbor.b var8 = (AutoSbor.b)var1.get(this.currentItemIndex);
               String var11 = var8.getQuery();
               if (mc.player != null && mc.player.networkHandler != null) {
                  mc.player.networkHandler.sendChatCommand("ah search " + var11);
               }

               this.state = AutoSbor.a.WAIT_AUCTION_SCREEN;
               this.auctionRefreshCount = 0;
               this.auctionPageScanned = 0;
               this.actionTimer.a();
               this.timeoutTimer.a();
            } else {
               this.b("§7----------------------------------------§r");
               this.b("§6[AutoSbor] §a§lПроверка стоимости сета завершена!§r");

               for (String var10 : this.priceCheckResults) {
                  this.b(var10);
               }

               String var7 = a(this.totalCalculatedCost);
               this.b("§6[AutoSbor] §eИтоговая стоимость для сбора сета: §a§l" + var7 + " монет§r");
               this.b("§7----------------------------------------§r");
               this.m();
            }
         } else {
            AutoSbor.b var2 = null;

            for (int var3 = 0; var3 < var1.size(); var3++) {
               if (!this.skippedItems.contains(var3)) {
                  AutoSbor.b var4 = (AutoSbor.b)var1.get(var3);
                  int var5 = j(var4.getStack());
                  if (var5 < var4.getTargetCount()) {
                     var2 = var4;
                     this.currentItemIndex = var3;
                     break;
                  }
               }
            }

            if (var2 == null) {
               this.b("§aВсе предметы успешно собраны!§r");
               this.m();
            } else {
               String var9 = var2.getQuery();
               if (mc.player != null && mc.player.networkHandler != null) {
                  mc.player.networkHandler.sendChatCommand("ah search " + var9);
               }

               this.state = AutoSbor.a.WAIT_AUCTION_SCREEN;
               this.auctionRefreshCount = 0;
               this.auctionPageScanned = 0;
               this.actionTimer.a();
               this.timeoutTimer.a();
            }
         }
      }
   }

   private void t() {
      List var1 = this.w();
      if (this.currentItemIndex >= 0 && this.currentItemIndex < var1.size()) {
         AutoSbor.b var2 = (AutoSbor.b)var1.get(this.currentItemIndex);
         if (this.timeoutTimer.a(5000L)) {
            if (this.isPriceCheckMode) {
               String var14 = "§7- §f" + Z(var2.getStack()) + " §7x" + var2.getTargetCount() + ": §cтаймаут поиска§r";
               this.priceCheckResults.add(var14);
               this.b(var14);
               this.currentItemIndex++;
               this.auctionPageScanned = 0;
               if (mc.player != null && mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               this.state = AutoSbor.a.SEARCHING;
               this.actionTimer.a();
            } else {
               this.b("§cПревышено время ожидания аукциона!§r");
               this.state = AutoSbor.a.SEARCHING;
               this.actionTimer.a();
            }
         } else if (this.actionTimer.a(500L) && mc.currentScreen instanceof GenericContainerScreen var3) {
            String var15 = var3.getTitle() != null ? var3.getTitle().getString().toLowerCase(Locale.ROOT) : "";
            if (var15.contains("аукцион") || var15.contains("auction") || var15.contains("поиск") || var15.contains("ah")) {
               ScreenHandler var5 = var3.getScreenHandler();
               if (var5 == null) {
                  return;
               }

               int var6 = Math.min(45, var5.slots.size());
               boolean var7 = false;

               for (int var8 = 0; var8 < var6; var8++) {
                  ItemStack var9 = var5.getSlot(var8).getStack();
                  if (var9 != null && !var9.isEmpty() && !this.at(var9)) {
                     var7 = true;
                     break;
                  }
               }

               if (!var7 && !this.timeoutTimer.a(1000L)) {
                  return;
               }

               Slot var16 = null;
               int var17 = Integer.MAX_VALUE;

               for (int var10 = 0; var10 < var6; var10++) {
                  Slot var11 = var5.getSlot(var10);
                  ItemStack var12 = var11.getStack();
                  if (var12 != null && !var12.isEmpty() && !this.at(var12) && this.a(var2, var12)) {
                     if (var2.getMinDurability() > 0 && e(var2.getStack())) {
                        int var13 = f(var12);
                        if (var13 < var2.getMinDurability()) {
                           continue;
                        }
                     }

                     if (n(var2.getStack())) {
                        if (var2.getSharpness() > 0) {
                           int var24 = g(var12);
                           if (var24 < var2.getSharpness()) {
                              continue;
                           }
                        }

                        if (var2.isCrit2() && !h(var12)) {
                           continue;
                        }
                     }

                     if (m(var2.getStack()) && var2.getEfficiency() > 0) {
                        int var25 = i(var12);
                        if (var25 < var2.getEfficiency()) {
                           continue;
                        }
                     }

                     if (V(var2.getStack())) {
                        if (var2.getProtection() > 0) {
                           int var26 = a(var12);
                           if (var26 < var2.getProtection()) {
                              continue;
                           }
                        }

                        if (var2.getUnbreaking() > 0) {
                           int var27 = b(var12);
                           if (var27 < var2.getUnbreaking()) {
                              continue;
                           }
                        }

                        if (var2.getRespiration() > 0) {
                           int var28 = c(var12);
                           if (var28 < var2.getRespiration()) {
                              continue;
                           }
                        }
                     }

                     if (W(var2.getStack())) {
                        if (var2.getProtection() > 0) {
                           int var29 = a(var12);
                           if (var29 < var2.getProtection()) {
                              continue;
                           }
                        }

                        if (var2.getUnbreaking() > 0) {
                           int var30 = b(var12);
                           if (var30 < var2.getUnbreaking()) {
                              continue;
                           }
                        }
                     }

                     if (X(var2.getStack())) {
                        if (var2.getProtection() > 0) {
                           int var31 = a(var12);
                           if (var31 < var2.getProtection()) {
                              continue;
                           }
                        }

                        if (var2.getUnbreaking() > 0) {
                           int var32 = b(var12);
                           if (var32 < var2.getUnbreaking()) {
                              continue;
                           }
                        }
                     }

                     if (Y(var2.getStack())) {
                        if (var2.getProtection() > 0) {
                           int var33 = a(var12);
                           if (var33 < var2.getProtection()) {
                              continue;
                           }
                        }

                        if (var2.getUnbreaking() > 0) {
                           int var34 = b(var12);
                           if (var34 < var2.getUnbreaking()) {
                              continue;
                           }
                        }

                        if (var2.getDepthStrider() > 0) {
                           int var35 = d(var12);
                           if (var35 < var2.getDepthStrider()) {
                              continue;
                           }
                        }
                     }

                     int var36 = this.aq(var12);
                     if (var36 > 0 && var36 < var17) {
                        var17 = var36;
                        var16 = var11;
                     }
                  }
               }

               if (var16 == null) {
                  Slot var18 = this.b(var5);
                  if (var18 != null && this.auctionPageScanned < 7) {
                     this.auctionPageScanned++;
                     if (mc.interactionManager != null && mc.player != null) {
                        mc.interactionManager.clickSlot(var5.syncId, var18.id, 0, SlotActionType.PICKUP, mc.player);
                     }

                     this.actionTimer.a();
                     this.timeoutTimer.a();
                     return;
                  }
               }

               if (this.isPriceCheckMode) {
                  if (var16 != null) {
                     long var20 = (long)var17 * var2.getTargetCount();
                     this.totalCalculatedCost += var20;
                     String var23 = "§7- §f" + Z(var2.getStack()) + " §7x" + var2.getTargetCount() + ": §e" + a(var20) + " монет §7(" + a(var17) + "/шт)§r";
                     this.priceCheckResults.add(var23);
                     this.b(var23);
                  } else {
                     String var21 = var2.getMinDurability() > 0 && e(var2.getStack())
                        ? "§cне найден (прочность ≥" + var2.getMinDurability() + "%)§r"
                        : "§cне найден на аукционе§r";
                     String var22 = "§7- §f" + Z(var2.getStack()) + " §7x" + var2.getTargetCount() + ": " + var21;
                     this.priceCheckResults.add(var22);
                     this.b(var22);
                  }

                  this.currentItemIndex++;
                  this.auctionPageScanned = 0;
                  if (mc.player != null && mc.currentScreen != null) {
                     mc.player.closeHandledScreen();
                  }

                  this.state = AutoSbor.a.SEARCHING;
                  this.actionTimer.a();
                  return;
               }

               if (var16 == null) {
                  Slot var19 = this.a(var5);
                  if (var19 != null && this.auctionRefreshCount < 15) {
                     this.auctionRefreshCount++;
                     if (mc.interactionManager != null && mc.player != null) {
                        mc.interactionManager.clickSlot(var5.syncId, var19.id, 0, SlotActionType.PICKUP, mc.player);
                     }

                     this.actionTimer.a();
                     this.timeoutTimer.a();
                     return;
                  }

                  if (var2.getMinDurability() > 0 && e(var2.getStack())) {
                     this.b("§cПредмет с нужной прочностью не найден: §f" + var2.getQuery() + " (≥" + var2.getMinDurability() + "%)§r");
                  } else {
                     this.b("§cПредмет не найден на аукционе: §f" + var2.getQuery() + "§r");
                  }

                  this.skippedItems.add(this.currentItemIndex);
                  this.auctionPageScanned = 0;
                  this.state = AutoSbor.a.SEARCHING;
                  this.actionTimer.a();
                  return;
               }

               if (mc.interactionManager != null && mc.player != null) {
                  mc.interactionManager.clickSlot(var5.syncId, var16.id, 1, SlotActionType.PICKUP, mc.player);
               }

               this.state = AutoSbor.a.WAIT_PURCHASE_DIALOG;
               this.lastAdjustCount = -1;
               this.adjustStuckCount = 0;
               this.auctionRefreshCount = 0;
               this.auctionPageScanned = 0;
               this.actionTimer.a();
               this.timeoutTimer.a();
            }
         }
      } else {
         this.state = AutoSbor.a.SEARCHING;
         this.actionTimer.a();
      }
   }

   private void u() {
      if (this.timeoutTimer.a(10000L)) {
         this.state = AutoSbor.a.SEARCHING;
         this.actionTimer.a();
      } else if (this.actionTimer.a(150L) && mc.currentScreen instanceof HandledScreen var1) {
         ScreenHandler var20 = var1.getScreenHandler();
         if (var20 == null) {
            return;
         }

         int var3 = var20.slots.size() - 36;
         if (var3 >= 45) {
            if (this.timeoutTimer.a(2500L)) {
               this.state = AutoSbor.a.WAIT_AUCTION_SCREEN;
               this.actionTimer.a();
            }

            return;
         }

         List var4 = this.w();
         if (this.currentItemIndex < 0 || this.currentItemIndex >= var4.size()) {
            this.state = AutoSbor.a.SEARCHING;
            return;
         }

         AutoSbor.b var5 = (AutoSbor.b)var4.get(this.currentItemIndex);
         int var6 = var5.getTargetCount() - j(var5.getStack());
         if (var6 <= 0) {
            this.state = AutoSbor.a.WAIT_PURCHASE_COMPLETE;
            this.actionTimer.a();
            return;
         }

         int var7 = Math.max(1, Math.min(27, var3));
         boolean var8 = false;
         int var9 = var3 == 5 ? 2 : (var3 == 9 ? 4 : 4);
         int var10 = -1;
         int var11 = -1;
         int var12 = -1;
         int var13 = -1;
         int var14 = -1;
         ArrayList var15 = new ArrayList();
         ArrayList var16 = new ArrayList();

         for (int var17 = 0; var17 < var7 && var17 < var20.slots.size(); var17++) {
            ItemStack var18 = var20.getSlot(var17).getStack();
            if (var18 != null && !var18.isEmpty()) {
               if (var18.isOf(Items.LIME_CONCRETE)
                  || var18.isOf(Items.LIME_TERRACOTTA)
                  || var18.isOf(Items.LIME_WOOL)
                  || var18.isOf(Items.GREEN_CONCRETE)
                  || var18.isOf(Items.GREEN_TERRACOTTA)
                  || var18.isOf(Items.GREEN_WOOL)
                  || var18.isOf(Items.LIME_DYE)
                  || var18.isOf(Items.GREEN_DYE)) {
                  var8 = true;
                  var15.add(var17);
                  String var25 = as(var18).toLowerCase(Locale.ROOT);
                  if (var25.contains("10")) {
                     var10 = var17;
                  } else if (var25.contains("1")) {
                     var11 = var17;
                  }
               } else if (var18.isOf(Items.RED_CONCRETE) || var18.isOf(Items.RED_TERRACOTTA) || var18.isOf(Items.RED_WOOL) || var18.isOf(Items.RED_DYE)) {
                  var8 = true;
                  var16.add(var17);
                  String var19 = as(var18).toLowerCase(Locale.ROOT);
                  if (var19.contains("10")) {
                     var12 = var17;
                  } else if (var19.contains("1")) {
                     var13 = var17;
                  }
               } else if (var18.isOf(Items.LIME_STAINED_GLASS_PANE)
                  || var18.isOf(Items.GREEN_STAINED_GLASS_PANE)
                  || var18.isOf(Items.EMERALD_BLOCK)
                  || var18.isOf(Items.LIME_STAINED_GLASS)
                  || var18.isOf(Items.GREEN_STAINED_GLASS)) {
                  var14 = var17;
               } else if (!var18.isOf(Items.GRAY_STAINED_GLASS_PANE)
                  && !var18.isOf(Items.BLACK_STAINED_GLASS_PANE)
                  && !var18.isOf(Items.LIGHT_GRAY_STAINED_GLASS_PANE)
                  && !var18.isOf(Items.RED_STAINED_GLASS_PANE)
                  && !var18.isOf(Items.RED_STAINED_GLASS)
                  && !var18.isOf(Items.BARRIER)) {
                  var9 = var17;
               }
            }
         }

         if (var15.size() >= 2) {
            if (var11 == -1 && var10 == -1) {
               var11 = (Integer)var15.get(0);
               var10 = (Integer)var15.get(1);
            } else if (var11 == -1) {
               var11 = (Integer)var15.get(0);
            } else if (var10 == -1) {
               var10 = (Integer)var15.get(var15.size() - 1);
            }
         } else if (var15.size() == 1 && var11 == -1) {
            var11 = (Integer)var15.get(0);
         }

         if (var16.size() >= 2) {
            if (var13 == -1 && var12 == -1) {
               var12 = (Integer)var16.get(0);
               var13 = (Integer)var16.get(1);
            } else if (var13 == -1) {
               var13 = (Integer)var16.get(var16.size() - 1);
            } else if (var12 == -1) {
               var12 = (Integer)var16.get(0);
            }
         } else if (var16.size() == 1 && var13 == -1) {
            var13 = (Integer)var16.get(0);
         }

         if (var8 || var3 <= 9) {
            Slot var21 = var20.getSlot(var9);
            ItemStack var23 = var21 != null ? var21.getStack() : ItemStack.EMPTY;
            int var26 = this.a(var23, ar(var23));
            if (var26 <= 0) {
               var26 = Math.max(1, var23.getCount());
            }

            if (var26 == this.lastAdjustCount) {
               this.adjustStuckCount++;
            } else {
               this.lastAdjustCount = var26;
               this.adjustStuckCount = 0;
            }

            if (var26 > var6) {
               if (this.adjustStuckCount >= 3) {
                  if (mc.player != null) {
                     mc.player.closeHandledScreen();
                  }

                  this.state = AutoSbor.a.SEARCHING;
                  this.actionTimer.a();
                  return;
               }

               if (var26 - var6 >= 10 && var12 != -1 && var12 < var20.slots.size()) {
                  if (mc.interactionManager != null && mc.player != null) {
                     mc.interactionManager.clickSlot(var20.syncId, var12, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.actionTimer.a();
                  this.timeoutTimer.a();
                  return;
               }

               if (var13 != -1 && var13 < var20.slots.size()) {
                  if (mc.interactionManager != null && mc.player != null) {
                     mc.interactionManager.clickSlot(var20.syncId, var13, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.actionTimer.a();
                  this.timeoutTimer.a();
                  return;
               }

               if (mc.player != null) {
                  mc.player.closeHandledScreen();
               }

               this.state = AutoSbor.a.SEARCHING;
               this.actionTimer.a();
               return;
            }

            if (var26 < var6 && this.adjustStuckCount < 3) {
               if (var6 - var26 >= 10 && var10 != -1 && var10 < var20.slots.size()) {
                  if (mc.interactionManager != null && mc.player != null) {
                     mc.interactionManager.clickSlot(var20.syncId, var10, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.actionTimer.a();
                  this.timeoutTimer.a();
                  return;
               }

               if (var11 != -1 && var11 < var20.slots.size()) {
                  if (mc.interactionManager != null && mc.player != null) {
                     mc.interactionManager.clickSlot(var20.syncId, var11, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.actionTimer.a();
                  this.timeoutTimer.a();
                  return;
               }
            }

            if (var26 <= var6) {
               if (mc.interactionManager != null && mc.player != null) {
                  mc.interactionManager.clickSlot(var20.syncId, var9, 0, SlotActionType.PICKUP, mc.player);
               }

               this.state = AutoSbor.a.WAIT_PURCHASE_COMPLETE;
               this.actionTimer.a();
               this.timeoutTimer.a();
               return;
            }
         }

         if (var14 != -1) {
            int var22 = 1;
            if (var9 != -1 && var9 < var20.slots.size()) {
               ItemStack var24 = var20.getSlot(var9).getStack();
               if (var24 != null && !var24.isEmpty()) {
                  var22 = this.a(var24, ar(var24));
                  if (var22 <= 0) {
                     var22 = Math.max(1, var24.getCount());
                  }
               }
            }

            if (var22 > var6) {
               if (mc.player != null) {
                  mc.player.closeHandledScreen();
               }

               this.state = AutoSbor.a.SEARCHING;
               this.actionTimer.a();
               return;
            }

            if (mc.interactionManager != null && mc.player != null) {
               mc.interactionManager.clickSlot(var20.syncId, var14, 0, SlotActionType.PICKUP, mc.player);
            }

            this.state = AutoSbor.a.WAIT_PURCHASE_COMPLETE;
            this.actionTimer.a();
            this.timeoutTimer.a();
         }
      }
   }

   private void v() {
      if (this.actionTimer.a(500L)) {
         if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         List var1 = this.w();
         if (this.currentItemIndex >= 0 && this.currentItemIndex < var1.size()) {
            AutoSbor.b var2 = (AutoSbor.b)var1.get(this.currentItemIndex);
            int var3 = j(var2.getStack());
            if (var3 >= var2.getTargetCount()) {
               this.b("§aКуплено: §f" + var2.getQuery() + " (" + var3 + "/" + var2.getTargetCount() + ")§r");
            }
         }

         this.state = AutoSbor.a.SEARCHING;
         this.actionTimer.a();
      }
   }

   public static boolean e(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (var0.isOf(Items.ELYTRA)) {
         return true;
      } else if (!var0.isIn(ItemTags.HEAD_ARMOR)
         && !var0.isIn(ItemTags.CHEST_ARMOR)
         && !var0.isIn(ItemTags.LEG_ARMOR)
         && !var0.isIn(ItemTags.FOOT_ARMOR)
         && !var0.isIn(ItemTags.SWORDS)
         && !var0.isIn(ItemTags.PICKAXES)
         && !var0.isIn(ItemTags.AXES)) {
         return !B(var0) && !C(var0) && !D(var0) && !E(var0) && !aj(var0) && !l(var0) && !k(var0)
            ? G(var0) || H(var0) || I(var0) || J(var0) || F(var0) || K(var0)
            : true;
      } else {
         return true;
      }
   }

   public static int f(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (String var3 : ar(var0)) {
            String var4 = h(var3);
            Matcher var5 = DURABILITY_FRACTION.matcher(var4);
            if (var5.find()) {
               int var6 = j(var5.group(1));
               int var7 = j(var5.group(2));
               if (var7 > 0 && var6 >= 0) {
                  return Math.clamp((int)Math.round(var6 * 100.0 / var7), 0, 100);
               }
            }

            Matcher var10 = DURABILITY_PERCENT.matcher(var4);
            if (var10.find()) {
               int var11 = j(var10.group(1));
               if (var11 >= 0) {
                  return Math.clamp(var11, 0, 100);
               }
            }
         }

         if (var0.getMaxDamage() > 0) {
            int var8 = var0.getMaxDamage();
            int var9 = var8 - var0.getDamage();
            return Math.clamp((int)Math.round(var9 * 100.0 / var8), 0, 100);
         } else {
            return 100;
         }
      } else {
         return 100;
      }
   }

   public static int g(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("sharpness") || var4.contains("острота")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("острота") || var5.contains("sharpness")) {
               if (!var5.contains("x") && !var5.contains("10")) {
                  if (!var5.contains("ix") && !var5.contains("9")) {
                     if (!var5.contains("viii") && !var5.contains("8")) {
                        if (!var5.contains("vii") && !var5.contains("7")) {
                           if (!var5.contains("vi") && !var5.contains("6")) {
                              if (!var5.contains("v") && !var5.contains("5")) {
                                 if (!var5.contains("iv") && !var5.contains("4")) {
                                    if (!var5.contains("iii") && !var5.contains("3")) {
                                       if (!var5.contains("ii") && !var5.contains("2")) {
                                          if (!var5.contains("i") && !var5.contains("1")) {
                                             continue;
                                          }

                                          return 1;
                                       }

                                       return 2;
                                    }

                                    return 3;
                                 }

                                 return 4;
                              }

                              return 5;
                           }

                           return 6;
                        }

                        return 7;
                     }

                     return 8;
                  }

                  return 9;
               }

               return 10;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static boolean h(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (String var3 : ar(var0)) {
            String var4 = h(var3).toLowerCase(Locale.ROOT);
            if (var4.contains("критический ii")
               || var4.contains("критический 2")
               || var4.contains("крит ii")
               || var4.contains("крит 2")
               || var4.contains("критический")
               || var4.contains("крит")) {
               return true;
            }
         }

         NbtComponent var5 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var5 != null) {
            String var6 = var5.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var6.contains("критический") || var6.contains("critical2") || var6.contains("crit2") || var6.contains("крит")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static int i(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var1 != null) {
            for (Entry var3 : var1.getEnchantmentEntries()) {
               String var4 = ((RegistryEntry<?>)var3.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var4.contains("efficiency") || var4.contains("эффективность")) {
                  return var3.getIntValue();
               }
            }
         }

         for (String var8 : ar(var0)) {
            String var5 = h(var8).toLowerCase(Locale.ROOT);
            if (var5.contains("эффективность") || var5.contains("efficiency")) {
               if (!var5.contains("x") && !var5.contains("10")) {
                  if (!var5.contains("ix") && !var5.contains("9")) {
                     if (!var5.contains("viii") && !var5.contains("8")) {
                        if (!var5.contains("vii") && !var5.contains("7")) {
                           if (!var5.contains("vi") && !var5.contains("6")) {
                              if (!var5.contains("v") && !var5.contains("5")) {
                                 if (!var5.contains("iv") && !var5.contains("4")) {
                                    if (!var5.contains("iii") && !var5.contains("3")) {
                                       if (!var5.contains("ii") && !var5.contains("2")) {
                                          if (!var5.contains("i") && !var5.contains("1")) {
                                             continue;
                                          }

                                          return 1;
                                       }

                                       return 2;
                                    }

                                    return 3;
                                 }

                                 return 4;
                              }

                              return 5;
                           }

                           return 6;
                        }

                        return 7;
                     }

                     return 8;
                  }

                  return 9;
               }

               return 10;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private Slot a(ScreenHandler var1) {
      if (var1 == null) {
         return null;
      }

      int var2 = var1.slots.size() - 36;

      for (int var3 = 45; var3 < var2 && var3 < var1.slots.size(); var3++) {
         Slot var8 = var1.getSlot(var3);
         ItemStack var5 = var8.getStack();
         if (var5 != null && !var5.isEmpty()) {
            String var6 = h(as(var5)).toLowerCase(Locale.ROOT);
            if (var6.contains("обнов") || var6.contains("refresh")) {
               return var8;
            }

            if ((var5.isOf(Items.LIME_DYE) || var5.isOf(Items.GREEN_DYE)) && !var6.contains("след") && !var6.contains("далее") && !var6.contains("вперед")) {
               return var8;
            }
         }
      }

      if (var1.slots.size() > 49) {
         Slot var7 = var1.getSlot(49);
         if (var7 != null && !var7.getStack().isEmpty()) {
            String var4 = h(as(var7.getStack())).toLowerCase(Locale.ROOT);
            if (var4.contains("обнов") || var4.contains("refresh")) {
               return var7;
            }
         }
      }

      return null;
   }

   private Slot b(ScreenHandler var1) {
      if (var1 == null) {
         return null;
      }

      int var2 = var1.slots.size() - 36;

      for (int var3 = 45; var3 < var2 && var3 < var1.slots.size(); var3++) {
         Slot var4 = var1.getSlot(var3);
         ItemStack var5 = var4.getStack();
         if (var5 != null && !var5.isEmpty()) {
            String var6 = h(as(var5)).toLowerCase(Locale.ROOT);
            if (!var6.contains("назад") && !var6.contains("prev") && !var6.contains("предыдущ")) {
               if (var6.contains("след")
                  || var6.contains("вперед")
                  || var6.contains("вперёд")
                  || var6.contains("далее")
                  || var6.contains("next")
                  || var6.contains("▶")
                  || var6.contains("►")
                  || var6.contains("→")
                  || var6.contains("»")) {
                  return var4;
               }

               for (String var9 : ar(var5)) {
                  String var10 = h(var9).toLowerCase(Locale.ROOT);
                  if (var10.contains("след")
                     || var10.contains("вперед")
                     || var10.contains("вперёд")
                     || var10.contains("далее")
                     || var10.contains("next")
                     || var10.contains("▶")
                     || var10.contains("►")
                     || var10.contains("→")
                     || var10.contains("»")) {
                     return var4;
                  }
               }
            }
         }
      }

      if (var1.slots.size() > 53) {
         Slot var11 = var1.getSlot(53);
         if (var11 != null && !var11.getStack().isEmpty() && var11.getStack().isOf(Items.ARROW)) {
            return var11;
         }
      }

      return null;
   }

   private List<AutoSbor.b> w() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < TOP_SLOTS.length; var2++) {
         ItemStack var3 = TOP_SLOTS[var2];
         if (var3 != null && !var3.isEmpty()) {
            var1.add(
               new AutoSbor.b(
                  var3,
                  ap(var3),
                  var3.getCount(),
                  TOP_MIN_DURABILITY[var2],
                  TOP_SHARPNESS[var2],
                  TOP_CRIT_2[var2],
                  TOP_EFFICIENCY[var2],
                  TOP_PROTECTION[var2],
                  TOP_UNBREAKING[var2],
                  TOP_RESPIRATION[var2],
                  TOP_DEPTH_STRIDER[var2]
               )
            );
         }
      }

      for (int var4 = 0; var4 < INVENTORY_SLOTS.length; var4++) {
         ItemStack var5 = INVENTORY_SLOTS[var4];
         if (var5 != null && !var5.isEmpty()) {
            var1.add(
               new AutoSbor.b(
                  var5,
                  ap(var5),
                  var5.getCount(),
                  INVENTORY_MIN_DURABILITY[var4],
                  INVENTORY_SHARPNESS[var4],
                  INVENTORY_CRIT_2[var4],
                  INVENTORY_EFFICIENCY[var4],
                  INVENTORY_PROTECTION[var4],
                  INVENTORY_UNBREAKING[var4],
                  INVENTORY_RESPIRATION[var4],
                  INVENTORY_DEPTH_STRIDER[var4]
               )
            );
         }
      }

      return var1;
   }

   public static int j(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         if (var1.player == null) {
            return 0;
         }

         int var2 = 0;
         int var3 = var1.player.getInventory().size();

         for (int var4 = 0; var4 < var3; var4++) {
            ItemStack var5 = var1.player.getInventory().getStack(var4);
            if (var5 != null && !var5.isEmpty() && a(var0, var5)) {
               var2 += var5.getCount();
            }
         }

         return var2;
      } else {
         return 0;
      }
   }

   public static boolean a(ItemStack var0, ItemStack var1) {
      if (var0 == null || var0.isEmpty() || var1 == null || var1.isEmpty()) {
         return false;
      }

      if (N(var0)) {
         return N(var1);
      }

      if (N(var1)) {
         return false;
      }

      if (G(var0)) {
         return G(var1);
      }

      if (G(var1)) {
         return false;
      }

      if (H(var0)) {
         return H(var1);
      }

      if (H(var1)) {
         return false;
      }

      if (I(var0)) {
         return I(var1);
      }

      if (I(var1)) {
         return false;
      }

      if (J(var0)) {
         return J(var1);
      }

      if (J(var1)) {
         return false;
      }

      if (F(var0)) {
         return F(var1);
      }

      if (F(var1)) {
         return false;
      }

      if (B(var0)) {
         return B(var1);
      }

      if (B(var1)) {
         return false;
      }

      if (C(var0)) {
         return C(var1);
      }

      if (C(var1)) {
         return false;
      }

      if (D(var0)) {
         return D(var1);
      }

      if (D(var1)) {
         return false;
      }

      if (E(var0)) {
         return E(var1);
      }

      if (E(var1)) {
         return false;
      }

      if (aj(var0)) {
         return aj(var1);
      }

      if (aj(var1)) {
         return false;
      }

      if (l(var0)) {
         return l(var1);
      }

      if (l(var1)) {
         return false;
      }

      if (k(var0)) {
         return k(var1);
      }

      if (k(var1)) {
         return false;
      }

      if (K(var0)) {
         return K(var1);
      }

      if (K(var1)) {
         return false;
      }

      if (O(var0)) {
         return O(var1);
      }

      if (O(var1)) {
         return false;
      }

      if (P(var0)) {
         return P(var1);
      }

      if (P(var1)) {
         return false;
      }

      if (Q(var0)) {
         return Q(var1);
      }

      if (Q(var1)) {
         return false;
      }

      if (aa(var0)) {
         return aa(var1);
      }

      if (aa(var1)) {
         return false;
      }

      if (t(var0)) {
         return t(var1);
      }

      if (t(var1)) {
         return false;
      }

      if (u(var0)) {
         return u(var1);
      }

      if (u(var1)) {
         return false;
      }

      if (v(var0)) {
         return v(var1);
      }

      if (v(var1)) {
         return false;
      }

      if (ab(var0)) {
         return ab(var1);
      }

      if (ab(var1)) {
         return false;
      }

      if (ac(var0)) {
         return ac(var1);
      }

      if (ac(var1)) {
         return false;
      }

      if (ae(var0)) {
         return ae(var1);
      }

      if (ae(var1)) {
         return false;
      }

      if (ad(var0)) {
         return ad(var1);
      }

      if (ad(var1)) {
         return false;
      }

      if (L(var0)) {
         return L(var1);
      }

      if (L(var1)) {
         return false;
      }

      if (M(var0)) {
         return M(var1);
      }

      if (M(var1)) {
         return false;
      }

      if (o(var0)) {
         return o(var1);
      }

      if (o(var1)) {
         return false;
      }

      if (p(var0)) {
         return p(var1);
      }

      if (p(var1)) {
         return false;
      }

      if (q(var0)) {
         return q(var1);
      }

      if (q(var1)) {
         return false;
      }

      if (af(var0)) {
         return af(var1);
      }

      if (af(var1)) {
         return false;
      }

      if (ag(var0)) {
         return ag(var1);
      }

      if (ag(var1)) {
         return false;
      }

      if (ah(var0)) {
         return ah(var1);
      }

      if (ah(var1)) {
         return false;
      }

      if (am(var0)) {
         return am(var1);
      }

      if (am(var1)) {
         return false;
      }

      if (R(var0)) {
         return R(var1);
      }

      if (R(var1)) {
         return false;
      }

      if (S(var0)) {
         return S(var1);
      }

      if (S(var1)) {
         return false;
      }

      if (T(var0)) {
         return T(var1);
      }

      if (T(var1)) {
         return false;
      }

      if (U(var0)) {
         return U(var1);
      }

      if (U(var1)) {
         return false;
      }

      if (ai(var0)) {
         return ai(var1);
      }

      if (r(var0)) {
         return r(var1);
      }

      if (r(var1)) {
         return false;
      }

      if (s(var0)) {
         return s(var1);
      }

      if (s(var1)) {
         return false;
      }

      if (ao(var0)) {
         return ao(var1);
      }

      if (ao(var1)) {
         return false;
      }

      if (an(var0)) {
         return an(var1);
      }

      if (an(var1)) {
         return false;
      }

      if (V(var0)) {
         return V(var1);
      }

      if (V(var1) && var0.isOf(Items.NETHERITE_HELMET)) {
         return false;
      }

      if (W(var0)) {
         return W(var1);
      }

      if (W(var1) && var0.isOf(Items.NETHERITE_CHESTPLATE)) {
         return false;
      }

      if (X(var0)) {
         return X(var1);
      }

      if (X(var1) && var0.isOf(Items.NETHERITE_LEGGINGS)) {
         return false;
      }

      if (Y(var0)) {
         return Y(var1);
      }

      if (Y(var1) && var0.isOf(Items.NETHERITE_BOOTS)) {
         return false;
      }

      if (n(var0)) {
         return n(var1);
      }

      if (n(var1) && var0.isOf(Items.NETHERITE_SWORD)) {
         return false;
      }

      if (m(var0)) {
         return m(var1);
      }

      if (m(var1) && var0.isOf(Items.NETHERITE_PICKAXE)) {
         return false;
      }

      if (var0.getItem() != var1.getItem()) {
         return false;
      }

      Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
      if (var2 != null) {
         Text var3 = var1.get(DataComponentTypes.CUSTOM_NAME);
         if (var3 == null) {
            return false;
         }

         String var4 = h(var2.getString()).toLowerCase(Locale.ROOT);
         String var5 = h(var3.getString()).toLowerCase(Locale.ROOT);
         return var4.equals(var5);
      } else {
         return true;
      }
   }

   public static boolean k(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.NETHERITE_PICKAXE)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("stinger") && !var1.contains("стингер")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("stinger") || var5.contains("стингер")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean l(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.NETHERITE_PICKAXE)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("eternity") && !var1.contains("этернети") && !var1.contains("вечност")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if ((var5.contains("кирка") || var5.contains("pickaxe")) && (var5.contains("eternity") || var5.contains("этернети") || var5.contains("вечност"))) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean m(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_PICKAXE) ? false : !k(var0) && !l(var0);
      } else {
         return false;
      }
   }

   public static boolean n(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_SWORD) ? false : !aj(var0) && !F(var0) && !K(var0);
      } else {
         return false;
      }
   }

   public static boolean o(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PRISMARINE_SHARD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("трапк") || var1.contains("взрывн");
   }

   public static boolean p(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.FIRE_CHARGE)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("штучк") || var1.contains("взрывн");
   }

   public static boolean q(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.NETHER_STAR)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("стан") || var1.contains("stun");
   }

   public static boolean r(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.POTION)) {
         return false;
      }

      if (!N(var0) && !ah(var0)) {
         String var1 = as(var0).toLowerCase(Locale.ROOT);
         if (!var1.contains("взрывн") && !var1.contains("оседающ") && !var1.contains("splash") && !var1.contains("lingering")) {
            PotionContentsComponent var2 = var0.get(DataComponentTypes.POTION_CONTENTS);
            if (var2 != null) {
               if (var2.matches(Potions.LONG_TURTLE_MASTER)) {
                  return true;
               }

               if (var2.matches(Potions.TURTLE_MASTER) || var2.matches(Potions.STRONG_TURTLE_MASTER)) {
                  return false;
               }
            }

            List var3 = ar(var0);
            boolean var4 = var1.contains("черепаш") || var1.contains("turtle");
            boolean var5 = false;
            boolean var6 = false;
            boolean var7 = false;
            boolean var8 = false;

            for (String var10 : (Iterable<String>)(Iterable<?>)(var3)) {
               String var11 = h(var10).toLowerCase(Locale.ROOT);
               if (var11.contains("черепаш") || var11.contains("turtle")) {
                  var4 = true;
               }

               if (var11.contains("сопротивление") || var11.contains("resistance")) {
                  var5 = true;
               }

               if (var11.contains("замедление") || var11.contains("slowness")) {
                  var6 = true;
               }

               if (var11.contains("00:40") || var11.contains("0:40") || var11.contains("40 сек") || var11.contains("40сек") || var11.contains("40s")) {
                  var7 = true;
               }

               if (var11.contains("00:20") || var11.contains("0:20") || var11.contains("20 сек") || var11.contains("20сек") || var11.contains("20s")) {
                  var8 = true;
               }
            }

            if (var8) {
               return false;
            } else {
               return !var7 || !var4 && (!var5 || !var6)
                  ? var4 && !var8 && (var5 || var6 || var1.contains("00:40") || var1.contains("0:40") || var1.contains("длинн") || var1.contains("long"))
                  : true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean s(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.POTION)) {
         return false;
      }

      if (!N(var0) && !ah(var0)) {
         String var1 = as(var0).toLowerCase(Locale.ROOT);
         if (!var1.contains("взрывн") && !var1.contains("оседающ") && !var1.contains("splash") && !var1.contains("lingering")) {
            PotionContentsComponent var2 = var0.get(DataComponentTypes.POTION_CONTENTS);
            if (var2 != null) {
               if (var2.matches(Potions.STRONG_SWIFTNESS)) {
                  return true;
               }

               if (var2.matches(Potions.SWIFTNESS)) {
                  return false;
               }
            }

            List var3 = ar(var0);
            boolean var4 = false;
            boolean var5 = false;

            for (String var7 : (Iterable<String>)(Iterable<?>)(var3)) {
               String var8 = h(var7).toLowerCase(Locale.ROOT);
               if (var8.contains("скорост") || var8.contains("speed") || var8.contains("swiftness")) {
                  var4 = true;
                  if (var8.contains("ii") || var8.contains("2") || var8.contains("улучш")) {
                     var5 = true;
                  }
               }
            }

            return var4 && (var5 || var1.contains("улучшенн"));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean aa(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("цербер") && !var1.contains("cerber")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("цербер") || var5.contains("cerber") || var5.contains("проклятие утраты")) {
               return true;
            }
         }

         NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var6 != null) {
            String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var7.contains("цербер") || var7.contains("cerber") || var7.contains("сфера_цербера")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ab(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("броня 3") && !var1.contains("броня iii") || !var1.contains("урон 2") && !var1.contains("урон ii")) {
         List var2 = ar(var0);
         boolean var3 = false;
         boolean var4 = false;

         for (String var6 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var7 = h(var6).toLowerCase(Locale.ROOT);
            if (var7.contains("броня iii") || var7.contains("броня 3")) {
               var3 = true;
            }

            if (var7.contains("урон ii") || var7.contains("урон 2")) {
               var4 = true;
            }
         }

         if (var3 && var4) {
            return true;
         }

         NbtComponent var8 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var8 != null) {
            String var9 = var8.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var9.contains("мифическая_сфера2") || var9.contains("mythical2")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ac(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("броня 2") && !var1.contains("броня ii") || !var1.contains("урон 3") && !var1.contains("урон iii")) {
         List var2 = ar(var0);
         boolean var3 = false;
         boolean var4 = false;

         for (String var6 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var7 = h(var6).toLowerCase(Locale.ROOT);
            if (var7.contains("броня ii") || var7.contains("броня 2")) {
               var3 = true;
            }

            if (var7.contains("урон iii") || var7.contains("урон 3")) {
               var4 = true;
            }
         }

         if (var3 && var4) {
            return true;
         }

         NbtComponent var8 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var8 != null) {
            String var9 = var8.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var9.contains("мифическая_сфера3") || var9.contains("mythical3")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean t(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("флеш") && !var1.contains("flash")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("флеш") || var5.contains("flash")) {
               return true;
            }
         }

         NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var6 != null) {
            String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var7.contains("флеш") || var7.contains("flash") || var7.contains("сфера_флеша")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean u(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("armortality") && !var1.contains("арморталити") && !var1.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("armortality") || var5.contains("арморталити") || var5.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
               return true;
            }
         }

         NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var6 != null) {
            String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var7.contains("armortality") || var7.contains("арморталити") || var7.contains("сфера_арморталити")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean v(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("immortality") && !var1.contains("имморталити") && !var1.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ") && !var1.contains("immortal")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("immortality") || var5.contains("имморталити") || var5.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ")) {
               return true;
            }
         }

         NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var6 != null) {
            String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var7.contains("immortality") || var7.contains("имморталити") || var7.contains("сфера_имморталити") || var7.contains("immortal")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ad(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      if (!aa(var0) && !t(var0) && !u(var0) && !v(var0) && !ab(var0) && !ac(var0)) {
         String var1 = as(var0).toLowerCase(Locale.ROOT);
         if (var1.contains("урон") || var1.contains("damage")) {
            return false;
         }

         if (!var1.contains("скорост") && !var1.contains("speed")) {
            for (String var4 : ar(var0)) {
               String var5 = h(var4).toLowerCase(Locale.ROOT);
               if (var5.contains("скорость iii") || var5.contains("скорость 3") || var5.contains("speed iii") || var5.contains("speed 3")) {
                  return true;
               }
            }

            NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var6 != null) {
               String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
               if (var7.contains("hms-speed") || var7.contains("speed3") || var7.contains("скорость")) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean ae(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      if (!aa(var0) && !t(var0) && !u(var0) && !v(var0) && !ab(var0) && !ac(var0)) {
         String var1 = as(var0).toLowerCase(Locale.ROOT);
         if (var1.contains("скорост") || var1.contains("speed")) {
            return false;
         }

         if (!var1.contains("урон") && !var1.contains("damage") && !var1.contains("сил")) {
            for (String var4 : ar(var0)) {
               String var5 = h(var4).toLowerCase(Locale.ROOT);
               if (var5.contains("урон iii")
                  || var5.contains("урон 3")
                  || var5.contains("сила iii")
                  || var5.contains("сила 3")
                  || var5.contains("damage iii")
                  || var5.contains("damage 3")
                  || var5.contains("strength iii")
                  || var5.contains("strength 3")) {
                  return true;
               }
            }

            NbtComponent var6 = var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var6 != null) {
               String var7 = var6.copyNbt().toString().toLowerCase(Locale.ROOT);
               if (var7.contains("hms-damage") || var7.contains("damage3") || var7.contains("урон") || var7.contains("сила") || var7.contains("strength")) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean af(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.ENDER_PEARL)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("эндермен") && !var1.contains("enderman")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("эндермен") || var5.contains("enderman")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ag(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.FEATHER)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("гравитац") && !var1.contains("gravity")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("гравитац") || var5.contains("gravity")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ah(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!(var0.getItem() instanceof PotionItem) && !var0.isOf(Items.GLASS_BOTTLE)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("справедлив") && !var1.contains("justice")) {
         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("справедлив") || var5.contains("justice")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean ai(ItemStack var0) {
      return var0 != null && !var0.isEmpty() ? var0.isOf(Items.SNOWBALL) : false;
   }

   private static boolean aj(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = Registries.ITEM.getId(var0.getItem()).getPath().toLowerCase(Locale.ROOT);
         if (!var1.endsWith("_sword")) {
            return false;
         }

         String var2 = as(var0).toLowerCase(Locale.ROOT);
         if (!var2.contains("eternity") && !var2.contains("этернети") && !var2.contains("вечност")) {
            for (String var5 : ar(var0)) {
               String var6 = h(var5).toLowerCase(Locale.ROOT);
               if ((var6.contains("меч") || var6.contains("sword")) && (var6.contains("eternity") || var6.contains("этернети") || var6.contains("вечност"))) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean f(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = h(var0)
            .toLowerCase(Locale.ROOT)
            .replace(' ', ' ')
            .replace("і", "i")
            .replace("ɪ", "i")
            .replace("т", "t")
            .replace("ʏ", "y")
            .replace("ɴ", "n");
         return var1.contains("eternity") || var1.contains("этернити") || var1.contains("этернети") || var1.contains("вечност");
      } else {
         return false;
      }
   }

   private static boolean ak(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (f(as(var0))) {
         return true;
      }

      Text var1 = var0.get(DataComponentTypes.CUSTOM_NAME);
      if (var1 != null && f(var1.getString())) {
         return true;
      }

      Text var2 = var0.get(DataComponentTypes.ITEM_NAME);
      if (var2 != null && f(var2.getString())) {
         return true;
      }

      for (String var4 : ar(var0)) {
         if (f(var4)) {
            return true;
         }
      }

      NbtComponent var5 = var0.get(DataComponentTypes.CUSTOM_DATA);
      return var5 != null && f(var5.copyNbt().toString());
   }

   private static boolean g(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = h(var0)
            .toLowerCase(Locale.ROOT)
            .replace(' ', ' ')
            .replace("і", "i")
            .replace("ɪ", "i")
            .replace("ꜰ", "f")
            .replace("ғ", "f")
            .replace("ʏ", "y")
            .replace("ɴ", "n")
            .replace("т", "t");
         return var1.contains("infinity") || var1.contains("инфинити") || var1.contains("infin") || var1.contains("инфин") || var1.contains("бесконечн");
      } else {
         return false;
      }
   }

   private static boolean al(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (g(as(var0))) {
         return true;
      }

      Text var1 = var0.get(DataComponentTypes.CUSTOM_NAME);
      if (var1 != null && g(var1.getString())) {
         return true;
      }

      Text var2 = var0.get(DataComponentTypes.ITEM_NAME);
      if (var2 != null && g(var2.getString())) {
         return true;
      }

      for (String var4 : ar(var0)) {
         if (g(var4)) {
            return true;
         }
      }

      NbtComponent var5 = var0.get(DataComponentTypes.CUSTOM_DATA);
      return var5 != null && g(var5.copyNbt().toString());
   }

   public static boolean w(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (!var0.isOf(Items.NETHERITE_HELMET)
         && !var0.isOf(Items.DIAMOND_HELMET)
         && !var0.isOf(Items.IRON_HELMET)
         && !var0.isOf(Items.GOLDEN_HELMET)
         && !var0.isOf(Items.CHAINMAIL_HELMET)
         && !var0.isOf(Items.LEATHER_HELMET)
         && !var0.isOf(Items.TURTLE_HELMET)) {
         String var1 = var0.getItem().getTranslationKey().toLowerCase(Locale.ROOT);
         return var1.contains("helmet") || var1.contains("cap") || as(var0).toLowerCase(Locale.ROOT).contains("шлем");
      } else {
         return true;
      }
   }

   public static boolean x(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (!var0.isOf(Items.NETHERITE_CHESTPLATE)
         && !var0.isOf(Items.DIAMOND_CHESTPLATE)
         && !var0.isOf(Items.IRON_CHESTPLATE)
         && !var0.isOf(Items.GOLDEN_CHESTPLATE)
         && !var0.isOf(Items.CHAINMAIL_CHESTPLATE)
         && !var0.isOf(Items.LEATHER_CHESTPLATE)) {
         String var1 = var0.getItem().getTranslationKey().toLowerCase(Locale.ROOT);
         return var1.contains("chestplate") || var1.contains("tunic") || as(var0).toLowerCase(Locale.ROOT).contains("нагрудник");
      } else {
         return true;
      }
   }

   public static boolean y(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (!var0.isOf(Items.NETHERITE_LEGGINGS)
         && !var0.isOf(Items.DIAMOND_LEGGINGS)
         && !var0.isOf(Items.IRON_LEGGINGS)
         && !var0.isOf(Items.GOLDEN_LEGGINGS)
         && !var0.isOf(Items.CHAINMAIL_LEGGINGS)
         && !var0.isOf(Items.LEATHER_LEGGINGS)) {
         String var1 = var0.getItem().getTranslationKey().toLowerCase(Locale.ROOT);
         return var1.contains("leggings") || var1.contains("pants") || as(var0).toLowerCase(Locale.ROOT).contains("поножи");
      } else {
         return true;
      }
   }

   public static boolean z(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (!var0.isOf(Items.NETHERITE_BOOTS)
         && !var0.isOf(Items.DIAMOND_BOOTS)
         && !var0.isOf(Items.IRON_BOOTS)
         && !var0.isOf(Items.GOLDEN_BOOTS)
         && !var0.isOf(Items.CHAINMAIL_BOOTS)
         && !var0.isOf(Items.LEATHER_BOOTS)) {
         String var1 = var0.getItem().getTranslationKey().toLowerCase(Locale.ROOT);
         return var1.contains("boots") || as(var0).toLowerCase(Locale.ROOT).contains("ботинки");
      } else {
         return true;
      }
   }

   public static boolean A(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if (!var0.isOf(Items.NETHERITE_SWORD)
         && !var0.isOf(Items.DIAMOND_SWORD)
         && !var0.isOf(Items.IRON_SWORD)
         && !var0.isOf(Items.GOLDEN_SWORD)
         && !var0.isOf(Items.STONE_SWORD)
         && !var0.isOf(Items.WOODEN_SWORD)) {
         String var1 = var0.getItem().getTranslationKey().toLowerCase(Locale.ROOT);
         return var1.contains("sword") || as(var0).toLowerCase(Locale.ROOT).contains("меч");
      } else {
         return true;
      }
   }

   public static boolean B(ItemStack var0) {
      return w(var0) && ak(var0);
   }

   public static boolean C(ItemStack var0) {
      return x(var0) && ak(var0);
   }

   public static boolean D(ItemStack var0) {
      return y(var0) && ak(var0);
   }

   public static boolean E(ItemStack var0) {
      return z(var0) && ak(var0);
   }

   public static boolean F(ItemStack var0) {
      return A(var0) && al(var0);
   }

   public static boolean G(ItemStack var0) {
      return w(var0) && al(var0);
   }

   public static boolean H(ItemStack var0) {
      return x(var0) && al(var0);
   }

   public static boolean I(ItemStack var0) {
      return y(var0) && al(var0);
   }

   public static boolean J(ItemStack var0) {
      return z(var0) && al(var0);
   }

   public static boolean K(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.NETHERITE_SWORD) && !var0.isOf(Items.DIAMOND_SWORD)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("цербер") && !var1.contains("cerber")) {
         Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var2 != null) {
            String var3 = h(var2.getString()).toLowerCase(Locale.ROOT);
            if (var3.contains("цербер") || var3.contains("cerber")) {
               return true;
            }
         }

         for (String var5 : ar(var0)) {
            String var6 = h(var5).toLowerCase(Locale.ROOT);
            if (var6.contains("цербер") || var6.contains("cerber")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean L(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.WITHER_SKELETON_SKULL)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("вампиризм") || var1.contains("vampir");
   }

   public static boolean M(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.POPPED_CHORUS_FRUIT)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("трапк") || var1.contains("trap");
   }

   public static boolean N(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = as(var0).toLowerCase(Locale.ROOT);
         if (!var1.contains("побед") && !var1.contains("winner") && !var1.contains("victor")) {
            Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
            if (var2 != null) {
               String var3 = h(var2.getString()).toLowerCase(Locale.ROOT);
               if (var3.contains("побед") || var3.contains("winner") || var3.contains("victor")) {
                  return true;
               }
            }

            Text var9 = var0.get(DataComponentTypes.ITEM_NAME);
            if (var9 != null) {
               String var4 = h(var9.getString()).toLowerCase(Locale.ROOT);
               if (var4.contains("побед") || var4.contains("winner") || var4.contains("victor")) {
                  return true;
               }
            }

            boolean var10 = false;
            boolean var5 = false;

            for (String var7 : ar(var0)) {
               String var8 = h(var7).toLowerCase(Locale.ROOT);
               if (var8.contains("побед") || var8.contains("winner") || var8.contains("victor")) {
                  return true;
               }

               if (var8.contains("спешк") || var8.contains("haste")) {
                  var10 = true;
               }

               if (var8.contains("невидим") || var8.contains("invis") || var8.contains("исцелен")) {
                  var5 = true;
               }
            }

            if (!var10
               || !var5
               || !(var0.getItem() instanceof PotionItem)
                  && !var0.isOf(Items.POTION)
                  && !var0.isOf(Items.SPLASH_POTION)
                  && !var0.isOf(Items.LINGERING_POTION)
                  && !var0.isOf(Items.GLASS_BOTTLE)) {
               NbtComponent var11 = var0.get(DataComponentTypes.CUSTOM_DATA);
               if (var11 != null) {
                  String var12 = var11.copyNbt().toString().toLowerCase(Locale.ROOT);
                  if (var12.contains("побед") || var12.contains("winner") || var12.contains("victor") || var12.contains("winner_potion")) {
                     return true;
                  }
               }

               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean O(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("infinity") && !var1.contains("инфинити")) {
         Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var2 != null) {
            String var3 = h(var2.getString()).toLowerCase(Locale.ROOT);
            if (var3.contains("infinity") || var3.contains("инфинити")) {
               return true;
            }
         }

         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("infinity") || var5.contains("инфинити")) {
               return true;
            }
         }

         NbtComponent var7 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var7 != null) {
            String var8 = var7.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var8.contains("infinity") || var8.contains("инфинити")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean P(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("eternity") && !var1.contains("этернити") && !var1.contains("этернети") && !var1.contains("вечност")) {
         Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var2 != null) {
            String var3 = h(var2.getString()).toLowerCase(Locale.ROOT);
            if (var3.contains("eternity") || var3.contains("этернити") || var3.contains("этернети") || var3.contains("вечност")) {
               return true;
            }
         }

         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("eternity") || var5.contains("этернити") || var5.contains("этернети") || var5.contains("вечност")) {
               return true;
            }
         }

         NbtComponent var7 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var7 != null) {
            String var8 = var7.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var8.contains("eternity") || var8.contains("этернити") || var8.contains("этернети") || var8.contains("вечност")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean Q(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      if (!var1.contains("stinger") && !var1.contains("стингер")) {
         Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var2 != null) {
            String var3 = h(var2.getString()).toLowerCase(Locale.ROOT);
            if (var3.contains("stinger") || var3.contains("стингер")) {
               return true;
            }
         }

         for (String var4 : ar(var0)) {
            String var5 = h(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("stinger") || var5.contains("стингер")) {
               return true;
            }
         }

         NbtComponent var7 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var7 != null) {
            String var8 = var7.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (var8.contains("stinger") || var8.contains("стингер")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean R(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PINK_SHULKER_BOX)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("рюкзак") || var1.contains("1");
   }

   public static boolean S(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.LIGHT_BLUE_SHULKER_BOX)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("рюкзак") || var1.contains("2");
   }

   public static boolean T(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.RED_SHULKER_BOX)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("рюкзак") || var1.contains("3");
   }

   public static boolean U(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.MAGENTA_SHULKER_BOX)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("рюкзак") || var1.contains("4");
   }

   public static boolean V(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_HELMET) ? false : !B(var0) && !G(var0);
      } else {
         return false;
      }
   }

   public static boolean W(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_CHESTPLATE) ? false : !C(var0) && !H(var0);
      } else {
         return false;
      }
   }

   public static boolean X(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_LEGGINGS) ? false : !D(var0) && !I(var0);
      } else {
         return false;
      }
   }

   public static boolean Y(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.isOf(Items.NETHERITE_BOOTS) ? false : !E(var0) && !J(var0);
      } else {
         return false;
      }
   }

   private static boolean am(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.EXPERIENCE_BOTTLE)) {
         return false;
      }

      String var1 = as(var0).toLowerCase(Locale.ROOT);
      return var1.contains("50") || var1.contains("опыт");
   }

   private static boolean an(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.POTION)) {
         return false;
      }

      if (!N(var0) && !ah(var0)) {
         PotionContentsComponent var1 = var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var1 != null) {
            if (var1.matches(Potions.STRONG_HEALING)) {
               return true;
            }

            if (var1.matches(Potions.HEALING)) {
               return false;
            }
         }

         String var2 = as(var0).toLowerCase(Locale.ROOT);
         if (!var2.contains("взрывн") && !var2.contains("оседающ") && !var2.contains("splash") && !var2.contains("lingering")) {
            List var3 = ar(var0);
            boolean var4 = false;
            boolean var5 = false;

            for (String var7 : (Iterable<String>)(Iterable<?>)(var3)) {
               String var8 = h(var7).toLowerCase(Locale.ROOT);
               if (var8.contains("исцелен") || var8.contains("healing")) {
                  var4 = true;
                  if (var8.contains("ii") || var8.contains("2") || var8.contains("улучш")) {
                     var5 = true;
                  }
               }
            }

            return var4 && (var5 || var2.contains("ii") || var2.contains("2"));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean ao(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.POTION)) {
         return false;
      }

      if (!N(var0) && !ah(var0)) {
         PotionContentsComponent var1 = var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var1 != null) {
            if (var1.matches(Potions.STRONG_STRENGTH)) {
               return true;
            }

            if (var1.matches(Potions.STRENGTH)) {
               return false;
            }
         }

         String var2 = as(var0).toLowerCase(Locale.ROOT);
         if (!var2.contains("взрывн") && !var2.contains("оседающ") && !var2.contains("splash") && !var2.contains("lingering")) {
            List var3 = ar(var0);
            boolean var4 = false;
            boolean var5 = false;

            for (String var7 : (Iterable<String>)(Iterable<?>)(var3)) {
               String var8 = h(var7).toLowerCase(Locale.ROOT);
               if (var8.contains("сил") || var8.contains("strength")) {
                  var4 = true;
                  if (var8.contains("ii") || var8.contains("2") || var8.contains("улучш")) {
                     var5 = true;
                  }
               }
            }

            return var4 && (var5 || var2.contains("улучшенн"));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static String ap(ItemStack var0) {
      if (k(var0)) {
         return "Кирка STINGER";
      }

      if (l(var0)) {
         return "Кирка ETERNITY";
      }

      if (m(var0)) {
         return "Незеритовая кирка";
      }

      if (n(var0)) {
         return "Незеритовый меч";
      }

      if (o(var0)) {
         return "Взрывная трапка";
      }

      if (p(var0)) {
         return "Взрывная штучка";
      }

      if (q(var0)) {
         return "Стан";
      }

      if (r(var0)) {
         return "Зелье черепашьей мощи";
      }

      if (s(var0)) {
         return "Улучшенное зелье скорости";
      }

      if (aa(var0)) {
         return "Сфера Цербера";
      }

      if (t(var0)) {
         return "Сфера Флеша";
      }

      if (u(var0)) {
         return "Сфера Арморталити";
      }

      if (v(var0)) {
         return "Сфера Имморталити";
      }

      if (ab(var0)) {
         return "Сфера на броня 3";
      }

      if (ac(var0)) {
         return "Сфера на урон 3";
      }

      if (ad(var0)) {
         return "Сфера на скорость 3";
      }

      if (ae(var0)) {
         return "Сфера на урон 3";
      }

      if (af(var0)) {
         return "Эндермен";
      }

      if (ag(var0)) {
         return "Гравитация";
      }

      if (ah(var0)) {
         return "Справедливость";
      }

      if (ai(var0)) {
         return "Снежок";
      }

      if (var0.isOf(Items.GOLDEN_CARROT)) {
         return "Золотая морковь";
      }

      if (var0.isOf(Items.CHORUS_FRUIT)) {
         return "Плод хоруса";
      }

      if (F(var0)) {
         return "Меч Infinity";
      }

      if (G(var0)) {
         return "Шлем Infinity";
      }

      if (H(var0)) {
         return "Нагрудник Infinity";
      }

      if (I(var0)) {
         return "Поножи Infinity";
      }

      if (J(var0)) {
         return "Ботинки Infinity";
      }

      if (aj(var0)) {
         return "Меч Eternity";
      }

      if (B(var0)) {
         return "Шлем Eternity";
      }

      if (C(var0)) {
         return "Нагрудник Eternity";
      }

      if (D(var0)) {
         return "Поножи Eternity";
      }

      if (E(var0)) {
         return "Ботинки Eternity";
      }

      if (K(var0)) {
         return "Меч Цербера";
      }

      if (L(var0)) {
         return "Вампиризм";
      }

      if (M(var0)) {
         return "Трапка";
      }

      if (N(var0)) {
         return "Зелье победителя";
      }

      if (O(var0)) {
         return "Талисман Infinity";
      }

      if (P(var0)) {
         return "Талисман Eternity";
      }

      if (Q(var0)) {
         return "Талисман Stinger";
      }

      if (R(var0)) {
         return "Рюкзак 1 уровень";
      }

      if (S(var0)) {
         return "Рюкзак 2 уровень";
      }

      if (T(var0)) {
         return "Рюкзак 3 уровень";
      }

      if (U(var0)) {
         return "Рюкзак 4 уровень";
      }

      if (V(var0)) {
         return "Незеритовый шлем";
      }

      if (W(var0)) {
         return "Незеритовый нагрудник";
      }

      if (X(var0)) {
         return "Незеритовые поножи";
      }

      if (Y(var0)) {
         return "Незеритовые ботинки";
      }

      if (am(var0)) {
         return "Опыт 50";
      }

      if (an(var0)) {
         return "Зелье исцеление";
      }

      if (ao(var0)) {
         return "Улучшенное зелье силы";
      }

      if (var0.isOf(Items.ENDER_PEARL)) {
         return "Эндер-жемчуг";
      }

      if (var0.isOf(Items.ELYTRA)) {
         return "Элитры";
      }

      if (var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return "Тотем бессмертия";
      }

      if (var0.isOf(Items.GOLDEN_APPLE)) {
         return "Золотое яблоко";
      }

      if (var0.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
         return "Зачарованное яблоко";
      }

      if (var0.isOf(Items.FIREWORK_ROCKET)) {
         return "Фейерверк";
      }

      Text var1 = var0.get(DataComponentTypes.CUSTOM_NAME);
      return var1 != null ? h(var1.getString()) : h(var0.getName().getString());
   }

   private int aq(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         List var2 = ar(var1);
         int var3 = this.a(var1, var2);

         for (String var5 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var6 = h(var5).replace(' ', ' ');
            Matcher var7 = HW_UNIT_PRICE_PATTERN.matcher(var6);
            if (var7.find()) {
               long var8 = i(var7.group(1));
               if (var8 > 0L) {
                  return (int)Math.min(2147483647L, var8);
               }
            }

            Matcher var16 = HW_TOTAL_PRICE_PATTERN.matcher(var6);
            if (var16.find()) {
               long var9 = i(var16.group(1));
               if (var9 > 0L) {
                  return (int)Math.max(1L, var9 / var3);
               }
            }

            String var17 = var6.toLowerCase(Locale.ROOT);
            if (var17.contains("цена")
               || var17.contains("стоимост")
               || var17.contains("price")
               || var17.contains("купить")
               || var17.contains("$")
               || var17.contains("руб")
               || var17.contains("монет")) {
               long var10 = i(var6);
               if (var10 > 0L) {
                  if (!var17.contains("ед") && !var17.contains("шт") && !var17.contains("unit") && !var17.contains("за 1")) {
                     return (int)Math.max(1L, var10 / var3);
                  }

                  return (int)Math.min(2147483647L, var10);
               }
            }
         }

         for (String var13 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var14 = h(var13).replace(' ', ' ');
            long var15 = i(var14);
            if (var15 > 0L) {
               return (int)Math.max(1L, var15 / var3);
            }
         }

         return 1;
      } else {
         return Integer.MAX_VALUE;
      }
   }

   private int a(ItemStack var1, List<String> var2) {
      for (String var4 : var2) {
         String var5 = h(var4);
         Matcher var6 = COUNT_X.matcher(var5);
         if (var6.find()) {
            int var7 = j(var6.group(1));
            if (var7 > 0) {
               return var7;
            }
         }

         Matcher var10 = COUNT_PCS.matcher(var5);
         if (var10.find()) {
            int var8 = j(var10.group(1));
            if (var8 > 0) {
               return var8;
            }
         }

         Matcher var11 = COUNT_LABEL.matcher(var5);
         if (var11.find()) {
            int var9 = j(var11.group(1));
            if (var9 > 0) {
               return var9;
            }
         }
      }

      return Math.max(1, var1.getCount());
   }

   private static List<String> ar(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      if (var0 != null && !var0.isEmpty()) {
         MinecraftClient var2 = MinecraftClient.getInstance();

         try {
            TooltipContext var3 = var2.world != null ? TooltipContext.create(var2.world) : TooltipContext.DEFAULT;
            List var4 = var0.getTooltip(var3, var2.player, TooltipType.ADVANCED);
            if (var4 != null) {
               for (Text var6 : (Iterable<Text>)(Iterable<?>)(var4)) {
                  if (var6 != null) {
                     var1.add(var6.getString());
                  }
               }
            }
         } catch (Exception var7) {
         }

         if (var1.isEmpty()) {
            var1.add(as(var0));
         }

         LoreComponent var8 = var0.get(DataComponentTypes.LORE);
         if (var8 != null) {
            for (Text var10 : var8.lines()) {
               if (var10 != null) {
                  String var11 = var10.getString();
                  if (!var1.contains(var11)) {
                     var1.add(var11);
                  }
               }
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static String as(ItemStack var0) {
      try {
         return var0.getName().getString();
      } catch (Exception var2) {
         return "";
      }
   }

   public static String a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         StringBuilder var1 = new StringBuilder(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            switch (var3) {
               case ' ':
                  var1.append(' ');
                  break;
               case 'ǫ':
               case 'Ϙ':
                  var1.append('q');
                  break;
               case 'ɢ':
                  var1.append('g');
                  break;
               case 'ɪ':
               case 'і':
               case 'ⅰ':
                  var1.append('i');
                  break;
               case 'ɴ':
                  var1.append('n');
                  break;
               case 'ʀ':
                  var1.append('r');
                  break;
               case 'ʏ':
                  var1.append('y');
                  break;
               case 'ʙ':
                  var1.append('b');
                  break;
               case 'ʜ':
                  var1.append('h');
                  break;
               case 'ʟ':
                  var1.append('l');
                  break;
               case 'ѕ':
               case 'ꜱ':
                  var1.append('s');
                  break;
               case 'ғ':
               case 'ꜰ':
                  var1.append('f');
                  break;
               case 'ᴀ':
                  var1.append('a');
                  break;
               case 'ᴄ':
                  var1.append('c');
                  break;
               case 'ᴅ':
                  var1.append('d');
                  break;
               case 'ᴇ':
                  var1.append('e');
                  break;
               case 'ᴊ':
                  var1.append('j');
                  break;
               case 'ᴋ':
                  var1.append('k');
                  break;
               case 'ᴍ':
                  var1.append('m');
                  break;
               case 'ᴏ':
                  var1.append('o');
                  break;
               case 'ᴘ':
                  var1.append('p');
                  break;
               case 'ᴛ':
                  var1.append('t');
                  break;
               case 'ᴜ':
                  var1.append('u');
                  break;
               case 'ᴠ':
                  var1.append('v');
                  break;
               case 'ᴡ':
                  var1.append('w');
                  break;
               case 'ᴢ':
                  var1.append('z');
                  break;
               default:
                  var1.append(var3);
            }
         }

         return var1.toString();
      } else {
         return "";
      }
   }

   private static String h(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.replaceAll("(?i)§[0-9a-fk-or#x]", "").replaceAll("(?i)&[0-9a-fk-or#x]", "").replaceAll("§", "").trim();
      return a(var1);
   }

   private static long i(String var0) {
      if (var0 == null) {
         return -1L;
      }

      long var1 = 0L;
      boolean var3 = false;

      for (int var4 = 0; var4 < var0.length(); var4++) {
         char var5 = var0.charAt(var4);
         if (var5 >= '0' && var5 <= '9') {
            var1 = var1 * 10L + (var5 - '0');
            var3 = true;
         }
      }

      return var3 ? var1 : -1L;
   }

   private static int j(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (Exception var2) {
         return -1;
      }
   }

   private boolean a(AutoSbor.b var1, ItemStack var2) {
      if (var1 == null || var1.getStack() == null || var1.getStack().isEmpty()) {
         return false;
      } else if (var2 == null || var2.isEmpty() || this.at(var2)) {
         return false;
      } else if (N(var1.getStack())) {
         return N(var2);
      } else if (G(var1.getStack())) {
         return G(var2);
      } else if (H(var1.getStack())) {
         return H(var2);
      } else if (I(var1.getStack())) {
         return I(var2);
      } else if (J(var1.getStack())) {
         return J(var2);
      } else if (F(var1.getStack())) {
         return F(var2);
      } else if (B(var1.getStack())) {
         return B(var2);
      } else if (C(var1.getStack())) {
         return C(var2);
      } else if (D(var1.getStack())) {
         return D(var2);
      } else if (E(var1.getStack())) {
         return E(var2);
      } else if (aj(var1.getStack())) {
         return aj(var2);
      } else if (O(var1.getStack())) {
         return O(var2);
      } else if (P(var1.getStack())) {
         return P(var2);
      } else {
         return Q(var1.getStack()) ? Q(var2) : a(var1.getStack(), var2);
      }
   }

   private boolean at(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.getItem() != Items.BARRIER && var1.getItem() != Items.AIR) {
            String var2 = as(var1).toLowerCase(Locale.ROOT);
            return var2.contains("назад") || var2.contains("вперед") || var2.contains("следующая") || var2.contains("предыдущая");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public void b(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("§7[§bAutoSbor§7] " + var1), false);
      }
   }

   public static String Z(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return "";
      } else if (var0.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
         Text var6 = var0.get(DataComponentTypes.CUSTOM_NAME);
         return var6 != null ? h(var6.getString()) : "Зачарованное яблоко";
      } else if (var0.isOf(Items.GOLDEN_APPLE)) {
         Text var5 = var0.get(DataComponentTypes.CUSTOM_NAME);
         return var5 != null ? h(var5.getString()) : "Золотое яблоко";
      } else {
         Text var1 = var0.get(DataComponentTypes.CUSTOM_NAME);
         String var2 = var1 != null ? var1.getString() : var0.getName().getString();
         String var3 = h(var2);
         String var4 = var3.toLowerCase(Locale.ROOT);
         if (!var4.contains("armortality") && !var4.contains("арморталити") && !var4.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
            return !var4.contains("immortality") && !var4.contains("имморталити") && !var4.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ") && !var4.contains("immortal")
               ? var3
               : "Сфера Имморталити";
         } else {
            return "Сфера Арморталити";
         }
      }
   }

   private static boolean a(Item var0) {
      return var0 == Items.GOLDEN_APPLE
         || var0 == Items.ENCHANTED_GOLDEN_APPLE
         || var0 == Items.ENDER_PEARL
         || var0 == Items.ELYTRA
         || var0 == Items.TOTEM_OF_UNDYING
         || var0 == Items.GOLDEN_CARROT
         || var0 == Items.CHORUS_FRUIT
         || var0 == Items.FIREWORK_ROCKET
         || var0 == Items.EXPERIENCE_BOTTLE
         || var0 == Items.NETHERITE_HELMET
         || var0 == Items.NETHERITE_CHESTPLATE
         || var0 == Items.NETHERITE_LEGGINGS
         || var0 == Items.NETHERITE_BOOTS
         || var0 == Items.NETHERITE_SWORD
         || var0 == Items.NETHERITE_PICKAXE;
   }

   public static ItemStack a(String var0, String var1, int var2) {
      int var6 = Math.max(1, var2);
      if (var0 != null && !var0.isEmpty()) {
         String var3 = var0.toLowerCase(Locale.ROOT);
         ItemStack var4 = null;
         if (var3.contains("цербер") && var3.contains("сфера")) {
            var4 = AutoSborScreen.CERBERUS_SPHERE_STACK.copy();
         } else if (var3.contains("цербер") || var3.contains("cerber")) {
            var4 = AutoSborScreen.CERBERUS_SWORD_STACK.copy();
         } else if (var3.contains("флеш") || var3.contains("flash")) {
            var4 = AutoSborScreen.FLASH_SPHERE_STACK.copy();
         } else if (var3.contains("арморталити") || var3.contains("armortality") || var3.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
            var4 = AutoSborScreen.ARMORTALITY_SPHERE_STACK.copy();
         } else if (!var3.contains("имморталити") && !var3.contains("immortality") && !var3.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ") && !var3.contains("immortal")) {
            if (!var3.contains("броня 3") && !var3.contains("броня iii") || !var3.contains("урон 2") && !var3.contains("урон ii")) {
               if (!var3.contains("броня 2") && !var3.contains("броня ii") || !var3.contains("урон 3") && !var3.contains("урон iii")) {
                  if (!var3.contains("сфера") || !var3.contains("урон") && !var3.contains("damage") && !var3.contains("сил")) {
                     if (var3.contains("сфера") || var3.contains("speed")) {
                        var4 = AutoSborScreen.SPEED_SPHERE_STACK.copy();
                     } else if (var3.contains("stinger") || var3.contains("стингер")) {
                        var4 = AutoSborScreen.STINGER_PICKAXE_STACK.copy();
                     } else if (var3.contains("кирка") && var3.contains("eternity")) {
                        var4 = AutoSborScreen.ETERNITY_PICKAXE_STACK.copy();
                     } else if (!var3.contains("талисман") || !var3.contains("infinity") && !var3.contains("инфинити")) {
                        if (!var3.contains("талисман") || !var3.contains("eternity") && !var3.contains("этернити")) {
                           if (!var3.contains("талисман") || !var3.contains("stinger") && !var3.contains("стингер")) {
                              if (var3.contains("незеритовая кирка")) {
                                 var4 = new ItemStack(Items.NETHERITE_PICKAXE);
                              } else if (var3.contains("незеритовый меч")) {
                                 var4 = new ItemStack(Items.NETHERITE_SWORD);
                              } else if (var3.contains("взрывная трапка")) {
                                 var4 = AutoSborScreen.EXPLOSIVE_TRAP_STACK.copy();
                              } else if (var3.contains("взрывная штучка") || var3.contains("штучка")) {
                                 var4 = AutoSborScreen.EXPLOSIVE_THING_STACK.copy();
                              } else if (var3.contains("стан") || var3.contains("stun")) {
                                 var4 = AutoSborScreen.STUN_STACK.copy();
                              } else if (var3.contains("черепашь") || var3.contains("turtle")) {
                                 var4 = AutoSborScreen.TURTLE_MASTER_POTION_STACK.copy();
                              } else if (var3.contains("снежок") || var3.contains("snowball")) {
                                 var4 = AutoSborScreen.SNOWBALL_STACK.copy();
                              } else if (var3.contains("эндермен")) {
                                 var4 = AutoSborScreen.ENDERMAN_STACK.copy();
                              } else if (var3.contains("гравитац")) {
                                 var4 = AutoSborScreen.GRAVITY_STACK.copy();
                              } else if (var3.contains("справедлив")) {
                                 var4 = AutoSborScreen.JUSTICE_STACK.copy();
                              } else if (var3.contains("морков") || var3.contains("carrot")) {
                                 var4 = new ItemStack(Items.GOLDEN_CARROT);
                              } else if (var3.contains("хорус") || var3.contains("chorus")) {
                                 var4 = new ItemStack(Items.CHORUS_FRUIT);
                              } else if (var3.contains("меч") && var3.contains("eternity")) {
                                 var4 = AutoSborScreen.ETERNITY_SWORD_STACK.copy();
                              } else if (var3.contains("шлем") && var3.contains("eternity")) {
                                 var4 = AutoSborScreen.ETERNITY_HELMET_STACK.copy();
                              } else if (var3.contains("нагрудник") && var3.contains("eternity")) {
                                 var4 = AutoSborScreen.ETERNITY_CHESTPLATE_STACK.copy();
                              } else if (var3.contains("поножи") && var3.contains("eternity")) {
                                 var4 = AutoSborScreen.ETERNITY_LEGGINGS_STACK.copy();
                              } else if (var3.contains("ботинки") && var3.contains("eternity")) {
                                 var4 = AutoSborScreen.ETERNITY_BOOTS_STACK.copy();
                              } else if (var3.contains("меч") && var3.contains("infinity")) {
                                 var4 = AutoSborScreen.INFINITY_SWORD_STACK.copy();
                              } else if (var3.contains("шлем") && var3.contains("infinity")) {
                                 var4 = AutoSborScreen.INFINITY_HELMET_STACK.copy();
                              } else if (var3.contains("нагрудник") && var3.contains("infinity")) {
                                 var4 = AutoSborScreen.INFINITY_CHESTPLATE_STACK.copy();
                              } else if (var3.contains("поножи") && var3.contains("infinity")) {
                                 var4 = AutoSborScreen.INFINITY_LEGGINGS_STACK.copy();
                              } else if (var3.contains("ботинки") && var3.contains("infinity")) {
                                 var4 = AutoSborScreen.INFINITY_BOOTS_STACK.copy();
                              } else if (!var3.contains("рюкзак") || !var3.contains("1") && !var3.contains("i")) {
                                 if (!var3.contains("рюкзак") || !var3.contains("2") && !var3.contains("ii")) {
                                    if (!var3.contains("рюкзак") || !var3.contains("3") && !var3.contains("iii")) {
                                       if (!var3.contains("рюкзак") || !var3.contains("4") && !var3.contains("iv")) {
                                          if (var3.contains("вампиризм") || var3.contains("vampir")) {
                                             var4 = AutoSborScreen.VAMPIRISM_STACK.copy();
                                          } else if (var3.contains("трапка")) {
                                             var4 = AutoSborScreen.POPPED_CHORUS_TRAP_STACK.copy();
                                          } else if (var3.contains("победител") || var3.contains("winner")) {
                                             var4 = AutoSborScreen.WINNER_POTION_STACK.copy();
                                          } else if (var3.contains("незеритовый шлем")) {
                                             var4 = new ItemStack(Items.NETHERITE_HELMET);
                                          } else if (var3.contains("незеритовый нагрудник")) {
                                             var4 = new ItemStack(Items.NETHERITE_CHESTPLATE);
                                          } else if (var3.contains("незеритовые поножи")) {
                                             var4 = new ItemStack(Items.NETHERITE_LEGGINGS);
                                          } else if (var3.contains("незеритовые ботинки")) {
                                             var4 = new ItemStack(Items.NETHERITE_BOOTS);
                                          } else if (var3.contains("опыт") && var3.contains("50")) {
                                             var4 = AutoSborScreen.EXP_50_STACK.copy();
                                          } else if (var3.contains("зелье исцеление") || var3.contains("исцеление")) {
                                             var4 = AutoSborScreen.HEALING_POTION_STACK.copy();
                                          } else if (var3.contains("улучшенное зелье скорости") || var3.contains("зелье скорости")) {
                                             var4 = AutoSborScreen.SPEED_POTION_STACK.copy();
                                          } else if (var3.contains("улучшенное зелье силы")
                                             || var3.contains("зелье силы")
                                             || var3.contains("силы")
                                             || var3.contains("сила")) {
                                             var4 = AutoSborScreen.STRENGTH_POTION_STACK.copy();
                                          } else if (var3.contains("эндер-жемчуг") || var3.contains("жемчуг")) {
                                             var4 = new ItemStack(Items.ENDER_PEARL);
                                          } else if (var3.contains("элитры")) {
                                             var4 = new ItemStack(Items.ELYTRA);
                                          } else if (var3.contains("тотем")) {
                                             var4 = new ItemStack(Items.TOTEM_OF_UNDYING);
                                          } else if (var3.contains("зачарованн") || var3.contains("enchanted") || var3.contains("чарк")) {
                                             var4 = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
                                          } else if ((!var3.contains("золот") || !var3.contains("яблок"))
                                             && !var3.contains("golden_apple")
                                             && !var3.contains("golden apple")
                                             && !var3.contains("яблоко")) {
                                             if (var3.contains("фейерверк")) {
                                                var4 = new ItemStack(Items.FIREWORK_ROCKET);
                                             }
                                          } else {
                                             var4 = new ItemStack(Items.GOLDEN_APPLE);
                                          }
                                       } else {
                                          var4 = AutoSborScreen.BACKPACK_LVL4_STACK.copy();
                                       }
                                    } else {
                                       var4 = AutoSborScreen.BACKPACK_LVL3_STACK.copy();
                                    }
                                 } else {
                                    var4 = AutoSborScreen.BACKPACK_LVL2_STACK.copy();
                                 }
                              } else {
                                 var4 = AutoSborScreen.BACKPACK_LVL1_STACK.copy();
                              }
                           } else {
                              var4 = AutoSborScreen.TALISMAN_STINGER_STACK.copy();
                           }
                        } else {
                           var4 = AutoSborScreen.TALISMAN_ETERNITY_STACK.copy();
                        }
                     } else {
                        var4 = AutoSborScreen.TALISMAN_INFINITY_STACK.copy();
                     }
                  } else {
                     var4 = AutoSborScreen.DAMAGE_SPHERE_STACK.copy();
                  }
               } else {
                  var4 = AutoSborScreen.ARMOR2_DAMAGE3_SPHERE_STACK.copy();
               }
            } else {
               var4 = AutoSborScreen.ARMOR3_DAMAGE2_SPHERE_STACK.copy();
            }
         } else {
            var4 = AutoSborScreen.IMMORTALITY_SPHERE_STACK.copy();
         }

         if (var4 != null) {
            var4.setCount(var6);
            return var4;
         }
      }

      try {
         if (var1 != null && !var1.isEmpty()) {
            if ("minecraft:enchanted_golden_apple".equalsIgnoreCase(var1)) {
               return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, var6);
            }

            if ("minecraft:golden_apple".equalsIgnoreCase(var1)) {
               return new ItemStack(Items.GOLDEN_APPLE, var6);
            }

            Item var7 = Registries.ITEM.get(Identifier.of(var1));
            if (var7 != null && var7 != Items.AIR) {
               ItemStack var8 = new ItemStack(var7, var6);
               if (var0 != null && !var0.isEmpty() && !a(var7) && !var0.equalsIgnoreCase(var8.getName().getString())) {
                  var8.set(DataComponentTypes.CUSTOM_NAME, Text.literal(var0));
               }

               return var8;
            }
         }
      } catch (Throwable var6x) {
      }

      return ItemStack.EMPTY;
   }

   public static JsonObject q() {
      JsonObject var0 = new JsonObject();
      JsonArray var1 = new JsonArray();

      for (int var2 = 0; var2 < TOP_SLOTS.length; var2++) {
         ItemStack var3 = TOP_SLOTS[var2];
         if (var3 != null && !var3.isEmpty()) {
            JsonObject var4 = new JsonObject();
            var4.addProperty("slot", var2);
            var4.addProperty("name", Z(var3));
            var4.addProperty("id", Registries.ITEM.getId(var3.getItem()).toString());
            var4.addProperty("count", var3.getCount());
            var4.addProperty("durability", TOP_MIN_DURABILITY[var2]);
            var4.addProperty("sharpness", TOP_SHARPNESS[var2]);
            var4.addProperty("crit2", TOP_CRIT_2[var2]);
            var4.addProperty("efficiency", TOP_EFFICIENCY[var2]);
            var4.addProperty("protection", TOP_PROTECTION[var2]);
            var4.addProperty("unbreaking", TOP_UNBREAKING[var2]);
            var4.addProperty("respiration", TOP_RESPIRATION[var2]);
            var4.addProperty("depthStrider", TOP_DEPTH_STRIDER[var2]);
            var1.add(var4);
         }
      }

      var0.add("topSlots", var1);
      JsonArray var6 = new JsonArray();

      for (int var7 = 0; var7 < INVENTORY_SLOTS.length; var7++) {
         ItemStack var8 = INVENTORY_SLOTS[var7];
         if (var8 != null && !var8.isEmpty()) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("slot", var7);
            var5.addProperty("name", Z(var8));
            var5.addProperty("id", Registries.ITEM.getId(var8.getItem()).toString());
            var5.addProperty("count", var8.getCount());
            var5.addProperty("durability", INVENTORY_MIN_DURABILITY[var7]);
            var5.addProperty("sharpness", INVENTORY_SHARPNESS[var7]);
            var5.addProperty("crit2", INVENTORY_CRIT_2[var7]);
            var5.addProperty("efficiency", INVENTORY_EFFICIENCY[var7]);
            var5.addProperty("protection", INVENTORY_PROTECTION[var7]);
            var5.addProperty("unbreaking", INVENTORY_UNBREAKING[var7]);
            var5.addProperty("respiration", INVENTORY_RESPIRATION[var7]);
            var5.addProperty("depthStrider", INVENTORY_DEPTH_STRIDER[var7]);
            var6.add(var5);
         }
      }

      var0.add("inventorySlots", var6);
      return var0;
   }

   public static void a(JsonObject var0) {
      if (var0 != null) {
         Arrays.fill(TOP_SLOTS, ItemStack.EMPTY);
         Arrays.fill(INVENTORY_SLOTS, ItemStack.EMPTY);
         Arrays.fill(TOP_MIN_DURABILITY, 0);
         Arrays.fill(INVENTORY_MIN_DURABILITY, 0);
         Arrays.fill(TOP_SHARPNESS, 0);
         Arrays.fill(INVENTORY_SHARPNESS, 0);
         Arrays.fill(TOP_CRIT_2, false);
         Arrays.fill(INVENTORY_CRIT_2, false);
         Arrays.fill(TOP_EFFICIENCY, 0);
         Arrays.fill(INVENTORY_EFFICIENCY, 0);
         Arrays.fill(TOP_PROTECTION, 0);
         Arrays.fill(INVENTORY_PROTECTION, 0);
         Arrays.fill(TOP_UNBREAKING, 0);
         Arrays.fill(INVENTORY_UNBREAKING, 0);
         Arrays.fill(TOP_RESPIRATION, 0);
         Arrays.fill(INVENTORY_RESPIRATION, 0);
         Arrays.fill(TOP_DEPTH_STRIDER, 0);
         Arrays.fill(INVENTORY_DEPTH_STRIDER, 0);
         if (var0.has("topSlots")) {
            for (JsonElement var3 : var0.getAsJsonArray("topSlots")) {
               if (var3.isJsonObject()) {
                  JsonObject var4 = var3.getAsJsonObject();
                  int var5 = var4.has("slot") ? var4.get("slot").getAsInt() : -1;
                  if (var5 >= 0 && var5 < TOP_SLOTS.length) {
                     String var6 = var4.has("name") ? var4.get("name").getAsString() : "";
                     String var7 = var4.has("id") ? var4.get("id").getAsString() : "";
                     int var8 = var4.has("count") ? var4.get("count").getAsInt() : 1;
                     int var9 = var4.has("durability") ? var4.get("durability").getAsInt() : 0;
                     int var10 = var4.has("sharpness") ? var4.get("sharpness").getAsInt() : 0;
                     boolean var11 = var4.has("crit2") && var4.get("crit2").getAsBoolean();
                     int var12 = var4.has("efficiency") ? var4.get("efficiency").getAsInt() : 0;
                     int var13 = var4.has("protection") ? var4.get("protection").getAsInt() : 0;
                     int var14 = var4.has("unbreaking") ? var4.get("unbreaking").getAsInt() : 0;
                     int var15 = var4.has("respiration") ? var4.get("respiration").getAsInt() : 0;
                     int var16 = var4.has("depthStrider") ? var4.get("depthStrider").getAsInt() : 0;
                     ItemStack var17 = a(var6, var7, var8);
                     if (var17 != null && !var17.isEmpty()) {
                        TOP_SLOTS[var5] = var17;
                        TOP_MIN_DURABILITY[var5] = Math.clamp(var9, 0, 100);
                        TOP_SHARPNESS[var5] = Math.clamp(var10, 0, 9);
                        TOP_CRIT_2[var5] = var11;
                        TOP_EFFICIENCY[var5] = Math.clamp(var12, 0, 10);
                        TOP_PROTECTION[var5] = Math.clamp(var13, 0, 5);
                        TOP_UNBREAKING[var5] = Math.clamp(var14, 0, 5);
                        TOP_RESPIRATION[var5] = Math.clamp(var15, 0, 3);
                        TOP_DEPTH_STRIDER[var5] = Math.clamp(var16, 0, 3);
                     }
                  }
               }
            }
         }

         if (var0.has("inventorySlots")) {
            for (JsonElement var20 : var0.getAsJsonArray("inventorySlots")) {
               if (var20.isJsonObject()) {
                  JsonObject var21 = var20.getAsJsonObject();
                  int var22 = var21.has("slot") ? var21.get("slot").getAsInt() : -1;
                  if (var22 >= 0 && var22 < INVENTORY_SLOTS.length) {
                     String var23 = var21.has("name") ? var21.get("name").getAsString() : "";
                     String var24 = var21.has("id") ? var21.get("id").getAsString() : "";
                     int var25 = var21.has("count") ? var21.get("count").getAsInt() : 1;
                     int var26 = var21.has("durability") ? var21.get("durability").getAsInt() : 0;
                     int var27 = var21.has("sharpness") ? var21.get("sharpness").getAsInt() : 0;
                     boolean var28 = var21.has("crit2") && var21.get("crit2").getAsBoolean();
                     int var29 = var21.has("efficiency") ? var21.get("efficiency").getAsInt() : 0;
                     int var30 = var21.has("protection") ? var21.get("protection").getAsInt() : 0;
                     int var31 = var21.has("unbreaking") ? var21.get("unbreaking").getAsInt() : 0;
                     int var32 = var21.has("respiration") ? var21.get("respiration").getAsInt() : 0;
                     int var33 = var21.has("depthStrider") ? var21.get("depthStrider").getAsInt() : 0;
                     ItemStack var34 = a(var23, var24, var25);
                     if (var34 != null && !var34.isEmpty()) {
                        INVENTORY_SLOTS[var22] = var34;
                        INVENTORY_MIN_DURABILITY[var22] = Math.clamp(var26, 0, 100);
                        INVENTORY_SHARPNESS[var22] = Math.clamp(var27, 0, 9);
                        INVENTORY_CRIT_2[var22] = var28;
                        INVENTORY_EFFICIENCY[var22] = Math.clamp(var29, 0, 10);
                        INVENTORY_PROTECTION[var22] = Math.clamp(var30, 0, 5);
                        INVENTORY_UNBREAKING[var22] = Math.clamp(var31, 0, 5);
                        INVENTORY_RESPIRATION[var22] = Math.clamp(var32, 0, 3);
                        INVENTORY_DEPTH_STRIDER[var22] = Math.clamp(var33, 0, 3);
                     }
                  }
               }
            }
         }
      }
   }

   public static List<String> r() {
      ArrayList var0 = new ArrayList();

      try {
         if (!Files.exists(AUTOSBOR_CONFIGS_DIR)) {
            Files.createDirectories(AUTOSBOR_CONFIGS_DIR);
         }

         try (Stream<Path> var1 = Files.list(AUTOSBOR_CONFIGS_DIR)) {
            var1.filter(var0x -> var0x.getFileName().toString().endsWith(".json")).map(var0x -> {
               String var1x = var0x.getFileName().toString();
               return var1x.substring(0, var1x.length() - 5);
            }).sorted(String.CASE_INSENSITIVE_ORDER).forEach(var0::add);
         }
      } catch (Exception var6) {
      }

      return var0;
   }

   public static boolean c(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var4 = var0.trim();

         try {
            if (!Files.exists(AUTOSBOR_CONFIGS_DIR)) {
               Files.createDirectories(AUTOSBOR_CONFIGS_DIR);
            }

            Path var1 = AUTOSBOR_CONFIGS_DIR.resolve(var4 + ".json");
            JsonObject var2 = q();
            Files.writeString(var1, GSON.toJson(var2));
            return true;
         } catch (Exception var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean d(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var5 = var0.trim();

         try {
            Path var1 = AUTOSBOR_CONFIGS_DIR.resolve(var5 + ".json");
            if (!Files.exists(var1)) {
               return false;
            }

            String var2 = Files.readString(var1);
            JsonObject var3 = JsonParser.parseString(var2).getAsJsonObject();
            a(var3);
            ConfigManager.onSettingChanged();
            return true;
         } catch (Exception var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean e(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var3 = var0.trim();

         try {
            Path var1 = AUTOSBOR_CONFIGS_DIR.resolve(var3 + ".json");
            if (Files.exists(var1)) {
               Files.delete(var1);
               return true;
            }
         } catch (Exception var3x) {
         }

         return false;
      } else {
         return false;
      }
   }

   static {
      Arrays.fill(TOP_SLOTS, ItemStack.EMPTY);
      Arrays.fill(INVENTORY_SLOTS, ItemStack.EMPTY);
      Arrays.fill(TOP_MIN_DURABILITY, 0);
      Arrays.fill(INVENTORY_MIN_DURABILITY, 0);
      Arrays.fill(TOP_SHARPNESS, 0);
      Arrays.fill(INVENTORY_SHARPNESS, 0);
      Arrays.fill(TOP_CRIT_2, false);
      Arrays.fill(INVENTORY_CRIT_2, false);
      Arrays.fill(TOP_EFFICIENCY, 0);
      Arrays.fill(INVENTORY_EFFICIENCY, 0);
      Arrays.fill(TOP_PROTECTION, 0);
      Arrays.fill(INVENTORY_PROTECTION, 0);
      Arrays.fill(TOP_UNBREAKING, 0);
      Arrays.fill(INVENTORY_UNBREAKING, 0);
      Arrays.fill(TOP_RESPIRATION, 0);
      Arrays.fill(INVENTORY_RESPIRATION, 0);
      Arrays.fill(TOP_DEPTH_STRIDER, 0);
      Arrays.fill(INVENTORY_DEPTH_STRIDER, 0);
   }

   enum a {
      IDLE,
      SEARCHING,
      WAIT_AUCTION_SCREEN,
      WAIT_PURCHASE_DIALOG,
      WAIT_PURCHASE_COMPLETE;
   }

   record b(
      ItemStack stack,
      String query,
      int targetCount,
      int minDurability,
      int sharpness,
      boolean crit2,
      int efficiency,
      int protection,
      int unbreaking,
      int respiration,
      int depthStrider
   ) {
      public ItemStack getStack() {
         return this.stack;
      }

      public String getQuery() {
         return this.query;
      }

      public int getTargetCount() {
         return this.targetCount;
      }

      public int getMinDurability() {
         return this.minDurability;
      }

      public int getSharpness() {
         return this.sharpness;
      }

      public boolean isCrit2() {
         return this.crit2;
      }

      public int getEfficiency() {
         return this.efficiency;
      }

      public int getProtection() {
         return this.protection;
      }

      public int getUnbreaking() {
         return this.unbreaking;
      }

      public int getRespiration() {
         return this.respiration;
      }

      public int getDepthStrider() {
         return this.depthStrider;
      }
   }
}
