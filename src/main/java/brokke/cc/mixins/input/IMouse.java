package brokke.cc.mixins.input;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface IMouse {
   @Accessor("cursorDeltaX")
   double getCursorDeltaX();

   @Accessor("cursorDeltaX")
   void setCursorDeltaX(double var1);

   @Accessor("cursorDeltaY")
   double getCursorDeltaY();

   @Accessor("cursorDeltaY")
   void setCursorDeltaY(double var1);
}
