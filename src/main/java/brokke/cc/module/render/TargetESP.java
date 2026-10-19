package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.AimAssist;
import brokke.cc.module.combat.Aura;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render3DEvent;
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
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

@NewFunction(name = "TargetESP", description = "Отображение таргета при включенной киллауре", category = Category.RENDER)
public class TargetESP extends Function {
   private static final Identifier BLOOM_TEX = Identifier.of("brokke", "textures/targetesp/bloom.png");
   private static final Identifier GLOWING_TEX = Identifier.of("brokke", "textures/targetesp/glowing.png");
   private static final VertexFormat FORMAT_BLOOM = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static final VertexFormat FORMAT_COLOR = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static final Vector3f[] CRYSTAL_VERTS = new Vector3f[]{
      new Vector3f(0.0F, 1.5F, 0.0F),
      new Vector3f(0.0F, -1.5F, 0.0F),
      new Vector3f(1.0F, 0.0F, 0.0F),
      new Vector3f(-1.0F, 0.0F, 0.0F),
      new Vector3f(0.0F, 0.0F, 1.0F),
      new Vector3f(0.0F, 0.0F, -1.0F)
   };
   private static final int[][] CRYSTAL_FACES = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
   private static final float[] CRYSTAL_SHADES = new float[]{1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};
   private static final int UNIFORM_SIZE = 64;
   private static final int LENS_UNIFORM_SIZE = 304;
   private static RenderPipeline bloomPipeline;
   private static RenderPipeline crystalPipeline;
   private static RenderPipeline lensPipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer lensUniformBuffer;
   private static ByteBuffer lensUniformScratch;
   private static ByteBuffer matrixUniformScratch;
   private static GpuBuffer texturedVbo;
   private static GpuBuffer trianglesVbo;
   private final ModeSetting mode = new ModeSetting("Режим", "Кристаллы", "Души", "Кругляшок", "Jello");
   private final NumberSetting rotateSpeed = new NumberSetting("Скорость вращения", 1.0, 0.1, 3.0, 0.05);
   private final BooleanSetting distortion = new BooleanSetting("Искажение", false);
   private final NumberSetting distortionStrength = new NumberSetting("Сила искажения", 0.05, 0.01, 0.15, 0.01);
   private final BooleanSetting hurtColor = new BooleanSetting("Окрашивание при ударе", true);
   private final BooleanSetting onAim = new BooleanSetting("При наводке", true);
   private float appearValue = 0.0F;
   private float rotAngle = 0.0F;
   private long lastFrameTime = System.currentTimeMillis();
   private LivingEntity lastTarget = null;
   private Vec3d lastTargetPos = null;
   private float lastTargetWidth = 0.6F;
   private float lastTargetHeight = 1.8F;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private int sceneWidth;
   private int sceneHeight;

   public TargetESP() {
      this.addSettings(this.mode, this.rotateSpeed, this.distortion, this.distortionStrength, this.hurtColor, this.onAim);
      this.mode.onChanged(this::k);
      this.distortion.onChanged(this::k);
      this.k();
   }

   private void k() {
      boolean var1 = this.mode.isMode("Кристаллы");
      this.distortion.setVisible(var1);
      this.distortionStrength.setVisible(var1 && this.distortion.isToggled());
   }

   @Override
   public void onDisable() {
      this.appearValue = 0.0F;
      this.lastTarget = null;
      this.lastTargetPos = null;
      this.l();
   }

   private LivingEntity a(MinecraftClient var1) {
      Aura var2 = FunctionManager.a(Aura.class);
      if (var2 != null && var2.isActive() && var2.getTarget() != null && var2.getTarget().isAlive()) {
         return var2.getTarget();
      } else {
         AimAssist var3 = FunctionManager.a(AimAssist.class);
         if (var3 != null && var3.isActive() && var3.k() != null && var3.k().isAlive()) {
            return var3.k();
         } else {
            return this.onAim.isToggled() && var1.targetedEntity instanceof LivingEntity var4 && var4.isAlive() && var4 != var1.player ? var4 : null;
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         LivingEntity var3 = this.a(var2);
         boolean var4 = var3 != null && var3.isAlive();
         long var5 = System.currentTimeMillis();
         float var7 = Math.min(100.0F, Math.max(1.0F, (float)(var5 - this.lastFrameTime)));
         this.lastFrameTime = var5;
         float var8 = var4 ? 1.0F : 0.0F;
         float var9 = var4 ? var7 / 160.0F : var7 / 280.0F;
         this.appearValue = a(this.appearValue, var8, var9);
         if (var4) {
            this.lastTarget = var3;
            this.lastTargetPos = var3.getLerpedPos(var1.getTickDelta());
            this.lastTargetWidth = var3.getWidth();
            this.lastTargetHeight = var3.getHeight();
         }

         if (this.appearValue <= 0.001F) {
            this.lastTarget = null;
            this.lastTargetPos = null;
         } else {
            this.rotAngle = this.rotAngle + var7 / 1000.0F * 60.0F * (float)this.rotateSpeed.getValue();
            if (this.rotAngle > 360000.0F) {
               this.rotAngle %= 360.0F;
            }

            m();
            if (bloomPipeline != null && crystalPipeline != null && uniformBuffer != null) {
               Camera var10 = var1.getCamera();
               Vec3d var11 = var10.getCameraPos();
               float var12 = var1.getTickDelta();
               Matrix4f var13 = this.p();
               a(var13);
               Vec3d var14 = this.a(var12);
               if (var14 != null) {
                  Color var15 = Theme.getInstance().getAccent();
                  float var16 = var15.getRed() / 255.0F;
                  float var17 = var15.getGreen() / 255.0F;
                  float var18 = var15.getBlue() / 255.0F;
                  float var19 = this.a(this.lastTarget);
                  if (var19 > 0.001F) {
                     var16 += (1.0F - var16) * var19;
                     var17 *= 1.0F - var19;
                     var18 *= 1.0F - var19;
                  }

                  GpuTextureView var20 = this.a(BLOOM_TEX);
                  GpuTextureView var21 = this.a(GLOWING_TEX);
                  if (this.mode.isMode("Кристаллы")) {
                     BufferBuilder var22 = Tessellator.getInstance().begin(DrawMode.TRIANGLES, FORMAT_COLOR);
                     this.a(var22, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                     this.a(var22);
                     if (var20 != null) {
                        BufferBuilder var23 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                        this.b(var23, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                        this.a(var23, var20);
                     }

                     this.a(var14, this.lastTarget);
                  } else if (this.mode.isMode("Души")) {
                     if (var21 != null) {
                        BufferBuilder var24 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                        this.c(var24, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                        this.a(var24, var21);
                     }
                  } else if (this.mode.isMode("Кругляшок")) {
                     if (var21 != null) {
                        BufferBuilder var25 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                        this.d(var25, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                        this.a(var25, var21);
                     }
                  } else if (this.mode.isMode("Jello")) {
                     if (var20 != null) {
                        BufferBuilder var26 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                        this.e(var26, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                        this.a(var26, var20);
                     }

                     if (var21 != null) {
                        BufferBuilder var27 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                        this.f(var27, var10, var11, var14, this.lastTarget, var16, var17, var18, this.appearValue);
                        this.a(var27, var21);
                     }
                  }
               }
            }
         }
      }
   }

   private void a(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.5F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = 1.2F - 0.5F * var9;
      float var13 = var10 * var12;
      Matrix4f var14 = new Matrix4f();
      Quaternionf var15 = new Quaternionf();
      Vector3f var16 = new Vector3f();
      Vector3f var17 = new Vector3f(0.0F, 1.0F, 0.0F);

      for (int var18 = 0; var18 < 360; var18 += 20) {
         double var19 = Math.toRadians(var18 + this.rotAngle * 0.3F);
         double var21 = Math.sin(var19) * var13;
         double var23 = Math.cos(var19) * var13;
         double var25 = 0.1 + var11 * Math.abs(Math.sin(var18));
         Vec3d var27 = new Vec3d(var4.x + var21, var4.y + var25, var4.z + var23);
         Vec3d var28 = var4.add(0.0, var11 / 2.0, 0.0);
         Vec3d var29 = var4.add(var21, 1.0, var23);
         var16.set((float)(var28.x - var29.x), (float)(var28.y - var29.y), (float)(var28.z - var29.z)).normalize();
         var15.identity().rotationTo(var17, var16);
         float var30 = 0.1F * Math.min(1.0F, var9 * 1.25F);
         var14.translation((float)(var27.x - var3.x), (float)(var27.y - var3.y), (float)(var27.z - var3.z)).rotate(var15).scale(var30, var30, var30);

         for (int var31 = 0; var31 < CRYSTAL_FACES.length; var31++) {
            int[] var32 = CRYSTAL_FACES[var31];
            float var33 = CRYSTAL_SHADES[var31];
            Vector3f var34 = CRYSTAL_VERTS[var32[0]];
            Vector3f var35 = CRYSTAL_VERTS[var32[1]];
            Vector3f var36 = CRYSTAL_VERTS[var32[2]];
            float var37 = var6 * var33;
            float var38 = var7 * var33;
            float var39 = var8 * var33;
            var1.vertex(var14, var34.x, var34.y, var34.z).color(var37, var38, var39, var9);
            var1.vertex(var14, var35.x, var35.y, var35.z).color(var37, var38, var39, var9);
            var1.vertex(var14, var36.x, var36.y, var36.z).color(var37, var38, var39, var9);
         }
      }
   }

   private void b(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.5F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = 1.2F - 0.5F * var9;
      float var13 = var10 * var12;

      for (int var14 = 0; var14 < 360; var14 += 20) {
         double var15 = Math.toRadians(var14 + this.rotAngle * 0.3F);
         double var17 = Math.sin(var15) * var13;
         double var19 = Math.cos(var15) * var13;
         double var21 = 0.1 + var11 * Math.abs(Math.sin(var14));
         Vec3d var23 = new Vec3d(var4.x + var17, var4.y + var21, var4.z + var19);
         this.a(var1, var2, var3, var23, 1.0F * Math.min(1.0F, var9 * 1.25F), var6, var7, var8, var9 * 0.2F);
      }
   }

   private void c(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.5F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = Math.max(0.5F, 1.2F - 0.5F * var9);
      float var13 = var10 * var12;
      byte var14 = 2;
      byte var15 = 0;
      byte var16 = 0;

      for (int var17 = 0; var17 < 360; var17 += var14) {
         float var18 = (0.23F + 0.005F * var15) * Math.min(1.0F, var9 * 1.25F);
         float var19 = (0.7F + 0.005F * var15) * Math.min(1.0F, var9 * 1.25F);
         if (var16 > 0) {
            var16 -= var14;
         } else {
            var15 += var14;
            if (var15 > 50) {
               var16 = 100;
               var15 = 0;
            } else {
               double var20 = Math.toRadians(var17 + this.rotAngle);
               double var22 = Math.sin(var20) * var13;
               double var24 = Math.cos(var20) * var13;
               double var26 = var11 / 1.5F + var11 / 3.0F * Math.sin(Math.toRadians(var17 / 2.0F + this.rotAngle / 5.0F));
               Vec3d var28 = new Vec3d(var4.x + var22, var4.y + var26, var4.z + var24);
               this.a(var1, var2, var3, var28, var19, var6, var7, var8, var9 * 0.02F);
               this.a(var1, var2, var3, var28, var18, var6, var7, var8, var9 * 0.5F);
            }
         }
      }
   }

   private void d(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.5F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = this.a(var5);
      float var13 = Math.max(0.5F, 0.7F - 0.2F * var12 + (1.0F - var9) * 0.4F);
      float var14 = var10 * var13;

      for (int var15 = 0; var15 < 360; var15 += 2) {
         if (var15 / 45 % 2 != 0) {
            double var16 = Math.toRadians(var15 + this.rotAngle);
            double var18 = Math.sin(var16) * var14;
            double var20 = Math.cos(var16) * var14;
            double var22 = var11 / 1.75F + var11 / 2.0F * Math.sin(Math.toRadians(this.rotAngle / 2.0F));
            Vec3d var24 = new Vec3d(var4.x + var18, var4.y + var22, var4.z + var20);
            this.a(var1, var2, var3, var24, 0.8F * Math.min(1.0F, var9 * 1.25F), var6, var7, var8, var9 * 0.03F);
            this.a(var1, var2, var3, var24, 0.3F * Math.min(1.0F, var9 * 1.25F), var6, var7, var8, var9 * 0.7F);
         }
      }
   }

   private void e(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.45F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = this.a(var5);
      float var13 = Math.max(0.5F, 0.7F - 0.2F * var12 + (1.0F - var9) * 0.4F);
      float var14 = var10 * var13;

      for (int var15 = 0; var15 < 360; var15 += 2) {
         double var16 = Math.toRadians(var15 + this.rotAngle);
         double var18 = Math.sin(var16) * var14;
         double var20 = Math.cos(var16) * var14;

         for (int var22 = 0; var22 < 15; var22++) {
            double var23 = var11 / 1.75F + var11 / 2.0F * Math.sin(Math.toRadians(this.rotAngle / 1.5F + var22 * 2.0F));
            Vec3d var25 = new Vec3d(var4.x + var18, var4.y + var23, var4.z + var20);
            float var26 = var9 * (var22 / 15.0F) * 0.05F;
            this.a(var1, var2, var3, var25, 0.2F * Math.min(1.0F, var9 * 1.25F), var6, var7, var8, var26);
         }
      }
   }

   private void f(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, LivingEntity var5, float var6, float var7, float var8, float var9) {
      float var10 = (var5 != null ? var5.getWidth() : this.lastTargetWidth) * 1.45F;
      float var11 = var5 != null ? var5.getHeight() : this.lastTargetHeight;
      float var12 = this.a(var5);
      float var13 = Math.max(0.5F, 0.7F - 0.2F * var12 + (1.0F - var9) * 0.4F);
      float var14 = var10 * var13;

      for (int var15 = 0; var15 < 360; var15 += 2) {
         double var16 = Math.toRadians(var15 + this.rotAngle);
         double var18 = Math.sin(var16) * var14;
         double var20 = Math.cos(var16) * var14;
         double var22 = var11 / 1.75F + var11 / 2.0F * Math.sin(Math.toRadians(this.rotAngle / 1.5F + 30.0F));
         Vec3d var24 = new Vec3d(var4.x + var18, var4.y + var22, var4.z + var20);
         this.a(var1, var2, var3, var24, 0.2F * Math.min(1.0F, var9 * 1.25F), var6, var7, var8, var9 * 0.2F);
      }
   }

   private void a(BufferBuilder var1, Camera var2, Vec3d var3, Vec3d var4, float var5, float var6, float var7, float var8, float var9) {
      if (var1 != null && !(var9 <= 0.001F) && !(var5 <= 0.0F)) {
         double var10 = var4.distanceTo(var3);
         if (!(var10 < 0.15)) {
            float var12 = var5;
            float var13 = var9;
            if (var10 < var5 * 0.6F) {
               float var14 = (float)((var10 - 0.15) / (var5 * 0.6F - 0.15));
               if (var14 <= 0.0F) {
                  return;
               }

               var14 = Math.clamp(var14, 0.0F, 1.0F);
               var12 = Math.max(0.05F, var5 * var14);
               var13 = var9 * var14;
            }

            float var18 = var12 * 0.5F;
            MatrixStack var15 = new MatrixStack();
            var15.translate((float)(var4.x - var3.x), (float)(var4.y - var3.y), (float)(var4.z - var3.z));
            var15.multiply(var2.getRotation());
            Matrix4f var16 = var15.peek().getPositionMatrix();
            var1.vertex(var16, -var18, var18, 0.0F).texture(0.0F, 1.0F).color(var6, var7, var8, var13);
            var1.vertex(var16, var18, var18, 0.0F).texture(1.0F, 1.0F).color(var6, var7, var8, var13);
            var1.vertex(var16, var18, -var18, 0.0F).texture(1.0F, 0.0F).color(var6, var7, var8, var13);
            var1.vertex(var16, -var18, -var18, 0.0F).texture(0.0F, 0.0F).color(var6, var7, var8, var13);
         }
      }
   }

   private void a(Vec3d var1, LivingEntity var2) {
      if (this.distortion.isToggled() && !(this.appearValue <= 0.001F) && var1 != null) {
         float var3 = (float)this.distortionStrength.getValue() * this.appearValue;
         if (!(var3 <= 1.0E-4F)) {
            Window var4 = mc.getWindow();
            if (var4 != null) {
               int var5 = var4.getFramebufferWidth();
               int var6 = var4.getFramebufferHeight();
               if (var5 > 1 && var6 > 1) {
                  Framebuffer var7 = mc.getFramebuffer();
                  if (var7 != null && var7.getColorAttachment() != null && var7.getDepthAttachment() != null) {
                     Camera var8 = mc.gameRenderer.getCamera();
                     Vec3d var9 = var8.getCameraPos();
                     Matrix4f var10 = this.o();
                     Matrix4f var11 = this.p();
                     float var12 = var10.m11() / var10.m00();
                     float var13 = (var2 != null ? var2.getWidth() : this.lastTargetWidth) * 1.5F;
                     float var14 = var2 != null ? var2.getHeight() : this.lastTargetHeight;
                     float var15 = 1.2F - 0.5F * this.appearValue;
                     float[] var16 = new float[72];
                     int var17 = 0;

                     for (int var18 = 0; var18 < 360 && var17 < 18; var18 += 20) {
                        double var19 = Math.toRadians(var18 + this.rotAngle * 0.3F);
                        double var21 = Math.sin(var19) * var13 * var15;
                        double var23 = Math.cos(var19) * var13 * var15;
                        double var25 = 0.1 + var14 * Math.abs(Math.sin(var18));
                        double var27 = var1.x + var21 - var9.x;
                        double var29 = var1.y + var25 - var9.y;
                        double var31 = var1.z + var23 - var9.z;
                        Vector4f var33 = var11.transform(new Vector4f((float)var27, (float)var29, (float)var31, 1.0F));
                        if (!(var33.w <= 0.05F)) {
                           float var34 = var33.x / var33.w * 0.5F + 0.5F;
                           float var35 = var33.y / var33.w * 0.5F + 0.5F;
                           float var36 = Math.clamp(var33.z / var33.w * 0.5F + 0.5F, 0.0F, 1.0F);
                           float var37 = Math.min(0.45F * var10.m11() / var33.w * 0.5F, 0.35F);
                           if (!(var34 < -var37 * 2.0F) && !(var34 > 1.0F + var37 * 2.0F) && !(var35 < -var37 * 2.0F) && !(var35 > 1.0F + var37 * 2.0F)) {
                              var16[var17 * 4] = var34;
                              var16[var17 * 4 + 1] = var35;
                              var16[var17 * 4 + 2] = var36;
                              var16[var17 * 4 + 3] = var37;
                              var17++;
                           }
                        }
                     }

                     if (var17 != 0) {
                        this.a(var7, var5, var6, var12, var3, var16, var17);
                     }
                  }
               }
            }
         }
      }
   }

   private void a(Framebuffer var1, int var2, int var3, float var4, float var5, float[] var6, int var7) {
      n();
      if (lensPipeline != null && lensUniformBuffer != null && this.a(var2, var3)) {
         CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
         var8.copyTextureToTexture(var1.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, var2, var3);
         if (lensUniformScratch == null) {
            lensUniformScratch = MemoryUtil.memAlloc(304);
         }

         ByteBuffer var9 = lensUniformScratch;
         var9.clear();
         var9.putFloat(var7);
         var9.putFloat(var4);
         var9.putFloat(var5);
         var9.putFloat(0.0F);

         for (int var10 = 0; var10 < 18; var10++) {
            if (var10 < var7) {
               var9.putFloat(var6[var10 * 4]);
               var9.putFloat(var6[var10 * 4 + 1]);
               var9.putFloat(var6[var10 * 4 + 2]);
               var9.putFloat(var6[var10 * 4 + 3]);
            } else {
               var9.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
            }
         }

         var9.flip();
         var8.writeToBuffer(lensUniformBuffer.slice(), var9);
         GpuSampler var18 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
         GpuSampler var11 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

         try (RenderPass var12 = var8.createRenderPass(() -> "TargetESP Lens", var1.getColorAttachmentView(), OptionalInt.empty())) {
            var12.setPipeline(lensPipeline);
            var12.setUniform("TargetLensUniforms", lensUniformBuffer);
            var12.bindTexture("Sampler0", this.sceneTextureView, var11);
            var12.bindTexture("Sampler1", var1.getDepthAttachmentView(), var18);
            var12.draw(0, 6);
         } catch (Exception var17) {
         }
      }
   }

   private boolean a(int var1, int var2) {
      if (this.sceneTexture != null && this.sceneWidth == var1 && this.sceneHeight == var2) {
         return true;
      }

      this.l();

      try {
         this.sceneTexture = RenderSystem.getDevice().createTexture(() -> "TargetESP Scene Color", 13, TextureFormat.RGBA8, var1, var2, 1, 1);
         this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
         this.sceneWidth = var1;
         this.sceneHeight = var2;
         return true;
      } catch (Exception var4) {
         this.sceneTexture = null;
         this.sceneTextureView = null;
         return false;
      }
   }

   private void l() {
      if (this.sceneTextureView != null) {
         this.sceneTextureView.close();
         this.sceneTextureView = null;
      }

      if (this.sceneTexture != null) {
         this.sceneTexture.close();
         this.sceneTexture = null;
      }

      this.sceneWidth = 0;
      this.sceneHeight = 0;
   }

   private static void m() {
      if (bloomPipeline == null || crystalPipeline == null || uniformBuffer == null) {
         try {
            if (bloomPipeline == null) {
               bloomPipeline = RenderPipeline.builder()
                  .withLocation(Identifier.of("brokke", "target_esp_bloom"))
                  .withVertexShader(Identifier.of("brokke", "world_bloom_vertex"))
                  .withFragmentShader(Identifier.of("brokke", "world_bloom_fragment"))
                  .withVertexFormat(FORMAT_BLOOM, DrawMode.QUADS)
                  .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                  .withSampler("Sampler0")
                  .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                  .build();
            }

            if (crystalPipeline == null) {
               crystalPipeline = RenderPipeline.builder()
                  .withLocation(Identifier.of("brokke", "target_esp_crystals"))
                  .withVertexShader(Identifier.of("brokke", "world_lines_vertex"))
                  .withFragmentShader(Identifier.of("brokke", "world_lines_fragment"))
                  .withVertexFormat(FORMAT_COLOR, DrawMode.TRIANGLES)
                  .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                  .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                  .build();
            }

            if (uniformBuffer == null) {
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "TargetESP Uniforms", 136, 64L);
            }
         } catch (Exception var1) {
         }
      }
   }

   private static void n() {
      if (lensPipeline == null || lensUniformBuffer == null) {
         try {
            if (lensPipeline == null) {
               lensPipeline = RenderPipeline.builder()
                  .withLocation(Identifier.of("brokke", "target_lens"))
                  .withVertexShader(Identifier.of("brokke", "target_lens_vertex"))
                  .withFragmentShader(Identifier.of("brokke", "target_lens_fragment"))
                  .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
                  .withUniform("TargetLensUniforms", UniformType.UNIFORM_BUFFER)
                  .withSampler("Sampler0")
                  .withSampler("Sampler1")
                  .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .withBlend(BlendFunction.TRANSLUCENT)
                  .build();
            }

            if (lensUniformBuffer == null) {
               lensUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "TargetESP Lens Uniforms", 136, 304L);
            }
         } catch (Exception var1) {
         }
      }
   }

   private static void a(Matrix4f var0) {
      if (uniformBuffer != null) {
         if (matrixUniformScratch == null) {
            matrixUniformScratch = MemoryUtil.memAlloc(64);
         }

         ByteBuffer var1 = matrixUniformScratch;
         var1.clear();
         var1.putFloat(var0.m00()).putFloat(var0.m01()).putFloat(var0.m02()).putFloat(var0.m03());
         var1.putFloat(var0.m10()).putFloat(var0.m11()).putFloat(var0.m12()).putFloat(var0.m13());
         var1.putFloat(var0.m20()).putFloat(var0.m21()).putFloat(var0.m22()).putFloat(var0.m23());
         var1.putFloat(var0.m30()).putFloat(var0.m31()).putFloat(var0.m32()).putFloat(var0.m33());
         var1.flip();
         CommandEncoder var2 = RenderSystem.getDevice().createCommandEncoder();
         var2.writeToBuffer(uniformBuffer.slice(), var1);
      }
   }

   private GpuTextureView a(Identifier var1) {
      AbstractTexture var2 = mc.getTextureManager().getTexture(var1);
      return var2 != null ? var2.getGlTextureView() : null;
   }

   private void a(BufferBuilder var1, GpuTextureView var2) {
      BuiltBuffer var3 = var1.endNullable();
      if (var3 != null) {
         int var4 = var3.getDrawParameters().indexCount();
         if (var4 <= 0) {
            var3.close();
         } else {
            try {
               int var5 = var3.getBuffer().remaining();
               if (texturedVbo == null || texturedVbo.size() < var5) {
                  if (texturedVbo != null) {
                     texturedVbo.close();
                  }

                  texturedVbo = RenderSystem.getDevice().createBuffer(() -> "TargetESP Textured Quads VBO", 40, Math.max(var5, 32768));
               }

               CommandEncoder var6 = RenderSystem.getDevice().createCommandEncoder();
               var6.writeToBuffer(texturedVbo.slice(0L, var5), var3.getBuffer());
               ShapeIndexBuffer var7 = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer var8 = var7.getIndexBuffer(var4);
               Framebuffer var9 = mc.getFramebuffer();
               if (var9 == null) {
                  return;
               }

               GpuSampler var10 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

               try (RenderPass var11 = var6.createRenderPass(
                     () -> "TargetESP Textured Quads",
                     var9.getColorAttachmentView(),
                     OptionalInt.empty(),
                     var9.getDepthAttachmentView(),
                     OptionalDouble.empty()
                  )) {
                  var11.setPipeline(bloomPipeline);
                  var11.setVertexBuffer(0, texturedVbo);
                  var11.setIndexBuffer(var8, var7.getIndexType());
                  var11.setUniform("Uniforms", uniformBuffer);
                  var11.bindTexture("Sampler0", var2, var10);
                  var11.drawIndexed(0, 0, var4, 1);
                  return;
               }
            } catch (Exception var21) {
            } finally {
               var3.close();
            }

            return;
         }
      }
   }

   private void a(BufferBuilder var1) {
      BuiltBuffer var2 = var1.endNullable();
      if (var2 != null) {
         int var3 = var2.getDrawParameters().vertexCount();
         if (var3 <= 0) {
            var2.close();
         } else {
            try {
               int var4 = var2.getBuffer().remaining();
               if (trianglesVbo == null || trianglesVbo.size() < var4) {
                  if (trianglesVbo != null) {
                     trianglesVbo.close();
                  }

                  trianglesVbo = RenderSystem.getDevice().createBuffer(() -> "TargetESP Triangles VBO", 40, Math.max(var4, 32768));
               }

               CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
               var5.writeToBuffer(trianglesVbo.slice(0L, var4), var2.getBuffer());
               Framebuffer var6 = mc.getFramebuffer();
               if (var6 == null) {
                  return;
               }

               try (RenderPass var7 = var5.createRenderPass(
                     () -> "TargetESP Triangles", var6.getColorAttachmentView(), OptionalInt.empty(), var6.getDepthAttachmentView(), OptionalDouble.empty()
                  )) {
                  var7.setPipeline(crystalPipeline);
                  var7.setVertexBuffer(0, trianglesVbo);
                  var7.setUniform("Uniforms", uniformBuffer);
                  var7.draw(0, var3);
                  return;
               }
            } catch (Exception var17) {
            } finally {
               var2.close();
            }

            return;
         }
      }
   }

   private Matrix4f o() {
      Window var1 = mc.getWindow();
      float var2 = (float)var1.getFramebufferWidth() / Math.max(1, var1.getFramebufferHeight());
      float var3 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickProgress(true), true);
      return new Matrix4f().perspective((float)Math.toRadians(var3), var2, 0.05F, 1000.0F);
   }

   private Matrix4f p() {
      Matrix4f var1 = this.o();
      float var2 = mc.getRenderTickCounter().getTickProgress(true);
      Camera var3 = mc.gameRenderer.getCamera();
      double var4 = Math.toRadians(var3.getYaw());
      double var6 = Math.toRadians(var3.getPitch());
      double var8 = Math.cos(var4);
      double var10 = Math.sin(var4);
      double var12 = Math.cos(var6);
      double var14 = Math.sin(var6);
      Vec3d var16 = new Vec3d(-var10 * var12, -var14, var8 * var12);
      Vec3d var17 = new Vec3d(0.0, 1.0, 0.0);
      Vec3d var18 = var16.crossProduct(var17);
      if (var18.lengthSquared() < 1.0E-6) {
         var18 = var16.crossProduct(new Vec3d(0.0, 0.0, 1.0));
      }

      var18 = var18.normalize();
      Vec3d var19 = var18.crossProduct(var16).normalize();
      Matrix4f var20 = new Matrix4f().lookAt(0.0F, 0.0F, 0.0F, (float)var16.x, (float)var16.y, (float)var16.z, (float)var19.x, (float)var19.y, (float)var19.z);
      return new Matrix4f(var1).mul(var20);
   }

   private static float a(float var0, float var1, float var2) {
      if (var0 < var1) {
         return Math.min(var0 + var2, var1);
      } else {
         return var0 > var1 ? Math.max(var0 - var2, var1) : var0;
      }
   }

   private Vec3d a(float var1) {
      LivingEntity var2 = this.lastTarget;
      return var2 != null && var2.isAlive() ? var2.getLerpedPos(var1) : this.lastTargetPos;
   }

   private float a(LivingEntity var1) {
      if (this.hurtColor.isToggled() && var1 != null) {
         float var2 = Math.clamp(var1.hurtTime / 10.0F, 0.0F, 1.0F);
         return var2 * var2 * (3.0F - 2.0F * var2);
      } else {
         return 0.0F;
      }
   }
}
