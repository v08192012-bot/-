package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.config.HudConfig;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.setting.MultiSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render3DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

@NewFunction(name = "Prediction", description = "Отображает траекторию полёта эндер-жемчуга, трезубцев и т.п.", category = Category.RENDER)
public class Prediction extends Function {
   private static final Identifier BLOOM_TEX = Identifier.of("brokke", "textures/bloom.png");
   private static final Identifier HIT_TEX = Identifier.of("brokke", "textures/hit.png");
   private static final VertexFormat FORMAT_BLOOM = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static RenderPipeline bloomPipeline;
   private static RenderPipeline bloomPipelineWalls;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformScratch;
   private static GpuBuffer quadsVbo;
   private static final int UNIFORM_SIZE = 64;
   private final List<Prediction.a> predicted = new CopyOnWriteArrayList<>();
   private final MultiSetting entities = new MultiSetting("Отображать", "Эндер-жемчуги", "Трезубцы", "Снежки", "Стрелы", "Зелья");

   public Prediction() {
      this.entities.setOption("Эндер-жемчуги", true);
      this.entities.setOption("Трезубцы", true);
      this.entities.setOption("Снежки", true);
      this.entities.setOption("Стрелы", true);
      this.entities.setOption("Зелья", true);
      this.addSettings(this.entities);
   }

   @Override
   public void onDisable() {
      this.predicted.clear();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         this.predicted.clear();

         for (Entity var3 : mc.world.getEntities()) {
            this.a(var3);
         }
      }
   }

   private void a(Entity var1) {
      if (this.b(var1)) {
         if (var1 instanceof ProjectileEntity var2 && var2.getOwner() == null) {
            List<AbstractClientPlayerEntity> var3 = new ArrayList<>(mc.world.getPlayers());
            if (!var3.isEmpty()) {
               Collections.sort(var3, Comparator.comparingDouble(var1x -> var1x.distanceTo(var2)));
               var2.setOwner(var3.getFirst());
            }
         }

         ArrayList var12 = new ArrayList();
         Vec3d var13 = var1.getEntityPos();
         Vec3d var4 = var1.getVelocity();
         Entity var5 = null;
         int var6 = 0;

         for (int var7 = 0; var7 < 150; var7++) {
            Vec3d var8 = this.b(var1, var4);
            Vec3d var9 = var13.add(var8);
            var6 = var7;
            BlockHitResult var10 = mc.world.raycast(new RaycastContext(var13, var9, ShapeType.COLLIDER, FluidHandling.NONE, var1));
            Entity var11 = this.a(var1, var9);
            if (var11 != null) {
               var12.add(var9);
               var5 = var11;
               break;
            }

            if (var10.getType() != Type.MISS) {
               var12.add(var10.getPos());
               break;
            }

            var12.add(var9);
            var13 = var9;
            var4 = var8;
         }

         if (!var12.isEmpty()) {
            this.predicted.add(new Prediction.a(var1, var12, var6, var5));
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (mc.player != null && mc.world != null && !this.predicted.isEmpty()) {
         k();
         if (bloomPipeline != null && uniformBuffer != null) {
            AbstractTexture var2 = mc.getTextureManager().getTexture(BLOOM_TEX);
            AbstractTexture var3 = mc.getTextureManager().getTexture(HIT_TEX);
            if (var2 != null && var3 != null) {
               GpuTextureView var4 = var2.getGlTextureView();
               GpuTextureView var5 = var3.getGlTextureView();
               if (var4 != null && var5 != null) {
                  Camera var6 = var1.getCamera();
                  Vec3d var7 = var6.getCameraPos();
                  float var8 = var1.getTickDelta();
                  Matrix4f var9 = this.l();
                  a(var9);
                  Color var10 = Theme.getInstance().getAccent();
                  float var11 = var10.getRed() / 255.0F;
                  float var12 = var10.getGreen() / 255.0F;
                  float var13 = var10.getBlue() / 255.0F;
                  BufferBuilder var14 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                  int var15 = 0;

                  for (Prediction.a var17 : this.predicted) {
                     if (!var17.vectors.isEmpty()) {
                        Vec3d var18 = var17.vectors.getFirst();
                        Vec3d var19 = a(var17.entity, var8);
                        if (var19.distanceTo(mc.player.getEyePos()) > 2.0) {
                           for (int var20 = 0; var20 < 10; var20++) {
                              float var21 = var20 / 10.0F;
                              Vec3d var22 = var19.add(var18.subtract(var19).multiply(var21));
                              this.a(var22, var14, (float)var18.distanceTo(var19) / 5.5F, 1.0F, var6, var7, var11, var12, var13);
                              this.a(var22, var14, (float)var18.distanceTo(var19) * 0.75F, 0.08F, var6, var7, var11, var12, var13);
                              var15 += 2;
                           }
                        }

                        for (Vec3d var27 : var17.vectors) {
                           if (var27.distanceTo(mc.player.getEyePos()) > 2.0) {
                              for (int var29 = 0; var29 < 10; var29++) {
                                 float var23 = var29 / 10.0F;
                                 Vec3d var24 = var18.add(var27.subtract(var18).multiply(var23));
                                 this.a(var24, var14, (float)var27.distanceTo(var18) / 5.5F, 1.0F, var6, var7, var11, var12, var13);
                                 this.a(var24, var14, (float)var27.distanceTo(var18) * 0.75F, 0.08F, var6, var7, var11, var12, var13);
                                 var15 += 2;
                              }
                           }

                           var18 = var27;
                        }

                        if (var17.entity instanceof PotionEntity) {
                           float var26 = 9.0F;
                           float var28 = var26 / 2.0F;
                           Vec3d var30 = var17.vectors.getLast();
                           MatrixStack var31 = new MatrixStack();
                           var31.translate((float)(var30.x - var7.x), (float)(var30.y - var7.y), (float)(var30.z - var7.z));
                           var31.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0F));
                           Matrix4f var32 = var31.peek().getPositionMatrix();
                           var14.vertex(var32, -var28, var28, 0.0F).texture(0.0F, 1.0F).color(var11, var12, var13, 1.0F);
                           var14.vertex(var32, var28, var28, 0.0F).texture(1.0F, 1.0F).color(var11, var12, var13, 1.0F);
                           var14.vertex(var32, var28, -var28, 0.0F).texture(1.0F, 0.0F).color(var11, var12, var13, 1.0F);
                           var14.vertex(var32, -var28, -var28, 0.0F).texture(0.0F, 0.0F).color(var11, var12, var13, 1.0F);
                           var15++;
                        }
                     }
                  }

                  if (var15 > 0) {
                     this.a(var14, var4, bloomPipelineWalls);
                  }
               }
            }
         }
      }
   }

   private void a(Vec3d var1, BufferBuilder var2, float var3, float var4, Camera var5, Vec3d var6, float var7, float var8, float var9) {
      MatrixStack var10 = new MatrixStack();
      var10.translate((float)(var1.x - var6.x), (float)(var1.y - var6.y), (float)(var1.z - var6.z));
      var10.multiply(var5.getRotation());
      Matrix4f var11 = var10.peek().getPositionMatrix();
      float var12 = var3 / 2.0F;
      var2.vertex(var11, -var12, var12, 0.0F).texture(0.0F, 1.0F).color(var7, var8, var9, var4);
      var2.vertex(var11, var12, var12, 0.0F).texture(1.0F, 1.0F).color(var7, var8, var9, var4);
      var2.vertex(var11, var12, -var12, 0.0F).texture(1.0F, 0.0F).color(var7, var8, var9, var4);
      var2.vertex(var11, -var12, -var12, 0.0F).texture(0.0F, 0.0F).color(var7, var8, var9, var4);
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (mc.player != null && mc.world != null) {
         DrawContext var2 = var1.getContext();

         for (Prediction.a var4 : this.predicted) {
            if (!var4.vectors.isEmpty()) {
               Vec3d var5 = var4.vectors.getLast();
               if (var4.entity instanceof EnderPearlEntity) {
                  Entity var6 = var4.entity instanceof ProjectileEntity var7 ? var7.getOwner() : null;
                  this.a(var2, var5, var4.ticks, var6);
               } else {
                  float[] var43 = WorldToScreen.a(var5);
                  if (var43 != null) {
                     float var44 = var43[0];
                     float var45 = var43[1];
                     String var9 = var4.entity != null ? var4.entity.getName().getString().replace("Брошенный эндер-жемчуг", "Эндер-жемчуг") : "Снаряд";
                     if (var4.entity instanceof PotionEntity var10) {
                        var9 = var10.getStack().getName().getString();
                     }

                     String var46 = var9.replace("] ", "").replace("[", "") + String.format(Locale.US, " (%.1f сек)", var4.ticks / 20.0F);
                     ItemStack var47 = Items.ARROW.getDefaultStack();
                     if (var4.entity instanceof ThrownItemEntity var48) {
                        var47 = var48.getStack();
                     } else if (var4.entity instanceof PersistentProjectileEntity var12) {
                        var47 = var12.getItemStack();
                     } else if (var4.entity instanceof ItemEntity var13) {
                        var47 = var13.getStack();
                     }

                     float var49 = (float)var5.distanceTo(mc.player.getEyePos());
                     float var50 = MathHelper.clamp(1.0F - var49 / 35.0F, 0.65F, 1.0F);
                     float var51 = 6.5F;
                     float var54 = Msdf.a(MsdfManager.BOLD, var46, var51);
                     float var15 = 10.0F;
                     float var16 = 4.0F;
                     float var17 = 3.5F;
                     float var18 = var16 + var15 + var17 + var54 + var16;
                     float var19 = 13.0F;
                     float var20 = 4.5F;
                     float var21 = var44 - var18 / 2.0F;
                     float var22 = var45 - var19 - 4.0F;
                     Matrix4f var23 = this.a(var44, var45, var50);
                     HudConfig.a(var23, "pred", var21, var22, var18, var19, var20, -1275068416);
                     Msdf.a(
                        var23,
                        MsdfManager.BOLD,
                        var46,
                        var21 + var16 + var15 + var17,
                        var22 + (var19 - Msdf.a(MsdfManager.BOLD, var51)) / 2.0F + 0.5F,
                        var51,
                        -1
                     );
                     Matrix3x2fStack var24 = var2.getMatrices();
                     float var25 = 2.0F / mc.getWindow().getScaleFactor();
                     float var26 = var15 / 16.0F * var50 / var25;
                     float var27 = var44 + (var21 + var16 + var15 / 2.0F - var44) * var50;
                     float var28 = var45 + (var22 + var19 / 2.0F - var45) * var50;
                     var24.pushMatrix();
                     var24.translate(var27 * var25, var28 * var25);
                     var24.scale(var26, var26);
                     var24.translate(-8.0F, -8.0F);

                     try {
                        var2.drawItem(var47, 0, 0);
                     } catch (Exception var43x) {
                     }

                     var24.popMatrix();
                     float var29 = var22 + var19 + 2.5F;
                     if (var4.entity instanceof ProjectileEntity var30 && var30.getOwner() instanceof AbstractClientPlayerEntity var31) {
                        String var56 = var30.getOwner() == mc.player ? "От Вас" : "От " + var31.getName().getString();
                        float var33 = 6.25F;
                        float var34 = Msdf.a(MsdfManager.BOLD, var56, var33);
                        float var35 = 8.5F;
                        float var36 = 4.0F;
                        float var37 = 3.5F;
                        float var38 = var36 + var35 + var37 + var34 + var36;
                        float var39 = 12.0F;
                        float var40 = 4.0F;
                        float var41 = var44 - var38 / 2.0F;
                        HudConfig.a(var23, "pred_owner", var41, var29, var38, var39, var40, -1275068416);
                        this.a(var23, var31, var41 + var36, var29 + (var39 - var35) / 2.0F, var35);
                        Msdf.a(
                           var23,
                           MsdfManager.BOLD,
                           var56,
                           var41 + var36 + var35 + var37,
                           var29 + (var39 - Msdf.a(MsdfManager.BOLD, var33)) / 2.0F + 0.5F,
                           var33,
                           -1
                        );
                     }
                  }
               }
            }
         }
      }
   }

   private void a(DrawContext var1, Vec3d var2, int var3, Entity var4) {
      float[] var5 = WorldToScreen.a(var2);
      if (var5 != null) {
         float var6 = var5[0];
         float var7 = var5[1];
         float var8 = (float)var2.distanceTo(mc.player.getEyePos());
         float var9 = MathHelper.clamp(1.0F - var8 / 45.0F, 0.65F, 1.15F);
         Matrix4f var10 = this.a(var6, var7, var9);
         float var11 = 22.0F;
         float var12 = 6.0F;
         float var13 = var7 - var12 - var11 / 2.0F;
         Color var14 = Theme.getInstance().getAccent();
         int var15 = var14 != null ? var14.getRGB() : -1;
         this.a(var10, var6, var7, 4.5F, var12 + 2.0F, var15);
         RenderUtil.a(var10, var6 - var11 / 2.0F, var13 - var11 / 2.0F, var11, var11, var11 / 2.0F, var11 / 2.0F, var11 / 2.0F, var11 / 2.0F, false, var15);
         float var16 = var11 - 4.0F;
         RenderUtil.a(var10, var6 - var16 / 2.0F, var13 - var16 / 2.0F, var16, var16, var16 / 2.0F, var16 / 2.0F, var16 / 2.0F, var16 / 2.0F, false, -15855592);
         float var17 = 13.0F;
         Matrix3x2fStack var18 = var1.getMatrices();
         float var19 = 2.0F / mc.getWindow().getScaleFactor();
         float var20 = var17 / 16.0F * var9 / var19;
         float var21 = var6 + (var6 - var6) * var9;
         float var22 = var7 + (var13 - var7) * var9;
         var18.pushMatrix();
         var18.translate(var21 * var19, var22 * var19);
         var18.scale(var20, var20);
         var18.translate(-8.0F, -8.0F);

         try {
            var1.drawItem(Items.ENDER_PEARL.getDefaultStack(), 0, 0);
         } catch (Exception var42) {
         }

         var18.popMatrix();
         float var23 = var3 / 20.0F;
         String var24 = String.format(Locale.US, "%.1f сек", var23);
         float var25 = 6.25F;
         float var26 = Msdf.a(MsdfManager.BOLD, var24, var25);
         float var27 = 5.0F;
         float var28 = var26 + var27 * 2.0F;
         float var29 = 11.5F;
         float var30 = 4.5F;
         float var31 = var7 + 4.0F;
         if (var4 instanceof AbstractClientPlayerEntity var32 && var32 != mc.player) {
            String var33 = var32.getName().getString();
            float var34 = 6.25F;
            float var35 = Msdf.a(MsdfManager.BOLD, var33, var34);
            float var36 = 8.0F;
            float var37 = var36 + 4.0F + var35 + 6.0F;
            float var38 = 11.5F;
            float var39 = var6 - var37 / 2.0F;
            float var40 = var31;
            float var41 = 4.5F;
            RenderUtil.b(var10, var39, var40, var37, var38, var41, 4.5F, 0.4F, 2.5F, -1946157056);
            if (Theme.getInstance().getGuiStyle() == Theme.a.BLUR) {
               RenderUtil.a(var10, var39, var40, var37, var38, var41, 10.0F);
            }

            RenderUtil.a(var10, var39, var40, var37, var38, var41, var41, var41, var41, false, 1845493760);
            RenderUtil.a(var10, var39, var40, var37, var38, var41, 0.75F, 436207615);
            this.a(var10, var32, var39 + 2.0F, var40 + (var38 - var36) / 2.0F, var36);
            Msdf.a(var10, MsdfManager.BOLD, var33, var39 + var36 + 4.0F, var40 + (var38 - Msdf.a(MsdfManager.BOLD, var34)) / 2.0F + 0.5F, var34, -1);
            var31 += var38 + 2.5F;
         }

         float var43 = var6 - var28 / 2.0F;
         float var44 = var31;
         RenderUtil.b(var10, var43, var44, var28, var29, var30, 4.5F, 0.4F, 2.5F, -1946157056);
         if (Theme.getInstance().getGuiStyle() == Theme.a.BLUR) {
            RenderUtil.a(var10, var43, var44, var28, var29, var30, 10.0F);
         }

         RenderUtil.a(var10, var43, var44, var28, var29, var30, var30, var30, var30, false, 1845493760);
         RenderUtil.a(var10, var43, var44, var28, var29, var30, 0.75F, 436207615);
         Msdf.a(var10, MsdfManager.BOLD, var24, var43 + var27, var44 + (var29 - Msdf.a(MsdfManager.BOLD, var25)) / 2.0F + 0.5F, var25, -1);
      }
   }

   private void a(Matrix4f var1, float var2, float var3, float var4, float var5, int var6) {
      for (float var7 = 0.0F; var7 <= var5; var7 += 1.5F) {
         float var8 = var7 / var5;
         float var9 = var4 * (1.0F - var8);
         RenderUtil.a(var1, var2 - var9, var3 - var5 + var7, var9 * 2.0F, 1.75F, 0.0F, 0.0F, 0.0F, 0.0F, false, var6);
      }
   }

   private Matrix4f a(float var1, float var2, float var3) {
      Matrix4f var4 = RenderUtil.d();
      var4.translate(var1, var2, 0.0F);
      var4.scale(var3, var3, 1.0F);
      var4.translate(-var1, -var2, 0.0F);
      return var4;
   }

   private void a(Matrix4f var1, AbstractClientPlayerEntity var2, float var3, float var4, float var5) {
      RenderUtil.a(var1, var3, var4, var5, var5, 2.0F, 2.0F, 2.0F, 2.0F, false, 2013265920);
      Identifier var6 = SkinCache.a(mc, var2);
      if (var6 != null) {
         try {
            GpuTextureView var7 = mc.getTextureManager().getTexture(var6).getGlTextureView();
            TexturePipeline.b(var1, var3, var4, var5, var5, var7, -1, 1.5F, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
         } catch (Exception var8) {
         }
      }
   }

   private static void k() {
      if (bloomPipeline == null || uniformBuffer == null) {
         try {
            if (bloomPipeline == null) {
               bloomPipeline = b(false);
               bloomPipelineWalls = b(true);
            }

            if (uniformBuffer == null) {
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Prediction Uniforms", 136, 64L);
            }
         } catch (Exception var1) {
         }
      }
   }

   private static RenderPipeline b(boolean var0) {
      return RenderPipeline.builder()
         .withLocation(Identifier.of("brokke", var0 ? "pred_bloom_nodepth" : "pred_bloom"))
         .withVertexShader(Identifier.of("brokke", "world_bloom_vertex"))
         .withFragmentShader(Identifier.of("brokke", "world_bloom_fragment"))
         .withVertexFormat(FORMAT_BLOOM, DrawMode.QUADS)
         .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withDepthTestFunction(var0 ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
         .build();
   }

   private static void a(Matrix4f var0) {
      if (uniformBuffer != null) {
         if (uniformScratch == null) {
            uniformScratch = MemoryUtil.memAlloc(64);
         }

         uniformScratch.clear();
         uniformScratch.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         uniformScratch.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         uniformScratch.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         uniformScratch.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         uniformScratch.flip();
         CommandEncoder var1 = RenderSystem.getDevice().createCommandEncoder();
         var1.writeToBuffer(uniformBuffer.slice(), uniformScratch);
      }
   }

   private void a(BufferBuilder var1, GpuTextureView var2, RenderPipeline var3) {
      BuiltBuffer var4 = var1.endNullable();
      if (var4 != null) {
         int var5 = var4.getDrawParameters().indexCount();
         if (var5 <= 0) {
            var4.close();
         } else {
            try {
               int var6 = var4.getBuffer().remaining();
               if (quadsVbo == null || quadsVbo.size() < var6) {
                  if (quadsVbo != null) {
                     quadsVbo.close();
                  }

                  quadsVbo = RenderSystem.getDevice().createBuffer(() -> "Prediction Quads VBO", 40, Math.max(var6, 4096));
               }

               CommandEncoder var7 = RenderSystem.getDevice().createCommandEncoder();
               var7.writeToBuffer(quadsVbo.slice(0L, var6), var4.getBuffer());
               ShapeIndexBuffer var8 = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer var9 = var8.getIndexBuffer(var5);
               Framebuffer var10 = mc.getFramebuffer();
               if (var10 == null) {
                  return;
               }

               GpuSampler var11 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

               try (RenderPass var12 = var7.createRenderPass(
                     () -> "Prediction Quads", var10.getColorAttachmentView(), OptionalInt.empty(), var10.getDepthAttachmentView(), OptionalDouble.empty()
                  )) {
                  var12.setPipeline(var3);
                  var12.setVertexBuffer(0, quadsVbo);
                  var12.setIndexBuffer(var9, var8.getIndexType());
                  var12.setUniform("Uniforms", uniformBuffer);
                  var12.bindTexture("Sampler0", var2, var11);
                  var12.drawIndexed(0, 0, var5, 1);
                  return;
               }
            } catch (Exception var22) {
            } finally {
               var4.close();
            }

            return;
         }
      }
   }

   private Matrix4f l() {
      float var1 = mc.getRenderTickCounter().getTickProgress(true);
      Camera var2 = mc.gameRenderer.getCamera();
      double var3 = Math.toRadians(var2.getYaw());
      double var5 = Math.toRadians(var2.getPitch());
      double var7 = Math.cos(var3);
      double var9 = Math.sin(var3);
      double var11 = Math.cos(var5);
      double var13 = Math.sin(var5);
      Vec3d var15 = new Vec3d(-var9 * var11, -var13, var7 * var11);
      Vec3d var16 = new Vec3d(0.0, 1.0, 0.0);
      Vec3d var17 = var15.crossProduct(var16);
      if (var17.lengthSquared() < 1.0E-6) {
         var17 = var15.crossProduct(new Vec3d(0.0, 0.0, 1.0));
      }

      var17 = var17.normalize();
      Vec3d var18 = var17.crossProduct(var15).normalize();
      Matrix4f var19 = new Matrix4f().lookAt(0.0F, 0.0F, 0.0F, (float)var15.x, (float)var15.y, (float)var15.z, (float)var18.x, (float)var18.y, (float)var18.z);
      Window var20 = mc.getWindow();
      float var21 = (float)var20.getFramebufferWidth() / Math.max(1, var20.getFramebufferHeight());
      float var22 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(var2, var1, true);
      Matrix4f var23 = new Matrix4f().perspective((float)Math.toRadians(var22), var21, 0.05F, 1000.0F);
      return var23.mul(var19);
   }

   private static Vec3d a(Entity var0, float var1) {
      return var0.getLerpedPos(var1);
   }

   private boolean b(Entity var1) {
      boolean var2 = false;
      if (var1 instanceof EnderPearlEntity && this.entities.isOptionEnabled("Эндер-жемчуги")) {
         var2 = true;
      }

      if (var1 instanceof TridentEntity && this.entities.isOptionEnabled("Трезубцы")) {
         var2 = true;
      }

      if (var1 instanceof SnowballEntity && this.entities.isOptionEnabled("Снежки")) {
         var2 = true;
      }

      if (var1 instanceof ArrowEntity && this.entities.isOptionEnabled("Стрелы")) {
         var2 = true;
      }

      if (var1 instanceof PotionEntity && this.entities.isOptionEnabled("Зелья")) {
         var2 = true;
      }

      return var1 instanceof TridentEntity var3 && var3.returnTimer > 0
         ? false
         : var2 && (Math.abs(var1.getVelocity().x + var1.getVelocity().z) > 0.01F || Math.abs(var1.getVelocity().y) > 0.2F);
   }

   private Entity a(Entity var1, Vec3d var2) {
      Vec3d var3 = var1.getEntityPos();
      Vec3d var4 = var2.subtract(var3);
      if (var4.lengthSquared() == 0.0) {
         return null;
      }

      EntityHitResult var5 = ProjectileUtil.raycast(
         var1,
         var3,
         var2,
         var1.getBoundingBox().stretch(var4).expand(0.5),
         var1xx -> mc.player != var1xx && var1xx.isAlive() && !(var1xx instanceof ItemEntity) && !(var1xx instanceof ExperienceOrbEntity) && var1xx != var1,
         var4.lengthSquared()
      );
      return var5 != null ? var5.getEntity() : null;
   }

   private Vec3d b(Entity var1, Vec3d var2) {
      return var2.multiply(0.99).add(0.0, -var1.getFinalGravity(), 0.0);
   }

   record a(Entity entity, List<Vec3d> vectors, int ticks, Entity collidedEntity) {
      public Entity getEntity() {
         return this.entity;
      }

      public List<Vec3d> getVectors() {
         return this.vectors;
      }

      public int getTicks() {
         return this.ticks;
      }

      public Entity getCollidedEntity() {
         return this.collidedEntity;
      }
   }
}
