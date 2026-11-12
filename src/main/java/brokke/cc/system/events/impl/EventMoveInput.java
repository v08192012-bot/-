package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import net.minecraft.util.PlayerInput;

public class EventMoveInput extends Event {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private PlayerInput playerInput;

   public float getForward() {
      return this.forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public void setForward(float var1) {
      this.forward = var1;
   }

   public void setStrafe(float var1) {
      this.strafe = var1;
   }

   public void setJump(boolean var1) {
      this.jump = var1;
      // Если кто-то уже собрал полный PlayerInput (TestNerv), флаг jump из этого поля
      // миксин игнорирует. Держим обе формы ввода согласованными: иначе прыжок,
      // выставленный модулем (WindHop, крит-прыжок ауры), молча терялся.
      if (this.playerInput != null) {
         PlayerInput var2 = this.playerInput;
         this.playerInput = new PlayerInput(var2.forward(), var2.backward(), var2.left(), var2.right(), var1, var2.sneak(), var2.sprint());
      }
   }

   public void setSneak(boolean var1) {
      this.sneak = var1;
   }

   public void setSprint(boolean var1) {
      this.sprint = var1;
   }

   /** Полная замена ввода (биты клавиш), как в Opus: снек всегда остаётся прежним. */
   public PlayerInput getPlayerInput() {
      return this.playerInput;
   }

   public void setPlayerInput(PlayerInput var1) {
      this.playerInput = var1;
   }

   public EventMoveInput(float var1, float var2, boolean var3, boolean var4, boolean var5) {
      this.forward = var1;
      this.strafe = var2;
      this.jump = var3;
      this.sneak = var4;
      this.sprint = var5;
   }
}
