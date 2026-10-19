package cmode.module.render;

import cmode.module.Category;
import cmode.module.Function;
import cmode.module.NewFunction;
import cmode.render.ShaderEspRenderer;
import cmode.setting.BooleanSetting;
import cmode.setting.NumberSetting;
import cmode.system.events.EventHandler;
import cmode.system.events.impl.Render3DEvent;
import java.awt.Color;

@NewFunction(name = "Friends Markers", description = "Зелёная шейдерная подсветка друзей сквозь стены", category = Category.RENDER)
public class FriendsMarkers extends Function {
   public static final Color FRIEND_COLOR = new Color(70, 255, 120);
   private final NumberSetting glowRadius = new NumberSetting("Радиус свечения", 6.0, 1.0, 15.0, 0.5);
   private final NumberSetting glowIntensity = new NumberSetting("Сила свечения", 2.0, 0.5, 5.0, 0.1);
   private final NumberSetting outlineThickness = new NumberSetting("Толщина обводки", 2.0, 0.5, 6.0, 0.5);
   private final NumberSetting outlineStrength = new NumberSetting("Сила обводки", 2.5, 0.5, 5.0, 0.1);
   private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
   private final BooleanSetting fill = new BooleanSetting("Заливка", false);
   private final NumberSetting fillOpacity = new NumberSetting("Непрозрачность заливки", 0.25, 0.05, 1.0, 0.05);

   public FriendsMarkers() {
      this.addSettings(
         this.glowRadius,
         this.glowIntensity,
         this.outlineThickness,
         this.outlineStrength,
         this.throughWalls,
         this.fill,
         this.fillOpacity
      );
      this.fill.onChanged(() -> this.fillOpacity.setVisible(this.fill.isToggled()));
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

   public Color getColor() {
      return FRIEND_COLOR;
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
}
