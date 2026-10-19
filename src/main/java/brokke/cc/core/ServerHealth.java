package brokke.cc.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public final class ServerHealth {
   private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

   private ServerHealth() {
   }

   public static float a(PlayerEntity var0) {
      if (var0 == null) {
         return -1.0F;
      } else {
         Text var1 = var0.getCustomName();
         if (var1 == null) {
            return -1.0F;
         } else {
            Matcher var2 = NUMBER_PATTERN.matcher(var1.getString());

            try {
               return var2.find() ? (float)Integer.parseInt(var2.group()) : -1.0F;
            } catch (NumberFormatException var3) {
               return -1.0F;
            }
         }
      }
   }
}
