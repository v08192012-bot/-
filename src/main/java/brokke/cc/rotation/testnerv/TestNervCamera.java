package brokke.cc.rotation.testnerv;

import brokke.cc.core.FreeLookStorage;
import brokke.cc.manager.CommandManager;
import brokke.cc.module.combat.Aura;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.UpdateEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.world.World;

/**
 * Точный перенос драйвера {@code BcCamera} (вид {@code 4} = TestNerv) из Opus.
 *
 * Порядок тика ровно как в оригинале:
 *   1) если аура включена и выбран режим TestNerv — строится признаковый ряд из состояния
 *      игрока, его фактического ввода и цели, которая была найдена в прошлом тике;
 *   2) сеть выбирает пару классов yaw/pitch, шаг переводится в сетку сенсы с накоплением
 *      остатка, предохранитель (если он есть в модели) режет шаг, результат применяется
 *      к «серверной» наводке — той же, что уходит в пакетах;
 *   3) поправщик клавиш на событии ввода переписывает клавиши этим же признакам.
 *
 * Наводка живёт отдельно от камеры, как в Opus: при выключенной «Наводке от первого лица»
 * включается тихий свободный взгляд, при включённой — камера смотрит туда же, куда игрок.
 */
public final class TestNervCamera {
   public static final TestNervCamera INSTANCE = new TestNervCamera();
   public static final String NAME = "TestNerv";
   /** Температура ненулевых ходов — ровно как {@code new BcCamera(..., 4, 1.0)} в Opus. */
   private static final double TEMPERATURE = 1.0;
   private static final String MODEL_FILE = "bc_camera_testnerv.json";
   private static final String MODEL_RESOURCE = "assets/brokke/testnerv/bc_camera_testnerv.json.gz";
   private static final String KEY_FIX_FILE = "bc_keyfix_4.json";
   private static final String KEY_FIX_RESOURCE = "assets/brokke/testnerv/bc_keyfix_4.json.gz";
   /** Лимит отладочного CSV — как в Opus. */
   private static final int DEBUG_LOG_LIMIT = 72000;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   /** Счётчик ударов: признак «удар в этом тике» (в Opus — статический {@code intValue2}). */
   private static int attackCounter;

   private final Random random = new Random();
   private volatile TestNervNet network;
   private volatile TestNervKeyFix keyFix;
   private volatile String status = "не загружался";
   private volatile boolean loading;
   private TestNervNet.State state;
   private World world;
   /** Текущая наводка (в Opus — {@code RotationManager.currentRotation}). */
   private float rotationYaw;
   private float rotationPitch;
   /** Была ли наводка посчитана и применена хотя бы раз в этом эпизоде. */
   private boolean hasRotation;
   private double previousYaw;
   private double previousPitch;
   private double[] lastFeatures;
   private long tickCounter;
   private long lastAppliedTick = Long.MIN_VALUE;
   private long lastComputedTick = Long.MIN_VALUE;
   private int lastAttackCounter;
   private float lastCooldown = 1.0F;
   private int visibleKeyFix = -1;
   private boolean visibleAim;
   private BufferedWriter debugWriter;
   private int loggedTicks;

   private TestNervCamera() {
      EventBus.register(this);
   }

   /** Вызывается из миксина удара: счётчик живёт так же, как {@code BcCamera.method3546}. */
   public static void countAttack() {
      attackCounter++;
   }

   public String status() {
      return this.status;
   }

   public boolean isReady() {
      return this.network != null;
   }

   /**
    * Сброс эпизода: аура выключена, режим сменился, сеть ещё не загрузилась.
    * Поля очищаются ровно как в {@code BcCamera.reset()}; наводка игрока не трогается —
    * это делает аура своим возвратом камеры.
    */
   public void reset() {
      this.state = null;
      this.world = null;
      this.lastFeatures = null;
      this.hasRotation = false;
      this.lastAppliedTick = Long.MIN_VALUE;
      this.lastComputedTick = Long.MIN_VALUE;
      this.visibleKeyFix = -1;
   }

   /** Ленивая загрузка: как {@code BcCamera.method3547}, первый тик только запускает поток. */
   private synchronized TestNervNet ensureLoaded() {
      if (this.network != null) {
         return this.network;
      }

      if (!this.loading) {
         this.loading = true;
         Thread loaderThread = new Thread(this::load, "TestNerv model loader");
         loaderThread.setDaemon(true);
         loaderThread.start();
      }

      return null;
   }

   private void load() {
      try (InputStream modelStream = this.openModel()) {
         TestNervNet loaded = TestNervNet.load(modelStream);
         loaded.temperature = TEMPERATURE;
         this.status = NAME + " загружен (" + MODEL_FILE + ", температура ненулевых ходов " + TEMPERATURE + "). " + loaded.status;

         try (InputStream keyFixStream = this.openKeyFix()) {
            TestNervKeyFix loadedKeyFix = TestNervKeyFix.load(keyFixStream);
            this.keyFix = loadedKeyFix;
            this.status = this.status + " | " + loadedKeyFix.status;
         } catch (FileNotFoundException missing) {
            this.status = this.status + " | поправщика клавиш нет (" + KEY_FIX_FILE + ")";
         } catch (Exception error) {
            this.status = this.status + " | поправщик клавиш НЕ загружен (" + KEY_FIX_FILE + "): " + error.getMessage();
         }

         this.network = loaded;
      } catch (Exception error) {
         this.status = NAME + " НЕ загружен (" + MODEL_FILE + "): " + error.getMessage();
      }

      System.out.println(this.status);
      String message = this.status;
      mc.execute(() -> {
         if (mc.player != null) {
            CommandManager.d(message);
         }
      });
   }

   private InputStream openModel() throws IOException {
      for (Path candidate : List.of(Path.of(MODEL_FILE), Path.of("models", MODEL_FILE), Path.of("..", "models", MODEL_FILE))) {
         if (Files.isRegularFile(candidate)) {
            return Files.newInputStream(candidate);
         }
      }

      InputStream packed = TestNervCamera.class.getClassLoader().getResourceAsStream(MODEL_RESOURCE);

      if (packed == null) {
         throw new FileNotFoundException(MODEL_FILE + " не найден ни рядом с клиентом, ни в jar");
      }

      return new GZIPInputStream(packed);
   }

   private InputStream openKeyFix() throws IOException {
      for (Path candidate : List.of(Path.of(KEY_FIX_FILE), Path.of("models", KEY_FIX_FILE), Path.of("..", "models", KEY_FIX_FILE))) {
         if (Files.isRegularFile(candidate)) {
            return Files.newInputStream(candidate);
         }
      }

      InputStream packed = TestNervCamera.class.getClassLoader().getResourceAsStream(KEY_FIX_RESOURCE);

      if (packed == null) {
         throw new FileNotFoundException(KEY_FIX_FILE + " не найден ни рядом с клиентом, ни в jar");
      }

      return new GZIPInputStream(packed);
   }

   /** Шаг сетки сенсы: ровно {@code BcCamera.method3552}. */
   private static double gridStep() {
      double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
      return sensitivity * sensitivity * sensitivity * 8.0 * 0.15;
   }

   private static int keyMask(PlayerInput input) {
      return (input.forward() ? 1 : 0)
         | (input.backward() ? 2 : 0)
         | (input.left() ? 4 : 0)
         | (input.right() ? 8 : 0)
         | (input.jump() ? 16 : 0)
         | (input.sneak() ? 32 : 0)
         | (input.sprint() ? 64 : 0);
   }

   /** Физически зажатые клавиши: в Opus это {@code GameOptions.*.isPressed()}. */
   private static int pressedKeyMask() {
      return (mc.options.forwardKey.isPressed() ? 1 : 0)
         | (mc.options.backKey.isPressed() ? 2 : 0)
         | (mc.options.leftKey.isPressed() ? 4 : 0)
         | (mc.options.rightKey.isPressed() ? 8 : 0)
         | (mc.options.jumpKey.isPressed() ? 16 : 0)
         | (mc.options.sneakKey.isPressed() ? 32 : 0)
         | (mc.options.sprintKey.isPressed() ? 64 : 0);
   }

   /**
    * Один тик камеры. Выполняется раньше обработчика ауры, поэтому цель и наводка — те же,
    * что были в прошлом тике, и это ровно порядок Opus (TickEvent → обработчик ауры).
    */
   @EventHandler(priority = EventPriority.LOW)
   public void onTick(UpdateEvent event) {
      this.tickCounter++;
      Aura aura = Aura.INSTANCE;

      // Мёртвого игрока наводка не крутит: в Opus наводка на этот момент уже снята
      // менеджером ротации, и камера не «оживает» посреди смерти.
      if (mc.player == null || mc.world == null || mc.player.isDead() || aura == null || !aura.isActive() || !aura.rotationType.isSelected(NAME)) {
         this.reset();
         return;
      }

      TestNervNet network = this.ensureLoaded();

      if (network == null) {
         this.reset();
         return;
      }

      network.holdRatio = aura.getTestNervHold();
      this.visibleAim = Aura.clientLook.isEnabled();

      if (this.state == null || mc.world != this.world) {
         this.state = network.newState();
         this.hasRotation = false;
      }

      this.world = mc.world;

      if (!this.hasRotation) {
         this.rotationYaw = mc.player.getYaw();
         this.rotationPitch = mc.player.getPitch();
      }

      TestNervNet.Tick tick = new TestNervNet.Tick();
      tick.positionX = mc.player.getX();
      tick.positionY = mc.player.getY();
      tick.positionZ = mc.player.getZ();
      tick.keys = keyMask(mc.player.input.playerInput);
      tick.onGround = mc.player.isOnGround();
      tick.horizontalCollision = mc.player.horizontalCollision;
      int attacks = attackCounter;
      float cooldown = mc.player.getAttackCooldownProgress(0.0F);
      tick.attacked = attacks != this.lastAttackCounter || cooldown < this.lastCooldown - 1.0E-4F;
      this.lastAttackCounter = attacks;
      this.lastCooldown = cooldown;
      LivingEntity target = aura.getTarget();

      if (target != null && target.isAlive()) {
         tick.targetX = target.getX();
         tick.targetY = target.getY();
         tick.targetZ = target.getZ();
         tick.targetId = target.getId();
         tick.targetCandidates = 1;
         tick.targetSneaking = target.isSneaking();
      }

      tick.yaw = this.rotationYaw;
      tick.pitch = this.rotationPitch;

      if (this.hasRotation) {
         tick.deltaYaw = tick.yaw - this.previousYaw;
         tick.deltaPitch = tick.pitch - this.previousPitch;
         tick.rotationValid = Math.abs(tick.deltaYaw) <= 180.0;
      } else {
         tick.rotationValid = false;
      }

      this.previousYaw = tick.yaw;
      this.previousPitch = tick.pitch;
      this.hasRotation = true;
      this.state.push(tick);
      double grid = gridStep();

      if (this.lastAppliedTick != this.tickCounter - 1L) {
         this.state.resetAccumulators();
      }

      int pressedKeys = pressedKeyMask();
      double[] features = network.buildFeatures(this.state, grid);
      this.lastFeatures = features;
      double[] input = network.networkInput(features, pressedKeys);
      double[] step;

      if (network.hasGuide()) {
         step = network.sampleGuided(this.state, features, input, grid, this.random);
      } else if (network.limitTable != null) {
         step = network.sampleWithLimits(this.state, features, input, grid, this.random);
      } else if (network.guardByMask) {
         step = network.sampleGuarded(this.state, features, input, grid, this.random);
      } else {
         step = network.sampleDirect(this.state, input, grid, this.random);
      }

      double[] guarded = network.applyGuard(features, step[0], step[1]);
      double[] applied = network.applyToState(this.state, guarded[0], guarded[1], grid, tick.pitch);
      float yaw = (float)(this.rotationYaw + applied[0]);
      float pitch = (float)(this.rotationPitch + applied[1]);
      this.logTick(tick, applied, guarded, grid, cooldown, features);
      this.applyRotation(yaw, pitch);
      this.rotationYaw = yaw;
      this.rotationPitch = pitch;
      this.lastAppliedTick = this.tickCounter;
      this.lastComputedTick = this.tickCounter;
   }

   /** Наводка для сервера; камера либо следует за ней, либо остаётся свободной. */
   private void applyRotation(float yaw, float pitch) {
      if (mc.player == null) {
         return;
      }

      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);

      if (this.visibleAim) {
         FreeLookStorage.a(false);
      } else {
         FreeLookStorage.silent(true);
      }
   }

   /**
    * Поправщик клавиш: тот же тик, что и признаки ({@code longValue4 == longValue} в Opus),
    * живая цель и дистанция в пределах обучения. Снек всегда остаётся прежним.
    */
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onMoveInput(EventMoveInput event) {
      Aura aura = Aura.INSTANCE;
      if (aura != null && !aura.testNervKeys.isEnabled()) {
         // Поправщик отключён вручную: ввод остаётся ровно таким, каким его нажали.
         this.visibleKeyFix = -1;
         return;
      }

      TestNervKeyFix network = this.keyFix;
      double[] features = this.lastFeatures;
      this.visibleKeyFix = -1;

      if (network == null || features == null || this.lastComputedTick != this.tickCounter || !network.applies(features)) {
         return;
      }

      int currentKeys = (event.getForward() > 0.0F ? 1 : 0)
         | (event.getForward() < 0.0F ? 2 : 0)
         | (event.getStrafe() > 0.0F ? 4 : 0)
         | (event.getStrafe() < 0.0F ? 8 : 0)
         | (event.isJump() ? 16 : 0)
         | (event.isSneak() ? 32 : 0)
         | (event.isSprint() ? 64 : 0);
      int keys = network.sampleKeys(features, currentKeys, this.random);
      event.setPlayerInput(new PlayerInput(
         (keys & 1) != 0,
         (keys & 2) != 0,
         (keys & 4) != 0,
         (keys & 8) != 0,
         (keys & 16) != 0,
         event.isSneak(),
         (keys & 64) != 0
      ));
      this.visibleKeyFix = currentKeys;
   }

   @EventHandler
   public void onAttack(AttackEntityEvent event) {
      if (mc.player != null && event.getPlayer() == mc.player) {
         countAttack();
      }
   }

   /** Отладочный CSV: формат и лимит ровно как в Opus (72 000 тиков). */
   private void logTick(TestNervNet.Tick tick, double[] applied, double[] step, double grid, float cooldown, double[] features) {
      if (this.loggedTicks >= DEBUG_LOG_LIMIT) {
         return;
      }

      try {
         if (this.debugWriter == null) {
            this.debugWriter = Files.newBufferedWriter(
               Path.of("bc_camera4_log.csv"),
               StandardCharsets.UTF_8,
               StandardOpenOption.CREATE,
               StandardOpenOption.APPEND
            );
            StringBuilder header = new StringBuilder(
               "tick,age,applied_prev,process_calls_prev,grid,cooldown,x,y,z,keys,on_ground,hcoll,attack,tgt_id,tx,ty,tz,tgt_sneak,yaw,pitch,my,mp,mv_ok,act_yaw,act_pitch,q_yaw,q_pitch,cam_yaw,cam_pitch,model,ukeys_prev"
            );

            for (int index = 0; index < features.length; index++) {
               header.append(",f").append(index);
            }

            this.debugWriter.write(header.append('\n').toString());
         }

         StringBuilder line = new StringBuilder(512);
         line.append(this.tickCounter)
            .append(',')
            .append(mc.player.age)
            .append(',')
            .append(this.lastAppliedTick == this.tickCounter - 1L ? 1 : 0)
            .append(',')
            .append(0)
            .append(',')
            .append(grid)
            .append(',')
            .append(cooldown)
            .append(',')
            .append(tick.positionX)
            .append(',')
            .append(tick.positionY)
            .append(',')
            .append(tick.positionZ)
            .append(',')
            .append(tick.keys)
            .append(',')
            .append(tick.onGround ? 1 : 0)
            .append(',')
            .append(tick.horizontalCollision ? 1 : 0)
            .append(',')
            .append(tick.attacked ? 1 : 0)
            .append(',')
            .append(tick.targetId)
            .append(',')
            .append(tick.targetX)
            .append(',')
            .append(tick.targetY)
            .append(',')
            .append(tick.targetZ)
            .append(',')
            .append(tick.targetSneaking ? 1 : 0)
            .append(',')
            .append(tick.yaw)
            .append(',')
            .append(tick.pitch)
            .append(',')
            .append(tick.deltaYaw)
            .append(',')
            .append(tick.deltaPitch)
            .append(',')
            .append(tick.rotationValid ? 1 : 0)
            .append(',')
            .append(applied[0])
            .append(',')
            .append(applied[1])
            .append(',')
            .append(step[0])
            .append(',')
            .append(step[1])
            .append(',')
            .append(mc.player.getYaw())
            .append(',')
            .append(mc.player.getPitch())
            .append(',')
            .append(this.network != null ? this.network.source : "?")
            .append(',')
            .append(this.visibleKeyFix);

         for (double feature : features) {
            line.append(',').append((float)feature);
         }

         this.debugWriter.write(line.append('\n').toString());

         if (++this.loggedTicks % 20 == 0) {
            this.debugWriter.flush();
         }
      } catch (IOException error) {
         this.loggedTicks = DEBUG_LOG_LIMIT;
      }
   }
}
