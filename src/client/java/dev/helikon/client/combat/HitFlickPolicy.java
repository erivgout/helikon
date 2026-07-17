package dev.helikon.client.combat;

/**
 * Bounded, Minecraft-free knockback-flick geometry. Given the yaw that points at a melee target, it
 * returns the yaw the client should be facing at attack time so the server steers the sprint and
 * knockback-enchantment bonus in the chosen direction. The server still computes and authorizes the
 * result; it may reject, correct, or rubber-band it.
 */
public final class HitFlickPolicy {
    /** Direction of the attack-time flick, relative to the direction toward the target. */
    public enum Mode {
        /** Face directly away from the target so the bonus knockback pulls it toward the attacker. */
        REVERSE,
        /** Flick left of the target by the configured side angle. */
        LEFT,
        /** Flick right of the target by the configured side angle. */
        RIGHT
    }

    private HitFlickPolicy() {
    }

    /**
     * Computes the flicked yaw to face at attack time.
     *
     * @param yawToTarget the yaw (degrees) pointing from the attacker toward the target
     * @param mode        flick direction
     * @param sideAngle   yaw offset in degrees applied for {@link Mode#LEFT} and {@link Mode#RIGHT}
     * @return the wrapped flicked yaw in [-180, 180)
     */
    public static float flickedYaw(float yawToTarget, Mode mode, double sideAngle) {
        if (!Float.isFinite(yawToTarget) || mode == null || !Double.isFinite(sideAngle)
                || sideAngle < 0.0D || sideAngle > 180.0D) {
            throw new IllegalArgumentException("hit flick facts are invalid");
        }
        float offset = switch (mode) {
            case REVERSE -> 180.0F;
            case LEFT -> (float) -sideAngle;
            case RIGHT -> (float) sideAngle;
        };
        return wrap(yawToTarget + offset);
    }

    private static float wrap(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            return wrapped - 360.0F;
        }
        return wrapped < -180.0F ? wrapped + 360.0F : wrapped;
    }
}
