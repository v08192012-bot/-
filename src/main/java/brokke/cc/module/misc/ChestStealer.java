package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.Timer2;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventGameUpdate;
import java.util.Locale;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "Chest Stealer", description = "Автоматически забирает вещи из сундука", category = Category.MISC)
public class ChestStealer extends Function {
   private final NumberSetting delay = new NumberSetting("Задержка", 50.0, 10.0, 250.0, 1.0);
   private final BooleanSetting holyWorldPriority = new BooleanSetting("Приоритет HolyWorld", false);
   private final BooleanSetting autoOpen = new BooleanSetting("Открывать сундук/шалкер", false);
   private final BooleanSetting autoDisable = new BooleanSetting("Выключать после закрытия", false);
   private final Timer2 timer = new Timer2();
   private final Timer2 openTimer = new Timer2();
   private boolean wasInContainer = false;

   public ChestStealer() {
      this.addSettings(this.delay, this.holyWorldPriority, this.autoOpen, this.autoDisable);
   }

   @Override
   public void onEnable() {
      this.timer.a();
      this.openTimer.a();
      this.wasInContainer = false;
   }

   @Override
   public void onDisable() {
      this.timer.a();
      this.openTimer.a();
      this.wasInContainer = false;
   }

   @EventHandler
   public void onGameUpdate(EventGameUpdate var1) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null) {
            if (this.wasInContainer) {
               this.wasInContainer = false;
               if (this.autoDisable.getValue()) {
                  this.setEnabled(false);
                  return;
               }
            }

            this.k();
         } else if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            if (this.wasInContainer) {
               this.wasInContainer = false;
               if (this.autoDisable.getValue()) {
                  this.setEnabled(false);
               }
            }
         } else {
            int var2 = this.l();
            if (var2 > 0) {
               this.wasInContainer = true;
               if (this.timer.a((long)this.delay.getValue())) {
                  int var3 = -1;
                  int var4 = -1;

                  for (int var5 = 0; var5 < var2; var5++) {
                     Slot var6 = mc.player.currentScreenHandler.getSlot(var5);
                     if (var6 != null && var6.hasStack() && !var6.getStack().isEmpty()) {
                        ItemStack var7 = var6.getStack();
                        if (this.e(var7)) {
                           int var8 = this.a(var7);
                           if (var8 > var4) {
                              var4 = var8;
                              var3 = var5;
                           }
                        }
                     }
                  }

                  if (var3 != -1) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var3, 0, SlotActionType.QUICK_MOVE, mc.player);
                     this.timer.a();
                  }
               }
            }
         }
      }
   }

   private void k() {
      if (this.autoOpen.getValue() && mc.player != null && mc.world != null && mc.interactionManager != null && this.openTimer.a(300L)) {
         BlockPos var1 = null;
         double var2 = 25.0;
         if (mc.crosshairTarget instanceof BlockHitResult var4) {
            BlockPos var14 = var4.getBlockPos();
            if (this.a(mc.world.getBlockState(var14).getBlock())) {
               var1 = var14;
               var2 = 0.0;
            }
         }

         if (var1 == null) {
            BlockPos var12 = mc.player.getBlockPos();
            byte var15 = 4;

            for (int var6 = -var15; var6 <= var15; var6++) {
               for (int var7 = -var15; var7 <= var15; var7++) {
                  for (int var8 = -var15; var8 <= var15; var8++) {
                     BlockPos var9 = var12.add(var6, var7, var8);
                     if (this.a(mc.world.getBlockState(var9).getBlock())) {
                        double var10 = mc.player.squaredDistanceTo(var9.getX() + 0.5, var9.getY() + 0.5, var9.getZ() + 0.5);
                        if (var10 < var2) {
                           var2 = var10;
                           var1 = var9;
                        }
                     }
                  }
               }
            }
         }

         if (var1 != null) {
            BlockHitResult var13 = new BlockHitResult(Vec3d.ofCenter(var1), Direction.UP, var1, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, var13);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.openTimer.a();
         }
      }
   }

   private boolean a(Block var1) {
      return var1 instanceof ChestBlock
         || var1 instanceof EnderChestBlock
         || var1 instanceof TrappedChestBlock
         || var1 instanceof BarrelBlock
         || var1 instanceof ShulkerBoxBlock
         || var1 instanceof DispenserBlock
         || var1 instanceof DropperBlock
         || var1 instanceof HopperBlock;
   }

   private int a(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return -1;
      } else if (!this.holyWorldPriority.getValue()) {
         return 0;
      } else if (var1.isOf(Items.TRIPWIRE_HOOK)) {
         return 2;
      } else {
         return !var1.isOf(Items.TOTEM_OF_UNDYING)
               && !var1.isOf(Items.PLAYER_HEAD)
               && !var1.isOf(Items.PRISMARINE_SHARD)
               && !var1.isOf(Items.NETHER_STAR)
               && !var1.isOf(Items.NETHERITE_INGOT)
               && !var1.isOf(Items.NETHERITE_BLOCK)
               && !this.b(var1)
               && !var1.isOf(Items.TRIDENT)
               && !var1.isOf(Items.TNT)
               && !var1.isOf(Items.ENCHANTED_GOLDEN_APPLE)
               && !var1.isOf(Items.SPAWNER)
               && !var1.isOf(Items.ELYTRA)
               && !var1.isOf(Items.EXPERIENCE_BOTTLE)
               && !var1.isOf(Items.SNOWBALL)
               && !this.c(var1)
               && !this.d(var1)
            ? 0
            : 1;
      }
   }

   private boolean b(ItemStack var1) {
      return var1.isOf(Items.NETHERITE_HELMET)
         || var1.isOf(Items.NETHERITE_CHESTPLATE)
         || var1.isOf(Items.NETHERITE_LEGGINGS)
         || var1.isOf(Items.NETHERITE_BOOTS)
         || var1.isOf(Items.NETHERITE_SWORD)
         || var1.isOf(Items.NETHERITE_PICKAXE)
         || var1.isOf(Items.NETHERITE_AXE)
         || var1.isOf(Items.NETHERITE_SHOVEL)
         || var1.isOf(Items.NETHERITE_HOE);
   }

   private boolean c(ItemStack var1) {
      if (!var1.isOf(Items.ANCIENT_DEBRIS)) {
         return false;
      }

      String var2 = var1.getName().getString().toLowerCase(Locale.ROOT);
      return var2.contains("уникальный приват");
   }

   private boolean d(ItemStack var1) {
      if (!var1.isOf(Items.POTION) && !var1.isOf(Items.SPLASH_POTION) && !var1.isOf(Items.LINGERING_POTION)) {
         return false;
      }

      String var2 = var1.getName().getString().toLowerCase(Locale.ROOT);
      return var2.contains("победителя");
   }

   private int l() {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler var1) {
            if (mc.currentScreen instanceof GenericContainerScreen var4) {
               String var5 = var4.getTitle() != null ? var4.getTitle().getString().toLowerCase(Locale.ROOT) : "";
               if (var5.contains("аукцион")
                  || var5.contains("аукционы")
                  || var5.contains("поиск")
                  || var5.contains("auction")
                  || var5.equals("ah")
                  || var5.startsWith("ah ")) {
                  return -1;
               }
            }

            return var1.getRows() * 9;
         } else if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            return 27;
         } else if (mc.player.currentScreenHandler instanceof Generic3x3ContainerScreenHandler) {
            return 9;
         } else {
            return mc.player.currentScreenHandler instanceof HopperScreenHandler ? 5 : -1;
         }
      } else {
         return -1;
      }
   }

   private boolean e(ItemStack var1) {
      if (mc.player == null) {
         return false;
      }

      for (int var2 = 0; var2 < 36; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.isEmpty()) {
            return true;
         }

         if (ItemStack.areItemsAndComponentsEqual(var3, var1) && var3.getCount() < var3.getMaxCount()) {
            return true;
         }
      }

      return false;
   }
}
