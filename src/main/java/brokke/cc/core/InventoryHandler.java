package brokke.cc.core;

import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.DeltaInventoryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryHandler extends BaseHandler {
   private final List<InventoryHandler.a> tasks = new ArrayList<>();

   public List<InventoryHandler.a> getTasks() {
      return this.tasks;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.interactionManager != null && !this.tasks.isEmpty()) {
         InventoryHandler.a var2 = this.tasks.getFirst();
         StopHandler var3 = HandlerProcessor.a().getStopHandler();
         if (var3.getTicks() < var2.getBypass()) {
            int var4 = this.a(var2.getFromSlot());
            int var5 = var2.isArmorMove() ? var2.getToSlot() : this.a(var2.getToSlot());
            if (mc.player.playerScreenHandler.getSlot(var4).getStack().contains(DataComponentTypes.BUNDLE_CONTENTS)) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var4, 1, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
               if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var4, 0, SlotActionType.PICKUP, mc.player);
               }
            } else {
               int var6 = this.a(var2.getToSlot(), var5);
               if (var6 != -1) {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var4, var6, SlotActionType.SWAP, mc.player);
               } else {
                  int var7 = this.a(var2.getFromSlot(), var4);
                  if (var7 != -1) {
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var5, var7, SlotActionType.SWAP, mc.player);
                  } else if (var4 != var5) {
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var4, 0, SlotActionType.SWAP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var5, 0, SlotActionType.SWAP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, var4, 0, SlotActionType.SWAP, mc.player);
                  }
               }
            }

            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            this.tasks.remove(var2);
            if (!this.tasks.isEmpty()) {
               var3.setTicks(this.tasks.getFirst().getBypass());
            }
         }
      }
   }

   public void a(int var1, int var2, int var3) {
      this.a(new InventoryHandler.a(var1, var2, var3, false));
   }

   public void b(int var1, int var2, int var3) {
      this.a(new InventoryHandler.a(var1, 5 + var2, var3, true));
   }

   public void a(Item var1, int var2, int var3) {
      int var4 = DeltaInventoryUtil.b(var1);
      if (var4 != -1) {
         this.a(new InventoryHandler.a(var4, var2, var3, false));
      }
   }

   public void a(ItemStack var1, int var2, int var3) {
      int var4 = DeltaInventoryUtil.a(var1, false);
      if (var4 != -1) {
         this.a(new InventoryHandler.a(var4, var2, var3, false));
      }
   }

   private void a(InventoryHandler.a var1) {
      if (var1.getFromSlot() != -1 && var1.getToSlot() != -1) {
         if (this.tasks.isEmpty() && var1.getBypass() > 0) {
            HandlerProcessor.a().getStopHandler().setTicks(var1.getBypass());
         }

         this.tasks.add(var1);
      }
   }

   private int a(int var1) {
      return var1 >= 0 && var1 <= 8 ? var1 + 36 : var1;
   }

   private int a(int var1, int var2) {
      if (var1 != 40 && var1 != 45 && var2 != 45) {
         return var2 >= 36 && var2 <= 44 ? var2 - 36 : -1;
      } else {
         return 40;
      }
   }

   public record a(int fromSlot, int toSlot, int bypass, boolean armorMove) {
      public int getFromSlot() {
         return this.fromSlot;
      }

      public int getToSlot() {
         return this.toSlot;
      }

      public int getBypass() {
         return this.bypass;
      }

      public boolean isArmorMove() {
         return this.armorMove;
      }
   }
}
