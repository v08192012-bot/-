package brokke.cc.animation;

import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class NotificationManager {
   private static final String ID = "notifications";
   private static final NotificationManager INSTANCE = new NotificationManager();
   private static final List<Notification> notifications = new CopyOnWriteArrayList<>();
   private static final Set<String> alertedEffects = new HashSet<>();
   private static final int MAX_NOTIFICATIONS = 5;
   private static final float PILL_H = 15.0F;
   private static final float PILL_GAP = 3.0F;
   private static final float SWITCH_W = 15.0F;
   private static final float SWITCH_H = 9.0F;
   private static final float BADGE_PAD_LEFT = 3.5F;
   private static final float TEXT_PAD_RIGHT = 6.5F;
   private static boolean enabled = true;
   private final Animation placeholderAnim = new Animation();
   private float centerX = -1.0F;
   private float y = -1.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;

   public static float[] getPosition() {
      return new float[]{INSTANCE.centerX, INSTANCE.y};
   }

   public static void setPosition(float centerX, float y) {
      INSTANCE.centerX = centerX;
      INSTANCE.y = y;
      INSTANCE.initPos = centerX >= 0.0F && y >= 0.0F;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean enabledValue) {
      enabled = enabledValue;
   }

   public static void writeConfig(JsonObject config) {
      config.addProperty("enabled", enabled);
   }

   public static void readConfig(JsonObject config) {
      if (config.has("enabled")) {
         enabled = config.get("enabled").getAsBoolean();
      }
   }

   public static void register() {
      EventBus.register(INSTANCE);
   }

   public static synchronized void addNotification(String title, String message, NotificationType type, long durationMillis) {
      if (enabled) {
         while (notifications.size() >= 5) {
            notifications.remove(0);
         }

         notifications.add(new Notification(title, message, type, durationMillis));
      }
   }

   public static void notifyEnabled(String moduleName) {
      addNotification(moduleName, "включен!", NotificationType.ENABLE, 2000L);
   }

   public static void notifyDisabled(String moduleName) {
      addNotification(moduleName, "выключен!", NotificationType.DISABLE, 2000L);
   }

   public static void warnEffectExpiring(String effectName, int remainingSeconds) {
      addNotification("Зелье", effectName + " закончится через " + remainingSeconds + " сек.", NotificationType.WARNING, 3500L);
   }

   private void handleDragging(MinecraftClient client, float left, float top, float width, float height) {
      if (!(client.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
      } else {
         double scaleFactor = client.getWindow().getScaleFactor();
         float mouseX = (float)(client.mouse.getX() / scaleFactor) * RenderUtil.e();
         float mouseY = (float)(client.mouse.getY() / scaleFactor) * RenderUtil.e();
         boolean pressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == 1;
         if (pressed
            && !this.wasPressed
            && !HudConfig.b(mouseX, mouseY)
            && mouseX >= left - 2.0F
            && mouseX <= left + width + 2.0F
            && mouseY >= top - 2.0F
            && mouseY <= top + height + 2.0F) {
            this.dragging = true;
            this.dragDX = mouseX - this.centerX;
            this.dragDY = mouseY - this.y;
         }

         boolean wasDragging = this.dragging;
         if (!pressed) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = pressed;
         if (this.dragging) {
            float draggedLeft = mouseX - this.dragDX - width * 0.5F;
            float[] snappedPosition = HudConfig.b("notifications", draggedLeft, mouseY - this.dragDY, width, height);
            this.centerX = snappedPosition[0] + width * 0.5F;
            this.y = snappedPosition[1];
         } else if (wasDragging) {
            ConfigManager.onSettingChanged();
         }
      }
   }

   private static int withAlpha(int color, float opacity) {
      int alpha = Math.clamp(Math.round(opacity), 0, 255);
      return color & 16777215 | alpha << 24;
   }

   private static int interpolateColor(int startColor, int endColor, float progress) {
      progress = Math.clamp(progress, 0.0F, 1.0F);
      int startRed = startColor >> 16 & 0xFF;
      int startGreen = startColor >> 8 & 0xFF;
      int startBlue = startColor & 0xFF;
      int endRed = endColor >> 16 & 0xFF;
      int endGreen = endColor >> 8 & 0xFF;
      int endBlue = endColor & 0xFF;
      int red = (int)(startRed + (endRed - startRed) * progress);
      int green = (int)(startGreen + (endGreen - startGreen) * progress);
      int blue = (int)(startBlue + (endBlue - startBlue) * progress);
      return red << 16 | green << 8 | blue;
   }

   private static void drawSwitch(Matrix4f matrix, float x, float y, float progress, int accentColor, float opacity) {
      float trackRadius = 4.5F;
      float thumbSize = 6.6F;
      float thumbRadius = 3.3F;
      float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
      int trackColor = withAlpha(interpolateColor(2632500, accentColor, clampedProgress), (210.0F + 10.0F * clampedProgress) * opacity);
      int borderColor = withAlpha(interpolateColor(4540757, accentColor, clampedProgress), (140.0F - 20.0F * clampedProgress) * opacity);
      float thumbStartX = x + 1.2F;
      float thumbEndX = x + 15.0F - 1.2F - thumbSize;
      float thumbX = thumbStartX + (thumbEndX - thumbStartX) * clampedProgress;
      float thumbY = y + 1.2F;
      RenderUtil.a(matrix, x, y, 15.0F, 9.0F, trackRadius, trackColor);
      RenderUtil.a(matrix, x, y, 15.0F, 9.0F, trackRadius, 0.5F, borderColor);
      RenderUtil.a(matrix, thumbX, thumbY, thumbSize, thumbSize, thumbRadius, withAlpha(16777215, 255.0F * opacity));
   }

   @EventHandler
   public void onUpdate(UpdateEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (enabled && client.player != null) {
         Set<String> activeEffectKeys = new HashSet<>();

         for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (!effect.isInfinite()) {
               int remainingTicks = effect.getDuration();
               int amplifier = effect.getAmplifier();
               String effectKey = effect.getEffectType().getIdAsString() + ":" + amplifier;
               activeEffectKeys.add(effectKey);
               if (remainingTicks > 0 && remainingTicks <= 140 && !alertedEffects.contains(effectKey)) {
                  alertedEffects.add(effectKey);
                  String effectName = getEffectDisplayName(effect);
                  warnEffectExpiring(effectName, 7);
               }
            }
         }

         alertedEffects.retainAll(activeEffectKeys);
      } else {
         alertedEffects.clear();
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (enabled && !client.options.hudHidden) {
         float screenWidth = RenderUtil.a();
         float screenHeight = RenderUtil.b();

         while (notifications.size() > 5) {
            notifications.remove(0);
         }

         for (int expiredIndex = notifications.size() - 1; expiredIndex >= 0; expiredIndex--) {
            Notification expiringNotification = notifications.get(expiredIndex);
            boolean expired = expiringNotification.isExpired();
            expiringNotification.getAnim().animate(expired ? 0.0F : 1.0F, 0.0F, 6.0F);
            float fadeProgress = Math.clamp(expiringNotification.getAnim().getValue(), 0.0F, 1.0F);
            if (expired && fadeProgress <= 0.01F) {
               notifications.remove(expiredIndex);
            }
         }

         boolean chatOpen = client.currentScreen instanceof ChatScreen;
         if (notifications.isEmpty()) {
            this.placeholderAnim.setEasing(Easing.SPRING_SOFT);
            this.placeholderAnim.animate(chatOpen ? 1.0F : 0.0F, 0.0F, 5.5F);
            float placeholderOpacity = Math.clamp(this.placeholderAnim.getValue(), 0.0F, 1.0F);
            if (!(placeholderOpacity <= 0.01F)) {
               float separatorWidth = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
               String placeholderText = "Уведомления";
               float placeholderTextWidth = Msdf.a(MsdfManager.BOLD, placeholderText, 6.5F);
               float placeholderWidth = 22.5F + separatorWidth + 4.0F + placeholderTextWidth + 6.5F;
               float placeholderScale = HudConfig.a("notifications");
               float scaledPlaceholderWidth = placeholderWidth * placeholderScale;
               float scaledPlaceholderHeight = 15.0F * placeholderScale;
               if (!this.initPos || this.centerX < 0.0F || this.y < 0.0F) {
                  this.centerX = screenWidth * 0.5F;
                  this.y = screenHeight * 0.5F + 25.0F;
                  this.initPos = true;
               }

               float placeholderLeft = this.centerX - scaledPlaceholderWidth * 0.5F;
               this.handleDragging(client, placeholderLeft, this.y, scaledPlaceholderWidth, scaledPlaceholderHeight);
               HudConfig.c("notifications", placeholderLeft, this.y, scaledPlaceholderWidth, scaledPlaceholderHeight);
               Matrix4f placeholderMatrix = RenderUtil.d();
               placeholderMatrix.translate(this.centerX, this.y, 0.0F);
               placeholderMatrix.scale(placeholderScale, placeholderScale, 1.0F);
               placeholderMatrix.translate(-this.centerX, -this.y, 0.0F);
               float placeholderX = this.centerX - placeholderWidth * 0.5F;
               float placeholderRadius = 7.5F;
               int placeholderBackground = withAlpha(0, 180.0F * placeholderOpacity);
               HudConfig.a(placeholderMatrix, "notifications", placeholderX, this.y, placeholderWidth, 15.0F, placeholderRadius, placeholderBackground);
               float placeholderSwitchX = placeholderX + 3.5F;
               float placeholderSwitchY = this.y + 3.0F;
               Color placeholderAccent = Theme.getInstance().getAccent();
               int placeholderAccentRgb = placeholderAccent != null ? placeholderAccent.getRGB() & 16777215 : 5164408;
               drawSwitch(placeholderMatrix, placeholderSwitchX, placeholderSwitchY, 1.0F, placeholderAccentRgb, placeholderOpacity);
               float separatorX = placeholderSwitchX + 15.0F + 4.0F;
               float separatorY = this.y + (15.0F - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) * 0.5F;
               float placeholderTextX = separatorX + separatorWidth + 4.0F;
               float placeholderTextY = this.y + (15.0F - Msdf.a(MsdfManager.BOLD, 6.5F)) * 0.5F;
               Msdf.b();
               Msdf.a(placeholderMatrix, MsdfManager.MONTSERRAT, "|", separatorX, separatorY, 6.5F, withAlpha(7238530, 160.0F * placeholderOpacity));
               Msdf.a(placeholderMatrix, MsdfManager.BOLD, placeholderText, placeholderTextX, placeholderTextY, 6.5F, withAlpha(15790325, 255.0F * placeholderOpacity));
               Msdf.c();
               HudConfig.c("notifications");
            }
         } else {
            float maxPillWidth = 0.0F;

            for (Notification measuredNotification : notifications) {
               String measuredTitle = measuredNotification.getTitle() != null ? measuredNotification.getTitle() : "";
               String measuredMessage = measuredNotification.getMessage() != null ? measuredNotification.getMessage() : "";
               float measuredTitleWidth = Msdf.a(MsdfManager.BOLD, measuredTitle, 6.5F);
               float measuredSpaceWidth = !measuredTitle.isEmpty() && !measuredMessage.isEmpty() ? Msdf.a(MsdfManager.BOLD, " ", 6.5F) : 0.0F;
               float measuredMessageWidth = !measuredMessage.isEmpty() ? Msdf.a(MsdfManager.BOLD, measuredMessage, 6.5F) : 0.0F;
               float measuredPillWidth = 24.0F + measuredTitleWidth + measuredSpaceWidth + measuredMessageWidth + 6.5F;
               maxPillWidth = Math.max(maxPillWidth, measuredPillWidth);
            }

            float stackHeight = notifications.size() * 15.0F + Math.max(0, notifications.size() - 1) * 3.0F;
            float scale = HudConfig.a("notifications");
            float scaledStackWidth = maxPillWidth * scale;
            float scaledStackHeight = stackHeight * scale;
            if (!this.initPos || this.centerX < 0.0F || this.y < 0.0F) {
               this.centerX = screenWidth * 0.5F;
               this.y = screenHeight * 0.5F + 25.0F;
               this.initPos = true;
            }

            float stackLeft = this.centerX - scaledStackWidth * 0.5F;
            this.handleDragging(client, stackLeft, this.y, scaledStackWidth, scaledStackHeight);
            HudConfig.c("notifications", stackLeft, this.y, scaledStackWidth, scaledStackHeight);
            Matrix4f matrix = RenderUtil.d();
            matrix.translate(this.centerX, this.y, 0.0F);
            matrix.scale(scale, scale, 1.0F);
            matrix.translate(-this.centerX, -this.y, 0.0F);
            float targetOffsetY = 0.0F;
            float cornerRadius = 7.5F;
            Color accent = Theme.getInstance().getAccent();
            int accentRgb = accent != null ? accent.getRGB() & 16777215 : 5164408;

            for (int index = 0; index < notifications.size(); index++) {
               Notification notification = notifications.get(index);
               if (!notification.isYInitialized()) {
                  notification.getYAnim().reset(targetOffsetY);
                  notification.setYInitialized(true);
               }

               notification.getYAnim().animate(targetOffsetY, 0.0F, 6.0F);
               float animatedOffsetY = notification.getYAnim().getValue();
               float opacity = Math.clamp(notification.getAnim().getValue(), 0.0F, 1.0F);
               if (opacity > 0.005F) {
                  String title = notification.getTitle() != null ? notification.getTitle() : "";
                  String message = notification.getMessage() != null ? notification.getMessage() : "";
                  float titleWidth = Msdf.a(MsdfManager.BOLD, title, 6.5F);
                  float spaceWidth = !title.isEmpty() && !message.isEmpty() ? Msdf.a(MsdfManager.BOLD, " ", 6.5F) : 0.0F;
                  float messageWidth = !message.isEmpty() ? Msdf.a(MsdfManager.BOLD, message, 6.5F) : 0.0F;
                  float pillWidth = 24.0F + titleWidth + spaceWidth + messageWidth + 6.5F;
                  float pillX = this.centerX - pillWidth * 0.5F;
                  float pillY = this.y + animatedOffsetY + (1.0F - opacity) * -4.0F;
                  int backgroundColor = withAlpha(0, 180.0F * opacity);
                  HudConfig.a(matrix, pillX, pillY, pillWidth, 15.0F, cornerRadius, backgroundColor);
                  float switchX = pillX + 3.5F;
                  float switchY = pillY + 3.0F;
                  float switchTarget = notification.getType() == NotificationType.DISABLE ? 0.0F : 1.0F;
                  notification.getSwitchAnim().animate(switchTarget, 0.0F, 4.5F, Easing.FIGMA_EASE_IN_OUT);
                  float switchProgress = notification.getSwitchAnim().getValue();
                  int switchColor = accentRgb;
                  int typeColor = notification.getType().getColor().getRGB() & 16777215;
                  if (notification.getType() == NotificationType.WARNING || notification.getType() == NotificationType.INFO) {
                     switchColor = typeColor;
                  }

                  drawSwitch(matrix, switchX, switchY, switchProgress, switchColor, opacity);
                  float textX = switchX + 15.0F + 5.5F;
                  float textY = pillY + (15.0F - Msdf.a(MsdfManager.BOLD, 6.5F)) * 0.5F - 0.25F;
                  Msdf.b();
                  if (!title.isEmpty()) {
                     Msdf.a(matrix, MsdfManager.BOLD, title, textX, textY, 6.5F, withAlpha(16777215, 255.0F * opacity));
                  }

                  if (!message.isEmpty()) {
                     float messageX = textX + titleWidth + spaceWidth;
                     Msdf.a(matrix, MsdfManager.BOLD, message, messageX, textY, 6.5F, withAlpha(typeColor, 255.0F * opacity));
                  }

                  Msdf.c();
               }

               targetOffsetY += 18.0F;
            }

            HudConfig.c("notifications");
         }
      }
   }

   private static String getEffectDisplayName(StatusEffectInstance effect) {
      String translationKey = effect.getTranslationKey();
      String displayName = Text.translatable(translationKey).getString();
      int amplifier = effect.getAmplifier();
      if (amplifier > 0) {
         displayName = displayName + " " + toRomanNumeral(amplifier + 1);
      }

      return displayName;
   }

   private static String toRomanNumeral(int level) {
      return switch (level) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(level);
      };
   }
}
