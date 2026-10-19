package brokke.cc.mixins.interfaces;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleOption.class)
public interface ISimpleOption<T> {
   @Accessor("value")
   void setValueRaw(T var1);

   @Accessor("value")
   T getValueRaw();
}
