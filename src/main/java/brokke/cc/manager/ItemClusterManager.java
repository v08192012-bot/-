package brokke.cc.manager;

import brokke.cc.config.HudConfig;
import brokke.cc.cosmetic.DonateItem;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.WorldToScreen;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.ItemUtility;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public final class ItemClusterManager {
   public static final float TAG_FONT = 6.5F;
   public static final float ITEM_ICON_SIZE = 8.5F;
   public static final float PAD_X = 6.0F;
   public static final float PAD_Y = 5.0F;
   public static final float HEADER_GAP = 4.0F;
   public static final float ROW_GAP = 2.0F;
   public static final float ICON_GAP = 4.0F;
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<Long, ItemClusterManager.a> activeClusters = new HashMap<>();
   private static final Set<Integer> clusteredEntityIds = new HashSet<>();
   private static final List<ItemClusterManager.a> renderList = new ArrayList<>();
   private static long idCounter = 0L;

   public static void a() {
      activeClusters.clear();
      clusteredEntityIds.clear();
      renderList.clear();
   }

   public static boolean a(ItemEntity var0) {
      return var0 != null && clusteredEntityIds.contains(var0.getId());
   }

   public static void a(List<Entity> var0, float var1, int var2, float var3) {
      ArrayList var4 = new ArrayList();

      for (Entity var6 : var0) {
         if (var6 instanceof ItemEntity var7 && var7.isAlive() && !var7.getStack().isEmpty()) {
            var4.add(var7);
         }
      }

      clusteredEntityIds.clear();
      renderList.clear();

      for (ItemClusterManager.a var45 : activeClusters.values()) {
         var45.updatedThisFrame = false;
      }

      if (var4.isEmpty()) {
         activeClusters.entrySet().removeIf(var0x -> System.currentTimeMillis() - var0x.getValue().lastUpdateTime > 400L);
      } else {
         float var44 = var1 * var1;
         int var46 = var4.size();
         boolean[] var47 = new boolean[var46];

         for (int var8 = 0; var8 < var46; var8++) {
            if (!var47[var8]) {
               ArrayList var9 = new ArrayList();
               ArrayDeque var10 = new ArrayDeque();
               var10.add(var8);
               var47[var8] = true;

               while (!var10.isEmpty()) {
                  int var11 = (Integer)var10.poll();
                  ItemEntity var12 = (ItemEntity)var4.get(var11);
                  var9.add(var12);

                  for (int var13 = 0; var13 < var46; var13++) {
                     if (!var47[var13] && var12.squaredDistanceTo((Entity)var4.get(var13)) <= var44) {
                        var47[var13] = true;
                        var10.add(var13);
                     }
                  }
               }

               if (var9.size() >= 2) {
                  HashSet var48 = new HashSet();
                  double var49 = 0.0;
                  double var14 = 0.0;
                  double var16 = 0.0;

                  for (ItemEntity var19 : (Iterable<ItemEntity>)(Iterable<?>)(var9)) {
                     var48.add(var19.getId());
                     clusteredEntityIds.add(var19.getId());
                     Vec3d var20 = var19.getLerpedPos(var3);
                     var49 += var20.x;
                     var14 += var20.y;
                     var16 += var20.z;
                  }

                  double var50 = var49 / var9.size();
                  double var51 = var14 / var9.size();
                  double var22 = var16 / var9.size();
                  ItemClusterManager.a var24 = a(var48);
                  if (var24 == null) {
                     var24 = new ItemClusterManager.a(++idCounter, var50, var51, var22);
                     activeClusters.put(var24.id, var24);
                  }

                  var24.targetX = var50;
                  var24.targetY = var51;
                  var24.targetZ = var22;
                  var24.entityIds.clear();
                  var24.entityIds.addAll(var48);
                  var24.updatedThisFrame = true;
                  List var25 = a(var9);
                  int var26 = 0;

                  for (ItemClusterManager.c var28 : (Iterable<ItemClusterManager.c>)(Iterable<?>)(var25)) {
                     var26 += var28.count;
                  }

                  int var52 = Math.min(var25.size(), var2);
                  ArrayList var53 = new ArrayList(var25.subList(0, var52));
                  int var29 = 0;

                  for (ItemClusterManager.c var31 : (Iterable<ItemClusterManager.c>)(Iterable<?>)(var53)) {
                     var29 += var31.count;
                  }

                  int var54 = var26 - var29;
                  int var55 = var54 > 0 ? var54 : var26;
                  var24.headerText = "+" + var55;
                  var24.visibleRows = var53;
                  float var32 = Msdf.a(MsdfManager.BOLD, var24.headerText, 6.5F);
                  float var33 = Msdf.a(MsdfManager.BOLD, 6.5F);
                  float var34 = Math.max(8.5F, Msdf.a(MsdfManager.BOLD, 6.5F)) + 2.0F;
                  float var35 = 0.0F;

                  for (ItemClusterManager.c var37 : (Iterable<ItemClusterManager.c>)(Iterable<?>)(var53)) {
                     float var38 = 12.5F + var37.textWidth;
                     if (var38 > var35) {
                        var35 = var38;
                     }
                  }

                  var24.targetW = 6.0F + Math.max(var32, var35) + 6.0F;
                  var24.targetH = 5.0F + var33 + 4.0F + var53.size() * var34 + (var53.size() > 1 ? (var53.size() - 1) * 2.0F : 0.0F) + 5.0F;
                  long var56 = System.currentTimeMillis();
                  float var57 = Math.clamp((float)(var56 - var24.lastUpdateTime) / 1000.0F, 0.001F, 0.1F);
                  var24.lastUpdateTime = var56;
                  float var39 = 1.0F - (float)Math.exp(-15.0F * var57);
                  var24.smoothX = var24.smoothX + (var24.targetX - var24.smoothX) * var39;
                  var24.smoothY = var24.smoothY + (var24.targetY - var24.smoothY) * var39;
                  var24.smoothZ = var24.smoothZ + (var24.targetZ - var24.smoothZ) * var39;
                  float var40 = 1.0F - (float)Math.exp(-13.0F * var57);
                  if (var24.smoothW < 0.0F) {
                     var24.smoothW = var24.targetW;
                     var24.smoothH = var24.targetH;
                  } else {
                     var24.smoothW = var24.smoothW + (var24.targetW - var24.smoothW) * var40;
                     var24.smoothH = var24.smoothH + (var24.targetH - var24.smoothH) * var40;
                  }

                  var24.animProgress = Math.min(1.0F, var24.animProgress + var57 * 4.5F);

                  for (int var41 = 0; var41 < var53.size(); var41++) {
                     float var42 = var24.rowAnim.getOrDefault(var41, 0.0F);
                     var24.rowAnim.put(var41, Math.min(1.0F, var42 + var57 * 6.0F));
                  }

                  renderList.add(var24);
               }
            }
         }

         activeClusters.entrySet().removeIf(var0x -> !var0x.getValue().updatedThisFrame && System.currentTimeMillis() - var0x.getValue().lastUpdateTime > 400L);
      }
   }

   private static ItemClusterManager.a a(Set<Integer> var0) {
      ItemClusterManager.a var1 = null;
      int var2 = 0;

      for (ItemClusterManager.a var4 : activeClusters.values()) {
         int var5 = 0;

         for (int var7 : var0) {
            if (var4.entityIds.contains(var7)) {
               var5++;
            }
         }

         if (var5 > var2) {
            var2 = var5;
            var1 = var4;
         }
      }

      return var1;
   }

   private static List<ItemClusterManager.c> a(List<ItemEntity> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (ItemEntity var3 : var0) {
         ItemStack var4 = var3.getStack();
         if (!var4.isEmpty()) {
            String var5 = var4.getItem().getTranslationKey() + ":" + var4.getName().getString();
            ItemClusterManager.c var6 = (ItemClusterManager.c)var1.get(var5);
            if (var6 != null) {
               var6.count = var6.count + var4.getCount();
            } else {
               List var7 = a(var4);
               float var8 = 0.0F;

               for (ItemClusterManager.b var10 : (Iterable<ItemClusterManager.b>)(Iterable<?>)(var7)) {
                  var8 += Msdf.a(MsdfManager.BOLD, var10.text, 6.5F);
               }

               int var21 = b(var4);
               ItemClusterManager.c var22 = new ItemClusterManager.c(var4.copy(), var4.getCount(), var7, var8, var21);
               var1.put(var5, var22);
            }
         }
      }

      ArrayList var12 = new ArrayList(var1.values());

      for (ItemClusterManager.c var14 : (Iterable<ItemClusterManager.c>)(Iterable<?>)(var12)) {
         String var15 = " x" + var14.count;
         float var16 = 0.0F;

         for (ItemClusterManager.b var20 : var14.segments) {
            var16 += Msdf.a(MsdfManager.BOLD, var20.text, 6.5F);
         }

         float var17 = var16 + Msdf.a(MsdfManager.BOLD, var15, 6.5F);

         try {
            Field var19 = ItemClusterManager.c.class.getDeclaredField("textWidth");
            var19.setAccessible(true);
            var19.setFloat(var14, var17);
         } catch (Exception var11) {
         }
      }

      var12.sort((var0x, var1x) -> {
         int var2 = Integer.compare(((ItemClusterManager.c)var1x).priority, ((ItemClusterManager.c)var0x).priority);
         return var2 != 0 ? var2 : Integer.compare(((ItemClusterManager.c)var1x).count, ((ItemClusterManager.c)var0x).count);
      });
      return var12;
   }

   private static int b(ItemStack var0) {
      if (ItemUtility.c(var0) || ItemUtility.e(var0) != null) {
         return 1000;
      } else if (var0.isOf(Items.PLAYER_HEAD) || var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return 900;
      } else if (var0.isOf(Items.ENCHANTED_GOLDEN_APPLE) || var0.isOf(Items.END_CRYSTAL) || var0.isOf(Items.RESPAWN_ANCHOR)) {
         return 800;
      } else if (var0.getItem() instanceof SmithingTemplateItem) {
         return 750;
      } else if (var0.isOf(Items.ENCHANTED_BOOK)) {
         return 700;
      } else if (var0.isOf(Items.NETHERITE_SWORD)
         || var0.isOf(Items.NETHERITE_AXE)
         || var0.isOf(Items.NETHERITE_PICKAXE)
         || var0.isOf(Items.NETHERITE_HELMET)
         || var0.isOf(Items.NETHERITE_CHESTPLATE)
         || var0.isOf(Items.NETHERITE_LEGGINGS)
         || var0.isOf(Items.NETHERITE_BOOTS)
         || var0.isOf(Items.NETHERITE_INGOT)
         || var0.isOf(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
         return 650;
      } else if (var0.isOf(Items.TIPPED_ARROW) || var0.isOf(Items.SPECTRAL_ARROW)) {
         return 600;
      } else if (var0.isOf(Items.POTION) || var0.isOf(Items.SPLASH_POTION) || var0.isOf(Items.LINGERING_POTION)) {
         return 550;
      } else if (var0.isOf(Items.DIAMOND_SWORD)
         || var0.isOf(Items.DIAMOND_HELMET)
         || var0.isOf(Items.DIAMOND_CHESTPLATE)
         || var0.isOf(Items.DIAMOND_LEGGINGS)
         || var0.isOf(Items.DIAMOND_BOOTS)
         || var0.isOf(Items.DIAMOND)
         || var0.isOf(Items.DIAMOND_BLOCK)) {
         return 500;
      } else if (var0.isOf(Items.GOLDEN_APPLE) || var0.isOf(Items.EXPERIENCE_BOTTLE) || var0.isOf(Items.BOW) || var0.isOf(Items.CROSSBOW)) {
         return 400;
      } else if (var0.isOf(Items.ENDER_PEARL) || var0.isOf(Items.CHORUS_FRUIT) || var0.isOf(Items.OBSIDIAN)) {
         return 350;
      } else if (!var0.isOf(Items.GOLD_INGOT)
         && !var0.isOf(Items.RAW_GOLD)
         && !var0.isOf(Items.IRON_INGOT)
         && !var0.isOf(Items.RAW_IRON)
         && !var0.isOf(Items.EMERALD)) {
         return !var0.isOf(Items.BUCKET) && !var0.isOf(Items.WATER_BUCKET) && !var0.isOf(Items.LAVA_BUCKET) ? 100 : 250;
      } else {
         return 300;
      }
   }

   public static List<ItemClusterManager.b> a(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
      if (var2 != null) {
         a(var2, var1, -1);
      } else {
         Text var3 = var0.get(DataComponentTypes.ITEM_NAME);
         if (var3 != null) {
            a(var3, var1, -1);
         } else {
            DonateItem var4 = ItemUtility.e(var0);
            if (var4 != null) {
               a(var4.getName(), var1, 55040);
            } else {
               int var5 = -1;
               Formatting var6 = var0.getRarity().getFormatting();
               if (var6.getColorValue() != null) {
                  var5 = 0xFF000000 | var6.getColorValue();
               } else if (var0.getItem() instanceof SmithingTemplateItem) {
                  var5 = 65365;
               } else if (var0.isOf(Items.ENCHANTED_BOOK)) {
                  var5 = -11141121;
               }

               a(var0.getName(), var1, var5);
            }
         }
      }

      if (var1.isEmpty()) {
         var1.add(new ItemClusterManager.b(StreamerMode.a(var0.getName().getString()), -1));
      }

      ArrayList var7 = new ArrayList();

      for (ItemClusterManager.b var9 : (Iterable<ItemClusterManager.b>)(Iterable<?>)(var1)) {
         a(var9, var7);
      }

      return var7;
   }

   private static void a(Text var0, List<ItemClusterManager.b> var1, int var2) {
      var0.visit((var2xx, var3) -> {
         if (var3 != null && !var3.isEmpty()) {
            int var4 = var2;
            TextColor var5 = var2xx.getColor();
            if (var5 != null) {
               var4 = 0xFF000000 | var5.getRgb();
            }

            a(var3, var1, var4);
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.EMPTY);
   }

   private static void a(String var0, List<ItemClusterManager.b> var1, int var2) {
      if (var0 != null && !var0.isEmpty()) {
         int var3 = var2;
         StringBuilder var4 = new StringBuilder();

         for (int var5 = 0; var5 < var0.length(); var5++) {
            char var6 = var0.charAt(var5);
            if (var6 == 167 && var5 + 1 < var0.length()) {
               char var7 = Character.toLowerCase(var0.charAt(var5 + 1));
               int var8 = a(var7);
               if (var8 != -1) {
                  if (var4.length() > 0) {
                     var1.add(new ItemClusterManager.b(StreamerMode.a(var4.toString()), var3));
                     var4.setLength(0);
                  }

                  var3 = var8 == 0 ? var2 : var8;
                  var5++;
                  continue;
               }
            }

            var4.append(var6);
         }

         if (var4.length() > 0) {
            var1.add(new ItemClusterManager.b(StreamerMode.a(var4.toString()), var3));
         }
      }
   }

   private static void a(ItemClusterManager.b var0, List<ItemClusterManager.b> var1) {
      String var2 = var0.text;
      int var3 = var0.color;
      if (!var2.contains("[★]") && !var2.contains("★") && !var2.contains("⚡")) {
         var1.add(var0);
      } else {
         int var4 = 0;

         for (int var5 = 0; var5 < var2.length(); var5++) {
            if (var2.startsWith("[★]", var5)) {
               if (var5 > var4) {
                  var1.add(new ItemClusterManager.b(var2.substring(var4, var5), var3));
               }

               var1.add(new ItemClusterManager.b("[★]", -11141121));
               var5 += 2;
               var4 = var5 + 1;
            } else if (var2.charAt(var5) == 9733) {
               if (var5 > var4) {
                  var1.add(new ItemClusterManager.b(var2.substring(var4, var5), var3));
               }

               var1.add(new ItemClusterManager.b("★", -11141121));
               var4 = var5 + 1;
            } else if (var2.charAt(var5) == 9889) {
               if (var5 > var4) {
                  var1.add(new ItemClusterManager.b(var2.substring(var4, var5), var3));
               }

               var1.add(new ItemClusterManager.b("⚡", 65365));
               var4 = var5 + 1;
            }
         }

         if (var4 < var2.length()) {
            var1.add(new ItemClusterManager.b(var2.substring(var4), var3));
         }
      }
   }

   private static int a(char var0) {
      return switch (var0) {
         case '0' -> -16777216;
         case '1' -> -16777046;
         case '2' -> -16733696;
         case '3' -> -16733526;
         case '4' -> -5636096;
         case '5' -> -5635926;
         case '6' -> 43520;
         case '7' -> -5592406;
         case '8' -> -11184811;
         case '9' -> -11184641;
         default -> -1;
         case 'a' -> -11141291;
         case 'b' -> -11141121;
         case 'c' -> -43691;
         case 'd' -> -43521;
         case 'e' -> 65365;
         case 'f' -> -1;
         case 'r' -> 0;
      };
   }

   public static void a(float var0, float var1) {
      if (!renderList.isEmpty()) {
         float var2 = RenderUtil.b();

         for (ItemClusterManager.a var4 : renderList) {
            var4.userScale = var1;
            float var5 = (float)Math.sin(var4.animProgress * Math.PI * 0.5);
            if (!(var5 <= 0.01F)) {
               Vec3d var6 = new Vec3d(var4.smoothX, var4.smoothY + 0.35, var4.smoothZ);
               float[] var7 = WorldToScreen.a(var6);
               if (var7 != null && !(var7[1] < 0.0F) && !(var7[1] > var2)) {
                  float var8 = var7[0];
                  float var9 = var7[1] - 6.0F;
                  if (var9 < 15.0F) {
                     var9 = 15.0F;
                  }

                  float var10 = var4.smoothW;
                  float var11 = var4.smoothH;
                  float var12 = var8 - var10 / 2.0F;
                  float var13 = var9 - var11;
                  float var14 = var1 * (0.85F + 0.15F * var5);
                  Matrix4f var15 = RenderUtil.d();
                  var15.translate(var8, var9, 0.0F);
                  var15.scale(var14, var14, 1.0F);
                  var15.translate(-var8, -var9, 0.0F);
                  HudConfig.a(var15, "item_cluster", var12, var13, var10, var11, 5.5F, (int)(180.0F * var5) << 24);
                  float var16 = var12 + 6.0F;
                  float var17 = var13 + 5.0F;
                  int var18 = (int)(255.0F * var5);
                  int var19 = var18 << 24 | 9344421;
                  float var20 = Msdf.a(MsdfManager.BOLD, 6.5F);
                  float var21 = Math.max(8.5F, Msdf.a(MsdfManager.BOLD, 6.5F)) + 2.0F;
                  float var22 = var17 + var20 + 4.0F;
                  Msdf.b();
                  Msdf.a(var15, MsdfManager.BOLD, var4.headerText, var16, var17, 6.5F, var19);

                  for (int var23 = 0; var23 < var4.visibleRows.size(); var23++) {
                     ItemClusterManager.c var24 = var4.visibleRows.get(var23);
                     float var25 = var5 * var4.a(var23);
                     int var26 = (int)(255.0F * var25);
                     if (var26 > 0) {
                        float var27 = var22 + var23 * (var21 + 2.0F);
                        float var28 = var27 + (var21 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
                        float var29 = var12 + 6.0F + 8.5F + 4.0F;

                        for (ItemClusterManager.b var31 : var24.segments) {
                           int var32 = var26 << 24 | var31.color & 16777215;
                           Msdf.a(var15, MsdfManager.BOLD, var31.text, var29, var28, 6.5F, var32);
                           var29 += Msdf.a(MsdfManager.BOLD, var31.text, 6.5F);
                        }

                        String var33 = " x" + var24.count;
                        int var34 = var26 << 24 | 16777215;
                        Msdf.a(var15, MsdfManager.BOLD, var33, var29, var28, 6.5F, var34);
                     }
                  }

                  Msdf.c();
               }
            }
         }
      }
   }

   public static void a(DrawContext var0, float var1, float var2) {
      if (!renderList.isEmpty()) {
         float var3 = RenderUtil.b();
         Matrix3x2fStack var4 = var0.getMatrices();
         float var5 = 2.0F / mc.getWindow().getScaleFactor();

         for (ItemClusterManager.a var7 : renderList) {
            float var8 = (float)Math.sin(var7.animProgress * Math.PI * 0.5);
            if (!(var8 <= 0.01F)) {
               Vec3d var9 = new Vec3d(var7.smoothX, var7.smoothY + 0.35, var7.smoothZ);
               float[] var10 = WorldToScreen.a(var9);
               if (var10 != null && !(var10[1] < 0.0F) && !(var10[1] > var3)) {
                  float var11 = var10[0];
                  float var12 = var10[1] - 6.0F;
                  if (var12 < 15.0F) {
                     var12 = 15.0F;
                  }

                  float var13 = var7.smoothW;
                  float var14 = var7.smoothH;
                  float var15 = var11 - var13 / 2.0F;
                  float var16 = var12 - var14;
                  float var17 = var2 * (0.85F + 0.15F * var8);
                  float var18 = 0.53125F * var17 * var5;
                  float var19 = 8.5F * var17;
                  float var20 = Msdf.a(MsdfManager.BOLD, 6.5F);
                  float var21 = Math.max(8.5F, Msdf.a(MsdfManager.BOLD, 6.5F)) + 2.0F;
                  float var22 = var16 + 5.0F + var20 + 4.0F;

                  for (int var23 = 0; var23 < var7.visibleRows.size(); var23++) {
                     ItemClusterManager.c var24 = var7.visibleRows.get(var23);
                     float var25 = var22 + var23 * (var21 + 2.0F);
                     float var26 = var15 + 6.0F;
                     float var27 = var25 + (var21 - 8.5F) / 2.0F;
                     float var28 = var11 + (var26 + 4.25F - var11) * var17;
                     float var29 = var12 + (var27 + 4.25F - var12) * var17;
                     if (!ClickGui.a(var28 - var19 / 2.0F, var29 - var19 / 2.0F, var19, var19)) {
                        var4.pushMatrix();
                        var4.translate(var28 * var5, var29 * var5);
                        var4.scale(var18, var18);
                        var4.translate(-8.0F, -8.0F);

                        try {
                           var0.drawItem(var24.stack, 0, 0);
                        } catch (Exception var31) {
                        }

                        var4.popMatrix();
                     }
                  }
               }
            }
         }
      }
   }

   public static class a {
      public final long id;
      public double targetX;
      public double targetY;
      public double targetZ;
      public double smoothX;
      public double smoothY;
      public double smoothZ;
      public float targetW;
      public float targetH;
      public float smoothW = -1.0F;
      public float smoothH = -1.0F;
      public float animProgress = 0.0F;
      public long lastUpdateTime = System.currentTimeMillis();
      public final Set<Integer> entityIds = new HashSet<>();
      public final Map<Integer, Float> rowAnim = new HashMap<>();
      public List<ItemClusterManager.c> visibleRows = new ArrayList<>();
      public String headerText = "+0";
      public float userScale = 1.0F;
      public boolean updatedThisFrame = false;

      public a(long var1, double var3, double var5, double var7) {
         this.id = var1;
         this.targetX = var3;
         this.targetY = var5;
         this.targetZ = var7;
         this.smoothX = var3;
         this.smoothY = var5;
         this.smoothZ = var7;
      }

      public float a(int var1) {
         return this.rowAnim.getOrDefault(var1, 1.0F);
      }
   }

   public record b(String text, int color) {
      public String getText() {
         return this.text;
      }

      public int getColor() {
         return this.color;
      }
   }

   public static class c {
      public final ItemStack stack;
      public int count;
      public final List<ItemClusterManager.b> segments;
      public final float textWidth;
      public final int priority;

      public c(ItemStack var1, int var2, List<ItemClusterManager.b> var3, float var4, int var5) {
         this.stack = var1;
         this.count = var2;
         this.segments = var3;
         this.textWidth = var4;
         this.priority = var5;
      }
   }
}
