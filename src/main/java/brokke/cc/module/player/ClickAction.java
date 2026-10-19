package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.manager.FriendManager;
import brokke.cc.setting.BindSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.KeyEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

@NewFunction(name = "ClickAction", description = "Выполняет действие, привязанное к выбранной клавише", category = Category.PLAYER)
public class ClickAction extends Function {
   private final BindSetting pearlBind = new BindSetting("Эндер-жемчуг", -1);
   private final BindSetting friendBind = new BindSetting("Добавление друга", -1);

   public ClickAction() {
      this.addSettings(this.pearlBind, this.friendBind);
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && var1.getAction() == 1) {
         int var2 = var1.getKey();
         if (this.pearlBind.getKey() != -1 && var2 == this.pearlBind.getKey()) {
            HandlerProcessor.a().getUseableHandler().a(Items.ENDER_PEARL.getDefaultStack());
         }

         if (this.friendBind.getKey() != -1
            && var2 == this.friendBind.getKey()
            && mc.crosshairTarget instanceof EntityHitResult var3
            && var3.getEntity() instanceof AbstractClientPlayerEntity var6
            && var6 != mc.player) {
            String var7 = var6.getName().getString();
            if (FriendManager.a(var7)) {
               FriendManager.c(var7);
               this.a("Товарищ " + var7 + " был успешно удален из списка друзей.");
            } else {
               FriendManager.b(var7);
               this.a("Товарищ " + var7 + " был успешно добавлен в список друзей.");
            }
         }
      }
   }

   private void a(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("§7[§dClickAction§7] §f" + var1), false);
      }
   }
}
