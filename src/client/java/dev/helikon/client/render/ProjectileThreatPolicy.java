package dev.helikon.client.render;

import java.util.Optional;

/**
 * Minecraft-free decision: is a projectile on an incoming collision course with
 * the local player? The assessment models the projectile as travelling in a
 * straight line at its current per-tick velocity relative to the player and
 * finds the linear closest-approach point. This intentionally ignores gravity
 * and drag, so it is a conservative short-horizon warning rather than a full
 * ballistic prediction (which {@code Trajectories} already provides).
 */
public final class ProjectileThreatPolicy {
    private static final double MINIMUM_SPEED_SQUARED = 1.0E-6D;

    private ProjectileThreatPolicy() {
    }

    /**
     * Assesses one projectile relative to the local player.
     *
     * @param relativeX      projectile position minus player center, x (blocks)
     * @param relativeY      projectile position minus player center, y (blocks)
     * @param relativeZ      projectile position minus player center, z (blocks)
     * @param velocityX      projectile velocity minus player velocity, x (blocks per tick)
     * @param velocityY      projectile velocity minus player velocity, y (blocks per tick)
     * @param velocityZ      projectile velocity minus player velocity, z (blocks per tick)
     * @param hitRadius      blocks within which a closest approach counts as a threat
     * @param maximumTicks   only warn when the closest approach is at most this many ticks away
     * @param detectionRange only consider projectiles currently within this distance
     * @return a populated threat when the projectile is closing and will pass within the hit radius
     */
    public static Optional<ProjectileThreat> assess(
            double relativeX, double relativeY, double relativeZ,
            double velocityX, double velocityY, double velocityZ,
            double hitRadius, double maximumTicks, double detectionRange) {
        requireFinite(relativeX, relativeY, relativeZ, velocityX, velocityY, velocityZ);
        requirePositive(hitRadius, "hitRadius");
        requirePositive(maximumTicks, "maximumTicks");
        requirePositive(detectionRange, "detectionRange");

        double distanceSquared = relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ;
        if (distanceSquared > detectionRange * detectionRange) {
            return Optional.empty();
        }
        double speedSquared = velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ;
        if (speedSquared < MINIMUM_SPEED_SQUARED) {
            // A stuck or effectively stationary projectile (for example a landed arrow) is not a threat.
            return Optional.empty();
        }
        double closing = relativeX * velocityX + relativeY * velocityY + relativeZ * velocityZ;
        if (closing >= 0.0D) {
            // The projectile is moving away from or tangential to the player.
            return Optional.empty();
        }
        double ticksToClosest = -closing / speedSquared;
        if (ticksToClosest > maximumTicks) {
            return Optional.empty();
        }
        double approachX = relativeX + velocityX * ticksToClosest;
        double approachY = relativeY + velocityY * ticksToClosest;
        double approachZ = relativeZ + velocityZ * ticksToClosest;
        double closestSquared = approachX * approachX + approachY * approachY + approachZ * approachZ;
        if (closestSquared > hitRadius * hitRadius) {
            return Optional.empty();
        }
        return Optional.of(new ProjectileThreat(ticksToClosest, Math.sqrt(closestSquared), Math.sqrt(distanceSquared)));
    }

    private static void requireFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Projectile motion values must be finite");
            }
        }
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be a positive finite value");
        }
    }

    /** Immutable result of a positive incoming-projectile assessment. */
    public record ProjectileThreat(double timeToImpactTicks, double closestApproach, double distance) {
        public ProjectileThreat {
            if (!Double.isFinite(timeToImpactTicks) || timeToImpactTicks < 0.0D
                    || !Double.isFinite(closestApproach) || closestApproach < 0.0D
                    || !Double.isFinite(distance) || distance < 0.0D) {
                throw new IllegalArgumentException("Projectile threat values must be finite and non-negative");
            }
        }
    }
}
