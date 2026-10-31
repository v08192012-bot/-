package brokke.cc.rotation.spooky;

import java.util.Random;

/**
 * Ротация режима "SpokyTime" (в моде: AuraModule -> BcCameraTestnerv.method1851 -> Motor.method3659).
 *
 * Что делает:
 *  1. считает, куда надо смотреть (центр хитбокса цели) и насколько цель смещена
 *     относительно текущего прицела, вместе с границами хитбокса в углах;
 *  2. собирает 47 признаков (состояние прицела, скорость/ускорение цели, свои прошлые
 *     движения мыши, ввод, состояние игрока, кулдаун атаки);
 *  3. прогоняет их через GRU-политику и сэмплирует команду в градусах;
 *  4. ограничивает амплитуду огибающей, квантует в целые "пиксели" мыши с остатком
 *     и возвращает новую ротацию.
 *
 * Модель обучалась на записях живой мыши, поэтому это не "снап в цель", а имитация
 * человеческого доведения прицела: доводит быстро, но с перелётами, микродрожью и паузами.
 *
 * Использование (раз в тик, пока цель валидна):
 * <pre>
 *   AimRotation next = rot.tick(currentRotation, target, playerState, tickId);
 * </pre>
 * Применение — как в моде: ротация "тихая", то есть подменяет yaw/pitch в пакетах движения,
 * а не крутит камеру игроку (см. README).
 */
public final class SpookyRotation {

    /** Что нужно знать о цели. Хитбокс — обычный AABB в мировых координатах. */
    public static final class Target {
        public final int id;
        public final boolean alive;
        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;

        public Target(int id, boolean alive,
                      double minX, double minY, double minZ,
                      double maxX, double maxY, double maxZ) {
            this.id = id;
            this.alive = alive;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public double centerX() {
            return (minX + maxX) * 0.5;
        }

        public double centerY() {
            return (minY + maxY) * 0.5;
        }

        public double centerZ() {
            return (minZ + maxZ) * 0.5;
        }
    }

    /** Состояние локального игрока, которое попадает в признаки. */
    public static final class PlayerState {
        public final double eyeX;
        public final double eyeY;
        public final double eyeZ;
        public final boolean forward;
        public final boolean back;
        public final boolean left;
        public final boolean right;
        public final boolean jump;
        public final boolean onGround;
        public final boolean sprinting;
        public final boolean sneaking;
        /** 0..1, как PlayerEntity#getAttackCooldownProgress(0). */
        public final double attackCooldown;
        public final int ticksSinceLastAttack;

        public PlayerState(double eyeX, double eyeY, double eyeZ,
                           boolean forward, boolean back, boolean left, boolean right, boolean jump,
                           boolean onGround, boolean sprinting, boolean sneaking,
                           double attackCooldown, int ticksSinceLastAttack) {
            this.eyeX = eyeX;
            this.eyeY = eyeY;
            this.eyeZ = eyeZ;
            this.forward = forward;
            this.back = back;
            this.left = left;
            this.right = right;
            this.jump = jump;
            this.onGround = onGround;
            this.sprinting = sprinting;
            this.sneaking = sneaking;
            this.attackCooldown = attackCooldown;
            this.ticksSinceLastAttack = ticksSinceLastAttack;
        }
    }

    private static final int HISTORY = 8;
    private static final int MIN_INT = Integer.MIN_VALUE;

    private final SpookyModel model;
    private final Random random;
    private double pixelStep;
    /** Пользовательский множитель команды (1.0 = ровно поведение модели). */
    private double commandScale = 1.0;

    /** Снимок тика: текущая ротация, углы на цель, границы хитбокса и дистанция. */
    private final class Entry {
        double selfYaw;
        double selfPitch;
        double targetYaw;
        double targetPitch;
        double minYawOffset;
        double maxYawOffset;
        double minPitchOffset;
        double maxPitchOffset;
        double distance;
    }

    private final Entry[] history = new Entry[HISTORY];
    private int historySize;
    private final double[] last1 = new double[2];
    private final double[] last2 = new double[2];
    private final double[] peak = new double[2];
    private final double[] zeroRun = new double[2];
    private final double[] flipRun = new double[2];
    private final double[] lastBigSign = new double[2];
    private final double[] residual = new double[2];

    private double lastCooldown = 1.0;
    private int lastTickId = MIN_INT;
    private int lastTargetId = MIN_INT;
    private AimRotation lastResult;
    private boolean didAttackPreviousTick;

    public SpookyRotation(SpookyModel model, double pixelStep, Random random) {
        this.model = model;
        this.pixelStep = pixelStep;
        this.random = random;
        reset();
    }

    /** Полный сброс состояния (сменился тик-разрыв, цель умерла, вышли из игры). */
    public void reset() {
        model.reset();
        java.util.Arrays.fill(history, null);
        historySize = 0;
        java.util.Arrays.fill(last1, 0.0);
        java.util.Arrays.fill(last2, 0.0);
        java.util.Arrays.fill(peak, 0.0);
        java.util.Arrays.fill(zeroRun, 0.0);
        java.util.Arrays.fill(flipRun, 0.0);
        java.util.Arrays.fill(lastBigSign, 0.0);
        java.util.Arrays.fill(residual, 0.0);
        lastCooldown = 1.0;
        lastTickId = MIN_INT;
        lastTargetId = MIN_INT;
        lastResult = null;
        didAttackPreviousTick = false;
    }

    public SpookyModel model() {
        return model;
    }

    /** Шаг «пикселя» мыши; меняется вместе с настройкой чувствительности в игре. */
    public void setPixelStep(double pixelStep) {
        if (Double.isFinite(pixelStep) && pixelStep > 0.0) {
            this.pixelStep = pixelStep;
        }
    }

    /** Множитель амплитуды команды: применяется до огибающей, поэтому лимиты те же. */
    public void setCommandScale(double commandScale) {
        this.commandScale = Double.isFinite(commandScale) && commandScale > 0.0 ? commandScale : 1.0;
    }

    /**
     * Один тик ротации.
     *
     * @param current текущая (тихая) ротация; если её нет — реальные yaw/pitch игрока
     * @param target  цель или null
     * @param player  состояние игрока
     * @param tickId  монотонный номер тика (в MC — player.age)
     * @return новая ротация; если модель не готова/цель пропала — вернётся {@code current}
     */
    public AimRotation tick(AimRotation current, Target target, PlayerState player, int tickId) {
        if (target == null || !target.alive) {
            reset();
            return current;
        }
        if (tickId == lastTickId && lastResult != null) {
            return lastResult;
        }
        if (lastTickId != MIN_INT && tickId != lastTickId + 1) {
            reset();
            lastTargetId = target.id;
        } else if (target.id != lastTargetId) {
            lastTargetId = target.id;
            lastResult = null;
        }
        lastTickId = tickId;

        didAttackPreviousTick = player.attackCooldown < lastCooldown - 1.0E-4;
        lastCooldown = player.attackCooldown;

        double step = pixelStep;
        if (!Double.isFinite(step) || step <= 0.0) {
            remember(tickId, current);
            return current;
        }

        Entry entry = sample(current, target, player);
        if (entry == null) {
            remember(tickId, current);
            return current;
        }
        push(entry);

        double[] features = features(entry, current, player);
        if (!allFinite(features)) {
            remember(tickId, current);
            return current;
        }

        SpookyModel.Step out = model.step(features);
        double[] command = sampleCommand(out);
        if (commandScale != 1.0) {
            command[0] *= commandScale;
            command[1] *= commandScale;
        }

        if (model.hasEnvelope()) {
            for (int axis = 0; axis < 2; axis++) {
                double counts = Math.max(Math.abs(last1[axis]), Math.abs(last2[axis])) / step;
                double cap = model.envelopeCap(axis, counts) * step;
                command[axis] = SpookyMath.clamp(command[axis], -cap, cap);
            }
        }

        int yawPixels = quantize(command[0], 0, step);
        int pitchPixels = quantize(command[1], 1, step);

        AimRotation next = current.addDegrees(yawPixels * step, pitchPixels * step);
        double dYaw = SpookyMath.wrapDegrees((double) next.yaw - (double) current.yaw);
        double dPitch = (double) next.pitch - (double) current.pitch;
        updateStats(dYaw, dPitch);

        if (yawPixels == 0 && pitchPixels == 0) {
            remember(tickId, current);
            return current;
        }
        remember(tickId, next);
        return next;
    }

    private void remember(int tickId, AimRotation rotation) {
        lastTickId = tickId;
        lastResult = rotation;
    }

    /** Углы на цель + разброс по 8 углам хитбокса. */
    private Entry sample(AimRotation current, Target target, PlayerState player) {
        double dx = target.centerX() - player.eyeX;
        double dy = target.centerY() - player.eyeY;
        double dz = target.centerZ() - player.eyeZ;
        if (dx * dx + dy * dy + dz * dz < 1.0E-10) {
            return null;
        }
        Entry e = new Entry();
        e.selfYaw = current.yaw;
        e.selfPitch = current.pitch;
        double[] look = SpookyMath.lookAngles(dx, dy, dz);
        e.targetYaw = look[0];
        e.targetPitch = look[1];
        e.distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double minYaw = Double.MAX_VALUE;
        double maxYaw = -Double.MAX_VALUE;
        double minPitch = Double.MAX_VALUE;
        double maxPitch = -Double.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            double cx = ((corner & 1) == 0 ? target.minX : target.maxX) - player.eyeX;
            double cy = ((corner & 2) == 0 ? target.minY : target.maxY) - player.eyeY;
            double cz = ((corner & 4) == 0 ? target.minZ : target.maxZ) - player.eyeZ;
            double[] cornerLook = SpookyMath.lookAngles(cx, cy, cz);
            if (Double.isFinite(cornerLook[0]) && Double.isFinite(cornerLook[1])) {
                double dYaw = SpookyMath.wrapDegrees(cornerLook[0] - e.targetYaw);
                double dPitch = cornerLook[1] - e.targetPitch;
                minYaw = Math.min(minYaw, dYaw);
                maxYaw = Math.max(maxYaw, dYaw);
                minPitch = Math.min(minPitch, dPitch);
                maxPitch = Math.max(maxPitch, dPitch);
            }
        }
        if (minYaw == Double.MAX_VALUE) {
            return null;
        }
        e.minYawOffset = minYaw;
        e.maxYawOffset = maxYaw;
        e.minPitchOffset = minPitch;
        e.maxPitchOffset = maxPitch;
        return e;
    }

    private void push(Entry entry) {
        System.arraycopy(history, 0, history, 1, HISTORY - 1);
        history[0] = entry;
        if (historySize < HISTORY) {
            historySize++;
        }
    }

    private Entry at(int index) {
        return history[Math.min(index, historySize - 1)];
    }

    /** 47 признаков ровно в том порядке, в котором их ждёт модель. */
    private double[] features(Entry e, AimRotation current, PlayerState player) {
        int lag = model.latency;
        Entry a = at(lag);
        Entry b = at(lag + 1);
        Entry c = at(lag + 2);
        double s = model.asinhScale;

        // отклонение прицела от цели прямо сейчас (e_now_*)
        double nowYaw = SpookyMath.wrapDegrees(e.targetYaw - current.yaw);
        double nowPitch = e.targetPitch - current.pitch;
        // то же, но "глазами" снимка (e_lag_*)
        double lagYaw = SpookyMath.wrapDegrees(e.targetYaw - e.selfYaw);
        double lagPitch = e.targetPitch - e.selfPitch;

        // движение цели: скорость (tgt_vel_*) и ускорение (tgt_acc_*) между снимками
        double velYaw = SpookyMath.wrapDegrees(a.targetYaw - b.targetYaw);
        double velPitch = a.targetPitch - b.targetPitch;
        double accYaw = velYaw - SpookyMath.wrapDegrees(b.targetYaw - c.targetYaw);
        double accPitch = velPitch - (b.targetPitch - c.targetPitch);

        // extrapolation: где цель будет через latency+1 тиков (e_ext_*)
        double extYaw = SpookyMath.wrapDegrees(a.targetYaw + (lag + 1) * velYaw - current.yaw);
        double extPitch = a.targetPitch + (lag + 1) * velPitch - current.pitch;

        double abs = Math.max(Math.hypot(nowYaw, nowPitch), 1.0E-6);

        // попадание внутрь хитбокса (u_*, soft_inside)
        double centerYaw = 0.5 * (a.minYawOffset + a.maxYawOffset);
        double centerPitch = 0.5 * (a.minPitchOffset + a.maxPitchOffset);
        double halfYaw = Math.max(0.5 * (a.maxYawOffset - a.minYawOffset), 0.001);
        double halfPitch = Math.max(0.5 * (a.maxPitchOffset - a.minPitchOffset), 0.001);
        double insideYaw = (-nowYaw - centerYaw) / halfYaw;
        double insidePitch = (-nowPitch - centerPitch) / halfPitch;
        double softInside = SpookyMath.sigmoid(model.insideSharp * (1.0 - Math.max(Math.abs(insideYaw), Math.abs(insidePitch))));

        double[] f = new double[47];
        int i = 0;
        f[i++] = SpookyMath.asinh(nowYaw / s);                       // 0  e_now_yaw
        f[i++] = SpookyMath.asinh(nowPitch / s);                     // 1  e_now_pitch
        f[i++] = SpookyMath.asinh(lagYaw / s);                       // 2  e_lag_yaw
        f[i++] = SpookyMath.asinh(lagPitch / s);                     // 3  e_lag_pitch
        f[i++] = SpookyMath.asinh(extYaw / s);                       // 4  e_ext_yaw
        f[i++] = SpookyMath.asinh(extPitch / s);                     // 5  e_ext_pitch
        f[i++] = SpookyMath.asinh(abs / s);                          // 6  e_abs
        f[i++] = nowYaw / abs;                                  // 7  e_dir_yaw
        f[i++] = nowPitch / abs;                                // 8  e_dir_pitch
        f[i++] = SpookyMath.asinh(velYaw / s);                       // 9  tgt_vel_yaw
        f[i++] = SpookyMath.asinh(velPitch / s);                     // 10 tgt_vel_pitch
        f[i++] = SpookyMath.asinh(accYaw / s);                       // 11 tgt_acc_yaw
        f[i++] = SpookyMath.asinh(accPitch / s);                     // 12 tgt_acc_pitch
        f[i++] = SpookyMath.clamp(insideYaw, -4.0, 4.0);             // 13 u_yaw
        f[i++] = SpookyMath.clamp(insidePitch, -4.0, 4.0);           // 14 u_pitch
        f[i++] = softInside;                                    // 15 soft_inside
        f[i++] = SpookyMath.asinh(halfYaw / s);                      // 16 box_half_yaw
        f[i++] = SpookyMath.asinh(halfPitch / s);                    // 17 box_half_pitch
        f[i++] = Math.log1p(e.distance);                        // 18 log_dist
        f[i++] = SpookyMath.clamp(e.distance - b.distance, -2.0, 2.0); // 19 d_dist
        f[i++] = current.pitch / 90.0;                          // 20 cam_pitch_norm
        f[i++] = (90.0 - Math.abs(current.pitch)) / 90.0;       // 21 cam_pitch_headroom
        f[i++] = SpookyMath.asinh(last1[0] / s);                     // 22 self_last1_yaw
        f[i++] = SpookyMath.asinh(last1[1] / s);                     // 23 self_last1_pitch
        f[i++] = SpookyMath.asinh(last2[0] / s);                     // 24 self_last2_yaw
        f[i++] = SpookyMath.asinh(last2[1] / s);                     // 25 self_last2_pitch
        f[i++] = SpookyMath.asinh((last1[0] - last2[0]) / s);        // 26 self_acc_yaw
        f[i++] = SpookyMath.asinh((last1[1] - last2[1]) / s);        // 27 self_acc_pitch
        f[i++] = Math.tanh(zeroRun[0] / 8.0);                   // 28 self_zero_run_yaw
        f[i++] = Math.tanh(zeroRun[1] / 8.0);                   // 29 self_zero_run_pitch
        f[i++] = Math.tanh(flipRun[0] / 8.0);                   // 30 self_flip_yaw
        f[i++] = Math.tanh(flipRun[1] / 8.0);                   // 31 self_flip_pitch
        f[i++] = SpookyMath.clamp(Math.abs(last1[0]) / Math.max(peak[0], 1.0E-6), 0.0, 4.0); // 32
        f[i++] = SpookyMath.clamp(Math.abs(last1[1]) / Math.max(peak[1], 1.0E-6), 0.0, 4.0); // 33
        f[i++] = SpookyMath.asinh(peak[0] / s);                      // 34 self_peak_yaw
        f[i++] = SpookyMath.asinh(peak[1] / s);                      // 35 self_peak_pitch
        f[i++] = player.forward ? 1.0 : 0.0;                    // 36
        f[i++] = player.back ? 1.0 : 0.0;                       // 37
        f[i++] = player.left ? 1.0 : 0.0;                       // 38
        f[i++] = player.right ? 1.0 : 0.0;                      // 39
        f[i++] = player.jump ? 1.0 : 0.0;                       // 40
        f[i++] = player.onGround ? 1.0 : 0.0;                   // 41
        f[i++] = player.sprinting ? 1.0 : 0.0;                  // 42
        f[i++] = player.sneaking ? 1.0 : 0.0;                   // 43
        f[i++] = player.attackCooldown;                         // 44
        f[i++] = SpookyMath.clamp(player.ticksSinceLastAttack, 0.0, 50.0); // 45
        f[i] = didAttackPreviousTick ? 1.0 : 0.0;               // 46
        return f;
    }

    /** Сэмплирование команды из головы смеси (пиксели -> градусы). */
    private double[] sampleCommand(SpookyModel.Step out) {
        if (random.nextDouble() < SpookyMath.sigmoid(out.pause)) {
            return new double[]{0.0, 0.0};
        }
        int component = pickComponent(out.pi);
        double g1 = random.nextGaussian();
        double g2 = random.nextGaussian();
        double sdYaw = Math.exp(out.logSigma[component][0]);
        double sdPitch = Math.exp(out.logSigma[component][1]);
        double rho = out.rho[component];

        double rawYaw = out.mu[component][0] + sdYaw * g1;
        double rawPitch = out.mu[component][1] + sdPitch * (rho * g1 + Math.sqrt(Math.max(1.0 - rho * rho, 1.0E-6)) * g2);

        rawYaw = SpookyMath.clamp(rawYaw, -model.maxCmdYawAsinh(), model.maxCmdYawAsinh());
        rawPitch = SpookyMath.clamp(rawPitch, -model.maxCmdPitchAsinh(), model.maxCmdPitchAsinh());

        double dYaw = SpookyMath.clamp(model.actScaleYaw * Math.sinh(rawYaw), -model.maxCmdYaw, model.maxCmdYaw);
        double dPitch = SpookyMath.clamp(model.actScalePitch * Math.sinh(rawPitch), -model.maxCmdPitch, model.maxCmdPitch);
        return new double[]{dYaw, dPitch};
    }

    private int pickComponent(double[] pi) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : pi) {
            max = Math.max(max, v);
        }
        double total = 0.0;
        double[] weights = new double[pi.length];
        for (int i = 0; i < pi.length; i++) {
            weights[i] = Math.exp(pi[i] - max);
            total += weights[i];
        }
        double target = random.nextDouble() * total;
        double acc = 0.0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (target <= acc) {
                return i;
            }
        }
        return weights.length - 1;
    }

    /** Квантование под шаг мыши с переносом остатка в следующий тик. */
    private int quantize(double delta, int axis, double step) {
        double acc = delta + (model.actResidual ? residual[axis] : 0.0);
        double q = acc / step;
        q += Math.signum(q) * 1.0E-4;
        long pixels = model.actQuantRound ? Math.round(q) : (long) q;
        double applied = pixels * step;
        residual[axis] = model.actResidual ? SpookyMath.clamp(acc - applied, -step, step) : 0.0;
        return (int) pixels;
    }

    private void updateStats(double dYaw, double dPitch) {
        double[] delta = {dYaw, dPitch};
        for (int axis = 0; axis < 2; axis++) {
            double d = delta[axis];
            boolean big = Math.abs(d) > model.zeroEps;
            double sign = Math.signum(d);
            boolean flip = big && sign * lastBigSign[axis] < 0.0;
            zeroRun[axis] = big ? 0.0 : zeroRun[axis] + 1.0;
            flipRun[axis] = flip ? 0.0 : flipRun[axis] + 1.0;
            peak[axis] = Math.max(peak[axis] * model.peakDecay, Math.abs(d));
            if (big) {
                lastBigSign[axis] = sign;
            }
            last2[axis] = last1[axis];
            last1[axis] = d;
        }
    }

    private static boolean allFinite(double[] values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return false;
            }
        }
        return true;
    }
}
