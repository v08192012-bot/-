package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.core.AutoBuyEngine;
import brokke.cc.core.Timer2;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfFont;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.module.player.InventoryButtons;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.joml.Matrix4f;

public final class InventoryButtonsOverlay {
   private static final Map<String, Animation> HOVER_ANIMS = new HashMap<>();
   private static InventoryButtonsOverlay.a currentAction = null;
   private static final List<Integer> actionQueue = new ArrayList<>();
   private static final Timer2 actionTimer = new Timer2();
   private static HandledScreen<?> lastScreen = null;
   private static int lastGx;
   private static int lastGy;
   private static int lastGw;
   private static int lastGh;
   private static int lastMouseX;
   private static int lastMouseY;
   private static boolean hasPendingRender = false;

   private InventoryButtonsOverlay() {
   }

   private static Animation a(String var0) {
      return HOVER_ANIMS.computeIfAbsent(var0, var0x -> {
         Animation var1 = new Animation(0.0F);
         var1.setEasing(Easing.SPRING_SOFT);
         return var1;
      });
   }

   public static boolean a(HandledScreen<?> var0) {
      if (var0 == null) {
         return false;
      }

      if (var0 instanceof CreativeInventoryScreen) {
         return false;
      }

      InventoryButtons var1 = FunctionManager.a(InventoryButtons.class);
      if (var1 == null || !var1.isActive()) {
         return false;
      }

      if (var0 instanceof InventoryScreen) {
         return true;
      }

      if (var0 instanceof GenericContainerScreen var6) {
         if (!var1.inContainers.getValue()) {
            return false;
         }

         if (AutoBuyEngine.a(var6)) {
            return false;
         }

         String var7 = var6.getTitle() != null ? var6.getTitle().getString() : null;
         return var7 == null || !var7.toLowerCase(Locale.ROOT).contains("аукцион");
      } else {
         if (var1.inContainers.getValue()) {
            MinecraftClient var2 = MinecraftClient.getInstance();
            if (var2.player != null) {
               ScreenHandler var3 = var0.getScreenHandler();
               if (var3 != null) {
                  for (Slot var5 : var3.slots) {
                     if (var5.inventory == var2.player.getInventory()) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public static void a(HandledScreen<?> var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      lastScreen = var0;
      lastGx = var1;
      lastGy = var2;
      lastGw = var3;
      lastGh = var4;
      lastMouseX = var5;
      lastMouseY = var6;
      hasPendingRender = true;
   }

   public static void a(MinecraftClient var0) {
      if (hasPendingRender) {
         hasPendingRender = false;
         if (var0 == null || var0.currentScreen == null || var0.currentScreen != lastScreen) {
            lastScreen = null;
            a();
         } else if (!a(lastScreen)) {
            lastScreen = null;
            a();
         } else {
            b(var0);
            a(var0, lastGx, lastGy, lastGw, lastGh, lastMouseX, lastMouseY);
         }
      }
   }

   private static void b(MinecraftClient var0) {
      if (currentAction == null || actionQueue.isEmpty()) {
         currentAction = null;
      } else if (var0 != null && var0.player != null && var0.interactionManager != null && var0.currentScreen != null) {
         InventoryButtons var1 = FunctionManager.a(InventoryButtons.class);
         long var2 = var1 != null ? (long)var1.delay.getValue() : 15L;
         if (var2 > 0L) {
            if (actionTimer.a(var2)) {
               int var5 = actionQueue.remove(0);
               a(var0, var5, currentAction);
               actionTimer.a();
               if (actionQueue.isEmpty()) {
                  currentAction = null;
               }
            }
         } else {
            while (!actionQueue.isEmpty()) {
               int var4 = actionQueue.remove(0);
               a(var0, var4, currentAction);
            }

            currentAction = null;
         }
      } else {
         a();
      }
   }

   private static void a(MinecraftClient var0, int var1, InventoryButtonsOverlay.a var2) {
      ScreenHandler var3 = var0.player.currentScreenHandler;
      if (var3 != null && var1 >= 0 && var1 < var3.slots.size()) {
         Slot var4 = var3.getSlot(var1);
         if (var4 != null && var4.hasStack() && !var4.getStack().isEmpty()) {
            if (var2 == InventoryButtonsOverlay.a.STORE_ALL) {
               var0.interactionManager.clickSlot(var3.syncId, var1, 0, SlotActionType.QUICK_MOVE, var0.player);
            } else {
               var0.interactionManager.clickSlot(var3.syncId, var1, 1, SlotActionType.THROW, var0.player);
            }
         }
      }
   }

   public static void a(MinecraftClient var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      if (var0 != null && var0.getWindow() != null) {
         if (!Msdf.a()) {
            MsdfManager.a();
         }

         InventoryButtons var7 = FunctionManager.a(InventoryButtons.class);
         if (var7 != null && var7.isActive()) {
            int var8 = var0.getWindow().getScaledWidth();
            int var9 = var0.getWindow().getScaledHeight();
            Matrix4f var10 = new Matrix4f().setOrtho(0.0F, var8, var9, 0.0F, -1000.0F, 1000.0F);
            MsdfFont var11 = MsdfManager.BOLD != null ? MsdfManager.BOLD : MsdfManager.getDefaultFont();
            int var12 = Theme.getInstance().getAccent().getRGB();
            float var13 = var1 + var3 / 2.0F;
            float var14 = var2 - 19.5F;
            float var15 = 15.5F;
            boolean var16 = lastScreen instanceof InventoryScreen;
            boolean var17 = var7.secondButton.getValue();
            if (var17) {
               float var18 = 68.0F;
               float var19 = 5.0F;
               float var20 = var18 * 2.0F + var19;
               float var21 = var13 - var20 / 2.0F;
               float var22 = var21 + var18 + var19;
               String var23 = currentAction == InventoryButtonsOverlay.a.DROP_ALL ? "Сброс... (" + actionQueue.size() + ")" : "Выкинуть всё";
               a(var10, var11, var21, var14, var18, var15, "U", var23, currentAction == InventoryButtonsOverlay.a.DROP_ALL, var5, var6, var12, "drop_all");
               if (var16) {
                  String var24 = currentAction == InventoryButtonsOverlay.a.DROP_NO_ARMOR ? "Сброс... (" + actionQueue.size() + ")" : "Без брони";
                  a(
                     var10,
                     var11,
                     var22,
                     var14,
                     var18,
                     var15,
                     "v",
                     var24,
                     currentAction == InventoryButtonsOverlay.a.DROP_NO_ARMOR,
                     var5,
                     var6,
                     var12,
                     "drop_no_armor"
                  );
               } else {
                  String var28 = currentAction == InventoryButtonsOverlay.a.STORE_ALL ? "Склад... (" + actionQueue.size() + ")" : "Сложить всё";
                  a(var10, var11, var22, var14, var18, var15, "u", var28, currentAction == InventoryButtonsOverlay.a.STORE_ALL, var5, var6, var12, "store_all");
               }
            } else {
               float var25 = 88.0F;
               float var26 = var13 - var25 / 2.0F;
               String var27 = currentAction == InventoryButtonsOverlay.a.DROP_ALL ? "Сброс... (" + actionQueue.size() + ")" : "Выкинуть всё";
               a(var10, var11, var26, var14, var25, var15, "U", var27, currentAction == InventoryButtonsOverlay.a.DROP_ALL, var5, var6, var12, "drop_all");
            }
         }
      }
   }

   private static void a(
      Matrix4f var0,
      MsdfFont var1,
      float var2,
      float var3,
      float var4,
      float var5,
      String var6,
      String var7,
      boolean var8,
      double var9,
      double var11,
      int var13,
      String var14
   ) {
      boolean var15 = a(var9, var11, var2, var3, var4, var5);
      Animation var16 = a(var14);
      var16.animate(var15 ? 1.0F : 0.0F, 0.0F, 9.0F);
      float var17 = Math.clamp(var16.getValue(), 0.0F, 1.0F);
      int var18 = a(1250589, 1711657, var17);
      int var19 = var8 ? a(var18, -50384, 0.18F) : (var17 > 0.01F ? a(var18, var13, 0.1F * var17) : var18);
      int var20 = InterfaceModule.k() ? 135 : 235;
      int var21 = var20 << 24 | var19;
      if (InterfaceModule.l()) {
         RenderUtil.b(var0, var2, var3, var4, var5, 4.0F, 4.0F, 0.35F, 2.0F, 1073741824);
      }

      if (InterfaceModule.k()) {
         RenderUtil.a(var0, var2, var3, var4, var5, 4.0F, 8.0F);
      }

      RenderUtil.a(var0, var2, var3, var4, var5, 4.0F, var21);
      int var22 = var8 ? a(-47803, 180.0F) : a(a(-1, 24.0F), a(var13, 200.0F), var17);
      RenderUtil.a(var0, var2, var3, var4, var5, 4.0F, 0.6F, var22);
      float var23 = 6.0F;
      float var24 = Msdf.a(MsdfManager.DICONS, var6, var23);
      float var25 = 5.3F;
      float var26 = Msdf.a(var1, var7, var25);
      float var27 = 3.5F;
      float var28 = var24 + var27 + var26;
      float var29 = var2 + (var4 - var28) / 2.0F;
      int var30 = var8 ? -38037 : a(-3288352, -1, var17);
      int var31 = var8 ? -38037 : a(-6249035, var13, var17);
      float var32 = var3 + (var5 - Msdf.a(MsdfManager.DICONS, var23)) / 2.0F - 0.2F;
      float var33 = var3 + (var5 - Msdf.a(var1, var25)) / 2.0F - 0.5F;
      Msdf.a(var0, MsdfManager.DICONS, var6, var29, var32, var23, var31);
      Msdf.a(var0, var1, var7, var29 + var24 + var27, var33, var25, var30);
   }

   public static boolean a(HandledScreen<?> var0, double var1, double var3, int var5, int var6, int var7, int var8, int var9) {
      if (var5 != 0) {
         return false;
      }

      if (!a(var0)) {
         return false;
      }

      InventoryButtons var10 = FunctionManager.a(InventoryButtons.class);
      if (var10 != null && var10.isActive()) {
         float var11 = var6 + var8 / 2.0F;
         float var12 = var7 - 19.5F;
         float var13 = 15.5F;
         boolean var14 = var0 instanceof InventoryScreen;
         boolean var15 = var10.secondButton.getValue();
         if (var15) {
            float var16 = 68.0F;
            float var17 = 5.0F;
            float var18 = var16 * 2.0F + var17;
            float var19 = var11 - var18 / 2.0F;
            float var20 = var19 + var16 + var17;
            if (a(var1, var3, var19, var12, var16, var13)) {
               if (currentAction == InventoryButtonsOverlay.a.DROP_ALL) {
                  a();
               } else {
                  a(var0, InventoryButtonsOverlay.a.DROP_ALL);
               }

               return true;
            }

            if (a(var1, var3, var20, var12, var16, var13)) {
               InventoryButtonsOverlay.a var21 = var14 ? InventoryButtonsOverlay.a.DROP_NO_ARMOR : InventoryButtonsOverlay.a.STORE_ALL;
               if (currentAction == var21) {
                  a();
               } else {
                  a(var0, var21);
               }

               return true;
            }
         } else {
            float var22 = 88.0F;
            float var23 = var11 - var22 / 2.0F;
            if (a(var1, var3, var23, var12, var22, var13)) {
               if (currentAction == InventoryButtonsOverlay.a.DROP_ALL) {
                  a();
               } else {
                  a(var0, InventoryButtonsOverlay.a.DROP_ALL);
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void a(HandledScreen<?> var0, InventoryButtonsOverlay.a var1) {
      a();
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.interactionManager != null) {
         ScreenHandler var3 = var0.getScreenHandler();
         if (var3 != null) {
            if (!var3.getCursorStack().isEmpty()) {
               var2.interactionManager.clickSlot(var3.syncId, 64537, 0, SlotActionType.PICKUP, var2.player);
            }

            ArrayList var4 = new ArrayList();
            boolean var5 = var0 instanceof InventoryScreen;
            if (var1 == InventoryButtonsOverlay.a.STORE_ALL) {
               for (Slot var7 : var3.slots) {
                  if (var7.inventory == var2.player.getInventory() && var7.hasStack() && !var7.getStack().isEmpty()) {
                     var4.add(var7.id);
                  }
               }
            } else if (var1 == InventoryButtonsOverlay.a.DROP_ALL) {
               if (var5 && var3 instanceof PlayerScreenHandler) {
                  for (Slot var17 : var3.slots) {
                     if (var17.id != 0 && var17.hasStack() && !var17.getStack().isEmpty()) {
                        var4.add(var17.id);
                     }
                  }
               } else {
                  for (Slot var16 : var3.slots) {
                     if (var16.inventory == var2.player.getInventory() && var16.hasStack() && !var16.getStack().isEmpty()) {
                        var4.add(var16.id);
                     }
                  }
               }
            } else if (var1 == InventoryButtonsOverlay.a.DROP_NO_ARMOR) {
               if (var5 && var3 instanceof PlayerScreenHandler) {
                  for (Slot var19 : var3.slots) {
                     if (var19.id >= 9 && var19.id <= 44 && var19.hasStack() && !var19.getStack().isEmpty()) {
                        var4.add(var19.id);
                     } else if (var19.id >= 1 && var19.id <= 4 && var19.hasStack() && !var19.getStack().isEmpty()) {
                        var4.add(var19.id);
                     }
                  }
               } else {
                  for (Slot var18 : var3.slots) {
                     if (var18.inventory == var2.player.getInventory() && var18.hasStack() && !var18.getStack().isEmpty()) {
                        var4.add(var18.id);
                     }
                  }
               }
            }

            if (!var4.isEmpty()) {
               InventoryButtons var15 = FunctionManager.a(InventoryButtons.class);
               double var20 = var15 != null ? var15.delay.getValue() : 15.0;
               if (var20 <= 0.0) {
                  for (int var10 : (Iterable<Integer>)(Iterable<?>)(var4)) {
                     a(var2, var10, var1);
                  }
               } else {
                  actionQueue.addAll(var4);
                  currentAction = var1;
                  actionTimer.a();
               }
            }
         }
      }
   }

   public static void a() {
      actionQueue.clear();
      currentAction = null;
   }

   private static boolean a(double var0, double var2, float var4, float var5, float var6, float var7) {
      return var0 >= var4 && var0 <= var4 + var6 && var2 >= var5 && var2 <= var5 + var7;
   }

   private static int a(int var0, int var1, float var2) {
      var2 = Math.clamp(var2, 0.0F, 1.0F);
      int var3 = var0 >>> 24 & 0xFF;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >>> 24 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      int var11 = (int)(var4 + (var8 - var4) * var2);
      int var12 = (int)(var5 + (var9 - var5) * var2);
      int var13 = (int)(var6 + (var10 - var6) * var2);
      int var14 = (int)(var3 + (var7 - var3) * var2);
      return var14 << 24 | var11 << 16 | var12 << 8 | var13;
   }

   private static int a(int var0, float var1) {
      int var2 = Math.clamp((int)var1, 0, 255);
      return var2 << 24 | var0 & 16777215;
   }

   public enum a {
      DROP_ALL,
      DROP_NO_ARMOR,
      STORE_ALL;
   }
}
