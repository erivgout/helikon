package dev.helikon.client.combat;

import java.util.List;
import java.util.Objects;

/** Immutable Minecraft-free facts about one locally observed possible combat target. */
public record CombatTarget(
        String id,
        String name,
        CombatEntityType type,
        boolean friend,
        boolean suspectedBot,
        boolean alive,
        boolean visible,
        boolean lineOfSight,
        double distance,
        double angleDegrees,
        double relativeX,
        double relativeY,
        double relativeZ,
        double velocityX,
        double velocityY,
        double velocityZ,
        double health,
        int armor,
        String heldItem,
        List<String> effects
) {
    public CombatTarget {
        if (id == null || id.isBlank() || name == null || name.isBlank() || type == null || !Double.isFinite(distance)
                || distance < 0.0D || !Double.isFinite(angleDegrees) || angleDegrees < 0.0D || angleDegrees > 180.0D
                || !Double.isFinite(relativeX) || !Double.isFinite(relativeY) || !Double.isFinite(relativeZ)
                || !Double.isFinite(velocityX) || !Double.isFinite(velocityY) || !Double.isFinite(velocityZ)
                || !Double.isFinite(health) || health < 0.0D || armor < 0 || heldItem == null) {
            throw new IllegalArgumentException("combat target facts are invalid");
        }
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }
}
