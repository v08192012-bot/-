package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.InventoryUtility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@NewFunction(name = "AutoEat", description = "Автоматически ест еду при голоде", category = Category.PLAYER)
public class AutoEat extends Function {
   private static AutoEat instance;
   public final NumberSetting hunger = new NumberSetting("Голод", 16.0, 1.0, 19.0, 1.0);
   public final BooleanSetting gapples = new BooleanSetting("Золотые яблоки", false);
   public final BooleanSetting badFood = new BooleanSetting("Опасная еда", false);
   private boolean eating = false;
   private int originalSlot = -1;
   private int swappedInventorySlot = -1;
   private int hotbarTargetSlot = -1;
   private int eatTicks = 0;

   public AutoEat() {
      instance = this;
      this.addSettings(this.hunger, this.gapples, this.badFood);
   }

   public static AutoEat getInstance() {
      return instance;
   }

   public static boolean l() {
      return instance != null && instance.isActive() && instance.eating;
   }

   public boolean isEating() {
      return this.eating;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.o();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && this.isActive() && mc.currentScreen == null) {
         int var2 = mc.player.getHungerManager().getFoodLevel();
         if (this.eating) {
            this.b(var2);
         } else if (var2 <= (int)this.hunger.getValue() && mc.player.canConsume(false)) {
            this.n();
         }
      }
   }

   private void n() {
      int var1 = this.q();
      if (var1 != -1) {
         this.originalSlot = mc.player.getInventory().getSelectedSlot();
         if (var1 < 9) {
            this.hotbarTargetSlot = var1;
            this.swappedInventorySlot = -1;
            if (mc.player.getInventory().getSelectedSlot() != var1) {
               InventoryUtility.e(var1);
            }
         } else {
            int var2 = this.p();
            if (var2 != -1) {
               this.hotbarTargetSlot = var2;
            } else {
               this.hotbarTargetSlot = this.originalSlot;
            }

            this.swappedInventorySlot = var1;
            InventoryUtility.e(this.hotbarTargetSlot);
            if (mc.interactionManager != null && mc.player.currentScreenHandler != null) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var1, this.hotbarTargetSlot, SlotActionType.SWAP, mc.player);
            }
         }

         this.eating = true;
         this.eatTicks = 0;
         if (mc.options != null) {
            mc.options.useKey.setPressed(true);
         }

         if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         }
      }
   }

   private void b(int var1) {
      this.eatTicks++;
      ItemStack var2 = mc.player.getMainHandStack();
      if (!this.b(var2)) {
         this.o();
      } else if (var1 < 20 && (var1 <= (int)this.hunger.getValue() || mc.player.canConsume(false))) {
         if (this.eatTicks > 8 && !mc.player.isUsingItem()) {
            this.o();
         } else if (this.eatTicks > 45) {
            this.o();
         } else {
            if (mc.options != null) {
               mc.options.useKey.setPressed(true);
            }

            if (!mc.player.isUsingItem() && mc.interactionManager != null) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         }
      } else {
         this.o();
      }
   }

   private void o() {
      if (this.eating) {
         if (mc.options != null) {
            mc.options.useKey.setPressed(false);
         }

         if (this.swappedInventorySlot != -1 && this.hotbarTargetSlot != -1) {
            if (mc.interactionManager != null && mc.player != null && mc.player.currentScreenHandler != null) {
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, this.swappedInventorySlot, this.hotbarTargetSlot, SlotActionType.SWAP, mc.player);
            }

            this.swappedInventorySlot = -1;
         }

         if (this.originalSlot != -1 && mc.player != null) {
            if (mc.player.getInventory().getSelectedSlot() != this.originalSlot) {
               InventoryUtility.e(this.originalSlot);
            }

            this.originalSlot = -1;
         }

         this.eating = false;
         this.eatTicks = 0;
         this.hotbarTargetSlot = -1;
      }
   }

   private int p() {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getStack(var1).isEmpty()) {
            return var1;
         }
      }

      return -1;
   }

   private int q() {
      if (mc.player == null) {
         return -1;
      }

      int var1 = -1;
      int var2 = -1;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = mc.player.getInventory().getStack(var3);
         int var5 = this.a(var4);
         if (var5 > var2) {
            var2 = var5;
            var1 = var3;
         }
      }

      if (var1 != -1) {
         return var1;
      }

      for (int var6 = 9; var6 < 36; var6++) {
         ItemStack var7 = mc.player.getInventory().getStack(var6);
         int var8 = this.a(var7);
         if (var8 > var2) {
            var2 = var8;
            var1 = var6;
         }
      }

      return var1;
   }

   private int a(ItemStack var1) {
      if (!this.b(var1)) {
         return -1;
      }

      FoodComponent var2 = var1.get(DataComponentTypes.FOOD);
      if (var2 == null) {
         return -1;
      }

      int var3 = var2.nutrition() * 10 + (int)(var2.saturation() * 10.0F);
      if (var1.isOf(Items.GOLDEN_CARROT)) {
         var3 += 500;
      } else if (var1.isOf(Items.COOKED_BEEF)) {
         var3 += 400;
      } else if (var1.isOf(Items.COOKED_PORKCHOP)) {
         var3 += 390;
      } else if (var1.isOf(Items.COOKED_MUTTON)) {
         var3 += 300;
      } else if (var1.isOf(Items.COOKED_SALMON)) {
         var3 += 280;
      } else if (var1.isOf(Items.BREAD)) {
         var3 += 250;
      } else if (var1.isOf(Items.BAKED_POTATO)) {
         var3 += 240;
      } else if (var1.isOf(Items.COOKED_CHICKEN)) {
         var3 += 230;
      }

      return var3;
   }

   private boolean b(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         FoodComponent var2 = var1.get(DataComponentTypes.FOOD);
         if (var2 == null) {
            return false;
         } else {
            return this.gapples.getValue() || !var1.isOf(Items.GOLDEN_APPLE) && !var1.isOf(Items.ENCHANTED_GOLDEN_APPLE)
               ? this.badFood.getValue()
                  || !var1.isOf(Items.ROTTEN_FLESH)
                     && !var1.isOf(Items.SPIDER_EYE)
                     && !var1.isOf(Items.PUFFERFISH)
                     && !var1.isOf(Items.POISONOUS_POTATO)
                     && !var1.isOf(Items.CHORUS_FRUIT)
               : false;
         }
      } else {
         return false;
      }
   }
}
