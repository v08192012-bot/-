package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.DeltaInventoryUtil;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;

@NewFunction(name = "Elytra Helper", description = "Помощник с элитрами", category = Category.PLAYER)
public class ElytraUtils extends Function {
   private final BindSetting swapKey = new BindSetting("Свап на элитры", -1);
   private final BindSetting fireworkKey = new BindSetting("Фейерверк", -1);
   private final BooleanSetting startSetting = new BooleanSetting("Быстрый старт", false);
   private final BooleanSetting sprintSetting = new BooleanSetting("Спринт", false);
   private final BooleanSetting onlyHotBar = new BooleanSetting("Только хотбар", false);
   private final BooleanSetting autoFirework = new BooleanSetting("Авто-фейерверк", false);
   private final NumberSetting autoFireworkDelay = new NumberSetting("Задержка", 250.0, 100.0, 1000.0, 25.0);
   private long lastFireworkTime = 0L;
   private boolean prevJump = false;
   private static final List<Item> CHESTPLATE_TIERS = List.of(
      Items.NETHERITE_CHESTPLATE,
      Items.DIAMOND_CHESTPLATE,
      Items.CHAINMAIL_CHESTPLATE,
      Items.IRON_CHESTPLATE,
      Items.GOLDEN_CHESTPLATE,
      Items.LEATHER_CHESTPLATE
   );

   public ElytraUtils() {
      this.addSettings(this.swapKey, this.fireworkKey, this.startSetting, this.sprintSetting, this.onlyHotBar, this.autoFirework, this.autoFireworkDelay);
      this.startSetting.onChanged(this::k);
      this.autoFirework.onChanged(this::k);
      this.k();
   }

   private void k() {
      this.sprintSetting.setVisible(this.startSetting.isToggled());
      this.autoFireworkDelay.setVisible(this.autoFirework.isToggled());
   }

   @Override
   public void onDisable() {
      this.lastFireworkTime = 0L;
      this.prevJump = false;
      super.onDisable();
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (var1.getAction() == 1 && mc.currentScreen == null && mc.player != null) {
         if (this.swapKey.getKey() != -1 && this.swapKey.getKey() == var1.getKey()) {
            this.l();
         } else if (this.fireworkKey.getKey() != -1 && this.fireworkKey.getKey() == var1.getKey() && mc.player.isGliding()) {
            this.n();
         }
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (mc.player != null && mc.player.isAlive() && mc.player.currentScreenHandler instanceof PlayerScreenHandler && this.startSetting.isToggled()) {
         ItemStack var2 = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         if (var2.isOf(Items.ELYTRA)) {
            if (mc.player.isOnGround()) {
               var1.setJump(true);
               this.prevJump = true;
            } else if (!mc.player.isGliding()) {
               if (this.sprintSetting.isToggled()
                  && (mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.getAbilities().allowFlying)
                  && !mc.player.horizontalCollision
                  && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                  && !mc.player.isUsingItem()
                  && !mc.player.isSubmergedInWater()) {
                  mc.player.setSprinting(true);
               }

               var1.setJump(!this.prevJump);
               this.prevJump = var1.isJump();
            } else if (mc.player.isGliding() && mc.player.horizontalCollision) {
               var1.setForward(0.0F);
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      long var2;
      if (mc.player != null
         && mc.world != null
         && mc.player.currentScreenHandler instanceof PlayerScreenHandler
         && this.autoFirework.isToggled()
         && mc.player.isGliding()
         && (var2 = System.currentTimeMillis()) - this.lastFireworkTime >= (long)this.autoFireworkDelay.getValue()) {
         this.n();
         this.lastFireworkTime = var2;
      }
   }

   private void l() {
      if (mc.player != null && mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
         int var2;
         if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            var2 = this.onlyHotBar.isToggled() ? this.m() : DeltaInventoryUtil.a();
         } else {
            var2 = this.onlyHotBar.isToggled() ? DeltaInventoryUtil.a(Items.ELYTRA, true) : DeltaInventoryUtil.b(Items.ELYTRA);
         }

         if (var2 != -1) {
            HandlerProcessor.a().getInventoryHandler().b(var2, 1, 1);
         }
      }
   }

   private int m() {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (a(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private void n() {
      if (mc.player != null && mc.player.isGliding()) {
         HandlerProcessor.a().getUseableHandler().a(Items.FIREWORK_ROCKET.getDefaultStack());
      }
   }

   private static boolean a(ItemStack var0) {
      if (var0 == null || var0.isEmpty() || var0.isOf(Items.ELYTRA)) {
         return false;
      }

      if (var0.get(DataComponentTypes.GLIDER) != null) {
         return false;
      }

      EquippableComponent var1 = var0.get(DataComponentTypes.EQUIPPABLE);
      return var1 != null ? var1.slot() == EquipmentSlot.CHEST : CHESTPLATE_TIERS.contains(var0.getItem());
   }
}
