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

    /**
     * Simulates a gravity-affected projectile and checks every per-tick swept
     * segment, preventing fast arrows from skipping across the hit radius.
     */
    public static Optional<ProjectileThreat> assessBallistic(
            double relativeX, double relativeY, double relativeZ,
            double projectileVelocityX, double projectileVelocityY, double projectileVelocityZ,
            double playerVelocityX, double playerVelocityY, double playerVelocityZ,
            double hitRadius, double maximumTicks, double detectionRange,
            double gravityPerTick, double dragPerTick) {
        requireFinite(relativeX, relativeY, relativeZ,
                projectileVelocityX, projectileVelocityY, projectileVelocityZ,
                playerVelocityX, playerVelocityY, playerVelocityZ, gravityPerTick, dragPerTick);
        requirePositive(hitRadius, "hitRadius");
        requirePositive(maximumTicks, "maximumTicks");
        requirePositive(detectionRange, "detectionRange");
        if (gravityPerTick < 0.0D || dragPerTick <= 0.0D || dragPerTick > 1.0D) {
            throw new IllegalArgumentException("Ballistic gravity and drag are invalid");
        }

        double initialDistanceSquared = relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ;
        if (initialDistanceSquared > detectionRange * detectionRange) {
            return Optional.empty();
        }
        double radiusSquared = hitRadius * hitRadius;
        int steps = Math.max(1, (int) Math.ceil(maximumTicks));
        double x = relativeX;
        double y = relativeY;
        double z = relativeZ;
        double velocityX = projectileVelocityX;
        double velocityY = projectileVelocityY;
        double velocityZ = projectileVelocityZ;
        double closestSquared = initialDistanceSquared;
        for (int step = 0; step < steps; step++) {
            double deltaX = velocityX - playerVelocityX;
            double deltaY = velocityY - playerVelocityY;
            double deltaZ = velocityZ - playerVelocityZ;
            double segmentLengthSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            if (segmentLengthSquared >= MINIMUM_SPEED_SQUARED) {
                double fraction = clamp(-(x * deltaX + y * deltaY + z * deltaZ)
                        / segmentLengthSquared, 0.0D, 1.0D);
                double approachX = x + deltaX * fraction;
                double approachY = y + deltaY * fraction;
                double approachZ = z + deltaZ * fraction;
                double segmentClosestSquared = approachX * approachX
                        + approachY * approachY + approachZ * approachZ;
                closestSquared = Math.min(closestSquared, segmentClosestSquared);
                if (segmentClosestSquared <= radiusSquared) {
                    return Optional.of(new ProjectileThreat(step + fraction,
                            Math.sqrt(segmentClosestSquared), Math.sqrt(initialDistanceSquared)));
                }
            }
            x += deltaX;
            y += deltaY;
            z += deltaZ;
            velocityX *= dragPerTick;
            velocityY = velocityY * dragPerTick - gravityPerTick;
            velocityZ *= dragPerTick;
        }
        return Optional.empty();
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

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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
