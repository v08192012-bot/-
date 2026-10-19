package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.EnchantmentUtility;
import brokke.cc.util.InventoryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "PvP Sell", category = Category.MISC)
public class PvPSell extends Function {
   private final NumberSetting maxAuctionItems = new NumberSetting("Предметов на аукционе", 5.0, 1.0, 45.0, 1.0);
   private final NumberSetting priceMultiplier = new NumberSetting("Выставлять по цене", 0.96, 0.5, 1.0, 0.01)
      .withFormatter(var0 -> String.format("%.2f", var0));
   private final BooleanSetting showInfo = new BooleanSetting("Отображать инфо", true);
   private static final String HUD_ID = "pvpsell_info";
   private static final float HEADER_H = 14.0F;
   private static final float ROW_H = 12.5F;
   private static final float ROW_GAP = 2.5F;
   private static final float PAD_X = 5.5F;
   private static final Color SEP_COLOR = new Color(110, 115, 130);
   private static float hudX = 7.0F;
   private static float hudY = 100.0F;
   private static boolean hudInitPos = false;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private final Animation entryAnim = new Animation();
   private final Animation widthAnim = new Animation();
   private long startTime = 0L;
   private long totalEarned = 0L;
   private String lastSoldMessage = "";
   private long lastSoldTime = 0L;
   private final List<PvPSell.a> activeListings = new ArrayList<>();
   private static final Pattern NUM_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._]\\d{3})+|\\d+)");
   private static final Pattern COUNT_X = Pattern.compile("(?i)\\bx\\s*(\\d{1,4})\\b");
   private static final Pattern COUNT_PCS = Pattern.compile("(?i)\\b(\\d{1,4})\\s*(?:шт|штук|pcs)\\b");
   private static final Pattern COUNT_LABEL = Pattern.compile("(?i)(?:Количество|Count)[:\\s]+(\\d+)");
   private static final Pattern FUNTIME_PRICE_PATTERN = Pattern.compile("\\$([0-9][0-9\\s,._]*)");
   private static final Pattern HW_UNIT_PRICE_PATTERN = Pattern.compile("(?i)Цена за 1 ед\\.?:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern HW_TOTAL_PRICE_PATTERN = Pattern.compile("(?i)(?:^|\\n|\\r|[^а-яa-z])Цена:\\s*([\\d\\s\\u00A0]+)");
   private static final Pattern PURCHASE_PATTERN = Pattern.compile("(?i)купил у вас\\s+(.+?)(?:\\s+x(\\d+))?\\s+за\\s+([^\\n\\r]+)");
   private static final Pattern DURABILITY_PATTERN = Pattern.compile("(?i)(?:прочность|durability)[:\\s]+(\\d+)\\s*[/]\\s*(\\d+)");
   private PvPSell.d teleportState = PvPSell.d.NONE;
   private long teleportTimer = 0L;
   private PvPSell.c sellFlowState = PvPSell.c.IDLE;
   private PvPSell.b periodicAhState = PvPSell.b.NONE;
   private long lastAhPeriodicTime = 0L;
   private long periodicAhTimer = 0L;
   private long searchTimer = 0L;
   private long pageTimer = 0L;
   private long screenTimer = 0L;
   private long sellTimer = 0L;
   private long parsedUnitPrice = 1L;
   private float currentForward = 0.0F;
   private float currentStrafe = 0.0F;
   private boolean currentJump = false;
   private int moveChangeTicks = 0;
   private int headTicks = 0;
   private float targetPitch = 0.0F;
   private float targetYawOffset = 10.0F;

   public static float[] k() {
      return new float[]{hudX, hudY};
   }

   public static void a(float var0, float var1) {
      hudX = var0;
      hudY = var1;
      hudInitPos = true;
   }

   public PvPSell() {
      this.addSettings(this.maxAuctionItems, this.priceMultiplier, this.showInfo);
   }

   @Override
   public void onEnable() {
      this.teleportState = PvPSell.d.NONE;
      this.teleportTimer = 0L;
      this.sellFlowState = PvPSell.c.IDLE;
      this.periodicAhState = PvPSell.b.NONE;
      this.activeListings.clear();
      this.lastAhPeriodicTime = System.currentTimeMillis();
      this.searchTimer = 0L;
      this.pageTimer = 0L;
      this.screenTimer = 0L;
      this.sellTimer = 0L;
      this.parsedUnitPrice = -1L;
      this.currentForward = 1.0F;
      this.currentStrafe = 0.0F;
      this.currentJump = false;
      this.moveChangeTicks = 0;
      this.headTicks = 0;
      this.targetPitch = 0.0F;
      this.targetYawOffset = 10.0F;
      this.startTime = System.currentTimeMillis();
      this.totalEarned = 0L;
      this.lastSoldMessage = "";
      this.lastSoldTime = 0L;
   }

   @Override
   public void onDisable() {
      this.teleportState = PvPSell.d.NONE;
      this.teleportTimer = 0L;
      this.sellFlowState = PvPSell.c.IDLE;
      this.periodicAhState = PvPSell.b.NONE;
      this.activeListings.clear();
      this.currentForward = 0.0F;
      this.currentStrafe = 0.0F;
      this.currentJump = false;
      this.moveChangeTicks = 0;
      this.headTicks = 0;
      this.targetPitch = 0.0F;
      this.targetYawOffset = 10.0F;
      if (RotationStorage.instance != null && RotationStorage.instance.k()) {
         RotationStorage.instance.j();
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (var1.getPacket() instanceof GameMessageS2CPacket var2 && mc.player != null) {
         String var13 = var2.content().getString();
         if (var13.contains("▶ Вы телепортированы") || var13.contains("Вы телепортированы")) {
            this.teleportState = PvPSell.d.WAITING_FOR_SPAWN;
            this.teleportTimer = System.currentTimeMillis();
            this.sellFlowState = PvPSell.c.IDLE;
            this.periodicAhState = PvPSell.b.NONE;
            this.currentForward = 0.0F;
            this.currentStrafe = 0.0F;
            this.currentJump = false;
            this.m();
            if (RotationStorage.instance != null && RotationStorage.instance.k()) {
               RotationStorage.instance.j();
            }

            return;
         }

         if (var2.overlay()) {
            return;
         }

         String var4 = this.a(var13);
         Matcher var5 = PURCHASE_PATTERN.matcher(var4);
         if (var5.find()) {
            long var6 = System.currentTimeMillis();
            if (var4.equals(this.lastSoldMessage) && var6 - this.lastSoldTime < 2000L) {
               return;
            }

            this.lastSoldMessage = var4;
            this.lastSoldTime = var6;
            String var8 = var5.group(1).trim();
            int var9 = var5.group(2) != null ? this.c(var5.group(2)) : 1;
            if (var9 <= 0) {
               var9 = 1;
            }

            this.a(var8, var9);
            if (var5.groupCount() >= 3 && var5.group(3) != null) {
               Matcher var10 = NUM_PATTERN.matcher(var5.group(3));
               if (var10.find()) {
                  long var11 = this.b(var10.group(1));
                  if (var11 > 0L) {
                     this.totalEarned += var11;
                  }
               }
            }
         }
      }
   }

   private void a(String var1, int var2) {
      int var3 = var2;
      Iterator var4 = this.activeListings.iterator();

      while (var4.hasNext() && var3 > 0) {
         PvPSell.a var5 = (PvPSell.a)var4.next();
         String var6 = this.a(var5.name).trim().toLowerCase(Locale.ROOT);
         String var7 = this.a(var1).trim().toLowerCase(Locale.ROOT);
         if (var6.contains(var7) || var7.contains(var6)) {
            if (var5.remainingCount <= var3) {
               var3 -= var5.remainingCount;
               var4.remove();
            } else {
               var5.remainingCount -= var3;
               var3 = 0;
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
         long var2 = System.currentTimeMillis();
         if (this.teleportState == PvPSell.d.WAITING_FOR_SPAWN) {
            if (var2 - this.teleportTimer >= 2000L) {
               if (mc.player.networkHandler != null) {
                  mc.player.networkHandler.sendChatCommand("spawn");
               }

               this.teleportState = PvPSell.d.WAITING_AFTER_SPAWN;
               this.teleportTimer = System.currentTimeMillis();
            }
         } else if (this.teleportState == PvPSell.d.WAITING_AFTER_SPAWN) {
            if (var2 - this.teleportTimer >= 3000L) {
               this.teleportState = PvPSell.d.NONE;
               this.moveChangeTicks = 0;
            }
         } else {
            this.headTicks++;
            if (this.headTicks % 15 == 0) {
               this.targetPitch = -45.0F + ThreadLocalRandom.current().nextFloat() * 90.0F;
               this.targetYawOffset = 6.0F + ThreadLocalRandom.current().nextFloat() * 10.0F;
            }

            float var4 = mc.player.getPitch();
            float var5 = Math.clamp(var4 + (this.targetPitch - var4) * 0.25F, -89.0F, 89.0F);
            float var6 = mc.player.getYaw() + this.targetYawOffset;
            RotationStorage.a(new Rotation2(var6, var5), 180.0F, 180.0F, 2, 1, false);
            if (--this.moveChangeTicks <= 0) {
               this.moveChangeTicks = 10 + ThreadLocalRandom.current().nextInt(20);
               float[] var7 = new float[]{1.0F, 1.0F, -1.0F, 0.0F};
               float[] var8 = new float[]{1.0F, -1.0F, 0.0F, 1.0F, -1.0F};
               this.currentForward = var7[ThreadLocalRandom.current().nextInt(var7.length)];
               this.currentStrafe = var8[ThreadLocalRandom.current().nextInt(var8.length)];
               if (this.currentForward == 0.0F && this.currentStrafe == 0.0F) {
                  this.currentForward = 1.0F;
               }
            }

            this.currentJump = ThreadLocalRandom.current().nextBoolean() || mc.player.isOnGround();
            if (this.periodicAhState == PvPSell.b.NONE) {
               if (var2 - this.lastAhPeriodicTime >= 120000L
                  && (this.sellFlowState == PvPSell.c.IDLE || this.sellFlowState == PvPSell.c.WAIT_5S_NEXT_ITEM)
                  && mc.player.networkHandler != null) {
                  mc.player.networkHandler.sendChatCommand("ah");
                  this.periodicAhState = PvPSell.b.WAITING_3S_TO_CLOSE;
                  this.periodicAhTimer = var2;
                  this.lastAhPeriodicTime = var2;
                  return;
               }
            } else if (this.periodicAhState == PvPSell.b.WAITING_3S_TO_CLOSE) {
               if (var2 - this.periodicAhTimer >= 3000L) {
                  this.m();
                  this.periodicAhState = PvPSell.b.NONE;
               }

               return;
            }

            if (this.sellFlowState == PvPSell.c.IDLE) {
               if (this.activeListings.size() < (int)this.maxAuctionItems.getValue() && this.l()) {
                  this.sellFlowState = PvPSell.c.WAIT_2S_BEFORE_SEARCH;
                  this.searchTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.WAIT_2S_BEFORE_SEARCH) {
               if (var2 - this.searchTimer >= 2000L && mc.player.networkHandler != null) {
                  ItemStack var14 = mc.player.getMainHandStack();
                  if (this.d(var14)) {
                     mc.player.networkHandler.sendChatCommand("ah search Тотем бессмертия");
                  } else {
                     mc.player.networkHandler.sendChatCommand("ah search");
                  }

                  this.sellFlowState = PvPSell.c.SEARCH_SENT;
                  this.searchTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.PAGE_PRE_PAUSE) {
               if (var2 - this.pageTimer >= 500L) {
                  if (this.a(mc.currentScreen)) {
                     ScreenHandler var15 = ((GenericContainerScreen)mc.currentScreen).getScreenHandler();
                     int var18 = this.a(var15);
                     if (var18 != -1 && mc.interactionManager != null && mc.player != null) {
                        mc.interactionManager.clickSlot(var15.syncId, var18, 0, SlotActionType.PICKUP, mc.player);
                     }
                  }

                  this.sellFlowState = PvPSell.c.PAGE_POST_PAUSE;
                  this.pageTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.PAGE_POST_PAUSE) {
               if (var2 - this.pageTimer >= 500L) {
                  this.sellFlowState = PvPSell.c.SEARCH_SENT;
                  this.searchTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.SEARCH_SENT) {
               if (this.a(mc.currentScreen)) {
                  ScreenHandler var16 = ((GenericContainerScreen)mc.currentScreen).getScreenHandler();
                  if (var2 - this.searchTimer >= 400L && this.b(var16)) {
                     ItemStack var19 = mc.player.getMainHandStack();
                     long var9 = this.a(var16, var19);
                     if (var9 > 0L) {
                        this.parsedUnitPrice = var9;
                        this.sellFlowState = PvPSell.c.WAIT_2S_BEFORE_CLOSE;
                        this.screenTimer = var2;
                     } else {
                        int var11 = this.a(var16);
                        if (var11 != -1) {
                           this.sellFlowState = PvPSell.c.PAGE_PRE_PAUSE;
                           this.pageTimer = var2;
                        } else {
                           this.m();
                           this.parsedUnitPrice = -1L;
                           this.sellFlowState = PvPSell.c.WAIT_5S_NEXT_ITEM;
                           this.sellTimer = var2;
                        }
                     }
                  } else if (var2 - this.searchTimer >= 5000L) {
                     this.m();
                     this.parsedUnitPrice = -1L;
                     this.sellFlowState = PvPSell.c.WAIT_5S_NEXT_ITEM;
                     this.sellTimer = var2;
                  }
               } else if (var2 - this.searchTimer >= 5000L) {
                  this.m();
                  this.parsedUnitPrice = -1L;
                  this.sellFlowState = PvPSell.c.WAIT_5S_NEXT_ITEM;
                  this.sellTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.WAIT_2S_BEFORE_CLOSE) {
               if (var2 - this.screenTimer >= 2000L) {
                  this.m();
                  if (this.parsedUnitPrice > 0L) {
                     ItemStack var17 = mc.player.getMainHandStack();
                     int var20 = var17 != null && !var17.isEmpty() ? var17.getCount() : 1;
                     String var21 = var17 != null && !var17.isEmpty() ? this.m(var17) : "";
                     long var10 = Math.max(1L, (long)Math.floor(this.parsedUnitPrice * this.priceMultiplier.getValue()));
                     long var12 = Math.max(1L, var10 * var20);
                     if (mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendChatCommand("ah sell " + var12);
                        if (!var21.isEmpty() && var20 > 0) {
                           this.activeListings.add(new PvPSell.a(var21, var20));
                        }
                     }
                  }

                  this.parsedUnitPrice = -1L;
                  this.sellFlowState = PvPSell.c.WAIT_5S_NEXT_ITEM;
                  this.sellTimer = var2;
               }
            } else if (this.sellFlowState == PvPSell.c.WAIT_5S_NEXT_ITEM && var2 - this.sellTimer >= 5000L) {
               this.sellFlowState = PvPSell.c.IDLE;
            }
         }
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (this.teleportState == PvPSell.d.NONE && this.sellFlowState != PvPSell.c.PAGE_PRE_PAUSE && this.sellFlowState != PvPSell.c.PAGE_POST_PAUSE) {
         var1.setForward(this.currentForward);
         var1.setStrafe(this.currentStrafe);
         var1.setJump(this.currentJump);
         var1.setSprint(true);
      } else {
         var1.setForward(0.0F);
         var1.setStrafe(0.0F);
         var1.setJump(false);
         var1.setSprint(false);
      }
   }

   private boolean l() {
      if (mc.player == null) {
         return false;
      }

      ItemStack var1 = mc.player.getMainHandStack();
      if (var1 != null && !var1.isEmpty()) {
         return true;
      }

      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3 != null && !var3.isEmpty()) {
            InventoryUtility.e(var2);
            return true;
         }
      }

      for (int var5 = 9; var5 < 36; var5++) {
         ItemStack var6 = mc.player.getInventory().getStack(var5);
         if (var6 != null && !var6.isEmpty() && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
            int var4 = mc.player.getInventory().getSelectedSlot();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var5, var4, SlotActionType.SWAP, mc.player);
            return true;
         }
      }

      return false;
   }

   private int a(ScreenHandler var1) {
      if (var1 == null) {
         return -1;
      }

      for (int var2 = 45; var2 < var1.slots.size(); var2++) {
         Slot var3 = var1.getSlot(var2);
         ItemStack var4 = var3.getStack();
         if (var4 != null && !var4.isEmpty()) {
            if (var4.isOf(Items.LIME_DYE)) {
               return var2;
            }

            String var5 = this.m(var4).toLowerCase(Locale.ROOT);
            if (var5.contains("следующая") || var5.contains("следующ") || var5.contains("next")) {
               return var2;
            }
         }
      }

      return -1;
   }

   private boolean a(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (var1.isOf(Items.NETHERITE_SWORD)) {
         return true;
      }

      String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
      if (var2.equals("netherite_sword")) {
         return true;
      }

      String var3 = this.m(var1).toLowerCase(Locale.ROOT);
      return var3.contains("незерит") && (var3.contains("меч") || var3.contains("sword"));
   }

   private int b(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            ItemEnchantmentsComponent var2 = var1.get(DataComponentTypes.ENCHANTMENTS);
            if (var2 == null && var1.isOf(Items.ENCHANTED_BOOK)) {
               var2 = var1.get(DataComponentTypes.STORED_ENCHANTMENTS);
            }

            if (var2 != null) {
               for (Entry var4 : var2.getEnchantmentEntries()) {
                  String var5 = ((RegistryEntry<?>)var4.getKey()).getIdAsString().toLowerCase(Locale.ROOT);
                  if (var5.endsWith(":sharpness") || var5.equals("sharpness") || var5.contains("sharpness")) {
                     return var4.getIntValue();
                  }
               }
            }
         } catch (Exception var10) {
         }

         try {
            int var11 = EnchantmentUtility.a(var1, Enchantments.SHARPNESS);
            if (var11 > 0) {
               return var11;
            }
         } catch (Exception var9) {
         }

         label171:
         for (String var14 : this.l(var1)) {
            if (var14 != null && !var14.isEmpty()) {
               String var15 = this.a(var14).trim().toLowerCase(Locale.ROOT);
               if (var15.contains("острота") || var15.contains("sharpness")) {
                  String var16 = var15.replace('Ⅹ', 'x')
                     .replace('Ⅸ', '9')
                     .replace('Ⅷ', '8')
                     .replace('Ⅶ', '7')
                     .replace('Ⅵ', '6')
                     .replace('Ⅴ', '5')
                     .replace('Ⅳ', '4')
                     .replace('Ⅲ', '3')
                     .replace('Ⅱ', '2')
                     .replace('Ⅰ', '1');
                  if (!var16.contains("острота 10") && !var16.contains("sharpness 10") && !var16.contains("острота x") && !var16.contains("sharpness x")) {
                     if (!var16.contains("острота 9") && !var16.contains("sharpness 9") && !var16.contains("острота ix") && !var16.contains("sharpness ix")) {
                        if (!var16.contains("острота 8")
                           && !var16.contains("sharpness 8")
                           && !var16.contains("острота viii")
                           && !var16.contains("sharpness viii")) {
                           if (!var16.contains("острота 7")
                              && !var16.contains("sharpness 7")
                              && !var16.contains("острота vii")
                              && !var16.contains("sharpness vii")) {
                              if (!var16.contains("острота 6")
                                 && !var16.contains("sharpness 6")
                                 && !var16.contains("острота vi")
                                 && !var16.contains("sharpness vi")) {
                                 if (!var16.contains("острота 5")
                                    && !var16.contains("sharpness 5")
                                    && !var16.contains("острота v")
                                    && !var16.contains("sharpness v")) {
                                    if (!var16.contains("острота 4")
                                       && !var16.contains("sharpness 4")
                                       && !var16.contains("острота iv")
                                       && !var16.contains("sharpness iv")) {
                                       if (!var16.contains("острота 3")
                                          && !var16.contains("sharpness 3")
                                          && !var16.contains("острота iii")
                                          && !var16.contains("sharpness iii")) {
                                          if (!var16.contains("острота 2")
                                             && !var16.contains("sharpness 2")
                                             && !var16.contains("острота ii")
                                             && !var16.contains("sharpness ii")) {
                                             if (!var16.contains("острота 1")
                                                && !var16.contains("sharpness 1")
                                                && !var16.contains("острота i")
                                                && !var16.contains("sharpness i")) {
                                                Matcher var6 = NUM_PATTERN.matcher(var16);

                                                long var7;
                                                do {
                                                   if (!var6.find()) {
                                                      continue label171;
                                                   }

                                                   var7 = this.b(var6.group(1));
                                                } while (var7 < 1L || var7 > 100L);

                                                return (int)var7;
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
         }

         return 0;
      } else {
         return 0;
      }
   }

   private int c(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            ItemEnchantmentsComponent var2 = var1.get(DataComponentTypes.ENCHANTMENTS);
            if (var2 == null && var1.isOf(Items.ENCHANTED_BOOK)) {
               var2 = var1.get(DataComponentTypes.STORED_ENCHANTMENTS);
            }

            if (var2 != null) {
               for (Entry var4 : var2.getEnchantmentEntries()) {
                  String var5 = ((RegistryEntry<?>)var4.getKey()).getIdAsString().toLowerCase(Locale.ROOT);
                  if ((var5.endsWith(":protection") || var5.equals("protection") || var5.contains("protection"))
                     && !var5.contains("projectile")
                     && !var5.contains("fire")
                     && !var5.contains("blast")
                     && !var5.contains("fall")) {
                     return var4.getIntValue();
                  }
               }
            }
         } catch (Exception var10) {
         }

         try {
            int var11 = EnchantmentUtility.a(var1, Enchantments.PROTECTION);
            if (var11 > 0) {
               return var11;
            }
         } catch (Exception var9) {
         }

         label239:
         for (String var14 : this.l(var1)) {
            if (var14 != null && !var14.isEmpty()) {
               String var15 = this.a(var14).trim().toLowerCase(Locale.ROOT);
               if ((var15.contains("защита") || var15.contains("protection"))
                  && !var15.contains("от снарядов")
                  && !var15.contains("снарядов")
                  && !var15.contains("от огня")
                  && !var15.contains("огня")
                  && !var15.contains("от взрывов")
                  && !var15.contains("взрывоустойчивость")
                  && !var15.contains("projectile")
                  && !var15.contains("fire")
                  && !var15.contains("blast")
                  && !var15.contains("падения")
                  && !var15.contains("fall")
                  && !var15.contains("feather")) {
                  String var16 = var15.replace('Ⅹ', 'x')
                     .replace('Ⅸ', '9')
                     .replace('Ⅷ', '8')
                     .replace('Ⅶ', '7')
                     .replace('Ⅵ', '6')
                     .replace('Ⅴ', '5')
                     .replace('Ⅳ', '4')
                     .replace('Ⅲ', '3')
                     .replace('Ⅱ', '2')
                     .replace('Ⅰ', '1');
                  if (!var16.contains("защита 10") && !var16.contains("protection 10") && !var16.contains("защита x") && !var16.contains("protection x")) {
                     if (!var16.contains("защита 9") && !var16.contains("protection 9") && !var16.contains("защита ix") && !var16.contains("protection ix")) {
                        if (!var16.contains("защита 8")
                           && !var16.contains("protection 8")
                           && !var16.contains("защита viii")
                           && !var16.contains("protection viii")) {
                           if (!var16.contains("защита 7")
                              && !var16.contains("protection 7")
                              && !var16.contains("защита vii")
                              && !var16.contains("protection vii")) {
                              if (!var16.contains("защита 6")
                                 && !var16.contains("protection 6")
                                 && !var16.contains("защита vi")
                                 && !var16.contains("protection vi")) {
                                 if (!var16.contains("защита 5")
                                    && !var16.contains("protection 5")
                                    && !var16.contains("защита v")
                                    && !var16.contains("protection v")) {
                                    if (!var16.contains("защита 4")
                                       && !var16.contains("protection 4")
                                       && !var16.contains("защита iv")
                                       && !var16.contains("protection iv")) {
                                       if (!var16.contains("защита 3")
                                          && !var16.contains("protection 3")
                                          && !var16.contains("защита iii")
                                          && !var16.contains("protection iii")) {
                                          if (!var16.contains("защита 2")
                                             && !var16.contains("protection 2")
                                             && !var16.contains("защита ii")
                                             && !var16.contains("protection ii")) {
                                             if (!var16.contains("защита 1")
                                                && !var16.contains("protection 1")
                                                && !var16.contains("защита i")
                                                && !var16.contains("protection i")) {
                                                Matcher var6 = NUM_PATTERN.matcher(var16);

                                                long var7;
                                                do {
                                                   if (!var6.find()) {
                                                      continue label239;
                                                   }

                                                   var7 = this.b(var6.group(1));
                                                } while (var7 < 1L || var7 > 100L);

                                                return (int)var7;
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
         }

         return 0;
      } else {
         return 0;
      }
   }

   private boolean d(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (!var1.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      String var2 = this.a(this.m(var1)).toLowerCase(Locale.ROOT);
      return var2.contains("обычный талисман") || var2.contains("эпический талисман");
   }

   private boolean e(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = this.a(this.m(var1)).toLowerCase(Locale.ROOT);
         return var2.contains("мифическая сфера") || var2.contains("сфера");
      } else {
         return false;
      }
   }

   private List<String> f(ItemStack var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && !var1.isEmpty()) {
         for (String var5 : this.l(var1)) {
            if (var5 != null && !var5.isEmpty()) {
               String var6 = this.a(var5).trim().toLowerCase(Locale.ROOT);
               if (var6.startsWith("•") || var6.startsWith("-") || var6.startsWith("*")) {
                  var6 = var6.substring(1).trim();
               }

               if (var6.contains("урон")
                  || var6.contains("броня")
                  || var6.contains("сила")
                  || var6.contains("скорость")
                  || var6.contains("здоров")
                  || var6.contains("реген")
                  || var6.contains("вампир")
                  || var6.contains("сопротив")
                  || var6.contains("удача")
                  || var6.contains("крит")
                  || var6.contains("эффект")) {
                  var2.add(var6);
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private boolean a(ItemStack var1, ItemStack var2) {
      List var3 = this.f(var1);
      List var4 = this.f(var2);
      if (var3.isEmpty()) {
         return this.e(var2);
      }

      if (var3.size() != var4.size()) {
         return false;
      }

      for (String var6 : (Iterable<String>)(Iterable<?>)(var3)) {
         boolean var7 = false;

         for (String var9 : (Iterable<String>)(Iterable<?>)(var4)) {
            if (var9.equals(var6) || var9.contains(var6) || var6.contains(var9)) {
               var7 = true;
               break;
            }
         }

         if (!var7) {
            return false;
         }
      }

      return true;
   }

   private boolean g(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      } else if (var1.isOf(Items.ELYTRA)) {
         return true;
      } else {
         String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
         if (!var2.endsWith("_helmet")
            && !var2.endsWith("_chestplate")
            && !var2.endsWith("_leggings")
            && !var2.endsWith("_boots")
            && !var2.equals("turtle_helmet")) {
            String var3 = this.m(var1).toLowerCase(Locale.ROOT);
            return var3.contains("шлем")
               || var3.contains("нагрудник")
               || var3.contains("поножи")
               || var3.contains("ботинки")
               || var3.contains("элитры")
               || var3.contains("helmet")
               || var3.contains("chestplate")
               || var3.contains("leggings")
               || var3.contains("boots")
               || var3.contains("elytra");
         } else {
            return true;
         }
      }
   }

   private double h(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         for (String var4 : this.l(var1)) {
            String var5 = this.a(var4);
            Matcher var6 = DURABILITY_PATTERN.matcher(var5);
            if (var6.find()) {
               long var7 = this.b(var6.group(1));
               long var9 = this.b(var6.group(2));
               if (var9 > 0L) {
                  return (double)var7 / var9 * 100.0;
               }
            }
         }

         int var11 = var1.getMaxDamage();
         if (var11 > 0) {
            int var12 = var1.getDamage();
            return (double)(var11 - var12) / var11 * 100.0;
         } else {
            return 100.0;
         }
      } else {
         return 100.0;
      }
   }

   private String i(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = this.a(this.m(var1)).toLowerCase(Locale.ROOT);
         if (var2.contains("eternity") || var2.contains("этернити")) {
            return "eternity";
         } else if (var2.contains("титан")) {
            return "титан";
         } else if (var2.contains("крушител")) {
            return "крушитель";
         } else if (var2.contains("дракон")) {
            return "дракон";
         } else if (var2.contains("ангел")) {
            return "ангел";
         } else if (var2.contains("хаос")) {
            return "хаос";
         } else if (var2.contains("призрак")) {
            return "призрак";
         } else if (!var2.contains("аполон") && !var2.contains("аполлон")) {
            return var2.contains("※") ? var2 : "";
         } else {
            return "аполлон";
         }
      } else {
         return "";
      }
   }

   private boolean b(ScreenHandler var1) {
      if (var1 == null) {
         return false;
      }

      int var2 = Math.min(45, var1.slots.size());

      for (int var3 = 0; var3 < var2; var3++) {
         ItemStack var4 = var1.getSlot(var3).getStack();
         if (var4 != null && !var4.isEmpty() && var4.getItem() != Items.AIR && !this.k(var4)) {
            return true;
         }
      }

      return false;
   }

   private long a(ScreenHandler var1, ItemStack var2) {
      if (var1 == null) {
         return -1L;
      }

      long var3 = Long.MAX_VALUE;
      int var5 = Math.min(45, var1.slots.size());
      boolean var6 = var2 != null && !var2.isEmpty() && this.g(var2);
      double var7 = var6 ? this.h(var2) : 100.0;
      int var9 = var6 ? this.c(var2) : 0;
      boolean var10 = var2 != null && !var2.isEmpty() && this.a(var2);
      int var11 = var10 ? this.b(var2) : 0;
      boolean var12 = var2 != null && !var2.isEmpty() && this.e(var2);
      String var13 = this.i(var2);

      for (int var14 = 0; var14 < var5; var14++) {
         Slot var15 = var1.getSlot(var14);
         ItemStack var16 = var15.getStack();
         if (var16 != null
            && !var16.isEmpty()
            && var16.getItem() != Items.AIR
            && !this.k(var16)
            && (var2 == null || var2.isEmpty() || var16.getItem() == var2.getItem())) {
            String var17 = this.i(var16);
            if ((!var13.isEmpty() ? var17.equals(var13) || var17.contains(var13) || var13.contains(var17) : var17.isEmpty())
               && (!var12 || this.e(var16) && this.a(var2, var16))) {
               if (var10) {
                  if (!this.a(var16)) {
                     continue;
                  }

                  if (var11 > 0) {
                     int var18 = this.b(var16);
                     if (var18 != var11) {
                        continue;
                     }
                  }
               }

               if (var6) {
                  if (var9 > 0) {
                     int var20 = this.c(var16);
                     if (var20 != var9) {
                        continue;
                     }
                  }

                  double var21 = this.h(var16);
                  if (var7 >= 15.0 && var7 < 30.0
                     ? var21 < 15.0 || var21 >= 30.0
                     : (var7 >= 30.0 && var7 < 65.0 ? var21 < 30.0 || var21 >= 65.0 : var7 >= 65.0 && var21 < 65.0)) {
                     continue;
                  }
               }

               long var22 = this.j(var16);
               if (var22 > 0L && var22 < var3) {
                  var3 = var22;
               }
            }
         }
      }

      return var3 == Long.MAX_VALUE ? -1L : var3;
   }

   private long j(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (this.k(var1)) {
            return -1L;
         }

         List var2 = this.l(var1);
         int var3 = Math.max(1, this.a(var1, var2));

         for (String var5 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var6 = this.a(var5).replace(' ', ' ');
            Matcher var7 = HW_UNIT_PRICE_PATTERN.matcher(var6);
            if (var7.find()) {
               long var8 = this.b(var7.group(1));
               if (var8 > 0L) {
                  return var8;
               }
            }
         }

         for (String var15 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var19 = this.a(var15).replace(' ', ' ');
            Matcher var23 = HW_TOTAL_PRICE_PATTERN.matcher(var19);
            if (var23.find()) {
               long var27 = this.b(var23.group(1));
               if (var27 > 0L) {
                  return Math.max(1L, var27 / var3);
               }
            }
         }

         for (String var16 : (Iterable<String>)(Iterable<?>)(var2)) {
            Matcher var20 = FUNTIME_PRICE_PATTERN.matcher(this.a(var16));
            if (var20.find()) {
               long var24 = this.b(var20.group(1));
               if (var24 > 0L) {
                  return Math.max(1L, var24 / var3);
               }
            }
         }

         ComponentMap var14 = var1.getComponents();
         if (var14 != null) {
            String var17 = var14.toString();
            int var21 = var17.indexOf("literal{ $");
            if (var21 != -1) {
               int var25 = var17.indexOf("}[style={color=green}]", var21);
               if (var25 != -1) {
                  String var28 = var17.substring(var21 + 10, var25).replaceAll("[^0-9]", "");

                  try {
                     long var9 = Long.parseLong(var28);
                     if (var9 > 0L) {
                        return Math.max(1L, var9 / var3);
                     }
                  } catch (Exception var11) {
                  }
               }
            }
         }

         for (String var22 : (Iterable<String>)(Iterable<?>)(var2)) {
            String var26 = this.a(var22).toLowerCase(Locale.ROOT);
            if (var26.contains("цена") || var26.contains("price") || var26.contains("стоим") || var26.contains("$")) {
               Matcher var29 = NUM_PATTERN.matcher(var26);
               if (var29.find()) {
                  long var30 = this.b(var29.group(1));
                  if (var30 > 0L) {
                     if (!var26.contains("за шт") && !var26.contains("за 1") && !var26.contains("/шт")) {
                        return Math.max(1L, var30 / var3);
                     }

                     return var30;
                  }
               }
            }
         }

         return -1L;
      } else {
         return -1L;
      }
   }

   private boolean k(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.getItem() != Items.BARRIER && var1.getItem() != Items.AIR) {
            String var2 = this.m(var1).toLowerCase(Locale.ROOT);
            return var2.contains("назад") || var2.contains("вперед") || var2.contains("следующая") || var2.contains("предыдущая");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private boolean a(Screen var1) {
      if (var1 instanceof GenericContainerScreen var2) {
         String var3 = var2.getTitle() != null ? var2.getTitle().getString() : "";
         if (var3.isEmpty()) {
            return false;
         }

         String var4 = var3.toLowerCase(Locale.ROOT);
         return var4.contains("аукцион") || var4.contains("аукционы") || var4.contains("поиск") || var4.contains("auction") || var4.contains("ah");
      } else {
         return false;
      }
   }

   private List<String> l(ItemStack var1) {
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
            var2.add(this.m(var1));
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

   private int a(ItemStack var1, List<String> var2) {
      if (var1 != null && !var1.isEmpty()) {
         for (String var4 : var2) {
            String var5 = this.a(var4);
            Matcher var6 = COUNT_X.matcher(var5);
            if (var6.find()) {
               int var7 = this.c(var6.group(1));
               if (var7 > 0) {
                  return var7;
               }
            }

            Matcher var15 = COUNT_PCS.matcher(var5);
            if (var15.find()) {
               int var8 = this.c(var15.group(1));
               if (var8 > 0) {
                  return var8;
               }
            }

            Matcher var16 = COUNT_LABEL.matcher(var5);
            if (var16.find()) {
               int var9 = this.c(var16.group(1));
               if (var9 > 0) {
                  return var9;
               }
            }
         }

         String var10 = this.m(var1);
         Matcher var11 = COUNT_X.matcher(var10);
         if (var11.find()) {
            int var12 = this.c(var11.group(1));
            if (var12 > 0) {
               return var12;
            }
         }

         Matcher var13 = COUNT_PCS.matcher(var10);
         if (var13.find()) {
            int var14 = this.c(var13.group(1));
            if (var14 > 0) {
               return var14;
            }
         }

         return Math.max(1, var1.getCount());
      } else {
         return 1;
      }
   }

   private String m(ItemStack var1) {
      try {
         return var1.getName().getString();
      } catch (Exception var3) {
         return "";
      }
   }

   private String a(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder(var1.length());

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = this.a(var1.charAt(var3));
            if (var4 == 167) {
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

   private char a(char var1) {
      return var1;
   }

   private long b(String var1) {
      if (var1 == null) {
         return -1L;
      }

      long var2 = 0L;
      boolean var4 = false;

      for (int var5 = 0; var5 < var1.length(); var5++) {
         char var6 = var1.charAt(var5);
         if (var6 >= '0' && var6 <= '9') {
            var2 = var2 * 10L + (var6 - '0');
            var4 = true;
         }
      }

      return var4 ? var2 : -1L;
   }

   private int c(String var1) {
      try {
         return Integer.parseInt(var1);
      } catch (Exception var3) {
         return -1;
      }
   }

   private void m() {
      if (mc.player != null && mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }
   }

   private void a(MinecraftClient var1, float var2, float var3) {
      if (!(var1.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
      } else {
         double var4 = var1.getWindow().getScaleFactor();
         float var6 = (float)(var1.mouse.getX() / var4) * RenderUtil.e();
         float var7 = (float)(var1.mouse.getY() / var4) * RenderUtil.e();
         boolean var8 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1;
         if (var8
            && !this.wasPressed
            && !HudConfig.b(var6, var7)
            && var6 >= hudX - 2.0F
            && var6 <= hudX + var2 + 2.0F
            && var7 >= hudY - 2.0F
            && var7 <= hudY + var3 + 2.0F) {
            this.dragging = true;
            this.dragDX = var6 - hudX;
            this.dragDY = var7 - hudY;
         }

         boolean var9 = this.dragging;
         if (!var8) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = var8;
         if (this.dragging) {
            float[] var10 = HudConfig.b("pvpsell_info", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            hudX = var10[0];
            hudY = var10[1];
         } else if (var9) {
            ConfigManager.onSettingChanged();
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      if (mc.player != null && !mc.options.hudHidden) {
         boolean var2 = this.isActive() && this.showInfo.isToggled() || mc.currentScreen instanceof ChatScreen && this.showInfo.isToggled() && this.isActive();
         this.entryAnim.setEasing(Easing.SPRING_SOFT);
         this.entryAnim.animate(var2 ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var3 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F);
         if (!(var3 <= 0.01F)) {
            float var4 = (1.0F - var3) * 4.0F;
            Color var5 = Theme.getInstance().getAccent();
            float var6 = Msdf.a(MsdfManager.DICONS, "D", 7.0F);
            float var7 = Msdf.a(MsdfManager.DICONS, 7.0F);
            float var8 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
            float var9 = Msdf.a(MsdfManager.MONTSERRAT, 6.5F);
            float var10 = Msdf.a(MsdfManager.BOLD, "PvP Sell Info", 7.0F);
            float var11 = Msdf.a(MsdfManager.BOLD, 7.0F);
            float var12 = 5.5F + var6 + 3.5F + var8 + 3.5F + var10 + 5.5F;
            long var13 = this.startTime > 0L ? System.currentTimeMillis() - this.startTime : 0L;
            long var15 = var13 / 1000L;
            long var17 = var15 / 3600L;
            long var19 = var15 % 3600L / 60L;
            long var21 = var15 % 60L;
            String var23 = var17 > 0L
               ? String.format(Locale.ROOT, "%02d:%02d:%02d", var17, var19, var21)
               : String.format(Locale.ROOT, "%02d:%02d", var19, var21);
            String var24 = String.format(Locale.ROOT, "%,d $", this.totalEarned).replace(',', ' ');
            String var25 = "Заработано:";
            String var26 = "Время работы:";
            float var27 = Msdf.a(MsdfManager.BOLD, var25, 6.25F);
            float var28 = Msdf.a(MsdfManager.BOLD, var24, 6.25F);
            float var29 = 5.5F + var27 + 4.0F + var28 + 5.5F;
            float var30 = Msdf.a(MsdfManager.BOLD, var26, 6.25F);
            float var31 = Msdf.a(MsdfManager.BOLD, var23, 6.25F);
            float var32 = 5.5F + var30 + 4.0F + var31 + 5.5F;
            float var33 = Math.max(var12, Math.max(var29, var32));
            this.widthAnim.setEasing(Easing.SPRING_SOFT);
            this.widthAnim.animate(var33, 0.0F, 6.0F);
            float var34 = Math.max(var12, this.widthAnim.getValue());
            float var35 = 44.0F;
            float var36 = HudConfig.a("pvpsell_info");
            float var37 = var34 * var36;
            float var38 = var35 * var36;
            if (!hudInitPos) {
               hudX = 7.0F;
               hudY = 100.0F;
               hudInitPos = true;
            }

            this.a(mc, var37, var38);
            HudConfig.c("pvpsell_info", hudX, hudY, var37, var38);
            float var39 = hudY + var4;
            Matrix4f var40 = RenderUtil.d();
            var40.translate(hudX, var39, 0.0F);
            var40.scale(var36, var36, 1.0F);
            var40.translate(-hudX, -var39, 0.0F);
            HudConfig.a(var40, "pvpsell_info", hudX, var39, var34, 14.0F, 5.0F, new Color(0, 0, 0, (int)(180.0F * var3)));
            float var41 = var39 + 14.0F + 2.5F;
            HudConfig.a(var40, hudX, var41, var34, 12.5F, 4.5F, new Color(0, 0, 0, (int)(180.0F * var3)));
            float var42 = var41 + 12.5F + 2.5F;
            HudConfig.a(var40, hudX, var42, var34, 12.5F, 4.5F, new Color(0, 0, 0, (int)(180.0F * var3)));
            float var43 = var39 + (14.0F - var7) * 0.5F;
            Msdf.b();
            Msdf.a(var40, MsdfManager.DICONS, "D", hudX + 5.5F, var43, 7.0F, a(var5, 255.0F * var3));
            Msdf.a(var40, MsdfManager.MONTSERRAT, "|", hudX + 5.5F + var6 + 3.5F, var39 + (14.0F - var9) * 0.5F, 6.5F, a(SEP_COLOR, 160.0F * var3));
            Msdf.a(
               var40,
               MsdfManager.BOLD,
               "PvP Sell Info",
               hudX + 5.5F + var6 + 3.5F + var8 + 3.5F,
               var39 + (14.0F - var11) * 0.5F,
               7.0F,
               a(Color.WHITE, 255.0F * var3)
            );
            float var44 = var41 + (12.5F - Msdf.a(MsdfManager.BOLD, 6.25F)) * 0.5F;
            Msdf.a(var40, MsdfManager.BOLD, var25, hudX + 5.5F, var44, 6.25F, a(new Color(150, 150, 158), 220.0F * var3));
            Msdf.a(var40, MsdfManager.BOLD, var24, hudX + 5.5F + var27 + 4.0F, var44, 6.25F, a(var5, 255.0F * var3));
            float var45 = var42 + (12.5F - Msdf.a(MsdfManager.BOLD, 6.25F)) * 0.5F;
            Msdf.a(var40, MsdfManager.BOLD, var26, hudX + 5.5F, var45, 6.25F, a(new Color(150, 150, 158), 220.0F * var3));
            Msdf.a(var40, MsdfManager.BOLD, var23, hudX + 5.5F + var30 + 4.0F, var45, 6.25F, a(Color.WHITE, 255.0F * var3));
            Msdf.c();
            HudConfig.c("pvpsell_info");
         }
      }
   }

   private static int a(Color var0, float var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0.getRGB() & 16777215;
   }

   static class a {
      final String name;
      int remainingCount;

      public a(String var1, int var2) {
         this.name = var1;
         this.remainingCount = var2;
      }
   }

   enum b {
      NONE,
      WAITING_3S_TO_CLOSE;
   }

   enum c {
      IDLE,
      WAIT_2S_BEFORE_SEARCH,
      SEARCH_SENT,
      PAGE_PRE_PAUSE,
      PAGE_POST_PAUSE,
      WAIT_2S_BEFORE_CLOSE,
      WAIT_5S_NEXT_ITEM;
   }

   enum d {
      NONE,
      WAITING_FOR_SPAWN,
      WAITING_AFTER_SPAWN;
   }
}
