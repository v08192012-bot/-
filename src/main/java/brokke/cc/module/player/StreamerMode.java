package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.manager.FriendManager;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.TextSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.TextFactoryEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@NewFunction(name = "StreamerMode", description = "Скрывает некоторые данные на экране", category = Category.PLAYER)
public class StreamerMode extends Function {
   public final TextSetting nameProtectName = new TextSetting("Никнейм", "Shelka new");
   public final BooleanSetting nameProtect = new BooleanSetting("Скрывать ник", true);
   public final BooleanSetting nameProtectReplaceFriendNicknames = new BooleanSetting("Скрывать друзей", true);
   public final BooleanSetting hideAnarchy = new BooleanSetting("Скрывать гриф", true);
   public final TextSetting groupSetting = new TextSetting("Группа", "Мл.Сотрудник");
   public final BooleanSetting hideStats = new BooleanSetting("Скрывать статистику", true);
   private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
   private static final Style YELLOW_BOLD = Style.EMPTY.withColor(Formatting.YELLOW).withBold(true);
   private static final Style WHITE_STYLE = Style.EMPTY.withColor(Formatting.WHITE);
   private static final Style GOLD_STYLE = Style.EMPTY.withColor(Formatting.GOLD);
   private static final Pattern GROUP_PATTERN = Pattern.compile("(?iu)(?:Группа|Донат)\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern KILLS_PATTERN = Pattern.compile("(?iu)Убийст\\p{L}*\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern DEATHS_PATTERN = Pattern.compile("(?iu)Смерт\\p{L}*\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern PING_PATTERN = Pattern.compile("(?iu)Пинг\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern SAPPHIRE_PATTERN = Pattern.compile("(?iu)Сапфир\\p{L}*\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern COINS_PATTERN = Pattern.compile("(?iu)Монет\\p{L}*\\s*[:.][ \\t]*([^\\n]*)");
   private static final Pattern ANARCHY_PATTERN = Pattern.compile("(?iu)(?:Клан|Соло|Дуо|Трио|Гриф|Анархи[яиюеей])[ \\-]*(?:Лайт|PvP|ФУ|Гул)?[ \\-]*#?\\s*\\d+");
   private static final Pattern HASH_NUM_PATTERN = Pattern.compile("#\\s*\\d+\\s*✖?");
   private static final Pattern RCT_INPUT_PATTERN = Pattern.compile("(?i)([\\./]?(?:rct|an)\\s+)(\\d+)");
   private static final Map<String, Pattern> NAME_PATTERNS = new ConcurrentHashMap<>();
   private final Map<String, String> stringCache = new LinkedHashMap<String, String>(512, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, String> var1) {
         return this.size() > 512;
      }
   };

   public StreamerMode() {
      this.addSettings(this.nameProtectName, this.nameProtect, this.nameProtectReplaceFriendNicknames, this.hideAnarchy, this.groupSetting, this.hideStats);
   }

   @EventHandler
   public void onTextFactory(TextFactoryEvent var1) {
      if (this.isActive()) {
         String var2 = this.l();
         if (this.nameProtect.getValue()) {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
               var1.replaceText(mc.getSession().getUsername(), var2);
            }

            if (mc.player != null) {
               String var3 = mc.player.getName().getString();
               if (var3 != null && !var3.isEmpty() && (mc.getSession() == null || !var3.equalsIgnoreCase(mc.getSession().getUsername()))) {
                  var1.replaceText(var3, var2);
               }
            }
         }

         if (this.nameProtectReplaceFriendNicknames.getValue()) {
            for (String var4 : FriendManager.b()) {
               var1.replaceText(var4, var2);
            }
         }
      }
   }

   public static boolean k() {
      StreamerMode var0 = p();
      return var0 != null && var0.isActive();
   }

   public static String a(String var0) {
      StreamerMode var1 = p();
      return var1 != null && var0 != null && !var0.isEmpty() ? var1.b(var0) : var0;
   }

   public static Text a(Text var0) {
      StreamerMode var1 = p();
      return var1 != null && var0 != null ? var1.b(var0) : var0;
   }

   private static StreamerMode p() {
      StreamerMode var0 = FunctionManager.a(StreamerMode.class);
      return var0 != null && var0.isActive() ? var0 : null;
   }

   public String l() {
      String var1 = this.nameProtectName.getValue();
      return var1 != null && !var1.isBlank() ? var1 : "Shelka new";
   }

   public String m() {
      String var1 = this.groupSetting.getValue();
      return var1 != null && !var1.isBlank() ? var1 : "Мл.Сотрудник";
   }

   public String n() {
      return "§e§l" + this.m() + "§r";
   }

   public boolean o() {
      return this.hideAnarchy.getValue();
   }

   public String b(String var1) {
      if (var1 == null || var1.isEmpty()) {
         return var1;
      }

      if (this.isActive() && mc.player != null && mc.world != null) {
         String var2 = this.e(var1);
         String var3 = this.stringCache.get(var2);
         if (var3 != null) {
            return var3;
         }

         String var4 = var1;
         String var5 = this.l();
         String var6 = this.n();
         if (this.nameProtect.getValue()) {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
               var4 = this.a(var4, mc.getSession().getUsername(), var5);
            }

            if (mc.player != null) {
               String var7 = mc.player.getName().getString();
               if (var7 != null && !var7.isEmpty() && (mc.getSession() == null || !var7.equalsIgnoreCase(mc.getSession().getUsername()))) {
                  var4 = this.a(var4, var7, var5);
               }
            }
         }

         if (this.nameProtectReplaceFriendNicknames.getValue()) {
            for (String var8 : FriendManager.b()) {
               var4 = this.a(var4, var8, var5);
            }
         }

         if (this.hideAnarchy.getValue()) {
            var4 = this.b(var4, var5, var6);
         }

         this.stringCache.put(var2, var4);
         return var4;
      } else {
         return var1;
      }
   }

   private String a(String var1, String var2, String var3) {
      return var2 != null && !var2.isBlank() ? c(var2).matcher(var1).replaceAll(Matcher.quoteReplacement(var3)) : var1;
   }

   private String b(String var1, String var2, String var3) {
      if (var1 == null) {
         return null;
      }

      String var4 = var1;
      var4 = var4.replaceAll("(?iu)(?:Группа|Донат)\\s*[:.][ \\t]*(?=(?:§[0-9A-FK-ORXa-fk-orx])*+\\S)[^\\n]*", Matcher.quoteReplacement("Группа: " + var3));
      var4 = var4.replaceAll("(?m)^\\s*Нет\\s*$", "");
      var4 = var4.replaceAll("(?i)Убыли", "Убийство");
      var4 = var4.replaceAll("(?i)Убывство", "Убийство");
      var4 = var4.replaceAll("(?i)Убылист", "Убийство");
      var4 = var4.replaceAll("(?i)Смертель", "Смертей");
      var4 = var4.replaceAll("(?i)Смертен", "Смертей");
      String var12 = var4.replaceAll("(?i)Санфиров", "Сапфиров");
      if (this.hideStats.getValue()) {
         var4 = var12.replaceAll("(?iu)Убийст\\p{L}*\\s*[:.]\\s*\\d+\\s*[^\\n]*", "§fУбийство: 0§r");
         var4 = var4.replaceAll("(?iu)Смерт\\p{L}*\\s*[:.]\\s*\\d+\\s*\\+?\\s*[^\\n]*", "§fСмертей: 0§r");
         var4 = var4.replaceAll("(?iu)Пинг\\s*[:.]\\s*\\d+\\s*[ms]?\\s*[^\\n]*", "§fПинг: 0 ms§r");
         var4 = var4.replaceAll("(?iu)Сапфир\\p{L}*\\s*[:.]\\s*\\d+\\s*[^\\n]*", "§fСапфиров: 0§r");
         var12 = var4.replaceAll("(?iu)Монет\\p{L}*\\s*[:.]\\s*[\\d,\\s]+[^\\n]*", "§fМонеток: §r");
      }

      var4 = ANARCHY_PATTERN.matcher(var12).replaceAll(Matcher.quoteReplacement("§6brokke.cc§r"));
      var4 = HASH_NUM_PATTERN.matcher(var4).replaceAll(Matcher.quoteReplacement("§6brokke.cc§r"));
      var4 = var4.replaceAll("(?i)([\\./]?(?:rct|an)\\s+)\\d+", "$1§0██§r");
      var4 = var4.replaceAll("Соло-Лайт", "");
      var4 = var4.replaceAll("Клан-PvP", "");
      var4 = var4.replaceAll("Дуо-Лайт", "");
      var4 = var4.replaceAll("Трио-Лайт", "");
      var4 = var4.replaceAll("СолоЛайт", "");
      var4 = var4.replaceAll("КланPvP", "");
      var4 = var4.replaceAll("ДуоЛайт", "");
      var4 = var4.replaceAll("ТриоЛайт", "");
      var4 = var4.replaceAll("КланЛайт", "");
      var4 = var4.replaceAll("✖", "");
      var4 = var4.replaceAll("✕", "");
      var4 = var4.replaceAll("×", "");
      var4 = var4.replaceAll("⚠️", "");
      var4 = var4.replaceAll("•", "");
      var4 = var4.replaceAll("[+\\-]\\s*", "");
      var4 = var4.replaceAll("←+", "");
      var4 = var4.replaceAll("→+", "");
      var4 = var4.replaceAll("↔+", "");
      var4 = var4.replaceAll(" {3,}", "  ");
      var4 = var4.replaceAll(" \\n", "\n");
      var4 = var4.replaceAll("\\n ", "\n");
      var4 = var4.replaceAll("(?m)^  ", " ");
      return var4.replaceAll("\\n{3,}", "\n\n");
   }

   public Text b(Text var1) {
      if (var1 == null) {
         return null;
      }

      if (this.isActive() && mc.player != null && mc.world != null) {
         List var2 = c(var1);
         if (var2.isEmpty()) {
            return var1;
         }

         boolean var3 = false;
         String var4 = this.l();
         if (this.nameProtect.getValue()) {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
               var3 |= a(var2, mc.getSession().getUsername(), var4);
            }

            if (mc.player != null) {
               String var5 = mc.player.getName().getString();
               if (var5 != null && !var5.isEmpty() && (mc.getSession() == null || !var5.equalsIgnoreCase(mc.getSession().getUsername()))) {
                  var3 |= a(var2, var5, var4);
               }
            }
         }

         if (this.nameProtectReplaceFriendNicknames.getValue()) {
            for (String var6 : FriendManager.b()) {
               var3 |= a(var2, var6, var4);
            }
         }

         if (this.hideAnarchy.getValue()) {
            var3 |= this.a(var2, var4);
         }

         return var3 ? b(var2) : var1;
      } else {
         return var1;
      }
   }

   private boolean a(List<StreamerMode.a> var1, String var2) {
      boolean var3 = false;
      boolean var4 = var3 | a(var1, GROUP_PATTERN, 1, this.m(), YELLOW_BOLD);
      if (this.hideStats.getValue()) {
         var3 = var4 | a(var1, KILLS_PATTERN, 1, "0", WHITE_STYLE);
         var3 |= a(var1, DEATHS_PATTERN, 1, "0", WHITE_STYLE);
         var3 |= a(var1, PING_PATTERN, 1, "0 ms", WHITE_STYLE);
         var3 |= a(var1, SAPPHIRE_PATTERN, 1, "0", WHITE_STYLE);
         var4 = var3 | a(var1, COINS_PATTERN, 1, "миллионер", GOLD_STYLE);
      }

      var3 = var4 | a(var1, ANARCHY_PATTERN, 0, "brokke.cc", GOLD_STYLE);
      var3 |= a(var1, HASH_NUM_PATTERN, 0, "brokke.cc", GOLD_STYLE);
      return var3 | a(var1, RCT_INPUT_PATTERN, 2, "██", Style.EMPTY.withColor(Formatting.BLACK));
   }

   private static Pattern c(String var0) {
      return NAME_PATTERNS.computeIfAbsent(var0, StreamerMode::d);
   }

   private static Pattern d(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         if (var2 > 0) {
            var1.append("(?:§[0-9A-FK-ORXa-fk-orx])*+");
         }

         char var3 = var0.charAt(var2);
         char var4 = Character.toUpperCase(var3);
         char var5 = Character.toLowerCase(var3);
         var1.append('[').append(a(var5));
         if (var4 != var5) {
            var1.append(a(var4));
         }

         if (var4 >= 'A' && var4 <= 'Z') {
            var1.append(a("ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ".charAt(var4 - 'A')));
         }

         var1.append(']');
      }

      return Pattern.compile(var1.toString());
   }

   private static String a(char var0) {
      return "\\x{" + Integer.toHexString(var0) + "}";
   }

   private static boolean a(List<StreamerMode.a> var0, String var1, String var2) {
      return var1 != null && !var1.isBlank() ? a(var0, c(var1), 0, var2, null) : false;
   }

   private static List<StreamerMode.a> c(Text var0) {
      ArrayList var1 = new ArrayList();
      var0.visit((var1xx, var2) -> {
         for (int var3 = 0; var3 < var2.length(); var3++) {
            var1.add(new StreamerMode.a(var2.charAt(var3), var1xx));
         }

         return Optional.empty();
      }, Style.EMPTY);
      return var1;
   }

   private static String a(List<StreamerMode.a> var0) {
      StringBuilder var1 = new StringBuilder(var0.size());

      for (StreamerMode.a var3 : var0) {
         var1.append(var3.getC());
      }

      return var1.toString();
   }

   private static Text b(List<StreamerMode.a> var0) {
      MutableText var1 = Text.empty();
      if (var0.isEmpty()) {
         return var1;
      }

      StringBuilder var2 = new StringBuilder();
      Style var3 = var0.get(0).getStyle();

      for (StreamerMode.a var5 : var0) {
         if (!var5.getStyle().equals(var3)) {
            var1.append(Text.literal(var2.toString()).setStyle(var3));
            var2.setLength(0);
            var3 = var5.getStyle();
         }

         var2.append(var5.getC());
      }

      if (var2.length() > 0) {
         var1.append(Text.literal(var2.toString()).setStyle(var3));
      }

      return var1;
   }

   private static Style a(List<StreamerMode.a> var0, int var1) {
      if (var1 < var0.size()) {
         return var0.get(var1).getStyle();
      } else {
         return var1 > 0 && var1 - 1 < var0.size() ? var0.get(var1 - 1).getStyle() : Style.EMPTY;
      }
   }

   private static void a(List<StreamerMode.a> var0, int var1, int var2, String var3, Style var4) {
      ArrayList var5 = new ArrayList(var3.length());
      int var6 = Math.max(1, var2 - var1);
      int var7 = var3.length();

      for (int var8 = 0; var8 < var7; var8++) {
         Style var9;
         if (var4 != null) {
            var9 = var4;
         } else {
            int var10 = var1 + Math.min(var6 - 1, (int)((long)var8 * var6 / var7));
            var9 = a(var0, var10);
         }

         var5.add(new StreamerMode.a(var3.charAt(var8), var9));
      }

      var0.subList(var1, var2).clear();
      var0.addAll(var1, var5);
   }

   private static boolean a(List<StreamerMode.a> var0, Pattern var1, int var2, String var3, Style var4) {
      String var5 = a(var0);
      Matcher var6 = var1.matcher(var5);
      ArrayList var7 = new ArrayList();

      while (var6.find()) {
         if (var6.start(var2) >= 0) {
            var7.add(new int[]{var6.start(var2), var6.end(var2)});
         }
      }

      if (var7.isEmpty()) {
         return false;
      }

      boolean var8 = false;

      for (int var9 = var7.size() - 1; var9 >= 0; var9--) {
         int[] var10 = (int[])var7.get(var9);
         String var11 = var5.substring(var10[0], var10[1]);
         if (!var11.equals(var3)) {
            a(var0, var10[0], var10[1], var3, var4);
            var8 = true;
         }
      }

      return var8;
   }

   private String e(String var1) {
      String var2 = mc.getSession() != null ? mc.getSession().getUsername() : "";
      int var3 = FriendManager.b() != null ? FriendManager.b().hashCode() : 0;
      return var2
         + this.l()
         + this.n()
         + this.nameProtect.getValue()
         + this.nameProtectReplaceFriendNicknames.getValue()
         + this.hideAnarchy.getValue()
         + this.hideStats.getValue()
         + var3
         + var1;
   }

   record a(char c, Style style) {
      public char getC() {
         return this.c;
      }

      public Style getStyle() {
         return this.style;
      }
   }
}
