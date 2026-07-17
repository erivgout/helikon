package dev.helikon.client.combat;

import java.util.Optional;
import java.util.List;
import java.util.Objects;

/** Session-only local target and measured normal-attack distance state for combat HUDs. */
public final class CombatTargetTracker {
    private CombatTarget target;
    private double lastAttackDistance = -1.0D;

    public void observe(CombatTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    public void recordAttack(CombatTarget target) {
        observe(target);
        lastAttackDistance = target.distance();
    }

    public Optional<CombatTarget> target() {
        return Optional.ofNullable(target);
    }

    public Optional<Double> lastAttackDistance() {
        return lastAttackDistance < 0.0D ? Optional.empty() : Optional.of(lastAttackDistance);
    }

    public void clear() {
        target = null;
        lastAttackDistance = -1.0D;
    }

    /** Retains the current local target only while the current observation pass still contains it. */
    public void clearIfAbsent(List<CombatTarget> observedTargets) {
        Objects.requireNonNull(observedTargets, "observedTargets");
        if (target != null && observedTargets.stream().noneMatch(candidate -> candidate.id().equals(target.id()))) {
            clear();
        }
    }
}
