package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.RenderContext;
import brokke.cc.render.Theme;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EntityJumpEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryUtil;

@NewFunction(name = "JumpCircle", description = "Круг при прыжке", category = Category.RENDER)
public class JumpCircle extends Function {
   public static final JumpCircle INSTANCE = new JumpCircle();
   private static final int MAX_RIPPLES = 8;
   private static final int UNIFORM_SIZE = 448;
   private static RenderPipeline pipeline;
   private final List<JumpCircle.a> ripples = new ArrayList<>();
   private final Matrix4f inverseMvp = new Matrix4f();
   private final Matrix4f mvp = new Matrix4f();
   public final NumberSetting radius = new NumberSetting("Радиус", 2.0, 1.0, 5.0, 0.1);
   public final NumberSetting waveAmplitude = new NumberSetting("Амплитуда", 1.0, 0.1, 3.0, 0.05);
   public final BooleanSetting syncTheme = new BooleanSetting("Цвет темы", true);
   public final ColorSetting firstColor = new ColorSetting("Первый цвет", new Color(10179040));
   public final ColorSetting secondColor = new ColorSetting("Второй цвет", new Color(5672703));
   private GpuTexture sourceTexture;
   private GpuTextureView sourceTextureView;
   private GpuBuffer uniformBuffer;
   private GpuBuffer fullscreenBuffer;
   private ByteBuffer uniformData;
   private int sourceWidth;
   private int sourceHeight;
   private int ticksExisted;

   public JumpCircle() {
      this.addSettings(this.radius, this.waveAmplitude, this.syncTheme, this.firstColor, this.secondColor);
      this.syncTheme.onChanged(() -> {
         this.firstColor.setVisible(!this.syncTheme.isToggled());
         this.secondColor.setVisible(!this.syncTheme.isToggled());
      });
   }

   private static void a(WorldRenderContext var0) {
      if (INSTANCE.isActive()) {
         INSTANCE.b(var0);
      }
   }

   @Override
   public void onDisable() {
      this.ripples.clear();
      this.k();
      super.onDisable();
   }

   @EventHandler
   public void onJump(EntityJumpEvent var1) {
      if (mc.player != null && mc.world != null && var1.getEntity() == mc.player) {
         if (this.ripples.size() >= 8) {
            this.ripples.remove(0);
         }

         this.ripples.add(new JumpCircle.a(mc.player.getEntityPos()));
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      this.ticksExisted++;
      Iterator var2 = this.ripples.iterator();

      while (var2.hasNext()) {
         JumpCircle.a var3 = (JumpCircle.a)var2.next();
         var3.a();
         if (var3.b()) {
            var2.remove();
         }
      }
   }

   private void b(WorldRenderContext var1) {
      if (mc.player != null && mc.world != null && !this.ripples.isEmpty()) {
         Framebuffer var2 = mc.getFramebuffer();
         if (var2 != null && var2.getColorAttachment() != null && var2.getDepthAttachmentView() != null) {
            int var3 = var2.textureWidth;
            int var4 = var2.textureHeight;
            if (var3 > 0 && var4 > 0) {
               this.a(var3, var4);
               if (this.sourceTexture != null
                  && this.sourceTextureView != null
                  && this.uniformBuffer != null
                  && this.fullscreenBuffer != null
                  && pipeline != null) {
                  float var5 = mc.getRenderTickCounter().getTickProgress(false);
                  Quaternionf var6 = new Quaternionf(mc.gameRenderer.getCamera().getRotation()).conjugate();
                  Matrix4f var7 = new Matrix4f().rotation(var6);
                  Matrix4f var8 = this.a(var5);
                  this.mvp.set(var8).mul(var7);
                  this.inverseMvp.set(this.mvp).invert();
                  this.a(this.inverseMvp, this.mvp, var5);
                  CommandEncoder var9 = RenderSystem.getDevice().createCommandEncoder();
                  var9.copyTextureToTexture(var2.getColorAttachment(), this.sourceTexture, 0, 0, 0, 0, 0, var3, var4);
                  var9.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
                  GpuSampler var10 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                  try (RenderPass var11 = var9.createRenderPass(() -> "JumpCircle", var2.getColorAttachmentView(), OptionalInt.empty())) {
                     var11.setPipeline(pipeline);
                     var11.setVertexBuffer(0, this.fullscreenBuffer);
                     var11.bindTexture("texture_in", this.sourceTextureView, var10);
                     var11.bindTexture("depth_texture_in", var2.getDepthAttachmentView(), var10);
                     var11.setUniform("JumpData", this.uniformBuffer);
                     var11.draw(0, 3);
                  }
               }
            }
         }
      }
   }

   private Matrix4f a(float var1) {
      if (RenderContext.WORLD_READY) {
         return new Matrix4f(RenderContext.PROJECTION_MATRIX);
      }

      Camera var2 = mc.gameRenderer.getCamera();
      float var3 = (float)this.l();
      float var4 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(var2, var1, true);
      return new Matrix4f().perspective((float)Math.toRadians(var4), var3, 0.05F, 1000.0F);
   }

   private double l() {
      Window var1 = mc.getWindow();
      return (double)var1.getFramebufferWidth() / Math.max(1, var1.getFramebufferHeight());
   }

   private static void m() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder()
            .withLocation(Identifier.of("brokke", "jumpcircle"))
            .withVertexShader(Identifier.of("brokke", "jumpcircle_vertex"))
            .withFragmentShader(Identifier.of("brokke", "jumpcircle_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("JumpData", UniformType.UNIFORM_BUFFER)
            .withSampler("texture_in")
            .withSampler("depth_texture_in")
            .withoutBlend()
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
      }
   }

   private void a(int var1, int var2) {
      m();
      if (this.sourceTexture == null || this.sourceWidth != var1 || this.sourceHeight != var2) {
         this.p();
         this.sourceTexture = RenderSystem.getDevice().createTexture(() -> "brokke:jump_circle_source", 5, TextureFormat.RGBA8, var1, var2, 1, 1);
         this.sourceTextureView = RenderSystem.getDevice().createTextureView(this.sourceTexture);
         this.sourceWidth = var1;
         this.sourceHeight = var2;
      }

      if (this.uniformData == null) {
         this.uniformData = MemoryUtil.memAlloc(448).order(ByteOrder.nativeOrder());
      }

      if (this.uniformBuffer == null) {
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "brokke:jump_circle_uniforms", 136, 448L);
      }

      if (this.fullscreenBuffer == null) {
         this.fullscreenBuffer = this.n();
      }
   }

   private GpuBuffer n() {
      ByteBuffer var1 = MemoryUtil.memAlloc(4);
      var1.putInt(0).flip();
      GpuBuffer var2 = RenderSystem.getDevice().createBuffer(() -> "brokke:jump_circle_fullscreen_vertex", 32, var1);
      MemoryUtil.memFree(var1);
      return var2;
   }

   private void a(Matrix4f var1, Matrix4f var2, float var3) {
      this.uniformData.clear();
      float[] var4 = new float[16];
      var1.get(var4);

      for (float var8 : var4) {
         this.uniformData.putFloat(var8);
      }

      var2.get(var4);

      for (float var21 : var4) {
         this.uniformData.putFloat(var21);
      }

      this.uniformData.putFloat(0.1F).putFloat((float)this.waveAmplitude.getValue()).putFloat(0.0F).putFloat(20.0F);
      int var16 = this.syncTheme.isToggled() ? Theme.getInstance().getAccent().getRGB() : this.firstColor.getArgb();
      int var18 = this.syncTheme.isToggled() ? this.o() : this.secondColor.getArgb();
      this.a(this.uniformData, var16);
      this.a(this.uniformData, var18);
      double var20 = (this.ticksExisted + var3) / 8.0;
      this.uniformData.putFloat((float)Math.sin(var20));
      this.uniformData.putFloat((float)Math.cos(var20));
      this.uniformData.putFloat(0.0F).putFloat(0.0F);
      Vec3d var9 = mc.gameRenderer.getCamera().getCameraPos();
      int var10 = Math.max(0, this.ripples.size() - 8);

      for (int var11 = 0; var11 < 8; var11++) {
         int var12 = var10 + var11;
         if (var12 < this.ripples.size()) {
            JumpCircle.a var13 = this.ripples.get(var12);
            Vec3d var14 = var13.getOrigin();
            this.uniformData.putFloat((float)(var14.x - var9.x));
            this.uniformData.putFloat((float)(var14.y - var9.y));
            this.uniformData.putFloat((float)(var14.z - var9.z));
            this.uniformData.putFloat((float)this.radius.getValue() * var13.a(var3));
         } else {
            this.uniformData.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         }
      }

      for (int var22 = 0; var22 < 8; var22++) {
         int var23 = var10 + var22;
         float var24 = var23 < this.ripples.size() ? this.ripples.get(var23).b(var3) : 0.0F;
         this.uniformData.putFloat(var24);
         this.uniformData.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      }

      this.uniformData.flip();
   }

   private int o() {
      Color var1 = Theme.getInstance().getAccent();
      return new Color(Math.min(255, var1.getRed() + 70), Math.min(255, var1.getGreen() + 70), Math.min(255, var1.getBlue() + 70)).getRGB();
   }

   private void a(ByteBuffer var1, int var2) {
      var1.putFloat((var2 >> 16 & 0xFF) / 255.0F);
      var1.putFloat((var2 >> 8 & 0xFF) / 255.0F);
      var1.putFloat((var2 & 0xFF) / 255.0F);
      var1.putFloat((var2 >>> 24 & 0xFF) / 255.0F);
   }

   private void p() {
      if (this.sourceTextureView != null) {
         this.sourceTextureView.close();
         this.sourceTextureView = null;
      }

      if (this.sourceTexture != null) {
         this.sourceTexture.close();
         this.sourceTexture = null;
      }
   }

   public void k() {
      this.p();
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }

      if (this.fullscreenBuffer != null) {
         this.fullscreenBuffer.close();
         this.fullscreenBuffer = null;
      }

      if (this.uniformData != null) {
         MemoryUtil.memFree(this.uniformData);
         this.uniformData = null;
      }

      this.sourceWidth = 0;
      this.sourceHeight = 0;
   }

   static {
      WorldRenderEvents.AFTER_ENTITIES.register(JumpCircle::a);
   }

   public static class a {
      private final Vec3d origin;
      private int age;
      private int prevAge;
      private static final int MAX_AGE = 22;

      public a(Vec3d var1) {
         this.origin = var1;
         this.age = 0;
         this.prevAge = 0;
      }

      public void a() {
         this.prevAge = this.age++;
      }

      public boolean b() {
         return this.age >= 22;
      }

      public Vec3d getOrigin() {
         return this.origin;
      }

      public float a(float var1) {
         float var2 = MathHelper.lerp(var1, (float)this.prevAge, (float)this.age) / 22.0F;
         var2 = MathHelper.clamp(var2, 0.0F, 1.0F);
         return 1.0F - (float)Math.pow(1.0 - var2, 3.0);
      }

      public float b(float var1) {
         float var2 = MathHelper.lerp(var1, (float)this.prevAge, (float)this.age) / 22.0F;
         var2 = MathHelper.clamp(var2, 0.0F, 1.0F);
         return 1.0F - var2;
      }
   }
}
