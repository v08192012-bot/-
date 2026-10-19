package brokke.cc.animation;

import java.awt.Color;

public enum NotificationType {
   ENABLE(new Color(78, 205, 120), "m"),
   DISABLE(new Color(235, 87, 87), "u"),
   WARNING(new Color(242, 153, 74), "w"),
   INFO(new Color(110, 160, 245), "i");

   private final Color color;
   private final String icon;

   NotificationType(Color color, String icon) {
      this.color = color;
      this.icon = icon;
   }

   public Color getColor() {
      return this.color;
   }

   public String getIcon() {
      return this.icon;
   }
}
