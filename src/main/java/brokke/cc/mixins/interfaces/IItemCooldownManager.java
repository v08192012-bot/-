package brokke.cc.mixins.interfaces;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCooldownManager.class)
public interface IItemCooldownManager {
   @Accessor("entries")
   Map<Identifier, ?> getEntries();

   @Accessor("tick")
   int getTick();
}
