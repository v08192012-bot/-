package brokke.cc.rotation.spooky;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Нейросетевая часть SpokyTime-ротации (в моде это класс Motor, формат neurotest-motor-v1).
 *
 * Схема: нормировка -> 2 линейных слоя с SiLU -> 2 слоя GRU (256) -> головы смеси
 * (pi, mu, log_sigma, rho, pause). На каждом тике модель получает 47 признаков
 * (см. {@link SpookyRotation}) и выдаёт параметры распределения действия.
 *
 * Состояние GRU внутреннее: один экземпляр = одна непрерывная последовательность.
 * {@link #reset()} обязателен при смене цели/разрыве тиков (см. {@link SpookyRotation}).
 */
public final class SpookyModel {
    private static final byte[] MAGIC = {'Z', 'E', 'N', 'M', 'O', 'T', 'O', 'R'};
    private static final int VERSION = 1;

    public final int nFeatures;
    public final int mlpHidden;
    public final int gruHidden;
    public final int gruLayers;
    public final int mix;
    public final int latency;
    public final double asinhScale;
    public final double actScaleYaw;
    public final double actScalePitch;
    public final double insideSharp;
    public final double logSigmaMin;
    public final double logSigmaMax;
    public final double maxCmdYaw;
    public final double maxCmdPitch;
    public final double peakDecay;
    public final double zeroEps;
    public final boolean actQuantRound;
    public final boolean actResidual;

    /** Огибающая амплитуды: границы (в "пикселях") и лимиты скорости для yaw/pitch. */
    public final double[] envelopeEdges;
    public final double[] envelopeCapsYaw;
    public final double[] envelopeCapsPitch;

    private final double[] normMean;
    private final double[] normVar;
    private final double[][] inpW;
    private final double[][] inpB;
    private final double[][] gruWih;
    private final double[][] gruBih;
    private final double[][] gruWhh;
    private final double[][] gruBhh;
    private final double[][] headW;      // pi, mu, log_sigma, rho, pause
    private final double[][] headB;
    private final double[][] goldenFeatures;
    private final double[][] goldenPi;
    private final double[][] goldenMu;
    private final double[][] goldenLogSigma;
    private final double[][] goldenRho;
    private final double[] goldenPause;
    private final double[][] goldenHidden;

    /** Ограничитель амплитуды команды в asinh-пространстве (как в моде). */
    private final double maxCmdYawAsinh;
    private final double maxCmdPitchAsinh;

    private double[][] hidden;
    private double[] scratch;

    private SpookyModel(ByteBuffer in) throws IOException {
        for (byte b : MAGIC) {
            if (in.get() != b) {
                throw new IOException("не motor.bin: неверная сигнатура (ожидался ZENMOTOR)");
            }
        }
        int version = in.getInt();
        if (version != VERSION) {
            throw new IOException("версия формата " + version + ", поддерживается " + VERSION);
        }
        this.nFeatures = in.getInt();
        this.mlpHidden = in.getInt();
        this.gruHidden = in.getInt();
        this.gruLayers = in.getInt();
        this.mix = in.getInt();
        this.latency = in.getInt();
        if (nFeatures != 47) {
            throw new IOException("модель ждёт " + nFeatures + " признаков, рантайм собирает 47");
        }
        this.asinhScale = in.getDouble();
        this.actScaleYaw = in.getDouble();
        this.actScalePitch = in.getDouble();
        this.insideSharp = in.getDouble();
        this.logSigmaMin = in.getDouble();
        this.logSigmaMax = in.getDouble();
        this.maxCmdYaw = in.getDouble();
        this.maxCmdPitch = in.getDouble();
        this.peakDecay = in.getDouble();
        this.zeroEps = in.getDouble();
        this.actQuantRound = (in.get() & 0xFF) != 0;
        this.actResidual = (in.get() & 0xFF) != 0;

        boolean hasEnvelope = (in.get() & 0xFF) != 0;
        if (hasEnvelope) {
            this.envelopeEdges = readDoubles(in, in.getInt());
            this.envelopeCapsYaw = readDoubles(in, in.getInt());
            this.envelopeCapsPitch = readDoubles(in, in.getInt());
        } else {
            this.envelopeEdges = null;
            this.envelopeCapsYaw = null;
            this.envelopeCapsPitch = null;
        }

        this.normMean = readDoubles(in, nFeatures);
        this.normVar = readDoubles(in, nFeatures);

        int inpCount = in.getInt();
        inpW = new double[inpCount][];
        inpB = new double[inpCount][];
        for (int i = 0; i < inpCount; i++) {
            int rows = in.getInt();
            int cols = in.getInt();
            if (cols != (i == 0 ? nFeatures : mlpHidden)) {
                throw new IOException("входной слой " + i + ": " + cols + " входов");
            }
            inpW[i] = readDoubles(in, rows * cols);
            inpB[i] = readDoubles(in, rows);
            if (rows != mlpHidden) {
                throw new IOException("входной слой " + i + ": " + rows + " выходов вместо " + mlpHidden);
            }
        }

        int gruCount = in.getInt();
        if (gruCount != gruLayers) {
            throw new IOException("слоёв GRU " + gruCount + ", в конфиге " + gruLayers);
        }
        gruWih = new double[gruCount][];
        gruBih = new double[gruCount][];
        gruWhh = new double[gruCount][];
        gruBhh = new double[gruCount][];
        for (int i = 0; i < gruCount; i++) {
            int hHidden = in.getInt();
            int inDim = in.getInt();
            if (hHidden != gruHidden || inDim != (i == 0 ? mlpHidden : gruHidden)) {
                throw new IOException("слой GRU " + i + ": " + hHidden + "/" + inDim);
            }
            gruWih[i] = readDoubles(in, 3 * gruHidden * inDim);
            gruBih[i] = readDoubles(in, 3 * gruHidden);
            gruWhh[i] = readDoubles(in, 3 * gruHidden * gruHidden);
            gruBhh[i] = readDoubles(in, 3 * gruHidden);
        }

        headW = new double[5][];
        headB = new double[5][];
        int[] expectRows = {mix, 2 * mix, 2 * mix, mix, 1};
        for (int i = 0; i < 5; i++) {
            int rows = in.getInt();
            int cols = in.getInt();
            if (cols != gruHidden || rows != expectRows[i]) {
                throw new IOException("голова " + i + ": " + rows + "x" + cols);
            }
            headW[i] = readDoubles(in, rows * cols);
            headB[i] = readDoubles(in, rows);
        }

        int goldenCount = in.getInt();
        goldenFeatures = new double[goldenCount][];
        goldenPi = new double[goldenCount][];
        goldenMu = new double[goldenCount][];
        goldenLogSigma = new double[goldenCount][];
        goldenRho = new double[goldenCount][];
        goldenPause = new double[goldenCount];
        goldenHidden = new double[goldenCount][];
        for (int i = 0; i < goldenCount; i++) {
            goldenFeatures[i] = readDoubles(in, nFeatures);
            goldenPi[i] = readDoubles(in, mix);
            goldenMu[i] = readDoubles(in, 2 * mix);
            goldenLogSigma[i] = readDoubles(in, 2 * mix);
            goldenRho[i] = readDoubles(in, mix);
            goldenPause[i] = in.getDouble();
            goldenHidden[i] = readDoubles(in, gruLayers * gruHidden);
        }

        this.maxCmdYawAsinh = SpookyMath.asinh(maxCmdYaw / actScaleYaw);
        this.maxCmdPitchAsinh = SpookyMath.asinh(maxCmdPitch / actScalePitch);
        reset();
    }

    public static SpookyModel load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    public static SpookyModel load(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            byte[] raw = in.readAllBytes();
            // В клиенте модель лежит сжатой (3.7 МБ вместо 7 МБ в jar): веса от этого
            // не меняются, sha256 распакованного файла тот же.
            if (raw.length > 2 && (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B) {
                try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(raw))) {
                    raw = gzip.readAllBytes();
                }
            }
            return new SpookyModel(ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    private static double[] readDoubles(ByteBuffer in, int count) {
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = in.getDouble();
        }
        return out;
    }

    /** Сброс внутреннего состояния GRU (новая цель, телепорт, пауза). */
    public void reset() {
        this.hidden = new double[gruLayers][gruHidden];
        this.scratch = null;
    }

    /** asinh-границы команды (используются при сэмплировании). */
    public double maxCmdYawAsinh() {
        return maxCmdYawAsinh;
    }

    public double maxCmdPitchAsinh() {
        return maxCmdPitchAsinh;
    }

    public boolean hasEnvelope() {
        return envelopeEdges != null && envelopeCapsYaw != null && envelopeCapsPitch != null;
    }

    /**
     * Лимит скорости для оси (0 = yaw, 1 = pitch) при текущей "скорости" в пикселях за тик.
     * Куски огибающей интерполируются ступенькой, как в моде.
     */
    public double envelopeCap(int axis, double countsPerTick) {
        double[] caps = axis == 0 ? envelopeCapsYaw : envelopeCapsPitch;
        int idx = 0;
        while (idx < envelopeEdges.length && envelopeEdges[idx] < countsPerTick) {
            idx++;
        }
        return caps[Math.min(idx, caps.length - 1)];
    }

    /** Результат одного тика: параметры смеси + скрытое состояние. */
    public static final class Step {
        public final double[] pi;
        public final double[][] mu;
        public final double[][] logSigma;
        public final double[] rho;
        public final double pause;
        final double[] hiddenOut;

        Step(double[] pi, double[][] mu, double[][] logSigma, double[] rho, double pause, double[] hiddenOut) {
            this.pi = pi;
            this.mu = mu;
            this.logSigma = logSigma;
            this.rho = rho;
            this.pause = pause;
            this.hiddenOut = hiddenOut;
        }
    }

    /** Один шаг сети. Состояние GRU обновляется. */
    public Step step(double[] features) {
        if (features.length != nFeatures) {
            throw new IllegalArgumentException("нужно " + nFeatures + " признаков, пришло " + features.length);
        }
        double[] x = new double[nFeatures];
        for (int i = 0; i < nFeatures; i++) {
            x[i] = (features[i] - normMean[i]) / Math.sqrt(normVar[i] + 1.0E-4);
        }
        for (int layer = 0; layer < inpW.length; layer++) {
            x = linear(inpW[layer], inpB[layer], x);
            for (int i = 0; i < x.length; i++) {
                x[i] = SpookyMath.silu(x[i]);
            }
        }
        for (int layer = 0; layer < gruLayers; layer++) {
            x = gruStep(layer, x);
        }
        Step step = new Step(
                linear(headW[0], headB[0], x),
                reshape(linear(headW[1], headB[1], x), 2),
                reshape(linear(headW[2], headB[2], x), 2),
                linear(headW[3], headB[3], x),
                linear(headW[4], headB[4], x)[0],
                flatten(hidden));
        for (int k = 0; k < mix; k++) {
            step.logSigma[k][0] = SpookyMath.clamp(step.logSigma[k][0], logSigmaMin, logSigmaMax);
            step.logSigma[k][1] = SpookyMath.clamp(step.logSigma[k][1], logSigmaMin, logSigmaMax);
            step.rho[k] = Math.tanh(step.rho[k]) * 0.95;
        }
        return step;
    }

    private double[] gruStep(int layer, double[] input) {
        double[] n = linear(gruWih[layer], gruBih[layer], input);
        double[] g = linear(gruWhh[layer], gruBhh[layer], hidden[layer]);
        double[] prev = hidden[layer];
        double[] next = new double[gruHidden];
        for (int j = 0; j < gruHidden; j++) {
            double r = SpookyMath.sigmoid(n[j] + g[j]);
            double z = SpookyMath.sigmoid(n[gruHidden + j] + g[gruHidden + j]);
            double cand = Math.tanh(n[2 * gruHidden + j] + r * g[2 * gruHidden + j]);
            next[j] = (1.0 - z) * cand + z * prev[j];
        }
        hidden[layer] = next;
        return next;
    }

    /** Плотный слой: w лежит построчно (rows x x.length), b длиной rows. */
    private static double[] linear(double[] w, double[] b, double[] x) {
        double[] out = new double[b.length];
        for (int i = 0; i < b.length; i++) {
            double acc = b[i];
            int offset = i * x.length;
            for (int j = 0; j < x.length; j++) {
                acc += w[offset + j] * x[j];
            }
            out[i] = acc;
        }
        return out;
    }

    private static double[][] reshape(double[] flat, int width) {
        int rows = flat.length / width;
        double[][] out = new double[rows][width];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(flat, r * width, out[r], 0, width);
        }
        return out;
    }

    private static double[] flatten(double[][] rows) {
        int per = rows[0].length;
        double[] out = new double[rows.length * per];
        for (int i = 0; i < rows.length; i++) {
            System.arraycopy(rows[i], 0, out, i * per, per);
        }
        return out;
    }

    // --- доступ к эталонным тикам для самопроверки ---

    public int goldenCount() {
        return goldenFeatures.length;
    }

    public double[] goldenFeatures(int i) {
        return goldenFeatures[i].clone();
    }

    public Step goldenExpected(int i) {
        return new Step(goldenPi[i], reshape(goldenMu[i], 2), reshape(goldenLogSigma[i], 2),
                goldenRho[i], goldenPause[i], goldenHidden[i]);
    }

    /** Максимальное расхождение forward-pass с эталоном (должно быть ~1e-15). */
    public double selfCheck() {
        reset();
        double worst = 0.0;
        for (int i = 0; i < goldenCount(); i++) {
            Step got = step(goldenFeatures(i));
            Step want = goldenExpected(i);
            worst = Math.max(worst, maxDelta(got.pi, want.pi));
            for (int k = 0; k < mix; k++) {
                worst = Math.max(worst, maxDelta(got.mu[k], want.mu[k]));
                worst = Math.max(worst, maxDelta(got.logSigma[k], want.logSigma[k]));
                worst = Math.max(worst, Math.abs(got.rho[k] - want.rho[k]));
            }
            worst = Math.max(worst, Math.abs(got.pause - want.pause));
            worst = Math.max(worst, maxDelta(got.hiddenOut, want.hiddenOut));
        }
        reset();
        return worst;
    }

    static double maxDelta(double[] a, double[] b) {
        double worst = 0.0;
        for (int i = 0; i < a.length; i++) {
            worst = Math.max(worst, Math.abs(a[i] - b[i]));
        }
        return worst;
    }
}
