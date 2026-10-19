package brokke.cc.core;

import brokke.cc.config.AutoBuyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoPay {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private int timer = 0;
   private boolean waiting = false;
   private int waitTicks = 0;
   private boolean started = false;

   public void a() {
      if (this.mc.player != null) {
         AutoBuyConfig var1 = AutoBuyConfig.getInstance();
         if (!var1.autoPay) {
            this.started = false;
            this.waiting = false;
         } else {
            if (!this.started) {
               this.started = true;
               this.timer = this.a(var1);
            }

            if (this.waiting) {
               this.waitTicks--;
               if (this.waitTicks <= 0) {
                  this.waiting = false;
                  this.timer = this.a(var1);
               }
            } else if (this.timer > 0) {
               this.timer--;
            } else {
               this.mc.player.networkHandler.sendChatCommand("money");
               this.waiting = true;
               this.waitTicks = 100;
            }
         }
      }
   }

   public void a(long var1) {
      if (this.waiting) {
         this.waiting = false;
         AutoBuyConfig var3 = AutoBuyConfig.getInstance();
         this.timer = this.a(var3);
         if (var3.autoPayNick != null && !var3.autoPayNick.isBlank() && var1 > var3.autoPayKeepBalance) {
            long var4 = var1 - var3.autoPayKeepBalance;
            this.mc.player.networkHandler.sendChatCommand("pay " + var3.autoPayNick.trim() + " " + var4);
            if (this.mc.player != null) {
               this.mc.player.sendMessage(Text.literal("§a[AutoPay] Переведено §f" + var4 + "§a игроку §f" + var3.autoPayNick.trim()), false);
            }
         }
      }
   }

   private int a(AutoBuyConfig var1) {
      return Math.max(60, var1.autoPayIntervalSeconds * 20);
   }
}
