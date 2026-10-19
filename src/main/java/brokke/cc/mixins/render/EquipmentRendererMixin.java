package brokke.cc.mixins.render;

import brokke.cc.module.render.ArmorDurability;
import brokke.cc.module.FunctionManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {
   @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;getDyeColor(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$Layer;I)I")
   )
   private int tintArmorDurability(int var1, @Local(argsOnly = true) ItemStack var2) {
      ArmorDurability var3 = FunctionManager.a(ArmorDurability.class);
      return var3 != null && var3.isActive() && var2 != null && var2.isDamageable() ? var3.a(var2) : var1;
   }
}
