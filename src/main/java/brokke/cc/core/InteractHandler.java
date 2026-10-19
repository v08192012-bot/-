package brokke.cc.core;

import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Hand;

public class InteractHandler extends BaseHandler {
   private final List<InteractHandler.a> tasks = new ArrayList<>();

   public List<InteractHandler.a> getTasks() {
      return this.tasks;
   }

   public void a(int var1) {
      if (this.tasks.isEmpty() && HandlerProcessor.a().getInventoryHandler().getTasks().isEmpty()) {
         this.tasks.add(new InteractHandler.a(var1));
      }
   }

   public boolean b() {
      return !this.tasks.isEmpty();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.interactionManager != null && !this.tasks.isEmpty() && mc.player.age > 40) {
         InventoryHandler var2 = HandlerProcessor.a().getInventoryHandler();
         InteractHandler.a var3 = this.tasks.getFirst();
         boolean var4 = var3.getEatSlot() > 8;
         var3.setTicks(var3.getTicks() + 1);
         if (var3.getTicks() == 1) {
            if (var4) {
               var2.a(var3.getEatSlot(), var3.getSelectedSlot(), 2);
            } else {
               mc.player.getInventory().setSelectedSlot(var3.getEatSlot());
            }
         } else if (!var3.isReturned() && var3.getTicks() > 0 && var2.getTasks().isEmpty()) {
            if (mc.player.isUsingItem()) {
               var3.setReturned(true);
            } else {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         } else if (var3.isReturned() && !mc.player.isUsingItem() && var2.getTasks().isEmpty()) {
            if (var4) {
               var2.a(var3.getSelectedSlot(), var3.getEatSlot(), 2);
            } else {
               mc.player.getInventory().setSelectedSlot(var3.getSelectedSlot());
            }

            this.tasks.remove(var3);
         }

         if (var3.getTicks() >= 60) {
            this.tasks.remove(var3);
         }
      }
   }

   public static final class a {
      private final int selectedSlot = BaseHandler.mc.player != null ? BaseHandler.mc.player.getInventory().getSelectedSlot() : 0;
      private final int eatSlot;
      private boolean returned;
      private int ticks;

      public a(int var1) {
         this.eatSlot = var1;
      }

      public void setReturned(boolean var1) {
         this.returned = var1;
      }

      public void setTicks(int var1) {
         this.ticks = var1;
      }

      public int getSelectedSlot() {
         return this.selectedSlot;
      }

      public int getEatSlot() {
         return this.eatSlot;
      }

      public boolean isReturned() {
         return this.returned;
      }

      public int getTicks() {
         return this.ticks;
      }
   }
}
