package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.EnchantmentUtility;
import brokke.cc.util.ItemUtility;
import brokke.cc.util.TimerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@NewFunction(name = "Auto Armor", description = "Автоматически надевает лучшую броню", category = Category.COMBAT)
public class AutoArmor extends Function {
   private final NumberSetting delay = new NumberSetting("Задержка", 250.0, 50.0, 1000.0, 50.0);
   private final BooleanSetting elytra = new BooleanSetting("Не снимать элитры", false);
   private final TimerUtil timer = new TimerUtil();
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

   public AutoArmor() {
      this.addSettings(this.delay, this.elytra);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.interactionManager != null) {
         PlayerInventory var2 = mc.player.getInventory();
         int[] var3 = new int[4];
         int[] var4 = new int[4];
         Arrays.fill(var3, -1);
         Arrays.fill(var4, -1);

         for (int var5 = 0; var5 < 4; var5++) {
            ItemStack var6 = mc.player.getEquippedStack(ARMOR_SLOTS[var5]);
            if (!var6.isEmpty()) {
               var4[var5] = this.c(var6);
            }
         }

         for (int var10 = 0; var10 < 36; var10++) {
            ItemStack var12 = var2.getStack(var10);
            if (!var12.isEmpty() && this.a(var12)) {
               int var7 = this.b(var12);
               if (var7 >= 0) {
                  int var8 = this.c(var12);
                  if (var8 > var4[var7]) {
                     var3[var7] = var10;
                     var4[var7] = var8;
                  }
               }
            }
         }

         ArrayList var11 = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
         Collections.shuffle(var11);

         for (int var14 : (Iterable<Integer>)(Iterable<?>)(var11)) {
            int var15 = var3[var14];
            if (var15 != -1) {
               ItemStack var9 = mc.player.getEquippedStack(ARMOR_SLOTS[var14]);
               if ((var9.isEmpty() || var2.getEmptySlot() != -1)
                  && (!this.elytra.isToggled() || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA || var14 != 2)) {
                  this.a(var15, var14);
                  break;
               }
            }
         }
      }
   }

   private boolean a(ItemStack var1) {
      return ItemUtility.i(var1) != null;
   }

   private int b(ItemStack var1) {
      EquipmentSlot var2 = ItemUtility.i(var1);
      if (var2 == EquipmentSlot.HEAD) {
         return 3;
      } else if (var2 == EquipmentSlot.CHEST) {
         return 2;
      } else if (var2 == EquipmentSlot.LEGS) {
         return 1;
      } else {
         return var2 == EquipmentSlot.FEET ? 0 : -1;
      }
   }

   private int c(ItemStack var1) {
      if (ItemUtility.h(var1)) {
         return Integer.MAX_VALUE;
      }

      int var2 = ItemUtility.j(var1);
      int var3 = ItemUtility.k(var1);
      int var4 = EnchantmentUtility.a(var1, Enchantments.PROTECTION);
      return var2 * 5 + var4 * 3 + var3;
   }

   private void a(int var1, int var2) {
      if (mc.player != null && mc.interactionManager != null) {
         if (var1 < 9) {
            var1 += 36;
         }

         if (this.timer.a((long)this.delay.getValue())) {
            ItemStack var3 = mc.player.getEquippedStack(ARMOR_SLOTS[var2]);
            if (!var3.isEmpty()) {
               mc.interactionManager.clickSlot(0, 8 - var2, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            mc.interactionManager.clickSlot(0, var1, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.timer.a();
         }
      }
   }
}
