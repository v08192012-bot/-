package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Aura;
import brokke.cc.setting.BooleanSetting;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SmithingTableBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TrappedChestBlock;

@NewFunction(name = "NoInteract", description = "Блокирует взаимодействие с блоками", category = Category.PLAYER)
public class NoInteract extends Function {
   private final BooleanSetting allBlocks = new BooleanSetting("Все блоки", false);
   private final BooleanSetting onlyAura = new BooleanSetting("Только с Аурой", false);

   public NoInteract() {
      this.addSettings(this.allBlocks, this.onlyAura);
   }

   public boolean a(Block var1) {
      if (!this.isActive()) {
         return false;
      }

      if (this.onlyAura.isToggled()) {
         Function var2 = FunctionManager.a(Aura.class);
         if (var2 == null || !var2.isActive()) {
            return false;
         }
      }

      return this.allBlocks.isToggled() ? true : this.b(var1);
   }

   private boolean b(Block var1) {
      return var1 instanceof CraftingTableBlock
         || var1 instanceof EnchantingTableBlock
         || var1 instanceof BedBlock
         || var1 instanceof ChestBlock
         || var1 instanceof EnderChestBlock
         || var1 instanceof TrappedChestBlock
         || var1 instanceof AbstractFurnaceBlock
         || var1 instanceof BarrelBlock
         || var1 instanceof ShulkerBoxBlock
         || var1 instanceof DropperBlock
         || var1 instanceof DispenserBlock
         || var1 instanceof HopperBlock
         || var1 instanceof AnvilBlock
         || var1 instanceof AbstractCauldronBlock
         || var1 instanceof AbstractSignBlock
         || var1 instanceof BellBlock
         || var1 instanceof ComposterBlock
         || var1 instanceof BrewingStandBlock
         || var1 instanceof JukeboxBlock
         || var1 instanceof CommandBlock
         || var1 instanceof BeaconBlock
         || var1 instanceof RespawnAnchorBlock
         || var1 instanceof GrindstoneBlock
         || var1 instanceof LecternBlock
         || var1 instanceof CartographyTableBlock
         || var1 instanceof LoomBlock
         || var1 instanceof SmithingTableBlock
         || var1 instanceof StonecutterBlock
         || var1 instanceof DoorBlock
         || var1 instanceof TrapdoorBlock
         || var1 instanceof FenceGateBlock
         || var1 instanceof ButtonBlock
         || var1 instanceof LeverBlock
         || var1 instanceof NoteBlock;
   }
}
