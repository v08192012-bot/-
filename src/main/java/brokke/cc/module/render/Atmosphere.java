package brokke.cc.module.render;

import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.ColorSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.UpdateEvent;
import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@NewFunction(
   name = "Atmosphere",
   description = "Осенняя атмосфера: золотая листва на деревьях и падающие листья",
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
   private static final int DEFAULT_LEAF_COLOR = 0x86A35A;
   private static Atmosphere instance;

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
   private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
   private int[] leafPalette = LEAF_MIXED;
   private int[] grassPalette = GRASS_MIXED;
   private int lastCustomRgb = 0xFFE08A2E;
   private float spawnCarry;
   private boolean colorsDirty;
   private long lastColorRefresh;

   public Atmosphere() {
      instance = this;
      this.addSettings(
         this.palette,
         this.customColor,
         this.leaves,
         this.conifers,
         this.grass,
         this.strength,
         this.fallingLeaves,
         this.density,
         this.radius,
         this.wind
      );
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
   }

   public static Atmosphere getInstance() {
      return instance;
   }

   private void updatePalettes() {
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
      boolean particles = this.fallingLeaves.isToggled();
      boolean tinting = this.leaves.isToggled() || this.grass.isToggled();
      this.customColor.setVisible(this.palette.isSelected("Своя"));
      this.conifers.setVisible(this.leaves.isToggled());
      this.strength.setVisible(tinting);
      this.density.setVisible(particles);
      this.radius.setVisible(particles);
      this.wind.setVisible(particles);
   }

   private void markColorsDirty() {
      this.colorsDirty = true;
   }

   @Override
   public void onEnable() {
      this.spawnCarry = 0.0F;
      this.colorsDirty = false;
      this.refreshColors();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.spawnCarry = 0.0F;
      this.colorsDirty = false;
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

      if (this.palette.isSelected("Своя")) {
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

      if (this.fallingLeaves.isToggled()) {
         this.tickParticles();
      }
   }

   private void tickParticles() {
      this.spawnCarry += (float)(this.density.getValue() / 20.0);
      int attempts = 0;

      while (this.spawnCarry >= 1.0F && attempts < 6) {
         this.spawnCarry -= 1.0F;
         attempts++;
         if (!this.trySpawnLeaf()) {
            break;
         }
      }

      if (this.spawnCarry > 4.0F) {
         this.spawnCarry = 4.0F;
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

      if (state.isOf(Blocks.CHERRY_LEAVES)) {
         return originalColor;
      }

      if (state.isOf(Blocks.SPRUCE_LEAVES) && !module.conifers.isToggled()) {
         return originalColor;
      }

      float strength = (float)module.strength.getValue();
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
