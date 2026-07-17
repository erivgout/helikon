package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Chooses a bounded safe escape destination when an incoming projectile is about to hit. */
public final class EndermanAura extends Module {
    private final NumberSetting detectionRange;
    private final NumberSetting warningSeconds;
    private final NumberSetting hitRadius;
    private final NumberSetting teleportDistance;
    private final IntegerSetting cooldownTicks;
    private final BooleanSetting excludeFriendProjectiles;
    private final BooleanSetting cancelVelocity;
    private long lastTeleportTick = Long.MIN_VALUE;

    public EndermanAura() {
        super("enderman_aura", "Enderman Aura",
                "Teleports to a nearby collision-free position when a loaded projectile is about to hit.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        detectionRange = addSetting(new NumberSetting("detection_range", "Detection range",
                "Maximum distance at which loaded projectiles are considered.", 32.0D, 4.0D, 64.0D));
        warningSeconds = addSetting(new NumberSetting("warning_seconds", "Warning time",
                "Maximum predicted time before closest approach.", 1.25D, 0.1D, 4.0D));
        hitRadius = addSetting(new NumberSetting("hit_radius", "Hit radius",
                "Closest-approach distance that counts as an incoming hit.", 1.4D, 0.5D, 3.0D));
        teleportDistance = addSetting(new NumberSetting("teleport_distance", "Teleport distance",
                "Preferred horizontal escape distance.", 6.0D, 2.0D, 12.0D));
        cooldownTicks = addSetting(new IntegerSetting("cooldown_ticks", "Cooldown ticks",
                "Minimum client ticks between escape attempts.", 20, 1, 200));
        excludeFriendProjectiles = addSetting(new BooleanSetting("exclude_friend_projectiles",
                "Exclude friend projectiles", "Ignore projectiles owned by locally saved friends.", true));
        cancelVelocity = addSetting(new BooleanSetting("cancel_velocity", "Cancel velocity",
                "Clear local velocity after a successful escape.", false));
    }

    public Optional<EscapePlan> choose(long tick, List<Threat> threats, List<Destination> destinations) {
        if (tick < 0L || threats == null || destinations == null) {
            throw new IllegalArgumentException("Enderman Aura inputs are invalid");
        }
        if (!isEnabled() || (lastTeleportTick != Long.MIN_VALUE
                && tick - lastTeleportTick < cooldownTicks.value())) {
            return Optional.empty();
        }
        Optional<Threat> incoming = threats.stream()
                .filter(threat -> !threat.selfOwned())
                .filter(threat -> !excludeFriendProjectiles.value() || !threat.friendOwned())
                .filter(threat -> threat.distance() <= detectionRange.value())
                .filter(threat -> threat.timeToImpactTicks() <= warningSeconds.value() * 20.0D)
                .filter(threat -> threat.closestApproach() <= hitRadius.value())
                .min(Comparator.comparingDouble(Threat::timeToImpactTicks));
        if (incoming.isEmpty()) {
            return Optional.empty();
        }
        return destinations.stream()
                .filter(Destination::loaded)
                .filter(Destination::collisionFree)
                .filter(Destination::safeFloor)
                .filter(destination -> destination.distance() >= teleportDistance.value() * 0.65D)
                .max(Comparator.comparingDouble(Destination::projectileSeparation)
                        .thenComparingDouble(Destination::distance))
                .map(destination -> new EscapePlan(incoming.orElseThrow(), destination));
    }

    public void markTeleported(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("Teleport tick must be non-negative");
        }
        lastTeleportTick = tick;
    }

    public void onContextLost() {
        lastTeleportTick = Long.MIN_VALUE;
    }

    public double detectionRange() {
        return detectionRange.value();
    }

    public double warningTicks() {
        return warningSeconds.value() * 20.0D;
    }

    public double hitRadius() {
        return hitRadius.value();
    }

    public double teleportDistance() {
        return teleportDistance.value();
    }

    public boolean excludeFriendProjectiles() {
        return excludeFriendProjectiles.value();
    }

    public boolean cancelVelocity() {
        return cancelVelocity.value();
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }

    public record Threat(int entityId, boolean selfOwned, boolean friendOwned, double timeToImpactTicks,
                         double closestApproach, double distance) {
        public Threat {
            if (entityId < 0 || !Double.isFinite(timeToImpactTicks) || timeToImpactTicks < 0.0D
                    || !Double.isFinite(closestApproach) || closestApproach < 0.0D
                    || !Double.isFinite(distance) || distance < 0.0D) {
                throw new IllegalArgumentException("Projectile threat is invalid");
            }
        }
    }

    public record Destination(double x, double y, double z, double distance, double projectileSeparation,
                              boolean loaded, boolean collisionFree, boolean safeFloor) {
        public Destination {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(distance) || distance < 0.0D
                    || !Double.isFinite(projectileSeparation) || projectileSeparation < 0.0D) {
                throw new IllegalArgumentException("Escape destination is invalid");
            }
        }
    }

    public record EscapePlan(Threat threat, Destination destination) {
        public EscapePlan {
            if (threat == null || destination == null) {
                throw new IllegalArgumentException("Escape plan is incomplete");
            }
        }
    }
}
