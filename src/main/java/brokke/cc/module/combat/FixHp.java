package brokke.cc.module.combat;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

@NewFunction(name = "FixHP", description = "Чинит отображение HP в неймтегах и таргет худе по scoreboard'у, у невидимых показывает ?", category = Category.COMBAT)
public class FixHp extends Function {
   private static FixHp instance;
   private final BooleanSetting scoreHp = new BooleanSetting("HP из scoreboard", true);
   private final BooleanSetting questionMark = new BooleanSetting("Вопрос у невидимых", true);

   public FixHp() {
      this.addSettings(this.scoreHp, this.questionMark);
   }

   public static FixHp getInstance() {
      if (instance == null) {
         instance = FunctionManager.a(FixHp.class);
      }

      return instance;
   }

   public static float scoreHealth(PlayerEntity var0) {
      FixHp var1 = getInstance();
      if (var1 == null || !var1.isEnabled() || !var1.scoreHp.isToggled() || var0 == null || mc.world == null) {
         return -1.0F;
      } else {
         Scoreboard var2 = mc.world.getScoreboard();
         ScoreboardObjective var3 = var2.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
         if (var3 == null) {
            return -1.0F;
         } else {
            ReadableScoreboardScore var4 = var2.getScore(ScoreHolder.fromName(var0.getNameForScoreboard()), var3);
            return var4 == null ? -1.0F : (float)var4.getScore();
         }
      }
   }

   public static boolean isUnknown(PlayerEntity var0) {
      FixHp var1 = getInstance();
      return var1 != null && var1.isEnabled() && var1.questionMark.isToggled() && var0 != null && var0.isInvisible();
   }
}
