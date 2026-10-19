package brokke.cc.ui;

import brokke.cc.manager.NicknameManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class BrokkeRenameScreen extends Screen {
   private final String target;
   private final Screen prev;
   private TextFieldWidget field;

   public BrokkeRenameScreen(String var1, Screen var2) {
      super(Text.literal("Переименовать"));
      this.target = var1;
      this.prev = var2;
   }

   @Override
   protected void init() {
      String var1 = NicknameManager.a(this.target);
      this.field = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Text.literal("Новое имя"));
      this.field.setText(var1 != null ? var1 : this.target);
      this.field.setMaxLength(32);
      this.field.setFocused(true);
      this.addDrawableChild(this.field);
      this.addDrawableChild(ButtonWidget.builder(Text.literal("OK"), var1x -> this.a()).dimensions(this.width / 2 - 100, this.height / 2 + 20, 95, 20).build());
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Отмена"), var1x -> this.b()).dimensions(this.width / 2 + 5, this.height / 2 + 20, 95, 20).build()
      );
   }

   private void a() {
      NicknameManager.a(this.target, this.field.getText());
      this.b();
   }

   private void b() {
      this.client.setScreen(this.prev);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawText(this.textRenderer, "Новое имя для " + this.target, this.width / 2 - 100, this.height / 2 - 35, 16777215, false);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (input.getKeycode() == 256) {
         this.b();
         return true;
      } else if (input.getKeycode() == 257 && this.field.isFocused()) {
         this.a();
         return true;
      } else {
         return this.field.keyPressed(input) ? true : super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      return this.field.charTyped(input) || super.charTyped(input);
   }
}
