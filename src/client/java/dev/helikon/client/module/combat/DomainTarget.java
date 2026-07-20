package dev.helikon.client.module.combat;

import java.util.Objects;

/** Minecraft-free target facts required by Domain Expansion. */
public record DomainTarget(
        String id,
        String name,
        DomainPosition feet,
        double x,
        double z,
        double distance,
        double health,
        double angleDegrees,
        double velocityX,
        double velocityZ,
        double facingX,
        double facingZ,
        boolean friend,
        boolean alive,
        boolean spectator,
        boolean creative,
        boolean loaded,
        boolean crosshair
) {
    public DomainTarget {
        id = requireText(id, "id");
        name = requireText(name, "name");
        feet = Objects.requireNonNull(feet, "feet");
        if (!finite(x, z, distance, health, angleDegrees, velocityX, velocityZ, facingX, facingZ)
                || distance < 0.0D || health < 0.0D || angleDegrees < 0.0D || angleDegrees > 180.0D) {
            throw new IllegalArgumentException("Domain target facts are invalid");
        }
    }

    public double escapeX() {
        return Math.abs(velocityX) + Math.abs(velocityZ) >= 0.04D ? velocityX : facingX;
    }

    public double escapeZ() {
        return Math.abs(velocityX) + Math.abs(velocityZ) >= 0.04D ? velocityZ : facingZ;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
