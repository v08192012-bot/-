package brokke.cc.mixins.render;

import brokke.cc.core.IItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements IItemEntityRenderState {
   @Unique
   private boolean onGround;

   @Override
   public boolean isOnGround() {
      return this.onGround;
   }

   @Override
   public void setOnGround(boolean var1) {
      this.onGround = var1;
   }
}
