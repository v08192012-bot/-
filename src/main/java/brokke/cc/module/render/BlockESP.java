package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.WorldToScreen;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.MultiSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "BlockESP", description = "Подсвечивает выбранные блоки в мире", category = Category.RENDER)
public class BlockESP extends Function {
   private final ModeSetting mode = new ModeSetting("Режим", "Все", "Только обновление");
   private final NumberSetting range = new NumberSetting("Дистанция", 64.0, 8.0, 128.0, 4.0);
   private final NumberSetting delay = new NumberSetting("Задержка сканирования", 4.0, 1.0, 30.0, 1.0);
   private final BooleanSetting tracers = new BooleanSetting("Трейсера", false);
   private final BooleanSetting fill = new BooleanSetting("Заливка", true);
   private final BooleanSetting outline = new BooleanSetting("Обводка", true);
   private final MultiSetting blocks = new MultiSetting(
      "Блоки", "Алмазы", "Обломки", "Изумруды", "Золото", "Железо", "Редстоун", "Лазурит", "Медь", "Уголь", "Кварц", "Сундуки", "Шалкеры", "Спавнера"
   );
   private final Set<BlockPos> foundBlocks = ConcurrentHashMap.newKeySet();
   private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
      Thread scanThread = new Thread(task, "brokke-block-esp");
      scanThread.setDaemon(true);
      return scanThread;
   });
   private final AtomicInteger searchToken = new AtomicInteger();
   private volatile boolean scanning;
   private long lastScanTime;
   private static final int MAX_RENDERED_BLOCKS = 512;
   private static final int[][] EDGES = {
      {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6},
      {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
   };
   private static final int[][] FACES = {
      {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4},
      {2, 3, 7, 6}, {0, 3, 7, 4}, {1, 2, 6, 5}
   };
   private final float[][] screenCorners = new float[8][];

   public BlockESP() {
      this.blocks.setOption("Алмазы", true);
      this.blocks.setOption("Обломки", true);
      this.blocks.setOption("Изумруды", true);
      this.blocks.setOption("Золото", true);
      this.addSettings(this.mode, this.range, this.delay, this.tracers, this.fill, this.outline, this.blocks);
      this.mode.onChanged(this::updateScanSettingsVisibility);
      this.updateScanSettingsVisibility();
   }

   private void updateScanSettingsVisibility() {
      boolean fullScanEnabled = this.mode.isSelected("Все");
      this.range.setVisible(fullScanEnabled);
      this.delay.setVisible(fullScanEnabled);
   }

   @Override
   public void onEnable() {
      this.cancelScan();
      this.lastScanTime = 0L;
      if (this.mode.isSelected("Все")) {
         this.startScan();
      }
   }

   @Override
   public void onDisable() {
      this.cancelScan();
      this.foundBlocks.clear();
   }

   private void cancelScan() {
      this.searchToken.incrementAndGet();
      this.scanning = false;
   }

   @EventHandler
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) {
         this.cancelScan();
         return;
      }
      long scanDelayMillis = (long)(this.delay.getValue() * 1000.0);
      if (this.mode.isSelected("Все") && !this.scanning
         && System.currentTimeMillis() - this.lastScanTime >= scanDelayMillis) {
         this.startScan();
      }
   }

   private void startScan() {
      if (mc.player == null || mc.world == null || this.scanning) {
         return;
      }
      this.scanning = true;
      int token = this.searchToken.incrementAndGet();
      double radius = this.range.getValue();
      Vec3d playerPosition = mc.player.getEntityPos();
      ClientWorld world = mc.world;
      CompletableFuture.supplyAsync(() -> this.findBlocks(token, playerPosition, radius, world), this.executor)
         .thenAccept(results -> mc.execute(() -> this.applyScanResults(token, results)))
         .exceptionally(failure -> {
            this.scanning = false;
            return null;
         });
   }

   private Set<BlockPos> findBlocks(int token, Vec3d center, double radius, ClientWorld world) {
      if (token != this.searchToken.get() || world == null) {
         return Collections.emptySet();
      }

      int blockRadius = (int)Math.ceil(radius);
      int minX = (int)Math.floor(center.x - blockRadius);
      int maxX = (int)Math.ceil(center.x + blockRadius);
      int minZ = (int)Math.floor(center.z - blockRadius);
      int maxZ = (int)Math.ceil(center.z + blockRadius);
      int minY = Math.max(world.getBottomY() + 1, (int)Math.floor(center.y - blockRadius));
      int maxY = Math.min(world.getTopYInclusive(), (int)Math.ceil(center.y + blockRadius));
      int minChunkX = minX >> 4;
      int maxChunkX = maxX >> 4;
      int minChunkZ = minZ >> 4;
      int maxChunkZ = maxZ >> 4;
      double radiusSquared = radius * radius;
      Set<BlockPos> matches = new HashSet<>();
      BlockPos.Mutable position = new BlockPos.Mutable();

      for (int chunkX = minChunkX; chunkX <= maxChunkX && token == this.searchToken.get(); chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && token == this.searchToken.get(); chunkZ++) {
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
               continue;
            }
            int chunkMinX = Math.max(minX, chunkX << 4);
            int chunkMaxX = Math.min(maxX, (chunkX << 4) + 15);
            int chunkMinZ = Math.max(minZ, chunkZ << 4);
            int chunkMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);

            for (int x = chunkMinX; x <= chunkMaxX && token == this.searchToken.get(); x++) {
               for (int z = chunkMinZ; z <= chunkMaxZ && token == this.searchToken.get(); z++) {
                  double offsetX = x + 0.5 - center.x;
                  double offsetZ = z + 0.5 - center.z;
                  if (offsetX * offsetX + offsetZ * offsetZ > radiusSquared) {
                     continue;
                  }
                  for (int y = minY; y <= maxY; y++) {
                     position.set(x, y, z);
                     Block block = world.getBlockState(position).getBlock();
                     if (this.isSelectedBlock(block)) {
                        matches.add(position.toImmutable());
                     }
                  }
               }
            }
         }
      }
      return matches;
   }

   private void applyScanResults(int token, Set<BlockPos> results) {
      if (token == this.searchToken.get() && this.isActive() && mc.world != null) {
         this.foundBlocks.clear();
         this.foundBlocks.addAll(results);
         this.lastScanTime = System.currentTimeMillis();
      }
      this.scanning = false;
   }

   @EventHandler
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkUpdate) {
         chunkUpdate.visitUpdates((position, state) -> this.updateBlock(position.toImmutable(), state.getBlock()));
      } else if (event.getPacket() instanceof BlockUpdateS2CPacket blockUpdate) {
         this.updateBlock(blockUpdate.getPos().toImmutable(), blockUpdate.getState().getBlock());
      }
   }

   private void updateBlock(BlockPos position, Block block) {
      if (this.isSelectedBlock(block)) {
         this.foundBlocks.add(position);
      } else {
         this.foundBlocks.remove(position);
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent event) {
      if (mc.player == null || mc.world == null || mc.options.hudHidden || this.foundBlocks.isEmpty()) {
         return;
      }
      BufferBuilder buffer = ColorPipeline.a();
      if (buffer == null) {
         return;
      }
      double maxDistanceSquared = this.range.getValue() * this.range.getValue() + 256.0;
      Vec3d eyePosition = mc.player.getEyePos();
      boolean hasGeometry = false;
      int renderedBlocks = 0;

      for (BlockPos position : this.foundBlocks) {
         if (renderedBlocks >= MAX_RENDERED_BLOCKS) {
            break;
         }
         if (position.getSquaredDistance(mc.player.getEntityPos()) > maxDistanceSquared) {
            continue;
         }
         Block block = mc.world.getBlockState(position).getBlock();
         if (!this.isSelectedBlock(block)) {
            continue;
         }
         int rgb = this.getBlockColor(block) & 0xFFFFFF;
         int outlineColor = 0xDC000000 | rgb;
         int fillColor = 0x28000000 | rgb;
         if (this.projectBlockCorners(new Box(position))) {
            if (this.fill.isToggled()) {
               this.drawFaces(buffer, fillColor);
            }
            if (this.outline.isToggled()) {
               this.drawEdges(buffer, outlineColor);
            }
            hasGeometry = true;
            renderedBlocks++;
         }
         if (this.tracers.isToggled()) {
            float[][] tracerPoints = WorldToScreen.a(eyePosition, position.toCenterPos());
            if (tracerPoints != null) {
               ColorPipeline.a(buffer, tracerPoints[0][0], tracerPoints[0][1],
                  tracerPoints[1][0], tracerPoints[1][1], 1.2F, outlineColor);
               hasGeometry = true;
            }
         }
      }
      if (hasGeometry) {
         ColorPipeline.a(buffer, false);
      }
   }

   private boolean projectBlockCorners(Box box) {
      this.screenCorners[0] = WorldToScreen.a(box.minX, box.minY, box.minZ);
      this.screenCorners[1] = WorldToScreen.a(box.maxX, box.minY, box.minZ);
      this.screenCorners[2] = WorldToScreen.a(box.maxX, box.minY, box.maxZ);
      this.screenCorners[3] = WorldToScreen.a(box.minX, box.minY, box.maxZ);
      this.screenCorners[4] = WorldToScreen.a(box.minX, box.maxY, box.minZ);
      this.screenCorners[5] = WorldToScreen.a(box.maxX, box.maxY, box.minZ);
      this.screenCorners[6] = WorldToScreen.a(box.maxX, box.maxY, box.maxZ);
      this.screenCorners[7] = WorldToScreen.a(box.minX, box.maxY, box.maxZ);
      for (float[] corner : this.screenCorners) {
         if (corner != null) {
            return true;
         }
      }
      return false;
   }

   private void drawFaces(BufferBuilder buffer, int color) {
      for (int[] face : FACES) {
         float[] first = this.screenCorners[face[0]];
         float[] second = this.screenCorners[face[1]];
         float[] third = this.screenCorners[face[2]];
         float[] fourth = this.screenCorners[face[3]];
         if (first != null && second != null && third != null && fourth != null) {
            ColorPipeline.a(buffer, first, second, third, fourth, color, color, color, color);
         }
      }
   }

   private void drawEdges(BufferBuilder buffer, int color) {
      for (int[] edge : EDGES) {
         float[] start = this.screenCorners[edge[0]];
         float[] end = this.screenCorners[edge[1]];
         if (start != null && end != null) {
            ColorPipeline.a(buffer, start[0], start[1], end[0], end[1], 1.0F, color);
         }
      }
   }

   private boolean isSelectedBlock(Block block) {
      if (block == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
         return false;
      }
      return this.blocks.isOptionEnabled("Алмазы")
            && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.DIAMOND_BLOCK)
         || this.blocks.isOptionEnabled("Обломки")
            && (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK)
         || this.blocks.isOptionEnabled("Изумруды")
            && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE || block == Blocks.EMERALD_BLOCK)
         || this.blocks.isOptionEnabled("Золото")
            && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE
               || block == Blocks.RAW_GOLD_BLOCK || block == Blocks.GOLD_BLOCK)
         || this.blocks.isOptionEnabled("Железо")
            && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.RAW_IRON_BLOCK || block == Blocks.IRON_BLOCK)
         || this.blocks.isOptionEnabled("Редстоун")
            && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE || block == Blocks.REDSTONE_BLOCK)
         || this.blocks.isOptionEnabled("Лазурит")
            && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE || block == Blocks.LAPIS_BLOCK)
         || this.blocks.isOptionEnabled("Медь")
            && (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.RAW_COPPER_BLOCK || block == Blocks.COPPER_BLOCK)
         || this.blocks.isOptionEnabled("Уголь")
            && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.COAL_BLOCK)
         || this.blocks.isOptionEnabled("Кварц") && block == Blocks.NETHER_QUARTZ_ORE
         || this.blocks.isOptionEnabled("Сундуки")
            && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST || block == Blocks.BARREL)
         || this.blocks.isOptionEnabled("Шалкеры") && block instanceof ShulkerBoxBlock
         || this.blocks.isOptionEnabled("Спавнера") && (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER);
   }

   private int getBlockColor(Block block) {
      if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.DIAMOND_BLOCK) {
         return -16718337;
      } else if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK) {
         return -7710154;
      } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE || block == Blocks.EMERALD_BLOCK) {
         return -16711851;
      } else if (block == Blocks.GOLD_ORE
         || block == Blocks.DEEPSLATE_GOLD_ORE
         || block == Blocks.NETHER_GOLD_ORE
         || block == Blocks.RAW_GOLD_BLOCK
         || block == Blocks.GOLD_BLOCK) {
         return 55040;
      } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.RAW_IRON_BLOCK || block == Blocks.IRON_BLOCK) {
         return -2576493;
      } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE || block == Blocks.REDSTONE_BLOCK) {
         return -57312;
      } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE || block == Blocks.LAPIS_BLOCK) {
         return -14787585;
      } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.RAW_COPPER_BLOCK || block == Blocks.COPPER_BLOCK) {
         return -1606570;
      } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.COAL_BLOCK) {
         return -12566464;
      } else if (block == Blocks.NETHER_QUARTZ_ORE) {
         return -1382946;
      } else if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST || block == Blocks.BARREL) {
         return 38912;
      } else if (block instanceof ShulkerBoxBlock) {
         return -4560696;
      } else if (block != Blocks.SPAWNER && block != Blocks.TRIAL_SPAWNER) {
         MapColor mapColor = block.getDefaultMapColor();
         return mapColor != null ? 0xFF000000 | mapColor.color : -1;
      } else {
         return -65451;
      }
   }
}
