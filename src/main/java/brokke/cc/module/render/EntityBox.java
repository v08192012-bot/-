package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "EntityBox", description = "Отображает 3D хитбоксы игроков", category = Category.RENDER)
public class EntityBox extends Function {
   private final BooleanSetting onlyArmor = new BooleanSetting("Только в броне", false);
   private final ModeSetting colorSource = new ModeSetting("Источник цвета", "Клиентский", "Свой");
   private final ColorSetting colorSetting = new ColorSetting("Цвет", new Color(255, 255, 255));
   private static final int[][] EDGES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private static final int[][] FACES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final float[][] screenCorners = new float[8][];

   public EntityBox() {
      this.addSettings(this.onlyArmor, this.colorSource, this.colorSetting);
      this.colorSource.onChanged(this::k);
      this.k();
   }

   private void k() {
      this.colorSetting.setVisible(this.colorSource.isSelected("Свой"));
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         float var2 = var1.getTickCounter().getTickProgress(true);
         Color var3 = Theme.getInstance().getAccent();
         Color var4 = this.colorSource.isSelected("Свой") ? this.colorSetting.getColor() : var3;
         if (var4 == null) {
            var4 = var3;
         }

         int var5 = var4.getAlpha() << 24 | var4.getRed() << 16 | var4.getGreen() << 8 | var4.getBlue();
         int var6 = 754974720 | var4.getRed() << 16 | var4.getGreen() << 8 | var4.getBlue();
         BufferBuilder var7 = ColorPipeline.a();
         if (var7 != null) {
            for (PlayerEntity var9 : mc.world.getPlayers()) {
               if (this.a(var9)) {
                  Vec3d var10 = var9.getLerpedPos(var2);
                  Vec3d var11 = var10.subtract(var9.getEntityPos());
                  Box var12 = var9.getBoundingBox().offset(var11);
                  int var13 = 0;
                  this.screenCorners[0] = WorldToScreen.a(var12.minX, var12.minY, var12.minZ);
                  if (this.screenCorners[0] != null) {
                     var13++;
                  }

                  this.screenCorners[1] = WorldToScreen.a(var12.maxX, var12.minY, var12.minZ);
                  if (this.screenCorners[1] != null) {
                     var13++;
                  }

                  this.screenCorners[2] = WorldToScreen.a(var12.maxX, var12.minY, var12.maxZ);
                  if (this.screenCorners[2] != null) {
                     var13++;
                  }

                  this.screenCorners[3] = WorldToScreen.a(var12.minX, var12.minY, var12.maxZ);
                  if (this.screenCorners[3] != null) {
                     var13++;
                  }

                  this.screenCorners[4] = WorldToScreen.a(var12.minX, var12.maxY, var12.minZ);
                  if (this.screenCorners[4] != null) {
                     var13++;
                  }

                  this.screenCorners[5] = WorldToScreen.a(var12.maxX, var12.maxY, var12.minZ);
                  if (this.screenCorners[5] != null) {
                     var13++;
                  }

                  this.screenCorners[6] = WorldToScreen.a(var12.maxX, var12.maxY, var12.maxZ);
                  if (this.screenCorners[6] != null) {
                     var13++;
                  }

                  this.screenCorners[7] = WorldToScreen.a(var12.minX, var12.maxY, var12.maxZ);
                  if (this.screenCorners[7] != null) {
                     var13++;
                  }

                  if (var13 != 0) {
                     for (int[] var17 : FACES) {
                        float[] var18 = this.screenCorners[var17[0]];
                        float[] var19 = this.screenCorners[var17[1]];
                        float[] var20 = this.screenCorners[var17[2]];
                        float[] var21 = this.screenCorners[var17[3]];
                        if (var18 != null && var19 != null && var20 != null && var21 != null) {
                           ColorPipeline.a(var7, var18, var19, var20, var21, var6, var6, var6, var6);
                        }
                     }

                     for (int[] var25 : EDGES) {
                        float[] var26 = this.screenCorners[var25[0]];
                        float[] var27 = this.screenCorners[var25[1]];
                        if (var26 != null && var27 != null) {
                           ColorPipeline.a(var7, var26[0], var26[1], var27[0], var27[1], 1.0F, var5);
                        }
                     }
                  }
               }
            }

            ColorPipeline.a(var7, false);
         }
      }
   }

   private boolean a(PlayerEntity var1) {
      if (var1 != null && !var1.isRemoved() && var1.isAlive()) {
         return var1 == mc.player && mc.options.getPerspective().isFirstPerson() ? false : !this.onlyArmor.isToggled() || this.b(var1);
      } else {
         return false;
      }
   }

   private boolean b(PlayerEntity var1) {
      for (EquipmentSlot var5 : ARMOR_SLOTS) {
         ItemStack var6 = var1.getEquippedStack(var5);
         if (var6 != null && !var6.isEmpty()) {
            return true;
         }
      }

      return false;
   }
}
