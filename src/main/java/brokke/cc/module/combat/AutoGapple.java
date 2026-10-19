package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.HotbarSlot;
import brokke.cc.core.ItemSlot;
import brokke.cc.core.OffhandSlot;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.InventoryUtility;
import brokke.cc.util.SwapHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@NewFunction(name = "AutoGapple", description = "Автоматически ест золотые яблоки", category = Category.COMBAT)
public class AutoGapple extends Function {
   private boolean eating;
   private ItemSlot swappedSlot = null;
   private ItemStack savedOffhandItem = null;
   private int savedMainHandSlot = -1;
   private final ModeSetting eatMode = new ModeSetting("Тип", "В руке", "В левой руке");
   private final ModeSetting swapMode = new ModeSetting("Режим", "HolyWorld", "Grim");
   private final NumberSetting healthEat = new NumberSetting("Здоровье", 15.0, 1.0, 20.0, 1.0);

   public AutoGapple() {
      this.addSettings(this.eatMode, this.swapMode, this.healthEat);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.interactionManager != null) {
         float var2 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         boolean var3 = var2 <= this.healthEat.getValue();
         if (this.eatMode.isSelected("В руке")) {
            this.b(var3);
         } else {
            this.c(var3);
         }
      }
   }

   private void b(boolean var1) {
      if (var1 && !mc.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) {
         int var2 = this.m();
         if (var2 == -1) {
            this.l();
         } else {
            if (this.savedMainHandSlot == -1) {
               this.savedMainHandSlot = mc.player.getInventory().getSelectedSlot();
            }

            if (mc.player.getInventory().getSelectedSlot() != var2) {
               InventoryUtility.e(var2);
            }

            this.n();
         }
      } else {
         this.l();
      }
   }

   private void l() {
      this.o();
      if (this.savedMainHandSlot != -1) {
         int var1 = this.savedMainHandSlot;
         this.savedMainHandSlot = -1;
         if (mc.player != null && mc.player.getInventory().getSelectedSlot() != var1) {
            InventoryUtility.e(var1);
         }
      }
   }

   private void c(boolean var1) {
      if (!SwapHelper.b()) {
         AutoTotem var2 = FunctionManager.a(AutoTotem.class);
         if (var2 != null && var2.isActive() && var2.m()) {
            this.o();
         } else {
            OffhandSlot var3 = InventoryUtility.e();
            ItemStack var4 = var3.a();
            if (!var1 || mc.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) {
               this.o();
               if (this.savedOffhandItem != null && this.swappedSlot != null) {
                  ItemSlot var7 = this.swappedSlot;
                  this.savedOffhandItem = null;
                  this.swappedSlot = null;
                  SwapHelper.b var8 = this.swapMode.isSelected("HolyWorld") ? SwapHelper.b.HOLYWORLD : SwapHelper.b.GRIM;
                  SwapHelper.a(var8, () -> InventoryUtility.a(var7));
               }
            } else if (this.a(var4)) {
               this.n();
            } else {
               ItemSlot var5 = this.p();
               if (var5 != null) {
                  SwapHelper.b var6 = this.swapMode.isSelected("HolyWorld") ? SwapHelper.b.HOLYWORLD : SwapHelper.b.GRIM;
                  if (var3.e()) {
                     SwapHelper.a(var6, () -> {
                        InventoryUtility.a(var5);
                        this.n();
                     });
                  } else if (this.savedOffhandItem == null) {
                     this.savedOffhandItem = var3.a().copy();
                     this.swappedSlot = var5;
                     SwapHelper.a(var6, () -> {
                        InventoryUtility.a(var5);
                        this.n();
                     });
                  }
               }
            }
         }
      }
   }

   private int m() {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (this.a(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private void n() {
      this.eating = true;
      mc.options.useKey.setPressed(true);
   }

   private void o() {
      if (this.eating) {
         mc.options.useKey.setPressed(false);
         this.eating = false;
      }
   }

   public boolean isEating() {
      return this.eating;
   }

   private boolean a(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         Item var2 = var1.getItem();
         return var2 == Items.GOLDEN_APPLE || var2 == Items.ENCHANTED_GOLDEN_APPLE;
      } else {
         return false;
      }
   }

   private ItemSlot p() {
      for (int var1 = 0; var1 < 9; var1++) {
         HotbarSlot var2 = InventoryUtility.a(var1);
         if (var2.a(Items.GOLDEN_APPLE) || var2.a(Items.ENCHANTED_GOLDEN_APPLE)) {
            return var2;
         }
      }

      return null;
   }

   @Override
   public void onDisable() {
      this.l();
      if (this.savedOffhandItem != null && this.swappedSlot != null) {
         InventoryUtility.a(this.swappedSlot);
         this.savedOffhandItem = null;
         this.swappedSlot = null;
      }

      super.onDisable();
   }
}
