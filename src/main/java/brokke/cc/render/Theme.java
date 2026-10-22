package brokke.cc.render;

import brokke.cc.config.ConfigManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class Theme {
   private static final Theme INSTANCE = new Theme();
   private Color accent = new Color(78, 186, 142);
   private Color background = new Color(0, 0, 0);
   private Theme.a guiStyle = Theme.a.BLUR;
   /** Режим клик-гуи: классические панели или окно в стиле CS. */
   private Theme.b guiMode = Theme.b.PANELS;
   private final List<Color> palette = new ArrayList<>(
      List.of(new Color(78, 186, 142), new Color(30, 136, 229), new Color(0, 230, 118), new Color(255, 193, 7), new Color(255, 61, 0), new Color(156, 39, 176))
   );

   private Theme() {
   }

   public static Theme getInstance() {
      return INSTANCE;
   }

   public Color getAccent() {
      return this.accent;
   }

   public void a(Color var1) {
      if (var1 != null) {
         this.accent = var1;
         ConfigManager.onSettingChanged();
      }
   }

   public Color c() {
      return new Color(0, 0, 0, 180);
   }

   public void setBackground(Color var1) {
      this.background = var1;
   }

   public Theme.a getGuiStyle() {
      return this.guiStyle;
   }

   public void a(Theme.a var1) {
      if (var1 != null) {
         this.guiStyle = var1;
         ConfigManager.onSettingChanged();
      }
   }

   public Theme.b getGuiMode() {
      return this.guiMode;
   }

   public void a(Theme.b var1) {
      if (var1 != null && var1 != this.guiMode) {
         this.guiMode = var1;
         ConfigManager.onSettingChanged();
      }
   }

   public List<Color> getPalette() {
      return this.palette;
   }

   public void c(Color var1) {
      if (var1 != null) {
         Color var2 = new Color(var1.getRed(), var1.getGreen(), var1.getBlue());

         for (Color var4 : this.palette) {
            if ((var4.getRGB() & 16777215) == (var2.getRGB() & 16777215)) {
               return;
            }
         }

         if (this.palette.size() >= 9) {
            this.palette.remove(this.palette.size() - 1);
         }

         this.palette.add(var2);
         ConfigManager.onSettingChanged();
      }
   }

   public void d(Color var1) {
      if (var1 != null && this.palette.size() > 1) {
         this.palette.removeIf(var1xx -> (var1xx.getRGB() & 16777215) == (var1.getRGB() & 16777215));
         ConfigManager.onSettingChanged();
      }
   }

   public void a(List<Color> var1) {
      if (var1 != null && !var1.isEmpty()) {
         this.palette.clear();
         this.palette.addAll(var1);
      }
   }

   public enum b {
      PANELS("Панели"),
      CS("CS");

      private final String label;

      b(String var1) {
         this.label = var1;
      }

      public String getLabel() {
         return this.label;
      }
   }

   public enum a {
      BLUR("Блюр"),
      NORMAL("Обычный");

      private final String label;

      a(String nullxx) {
         this.label = nullxx;
      }

      public String getLabel() {
         return this.label;
      }
   }
}
