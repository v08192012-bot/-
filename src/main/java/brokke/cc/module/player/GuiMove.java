package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.Timer2;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.SendPacketEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.ui.ClickGui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "GuiMove", description = "Позволяет двигаться в открытых меню", category = Category.PLAYER)
public class GuiMove extends Function {
   public static boolean bypassQueue = false;
   private final List<Packet<?>> packets = new ArrayList<>();
   private final ModeSetting mode = new ModeSetting("Режим", "HolyWorld", "Grim", "Visual", "Vanilla");
   private final Timer2 timer = new Timer2();
   private int ticksDelay = 0;
   private boolean needSend = false;
   private boolean hasQueuedClicks = false;
   private boolean sending = false;

   public GuiMove() {
      this.addSettings(this.mode);
      this.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.packets.clear();
      this.ticksDelay = 0;
      this.needSend = false;
      this.hasQueuedClicks = false;
      this.sending = false;
      bypassQueue = false;
      this.n();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null) {
         if (this.ticksDelay > 0) {
            this.ticksDelay--;
         }

         if (!this.mode.isMode("HolyWorld") && !this.mode.isMode("Vanilla")) {
            if (!this.mode.isMode("Grim")) {
               if (this.mode.isMode("Visual") && !this.k()) {
                  this.m();
               }
            } else if ((this.ticksDelay <= 0 || this.timer.a(99L))
               && (mc.currentScreen == null || mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen)) {
               this.m();
               if (this.needSend && this.ticksDelay == 0) {
                  this.l();
                  this.needSend = false;
                  this.hasQueuedClicks = false;
               }
            }
         } else if (!this.o() && this.p()) {
            this.m();
         }
      }
   }

   @EventHandler
   public void onSendPacket(SendPacketEvent var1) {
      if (!this.sending && !this.mode.isMode("HolyWorld") && !this.mode.isMode("Vanilla") && !bypassQueue && this.mode.isMode("Grim")) {
         if (var1.getPacket() instanceof ClickSlotC2SPacket var2) {
            if (mc.currentScreen == null) {
               this.packets.add(var2);
               var1.cancel();
               this.hasQueuedClicks = true;
               this.ticksDelay = 2;
               this.needSend = true;
               this.timer.a();
               return;
            }

            if (mc.currentScreen instanceof InventoryScreen && this.k()) {
               this.packets.add(var2);
               var1.cancel();
               return;
            }
         }

         if (var1.getPacket() instanceof CloseHandledScreenC2SPacket && this.k()) {
            if (!this.packets.isEmpty()) {
               this.hasQueuedClicks = true;
               this.ticksDelay = 2;
               this.needSend = true;
               this.timer.a();
            }

            var1.cancel();
         }
      }
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (mc.currentScreen instanceof HandledScreen || mc.currentScreen instanceof ClickGui) {
         var1.setSneak(false);
      }

      if (this.mode.isMode("Grim") && this.ticksDelay > 0 && !this.timer.a(195L)) {
         var1.setForward(0.0F);
         var1.setStrafe(0.0F);
         var1.setJump(false);
         var1.setSneak(false);
         var1.setSprint(false);
      }
   }

   private boolean k() {
      return mc.player == null ? false : mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F;
   }

   private void l() {
      if (!this.packets.isEmpty() && mc.player != null && mc.player.networkHandler != null) {
         this.sending = true;

         for (Packet var2 : this.packets) {
            mc.player.networkHandler.sendPacket(var2);
         }

         int var3 = mc.player.currentScreenHandler != null ? mc.player.currentScreenHandler.syncId : 0;
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(var3));
         this.packets.clear();
         this.sending = false;
      }
   }

   private void m() {
      if (mc.options != null && mc.getWindow() != null && !this.o() && this.p()) {
         Window var1 = mc.getWindow();
         int var2;
         KeyBinding[] var3 = (var2 = !(mc.currentScreen instanceof HandledScreen) && !(mc.currentScreen instanceof ClickGui) ? 0 : 1) != 0
            ? new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey}
            : new KeyBinding[]{
               mc.options.forwardKey,
               mc.options.backKey,
               mc.options.leftKey,
               mc.options.rightKey,
               mc.options.jumpKey,
               mc.options.sneakKey,
               mc.options.sprintKey
            };
         if (var2 != 0) {
            mc.options.sneakKey.setPressed(false);
         }

         for (KeyBinding var7 : var3) {
            Key var8 = InputUtil.fromTranslationKey(var7.getBoundKeyTranslationKey());
            if (var8.getCategory() == Type.KEYSYM) {
               var7.setPressed(InputUtil.isKeyPressed(var1, var8.getCode()));
            } else if (var8.getCategory() == Type.MOUSE) {
               var7.setPressed(GLFW.glfwGetMouseButton(var1.getHandle(), var8.getCode()) == 1);
            }
         }
      }
   }

   private void n() {
      if (mc.options != null) {
         KeyBinding[] var1 = new KeyBinding[]{
            mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sneakKey, mc.options.sprintKey
         };

         for (KeyBinding var5 : var1) {
            var5.setPressed(false);
         }
      }
   }

   private boolean o() {
      return mc.currentScreen instanceof ClickGui var1
         ? var1.r()
         : mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen;
   }

   private boolean p() {
      return mc.currentScreen instanceof HandledScreen || mc.currentScreen instanceof ClickGui || mc.currentScreen != null;
   }
}
