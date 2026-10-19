package brokke.cc.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;

public class HiddenInventoryScreen extends HandledScreen<PlayerScreenHandler> {
   public HiddenInventoryScreen(PlayerEntity var1) {
      super(var1.playerScreenHandler, var1.getInventory(), Text.empty());
   }

   @Override
   protected void init() {
   }

   @Override
   protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   @Override
   public void removed() {
   }
}
