package brokke.cc.core;

import brokke.cc.util.TextNormalizer;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public final class PriceParser {
   private static final Pattern UNIT_PRICE = Pattern.compile("(?i)Цена за 1 ед\\.?:\\s*\\$?([\\d\\s\\u00A0.,]+)");
   private static final Pattern TOTAL_PRICE = Pattern.compile("(?i)(?:^|\\n|\\r)Цена:\\s*\\$?([\\d\\s\\u00A0.,]+)");
   private static final Pattern FUNTIME_DOLLAR_PRICE = Pattern.compile("\\$([0-9][0-9\\s,._]*)");
   private static final Pattern DOLLAR_SUFFIX_PRICE = Pattern.compile("([0-9][0-9\\s,._]*)\\s*\\$");
   private static final Pattern GENERAL_PRICE = Pattern.compile("(?i)(?:цена|стоимость|price)[:\\s]*\\$?([0-9][0-9\\s,._]*)");
   private static final Pattern DURABILITY = Pattern.compile("(?:Прочность|Прочка|Durability)[^\\d]*([\\d]+)\\s*%");
   private static final Pattern SELLER = Pattern.compile("(?:Продавец|Seller|Игрок|Владелец)[:\\s]+([A-Za-z0-9_]{2,32})");
   private static final Pattern COUNT_X = Pattern.compile("(?i)(?:^|\\s)[xх]([0-9]{1,4})(?:$|\\s)");
   private static final Pattern COUNT_PCS = Pattern.compile("(?i)(?:^|\\s)([0-9]{1,4})\\s*(?:шт|pcs|ед)");
   private static final Pattern COUNT_LORE = Pattern.compile("(?i)(?:Количество|Кол-во)[:\\s]*([0-9]{1,4})");

   private PriceParser() {
   }

   public static Lot a(int var0, ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.getItem() == Items.BARRIER) {
            return Lot.a(var0);
         }

         String var2 = var1.getName().getString();
         String var3 = d(var1);
         int var4 = b(var1, var3);
         long var5 = a(UNIT_PRICE, var3);
         long var7 = a(TOTAL_PRICE, var3);
         if (var5 <= 0L && var7 <= 0L) {
            long var9 = c(var1);
            if (var9 > 0L) {
               var7 = var9;
            }
         }

         if (var5 <= 0L && var7 <= 0L) {
            long var18 = a(FUNTIME_DOLLAR_PRICE, var3);
            if (var18 > 0L) {
               var7 = var18;
            }
         }

         if (var5 <= 0L && var7 <= 0L) {
            long var19 = a(DOLLAR_SUFFIX_PRICE, var3);
            if (var19 > 0L) {
               var7 = var19;
            }
         }

         if (var5 <= 0L && var7 <= 0L) {
            long var20 = a(GENERAL_PRICE, var3);
            if (var20 > 0L) {
               var7 = var20;
            }
         }

         if (var5 <= 0L && var7 > 0L && var4 > 0) {
            var5 = var7 / var4;
         } else if (var5 > 0L && var7 <= 0L && var4 > 0) {
            var7 = var5 * var4;
         }

         if (var5 <= 0L) {
            return null;
         }

         String var21 = Registries.ITEM.getId(var1.getItem()).toString();
         int var10 = a(var1, var3);
         String var11 = "";
         Matcher var12 = SELLER.matcher(var3);
         if (var12.find()) {
            var11 = var12.group(1);
         }

         HashMap var13 = new HashMap();
         HashSet var14 = new HashSet();
         HashMap var15 = new HashMap();
         boolean var16 = a(var1, var13, var14, var15);
         Lot var17 = new Lot(var0, var21, var2, var3, var11, var4, var5, var7, var10, false, var16, var13, var14, var15);
         var17.potionEffects = b(var1);
         var17.sharpness = c(var1, var3);
         return var17;
      } else {
         return null;
      }
   }

   public static Lot a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty() && var0.getItem() != Items.BARRIER) {
         String var1 = var0.getName().getString();
         String var2 = d(var0);
         String var3 = Registries.ITEM.getId(var0.getItem()).toString();
         int var4 = a(var0, var2);
         HashMap var5 = new HashMap();
         HashSet var6 = new HashSet();
         HashMap var7 = new HashMap();
         boolean var8 = a(var0, var5, var6, var7);
         Lot var9 = new Lot(-1, var3, var1, var2, "", var0.getCount(), 0L, 0L, var4, false, var8, var5, var6, var7);
         var9.potionEffects = b(var0);
         var9.sharpness = c(var0, var2);
         return var9;
      } else {
         return null;
      }
   }

   private static Set<String> b(ItemStack var0) {
      HashSet var1 = new HashSet();

      try {
         PotionContentsComponent var2 = var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var2 != null) {
            for (StatusEffectInstance var4 : var2.getEffects()) {
               var1.add(var4.getEffectType().getIdAsString());
            }
         }
      } catch (Throwable var5) {
      }

      return var1;
   }

   private static boolean a(ItemStack var0, Map<String, String> var1, Set<String> var2, Map<String, String> var3) {
      boolean var4 = false;

      try {
         if (var0.get(DataComponentTypes.CUSTOM_NAME) != null) {
            var4 = true;
         }
      } catch (Throwable var16) {
      }

      try {
         ItemEnchantmentsComponent var5 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var5 != null && !var5.isEmpty()) {
            var4 = true;
         }
      } catch (Throwable var151) {
      }

      try {
         NbtComponent var19 = var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var19 == null || var19.isEmpty()) {
            return var4;
         }

         var4 = true;
         NbtCompound var6 = var19.copyNbt();
         if (var6 == null) {
            return var4;
         }

         if (var6.get("PublicBukkitValues") instanceof NbtCompound var8) {
            for (String var10 : var8.getKeys()) {
               var1.put(var10, a(var8.get(var10)));
            }
         }

         NbtElement var20 = var6.get("Enchantments");
         if (var20 instanceof NbtList) {
            for (NbtElement var11 : (NbtList)var20) {
               if (var11 instanceof NbtCompound var12) {
                  NbtElement var13 = var12.get("id");
                  if (var13 != null) {
                     String var14 = a(var13);
                     if (var14 != null && !var14.isEmpty() && !var14.startsWith("minecraft:")) {
                        var2.add(var14);
                     }
                  }
               }
            }
         }

         for (String var24 : var6.getKeys()) {
            if (!var24.equals("PublicBukkitValues")
               && !var24.equals("Enchantments")
               && !var24.equals("display")
               && !var24.equals("HideFlags")
               && !var24.startsWith("VV|")) {
               NbtElement var25 = var6.get(var24);
               if (var25 != null) {
                  if (var25 instanceof NbtCompound var26) {
                     for (String var28 : var26.getKeys()) {
                        NbtElement var15 = var26.get(var28);
                        if (var15 != null && !(var15 instanceof NbtCompound) && !(var15 instanceof NbtList)) {
                           var3.put(var24 + "." + var28, a(var15));
                        }
                     }
                  } else if (!(var25 instanceof NbtList)) {
                     var3.put(var24, a(var25));
                  }
               }
            }
         }
      } catch (Throwable var17) {
      }

      return var4;
   }

   private static String a(NbtElement var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.toString();
      if (!(var0 instanceof NbtString)) {
         if (!var1.isEmpty()) {
            char var2 = var1.charAt(var1.length() - 1);
            if ("bBsSlLfFdD".indexOf(var2) >= 0 && var1.length() > 1) {
               String var3 = var1.substring(0, var1.length() - 1);
               if (var3.matches("-?\\d+(\\.\\d+)?")) {
                  return var3;
               }
            }
         }

         return var1;
      } else {
         if (var1.length() >= 2 && var1.charAt(0) == '"' && var1.charAt(var1.length() - 1) == '"') {
            var1 = var1.substring(1, var1.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
         } else if (var1.length() >= 2 && var1.charAt(0) == '\'' && var1.charAt(var1.length() - 1) == '\'') {
            var1 = var1.substring(1, var1.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
         }

         return var1;
      }
   }

   private static int a(ItemStack var0, String var1) {
      Matcher var2 = DURABILITY.matcher(var1);
      if (var2.find()) {
         try {
            return a(Integer.parseInt(var2.group(1)));
         } catch (NumberFormatException var4) {
         }
      }

      if (var0.isDamageable() && var0.getMaxDamage() > 0) {
         int var3 = var0.getMaxDamage() - var0.getDamage();
         return a((int)Math.round(var3 * 100.0 / var0.getMaxDamage()));
      } else {
         return 100;
      }
   }

   private static int a(int var0) {
      return Math.max(0, Math.min(100, var0));
   }

   private static int b(ItemStack var0, String var1) {
      Matcher var2 = COUNT_X.matcher(var1);
      if (var2.find()) {
         try {
            int var3 = Integer.parseInt(var2.group(1));
            if (var3 > 0) {
               return var3;
            }
         } catch (NumberFormatException var8) {
         }
      }

      Matcher var9 = COUNT_PCS.matcher(var1);
      if (var9.find()) {
         try {
            int var4 = Integer.parseInt(var9.group(1));
            if (var4 > 0) {
               return var4;
            }
         } catch (NumberFormatException var7) {
         }
      }

      Matcher var10 = COUNT_LORE.matcher(var1);
      if (var10.find()) {
         try {
            int var5 = Integer.parseInt(var10.group(1));
            if (var5 > 0) {
               return var5;
            }
         } catch (NumberFormatException var6) {
         }
      }

      return Math.max(1, var0.getCount());
   }

   private static long c(ItemStack var0) {
      try {
         ComponentMap var1 = var0.getComponents();
         if (var1 != null) {
            String var2 = var1.toString();
            int var3 = var2.indexOf("literal{ $");
            if (var3 != -1) {
               int var4 = var2.indexOf("}[style={color=", var3);
               if (var4 != -1) {
                  String var5 = var2.substring(var3 + 10, var4).replaceAll("[^0-9]", "");
                  if (!var5.isEmpty()) {
                     long var6 = Long.parseLong(var5);
                     if (var6 > 0L) {
                        return var6;
                     }
                  }
               }
            }
         }
      } catch (Throwable var8) {
      }

      return -1L;
   }

   private static long a(Pattern var0, String var1) {
      Matcher var2 = var0.matcher(var1);
      if (!var2.find()) {
         return -1L;
      }

      String var3 = var2.group(1).replaceAll("[\\s\\u00A0.,_]+", "");
      if (var3.isEmpty()) {
         return -1L;
      }

      try {
         return Long.parseLong(var3);
      } catch (NumberFormatException var5) {
         return -1L;
      }
   }

   private static String d(ItemStack var0) {
      StringBuilder var1 = new StringBuilder();
      MinecraftClient var2 = MinecraftClient.getInstance();

      try {
         TooltipContext var3 = var2.world != null ? TooltipContext.create(var2.world) : TooltipContext.DEFAULT;
         List var4 = var0.getTooltip(var3, var2.player, TooltipType.ADVANCED);
         if (var4 != null) {
            for (Text var6 : (Iterable<Text>)(Iterable<?>)(var4)) {
               if (var6 != null) {
                  var1.append(var6.getString()).append('\n');
               }
            }
         }
      } catch (Throwable var10) {
      }

      LoreComponent var11 = var0.get(DataComponentTypes.LORE);
      if (var11 != null) {
         for (Text var14 : var11.lines()) {
            if (var14 != null) {
               var1.append(var14.getString()).append('\n');
            }
         }
      }

      try {
         ItemEnchantmentsComponent var13 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var13 != null) {
            for (Entry var16 : var13.getEnchantmentEntries()) {
               String var7 = ((RegistryEntry<?>)var16.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               int var8 = var16.getIntValue();
               if (var7.contains("sharpness") || var7.contains("острота")) {
                  var1.append("Острота ").append(var8).append('\n');
                  var1.append("Острота ").append(b(var8)).append('\n');
                  var1.append("Sharpness ").append(var8).append('\n');
                  var1.append("Sharpness ").append(b(var8)).append('\n');
               }
            }
         }
      } catch (Throwable var9) {
      }

      return var1.toString();
   }

   private static int c(ItemStack var0, String var1) {
      try {
         ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var2 != null) {
            for (Entry var4 : var2.getEnchantmentEntries()) {
               String var5 = ((RegistryEntry<?>)var4.getKey()).getKey().map(var0x -> var0x.getValue().getPath()).orElse("");
               if (var5.contains("sharpness") || var5.contains("острота")) {
                  return var4.getIntValue();
               }
            }
         }
      } catch (Throwable var6) {
      }

      String var7 = var1.toLowerCase(Locale.ROOT);
      String var8 = TextNormalizer.a(var1);
      if (var8.contains("острота 10")
         || var8.contains("sharpness 10")
         || var8.contains("острота x")
         || var8.contains("sharpness x")
         || var7.contains("острота 10")
         || var7.contains("sharpness 10")
         || var7.contains("острота x")
         || var7.contains("sharpness x")
         || var7.contains("острота ⅹ")
         || var7.contains("sharpness ⅹ")) {
         return 10;
      } else if (var8.contains("острота 9")
         || var8.contains("sharpness 9")
         || var8.contains("острота ix")
         || var8.contains("sharpness ix")
         || var7.contains("острота 9")
         || var7.contains("sharpness 9")
         || var7.contains("острота ix")
         || var7.contains("sharpness ix")
         || var7.contains("острота ⅸ")
         || var7.contains("sharpness ⅸ")) {
         return 9;
      } else if (var8.contains("острота 8")
         || var8.contains("sharpness 8")
         || var8.contains("острота viii")
         || var8.contains("sharpness viii")
         || var7.contains("острота 8")
         || var7.contains("sharpness 8")
         || var7.contains("острота viii")
         || var7.contains("sharpness viii")
         || var7.contains("острота ⅷ")
         || var7.contains("sharpness ⅷ")) {
         return 8;
      } else if (var8.contains("острота 7")
         || var8.contains("sharpness 7")
         || var8.contains("острота vii")
         || var8.contains("sharpness vii")
         || var7.contains("острота 7")
         || var7.contains("sharpness 7")
         || var7.contains("острота vii")
         || var7.contains("sharpness vii")
         || var7.contains("острота Ⅶ")
         || var7.contains("sharpness Ⅶ")
         || var7.contains("острота ⅶ")
         || var7.contains("sharpness ⅶ")) {
         return 7;
      } else if (var8.contains("острота 6")
         || var8.contains("sharpness 6")
         || var8.contains("острота vi")
         || var8.contains("sharpness vi")
         || var7.contains("острота 6")
         || var7.contains("sharpness 6")
         || var7.contains("острота vi")
         || var7.contains("sharpness vi")
         || var7.contains("острота ⅵ")
         || var7.contains("sharpness ⅵ")) {
         return 6;
      } else if (var8.contains("острота 5")
         || var8.contains("sharpness 5")
         || var8.contains("острота v")
         || var8.contains("sharpness v")
         || var7.contains("острота 5")
         || var7.contains("sharpness 5")
         || var7.contains("острота v")
         || var7.contains("sharpness v")
         || var7.contains("острота ⅴ")
         || var7.contains("sharpness ⅴ")) {
         return 5;
      } else if (var8.contains("острота 4")
         || var8.contains("sharpness 4")
         || var8.contains("острота iv")
         || var8.contains("sharpness iv")
         || var7.contains("острота 4")
         || var7.contains("sharpness 4")
         || var7.contains("острота iv")
         || var7.contains("sharpness iv")
         || var7.contains("острота ⅳ")
         || var7.contains("sharpness ⅳ")) {
         return 4;
      } else if (var8.contains("острота 3")
         || var8.contains("sharpness 3")
         || var8.contains("острота iii")
         || var8.contains("sharpness iii")
         || var7.contains("острота 3")
         || var7.contains("sharpness 3")
         || var7.contains("острота iii")
         || var7.contains("sharpness iii")
         || var7.contains("острота ⅲ")
         || var7.contains("sharpness ⅲ")) {
         return 3;
      } else if (!var8.contains("острота 2")
         && !var8.contains("sharpness 2")
         && !var8.contains("острота ii")
         && !var8.contains("sharpness ii")
         && !var7.contains("острота 2")
         && !var7.contains("sharpness 2")
         && !var7.contains("острота ii")
         && !var7.contains("sharpness ii")
         && !var7.contains("острота ⅱ")
         && !var7.contains("sharpness ⅱ")) {
         return !var8.contains("острота 1")
               && !var8.contains("sharpness 1")
               && !var8.contains("острота i")
               && !var8.contains("sharpness i")
               && !var7.contains("острота 1")
               && !var7.contains("sharpness 1")
               && !var7.contains("острота i")
               && !var7.contains("sharpness i")
               && !var7.contains("острота ⅰ")
               && !var7.contains("sharpness ⅰ")
            ? 0
            : 1;
      } else {
         return 2;
      }
   }

   private static String b(int var0) {
      return switch (var0) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(var0);
      };
   }
}
