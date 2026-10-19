package brokke.cc.core;

import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;

public class DescriptionCondition implements ItemFilter {
   private final String description;
   private ItemType type;
   private int requiredLevel;

   public DescriptionCondition(String var1) {
      this(var1, 0, ItemType.ON);
   }

   public DescriptionCondition(String var1, int var2) {
      this(var1, var2, ItemType.ON);
   }

   public DescriptionCondition(String var1, int var2, ItemType var3) {
      this.description = var1;
      this.type = var3;
      this.requiredLevel = var2;
   }

   public String getDescription() {
      return this.description;
   }

   public ItemType getType() {
      return this.type;
   }

   public int getRequiredLevel() {
      return this.requiredLevel;
   }

   @Override
   public boolean a(ItemStack var1) {
      if (this.type == ItemType.OFF) {
         return true;
      }

      MinecraftClient var2 = MinecraftClient.getInstance();
      String var3 = var1.getTooltip(TooltipContext.DEFAULT, var2.player, TooltipType.BASIC)
         .stream()
         .skip(1L)
         .map(var0 -> var0.getString().replaceAll("§.", "").toLowerCase().replaceAll("\\s+", " ").trim())
         .collect(Collectors.joining(" "));
      String var4 = this.description.replaceAll("§.", "").toLowerCase().replaceAll("\\s+", " ").trim();
      if (this.type == ItemType.DENY) {
         return !var3.contains(var4);
      }

      if (!var3.contains(var4)) {
         return false;
      }

      if (this.requiredLevel == 0) {
         return true;
      }

      String var5 = var3.substring(var3.indexOf(var4) + var4.length()).trim();
      int var6 = var5.indexOf(32);
      String var7 = var6 > 0 ? var5.substring(0, var6) : var5;
      int var8 = a(var7);
      return Math.max(1, var8) >= this.requiredLevel;
   }

   private static int a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var6 = var0.trim().toUpperCase();

         return switch (var6) {
            case "I", "1" -> {
               byte var17 = 1;
               byte var19 = var17;
               yield var19;
            }
            case "II", "2" -> {
               byte var16 = 2;
               byte var18 = var16;
               yield var18;
            }
            case "III", "3" -> {
               byte var15 = 3;
               byte var17 = var15;
               yield var17;
            }
            case "IV", "4" -> {
               byte var14 = 4;
               byte var16 = var14;
               yield var16;
            }
            case "V", "5" -> {
               byte var13 = 5;
               byte var15 = var13;
               yield var15;
            }
            case "VI", "6" -> {
               byte var12 = 6;
               byte var14 = var12;
               yield var14;
            }
            case "VII", "7" -> {
               byte var11 = 7;
               byte var13 = var11;
               yield var13;
            }
            case "VIII", "8" -> {
               byte var10 = 8;
               byte var12 = var10;
               yield var12;
            }
            case "IX", "9" -> {
               byte var9 = 9;
               byte var11 = var9;
               yield var11;
            }
            case "X", "10" -> {
               byte var8 = 10;
               byte var10 = var8;
               yield var10;
            }
            default -> {
               label90: {
                  int var7;
                  try {
                     var7 = Integer.parseInt(var6);
                  } catch (Exception var5) {
                     byte var3 = 0;
                     byte var4 = var3;
                     yield var4;
                  }

                  int var9 = var7;
                  yield var9;
               }
            }
         };
      } else {
         return 0;
      }
   }
}
