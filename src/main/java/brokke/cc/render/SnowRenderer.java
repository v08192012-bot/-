package brokke.cc.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;

/**
 * Снежный покров.
 *
 * В каждом просканированном столбце рисуется настоящий ванильный блок снега
 * (blockstate {@code minecraft:snow}, свойство {@code layers} от 1 до 4) через
 * {@link net.minecraft.client.render.block.BlockRenderManager#renderBlockAsEntity}.
 * Это тот же путь отрисовки, которым Minecraft рисует падающие блоки: берётся
 * ванильная модель {@code block/snow_heightN} из ресурсов игры, её геометрия,
 * UV и освещение. Поэтому покров выглядит как настоящий майнкрафтовский снег —
 * с толщиной слоя и боковыми гранями, а не как текстура, натянутая на блок.
 */
public final class SnowRenderer {
   private static final int MAX_LAYERS = 4;
   private static final int MAX_COLUMNS = 4096;
   private static final double MAX_DISTANCE = 56.0;
   private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
   private static final BlockPos.Mutable LIGHT_POS = new BlockPos.Mutable();
   private static BlockState[] layerStates;

   private SnowRenderer() {
   }

   public static void render(Long2IntMap columns, float relief) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null || client.player == null || columns.isEmpty()) {
         return;
      }

      Camera camera = client.gameRenderer.getCamera();
      if (camera == null) {
         return;
      }

      BlockState[] states = states();
      Vec3d cameraPos = camera.getCameraPos();
      Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
      MatrixStack matrices = new MatrixStack();
      MatrixStack.Entry entry = matrices.peek();
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();

      try {
         // WorldRenderer уже снял видовую матрицу со стека, возвращаем её,
         // иначе ванильный слой нарисуется в экранных координатах.
         modelView.mul(RenderContext.VIEW_MATRIX);
         immediate.draw();
         int drawn = 0;

         for (Long2IntMap.Entry column : columns.long2IntEntrySet()) {
            if (drawn >= MAX_COLUMNS) {
               break;
            }

            long key = column.getLongKey();
            int x = BlockPos.unpackLongX(key);
            int z = BlockPos.unpackLongZ(key);
            int y = column.getIntValue();
            double dx = x + 0.5 - cameraPos.x;
            double dz = z + 0.5 - cameraPos.z;
            if (dx * dx + dz * dz > MAX_DISTANCE_SQ) {
               continue;
            }

            BlockState state = states[snowLayers(x, z, relief)];
            entry.getPositionMatrix().translation((float)(x - cameraPos.x), (float)(y - cameraPos.y), (float)(z - cameraPos.z));
            LIGHT_POS.set(x, y, z);
            int light = WorldRenderer.getLightmapCoordinates(client.world, LIGHT_POS);
            client.getBlockRenderManager().renderBlockAsEntity(state, matrices, immediate, light, OverlayTexture.DEFAULT_UV);
            drawn++;
         }

         if (drawn > 0) {
            immediate.draw();
         }
      } catch (Exception exception) {
         // пропускаем кадр, если ванильная модель не успела загрузиться
      } finally {
         modelView.popMatrix();
      }
   }

   private static BlockState[] states() {
      if (layerStates == null) {
         BlockState base = Blocks.SNOW.getDefaultState();
         BlockState[] states = new BlockState[MAX_LAYERS + 1];
         states[1] = base;
         for (int layers = 2; layers <= MAX_LAYERS; layers++) {
            states[layers] = base.with(SnowBlock.LAYERS, layers);
         }

         layerStates = states;
      }

      return layerStates;
   }

   /**
    * Толщина слоя в «сутробах»: чем выше рельеф, тем больше layers у ванильного
    * блока снега. При relief = 0 везде лежит самый тонкий слой (2 пикселя).
    */
   private static int snowLayers(int x, int z, float relief) {
      if (relief <= 0.01F) {
         return 1;
      }

      float scale = 0.18F;
      float fx = x * scale;
      float fz = z * scale;
      int x0 = MathHelper.floor(fx);
      int z0 = MathHelper.floor(fz);
      float tx = smooth(fx - x0);
      float tz = smooth(fz - z0);
      float h00 = hash01(x0, z0);
      float h10 = hash01(x0 + 1, z0);
      float h01 = hash01(x0, z0 + 1);
      float h11 = hash01(x0 + 1, z0 + 1);
      float noise = (h00 * (1.0F - tx) + h10 * tx) * (1.0F - tz) + (h01 * (1.0F - tx) + h11 * tx) * tz;
      int extra = Math.round(noise * relief * 3.0F);
      return MathHelper.clamp(1 + extra, 1, MAX_LAYERS);
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
}
