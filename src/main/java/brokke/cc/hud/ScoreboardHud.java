package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.core.ScoreboardAnimator;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.render.RenderUtil;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventManager;
import brokke.cc.system.events.impl.HudRenderEvent;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class ScoreboardHud {
   private static final ScoreboardHud INSTANCE = new ScoreboardHud();
   private static final String ID = "scoreboard";
   private static boolean enabled = false;
   private static final float PAD_X = 7.0F;
   private static final float PAD_Y = 6.0F;
   private static final float HEADER_GAP = 5.0F;
   private static final float ROW_HEIGHT = 10.0F;
   private static final float MIN_WIDTH = 72.0F;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Animation entryAnim = new Animation();
   private float x = -1.0F;
   private float y = -1.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private float panelW;
   private float panelH;
   private boolean visible;
   private boolean rightAnchored = true;
   private float anchorEdge = 0.0F;
   private boolean anchorReady = false;
   private Text cachedTitle;
   private final List<Text> cachedNames = new ArrayList<>();
   private long lastScoreboardUpdate = 0L;
   private ScoreboardObjective lastObjective = null;

   public static float[] a() {
      if (INSTANCE.initPos && !(INSTANCE.x < 0.0F) && !(INSTANCE.y < 0.0F)) {
         return new float[]{INSTANCE.x, INSTANCE.y};
      }

      float var0 = RenderUtil.a();
      return new float[]{Math.max(7.0F, var0 - 150.0F), 6.0F};
   }

   public static void a(float var0, float var1) {
      if (var0 >= 0.0F && var1 >= 0.0F) {
         INSTANCE.x = var0;
         INSTANCE.y = var1;
         INSTANCE.initPos = true;
         INSTANCE.anchorReady = false;
      }
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      enabled = var0;
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
   }

   public static void b(JsonObject var0) {
      if (var0.has("enabled")) {
         enabled = var0.get("enabled").getAsBoolean();
      }
   }

   public static void c() {
      EventBus.register(INSTANCE);
   }

   private static ScoreboardObjective a(MinecraftClient var0) {
      if (var0 != null && var0.player != null && var0.world != null) {
         Scoreboard var1 = var0.world.getScoreboard();
         Team var2 = var1.getScoreHolderTeam(var0.player.getNameForScoreboard());
         if (var2 != null) {
            Formatting var3 = var2.getColor();
            if (var3 != null) {
               ScoreboardDisplaySlot var4 = ScoreboardDisplaySlot.fromFormatting(var3);
               if (var4 != null) {
                  ScoreboardObjective var5 = var1.getObjectiveForSlot(var4);
                  if (var5 != null) {
                     return var5;
                  }
               }
            }
         }

         return var1.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      } else {
         return null;
      }
   }

   private void a(MinecraftClient var1, float var2, float var3) {
      if (!(var1.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
      } else {
         double var4 = var1.getWindow().getScaleFactor();
         float var6 = (float)(var1.mouse.getX() / var4) * RenderUtil.e();
         float var7 = (float)(var1.mouse.getY() / var4) * RenderUtil.e();
         int var8;
         if ((var8 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1 ? 1 : 0) != 0
            && !this.wasPressed
            && !HudConfig.b(var6, var7)
            && var6 >= this.x - 2.0F
            && var6 <= this.x + var2 + 2.0F
            && var7 >= this.y - 2.0F
            && var7 <= this.y + var3 + 2.0F) {
            this.dragging = true;
            this.dragDX = var6 - this.x;
            this.dragDY = var7 - this.y;
         }

         boolean var9 = this.dragging;
         if (var8 == 0) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            float[] var10 = HudConfig.b("scoreboard", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.x = var10[0];
            this.y = var10[1];
            float var11 = RenderUtil.a();
            this.rightAnchored = this.x + var2 / 2.0F >= var11 / 2.0F;
            this.anchorEdge = this.rightAnchored ? this.x + var2 : this.x;
            this.anchorReady = true;
         } else if (var9) {
            float var12 = RenderUtil.a();
            this.rightAnchored = this.x + var2 / 2.0F >= var12 / 2.0F;
            this.anchorEdge = this.rightAnchored ? this.x + var2 : this.x;
            this.anchorReady = true;
            ConfigManager.onSettingChanged();
         }
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && var2.currentScreen != EventManager.clickGui) {
         ScoreboardObjective var3 = a(var2);
         this.visible = var2.player != null && !var2.options.hudHidden && var3 != null;
         if (this.visible) {
            this.entryAnim.setEasing(Easing.SPRING);
            this.entryAnim.animate(1.0F, 0.0F, 8.0F);
            float var4 = Math.clamp(this.entryAnim.getValue(), 0.0F, 1.0F);
            float var5 = (1.0F - var4) * 6.0F;
            DrawContext var6 = var1.getContext();
            float var7 = 2.0F / var2.getWindow().getScaleFactor();
            float var8 = HudConfig.a("scoreboard");
            long var9;
            if ((var9 = System.currentTimeMillis()) - this.lastScoreboardUpdate > 100L || this.lastObjective != var3 || this.cachedTitle == null) {
               if (this.lastObjective != var3) {
                  ScoreboardAnimator.clearTRACKER();
               }

               this.lastScoreboardUpdate = var9;
               this.lastObjective = var3;
               Scoreboard var11 = var3.getScoreboard();
               List<ScoreboardEntry> var12 = new ArrayList<>(var11.getScoreboardEntries(var3));
               var12.removeIf(ScoreboardEntry::hidden);
               var12.sort(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER));
               if (var12.size() > 15) {
                  var12 = new ArrayList<>(var12.subList(0, 15));
               }

               this.cachedTitle = StreamerMode.a(var3.getDisplayName());
               this.cachedNames.clear();

               for (ScoreboardEntry var14 : var12) {
                  Team var15 = var11.getScoreHolderTeam(var14.owner());
                  Text var16 = StreamerMode.a(Team.decorateName(var15, var14.name()));
                  this.cachedNames.add(var16);
               }
            }

            Text var27 = this.cachedTitle;
            List var28 = this.cachedNames;
            int var29 = var28.size();
            ArrayList var30 = new ArrayList(var29);
            int var31 = var2.textRenderer.getWidth(var27);

            for (int var32 = 0; var32 < var29; var32++) {
               Text var17 = ScoreboardAnimator.a(var32, (Text)var28.get(var32), var9);
               var30.add(var17);
               int var18;
               if ((var18 = var2.textRenderer.getWidth(var17)) > var31) {
                  var31 = var18;
               }
            }

            float var33 = Math.max(72.0F, var31 + 14.0F);
            float var34 = 21.0F + (var29 == 0 ? 0.0F : 5.0F + var29 * 10.0F);
            float var35;
            float var19 = (var35 = RenderUtil.a()) > 30.0F ? Math.min(1.0F, (var35 - 10.0F) / (var33 * var7 * var8)) : 1.0F;
            float var20 = var7 * var8 * var19;
            this.panelW = var33 * var20;
            this.panelH = var34 * var20;
            if ((!this.initPos || this.x < 0.0F || this.y < 0.0F) && var35 > 30.0F) {
               this.rightAnchored = true;
               this.anchorEdge = var35 - 6.0F;
               this.x = this.anchorEdge - this.panelW;
               this.y = 6.0F;
               this.initPos = true;
               this.anchorReady = true;
            }

            this.a(var2, this.panelW, this.panelH);
            HudConfig.c("scoreboard", this.x, this.y, this.panelW, this.panelH);
            if (var35 > 30.0F) {
               if (!this.anchorReady) {
                  this.rightAnchored = this.x + this.panelW / 2.0F >= var35 / 2.0F;
                  this.anchorEdge = this.rightAnchored ? this.x + this.panelW : this.x;
                  this.anchorReady = true;
               }

               if (this.dragging) {
                  this.rightAnchored = this.x + this.panelW / 2.0F >= var35 / 2.0F;
                  this.anchorEdge = this.rightAnchored ? this.x + this.panelW : this.x;
               } else {
                  this.x = this.rightAnchored ? this.anchorEdge - this.panelW : this.anchorEdge;
               }

               if (this.x > var35 - 2.0F) {
                  this.x = var35 - 2.0F;
                  if (this.rightAnchored) {
                     this.anchorEdge = this.x + this.panelW;
                  }
               } else if (this.x < 2.0F) {
                  this.x = 2.0F;
                  if (!this.rightAnchored) {
                     this.anchorEdge = this.x;
                  }
               }
            }

            HudConfig.a("scoreboard", this.x, this.y + var5, this.panelW, this.panelH - 1.0F, 6.0F, (int)(175.0F * var4) << 24);
            int var21 = var2.textRenderer.getWidth(var27);
            var6.getMatrices().pushMatrix();
            var6.getMatrices().translate(this.x, this.y + var5);
            var6.getMatrices().scale(var20, var20);
            int var22 = (int)((var33 - var21) * 0.5F);
            byte var23 = 6;
            var6.drawText(var2.textRenderer, var27, var22, var23, -1, false);
            if (var29 > 0) {
               RenderUtil.a(this.x + 7.0F * var20, this.y + 17.0F * var20, this.panelW - 14.0F * var20, 1.0F, 0.5F, 603979775);

               for (int var24 = 0; var24 < var29; var24++) {
                  int var25 = (int)(20.0F + var24 * 10.0F);
                  byte var26 = 7;
                  var6.drawText(var2.textRenderer, (Text)var30.get(var24), var26, var25, -1, false);
               }
            }

            var6.getMatrices().popMatrix();
            HudConfig.c("scoreboard");
         }
      }
   }
}
