package cmode.module.render;

import cmode.manager.FriendManager;
import cmode.module.Category;
import cmode.module.Function;
import cmode.module.NewFunction;
import cmode.render.RenderUtil;
import cmode.render.TexturePipeline;
import cmode.setting.BooleanSetting;
import cmode.setting.NumberSetting;
import cmode.system.events.EventHandler;
import cmode.system.events.EventPriority;
import cmode.system.events.impl.Render2DEvent;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@NewFunction(name = "Arrows", description = "Зелёные стрелки, указывающие направление на друзей", category = Category.RENDER)
public class Arrows extends Function {
   private static final Identifier POINTER_TEX = Identifier.of("cmode", "textures/pointer.png");
   private static final Color FRIEND_COLOR = new Color(85, 255, 85);
   private final NumberSetting arrowSize = new NumberSetting("Размер стрелки", 7.0, 5.0, 15.0, 1.0);
   private final NumberSetting centerOffset = new NumberSetting("Отступ от центра", 30.0, 20.0, 50.0, 1.0);
   private final BooleanSetting glow = new BooleanSetting("Свечение", true);
   private float smoothedYaw;
   private long lastFrameTime = System.currentTimeMillis();

   public Arrows() {
      this.addSettings(this.arrowSize, this.centerOffset, this.glow);
   }

   private static int a(int var0, float var1) {
      int var2 = (int)((var0 >> 24 & 0xFF) * var1);
      var2 = Math.clamp(var2, 0, 255);
      return var2 << 24 | var0 & 16777215;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onRender2D(Render2DEvent var1) {
      if (mc.player == null || mc.world == null || mc.options.hudHidden) {
         return;
      }

      AbstractTexture var2 = mc.getTextureManager().getTexture(POINTER_TEX);
      if (var2 == null) {
         return;
      }

      GpuTextureView var3 = var2.getGlTextureView();
      if (var3 == null) {
         return;
      }

      long var4 = System.currentTimeMillis();
      float var6 = Math.min(100.0F, Math.max(1.0F, (float)(var4 - this.lastFrameTime))) / 1000.0F;
      this.lastFrameTime = var4;
      this.smoothedYaw = this.smoothedYaw + MathHelper.wrapDegrees(mc.player.getYaw() - this.smoothedYaw) * (1.0F - (float)Math.exp(-15.0F * var6));
      float var7 = var1.getTickCounter().getTickProgress(true);
      Vec3d var8 = mc.player.getLerpedPos(var7);
      float var9 = RenderUtil.a() / 2.0F;
      float var10 = RenderUtil.b() / 2.0F;
      float var11 = (float)this.centerOffset.getValue();
      float var12 = (float)this.arrowSize.getValue();
      Matrix4f var13 = RenderUtil.c();
      int var14 = FRIEND_COLOR.getRGB();

      for (PlayerEntity var15 : mc.world.getPlayers()) {
         if (var15 != mc.player && var15.isAlive() && FriendManager.a(var15.getName().getString())) {
            Vec3d var16 = var15.getLerpedPos(var7);
            float var17 = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(var16.x - var8.x, var8.z - var16.z)) - this.smoothedYaw);
            float var18 = (float)Math.toRadians(var17);
            float var19 = var9 + (float)Math.sin(var18) * var11;
            float var20 = var10 - (float)Math.cos(var18) * var11;

            if (this.glow.isToggled()) {
               int var21 = a(var14, 1.0F);
               float var22 = var12 * 1.65F;
               float var23 = var12 * 1.35F;
               float var24 = var12 * 1.1F;
               TexturePipeline.d(var13, var19 - var22 / 2.0F, var20 - var22 / 2.0F, var22, var22, var3, a(var21, 0.35F), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var18);
               TexturePipeline.d(var13, var19 - var23 / 2.0F, var20 - var23 / 2.0F, var23, var23, var3, a(var21, 0.65F), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var18);
               TexturePipeline.d(var13, var19 - var24 / 2.0F, var20 - var24 / 2.0F, var24, var24, var3, var21, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var18);
            } else {
               TexturePipeline.c(var13, var19 - var12 / 2.0F, var20 - var12 / 2.0F, var12, var12, var3, var14, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var18);
            }
         }
      }
   }
}
