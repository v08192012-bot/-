package brokke.cc.core;

import brokke.cc.mixins.interfaces.IClientPlayerInteractionManager;
import brokke.cc.rotation.Rotation2;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.DeltaInventoryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class UseableHandler extends BaseHandler {
   private final List<UseableHandler.a> tasks = new ArrayList<>();

   public List<UseableHandler.a> getTasks() {
      return this.tasks;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.interactionManager != null && !this.tasks.isEmpty()) {
         UseableHandler.a var2 = this.tasks.getFirst();
         int var3 = var2.getItemStack().getItem() == Items.SPLASH_POTION
            ? DeltaInventoryUtil.a(true)
            : DeltaInventoryUtil.a(var2.getItemStack().getItem(), true);
         int var4 = var2.getItemStack().getItem() == Items.SPLASH_POTION
            ? DeltaInventoryUtil.a(false)
            : DeltaInventoryUtil.a(var2.getItemStack().getItem(), false);
         if (var2.getTicks() == -1 && var3 == -1 && var4 == -1) {
            this.tasks.remove(var2);
            return;
         }

         var2.setTicks(var2.getTicks() + 1);
         if (var2.getTicks() == 0) {
            var2.setSelectedSlot(mc.player.getInventory().getSelectedSlot());
            if (var3 != -1) {
               var2.setFromInventory(false);
               var2.setItemSlot(var3);
               if (var3 != mc.player.getInventory().getSelectedSlot()) {
                  this.a(var3);
                  return;
               }

               return;
            }

            if (var4 != -1) {
               var2.setFromInventory(true);
               int var5 = DeltaInventoryUtil.a(mc.player.getInventory().getStack(var4), var2.getItemStack());
               if (var5 != -1) {
                  mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(var4 < 9 ? 36 + var4 : var4, var5));
               }

               var2.setItemSlot(var5 != -1 && mc.player.getMainHandStack().isEmpty() ? var2.getItemSlot() : var4);
               HandlerProcessor.a().getInventoryHandler().a(var4, mc.player.getInventory().getSelectedSlot(), 1);
               return;
            }

            return;
         }

         if (!var2.isFromInventory()) {
            if (var2.getTicks() == 1) {
               this.a(var2);
               if (var2.getSelectedSlot() != mc.player.getInventory().getSelectedSlot()) {
                  this.a(var2.getSelectedSlot());
               }

               this.tasks.remove(var2);
            }
         } else if (var2.getTicks() == 3) {
            this.a(var2);
         } else if (var2.getTicks() >= 5) {
            if (mc.player.getInventory().getStack(var2.getItemSlot()).contains(DataComponentTypes.BUNDLE_CONTENTS)) {
               mc.player.getInventory().setStack(var2.getSelectedSlot(), ItemStack.EMPTY);
               HandlerProcessor.a().getInventoryHandler().a(var2.getItemSlot(), 36 + var2.getSelectedSlot(), 1);
            } else if (var2.getItemSlot() > 8) {
               HandlerProcessor.a().getInventoryHandler().a(var2.getSelectedSlot(), var2.getItemSlot(), 1);
            } else if (var2.getSelectedSlot() != mc.player.getInventory().getSelectedSlot()) {
               this.a(var2.getSelectedSlot());
            }

            this.tasks.remove(var2);
         }
      }
   }

   public void a(int var1) {
      if (mc.player != null && var1 >= 0 && var1 <= 8) {
         mc.player.getInventory().setSelectedSlot(var1);
         if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var1));
         }
      }
   }

   public void a(UseableHandler.a var1) {
      if (mc.interactionManager != null && mc.world != null && mc.player != null) {
         float var2 = var1.getRotation() != null ? var1.getRotation().getYaw() : mc.player.getYaw();
         float var3 = var1.getRotation() != null ? var1.getRotation().getPitch() : mc.player.getPitch();
         if (var1.getRotation() != null) {
            mc.player.setYaw(var2);
            mc.player.setPitch(var3);
         }

         ((IClientPlayerInteractionManager)mc.interactionManager)
            .invokeSendSequencedPacket(mc.world, var2xx -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var2xx, var2, var3));
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   public void a(ItemStack var1) {
      this.tasks.add(new UseableHandler.a(var1));
   }

   public void a(ItemStack var1, Rotation2 var2) {
      this.tasks.add(new UseableHandler.a(var1, var2));
   }

   public static final class a {
      private final ItemStack itemStack;
      private final Rotation2 rotation;
      private int selectedSlot;
      private int itemSlot;
      private int ticks = -1;
      private boolean fromInventory;

      public a(ItemStack var1) {
         this(var1, null);
      }

      public a(ItemStack var1, Rotation2 var2) {
         this.itemStack = var1;
         this.rotation = var2;
      }

      public Rotation2 getRotation() {
         return this.rotation;
      }

      public void setSelectedSlot(int var1) {
         this.selectedSlot = var1;
      }

      public void setItemSlot(int var1) {
         this.itemSlot = var1;
      }

      public void setTicks(int var1) {
         this.ticks = var1;
      }

      public void setFromInventory(boolean var1) {
         this.fromInventory = var1;
      }

      public ItemStack getItemStack() {
         return this.itemStack;
      }

      public int getSelectedSlot() {
         return this.selectedSlot;
      }

      public int getItemSlot() {
         return this.itemSlot;
      }

      public int getTicks() {
         return this.ticks;
      }

      public boolean isFromInventory() {
         return this.fromInventory;
      }
   }
}
