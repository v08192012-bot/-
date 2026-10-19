package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;

@NewFunction(name = "NoRender", description = "Убирает выбранные элементы рендера", category = Category.RENDER)
public class NoRender extends Function {
   public final BooleanSetting fire = new BooleanSetting("Огонь", false);
   public final BooleanSetting negativeEffects = new BooleanSetting("Плохие эффекты", false);
   public final BooleanSetting blockOverlay = new BooleanSetting("Оверлей в блоке", false);
   public final BooleanSetting cameraClip = new BooleanSetting("Отображение от F5", false);
   public final BooleanSetting particles = new BooleanSetting("Частицы", false);
   public final BooleanSetting weather = new BooleanSetting("Погода", false);
   public final BooleanSetting clouds = new BooleanSetting("Облака", false);
   public final BooleanSetting blockEntities = new BooleanSetting("Блок-сущности", false);
   public final BooleanSetting shadows = new BooleanSetting("Тени", false);
   public final BooleanSetting totemAnimation = new BooleanSetting("Анимацию тотема", false);
   public final BooleanSetting hurtCam = new BooleanSetting("Тряску при уроне", false);
   public final BooleanSetting vignette = new BooleanSetting("Виньетка", false);
   public final BooleanSetting bossbar = new BooleanSetting("Боссбар", false);
   public final BooleanSetting witherHearts = new BooleanSetting("Чёрные сердца (иссушение)", false);
   public final BooleanSetting blindness = new BooleanSetting("Слепота", false);
   private boolean prevShadows = true;
   private boolean prevCaptured = false;

   public NoRender() {
      this.addSettings(
         this.fire,
         this.negativeEffects,
         this.blockOverlay,
         this.cameraClip,
         this.particles,
         this.weather,
         this.clouds,
         this.blockEntities,
         this.shadows,
         this.totemAnimation,
         this.hurtCam,
         this.vignette,
         this.bossbar,
         this.witherHearts,
         this.blindness
      );
   }

   public static NoRender k() {
      return FunctionManager.a(NoRender.class);
   }

   private static boolean a(BooleanSetting var0) {
      NoRender var1 = k();
      return var1 != null && var1.isActive() && var0 != null && var0.isToggled();
   }

   public static boolean l() {
      return a(k() == null ? null : k().fire);
   }

   public static boolean m() {
      return a(k() == null ? null : k().blockOverlay);
   }

   public static boolean n() {
      return a(k() == null ? null : k().cameraClip);
   }

   public static boolean o() {
      return a(k() == null ? null : k().vignette);
   }

   public static boolean p() {
      return a(k() == null ? null : k().negativeEffects);
   }

   public static boolean q() {
      return a(k() == null ? null : k().particles);
   }

   public static boolean r() {
      return a(k() == null ? null : k().weather);
   }

   public static boolean s() {
      return a(k() == null ? null : k().clouds);
   }

   public static boolean t() {
      return a(k() == null ? null : k().blockEntities);
   }

   public static boolean u() {
      return a(k() == null ? null : k().totemAnimation);
   }

   public static boolean v() {
      return a(k() == null ? null : k().hurtCam);
   }

   public static boolean w() {
      return a(k() == null ? null : k().bossbar);
   }

   public static boolean y() {
      return a(k() == null ? null : k().witherHearts);
   }

   public static boolean z() {
      return a(k() == null ? null : k().blindness);
   }

   @Override
   public void onEnable() {
      this.prevCaptured = false;
      this.x();
   }

   @Override
   public void onDisable() {
      if (mc.options != null && this.prevCaptured) {
         mc.options.getEntityShadows().setValue(this.prevShadows);
      }

      this.prevCaptured = false;
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.x();
   }

   private void x() {
      if (mc.options != null) {
         if (!this.prevCaptured) {
            this.prevShadows = mc.options.getEntityShadows().getValue();
            this.prevCaptured = true;
         }

         boolean var1 = mc.options.getEntityShadows().getValue();
         boolean var2 = this.shadows.isToggled();
         if (var1 != var2) {
            mc.options.getEntityShadows().setValue(var2);
         }
      }
   }
}
