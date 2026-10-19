package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.Timer2;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.MultiSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventClickSlot;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.EnchantmentUtility;
import brokke.cc.util.ItemUtility;
import brokke.cc.util.ServerUtility;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "AH Helper", description = "Помощник в поиске дешевых предметов", category = Category.PLAYER)
public class AuctionHelper extends Function {
   private final ColorSetting cheapSlotColor = new ColorSetting("Цвет дешевого", new Color(64, 255, 64, 140));
   private final BooleanSetting wholeArmor = new BooleanSetting("Целая броня", false);
   private final ColorSetting wholeArmorColor = new ColorSetting("Цвет целой брони", new Color(0, 200, 255, 140)).visibleWhen(this.wholeArmor::isToggled);
   private final BooleanSetting sharpSword = new BooleanSetting("Меч Остр 7", false);
   private final ColorSetting sharpSwordColor = new ColorSetting("Цвет меча Остр 7", new Color(255, 80, 80, 140)).visibleWhen(this.sharpSword::isToggled);
   private final BooleanSetting showSpheres = new BooleanSetting("Показывать сферы", false);
   private final MultiSetting sphereTypes = new MultiSetting("Сферы", "Урон 3 Броня 2", "Броня 3 Урон 2").visibleWhen(this.showSpheres::isToggled);
   private final ColorSetting spheresColor = new ColorSetting("Цвет сфер", new Color(180, 70, 255, 140)).visibleWhen(this.showSpheres::isToggled);
   private final ModeSetting serverMode = new ModeSetting("Сервер", "Авто", "HolyWorld", "FunTime");
   private final BooleanSetting quickBuy = new BooleanSetting("Быстрая покупка (Shift + ЛКМ)", true);
   private final BooleanSetting autoConfirm = new BooleanSetting("Автоподтверждение", true).visibleWhen(this::l);
   private final BooleanSetting autoSell = new BooleanSetting("Продажа", true).visibleWhen(this::l);
   private final ModeSetting priceFind = new ModeSetting("Поиск цены", "/ah sell auto", "/ah search").visibleWhen(() -> this.l() && this.autoSell.isToggled());
   private final NumberSetting discount = new NumberSetting("Скидка", 0.95, 0.1, 1.0, 0.01)
      .withFormatter(var0 -> String.format("%.2f", var0))
      .visibleWhen(() -> this.l() && this.autoSell.isToggled());
   private final BindSetting sellKeyHw = new BindSetting("Кнопка продажи HW", -1).visibleWhen(() -> this.l() && this.autoSell.isToggled());
   private final Timer2 timer = new Timer2();
   private final Timer2 timer2 = new Timer2();
   private final Timer2 quickBuyTimer = new Timer2();
   private boolean quickBuyActive = false;
   private AuctionHelper.a sellState = AuctionHelper.a.IDLE;
   private ItemStack sellStack = ItemStack.EMPTY;
   private long targetPrice = -1L;
   private int intField = 1;
   private Slot lowSumSlotId = null;
   private final Slot[] wholeArmorSlots = new Slot[4];
   private Slot sharpSwordSlotId = null;
   private Slot sphereDmg3Arm2SlotId = null;
   private Slot sphereArm3Dmg2SlotId = null;
   private int lastScanRevision = -1;
   private static final Pattern NUM_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._]\\d{3})+|\\d+)");
   private static final Pattern COUNT_X = Pattern.compile("(?i)\\bx\\s*(\\d{1,4})\\b");
   private static final Pattern COUNT_PCS = Pattern.compile("(?i)\\b(\\d{1,4})\\s*(?:шт|штук|pcs)\\b");
   private static final Pattern COUNT_LABEL = Pattern.compile("(?i)(?:Количество|Count)[:\\s]+(\\d+)");
   private static final Pattern FUNTIME_PRICE_PATTERN = Pattern.compile("\\$([0-9][0-9\\s,._]*)");
   private static final Pattern HW_UNIT_PRICE_PATTERN = Pattern.compile("(?i)Цена за 1 ед\\.?:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern HW_TOTAL_PRICE_PATTERN = Pattern.compile("(?i)(?:^|\\n|\\r|[^а-яa-z])Цена:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern PROT_5_LORE_PATTERN = Pattern.compile("(?i)(?:^|[^а-яa-z])(?:защита|protection)(?:\\s*:\\s*|\\s+)([0-9]+|[ivx]+)\\b");
   private static final Pattern DURABILITY_PATTERN = Pattern.compile("(?i)(?:прочность|durability)[:\\s]+(\\d+)\\s*[/]\\s*(\\d+)");
   private static final String[] COMMON_CONTAINER_NAMES = new String[]{
      "Сундук",
      "Chest",
      "Бочка",
      "Barrel",
      "Шалкер",
      "Shulker",
      "Верстак",
      "Crafting",
      "Печь",
      "Furnace",
      "Дымящаяся",
      "Smoker",
      "Плавильная",
      "Blast",
      "Раздатчик",
      "Dispenser",
      "Выбрасыватель",
      "Dropper",
      "Воронка",
      "Hopper",
      "Стойка для брони",
      "Armor Stand",
      "Стол зачарования",
      "Enchanting",
      "Точило",
      "Grindstone",
      "Картографическая",
      "Cartography",
      "Кузнечный",
      "Smithing",
      "Ткацкий",
      "Loom",
      "Камнерез",
      "Stonecutter",
      "Варочная",
      "Brewing",
      "Наковальня",
      "Anvil",
      "Маяк",
      "Beacon",
      "Эндер-сундук",
      "Ender Chest"
   };

   public AuctionHelper() {
      this.wholeArmor.onChanged(() -> this.lastScanRevision = -1);
      this.sharpSword.onChanged(() -> this.lastScanRevision = -1);
      this.showSpheres.onChanged(() -> this.lastScanRevision = -1);
      this.addSettings(
         this.cheapSlotColor,
         this.wholeArmor,
         this.wholeArmorColor,
         this.sharpSword,
         this.sharpSwordColor,
         this.showSpheres,
         this.sphereTypes,
         this.spheresColor,
         this.serverMode,
         this.quickBuy,
         this.autoConfirm,
         this.autoSell,
         this.priceFind,
         this.discount,
         this.sellKeyHw
      );
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.r();
      this.quickBuyActive = false;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.r();
      this.quickBuyActive = false;
      this.lowSumSlotId = null;
      Arrays.fill(this.wholeArmorSlots, null);
      this.sharpSwordSlotId = null;
      this.sphereDmg3Arm2SlotId = null;
      this.sphereArm3Dmg2SlotId = null;
      this.lastScanRevision = -1;
   }

   @EventHandler
   public void onSlotClick(EventClickSlot var1) {
      if (mc.player != null
         && this.isActive()
         && this.quickBuy.isToggled()
         && var1.getSlotId() >= 0
         && this.a(mc.currentScreen)
         && (
            var1.getActionType() == SlotActionType.QUICK_MOVE
               || mc.getWindow() != null && (GLFW.glfwGetKey(mc.getWindow().getHandle(), 340) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 344) == 1)
         )
         && var1.getButton() == 0
         && mc.currentScreen instanceof GenericContainerScreen var3) {
         ScreenHandler var9 = var3.getScreenHandler();
         if (var9 != null) {
            int var5 = 6;
            if (var9 instanceof GenericContainerScreenHandler var6) {
               var5 = var6.getRows();
            }

            int var10 = Math.min(45, (var5 - 1) * 9);
            if (var1.getSlotId() < var10 && var1.getSlotId() < var9.slots.size()) {
               Slot var7 = var9.getSlot(var1.getSlotId());
               if (var7 != null && var7.hasStack()) {
                  ItemStack var8 = var7.getStack();
                  if (!var8.isEmpty() && !this.u(var8)) {
                     var1.cancel();
                     this.quickBuyActive = true;
                     this.quickBuyTimer.a();
                     if (mc.interactionManager != null) {
                        mc.interactionManager.clickSlot(var1.getWindowId(), var1.getSlotId(), 0, SlotActionType.PICKUP, mc.player);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (mc.player != null
         && this.l()
         && this.autoSell.isToggled()
         && this.sellKeyHw.getKey() != -1
         && var1.getAction() == 1
         && var1.getKey() == this.sellKeyHw.getKey()) {
         this.n();
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.player != null && var1.getPacket() instanceof GameMessageS2CPacket var2) {
         String var5 = var2.content() == null ? "" : var2.content().getString();
         if (var5.isEmpty()) {
            return;
         }

         String var4 = this.h(var5).replace(' ', ' ').trim();
         if ((this.autoConfirm.isToggled() || this.sellState == AuctionHelper.a.WAIT_PRICE_SCREEN) && var4.startsWith("▶ Введите /ah sell auto confirm,")) {
            if (mc.player.networkHandler != null) {
               mc.player.networkHandler.sendChatCommand("ah sell auto confirm");
            }

            this.r();
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (this.quickBuyActive) {
            if (this.quickBuyTimer.a(2000L)) {
               this.quickBuyActive = false;
            } else if (mc.currentScreen != null && this.b(mc.currentScreen)) {
               this.p();
            }
         }

         if (this.sellState == AuctionHelper.a.WAIT_PRICE_SCREEN) {
            if (this.timer.a(5000L)) {
               this.k("§cНе удалось найти цену на аукционе§r");
               this.q();
               this.r();
            } else if (this.timer2.a(250L) && this.a(mc.currentScreen)) {
               Integer var3 = this.o();
               if (var3 != null && var3 > 0) {
                  this.targetPrice = Math.max(1L, Math.round(var3.intValue() * this.discount.getValue()));
                  this.q();
                  this.k("§aНайдена цена: §f" + this.targetPrice + "§r");
                  this.sellState = AuctionHelper.a.WAIT_SELL_COMMAND;
                  this.timer.a();
                  this.timer2.a();
               } else if (this.timer.a(5000L)) {
                  this.k("§cНе удалось найти цену на аукционе§r");
                  this.q();
                  this.r();
               }
            }
         } else if (this.sellState == AuctionHelper.a.WAIT_SELL_COMMAND) {
            if (this.targetPrice > 0L && mc.player.networkHandler != null) {
               ItemStack var2 = mc.player.getMainHandStack();
               if (var2.isEmpty() || !ItemStack.areItemsAndComponentsEqual(var2, this.sellStack)) {
                  this.k("§cДержи тот же предмет в руке§r");
                  this.r();
               } else if ((mc.currentScreen == null || this.timer.a(1500L)) && this.timer2.a(250L)) {
                  mc.player.networkHandler.sendChatCommand("ah sell " + this.targetPrice);
                  this.r();
               }
            } else {
               this.r();
            }
         }
      }
   }

   private void n() {
      if (this.sellState == AuctionHelper.a.IDLE && this.l() && mc.player != null) {
         ItemStack var1 = mc.player.getMainHandStack();
         if (var1 != null && !var1.isEmpty()) {
            this.sellStack = var1.copy();
            this.targetPrice = -1L;
            this.intField = 1;
            this.timer.a();
            this.timer2.a();
            if (mc.player.networkHandler != null) {
               if (this.priceFind.isSelected("/ah sell auto")) {
                  mc.player.networkHandler.sendChatCommand("ah sell auto");
               } else {
                  mc.player.networkHandler.sendChatCommand("ah search");
               }

               this.sellState = AuctionHelper.a.WAIT_PRICE_SCREEN;
            }
         } else {
            this.k("§cВозьми предмет в руку§r");
         }
      }
   }

   private Integer o() {
      if (mc.player != null && mc.player.currentScreenHandler != null && !this.sellStack.isEmpty()) {
         ScreenHandler var1 = mc.player.currentScreenHandler;
         ArrayList var2 = new ArrayList();
         int var3 = Math.min(45, var1.slots.size());

         for (int var4 = 0; var4 < var3; var4++) {
            Slot var5 = var1.getSlot(var4);
            ItemStack var6 = var5.getStack();
            int var7;
            if (var6 != null
               && !var6.isEmpty()
               && var6.getItem() != Items.AIR
               && !this.u(var6)
               && !this.t(var6)
               && (var7 = this.b(var6)) != Integer.MAX_VALUE
               && var7 > 0) {
               var2.add(var7);
            }
         }

         if (var2.isEmpty()) {
            return null;
         }

         var2.sort(Comparator.naturalOrder());
         int var8 = Math.min(Math.max(0, this.intField - 1), var2.size() - 1);
         long var9 = (long)((Integer)var2.get(var8)).intValue() * Math.max(1, this.sellStack.getCount());
         return (int)Math.min(2147483647L, var9);
      } else {
         return null;
      }
   }

   public void a(DrawContext var1, HandledScreen<?> var2, int var3, int var4) {
      if (mc.player != null && this.isActive()) {
         if (this.quickBuyActive && this.b(var2)) {
            if (this.quickBuyTimer.a(2000L)) {
               this.quickBuyActive = false;
            } else {
               this.p();
            }
         }

         if (!this.a(var2)) {
            this.lowSumSlotId = null;
            Arrays.fill(this.wholeArmorSlots, null);
            this.sharpSwordSlotId = null;
            this.sphereDmg3Arm2SlotId = null;
            this.sphereArm3Dmg2SlotId = null;
         } else {
            ScreenHandler var5 = var2.getScreenHandler();
            if (var5 != null) {
               this.lowSumSlotId = null;
               Arrays.fill(this.wholeArmorSlots, null);
               this.sharpSwordSlotId = null;
               this.sphereDmg3Arm2SlotId = null;
               this.sphereArm3Dmg2SlotId = null;
               int var6 = Integer.MAX_VALUE;
               int[] var7 = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
               int var8 = Integer.MAX_VALUE;
               int var9 = Integer.MAX_VALUE;
               int var10 = Integer.MAX_VALUE;
               int var11 = Math.min(45, var5.slots.size());

               for (int var12 = 0; var12 < var11; var12++) {
                  Slot var13 = var5.getSlot(var12);
                  ItemStack var14 = var13.getStack();
                  if (var14 != null && !var14.isEmpty() && !this.u(var14)) {
                     int var15;
                     if ((var15 = this.b(var14)) != Integer.MAX_VALUE && var15 > 0 && var15 < var6) {
                        var6 = var15;
                        this.lowSumSlotId = var13;
                     }

                     int var16;
                     if (this.wholeArmor.isToggled()
                        && this.l(var14)
                        && (var16 = this.m(var14)) >= 0
                        && var15 != Integer.MAX_VALUE
                        && var15 > 0
                        && var15 < var7[var16]) {
                        var7[var16] = var15;
                        this.wholeArmorSlots[var16] = var13;
                     }

                     if (this.sharpSword.isToggled() && this.i(var14) && var15 != Integer.MAX_VALUE && var15 > 0 && var15 < var8) {
                        var8 = var15;
                        this.sharpSwordSlotId = var13;
                     }

                     if (this.showSpheres.isToggled()) {
                        if (this.sphereTypes.isOptionEnabled("Урон 3 Броня 2") && this.e(var14) && var15 != Integer.MAX_VALUE && var15 > 0 && var15 < var9) {
                           var9 = var15;
                           this.sphereDmg3Arm2SlotId = var13;
                        }

                        if (this.sphereTypes.isOptionEnabled("Броня 3 Урон 2") && this.f(var14) && var15 != Integer.MAX_VALUE && var15 > 0 && var15 < var10) {
                           var10 = var15;
                           this.sphereArm3Dmg2SlotId = var13;
                        }
                     }
                  }
               }

               if (this.lowSumSlotId != null) {
                  this.a(var1, var3, var4, this.lowSumSlotId, this.cheapSlotColor.getArgb());
               }

               if (this.wholeArmor.isToggled()) {
                  for (Slot var20 : this.wholeArmorSlots) {
                     if (var20 != null) {
                        this.a(var1, var3, var4, var20, this.wholeArmorColor.getArgb());
                     }
                  }
               }

               if (this.sharpSword.isToggled() && this.sharpSwordSlotId != null) {
                  this.a(var1, var3, var4, this.sharpSwordSlotId, this.sharpSwordColor.getArgb());
               }

               if (this.showSpheres.isToggled()) {
                  if (this.sphereDmg3Arm2SlotId != null) {
                     this.a(var1, var3, var4, this.sphereDmg3Arm2SlotId, this.spheresColor.getArgb());
                  }

                  if (this.sphereArm3Dmg2SlotId != null && this.sphereArm3Dmg2SlotId != this.sphereDmg3Arm2SlotId) {
                     this.a(var1, var3, var4, this.sphereArm3Dmg2SlotId, this.spheresColor.getArgb());
                  }
               }
            }
         }
      }
   }

   private boolean e(ItemStack var1) {
      int[] var2 = this.h(var1);
      return var2[0] == 3 && var2[1] == 2;
   }

   private boolean f(ItemStack var1) {
      int[] var2 = this.h(var1);
      return var2[0] == 2 && var2[1] == 3;
   }

   private boolean g(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (var1.isOf(Items.PLAYER_HEAD)) {
         return true;
      }

      if (ItemUtility.g(var1)) {
         return true;
      }

      String var2 = this.w(var1).toLowerCase(Locale.ROOT);
      return var2.contains("сфер") || var2.contains("sphere");
   }

   private int[] h(ItemStack var1) {
      if (!this.g(var1)) {
         return new int[]{0, 0};
      }

      int var2 = 0;
      int var3 = 0;

      for (String var6 : this.v(var1)) {
         String var7 = this.h(var6).trim().toLowerCase(Locale.ROOT);
         if (var2 == 0 && this.a(var7)) {
            var2 = this.c(var7);
         }

         if (var3 == 0 && this.b(var7)) {
            var3 = this.c(var7);
         }
      }

      String var10 = this.w(var1).toLowerCase(Locale.ROOT);
      if (var2 == 0 || var3 == 0) {
         if ((var10.contains("урон") || var10.contains("damage") || var10.contains("сила")) && var2 == 0) {
            var2 = this.a(var10, "урон", "damage", "сила");
         }

         if ((var10.contains("броня") || var10.contains("armor") || var10.contains("защит")) && var3 == 0) {
            var3 = this.a(var10, "броня", "armor", "защит");
         }
      }

      NbtCompound var11 = ItemUtility.b(var1);
      if (var11 != null) {
         String var12 = var11.toString().toLowerCase(Locale.ROOT);
         if (var2 == 0) {
            int var8;
            if ((var8 = this.a(var12, "hms-damage")) == 0) {
               var8 = this.a(var12, "damage");
            }

            if (var8 == 0) {
               var8 = this.a(var12, "sdamage");
            }

            if (var8 > 0) {
               var2 = var8;
            }
         }

         if (var3 == 0) {
            int var14;
            if ((var14 = this.a(var12, "hms-armor")) == 0) {
               var14 = this.a(var12, "hms-protection");
            }

            if (var14 == 0) {
               var14 = this.a(var12, "armor");
            }

            if (var14 == 0) {
               var14 = this.a(var12, "sarmor");
            }

            if (var14 > 0) {
               var3 = var14;
            }
         }
      }

      AttributeModifiersComponent var13 = var1.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (var13 != null) {
         String var15 = var13.toString().toLowerCase(Locale.ROOT);
         int var9;
         if (var2 == 0 && (var15.contains("attack_damage") || var15.contains("damage")) && (var9 = this.b(var15, "attack_damage")) > 0) {
            var2 = var9;
         }

         if (var3 == 0 && var15.contains("armor") && (var9 = this.b(var15, "armor")) > 0) {
            var3 = var9;
         }
      }

      return new int[]{var2, var3};
   }

   private boolean a(String var1) {
      return var1 != null && !var1.isEmpty()
         ? var1.contains("урон") || var1.contains("damage") || var1.contains("сила") || var1.contains("силы") || var1.contains("strength")
         : false;
   }

   private boolean b(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         return !var1.contains("снарядов") && !var1.contains("огня") && !var1.contains("взрыв") && !var1.contains("падения") && !var1.contains("падение")
            ? var1.contains("броня")
               || var1.contains("брони")
               || var1.contains("защит")
               || var1.contains("armor")
               || var1.contains("protection")
               || var1.contains("сопротивлен")
            : false;
      } else {
         return false;
      }
   }

   private int c(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = " "
            + var1.toLowerCase(Locale.ROOT)
               .replace('Ⅰ', '1')
               .replace('Ⅱ', '2')
               .replace('Ⅲ', '3')
               .replace('Ⅳ', '4')
               .replace('Ⅴ', '5')
               .replace('Ⅵ', '6')
               .replace('Ⅶ', '7')
               .replace('Ⅷ', '8')
               .replace('Ⅸ', '9')
               .replace('Ⅹ', 'x')
            + " ";
         if (var2.contains("viii") || var2.contains("80%") || var2.contains("+8") || var2.contains(" 8 ")) {
            return 8;
         }

         if (var2.contains("vii") || var2.contains("70%") || var2.contains("+7") || var2.contains(" 7 ")) {
            return 7;
         }

         if (var2.contains("vi") || var2.contains("60%") || var2.contains("+6") || var2.contains(" 6 ")) {
            return 6;
         }

         if (var2.contains("iv") || var2.contains("40%") || var2.contains("+4") || var2.contains(" 4 ")) {
            return 4;
         }

         if (var2.contains("iii") || var2.contains("30%") || var2.contains("+3") || var2.contains(" 3 ")) {
            return 3;
         }

         if (var2.contains("ii") || var2.contains("20%") || var2.contains("+2") || var2.contains(" 2 ")) {
            return 2;
         }

         if (var2.contains("ix") || var2.contains("90%") || var2.contains("+9") || var2.contains(" 9 ")) {
            return 9;
         }

         if (var2.contains(" x ") || var2.contains("100%") || var2.contains("+10") || var2.contains(" 10 ")) {
            return 10;
         }

         if (!var2.contains(" v ") && !var2.contains("50%") && !var2.contains("+5") && !var2.contains(" 5 ")) {
            if (!var2.contains(" i ") && !var2.contains("10%") && !var2.contains("+1") && !var2.contains(" 1 ")) {
               Matcher var3 = NUM_PATTERN.matcher(var2);

               while (var3.find()) {
                  long var4;
                  if ((var4 = this.i(var3.group(1))) >= 1L && var4 <= 10L) {
                     return (int)var4;
                  }
               }

               return 0;
            } else {
               return 1;
            }
         } else {
            return 5;
         }
      } else {
         return 0;
      }
   }

   private int a(String var1, String... var2) {
      if (var1 != null && !var1.isEmpty()) {
         String var3 = var1.toLowerCase(Locale.ROOT);
         int var4 = -1;
         int var5 = 0;

         for (String var9 : var2) {
            int var10;
            if ((var10 = var3.indexOf(var9)) != -1 && (var4 == -1 || var10 < var4)) {
               var4 = var10;
               var5 = var9.length();
            }
         }

         if (var4 == -1) {
            return 0;
         }

         String var13 = var3.substring(var4 + var5);
         int var14 = var13.length();

         for (String var11 : new String[]{"урон", "damage", "сила", "броня", "armor", "защит", "скорост", "speed", "|", ",", "/", ";", "•"}) {
            int var12;
            if ((var12 = var13.indexOf(var11)) != -1 && var12 < var14) {
               var14 = var12;
            }
         }

         String var16 = var13.substring(0, var14).trim();
         return this.c(var16);
      } else {
         return 0;
      }
   }

   private int a(String var1, String var2) {
      if (var1 == null) {
         return 0;
      }

      int var3;
      if ((var3 = var1.indexOf(var2)) == -1) {
         return 0;
      }

      int var4;
      if ((var4 = var1.indexOf("\"lvl\":", var3)) == -1) {
         var4 = var1.indexOf("lvl:", var3);
      }

      if (var4 != -1 && var4 - var3 < 60) {
         int var5 = var4 + (var1.charAt(var4) == '"' ? 6 : 4);

         while (var5 < var1.length() && !Character.isDigit(var1.charAt(var5))) {
            var5++;
         }

         int var6 = var5;

         while (var6 < var1.length() && Character.isDigit(var1.charAt(var6))) {
            var6++;
         }

         if (var6 > var5) {
            try {
               return Integer.parseInt(var1.substring(var5, var6));
            } catch (Exception var8) {
            }
         }
      }

      return 0;
   }

   private int b(String var1, String var2) {
      if (var1 == null) {
         return 0;
      }

      int var3;
      int var4;
      if ((var3 = var1.indexOf(var2)) != -1 && (var4 = var1.indexOf("amount=", var3)) != -1) {
         int var5;
         int var6 = var5 = var4 + 7;

         while (var6 < var1.length() && (Character.isDigit(var1.charAt(var6)) || var1.charAt(var6) == '.')) {
            var6++;
         }

         if (var6 > var5) {
            try {
               double var7;
               if ((var7 = Double.parseDouble(var1.substring(var5, var6))) >= 2.5 && var7 <= 3.5) {
                  return 3;
               }

               if (var7 >= 1.5 && var7 < 2.5) {
                  return 2;
               }

               if (var7 >= 3.5) {
                  return 4;
               }

               if (var7 >= 0.5 && var7 < 1.5) {
                  return 1;
               }
            } catch (Exception var9) {
            }
         }
      }

      return 0;
   }

   private boolean i(ItemStack var1) {
      return this.j(var1) && this.k(var1);
   }

   private boolean j(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
         if (var2.endsWith("_sword")) {
            return true;
         }

         String var3 = this.w(var1).toLowerCase(Locale.ROOT);
         return var3.contains("меч") || var3.contains("sword");
      } else {
         return false;
      }
   }

   private boolean k(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            ItemEnchantmentsComponent var2 = var1.get(DataComponentTypes.ENCHANTMENTS);
            if (var2 == null && var1.isOf(Items.ENCHANTED_BOOK)) {
               var2 = var1.get(DataComponentTypes.STORED_ENCHANTMENTS);
            }

            if (var2 != null) {
               for (Entry var4 : var2.getEnchantmentEntries()) {
                  if (var4.getIntValue() >= 7) {
                     String var5 = ((RegistryEntry<?>)var4.getKey()).getIdAsString().toLowerCase(Locale.ROOT);
                     if (var5.endsWith(":sharpness") || var5.equals("sharpness") || var5.contains("sharpness")) {
                        return true;
                     }
                  }
               }
            }
         } catch (Exception var7) {
         }

         try {
            if (EnchantmentUtility.a(var1, Enchantments.SHARPNESS) >= 7) {
               return true;
            }
         } catch (Exception var6) {
         }

         for (String var11 : this.v(var1)) {
            if (this.d(var11)) {
               return true;
            }
         }

         NbtCompound var10 = ItemUtility.b(var1);
         if (var10 != null) {
            String var12 = var10.toString().toLowerCase(Locale.ROOT);
            if (var12.contains("sharpness") && (var12.contains("lvl:7") || var12.contains("lvl: 7") || var12.contains("lvl:7s") || var12.contains("level:7"))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean d(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = this.h(var1).trim().toLowerCase(Locale.ROOT);
         if (!var2.contains("острота") && !var2.contains("sharpness")) {
            return false;
         }

         String var6 = var2.replace('Ⅶ', '7')
            .replace('Ⅷ', '8')
            .replace('Ⅸ', '9')
            .replace('Ⅹ', 'x')
            .replace('Ⅵ', '6')
            .replace('Ⅴ', '5')
            .replace('Ⅳ', '4')
            .replace('Ⅲ', '3')
            .replace('Ⅱ', '2')
            .replace('Ⅰ', '1');
         if ((
               var6.contains("острота vi")
                  || var6.contains("острота 6")
                  || var6.contains("sharpness vi")
                  || var6.contains("sharpness 6")
                  || var6.contains("острота v")
                  || var6.contains("острота 5")
                  || var6.contains("sharpness v")
                  || var6.contains("sharpness 5")
                  || var6.contains("острота iv")
                  || var6.contains("острота 4")
                  || var6.contains("sharpness iv")
                  || var6.contains("sharpness 4")
                  || var6.contains("острота iii")
                  || var6.contains("острота 3")
                  || var6.contains("sharpness iii")
                  || var6.contains("sharpness 3")
                  || var6.contains("острота ii")
                  || var6.contains("острота 2")
                  || var6.contains("sharpness ii")
                  || var6.contains("sharpness 2")
                  || var6.contains("острота i")
                  || var6.contains("острота 1")
                  || var6.contains("sharpness i")
                  || var6.contains("sharpness 1")
            )
            && !var6.contains("острота vii")
            && !var6.contains("острота 7")
            && !var6.contains("sharpness vii")
            && !var6.contains("sharpness 7")
            && !var6.contains("viii")
            && !var6.contains("ix")
            && !var6.contains("x")
            && !var6.contains("8")
            && !var6.contains("9")
            && !var6.contains("10")) {
            return false;
         }

         if (!var6.contains("острота vii")
            && !var6.contains("острота 7")
            && !var6.contains("sharpness vii")
            && !var6.contains("sharpness 7")
            && !var6.contains("острота viii")
            && !var6.contains("острота 8")
            && !var6.contains("sharpness viii")
            && !var6.contains("sharpness 8")
            && !var6.contains("острота ix")
            && !var6.contains("острота 9")
            && !var6.contains("sharpness ix")
            && !var6.contains("sharpness 9")
            && !var6.contains("острота x")
            && !var6.contains("острота 10")
            && !var6.contains("sharpness x")
            && !var6.contains("sharpness 10")) {
            Matcher var3 = NUM_PATTERN.matcher(var6);

            while (var3.find()) {
               long var4;
               if ((var4 = this.i(var3.group(1))) >= 7L && var4 <= 100L) {
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

   private boolean l(ItemStack var1) {
      return this.n(var1) && this.o(var1) && this.p(var1);
   }

   private int m(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
         String var3 = this.w(var1).toLowerCase(Locale.ROOT);
         if (var2.endsWith("_helmet") || var2.equals("turtle_helmet") || var3.contains("шлем") || var3.contains("helmet")) {
            return 0;
         }

         if (var2.endsWith("_chestplate") || var3.contains("нагрудник") || var3.contains("chestplate")) {
            return 1;
         }

         if (var2.endsWith("_leggings") || var3.contains("поножи") || var3.contains("leggings")) {
            return 2;
         }

         if (!var2.endsWith("_boots") && !var3.contains("ботинки") && !var3.contains("boots")) {
            EquipmentSlot var4 = ItemUtility.i(var1);
            if (var4 == EquipmentSlot.HEAD) {
               return 0;
            } else if (var4 == EquipmentSlot.CHEST) {
               return 1;
            } else if (var4 == EquipmentSlot.LEGS) {
               return 2;
            } else {
               return var4 == EquipmentSlot.FEET ? 3 : -1;
            }
         } else {
            return 3;
         }
      } else {
         return -1;
      }
   }

   private boolean n(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (!var1.isOf(Items.ELYTRA)
         && !var1.isOf(Items.CARVED_PUMPKIN)
         && !var1.isOf(Items.PLAYER_HEAD)
         && !var1.isOf(Items.SKELETON_SKULL)
         && !var1.isOf(Items.WITHER_SKELETON_SKULL)
         && !var1.isOf(Items.ZOMBIE_HEAD)
         && !var1.isOf(Items.CREEPER_HEAD)
         && !var1.isOf(Items.DRAGON_HEAD)
         && !var1.isOf(Items.PIGLIN_HEAD)) {
         String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
         if (!var2.endsWith("_helmet")
            && !var2.endsWith("_chestplate")
            && !var2.endsWith("_leggings")
            && !var2.endsWith("_boots")
            && !var2.equals("turtle_helmet")) {
            String var3 = this.w(var1).toLowerCase(Locale.ROOT);
            return !var3.contains("шлем")
                  && !var3.contains("нагрудник")
                  && !var3.contains("поножи")
                  && !var3.contains("ботинки")
                  && !var3.contains("helmet")
                  && !var3.contains("chestplate")
                  && !var3.contains("leggings")
                  && !var3.contains("boots")
               ? ItemUtility.i(var1) != null
               : true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean o(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         int var2;
         if ((var2 = var1.getMaxDamage()) > 0) {
            int var3 = var1.getDamage();
            if (var2 - var3 <= var2 * 0.4) {
               return false;
            }
         }

         for (String var5 : this.v(var1)) {
            String var6 = this.h(var5);
            Matcher var7 = DURABILITY_PATTERN.matcher(var6);
            if (var7.find()) {
               long var8 = this.i(var7.group(1));
               long var10;
               if ((var10 = this.i(var7.group(2))) > 0L && var8 <= var10 * 0.4) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean p(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            ItemEnchantmentsComponent var2 = var1.get(DataComponentTypes.ENCHANTMENTS);
            if (var2 == null && var1.isOf(Items.ENCHANTED_BOOK)) {
               var2 = var1.get(DataComponentTypes.STORED_ENCHANTMENTS);
            }

            if (var2 != null) {
               for (Entry var4 : var2.getEnchantmentEntries()) {
                  if (var4.getIntValue() >= 5) {
                     String var5 = ((RegistryEntry<?>)var4.getKey()).getIdAsString().toLowerCase(Locale.ROOT);
                     if ((var5.endsWith(":protection") || var5.equals("protection") || var5.contains("protection"))
                        && !var5.contains("projectile")
                        && !var5.contains("fire")
                        && !var5.contains("blast")
                        && !var5.contains("fall")) {
                        return true;
                     }
                  }
               }
            }
         } catch (Exception var7) {
         }

         try {
            if (EnchantmentUtility.a(var1, Enchantments.PROTECTION) >= 5) {
               return true;
            }
         } catch (Exception var6) {
         }

         for (String var11 : this.v(var1)) {
            if (this.e(var11)) {
               return true;
            }
         }

         NbtCompound var10 = ItemUtility.b(var1);
         if (var10 != null) {
            String var12 = var10.toString().toLowerCase(Locale.ROOT);
            if (var12.contains("protection") && (var12.contains("lvl:5") || var12.contains("lvl: 5") || var12.contains("lvl:5s") || var12.contains("level:5"))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean e(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = this.h(var1).trim().toLowerCase(Locale.ROOT);
         if (!var2.contains("защита") && !var2.contains("protection")) {
            return false;
         }

         if (!var2.contains("от снарядов")
            && !var2.contains("снарядов")
            && !var2.contains("от огня")
            && !var2.contains("огня")
            && !var2.contains("от взрывов")
            && !var2.contains("взрывоустойчивость")
            && !var2.contains("projectile")
            && !var2.contains("fire")
            && !var2.contains("blast")
            && !var2.contains("падения")
            && !var2.contains("fall")
            && !var2.contains("feather")) {
            String var6 = var2.replace('Ⅴ', 'v')
               .replace('Ⅵ', 'v')
               .replace('Ⅶ', 'v')
               .replace('Ⅷ', 'v')
               .replace('Ⅸ', 'x')
               .replace('Ⅹ', 'x')
               .replace('Ⅳ', '4')
               .replace('Ⅲ', '3')
               .replace('Ⅱ', '2')
               .replace('Ⅰ', '1');
            if ((
                  var6.contains("защита iv")
                     || var6.contains("защита 4")
                     || var6.contains("protection iv")
                     || var6.contains("protection 4")
                     || var6.contains("защита iii")
                     || var6.contains("защита 3")
                     || var6.contains("protection iii")
                     || var6.contains("protection 3")
                     || var6.contains("защита ii")
                     || var6.contains("защита 2")
                     || var6.contains("protection ii")
                     || var6.contains("protection 2")
                     || var6.contains("защита i")
                     || var6.contains("защита 1")
                     || var6.contains("protection i")
                     || var6.contains("protection 1")
               )
               && !var6.contains("защита v")
               && !var6.contains("защита 5")
               && !var6.contains("protection v")
               && !var6.contains("protection 5")
               && !var6.contains("vi")
               && !var6.contains("vii")
               && !var6.contains("viii")
               && !var6.contains("ix")
               && !var6.contains("x")
               && !var6.contains("6")
               && !var6.contains("7")
               && !var6.contains("8")
               && !var6.contains("9")
               && !var6.contains("10")) {
               return false;
            }

            if (!var6.contains("защита v")
               && !var6.contains("защита 5")
               && !var6.contains("protection v")
               && !var6.contains("protection 5")
               && !var6.contains("защита vi")
               && !var6.contains("защита vii")
               && !var6.contains("защита viii")
               && !var6.contains("защита ix")
               && !var6.contains("защита x")
               && !var6.contains("protection vi")
               && !var6.contains("protection vii")
               && !var6.contains("protection viii")
               && !var6.contains("protection ix")
               && !var6.contains("protection x")) {
               Matcher var3 = NUM_PATTERN.matcher(var6);

               while (var3.find()) {
                  long var4;
                  if ((var4 = this.i(var3.group(1))) >= 5L && var4 <= 100L) {
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
      } else {
         return false;
      }
   }

   private void a(DrawContext var1, int var2, int var3, Slot var4, int var5) {
      int var6 = var2 + var4.x;
      int var7 = var3 + var4.y;
      var1.fill(var6, var7, var6 + 16, var7 + 16, var5);
   }

   public String a(ItemStack var1) {
      if (!this.isActive() || !this.k() || var1 == null || var1.isEmpty()) {
         return null;
      } else if (mc.currentScreen != null && mc.currentScreen instanceof HandledScreen) {
         int var2;
         return (var2 = this.d(var1)) > 1 && var2 != var1.getCount() ? String.valueOf(var2) : null;
      } else {
         return null;
      }
   }

   public boolean k() {
      return this.autoSell.isToggled() && this.autoSell.isVisible();
   }

   public int b(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         int var2;
         if ((var2 = this.c(var1)) != Integer.MAX_VALUE && var2 > 0) {
            int var3 = Math.max(1, this.d(var1));
            return Math.max(1, var2 / var3);
         } else {
            return Integer.MAX_VALUE;
         }
      } else {
         return Integer.MAX_VALUE;
      }
   }

   public int c(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         int var2;
         if (this.m() && (var2 = this.q(var1)) > 0) {
            return var2;
         } else {
            return (var2 = this.r(var1)) > 0 ? var2 : this.s(var1);
         }
      } else {
         return Integer.MAX_VALUE;
      }
   }

   private int q(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         ComponentMap var2 = var1.getComponents();
         if (var2 != null) {
            String var3 = var2.toString();
            int var4;
            int var5;
            if ((var4 = var3.indexOf("literal{ $")) != -1 && (var5 = var3.indexOf("}[style={color=green}]", var4)) != -1) {
               String var6 = var3.substring(var4 + 10, var5).replaceAll("[^0-9]", "");

               try {
                  long var7;
                  if ((var7 = Long.parseLong(var6)) > 0L) {
                     return (int)Math.min(2147483647L, var7);
                  }
               } catch (Exception var9) {
               }
            }
         }

         for (String var13 : this.v(var1)) {
            int var15;
            if ((var15 = this.f(var13)) > 0) {
               return var15;
            }
         }

         String var12 = this.w(var1);
         int var14;
         return (var14 = this.f(var12)) > 0 ? var14 : -1;
      } else {
         return -1;
      }
   }

   private int r(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         for (String var4 : this.v(var1)) {
            String var5 = this.h(var4).replace(' ', ' ');
            Matcher var6 = HW_UNIT_PRICE_PATTERN.matcher(var5);
            long var7;
            if (var6.find() && (var7 = this.i(var6.group(1))) > 0L) {
               int var9 = Math.max(1, this.d(var1));
               return (int)Math.min(2147483647L, var7 * var9);
            }

            Matcher var10 = HW_TOTAL_PRICE_PATTERN.matcher(var5);
            long var8;
            if (var10.find() && (var8 = this.i(var10.group(1))) > 0L) {
               return (int)Math.min(2147483647L, var8);
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private int s(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         List var2 = this.v(var1);
         int var3 = Math.max(1, this.d(var1));

         for (String var5 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var6 = this.h(var5).toLowerCase(Locale.ROOT);
            if (var6.contains("цена") || var6.contains("price") || var6.contains("стоим") || var6.contains("$")) {
               Matcher var7 = NUM_PATTERN.matcher(var6);
               long var8;
               if (var7.find() && (var8 = this.i(var7.group(1))) > 0L) {
                  if (var6.contains("за шт") || var6.contains("за 1") || var6.contains("/шт")) {
                     var8 *= var3;
                  }

                  return (int)Math.min(2147483647L, var8);
               }
            }
         }

         return Integer.MAX_VALUE;
      } else {
         return Integer.MAX_VALUE;
      }
   }

   private int f(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         Matcher var2 = FUNTIME_PRICE_PATTERN.matcher(this.h(var1));
         long var3;
         return var2.find() && (var3 = this.i(var2.group(1))) > 0L ? (int)Math.min(2147483647L, var3) : -1;
      } else {
         return -1;
      }
   }

   public int d(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         for (String var4 : this.v(var1)) {
            String var5 = this.h(var4);
            Matcher var6 = COUNT_X.matcher(var5);
            int var7;
            if (var6.find() && (var7 = this.j(var6.group(1))) > 0) {
               return var7;
            }

            Matcher var15 = COUNT_PCS.matcher(var5);
            int var8;
            if (var15.find() && (var8 = this.j(var15.group(1))) > 0) {
               return var8;
            }

            Matcher var16 = COUNT_LABEL.matcher(var5);
            int var9;
            if (var16.find() && (var9 = this.j(var16.group(1))) > 0) {
               return var9;
            }
         }

         String var10 = this.w(var1);
         Matcher var11 = COUNT_X.matcher(var10);
         int var12;
         if (var11.find() && (var12 = this.j(var11.group(1))) > 0) {
            return var12;
         }

         Matcher var13 = COUNT_PCS.matcher(var10);
         int var14;
         return var13.find() && (var14 = this.j(var13.group(1))) > 0 ? var14 : Math.max(1, var1.getCount());
      } else {
         return 1;
      }
   }

   private boolean t(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         LoreComponent var2 = var1.get(DataComponentTypes.LORE);
         if (var2 != null) {
            for (Text var4 : var2.lines()) {
               if (var4.getString().toLowerCase(Locale.ROOT).contains("только полностью")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean u(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.getItem() != Items.BARRIER && var1.getItem() != Items.AIR) {
            String var2 = this.w(var1).toLowerCase(Locale.ROOT);
            return var2.contains("назад") || var2.contains("вперед") || var2.contains("следующая") || var2.contains("предыдущая");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public boolean a(Screen var1) {
      if (var1 instanceof GenericContainerScreen var2) {
         String var3 = var2.getTitle() != null ? var2.getTitle().getString() : "";
         if (var3.isEmpty()) {
            return false;
         } else {
            String var4 = var3.toLowerCase(Locale.ROOT);
            if (this.m()) {
               return this.g(var4)
                  ? false
                  : var4.contains("аукцион")
                     || var4.contains("аукционы")
                     || var4.contains("поиск")
                     || var4.contains("auction")
                     || var4.contains("ah")
                     || var4.contains("market")
                     || var4.contains("shop")
                     || var2.getScreenHandler().slots.size() >= 54;
            } else {
               return var4.contains("аукцион") || var4.contains("аукционы") || var4.contains("поиск") || var4.contains("auction") || var4.contains("ah");
            }
         }
      } else {
         return false;
      }
   }

   private void p() {
      if (mc.player != null && mc.interactionManager != null) {
         ScreenHandler var1 = mc.player.currentScreenHandler;
         int var2;
         if (var1 != null && (var2 = this.a(var1)) >= 0) {
            mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
            this.quickBuyActive = false;
         }
      }
   }

   public boolean b(Screen var1) {
      if (var1 instanceof GenericContainerScreen var2) {
         String var3 = var2.getTitle() != null ? var2.getTitle().getString() : "";
         if (var3.isEmpty()) {
            return false;
         }

         String var4 = var3.toLowerCase(Locale.ROOT);
         if (!var4.contains("покупк")
            && !var4.contains("подтвержд")
            && !var4.contains("confirm")
            && !var4.contains("purchase")
            && !var4.contains("купить")
            && !var4.contains("уверены")) {
            if (this.quickBuyActive && var2.getScreenHandler() != null) {
               int var5 = 3;
               if (var2.getScreenHandler() instanceof GenericContainerScreenHandler var6) {
                  var5 = var6.getRows();
               }

               if (var5 <= 4 && this.a(var2.getScreenHandler()) >= 0) {
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

   private int a(ScreenHandler var1) {
      if (var1 == null) {
         return -1;
      }

      int var2;
      if ((var2 = Math.max(0, var1.slots.size() - 36)) <= 0) {
         var2 = var1.slots.size();
      }

      for (int var3 = 0; var3 < var2; var3++) {
         Slot var4 = var1.getSlot(var3);
         if (var4 != null && var4.hasStack()) {
            ItemStack var5 = var4.getStack();
            if (!var5.isEmpty()) {
               String var6 = this.w(var5).toLowerCase(Locale.ROOT);
               if (var6.contains("подтверд")
                  || var6.contains("купить")
                  || var6.contains("соглас")
                  || var6.contains("confirm")
                  || var6.contains("buy")
                  || var6.contains("принять")) {
                  return var3;
               }

               for (String var9 : this.v(var5)) {
                  String var10 = this.h(var9).toLowerCase(Locale.ROOT);
                  if (var10.contains("нажмите для покупки")
                     || var10.contains("подтвердить покупку")
                     || var10.contains("купить предмет")
                     || var10.contains("click to confirm")) {
                     return var3;
                  }
               }
            }
         }
      }

      for (int var11 = 0; var11 < var2; var11++) {
         Slot var12 = var1.getSlot(var11);
         if (var12 != null && var12.hasStack()) {
            ItemStack var13 = var12.getStack();
            if (!var13.isEmpty()) {
               String var14 = Registries.ITEM.getId(var13.getItem()).getPath().toLowerCase(Locale.ROOT);
               if (var14.contains("lime_stained_glass")
                  || var14.contains("green_stained_glass")
                  || var14.contains("lime_concrete")
                  || var14.contains("green_concrete")
                  || var14.contains("lime_wool")
                  || var14.contains("green_wool")
                  || var14.contains("lime_terracotta")
                  || var14.contains("green_terracotta")
                  || var14.contains("lime_dye")
                  || var14.contains("green_dye")
                  || var14.contains("emerald_block")
                  || var14.contains("emerald")
                  || var14.contains("lime_banner")
                  || var14.contains("green_banner")) {
                  return var11;
               }
            }
         }
      }

      return -1;
   }

   private boolean g(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         for (String var5 : COMMON_CONTAINER_NAMES) {
            if (var1.contains(var5.toLowerCase(Locale.ROOT))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private List<String> v(ItemStack var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && !var1.isEmpty()) {
         try {
            TooltipContext var3 = mc.world != null ? TooltipContext.create(mc.world) : TooltipContext.DEFAULT;
            List var4 = var1.getTooltip(var3, mc.player, TooltipType.ADVANCED);
            if (var4 != null) {
               for (Text var6 : (Iterable<Text>)(Iterable<?>)(var4)) {
                  if (var6 != null) {
                     var2.add(var6.getString());
                  }
               }
            }
         } catch (Exception var7) {
         }

         if (var2.isEmpty()) {
            var2.add(this.w(var1));
         }

         LoreComponent var8 = var1.get(DataComponentTypes.LORE);
         if (var8 != null) {
            for (Text var10 : var8.lines()) {
               if (var10 != null) {
                  String var11 = var10.getString();
                  if (!var2.contains(var11)) {
                     var2.add(var11);
                  }
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private String w(ItemStack var1) {
      try {
         return var1.getName().getString();
      } catch (Exception var3) {
         return "";
      }
   }

   private String h(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder(var1.length());

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4;
            if ((var4 = var1.charAt(var3)) == 167) {
               var3++;
            } else {
               var2.append(var4);
            }
         }

         return var2.toString();
      } else {
         return "";
      }
   }

   private long i(String var1) {
      if (var1 == null) {
         return -1L;
      }

      long var2 = 0L;
      byte var4 = 0;

      for (int var5 = 0; var5 < var1.length(); var5++) {
         char var6;
         if ((var6 = var1.charAt(var5)) >= '0' && var6 <= '9') {
            var2 = var2 * 10L + (var6 - '0');
            var4 = 1;
         }
      }

      return var4 != 0 ? var2 : -1L;
   }

   private int j(String var1) {
      try {
         return Integer.parseInt(var1);
      } catch (Exception var3) {
         return -1;
      }
   }

   private void k(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("§7[§bAH Helper§7] " + var1), false);
      }
   }

   private void q() {
      if (mc.player != null && mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }
   }

   private void r() {
      this.sellState = AuctionHelper.a.IDLE;
      this.sellStack = ItemStack.EMPTY;
      this.targetPrice = -1L;
      this.timer.a();
      this.timer2.a();
   }

   public boolean l() {
      if (this.serverMode.isSelected("HolyWorld")) {
         return true;
      } else {
         return this.serverMode.isSelected("FunTime") ? false : ServerUtility.f() || !ServerUtility.c();
      }
   }

   public boolean m() {
      if (this.serverMode.isSelected("FunTime")) {
         return true;
      } else {
         return this.serverMode.isSelected("HolyWorld") ? false : ServerUtility.c();
      }
   }

   enum a {
      IDLE,
      WAIT_PRICE_SCREEN,
      WAIT_SELL_COMMAND;
   }
}
