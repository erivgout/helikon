package dev.helikon.client.combat;

import java.util.Comparator;
import java.util.List;

/** Deterministic local target filtering and ordering without Minecraft dependencies. */
public final class CombatTargetFilter {
    private CombatTargetFilter() {
    }

    public enum Priority {
        DISTANCE,
        HEALTH,
        ANGLE
    }

    public record Options(boolean players, boolean hostiles, boolean passive, boolean excludeFriends,
                          boolean excludeBots, double range, double fieldOfView, boolean lineOfSightRequired) {
        public Options {
            if (!Double.isFinite(range) || range <= 0.0D || !Double.isFinite(fieldOfView)
                    || fieldOfView <= 0.0D || fieldOfView > 180.0D) {
                throw new IllegalArgumentException("combat target filter bounds are invalid");
            }
        }
    }

    public static boolean allows(CombatTarget target, Options options) {
        if (target == null || options == null || !target.alive() || target.distance() > options.range()
                || target.angleDegrees() > options.fieldOfView() || (options.excludeFriends() && target.friend())
                || (options.excludeBots() && target.suspectedBot()) || (options.lineOfSightRequired() && !target.lineOfSight())) {
            return false;
        }
        return switch (target.type()) {
            case PLAYER -> options.players();
            case HOSTILE -> options.hostiles();
            case PASSIVE -> options.passive();
        };
    }

    public static List<CombatTarget> ordered(List<CombatTarget> candidates, Options options, Priority priority) {
        if (candidates == null || options == null || priority == null) {
            throw new IllegalArgumentException("combat target inputs must not be null");
        }
        Comparator<CombatTarget> comparator = switch (priority) {
            case DISTANCE -> Comparator.comparingDouble(CombatTarget::distance)
                    .thenComparingDouble(CombatTarget::angleDegrees).thenComparing(CombatTarget::id);
            case HEALTH -> Comparator.comparingDouble(CombatTarget::health)
                    .thenComparingDouble(CombatTarget::distance).thenComparing(CombatTarget::id);
            case ANGLE -> Comparator.comparingDouble(CombatTarget::angleDegrees)
                    .thenComparingDouble(CombatTarget::distance).thenComparing(CombatTarget::id);
        };
        return candidates.stream().filter(target -> allows(target, options)).sorted(comparator).toList();
    }
}
