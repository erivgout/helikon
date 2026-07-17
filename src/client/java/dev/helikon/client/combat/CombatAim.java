package dev.helikon.client.combat;

/** Bounded local yaw/pitch prediction and smoothing for normal held-bow aim assistance. */
public final class CombatAim {
    private CombatAim() {
    }

    public record Rotation(float yaw, float pitch) {
        public Rotation {
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
                throw new IllegalArgumentException("rotation is invalid");
            }
        }
    }

    public static Rotation predictedRotation(CombatTarget target, double projectileSpeed, double gravity, boolean predict) {
        if (target == null || !Double.isFinite(projectileSpeed) || projectileSpeed <= 0.0D
                || !Double.isFinite(gravity) || gravity < 0.0D) {
            throw new IllegalArgumentException("aim facts are invalid");
        }
        double travelTicks = predict ? target.distance() / projectileSpeed : 0.0D;
        double x = target.relativeX() + target.velocityX() * travelTicks;
        double z = target.relativeZ() + target.velocityZ() * travelTicks;
        double y = target.relativeY() + target.velocityY() * travelTicks + 0.5D * gravity * travelTicks * travelTicks;
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        return new Rotation(wrap(yaw), Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    public static Rotation limit(Rotation current, Rotation desired, double maximumChange) {
        if (current == null || desired == null || !Double.isFinite(maximumChange) || maximumChange <= 0.0D) {
            throw new IllegalArgumentException("aim smoothing facts are invalid");
        }
        float yaw = current.yaw() + clamp(wrap(desired.yaw() - current.yaw()), (float) maximumChange);
        float pitch = current.pitch() + clamp(desired.pitch() - current.pitch(), (float) maximumChange);
        return new Rotation(wrap(yaw), Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static float clamp(float delta, float maximum) {
        return Math.max(-maximum, Math.min(maximum, delta));
    }

    private static float wrap(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            return wrapped - 360.0F;
        }
        return wrapped < -180.0F ? wrapped + 360.0F : wrapped;
    }
}
