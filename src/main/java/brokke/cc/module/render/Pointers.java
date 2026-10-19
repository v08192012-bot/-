package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.manager.FriendManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.Render2DEvent;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@NewFunction(name = "Arrows", description = "Указывает стрелками направление к игрокам", category = Category.RENDER)
public class Pointers extends Function {
   private static final Identifier POINTER_TEX = Identifier.of("brokke", "textures/pointer.png");
   private static final Identifier BLOOM_TEX = Identifier.of("brokke", "textures/targetesp/bloom.png");
   private static final float GHOST_ALPHA_MULT = 0.6F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
   private final BooleanSetting friendFilter = new BooleanSetting("Фильтр по друзьям", false);
   private final BooleanSetting hideNaked = new BooleanSetting("Не отображать голых", false);
   private final NumberSetting arrowSize = new NumberSetting("Размер стрелки", 7.0, 5.0, 15.0, 1.0);
   private final NumberSetting centerOffset = new NumberSetting("Отступ от центра", 30.0, 20.0, 50.0, 1.0);
   private final BooleanSetting glow = new BooleanSetting("Свечение", true);
   private float opacity;
   private long lastFrameTime = System.currentTimeMillis();

   public Pointers() {
      this.addSettings(this.friendFilter, this.hideNaked, this.arrowSize, this.centerOffset, this.glow);
   }

   private boolean a(PlayerEntity var1) {
      for (EquipmentSlot var5 : ARMOR_SLOTS) {
         if (!var1.getEquippedStack(var5).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static int a(int var0, float var1) {
      int var2 = (int)((var0 >> 24 & 0xFF) * var1);
      var2 = Math.clamp(var2, 0, 255);
      return var2 << 24 | var0 & 16777215;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onRender2D(Render2DEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         AbstractTexture var2 = mc.getTextureManager().getTexture(POINTER_TEX);
         if (var2 != null) {
            GpuTextureView var3 = var2.getGlTextureView();
            if (var3 != null) {
               AbstractTexture var4 = mc.getTextureManager().getTexture(BLOOM_TEX);
               GpuTextureView var5 = var4 != null ? var4.getGlTextureView() : null;
               long var6 = System.currentTimeMillis();
               float var8 = Math.min(100.0F, Math.max(1.0F, (float)(var6 - this.lastFrameTime))) / 1000.0F;
               this.lastFrameTime = var6;
               float var9 = mc.player.getYaw();
               this.opacity = this.opacity + MathHelper.wrapDegrees(var9 - this.opacity) * (1.0F - (float)Math.exp(-15.0F * var8));
               float var10 = var1.getTickCounter().getTickProgress(true);
               Vec3d var11 = mc.player.getLerpedPos(var10);
               float var12 = RenderUtil.a();
               float var13 = RenderUtil.b();
               float var14 = var12 / 2.0F;
               float var15 = var13 / 2.0F;
               float var16 = (float)this.centerOffset.getValue();
               float var17 = (float)this.arrowSize.getValue();
               Matrix4f var18 = RenderUtil.c();
               Color var19 = Theme.getInstance().getAccent();
               int var20 = var19 != null ? var19.getRGB() : -11617650;

               for (PlayerEntity var22 : mc.world.getPlayers()) {
                  if (var22 != mc.player && var22.isAlive() && (!this.hideNaked.isToggled() || !this.a(var22))) {
                     boolean var23 = FriendManager.a(var22.getName().getString());
                     if (!this.friendFilter.isToggled() || var23) {
                        Vec3d var24 = var22.getLerpedPos(var10);
                        float var25 = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(var24.x - var11.x, var11.z - var24.z)) - this.opacity);
                        float var26 = (float)Math.toRadians(var25);
                        float var27 = var14 + (float)Math.sin(var26) * var16;
                        float var28 = var15 - (float)Math.cos(var26) * var16;
                        int var29 = var23 ? -11141291 : var20;
                        if (this.glow.isToggled()) {
                           int var30 = Math.clamp(153L, 0, 255);
                           int var31 = var30 << 24 | var29 & 16777215;
                           if (var5 != null) {
                              float var32 = var17 * 3.2F;
                              float var36 = var32 * 1.6F;
                           }

                           float var35 = var17 * 1.65F;
                           float var36 = var17 * 1.35F;
                           float var34 = var17 * 1.1F;
                           TexturePipeline.d(
                              var18, var27 - var35 / 2.0F, var28 - var35 / 2.0F, var35, var35, var3, a(var31, 0.35F), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var26
                           );
                           TexturePipeline.d(
                              var18, var27 - var36 / 2.0F, var28 - var36 / 2.0F, var36, var36, var3, a(var31, 0.65F), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var26
                           );
                           TexturePipeline.d(var18, var27 - var34 / 2.0F, var28 - var34 / 2.0F, var34, var34, var3, var31, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var26);
                        } else {
                           TexturePipeline.c(var18, var27 - var17 / 2.0F, var28 - var17 / 2.0F, var17, var17, var3, var29, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var26);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
