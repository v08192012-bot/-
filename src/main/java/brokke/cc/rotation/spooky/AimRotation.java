package brokke.cc.rotation.spooky;

/** Иммутабельная пара yaw/pitch в градусах. pitch всегда в [-90, 90]. */
public final class AimRotation {
    public final float yaw;
    public final float pitch;

    public AimRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = SpookyMath.clamp(pitch, -90.0F, 90.0F);
    }

    public AimRotation addDegrees(double dYaw, double dPitch) {
        return new AimRotation((float) (this.yaw + dYaw), (float) (this.pitch + dPitch));
    }

    /** Разница по yaw с учётом перехода через 180/-180. */
    public double deltaYawTo(AimRotation other) {
        return SpookyMath.wrapDegrees((double) other.yaw - (double) this.yaw);
    }

    public double deltaPitchTo(AimRotation other) {
        return (double) other.pitch - (double) this.pitch;
    }

    public double distanceTo(AimRotation other) {
        double dYaw = deltaYawTo(other);
        double dPitch = deltaPitchTo(other);
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }

    /** Округляет разницу до целого числа "пикселей" мыши (финальная квантование мода). */
    public AimRotation snapToPixelGrid(AimRotation target, double pixelStep) {
        if (pixelStep <= 0.0 || !Double.isFinite(pixelStep)) {
            return this;
        }
        double dYaw = SpookyMath.wrapDegrees((double) target.yaw - (double) this.yaw);
        double dPitch = (double) target.pitch - (double) this.pitch;
        return new AimRotation(
                (float) (this.yaw + Math.round(dYaw / pixelStep) * pixelStep),
                (float) (this.pitch + Math.round(dPitch / pixelStep) * pixelStep));
    }

    @Override
    public String toString() {
        return String.format("AimRotation[yaw=%.4f, pitch=%.4f]", yaw, pitch);
    }
}
