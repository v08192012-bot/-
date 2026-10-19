package brokke.cc.irc;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;

public class IrcNotification {
   private final String title;
   private final String sender;
   private final String message;
   private final String time;
   private final long duration;
   private final long startTime;
   private final Animation anim = new Animation(0.0F);
   private final Animation yAnim = new Animation(0.0F);
   private boolean yInitialized;

   public IrcNotification(String var1, String var2, String var3, String var4, long var5) {
      this.title = var1;
      this.sender = var2;
      this.message = var3;
      this.time = var4;
      this.duration = var5;
      this.startTime = System.currentTimeMillis();
      this.anim.setEasing(Easing.SPRING_SOFT);
      this.yAnim.setEasing(Easing.SPRING_SOFT);
   }

   public String getTitle() {
      return this.title;
   }

   public String getSender() {
      return this.sender;
   }

   public String getMessage() {
      return this.message;
   }

   public String getTime() {
      return this.time;
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

   public boolean isYInitialized() {
      return this.yInitialized;
   }

   public void setYInitialized(boolean var1) {
      this.yInitialized = var1;
   }

   public boolean j() {
      return System.currentTimeMillis() - this.startTime >= this.duration;
   }

   public float k() {
      long var1 = System.currentTimeMillis() - this.startTime;
      return Math.clamp(1.0F - (float)var1 / (float)this.duration, 0.0F, 1.0F);
   }
}
