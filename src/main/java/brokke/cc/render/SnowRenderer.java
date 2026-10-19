package brokke.cc.render;

import brokke.cc.mixins.render.IGameRenderer;
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
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class SnowRenderer {
   private static final Identifier VANILLA_SNOW = Identifier.of("minecraft", "block/snow");
   private static final Identifier BLOCKS_ATLAS = Identifier.of("minecraft", "textures/atlas/blocks.png");
   private static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   private static final Matrix4f TEMP_MAT = new Matrix4f();
   private static final int UNIFORM_SIZE = 64;
   private static final int MAX_QUADS = 12000;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static ByteBuffer uniformScratch;

   private SnowRenderer() {
   }

   public static void render(Long2IntMap columns, boolean sparkle, float relief, long timeMillis) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null || client.player == null || columns.isEmpty()) {
         return;
      }

      Camera camera = client.gameRenderer.getCamera();
      if (camera == null) {
         return;
      }

      Framebuffer framebuffer = client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachmentView() == null || framebuffer.getDepthAttachmentView() == null) {
         return;
      }

      if (!initialize()) {
         return;
      }

      SpriteAtlasTexture atlas;
      Sprite sprite;
      try {
         AbstractTexture texture = client.getTextureManager().getTexture(BLOCKS_ATLAS);
         if (!(texture instanceof SpriteAtlasTexture blockAtlas)) {
            return;
         }

         atlas = blockAtlas;
         sprite = atlas.getSprite(VANILLA_SNOW);
      } catch (Exception exception) {
         return;
      }

      if (sprite == null) {
         return;
      }

      float spriteMinU = sprite.getMinU();
      float spriteMaxU = sprite.getMaxU();
      float spriteMinV = sprite.getMinV();
      float spriteMaxV = sprite.getMaxV();

      float tickDelta = client.getRenderTickCounter().getTickProgress(true);
      Matrix4f projection = createProjection(camera, tickDelta);
      uploadProjection(projection);

      Vec3d cameraPos = camera.getCameraPos();
      float sparklePhase = (float)(timeMillis % 4_000_000L) / 600.0F;
      BufferBuilder builder = null;
      int quadCount = 0;

      for (Long2IntMap.Entry entry : columns.long2IntEntrySet()) {
         if (quadCount >= MAX_QUADS) {
            break;
         }

         long key = entry.getLongKey();
         int x = BlockPos.unpackLongX(key);
         int z = BlockPos.unpackLongZ(key);
         int y = entry.getIntValue();
         double dx = x + 0.5 - cameraPos.x;
         double dz = z + 0.5 - cameraPos.z;
         if (dx * dx + dz * dz > 64.0 * 64.0) {
            continue;
         }

         int hash = mix(x, z);
         float inset = 0.012F + (hash & 7) * 0.007F;
         float lift = 0.012F + ((hash >>> 3) & 3) * 0.003F;
         float alpha = 0.9F + ((hash >>> 5) & 15) / 15.0F * 0.1F;
         float brightness = 0.92F + ((hash >>> 13) & 7) / 7.0F * 0.08F;
         if (sparkle && (hash >>> 9 & 31) == 0) {
            alpha = 1.0F;
            // редкие кристаллики вспыхивают на солнце
            brightness = Math.min(1.0F, brightness + 0.05F + 0.05F * (float)Math.sin(sparklePhase * 1.7F + (hash & 127)));
         }
         float red = brightness;
         float green = brightness;
         float blue = Math.min(1.0F, brightness * 1.02F);

         // UV берём у ванильного спрайта block/snow из атласа блоков
         float u0 = spriteMinU;
         float u1 = spriteMaxU;
         float v0 = spriteMinV;
         float v1 = spriteMaxV;
         if ((hash & 1) != 0) {
            float swap = u0;
            u0 = u1;
            u1 = swap;
         }
         if ((hash & 2) != 0) {
            float swap = v0;
            v0 = v1;
            v1 = swap;
         }

         if (builder == null) {
            builder = Tessellator.getInstance().begin(DrawMode.QUADS, FORMAT);
         }

         float half = 0.5F;
         float x0 = -half + inset;
         float x1 = half - inset;
         float z0 = -half + inset;
         float z1 = half - inset;

         // высота каждого угла берётся из общей карты высот, поэтому соседние
         // блоки стыкуются без щелей — получаются мягкие сугробы
         float h00 = snowHeight(x, z, relief);
         float h10 = snowHeight(x + 1, z, relief);
         float h01 = snowHeight(x, z + 1, relief);
         float h11 = snowHeight(x + 1, z + 1, relief);
         float shadeSE = 0.94F + h11 * 1.2F;

         TEMP_MAT.translation((float)(x + 0.5 - cameraPos.x), (float)(y + lift - cameraPos.y), (float)(z + 0.5 - cameraPos.z));
         builder.vertex(TEMP_MAT, x0, h00, z0).texture(u0, v0).color(red * 0.97F, green * 0.98F, blue, alpha);
         builder.vertex(TEMP_MAT, x0, h01, z1).texture(u0, v1).color(red * 0.96F, green * 0.97F, blue, alpha);
         builder.vertex(TEMP_MAT, x1, h11, z1).texture(u1, v1).color(red * shadeSE, green * shadeSE, blue, alpha);
         builder.vertex(TEMP_MAT, x1, h10, z0).texture(u1, v0).color(red * 0.98F, green * 0.99F, blue, alpha);
         quadCount++;
      }

      if (builder != null && quadCount > 0) {
         draw(builder, atlas.getGlTextureView());
      }
   }

   private static boolean initialize() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null) {
         return true;
      }

      try {
         if (pipeline == null) {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("brokke", "snow_cover"))
               .withVertexShader(Identifier.of("brokke", "world_bloom_vertex"))
               .withFragmentShader(Identifier.of("brokke", "world_bloom_fragment"))
               .withVertexFormat(FORMAT, DrawMode.QUADS)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withSampler("Sampler0")
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
               .build();
         }

         if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Snow Cover Uniforms", 136, UNIFORM_SIZE);
         }

         if (vertexBuffer == null) {
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Snow Cover VBO", 40, 262144L);
         }

         return pipeline != null && uniformBuffer != null && vertexBuffer != null;
      } catch (Exception exception) {
         pipeline = null;
         return false;
      }
   }

   private static void uploadProjection(Matrix4f matrix) {
      if (uniformScratch == null) {
         uniformScratch = MemoryUtil.memAlloc(UNIFORM_SIZE);
      }

      uniformScratch.clear();
      uniformScratch.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      uniformScratch.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      uniformScratch.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      uniformScratch.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
      uniformScratch.flip();
      RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(), uniformScratch);
   }

   private static void draw(BufferBuilder builder, GpuTextureView textureView) {
      BuiltBuffer built = builder.endNullable();
      if (built == null) {
         return;
      }

      try {
         int indexCount = built.getDrawParameters().indexCount();
         if (indexCount <= 0) {
            built.close();
            return;
         }

         int bytes = built.getBuffer().remaining();
         if (vertexBuffer.size() < bytes) {
            vertexBuffer.close();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Snow Cover VBO", 40, Math.max(bytes, 262144));
         }

         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(vertexBuffer.slice(0L, bytes), built.getBuffer());
         ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
         GpuBuffer indices = sequential.getIndexBuffer(indexCount);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer == null) {
            return;
         }

         try (RenderPass pass = encoder.createRenderPass(
               () -> "Snow Cover",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               OptionalDouble.empty()
            )) {
            pass.setPipeline(pipeline);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indices, sequential.getIndexType());
            pass.setUniform("Uniforms", uniformBuffer);
            pass.bindTexture("Sampler0", textureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.drawIndexed(0, 0, indexCount, 1);
         }
      } catch (Exception exception) {
         // пропускаем кадр, если буфер не удалось загрузить
      } finally {
         built.close();
      }
   }

   private static Matrix4f createProjection(Camera camera, float tickDelta) {
      MinecraftClient client = MinecraftClient.getInstance();
      double yaw = Math.toRadians(camera.getYaw());
      double pitch = Math.toRadians(camera.getPitch());
      double cosYaw = Math.cos(yaw);
      double sinYaw = Math.sin(yaw);
      double cosPitch = Math.cos(pitch);
      double sinPitch = Math.sin(pitch);
      Vec3d forward = new Vec3d(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
      Vec3d up = new Vec3d(0.0, 1.0, 0.0);
      Vec3d right = forward.crossProduct(up);
      if (right.lengthSquared() < 1.0E-6) {
         right = forward.crossProduct(new Vec3d(0.0, 0.0, 1.0));
      }

      right = right.normalize();
      Vec3d correctedUp = right.crossProduct(forward).normalize();
      Matrix4f view = new Matrix4f()
         .lookAt(0.0F, 0.0F, 0.0F, (float)forward.x, (float)forward.y, (float)forward.z, (float)correctedUp.x, (float)correctedUp.y, (float)correctedUp.z);
      Window window = client.getWindow();
      float aspect = (float)window.getFramebufferWidth() / Math.max(1, window.getFramebufferHeight());
      float fov = ((IGameRenderer)client.gameRenderer).invokeGetFov(camera, tickDelta, true);
      Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(fov), aspect, 0.05F, 1024.0F);
      return projection.mul(view);
   }

   private static float snowHeight(int cornerX, int cornerZ, float relief) {
      float cellScale = 0.5F;
      float fx = cornerX * cellScale;
      float fz = cornerZ * cellScale;
      int x0 = MathHelper.floor(fx);
      int z0 = MathHelper.floor(fz);
      float tx = smooth(fx - x0);
      float tz = smooth(fz - z0);
      float h00 = hash01(x0, z0);
      float h10 = hash01(x0 + 1, z0);
      float h01 = hash01(x0, z0 + 1);
      float h11 = hash01(x0 + 1, z0 + 1);
      float noise = (h00 * (1.0F - tx) + h10 * tx) * (1.0F - tz) + (h01 * (1.0F - tx) + h11 * tx) * tz;
      float amplitude = 0.018F + relief * 0.085F;
      return 0.01F + noise * amplitude;
   }

   private static float hash01(int x, int z) {
      return (mix(x, z) >>> 8 & 0xFFFF) / 65535.0F;
   }

   private static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   private static int mix(int x, int z) {
      int hash = x * 374761393 + z * 668265263;
      hash = (hash ^ hash >>> 13) * 1274126177;
      return hash ^ hash >>> 16;
   }

   public static void close() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }

      if (uniformScratch != null) {
         MemoryUtil.memFree(uniformScratch);
         uniformScratch = null;
      }

      pipeline = null;
   }
}
