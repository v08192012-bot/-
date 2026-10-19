package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.manager.PartyManager;
import brokke.cc.nav.PartyMarker;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.WorldToScreen;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.ui.ClickGui;
import java.awt.Color;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

@NewFunction(name = "Party", description = "Метки пати и союзников", category = Category.MISC)
public class Party extends Function {
   private final BindSetting markKey = new BindSetting("Клавиша метки", -102);
   private final NumberSetting duration = new NumberSetting("Время жизни", 20.0, 1.0, 120.0, 1.0);
   private final BooleanSetting sound = new BooleanSetting("Звук", true);
   private static final ItemStack COMPASS_STACK = new ItemStack(Items.COMPASS);
   private static final float FONT_SIZE = 6.75F;
   private static final float ICON_SIZE = 9.5F;
   private static final float PAD_X = 5.0F;
   private static final float SEP_PAD = 3.5F;
   private static final float GAP = 3.5F;
   private static final float BOX_H = 14.5F;
   private static final float RADIUS = 5.0F;

   public Party() {
      this.addSettings(this.markKey, this.duration, this.sound);
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      PartyManager.getInstance().i();
      long var2 = System.currentTimeMillis();
      long var4 = (long)(this.duration.getValue() * 1000.0);

      for (PartyMarker var7 : PartyManager.getInstance().m()) {
         if (var2 - var7.getCreatedAt() > var4) {
            if ("local".equals(var7.getId())) {
               PartyManager.getInstance().k();
            } else {
               PartyManager.getInstance().c(var7.getId());
            }
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent var1) {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && var1.getAction() == 1) {
         int var2 = this.markKey.getKey() == -1 ? -102 : this.markKey.getKey();
         if (var1.getKey() == var2) {
            this.k();
         }
      }
   }

   private void k() {
      if (mc.player != null && mc.world != null) {
         Party.a var1 = this.a(256.0);
         if (var1 != null) {
            PartyMarker var2 = PartyManager.getInstance().j();
            if (var2 != null && var1.a(var2, mc.world)) {
               PartyManager.getInstance().k();
               if (this.sound.isToggled()) {
                  mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.6F);
               }
            } else {
               String var3 = PartyManager.getInstance().h();
               PartyMarker var4;
               if (var1.player != null) {
                  String var5 = var1.player.getName().getString();
                  var4 = new PartyMarker("local", var3, true, var1.player.getUuid(), var5, var1.player.getEyePos());
               } else {
                  if (var1.blockPos == null) {
                     return;
                  }

                  Vec3d var6 = new Vec3d(var1.blockPos.getX() + 0.5, var1.blockPos.getY() + 1.0, var1.blockPos.getZ() + 0.5);
                  var4 = new PartyMarker("local", var3, false, null, "Метка 1", var6);
               }

               PartyManager.getInstance().a(var4);
               if (this.sound.isToggled()) {
                  mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
               }
            }
         }
      }
   }

   private Party.a a(double var1) {
      if (mc.player != null && mc.world != null) {
         Vec3d var3 = mc.player.getEyePos();
         Vec3d var4 = mc.player.getRotationVec(1.0F);
         Vec3d var5 = var3.add(var4.multiply(var1));
         Box var6 = mc.player.getBoundingBox().stretch(var4.multiply(var1)).expand(1.0);
         double var7 = var1 * var1;
         PlayerEntity var9 = null;

         for (PlayerEntity var11 : mc.world.getEntitiesByClass(PlayerEntity.class, var6, var0 -> var0 != mc.player && var0.isAlive() && !var0.isSpectator())) {
            Box var12 = var11.getBoundingBox().expand(0.35);
            Optional var13 = var12.raycast(var3, var5);
            if (var13.isPresent()) {
               double var14 = var3.squaredDistanceTo((Vec3d)var13.get());
               if (var14 < var7) {
                  var7 = var14;
                  var9 = var11;
               }
            }
         }

         BlockHitResult var16 = mc.world.raycast(new RaycastContext(var3, var5, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         double var17 = var16.getType() == Type.BLOCK ? var3.squaredDistanceTo(var16.getPos()) : Double.MAX_VALUE;
         if (var9 != null && var7 < var17) {
            return new Party.a(var9, null, var9.getEyePos());
         } else {
            return var16.getType() == Type.BLOCK ? new Party.a(null, var16.getBlockPos(), var16.getPos()) : null;
         }
      } else {
         return null;
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         Collection var2 = PartyManager.getInstance().m();
         if (!var2.isEmpty()) {
            DrawContext var3 = var1.getContext();
            long var4 = System.currentTimeMillis();
            long var6 = (long)(this.duration.getValue() * 1000.0);
            int var8 = -46261;

            for (PartyMarker var10 : (Iterable<PartyMarker>)(Iterable<?>)(var2)) {
               if (var4 - var10.getCreatedAt() > var6) {
                  if ("local".equals(var10.getId())) {
                     PartyManager.getInstance().k();
                  } else {
                     PartyManager.getInstance().c(var10.getId());
                  }
               } else if (var10.b(mc.world)) {
                  Vec3d var11 = var10.a(mc.world);
                  float[] var12 = WorldToScreen.a(var11);
                  if (var12 != null) {
                     double var13 = Math.sqrt(mc.player.squaredDistanceTo(var11));
                     String var15 = Math.round(var13) + "м";
                     if (var10.isPlayerTarget()) {
                        String var18 = var10.getLabel() != null && !var10.getLabel().isBlank() ? var10.getLabel() : "Игрок";
                        this.a(var3, var12[0], var12[1], var18, var15, var13, var8);
                     } else {
                        String var16 = var10.getLabel() != null && !var10.getLabel().isBlank() ? var10.getLabel() : "Метка 1";
                        String var17 = var10.getSenderName() != null && !var10.getSenderName().isBlank()
                           ? var10.getSenderName()
                           : PartyManager.getInstance().h();
                        this.a(var3, var12[0], var12[1], var16, var17, var15, var13, var8);
                     }
                  }
               }
            }
         }
      }
   }

   private void a(DrawContext var1, float var2, float var3, String var4, String var5, String var6, double var7, int var9) {
      float var10 = (float)MathHelper.clamp(1.0 - var7 / 150.0, 0.75, 1.0);
      float var11 = 22.0F;
      float var12 = 11.0F;
      float var13 = Msdf.a(MsdfManager.BOLD, var4, 6.75F);
      float var14 = Msdf.a(MsdfManager.BOLD, var6, 6.75F);
      float var15 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
      float var16 = var13 + 3.5F + var14;
      float var17 = Msdf.a(MsdfManager.MONTSERRAT, "От: ", 5.75F);
      float var18 = Msdf.a(MsdfManager.BOLD, var5, 5.75F);
      float var19 = var17 + var18;
      float var20 = Math.max(var16, var19);
      float var21 = 5.0F + var12 + 3.5F + var15 + 3.5F + var20 + 5.0F;
      float var22 = var2 - var21 / 2.0F;
      float var23 = var3 - var11;
      if (!ClickGui.a(var2 - var21 * var10 / 2.0F, var3 - var11 * var10, var21 * var10, var11 * var10)) {
         Matrix4f var24 = this.a(var2, var3, var10);
         HudConfig.a(var24, "party_b_" + (var4 + var5).hashCode(), var22, var23, var21, var11, 5.0F, new Color(0, 0, 0, 190));
         float var25 = var22 + 5.0F;
         float var26 = var23 + (var11 - var12) / 2.0F;
         float var27 = 2.0F / mc.getWindow().getScaleFactor();
         float var28 = var12 / 16.0F * var10 * var27;
         float var29 = var2 + (var25 + var12 / 2.0F - var2) * var10;
         float var30 = var3 + (var26 + var12 / 2.0F - var3) * var10;
         Matrix3x2fStack var31 = var1.getMatrices();
         var31.pushMatrix();
         var31.translate(var29 * var27, var30 * var27);
         var31.scale(var28, var28);
         var31.translate(-8.0F, -8.0F);

         try {
            var1.drawItem(COMPASS_STACK, 0, 0);
         } catch (Exception var35) {
         }

         var31.popMatrix();
         float var32 = var25 + var12 + 3.5F;
         float var33 = var23 + (var11 - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
         Msdf.a(var24, MsdfManager.MONTSERRAT, "|", var32, var33, 6.5F, -1603374206);
         float var34 = var32 + var15 + 3.5F;
         Msdf.a(var24, MsdfManager.BOLD, var4, var34, var23 + 3.0F, 6.75F, -1);
         Msdf.a(var24, MsdfManager.BOLD, var6, var34 + var13 + 3.5F, var23 + 3.0F, 6.75F, var9);
         Msdf.a(var24, MsdfManager.MONTSERRAT, "От: ", var34, var23 + 12.0F, 5.75F, -1600085761);
         Msdf.a(var24, MsdfManager.BOLD, var5, var34 + var17, var23 + 12.0F, 5.75F, -1);
      }
   }

   private void a(DrawContext var1, float var2, float var3, String var4, String var5, double var6, int var8) {
      float var9 = (float)MathHelper.clamp(1.0 - var6 / 150.0, 0.75, 1.0);
      float var10 = Msdf.a(MsdfManager.BOLD, var4, 6.75F);
      float var11 = Msdf.a(MsdfManager.BOLD, var5, 6.75F);
      float var12 = Msdf.a(MsdfManager.MONTSERRAT, "|", 6.5F);
      float var13 = 18.0F + var12 + 3.5F + var10 + 3.5F + var11 + 5.0F;
      float var14 = var2 - var13 / 2.0F;
      float var15 = var3 - 14.5F;
      if (!ClickGui.a(var2 - var13 * var9 / 2.0F, var3 - 14.5F * var9, var13 * var9, 14.5F * var9)) {
         Matrix4f var16 = this.a(var2, var3, var9);
         HudConfig.a(var16, "party_p_" + var4.hashCode(), var14, var15, var13, 14.5F, 5.0F, new Color(0, 0, 0, 190));
         float var17 = var14 + 5.0F;
         float var18 = var15 + 2.5F;
         float var19 = 2.0F / mc.getWindow().getScaleFactor();
         float var20 = 0.59375F * var9 * var19;
         float var21 = var2 + (var17 + 4.75F - var2) * var9;
         float var22 = var3 + (var18 + 4.75F - var3) * var9;
         Matrix3x2fStack var23 = var1.getMatrices();
         var23.pushMatrix();
         var23.translate(var21 * var19, var22 * var19);
         var23.scale(var20, var20);
         var23.translate(-8.0F, -8.0F);

         try {
            var1.drawItem(COMPASS_STACK, 0, 0);
         } catch (Exception var27) {
         }

         var23.popMatrix();
         float var24 = var17 + 9.5F + 3.5F;
         float var25 = var15 + (14.5F - Msdf.a(MsdfManager.MONTSERRAT, 6.5F)) / 2.0F;
         Msdf.a(var16, MsdfManager.MONTSERRAT, "|", var24, var25, 6.5F, -1603374206);
         var24 += var12 + 3.5F;
         float var26 = var15 + (14.5F - Msdf.a(MsdfManager.BOLD, 6.75F)) / 2.0F;
         Msdf.a(var16, MsdfManager.BOLD, var4, var24, var26, 6.75F, -1);
         var24 += var10 + 3.5F;
         Msdf.a(var16, MsdfManager.BOLD, var5, var24, var26, 6.75F, var8);
      }
   }

   private Matrix4f a(float var1, float var2, float var3) {
      Matrix4f var4 = RenderUtil.d();
      var4.translate(var1, var2, 0.0F);
      var4.scale(var3, var3, 1.0F);
      var4.translate(-var1, -var2, 0.0F);
      return var4;
   }

   @Override
   public void onDisable() {
      PartyManager.getInstance().clearMarkers();
      super.onDisable();
   }

   record a(PlayerEntity player, BlockPos blockPos, Vec3d hitPos) {
      boolean a(PartyMarker var1, World var2) {
         if (var1.isPlayerTarget() && this.player != null) {
            return this.player.getUuid().equals(var1.getTargetPlayerUuid());
         } else {
            return !var1.isPlayerTarget() && this.blockPos != null && var1.getStaticPos() != null
               ? this.hitPos.squaredDistanceTo(var1.getStaticPos()) < 2.25
               : false;
         }
      }

      public PlayerEntity getPlayer() {
         return this.player;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public Vec3d getHitPos() {
         return this.hitPos;
      }
   }
}
