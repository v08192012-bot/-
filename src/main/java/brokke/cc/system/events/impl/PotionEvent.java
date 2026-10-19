package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.util.math.BlockPos;

public class PotionEvent extends Event {
   private final PotionEvent.Type type;
   private final int data;
   private final BlockPos pos;

   public PotionEvent(PotionEvent.Type var1, int var2, BlockPos var3) {
      this.type = var1;
      this.data = var2;
      this.pos = var3;
   }

   public PotionEvent.Type getType() {
      return this.type;
   }

   public int getData() {
      return this.data;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public enum Type {
      PARTICLES;
   }
}
