package brokke.cc.neuro;

import brokke.cc.core.NeuroRecorder;
import brokke.cc.manager.CommandManager;
import brokke.cc.rotation.SpaceTimeModel;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.MinecraftClient;

/**
 * Обучение нейромодели прямо в клиенте (без Python и torch).
 *
 * Архитектура ровно как в Tools/neuro/train_spacetime.py и в SpaceTimeModel:
 *   GRU(17 -> 32) + MDN-голова (1 freeze + 3 компоненты по 6), лосс — NLL
 *   2D-гауссовой смеси с корреляцией rho (+ BCE по freeze-выходу).
 *
 * MSE не используется специально: он сглаживает шаги и убивает «живость»
 * ротации (см. гайд по нейро-ротациям).
 *
 * Что подаётся на вход (17 фичей, те же, что считает SpaceTimeDriver):
 *   ошибка до цели по yaw/pitch, её дельта, угловая скорость цели, прошлые шаги,
 *   нормированная ошибка, дистанция, on-target, тики с последнего удара, freeze.
 * Выход: dyaw/dpitch в asinh-пространстве (метки — реальные дельты мыши за тик).
 *
 * Данные: CSV от .neuro record (бей фейкового игрока в плоском мире).
 * Результат: run/brokke/neuro/spacetime.bin — режим «Нейро» подхватывает его
 * без перезапуска (NeuroModels.reload()).
 */
public final class NeuroTrainer {
   private static final int FEATURES = SpaceTimeModel.FEATURES;
   /** 17 базовых фич (+1 = прыжок цели, если датасет записан новой версией рекордера). */
   private static final int FEATURES_MAX = FEATURES + 1;
   private static final int HIDDEN = SpaceTimeModel.HIDDEN;
   private static final int MIX = SpaceTimeModel.MIXTURES;
   private static final int OUT = SpaceTimeModel.OUT;
   private static final int GATES = 3 * HIDDEN;
   private static final int WINDOW = 16;
   private static final int BATCH = 8;
   private static final int MAX_WINDOWS_PER_EPOCH = 1500;
   private static final int MAX_IDLE = 40;
   private static final float FREEZE_EPS = 0.05F;
   private static final float FREEZE_WEIGHT = 1.0F;
   private static final float YAW_LIMIT = 179.0F;
   private static final float PITCH_LIMIT = 90.0F;
   private static final int FREEZE_CUT = 42;
   private static final float MEAN_MIN = -4.0F;
   private static final float MEAN_MAX = 4.0F;
   private static final float LOGS_MIN = -4.0F;
   private static final float LOGS_MAX = 1.5F;
   private static final float RHO_CLAMP = 0.95F;
   private static final double LR = 0.003;
   private static final double CLIP = 5.0;
   private static volatile boolean running;
   private static volatile String stage = "";

   private NeuroTrainer() {
   }

   public static boolean isRunning() {
      return running;
   }

   public static String status() {
      if (!running) {
         return "§bNeuro-тренер: §7свободен. Данные: §f" + dataDir() + "§7, модель: §f" + NeuroModels.file();
      }

      return "§bNeuro-тренер: §f" + stage;
   }

   public static Path dataDir() {
      return NeuroModels.dir().resolve("data");
   }

   /** Запуск обучения в фоновом потоке. */
   public static String start(int epochs) {
      if (running) {
         return "Обучение уже идёт: " + stage;
      }

      if (NeuroRecorder.INSTANCE.isRecording()) {
         return "Идёт запись датасета — сначала .neuro stop, потом train";
      }

      List<Path> files = listData();

      if (files.isEmpty()) {
         return "Нет датасета в " + dataDir() + " — сначала .neuro record 20 (бей фейкового игрока)";
      }

      running = true;
      stage = "загрузка данных";
      Thread var2 = new Thread(() -> {
         try {
            train(files, Math.max(1, Math.min(epochs, 500)));
         } catch (Throwable var6) {
            log("§c[Neuro] Ошибка обучения: §f" + var6);
            var6.printStackTrace();
         } finally {
            running = false;
            stage = "";
         }
      }, "Brokke-Neuro-Trainer");
      var2.setDaemon(true);
      var2.start();
      return null;
   }

   private static List<Path> listData() {
      ArrayList<Path> var0 = new ArrayList<>();
      Path var1 = dataDir();

      try {
         if (Files.isDirectory(var1)) {
            Files.list(var1).filter(var2 -> var2.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")).sorted().forEach(var0::add);
         }
      } catch (Exception var3) {
         log("§c[Neuro] Не смог прочитать " + var1 + ": " + var3.getMessage());
      }

      return var0;
   }

   // ======================= данные =======================

   private static final class Series {
      final float[][] x;
      final float[] y1;
      final float[] y2;
      final float[] dyaw;
      final float[] dpitch;
      final float[] freeze;
      final int features;

      Series(float[][] x, float[] y1, float[] y2, float[] dyaw, float[] dpitch, float[] freeze, int features) {
         this.x = x;
         this.y1 = y1;
         this.y2 = y2;
         this.dyaw = dyaw;
         this.dpitch = dpitch;
         this.freeze = freeze;
         this.features = features;
      }
   }

   private static Series readCsv(Path path) {
      List<String> lines;

      try {
         lines = Files.readAllLines(path, StandardCharsets.UTF_8);
      } catch (Exception var16) {
         log("§c[Neuro] Пропуск " + path.getFileName() + ": " + var16.getMessage());
         return null;
      }

      if (lines.size() < 3) {
         return null;
      }

      String[] var2 = lines.get(0).split(",");
      int[] var3 = new int[FEATURES_MAX];
      java.util.Arrays.fill(var3, -1);
      int feat = 0;
      int var4 = -1;
      int var5 = -1;

      for (int var6 = 0; var6 < var2.length; var6++) {
         String var7 = var2[var6].trim();

         if (var7.length() > 1 && var7.charAt(0) == 'x') {
            try {
               int var8 = Integer.parseInt(var7.substring(1));
               if (var8 >= 0 && var8 < FEATURES_MAX) {
                  var3[var8] = var6;
                  feat = Math.max(feat, var8 + 1);
               }
            } catch (NumberFormatException var14) {
            }
         }

         if (var7.equals("dyaw")) {
            var4 = var6;
         } else if (var7.equals("dpitch")) {
            var5 = var6;
         }
      }

      if (var4 < 0 || var5 < 0) {
         log("§c[Neuro] Пропуск " + path.getFileName() + ": нет колонок dyaw/dpitch");
         return null;
      }

      if (feat < FEATURES) {
         log("§c[Neuro] Пропуск " + path.getFileName() + ": мало x-колонок (" + feat + ")");
         return null;
      }

      for (int var9 = 0; var9 < feat; var9++) {
         if (var3[var9] < 0) {
            log("§c[Neuro] Пропуск " + path.getFileName() + ": нет колонки x" + var9);
            return null;
         }
      }

      int var17 = lines.size() - 1;
      float[][] var18 = new float[var17][feat];
      float[] var19 = new float[var17];
      float[] var20 = new float[var17];
      int var21 = 0;

      for (int var22 = 1; var22 < lines.size(); var22++) {
         String var23 = lines.get(var22);
         if (var23 != null && !var23.isEmpty()) {
            String[] var24 = var23.split(",");
            if (var24.length > Math.max(var4, var5)) {
               try {
                  for (int var25 = 0; var25 < feat; var25++) {
                     var18[var21][var25] = Float.parseFloat(var24[var3[var25]]);
                  }

                  var19[var21] = Float.parseFloat(var24[var4]);
                  var20[var21] = Float.parseFloat(var24[var5]);
                  var21++;
               } catch (Exception var15) {
                  // битая строка — пропускаем
               }
            }
         }
      }

      if (var21 < WINDOW) {
         return null;
      }

      if (var21 < var17) {
         float[][] var26 = new float[var21][feat];
         float[] var27 = new float[var21];
         float[] var28 = new float[var21];
         System.arraycopy(var18, 0, var26, 0, var21);
         System.arraycopy(var19, 0, var27, 0, var21);
         System.arraycopy(var20, 0, var28, 0, var21);
         var18 = var26;
         var19 = var27;
         var20 = var28;
      }

      // вырезаем длинные стояния: иначе модель учится «всегда стоять»
      boolean[] var29 = new boolean[var21];
      int var30 = 0;

      while (var30 < var21) {
         boolean var31 = Math.abs(var19[var30]) < FREEZE_EPS && Math.abs(var20[var30]) < FREEZE_EPS;
         if (var31) {
            int var32 = var30;

            while (var32 < var21 && Math.abs(var19[var32]) < FREEZE_EPS && Math.abs(var20[var32]) < FREEZE_EPS) {
               var32++;
            }

            if (var32 - var30 <= MAX_IDLE) {
               for (int var33 = var30; var33 < var32; var33++) {
                  var29[var33] = true;
               }
            }

            var30 = var32;
         } else {
            var29[var30] = true;
            var30++;
         }
      }

      int var34 = 0;

      for (boolean var35 : var29) {
         if (var35) {
            var34++;
         }
      }

      if (var34 < WINDOW) {
         return null;
      }

      float[][] var36 = new float[var34][feat];
      float[] var37 = new float[var34];
      float[] var38 = new float[var34];
      float[] var39 = new float[var34];
      float[] var44 = new float[var34];
      float[] var45 = new float[var34];
      int var40 = 0;

      for (int var41 = 0; var41 < var21; var41++) {
         if (var29[var41]) {
            System.arraycopy(var18[var41], 0, var36[var40], 0, feat);
            var37[var40] = (float) Math.log(var19[var41] + Math.sqrt(var19[var41] * var19[var41] + 1.0));
            var38[var40] = (float) Math.log(var20[var41] + Math.sqrt(var20[var41] * var20[var41] + 1.0));
            var44[var40] = var19[var41];
            var45[var40] = var20[var41];
            var39[var40] = Math.abs(var19[var41]) < FREEZE_EPS && Math.abs(var20[var41]) < FREEZE_EPS ? 1.0F : 0.0F;
            var40++;
         }
      }

      log(String.format(Locale.ROOT, "§7[Neuro] %s: §f%d §7тиков", path.getFileName(), var34));
      return new Series(var36, var37, var38, var44, var45, var39, feat);
   }

   // ======================= обучение =======================

   private static void train(List<Path> files, int epochs) throws Exception {
      ArrayList<Series> var2 = new ArrayList<>();
      int var3 = 0;

      for (Path var5 : files) {
         Series var6 = readCsv(var5);
         if (var6 != null) {
            var2.add(var6);
            var3 += var6.y1.length;
         }
      }

      if (var2.isEmpty() || var3 < WINDOW) {
         log("§c[Neuro] В датасете нет пригодных строк");
         return;
      }

      // формат фич должен быть одинаковым во всех файлах (17 старых / 18 с прыжком)
      int feat = 0;

      for (Series var4 : var2) {
         feat = Math.max(feat, var4.features);
      }

      int dropped = 0;
      var3 = 0;

      for (java.util.Iterator<Series> var5 = var2.iterator(); var5.hasNext(); ) {
         Series var6 = var5.next();
         if (var6.features != feat) {
            var5.remove();
            dropped++;
         } else {
            var3 += var6.y1.length;
         }
      }

      if (dropped > 0) {
         log("§7[Neuro] Пропущено файлов со старым форматом фич: §f" + dropped);
      }

      if (var2.isEmpty() || var3 < WINDOW) {
         log("§c[Neuro] В датасете нет пригодных строк");
         return;
      }

      stage = "инициализация";
      float[] var63 = new float[GATES * feat];
      float[] var64 = new float[GATES * HIDDEN];
      float[] var65 = new float[GATES];
      float[] var66 = new float[GATES];
      float[] var7 = new float[OUT * HIDDEN];
      float[] var8 = new float[OUT];
      Random var9 = new Random(42L);
      float var10 = (float) (1.0 / Math.sqrt(HIDDEN));

      for (int var11 = 0; var11 < var63.length; var11++) {
         var63[var11] = (float) var9.nextGaussian() * 0.08F;
      }

      for (int var70 = 0; var70 < var64.length; var70++) {
         var64[var70] = (float) var9.nextGaussian() * 0.35F * var10;
      }

      for (int var71 = 0; var71 < var7.length; var71++) {
         var7[var71] = (float) var9.nextGaussian() * 0.05F;
      }

      for (int var72 = 0; var72 < MIX; var72++) {
         var8[1 + 6 * var72 + 3] = 0.2F;
         var8[1 + 6 * var72 + 4] = 0.2F;
      }

      Params var12 = new Params(var63);
      Params var13 = new Params(var64);
      Params var14 = new Params(var65);
      Params var15 = new Params(var66);
      Params var16 = new Params(var7);
      Params var17 = new Params(var8);
      ArrayList<Params> var18 = new ArrayList<>();
      var18.add(var12);
      var18.add(var13);
      var18.add(var14);
      var18.add(var15);
      var18.add(var16);
      var18.add(var17);

      // окна
      ArrayList<int[]> var19 = new ArrayList<>();

      for (int var20 = 0; var20 < var2.size(); var20++) {
         int var21 = var2.get(var20).y1.length;

         for (int var22 = 0; var22 + WINDOW <= var21; var22 += WINDOW) {
            var19.add(new int[]{var20, var22});
         }
      }

      if (var19.isEmpty()) {
         log("§c[Neuro] Слишком мало данных для окон по " + WINDOW + " тиков");
         return;
      }

      log(String.format(Locale.ROOT, "§7[Neuro] тиков: §f%d§7, окон: §f%d§7, эпох: §f%d", var3, var19.size(), epochs));
      Random var23 = new Random(1337L);
      int[] var24 = new int[var19.size()];

      for (int var25 = 0; var25 < var24.length; var25++) {
         var24[var25] = var25;
      }

      int var73 = 0;

      for (int var26 = 1; var26 <= epochs; var26++) {
         // shuffle
         for (int var27 = var24.length - 1; var27 > 0; var27--) {
            int var28 = var23.nextInt(var27 + 1);
            int var29 = var24[var27];
            var24[var27] = var24[var28];
            var24[var28] = var29;
         }

         var73++;
         int var74 = Math.min(var19.size(), MAX_WINDOWS_PER_EPOCH);
         double var75 = 0.0;
         int var76 = 0;
         int var77 = 0;

         for (int var30 = 0; var30 < var74; var30 += BATCH) {
            int var31 = Math.min(BATCH, var74 - var30);
            var12.zeroGrad();
            var13.zeroGrad();
            var14.zeroGrad();
            var15.zeroGrad();
            var16.zeroGrad();
            var17.zeroGrad();

            for (int var32 = 0; var32 < var31; var32++) {
               int[] var33 = var19.get(var24[var30 + var32]);
               var75 += backward(var2.get(var33[0]), var33[1], var12, var13, var14, var15, var16, var17);
               var76++;
            }

            double var79 = 0.0;

            for (Params var34 : var18) {
               var79 += var34.norm2();
            }

            double var35 = Math.sqrt(var79);
            double var37 = var35 > CLIP ? CLIP / var35 : 1.0;
            var77++;

            for (Params var80 : var18) {
               var80.step(var37, var77, LR);
            }
         }

         stage = String.format(Locale.ROOT, "эпоха %d/%d, NLL %.4f", var26, epochs, var75 / Math.max(1, var76));
         if (var26 == 1 || var26 % 5 == 0 || var26 == epochs) {
            log(String.format(Locale.ROOT, "§7[Neuro] эпоха §f%d§7/§f%d§7: NLL §f%.4f", var26, epochs, var75 / Math.max(1, var76)));
         }
      }

      stage = "экспорт модели";
      Path var81 = NeuroModels.file();
      Files.createDirectories(var81.getParent());
      float[] var82 = errorCurve(var2);
      writeModel(var81, var12.data, var13.data, var14.data, var15.data, var16.data, var17.data, var82, feat);
      NeuroModels.reload();
      log("§a[Neuro] Модель обучена и загружена: §f" + var81 + " §7(" + Files.size(var81) + " байт)");
      log("§7Включи ауру и выбери ротацию §fНейро§7.");
   }

   /** Один шаг BPTT по окну: forward с сохранением состояния, backward, накопление градиентов. */
   private static double backward(Series series, int start, Params wi, Params wh, Params bi, Params bh, Params outW, Params outB) {
      int feat = series.features;
      float[][] xs = new float[WINDOW][feat];
      float[][] hs = new float[WINDOW + 1][HIDDEN];
      float[][] preIs = new float[WINDOW][GATES];
      float[][] preHs = new float[WINDOW][GATES];
      float[][] rs = new float[WINDOW][HIDDEN];
      float[][] zs = new float[WINDOW][HIDDEN];
      float[][] ns = new float[WINDOW][HIDDEN];
      float[][] outs = new float[WINDOW][OUT];

      // forward
      for (int var8 = 0; var8 < WINDOW; var8++) {
         float[] var9 = series.x[start + var8];
         xs[var8] = var9;
         float[] var10 = hs[var8];

         for (int var11 = 0; var11 < GATES; var11++) {
            float var12 = bi.data[var11];
            float var13 = bh.data[var11];
            int var14 = var11 * feat;

            for (int var15 = 0; var15 < feat; var15++) {
               var12 += wi.data[var14 + var15] * var9[var15];
            }

            int var16 = var11 * HIDDEN;

            for (int var17 = 0; var17 < HIDDEN; var17++) {
               var13 += wh.data[var16 + var17] * var10[var17];
            }

            preIs[var8][var11] = var12;
            preHs[var8][var11] = var13;
         }

         float[] var18 = hs[var8 + 1];

         for (int var19 = 0; var19 < HIDDEN; var19++) {
            float var20 = sigmoid(preIs[var8][var19] + preHs[var8][var19]);
            float var21 = sigmoid(preIs[var8][HIDDEN + var19] + preHs[var8][HIDDEN + var19]);
            float var22 = (float) Math.tanh(preIs[var8][2 * HIDDEN + var19] + var20 * preHs[var8][2 * HIDDEN + var19]);
            rs[var8][var19] = var20;
            zs[var8][var19] = var21;
            ns[var8][var19] = var22;
            var18[var19] = (1.0F - var21) * var22 + var21 * var10[var19];
         }

         for (int var74 = 0; var74 < OUT; var74++) {
            float var23 = outB.data[var74];
            int var24 = var74 * HIDDEN;

            for (int var25 = 0; var25 < HIDDEN; var25++) {
               var23 += outW.data[var24 + var25] * var18[var25];
            }

            outs[var8][var74] = var23;
         }
      }

      // backward
      double var77 = 0.0;
      float[] var79 = new float[HIDDEN];

      for (int var26 = WINDOW - 1; var26 >= 0; var26--) {
         float[] var27 = outs[var26];
         float[] var28 = new float[OUT];
         var77 += mdnGrad(var27, series.y1[start + var26], series.y2[start + var26], series.freeze[start + var26], var28);
         float[] var29 = hs[var26 + 1];
         float[] var30 = new float[HIDDEN];

         for (int var31 = 0; var31 < OUT; var31++) {
            float var32 = var28[var31];
            outB.grad[var31] += var32;
            int var33 = var31 * HIDDEN;

            for (int var34 = 0; var34 < HIDDEN; var34++) {
               outW.grad[var33 + var34] += var32 * var29[var34];
               var30[var34] += var32 * outW.data[var33 + var34];
            }
         }

         for (int var76 = 0; var76 < HIDDEN; var76++) {
            var30[var76] += var79[var76];
         }

         float[] var82 = new float[GATES];
         float[] var83 = new float[GATES];
         float[] var84 = new float[HIDDEN];
         float[] var35 = hs[var26];
         float[] var36 = preHs[var26];

         for (int var37 = 0; var37 < HIDDEN; var37++) {
            float var38 = var30[var37];
            float var39 = zs[var26][var37];
            float var40 = rs[var26][var37];
            float var41 = ns[var26][var37];
            float var42 = var38 * (1.0F - var39);
            float var43 = var38 * (var35[var37] - var41);
            float var44 = var42 * (1.0F - var41 * var41);
            var82[2 * HIDDEN + var37] = var44;
            var83[2 * HIDDEN + var37] = var44 * var40;
            float var45 = var44 * var36[2 * HIDDEN + var37];
            float var46 = var43 * var39 * (1.0F - var39);
            var82[HIDDEN + var37] = var46;
            var83[HIDDEN + var37] = var46;
            float var47 = var45 * var40 * (1.0F - var40);
            var82[var37] = var47;
            var83[var37] = var47;
            var84[var37] = var38 * var39;
         }

         float[] var85 = xs[var26];

         for (int var87 = 0; var87 < GATES; var87++) {
            float var88 = var82[var87];
            float var89 = var83[var87];
            bi.grad[var87] += var88;
            bh.grad[var87] += var89;
            int var90 = var87 * feat;

            for (int var91 = 0; var91 < feat; var91++) {
               wi.grad[var90 + var91] += var88 * var85[var91];
            }

            int var92 = var87 * HIDDEN;

            for (int var93 = 0; var93 < HIDDEN; var93++) {
               wh.grad[var92 + var93] += var89 * var35[var93];
               var84[var93] += var89 * wh.data[var92 + var93];
            }
         }

         var79 = var84;
      }

      return var77 / WINDOW;
   }

   /**
    * NLL 2D-гауссовой смеси (с корреляцией) + BCE freeze-головы.
    * Заполняет dout[j] = dL/do[j].
    */
   private static double mdnGrad(float[] o, float y1, float y2, float freeze, float[] dout) {
      double var5 = 0.0;
      float var7 = sigmoid(o[0]);
      var5 += FREEZE_WEIGHT * (-(freeze * Math.log(var7 + 1.0E-7F) + (1.0F - freeze) * Math.log(1.0F - var7 + 1.0E-7F)));
      dout[0] = FREEZE_WEIGHT * (var7 - freeze);
      float[] var8 = new float[MIX];
      float[] var9 = new float[MIX];
      float[] var10 = new float[MIX];
      float[] var11 = new float[MIX];
      float[] var12 = new float[MIX];
      float[] var13 = new float[MIX];
      float[] var14 = new float[MIX];
      float[] var15 = new float[MIX];
      float[] var16 = new float[MIX];
      float var17 = o[1];
      for (int var18 = 1; var18 < MIX; var18++) {
         var17 = Math.max(var17, o[1 + 6 * var18]);
      }

      float var76 = 0.0F;

      for (int var19 = 0; var19 < MIX; var19++) {
         var76 += (float) Math.exp(o[1 + 6 * var19] - var17);
      }

      for (int var77 = 0; var77 < MIX; var77++) {
         var8[var77] = (float) Math.exp(o[1 + 6 * var77] - var17) / var76;
      }

      float var78 = -1.0E30F;

      for (int var20 = 0; var20 < MIX; var20++) {
         int var21 = 1 + 6 * var20;
         float var22 = Math.max(MEAN_MIN, Math.min(MEAN_MAX, o[var21 + 1]));
         float var23 = Math.max(MEAN_MIN, Math.min(MEAN_MAX, o[var21 + 2]));
         float var24 = Math.max(LOGS_MIN, Math.min(LOGS_MAX, o[var21 + 3]));
         float var25 = Math.max(LOGS_MIN, Math.min(LOGS_MAX, o[var21 + 4]));
         float var26 = (float) Math.tanh(o[var21 + 5]) * RHO_CLAMP;
         float var27 = (float) Math.exp(var24);
         float var28 = (float) Math.exp(var25);
         float var29 = (y1 - var22) / var27;
         float var30 = (y2 - var23) / var28;
         float var31 = Math.max(1.0E-6F, 1.0F - var26 * var26);
         float var32 = var29 * var29 - 2.0F * var26 * var29 * var30 + var30 * var30;
         var9[var20] = var29;
         var10[var20] = var30;
         var11[var20] = var22;
         var12[var20] = var23;
         var13[var20] = var24;
         var14[var20] = var25;
         var15[var20] = var26;
         var16[var20] = var32;
         float var33 = -var24 - var25 - 0.5F * (float) Math.log(var31) - 0.5F * var32 / var31;
         var78 = Math.max(var78, o[var21] + var33);
      }

      float var79 = 0.0F;

      for (int var34 = 0; var34 < MIX; var34++) {
         int var35 = 1 + 6 * var34;
         float var36 = var9[var34];
         float var37 = var10[var34];
         float var38 = var15[var34];
         float var39 = Math.max(1.0E-6F, 1.0F - var38 * var38);
         float var40 = -var13[var34] - var14[var34] - 0.5F * (float) Math.log(var39) - 0.5F * var16[var34] / var39;
         var79 += (float) Math.exp(o[var35] + var40 - var78);
      }

      var5 += -((float) Math.log(var79) + var78);

      for (int var41 = 0; var41 < MIX; var41++) {
         int var42 = 1 + 6 * var41;
         float var43 = var9[var41];
         float var44 = var10[var41];
         float var45 = var15[var41];
         float var46 = Math.max(1.0E-6F, 1.0F - var45 * var45);
         float var47 = -var13[var41] - var14[var41] - 0.5F * (float) Math.log(var46) - 0.5F * var16[var41] / var46;
         // ответственность компоненты: softmax по (logit + log N(x)) — именно она, а не
         // ненормированный exp, входит во все производные NLL
         float var48 = (float) Math.exp(o[var42] + var47 - var78) / var79;
         dout[var42] += -var48;
         float var50 = -var48;
         float var51 = (var43 - var45 * var44) / (var46 * (float) Math.exp(var13[var41]));
         dout[var42 + 1] += var50 * var51 * clampGate(o[var42 + 1], MEAN_MIN, MEAN_MAX);
         float var52 = (var44 - var45 * var43) / (var46 * (float) Math.exp(var14[var41]));
         dout[var42 + 2] += var50 * var52 * clampGate(o[var42 + 2], MEAN_MIN, MEAN_MAX);
         float var53 = -1.0F + var43 * (var43 - var45 * var44) / var46;
         dout[var42 + 3] += var50 * var53 * clampGate(o[var42 + 3], LOGS_MIN, LOGS_MAX);
         float var54 = -1.0F + var44 * (var44 - var45 * var43) / var46;
         dout[var42 + 4] += var50 * var54 * clampGate(o[var42 + 4], LOGS_MIN, LOGS_MAX);
         float var55 = ((var45 * var46 - var45 * var16[var41] + var43 * var44 * var46) / (var46 * var46)) * RHO_CLAMP;
         float var56 = (float) Math.tanh(o[var42 + 5]);
         dout[var42 + 5] += var50 * var55 * (1.0F - var56 * var56);
      }

      return var5;
   }

   private static float clampGate(float value, float min, float max) {
      return value <= min || value >= max ? 0.0F : 1.0F;
   }

   private static float sigmoid(float x) {
      return 1.0F / (1.0F + (float) Math.exp(-x));
   }

   // ======================= экспорт =======================

   private static float[] errorCurve(List<Series> series) {
      float[] var1 = new float[101];
      int[] var2 = new int[101];
      double[] var3 = new double[101];

      for (Series var5 : series) {
         for (int var6 = 0; var6 < var5.y1.length; var6++) {
            double var7 = Math.sinh(3.0 * var5.x[var6][0]);
            double var9 = Math.sinh(3.0 * var5.x[var6][1]);
            double var11 = Math.hypot(var7 / 20.0, var9 / 20.0);
            int var13 = (int) Math.min(100.0, Math.max(0.0, var11) * 100.0);
            double var14 = Math.hypot(
               Math.sinh(var5.y1[var6]), Math.sinh(var5.y2[var6])
            );
            var3[var13] += var14;
            var2[var13]++;
         }
      }

      for (int var15 = 0; var15 < 101; var15++) {
         var1[var15] = var2[var15] > 0 ? (float) (var3[var15] / var2[var15]) : Float.NaN;
      }

      // заполняем дырки линейно
      int var16 = -1;

      for (int var17 = 0; var17 < 101; var17++) {
         if (!Float.isNaN(var1[var17])) {
            if (var16 < 0) {
               for (int var18 = 0; var18 < var17; var18++) {
                  var1[var18] = var1[var17];
               }
            } else if (var17 - var16 > 1) {
               for (int var19 = var16 + 1; var19 < var17; var19++) {
                  float var20 = (float) (var19 - var16) / (var17 - var16);
                  var1[var19] = var1[var16] + (var1[var17] - var1[var16]) * var20;
               }
            }

            var16 = var17;
         }
      }

      if (var16 < 0) {
         for (int var21 = 0; var21 < 101; var21++) {
            var1[var21] = 1.0F;
         }
      } else {
         for (int var22 = var16 + 1; var22 < 101; var22++) {
            var1[var22] = var1[var16];
         }
      }

      float var23 = 0.05F;

      for (int var24 = 0; var24 < 101; var24++) {
         var23 = Math.max(var23, var1[var24]);
         var1[var24] = Math.min(90.0F, var23);
      }

      return var1;
   }

   private static void writeModel(
      Path path, float[] wi, float[] wh, float[] bi, float[] bh, float[] outW, float[] outB, float[] curve, int feat
   ) throws Exception {
      Files.createDirectories(path.getParent());

      try (OutputStream var9 = Files.newOutputStream(path); DataOutputStream var10 = new DataOutputStream(var9)) {
         var10.writeInt(HIDDEN);
         var10.writeInt(MIX);
         var10.writeInt(FREEZE_CUT);
         var10.writeFloat(YAW_LIMIT);
         var10.writeFloat(PITCH_LIMIT);
         var10.writeInt(curve.length);

         for (float var26 : curve) {
            var10.writeFloat(var26);
         }

         var10.writeInt(GATES);
         var10.writeInt(feat);
         for (float var29 : wi) {
            var10.writeFloat(var29);
         }

         var10.writeInt(GATES);
         var10.writeInt(HIDDEN);
         for (float var32 : wh) {
            var10.writeFloat(var32);
         }

         var10.writeInt(GATES);
         for (float var35 : bi) {
            var10.writeFloat(var35);
         }

         var10.writeInt(GATES);
         for (float var38 : bh) {
            var10.writeFloat(var38);
         }

         var10.writeInt(OUT);
         var10.writeInt(HIDDEN);
         for (float var41 : outW) {
            var10.writeFloat(var41);
         }

         var10.writeInt(OUT);
         for (float var44 : outB) {
            var10.writeFloat(var44);
         }
      }
   }

   private static void log(String message) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      var1.execute(() -> CommandManager.d(message));
   }

   /** Параметры + Adam-состояние. */
   private static final class Params {
      final float[] data;
      final float[] grad;
      final float[] m;
      final float[] v;

      Params(float[] data) {
         this.data = data;
         this.grad = new float[data.length];
         this.m = new float[data.length];
         this.v = new float[data.length];
      }

      void zeroGrad() {
         java.util.Arrays.fill(this.grad, 0.0F);
      }

      double norm2() {
         double var1 = 0.0;

         for (float var4 : this.grad) {
            var1 += var4 * var4;
         }

         return var1;
      }

      void step(double clip, int step, double lr) {
         double var5 = 0.9;
         double var7 = 0.999;
         double var9 = 1.0E-8;
         double var11 = 1.0 - Math.pow(var5, step);
         double var13 = 1.0 - Math.pow(var7, step);

         for (int var15 = 0; var15 < this.data.length; var15++) {
            float var16 = (float) (this.grad[var15] * clip);
            this.m[var15] = (float) (var5 * this.m[var15] + (1.0 - var5) * var16);
            this.v[var15] = (float) (var7 * this.v[var15] + (1.0 - var7) * var16 * var16);
            double var17 = this.m[var15] / var11;
            double var19 = this.v[var15] / var13;
            this.data[var15] = (float) (this.data[var15] - lr * var17 / (Math.sqrt(var19) + var9));
         }
      }
   }
}
