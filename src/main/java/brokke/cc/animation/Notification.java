package brokke.cc.animation;

public class Notification {
   private final String title;
   private final String message;
   private final NotificationType type;
   private final long duration;
   private final long startTime;
   private final Animation anim = new Animation(0.0F);
   private final Animation yAnim = new Animation(0.0F);
   private final Animation switchAnim;
   private boolean yInitialized;

   public Notification(String title, String message, NotificationType type, long duration) {
      this.title = title;
      this.message = message;
      this.type = type;
      this.duration = duration;
      this.startTime = System.currentTimeMillis();
      this.anim.setEasing(Easing.SPRING_SOFT);
      this.yAnim.setEasing(Easing.SPRING_SOFT);
      this.switchAnim = new Animation(type == NotificationType.DISABLE ? 1.0F : 0.0F);
      this.switchAnim.setEasing(Easing.FIGMA_EASE_IN_OUT);
   }

   public String getTitle() {
      return this.title;
   }

   public String getMessage() {
      return this.message;
   }

   public NotificationType getType() {
      return this.type;
   }

   public long getDuration() {
      return this.duration;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public Animation getAnim() {
      return this.anim;
   }

   public Animation getYAnim() {
      return this.yAnim;
   }

   public Animation getSwitchAnim() {
      return this.switchAnim;
   }

   public boolean isYInitialized() {
      return this.yInitialized;
   }

   public void setYInitialized(boolean initialized) {
      this.yInitialized = initialized;
   }

   public boolean isExpired() {
      return System.currentTimeMillis() - this.startTime >= this.duration;
   }

   public float getRemainingProgress() {
      long elapsedMillis = System.currentTimeMillis() - this.startTime;
      return Math.clamp(1.0F - (float)elapsedMillis / (float)this.duration, 0.0F, 1.0F);
   }

   public String getDisplayText() {
      if (this.title != null && !this.title.isEmpty()) {
         return this.message != null && !this.message.isEmpty() ? this.title + " " + this.message : this.title;
      } else {
         return this.message != null ? this.message : "";
      }
   }
}
