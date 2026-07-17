package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Minecraft-free friend-following and nearby-threat decisions for Protect. */
public final class Protect extends Module {
    public record Context(boolean screenOpen, boolean passenger, boolean abilityFlying, boolean fallFlying,
                          boolean attackReady, List<CombatTarget> targets) {
        public Context {
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        }
    }

    public record Action(CombatTarget protectedFriend, Optional<CombatTarget> threat, boolean move,
                         double velocityX, double velocityZ, boolean attack) {
        public Action {
            Objects.requireNonNull(protectedFriend, "protectedFriend");
            threat = Objects.requireNonNull(threat, "threat");
            if (!Double.isFinite(velocityX) || !Double.isFinite(velocityZ)) {
                throw new IllegalArgumentException("Protect velocity must be finite");
            }
            if (!move && (velocityX != 0.0D || velocityZ != 0.0D)) {
                throw new IllegalArgumentException("A stationary Protect action cannot contain velocity");
            }
            if (attack && threat.isEmpty()) {
                throw new IllegalArgumentException("Protect cannot attack without a threat");
            }
        }
    }

    private final StringSetting protectedFriend;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final NumberSetting followRange;
    private final NumberSetting followDistance;
    private final NumberSetting movementSpeed;
    private final NumberSetting protectionRadius;
    private final NumberSetting attackRange;
    private final NumberSetting rotationSpeed;
    private final IntegerSetting delayTicks;
    private String currentProtectedId;
    private long lastAttackTick = -1L;

    public Protect() {
        super("protect", "Protect", "Follows a listed friend and attacks nearby eligible threats.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        protectedFriend = addSetting(new StringSetting("protected_friend", "Protected friend",
                "Exact friend-list name to protect; blank selects the nearest loaded friend.", "", 16, true));
        players = addSetting(new BooleanSetting("players", "Players", "Defend against non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Defend against hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Defend against passive mobs.", false));
        followRange = addSetting(new NumberSetting("follow_range", "Follow range",
                "Maximum local distance at which a listed friend can be selected.", 32.0D, 2.0D, 64.0D));
        followDistance = addSetting(new NumberSetting("follow_distance", "Follow distance",
                "Horizontal distance at which automatic following stops.", 3.0D, 0.5D, 8.0D));
        movementSpeed = addSetting(new NumberSetting("movement_speed", "Movement speed",
                "Horizontal follow velocity in blocks per tick.", 0.18D, 0.05D, 0.30D));
        protectionRadius = addSetting(new NumberSetting("protection_radius", "Protection radius",
                "Maximum distance between a threat and the protected friend.", 6.0D, 2.0D, 16.0D));
        attackRange = addSetting(new NumberSetting("attack_range", "Attack range",
                "Maximum local distance at which a normal attack is requested.", 3.0D, 1.0D, 6.0D));
        rotationSpeed = addSetting(new NumberSetting("rotation_speed", "Rotation speed",
                "Maximum local yaw or pitch adjustment toward a threat per tick.", 12.0D, 0.25D, 30.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Attack delay",
                "Minimum client ticks between ordinary attack requests.", 4, 2, 40));
    }

    /**
     * Selects one listed friend, computes bounded following, then chooses the closest eligible
     * threat to that friend. Attack cadence is consumed only when a normal attack is requested.
     */
    public Optional<Action> update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("Protect inputs are invalid");
        }
        if (!isEnabled() || context.screenOpen() || context.passenger()
                || context.abilityFlying() || context.fallFlying()) {
            currentProtectedId = null;
            return Optional.empty();
        }

        CombatTarget friend = selectProtectedFriend(context.targets());
        if (friend == null) {
            currentProtectedId = null;
            return Optional.empty();
        }
        currentProtectedId = friend.id();

        double horizontalDistance = Math.hypot(friend.relativeX(), friend.relativeZ());
        boolean move = horizontalDistance > followDistance.value() && horizontalDistance > 0.0D;
        double velocityX = 0.0D;
        double velocityZ = 0.0D;
        if (move) {
            double scale = movementSpeed.value() / horizontalDistance;
            velocityX = friend.relativeX() * scale;
            velocityZ = friend.relativeZ() * scale;
        }

        Optional<CombatTarget> threat = selectThreat(friend, context.targets());
        boolean cadenceReady = lastAttackTick < 0L || tick - lastAttackTick >= delayTicks.value();
        boolean attack = threat.isPresent() && context.attackReady() && cadenceReady
                && threat.orElseThrow().distance() <= attackRange.value();
        if (attack) {
            lastAttackTick = tick;
        }
        return Optional.of(new Action(friend, threat, move, velocityX, velocityZ, attack));
    }

    /** Returns a bounded local view adjustment toward the selected threat. */
    public CombatAim.Rotation rotateToward(CombatTarget target, CombatAim.Rotation current) {
        if (target == null || current == null) {
            throw new IllegalArgumentException("Protect rotation inputs must not be null");
        }
        return CombatAim.limit(current, CombatAim.predictedRotation(target, 1.0D, 0.0D, false),
                rotationSpeed.value());
    }

    public void resetTransientState() {
        currentProtectedId = null;
        lastAttackTick = -1L;
    }

    private CombatTarget selectProtectedFriend(List<CombatTarget> targets) {
        String configured = protectedFriend.value().trim();
        List<CombatTarget> eligible = targets.stream()
                .filter(target -> target.type() == CombatEntityType.PLAYER && target.friend() && target.alive())
                .filter(target -> target.distance() <= followRange.value())
                .filter(target -> configured.isEmpty() || target.name().equalsIgnoreCase(configured))
                .sorted(Comparator.comparingDouble(CombatTarget::distance).thenComparing(CombatTarget::id))
                .toList();
        if (currentProtectedId != null) {
            for (CombatTarget target : eligible) {
                if (target.id().equals(currentProtectedId)) {
                    return target;
                }
            }
        }
        return eligible.isEmpty() ? null : eligible.getFirst();
    }

    private Optional<CombatTarget> selectThreat(CombatTarget friend, List<CombatTarget> targets) {
        return targets.stream()
                .filter(target -> !target.id().equals(friend.id()))
                .filter(target -> !target.friend() && !target.suspectedBot() && target.alive() && target.lineOfSight())
                .filter(target -> typeEnabled(target.type()))
                .filter(target -> distanceBetween(friend, target) <= protectionRadius.value())
                .sorted(Comparator.comparingDouble((CombatTarget target) -> distanceBetween(friend, target))
                        .thenComparingDouble(CombatTarget::distance)
                        .thenComparing(CombatTarget::id))
                .findFirst();
    }

    private boolean typeEnabled(CombatEntityType type) {
        return switch (type) {
            case PLAYER -> players.value();
            case HOSTILE -> hostiles.value();
            case PASSIVE -> passive.value();
        };
    }

    private static double distanceBetween(CombatTarget first, CombatTarget second) {
        double x = first.relativeX() - second.relativeX();
        double y = first.relativeY() - second.relativeY();
        double z = first.relativeZ() - second.relativeZ();
        return Math.sqrt(x * x + y * y + z * z);
    }

    @Override
    protected void onDisable() {
        resetTransientState();
    }
}
