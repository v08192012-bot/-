package brokke.cc.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimationSystem {
   private static final float MIN_DELTA_SECONDS = 1.0E-4F;
   private static final float MAX_DELTA_SECONDS = 0.06666667F;
   private static final AnimationSystem INSTANCE = new AnimationSystem();
   private final Object lock = new Object();
   private final List<AnimationSystem.AnimatedObject> animatedObjects = new ArrayList<>();
   private long lastTickNanos = System.nanoTime();
   private float lastDeltaSeconds = 0.0F;

   private AnimationSystem() {
   }

   public static AnimationSystem getInstance() {
      return INSTANCE;
   }

   public void tick() {
      long nowNanos = System.nanoTime();
      long elapsedNanos = nowNanos - this.lastTickNanos;
      this.lastTickNanos = nowNanos;
      if (elapsedNanos < 0L) {
         elapsedNanos = 0L;
      }

      float deltaSeconds = (float)elapsedNanos / 1.0E9F;
      if (deltaSeconds < MIN_DELTA_SECONDS) {
         deltaSeconds = MIN_DELTA_SECONDS;
      } else if (deltaSeconds > MAX_DELTA_SECONDS) {
         deltaSeconds = MAX_DELTA_SECONDS;
      }

      this.lastDeltaSeconds = deltaSeconds;
      synchronized (this.lock) {
         if (!this.animatedObjects.isEmpty()) {
            Iterator<AnimationSystem.AnimatedObject> iterator = this.animatedObjects.iterator();

            while (iterator.hasNext()) {
               AnimationSystem.AnimatedObject animatedObject = iterator.next();
               boolean active = animatedObject.update(deltaSeconds);
               if (!active) {
                  iterator.remove();
               }
            }
         }
      }
   }

   public float getLastDeltaSeconds() {
      return this.lastDeltaSeconds;
   }

   public void register(AnimationSystem.AnimatedObject animatedObject) {
      if (animatedObject != null) {
         synchronized (this.lock) {
            if (!this.animatedObjects.contains(animatedObject)) {
               this.animatedObjects.add(animatedObject);
            }
         }
      }
   }

   public void unregister(AnimationSystem.AnimatedObject animatedObject) {
      if (animatedObject != null) {
         synchronized (this.lock) {
            this.animatedObjects.remove(animatedObject);
         }
      }
   }

   public interface AnimatedObject {
      boolean update(float deltaSeconds);
   }
}
