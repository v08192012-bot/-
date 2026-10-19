package brokke.cc.animation;

@FunctionalInterface
public interface EasingFunction {
   float ease(float progress);

   static EasingFunction linear() {
      return progress -> progress;
   }

   default EasingFunction andThen(EasingFunction next) {
      return next == null ? this : progress -> next.ease(this.ease(progress));
   }
}
