package brokke.cc.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class ScoreboardAnimator {
   private static final Pattern NUMBER_PATTERN = Pattern.compile(
      "(?<![\\wА-Яа-яЁё])(\\d{1,3}(?:[,\\s .]\\d{3})+(?:[.,]\\d+)?|\\d+(?:[.,]\\d+)?)(?![\\wА-Яа-яЁё])"
   );
   private static final Map<String, ScoreboardAnimator.a> TRACKER = new ConcurrentHashMap<>();
   private static long lastCleanup = 0L;

   private ScoreboardAnimator() {
   }

   public static Text a(int var0, Text var1, long var2) {
      if (var1 == null) {
         return null;
      }

      if (var2 - lastCleanup > 10000L) {
         lastCleanup = var2;
         a(var2);
      }

      ScoreboardAnimator.b var4 = a(var1);
      String var5 = var4.value;
      if (var5.isEmpty()) {
         return var1;
      }

      ArrayList var6 = new ArrayList();
      Matcher var7 = NUMBER_PATTERN.matcher(var5);

      while (var7.find()) {
         var6.add(new ScoreboardAnimator.c(var7.start(), var7.end(), var7.group()));
      }

      if (var6.isEmpty()) {
         return var1;
      }

      ArrayList var8 = new ArrayList<>(var4.styles);
      byte var9 = 0;

      for (int var10 = var6.size() - 1; var10 >= 0; var10--) {
         ScoreboardAnimator.c var11 = (ScoreboardAnimator.c)var6.get(var10);
         ScoreboardAnimator.d var12 = a(var11.matched);
         if (var12 != null) {
            String var13 = var5.substring(0, var11.start);
            String var14 = var5.substring(var11.end);
            String var15 = var13.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").replaceAll("^[▍▲▼■•*\\s\\u00A0\\-]+", "").trim();
            String var16 = var14.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").replaceAll("[\\s\\u00A0\\-]+$", "").trim();
            String var17 = var15 + "|||" + var16;
            if ("|||".equals(var17)) {
               var17 = "row_" + var0 + "_m_" + var10;
            } else {
               var17 = var17 + "#" + var10;
            }

            ScoreboardAnimator.a var18 = TRACKER.computeIfAbsent(var17, var0x -> new ScoreboardAnimator.a());
            var18.a(var12.val, var12.separator, var12.decimals, var2);
            String var21 = a(var18.a(var2), var18.separator, var18.decimalPlaces);
            if (!var21.equals(var11.matched)) {
               int var22 = var5.codePointCount(0, var11.start);
               int var23 = var5.codePointCount(0, var11.end);
               Style var24 = var22 < var8.size() ? (Style)var8.get(var22) : Style.EMPTY;

               for (int var25 = var23 - 1; var25 >= var22; var25--) {
                  if (var25 < var8.size()) {
                     var8.remove(var25);
                  }
               }

               int var29 = var21.codePointCount(0, var21.length());

               for (int var26 = 0; var26 < var29; var26++) {
                  int var27 = Math.min(var22 + var26, var8.size());
                  var8.add(var27, var24);
               }

               var5 = var5.substring(0, var11.start) + var21 + var5.substring(var11.end);
               var9 = 1;
            }
         }
      }

      return var9 != 0 ? a(var5, var8) : var1;
   }

   public static void clearTRACKER() {
      TRACKER.clear();
   }

   private static void a(long var0) {
      Iterator var2 = TRACKER.entrySet().iterator();

      while (var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         if (var0 - ((ScoreboardAnimator.a)var3.getValue()).getLastSeenTime() > 15000L) {
            var2.remove();
         }
      }
   }

   private static ScoreboardAnimator.d a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         try {
            if (var0.matches("\\d{1,3}(,\\d{3})+")) {
               double var8 = Double.parseDouble(var0.replace(",", ""));
               return new ScoreboardAnimator.d(var8, ",", 0);
            } else if (var0.matches("\\d{1,3}([ \\u00A0]\\d{3})+")) {
               double var7 = Double.parseDouble(var0.replace(" ", "").replace(" ", ""));
               return new ScoreboardAnimator.d(var7, " ", 0);
            } else if (var0.matches("\\d{1,3}(\\.\\d{3})+")) {
               double var6 = Double.parseDouble(var0.replace(".", ""));
               return new ScoreboardAnimator.d(var6, ".", 0);
            } else if (var0.contains(".")) {
               double var5 = Double.parseDouble(var0);
               int var3 = var0.length() - var0.indexOf(46) - 1;
               return new ScoreboardAnimator.d(var5, "", var3);
            } else {
               double var1 = Double.parseDouble(var0);
               return new ScoreboardAnimator.d(var1, "", 0);
            }
         } catch (NumberFormatException var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String a(double var0, String var2, int var3) {
      if (var3 == 0) {
         long var4 = Math.round(var0);
         if (",".equals(var2)) {
            return String.format(Locale.US, "%,d", var4);
         } else if (" ".equals(var2)) {
            return String.format(Locale.US, "%,d", var4).replace(',', ' ');
         } else {
            return ".".equals(var2) ? String.format(Locale.US, "%,d", var4).replace(',', '.') : Long.toString(var4);
         }
      } else {
         return String.format(Locale.US, "%." + var3 + "f", var0);
      }
   }

   private static ScoreboardAnimator.b a(Text var0) {
      StringBuilder var1 = new StringBuilder();
      ArrayList var2 = new ArrayList();
      var0.asOrderedText().accept((var2xx, var3, var4) -> {
         var1.appendCodePoint(var4);
         var2.add(var3 == null ? Style.EMPTY : var3);
         return true;
      });
      return new ScoreboardAnimator.b(var1.toString(), var2);
   }

   private static Text a(String var0, List<Style> var1) {
      MutableText var2 = Text.empty();
      if (var0.isEmpty()) {
         return var2;
      }

      StringBuilder var3 = new StringBuilder();
      Style var4 = Style.EMPTY;
      byte var5 = 0;
      int var6 = 0;

      for (int var7 = 0; var6 < var0.length(); var7++) {
         int var8 = var0.codePointAt(var6);
         Style var9 = var7 < var1.size() ? var1.get(var7) : Style.EMPTY;
         if (var5 == 0) {
            var4 = var9 == null ? Style.EMPTY : var9;
            var5 = 1;
         } else if (!var4.equals(var9)) {
            var2.append(Text.literal(var3.toString()).setStyle(var4));
            var3.setLength(0);
            var4 = var9 == null ? Style.EMPTY : var9;
         }

         var3.appendCodePoint(var8);
         var6 += Character.charCount(var8);
      }

      if (var3.length() > 0) {
         var2.append(Text.literal(var3.toString()).setStyle(var4));
      }

      return var2;
   }

   public static class a {
      private double currentValue;
      private double startValue;
      private double targetValue;
      private long startTime;
      private long duration = 600L;
      String separator = "";
      int decimalPlaces = 0;
      private boolean initialized = false;
      private long lastSeenTime;

      public void a(double var1, String var3, int var4, long var5) {
         this.lastSeenTime = var5;
         this.separator = var3;
         this.decimalPlaces = var4;
         if (!this.initialized) {
            this.currentValue = var1;
            this.startValue = var1;
            this.targetValue = var1;
            this.initialized = true;
         } else if (Math.abs(var1 - this.targetValue) > 1.0E-6) {
            this.startValue = this.currentValue;
            this.targetValue = var1;
            this.startTime = var5;
            double var7 = Math.abs(this.targetValue - this.startValue);
            this.duration = (long)Math.clamp(350.0 + Math.min(var7 * 2.0, 450.0), 350.0, 800.0);
         }
      }

      public double a(long var1) {
         if (!this.initialized) {
            return this.targetValue;
         } else if (Math.abs(this.targetValue - this.startValue) < 1.0E-6) {
            this.currentValue = this.targetValue;
            return this.targetValue;
         } else {
            long var3 = var1 - this.startTime;
            if (var3 >= this.duration) {
               this.currentValue = this.targetValue;
               return this.targetValue;
            } else {
               float var5 = Math.clamp((float)var3 / (float)this.duration, 0.0F, 1.0F);
               float var6 = 1.0F - var5;
               float var7 = 1.0F - var6 * var6 * var6;
               this.currentValue = this.startValue + (this.targetValue - this.startValue) * var7;
               return this.currentValue;
            }
         }
      }

      public boolean b(long var1) {
         return !this.initialized || var1 - this.startTime >= this.duration || Math.abs(this.currentValue - this.targetValue) < 1.0E-6;
      }

      public long getLastSeenTime() {
         return this.lastSeenTime;
      }
   }

   record b(String value, List<Style> styles) {
      public String getValue() {
         return this.value;
      }

      public List<Style> getStyles() {
         return this.styles;
      }
   }

   record c(int start, int end, String matched) {
      public int getStart() {
         return this.start;
      }

      public int getEnd() {
         return this.end;
      }

      public String getMatched() {
         return this.matched;
      }
   }

   record d(double val, String separator, int decimals) {
      public double getVal() {
         return this.val;
      }

      public String getSeparator() {
         return this.separator;
      }

      public int getDecimals() {
         return this.decimals;
      }
   }
}
