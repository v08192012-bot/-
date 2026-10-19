package brokke.cc.module.render;

import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.NewFunction;
import brokke.cc.render.SnowRenderer;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.Render3DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@NewFunction(
   name = "Atmosphere",
   description = "Атмосфера сезона: осенняя листва или зимний снег с падающими частицами",
   category = Category.RENDER
)
public class Atmosphere extends Function {
   private static final int[] LEAF_MIXED = new int[]{0xD97B29, 0xE8A33D, 0xC0392B, 0xF2C14E, 0xB85C1E, 0xA93226, 0xE07A2F};
   private static final int[] LEAF_GOLDEN = new int[]{0xE8B23A, 0xF2C94C, 0xD9A02B, 0xC58A22, 0xF0BE4A};
   private static final int[] LEAF_CRIMSON = new int[]{0xB03A2E, 0xC0392B, 0x8E2B20, 0xD9534F, 0x9E2A1F};
   private static final int[] LEAF_AMBER = new int[]{0xD97B29, 0xF2A33C, 0xB85C1E, 0xE08E2B, 0xC96A20};
   private static final int[] LEAF_COPPER = new int[]{0xB87333, 0xA65E2E, 0xC98A4B, 0x8C4A24, 0xD19A5A};
   private static final int[] GRASS_MIXED = new int[]{0xC9A44A, 0xB08B3E, 0xD4B45C, 0x9C7A32, 0xBFA04A};
   private static final int[] GRASS_GOLDEN = new int[]{0xD4B45C, 0xC9A44A, 0xBFA04A, 0xDCC06A};
   private static final int[] GRASS_CRIMSON = new int[]{0xB07A3A, 0xA66A32, 0xC08A4A, 0x96602C};
   private static final int[] GRASS_AMBER = new int[]{0xC08A3E, 0xB07A32, 0xD09A4A, 0x9E6C2C};
   private static final int[] GRASS_COPPER = new int[]{0xA67A3E, 0x8C6230, 0xB88A4A, 0x7E5A2A};
   private static final int[] FROST_LEAF = new int[]{0xDCEAF2, 0xC6DCE8, 0xB7D2E2, 0xE8F4FA, 0xAFCBDD, 0xD2E4EE};
   private static final int[] FROST_GRASS = new int[]{0xE6F1F7, 0xD3E4EE, 0xC4DAE8, 0xF0F8FC, 0xCDDFEA};
   private static final int DEFAULT_LEAF_COLOR = 0x86A35A;
   private static final int NO_SNOW = Integer.MIN_VALUE;
   private static final int COLUMNS_PER_TICK = 96;
   private static Atmosphere instance;

   public final ModeSetting season = new ModeSetting("Сезон", "Осень", "Зима");
   public final ModeSetting palette = new ModeSetting(
      "Палитра", "Смешанная", "Золотая", "Багровая", "Янтарная", "Медная", "Своя"
   );
   public final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(224, 138, 46));
   public final BooleanSetting leaves = new BooleanSetting("Листва", true);
   public final BooleanSetting conifers = new BooleanSetting("Хвойные", false);
   public final BooleanSetting grass = new BooleanSetting("Трава", true);
   public final NumberSetting strength = new NumberSetting("Сила покраски", 0.85, 0.0, 1.0, 0.05);
   public final BooleanSetting fallingLeaves = new BooleanSetting("Падающие листья", true);
   public final NumberSetting density = new NumberSetting("Плотность", 18.0, 1.0, 100.0, 1.0);
   public final NumberSetting radius = new NumberSetting("Радиус", 22.0, 6.0, 48.0, 1.0);
   public final NumberSetting wind = new NumberSetting("Ветер", 0.35, 0.0, 1.0, 0.05);
   public final BooleanSetting snowCover = new BooleanSetting("Снежный покров", true);
   public final NumberSetting snowRadius = new NumberSetting("Радиус снега", 24.0, 8.0, 48.0, 1.0);
   public final BooleanSetting snowfall = new BooleanSetting("Снегопад", true);
   public final NumberSetting snowDensity = new NumberSetting("Плотность снега", 30.0, 2.0, 120.0, 1.0);
   public final NumberSetting snowWind = new NumberSetting("Порывы ветра", 0.25, 0.0, 1.0, 0.05);
   public final BooleanSetting sparkle = new BooleanSetting("Блики на снегу", true);
   private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
   private final Long2IntOpenHashMap snowColumns = new Long2IntOpenHashMap();
   private final List<Long> columnOffsets = new ArrayList<>();
   private int[] leafPalette = LEAF_MIXED;
   private int[] grassPalette = GRASS_MIXED;
   private int lastCustomRgb = 0xFFE08A2E;
   private int scanCursor;
   private int scannedRadius = -1;
   private float leafCarry;
   private float snowCarry;
   private boolean colorsDirty;
   private long lastColorRefresh;

   public Atmosphere() {
      instance = this;
      this.addSettings(
         this.season,
         this.palette,
         this.customColor,
         this.leaves,
         this.conifers,
         this.grass,
         this.strength,
         this.fallingLeaves,
         this.density,
         this.radius,
         this.wind,
         this.snowCover,
         this.snowRadius,
         this.snowfall,
         this.snowDensity,
         this.snowWind,
         this.sparkle
      );
      this.season.onChanged(() -> {
         this.updatePalettes();
         this.updateVisibility();
         this.markColorsDirty();
         this.snowColumns.clear();
         this.scannedRadius = -1;
      });
      this.palette.onChanged(() -> {
         this.updatePalettes();
         this.updateVisibility();
         this.markColorsDirty();
      });
      this.leaves.onChanged(() -> {
         this.updateVisibility();
         this.markColorsDirty();
      });
      this.conifers.onChanged(() -> {
         this.updateVisibility();
         this.markColorsDirty();
      });
      this.grass.onChanged(() -> {
         this.updateVisibility();
         this.markColorsDirty();
      });
      this.strength.onChanged(() -> {
         this.updateVisibility();
         this.markColorsDirty();
      });
      this.fallingLeaves.onChanged(this::updateVisibility);
      this.snowCover.onChanged(this::updateVisibility);
      this.snowfall.onChanged(this::updateVisibility);
      this.snowRadius.onChanged(() -> this.scannedRadius = -1);
   }

   public static Atmosphere getInstance() {
      return instance;
   }

   private boolean isWinter() {
      return this.season.isSelected("Зима");
   }

   private void updatePalettes() {
      if (this.isWinter()) {
         this.leafPalette = FROST_LEAF;
         this.grassPalette = FROST_GRASS;
         return;
      }

      if (this.palette.isSelected("Золотая")) {
         this.leafPalette = LEAF_GOLDEN;
         this.grassPalette = GRASS_GOLDEN;
      } else if (this.palette.isSelected("Багровая")) {
         this.leafPalette = LEAF_CRIMSON;
         this.grassPalette = GRASS_CRIMSON;
      } else if (this.palette.isSelected("Янтарная")) {
         this.leafPalette = LEAF_AMBER;
         this.grassPalette = GRASS_AMBER;
      } else if (this.palette.isSelected("Медная")) {
         this.leafPalette = LEAF_COPPER;
         this.grassPalette = GRASS_COPPER;
      } else if (this.palette.isSelected("Своя")) {
         this.leafPalette = null;
         this.grassPalette = null;
      } else {
         this.leafPalette = LEAF_MIXED;
         this.grassPalette = GRASS_MIXED;
      }
   }

   private void updateVisibility() {
      boolean winter = this.isWinter();
      boolean autumn = !winter;
      boolean particles = this.fallingLeaves.isToggled();
      boolean tinting = this.leaves.isToggled() || this.grass.isToggled();
      this.palette.setVisible(autumn);
      this.customColor.setVisible(autumn && this.palette.isSelected("Своя"));
      this.conifers.setVisible(this.leaves.isToggled());
      this.strength.setVisible(tinting);
      this.fallingLeaves.setVisible(autumn);
      this.density.setVisible(autumn && particles);
      this.radius.setVisible(autumn && particles);
      this.wind.setVisible(autumn && particles);
      this.snowCover.setVisible(winter);
      this.snowRadius.setVisible(winter && this.snowCover.isToggled());
      this.snowfall.setVisible(winter);
      this.snowDensity.setVisible(winter && this.snowfall.isToggled());
      this.snowWind.setVisible(winter && this.snowfall.isToggled());
      this.sparkle.setVisible(winter && this.snowCover.isToggled());
   }

   private void markColorsDirty() {
      this.colorsDirty = true;
   }

   @Override
   public void onEnable() {
      this.leafCarry = 0.0F;
      this.snowCarry = 0.0F;
      this.scanCursor = 0;
      this.scannedRadius = -1;
      this.colorsDirty = false;
      this.updatePalettes();
      this.updateVisibility();
      this.refreshColors();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.leafCarry = 0.0F;
      this.snowCarry = 0.0F;
      this.colorsDirty = false;
      this.snowColumns.clear();
      this.scannedRadius = -1;
      this.refreshColors();
      super.onDisable();
   }

   private void refreshColors() {
      if (mc.world != null) {
         this.lastColorRefresh = System.currentTimeMillis();
         mc.world.reloadColor();
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent event) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      if (this.palette.isSelected("Своя") && !this.isWinter()) {
         int rgb = this.customColor.getArgb();
         if (rgb != this.lastCustomRgb) {
            this.lastCustomRgb = rgb;
            this.markColorsDirty();
         }
      }

      if (this.colorsDirty) {
         long now = System.currentTimeMillis();
         if (now - this.lastColorRefresh >= 300L) {
            this.colorsDirty = false;
            this.refreshColors();
         }
      }

      if (this.isWinter()) {
         if (this.snowCover.isToggled()) {
            this.tickSnowCover();
         } else {
            this.snowColumns.clear();
         }

         if (this.snowfall.isToggled()) {
            this.tickSnowfall();
         }
      } else if (this.fallingLeaves.isToggled()) {
         this.tickFallingLeaves();
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (!this.isWinter() || !this.snowCover.isToggled() || this.snowColumns.isEmpty()) {
         return;
      }

      SnowRenderer.render(this.snowColumns, this.sparkle.isToggled(), System.currentTimeMillis());
   }

   private void tickSnowCover() {
      if (this.columnOffsets.isEmpty() || this.scannedRadius != (int)this.snowRadius.getValue()) {
         this.rebuildColumnOffsets();
      }

      int size = this.columnOffsets.size();
      if (size == 0) {
         return;
      }

      int baseX = MathHelper.floor(mc.player.getX());
      int baseY = MathHelper.floor(mc.player.getY());
      int baseZ = MathHelper.floor(mc.player.getZ());
      int amount = Math.min(COLUMNS_PER_TICK, size);

      for (int i = 0; i < amount; i++) {
         if (this.scanCursor >= size) {
            this.scanCursor = 0;
            this.pruneSnowColumns(baseX, baseZ);
         }

         long offset = this.columnOffsets.get(this.scanCursor++);
         int x = baseX + (int)(offset >> 32);
         int z = baseZ + (int)offset;
         long key = BlockPos.asLong(x, 0, z);
         int top = this.findSnowTop(x, z, baseY);
         if (top == NO_SNOW) {
            this.snowColumns.remove(key);
         } else {
            this.snowColumns.put(key, top);
         }
      }
   }

   private void pruneSnowColumns(int baseX, int baseZ) {
      if (this.snowColumns.isEmpty()) {
         return;
      }

      int limit = (this.scannedRadius + 3) * (this.scannedRadius + 3);
      List<Long> stale = new ArrayList<>();

      for (long key : this.snowColumns.keySet()) {
         int dx = BlockPos.unpackLongX(key) - baseX;
         int dz = BlockPos.unpackLongZ(key) - baseZ;
         if (dx * dx + dz * dz > limit) {
            stale.add(key);
         }
      }

      for (long key : stale) {
         this.snowColumns.remove(key);
      }
   }

   private void rebuildColumnOffsets() {
      this.columnOffsets.clear();
      this.scannedRadius = MathHelper.floor(this.snowRadius.getValue());
      int limit = this.scannedRadius * this.scannedRadius;

      for (int dx = -this.scannedRadius; dx <= this.scannedRadius; dx++) {
         for (int dz = -this.scannedRadius; dz <= this.scannedRadius; dz++) {
            if (dx * dx + dz * dz <= limit) {
               this.columnOffsets.add((long)dx << 32 | dz & 4294967295L);
            }
         }
      }

      this.scanCursor = 0;
   }

   private int findSnowTop(int x, int z, int baseY) {
      int top = baseY + 16;
      int bottom = baseY - 24;

      for (int y = top; y >= bottom; y--) {
         this.scanPos.set(x, y, z);
         BlockState state = mc.world.getBlockState(this.scanPos);
         if (state.isAir()) {
            continue;
         }

         if (state.isReplaceable()) {
            continue;
         }

         if (!this.isSnowable(state)) {
            return NO_SNOW;
         }

         BlockState above = mc.world.getBlockState(this.scanPos.up());
         if (above.isAir() || above.isReplaceable()) {
            return y + 1;
         }

         return NO_SNOW;
      }

      return NO_SNOW;
   }

   private boolean isSnowable(BlockState state) {
      if (state.isAir() || !state.getFluidState().isEmpty() || state.isOf(Blocks.SNOW)) {
         return false;
      }

      if (state.isIn(BlockTags.LEAVES)) {
         return true;
      }

      return state.isSideSolidFullSquare(mc.world, this.scanPos, Direction.UP);
   }

   private void tickFallingLeaves() {
      this.leafCarry += (float)(this.density.getValue() / 20.0);
      int spawned = 0;

      while (this.leafCarry >= 1.0F && spawned < 5) {
         this.leafCarry -= 1.0F;
         spawned++;
         if (!this.trySpawnLeaf()) {
            break;
         }
      }

      if (this.leafCarry > 3.0F) {
         this.leafCarry = 3.0F;
      }
   }

   private boolean trySpawnLeaf() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double angle = random.nextDouble() * (Math.PI * 2);
      double distance = Math.sqrt(random.nextDouble()) * this.radius.getValue();
      int x = MathHelper.floor(mc.player.getX() + Math.cos(angle) * distance);
      int z = MathHelper.floor(mc.player.getZ() + Math.sin(angle) * distance);
      int top = MathHelper.floor(mc.player.getY()) + 20;
      int bottom = MathHelper.floor(mc.player.getY()) - 6;

      for (int y = top; y >= bottom; y--) {
         this.scanPos.set(x, y, z);
         BlockState state = mc.world.getBlockState(this.scanPos);
         if (!state.isIn(BlockTags.LEAVES)) {
            continue;
         }

         if (!mc.world.getBlockState(this.scanPos.down()).isAir()) {
            continue;
         }

         int color = this.pickColor(random.nextInt(), true);
         double windStrength = this.wind.getValue();
         double velocityX = (random.nextDouble() - 0.5) * 0.04 + windStrength * 0.035;
         double velocityY = -0.012 - random.nextDouble() * 0.02;
         double velocityZ = (random.nextDouble() - 0.5) * 0.04 + windStrength * 0.015;
         mc.particleManager
            .addParticle(
               TintedParticleEffect.create(ParticleTypes.TINTED_LEAVES, color),
               x + 0.2 + random.nextDouble() * 0.6,
               y - 0.12,
               z + 0.2 + random.nextDouble() * 0.6,
               velocityX,
               velocityY,
               velocityZ
            );
         return true;
      }

      return false;
   }

   private void tickSnowfall() {
      this.snowCarry += (float)(this.snowDensity.getValue() / 20.0);
      int spawned = 0;

      while (this.snowCarry >= 1.0F && spawned < 12) {
         this.snowCarry -= 1.0F;
         spawned++;
         this.spawnSnowflake();
      }

      if (this.snowCarry > 8.0F) {
         this.snowCarry = 8.0F;
      }
   }

   private void spawnSnowflake() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double angle = random.nextDouble() * (Math.PI * 2);
      double distance = Math.sqrt(random.nextDouble()) * this.snowRadius.getValue();
      double x = mc.player.getX() + Math.cos(angle) * distance;
      double z = mc.player.getZ() + Math.sin(angle) * distance;
      double y = mc.player.getY() + 3.0 + random.nextDouble() * 11.0;

      if (!mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir()) {
         return;
      }

      double windStrength = this.snowWind.getValue();
      double velocityX = (random.nextDouble() - 0.5) * 0.02 + windStrength * 0.02;
      double velocityY = -0.008 - random.nextDouble() * 0.014;
      double velocityZ = (random.nextDouble() - 0.5) * 0.02 + windStrength * 0.01;
      mc.particleManager.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, velocityX, velocityY, velocityZ);
   }

   private int pickColor(int hash, boolean leaf) {
      int[] palette = leaf ? this.leafPalette : this.grassPalette;
      int base = palette == null ? this.customColor.getArgb() & 0xFFFFFF : palette[Math.floorMod(hash, palette.length)];
      float brightness = 0.86F + (hash >>> 8 & 0xFF) / 255.0F * 0.28F;
      return shade(base, brightness);
   }

   private boolean isAutumnGrass(BlockState state) {
      return state.isOf(Blocks.GRASS_BLOCK)
         || state.isOf(Blocks.SHORT_GRASS)
         || state.isOf(Blocks.TALL_GRASS)
         || state.isOf(Blocks.FERN)
         || state.isOf(Blocks.LARGE_FERN);
   }

   public static int tintBlock(BlockState state, BlockPos pos, int originalColor) {
      Atmosphere module = instance;
      if (module == null || !module.isActive() || state == null) {
         return originalColor;
      }

      boolean leaf = state.isIn(BlockTags.LEAVES) || state.isOf(Blocks.VINE);
      boolean grassy = !leaf && module.isAutumnGrass(state);
      if (!leaf && !grassy) {
         return originalColor;
      }

      if (leaf && !module.leaves.isToggled()) {
         return originalColor;
      }

      if (grassy && !module.grass.isToggled()) {
         return originalColor;
      }

      if (!module.isWinter() && state.isOf(Blocks.CHERRY_LEAVES)) {
         return originalColor;
      }

      if (state.isOf(Blocks.SPRUCE_LEAVES) && !module.conifers.isToggled()) {
         return originalColor;
      }

      float strength = (float)module.strength.getValue();
      if (module.isWinter()) {
         strength *= 0.8F;
      }

      if (strength <= 0.001F) {
         return originalColor;
      }

      int base = originalColor == -1 ? DEFAULT_LEAF_COLOR : originalColor;
      int hash = pos != null ? pos.hashCode() * 31 + 17 : state.hashCode();
      int target = matchLuminance(module.pickColor(hash, leaf), base);
      return blend(base, target, strength);
   }

   private static int matchLuminance(int color, int reference) {
      float referenceLuma = luma(reference) / 255.0F;
      return shade(color, MathHelper.clamp(referenceLuma / 0.52F, 0.75F, 1.08F));
   }

   private static int blend(int from, int to, float amount) {
      float t = MathHelper.clamp(amount, 0.0F, 1.0F);
      int red = (int)((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
      int green = (int)((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
      int blue = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
      return red << 16 | green << 8 | blue;
   }

   private static int shade(int color, float factor) {
      int red = MathHelper.clamp(Math.round((color >> 16 & 0xFF) * factor), 0, 255);
      int green = MathHelper.clamp(Math.round((color >> 8 & 0xFF) * factor), 0, 255);
      int blue = MathHelper.clamp(Math.round((color & 0xFF) * factor), 0, 255);
      return red << 16 | green << 8 | blue;
   }

   private static float luma(int color) {
      return (color >> 16 & 0xFF) * 0.299F + (color >> 8 & 0xFF) * 0.587F + (color & 0xFF) * 0.114F;
   }
}
