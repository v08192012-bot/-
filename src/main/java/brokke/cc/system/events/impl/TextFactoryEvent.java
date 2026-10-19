package brokke.cc.system.events.impl;

import brokke.cc.system.events.Event;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFactoryEvent extends Event {
   private String text;
   private String cachedStrippedText;
   private String cachedStrippedLower;
   private boolean cacheDirty = true;
   private static final String STANDARD_FORMAT = "[§&][0-9a-fA-Fk-oK-OrR]";
   private static final String HEX_COLOR_1 = "[§&][xX](?:[§&][0-9a-fA-F]){6}";
   private static final String HEX_COLOR_2 = "[§&]#[0-9a-fA-F]{6}";
   private static final String HEX_COLOR_3 = "[§&]<#[0-9a-fA-F]{6}>";
   private static final String HEX_COLOR_4 = "<#[0-9a-fA-F]{6}>";
   private static final String HEX_COLOR_5 = "\\{#[0-9a-fA-F]{6}\\}";
   private static final String TAG_FORMAT = "<(?:/?[a-zA-Z0-9_#:-]+)>";
   private static final String ANY_FORMAT = "(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>)";
   private static final String FORMAT_CODES_PATTERN = "(?:(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>))*";
   private static final Pattern STRIP_PATTERN = Pattern.compile(
      "(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>)"
   );

   public TextFactoryEvent(String var1) {
      this.text = var1;
      this.cacheDirty = true;
   }

   public void setText(String var1) {
      this.text = var1;
      this.cacheDirty = true;
   }

   public String getText() {
      return this.text;
   }

   private String getStrippedText() {
      if (!this.cacheDirty && this.cachedStrippedText != null) {
         return this.cachedStrippedText;
      }

      this.cachedStrippedText = this.stripFormatCodes(this.text);
      this.cachedStrippedLower = this.cachedStrippedText != null ? this.cachedStrippedText.toLowerCase(Locale.ROOT) : null;
      this.cacheDirty = false;
      return this.cachedStrippedText;
   }

   private String getStrippedLower() {
      if (this.cacheDirty || this.cachedStrippedLower == null) {
         this.getStrippedText();
      }

      return this.cachedStrippedLower;
   }

   private void markDirty() {
      this.cacheDirty = true;
   }

   public void replaceText(String var1, String var2) {
      if (this.text != null && !this.text.isEmpty() && var1 != null && !var1.isEmpty()) {
         String var3 = this.stripFormatCodes(var1);
         String var4 = this.getStrippedLower();
         if (var4 != null && var3 != null && !var3.isEmpty()) {
            String var5 = var3.toLowerCase(Locale.ROOT);
            if (var4.contains(var5)) {
               String var6 = this.buildPatternWithFormatCodes(var3);
               Pattern var7 = Pattern.compile(var6, 2);
               Matcher var8 = var7.matcher(this.text);
               StringBuilder var9 = new StringBuilder();

               while (var8.find()) {
                  String var10 = var8.group();
                  String var11 = this.extractAllFormats(var10);
                  var8.appendReplacement(var9, Matcher.quoteReplacement(var11 + var2));
               }

               var8.appendTail(var9);
               this.text = var9.toString();
               this.markDirty();
            }
         }
      }
   }

   public void replaceTextContains(String var1, String var2) {
      if (this.text != null && !this.text.isEmpty() && var1 != null && !var1.isEmpty()) {
         String var3 = this.stripFormatCodes(var1);
         String var4 = this.getStrippedLower();
         if (var4 != null && var3 != null && !var3.isEmpty()) {
            String var5 = var3.toLowerCase(Locale.ROOT);
            if (var4.contains(var5)) {
               String var6 = this.buildPatternWithFormatCodes(var3);
               String var7 = "(?:(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>)|[^\\s§])*";
               String var8 = var7 + var6 + var7;
               Pattern var9 = Pattern.compile(var8, 2);
               Matcher var10 = var9.matcher(this.text);
               StringBuilder var11 = new StringBuilder();

               while (var10.find()) {
                  String var12 = var10.group();
                  String var13 = this.extractAllFormats(var12);
                  var10.appendReplacement(var11, Matcher.quoteReplacement(var13 + var2));
               }

               var10.appendTail(var11);
               this.text = var11.toString();
               this.markDirty();
            }
         }
      }
   }

   public void replaceTextPartial(String var1, String var2) {
      if (this.text != null && !this.text.isEmpty() && var1 != null && !var1.isEmpty()) {
         String var3 = this.stripFormatCodes(var1);
         String var4 = this.getStrippedLower();
         if (var4 != null && var3 != null && !var3.isEmpty()) {
            String var5 = var3.toLowerCase(Locale.ROOT);
            if (var4.contains(var5)) {
               String var6 = this.buildPatternWithFormatCodes(var3);
               Pattern var7 = Pattern.compile(var6, 2);
               Matcher var8 = var7.matcher(this.text);
               StringBuilder var9 = new StringBuilder();

               while (var8.find()) {
                  String var10 = var8.group();
                  String var11 = this.extractAllFormats(var10);
                  var8.appendReplacement(var9, Matcher.quoteReplacement(var11 + var2));
               }

               var8.appendTail(var9);
               this.text = var9.toString();
               this.markDirty();
            }
         }
      }
   }

   public void replaceTextExact(String var1, String var2) {
      if (this.text != null && !this.text.isEmpty() && var1 != null && !var1.isEmpty()) {
         String var3 = this.stripFormatCodes(var1);
         String var4 = this.getStrippedLower();
         if (var4 != null && var3 != null && !var3.isEmpty()) {
            String var5 = var3.toLowerCase(Locale.ROOT);
            if (var4.contains(var5)) {
               String var6 = this.buildPatternWithFormatCodes(var3);
               String var7 = "(?<![a-zA-Z0-9_])" + var6 + "(?![a-zA-Z0-9_])";
               Pattern var8 = Pattern.compile(var7, 2);
               Matcher var9 = var8.matcher(this.text);
               StringBuilder var10 = new StringBuilder();

               while (var9.find()) {
                  String var11 = var9.group();
                  String var12 = this.extractAllFormats(var11);
                  var9.appendReplacement(var10, Matcher.quoteReplacement(var12 + var2));
               }

               var9.appendTail(var10);
               this.text = var10.toString();
               this.markDirty();
            }
         }
      }
   }

   private String buildPatternWithFormatCodes(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder();
         var2.append(
            "(?:(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>))*"
         );

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = var1.charAt(var3);
            if (this.isRegexSpecialChar(var4)) {
               var2.append("\\");
            }

            var2.append(var4);
            var2.append(
               "(?:(?:[§&][xX](?:[§&][0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6}|[§&]<#[0-9a-fA-F]{6}>|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}|[§&][0-9a-fA-Fk-oK-OrR]|<(?:/?[a-zA-Z0-9_#:-]+)>))*"
            );
         }

         return var2.toString();
      } else {
         return "";
      }
   }

   private String extractAllFormats(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder();
         byte var3 = 0;

         while (var3 < var1.length() && var1.charAt(var3) == 167 && var3 + 1 < var1.length()) {
            char var4 = var1.charAt(var3 + 1);
            if ((var4 == 'x' || var4 == 'X') && var3 + 13 <= var1.length()) {
               boolean var5 = true;

               for (int var6 = 0; var6 < 6; var6++) {
                  int var7 = var3 + 2 + var6 * 2;
                  if (var7 + 1 >= var1.length() || var1.charAt(var7) != 167) {
                     var5 = false;
                     break;
                  }

                  char var8 = var1.charAt(var7 + 1);
                  if (!this.isHexChar(var8)) {
                     var5 = false;
                     break;
                  }
               }

               if (var5) {
                  var2.append(var1, var3, var3 + 14);
                  var3 = (byte)(var3 + 14);
                  continue;
               }
            }

            if (!this.isFormatChar(var4)) {
               break;
            }

            var2.append(var1.charAt(var3));
            var2.append(var4);
            var3 = (byte)(var3 + 2);
         }

         return var2.toString();
      } else {
         return "";
      }
   }

   private boolean isFormatChar(char var1) {
      return var1 >= '0' && var1 <= '9'
         || var1 >= 'a' && var1 <= 'f'
         || var1 >= 'A' && var1 <= 'F'
         || var1 >= 'k' && var1 <= 'o'
         || var1 >= 'K' && var1 <= 'O'
         || var1 == 'r'
         || var1 == 'R';
   }

   private boolean isHexChar(char var1) {
      return var1 >= '0' && var1 <= '9' || var1 >= 'a' && var1 <= 'f' || var1 >= 'A' && var1 <= 'F';
   }

   public String stripFormatCodes(String var1) {
      return var1 == null ? null : STRIP_PATTERN.matcher(var1).replaceAll("");
   }

   private boolean isRegexSpecialChar(char var1) {
      return "\\^$.|?*+()[]{}".indexOf(var1) != -1;
   }

   public boolean containsIgnoreFormat(String var1) {
      if (this.text != null && var1 != null) {
         String var2 = this.getStrippedLower();
         String var3 = this.stripFormatCodes(var1);
         return var2 != null && var3 != null ? var2.contains(var3.toLowerCase(Locale.ROOT)) : false;
      } else {
         return false;
      }
   }
}
