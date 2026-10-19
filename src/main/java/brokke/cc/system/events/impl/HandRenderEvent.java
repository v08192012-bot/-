package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class HandRenderEvent extends Event {
   private final Arm arm;
   private final float swingProgress;
   private final ItemStack itemStack;
   private final float equipProgress;
   private final MatrixStack matrices;
   private final Hand hand;

   public HandRenderEvent(Arm var1, float var2, ItemStack var3, float var4, MatrixStack var5, Hand var6) {
      this.arm = var1;
      this.swingProgress = var2;
      this.itemStack = var3;
      this.equipProgress = var4;
      this.matrices = var5;
      this.hand = var6;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }

   public Arm getArm() {
      return this.arm;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public Hand getHand() {
      return this.hand;
   }
}
