package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Minecraft-free target, approach, aim, and attack decisions for a small bounded combat bot. */
public final class FightBot extends Module {
    public record Context(boolean screenOpen, boolean passenger, boolean abilityFlying, boolean fallFlying,
                          boolean attackReady, List<CombatTarget> targets) {
        public Context {
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        }
    }

    public record Action(CombatTarget target, boolean move, double velocityX, double velocityZ, boolean attack) {
        public Action {
            Objects.requireNonNull(target, "target");
            if (!Double.isFinite(velocityX) || !Double.isFinite(velocityZ)) {
                throw new IllegalArgumentException("FightBot velocity must be finite");
            }
            if (!move && (velocityX != 0.0D || velocityZ != 0.0D)) {
                throw new IllegalArgumentException("A stationary FightBot action cannot contain velocity");
            }
        }
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting searchRange;
    private final NumberSetting stopDistance;
    private final NumberSetting movementSpeed;
    private final NumberSetting attackRange;
    private final NumberSetting rotationSpeed;
    private final IntegerSetting delayTicks;
    private String currentTargetId;
    private long lastAttackTick = -1L;

    public FightBot() {
        super("fight_bot", "FightBot", "Walks toward and normally attacks one nearby eligible enemy.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Target non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Target hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Target passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
        searchRange = addSetting(new NumberSetting("search_range", "Search range",
                "Maximum local distance at which an enemy can be selected.", 16.0D, 3.0D, 32.0D));
        stopDistance = addSetting(new NumberSetting("stop_distance", "Stop distance",
                "Horizontal distance at which automatic approach movement stops.", 2.5D, 0.5D, 5.0D));
        movementSpeed = addSetting(new NumberSetting("movement_speed", "Movement speed",
                "Horizontal approach velocity in blocks per tick.", 0.18D, 0.05D, 0.30D));
        attackRange = addSetting(new NumberSetting("attack_range", "Attack range",
                "Maximum local distance at which a normal attack is requested.", 3.0D, 1.0D, 6.0D));
        rotationSpeed = addSetting(new NumberSetting("rotation_speed", "Rotation speed",
                "Maximum local yaw or pitch adjustment per client tick.", 12.0D, 0.25D, 30.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Attack delay",
                "Minimum client ticks between ordinary attack requests.", 4, 2, 40));
    }

    /**
     * Locks the nearest eligible target, computes one bounded approach velocity, and consumes
     * attack cadence only when a normal attack is requested.
     */
    public Optional<Action> update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("FightBot inputs are invalid");
        }
        if (!isEnabled() || context.screenOpen() || context.passenger()
                || context.abilityFlying() || context.fallFlying()) {
            currentTargetId = null;
            return Optional.empty();
        }

        List<CombatTarget> allowed = CombatTargetFilter.ordered(context.targets(), targetOptions(),
                CombatTargetFilter.Priority.DISTANCE);
        CombatTarget target = lockedTarget(allowed);
        if (target == null) {
            currentTargetId = null;
            return Optional.empty();
        }
        currentTargetId = target.id();

        double horizontalDistance = Math.hypot(target.relativeX(), target.relativeZ());
        boolean move = horizontalDistance > stopDistance.value() && horizontalDistance > 0.0D;
        double velocityX = 0.0D;
        double velocityZ = 0.0D;
        if (move) {
            double scale = movementSpeed.value() / horizontalDistance;
            velocityX = target.relativeX() * scale;
            velocityZ = target.relativeZ() * scale;
        }

        boolean cadenceReady = lastAttackTick < 0L || tick - lastAttackTick >= delayTicks.value();
        boolean attack = context.attackReady() && cadenceReady && target.distance() <= attackRange.value();
        if (attack) {
            lastAttackTick = tick;
        }
        return Optional.of(new Action(target, move, velocityX, velocityZ, attack));
    }

    /** Returns a bounded local view adjustment toward the active target. */
    public CombatAim.Rotation rotateToward(CombatTarget target, CombatAim.Rotation current) {
        if (target == null || current == null) {
            throw new IllegalArgumentException("FightBot rotation inputs must not be null");
        }
        return CombatAim.limit(current, CombatAim.predictedRotation(target, 1.0D, 0.0D, false),
                rotationSpeed.value());
    }

    public boolean excludeFriends() {
        return excludeFriends.value();
    }

    public void resetTransientState() {
        currentTargetId = null;
        lastAttackTick = -1L;
    }

    private CombatTarget lockedTarget(List<CombatTarget> allowed) {
        if (currentTargetId != null) {
            for (CombatTarget target : allowed) {
                if (target.id().equals(currentTargetId)) {
                    return target;
                }
            }
        }
        return allowed.isEmpty() ? null : allowed.getFirst();
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(),
                excludeFriends.value(), true, searchRange.value(), 180.0D, true);
    }

    @Override
    protected void onDisable() {
        resetTransientState();
    }
}
