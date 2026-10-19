package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.player.AutoEat;
import brokke.cc.core.Timer2;
import brokke.cc.mixins.interfaces.IClientPlayerInteractionManager;
import brokke.cc.nav.GoalNear;
import brokke.cc.nav.GotoTask;
import brokke.cc.nav.NavigationManager;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.TextSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.GCDUtil2;
import brokke.cc.util.HotbarUtil;
import brokke.cc.util.InventoryUtility;
import brokke.cc.util.PotionUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;

@NewFunction(name = "AutoZamok", description = "Автоматический сбор шалкеров на Замке", category = Category.MISC)
public class AutoZamok extends Function {
   private static final Vec3d[] SHULKER_SPAWNS = new Vec3d[]{
      new Vec3d(-5.5, 64.0, 6.4),
      new Vec3d(-5.5, 64.0, -5.6),
      new Vec3d(0.4, 64.0, -12.5),
      new Vec3d(6.5, 64.0, -5.3),
      new Vec3d(13.4, 64.0, 0.5),
      new Vec3d(6.3, 64.0, 6.5),
      new Vec3d(6.5, 83.0, 6.4),
      new Vec3d(15.4, 79.0, 15.4),
      new Vec3d(15.5, 79.0, -14.5),
      new Vec3d(-5.5, 83.0, -5.5),
      new Vec3d(-14.5, 79.0, 15.6),
      new Vec3d(-5.5, 83.0, 6.5)
   };
   private static final BlockPos ZAMOK_CENTER_POS = new BlockPos(14, 64, 0);
   public final TextSetting homeSklad = new TextSetting("Дом склада", "sklad");
   public final TextSetting homeZamok = new TextSetting("Дом замка", "zamok");
   public final FloatSetting tpWarmupDelay = new FloatSetting("Задержка телепорта", 4.5F, 3.0F, 10.0F, 0.5F);
   public final FloatSetting tpCooldownDelay = new FloatSetting("Кулдаун телепорта", 4.0F, 1.0F, 15.0F, 0.5F);
   public final FloatSetting chestDelay = new FloatSetting("Задержка сундука", 150.0F, 50.0F, 500.0F, 25.0F);
   public final FloatSetting potionDelay = new FloatSetting("Задержка зелий", 500.0F, 200.0F, 1500.0F, 50.0F);
   public final FloatSetting postTpDelay = new FloatSetting("Пауза после ТП", 1.0F, 0.2F, 3.0F, 0.1F);
   private AutoZamok.a state = AutoZamok.a.IDLE;
   private final Timer2 actionTimer = new Timer2();
   private final Timer2 drinkTimer = new Timer2();
   private final Timer2 slotActionTimer = new Timer2();
   private final Timer2 patrolPointTimer = new Timer2();
   private final Timer2 fleeTimer = new Timer2();
   private final Timer2 fleeRepathTimer = new Timer2();
   private final List<BlockPos> chestQueue = new ArrayList<>();
   private final List<Integer> visitedPatrolSpawns = new ArrayList<>();
   private int currentChestIndex = 0;
   private int currentPatrolTargetIndex = -1;
   private BlockPos currentTargetChest = null;
   private BlockPos currentShulkerPos = null;
   private float lastPlayerHealth = 20.0F;
   private PlayerEntity fleeingFromPlayer = null;
   private long nextAllowedTpTime = 0L;
   private boolean tpFailedCooldown = false;
   private boolean tpCancelled = false;

   public AutoZamok() {
      this.addSettings(this.homeSklad, this.homeZamok, this.tpWarmupDelay, this.tpCooldownDelay, this.chestDelay, this.potionDelay, this.postTpDelay);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.chestQueue.clear();
      this.currentChestIndex = 0;
      this.currentTargetChest = null;
      this.currentShulkerPos = null;
      this.visitedPatrolSpawns.clear();
      this.currentPatrolTargetIndex = -1;
      this.tpFailedCooldown = false;
      this.tpCancelled = false;
      this.lastPlayerHealth = mc.player != null ? mc.player.getHealth() : 20.0F;
      this.fleeingFromPlayer = null;
      this.fleeTimer.a();
      this.fleeRepathTimer.a();
      this.actionTimer.a();
      this.slotActionTimer.a();
      this.patrolPointTimer.a();
      this.state = AutoZamok.a.TP_TO_SKLAD;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.state = AutoZamok.a.IDLE;
      this.visitedPatrolSpawns.clear();
      this.currentPatrolTargetIndex = -1;
      this.fleeingFromPlayer = null;
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
         mc.options.attackKey.setPressed(false);
      }

      this.k();
      this.ab();
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (this.isActive()) {
         if (var1.getPacket() instanceof GameMessageS2CPacket var2) {
            String var4 = var2.content().getString();
            this.a(var4);
         } else if (var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.nextAllowedTpTime = System.currentTimeMillis() + (long)(this.tpCooldownDelay.getFloatValue() * 1000.0F);
         }
      }
   }

   private void a(String var1) {
      if (var1 != null) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         if (var2.contains("времени до следующей телепортации")) {
            Matcher var3 = Pattern.compile("(\\d+)").matcher(var2);
            long var4 = 2L;
            if (var3.find()) {
               try {
                  var4 = Long.parseLong(var3.group(1));
               } catch (Exception var7) {
               }
            }

            this.nextAllowedTpTime = System.currentTimeMillis() + (var4 + 1L) * 1000L + 500L;
            this.tpFailedCooldown = true;
            this.d("§cКулдаун телепортации: " + var4 + " сек. Ожидание...");
         } else if (var2.contains("телепортация отменена")) {
            this.tpCancelled = true;
            this.nextAllowedTpTime = System.currentTimeMillis() + 1500L;
            this.d("§cТелепортация была отменена сервером. Повтор...");
         } else if (var2.contains("телепортирование к") || var2.contains("телепортирование начинается")) {
            this.nextAllowedTpTime = System.currentTimeMillis() + (long)(this.tpCooldownDelay.getFloatValue() * 1000.0F);
         }
      }
   }

   private void k() {
      NavigationManager.getInstance().c();
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.useKey.setPressed(false);
         mc.options.attackKey.setPressed(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   private void a(String var1, AutoZamok.a var2) {
      long var3 = System.currentTimeMillis();
      if (var3 >= this.nextAllowedTpTime) {
         this.ad();
         this.ab();
         this.ac();
         this.k();
         this.tpFailedCooldown = false;
         this.tpCancelled = false;
         this.c("home " + var1);
         this.state = var2;
         this.actionTimer.a();
         this.nextAllowedTpTime = var3 + (long)(this.tpCooldownDelay.getFloatValue() * 1000.0F);
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && this.isActive()) {
         if (!mc.player.isDead() && !(mc.player.getHealth() <= 0.0F) && !(mc.currentScreen instanceof DeathScreen)) {
            this.N();
            if (!AutoEat.l() && (!mc.player.isUsingItem() || !this.a(mc.player.getActiveItem()))) {
               switch (this.state) {
                  case TP_TO_SKLAD:
                     this.l();
                     break;
                  case WAIT_TP_SKLAD:
                     this.m();
                     break;
                  case SEARCH_SUPPLIES:
                     this.n();
                     break;
                  case OPEN_NEXT_SUPPLY_CHEST:
                     this.o();
                     break;
                  case WAIT_CHEST_SUPPLIES:
                     this.p();
                     break;
                  case DRINK_INVIS:
                     this.r();
                     break;
                  case PREPARE_DRINK_INVIS:
                     this.s();
                     break;
                  case DRINKING_INVIS:
                     this.t();
                     break;
                  case WAIT_SPLASH_INVIS:
                     this.u();
                     break;
                  case PAUSE_AFTER_INVIS:
                     this.v();
                     break;
                  case DRINK_SPEED:
                     this.w();
                     break;
                  case PREPARE_DRINK_SPEED:
                     this.x();
                     break;
                  case DRINKING_SPEED:
                     this.y();
                     break;
                  case WAIT_SPLASH_SPEED:
                     this.z();
                     break;
                  case PAUSE_AFTER_SPEED:
                     this.A();
                     break;
                  case TP_TO_ZAMOK:
                     this.B();
                     break;
                  case WAIT_TP_ZAMOK:
                     this.C();
                     break;
                  case PATROL_SHULKER_SPAWNS:
                     this.H();
                     break;
                  case APPROACH_SHULKER:
                     this.I();
                     break;
                  case MINE_SHULKER:
                     this.J();
                     break;
                  case COLLECT_LOOT:
                     this.K();
                     break;
                  case RUN_AWAY:
                     this.M();
                     break;
                  case FLEE_PLAYER:
                     this.P();
                     break;
                  case STOP_BEFORE_SKLAD_TP:
                     this.Q();
                     break;
                  case WAIT_TP_SKLAD_RETURN:
                     this.R();
                     break;
                  case DEPOSIT_LOOT:
                     this.U();
                     break;
                  case OPEN_NEXT_DEPOSIT_CHEST:
                     this.V();
                     break;
                  case WAIT_CHEST_DEPOSIT:
                     this.W();
                     break;
                  case RESTOCK_SEARCH_CHESTS:
                     this.Y();
                     break;
                  case RESTOCK_OPEN_NEXT_CHEST:
                     this.Z();
                     break;
                  case RESTOCK_WAIT_CHEST:
                     this.aa();
               }
            }
         } else {
            mc.player.requestRespawn();
            if (mc.currentScreen instanceof DeathScreen) {
               mc.setScreen(null);
            }

            this.k();
            this.ab();
            this.state = AutoZamok.a.TP_TO_SKLAD;
            this.nextAllowedTpTime = System.currentTimeMillis() + 2500L;
            this.actionTimer.a();
            this.lastPlayerHealth = 20.0F;
            this.fleeingFromPlayer = null;
            this.d("§cПерсонаж погиб! Автовозрождение и запуск сбора с нуля.");
         }
      }
   }

   private void l() {
      this.k();
      if (System.currentTimeMillis() >= this.nextAllowedTpTime) {
         this.a(this.homeSklad.getValue(), AutoZamok.a.WAIT_TP_SKLAD);
      }
   }

   private void m() {
      this.k();
      this.ab();
      if (!this.tpFailedCooldown && !this.tpCancelled) {
         long var1 = (long)(this.tpWarmupDelay.getFloatValue() * 1000.0F);
         long var3 = (long)(this.postTpDelay.getFloatValue() * 1000.0F);
         if (this.actionTimer.a(var1 + var3)) {
            this.state = AutoZamok.a.SEARCH_SUPPLIES;
            this.actionTimer.a();
         }
      } else if (System.currentTimeMillis() >= this.nextAllowedTpTime && this.actionTimer.a(1000L)) {
         this.a(this.homeSklad.getValue(), AutoZamok.a.WAIT_TP_SKLAD);
      }
   }

   private void n() {
      boolean var1 = this.ah();
      boolean var2 = this.ai() || this.ak();
      if (var1 && var2) {
         this.state = AutoZamok.a.DRINK_INVIS;
         this.actionTimer.a();
      } else {
         this.ao();
         if (this.chestQueue.isEmpty()) {
            this.q();
         } else {
            this.currentChestIndex = 0;
            this.state = AutoZamok.a.OPEN_NEXT_SUPPLY_CHEST;
            this.actionTimer.a();
         }
      }
   }

   private void o() {
      if (this.actionTimer.a((long)this.chestDelay.getFloatValue()) && mc.currentScreen == null) {
         boolean var1 = this.ah();
         boolean var2 = this.ai() || this.ak();
         if (var1 && var2) {
            this.state = AutoZamok.a.DRINK_INVIS;
            this.actionTimer.a();
         } else if (this.currentChestIndex >= this.chestQueue.size()) {
            this.q();
         } else {
            this.currentTargetChest = this.chestQueue.get(this.currentChestIndex);
            this.c(this.currentTargetChest);
            this.state = AutoZamok.a.WAIT_CHEST_SUPPLIES;
            this.actionTimer.a();
            this.slotActionTimer.a();
         }
      }
   }

   private void p() {
      if (this.actionTimer.a(4000L)) {
         this.ar();
         this.currentChestIndex++;
         this.state = AutoZamok.a.OPEN_NEXT_SUPPLY_CHEST;
         this.actionTimer.a();
      } else if (mc.currentScreen instanceof GenericContainerScreen var1) {
         if (!this.slotActionTimer.a((long)this.chestDelay.getFloatValue())) {
            return;
         }

         GenericContainerScreenHandler var12 = var1.getScreenHandler();
         int var3 = var12.getRows() * 9;
         int var4 = -1;
         int var5 = this.ag();
         if (var5 != -1 && mc.player != null) {
            var4 = this.d(mc.player.getInventory().getStack(var5));
         }

         int var6 = -1;
         int var7 = var4;

         for (int var8 = 0; var8 < var3; var8++) {
            ItemStack var9 = var12.getSlot(var8).getStack();
            if (var9 != null && !var9.isEmpty()) {
               int var10 = this.d(var9);
               if (var10 > var7) {
                  var7 = var10;
                  var6 = var8;
               }
            }
         }

         if (var6 != -1) {
            this.a(var12, var6);
            this.slotActionTimer.a();
            return;
         }

         boolean var13 = !this.ai() && !this.ak();
         boolean var14 = !this.aj() && !this.al();

         for (int var15 = 0; var15 < var3; var15++) {
            ItemStack var11 = var12.getSlot(var15).getStack();
            if (var11 != null && !var11.isEmpty()) {
               if (var13 && this.f(var11)) {
                  this.a(var12, var15);
                  this.slotActionTimer.a();
                  return;
               }

               if (var14 && this.g(var11)) {
                  this.a(var12, var15);
                  this.slotActionTimer.a();
                  return;
               }
            }
         }

         this.ar();
         this.af();
         this.currentChestIndex++;
         this.state = AutoZamok.a.OPEN_NEXT_SUPPLY_CHEST;
         this.actionTimer.a();
      }
   }

   private void q() {
      this.d("§cНет кирки или невидимости! Завершение работы.§r");
      this.setEnabled(false);
   }

   private void r() {
      if (this.ai()) {
         this.state = AutoZamok.a.DRINK_SPEED;
         this.actionTimer.a();
      } else {
         int var1 = this.b(true);
         if (var1 == -1) {
            this.q();
         } else {
            this.b(var1);
            this.state = AutoZamok.a.PREPARE_DRINK_INVIS;
            this.actionTimer.a();
         }
      }
   }

   private void s() {
      ItemStack var1 = mc.player.getMainHandStack();
      if (this.f(var1)) {
         if (var1.getItem() instanceof SplashPotionItem) {
            this.am();
            this.state = AutoZamok.a.WAIT_SPLASH_INVIS;
            this.actionTimer.a();
         } else {
            this.state = AutoZamok.a.DRINKING_INVIS;
            this.drinkTimer.a();
            this.actionTimer.a();
         }
      } else if (this.actionTimer.a(1000L)) {
         int var2 = this.b(true);
         if (var2 != -1) {
            this.b(var2);
            this.actionTimer.a();
         } else {
            this.q();
         }
      }
   }

   private void t() {
      if (this.ai()) {
         if (mc.options != null) {
            mc.options.useKey.setPressed(false);
         }

         this.ae();
         this.ab();
         this.state = AutoZamok.a.PAUSE_AFTER_INVIS;
         this.actionTimer.a();
      } else {
         ItemStack var1 = mc.player.getMainHandStack();
         if (this.c(var1) || !this.f(var1) && this.drinkTimer.a(600L)) {
            if (mc.options != null) {
               mc.options.useKey.setPressed(false);
            }

            this.ae();
            this.ab();
            this.state = AutoZamok.a.PAUSE_AFTER_INVIS;
            this.actionTimer.a();
         } else {
            if (mc.options != null) {
               mc.options.useKey.setPressed(true);
            }

            if (!mc.player.isUsingItem() && mc.interactionManager != null) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }

            if (this.drinkTimer.a(2600L)) {
               if (mc.options != null) {
                  mc.options.useKey.setPressed(false);
               }

               this.ae();
               this.ab();
               this.state = AutoZamok.a.PAUSE_AFTER_INVIS;
               this.actionTimer.a();
            }
         }
      }
   }

   private void u() {
      if (this.ai() || this.actionTimer.a(1000L)) {
         this.ae();
         this.ab();
         this.state = AutoZamok.a.PAUSE_AFTER_INVIS;
         this.actionTimer.a();
      }
   }

   private void v() {
      this.ae();
      this.ab();
      if (this.actionTimer.a((long)this.potionDelay.getFloatValue())) {
         if (!this.ai()) {
            if (this.ak()) {
               this.state = AutoZamok.a.DRINK_INVIS;
               this.actionTimer.a();
            } else {
               this.q();
            }
         } else {
            this.state = AutoZamok.a.DRINK_SPEED;
            this.actionTimer.a();
         }
      }
   }

   private void w() {
      if (!this.aj() && this.al()) {
         int var1 = this.b(false);
         if (var1 == -1) {
            this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
            this.actionTimer.a();
         } else {
            this.b(var1);
            this.state = AutoZamok.a.PREPARE_DRINK_SPEED;
            this.actionTimer.a();
         }
      } else {
         this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
         this.actionTimer.a();
      }
   }

   private void x() {
      ItemStack var1 = mc.player.getMainHandStack();
      if (this.g(var1)) {
         if (var1.getItem() instanceof SplashPotionItem) {
            this.am();
            this.state = AutoZamok.a.WAIT_SPLASH_SPEED;
            this.actionTimer.a();
         } else {
            this.state = AutoZamok.a.DRINKING_SPEED;
            this.drinkTimer.a();
            this.actionTimer.a();
         }
      } else if (this.actionTimer.a(1000L)) {
         int var2 = this.b(false);
         if (var2 != -1) {
            this.b(var2);
            this.actionTimer.a();
         } else {
            this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
            this.actionTimer.a();
         }
      }
   }

   private void y() {
      if (this.aj()) {
         if (mc.options != null) {
            mc.options.useKey.setPressed(false);
         }

         this.ae();
         this.ab();
         this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
         this.actionTimer.a();
      } else {
         ItemStack var1 = mc.player.getMainHandStack();
         if (this.c(var1) || !this.g(var1) && this.drinkTimer.a(600L)) {
            if (mc.options != null) {
               mc.options.useKey.setPressed(false);
            }

            this.ae();
            this.ab();
            this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
            this.actionTimer.a();
         } else {
            if (mc.options != null) {
               mc.options.useKey.setPressed(true);
            }

            if (!mc.player.isUsingItem() && mc.interactionManager != null) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }

            if (this.drinkTimer.a(2600L)) {
               if (mc.options != null) {
                  mc.options.useKey.setPressed(false);
               }

               this.ae();
               this.ab();
               this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
               this.actionTimer.a();
            }
         }
      }
   }

   private void z() {
      if (this.aj() || this.actionTimer.a(1000L)) {
         this.ae();
         this.ab();
         this.state = AutoZamok.a.PAUSE_AFTER_SPEED;
         this.actionTimer.a();
      }
   }

   private void A() {
      this.ae();
      this.ab();
      if (this.actionTimer.a((long)this.potionDelay.getFloatValue())) {
         this.state = AutoZamok.a.TP_TO_ZAMOK;
         this.actionTimer.a();
      }
   }

   private void B() {
      this.k();
      this.ab();
      this.ac();
      if (!this.ai()) {
         this.d("§cОшибка: эффект невидимости отсутствует! Отмена телепортации на замок.");
         this.state = AutoZamok.a.SEARCH_SUPPLIES;
         this.actionTimer.a();
      } else if (System.currentTimeMillis() >= this.nextAllowedTpTime) {
         this.a(this.homeZamok.getValue(), AutoZamok.a.WAIT_TP_ZAMOK);
      }
   }

   private void C() {
      this.k();
      this.ab();
      if (!this.tpFailedCooldown && !this.tpCancelled) {
         long var1 = (long)(this.tpWarmupDelay.getFloatValue() * 1000.0F);
         long var3 = (long)(this.postTpDelay.getFloatValue() * 1000.0F);
         if (this.actionTimer.a(var1 + var3)) {
            if (mc.player.squaredDistanceTo(14.0, mc.player.getY(), 0.0) > 25000.0) {
               if (System.currentTimeMillis() >= this.nextAllowedTpTime && this.actionTimer.a(var1 + 2000L)) {
                  this.a(this.homeZamok.getValue(), AutoZamok.a.WAIT_TP_ZAMOK);
               }
            } else if (this.D()) {
               this.E();
            } else {
               BlockPos var5 = this.an();
               if (var5 != null) {
                  this.currentShulkerPos = var5;
                  NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
                  this.state = AutoZamok.a.APPROACH_SHULKER;
               } else {
                  this.F();
               }

               this.actionTimer.a();
            }
         }
      } else if (System.currentTimeMillis() >= this.nextAllowedTpTime && this.actionTimer.a(1000L)) {
         this.a(this.homeZamok.getValue(), AutoZamok.a.WAIT_TP_ZAMOK);
      }
   }

   private boolean D() {
      if (mc.player == null) {
         return true;
      }

      StatusEffectInstance var1 = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
      if (var1 == null) {
         return true;
      }

      int var2 = (int)(this.tpWarmupDelay.getFloatValue() * 20.0F) + 100;
      return var1.getDuration() <= var2;
   }

   private void E() {
      this.d("§cНевидимость заканчивается! Возвращаемся домой...");
      this.currentShulkerPos = null;
      this.k();
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
         mc.options.useKey.setPressed(false);
      }

      if (mc.interactionManager != null) {
         mc.interactionManager.cancelBlockBreaking();
      }

      this.ab();
      this.ac();
      this.state = AutoZamok.a.STOP_BEFORE_SKLAD_TP;
      this.actionTimer.a();
   }

   private void F() {
      this.visitedPatrolSpawns.clear();
      this.currentPatrolTargetIndex = -1;
      this.state = AutoZamok.a.PATROL_SHULKER_SPAWNS;
      this.patrolPointTimer.a();
      this.d("§eШалкер не обнаружен. Начинаем обход координат спавна...");
      this.G();
   }

   private void G() {
      if (mc.player != null) {
         this.ab();
         this.ac();
         if (this.visitedPatrolSpawns.size() >= SHULKER_SPAWNS.length) {
            this.visitedPatrolSpawns.clear();
         }

         int var1 = -1;
         double var2 = Double.MAX_VALUE;

         for (int var4 = 0; var4 < SHULKER_SPAWNS.length; var4++) {
            if (!this.visitedPatrolSpawns.contains(var4)) {
               double var5 = mc.player.squaredDistanceTo(SHULKER_SPAWNS[var4]);
               if (var5 < var2) {
                  var2 = var5;
                  var1 = var4;
               }
            }
         }

         if (var1 == -1) {
            this.visitedPatrolSpawns.clear();

            for (int var7 = 0; var7 < SHULKER_SPAWNS.length; var7++) {
               double var9 = mc.player.squaredDistanceTo(SHULKER_SPAWNS[var7]);
               if (var9 < var2) {
                  var2 = var9;
                  var1 = var7;
               }
            }
         }

         this.currentPatrolTargetIndex = var1;
         this.patrolPointTimer.a();
         if (var1 != -1) {
            Vec3d var8 = SHULKER_SPAWNS[var1];
            BlockPos var10 = BlockPos.ofFloored(var8.x, var8.y, var8.z);
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(var10, 3.5), "Patrol Spawn " + (var1 + 1)));
         }
      }
   }

   private void H() {
      this.ab();
      this.ac();
      if (this.D()) {
         this.E();
      } else {
         BlockPos var1 = this.an();
         if (var1 != null) {
            this.currentShulkerPos = var1;
            NavigationManager.getInstance().c();
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
            this.state = AutoZamok.a.APPROACH_SHULKER;
            this.actionTimer.a();
            this.d("§aОбнаружен шалкер! Идём к нему...");
         } else if (this.currentPatrolTargetIndex == -1) {
            this.G();
         } else {
            Vec3d var2 = SHULKER_SPAWNS[this.currentPatrolTargetIndex];
            double var3 = mc.player.squaredDistanceTo(var2);
            boolean var5 = var3 <= 16.0;
            boolean var6 = !NavigationManager.getInstance().d() && this.patrolPointTimer.a(1200L);
            boolean var7 = this.patrolPointTimer.a(9000L);
            if (var5 || var6 || var7) {
               if (!this.visitedPatrolSpawns.contains(this.currentPatrolTargetIndex)) {
                  this.visitedPatrolSpawns.add(this.currentPatrolTargetIndex);
               }

               this.G();
            }
         }
      }
   }

   private void I() {
      this.ab();
      if (this.D()) {
         this.E();
      } else if (this.currentShulkerPos != null && this.a(this.currentShulkerPos)) {
         BlockPos var6 = this.an();
         if (var6 != null && !var6.equals(this.currentShulkerPos)) {
            double var2 = mc.player.squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos));
            double var4 = mc.player.squaredDistanceTo(Vec3d.ofCenter(var6));
            if (var4 + 25.0 < var2) {
               this.currentShulkerPos = var6;
               NavigationManager.getInstance().c();
               NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
               return;
            }
         }

         double var7 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos));
         double var8 = mc.player.squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos));
         if (var8 <= 3.8 || var7 <= 4.8) {
            NavigationManager.getInstance().c();
            this.state = AutoZamok.a.MINE_SHULKER;
            this.actionTimer.a();
         } else if (!NavigationManager.getInstance().d()) {
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
         }
      } else {
         BlockPos var1 = this.an();
         if (var1 != null) {
            this.currentShulkerPos = var1;
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
         } else {
            NavigationManager.getInstance().c();
            this.F();
         }
      }
   }

   private void J() {
      if (this.D()) {
         this.E();
      } else {
         if (this.currentShulkerPos == null) {
            BlockPos var1 = this.an();
            if (var1 == null) {
               if (mc.options != null) {
                  mc.options.attackKey.setPressed(false);
               }

               this.ab();
               this.F();
               return;
            }

            this.currentShulkerPos = var1;
         }

         if (!this.a(this.currentShulkerPos)) {
            if (mc.options != null) {
               mc.options.attackKey.setPressed(false);
            }

            if (mc.interactionManager != null) {
               mc.interactionManager.cancelBlockBreaking();
            }

            this.ab();
            this.state = AutoZamok.a.COLLECT_LOOT;
            this.actionTimer.a();
         } else {
            double var25 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos));
            double var3 = mc.player.squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos));
            if (var25 > 16.0) {
               if (mc.options != null) {
                  mc.options.attackKey.setPressed(false);
               }

               if (!NavigationManager.getInstance().d()) {
                  NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
               }

               this.state = AutoZamok.a.APPROACH_SHULKER;
            } else {
               if (!(var3 > 4.2) || !(var25 > 5.5)) {
                  NavigationManager.getInstance().c();
               } else if (!NavigationManager.getInstance().d()) {
                  NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
               }

               this.af();
               Vec3d var5 = Vec3d.ofCenter(this.currentShulkerPos);
               Vec3d var6 = mc.player.getEyePos();
               Direction var7 = Direction.UP;
               double var8 = Double.MAX_VALUE;

               for (Direction var13 : Direction.values()) {
                  Vec3d var14 = var5.add(var13.getOffsetX() * 0.5, var13.getOffsetY() * 0.5, var13.getOffsetZ() * 0.5);
                  double var15 = var6.squaredDistanceTo(var14);
                  if (var15 < var8) {
                     var8 = var15;
                     var7 = var13;
                  }
               }

               Vec3d var26 = this.a(this.currentShulkerPos, var7);
               double var27 = var26.x - var6.x;
               double var28 = var26.y - var6.y;
               double var29 = var26.z - var6.z;
               double var17 = Math.max(Math.hypot(var27, var29), 0.001);
               float var19 = (float)Math.toDegrees(Math.atan2(var29, var27)) - 90.0F;
               float var20 = (float)(-Math.toDegrees(Math.atan2(var28, var17)));
               long var21 = System.currentTimeMillis();
               float var23 = (float)(Math.sin(var21 * 0.015) * 0.07 + Math.cos(var21 * 0.031) * 0.04);
               float var24 = (float)(Math.cos(var21 * 0.017) * 0.06 + Math.sin(var21 * 0.027) * 0.03);
               var19 += GCDUtil2.a(var23);
               var20 = MathHelper.clamp(var20 + GCDUtil2.a(var24), -90.0F, 90.0F);
               mc.player.setYaw(var19);
               mc.player.setPitch(var20);
               RotationStorage.a(new Rotation2(var19, var20), 360.0F, 360.0F, 360.0F, 360.0F, 2, 1, true);
               if (mc.interactionManager != null) {
                  mc.options.attackKey.setPressed(true);
                  mc.interactionManager.updateBlockBreakingProgress(this.currentShulkerPos, var7);
                  ((IClientPlayerInteractionManager)mc.interactionManager).setBlockBreakingCooldown(0);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   }

   private Vec3d a(BlockPos var1, Direction var2) {
      Vec3d var3 = Vec3d.ofCenter(var1).add(var2.getOffsetX() * 0.49, var2.getOffsetY() * 0.49, var2.getOffsetZ() * 0.49);
      double var4 = System.currentTimeMillis() * 0.0025;
      double var6 = Math.sin(var4 * 1.3) * 0.16 + Math.cos(var4 * 2.1) * 0.05;
      double var8 = Math.cos(var4 * 1.1) * 0.16 + Math.sin(var4 * 1.8) * 0.05;
      Vec3d var10;
      Vec3d var11;
      if (var2.getAxis() == Axis.Y) {
         var10 = new Vec3d(1.0, 0.0, 0.0);
         var11 = new Vec3d(0.0, 0.0, 1.0);
      } else if (var2.getAxis() == Axis.X) {
         var10 = new Vec3d(0.0, 1.0, 0.0);
         var11 = new Vec3d(0.0, 0.0, 1.0);
      } else {
         var10 = new Vec3d(1.0, 0.0, 0.0);
         var11 = new Vec3d(0.0, 1.0, 0.0);
      }

      return var3.add(var10.multiply(var6)).add(var11.multiply(var8));
   }

   private void K() {
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }

      this.ad();
      this.ab();
      if (this.D() && this.actionTimer.a(600L)) {
         this.E();
      } else if (this.currentShulkerPos != null && this.a(this.currentShulkerPos)) {
         this.state = AutoZamok.a.MINE_SHULKER;
         this.actionTimer.a();
      } else {
         if (this.currentShulkerPos != null) {
            List var1 = mc.world.getEntitiesByClass(ItemEntity.class, new Box(this.currentShulkerPos).expand(6.0), var0 -> var0 != null && var0.isAlive());
            if (!var1.isEmpty()) {
               ItemEntity var2 = (ItemEntity)var1.getFirst();
               if (!NavigationManager.getInstance().d() && mc.player.squaredDistanceTo(var2) > 1.5) {
                  NavigationManager.getInstance().a(new GotoTask(var2.getBlockPos()));
               }
            }
         }

         if (this.actionTimer.a(1800L)) {
            BlockPos var3 = this.an();
            if (var3 != null && !this.D()) {
               this.currentShulkerPos = var3;
               NavigationManager.getInstance().c();
               NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
               this.state = AutoZamok.a.APPROACH_SHULKER;
               this.actionTimer.a();
               this.d("§aОбнаружен шалкер! Идём к нему...");
               return;
            }

            NavigationManager.getInstance().c();
            this.L();
            this.state = AutoZamok.a.RUN_AWAY;
            this.actionTimer.a();
         }
      }
   }

   private void L() {
      this.ad();
      this.ab();
      double var1 = mc.player.getX() - (this.currentShulkerPos != null ? this.currentShulkerPos.getX() : 0);
      double var3 = mc.player.getZ() - (this.currentShulkerPos != null ? this.currentShulkerPos.getZ() : 0);
      if (Math.hypot(var1, var3) < 0.1) {
         var1 = 1.0;
         var3 = 0.0;
      }

      double var5 = Math.hypot(var1, var3);
      int var7 = MathHelper.floor(mc.player.getX() + var1 / var5 * 22.0);
      int var8 = MathHelper.floor(mc.player.getZ() + var3 / var5 * 22.0);
      NavigationManager.getInstance().a(new GotoTask(var7, var8));
   }

   private void M() {
      this.ad();
      this.ab();
      if (!this.D()) {
         BlockPos var1 = this.an();
         if (var1 != null) {
            this.currentShulkerPos = var1;
            NavigationManager.getInstance().c();
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
            this.state = AutoZamok.a.APPROACH_SHULKER;
            this.actionTimer.a();
            this.d("§aОбнаружен шалкер! Идём к нему...");
            return;
         }
      }

      double var3 = this.currentShulkerPos != null ? mc.player.squaredDistanceTo(Vec3d.ofCenter(this.currentShulkerPos)) : 400.0;
      if (var3 >= 324.0 || !NavigationManager.getInstance().d() || this.actionTimer.a(8000L)) {
         NavigationManager.getInstance().c();
         this.k();
         this.state = AutoZamok.a.STOP_BEFORE_SKLAD_TP;
         this.actionTimer.a();
      }
   }

   private void N() {
      if (mc.player != null && mc.world != null) {
         if (this.state != AutoZamok.a.TP_TO_SKLAD
            && this.state != AutoZamok.a.WAIT_TP_SKLAD
            && this.state != AutoZamok.a.WAIT_TP_SKLAD_RETURN
            && this.state != AutoZamok.a.WAIT_TP_ZAMOK) {
            float var1 = mc.player.getHealth();
            LivingEntity var2 = mc.player.getAttacker();
            if (var2 == null) {
               var2 = mc.player.getLastAttacker();
            }

            PlayerEntity var3 = null;
            if (var2 instanceof PlayerEntity var4 && var4 != mc.player) {
               var3 = var4;
            } else if (var1 < this.lastPlayerHealth - 0.2F) {
               List var5 = mc.world
                  .getEntitiesByClass(
                     PlayerEntity.class, mc.player.getBoundingBox().expand(8.0), var0 -> var0 != mc.player && !var0.isSpectator() && var0.isAlive()
                  );
               if (!var5.isEmpty()) {
                  var3 = (PlayerEntity)var5.get(0);
               }
            }

            this.lastPlayerHealth = var1;
            if (var3 != null && this.state != AutoZamok.a.FLEE_PLAYER) {
               this.a(var3);
            }
         } else {
            this.lastPlayerHealth = mc.player.getHealth();
         }
      }
   }

   private void a(PlayerEntity var1) {
      this.fleeingFromPlayer = var1;
      this.state = AutoZamok.a.FLEE_PLAYER;
      this.fleeTimer.a();
      this.fleeRepathTimer.a();
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
         mc.options.useKey.setPressed(false);
      }

      if (mc.interactionManager != null) {
         mc.interactionManager.cancelBlockBreaking();
      }

      if (mc.currentScreen != null) {
         this.ar();
      }

      this.ab();
      this.ac();
      this.d("§cВНИМАНИЕ! Получен урон от игрока " + var1.getName().getString() + "! Начинаем убегать!");
      this.O();
   }

   private void O() {
      if (mc.player != null) {
         Vec3d var1 = mc.player.getEntityPos();
         Vec3d var2;
         if (this.fleeingFromPlayer != null && this.fleeingFromPlayer.isAlive()) {
            Vec3d var9 = this.fleeingFromPlayer.getEntityPos();
            double var4 = var1.x - var9.x;
            double var6 = var1.z - var9.z;
            if (Math.hypot(var4, var6) < 0.2) {
               float var8 = (float)Math.toRadians(mc.player.getYaw() + 180.0F);
               var2 = new Vec3d(-Math.sin(var8), 0.0, Math.cos(var8)).normalize();
            } else {
               var2 = new Vec3d(var4, 0.0, var6).normalize();
            }
         } else {
            float var3 = (float)Math.toRadians(mc.player.getYaw() + 180.0F);
            var2 = new Vec3d(-Math.sin(var3), 0.0, Math.cos(var3)).normalize();
         }

         double var10 = 28.0;
         int var5 = MathHelper.floor(var1.x + var2.x * var10);
         int var11 = MathHelper.floor(var1.z + var2.z * var10);
         NavigationManager.getInstance().a(new GotoTask(var5, var11));
         this.fleeRepathTimer.a();
      }
   }

   private void P() {
      this.ab();
      this.ac();
      if (this.fleeingFromPlayer != null && this.fleeingFromPlayer.isAlive()) {
         double var1 = mc.player.squaredDistanceTo(this.fleeingFromPlayer);
         if (var1 < 144.0 && this.fleeRepathTimer.a(1500L)) {
            this.O();
         }

         if (var1 > 576.0 && this.fleeTimer.a(6000L)) {
            this.d("§aУспешно оторвались от игрока! Возвращаемся на базу.");
            this.fleeingFromPlayer = null;
            NavigationManager.getInstance().c();
            this.k();
            this.state = AutoZamok.a.STOP_BEFORE_SKLAD_TP;
            this.actionTimer.a();
            return;
         }
      } else if (this.fleeTimer.a(5000L)) {
         this.d("§aУгроза миновала! Возвращаемся на базу.");
         this.fleeingFromPlayer = null;
         NavigationManager.getInstance().c();
         this.k();
         this.state = AutoZamok.a.STOP_BEFORE_SKLAD_TP;
         this.actionTimer.a();
         return;
      }

      if (this.fleeTimer.a(12000L)) {
         this.d("§eВремя побега вышло. Возвращаемся на базу.");
         this.fleeingFromPlayer = null;
         NavigationManager.getInstance().c();
         this.k();
         this.state = AutoZamok.a.STOP_BEFORE_SKLAD_TP;
         this.actionTimer.a();
      }
   }

   private void Q() {
      this.ad();
      this.k();
      this.ab();
      if (!this.D()) {
         BlockPos var1 = this.an();
         if (var1 != null) {
            this.currentShulkerPos = var1;
            NavigationManager.getInstance().c();
            NavigationManager.getInstance().a(new GotoTask(new GoalNear(this.currentShulkerPos, 1.4), "Goto Shulker"));
            this.state = AutoZamok.a.APPROACH_SHULKER;
            this.actionTimer.a();
            this.d("§aОбнаружен шалкер! Идём к нему...");
            return;
         }
      }

      if (this.actionTimer.a(800L) && System.currentTimeMillis() >= this.nextAllowedTpTime) {
         this.a(this.homeSklad.getValue(), AutoZamok.a.WAIT_TP_SKLAD_RETURN);
      }
   }

   private void R() {
      this.k();
      this.ab();
      if (!this.tpFailedCooldown && !this.tpCancelled) {
         long var1 = (long)(this.tpWarmupDelay.getFloatValue() * 1000.0F);
         long var3 = (long)(this.postTpDelay.getFloatValue() * 1000.0F);
         if (this.actionTimer.a(var1 + var3)) {
            this.state = AutoZamok.a.DEPOSIT_LOOT;
            this.actionTimer.a();
         }
      } else if (System.currentTimeMillis() >= this.nextAllowedTpTime && this.actionTimer.a(1000L)) {
         this.a(this.homeSklad.getValue(), AutoZamok.a.WAIT_TP_SKLAD_RETURN);
      }
   }

   private boolean S() {
      if (mc.player == null) {
         return false;
      }

      boolean var1 = false;
      int var2 = 0;
      int var3 = 0;

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = mc.player.getInventory().getStack(var4);
         if (var5 != null && !var5.isEmpty()) {
            if (this.b(var5)) {
               return true;
            }

            if (this.e(var5) && !var1) {
               var1 = true;
            } else if (this.f(var5) && var2 < 2) {
               var2++;
            } else if (this.g(var5) && var3 < 2) {
               var3++;
            } else if (!this.a(var5)) {
               return true;
            }
         }
      }

      ItemStack var9 = mc.player.getOffHandStack();
      if (var9 != null && !var9.isEmpty()) {
         if (this.b(var9)) {
            return true;
         }

         if (this.e(var9) && !var1) {
            var1 = true;
         } else if (this.f(var9) && var2 < 2) {
            var2++;
         } else if (this.g(var9) && var3 < 2) {
            var3++;
         } else if (!this.a(var9)) {
            return true;
         }
      }

      return false;
   }

   private void T() {
      this.chestQueue.clear();
      if (mc.player != null && mc.world != null) {
         BlockPos var1 = mc.player.getBlockPos();
         byte var2 = 5;
         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();

         for (int var5 = -var2; var5 <= var2; var5++) {
            for (int var6 = -var2; var6 <= var2; var6++) {
               for (int var7 = -var2; var7 <= var2; var7++) {
                  BlockPos var8 = var1.add(var5, var6, var7);
                  if (!(mc.player.squaredDistanceTo(Vec3d.ofCenter(var8)) > 20.25)) {
                     BlockState var9 = mc.world.getBlockState(var8);
                     Block var10 = var9.getBlock();
                     if (this.a(var10)) {
                        if (var10 instanceof ChestBlock) {
                           ChestType var11 = var9.get(ChestBlock.CHEST_TYPE);
                           if (var11 == ChestType.LEFT) {
                              continue;
                           }
                        }

                        if (this.b(var8)) {
                           var3.add(var8);
                        } else {
                           var4.add(var8);
                        }
                     }
                  }
               }
            }
         }

         var3.sort(
            (var0, var1x) -> Double.compare(
               mc.player.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var0)), mc.player.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var1x))
            )
         );
         var4.sort(
            (var0, var1x) -> Double.compare(
               mc.player.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var0)), mc.player.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var1x))
            )
         );
         this.chestQueue.addAll(var3);
         this.chestQueue.addAll(var4);
      }
   }

   private void U() {
      this.ad();
      this.ac();
      if (!this.S()) {
         this.X();
      } else {
         this.T();
         if (this.chestQueue.isEmpty()) {
            this.X();
         } else {
            this.currentChestIndex = 0;
            this.state = AutoZamok.a.OPEN_NEXT_DEPOSIT_CHEST;
            this.actionTimer.a();
         }
      }
   }

   private void V() {
      if (this.actionTimer.a((long)this.chestDelay.getFloatValue()) && mc.currentScreen == null) {
         this.ad();
         this.ac();
         if (!this.S()) {
            this.X();
         } else if (this.currentChestIndex >= this.chestQueue.size()) {
            this.X();
         } else {
            this.currentTargetChest = this.chestQueue.get(this.currentChestIndex);
            this.c(this.currentTargetChest);
            this.state = AutoZamok.a.WAIT_CHEST_DEPOSIT;
            this.actionTimer.a();
            this.slotActionTimer.a();
         }
      }
   }

   private void W() {
      if (this.actionTimer.a(4000L)) {
         this.ar();
         this.ac();
         this.ad();
         this.currentChestIndex++;
         this.state = AutoZamok.a.OPEN_NEXT_DEPOSIT_CHEST;
         this.actionTimer.a();
      } else if (mc.currentScreen instanceof GenericContainerScreen var1) {
         if (!this.slotActionTimer.a((long)this.chestDelay.getFloatValue())) {
            return;
         }

         GenericContainerScreenHandler var15 = var1.getScreenHandler();
         int var3 = var15.getRows() * 9;
         int var4 = -1;
         int var5 = -1;

         for (int var6 = var3; var6 < var3 + 36; var6++) {
            ItemStack var7 = var15.getSlot(var6).getStack();
            int var8 = this.d(var7);
            if (var8 > var5) {
               var5 = var8;
               var4 = var6;
            }
         }

         int var16 = -1;
         int var17 = var5;

         for (int var18 = 0; var18 < var3; var18++) {
            ItemStack var9 = var15.getSlot(var18).getStack();
            int var10 = this.d(var9);
            if (var10 > var17) {
               var17 = var10;
               var16 = var18;
            }
         }

         if (var16 != -1) {
            this.a(var15, var16);
            this.slotActionTimer.a();
            return;
         }

         int var19 = 0;
         int var20 = 0;

         for (int var21 = var3; var21 < var3 + 36; var21++) {
            ItemStack var11 = var15.getSlot(var21).getStack();
            if (var11 != null && !var11.isEmpty()) {
               if (this.b(var11)) {
                  if (mc.interactionManager != null && mc.player != null) {
                     mc.interactionManager.clickSlot(var15.syncId, var21, 1, SlotActionType.THROW, mc.player);
                  }

                  this.slotActionTimer.a();
                  return;
               }

               if (var21 != var4) {
                  if (this.f(var11) && var19 < 2) {
                     var19++;
                  } else if (this.g(var11) && var20 < 2) {
                     var20++;
                  } else if (!this.a(var11)) {
                     boolean var12 = false;

                     for (int var13 = 0; var13 < var3; var13++) {
                        ItemStack var14 = var15.getSlot(var13).getStack();
                        if (var14 == null || var14.isEmpty()) {
                           var12 = true;
                           break;
                        }

                        if (ItemStack.areItemsAndComponentsEqual(var11, var14) && var14.getCount() < var14.getMaxCount()) {
                           var12 = true;
                           break;
                        }
                     }

                     if (var12) {
                        this.a(var15, var21);
                        this.slotActionTimer.a();
                        return;
                     }
                  }
               }
            }
         }

         this.ar();
         this.ac();
         this.ad();
         this.af();
         this.currentChestIndex++;
         this.state = AutoZamok.a.OPEN_NEXT_DEPOSIT_CHEST;
         this.actionTimer.a();
      }
   }

   private void X() {
      boolean var1 = this.ah();
      boolean var2 = this.ai() || this.ak();
      if (var1 && var2) {
         this.state = AutoZamok.a.DRINK_INVIS;
         this.actionTimer.a();
      } else {
         this.ao();
         if (this.chestQueue.isEmpty()) {
            this.q();
         } else {
            this.currentChestIndex = 0;
            this.state = AutoZamok.a.RESTOCK_OPEN_NEXT_CHEST;
            this.actionTimer.a();
         }
      }
   }

   private void Y() {
      this.X();
   }

   private void Z() {
      if (this.actionTimer.a((long)this.chestDelay.getFloatValue()) && mc.currentScreen == null) {
         boolean var1 = this.ah();
         boolean var2 = this.ai() || this.ak();
         if (var1 && var2) {
            this.state = AutoZamok.a.DRINK_INVIS;
            this.actionTimer.a();
         } else if (this.currentChestIndex >= this.chestQueue.size()) {
            this.q();
         } else {
            this.currentTargetChest = this.chestQueue.get(this.currentChestIndex);
            this.c(this.currentTargetChest);
            this.state = AutoZamok.a.RESTOCK_WAIT_CHEST;
            this.actionTimer.a();
            this.slotActionTimer.a();
         }
      }
   }

   private void aa() {
      if (this.actionTimer.a(4000L)) {
         this.ar();
         this.currentChestIndex++;
         this.state = AutoZamok.a.RESTOCK_OPEN_NEXT_CHEST;
         this.actionTimer.a();
      } else if (mc.currentScreen instanceof GenericContainerScreen var1) {
         if (!this.slotActionTimer.a((long)this.chestDelay.getFloatValue())) {
            return;
         }

         GenericContainerScreenHandler var12 = var1.getScreenHandler();
         int var3 = var12.getRows() * 9;
         int var4 = -1;
         int var5 = this.ag();
         if (var5 != -1 && mc.player != null) {
            var4 = this.d(mc.player.getInventory().getStack(var5));
         }

         int var6 = -1;
         int var7 = var4;

         for (int var8 = 0; var8 < var3; var8++) {
            ItemStack var9 = var12.getSlot(var8).getStack();
            if (var9 != null && !var9.isEmpty()) {
               int var10 = this.d(var9);
               if (var10 > var7) {
                  var7 = var10;
                  var6 = var8;
               }
            }
         }

         if (var6 != -1) {
            this.a(var12, var6);
            this.slotActionTimer.a();
            return;
         }

         boolean var13 = !this.ai() && !this.ak();
         boolean var14 = !this.aj() && !this.al();

         for (int var15 = 0; var15 < var3; var15++) {
            ItemStack var11 = var12.getSlot(var15).getStack();
            if (var11 != null && !var11.isEmpty()) {
               if (var13 && this.f(var11)) {
                  this.a(var12, var15);
                  this.slotActionTimer.a();
                  return;
               }

               if (var14 && this.g(var11)) {
                  this.a(var12, var15);
                  this.slotActionTimer.a();
                  return;
               }
            }
         }

         this.ar();
         this.af();
         this.currentChestIndex++;
         this.state = AutoZamok.a.RESTOCK_OPEN_NEXT_CHEST;
         this.actionTimer.a();
      }
   }

   private void ab() {
      if (mc.player != null) {
         this.ae();

         for (int var1 = 0; var1 < 9; var1++) {
            if (mc.player.getInventory().getStack(var1).isEmpty()) {
               if (mc.player.getInventory().getSelectedSlot() != var1) {
                  InventoryUtility.e(var1);
               }

               return;
            }
         }

         for (int var3 = 9; var3 < 36; var3++) {
            if (mc.player.getInventory().getStack(var3).isEmpty()) {
               int var2 = mc.player.getInventory().getSelectedSlot();
               InventoryUtility.d(var3, var2);
               return;
            }
         }
      }
   }

   private void ac() {
      if (mc.player != null && !mc.player.getOffHandStack().isEmpty()) {
         for (int var1 = 9; var1 < 36; var1++) {
            if (mc.player.getInventory().getStack(var1).isEmpty()) {
               InventoryUtility.a(InventoryUtility.b(var1));
               break;
            }
         }
      }
   }

   private boolean a(ItemStack var1) {
      return var1 != null && !var1.isEmpty() ? var1.contains(DataComponentTypes.FOOD) : false;
   }

   private boolean b(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (this.a(var1)) {
         return false;
      }

      if (var1.isOf(Items.GLASS_BOTTLE)) {
         return true;
      }

      if (var1.isOf(Items.CHAINMAIL_HELMET) || var1.isOf(Items.CHAINMAIL_CHESTPLATE) || var1.isOf(Items.CHAINMAIL_LEGGINGS) || var1.isOf(Items.CHAINMAIL_BOOTS)
         )
       {
         return true;
      }

      if (!var1.isOf(Items.STONE_SWORD) && !var1.isOf(Items.STONE_SHOVEL) && !var1.isOf(Items.STONE_AXE) && !var1.isOf(Items.STONE_HOE)) {
         if ((var1.isOf(Items.STONE_PICKAXE) || var1.isOf(Items.WOODEN_PICKAXE)) && mc.player != null) {
            int var2 = this.ag();
            if (var2 != -1) {
               ItemStack var3 = mc.player.getInventory().getStack(var2);
               if (this.d(var3) > this.d(var1)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean c(ItemStack var1) {
      return this.b(var1);
   }

   private void ad() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.currentScreen instanceof GenericContainerScreen var8) {
            GenericContainerScreenHandler var12 = var8.getScreenHandler();
            int var3 = var12.getRows() * 9;

            for (int var4 = var3; var4 < var12.slots.size(); var4++) {
               ItemStack var5 = var12.getSlot(var4).getStack();
               if (this.b(var5)) {
                  mc.interactionManager.clickSlot(var12.syncId, var4, 1, SlotActionType.THROW, mc.player);
               }
            }
         } else if (mc.currentScreen == null) {
            for (int var1 = 0; var1 < 9; var1++) {
               ItemStack var9 = mc.player.getInventory().getStack(var1);
               if (this.b(var9)) {
                  mc.interactionManager.clickSlot(0, 36 + var1, 1, SlotActionType.THROW, mc.player);
               }
            }

            for (int var6 = 9; var6 < 36; var6++) {
               ItemStack var10 = mc.player.getInventory().getStack(var6);
               if (this.b(var10)) {
                  mc.interactionManager.clickSlot(0, var6, 1, SlotActionType.THROW, mc.player);
               }
            }

            for (int var7 = 5; var7 <= 8; var7++) {
               ItemStack var11 = mc.player.playerScreenHandler.getSlot(var7).getStack();
               if (this.b(var11)) {
                  mc.interactionManager.clickSlot(0, var7, 1, SlotActionType.THROW, mc.player);
               }
            }

            if (this.b(mc.player.getOffHandStack())) {
               mc.interactionManager.clickSlot(0, 45, 1, SlotActionType.THROW, mc.player);
            }
         }
      }
   }

   private void ae() {
      this.ad();
   }

   private void af() {
      if (mc.player != null) {
         int var1 = this.ag();
         if (var1 != -1) {
            if (var1 < 9) {
               if (mc.player.getInventory().getSelectedSlot() != var1) {
                  InventoryUtility.e(var1);
               }
            } else {
               int var2 = mc.player.getInventory().getSelectedSlot();
               InventoryUtility.d(var1, var2);
            }
         }
      }
   }

   private int d(ItemStack var1) {
      if (var1 != null && !var1.isEmpty() && this.e(var1)) {
         Item var2 = var1.getItem();
         short var3 = 0;
         if (var2 == Items.NETHERITE_PICKAXE) {
            var3 = 6000;
         } else if (var2 == Items.DIAMOND_PICKAXE) {
            var3 = 5000;
         } else if (var2 == Items.GOLDEN_PICKAXE) {
            var3 = 4000;
         } else if (var2 == Items.IRON_PICKAXE) {
            var3 = 3000;
         } else if (var2 == Items.STONE_PICKAXE) {
            var3 = 2000;
         } else if (var2 == Items.WOODEN_PICKAXE) {
            var3 = 1000;
         } else {
            var3 = 500;
         }

         float var4 = var1.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
         int var6 = var3 + (int)(var4 * 100.0F);
         if (var1.hasEnchantments()) {
            var6 += 300;
         }

         if (var1.isDamaged()) {
            var6 -= (int)((float)var1.getDamage() / Math.max(1, var1.getMaxDamage()) * 100.0F);
         }

         return var6;
      } else {
         return -1;
      }
   }

   private int ag() {
      if (mc.player == null) {
         return -1;
      }

      int var1 = -1;
      int var2 = -1;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = mc.player.getInventory().getStack(var3);
         int var5 = this.d(var4);
         if (var5 > var2) {
            var2 = var5;
            var1 = var3;
         }
      }

      return var1;
   }

   private boolean e(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (HotbarUtil.b(var1.getItem())) {
         return true;
      }

      String var2 = var1.getName().getString().toLowerCase(Locale.ROOT);
      return var2.contains("кирк") || var2.contains("pickaxe");
   }

   private boolean ah() {
      return this.ag() != -1;
   }

   private boolean ai() {
      if (mc.player == null) {
         return false;
      }

      StatusEffectInstance var1 = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
      return var1 != null && var1.getDuration() > 200;
   }

   private boolean aj() {
      if (mc.player == null) {
         return false;
      }

      StatusEffectInstance var1 = mc.player.getStatusEffect(StatusEffects.SPEED);
      return var1 != null && var1.getDuration() > 200;
   }

   private boolean f(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (!(var1.getItem() instanceof PotionItem)) {
         return false;
      }

      if (PotionUtility.a(var1, StatusEffects.INVISIBILITY)) {
         return true;
      }

      String var2 = var1.getName().getString().toLowerCase(Locale.ROOT);
      if (!var2.contains("невидим") && !var2.contains("invis")) {
         PotionContentsComponent var3 = var1.get(DataComponentTypes.POTION_CONTENTS);
         if (var3 != null) {
            for (StatusEffectInstance var5 : var3.getEffects()) {
               if (var5.getEffectType().equals(StatusEffects.INVISIBILITY)) {
                  return true;
               }
            }

            if (var3.potion().isPresent()) {
               String var7 = var3.potion().get().getKey().map(var0 -> var0.getValue().getPath()).orElse("").toLowerCase(Locale.ROOT);
               if (var7.contains("invis")) {
                  return true;
               }
            }
         }

         for (String var9 : this.h(var1)) {
            String var6 = this.b(var9).toLowerCase(Locale.ROOT);
            if (var6.contains("невидим") || var6.contains("invis")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean g(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (!(var1.getItem() instanceof PotionItem)) {
         return false;
      }

      if (PotionUtility.a(var1, StatusEffects.SPEED)) {
         return true;
      }

      String var2 = var1.getName().getString().toLowerCase(Locale.ROOT);
      if (!var2.contains("скорост") && !var2.contains("speed")) {
         PotionContentsComponent var3 = var1.get(DataComponentTypes.POTION_CONTENTS);
         if (var3 != null) {
            for (StatusEffectInstance var5 : var3.getEffects()) {
               if (var5.getEffectType().equals(StatusEffects.SPEED)) {
                  return true;
               }
            }

            if (var3.potion().isPresent()) {
               String var7 = var3.potion().get().getKey().map(var0 -> var0.getValue().getPath()).orElse("").toLowerCase(Locale.ROOT);
               if (var7.contains("speed") || var7.contains("swiftness")) {
                  return true;
               }
            }
         }

         for (String var9 : this.h(var1)) {
            String var6 = this.b(var9).toLowerCase(Locale.ROOT);
            if (var6.contains("скорост") || var6.contains("speed") || var6.contains("стремительн")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private List<String> h(ItemStack var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && !var1.isEmpty()) {
         try {
            TooltipContext var3 = mc.world != null ? TooltipContext.create(mc.world) : TooltipContext.DEFAULT;
            List var4 = var1.getTooltip(var3, mc.player, TooltipType.BASIC);
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
            var2.add(var1.getName().getString());
         }

         return var2;
      } else {
         return var2;
      }
   }

   private String b(String var1) {
      return var1 == null ? "" : var1.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
   }

   private boolean ak() {
      return this.b(true) != -1;
   }

   private boolean al() {
      return this.b(false) != -1;
   }

   private int b(boolean var1) {
      if (mc.player == null) {
         return -1;
      }

      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var1 ? this.f(var3) : this.g(var3)) {
            return var2;
         }
      }

      for (int var4 = 9; var4 < 36; var4++) {
         ItemStack var5 = mc.player.getInventory().getStack(var4);
         if (var1 ? this.f(var5) : this.g(var5)) {
            return var4;
         }
      }

      return -1;
   }

   private void b(int var1) {
      if (mc.player != null) {
         if (var1 >= 0 && var1 < 9) {
            if (mc.player.getInventory().getSelectedSlot() != var1) {
               InventoryUtility.e(var1);
            }
         } else {
            int var2 = -1;

            for (int var3 = 0; var3 < 9; var3++) {
               if (mc.player.getInventory().getStack(var3).isEmpty()) {
                  var2 = var3;
                  break;
               }
            }

            if (var2 == -1) {
               var2 = mc.player.getInventory().getSelectedSlot();
            }

            InventoryUtility.e(var2);
            if (mc.interactionManager != null && mc.player.currentScreenHandler != null) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, var2, SlotActionType.SWAP, mc.player);
            }
         }
      }
   }

   private void am() {
      if (mc.player != null && mc.interactionManager != null) {
         mc.player.setPitch(90.0F);
         RotationStorage.a(new Rotation2(mc.player.getYaw(), 90.0F), 360.0F, 360.0F, 360.0F, 360.0F, 2, 1, false);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private BlockPos an() {
      if (mc.world != null && mc.player != null) {
         BlockPos var1 = null;
         double var2 = Double.MAX_VALUE;

         for (Vec3d var7 : SHULKER_SPAWNS) {
            int var8 = MathHelper.floor(var7.x);
            int var9 = MathHelper.floor(var7.y);
            int var10 = MathHelper.floor(var7.z);
            if (mc.world.isChunkLoaded(var8 >> 4, var10 >> 4)) {
               for (int var11 = -5; var11 <= 5; var11++) {
                  for (int var12 = -3; var12 <= 5; var12++) {
                     for (int var13 = -5; var13 <= 5; var13++) {
                        BlockPos var14 = new BlockPos(var8 + var11, var9 + var12, var10 + var13);
                        BlockState var15 = mc.world.getBlockState(var14);
                        if (var15.getBlock() instanceof ShulkerBoxBlock) {
                           double var16 = mc.player.squaredDistanceTo(Vec3d.ofCenter(var14));
                           if (var16 < var2) {
                              var2 = var16;
                              var1 = var14;
                           }
                        }
                     }
                  }
               }
            }
         }

         BlockPos var18 = mc.player.getBlockPos();
         byte var19 = 24;

         for (int var20 = -var19; var20 <= var19; var20++) {
            for (int var21 = -var19; var21 <= var19; var21++) {
               int var22 = var18.getX() + var20 >> 4;
               int var23 = var18.getZ() + var21 >> 4;
               if (mc.world.isChunkLoaded(var22, var23)) {
                  for (int var24 = -8; var24 <= 12; var24++) {
                     BlockPos var25 = var18.add(var20, var24, var21);
                     BlockState var26 = mc.world.getBlockState(var25);
                     if (var26.getBlock() instanceof ShulkerBoxBlock) {
                        double var27 = mc.player.squaredDistanceTo(Vec3d.ofCenter(var25));
                        if (var27 < var2) {
                           var2 = var27;
                           var1 = var25;
                        }
                     }
                  }
               }
            }
         }

         return var1;
      } else {
         return null;
      }
   }

   private boolean a(BlockPos var1) {
      return mc.world != null && var1 != null ? mc.world.getBlockState(var1).getBlock() instanceof ShulkerBoxBlock : false;
   }

   private void ao() {
      this.chestQueue.clear();
      if (mc.player != null && mc.world != null) {
         BlockPos var1 = mc.player.getBlockPos();
         byte var2 = 5;

         for (int var3 = -var2; var3 <= var2; var3++) {
            for (int var4 = -var2; var4 <= var2; var4++) {
               for (int var5 = -var2; var5 <= var2; var5++) {
                  BlockPos var6 = var1.add(var3, var4, var5);
                  BlockState var7 = mc.world.getBlockState(var6);
                  Block var8 = var7.getBlock();
                  if (this.a(var8)) {
                     if (var8 instanceof ChestBlock) {
                        ChestType var9 = var7.get(ChestBlock.CHEST_TYPE);
                        if (var9 == ChestType.LEFT) {
                           continue;
                        }
                     }

                     this.chestQueue.add(var6);
                  }
               }
            }
         }

         this.chestQueue
            .sort((var0, var1x) -> Double.compare(mc.player.squaredDistanceTo(Vec3d.ofCenter(var0)), mc.player.squaredDistanceTo(Vec3d.ofCenter(var1x))));
      }
   }

   private BlockPos ap() {
      if (mc.player != null && mc.world != null) {
         BlockPos var1 = mc.player.getBlockPos();
         BlockPos var2 = null;
         double var3 = Double.MAX_VALUE;
         byte var5 = 5;

         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               for (int var8 = -var5; var8 <= var5; var8++) {
                  BlockPos var9 = var1.add(var6, var7, var8);
                  BlockState var10 = mc.world.getBlockState(var9);
                  Block var11 = var10.getBlock();
                  if (this.a(var11) && this.b(var9)) {
                     double var12 = mc.player.squaredDistanceTo(Vec3d.ofCenter(var9));
                     if (var12 < var3) {
                        var3 = var12;
                        var2 = var9;
                     }
                  }
               }
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   private BlockPos aq() {
      if (mc.player != null && mc.world != null) {
         BlockPos var1 = mc.player.getBlockPos();
         BlockPos var2 = null;
         double var3 = Double.MAX_VALUE;
         byte var5 = 5;

         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               for (int var8 = -var5; var8 <= var5; var8++) {
                  BlockPos var9 = var1.add(var6, var7, var8);
                  BlockState var10 = mc.world.getBlockState(var9);
                  Block var11 = var10.getBlock();
                  if (this.a(var11)) {
                     double var12 = mc.player.squaredDistanceTo(Vec3d.ofCenter(var9));
                     if (var12 < var3) {
                        var3 = var12;
                        var2 = var9;
                     }
                  }
               }
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   private boolean a(Block var1) {
      return var1 instanceof ChestBlock || var1 instanceof TrappedChestBlock || var1 instanceof BarrelBlock;
   }

   private boolean b(BlockPos var1) {
      if (mc.world == null) {
         return false;
      }

      for (Direction var5 : Direction.values()) {
         BlockPos var6 = var1.offset(var5);
         BlockState var7 = mc.world.getBlockState(var6);
         if (var7.getBlock() instanceof AbstractSignBlock) {
            return true;
         }
      }

      return false;
   }

   private void c(BlockPos var1) {
      if (mc.player != null && mc.interactionManager != null && var1 != null) {
         BlockHitResult var2 = new BlockHitResult(Vec3d.ofCenter(var1), Direction.UP, var1, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var2);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void a(GenericContainerScreenHandler var1, int var2) {
      if (mc.interactionManager != null && mc.player != null && var1 != null) {
         mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.QUICK_MOVE, mc.player);
      }
   }

   private void ar() {
      if (mc.player != null) {
         mc.player.closeHandledScreen();
      }
   }

   private void c(String var1) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatCommand(var1);
      }
   }

   private void d(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("§6[AutoZamok] §f" + var1), false);
      }
   }

   enum a {
      IDLE,
      TP_TO_SKLAD,
      WAIT_TP_SKLAD,
      SEARCH_SUPPLIES,
      OPEN_NEXT_SUPPLY_CHEST,
      WAIT_CHEST_SUPPLIES,
      DRINK_INVIS,
      PREPARE_DRINK_INVIS,
      DRINKING_INVIS,
      WAIT_SPLASH_INVIS,
      PAUSE_AFTER_INVIS,
      DRINK_SPEED,
      PREPARE_DRINK_SPEED,
      DRINKING_SPEED,
      WAIT_SPLASH_SPEED,
      PAUSE_AFTER_SPEED,
      TP_TO_ZAMOK,
      WAIT_TP_ZAMOK,
      PATROL_SHULKER_SPAWNS,
      APPROACH_SHULKER,
      MINE_SHULKER,
      COLLECT_LOOT,
      RUN_AWAY,
      FLEE_PLAYER,
      STOP_BEFORE_SKLAD_TP,
      WAIT_TP_SKLAD_RETURN,
      DEPOSIT_LOOT,
      OPEN_NEXT_DEPOSIT_CHEST,
      WAIT_CHEST_DEPOSIT,
      RESTOCK_SEARCH_CHESTS,
      RESTOCK_OPEN_NEXT_CHEST,
      RESTOCK_WAIT_CHEST;
   }
}
