package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
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
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

@NewFunction(name = "Particles", description = "Визуальные дополнения мира", category = Category.RENDER)
public class Particles extends Function {
   private static final Identifier BLOOM_TEX = Identifier.of("brokke", "textures/bloom.png");
   private static final Identifier STAR_TEX = Identifier.of("brokke", "textures/particle/star.png");
   private static final Identifier DOLLAR_TEX = Identifier.of("brokke", "textures/particle/dollar.png");
   private static final Identifier HEART_TEX = Identifier.of("brokke", "textures/particle/heart.png");
   private static final VertexFormat FORMAT_BLOOM = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static final VertexFormat FORMAT_LINES = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static RenderPipeline bloomPipeline;
   private static RenderPipeline linesPipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer texturedVbo;
   private static GpuBuffer linesVbo;
   private static ByteBuffer uniformScratch;
   private static final Matrix4f TEMP_MAT = new Matrix4f();
   private static final int UNIFORM_SIZE = 64;
   private static final Particles.d[] RANDOM_SHAPES = new Particles.d[]{Particles.d.CUBES, Particles.d.DOLLAR, Particles.d.HEART, Particles.d.STAR};
   private static final Particles.d[] TEXTURE_SHAPES = new Particles.d[]{Particles.d.STAR, Particles.d.DOLLAR, Particles.d.HEART};
   private final ModeSetting mode = new ModeSetting("Режим", "Cubes", "Доллар", "Сердце", "Звезда", "Рандом");
   private final NumberSetting worldParticles = new NumberSetting("Кол-во партиклов", 100.0, 0.0, 300.0, 5.0);
   private final BooleanSetting onHit = new BooleanSetting("Партиклы при ударе", true);
   private final NumberSetting hitAmount = new NumberSetting("Кол-во при ударе", 15.0, 1.0, 50.0, 1.0);
   private final BooleanSetting onRun = new BooleanSetting("Партиклы при беге", true);
   private final NumberSetting runAmount = new NumberSetting("Кол-во при беге", 1.0, 1.0, 10.0, 1.0);
   private final List<Particles.c> particles = new CopyOnWriteArrayList<>();
   private int runTick = 0;

   public Particles() {
      this.addSettings(this.mode, this.worldParticles, this.onHit, this.hitAmount, this.onRun, this.runAmount);
      this.onHit.onChanged(() -> this.hitAmount.setVisible(this.onHit.isToggled()));
      this.onRun.onChanged(() -> this.runAmount.setVisible(this.onRun.isToggled()));
   }

   @Override
   public void onDisable() {
      this.particles.clear();
      this.runTick = 0;
   }

   @EventHandler
   public void onAttack(AttackEntityEvent var1) {
      if (this.onHit.isToggled() && var1.getTarget() != null && mc.world != null && mc.player != null) {
         Entity var2 = var1.getTarget();
         Vec3d var3 = var2.getEntityPos().add(0.0, var2.getHeight() / 2.0, 0.0);
         int var4 = (int)this.hitAmount.getValue();

         for (int var5 = 0; var5 < var4 && this.particles.size() < 1000; var5++) {
            Particles.d var6 = a(this.mode.getSelection());
            if (var6 == Particles.d.RANDOM) {
               var6 = RANDOM_SHAPES[ThreadLocalRandom.current().nextInt(RANDOM_SHAPES.length)];
            }

            this.particles
               .add(
                  new Particles.c(
                     var3.add(a(-0.3, 0.3), a(-0.3, 0.3), a(-0.3, 0.3)),
                     Vec3d.ZERO,
                     new Vec3d(a(-2.5, 2.5), a(0.5, 3.5), a(-2.5, 2.5)),
                     new Vec3d(a(-3.0, 3.0), a(-3.0, 3.0), a(-3.0, 3.0)),
                     (long)a(800.0, 1800.0),
                     a(0.12F, 0.28F),
                     var6
                  )
               );
         }
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         this.particles.removeIf(var0 -> var0.alpha.getValue() == 0.0F && var0.timer.a(var0.liveTicks));

         for (Particles.c var3 : this.particles) {
            var3.a();
         }

         if (this.onRun.isToggled() && mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
            this.runTick++;
            if (this.runTick % 3 == 0) {
               int var5 = (int)this.runAmount.getValue();

               for (int var7 = 0; var7 < var5 && this.particles.size() < 1000; var7++) {
                  Particles.d var4 = a(this.mode.getSelection());
                  if (var4 == Particles.d.RANDOM) {
                     var4 = RANDOM_SHAPES[ThreadLocalRandom.current().nextInt(RANDOM_SHAPES.length)];
                  }

                  this.particles
                     .add(
                        new Particles.c(
                           mc.player.getEntityPos().add(a(-0.25, 0.25), a(0.0, 0.15), a(-0.25, 0.25)),
                           Vec3d.ZERO,
                           new Vec3d(a(-0.4, 0.4), a(0.1, 0.8), a(-0.4, 0.4)),
                           new Vec3d(a(-1.0, 1.0), a(-1.0, 1.0), a(-1.0, 1.0)),
                           (long)a(350.0, 700.0),
                           a(0.08F, 0.16F),
                           var4
                        )
                     );
               }
            }
         } else {
            this.runTick = 0;
         }

         int var6 = (int)this.worldParticles.getValue();
         if (this.particles.size() < var6) {
            Particles.d var8 = a(this.mode.getSelection());
            if (var8 == Particles.d.RANDOM) {
               var8 = RANDOM_SHAPES[ThreadLocalRandom.current().nextInt(RANDOM_SHAPES.length)];
            }

            this.particles
               .add(
                  new Particles.c(
                     mc.player.getEntityPos().add(a(-20.0, 20.0), a(0.0, 5.0), a(-20.0, 20.0)),
                     Vec3d.ZERO,
                     new Vec3d(a(-1.0, 1.0), a(0.0, 2.0), a(-1.0, 1.0)),
                     new Vec3d(a(-1.0, 1.0), a(-1.0, 1.0), a(-1.0, 1.0)),
                     (long)a(1500.0, 4500.0),
                     a(0.1F, 0.3F),
                     var8
                  )
               );
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (mc.player != null && mc.world != null && !this.particles.isEmpty()) {
         k();
         if (bloomPipeline != null && linesPipeline != null && uniformBuffer != null) {
            GpuTextureView var2 = this.a(BLOOM_TEX);
            Camera var3 = var1.getCamera();
            Vec3d var4 = var3.getCameraPos();
            float var5 = var1.getTickDelta();
            Matrix4f var6 = this.l();
            a(var6);
            Color var7 = Theme.getInstance().getAccent();
            float var8 = var7.getRed() / 255.0F;
            float var9 = var7.getGreen() / 255.0F;
            float var10 = var7.getBlue() / 255.0F;
            BufferBuilder var11 = null;

            for (Particles.c var13 : this.particles) {
               var13.alpha.a(!var13.timer.a(var13.liveTicks));
               float var14 = var13.alpha.getValue();
               if (!(var14 <= 0.001F) && var2 != null) {
                  if (var11 == null) {
                     var11 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                  }

                  Vec3d var15 = a(var13.prev, var13.pos, var5);
                  float var16 = 4.0F * var13.size;
                  float var17 = var16 / 2.0F;
                  float var18 = var14 * 0.4F;
                  TEMP_MAT.translation((float)(var15.x - var4.x), (float)(var15.y - var4.y), (float)(var15.z - var4.z));
                  TEMP_MAT.rotate(var3.getRotation());
                  var11.vertex(TEMP_MAT, -var17, var17, 0.0F).texture(0.0F, 1.0F).color(var8, var9, var10, var18);
                  var11.vertex(TEMP_MAT, var17, var17, 0.0F).texture(1.0F, 1.0F).color(var8, var9, var10, var18);
                  var11.vertex(TEMP_MAT, var17, -var17, 0.0F).texture(1.0F, 0.0F).color(var8, var9, var10, var18);
                  var11.vertex(TEMP_MAT, -var17, -var17, 0.0F).texture(0.0F, 0.0F).color(var8, var9, var10, var18);
               }
            }

            if (var11 != null && var2 != null) {
               this.a(var11, var2);
            }

            Particles.d var28 = !this.mode.isSelected("Рандом") && !this.mode.isSelected("Random") ? a(this.mode.getSelection()) : null;
            if (var28 == null || var28 == Particles.d.CUBES) {
               BufferBuilder var29 = null;
               byte var31 = 0;

               for (Particles.c var35 : this.particles) {
                  if (var28 == null) {
                     Particles.d var37 = var35.shape != null ? var35.shape : Particles.d.CUBES;
                     if (var37 != Particles.d.CUBES) {
                        continue;
                     }
                  }

                  float var38 = var35.alpha.getValue();
                  if (!(var38 <= 0.001F)) {
                     if (var29 == null) {
                        var29 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, FORMAT_LINES);
                     }

                     Vec3d var40 = a(var35.prev, var35.pos, var5);
                     Vec3d var19 = a(var35.prevRot, var35.rotate, var5);
                     TEMP_MAT.translation((float)(var40.x - var4.x), (float)(var40.y - var4.y), (float)(var40.z - var4.z));
                     TEMP_MAT.rotateXYZ((float)var19.x, (float)var19.y, (float)var19.z);
                     TEMP_MAT.scale(var35.size, var35.size, var35.size);
                     float var20 = var38 * 0.4F;
                     float var21 = 0.8039216F * var38;
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, -0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, 0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, -0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, 0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, 0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, -0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, 0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, -0.5F).color(var8, var9, var10, var20);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, -0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, 0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, -0.5F, 0.5F).color(var8, var9, var10, var21);
                     var29.vertex(TEMP_MAT, -0.5F, 0.5F, 0.5F).color(var8, var9, var10, var21);
                     var31 = (byte)(var31 + 32);
                  }
               }

               if (var29 != null && var31 > 0) {
                  this.a(var29);
               }
            }

            Particles.d[] var30 = var28 == null ? TEXTURE_SHAPES : (var28 == Particles.d.CUBES ? new Particles.d[0] : new Particles.d[]{var28});

            for (Particles.d var39 : var30) {
               Identifier var41 = a(var39);
               if (var41 != null) {
                  GpuTextureView var42 = this.a(var41);
                  if (var42 != null) {
                     BufferBuilder var43 = null;

                     for (Particles.c var22 : this.particles) {
                        if (var28 == null) {
                           Particles.d var23 = var22.shape != null ? var22.shape : Particles.d.CUBES;
                           if (var23 != var39) {
                              continue;
                           }
                        }

                        float var45 = var22.alpha.getValue();
                        if (!(var45 <= 0.001F)) {
                           if (var43 == null) {
                              var43 = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT_BLOOM);
                           }

                           Vec3d var24 = a(var22.prev, var22.pos, var5);
                           Vec3d var25 = a(var22.prevRot, var22.rotate, var5);
                           float var26 = var22.size * 0.75F;
                           float var27 = var45 * 0.9F;
                           TEMP_MAT.translation((float)(var24.x - var4.x), (float)(var24.y - var4.y), (float)(var24.z - var4.z));
                           TEMP_MAT.rotate(var3.getRotation());
                           TEMP_MAT.rotateZ((float)var25.z);
                           var43.vertex(TEMP_MAT, -var26, var26, 0.0F).texture(0.0F, 1.0F).color(var8, var9, var10, var27);
                           var43.vertex(TEMP_MAT, var26, var26, 0.0F).texture(1.0F, 1.0F).color(var8, var9, var10, var27);
                           var43.vertex(TEMP_MAT, var26, -var26, 0.0F).texture(1.0F, 0.0F).color(var8, var9, var10, var27);
                           var43.vertex(TEMP_MAT, -var26, -var26, 0.0F).texture(0.0F, 0.0F).color(var8, var9, var10, var27);
                        }
                     }

                     if (var43 != null) {
                        this.a(var43, var42);
                     }
                  }
               }
            }
         }
      }
   }

   private Particles.d a(Particles.c var1) {
      if (!this.mode.isSelected("Рандом") && !this.mode.isSelected("Random")) {
         return a(this.mode.getSelection());
      } else {
         return var1.shape != null ? var1.shape : Particles.d.CUBES;
      }
   }

   private static void k() {
      if (bloomPipeline == null || linesPipeline == null || uniformBuffer == null) {
         try {
            if (bloomPipeline == null) {
               bloomPipeline = RenderPipeline.builder()
                  .withLocation(Identifier.of("brokke", "world_bloom"))
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

            if (linesPipeline == null) {
               linesPipeline = RenderPipeline.builder()
                  .withLocation(Identifier.of("brokke", "world_lines"))
                  .withVertexShader(Identifier.of("brokke", "world_lines_vertex"))
                  .withFragmentShader(Identifier.of("brokke", "world_lines_fragment"))
                  .withVertexFormat(FORMAT_LINES, DrawMode.DEBUG_LINES)
                  .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                  .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                  .build();
            }

            if (uniformBuffer == null) {
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Particles Uniforms", 136, 64L);
            }
         } catch (Exception var1) {
         }
      }
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

   private GpuTextureView a(Identifier var1) {
      AbstractTexture var2 = mc.getTextureManager().getTexture(var1);
      return var2 == null ? null : var2.getGlTextureView();
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

                  texturedVbo = RenderSystem.getDevice().createBuffer(() -> "Particles Textured Quads VBO", 40, Math.max(var5, 32768));
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
                     () -> "Particles Textured Quads",
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
               if (linesVbo == null || linesVbo.size() < var4) {
                  if (linesVbo != null) {
                     linesVbo.close();
                  }

                  linesVbo = RenderSystem.getDevice().createBuffer(() -> "Particles Lines VBO", 40, Math.max(var4, 32768));
               }

               CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
               var5.writeToBuffer(linesVbo.slice(0L, var4), var2.getBuffer());
               Framebuffer var6 = mc.getFramebuffer();
               if (var6 == null) {
                  return;
               }

               try (RenderPass var7 = var5.createRenderPass(
                     () -> "Particles Lines", var6.getColorAttachmentView(), OptionalInt.empty(), var6.getDepthAttachmentView(), OptionalDouble.empty()
                  )) {
                  var7.setPipeline(linesPipeline);
                  var7.setVertexBuffer(0, linesVbo);
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

   private static Vec3d a(Vec3d var0, Vec3d var1, float var2) {
      return new Vec3d(var0.x + (var1.x - var0.x) * var2, var0.y + (var1.y - var0.y) * var2, var0.z + (var1.z - var0.z) * var2);
   }

   private static double a(double var0, double var2) {
      return var0 + Math.random() * (var2 - var0);
   }

   private static float a(float var0, float var1) {
      return var0 + (float)Math.random() * (var1 - var0);
   }

   private static Identifier a(Particles.d var0) {
      return switch (var0) {
         case DOLLAR -> DOLLAR_TEX;
         case HEART -> HEART_TEX;
         case STAR -> STAR_TEX;
         default -> null;
      };
   }

   private static Particles.d a(String var0) {
      if (var0 == null) {
         return Particles.d.CUBES;
      }

      return switch (var0.toLowerCase()) {
         case "cubes", "кубы", "3d кубы" -> Particles.d.CUBES;
         case "доллар", "dollar" -> Particles.d.DOLLAR;
         case "сердце", "heart" -> Particles.d.HEART;
         case "звезда", "star" -> Particles.d.STAR;
         case "рандом", "random" -> Particles.d.RANDOM;
         default -> Particles.d.CUBES;
      };
   }

   static class a {
      private long duration;
      private float value;
      private Particles.b easing;
      private long startTime;
      private float startValue;
      private float targetValue;
      private boolean done;

      public a(long var1, float var3, Particles.b var4) {
         this.duration = var1;
         this.easing = var4;
         this.value = var3;
         this.startValue = var3;
         this.targetValue = var3;
         this.done = true;
      }

      public a(long var1, Particles.b var3) {
         this(var1, 0.0F, var3);
      }

      public void setDuration(long var1) {
         this.duration = var1;
      }

      public float getValue() {
         return this.value;
      }

      public void a(boolean var1) {
         this.a(var1 ? 1.0F : 0.0F);
      }

      public float a(float var1) {
         long var2 = System.currentTimeMillis();
         if (var1 != this.targetValue) {
            this.startValue = this.value;
            this.targetValue = var1;
            this.startTime = var2;
            this.done = false;
         }

         long var4 = var2 - this.startTime;
         if (var4 >= this.duration) {
            this.value = this.targetValue;
            this.done = true;
            return this.value;
         } else {
            float var6 = (float)var4 / (float)this.duration;
            float var7 = this.easing.a(var6, 0.0F, 1.0F, 1.0F);
            this.value = this.startValue + (this.targetValue - this.startValue) * var7;
            return this.value;
         }
      }
   }

   interface b {
      Particles.b FIGMA_EASE_IN_OUT = a(0.42, 0.0, 0.58, 1.0);

      float a(float var1, float var2, float var3, float var4);

      static Particles.b a(double var0, double var2, double var4, double var6) {
         return new Particles.b() {
            @Override
            public float a(float var1, float var2x, float var3, float var4x) {
               if (var4x <= 0.0F || var1 <= 0.0F) {
                  return var2x;
               }

               if (var1 >= var4x) {
                  return var2x + var3;
               }

               float var5 = var1 / var4x;
               float var6x = this.a((float)var0, (float)var4, var5);
               float var7 = this.d(var6x, (float)var2, (float)var6);
               return var2x + var3 * var7;
            }

            private float a(float var1, float var2x, float var3) {
               float var4x = var3;

               for (int var5 = 0; var5 < 8; var5++) {
                  float var6x = this.b(var4x, var1, var2x);
                  float var7 = this.c(var4x, var1, var2x);
                  if (Math.abs(var6x - var3) < 1.0E-5F || Math.abs(var7) < 1.0E-6F) {
                     break;
                  }

                  var4x -= (var6x - var3) / var7;
                  var4x = Math.max(0.0F, Math.min(1.0F, var4x));
               }

               return var4x;
            }

            private float b(float var1, float var2x, float var3) {
               return 3.0F * (1.0F - var1) * (1.0F - var1) * var1 * var2x + 3.0F * (1.0F - var1) * var1 * var1 * var3 + var1 * var1 * var1;
            }

            private float c(float var1, float var2x, float var3) {
               return 3.0F * ((1.0F - var1) * (1.0F - 3.0F * var1) * var2x + (2.0F * var1 - 3.0F * var1 * var1) * var3) + 3.0F * var1 * var1;
            }

            private float d(float var1, float var2x, float var3) {
               return 3.0F * (1.0F - var1) * (1.0F - var1) * var1 * var2x + 3.0F * (1.0F - var1) * var1 * var1 * var3 + var1 * var1 * var1;
            }
         };
      }
   }

   static class c {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveTicks;
      float size;
      final Particles.e timer = new Particles.e();
      final Particles.a alpha = new Particles.a(300L, Particles.b.FIGMA_EASE_IN_OUT);
      final Particles.d shape;

      public c(Vec3d var1, Vec3d var2, Vec3d var3, Vec3d var4, long var5, float var7, Particles.d var8) {
         this.pos = var1;
         this.rotate = var2;
         this.motion = var3.multiply(0.04F);
         this.rotateMotion = var4.multiply(0.04F);
         this.liveTicks = var5;
         this.size = var7;
         this.prevRot = var2;
         this.prev = var1;
         this.alpha.setDuration(Math.min(300L, Math.max(150L, var5 / 2L)));
         this.shape = var8 != null ? var8 : Particles.d.CUBES;
      }

      void a() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }
   }

   public enum d {
      CUBES,
      DOLLAR,
      HEART,
      STAR,
      RANDOM;
   }

   static class e {
      private final long millis = System.currentTimeMillis();

      boolean a(long var1) {
         return System.currentTimeMillis() - this.millis >= var1;
      }
   }
}
