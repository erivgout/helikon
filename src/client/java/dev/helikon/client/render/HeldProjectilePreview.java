package dev.helikon.client.render;

import java.util.Objects;
import java.util.Optional;

/**
 * Minecraft-free launch prediction for a held, not-yet-fired projectile. Given the
 * player's aim and the held item family, it reproduces Minecraft's verified
 * {@code shootFromRotation} launch direction and per-family gravity/drag so the
 * predicted path matches what an actual throw or shot would follow.
 *
 * <p>The server remains authoritative: an actual fired projectile is still subject
 * to the server's own physics, entity collisions, and validation, so this preview
 * is an honest local estimate of the launch, not a promise of where the projectile
 * lands.
 */
public final class HeldProjectilePreview {
    /** Verified arrow/trident gravity and drag: drag applied after gravity, velocity after movement. */
    private static final TrajectorySimulator.Physics ARROW_PHYSICS = new TrajectorySimulator.Physics(
            0.05D, 0.99D, TrajectorySimulator.GravityOrder.AFTER_DRAG, TrajectorySimulator.UpdateTiming.AFTER_MOVE
    );
    /** Verified thrown-item (snowball/egg/ender pearl) gravity and drag. */
    private static final TrajectorySimulator.Physics THROWN_PHYSICS = new TrajectorySimulator.Physics(
            0.03D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG, TrajectorySimulator.UpdateTiming.BEFORE_MOVE
    );
    /** Verified splash-potion gravity and drag. */
    private static final TrajectorySimulator.Physics POTION_PHYSICS = new TrajectorySimulator.Physics(
            0.05D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG, TrajectorySimulator.UpdateTiming.BEFORE_MOVE
    );

    /** Vanilla loaded-crossbow arrow launch speed. */
    private static final double CROSSBOW_SPEED = 3.15D;
    /** Vanilla thrown-trident launch speed. */
    private static final double TRIDENT_SPEED = 2.5D;
    /** Vanilla thrown-item launch speed for snowball, egg, and ender pearl. */
    private static final double THROWN_SPEED = 1.5D;
    /** Vanilla splash-potion launch speed. */
    private static final double POTION_SPEED = 0.5D;
    /** Vanilla splash-potion upward pitch offset in degrees. */
    private static final double POTION_PITCH_OFFSET = -20.0D;
    /** A fully charged bow multiplies its power by this factor for arrow speed. */
    private static final double BOW_SPEED_PER_POWER = 3.0D;
    /** Vanilla minimum draw power below which releasing a bow fires nothing. */
    private static final double MINIMUM_BOW_POWER = 0.1D;

    private HeldProjectilePreview() {
    }

    /** The held item families this preview can predict. */
    public enum Kind {
        BOW,
        CROSSBOW,
        TRIDENT,
        THROWABLE,
        SPLASH_POTION
    }

    /** The predicted launch: matching projectile physics and the initial velocity vector. */
    public record Launch(TrajectorySimulator.Physics physics, TrajectoryVector velocity) {
        public Launch {
            Objects.requireNonNull(physics, "physics");
            Objects.requireNonNull(velocity, "velocity");
        }
    }

    /**
     * Computes the launch for the held family at the given aim.
     *
     * @param kind           the held projectile family
     * @param yawDegrees     the player's yaw
     * @param pitchDegrees   the player's pitch
     * @param bowDrawTicks   how many ticks a bow has been drawn; ignored unless {@code kind == BOW}
     * @return the predicted launch, or empty when a bow is not drawn far enough to fire
     */
    public static Optional<Launch> launch(Kind kind, double yawDegrees, double pitchDegrees, int bowDrawTicks) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case BOW -> {
                double power = bowPower(bowDrawTicks);
                if (power < MINIMUM_BOW_POWER) {
                    yield Optional.empty();
                }
                yield Optional.of(new Launch(ARROW_PHYSICS,
                        velocity(yawDegrees, pitchDegrees, 0.0D, power * BOW_SPEED_PER_POWER)));
            }
            case CROSSBOW -> Optional.of(new Launch(ARROW_PHYSICS,
                    velocity(yawDegrees, pitchDegrees, 0.0D, CROSSBOW_SPEED)));
            case TRIDENT -> Optional.of(new Launch(ARROW_PHYSICS,
                    velocity(yawDegrees, pitchDegrees, 0.0D, TRIDENT_SPEED)));
            case THROWABLE -> Optional.of(new Launch(THROWN_PHYSICS,
                    velocity(yawDegrees, pitchDegrees, 0.0D, THROWN_SPEED)));
            case SPLASH_POTION -> Optional.of(new Launch(POTION_PHYSICS,
                    velocity(yawDegrees, pitchDegrees, POTION_PITCH_OFFSET, POTION_SPEED)));
        };
    }

    /**
     * Reproduces Minecraft's {@code getPowerForTime}: the normalized [0, 1] draw power
     * for a bow held for {@code drawTicks} ticks.
     */
    public static double bowPower(int drawTicks) {
        double charge = Math.max(0, drawTicks) / 20.0D;
        double power = (charge * charge + charge * 2.0D) / 3.0D;
        return Math.min(power, 1.0D);
    }

    /**
     * Reproduces Minecraft's {@code shootFromRotation}/{@code shoot} launch direction:
     * a unit vector from yaw and pitch, with an optional pitch offset applied only to
     * the vertical component, scaled to {@code speed}.
     */
    public static TrajectoryVector velocity(double yawDegrees, double pitchDegrees, double pitchOffsetDegrees,
                                            double speed) {
        if (!Double.isFinite(yawDegrees) || !Double.isFinite(pitchDegrees) || !Double.isFinite(pitchOffsetDegrees)
                || !Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("Invalid launch parameters");
        }
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double offsetPitch = Math.toRadians(pitchDegrees + pitchOffsetDegrees);
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(offsetPitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0D) {
            throw new IllegalArgumentException("Degenerate launch direction");
        }
        return new TrajectoryVector(x / length * speed, y / length * speed, z / length * speed);
    }
}
