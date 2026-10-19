package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.animation.NotificationManager;
import brokke.cc.animation.NotificationType;
import brokke.cc.config.ConfigManager;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.core.ItemSlot;
import brokke.cc.core.SlotGroup;
import brokke.cc.core.SlotGroups;
import brokke.cc.core.Timer2;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.render.WheelPipeline;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventClickSlot;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.util.CursorHelper;
import brokke.cc.util.ItemUtility;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

@NewFunction(name = "Auto Swap", description = "Свапает талисманы и сферы", category = Category.COMBAT)
public class AutoSwap extends Function {
   private final ModeSetting mod = new ModeSetting("Мод", "Дуо", "Колесо");
   private final BindSetting button = new BindSetting("Кнопка", -1).visibleWhen(() -> this.mod.isSelected("Дуо"));
   private final ModeSetting itemMode = new ModeSetting("Предмет", "Талисман", "Сфера").visibleWhen(() -> this.mod.isSelected("Дуо"));
   private final ModeSetting swapToMode = new ModeSetting("Свапать на", "Талисман", "Сфера").visibleWhen(() -> this.mod.isSelected("Дуо"));
   private final ModeSetting server = new ModeSetting("Сервер", "HW", "FT").visibleWhen(() -> this.mod.isSelected("Дуо"));
   private final BooleanSetting separateSwaps = new BooleanSetting("Отдельные свапы", false).visibleWhen(() -> this.mod.isSelected("Дуо"));
   private final BindSetting cerberusSphereBind = new BindSetting("Сфера цербера", -1)
      .visibleWhen(() -> this.mod.isSelected("Дуо") && this.separateSwaps.isToggled() && this.server.isSelected("HW"));
   private final BindSetting vortexTalismanBind = new BindSetting("Талисман вихря", -1)
      .visibleWhen(() -> this.mod.isSelected("Дуо") && this.separateSwaps.isToggled() && this.server.isSelected("FT"));
   private final BindSetting wheelBind = new BindSetting("Кнопка колеса", -1).visibleWhen(() -> this.mod.isSelected("Колесо"));
   private final NumberSetting wheelSlots = new NumberSetting("Ячейки", 3.0, 3.0, 8.0, 1.0).visibleWhen(() -> this.mod.isSelected("Колесо"));
   private final Timer2 timer = new Timer2();
   private static final int MAX_WHEEL_SLOTS = 8;
   private static final float TRAJECTORY_DEADZONE = 20.0F;
   private static final float HOVER_HYSTERESIS_DEG = 8.0F;
   private final ItemStack[] wheelItems = new ItemStack[8];
   private final Animation openAnim = new Animation(0.0F);
   private final Animation[] sectorHoverAnims = new Animation[8];
   private boolean wheelOpen;
   private int hoverIndex = -1;
   private int pendingPickSlot = -1;

   public AutoSwap() {
      for (int var1 = 0; var1 < 8; var1++) {
         this.wheelItems[var1] = ItemStack.EMPTY;
         this.sectorHoverAnims[var1] = new Animation(0.0F);
         this.sectorHoverAnims[var1].setEasing(Easing.SPRING_SOFT);
      }

      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.addSettings(
         this.mod,
         this.button,
         this.itemMode,
         this.swapToMode,
         this.server,
         this.separateSwaps,
         this.cerberusSphereBind,
         this.vortexTalismanBind,
         this.wheelBind,
         this.wheelSlots
      );
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.t();
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (mc.player != null) {
         if (this.mod.isSelected("Колесо")) {
            if (this.pendingPickSlot != -1 && mc.currentScreen == null) {
               this.pendingPickSlot = -1;
            }

            int var2;
            if ((var2 = this.wheelBind.getKey()) == -1) {
               var2 = this.button.getKey();
            }

            if (var2 == -1) {
               var2 = this.getKeybind();
            }

            if (var1.getAction() == 1) {
               if (var1.getKey() == 256) {
                  if (this.wheelOpen) {
                     this.t();
                  }

                  return;
               }

               if (var2 != -1 && var2 != 0 && var1.getKey() == var2) {
                  if (mc.currentScreen == null && !this.wheelOpen) {
                     this.wheelOpen = true;
                     this.openAnim.reset(0.0F);

                     for (int var3 = 0; var3 < 8; var3++) {
                        this.sectorHoverAnims[var3].reset(0.0F);
                     }

                     this.hoverIndex = -1;
                     this.pendingPickSlot = -1;
                     CursorHelper.c();
                     CursorHelper.b();
                  }

                  return;
               }

               if (this.wheelOpen) {
                  if (var1.getKey() == -100) {
                     this.p();
                     return;
                  }

                  if (var1.getKey() == -101) {
                     this.q();
                     return;
                  }

                  if (var1.getKey() == -102) {
                     this.r();
                     return;
                  }
               }
            } else if (var1.getAction() == 0 && var2 != -1 && var2 != 0 && var1.getKey() == var2 && this.wheelOpen) {
               this.s();
            }
         } else if (mc.currentScreen == null) {
            if (this.separateSwaps.isToggled()) {
               if (this.server.isSelected("HW")
                  && this.cerberusSphereBind.getKey() != -1
                  && var1.getAction() == 1
                  && this.cerberusSphereBind.getKey() == var1.getKey()) {
                  if (this.o()) {
                     this.m();
                  }

                  return;
               }

               if (this.server.isSelected("FT")
                  && this.vortexTalismanBind.getKey() != -1
                  && var1.getAction() == 1
                  && this.vortexTalismanBind.getKey() == var1.getKey()) {
                  if (this.o()) {
                     this.n();
                  }

                  return;
               }
            }

            if (var1.getAction() == 1 && this.button.getKey() != -1 && this.button.getKey() == var1.getKey() && this.o()) {
               this.l();
            }
         }
      }
   }

   private boolean o() {
      AutoTotem var1 = FunctionManager.a(AutoTotem.class);
      return var1 == null || !var1.l();
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (mc.player != null && this.mod.isSelected("Колесо") && this.wheelOpen) {
         if (mc.currentScreen != null) {
            this.t();
         } else {
            CursorHelper.c();
            CursorHelper.b();
            this.openAnim.animate(1.0F, 0.0F, 6.5F);
            float var2;
            if (!((var2 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F)) <= 0.01F)) {
               float var3 = 0.9F + 0.1F * var2;
               DrawContext var4 = var1.getContext();
               float var5 = RenderUtil.a() / 2.0F;
               float var6 = RenderUtil.b() / 2.0F;
               float var7 = 74.0F;
               float var8 = 46.0F;
               float var9 = 3.5F;
               int var10 = this.u();
               float var11 = (float)mc.mouse.getX() / 2.0F;
               float var12 = (float)mc.mouse.getY() / 2.0F;
               this.hoverIndex = this.a(var11, var12, var5, var6, var8 * var3, var7 * var3, var10, this.hoverIndex);
               float var13 = 360.0F / var10;
               float var14 = (var7 + var8) * 0.5F;

               for (int var15 = 0; var15 < var10; var15++) {
                  int var16 = var15 == this.hoverIndex ? 1 : 0;
                  this.sectorHoverAnims[var15].animate(var16 != 0 ? 1.0F : 0.0F, 0.0F, 6.0F);
               }

               int[] var29 = new int[var10];

               for (int var30 = 0; var30 < var10; var30++) {
                  float var17 = Math.clamp(this.sectorHoverAnims[var30].getValue(), 0.0F, 1.0F);
                  var29[var30] = this.a(var17, 1.0F);
               }

               if (!WheelPipeline.a(RenderUtil.c(), var5, var6, var8 * var3, var7 * var3, var9, 2.5F, var10, var29, var2, 0.0F)) {
                  BufferBuilder var31 = ColorPipeline.a();
                  if (var31 != null) {
                     for (int var18 = 0; var18 < var10; var18++) {
                        int var19 = this.a(Math.clamp(this.sectorHoverAnims[var18].getValue(), 0.0F, 1.0F), var2);
                        float var20 = -90.0F + var13 * var18 + var9 * 0.5F;
                        float var21 = -90.0F + var13 * (var18 + 1) - var9 * 0.5F;
                        this.a(var31, var5, var6, var8 * var3, var7 * var3, var20, var21, var19);
                     }

                     ColorPipeline.a(var31, false);
                  }
               }

               float var32 = mc.getWindow().getScaleFactor();
               float var33 = 2.0F / var32;

               for (int var34 = 0; var34 < var10; var34++) {
                  float var35;
                  double var36 = Math.toRadians(var35 = -90.0F + var13 * (var34 + 0.5F));
                  float var23 = var5 + (float)Math.cos(var36) * var14 * var3;
                  float var24 = var6 + (float)Math.sin(var36) * var14 * var3;
                  ItemStack var25 = this.e(var34);
                  if (!var25.isEmpty() && var25.getItem() != Items.AIR) {
                     Matrix3x2fStack var37 = var4.getMatrices();
                     var37.pushMatrix();
                     var37.translate(var23 * var33, var24 * var33);
                     var37.scale(var3, var3);
                     var4.drawItem(var25, -8, -8);
                     var4.drawStackOverlay(mc.textRenderer, var25, -8, -8);
                     var37.popMatrix();
                  } else {
                     float var26 = Msdf.a(MsdfManager.BOLD, "+", 9.0F * var3);
                     float var27 = Msdf.a(MsdfManager.BOLD, 9.0F * var3);
                     Color var28 = var34 == this.hoverIndex ? Color.WHITE : new Color(255, 255, 255, 140);
                     RenderUtil.a(var4, MsdfManager.BOLD, var23 - var26 / 2.0F, var24 - var27 / 2.0F, "+", 9.0F * var3, var28);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onClickSlot(EventClickSlot var1) {
      if (mc.player != null
         && this.mod.isSelected("Колесо")
         && this.pendingPickSlot != -1
         && mc.currentScreen instanceof HandledScreen
         && var1.getActionType() != SlotActionType.QUICK_CRAFT) {
         ScreenHandler var2 = mc.player.currentScreenHandler;
         if (var2 != null && var1.getSlotId() >= 0 && var1.getSlotId() < var2.slots.size()) {
            Slot var3 = var2.slots.get(var1.getSlotId());
            ItemStack var4 = var3.getStack();
            if ((var4 == null || var4.isEmpty()) && var2.getCursorStack() != null && !var2.getCursorStack().isEmpty()) {
               var4 = var2.getCursorStack();
            }

            if (var4 != null && !var4.isEmpty() && var4.getItem() != Items.AIR) {
               int var5 = this.pendingPickSlot;
               this.pendingPickSlot = -1;
               ItemStack var6 = var4.copy();
               var6.setCount(1);
               this.a(var5, var6);
               var1.cancel();
               mc.setScreen(null);
               if (mc.mouse != null) {
                  mc.mouse.lockCursor();
               }
            }
         }
      }
   }

   private void p() {
      if (this.hoverIndex >= 0) {
         if (this.f(this.hoverIndex)) {
            this.b(this.hoverIndex);
         } else {
            this.d(this.hoverIndex);
            this.t();
         }
      }
   }

   private void q() {
      if (this.hoverIndex < 0) {
         this.t();
      } else if (this.f(this.hoverIndex)) {
         this.b(this.hoverIndex);
      } else {
         this.c(this.hoverIndex);
         this.t();
      }
   }

   private void r() {
      if (this.hoverIndex >= 0 && !this.f(this.hoverIndex)) {
         this.c(this.hoverIndex);
      }
   }

   private void s() {
      if (this.wheelOpen) {
         int var1 = this.hoverIndex;
         this.t();
         if (var1 >= 0 && !this.f(var1)) {
            this.d(var1);
         }
      }
   }

   private void b(int var1) {
      if (mc.player != null) {
         this.pendingPickSlot = var1;
         this.wheelOpen = false;
         this.hoverIndex = -1;
         if (mc.mouse != null && mc.currentScreen == null) {
            mc.mouse.lockCursor();
         }

         mc.setScreen(new InventoryScreen(Objects.requireNonNull(mc.player)));
      }
   }

   private void c(int var1) {
      this.a(var1, ItemStack.EMPTY);
   }

   private void d(int var1) {
      if (this.timer.a(150L)) {
         ItemStack var2 = this.e(var1);
         if (!var2.isEmpty() && var2.getItem() != Items.AIR && mc.player != null) {
            Slot var3 = this.a(var2);
            if (var3 != null) {
               this.b(var3.id, var3.getStack());
            }
         }
      }
   }

   private void t() {
      this.wheelOpen = false;
      this.openAnim.reset(0.0F);

      for (int var1 = 0; var1 < 8; var1++) {
         this.sectorHoverAnims[var1].reset(0.0F);
      }

      this.hoverIndex = -1;
      this.pendingPickSlot = -1;
      if (mc != null && mc.mouse != null && mc.currentScreen == null) {
         mc.mouse.lockCursor();
      }
   }

   private int u() {
      return MathHelper.clamp((int)Math.round(this.wheelSlots.getValue()), 3, 8);
   }

   private void a(int var1, ItemStack var2) {
      if (var1 >= 0 && var1 < 8) {
         if (var2 != null && !var2.isEmpty() && var2.getItem() != Items.AIR) {
            ItemStack var3 = var2.copy();
            var3.setCount(1);
            this.wheelItems[var1] = var3;
            ConfigManager.onSettingChanged();
         } else {
            this.wheelItems[var1] = ItemStack.EMPTY;
            ConfigManager.onSettingChanged();
         }
      }
   }

   public JsonArray k() {
      JsonArray var1 = new JsonArray();

      for (int var2 = 0; var2 < 8; var2++) {
         ItemStack var3 = this.wheelItems[var2];
         if (var3 != null && !var3.isEmpty() && var3.getItem() != Items.AIR) {
            JsonObject var4 = new JsonObject();
            var4.addProperty("slot", var2);
            var4.addProperty("id", Registries.ITEM.getId(var3.getItem()).toString());
            String var5 = var3.getName().getString();
            if (var5 != null && !var5.isEmpty()) {
               var4.addProperty("name", var5);
            }

            String var6 = ItemUtility.d(var3);
            if (var6 != null && !var6.isEmpty()) {
               var4.addProperty("don", var6);
            }

            var1.add(var4);
         }
      }

      return var1;
   }

   public void a(JsonArray var1) {
      for (int var2 = 0; var2 < 8; var2++) {
         this.wheelItems[var2] = ItemStack.EMPTY;
      }

      if (var1 != null) {
         for (JsonElement var3 : var1) {
            if (var3.isJsonObject()) {
               JsonObject var4 = var3.getAsJsonObject();
               int var5;
               if (var4.has("slot") && var4.has("id") && (var5 = var4.get("slot").getAsInt()) >= 0 && var5 < 8) {
                  String var6 = var4.get("id").getAsString();

                  try {
                     Item var7 = Registries.ITEM.get(Identifier.of(var6));
                     if (var7 != null && var7 != Items.AIR) {
                        ItemStack var8 = new ItemStack(var7, 1);
                        if (var4.has("name")) {
                           String var9 = var4.get("name").getAsString();
                           if (var9 != null && !var9.isEmpty()) {
                              var8.set(DataComponentTypes.CUSTOM_NAME, Text.literal(var9));
                           }
                        }

                        if (var4.has("don")) {
                           String var13 = var4.get("don").getAsString();
                           if (var13 != null && !var13.isEmpty()) {
                              NbtCompound var10 = new NbtCompound();
                              var10.putString("don-item", var13);
                              var8.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var10));
                           }
                        }

                        this.wheelItems[var5] = var8;
                     }
                  } catch (Exception var11) {
                  }
               }
            }
         }
      }
   }

   private ItemStack e(int var1) {
      if (var1 >= 0 && var1 < 8) {
         ItemStack var2 = this.wheelItems[var1];
         return var2 == null ? ItemStack.EMPTY : var2;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private boolean f(int var1) {
      ItemStack var2 = this.e(var1);
      return var2.isEmpty() || var2.getItem() == Items.AIR;
   }

   private Slot a(ItemStack var1) {
      return mc.player != null && var1 != null && !var1.isEmpty()
         ? mc.player
            .playerScreenHandler
            .slots
            .stream()
            .filter(var0 -> var0.id != 45 && !var0.getStack().isEmpty())
            .filter(var2 -> this.a(var2.getStack(), var1))
            .max(Comparator.comparing(var0 -> var0.getStack().hasEnchantments()))
            .orElse(null)
         : null;
   }

   private boolean a(ItemStack var1, ItemStack var2) {
      return ItemUtility.a(var1, var2);
   }

   private int a(float var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8) {
      if (var6 <= 0.0F) {
         return -1;
      }

      float var9 = var1 - var3;
      float var10 = var2 - var4;
      float var11 = (float)Math.sqrt(var9 * var9 + var10 * var10);
      float var12 = 12.0F;
      float var13 = var6 + 240.0F;
      if (var11 < var12) {
         return var8;
      }

      if (var11 > var13) {
         return -1;
      }

      double var14;
      if ((var14 = Math.atan2(var10, var9) + (Math.PI / 2)) < 0.0) {
         var14 += Math.PI * 2;
      }

      double var21;
      float var16 = (float)Math.toDegrees(var21 = var14 % (Math.PI * 2));
      float var17 = 360.0F / var7;
      int var18;
      if ((var18 = (int)Math.floor(var21 / (Math.PI * 2) * var7)) >= 0 && var18 < var7) {
         if (var8 >= 0 && var8 < var7) {
            float var19 = this.a(var17 * (var8 + 0.5F));
            float var20;
            if ((var20 = Math.abs(this.a(var16) - this.a(var19))) > 180.0F) {
               var20 = 360.0F - var20;
            }

            if (var20 <= var17 * 0.5F + 8.0F) {
               return var8;
            }
         }

         return var18;
      } else {
         return -1;
      }
   }

   private float a(float var1) {
      float var2;
      return (var2 = var1 % 360.0F) < 0.0F ? var2 + 360.0F : var2;
   }

   private void a(BufferBuilder var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      float var9;
      if (!((var9 = var7 - var6) <= 0.0F)) {
         int var10 = Math.max(48, Math.round(var9));
         float var11 = var9 / var10;

         for (int var12 = 0; var12 < var10; var12++) {
            float var13 = (float)Math.toRadians(var6 + var11 * var12);
            float var14 = (float)Math.toRadians(var6 + var11 * (var12 + 1));
            float var15 = (float)Math.cos(var13);
            float var16 = (float)Math.sin(var13);
            float var17 = (float)Math.cos(var14);
            float var18 = (float)Math.sin(var14);
            float var19 = var2 + var15 * var4;
            float var20 = var3 + var16 * var4;
            float var21 = var2 + var15 * var5;
            float var22 = var3 + var16 * var5;
            float var23 = var2 + var17 * var5;
            float var24 = var3 + var18 * var5;
            float var25 = var2 + var17 * var4;
            float var26 = var3 + var18 * var4;
            ColorPipeline.a(var1, var19, var20, var21, var22, var23, var24, var8);
            ColorPipeline.a(var1, var19, var20, var23, var24, var25, var26, var8);
         }
      }
   }

   private int a(float var1, float var2) {
      Color var3 = Theme.getInstance().getAccent();
      int var4 = var3 != null ? var3.getRed() : 235;
      int var5 = var3 != null ? var3.getGreen() : 95;
      int var6 = var3 != null ? var3.getBlue() : 105;
      byte var7 = 25;
      byte var8 = 26;
      byte var9 = 31;
      int var11 = Math.clamp(Math.round((235.0F + 10.0F * var1) * var2), 0, 255);
      int var12 = (int)(var7 + (var4 - var7) * var1);
      int var13 = (int)(var8 + (var5 - var8) * var1);
      int var14 = (int)(var9 + (var6 - var9) * var1);
      return var11 << 24 | var12 << 16 | var13 << 8 | var14;
   }

   public void l() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && this.timer.a(150L) && this.o()) {
         SlotGroup var1 = SlotGroups.b().a(SlotGroups.a()).a(SlotGroups.d());
         int var2 = this.itemMode.isSelected("Сфера") && this.swapToMode.isSelected("Сфера") ? 1 : 0;
         if (this.server.isSelected("HW") && var2 != 0) {
            this.a(var1);
         } else {
            List<ItemSlot> var3 = this.a(var1, this.itemMode.getSelection());
            List<ItemSlot> var4 = this.a(var1, this.swapToMode.getSelection());
            if (!var3.isEmpty() || !var4.isEmpty()) {
               ItemSlot var5 = var3.stream().min(Comparator.comparingInt(var0 -> ItemUtility.r(var0.a()))).orElse(null);
               ItemSlot var6 = var4.stream().filter(var1x -> var1x != var5).min(Comparator.comparingInt(var0 -> ItemUtility.r(var0.a()))).orElse(null);
               if (var5 != null || var6 != null) {
                  ItemSlot var7 = var5 != null ? var5 : var6;
                  ItemSlot var8 = var6 != null ? var6 : var5;
                  ItemStack var9 = mc.player.getOffHandStack();
                  boolean var10 = this.a(var9, var7);
                  boolean var11 = this.a(var9, var8);
                  ItemSlot var12;
                  if (var10 && !var11) {
                     var12 = var8;
                  } else if (var11 && !var10) {
                     var12 = var7;
                  } else if (var7.get45() == 45) {
                     var12 = var8;
                  } else if (var8.get45() == 45) {
                     var12 = var7;
                  } else {
                     var12 = var7;
                  }

                  if (var12 != null && var12.get45() != 45 && !ItemStack.areEqual(var9, var12.a())) {
                     this.b(var12.get45(), var12.a());
                  }
               }
            }
         }
      }
   }

   private boolean a(ItemStack var1, ItemSlot var2) {
      if (var1 != null && var2 != null && !var1.isEmpty() && !var2.e()) {
         return var2.get45() == 45 ? true : ItemUtility.a(var1, var2.a());
      } else {
         return false;
      }
   }

   private List<ItemSlot> a(SlotGroup<ItemSlot> var1, String var2) {
      if ("Сфера".equals(var2)) {
         List var4 = var1.b(Items.PLAYER_HEAD)
            .stream()
            .filter(var0 -> ItemUtility.g(var0.a()))
            .filter(var1x -> !this.separateSwaps.isToggled() || !this.server.isSelected("HW") || !this.b(var1x.a()))
            .toList();
         return !var4.isEmpty()
            ? var4
            : var1.b(Items.PLAYER_HEAD)
               .stream()
               .filter(var1x -> !this.separateSwaps.isToggled() || !this.server.isSelected("HW") || !this.b(var1x.a()))
               .toList();
      } else {
         List var3 = var1.b(Items.TOTEM_OF_UNDYING)
            .stream()
            .filter(var0 -> ItemUtility.o(var0.a()) || ItemUtility.c(var0.a()) || ItemUtility.r(var0.a()) < 90)
            .filter(var1x -> !this.separateSwaps.isToggled() || !this.server.isSelected("FT") || !this.c(var1x.a()))
            .toList();
         return !var3.isEmpty()
            ? var3
            : var1.b(Items.TOTEM_OF_UNDYING)
               .stream()
               .filter(var1x -> !this.separateSwaps.isToggled() || !this.server.isSelected("FT") || !this.c(var1x.a()))
               .toList();
      }
   }

   private void a(SlotGroup<ItemSlot> var1) {
      List<ItemSlot> var2 = var1.b(Items.PLAYER_HEAD)
         .stream()
         .filter(var0 -> ItemUtility.g(var0.a()))
         .filter(var1x -> !this.separateSwaps.isToggled() || !this.server.isSelected("HW") || !this.b(var1x.a()))
         .toList();
      if (!var2.isEmpty()) {
         ItemSlot var3 = var2.stream()
            .filter(var0 -> ItemUtility.m(var0.a()) >= 3)
            .min(Comparator.comparingInt(var0 -> 10 - ItemUtility.m(var0.a())))
            .orElse(null);
         ItemSlot var4 = var2.stream().filter(var0 -> ItemUtility.m(var0.a()) < 3).min(Comparator.comparingInt(var0 -> ItemUtility.n(var0.a()))).orElse(null);
         ItemStack var6 = mc.player.getOffHandStack();
         ItemSlot var5;
         if (var3 != null && var4 != null) {
            var5 = ItemStack.areEqual(var6, var3.a()) ? var4 : var3;
         } else {
            ItemSlot var7 = var2.stream()
               .filter(var1x -> ItemUtility.m(var1x.a()) < 3 && var1x != var4)
               .min(Comparator.comparingInt(var0 -> ItemUtility.n(var0.a())))
               .orElse(null);
            if (var4 == null || var7 == null) {
               return;
            }

            var5 = ItemStack.areEqual(var6, var4.a()) ? var7 : var4;
         }

         if (var5 != null && var5.get45() != 45 && !ItemStack.areEqual(var6, var5.a())) {
            this.b(var5.get45(), var5.a());
         }
      }
   }

   public void m() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && this.timer.a(150L) && this.o()) {
         SlotGroup<ItemSlot> var1 = SlotGroups.b().a(SlotGroups.a());
         ItemSlot var2 = var1.b(this::b).stream().filter(var0 -> var0.get45() != 45).min(Comparator.comparingInt(var0 -> ItemUtility.r(var0.a()))).orElse(null);
         if (var2 != null) {
            this.b(var2.get45(), var2.a());
         }
      }
   }

   public void n() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && this.timer.a(150L) && this.o()) {
         SlotGroup<ItemSlot> var1 = SlotGroups.b().a(SlotGroups.a());
         ItemSlot var2 = var1.b(this::c).stream().filter(var0 -> var0.get45() != 45).findFirst().orElse(null);
         if (var2 != null) {
            this.b(var2.get45(), var2.a());
         }
      }
   }

   private boolean b(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = ItemUtility.a(var1.getName().getString()).toLowerCase(Locale.ROOT);
         return var2.contains("сфера цербера") || var2.contains("цербер") || var2.contains("cerber") || ItemUtility.l(var1);
      } else {
         return false;
      }
   }

   private boolean c(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = ItemUtility.a(var1.getName().getString()).toLowerCase(Locale.ROOT);
         return var2.contains("талисман вихря") || var2.contains("вихр") || var2.contains("vortex");
      } else {
         return false;
      }
   }

   private void b(int var1, ItemStack var2) {
      if (mc.player != null && mc.world != null && var1 != 45 && var1 >= 0) {
         HandlerProcessor.a().getInventoryHandler().a(var1, 45, 1);
         this.timer.a();
         if (var2 != null && !var2.isEmpty()) {
            String var3 = this.d(var2);
            NotificationManager.addNotification("Auto Swap", var3, NotificationType.ENABLE, 1500L);
         }
      }
   }

   private String d(ItemStack var1) {
      return ItemUtility.q(var1);
   }
}
