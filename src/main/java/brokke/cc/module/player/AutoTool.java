package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Aura;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.InventoryUtility;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

@NewFunction(name = "AutoTool", description = "Автоматически выбирает лучший инструмент из хотбара", category = Category.PLAYER)
public class AutoTool extends Function {
   public final BooleanSetting invisible = new BooleanSetting("Незаметный", false);
   private int oldSlot = -1;
   private int swappedInventorySlotId = -1;
   private boolean swapped;
   private ItemStack visualStack = ItemStack.EMPTY;
   private int visualResetTicks = 0;
   private int attackHoldTicks = 0;

   public AutoTool() {
      this.addSettings(this.invisible);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.options.attackKey.isPressed()) {
         this.attackHoldTicks++;
      } else {
         this.attackHoldTicks = 0;
         if (!this.swapped) {
            if (this.visualResetTicks > 0) {
               this.visualResetTicks--;
            } else {
               this.visualStack = ItemStack.EMPTY;
            }
         }
      }

      if (mc.player != null && mc.world != null && !mc.player.isCreative()) {
         Aura var2 = FunctionManager.a(Aura.class);
         if (var2 != null && var2.isActive()) {
            this.l();
         } else if (!(mc.crosshairTarget instanceof BlockHitResult var3 && mc.options.attackKey.isPressed())) {
            this.l();
         } else if (this.attackHoldTicks >= 1) {
            if (this.swappedInventorySlotId != -1) {
               if (this.oldSlot != -1 && mc.player.getInventory().getSelectedSlot() != this.oldSlot) {
                  InventoryUtility.e(this.oldSlot);
               }

               return;
            }

            BlockPos var7 = var3.getBlockPos();
            BlockState var5 = mc.world.getBlockState(var7);
            Slot var6 = this.a(var5);
            if (var6 != null) {
               this.a(var6);
            }
         }
      } else {
         this.l();
      }
   }

   private Slot a(BlockState var1) {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         Slot var2 = null;
         float var3 = 1.0F;

         for (Slot var5 : mc.player.currentScreenHandler.slots) {
            if (this.d(var5.id)) {
               float var6 = var5.getStack().getMiningSpeedMultiplier(var1);
               if (var6 > var3) {
                  var3 = var6;
                  var2 = var5;
               }
            }
         }

         if (var2 != null) {
            return var2;
         }

         Slot var9 = null;
         float var10 = 1.0F;

         for (Slot var7 : mc.player.currentScreenHandler.slots) {
            if (this.c(var7.id) && !this.d(var7.id)) {
               float var8 = var7.getStack().getMiningSpeedMultiplier(var1);
               if (var8 > var10) {
                  var10 = var8;
                  var9 = var7;
               }
            }
         }

         return var9;
      } else {
         return null;
      }
   }

   private void a(Slot var1) {
      if (mc.player != null && var1 != null && mc.interactionManager != null) {
         int var2 = mc.player.getInventory().getSelectedSlot();
         if (this.oldSlot == -1) {
            this.oldSlot = var2;
            this.visualStack = mc.player.getInventory().getStack(var2).copy();
            this.visualResetTicks = 0;
         }

         if (this.d(var1.id)) {
            int var3 = var1.id - 36;
            if (this.swappedInventorySlotId != -1) {
               this.m();
            }

            if (mc.player.getInventory().getSelectedSlot() != var3) {
               InventoryUtility.e(var3);
            }

            this.swapped = true;
         } else if (this.swappedInventorySlotId == var1.id) {
            if (mc.player.getInventory().getSelectedSlot() != this.oldSlot) {
               InventoryUtility.e(this.oldSlot);
            }

            this.swapped = true;
         } else {
            if (this.swappedInventorySlotId != -1) {
               this.m();
            }

            InventoryUtility.e(this.oldSlot);
            mc.interactionManager
               .clickSlot(mc.player.currentScreenHandler.syncId, var1.id, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
            this.swappedInventorySlotId = var1.id;
            this.swapped = true;
         }
      }
   }

   private void l() {
      if (!this.swapped) {
         this.oldSlot = -1;
         this.swappedInventorySlotId = -1;
      } else {
         this.m();
         if (mc.player != null && this.oldSlot != -1) {
            InventoryUtility.e(this.oldSlot);
         }

         this.swapped = false;
         this.visualResetTicks = 5;
         this.oldSlot = -1;
         this.swappedInventorySlotId = -1;
      }
   }

   private void m() {
      if (mc.player != null && mc.interactionManager != null && this.swappedInventorySlotId != -1) {
         Slot var1 = this.b(this.swappedInventorySlotId);
         if (var1 != null && this.oldSlot != -1) {
            InventoryUtility.e(this.oldSlot);
            mc.interactionManager
               .clickSlot(mc.player.currentScreenHandler.syncId, var1.id, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
         }

         this.swappedInventorySlotId = -1;
      }
   }

   private Slot b(int var1) {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         return var1 >= 0 && var1 < mc.player.currentScreenHandler.slots.size() ? mc.player.currentScreenHandler.slots.get(var1) : null;
      } else {
         return null;
      }
   }

   private boolean c(int var1) {
      return var1 >= 9 && var1 <= 44;
   }

   private boolean d(int var1) {
      return var1 >= 36 && var1 <= 44;
   }

   public ItemStack getVisualStack() {
      return this.visualStack;
   }

   @Override
   public void onDisable() {
      this.l();
      super.onDisable();
   }
}
