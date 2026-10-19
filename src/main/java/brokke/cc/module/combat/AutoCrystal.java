package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.core.SlotSearchResult;
import brokke.cc.core.Timer2;
import brokke.cc.manager.FriendManager;
import brokke.cc.rotation.Rotation2;
import brokke.cc.rotation.RotationStorage;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.DeltaInventoryUtil;
import brokke.cc.util.GCDUtil;
import brokke.cc.util.HotbarUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@NewFunction(
   name = "AutoCrystal",
   category = Category.COMBAT,
   description = "Выбирает подходящий тип взрыва по позиции противника: использует кристаллы при открытой позиции над основанием, а в остальных случаях — якоря с защитой от собственного урона."
)
public class AutoCrystal extends Function {
   public static AutoCrystal INSTANCE;
   private final BooleanSetting pingFix = new BooleanSetting("Ping Fix from TAB", true);
   private final BooleanSetting autoTarget = new BooleanSetting("Авто таргет", true);
   private final NumberSetting targetRange = new NumberSetting("Дистанция поиска цели", 6.0, 3.0, 10.0, 0.5);
   private final BooleanSetting bindOnly = new BooleanSetting("Только по зажатию бинда", false);
   private final BindSetting cycleBind = new BindSetting("Кнопка активации", 86);
   private final BooleanSetting onlySword = new BooleanSetting("Только с оружием", false);
   private final BooleanSetting preferCrystals = new BooleanSetting("Кристаллы при открытой позиции", true);
   private final BooleanSetting placeObsidian = new BooleanSetting("Ставить обсидиан", true).visibleWhen(this.preferCrystals::isToggled);
   private final BooleanSetting autoAnchor = new BooleanSetting("Авто якоря", true);
   private final BooleanSetting safeAnchor = new BooleanSetting("SafeAnchor (Защита)", true).visibleWhen(this.autoAnchor::isToggled);
   private final NumberSetting range = new NumberSetting("Дистанция взаимодействия", 4.5, 2.0, 6.0, 0.1);
   private final NumberSetting actionDelay = new NumberSetting("Задержка (тиков)", 0.0, 0.0, 5.0, 1.0);
   private final BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
   private final BooleanSetting autoSwap = new BooleanSetting("AutoSwap", true);
   private final BooleanSetting autoSwapSword = new BooleanSetting("Sword", true).visibleWhen(this.autoSwap::isToggled);
   private final BooleanSetting autoSwapAxe = new BooleanSetting("Axe", false).visibleWhen(this.autoSwap::isToggled);
   private final BooleanSetting autoSwapMace = new BooleanSetting("Mace", true).visibleWhen(this.autoSwap::isToggled);
   private final BooleanSetting autoTotem = new BooleanSetting("AutoTotem", true);
   private final NumberSetting totemHotbarSlot = new NumberSetting("Totem Hotbar Slot (1-9)", 1.0, 1.0, 9.0, 1.0).visibleWhen(this.autoTotem::isToggled);
   private final NumberSetting totemBatchDelay = new NumberSetting("Refill Delay (ticks)", 1.0, 0.0, 10.0, 1.0).visibleWhen(this.autoTotem::isToggled);
   private static final Pattern TAB_PING_REGEX = Pattern.compile("(?i)\\b(\\d{1,5})\\s*ms\\b");
   private static final float MACE_CRIT_CHARGE = 0.91F;
   private int delayTicks = 0;
   private final Map<Integer, Long> deadCrystals = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> placedAnchors = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> chargedAnchors = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> safeShieldAttempted = new ConcurrentHashMap<>();
   private boolean placingInternal = false;
   private boolean conflictingActionSentThisTick = false;
   private BlockPos lastPlacedObsidian = null;
   private long lastPlacedObsidianTick = 0L;
   private final Timer2 totemBatchTimer = new Timer2();
   private long tickCounter = 0L;
   private boolean totemRefillPending = false;
   private Integer maceRestoreSlot = null;
   private Integer maceActionSlot = null;
   private long maceRestoreAtTick = 0L;

   public boolean isPlacingInternal() {
      return this.placingInternal;
   }

   public AutoCrystal() {
      INSTANCE = this;
      this.addSettings(
         this.pingFix,
         this.autoTarget,
         this.targetRange,
         this.bindOnly,
         this.cycleBind,
         this.onlySword,
         this.preferCrystals,
         this.placeObsidian,
         this.autoAnchor,
         this.safeAnchor,
         this.range,
         this.actionDelay,
         this.clientLook,
         this.autoSwap,
         this.autoSwapSword,
         this.autoSwapAxe,
         this.autoSwapMace,
         this.autoTotem,
         this.totemHotbarSlot,
         this.totemBatchDelay
      );
   }

   @Override
   public void onEnable() {
      this.n();
      this.m();
      this.totemBatchTimer.a();
      this.p();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.b(true);
      this.m();
      this.p();
      if (RotationStorage.instance != null && RotationStorage.instance.k()) {
         RotationStorage.instance.j();
      }

      super.onDisable();
   }

   private void m() {
      this.delayTicks = 0;
      this.placingInternal = false;
      this.conflictingActionSentThisTick = false;
      this.deadCrystals.clear();
   }

   private void n() {
      this.maceRestoreSlot = null;
      this.maceActionSlot = null;
      this.maceRestoreAtTick = 0L;
   }

   private void a(int var1, int var2) {
      this.maceRestoreSlot = var1;
      this.maceActionSlot = var2;
      this.maceRestoreAtTick = this.tickCounter + 1L;
   }

   private void b(boolean var1) {
      if (this.maceRestoreSlot == null || mc.player == null) {
         this.n();
      } else if (var1 || this.tickCounter >= this.maceRestoreAtTick) {
         int var2 = this.maceRestoreSlot;
         Integer var3 = this.maceActionSlot;
         this.n();
         if ((var3 == null || mc.player.getInventory().getSelectedSlot() == var3)
            && var2 >= 0
            && var2 <= 8
            && var2 != mc.player.getInventory().getSelectedSlot()) {
            HandlerProcessor.a().getUseableHandler().a(var2);
         }
      }
   }

   private void o() {
      this.b(false);
   }

   private void p() {
      this.totemRefillPending = false;
   }

   private void q() {
      if (this.pingFix.isToggled() && mc.player != null && mc.getNetworkHandler() != null) {
         try {
            PlayerListHud var1 = mc.inGameHud.getPlayerListHud();
            Text var2 = null;

            for (Field var6 : PlayerListHud.class.getDeclaredFields()) {
               if (Text.class.isAssignableFrom(var6.getType())) {
                  var6.setAccessible(true);
                  Text var7 = (Text)var6.get(var1);
                  if (var7 != null && TAB_PING_REGEX.matcher(var7.getString()).find()) {
                     var2 = var7;
                     break;
                  }
               }
            }

            if (var2 == null) {
               return;
            }

            Matcher var11 = TAB_PING_REGEX.matcher(var2.getString());
            if (!var11.find()) {
               return;
            }

            int var12 = Integer.parseInt(var11.group(1));
            int var13 = MathHelper.clamp(var12, 0, 60000);
            PlayerListEntry var14 = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (var14 != null) {
               for (Field var9 : PlayerListEntry.class.getDeclaredFields()) {
                  if (var9.getType() == int.class) {
                     var9.setAccessible(true);
                     var9.setInt(var14, var13);
                     break;
                  }
               }
            }
         } catch (Exception var11) {
         }
      }
   }

   @EventHandler
   public void onAttackEntity(AttackEntityEvent var1) {
      if (mc.player != null && mc.world != null) {
         Entity var2 = var1.getTarget();
         if (this.autoSwap.isToggled()) {
            Integer var3 = this.a(var2);
            if (var3 != null && var3 >= 0 && var3 <= 8) {
               boolean var4 = mc.player.getInventory().getStack(var3).isOf(Items.MACE);
               int var5 = mc.player.getInventory().getSelectedSlot();
               if ((!this.conflictingActionSentThisTick || var3 == var5) && (!var4 || !(mc.player.getAttackCooldownProgress(0.5F) < 0.91F)) && var3 != var5) {
                  HandlerProcessor.a().getUseableHandler().a(var3);
                  this.conflictingActionSentThisTick = true;
                  if (var4) {
                     this.a(var5, var3);
                  }
               }
            }
         }
      }
   }

   private Integer a(Entity var1) {
      int var2 = mc.player.getInventory().getSelectedSlot();
      boolean var3 = this.maceRestoreSlot != null
         && this.maceActionSlot != null
         && this.maceActionSlot == var2
         && mc.player.getMainHandStack().isOf(Items.MACE);
      if (this.autoSwapMace.isToggled() && this.b(var1) && var3) {
         return var2;
      }

      if (this.autoSwapMace.isToggled() && this.b(var1) && this.r()) {
         Integer var4 = this.c(var1);
         if (var4 != null) {
            return var4;
         }
      }

      if (this.autoSwapAxe.isToggled() && var1 instanceof PlayerEntity var6 && this.a(var6)) {
         SlotSearchResult var5 = HotbarUtil.d();
         if (var5.isFound()) {
            return var5.getSlot();
         }
      }

      if (this.autoSwapSword.isToggled()) {
         SlotSearchResult var7 = HotbarUtil.i();
         if (var7.isFound()) {
            return var7.getSlot();
         }
      }

      return null;
   }

   private boolean r() {
      ItemStack var1 = mc.player.getMainHandStack();
      return var1.isIn(ItemTags.SWORDS);
   }

   private boolean a(PlayerEntity var1) {
      if (var1.isBlocking()) {
         return true;
      } else if (var1.getActiveItem().isOf(Items.SHIELD)) {
         return true;
      } else {
         return !var1.isUsingItem() ? false : var1.getMainHandStack().isOf(Items.SHIELD) || var1.getOffHandStack().isOf(Items.SHIELD);
      }
   }

   private boolean b(Entity var1) {
      return var1 instanceof LivingEntity
         && mc.player.fallDistance > 0.0
         && mc.player.getVelocity().y < 0.0
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.isInLava()
         && !mc.player.isGliding()
         && !mc.player.hasVehicle()
         && !mc.player.getAbilities().flying
         && !mc.player.isUsingRiptide()
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
   }

   private Integer c(Entity var1) {
      double var2 = mc.player.fallDistance;
      boolean var4 = var2 > 1.5 && !mc.player.isGliding();
      int var5 = -1;
      double var6 = -Double.MAX_VALUE;

      for (int var8 = 0; var8 < 9; var8++) {
         ItemStack var9 = mc.player.getInventory().getStack(var8);
         if (var9.isOf(Items.MACE)) {
            double var10 = this.a(var9, var1, var2, var4);
            if (var10 > var6) {
               var6 = var10;
               var5 = var8;
            }
         }
      }

      return var5 == -1 ? null : var5;
   }

   private double a(ItemStack var1, Entity var2, double var3, boolean var5) {
      int var6 = this.a(var1, Enchantments.DENSITY);
      int var7 = this.a(var1, Enchantments.BREACH);
      int var8 = this.a(var1, Enchantments.SMITE);
      int var9 = this.a(var1, Enchantments.BANE_OF_ARTHROPODS);
      int var10 = this.a(var1, Enchantments.WIND_BURST);
      double var11 = 6.0;
      if (var5) {
         if (var3 <= 3.0) {
            var11 += 4.0 * var3;
         } else if (var3 <= 8.0) {
            var11 += 12.0 + 2.0 * (var3 - 3.0);
         } else {
            var11 += 22.0 + var3 - 8.0;
         }

         var11 += 0.5 * var6 * var3;
         var11 += 5.0 * var10;
      }

      double var15 = var11 + 2.0 * var7;
      if (var2 instanceof LivingEntity) {
         if (var8 > 0 && this.d(var2)) {
            var15 += 2.5 * var8;
         }

         if (var9 > 0 && this.e(var2)) {
            var15 += 2.5 * var9;
         }
      }

      return var15;
   }

   private int a(ItemStack var1, RegistryKey<Enchantment> var2) {
      ItemEnchantmentsComponent var3 = var1.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

      for (RegistryEntry var5 : var3.getEnchantments()) {
         if (var5.matchesKey(var2)) {
            return var3.getLevel(var5);
         }
      }

      return 0;
   }

   private boolean d(Entity var1) {
      EntityType var2 = var1.getType();
      return var2 == EntityType.BOGGED
         || var2 == EntityType.DROWNED
         || var2 == EntityType.HUSK
         || var2 == EntityType.PHANTOM
         || var2 == EntityType.SKELETON
         || var2 == EntityType.SKELETON_HORSE
         || var2 == EntityType.STRAY
         || var2 == EntityType.WITHER
         || var2 == EntityType.WITHER_SKELETON
         || var2 == EntityType.ZOGLIN
         || var2 == EntityType.ZOMBIE
         || var2 == EntityType.ZOMBIE_HORSE
         || var2 == EntityType.ZOMBIE_VILLAGER
         || var2 == EntityType.ZOMBIFIED_PIGLIN;
   }

   private boolean e(Entity var1) {
      EntityType var2 = var1.getType();
      return var2 == EntityType.BEE
         || var2 == EntityType.CAVE_SPIDER
         || var2 == EntityType.ENDERMITE
         || var2 == EntityType.SILVERFISH
         || var2 == EntityType.SPIDER;
   }

   public boolean l() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (this.onlySword.isToggled()) {
            ItemStack var1 = mc.player.getMainHandStack();
            if (var1.isEmpty() || !var1.isIn(ItemTags.SWORDS) && !var1.isIn(ItemTags.AXES) && !var1.isOf(Items.MACE)) {
               return false;
            }
         }

         if (!this.bindOnly.isToggled()) {
            return true;
         } else {
            int var4 = this.cycleBind.getKey();
            if (var4 != -1 && var4 != 0) {
               long var2 = mc.getWindow().getHandle();
               return var4 < 0 ? GLFW.glfwGetMouseButton(var2, -var4 - 100) == 1 : GLFW.glfwGetKey(var2, var4) == 1;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         this.tickCounter++;
         this.q();
         this.o();
         if (!mc.player.isDead() && !(mc.player.getHealth() <= 0.0F)) {
            this.v();
            this.conflictingActionSentThisTick = false;
            if (!this.autoTotem.isToggled() || !this.w() || !this.x()) {
               if (!this.l()) {
                  if (RotationStorage.instance != null && RotationStorage.instance.k()) {
                     RotationStorage.instance.j();
                  }
               } else if (this.delayTicks > 0) {
                  this.delayTicks--;
               } else {
                  PlayerEntity var2 = this.s();
                  if (var2 == null) {
                     if (RotationStorage.instance != null && RotationStorage.instance.k()) {
                        RotationStorage.instance.j();
                     }
                  } else {
                     this.b(var2);
                  }
               }
            }
         }
      }
   }

   private boolean a(Vec3d var1) {
      if (mc.player == null) {
         return false;
      }

      Vec3d var2 = mc.player.getEyePos();
      double var3 = var1.x - var2.x;
      double var5 = var1.y - var2.y;
      double var7 = var1.z - var2.z;
      double var9 = Math.sqrt(var3 * var3 + var7 * var7);
      float var11 = (float)Math.toDegrees(Math.atan2(var7, var3)) - 90.0F;
      float var12 = (float)(-Math.toDegrees(Math.atan2(var5, var9)));
      float var13 = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - var11));
      float var14 = Math.abs(mc.player.getPitch() - var12);
      return var13 < 3.5F && var14 < 3.5F;
   }

   private Rotation2 a(float var1, float var2, float var3, float var4) {
      float var5 = GCDUtil.a();
      if (var5 <= 1.0E-4F) {
         var5 = 0.005F;
      }

      float var6 = MathHelper.wrapDegrees(var3 - var1);
      float var7 = var4 - var2;
      float var8 = Math.round(var6 / var5) * var5;
      float var9 = Math.round(var7 / var5) * var5;
      if (Math.abs(var8) < 1.0E-4F && Math.abs(var9) < 1.0E-4F) {
         var8 = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * var5;
         var9 = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * var5;
      }

      float var10 = var1 + var8;
      float var11 = MathHelper.clamp(var2 + var9, -90.0F, 90.0F);
      return new Rotation2(var10, var11);
   }

   private PlayerEntity s() {
      if (!this.autoTarget.isToggled()
         && mc.crosshairTarget instanceof EntityHitResult var2
         && var2.getEntity() instanceof PlayerEntity var3
         && var3 != mc.player
         && !var3.isDead()
         && var3.getHealth() > 0.0F
         && !FriendManager.a(var3.getName().getString())) {
         return var3;
      }

      double var10 = this.targetRange.getValue() * this.targetRange.getValue();
      PlayerEntity var11 = null;
      double var12 = var10;

      for (PlayerEntity var7 : mc.world.getPlayers()) {
         if (var7 != mc.player && !var7.isDead() && !(var7.getHealth() <= 0.0F) && !FriendManager.a(var7.getName().getString())) {
            double var8 = mc.player.squaredDistanceTo(var7);
            if (var8 < var12) {
               var12 = var8;
               var11 = var7;
            }
         }
      }

      return var11;
   }

   private Vec3d a(BlockPos var1, Direction var2) {
      Vec3d var3 = mc.player != null ? mc.player.getEyePos() : Vec3d.ofCenter(var1);
      Vec3d var4 = Vec3d.ofCenter(var1);
      Vec3d var5 = var4.subtract(var3);
      double var6 = var5.length();
      if (var6 > 1.0E-4) {
         var5 = var5.multiply(1.0 / var6);
      }

      double var8 = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.08;
      double var10 = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.08;
      double var12 = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.08;
      double var14 = var4.x + var8;
      double var16 = var4.y + var10;
      double var18 = var4.z + var12;
      switch (var2) {
         case UP:
            var16 = var1.getY() + 1.0;
            if (Math.abs(var5.y) > 1.0E-4) {
               double var26 = (var16 - var3.y) / var5.y;
               if (var26 > 0.0) {
                  var14 = var3.x + var26 * var5.x + var8;
                  var18 = var3.z + var26 * var5.z + var12;
               }
            }

            var14 = MathHelper.clamp(var14, var1.getX() + 0.15, var1.getX() + 0.85);
            var18 = MathHelper.clamp(var18, var1.getZ() + 0.15, var1.getZ() + 0.85);
            break;
         case DOWN:
            var16 = var1.getY();
            if (Math.abs(var5.y) > 1.0E-4) {
               double var25 = (var16 - var3.y) / var5.y;
               if (var25 > 0.0) {
                  var14 = var3.x + var25 * var5.x + var8;
                  var18 = var3.z + var25 * var5.z + var12;
               }
            }

            var14 = MathHelper.clamp(var14, var1.getX() + 0.15, var1.getX() + 0.85);
            var18 = MathHelper.clamp(var18, var1.getZ() + 0.15, var1.getZ() + 0.85);
            break;
         case NORTH:
            var18 = var1.getZ();
            if (Math.abs(var5.z) > 1.0E-4) {
               double var24 = (var18 - var3.z) / var5.z;
               if (var24 > 0.0) {
                  var14 = var3.x + var24 * var5.x + var8;
                  var16 = var3.y + var24 * var5.y + var10;
               }
            }

            var14 = MathHelper.clamp(var14, var1.getX() + 0.15, var1.getX() + 0.85);
            var16 = MathHelper.clamp(var16, var1.getY() + 0.15, var1.getY() + 0.85);
            break;
         case SOUTH:
            var18 = var1.getZ() + 1.0;
            if (Math.abs(var5.z) > 1.0E-4) {
               double var23 = (var18 - var3.z) / var5.z;
               if (var23 > 0.0) {
                  var14 = var3.x + var23 * var5.x + var8;
                  var16 = var3.y + var23 * var5.y + var10;
               }
            }

            var14 = MathHelper.clamp(var14, var1.getX() + 0.15, var1.getX() + 0.85);
            var16 = MathHelper.clamp(var16, var1.getY() + 0.15, var1.getY() + 0.85);
            break;
         case WEST:
            var14 = var1.getX();
            if (Math.abs(var5.x) > 1.0E-4) {
               double var22 = (var14 - var3.x) / var5.x;
               if (var22 > 0.0) {
                  var16 = var3.y + var22 * var5.y + var10;
                  var18 = var3.z + var22 * var5.z + var12;
               }
            }

            var16 = MathHelper.clamp(var16, var1.getY() + 0.15, var1.getY() + 0.85);
            var18 = MathHelper.clamp(var18, var1.getZ() + 0.15, var1.getZ() + 0.85);
            break;
         case EAST:
            var14 = var1.getX() + 1.0;
            if (Math.abs(var5.x) > 1.0E-4) {
               double var20 = (var14 - var3.x) / var5.x;
               if (var20 > 0.0) {
                  var16 = var3.y + var20 * var5.y + var10;
                  var18 = var3.z + var20 * var5.z + var12;
               }
            }

            var16 = MathHelper.clamp(var16, var1.getY() + 0.15, var1.getY() + 0.85);
            var18 = MathHelper.clamp(var18, var1.getZ() + 0.15, var1.getZ() + 0.85);
      }

      return new Vec3d(var14, var16, var18);
   }

   private void b(PlayerEntity var1) {
      boolean var2 = mc.world.getRegistryKey().getValue().getPath().equals("the_nether");
      boolean var3 = this.preferCrystals.isToggled() && this.c(var1);
      if ((!var3 || !this.d(var1))
         && (!this.autoAnchor.isToggled() || var2 || !this.f(var1))
         && RotationStorage.instance != null
         && RotationStorage.instance.k()) {
         RotationStorage.instance.j();
      }
   }

   private boolean a(Item var1) {
      if (mc.player == null) {
         return false;
      } else {
         return !mc.player.getMainHandStack().isOf(var1) && !mc.player.getOffHandStack().isOf(var1)
            ? DeltaInventoryUtil.a(var1, true) != -1 || DeltaInventoryUtil.a(var1, false) != -1
            : true;
      }
   }

   private boolean c(PlayerEntity var1) {
      if (var1 != null && this.a(Items.END_CRYSTAL)) {
         int var2 = mc.player.getBlockPos().getY();
         int var3 = var1.getBlockPos().getY();
         if (var3 <= var2) {
            return false;
         }

         List var4 = this.e(var1);
         if (var4.isEmpty()) {
            return false;
         }

         boolean var5 = false;

         for (BlockPos var7 : (Iterable<BlockPos>)(Iterable<?>)(var4)) {
            if (mc.world.getBlockState(var7).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(var7).isOf(Blocks.BEDROCK)) {
               var5 = true;
               break;
            }
         }

         return var5 ? true : this.placeObsidian.isToggled() && this.a(Items.OBSIDIAN);
      } else {
         return false;
      }
   }

   private boolean d(PlayerEntity var1) {
      if (var1 != null && mc.player != null && mc.world != null) {
         if (this.lastPlacedObsidian != null && this.tickCounter <= this.lastPlacedObsidianTick) {
            BlockPos var2 = this.lastPlacedObsidian;
            if (mc.world.getBlockState(var2).isOf(Blocks.OBSIDIAN)) {
               if (this.f(var2)) {
                  EndCrystalEntity var3 = this.g(var2);
                  if (var3 != null && mc.player.squaredDistanceTo(var3) > var1.squaredDistanceTo(var3)) {
                     Vec3d var4 = var3.getBoundingBox().getCenter();
                     boolean var5 = this.a(var4);
                     this.b(var4);
                     if (var5) {
                        this.a(var3);
                        this.delayTicks = (int)this.actionDelay.getValue();
                     }

                     return true;
                  }
               } else if (this.e(var2)) {
                  Hand var13 = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL) ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  if (var13 == Hand.OFF_HAND || this.b(Items.END_CRYSTAL)) {
                     Vec3d var16 = this.a(var2, Direction.UP);
                     boolean var19 = this.a(var16);
                     this.b(var16);
                     if (var19 && this.a(var2, var16, var13)) {
                        this.delayTicks = (int)this.actionDelay.getValue();
                     }

                     return true;
                  }
               }
            } else {
               this.lastPlacedObsidian = null;
            }
         }

         EndCrystalEntity var12 = this.h(var1.getBlockPos());
         if (var12 != null && mc.player.squaredDistanceTo(var12) > var1.squaredDistanceTo(var12)) {
            Vec3d var15 = var12.getBoundingBox().getCenter();
            boolean var18 = this.a(var15);
            this.b(var15);
            if (var18) {
               this.a(var12);
               this.delayTicks = (int)this.actionDelay.getValue();
            }

            return true;
         } else {
            for (BlockPos var20 : this.e(var1)) {
               BlockState var6 = mc.world.getBlockState(var20);
               if (!var6.isOf(Blocks.OBSIDIAN) && !var6.isOf(Blocks.BEDROCK)) {
                  if (this.placeObsidian.isToggled() && this.d(var20)) {
                     Direction var22 = this.a(var20);
                     if (var22 != null) {
                        Hand var24 = mc.player.getOffHandStack().isOf(Items.OBSIDIAN) ? Hand.OFF_HAND : Hand.MAIN_HAND;
                        if (var24 != Hand.MAIN_HAND || this.b(Items.OBSIDIAN)) {
                           BlockPos var26 = var20.offset(var22.getOpposite());
                           Vec3d var10 = this.a(var26, var22);
                           boolean var11 = this.a(var10);
                           this.b(var10);
                           if (var11 && this.a(var26, var22, var10, var24)) {
                              this.delayTicks = (int)this.actionDelay.getValue();
                           }

                           return true;
                        }
                     }
                  }
               } else if (this.f(var20)) {
                  EndCrystalEntity var21 = this.g(var20);
                  if (var21 != null && mc.player.squaredDistanceTo(var21) > var1.squaredDistanceTo(var21)) {
                     Vec3d var23 = var21.getBoundingBox().getCenter();
                     boolean var25 = this.a(var23);
                     this.b(var23);
                     if (var25) {
                        this.a(var21);
                        this.delayTicks = (int)this.actionDelay.getValue();
                     }

                     return true;
                  }
               } else if (this.e(var20)) {
                  Hand var7 = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL) ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  if (var7 != Hand.MAIN_HAND || this.b(Items.END_CRYSTAL)) {
                     Vec3d var8 = this.a(var20, Direction.UP);
                     boolean var9 = this.a(var8);
                     this.b(var8);
                     if (var9 && this.a(var20, var8, var7)) {
                        this.delayTicks = (int)this.actionDelay.getValue();
                     }

                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private List<BlockPos> e(PlayerEntity var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && mc.player != null && mc.world != null) {
         BlockPos var3 = var1.getBlockPos();
         double var4 = this.range.getValue() * this.range.getValue();
         int var6 = var3.getY();

         for (int var7 = -2; var7 <= 2; var7++) {
            for (int var8 = -2; var8 <= 2; var8++) {
               for (int var9 = -1; var9 <= 1; var9++) {
                  BlockPos var10 = new BlockPos(var3.getX() + var7, var6 + var9, var3.getZ() + var8);
                  Vec3d var11 = Vec3d.ofCenter(var10);
                  Vec3d var12 = new Vec3d(var10.getX() + 0.5, var10.getY() + 1.0, var10.getZ() + 0.5);
                  if (!(mc.player.squaredDistanceTo(var11) > var4) && !(mc.player.squaredDistanceTo(var12) <= var1.squaredDistanceTo(var12))) {
                     BlockState var13 = mc.world.getBlockState(var10);
                     boolean var14 = var13.isOf(Blocks.OBSIDIAN) || var13.isOf(Blocks.BEDROCK);
                     boolean var15 = this.placeObsidian.isToggled() && this.d(var10) && this.a(var10) != null;
                     if (var14 || var15) {
                        BlockPos var16 = var10.up();
                        BlockPos var17 = var10.up(2);
                        BlockState var18 = mc.world.getBlockState(var16);
                        BlockState var19 = mc.world.getBlockState(var17);
                        if ((var18.isAir() || var18.isReplaceable()) && (var19.isAir() || var19.isReplaceable())) {
                           var2.add(var10);
                        }
                     }
                  }
               }
            }
         }

         var2.sort((var1xx, var2x) -> {
            boolean var3x = mc.world.getBlockState((BlockPos)var1xx).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState((BlockPos)var1xx).isOf(Blocks.BEDROCK);
            boolean var4x = mc.world.getBlockState((BlockPos)var2x).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState((BlockPos)var2x).isOf(Blocks.BEDROCK);
            if (var3x != var4x) {
               return var3x ? -1 : 1;
            }

            double var5 = var1.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var1xx));
            double var7x = var1.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var2x));
            return Double.compare(var5, var7x);
         });
         return var2;
      } else {
         return var2;
      }
   }

   private Direction a(BlockPos var1) {
      for (Direction var5 : Direction.values()) {
         BlockPos var6 = var1.offset(var5.getOpposite());
         BlockState var7 = mc.world.getBlockState(var6);
         if (!var7.isAir() && !var7.isReplaceable() && !var7.isLiquid()) {
            return var5;
         }
      }

      return null;
   }

   private boolean f(PlayerEntity var1) {
      if (var1 != null && mc.player != null && mc.world != null) {
         for (BlockPos var4 : this.g(var1)) {
            if (this.i(var4)) {
               if (this.safeAnchor.isToggled() && this.b(var4) && !this.safeShieldAttempted.containsKey(var4)) {
                  BlockPos var5 = this.c(var4);
                  if (var5 != null && !mc.world.getBlockState(var5).isOf(Blocks.GLOWSTONE)) {
                     Direction var6 = this.a(var5);
                     if (var6 != null && this.b(Items.GLOWSTONE)) {
                        BlockPos var7 = var5.offset(var6.getOpposite());
                        Vec3d var8 = this.a(var7, var6);
                        boolean var9 = this.a(var8);
                        this.b(var8);
                        if (var9 && this.a(var5, var7, var6, var8)) {
                           this.safeShieldAttempted.put(var4, this.tickCounter + 20L);
                           this.delayTicks = (int)this.actionDelay.getValue();
                        }

                        return true;
                     }
                  }
               }

               if (this.u()) {
                  Vec3d var12 = this.a(var4, Direction.UP);
                  boolean var15 = this.a(var12);
                  this.b(var12);
                  if (var15 && this.b(var4, var12)) {
                     this.delayTicks = (int)this.actionDelay.getValue();
                  }

                  return true;
               }
            } else if (this.b(Items.GLOWSTONE)) {
               Vec3d var13 = this.a(var4, Direction.UP);
               boolean var16 = this.a(var13);
               this.b(var13);
               if (var16 && this.a(var4, var13)) {
                  this.delayTicks = (int)this.actionDelay.getValue();
               }

               return true;
            }
         }

         for (BlockPos var14 : this.h(var1)) {
            Direction var17 = this.a(var14);
            if (var17 != null && this.b(Items.RESPAWN_ANCHOR)) {
               BlockPos var18 = var14.offset(var17.getOpposite());
               Vec3d var19 = this.a(var18, var17);
               boolean var20 = this.a(var19);
               this.b(var19);
               if (var20 && this.a(var18, var17, var19)) {
                  this.delayTicks = (int)this.actionDelay.getValue();
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private List<BlockPos> g(PlayerEntity var1) {
      ArrayList var2 = new ArrayList();
      BlockPos var3 = var1.getBlockPos();
      double var4 = this.range.getValue() * this.range.getValue();

      for (int var6 = -3; var6 <= 3; var6++) {
         for (int var7 = -2; var7 <= 2; var7++) {
            for (int var8 = -3; var8 <= 3; var8++) {
               BlockPos var9 = var3.add(var6, var7, var8);
               if (!(mc.player.squaredDistanceTo(Vec3d.ofCenter(var9)) > var4)) {
                  boolean var10 = mc.world.getBlockState(var9).isOf(Blocks.RESPAWN_ANCHOR);
                  boolean var11 = this.placedAnchors.containsKey(var9) && this.tickCounter <= this.placedAnchors.get(var9);
                  if (var10 || var11) {
                     var2.add(var9);
                  }
               }
            }
         }
      }

      var2.sort(Comparator.comparingDouble(var1xx -> var1.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var1xx))));
      return var2;
   }

   private List<BlockPos> h(PlayerEntity var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && mc.player != null && mc.world != null) {
         BlockPos var3 = var1.getBlockPos();
         double var4 = this.range.getValue() * this.range.getValue();
         int var6 = var3.getY();

         for (int var7 = -2; var7 <= 2; var7++) {
            for (int var8 = -2; var8 <= 2; var8++) {
               for (int var9 = -1; var9 <= 1; var9++) {
                  BlockPos var10 = new BlockPos(var3.getX() + var7, var6 + var9, var3.getZ() + var8);
                  Vec3d var11 = Vec3d.ofCenter(var10);
                  if (!(mc.player.squaredDistanceTo(var11) > var4)) {
                     BlockState var12 = mc.world.getBlockState(var10);
                     if (var12.isAir() || var12.isReplaceable()) {
                        Box var13 = new Box(var10);
                        boolean var14 = false;

                        for (Entity var16 : mc.world.getEntities()) {
                           if (!(
                                 var16 instanceof EndCrystalEntity var17
                                    && (!var17.isAlive() || var17.isRemoved() || this.deadCrystals.containsKey(var17.getId()))
                              )
                              && var16.isAlive()
                              && !var16.isRemoved()
                              && !(var16 instanceof ItemEntity)
                              && !(var16 instanceof ExperienceOrbEntity)
                              && var16.getBoundingBox().intersects(var13)) {
                              var14 = true;
                              break;
                           }
                        }

                        if (!var14 && this.a(var10) != null) {
                           var2.add(var10);
                        }
                     }
                  }
               }
            }
         }

         var2.sort(Comparator.comparingDouble(var1xx -> var1.squaredDistanceTo(Vec3d.ofCenter((BlockPos)var1xx))));
         return var2;
      } else {
         return var2;
      }
   }

   private boolean a(BlockPos var1, Direction var2, Vec3d var3) {
      if (mc.player != null && mc.interactionManager != null && var1 != null && var2 != null) {
         this.placingInternal = true;

         try {
            BlockHitResult var4 = new BlockHitResult(var3, var2, var1, false);
            ActionResult var5 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var4);
            if (var5.isAccepted()) {
               mc.player.swingHand(Hand.MAIN_HAND);
               this.conflictingActionSentThisTick = true;
               BlockPos var6 = var1.offset(var2);
               this.placedAnchors.put(var6, this.tickCounter + 40L);
               if (mc.world != null) {
                  mc.world.setBlockState(var6, Blocks.RESPAWN_ANCHOR.getDefaultState());
               }

               return true;
            }
         } finally {
            this.placingInternal = false;
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean a(BlockPos var1, Vec3d var2) {
      if (mc.player != null && mc.interactionManager != null && var1 != null) {
         this.placingInternal = true;

         try {
            BlockHitResult var3 = new BlockHitResult(var2, Direction.UP, var1, false);
            ActionResult var4 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var3);
            if (var4.isAccepted()) {
               mc.player.swingHand(Hand.MAIN_HAND);
               this.conflictingActionSentThisTick = true;
               this.chargedAnchors.put(var1, this.tickCounter + 40L);
               if (mc.world != null) {
                  try {
                     mc.world.setBlockState(var1, Blocks.RESPAWN_ANCHOR.getDefaultState().with(RespawnAnchorBlock.CHARGES, 1));
                  } catch (Exception var9) {
                  }
               }

               return true;
            }
         } finally {
            this.placingInternal = false;
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean b(BlockPos var1, Vec3d var2) {
      if (mc.player != null && mc.interactionManager != null && var1 != null) {
         this.placingInternal = true;

         try {
            BlockHitResult var3 = new BlockHitResult(var2, Direction.UP, var1, false);
            ActionResult var4 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var3);
            if (var4.isAccepted()) {
               mc.player.swingHand(Hand.MAIN_HAND);
               this.conflictingActionSentThisTick = true;
               this.chargedAnchors.remove(var1);
               this.placedAnchors.remove(var1);
               this.safeShieldAttempted.remove(var1);
               if (mc.world != null) {
                  mc.world.setBlockState(var1, Blocks.AIR.getDefaultState());
               }

               return true;
            }
         } finally {
            this.placingInternal = false;
         }

         return false;
      } else {
         return false;
      }
   }

   private int t() {
      int var1 = DeltaInventoryUtil.a(Items.TOTEM_OF_UNDYING, true);
      if (var1 != -1) {
         return var1;
      }

      int var2 = DeltaInventoryUtil.a(Items.NETHERITE_SWORD, true);
      if (var2 == -1) {
         var2 = DeltaInventoryUtil.a(Items.DIAMOND_SWORD, true);
      }

      if (var2 != -1) {
         return var2;
      }

      int var3 = DeltaInventoryUtil.a(Items.MACE, true);
      if (var3 != -1) {
         return var3;
      }

      int var4 = mc.player.getInventory().getSelectedSlot();
      if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         return var4;
      }

      for (int var5 = 0; var5 < 9; var5++) {
         ItemStack var6 = mc.player.getInventory().getStack(var5);
         if (!var6.isOf(Items.GLOWSTONE)) {
            return var5;
         }
      }

      return -1;
   }

   private boolean b(BlockPos var1) {
      return var1.getY() == MathHelper.floor(mc.player.getY());
   }

   private BlockPos c(BlockPos var1) {
      double var2 = mc.player.getX() - (var1.getX() + 0.5);
      double var4 = mc.player.getZ() - (var1.getZ() + 0.5);
      int var6 = var2 > 0.05 ? 1 : (var2 < -0.05 ? -1 : 0);
      int var7 = var4 > 0.05 ? 1 : (var4 < -0.05 ? -1 : 0);
      if (Math.abs(var2) >= Math.abs(var4) && var6 != 0) {
         return new BlockPos(var1.getX() + var6, var1.getY(), var1.getZ());
      } else {
         return var7 != 0 ? new BlockPos(var1.getX(), var1.getY(), var1.getZ() + var7) : null;
      }
   }

   private boolean a(BlockPos var1, BlockPos var2, Direction var3, Vec3d var4) {
      if (mc.player != null && mc.interactionManager != null && var2 != null && var3 != null) {
         BlockState var5 = mc.world.getBlockState(var1);
         if (var5.isOf(Blocks.GLOWSTONE)) {
            return true;
         }

         if (!var5.isAir() && !var5.isReplaceable()) {
            return false;
         }

         this.placingInternal = true;

         try {
            BlockHitResult var6 = new BlockHitResult(var4, var3, var2, false);
            ActionResult var7 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var6);
            if (var7.isAccepted()) {
               mc.player.swingHand(Hand.MAIN_HAND);
               this.conflictingActionSentThisTick = true;
               return true;
            }
         } finally {
            this.placingInternal = false;
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean a(BlockPos var1, Direction var2, Vec3d var3, Hand var4) {
      if (mc.player != null && mc.interactionManager != null && var1 != null && var2 != null) {
         this.placingInternal = true;

         ActionResult var5;
         try {
            BlockHitResult var6 = new BlockHitResult(var3, var2, var1, false);
            var5 = mc.interactionManager.interactBlock(mc.player, var4, var6);
         } finally {
            this.placingInternal = false;
         }

         if (var5.isAccepted()) {
            mc.player.swingHand(var4);
            this.conflictingActionSentThisTick = true;
            BlockPos var10 = var1.offset(var2);
            this.lastPlacedObsidian = var10;
            this.lastPlacedObsidianTick = this.tickCounter + 20L;
            if (mc.world != null) {
               mc.world.setBlockState(var10, Blocks.OBSIDIAN.getDefaultState());
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean a(BlockPos var1, Vec3d var2, Hand var3) {
      if (mc.player != null && mc.interactionManager != null && var1 != null) {
         BlockHitResult var4 = new BlockHitResult(var2, Direction.UP, var1, false);
         this.placingInternal = true;

         ActionResult var5;
         try {
            var5 = mc.interactionManager.interactBlock(mc.player, var3, var4);
         } finally {
            this.placingInternal = false;
         }

         if (var5.isAccepted()) {
            mc.player.swingHand(var3);
            this.conflictingActionSentThisTick = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void a(EndCrystalEntity var1) {
      if (mc.player != null && mc.interactionManager != null && var1 != null) {
         mc.interactionManager.attackEntity(mc.player, var1);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.deadCrystals.put(var1.getId(), System.currentTimeMillis());

         try {
            var1.discard();
         } catch (Exception var3) {
         }

         this.conflictingActionSentThisTick = true;
      }
   }

   private void b(Vec3d var1) {
      Vec3d var2 = mc.player.getEyePos();
      double var3 = var1.x - var2.x;
      double var5 = var1.y - var2.y;
      double var7 = var1.z - var2.z;
      double var9 = Math.sqrt(var3 * var3 + var7 * var7);
      float var11 = (float)Math.toDegrees(Math.atan2(var7, var3)) - 90.0F;
      float var12 = (float)(-Math.toDegrees(Math.atan2(var5, var9)));
      Rotation2 var13 = this.a(mc.player.getYaw(), mc.player.getPitch(), var11, var12);
      RotationStorage.a(var13, 180.0F, 180.0F, 180.0F, 180.0F, 1, 1, this.clientLook.isToggled());
   }

   private boolean d(BlockPos var1) {
      if (mc.world != null && mc.player != null && var1 != null) {
         if (mc.player.squaredDistanceTo(Vec3d.ofCenter(var1)) > this.range.getValue() * this.range.getValue()) {
            return false;
         }

         BlockState var2 = mc.world.getBlockState(var1);
         if (!var2.isAir() && !var2.isReplaceable()) {
            return false;
         }

         Box var3 = new Box(var1);

         for (Entity var5 : mc.world.getEntities()) {
            if (!(var5 instanceof EndCrystalEntity var6 && (!var6.isAlive() || var6.isRemoved() || this.deadCrystals.containsKey(var6.getId())))
               && var5.isAlive()
               && !var5.isRemoved()
               && !(var5 instanceof ItemEntity)
               && !(var5 instanceof ExperienceOrbEntity)
               && var5.getBoundingBox().intersects(var3)) {
               return false;
            }
         }

         BlockPos var8 = var1.up();
         BlockPos var9 = var1.up(2);
         BlockState var10 = mc.world.getBlockState(var8);
         BlockState var7 = mc.world.getBlockState(var9);
         return (var10.isAir() || var10.isReplaceable()) && (var7.isAir() || var7.isReplaceable());
      } else {
         return false;
      }
   }

   private boolean e(BlockPos var1) {
      if (mc.world != null && mc.player != null && var1 != null) {
         if (mc.player.squaredDistanceTo(Vec3d.ofCenter(var1)) > this.range.getValue() * this.range.getValue()) {
            return false;
         }

         BlockPos var2 = var1.up();
         BlockPos var3 = var1.up(2);
         BlockState var4 = mc.world.getBlockState(var2);
         BlockState var5 = mc.world.getBlockState(var3);
         if (!var4.isAir() && !var4.isReplaceable()) {
            return false;
         }

         if (!var5.isAir() && !var5.isReplaceable()) {
            return false;
         }

         Box var6 = new Box(var1.getX(), var1.getY() + 1.0, var1.getZ(), var1.getX() + 1.0, var1.getY() + 3.0, var1.getZ() + 1.0);

         for (Entity var8 : mc.world.getEntities()) {
            if (!(var8 instanceof EndCrystalEntity var9 && (!var9.isAlive() || var9.isRemoved() || this.deadCrystals.containsKey(var9.getId())))
               && var8.isAlive()
               && !var8.isRemoved()
               && !(var8 instanceof ItemEntity)
               && !(var8 instanceof ExperienceOrbEntity)
               && var8.getBoundingBox().intersects(var6)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean f(BlockPos var1) {
      return this.g(var1) != null;
   }

   private EndCrystalEntity g(BlockPos var1) {
      if (mc.world != null && var1 != null) {
         Box var2 = new Box(var1.getX(), var1.getY() + 0.5, var1.getZ(), var1.getX() + 1.0, var1.getY() + 2.5, var1.getZ() + 1.0);

         for (Entity var4 : mc.world.getEntities()) {
            if (var4 instanceof EndCrystalEntity var5
               && !this.deadCrystals.containsKey(var5.getId())
               && var5.isAlive()
               && !var5.isRemoved()
               && var5.getBoundingBox().intersects(var2)) {
               return var5;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private EndCrystalEntity h(BlockPos var1) {
      if (mc.player != null && mc.world != null) {
         EndCrystalEntity var2 = null;
         double var3 = this.range.getValue() * this.range.getValue();
         Vec3d var5 = Vec3d.ofCenter(var1);

         for (Entity var7 : mc.world.getEntities()) {
            if (var7 instanceof EndCrystalEntity var8 && !this.deadCrystals.containsKey(var8.getId()) && var8.isAlive() && !var8.isRemoved()) {
               double var9 = mc.player.squaredDistanceTo(var8);
               double var11 = var5.squaredDistanceTo(var8.getX(), var8.getY(), var8.getZ());
               if (!(var9 > var3) && !(var9 <= var11) && var11 <= var3) {
                  var3 = var11;
                  var2 = var8;
               }
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   private boolean i(BlockPos var1) {
      if (this.chargedAnchors.containsKey(var1) && this.tickCounter <= this.chargedAnchors.get(var1)) {
         return true;
      }

      BlockState var2 = mc.world.getBlockState(var1);
      if (!var2.isOf(Blocks.RESPAWN_ANCHOR)) {
         return false;
      }

      try {
         if (var2.get(RespawnAnchorBlock.CHARGES) > 0) {
            return true;
         }
      } catch (Exception var4) {
      }

      return var2.getLuminance() > 0;
   }

   private boolean b(Item var1) {
      if (mc.player == null) {
         return false;
      } else if (mc.player.getMainHandStack().isOf(var1)) {
         return true;
      } else {
         int var2 = DeltaInventoryUtil.a(var1, true);
         if (var2 != -1) {
            this.b(var2);
            return true;
         } else {
            int var3 = DeltaInventoryUtil.a(var1, false);
            if (var3 != -1) {
               HandlerProcessor.a().getInventoryHandler().a(var3, mc.player.getInventory().getSelectedSlot(), 1);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private boolean u() {
      int var1 = this.t();
      if (var1 == -1) {
         return false;
      }

      this.b(var1);
      return true;
   }

   private void b(int var1) {
      if (var1 >= 0 && var1 <= 8 && mc.player != null) {
         int var2 = mc.player.getInventory().getSelectedSlot();
         if (var1 != var2) {
            HandlerProcessor.a().getUseableHandler().a(var1);
         }
      }
   }

   private void v() {
      long var1 = System.currentTimeMillis();
      this.deadCrystals.entrySet().removeIf(var2 -> var1 - var2.getValue() > 1000L);
   }

   private boolean w() {
      return mc.currentScreen == null || mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof ChatScreen;
   }

   private boolean x() {
      int var1 = (int)this.totemHotbarSlot.getValue() - 1;
      int var4 = MathHelper.clamp(var1, 0, 8);
      ItemStack var2 = mc.player.getInventory().getStack(var4);
      if (var2.isOf(Items.TOTEM_OF_UNDYING)) {
         this.totemRefillPending = false;
         return false;
      }

      if (!this.totemBatchTimer.a((long)this.totemBatchDelay.getValue() * 50L)) {
         return false;
      }

      int var3 = this.y();
      if (var3 == -1) {
         return false;
      }

      HandlerProcessor.a().getInventoryHandler().a(var3, var4, 1);
      this.totemBatchTimer.a();
      this.conflictingActionSentThisTick = true;
      return true;
   }

   private int y() {
      if (mc.player != null && mc.player.playerScreenHandler != null) {
         for (Slot var2 : mc.player.playerScreenHandler.slots) {
            if (var2.id >= 9 && var2.id < 36 && var2.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
               return var2.id;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.world != null && var1.getPacket() instanceof ExplosionS2CPacket var2) {
         BlockPos var7 = BlockPos.ofFloored(var2.center());
         this.chargedAnchors.remove(var7);
         this.placedAnchors.remove(var7);
         this.safeShieldAttempted.remove(var7);

         for (Entity var5 : mc.world.getEntities()) {
            if (var5 instanceof EndCrystalEntity var6) {
               this.deadCrystals.put(var6.getId(), System.currentTimeMillis());
            }
         }
      }
   }

   static class a {
      final int x;
      final int y;
      final int z;
      final Direction side;

      a(int var1, int var2, int var3, Direction var4) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.side = var4;
      }
   }
}
