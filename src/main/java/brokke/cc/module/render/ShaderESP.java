package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.ShaderEspRenderer;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render3DEvent;
import java.awt.Color;

@NewFunction(name = "ShaderESP", description = "Шейдерная обводка и свечение вокруг игроков", category = Category.RENDER)
public class ShaderESP extends Function {
   private final ModeSetting colorMode = new ModeSetting("Режим цвета", "Градиент", "Статичный", "От темы");
   private final ColorSetting primaryColor = new ColorSetting("Основной цвет", new Color(255, 120, 0));
   private final ColorSetting secondaryColor = new ColorSetting("Второй цвет", new Color(0, 180, 255));
   private final NumberSetting gradientSpeed = new NumberSetting("Скорость градиента", 1.0, 0.1, 3.0, 0.1);
   private final NumberSetting glowRadius = new NumberSetting("Радиус свечения", 6.0, 1.0, 15.0, 0.5);
   private final NumberSetting glowIntensity = new NumberSetting("Сила свечения", 2.0, 0.5, 5.0, 0.1);
   private final NumberSetting outlineThickness = new NumberSetting("Толщина обводки", 2.0, 0.5, 6.0, 0.5);
   private final NumberSetting outlineStrength = new NumberSetting("Сила обводки", 2.5, 0.5, 5.0, 0.1);
   private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
   private final BooleanSetting fill = new BooleanSetting("Заливка", false);
   private final NumberSetting fillOpacity = new NumberSetting("Непрозрачность заливки", 0.25, 0.05, 1.0, 0.05);
   private final BooleanSetting players = new BooleanSetting("Игроки", true);
   private final BooleanSetting friends = new BooleanSetting("Друзья", true);
   private final ColorSetting friendColor = new ColorSetting("Цвет друзей", new Color(60, 255, 130));
   private final BooleanSetting invisibles = new BooleanSetting("Невидимые", true);
   private final BooleanSetting mobs = new BooleanSetting("Мобы", false);
   private final BooleanSetting self = new BooleanSetting("Себя", false);

   public ShaderESP() {
      this.addSettings(
         this.colorMode,
         this.primaryColor,
         this.secondaryColor,
         this.friendColor,
         this.gradientSpeed,
         this.glowRadius,
         this.glowIntensity,
         this.outlineThickness,
         this.outlineStrength,
         this.throughWalls,
         this.fill,
         this.fillOpacity,
         this.players,
         this.friends,
         this.invisibles,
         this.mobs,
         this.self
      );
      this.colorMode.onChanged(this::B);
      this.fill.onChanged(this::B);
      this.friends.onChanged(this::B);
      this.B();
   }

   private void B() {
      boolean var1 = this.colorMode.isSelected("Градиент");
      boolean var2 = this.colorMode.isSelected("Статичный");
      this.primaryColor.setVisible(var1 || var2);
      this.secondaryColor.setVisible(var1);
      this.friendColor.setVisible(this.friends.isToggled());
      this.gradientSpeed.setVisible(var1);
      this.fillOpacity.setVisible(this.fill.isToggled());
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (mc.player != null && mc.world != null) {
         ShaderEspRenderer.a(var1, this);
      }
   }

   @Override
   public void onDisable() {
      ShaderEspRenderer.a();
      super.onDisable();
   }

   public ModeSetting getColorMode() {
      return this.colorMode;
   }

   public ColorSetting getPrimaryColor() {
      return this.primaryColor;
   }

   public ColorSetting getSecondaryColor() {
      return this.secondaryColor;
   }

   public NumberSetting getGradientSpeed() {
      return this.gradientSpeed;
   }

   public NumberSetting getGlowRadius() {
      return this.glowRadius;
   }

   public NumberSetting getGlowIntensity() {
      return this.glowIntensity;
   }

   public NumberSetting getOutlineThickness() {
      return this.outlineThickness;
   }

   public NumberSetting getOutlineStrength() {
      return this.outlineStrength;
   }

   public BooleanSetting getThroughWalls() {
      return this.throughWalls;
   }

   public BooleanSetting getFill() {
      return this.fill;
   }

   public NumberSetting getFillOpacity() {
      return this.fillOpacity;
   }

   public BooleanSetting getPlayers() {
      return this.players;
   }

   public BooleanSetting getFriends() {
      return this.friends;
   }

   public ColorSetting getFriendColor() {
      return this.friendColor;
   }

   public BooleanSetting getInvisibles() {
      return this.invisibles;
   }

   public BooleanSetting getMobs() {
      return this.mobs;
   }

   public BooleanSetting getSelf() {
      return this.self;
   }
}
