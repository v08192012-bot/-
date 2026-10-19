package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.core.Timer2;
import brokke.cc.manager.TaskScheduler;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.ItemUtility;
import java.util.Comparator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "Auto Totem", description = "Автоматически берет тотем в левую руку", category = Category.COMBAT)
public class AutoTotem extends Function {
   private final NumberSetting health = new NumberSetting("Здоровье", 6.0, 1.0, 20.0, 0.5);
   private final NumberSetting healthElytra = new NumberSetting("Здоровье на элитре", 6.0, 1.0, 20.0, 0.5);
   private final BooleanSetting stopUsing = new BooleanSetting("Останавливать использование", true);
   private final BooleanSetting crystal = new BooleanSetting("Кристаллы", true);
   private final NumberSetting crystalRange = new NumberSetting("Дистанция кристаллов", 19.0, 1.0, 80.0, 1.0).visibleWhen(() -> this.crystal.isToggled());
   private final BooleanSetting anchor = new BooleanSetting("Якори", true);
   private final NumberSetting anchorRange = new NumberSetting("Дистанция якорей", 6.0, 1.0, 20.0, 0.5).visibleWhen(() -> this.anchor.isToggled());
   private final BooleanSetting tnt = new BooleanSetting("Динамит", true);
   private final NumberSetting tntRange = new NumberSetting("Дистанция ТНТ", 19.0, 1.0, 80.0, 1.0).visibleWhen(() -> this.tnt.isToggled());
   private final BooleanSetting trident = new BooleanSetting("Трезубец", true);
   private final BooleanSetting fall = new BooleanSetting("Падение", true);
   private final BooleanSetting hud = new BooleanSetting("Счетчик тотемов", true);
   private static float posX = -1.0F;
   private static float posY = -1.0F;
   private static float targetX = -1.0F;
   private static float targetY = -1.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private int savedSlot = -1;
   private final Timer2 swapTimer = new Timer2();
   private final Timer2 returnTimer = new Timer2();
   private int totemCount = 0;

   public static float[] k() {
      return new float[]{posX, posY};
   }

   public static void a(float var0, float var1) {
      posX = var0;
      posY = var1;
      targetX = var0;
      targetY = var1;
   }

   public AutoTotem() {
      this.addSettings(
         this.health,
         this.healthElytra,
         this.stopUsing,
         this.crystal,
         this.crystalRange,
         this.anchor,
         this.anchorRange,
         this.tnt,
         this.tntRange,
         this.trident,
         this.fall,
         this.hud
      );
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (this.savedSlot != -1 && mc.player != null && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         int var1 = this.savedSlot;
         this.savedSlot = -1;
         TaskScheduler.a(AutoTotem.class, () -> TaskScheduler.a(var1), 100);
      }

      this.savedSlot = -1;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && !(mc.player.getMaxHealth() <= 2.0F)) {
         this.r();
         boolean var2 = this.n();
         boolean var3 = this.q();
         ItemStack var4 = mc.player.getOffHandStack();
         boolean var5 = var4.isOf(Items.TOTEM_OF_UNDYING);
         if (var2 && (!var3 || var5)) {
            this.returnTimer.a();
            Slot var10 = this.o();
            int var7 = var5 ? ItemUtility.f(var4) : -1;
            int var8 = var10 != null ? ItemUtility.f(var10.getStack()) : -1;
            if ((!var5 || var10 != null && var7 > var8)
               && this.totemCount > 0
               && this.p()
               && TaskScheduler.a(AutoTotem.class)
               && this.swapTimer.a(200L)
               && var10 != null) {
               if (this.savedSlot == -1) {
                  this.savedSlot = var10.id;
               }

               TaskScheduler.a(AutoTotem.class, () -> TaskScheduler.a(var10.id), 100);
               this.swapTimer.a();
            }
         } else if (this.savedSlot != -1 && this.returnTimer.a(350L) && TaskScheduler.a(AutoTotem.class) && this.p()) {
            if (var5) {
               int var6 = this.savedSlot;
               this.savedSlot = -1;
               TaskScheduler.a(AutoTotem.class, () -> TaskScheduler.a(var6), 100);
               this.swapTimer.a();
            } else {
               this.savedSlot = -1;
            }
         }
      }
   }

   private Slot o() {
      return mc.player != null && mc.player.playerScreenHandler != null
         ? mc.player
            .playerScreenHandler
            .slots
            .stream()
            .filter(var0 -> var0.id != 45 && var0.getStack().isOf(Items.TOTEM_OF_UNDYING))
            .min(
               Comparator.<Slot, Integer>comparing(var0 -> ItemUtility.f(var0.getStack()))
                  .thenComparing(var0 -> var0.getStack().hasEnchantments())
                  .thenComparingInt(var0 -> var0.id)
            )
            .orElse(null)
         : null;
   }

   public boolean l() {
      return this.isActive() && mc.player != null ? this.n() || this.savedSlot != -1 : false;
   }

   public boolean m() {
      return this.n();
   }

   public boolean n() {
      if (mc.player != null && mc.world != null) {
         float var1 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         float var2 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
            ? (float)this.healthElytra.getValue()
            : (float)this.health.getValue();
         if (var1 <= var2) {
            return true;
         }

         if (this.crystal.isToggled()) {
            double var3 = this.crystalRange.getValue();
            if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(var3), var0 -> true).isEmpty()) {
               return true;
            }
         }

         if (this.anchor.isToggled()) {
            double var12 = this.anchorRange.getValue();
            BlockPos var5 = mc.player.getBlockPos();

            int var6;
            for (int var7 = -(var6 = (int)Math.ceil(var12)); var7 <= var6; var7++) {
               for (int var8 = -var6; var8 <= var6; var8++) {
                  for (int var9 = -var6; var9 <= var6; var9++) {
                     BlockPos var10 = var5.add(var7, var8, var9);
                     if (mc.player.squaredDistanceTo(Vec3d.ofCenter(var10)) <= var12 * var12) {
                        BlockState var11 = mc.world.getBlockState(var10);
                        if (var11.isOf(Blocks.RESPAWN_ANCHOR)
                           && (var11.getLuminance() > 0 || var11.contains(RespawnAnchorBlock.CHARGES) && var11.get(RespawnAnchorBlock.CHARGES) > 0)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         if (this.tnt.isToggled()) {
            double var13 = this.tntRange.getValue();
            if (!mc.world.getEntitiesByClass(TntEntity.class, mc.player.getBoundingBox().expand(var13), var0 -> true).isEmpty()) {
               return true;
            }
         }

         return this.fall.isToggled()
               && !mc.player.isOnGround()
               && !mc.player.isGliding()
               && !mc.player.isTouchingWater()
               && !mc.player.isClimbing()
               && !mc.player.isInLava()
               && a(mc.player, 30) >= var1
            ? true
            : this.trident.isToggled()
               && mc.world
                  .getEntitiesByClass(TridentEntity.class, mc.player.getBoundingBox().expand(5.0), var0 -> var0.isAlive() && var0.getOwner() != mc.player)
                  .stream()
                  .anyMatch(var0 -> var0.getVelocity().lengthSquared() > 0.1);
      } else {
         return false;
      }
   }

   private boolean p() {
      return this.stopUsing.isToggled() || !mc.player.isUsingItem();
   }

   private boolean q() {
      return mc.player != null && mc.player.getItemCooldownManager() != null
         ? mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack())
         : false;
   }

   private static float a(PlayerEntity var0, int var1) {
      if (var0 != null && var0.getEntityWorld() != null) {
         Vec3d var2 = var0.getVelocity();
         Box var3 = var0.getBoundingBox();
         double var4 = 0.0;

         for (int var6 = 0; var6 < var1; var6++) {
            var2 = var2.add(0.0, -0.08, 0.0).multiply(0.98, 0.98, 0.98);
            var3 = var3.offset(var2);
            if (!var0.getEntityWorld().isSpaceEmpty(var3.offset(0.0, -0.001, 0.0))) {
               break;
            }

            if (var2.y < 0.0) {
               var4 -= var2.y;
            }
         }

         if (var4 <= 3.0) {
            return 0.0F;
         }

         float var7 = MathHelper.floor((float)(var4 - 3.0));
         ItemStack var8 = var0.getEquippedStack(EquipmentSlot.FEET);
         int var9 = 0;
         ItemEnchantmentsComponent var10 = var8.get(DataComponentTypes.ENCHANTMENTS);
         if (var10 != null) {
            for (RegistryEntry var12 : var10.getEnchantments()) {
               if (var12.matchesKey(Enchantments.FEATHER_FALLING)) {
                  var9 = var10.getLevel(var12);
                  break;
               }
            }
         }

         if (var9 > 0) {
            var7 = Math.max(var7 - var7 * 0.15F * var9, 0.0F);
         }

         return var0.hasStatusEffect(StatusEffects.SLOW_FALLING) ? 0.0F : var7;
      } else {
         return 0.0F;
      }
   }

   private void r() {
      if (mc.player != null && mc.player.playerScreenHandler != null) {
         this.totemCount = (int)mc.player.playerScreenHandler.slots.stream().filter(var0 -> var0.getStack().isOf(Items.TOTEM_OF_UNDYING)).count();
      } else {
         this.totemCount = 0;
      }
   }

   private void a(float var1, float var2, float var3, float var4) {
      if (mc.currentScreen != null && mc.currentScreen instanceof ChatScreen) {
         double var5 = mc.getWindow().getScaleFactor();
         float var7 = (float)(mc.mouse.getX() / var5);
         float var8 = (float)(mc.mouse.getY() / var5);
         int var9;
         if ((var9 = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1 ? 1 : 0) != 0
            && !this.wasPressed
            && var7 >= var1
            && var7 <= var1 + var3
            && var8 >= var2
            && var8 <= var2 + var4) {
            this.dragging = true;
            this.dragDX = var7 - targetX;
            this.dragDY = var8 - targetY;
         }

         if (var9 == 0) {
            if (this.dragging) {
               HudConfig.b();
               ConfigManager.onSettingChanged();
            }

            this.dragging = false;
         }

         this.wasPressed = var9 != 0;
         if (this.dragging) {
            float[] var10 = HudConfig.b("autototem", var7 - this.dragDX, var8 - this.dragDY, var3, var4);
            targetX = var10[0];
            targetY = var10[1];
         }

         if (this.dragging || targetX >= 0.0F && (Math.abs(targetX - posX) > 0.05F || Math.abs(targetY - posY) > 0.05F)) {
            float[] var11 = HudConfig.a("autototem", posX, posY, targetX, targetY);
            posX = var11[0];
            posY = var11[1];
         }
      } else {
         this.dragging = false;
         this.wasPressed = false;
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (this.hud.isToggled() && mc.player != null) {
         boolean var2 = mc.currentScreen instanceof ChatScreen;
         if (this.totemCount > 0 || var2) {
            int var3 = mc.options.getMainArm().getValue() == Arm.LEFT ? 1 : 0;
            int var4 = mc.getWindow().getScaledWidth();
            int var5 = mc.getWindow().getScaledHeight();
            float var6 = var3 != 0 ? var4 / 2.0F - 103.0F : var4 / 2.0F + 103.0F;
            float var7 = var5 - 21.0F;
            if (posX < 0.0F || posY < 0.0F) {
               posX = var6;
               posY = var7;
               targetX = var6;
               targetY = var7;
            }

            this.a(posX - 8.0F, posY, 22.0F, 22.0F);
            int var8 = (int)posX;
            int var9 = (int)posY;
            float var10 = 2.0F / mc.getWindow().getScaleFactor();
            if (!ClickGui.a((var8 - 8.0F) * var10, (var9 + 3.0F) * var10, 16.0F * var10, 16.0F * var10)) {
               var1.getContext().drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), var8 - 8, var9 + 3);
               String var11 = String.valueOf(this.totemCount);
               int var12 = mc.textRenderer.getWidth(var11);
               var1.getContext().drawTextWithShadow(mc.textRenderer, var11, var8 - var12 / 2 + 8, var9 + 15, -1);
            }
         }
      }
   }
}
