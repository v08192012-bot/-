package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class EventEntityCollision extends Event {
   private final Block block;
   private final BlockPos pos;

   public EventEntityCollision(Block var1, BlockPos var2) {
      this.block = var1;
      this.pos = var2;
   }

   public Block getBlock() {
      return this.block;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
