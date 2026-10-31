package brokke.cc.rotation.spooky;

/** Математика ровно в том виде, в каком она используется моделью. */
public final class SpookyMath {
    private SpookyMath() {
    }

    /** MathHelper.wrapDegrees: приводит угол в диапазон (-180, 180]. */
    public static double wrapDegrees(double deg) {
        double d = deg % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }

    public static float wrapDegrees(float deg) {
        return (float) wrapDegrees((double) deg);
    }

    public static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static double sigmoid(double v) {
        return 1.0 / (1.0 + Math.exp(-v));
    }

    /** SiLU (swish): x * sigmoid(x). Именно она стоит после входных слоёв модели. */
    public static double silu(double v) {
        return v / (1.0 + Math.exp(-v));
    }

    public static double asinh(double v) {
        return v >= 0.0 ? Math.log(v + Math.sqrt(v * v + 1.0)) : -Math.log(-v + Math.sqrt(v * v + 1.0));
    }

    /** yaw/pitch из вектора направления: X/Z -> yaw, Y/горизонталь -> pitch. */
    public static double[] lookAngles(double dx, double dy, double dz) {
        double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return new double[]{wrapDegrees(yaw), wrapDegrees(pitch)};
    }

    /** Шаг одного "пикселя" мыши в градусах: та же формула, что в Minecraft. */
    public static double pixelStep(double sensitivity) {
        double f = sensitivity * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }
}
