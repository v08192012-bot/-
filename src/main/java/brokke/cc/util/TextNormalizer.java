package brokke.cc.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

public final class TextNormalizer {
   private TextNormalizer() {
   }

   public static String a(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = Normalizer.normalize(var0, Form.NFKC);
      var1 = b(var1);
      var1 = var1.toLowerCase(Locale.ROOT);
      var1 = var1.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
      return var1.replaceAll("\\s+", " ").trim();
   }

   private static String b(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         var1.append(a(var0.charAt(var2)));
      }

      return var1.toString();
   }

   private static char a(char var0) {
      return switch (var0) {
         case 'ǫ' -> 'Q';
         case 'ɢ' -> 'G';
         case 'ɪ' -> 'I';
         case 'ɴ' -> 'N';
         case 'ʀ' -> 'R';
         case 'ʏ' -> 'Y';
         case 'ʙ' -> 'B';
         case 'ʜ' -> 'H';
         case 'ʟ' -> 'L';
         case 'ғ' -> 'F';
         case 'ᴀ' -> 'A';
         case 'ᴄ' -> 'C';
         case 'ᴅ' -> 'D';
         case 'ᴇ' -> 'E';
         case 'ᴊ' -> 'J';
         case 'ᴋ' -> 'K';
         case 'ᴍ' -> 'M';
         case 'ᴏ' -> 'O';
         case 'ᴘ' -> 'P';
         case 'ᴛ' -> 'T';
         case 'ᴜ' -> 'U';
         case 'ᴠ' -> 'V';
         case 'ᴡ' -> 'W';
         case 'ᴢ' -> 'Z';
         case 'ꜰ' -> 'F';
         case 'ꜱ' -> 'S';
         default -> var0;
      };
   }

   public static boolean a(String var0, String var1) {
      if (var0 == null || var0.isBlank()) {
         return true;
      }

      if (var1 == null) {
         return false;
      }

      String var2 = a(var0);
      String var3 = a(var1);
      return !var2.isEmpty() && !var3.isEmpty() ? var2.equals(var3) || c(var3, var2) || c(var2, var3) : true;
   }

   public static boolean b(String var0, String var1) {
      if (var1 == null || var1.isBlank()) {
         return true;
      }

      if (var0 == null) {
         return false;
      }

      String var2 = a(var0);
      String var3 = a(var1);
      return var3.isEmpty() || c(var2, var3);
   }

   private static boolean c(String var0, String var1) {
      if (var0.equals(var1)) {
         return true;
      }

      int var2 = 0;

      while (true) {
         int var3 = var0.indexOf(var1, var2);
         if (var3 < 0) {
            return false;
         }

         boolean var4 = var3 == 0 || var0.charAt(var3 - 1) == ' ';
         int var5 = var3 + var1.length();
         boolean var6 = var5 == var0.length() || var0.charAt(var5) == ' ';
         if (var4 && var6) {
            return true;
         }

         var2 = var3 + 1;
      }
   }
}
