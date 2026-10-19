package brokke.cc.module.misc;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.Aura;
import brokke.cc.core.HandlerProcessor;
import brokke.cc.core.RCTCommand;
import brokke.cc.manager.FriendManager;
import brokke.cc.mixins.interfaces.IBossBarHud;
import brokke.cc.mixins.interfaces.IClientPlayerInteractionManager;
import brokke.cc.mixins.render.IGameRenderer;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.rotation.Rotation2;
import brokke.cc.setting.BindSetting;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.FloatSetting;
import brokke.cc.setting.ModeSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.FinishEatEvent;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.KeyEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.DeltaInventoryUtil;
import brokke.cc.util.PotionUtility;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "ServerAssistant", description = "Помощник для серверов", category = Category.MISC)
public class ServerHelper extends Function {
   private final ModeSetting mode = new ModeSetting("Режим", "HolyWorld", "Funtime");
   private final BindSetting healKey = new BindSetting("Исцеление", -1);
   private final BindSetting brokkeKey = new BindSetting("Хорус", -1);
   private final BooleanSetting throwHeal = new BooleanSetting("Кидать исцел", false);
   private final FloatSetting throwHealRadius = new FloatSetting("Радиус", 4.0F, 1.0F, 6.0F, 0.1F);
   private final BooleanSetting onlyWinnerPotion = new BooleanSetting("Только на победилку", false);
   private final BindSetting disorientationKey = new BindSetting("Дезориентация", -1);
   private final BindSetting trapKey = new BindSetting("Трапка", -1);
   private final BindSetting plastKey = new BindSetting("Пласт", -1);
   private final BindSetting pilKey = new BindSetting("Явная пыль", -1);
   private final BindSetting snegKey = new BindSetting("Снег заморозки", -1);
   private final BindSetting auraKey = new BindSetting("Заряд ветра", -1);
   private final BindSetting netherStarKey = new BindSetting("Стан", -1);
   private final BindSetting prismarineShardKey = new BindSetting("Взрывная трапка", -1);
   private final BindSetting poppedbrokkeKey = new BindSetting("Трапка", -1);
   private final BindSetting fireChargeKey = new BindSetting("Взрывная штучка", -1);
   private final BindSetting snowballKey = new BindSetting("Ком снега", -1);
   private final BooleanSetting snowballEffect = new BooleanSetting("Эффект снежка", false);
   private final BooleanSetting outlineSetting = new BooleanSetting("Отображать радиус", true);
   private final BooleanSetting autoRct = new BooleanSetting("Авто RCT (дабл чар)", false);
   private static final UUID STUN_BOSS_BAR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000077");
   private static final UUID TRAP_BOSS_BAR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000078");
   private ClientBossBar stunBossBar;
   private ClientBossBar trapBossBar;
   private PlayerEntity lastAttackedPlayer;
   private long lastAttackTime;
   private long lastThrowPotionTime;
   private UUID lastPotionTargetUuid;
   private boolean hasThrownForCurrentPotion;
   private final List<ServerHelper.c> trackedConsumables = new CopyOnWriteArrayList<>();
   private static final int[][] EDGES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private static final long FREEZE_SNOWBALL_DURATION_MS = 10000L;
   private static final int FREEZE_OWNERLESS_SNOWBALL_MAX_AGE = 3;
   private static final double FREEZE_OWNERLESS_SNOWBALL_RANGE_SQ = 20.25;
   private static final double FREEZE_OWNERLESS_SNOWBALL_DIRECTION_DOT = 0.45;
   private static final double FREEZE_SEGMENT_EXPAND = 0.25;
   private static final double FREEZE_HITBOX_EXPAND = 0.2;
   private static final int SNOW_GLOW_COLOR = 3842303;
   private final List<ServerHelper.d> trackedSnowballs = new ArrayList<>();
   private final List<ServerHelper.a> freezeMarks = new ArrayList<>();

   public ServerHelper() {
      this.addSettings(
         this.mode,
         this.healKey,
         this.brokkeKey,
         this.disorientationKey,
         this.trapKey,
         this.plastKey,
         this.pilKey,
         this.snegKey,
         this.auraKey,
         this.netherStarKey,
         this.prismarineShardKey,
         this.poppedbrokkeKey,
         this.fireChargeKey,
         this.snowballKey,
         this.snowballEffect,
         this.outlineSetting,
         this.throwHeal,
         this.throwHealRadius,
         this.onlyWinnerPotion,
         this.autoRct
      );
      this.mode.onChanged(this::p);
      this.throwHeal.onChanged(this::p);
      this.p();
   }

   private void p() {
      boolean var1 = this.mode.isMode("Funtime");
      boolean var2 = this.mode.isMode("HolyWorld");
      boolean var3 = this.throwHeal.isToggled();
      this.autoRct.setVisible(var2);
      this.throwHealRadius.setVisible(var3);
      this.onlyWinnerPotion.setVisible(var3);
      this.disorientationKey.setVisible(var1);
      this.trapKey.setVisible(var1);
      this.plastKey.setVisible(var1);
      this.pilKey.setVisible(var1);
      this.snegKey.setVisible(var1);
      this.auraKey.setVisible(var1);
      this.netherStarKey.setVisible(var2);
      this.prismarineShardKey.setVisible(var2);
      this.poppedbrokkeKey.setVisible(var2);
      this.fireChargeKey.setVisible(var2);
      this.snowballKey.setVisible(var2);
      this.snowballEffect.setVisible(var2);
      if (!var2) {
         this.w();
      }
   }

   public boolean k() {
      return this.mode.isMode("Funtime");
   }

   public boolean l() {
      return this.mode.isMode("HolyWorld");
   }

   public List<ServerHelper.b> m() {
      return List.of(
         new ServerHelper.b("Дезориентация", Items.ENDER_EYE, this.disorientationKey),
         new ServerHelper.b("Трапка", Items.NETHERITE_SCRAP, this.trapKey),
         new ServerHelper.b("Пласт", Items.DRIED_KELP, this.plastKey),
         new ServerHelper.b("Явная пыль", Items.SUGAR, this.pilKey),
         new ServerHelper.b("Снег заморозки", Items.SNOWBALL, this.snegKey),
         new ServerHelper.b("Заряд ветра", Items.WIND_CHARGE, this.auraKey)
      );
   }

   public List<ServerHelper.b> n() {
      return List.of(
         new ServerHelper.b("стан", Items.NETHER_STAR, this.netherStarKey),
         new ServerHelper.b("взрывная трапка", Items.PRISMARINE_SHARD, this.prismarineShardKey),
         new ServerHelper.b("трапка хорус", Items.POPPED_CHORUS_FRUIT, this.poppedbrokkeKey),
         new ServerHelper.b("взрывная штучка", Items.FIRE_CHARGE, this.fireChargeKey),
         new ServerHelper.b("ком снега", Items.SNOWBALL, this.snowballKey)
      );
   }

   public boolean a(Entity var1) {
      if (var1 != null && this.isActive() && this.l() && this.snowballEffect.isToggled() && !this.freezeMarks.isEmpty()) {
         long var2 = System.currentTimeMillis();

         for (int var4 = 0; var4 < this.freezeMarks.size() && var4 < this.freezeMarks.size(); var4++) {
            ServerHelper.a var5 = this.freezeMarks.get(var4);
            if (var5.getPlayerId().equals(var1.getUuid()) && var2 < var5.getExpiresAt()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public int get3842303() {
      return 3842303;
   }

   private void q() {
      if (mc.world != null && mc.player != null && this.isActive() && this.l() && this.snowballEffect.isToggled()) {
         long var1 = System.currentTimeMillis();
         this.r();
         this.freezeMarks.removeIf(var3x -> {
            if (var3x.getExpiresAt() <= var1) {
               return true;
            }

            PlayerEntity var4x = this.a(var3x.getPlayerId());
            return var4x == null || !var4x.isAlive() || var4x.isSpectator();
         });

         for (int var3 = this.trackedSnowballs.size() - 1; var3 >= 0; var3--) {
            ServerHelper.d var4 = this.trackedSnowballs.get(var3);
            SnowballEntity var5 = var4.getEntity();
            if (var5 != null && !var5.isRemoved() && !var5.isOnGround()) {
               Vec3d var6 = new Vec3d(var5.getX(), var5.getY(), var5.getZ());
               PlayerEntity var7 = this.a(var4.getLastPos(), var6, var5);
               if (var7 != null) {
                  this.a(var7, var1, var1 + 10000L);
                  this.trackedSnowballs.remove(var3);
               } else {
                  this.trackedSnowballs.set(var3, var4.a(var6));
               }
            } else {
               this.trackedSnowballs.remove(var3);
            }
         }

         for (PlayerEntity var9 : mc.world.getPlayers()) {
            if (var9 != null && var9.isAlive() && !var9.isSpectator() && this.a(var9)) {
               this.a(var9, var1, this.a(var9, var1));
            }
         }

         this.s();
      } else {
         this.trackedSnowballs.clear();
         this.freezeMarks.clear();
      }
   }

   private void r() {
      for (Entity var2 : mc.world.getEntities()) {
         if (var2 instanceof SnowballEntity var3) {
            boolean var4 = this.trackedSnowballs.stream().anyMatch(var1 -> var1.getEntity() == var3);
            if (!var4) {
               this.trackedSnowballs.add(new ServerHelper.d(var3, this.a(var3)));
            }
         }
      }
   }

   private Vec3d a(SnowballEntity var1) {
      Vec3d var2 = new Vec3d(var1.getX(), var1.getY(), var1.getZ());
      Vec3d var3 = new Vec3d(var1.lastRenderX, var1.lastRenderY, var1.lastRenderZ);
      if (var2.squaredDistanceTo(var3) > 1.0E-4) {
         return var3;
      }

      Vec3d var4 = var1.getVelocity();
      if (var4.lengthSquared() > 1.0E-4) {
         return var2.subtract(var4);
      }

      Entity var5 = var1.getOwner();
      return var5 != null ? var5.getEyePos() : var2;
   }

   private PlayerEntity a(Vec3d var1, Vec3d var2, SnowballEntity var3) {
      if (var1 != null && var2 != null) {
         Box var4 = new Box(var1, var2).expand(0.25);
         PlayerEntity var5 = null;
         double var6 = Double.MAX_VALUE;
         Entity var8 = var3 != null ? var3.getOwner() : null;

         for (PlayerEntity var10 : mc.world
            .getEntitiesByClass(PlayerEntity.class, var4, var1x -> var1x.isAlive() && !var1x.isSpectator() && (var8 == null || var1x != var8))) {
            Optional var11 = var10.getBoundingBox().expand(0.2).raycast(var1, var2);
            if (!var11.isEmpty()) {
               double var12 = var1.squaredDistanceTo((Vec3d)var11.get());
               if (var12 < var6) {
                  var6 = var12;
                  var5 = var10;
               }
            }
         }

         return var5;
      } else {
         return null;
      }
   }

   private boolean a(PlayerEntity var1) {
      return var1 != null && this.a(var1.getStatusEffect(StatusEffects.SLOWNESS));
   }

   private boolean a(StatusEffectInstance var1) {
      return var1 != null && var1.getAmplifier() >= 5;
   }

   private long a(PlayerEntity var1, long var2) {
      if (var1 == null) {
         return var2 + 10000L;
      }

      StatusEffectInstance var4 = var1.getStatusEffect(StatusEffects.SLOWNESS);
      return !this.a(var4) ? var2 + 10000L : var2 + Math.max(50L, var4.getDuration() * 50L);
   }

   private void a(PlayerEntity var1, long var2, long var4) {
      UUID var6 = var1.getUuid();
      this.freezeMarks.removeIf(var1x -> var1x.getPlayerId().equals(var6));
      this.freezeMarks.add(new ServerHelper.a(var6, var2, var4));
   }

   private PlayerEntity a(UUID var1) {
      return mc.world != null && var1 != null ? mc.world.getPlayerByUuid(var1) : null;
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      if (mc.world != null && mc.player != null && this.isActive() && this.l() && this.snowballEffect.isToggled() && !this.freezeMarks.isEmpty()) {
         DrawContext var2 = var1.getContext();
         long var3 = System.currentTimeMillis();
         float var5 = mc.getRenderTickCounter().getTickProgress(true);
         ItemStack var6 = new ItemStack(Items.SNOWBALL);
         int var7 = -12934913;

         for (int var8 = 0; var8 < this.freezeMarks.size() && var8 < this.freezeMarks.size(); var8++) {
            ServerHelper.a var9 = this.freezeMarks.get(var8);
            if (var9.getExpiresAt() > var3) {
               PlayerEntity var10 = this.a(var9.getPlayerId());
               if (var10 != null && var10.isAlive() && !var10.isSpectator()) {
                  float var11 = MathHelper.clamp(
                     (float)(var9.getExpiresAt() - var3) / (float)Math.max(1L, var9.getExpiresAt() - var9.getStartedAt()), 0.0F, 1.0F
                  );
                  Vec3d var12 = var10.getLerpedPos(var5);
                  double var13 = var12.x;
                  double var15 = var12.y + var10.getHeight() * 0.5;
                  double var17 = var12.z;
                  float[] var19 = this.a(new Vec3d(var13, var15, var17));
                  if (var19 != null) {
                     this.a(var2, var19[0], var19[1], 9.0F, var6, var11, var7);
                  }
               }
            }
         }
      }
   }

   private void a(DrawContext var1, float var2, float var3, float var4, ItemStack var5, float var6, int var7) {
      RenderUtil.a(var2, var3, var4, new Color(var7, true));
      float var8 = 2.0F / mc.getWindow().getScaleFactor();
      Matrix3x2fStack var9 = var1.getMatrices();
      var9.pushMatrix();
      var9.translate(var2 * var8, var3 * var8);
      float var10 = var4 / 16.0F * var8 * 0.8F;
      var9.scale(var10, var10);
      var9.translate(-8.0F, -8.0F);
      var1.drawItem(var5, 0, 0);
      var1.drawStackOverlay(mc.textRenderer, var5, 0, 0);
      var9.popMatrix();
   }

   private float[] a(Vec3d var1) {
      Camera var2 = mc.gameRenderer.getCamera();
      Vec3d var3 = var2.getCameraPos();
      double var4 = var1.x - var3.x;
      double var6 = var1.y - var3.y;
      double var8 = var1.z - var3.z;
      double var10 = Math.toRadians(var2.getYaw());
      double var12 = Math.toRadians(var2.getPitch());
      double var14 = Math.cos(var10);
      double var16 = Math.sin(var10);
      double var18 = Math.cos(var12);
      double var20 = Math.sin(var12);
      double var22 = -var16 * var18;
      double var24 = -var20;
      double var26 = var14 * var18;
      double var28 = -var14;
      double var30 = -var16;
      double var32 = -var16 * var20;
      double var34 = var18;
      double var36 = var14 * var20;
      double var38 = var4 * var28 + var8 * var30;
      double var40 = var4 * var32 + var6 * var34 + var8 * var36;
      double var42 = var4 * var22 + var6 * var24 + var8 * var26;
      if (var42 < 0.05) {
         return null;
      }

      float var44 = RenderUtil.a();
      float var45 = RenderUtil.b();
      float var46 = ((IGameRenderer)mc.gameRenderer).invokeGetFov(var2, mc.getRenderTickCounter().getTickProgress(true), true);
      double var47 = var45 / 2.0 / Math.tan(Math.toRadians(var46) / 2.0);
      return new float[]{var44 / 2.0F + (float)(var38 / var42 * var47), var45 / 2.0F - (float)(var40 / var42 * var47), (float)(var47 / var42)};
   }

   private void s() {
      if (!this.freezeMarks.isEmpty()) {
         ThreadLocalRandom var1 = ThreadLocalRandom.current();

         for (ServerHelper.a var3 : this.freezeMarks) {
            PlayerEntity var4 = this.a(var3.getPlayerId());
            if (var4 != null && var4.isAlive() && !var4.isSpectator()) {
               Box var5 = var4.getBoundingBox();
               double var6 = var5.maxX - var5.minX;
               double var8 = var5.maxZ - var5.minZ;
               double var10 = var5.maxY - var5.minY;

               for (int var12 = 0; var12 < 4; var12++) {
                  double var13 = var5.minX - 0.15 + var1.nextDouble() * (var6 + 0.3);
                  double var15 = var5.minZ - 0.15 + var1.nextDouble() * (var8 + 0.3);
                  double var17 = var5.minY + var1.nextDouble() * (var10 + 0.4);
                  mc.world.addParticleClient(ParticleTypes.SNOWFLAKE, var13, var17, var15, 0.0, -0.05, 0.0);
               }
            }
         }
      }
   }

   @EventHandler
   public void onBinding(KeyEvent var1) {
      if (var1.getAction() == 1 && mc.currentScreen == null && mc.player != null && mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
         int var2 = var1.getKey();
         Item var3 = null;
         if (this.healKey.getKey() != -1 && var2 == this.healKey.getKey()) {
            var3 = Items.SPLASH_POTION;
         } else if (this.brokkeKey.getKey() != -1 && var2 == this.brokkeKey.getKey()) {
            var3 = Items.CHORUS_FRUIT;
         } else if (this.mode.isMode("Funtime")) {
            if (var2 == this.disorientationKey.getKey()) {
               var3 = Items.ENDER_EYE;
            } else if (var2 == this.trapKey.getKey()) {
               var3 = Items.NETHERITE_SCRAP;
            } else if (var2 == this.plastKey.getKey()) {
               var3 = Items.DRIED_KELP;
            } else if (var2 == this.pilKey.getKey()) {
               var3 = Items.SUGAR;
            } else if (var2 == this.snegKey.getKey()) {
               var3 = Items.SNOWBALL;
            } else if (var2 == this.auraKey.getKey()) {
               var3 = Items.WIND_CHARGE;
            }
         } else if (this.mode.isMode("HolyWorld")) {
            if (var2 == this.netherStarKey.getKey()) {
               var3 = Items.NETHER_STAR;
            } else if (var2 == this.prismarineShardKey.getKey()) {
               var3 = Items.PRISMARINE_SHARD;
            } else if (var2 == this.poppedbrokkeKey.getKey()) {
               var3 = Items.POPPED_CHORUS_FRUIT;
            } else if (var2 == this.fireChargeKey.getKey()) {
               var3 = Items.FIRE_CHARGE;
            } else if (var2 == this.snowballKey.getKey()) {
               var3 = Items.SNOWBALL;
            }
         }

         if (var3 != null) {
            HandlerProcessor.a().getUseableHandler().a(var3.getDefaultStack());
         }
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (this.isActive() && mc.world != null) {
         if (var1.getPacket() instanceof PlaySoundS2CPacket var2
            && this.l()
            && var2.getSound().getKey().isPresent()
            && var2.getSound().getKey().get().getValue().getPath().equals("block.beacon.deactivate")) {
            this.b(new Vec3d(var2.getX(), var2.getY(), var2.getZ()));
         }

         if (var1.getPacket() instanceof ParticleS2CPacket var4 && this.l()) {
            if (var4.getParameters().getType() == ParticleTypes.POOF) {
               this.trackedConsumables
                  .add(new ServerHelper.c(Items.PRISMARINE_SHARD, new Vec3d(var4.getX(), var4.getY(), var4.getZ()), System.currentTimeMillis() + 11000L));
            } else if (var4.getParameters().getType() == ParticleTypes.SQUID_INK) {
               this.b(new Vec3d(var4.getX(), var4.getY(), var4.getZ()));
            }
         }
      }
   }

   private void b(Vec3d var1) {
      long var2 = System.currentTimeMillis();
      boolean var4 = this.trackedConsumables.stream().anyMatch(var1xx -> var1xx.item == Items.NETHER_STAR && var1xx.pos.squaredDistanceTo(var1) < 64.0);
      if (!var4) {
         this.trackedConsumables.add(new ServerHelper.c(Items.NETHER_STAR, var1, var2 + 15000L));
      }
   }

   @EventHandler
   public void onAttack(AttackEntityEvent var1) {
      if (var1.getPlayer() == mc.player && var1.getTarget() instanceof PlayerEntity var2 && !FriendManager.a(var2.getName().getString())) {
         this.lastAttackedPlayer = var2;
         this.lastAttackTime = System.currentTimeMillis();
      }
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         long var2 = System.currentTimeMillis();
         this.trackedConsumables.removeIf(var2xx -> var2 >= var2xx.expireTime);
         this.t();
         this.q();
         this.x();
      } else {
         this.w();
      }
   }

   private void t() {
      if (mc.player != null && mc.world != null && this.isActive() && this.l()) {
         long var1 = System.currentTimeMillis();
         long var3 = 0L;
         long var5 = 0L;

         for (ServerHelper.c var8 : this.trackedConsumables) {
            if (var8.item == Items.NETHER_STAR && var8.expireTime > var1) {
               if (var8.expireTime > var3) {
                  var3 = var8.expireTime;
               }
            } else if (var8.item == Items.PRISMARINE_SHARD && var8.expireTime > var1 && var8.expireTime > var5) {
               var5 = var8.expireTime;
            }
         }

         if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            Map var14 = ((IBossBarHud)mc.inGameHud.getBossBarHud()).getBossBars();
            if (var3 > var1) {
               long var15 = var3 - var1;
               float var10 = MathHelper.clamp((float)var15 / 15000.0F, 0.0F, 1.0F);
               int var11 = (int)Math.ceil(var15 / 1000.0);
               String var12 = var11 <= 5 ? "§c" : "§f";
               MutableText var13 = Text.literal("§fДействие стана §7- " + var12 + var11 + " §f" + b(var11));
               if (this.stunBossBar == null) {
                  this.stunBossBar = new ClientBossBar(
                     STUN_BOSS_BAR_UUID, var13, var10, net.minecraft.entity.boss.BossBar.Color.GREEN, Style.PROGRESS, false, false, false
                  );
               } else {
                  this.stunBossBar.setName(var13);
                  this.stunBossBar.setPercent(var10);
                  this.stunBossBar.setColor(net.minecraft.entity.boss.BossBar.Color.GREEN);
               }

               var14.put(STUN_BOSS_BAR_UUID, this.stunBossBar);
            } else {
               this.u();
            }

            if (var5 > var1) {
               long var16 = var5 - var1;
               float var17 = MathHelper.clamp((float)var16 / 11000.0F, 0.0F, 1.0F);
               int var18 = (int)Math.ceil(var16 / 1000.0);
               String var19 = var18 <= 5 ? "§c" : "§f";
               MutableText var20 = Text.literal("§fДействие Взрывной трапки §7- " + var19 + var18 + " §f" + b(var18));
               if (this.trapBossBar == null) {
                  this.trapBossBar = new ClientBossBar(
                     TRAP_BOSS_BAR_UUID, var20, var17, net.minecraft.entity.boss.BossBar.Color.WHITE, Style.PROGRESS, false, false, false
                  );
               } else {
                  this.trapBossBar.setName(var20);
                  this.trapBossBar.setPercent(var17);
                  this.trapBossBar.setColor(net.minecraft.entity.boss.BossBar.Color.WHITE);
               }

               var14.put(TRAP_BOSS_BAR_UUID, this.trapBossBar);
            } else {
               this.v();
            }
         } else {
            this.w();
         }
      } else {
         this.w();
      }
   }

   private void u() {
      if (this.stunBossBar != null) {
         if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            try {
               ((IBossBarHud)mc.inGameHud.getBossBarHud()).getBossBars().remove(STUN_BOSS_BAR_UUID);
            } catch (Exception var2) {
            }
         }

         this.stunBossBar = null;
      }
   }

   private void v() {
      if (this.trapBossBar != null) {
         if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            try {
               ((IBossBarHud)mc.inGameHud.getBossBarHud()).getBossBars().remove(TRAP_BOSS_BAR_UUID);
            } catch (Exception var2) {
            }
         }

         this.trapBossBar = null;
      }
   }

   private void w() {
      this.u();
      this.v();
   }

   private static String b(int var0) {
      int var1 = var0 % 100;
      int var2 = var0 % 10;
      if (var1 >= 11 && var1 <= 19) {
         return "секунд";
      } else if (var2 == 1) {
         return "секунда";
      } else {
         return var2 >= 2 && var2 <= 4 ? "секунды" : "секунд";
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden && this.isActive() && this.outlineSetting.isToggled()) {
         BufferBuilder var2 = ColorPipeline.a();
         if (var2 != null) {
            boolean var3 = false;
            if (this.l()) {
               boolean var4 = this.a(Items.NETHER_STAR, this.netherStarKey);
               boolean var5 = this.a(Items.PRISMARINE_SHARD, this.prismarineShardKey);
               boolean var6 = this.a(Items.POPPED_CHORUS_FRUIT, this.poppedbrokkeKey);
               if (var4) {
                  this.b(var2, mc.player.getBlockPos(), 15.01F);
                  var3 = true;
               }

               if (var5) {
                  this.a(var2, mc.player.getBlockPos(), 3.99F);
                  var3 = true;
               }

               if (var6) {
                  this.a(var2, mc.player.getBlockPos(), 1.99F);
                  var3 = true;
               }
            } else if (this.k()) {
               boolean var7 = this.a(Items.NETHERITE_SCRAP, this.trapKey);
               if (var7) {
                  this.a(var2, mc.player.getBlockPos(), 1.99F);
                  var3 = true;
               }
            }

            for (ServerHelper.c var9 : this.trackedConsumables) {
               if (var9.item == Items.NETHER_STAR) {
                  this.b(var2, BlockPos.ofFloored(var9.pos), 15.01F);
                  var3 = true;
               } else if (var9.item == Items.PRISMARINE_SHARD) {
                  this.a(var2, BlockPos.ofFloored(var9.pos), 15.01F);
                  var3 = true;
               } else if (var9.item == Items.NETHERITE_SCRAP) {
                  this.a(var2, BlockPos.ofFloored(var9.pos), 1.99F);
                  var3 = true;
               }
            }

            if (var3) {
               ColorPipeline.a(var2, false);
            }
         }
      }
   }

   private boolean a(Item var1, BindSetting var2) {
      if (mc.player == null) {
         return false;
      }

      if (!mc.player.getMainHandStack().isOf(var1) && !mc.player.getOffHandStack().isOf(var1)) {
         if (mc.currentScreen == null && var2 != null && var2.getKey() != -1 && mc.getWindow() != null) {
            long var3 = mc.getWindow().getHandle();
            int var5 = var2.getKey();
            if (var5 >= 0 && var5 <= 348) {
               return GLFW.glfwGetKey(var3, var5) == 1;
            }

            if (var5 >= 0) {
               return GLFW.glfwGetMouseButton(var3, var5) == 1;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void a(BufferBuilder var1, BlockPos var2, float var3) {
      Box var4 = new Box(var2.up()).contract(0.0, 0.2, 0.0).expand(var3);
      boolean var5 = mc.world
         .getPlayers()
         .stream()
         .anyMatch(
            var1x -> var1x != mc.player
               && var1x.isAlive()
               && !var1x.isRemoved()
               && var4.intersects(var1x.getBoundingBox())
               && !FriendManager.a(var1x.getName().getString())
         );
      Color var6 = Theme.getInstance().getAccent();
      int var7 = -436207616 | var6.getRed() << 16 | var6.getGreen() << 8 | var6.getBlue();
      int var8 = -251707071;
      int var9 = var5 ? var8 : var7;
      this.a(var1, var4, var9);
   }

   private void b(BufferBuilder var1, BlockPos var2, float var3) {
      Box var4 = new Box(var2.up()).contract(0.0, 0.2, 0.0).expand(var3);
      boolean var5 = mc.world
         .getPlayers()
         .stream()
         .anyMatch(
            var1x -> var1x != mc.player
               && var1x.isAlive()
               && !var1x.isRemoved()
               && var4.intersects(var1x.getBoundingBox())
               && !FriendManager.a(var1x.getName().getString())
         );
      Color var6 = Theme.getInstance().getAccent();
      int var7 = -436207616 | var6.getRed() << 16 | var6.getGreen() << 8 | var6.getBlue();
      int var8 = -251707071;
      int var9 = var5 ? var8 : var7;
      this.a(var1, var4, var9);
   }

   private void a(BufferBuilder var1, Box var2, int var3) {
      Vec3d[] var4 = new Vec3d[]{
         new Vec3d(var2.minX, var2.minY, var2.minZ),
         new Vec3d(var2.maxX, var2.minY, var2.minZ),
         new Vec3d(var2.maxX, var2.minY, var2.maxZ),
         new Vec3d(var2.minX, var2.minY, var2.maxZ),
         new Vec3d(var2.minX, var2.maxY, var2.minZ),
         new Vec3d(var2.maxX, var2.maxY, var2.minZ),
         new Vec3d(var2.maxX, var2.maxY, var2.maxZ),
         new Vec3d(var2.minX, var2.maxY, var2.maxZ)
      };

      for (int[] var8 : EDGES) {
         float[][] var9 = WorldToScreen.a(var4[var8[0]], var4[var8[1]]);
         if (var9 != null) {
            ColorPipeline.a(var1, var9[0][0], var9[0][1], var9[1][0], var9[1][1], 1.5F, var3);
         }
      }
   }

   private void x() {
      if (!this.isActive() || !this.throwHeal.isToggled() || mc.player == null || mc.world == null) {
         this.hasThrownForCurrentPotion = false;
         this.lastPotionTargetUuid = null;
      } else if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
         PlayerEntity var1 = this.y();
         if (var1 != null && var1.isAlive() && !var1.isSpectator() && !var1.isRemoved()) {
            if (mc.player.distanceTo(var1) > this.throwHealRadius.getFloatValue()) {
               this.hasThrownForCurrentPotion = false;
            } else {
               if (this.lastPotionTargetUuid == null || !this.lastPotionTargetUuid.equals(var1.getUuid())) {
                  this.lastPotionTargetUuid = var1.getUuid();
                  this.hasThrownForCurrentPotion = false;
               }

               if (!this.b(var1)) {
                  this.hasThrownForCurrentPotion = false;
               } else if (!this.hasThrownForCurrentPotion) {
                  long var2 = System.currentTimeMillis();
                  if (var2 - this.lastThrowPotionTime >= 650L) {
                     Rotation2 var4 = this.b((Entity)var1);
                     int var5 = DeltaInventoryUtil.a(true);
                     int var6 = DeltaInventoryUtil.a(false);
                     if (var5 != -1) {
                        int var7 = mc.player.getInventory().getSelectedSlot();
                        if (var5 != var7) {
                           mc.player.getInventory().setSelectedSlot(var5);
                           if (mc.player.networkHandler != null) {
                              mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var5));
                           }
                        }

                        mc.player.setYaw(var4.getYaw());
                        mc.player.setPitch(var4.getPitch());
                        if (mc.interactionManager != null) {
                           ((IClientPlayerInteractionManager)mc.interactionManager)
                              .invokeSendSequencedPacket(
                                 mc.world, var1x -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var1x, var4.getYaw(), var4.getPitch())
                              );
                           mc.player.swingHand(Hand.MAIN_HAND);
                        }

                        if (var5 != var7) {
                           mc.player.getInventory().setSelectedSlot(var7);
                           if (mc.player.networkHandler != null) {
                              mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var7));
                           }
                        }

                        this.lastThrowPotionTime = var2;
                        this.hasThrownForCurrentPotion = true;
                     } else if (var6 != -1) {
                        if (!HandlerProcessor.a().getUseableHandler().getTasks().isEmpty()) {
                           return;
                        }

                        mc.player.setYaw(var4.getYaw());
                        mc.player.setPitch(var4.getPitch());
                        HandlerProcessor.a().getUseableHandler().a(Items.SPLASH_POTION.getDefaultStack(), var4);
                        this.lastThrowPotionTime = var2;
                        this.hasThrownForCurrentPotion = true;
                     }
                  }
               }
            }
         } else {
            this.hasThrownForCurrentPotion = false;
            this.lastPotionTargetUuid = null;
         }
      }
   }

   private PlayerEntity y() {
      if (mc.player != null && mc.world != null) {
         float var1 = this.throwHealRadius.getFloatValue();
         Aura var2 = FunctionManager.a(Aura.class);
         if (var2 != null
            && var2.isActive()
            && var2.getTarget() instanceof PlayerEntity var3
            && var3.isAlive()
            && !var3.isSpectator()
            && !var3.isRemoved()
            && !FriendManager.a(var3.getName().getString())
            && mc.player.distanceTo(var3) <= var1
            && this.b(var3)) {
            return var3;
         }

         long var12 = System.currentTimeMillis();
         if (this.lastAttackedPlayer != null
            && this.lastAttackedPlayer.isAlive()
            && !this.lastAttackedPlayer.isSpectator()
            && !this.lastAttackedPlayer.isRemoved()
            && !FriendManager.a(this.lastAttackedPlayer.getName().getString())
            && mc.player.distanceTo(this.lastAttackedPlayer) <= var1
            && var12 - this.lastAttackTime <= 8000L
            && this.b(this.lastAttackedPlayer)) {
            return this.lastAttackedPlayer;
         }

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            if (var6 != mc.player
               && var6.isAlive()
               && !var6.isSpectator()
               && !var6.isRemoved()
               && !FriendManager.a(var6.getName().getString())
               && mc.player.distanceTo(var6) <= var1
               && this.b(var6)) {
               return var6;
            }
         }

         if (var2 != null
            && var2.isActive()
            && var2.getTarget() instanceof PlayerEntity var13
            && var13.isAlive()
            && !var13.isSpectator()
            && !var13.isRemoved()
            && !FriendManager.a(var13.getName().getString())
            && mc.player.distanceTo(var13) <= var1) {
            return var13;
         }

         if (this.lastAttackedPlayer != null
            && this.lastAttackedPlayer.isAlive()
            && !this.lastAttackedPlayer.isSpectator()
            && !this.lastAttackedPlayer.isRemoved()
            && !FriendManager.a(this.lastAttackedPlayer.getName().getString())
            && mc.player.distanceTo(this.lastAttackedPlayer) <= var1
            && var12 - this.lastAttackTime <= 8000L) {
            return this.lastAttackedPlayer;
         }

         PlayerEntity var14 = null;
         double var16 = (double)var1 * var1;

         for (PlayerEntity var9 : mc.world.getPlayers()) {
            if (var9 != mc.player && var9.isAlive() && !var9.isSpectator() && !var9.isRemoved() && !FriendManager.a(var9.getName().getString())) {
               double var10 = mc.player.squaredDistanceTo(var9);
               if (var10 <= var16) {
                  var16 = var10;
                  var14 = var9;
               }
            }
         }

         return var14;
      } else {
         return null;
      }
   }

   private boolean b(PlayerEntity var1) {
      if (var1 == null) {
         return false;
      }

      boolean var2 = var1.isUsingItem() || var1.getItemUseTimeLeft() > 0 || var1.getItemUseTime() > 0;
      if (!var2) {
         return false;
      }

      ItemStack var3 = var1.getActiveItem();
      ItemStack var4 = var1.getMainHandStack();
      ItemStack var5 = var1.getOffHandStack();
      return this.onlyWinnerPotion.isToggled() ? this.a(var3) || this.a(var4) || this.a(var5) : this.b(var3) || this.b(var4) || this.b(var5);
   }

   private boolean a(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var1.getName().getString().toLowerCase();
         if (var2.contains("победител") || var2.contains("победы")) {
            return true;
         }

         if (!DeltaInventoryUtil.a(var1, "победител") && !DeltaInventoryUtil.a(var1, "Победител")) {
            List var3 = PotionUtility.a(var1);
            boolean var4 = false;
            boolean var5 = false;
            boolean var6 = false;

            for (StatusEffectInstance var8 : (Iterable<StatusEffectInstance>)(Iterable<?>)(var3)) {
               if (var8.getEffectType() == StatusEffects.HASTE) {
                  var4 = true;
               }

               if (var8.getEffectType() == StatusEffects.INVISIBILITY) {
                  var5 = true;
               }

               if (var8.getEffectType() == StatusEffects.INSTANT_HEALTH) {
                  var6 = true;
               }
            }

            return var4 && (var5 || var6);
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean b(ItemStack var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (var1.isOf(Items.POTION)) {
         return true;
      }

      if (this.a(var1)) {
         return true;
      }

      if (PotionUtility.a(var1, StatusEffects.INSTANT_HEALTH)) {
         return true;
      }

      String var2 = var1.getName().getString().toLowerCase();
      return !var2.contains("исцелен") && !var2.contains("heal") && !var2.contains("зелье") && !var2.contains("лечени")
         ? DeltaInventoryUtil.a(var1, "исцелен")
            || DeltaInventoryUtil.a(var1, "Исцелен")
            || DeltaInventoryUtil.a(var1, "Instant Health")
            || DeltaInventoryUtil.a(var1, "Мгновенное лечение")
            || DeltaInventoryUtil.a(var1, "зелье")
         : true;
   }

   private Rotation2 b(Entity var1) {
      if (mc.player != null && var1 != null) {
         Vec3d var2 = mc.player.getEyePos();
         Vec3d var3 = new Vec3d(var1.getX(), var1.getY() + var1.getStandingEyeHeight() * 0.5, var1.getZ());
         double var4 = var3.x - var2.x;
         double var6 = var3.y - var2.y;
         double var8 = var3.z - var2.z;
         double var10 = Math.sqrt(var4 * var4 + var8 * var8);
         float var12 = (float)(Math.toDegrees(Math.atan2(var8, var4)) - 90.0);
         float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
         float var14 = var13;
         if (var10 > 1.5) {
            var14 -= (float)Math.min(18.0, var10 * 1.2);
         }

         return new Rotation2(var12, MathHelper.clamp(var14, -89.0F, 89.0F));
      } else {
         return new Rotation2(0.0F, 0.0F);
      }
   }

   @EventHandler
   public void onFinishEat(FinishEatEvent var1) {
      if (this.isActive()
         && this.l()
         && this.autoRct.isToggled()
         && var1.getUser() == mc.player
         && var1.getStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)
         && !RCTCommand.g()
         && mc.player != null
         && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage(".rct");
      }
   }

   @Override
   public void onDisable() {
      this.w();
      this.freezeMarks.clear();
      this.trackedSnowballs.clear();
      this.trackedConsumables.clear();
      this.lastAttackedPlayer = null;
      this.lastPotionTargetUuid = null;
      this.hasThrownForCurrentPotion = false;
      super.onDisable();
   }

   record a(UUID playerId, long startedAt, long expiresAt) {
      public UUID getPlayerId() {
         return this.playerId;
      }

      public long getStartedAt() {
         return this.startedAt;
      }

      public long getExpiresAt() {
         return this.expiresAt;
      }
   }

   public record b(String name, Item item, BindSetting bind) {
      public String getName() {
         return this.name;
      }

      public Item getItem() {
         return this.item;
      }

      public BindSetting getBind() {
         return this.bind;
      }
   }

   record c(Item item, Vec3d pos, long expireTime) {
      public Item getItem() {
         return this.item;
      }

      public Vec3d getPos() {
         return this.pos;
      }

      public long getExpireTime() {
         return this.expireTime;
      }
   }

   record d(SnowballEntity entity, Vec3d lastPos) {
      public ServerHelper.d a(Vec3d var1) {
         return new ServerHelper.d(this.entity, var1);
      }

      public SnowballEntity getEntity() {
         return this.entity;
      }

      public Vec3d getLastPos() {
         return this.lastPos;
      }
   }
}
