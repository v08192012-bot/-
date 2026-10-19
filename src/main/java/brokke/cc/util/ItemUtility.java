package brokke.cc.util;

import brokke.cc.cosmetic.DonateItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ItemUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)[§&][0-9a-z]");
   private static final Pattern UNICODE_RANGE_PATTERN = Pattern.compile("[\\uE000-\\uF8FF]");
   private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[\\?\\uFFFD]+");
   private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
   private static final Pattern TALISMAN_REGEX = Pattern.compile("(?i)((?:[a-zA-Zа-яА-Я0-9_-]+\\s+)*талисман(?:\\s+[a-zA-Zа-яА-Я0-9_-]+)*)");
   private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
   private static final Pattern DURA_SLASH_PATTERN = Pattern.compile("(\\d[\\d\\s.,]*)\\s*[/|\\\\]\\s*(\\d[\\d\\s.,]*)");
   private static final Pattern DURA_PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
   private static final Pattern DURA_SINGLE_PATTERN = Pattern.compile("[:：\\-]?\\s*(\\d[\\d\\s.,]+)");
   private static final Pattern DURA_GENERIC_PATTERN = Pattern.compile("(\\d{2,})\\s*[/|\\\\]\\s*(\\d{2,})");

   public static List<ItemStack> a(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      ContainerComponent var2 = var0.get(DataComponentTypes.CONTAINER);
      if (var2 == null) {
         BundleContentsComponent var6 = var0.get(DataComponentTypes.BUNDLE_CONTENTS);
         if (var6 != null) {
            for (ItemStack var5 : var6.iterate()) {
               var1.add(var5);
            }
         }

         return var1;
      } else {
         for (ItemStack var4 : var2.iterateNonEmpty()) {
            var1.add(var4);
         }

         return var1;
      }
   }

   public static NbtCompound b(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         NbtComponent var1 = var0.get(DataComponentTypes.CUSTOM_DATA);
         return var1 != null ? var1.copyNbt() : null;
      } else {
         return null;
      }
   }

   public static boolean a(ItemStack var0, String var1) {
      NbtCompound var2 = b(var0);
      if (var2 == null) {
         return false;
      } else if (var2.contains("don-item")) {
         String var3 = var2.getString("don-item").orElse("");
         return var3.contains(var1);
      } else {
         return false;
      }
   }

   public static boolean c(ItemStack var0) {
      NbtCompound var1 = b(var0);
      return var1 != null && var1.contains("don-item");
   }

   public static String d(ItemStack var0) {
      NbtCompound var1 = b(var0);
      if (var1 == null) {
         return "";
      } else if (var1.contains("don-item")) {
         return var1.getString("don-item").orElse("");
      } else {
         return var1.contains("spooky-item") ? var1.getString("spooky-item").orElse("") : "";
      }
   }

   public static DonateItem e(ItemStack var0) {
      if (var0 != null && !var0.isEmpty() && !var0.isOf(Items.PLAYER_HEAD)) {
         String var1 = d(var0);
         if (!var1.isEmpty()) {
            for (DonateItem var5 : DonateItem.values()) {
               for (String var9 : var5.getNbt()) {
                  if (var1.equalsIgnoreCase(var9)) {
                     return var5;
                  }
               }
            }
         }

         String var11 = var0.getName().getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim().toLowerCase();
         if (!var11.isEmpty()) {
            for (DonateItem var15 : DonateItem.values()) {
               for (String var10 : var15.getNbt()) {
                  if (var11.contains(var10.toLowerCase())) {
                     return var15;
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static int f(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         DonateItem var1 = e(var0);
         if (var1 != null) {
            return 12 - var1.getTotem();
         } else {
            String var2 = var0.getName().getString().toLowerCase();
            if (var2.contains("infinity") || var2.contains("инфинити")) {
               return 11;
            } else if (var2.contains("eternity") || var2.contains("этернити")) {
               return 10;
            } else if (var2.contains("stinger") || var2.contains("стингер")) {
               return 9;
            } else if (var2.contains("satir") || var2.contains("сатир")) {
               return 8;
            } else if (var2.contains("mythical") || var2.contains("мифическ")) {
               return 7;
            } else if (var2.contains("legendary") || var2.contains("легендарн")) {
               return 6;
            } else if (var2.contains("epic") || var2.contains("эпическ")) {
               return 5;
            } else if (var2.contains("крушител") || var2.contains("krush")) {
               return 11;
            } else if (var2.contains("карател") || var2.contains("karatel")) {
               return 10;
            } else if (var2.contains("гармони") || var2.contains("garmon")) {
               return 9;
            } else if (var2.contains("дедал") || var2.contains("dedal")) {
               return 8;
            } else if (var2.contains("ехидн") || var2.contains("ehidna")) {
               return 7;
            } else if (var2.contains("гран") || var2.contains("gran")) {
               return 6;
            } else if (var2.contains("тритон") || var2.contains("triton")) {
               return 5;
            } else if (!var2.contains("феникс") && !var2.contains("fenix")) {
               return var0.hasEnchantments() ? 1 : 0;
            } else {
               return 4;
            }
         }
      } else {
         return -1;
      }
   }

   public static boolean g(ItemStack var0) {
      if (var0 != null && !var0.isEmpty() && var0.isOf(Items.PLAYER_HEAD)) {
         String var1 = var0.getName().getString().toLowerCase();
         if (!var1.contains("сфер") && !var1.contains("sphere")) {
            NbtCompound var2 = b(var0);
            if (var2 != null) {
               if (var2.contains("sphereEffect")) {
                  return true;
               }

               String var3 = d(var0).toLowerCase();
               if (var3.startsWith("sph-") || var3.startsWith("sphere-") || var3.startsWith("attribute-item-s")) {
                  return true;
               }
            }

            LoreComponent var7 = var0.get(DataComponentTypes.LORE);
            if (var7 != null) {
               for (Text var5 : var7.lines()) {
                  String var6 = var5.getString().toLowerCase();
                  if (var6.contains("сфер") || var6.contains("sphere")) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean h(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0.getName().getString().toLowerCase();
         if (!var1.contains("шлем солнца") && !var1.contains("sunhelmet")) {
            NbtCompound var2 = b(var0);
            if (var2 != null && var2.contains("kringeItems")) {
               NbtCompound var3 = var2.getCompound("kringeItems").orElse(null);
               if (var3 != null && "SunHelmet".equals(var3.getString("type").orElse(""))) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static EquipmentSlot i(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return null;
      } else if (var0.isIn(ItemTags.HEAD_ARMOR)) {
         return EquipmentSlot.HEAD;
      } else if (var0.isIn(ItemTags.CHEST_ARMOR)) {
         return EquipmentSlot.CHEST;
      } else if (var0.isIn(ItemTags.LEG_ARMOR)) {
         return EquipmentSlot.LEGS;
      } else {
         return var0.isIn(ItemTags.FOOT_ARMOR) ? EquipmentSlot.FEET : null;
      }
   }

   public static int j(ItemStack var0) {
      EquipmentSlot var1 = i(var0);
      if (var1 == null) {
         return 0;
      }

      AttributeModifiersComponent var2 = var0.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      return (int)Math.round(var2.applyOperations(EntityAttributes.ARMOR, 0.0, var1));
   }

   public static int k(ItemStack var0) {
      EquipmentSlot var1 = i(var0);
      if (var1 == null) {
         return 0;
      }

      AttributeModifiersComponent var2 = var0.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      return (int)var2.applyOperations(EntityAttributes.ARMOR_TOUGHNESS, 0.0, var1);
   }

   public static boolean l(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0.getName().getString().toLowerCase();
         if (!var1.contains("cerber") && !var1.contains("цербер") && !var1.contains("ᴄᴇʀʙᴇʀ")) {
            String var2 = d(var0).toLowerCase();
            if (!var2.contains("cerber") && !var2.contains("цербер") && !var2.contains("ᴄᴇʀʙᴇʀ")) {
               LoreComponent var3 = var0.get(DataComponentTypes.LORE);
               if (var3 != null) {
                  for (Text var5 : var3.lines()) {
                     String var6 = var5.getString().toLowerCase();
                     if (var6.contains("cerber") || var6.contains("цербер") || var6.contains("ᴄᴇʀʙᴇʀ")) {
                        return true;
                     }
                  }
               }

               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static int m(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         NbtCompound var1 = b(var0);
         if (var1 != null && var1.contains("sphereEffect")) {
            NbtCompound var2 = var1.getCompound("sphereEffect").orElse(null);
            if (var2 != null) {
               String var3 = var2.getString("effects").orElse("");
               int var4 = var3.indexOf("hms-speed");
               if (var4 != -1) {
                  int var5 = var3.indexOf("\"lvl\":", var4);
                  if (var5 != -1) {
                     int var6 = var5 + 6;
                     int var7 = var6;

                     while (var7 < var3.length() && Character.isDigit(var3.charAt(var7))) {
                        var7++;
                     }

                     if (var7 > var6) {
                        try {
                           return Integer.parseInt(var3.substring(var6, var7));
                        } catch (Exception var10) {
                        }
                     }
                  }
               }
            }
         }

         LoreComponent var11 = var0.get(DataComponentTypes.LORE);
         if (var11 != null) {
            for (Text var13 : var11.lines()) {
               String var14 = var13.getString();
               if (var14.toLowerCase().contains("скорост")) {
                  for (char var9 : var14.toCharArray()) {
                     if (Character.isDigit(var9)) {
                        return Character.getNumericValue(var9);
                     }
                  }
               }
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static int n(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return 999;
      }

      if (l(var0)) {
         return 200;
      }

      int var1 = m(var0);
      return var1 >= 3 ? 10 - var1 : r(var0);
   }

   public static String a(String var0) {
      if (var0 == null) {
         return "";
      }

      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         int var3 = var0.codePointAt(var2);
         if (Character.isSupplementaryCodePoint(var3)) {
            var2++;
            if (var3 >= 119808 && var3 <= 120831) {
               if (var3 >= 119808 && var3 <= 119833) {
                  var1.append((char)(65 + (var3 - 119808)));
               } else if (var3 >= 119834 && var3 <= 119859) {
                  var1.append((char)(97 + (var3 - 119834)));
               } else if (var3 >= 119860 && var3 <= 119885) {
                  var1.append((char)(65 + (var3 - 119860)));
               } else if (var3 >= 119886 && var3 <= 119911) {
                  var1.append((char)(97 + (var3 - 119886)));
               } else if (var3 >= 119912 && var3 <= 119937) {
                  var1.append((char)(65 + (var3 - 119912)));
               } else if (var3 >= 119938 && var3 <= 119963) {
                  var1.append((char)(97 + (var3 - 119938)));
               } else if (var3 >= 119964 && var3 <= 119989) {
                  var1.append((char)(65 + (var3 - 119964)));
               } else if (var3 >= 119990 && var3 <= 120015) {
                  var1.append((char)(97 + (var3 - 119990)));
               } else if (var3 >= 120016 && var3 <= 120041) {
                  var1.append((char)(65 + (var3 - 120016)));
               } else if (var3 >= 120042 && var3 <= 120067) {
                  var1.append((char)(97 + (var3 - 120042)));
               } else if (var3 >= 120068 && var3 <= 120093) {
                  var1.append((char)(65 + (var3 - 120068)));
               } else if (var3 >= 120094 && var3 <= 120119) {
                  var1.append((char)(97 + (var3 - 120094)));
               } else if (var3 >= 120120 && var3 <= 120145) {
                  var1.append((char)(65 + (var3 - 120120)));
               } else if (var3 >= 120146 && var3 <= 120171) {
                  var1.append((char)(97 + (var3 - 120146)));
               } else if (var3 >= 120172 && var3 <= 120197) {
                  var1.append((char)(65 + (var3 - 120172)));
               } else if (var3 >= 120198 && var3 <= 120223) {
                  var1.append((char)(97 + (var3 - 120198)));
               } else if (var3 >= 120224 && var3 <= 120249) {
                  var1.append((char)(65 + (var3 - 120224)));
               } else if (var3 >= 120250 && var3 <= 120275) {
                  var1.append((char)(97 + (var3 - 120250)));
               } else if (var3 >= 120276 && var3 <= 120301) {
                  var1.append((char)(65 + (var3 - 120276)));
               } else if (var3 >= 120302 && var3 <= 120327) {
                  var1.append((char)(97 + (var3 - 120302)));
               } else if (var3 >= 120328 && var3 <= 120353) {
                  var1.append((char)(65 + (var3 - 120328)));
               } else if (var3 >= 120354 && var3 <= 120379) {
                  var1.append((char)(97 + (var3 - 120354)));
               } else if (var3 >= 120380 && var3 <= 120405) {
                  var1.append((char)(65 + (var3 - 120380)));
               } else if (var3 >= 120406 && var3 <= 120431) {
                  var1.append((char)(97 + (var3 - 120406)));
               } else if (var3 >= 120432 && var3 <= 120457) {
                  var1.append((char)(65 + (var3 - 120432)));
               } else if (var3 >= 120458 && var3 <= 120483) {
                  var1.append((char)(97 + (var3 - 120458)));
               } else if (var3 >= 120782 && var3 <= 120831) {
                  var1.append((char)(48 + (var3 - 120782) % 10));
               }
            }
         } else {
            char var4 = (char)var3;
            switch (var4) {
               case '¦':
               case 'ı':
               case 'ǀ':
               case 'ɪ':
               case 'І':
               case 'Ї':
               case 'і':
               case 'ї':
               case 'ᶦ':
               case 'ℐ':
               case 'ℑ':
               case 'ℒ':
                  var1.append('I');
                  break;
               case 'Ø':
               case 'ø':
               case 'ᴏ':
               case 'ᵒ':
                  var1.append('O');
                  break;
               case 'Ŀ':
               case 'ŀ':
               case 'Ł':
               case 'ł':
               case 'ʟ':
               case 'ˡ':
                  var1.append('L');
                  break;
               case 'Ş':
               case 'ş':
               case 'ˢ':
               case 'Ѕ':
               case 'ѕ':
               case 'ꜱ':
                  var1.append('S');
                  break;
               case 'Ɓ':
               case 'Ƃ':
               case 'ʙ':
               case 'ᵇ':
                  var1.append('B');
                  break;
               case 'Ɖ':
               case 'Ɗ':
               case 'ԁ':
               case 'ᴅ':
               case 'ᵈ':
                  var1.append('D');
                  break;
               case 'Ǝ':
               case 'ǝ':
               case 'ᴇ':
               case 'ᵉ':
               case 'ℇ':
               case 'ℰ':
                  var1.append('E');
                  break;
               case 'Ƒ':
               case 'ƒ':
               case 'Ϝ':
               case 'ϝ':
               case 'Ғ':
               case 'ғ':
               case 'ᶠ':
               case 'ℱ':
               case 'Ⅎ':
               case 'ꜰ':
               case 'Ꞙ':
               case 'ꞙ':
               case 'ꟻ':
                  var1.append('F');
                  break;
               case 'Ɠ':
               case 'ɢ':
               case 'Ԍ':
               case 'ԍ':
               case 'ᵍ':
                  var1.append('G');
                  break;
               case 'ǫ':
               case 'ϙ':
               case 'ℚ':
                  var1.append('Q');
                  break;
               case 'ɐ':
               case 'ᴀ':
               case 'ᵃ':
               case 'Ɐ':
                  var1.append('A');
                  break;
               case 'ɴ':
               case 'ᵑ':
               case 'ℕ':
                  var1.append('N');
                  break;
               case 'ʀ':
               case 'ʁ':
               case 'ʳ':
               case 'ℛ':
               case 'ℜ':
               case 'ℝ':
                  var1.append('R');
                  break;
               case 'ʏ':
               case 'ʸ':
                  var1.append('Y');
                  break;
               case 'ʜ':
               case 'Һ':
               case 'һ':
               case 'ℋ':
               case 'ℌ':
               case 'ℍ':
                  var1.append('H');
                  break;
               case 'ʲ':
               case 'Ј':
               case 'ј':
               case 'ᴊ':
                  var1.append('J');
                  break;
               case 'ʷ':
               case 'ᴡ':
                  var1.append('W');
                  break;
               case 'ˣ':
                  var1.append('X');
                  break;
               case 'Қ':
               case 'қ':
               case 'ᴋ':
               case 'ᵏ':
                  var1.append('K');
                  break;
               case 'ᴄ':
               case 'ᶜ':
               case 'ℂ':
               case 'ℭ':
                  var1.append('C');
                  break;
               case 'ᴍ':
               case 'ᵐ':
               case 'ℳ':
                  var1.append('M');
                  break;
               case 'ᴘ':
               case 'ᵖ':
               case 'ℙ':
                  var1.append('P');
                  break;
               case 'ᴙ':
                  var1.append('Я');
                  break;
               case 'ᴛ':
               case 'ᵗ':
                  var1.append('T');
                  break;
               case 'ᴜ':
               case 'ᵘ':
                  var1.append('U');
                  break;
               case 'ᴠ':
               case 'ᵛ':
                  var1.append('V');
                  break;
               case 'ᴢ':
               case 'ℤ':
               case 'ℨ':
                  var1.append('Z');
                  break;
               case 'ᴦ':
                  var1.append('Г');
                  break;
               case 'ᴧ':
               case 'ᴫ':
                  var1.append('Л');
                  break;
               case 'ᴨ':
                  var1.append('П');
                  break;
               case 'ᴩ':
                  var1.append('Р');
                  break;
               default:
                  if (var4 >= 'Ａ' && var4 <= 'Ｚ') {
                     var1.append((char)(65 + (var4 - 'Ａ')));
                  } else if (var4 >= 'ａ' && var4 <= 'ｚ') {
                     var1.append((char)(97 + (var4 - 'ａ')));
                  } else if (var4 >= '０' && var4 <= '９') {
                     var1.append((char)(48 + (var4 - '０')));
                  } else {
                     var1.append(var4);
                  }
            }
         }
      }

      return var1.toString();
   }

   public static String b(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = a(var0);
      var1 = COLOR_CODE_PATTERN.matcher(var1).replaceAll("");
      var1 = UNICODE_RANGE_PATTERN.matcher(var1).replaceAll("");
      var1 = INVALID_CHARS_PATTERN.matcher(var1).replaceAll(" ");
      var1 = var1.replaceAll("^[\\s\\-\\.\\,\\[\\]\\(\\)\\*#♪♫☿з⭐★✦✧<>]+", "");
      var1 = var1.replaceAll("[\\s\\-\\.\\,\\[\\]\\(\\)\\*#♪♫☿з⭐★✦✧<>]+$", "");
      return MULTI_SPACE_PATTERN.matcher(var1.trim()).replaceAll(" ");
   }

   public static boolean o(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (var0.hasEnchantments()) {
         return true;
      }

      ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
      if (var1 != null && !var1.isEmpty()) {
         return true;
      }

      ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
      if (var2 != null && !var2.isEmpty()) {
         return true;
      }

      Boolean var3 = var0.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
      if (Boolean.TRUE.equals(var3)) {
         return true;
      }

      if (var0.hasGlint()) {
         return true;
      }

      NbtCompound var4 = b(var0);
      return var4 != null && (var4.contains("Enchantments") || var4.contains("ench") || var4.contains("StoredEnchantments") || var4.contains("enchantments"));
   }

   public static String a(int var0) {
      return switch (var0) {
         case 1 -> "Талисман Мрака";
         case 2 -> "Талисман Вихря";
         case 3 -> "Талисман Демона";
         case 4 -> "Талисман Раздора";
         case 5 -> "Талисман Ярости";
         case 6 -> "Талисман Крушителя";
         case 7 -> "Талисман Карателя";
         case 8 -> "Талисман Тирана";
         case 9 -> "Талисман Гринча";
         case 10 -> "Талисман Ареса";
         case 11 -> "Талисман Хаоса";
         case 12 -> "Талисман Титана";
         case 13 -> "Талисман Гидры";
         case 14 -> "Талисман Бестии";
         case 15 -> "Талисман Икара";
         case 16 -> "Талисман Сатиры";
         case 17 -> "Талисман Эриды";
         case 18 -> "Талисман Мороза";
         case 19 -> "Талисман Гармонии";
         case 20 -> "Талисман Дедала";
         case 21 -> "Талисман Ехидна";
         case 22 -> "Талисман Грани";
         case 23 -> "Талисман Тритона";
         case 24 -> "Талисман Феникс";
         case 25 -> "Талисман INFINITY";
         case 26 -> "Талисман ETERNITY";
         case 27 -> "Талисман STINGER";
         default -> null;
      };
   }

   public static String c(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase(Locale.ROOT);
         if (var1.contains("infinity") || var1.contains("инфинити")) {
            return "Талисман INFINITY";
         } else if (var1.contains("eternity") || var1.contains("этернити") || var1.contains("этернети") || var1.contains("вечност")) {
            return "Талисман ETERNITY";
         } else if (var1.contains("stinger") || var1.contains("стингер")) {
            return "Талисман STINGER";
         } else if (var1.contains("карател") || var1.contains("karatel")) {
            return "Талисман Карателя";
         } else if (var1.contains("крушител") || var1.contains("krush")) {
            return "Талисман Крушителя";
         } else if (var1.contains("ярост") || var1.contains("yarost") || var1.contains("rage")) {
            return "Талисман Ярости";
         } else if (var1.contains("раздор") || var1.contains("razdor")) {
            return "Талисман Раздора";
         } else if (var1.contains("демон") || var1.contains("demon")) {
            return "Талисман Демона";
         } else if (var1.contains("вихр") || var1.contains("vihr") || var1.contains("vortex")) {
            return "Талисман Вихря";
         } else if (var1.contains("мрак") || var1.contains("mrak")) {
            return "Талисман Мрака";
         } else if (var1.contains("тиран") || var1.contains("tiran")) {
            return "Талисман Тирана";
         } else if (var1.contains("гринч") || var1.contains("grinch")) {
            return "Талисман Гринча";
         } else if (var1.contains("арес") || var1.contains("ares")) {
            return "Талисман Ареса";
         } else if (var1.contains("хаос") || var1.contains("haos") || var1.contains("chaos")) {
            return "Талисман Хаоса";
         } else if (var1.contains("титан") || var1.contains("titan")) {
            return "Талисман Титана";
         } else if (var1.contains("гидр") || var1.contains("gidr") || var1.contains("hydra")) {
            return "Талисман Гидры";
         } else if (var1.contains("бести") || var1.contains("besti")) {
            return "Талисман Бестии";
         } else if (var1.contains("икар") || var1.contains("ikar")) {
            return "Талисман Икара";
         } else if (var1.contains("сатир") || var1.contains("satir")) {
            return "Талисман Сатиры";
         } else if (var1.contains("эрид") || var1.contains("erid")) {
            return "Талисман Эриды";
         } else if (var1.contains("мороз") || var1.contains("moroz")) {
            return "Талисман Мороза";
         } else if (var1.contains("гармон") || var1.contains("garmon")) {
            return "Талисман Гармонии";
         } else if (var1.contains("дедал") || var1.contains("dedal")) {
            return "Талисман Дедала";
         } else if (var1.contains("ехидн") || var1.contains("ehidn")) {
            return "Талисман Ехидна";
         } else if (var1.contains("грани") || var1.contains("grani")) {
            return "Талисман Грани";
         } else if (var1.contains("тритон") || var1.contains("triton")) {
            return "Талисман Тритона";
         } else if (var1.contains("феникс") || var1.contains("fenix") || var1.contains("phoenix")) {
            return "Талисман Феникс";
         } else if (var1.contains("мифическ") || var1.contains("mythical")) {
            return "Мифический талисман";
         } else if (!var1.contains("легендарн") && !var1.contains("legendary")) {
            return !var1.contains("эпическ") && !var1.contains("epic") ? null : "Эпический талисман";
         } else {
            return "Легендарный талисман";
         }
      } else {
         return null;
      }
   }

   private static String d(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = b(var0);
         Matcher var2 = TALISMAN_REGEX.matcher(var1);
         if (var2.find()) {
            String var3 = var2.group(1).trim();
            if (!var3.isEmpty()) {
               String var4 = c(var3);
               if (var4 != null) {
                  return var4;
               }

               return var3;
            }
         }

         return c(var1);
      } else {
         return null;
      }
   }

   private static String a(List<String> var0) {
      if (var0 != null && !var0.isEmpty()) {
         boolean var1 = false;
         boolean var2 = false;
         boolean var3 = false;
         boolean var4 = false;
         boolean var5 = false;
         boolean var6 = false;

         for (String var8 : var0) {
            String var9 = var8.toLowerCase(Locale.ROOT);
            if (var9.contains("макс. здоровье") || var9.contains("макс.здоровье") || var9.contains("здоровь") || var9.contains("health")) {
               var1 = true;
            }

            if (var9.contains("броня") || var9.contains("защит") || var9.contains("armor")) {
               var2 = true;
            }

            if (var9.contains("урон") || var9.contains("damage")) {
               var3 = true;
            }

            if (var9.contains("скорост") || var9.contains("speed")) {
               var4 = true;
               if ((!var9.contains("скорость i") || var9.contains("скорость ii"))
                  && !var9.contains("скорость 1")
                  && !var9.contains("speed 1")
                  && (!var9.contains("speed i") || var9.contains("speed ii"))) {
                  if (var9.contains("скорость ii") || var9.contains("скорость 2") || var9.contains("speed ii") || var9.contains("speed 2")) {
                     var6 = true;
                  }
               } else {
                  var5 = true;
               }
            }
         }

         if (var1 && var2 && var3 && var4) {
            return "Талисман INFINITY";
         } else if (var3 && var2 && var4 && !var1) {
            return var5 && !var6 ? "Талисман STINGER" : "Талисман ETERNITY";
         } else if (var1 && var2) {
            return "Талисман Титана";
         } else if (var4 && var3) {
            return "Талисман STINGER";
         } else {
            return var4 && var2 ? "Талисман Сатиры" : null;
         }
      } else {
         return null;
      }
   }

   private static int e(String var0) {
      if (var0 == null) {
         return -1;
      }

      Matcher var1 = DIGIT_PATTERN.matcher(var0);
      if (var1.find()) {
         try {
            return Integer.parseInt(var1.group());
         } catch (Exception var3) {
         }
      }

      return -1;
   }

   public static String p(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return null;
      }

      if (!var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return null;
      }

      DonateItem var1 = e(var0);
      if (var1 != null) {
         String var2 = b(var1.getName());
         if (var2 != null && !var2.isEmpty()) {
            return var2;
         }
      }

      Text var23 = var0.get(DataComponentTypes.CUSTOM_NAME);
      if (var23 == null) {
         var23 = var0.get(DataComponentTypes.ITEM_NAME);
      }

      String var3 = var23 != null ? var23.getString() : "";
      if (var3.isEmpty()) {
         String var4 = var0.getName().getString();
         String var5 = var0.getItem().getName().getString();
         if (!var4.equalsIgnoreCase(var5)) {
            var3 = var4;
         }
      }

      String var24 = b(var3);
      LoreComponent var25 = var0.get(DataComponentTypes.LORE);
      ArrayList var6 = new ArrayList();
      if (var25 != null) {
         for (Text var8 : var25.lines()) {
            String var9 = b(var8.getString());
            if (!var9.isEmpty()) {
               var6.add(var9);
            }
         }
      }

      String var26 = d(var24);
      if (var26 != null) {
         return var26;
      }

      for (String var30 : (Iterable<String>)(Iterable<?>)(var6)) {
         var26 = d(var30);
         if (var26 != null) {
            return var26;
         }
      }

      NbtCompound var29 = b(var0);
      String var31 = var29 != null ? var29.toString().toLowerCase(Locale.ROOT) : "";
      String var10 = "";

      try {
         var10 = var0.getComponents().toString().toLowerCase(Locale.ROOT);
      } catch (Exception var22) {
      }

      String var11 = (var24 + " " + String.join(" ", var6) + " " + var31 + " " + var10).toLowerCase(Locale.ROOT);
      String var12 = c(var11);
      if (var12 != null) {
         return var12;
      }

      Identifier var13 = var0.get(DataComponentTypes.ITEM_MODEL);
      if (var13 != null) {
         String var14 = var13.getPath().toLowerCase(Locale.ROOT);
         String var15 = c(var14);
         if (var15 != null) {
            return var15;
         }

         int var16 = e(var14);
         if (var16 > 0) {
            String var17 = a(var16);
            if (var17 != null) {
               return var17;
            }
         }
      }

      CustomModelDataComponent var32 = var0.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      if (var32 != null && !var32.floats().isEmpty()) {
         int var33 = var32.floats().getFirst().intValue();
         if (var33 > 0) {
            String var35 = a(var33);
            if (var35 != null) {
               return var35;
            }
         }
      }

      String var34 = a(var6);
      if (var34 != null) {
         return var34;
      }

      AttributeModifiersComponent var36 = var0.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (var36 != null) {
         String var37 = var36.toString().toLowerCase(Locale.ROOT);
         boolean var18 = var37.contains("max_health") || var37.contains("health");
         boolean var19 = var37.contains("attack_damage") || var37.contains("damage");
         boolean var20 = var37.contains("movement_speed") || var37.contains("speed");
         boolean var21 = var37.contains("armor");
         if (var18 && (var19 || var20 || var21)) {
            return "Талисман INFINITY";
         }

         if (var19 && var20 && var21) {
            return "Талисман ETERNITY";
         }

         if (var19 && var20) {
            return "Талисман STINGER";
         }

         if (var18 && var21) {
            return "Талисман Титана";
         }

         if (var20 && var21) {
            return "Талисман Сатиры";
         }
      }

      if (!var24.isEmpty() && !var24.equalsIgnoreCase("Тотем бессмертия") && !var24.equalsIgnoreCase("Totem of Undying") && !var24.equalsIgnoreCase("Тотем")) {
         return var24;
      } else {
         return o(var0) ? "Талисман" : "Тотем бессмертия";
      }
   }

   public static String q(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         if (var0.isOf(Items.TOTEM_OF_UNDYING)) {
            String var1 = p(var0);
            if (var1 != null && !var1.isEmpty()) {
               return var1;
            }
         }

         Text var3 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var3 == null) {
            var3 = var0.get(DataComponentTypes.ITEM_NAME);
         }

         if (var3 != null) {
            String var2 = b(var3.getString());
            if (!var2.isEmpty()) {
               return var2;
            }
         }

         String var4 = b(var0.getName().getString());
         return !var4.isEmpty() ? var4 : b(var0.getItem().getName().getString());
      } else {
         return "Предмет";
      }
   }

   public static boolean a(ItemStack var0, ItemStack var1) {
      if (var0 != null && var1 != null && !var0.isEmpty() && !var1.isEmpty()) {
         if (var0.getItem() != var1.getItem()) {
            return false;
         }

         if (!var0.isOf(Items.TOTEM_OF_UNDYING)) {
            return var0.isOf(Items.PLAYER_HEAD) ? ItemStack.areItemsAndComponentsEqual(var0, var1) : ItemStack.areItemsAndComponentsEqual(var0, var1);
         }

         String var2 = p(var0);
         String var3 = p(var1);
         return var2 != null && var3 != null && var2.equalsIgnoreCase(var3) ? true : ItemStack.areItemsAndComponentsEqual(var0, var1);
      } else {
         return false;
      }
   }

   public static int r(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return 999;
      }

      if (l(var0)) {
         return 1;
      }

      String var1 = p(var0);
      if (var1 != null) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         if (var2.contains("infinity") || var2.contains("инфинити")) {
            return 1;
         }

         if (var2.contains("eternity") || var2.contains("этернити") || var2.contains("этернети") || var2.contains("вечност")) {
            return 2;
         }

         if (var2.contains("stinger") || var2.contains("стингер")) {
            return 3;
         }

         if (var2.contains("карател") || var2.contains("karatel")) {
            return 4;
         }

         if (var2.contains("ярост") || var2.contains("yarost")) {
            return 5;
         }

         if (var2.contains("крушител") || var2.contains("krush")) {
            return 6;
         }

         if (var2.contains("тиран") || var2.contains("tiran")) {
            return 7;
         }

         if (var2.contains("демон") || var2.contains("demon")) {
            return 8;
         }

         if (var2.contains("вихр") || var2.contains("vihr")) {
            return 9;
         }

         if (var2.contains("мрак") || var2.contains("mrak")) {
            return 10;
         }

         if (var2.contains("гринч") || var2.contains("grinch")) {
            return 11;
         }

         if (var2.contains("арес") || var2.contains("ares")) {
            return 12;
         }

         if (var2.contains("хаос") || var2.contains("haos") || var2.contains("chaos")) {
            return 13;
         }

         if (var2.contains("титан") || var2.contains("titan")) {
            return 14;
         }

         if (var2.contains("гидр") || var2.contains("gidr") || var2.contains("hydra")) {
            return 15;
         }

         if (var2.contains("бести") || var2.contains("besti")) {
            return 16;
         }

         if (var2.contains("икар") || var2.contains("ikar")) {
            return 17;
         }

         if (var2.contains("сатир") || var2.contains("satir")) {
            return 18;
         }

         if (var2.contains("эрид") || var2.contains("erid")) {
            return 19;
         }

         if (var2.contains("мороз") || var2.contains("moroz")) {
            return 20;
         }

         if (var2.contains("гармон") || var2.contains("garmon")) {
            return 21;
         }

         if (var2.contains("дедал") || var2.contains("dedal")) {
            return 22;
         }

         if (var2.contains("ехидн") || var2.contains("ehidn")) {
            return 23;
         }

         if (var2.contains("грани") || var2.contains("grani")) {
            return 24;
         }

         if (var2.contains("тритон") || var2.contains("triton")) {
            return 25;
         }

         if (var2.contains("феникс") || var2.contains("fenix") || var2.contains("phoenix")) {
            return 26;
         }

         if (var2.contains("мифическ") || var2.contains("mythical")) {
            return 27;
         }

         if (var2.contains("легендарн") || var2.contains("legendary")) {
            return 28;
         }

         if (var2.contains("эпическ") || var2.contains("epic")) {
            return 29;
         }
      }

      String var11 = a(var0.getName().getString()).toLowerCase(Locale.ROOT);
      if (var11.contains("infinity") || var11.contains("инфинити")) {
         return 1;
      }

      if (var11.contains("eternity") || var11.contains("этернити") || var11.contains("этернети") || var11.contains("вечност")) {
         return 2;
      }

      if (var11.contains("stinger") || var11.contains("стингер")) {
         return 3;
      }

      if (var11.contains("карател") || var11.contains("karatel")) {
         return 4;
      }

      if (var11.contains("ярост") || var11.contains("yarost")) {
         return 5;
      }

      if (var11.contains("крушител") || var11.contains("krush")) {
         return 6;
      }

      if (var11.contains("тиран") || var11.contains("tiran")) {
         return 7;
      }

      if (var11.contains("демон") || var11.contains("demon")) {
         return 8;
      }

      if (var11.contains("вихр") || var11.contains("vihr")) {
         return 9;
      }

      if (var11.contains("мрак") || var11.contains("mrak")) {
         return 10;
      }

      if (var11.contains("гринч") || var11.contains("grinch")) {
         return 11;
      }

      if (var11.contains("арес") || var11.contains("ares")) {
         return 12;
      }

      if (var11.contains("хаос") || var11.contains("haos") || var11.contains("chaos")) {
         return 13;
      }

      if (var11.contains("титан") || var11.contains("titan")) {
         return 14;
      }

      if (var11.contains("гидр") || var11.contains("gidr") || var11.contains("hydra")) {
         return 15;
      }

      if (var11.contains("бести") || var11.contains("besti")) {
         return 16;
      }

      if (var11.contains("икар") || var11.contains("ikar")) {
         return 17;
      }

      if (var11.contains("сатир") || var11.contains("satir")) {
         return 18;
      }

      if (var11.contains("эрид") || var11.contains("erid")) {
         return 19;
      }

      if (var11.contains("мороз") || var11.contains("moroz")) {
         return 20;
      }

      if (!o(var0) && !c(var0)) {
         return 99;
      }

      for (DonateItem var6 : DonateItem.values()) {
         for (String var10 : var6.getNbt()) {
            if (d(var0).equalsIgnoreCase(var10)) {
               return 35 - var6.getFactor();
            }
         }
      }

      return 98;
   }

   public static int s(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return 0;
      }

      if (l(var0)) {
         return 2500;
      }

      if (h(var0)) {
         return 2000;
      }

      DonateItem var1 = e(var0);
      if (var1 != null) {
         return switch (var1) {
            case TAL_KRUSH, TAL_KARATEL, TAL_GARMONIA, TAL_DEDALA, TAL_EHIDNA, TAL_INFINITY, TAL_ETERNITY, TAL_STINGER, TAL_SATIRI -> 2500;
            case TAL_GRAN, TAL_TRITON, TAL_FENIX -> 2000;
            default -> 2000;
         };
      } else {
         CustomModelDataComponent var2 = var0.get(DataComponentTypes.CUSTOM_MODEL_DATA);
         if (var2 != null && !var2.floats().isEmpty()) {
            int var3 = var2.floats().getFirst().intValue();
            if (var3 >= 1 && var3 <= 21) {
               return 2500;
            }

            if (var3 >= 22 && var3 <= 30) {
               return 2000;
            }
         }

         String var7 = var0.getName().getString().toLowerCase();
         String var4 = d(var0).toLowerCase();
         if (var7.contains("титан")
            || var7.contains("titan")
            || var4.contains("titan")
            || var7.contains("пандор")
            || var7.contains("pandora")
            || var4.contains("pandora")
            || var7.contains("андромед")
            || var7.contains("andromeda")
            || var4.contains("andromeda")
            || var7.contains("химер")
            || var7.contains("himera")
            || var4.contains("himera")
            || var7.contains("астре")
            || var7.contains("astreya")
            || var4.contains("astreya")
            || var7.contains("аполлон")
            || var7.contains("apollon")
            || var4.contains("apollon")
            || var7.contains("осирис")
            || var7.contains("osiris")
            || var4.contains("osiris")
            || var7.contains("крушител")
            || var7.contains("карател")
            || var7.contains("гармони")
            || var7.contains("дедал")
            || var7.contains("ехидн")
            || var7.contains("цербер")) {
            return 2500;
         }

         if (!var7.contains("гран") && !var7.contains("тритон") && !var7.contains("феникс") && !var7.contains("дракон") && !var7.contains("dragon")) {
            int var5 = var0.getMaxDamage();
            if (var5 > 0) {
               return var5;
            }

            int var6 = var0.getItem().getDefaultStack().getMaxDamage();
            return var6 > 0 ? var6 : t(var0);
         } else {
            return 2000;
         }
      }
   }

   public static int t(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         Item var1 = var0.getItem();
         if (var1 == Items.NETHERITE_HELMET) {
            return 407;
         } else if (var1 == Items.NETHERITE_CHESTPLATE) {
            return 592;
         } else if (var1 == Items.NETHERITE_LEGGINGS) {
            return 555;
         } else if (var1 == Items.NETHERITE_BOOTS) {
            return 481;
         } else if (var1 == Items.DIAMOND_HELMET) {
            return 363;
         } else if (var1 == Items.DIAMOND_CHESTPLATE) {
            return 528;
         } else if (var1 == Items.DIAMOND_LEGGINGS) {
            return 495;
         } else if (var1 == Items.DIAMOND_BOOTS) {
            return 429;
         } else if (var1 == Items.IRON_HELMET) {
            return 165;
         } else if (var1 == Items.IRON_CHESTPLATE) {
            return 240;
         } else if (var1 == Items.IRON_LEGGINGS) {
            return 225;
         } else if (var1 == Items.IRON_BOOTS) {
            return 195;
         } else if (var1 == Items.GOLDEN_HELMET) {
            return 77;
         } else if (var1 == Items.GOLDEN_CHESTPLATE) {
            return 112;
         } else if (var1 == Items.GOLDEN_LEGGINGS) {
            return 105;
         } else if (var1 == Items.GOLDEN_BOOTS) {
            return 91;
         } else if (var1 == Items.CHAINMAIL_HELMET) {
            return 165;
         } else if (var1 == Items.CHAINMAIL_CHESTPLATE) {
            return 240;
         } else if (var1 == Items.CHAINMAIL_LEGGINGS) {
            return 225;
         } else if (var1 == Items.CHAINMAIL_BOOTS) {
            return 195;
         } else if (var1 == Items.LEATHER_HELMET) {
            return 55;
         } else if (var1 == Items.LEATHER_CHESTPLATE) {
            return 80;
         } else if (var1 == Items.LEATHER_LEGGINGS) {
            return 75;
         } else if (var1 == Items.LEATHER_BOOTS) {
            return 65;
         } else if (var1 == Items.TURTLE_HELMET) {
            return 275;
         } else if (var1 == Items.ELYTRA) {
            return 432;
         } else if (var1 == Items.SHIELD) {
            return 336;
         } else if (var1 == Items.NETHERITE_SWORD) {
            return 2031;
         } else if (var1 == Items.DIAMOND_SWORD) {
            return 1561;
         } else if (var1 == Items.IRON_SWORD) {
            return 250;
         } else if (var1 == Items.GOLDEN_SWORD) {
            return 32;
         } else if (var1 == Items.STONE_SWORD) {
            return 131;
         } else if (var1 == Items.WOODEN_SWORD) {
            return 59;
         } else if (var1 == Items.NETHERITE_AXE || var1 == Items.NETHERITE_PICKAXE) {
            return 2031;
         } else if (var1 == Items.DIAMOND_AXE || var1 == Items.DIAMOND_PICKAXE) {
            return 1561;
         } else if (var1 == Items.IRON_AXE || var1 == Items.IRON_PICKAXE) {
            return 250;
         } else if (var1 == Items.MACE) {
            return 500;
         } else if (var1 == Items.BOW) {
            return 384;
         } else if (var1 == Items.CROSSBOW) {
            return 465;
         } else {
            return var1 == Items.TRIDENT ? 250 : 0;
         }
      } else {
         return 0;
      }
   }

   public static int a(NbtCompound var0, String... var1) {
      if (var0 == null) {
         return -1;
      }

      for (String var3 : var0.getKeys()) {
         String var4 = var3.toLowerCase();

         for (String var8 : var1) {
            if (var4.contains(var8)) {
               NbtElement var9 = var0.get(var3);
               if (var9 != null) {
                  try {
                     if (var9 instanceof NbtInt var21) {
                        return var21.intValue();
                     }

                     if (var9 instanceof NbtShort var20) {
                        return var20.shortValue();
                     }

                     if (var9 instanceof NbtByte var19) {
                        return var19.byteValue();
                     }

                     if (var9 instanceof NbtLong var18) {
                        return (int)var18.longValue();
                     }

                     if (var9 instanceof NbtDouble var17) {
                        return (int)Math.round(var17.doubleValue());
                     }

                     if (var9 instanceof NbtFloat var16) {
                        return Math.round(var16.floatValue());
                     }

                     if (var9 instanceof NbtString var10) {
                        String var11 = var10.value().replaceAll("[^0-9]", "");
                        if (!var11.isEmpty()) {
                           return Integer.parseInt(var11);
                        }
                     }
                  } catch (Exception var12) {
                  }
               }
            }
         }

         try {
            Optional var14 = var0.getCompound(var3);
            if (var14.isPresent()) {
               int var15 = a((NbtCompound)var14.get(), var1);
               if (var15 != -1) {
                  return var15;
               }
            }
         } catch (Exception var13) {
         }
      }

      return -1;
   }

   public static boolean u(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         LoreComponent var1 = var0.get(DataComponentTypes.LORE);
         if (var1 != null && !var1.lines().isEmpty()) {
            for (Text var3 : var1.lines()) {
               String var4 = var3.getString();
               if (var4 != null && (var4.contains("/") || var4.toLowerCase().contains("прочн") || var4.toLowerCase().contains("durability"))) {
                  return true;
               }
            }
         }

         NbtCompound var5 = b(var0);
         return var5 != null ? a(var5, "custom_durability", "custom-durability", "item_durability", "holyworld:durability", "holyworld:dura") != -1 : false;
      } else {
         return false;
      }
   }

   public static ItemUtility.a v(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         LoreComponent var1 = var0.get(DataComponentTypes.LORE);
         if (var1 != null) {
            for (Text var3 : var1.lines()) {
               String var4 = var3.getString();
               if (var4 != null && !var4.isEmpty()) {
                  String var5 = var4.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                  String var6 = var5.toLowerCase();
                  Matcher var7 = DURA_SLASH_PATTERN.matcher(var5);
                  if (var7.find()) {
                     try {
                        int var8 = Integer.parseInt(var7.group(1).replaceAll("[\\s.,]", ""));
                        int var9 = Integer.parseInt(var7.group(2).replaceAll("[\\s.,]", ""));
                        if (var9 > 0 && var9 >= var8) {
                           return new ItemUtility.a(var8, var9);
                        }
                     } catch (Exception var12) {
                     }
                  }

                  if (var6.contains("прочност") || var6.contains("durability") || var6.contains("использовани") || var6.contains("зарядов")) {
                     Matcher var24 = DURA_PERCENT_PATTERN.matcher(var5);
                     if (var24.find()) {
                        try {
                           float var26 = Float.parseFloat(var24.group(1));
                           int var27 = s(var0);
                           int var28 = Math.round(var27 * (var26 / 100.0F));
                           return new ItemUtility.a(var28, var27);
                        } catch (Exception var14) {
                        }
                     }

                     Matcher var25 = DURA_SINGLE_PATTERN.matcher(var5);
                     if (var25.find()) {
                        try {
                           int var10 = Integer.parseInt(var25.group(1).replaceAll("[\\s.,]", ""));
                           int var11 = s(var0);
                           if (var11 < var10) {
                              var11 = var10;
                           }

                           return new ItemUtility.a(var10, var11);
                        } catch (Exception var13) {
                        }
                     }
                  }
               }
            }
         }

         NbtCompound var15 = b(var0);
         if (var15 != null) {
            int var16 = a(
               var15,
               "custom_durability",
               "custom-durability",
               "item_durability",
               "holyworld:durability",
               "holyworld:dura",
               "current_durability",
               "cur_durability"
            );
            int var18 = a(var15, "max_durability", "max-durability", "maxdurability", "custom_max_durability", "holyworld:max_durability");
            if (var16 >= 0 && var18 > 0) {
               return new ItemUtility.a(var16, var18);
            }

            if (var16 >= 0) {
               int var21 = s(var0);
               return new ItemUtility.a(var16, Math.max(var21, var16));
            }
         }

         int var17 = s(var0);
         if (var0.isDamageable()) {
            int var19 = var0.getMaxDamage();
            int var20 = Math.max(0, var19 - var0.getDamage());
            if (var17 > var19 && var19 > 0) {
               float var22 = Math.clamp((float)var20 / var19, 0.0F, 1.0F);
               int var23 = Math.round(var22 * var17);
               return new ItemUtility.a(var23, var17);
            } else {
               return new ItemUtility.a(var20, var19);
            }
         } else {
            return var17 > 0 ? new ItemUtility.a(var17, var17) : null;
         }
      } else {
         return null;
      }
   }

   private ItemUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public record a(int current, int max) {
      public boolean a() {
         return this.max > 0;
      }

      public int getCurrent() {
         return this.current;
      }

      public int getMax() {
         return this.max;
      }
   }
}
