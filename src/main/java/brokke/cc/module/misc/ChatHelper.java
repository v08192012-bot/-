package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.manager.CommandManager;
import brokke.cc.nav.Waypoint;
import brokke.cc.nav.WaypointStorage;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import java.awt.Color;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

@NewFunction(name = "ChatHelper", description = "Помощник для чата", category = Category.MISC)
public class ChatHelper extends Function {
   private final BooleanSetting autoEventGps = new BooleanSetting("Авто-метка (/event)", true);
   private final BooleanSetting ahCalculator = new BooleanSetting("Калькулятор (/ah sell)", true);
   private final BooleanSetting russianLayout = new BooleanSetting("Печать по русский", true);
   private static final String RU_MAP = "йцукенгшщзхъфывапролджэячсмитьбюёЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮЁ";
   private static final String EN_MAP = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>~";
   private static final Set<String> CHAT_MSG_COMMANDS = Set.of("msg", "tell", "w", "m", "pm", "r", "reply", "mail", "bc", "broadcast");
   private static final Pattern EVENT_PATTERN_1 = Pattern.compile(
      "(?i)(?:▶\\s*)?([^\\n\\r—:\\d]+?)\\s+находится\\s+на\\s+координатах\\s+(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)"
   );
   private static final Pattern EVENT_PATTERN_2 = Pattern.compile(
      "(?i)(?:▶\\s*)?([^\\n\\r—:\\d]+?)\\s+(?:упал|приземлился|появился|спавн)\\s+(?:на\\s+)?координат[ыах:]*\\s+(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)"
   );
   private static final Pattern EVENT_PATTERN_3 = Pattern.compile("(?i)находится\\s+на\\s+координатах\\s+(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)");
   private static final Pattern EVENT_PATTERN_4 = Pattern.compile("(?i)(?:координаты|координатах|корды)[:\\s]+(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)");
   private static final Pattern EVENT_PATTERN_5 = Pattern.compile("(?i)X:\\s*(-?\\d+)[,\\s]+Y:\\s*(-?\\d+)[,\\s]+Z:\\s*(-?\\d+)");
   private static String lastEventKey = "";
   private static long lastEventTime = 0L;

   public ChatHelper() {
      this.addSettings(this.autoEventGps, this.ahCalculator, this.russianLayout);
   }

   public static ChatHelper k() {
      return FunctionManager.a(ChatHelper.class);
   }

   public static String a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         ChatHelper var1 = k();
         if (var1 != null && var1.isActive()) {
            if (var0.startsWith(".") && var1.russianLayout.isToggled()) {
               String var2 = var0.substring(1);
               int var3 = var2.indexOf(32);
               String var4 = var3 == -1 ? var2 : var2.substring(0, var3);
               String var5 = var3 == -1 ? "" : var2.substring(var3);
               if (e(var4)) {
                  return "." + d(var4) + var5;
               }
            }

            return var0;
         } else {
            return var0;
         }
      } else {
         return var0;
      }
   }

   public static String b(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         ChatHelper var1 = k();
         if (var1 != null && var1.isActive()) {
            String var2 = var0;
            if (var1.russianLayout.isToggled()) {
               var2 = g(var2);
            }

            if (var1.ahCalculator.isToggled()) {
               var2 = h(var2);
            }

            return var2;
         } else {
            return var0;
         }
      } else {
         return var0;
      }
   }

   private static String g(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0.trim();
         int var2 = var1.indexOf(32);
         String var3 = var2 == -1 ? var1 : var1.substring(0, var2);
         String var4 = var2 == -1 ? "" : var1.substring(var2 + 1);
         if (!e(var3)) {
            return var0;
         }

         String var12 = d(var3);
         String var5 = var12.toLowerCase(Locale.ROOT);
         if (CHAT_MSG_COMMANDS.contains(var5)) {
            return var4.isEmpty() ? var12 : var12 + " " + var4;
         }

         if (!var4.isEmpty()) {
            String[] var6 = var4.split(" ", -1);
            StringBuilder var7 = new StringBuilder(var12);

            for (String var11 : var6) {
               var7.append(' ');
               if (e(var11)) {
                  var7.append(d(var11));
               } else {
                  var7.append(var11);
               }
            }

            return var7.toString();
         } else {
            return var12;
         }
      } else {
         return var0;
      }
   }

   private static String h(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0.trim();
         String var2 = var1.toLowerCase(Locale.ROOT);
         String var3 = null;
         if (var2.startsWith("ah sell ")) {
            var3 = var1.substring(0, "ah sell ".length());
         } else if (var2.startsWith("ah dsell ")) {
            var3 = var1.substring(0, "ah dsell ".length());
         } else if (var2.startsWith("ah bsell ")) {
            var3 = var1.substring(0, "ah bsell ".length());
         }

         if (var3 == null) {
            return var0;
         } else {
            String var4 = var1.substring(var3.length()).trim();
            if (!var4.isEmpty() && !var4.equalsIgnoreCase("auto") && !var4.toLowerCase(Locale.ROOT).startsWith("auto ")) {
               Long var5 = f(var4);
               return var5 != null && var5 > 0L ? var3 + var5 : var0;
            } else {
               return var0;
            }
         }
      } else {
         return var0;
      }
   }

   public static void a(Text var0) {
      if (var0 != null) {
         ChatHelper var1 = k();
         if (var1 != null && var1.isActive() && var1.autoEventGps.isToggled()) {
            var1.c(var0.getString());
         }
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (this.isActive() && this.autoEventGps.isToggled() && var1.getPacket() instanceof GameMessageS2CPacket var2 && var2.content() != null) {
         this.c(var2.content().getString());
      }
   }

   public void c(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var1.replaceAll("(?i)§[0-9a-fk-orx]", "").trim();
         if (!var2.isEmpty()) {
            Matcher var3 = EVENT_PATTERN_1.matcher(var2);
            if (var3.find()) {
               String var9 = j(var3.group(1));
               a(var9, var3.group(2), var3.group(3), var3.group(4));
            } else {
               var3 = EVENT_PATTERN_2.matcher(var2);
               if (var3.find()) {
                  String var4 = j(var3.group(1));
                  a(var4, var3.group(2), var3.group(3), var3.group(4));
               } else {
                  var3 = EVENT_PATTERN_3.matcher(var2);
                  if (var3.find()) {
                     a("Ивент", var3.group(1), var3.group(2), var3.group(3));
                  } else {
                     var3 = EVENT_PATTERN_4.matcher(var2);
                     if (var3.find() && i(var2)) {
                        a("Ивент", var3.group(1), var3.group(2), var3.group(3));
                     } else {
                        var3 = EVENT_PATTERN_5.matcher(var2);
                        if (var3.find() && i(var2)) {
                           a("Ивент", var3.group(1), var3.group(2), var3.group(3));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean i(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);
      return var1.contains("ивент")
         || var1.contains("event")
         || var1.contains("кубик")
         || var1.contains("тайник")
         || var1.contains("аирдроп")
         || var1.contains("метеорит")
         || var1.contains("сундук")
         || var1.contains("вулкан")
         || var1.contains("босс")
         || var1.contains("сокровище")
         || var1.contains("припасов")
         || var1.contains("замок");
   }

   private static String j(String var0) {
      if (var0 == null) {
         return "Ивент";
      }

      String var1 = var0.replace("▶", "").replace("[", "").replace("]", "").trim();
      return var1.isEmpty() ? "Ивент" : var1;
   }

   private static void a(String var0, String var1, String var2, String var3) {
      try {
         int var4 = Integer.parseInt(var1.trim());
         int var5 = Integer.parseInt(var2.trim());
         int var6 = Integer.parseInt(var3.trim());
         String var7 = var4 + ":" + var5 + ":" + var6;
         long var8 = System.currentTimeMillis();
         if (var7.equals(lastEventKey) && var8 - lastEventTime < 2500L) {
            return;
         }

         lastEventKey = var7;
         lastEventTime = var8;
         WaypointStorage.getInstance().b(new Waypoint(var4, var5, var6, var0));
         CommandManager.d("§a[ChatHelper] §fМетка «§b" + var0 + "§f» поставлена: §a" + var4 + " " + var5 + " " + var6);
      } catch (Exception var10) {
      }
   }

   public static String d(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         StringBuilder var1 = new StringBuilder(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            int var4 = "йцукенгшщзхъфывапролджэячсмитьбюёЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮЁ".indexOf(var3);
            if (var4 != -1) {
               var1.append("qwertyuiop[]asdfghjkl;'zxcvbnm,.`QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>~".charAt(var4));
            } else {
               var1.append(var3);
            }
         }

         return var1.toString();
      } else {
         return var0;
      }
   }

   public static boolean e(String var0) {
      if (var0 == null) {
         return false;
      }

      for (int var1 = 0; var1 < var0.length(); var1++) {
         char var2 = var0.charAt(var1);
         if (var2 >= 1072 && var2 <= 1103 || var2 >= 1040 && var2 <= 1071 || var2 == 1105 || var2 == 1025) {
            return true;
         }
      }

      return false;
   }

   public static Long f(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            String var1 = var0.trim().toLowerCase(Locale.ROOT);
            String var7 = var1.replace("кк", "*1000000")
               .replace("kk", "*1000000")
               .replace("к", "*1000")
               .replace("k", "*1000")
               .replace("м", "*1000000")
               .replace("m", "*1000000")
               .replace("х", "*")
               .replace("x", "*")
               .replaceAll("\\s+", "");
            if (var7.isEmpty()) {
               return null;
            } else {
               double var2 = new ChatHelper.a(var7).a();
               if (!Double.isNaN(var2) && !Double.isInfinite(var2)) {
                  long var4 = Math.round(var2);
                  return var4 <= 0L ? null : var4;
               } else {
                  return null;
               }
            }
         } catch (Exception var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void a(DrawContext var0, TextFieldWidget var1) {
      if (var0 != null && var1 != null) {
         ChatHelper var2 = k();
         if (var2 != null && var2.isActive() && var2.ahCalculator.isToggled()) {
            String var3 = var1.getText();
            if (var3 != null && !var3.isBlank()) {
               String var4 = var3.trim();
               String var5 = var4.toLowerCase(Locale.ROOT);
               if (var2.russianLayout.isToggled()) {
                  if (var5.startsWith("/фр ыудд ") || var5.startsWith("/фр sell ") || var5.startsWith("/ah ыудд ")) {
                     var4 = "/ah sell " + var4.substring(var4.indexOf(32, var4.indexOf(32) + 1) + 1).trim();
                     var5 = var4.toLowerCase(Locale.ROOT);
                  } else if (var5.startsWith("/фр dsell ") || var5.startsWith("/фр bsell ")) {
                     var4 = "/ah " + var4.substring("/фр ".length()).trim();
                     var5 = var4.toLowerCase(Locale.ROOT);
                  }
               }

               String var6 = null;
               if (var5.startsWith("/ah sell ")) {
                  var6 = "/ah sell ";
               } else if (var5.startsWith("/ah dsell ")) {
                  var6 = "/ah dsell ";
               } else if (var5.startsWith("/ah bsell ")) {
                  var6 = "/ah bsell ";
               }

               if (var6 != null) {
                  String var7 = var4.substring(var6.length()).trim();
                  if (!var7.isEmpty() && !var7.equalsIgnoreCase("auto") && !var7.toLowerCase(Locale.ROOT).startsWith("auto ")) {
                     Long var8 = f(var7);
                     if (var8 != null && var8 > 0L) {
                        String var9 = a(var8);
                        float var10 = Msdf.a(MsdfManager.BOLD, var9, 5.5F);
                        float var11 = 4.5F;
                        float var12 = var10 + var11 * 2.0F;
                        float var13 = 12.0F;
                        float var14 = var1.getX() + 1.0F;
                        float var15 = var1.getY() - var13 - 3.0F;
                        RenderUtil.a(var14, var15, var12, var13, 3.5F, new Color[]{new Color(15, 17, 24, 220)});
                        Color var16 = Theme.getInstance().getAccent();
                        RenderUtil.a(var14, var15, var12, var13, 3.5F, 0.5F, new Color[]{new Color(var16.getRed(), var16.getGreen(), var16.getBlue(), 180)});
                        RenderUtil.a(var0, MsdfManager.BOLD, var14 + var11, var15 + 3.0F, var9, 5.5F, new Color(245, 245, 250));
                     }
                  }
               }
            }
         }
      }
   }

   public static String a(long var0) {
      return String.format(Locale.GERMANY, "%,d", var0);
   }

   static class a {
      private final String str;
      private int pos = -1;
      private int ch;

      public a(String var1) {
         this.str = var1;
      }

      private void b() {
         this.ch = ++this.pos < this.str.length() ? this.str.charAt(this.pos) : -1;
      }

      private boolean a(int var1) {
         while (this.ch == 32) {
            this.b();
         }

         if (this.ch == var1) {
            this.b();
            return true;
         } else {
            return false;
         }
      }

      public double a() {
         this.b();
         double var1 = this.c();
         return this.pos < this.str.length() ? Double.NaN : var1;
      }

      private double c() {
         double var1 = this.d();

         while (true) {
            while (this.a(43)) {
               var1 += this.d();
            }

            if (!this.a(45)) {
               return var1;
            }

            var1 -= this.d();
         }
      }

      private double d() {
         double var1 = this.e();

         while (true) {
            while (this.a(42)) {
               var1 *= this.e();
            }

            if (!this.a(47)) {
               return var1;
            }

            double var3 = this.e();
            if (var3 == 0.0) {
               return Double.NaN;
            }

            var1 /= var3;
         }
      }

      private double e() {
         if (this.a(43)) {
            return this.e();
         }

         if (this.a(45)) {
            return -this.e();
         }

         int var3 = this.pos;
         double var1;
         if (this.a(40)) {
            var1 = this.c();
            if (!this.a(41)) {
               return Double.NaN;
            }
         } else {
            if ((this.ch < 48 || this.ch > 57) && this.ch != 46) {
               return Double.NaN;
            }

            while (this.ch >= 48 && this.ch <= 57 || this.ch == 46) {
               this.b();
            }

            try {
               var1 = Double.parseDouble(this.str.substring(var3, this.pos));
            } catch (Exception var5) {
               return Double.NaN;
            }
         }

         return var1;
      }
   }
}
