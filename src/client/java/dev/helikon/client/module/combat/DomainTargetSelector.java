package dev.helikon.client.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stateful target dwell and per-target cooldown policy for automatic proximity activation. */
public final class DomainTargetSelector {
    public enum Priority {
        NEAREST,
        LOWEST_HEALTH,
        CROSSHAIR
    }

    private final Map<String, Long> firstEligibleTick = new HashMap<>();
    private final Map<String, Long> cooldownUntilTick = new HashMap<>();

    public Optional<DomainTarget> select(
            long tick,
            List<DomainTarget> candidates,
            double range,
            int dwellTicks,
            boolean ignoreFriends,
            Priority priority,
            boolean useCooldown
    ) {
        if (tick < 0L || candidates == null || !Double.isFinite(range) || range < 0.0D || dwellTicks < 0
                || priority == null) {
            throw new IllegalArgumentException("Domain target selection inputs are invalid");
        }
        List<DomainTarget> eligible = new ArrayList<>();
        for (DomainTarget candidate : candidates) {
            if (!valid(candidate, range, ignoreFriends)
                    || useCooldown && tick < cooldownUntilTick.getOrDefault(candidate.id(), Long.MIN_VALUE)) {
                firstEligibleTick.remove(candidate.id());
                continue;
            }
            long first = firstEligibleTick.computeIfAbsent(candidate.id(), ignored -> tick);
            if (tick - first >= dwellTicks) {
                eligible.add(candidate);
            }
        }
        firstEligibleTick.keySet().removeIf(id -> candidates.stream().noneMatch(candidate -> candidate.id().equals(id)));
        return eligible.stream().min(comparator(priority));
    }

    public void coolDown(String targetId, long tick, int cooldownTicks) {
        if (targetId == null || targetId.isBlank() || tick < 0L || cooldownTicks < 0) {
            throw new IllegalArgumentException("Domain target cooldown inputs are invalid");
        }
        cooldownUntilTick.put(targetId, tick + cooldownTicks);
        firstEligibleTick.remove(targetId);
    }

    public boolean coolingDown(String targetId, long tick) {
        return tick < cooldownUntilTick.getOrDefault(targetId, Long.MIN_VALUE);
    }

    public void resetDwell() {
        firstEligibleTick.clear();
    }

    public void reset() {
        firstEligibleTick.clear();
        cooldownUntilTick.clear();
    }

    private static boolean valid(DomainTarget target, double range, boolean ignoreFriends) {
        return target.alive() && target.loaded() && !target.spectator() && !target.creative()
                && (!ignoreFriends || !target.friend()) && target.distance() <= range;
    }

    private static Comparator<DomainTarget> comparator(Priority priority) {
        Comparator<DomainTarget> stable = Comparator.comparingDouble(DomainTarget::distance)
                .thenComparing(DomainTarget::id);
        return switch (priority) {
            case NEAREST -> stable;
            case LOWEST_HEALTH -> Comparator.comparingDouble(DomainTarget::health).thenComparing(stable);
            case CROSSHAIR -> Comparator.comparing(DomainTarget::crosshair).reversed().thenComparing(stable);
        };
    }
}
