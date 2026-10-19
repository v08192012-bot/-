package brokke.cc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

public class AnarchyUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Pattern HOLYWORLD_LINE = Pattern.compile("#\\s*(\\d{1,3})\\s*-?\\s*\\u25c6");
   private static final Pattern NAMED_ANARCHY = Pattern.compile(
      "(?iu)(?:\\u0430\\u043d\\u0430\\u0440\\u0445(?:\\u0438\\u044f|\\u0438\\u0438)?|lite|\\u043b\\u0430\\u0439\\u0442)[^0-9]{0,16}(\\d{1,3})"
   );
   private static final Pattern FALLBACK_PATTERN = Pattern.compile(
      "(?iu)(?:#|an|\\u0430\\u043d\\u0430\\u0440\\u0445\\u0438\\u044f|\\u043b\\u0430\\u0439\\u0442|lite)\\s*0*(\\d{1,2})(?!\\d)"
   );

   public static int a() {
      if (mc.world == null) {
         return -1;
      }

      Scoreboard var0 = mc.world.getScoreboard();
      if (var0 == null) {
         return -1;
      }

      ScoreboardObjective var1 = var0.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (var1 == null) {
         return -1;
      }

      int var2 = a(var1.getDisplayName().getString());
      if (var2 != -1) {
         return var2;
      }

      for (ScoreboardEntry var4 : var0.getScoreboardEntries(var1)) {
         Team var5 = var0.getScoreHolderTeam(var4.owner());
         String var6 = Team.decorateName(var5, var4.name()).getString();
         int var7 = a(var6);
         if (var7 != -1) {
            return var7;
         }
      }

      return -1;
   }

   public static int a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         Matcher var1 = HOLYWORLD_LINE.matcher(var0);
         if (var1.find()) {
            return b(var1.group(1));
         }

         Matcher var2 = NAMED_ANARCHY.matcher(var0);
         if (var2.find()) {
            return b(var2.group(1));
         }

         Matcher var3 = FALLBACK_PATTERN.matcher(var0);
         return var3.find() ? b(var3.group(1)) : -1;
      } else {
         return -1;
      }
   }

   private static int b(String var0) {
      try {
         int var1 = Integer.parseInt(var0);
         return var1 >= 1 && var1 <= 36 ? var1 : -1;
      } catch (NumberFormatException var2) {
         return -1;
      }
   }

   public static boolean b() {
      return a() != -1;
   }

   public static void a(int var0) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatCommand("an" + var0);
      }
   }
}
