package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Minecraft-free scheduler for legitimate vanilla melee repel attacks. */
public final class GojosInfinity extends Module {
    public enum RepelMode {
        STRONGEST_THREAT,
        CROWD_EMERGENCY
    }

    public enum TargetKind {
        PLAYER,
        HOSTILE,
        ANIMAL
    }

    public record Threat(
            String id,
            TargetKind kind,
            boolean friend,
            boolean ownerPet,
            boolean armorStand,
            boolean suspectedBot,
            boolean alive,
            boolean lineOfSight,
            boolean legalAttackRange,
            double distance,
            double closingSpeed,
            double predictedImpactTicks
    ) {
        public Threat {
            if (id == null || id.isBlank() || kind == null
                    || !Double.isFinite(distance) || distance < 0.0D
                    || !Double.isFinite(closingSpeed)
                    || !Double.isFinite(predictedImpactTicks) || predictedImpactTicks < 0.0D) {
                throw new IllegalArgumentException("Infinity threat facts are invalid");
            }
        }
    }

    public record AttackPlan(List<String> targetIds, boolean sprintReset, boolean silentRotation) {
        public AttackPlan {
            targetIds = List.copyOf(Objects.requireNonNull(targetIds, "targetIds"));
            if (targetIds.isEmpty() || targetIds.stream().anyMatch(id -> id == null || id.isBlank())) {
                throw new IllegalArgumentException("Infinity attack plan is invalid");
            }
        }
    }

    private final NumberSetting detectionRadius;
    private final NumberSetting repelDistance;
    private final IntegerSetting attackInterval;
    private final NumberSetting minimumAttackCharge;
    private final IntegerSetting targetsPerTick;
    private final EnumSetting<RepelMode> repelMode;
    private final BooleanSetting sprintReset;
    private final BooleanSetting silentRotation;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting animals;
    private final BooleanSetting excludeFriends;
    private final BooleanSetting excludeOwnerPets;
    private long lastAttackTick = Long.MIN_VALUE;

    public GojosInfinity() {
        super("gojo_infinity", "Gojo's Infinity",
                "Repels approaching living threats with legitimate, server-authoritative vanilla attacks.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        detectionRadius = addSetting(new NumberSetting("detection_radius", "Detection radius",
                "Maximum local distance for scanning already-loaded living threats.",
                6.0D, 3.0D, 12.0D));
        repelDistance = addSetting(new NumberSetting("repel_distance", "Repel distance",
                "Desired maximum distance at which an approaching target may be attacked.",
                3.0D, 1.0D, 6.0D));
        attackInterval = addSetting(new IntegerSetting("attack_interval", "Attack interval",
                "Minimum client ticks between repel pulses.", 10, 1, 40));
        minimumAttackCharge = addSetting(new NumberSetting("minimum_attack_charge", "Minimum attack charge",
                "Required vanilla attack-cooldown progress before a repel pulse.",
                0.90D, 0.0D, 1.0D));
        targetsPerTick = addSetting(new IntegerSetting("targets_per_tick", "Targets per tick",
                "Maximum living targets attempted during one pulse.", 1, 1, 4));
        repelMode = addSetting(new EnumSetting<>("repel_mode", "Repel mode",
                "Strongest Threat waits for the earliest impact; Crowd Emergency attacks several sequentially.",
                RepelMode.class, RepelMode.STRONGEST_THREAT));
        sprintReset = addSetting(new BooleanSetting("sprint_reset", "Sprint reset",
                "Send ordinary stop/start sprint commands before each attack to attempt vanilla sprint-hit knockback.",
                true));
        silentRotation = addSetting(new BooleanSetting("silent_rotation", "Silent rotation",
                "Face each target in server movement updates without moving the visible camera.",
                true));
        players = addSetting(new BooleanSetting("players", "Players",
                "Repel approaching non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles",
                "Repel approaching hostile and neutral mobs.", true));
        animals = addSetting(new BooleanSetting("animals", "Animals",
                "Repel approaching animals and other passive living entities.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never attack a locally listed friend.", true));
        excludeOwnerPets = addSetting(new BooleanSetting("exclude_owner_pets", "Exclude owner pets",
                "Never attack a tame entity owned by the local player.", true));
    }

    /** Chooses one strongest target or a bounded crowd pulse without Minecraft dependencies. */
    public java.util.Optional<AttackPlan> plan(long tick, double attackCharge, List<Threat> threats) {
        if (tick < 0L || !Double.isFinite(attackCharge) || attackCharge < 0.0D || attackCharge > 1.0D
                || threats == null) {
            throw new IllegalArgumentException("Infinity planning inputs are invalid");
        }
        if (!isEnabled() || attackCharge < minimumAttackCharge.value()
                || (lastAttackTick != Long.MIN_VALUE && tick - lastAttackTick < attackInterval.value())) {
            return java.util.Optional.empty();
        }
        List<Threat> eligible = threats.stream()
                .filter(this::eligible)
                .sorted(Comparator.comparingDouble(Threat::predictedImpactTicks)
                        .thenComparingDouble(Threat::distance)
                        .thenComparing(Threat::id))
                .toList();
        if (eligible.isEmpty()) {
            return java.util.Optional.empty();
        }
        int limit = repelMode.value() == RepelMode.STRONGEST_THREAT
                ? 1 : Math.min(targetsPerTick.value(), eligible.size());
        return java.util.Optional.of(new AttackPlan(
                eligible.stream().limit(limit).map(Threat::id).toList(),
                sprintReset.value(), silentRotation.value()));
    }

    public void markExecuted(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("Infinity execution tick is invalid");
        }
        lastAttackTick = tick;
    }

    public double detectionRadius() {
        return detectionRadius.value();
    }

    public double repelDistance() {
        return repelDistance.value();
    }

    public void onContextLost() {
        lastAttackTick = Long.MIN_VALUE;
    }

    private boolean eligible(Threat threat) {
        return threat != null && threat.alive() && threat.lineOfSight() && threat.legalAttackRange()
                && !threat.armorStand() && !threat.suspectedBot()
                && threat.distance() <= detectionRadius.value()
                && threat.distance() <= repelDistance.value()
                && threat.closingSpeed() >= -1.0E-4D
                && (!threat.friend() || !excludeFriends.value())
                && (!threat.ownerPet() || !excludeOwnerPets.value())
                && switch (threat.kind()) {
                    case PLAYER -> players.value();
                    case HOSTILE -> hostiles.value();
                    case ANIMAL -> animals.value();
                };
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }
}
