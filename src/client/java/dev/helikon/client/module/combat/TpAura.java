package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Chooses one TP-Aura target and builds bounded straight-line movement paths without Minecraft dependencies. */
public final class TpAura extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting attackDistance;
    private final NumberSetting maximumStepDistance;
    private final IntegerSetting maximumSteps;
    private final IntegerSetting delayTicks;
    private long lastAttackTick = -1L;

    public TpAura() {
        super("tp_aura", "TP-Aura",
                "Packet-moves toward one validated target, requests one ordinary attack, and returns.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never select a locally listed friend.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum locally considered target distance.",
                12.0D, 4.0D, 24.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view",
                "Maximum target angle from the current view.", 90.0D, 5.0D, 180.0D));
        attackDistance = addSetting(new NumberSetting("attack_distance", "Attack distance",
                "Requested stopping distance from the target before the ordinary attack.",
                2.5D, 1.5D, 3.0D));
        maximumStepDistance = addSetting(new NumberSetting("maximum_step_distance", "Maximum step distance",
                "Maximum distance between consecutive vanilla-shaped movement updates.",
                3.0D, 0.25D, 4.0D));
        maximumSteps = addSetting(new IntegerSetting("maximum_steps", "Maximum steps",
                "Maximum movement updates allowed in each direction.", 6, 1, 12));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay ticks",
                "Minimum ticks between completed teleport-attack-return attempts.", 10, 1, 40));
    }

    public Optional<AttackPlan> nextPlan(long tick, boolean attackReady, List<CombatTarget> candidates) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("TP-Aura inputs are invalid");
        }
        if (!isEnabled() || !attackReady
                || (lastAttackTick >= 0L && tick - lastAttackTick < delayTicks.value())) {
            return Optional.empty();
        }
        return CombatTargetFilter.ordered(candidates, targetOptions(), CombatTargetFilter.Priority.DISTANCE).stream()
                .filter(target -> target.distance() > attackDistance.value())
                .filter(target -> target.distance() - attackDistance.value()
                        <= maximumStepDistance.value() * maximumSteps.value())
                .findFirst()
                .map(target -> new AttackPlan(target, attackDistance.value(),
                        maximumStepDistance.value(), maximumSteps.value()));
    }

    public List<Point> buildPath(Point from, Point to, AttackPlan plan) {
        if (from == null || to == null || plan == null) {
            throw new IllegalArgumentException("TP-Aura path inputs are invalid");
        }
        double x = to.x() - from.x();
        double y = to.y() - from.y();
        double z = to.z() - from.z();
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance < 1.0E-6D) {
            return List.of();
        }
        int steps = (int) Math.ceil(distance / plan.maximumStepDistance());
        if (steps > plan.maximumSteps()) {
            return List.of();
        }
        List<Point> path = new ArrayList<>(steps);
        for (int index = 1; index <= steps; index++) {
            double progress = index / (double) steps;
            path.add(new Point(from.x() + x * progress, from.y() + y * progress, from.z() + z * progress));
        }
        return List.copyOf(path);
    }

    public void markExecuted(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("TP-Aura execution tick is invalid");
        }
        lastAttackTick = tick;
    }

    public void onContextLost() {
        lastAttackTick = -1L;
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(),
                excludeFriends.value(), true, range.value(), fieldOfView.value(), true);
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }

    public record Point(double x, double y, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("TP-Aura point must be finite");
            }
        }
    }

    public record AttackPlan(CombatTarget target, double attackDistance,
                             double maximumStepDistance, int maximumSteps) {
        public AttackPlan {
            if (target == null || !Double.isFinite(attackDistance) || attackDistance <= 0.0D
                    || !Double.isFinite(maximumStepDistance) || maximumStepDistance <= 0.0D
                    || maximumSteps < 1 || maximumSteps > 12) {
                throw new IllegalArgumentException("TP-Aura attack plan is invalid");
            }
        }
    }
}
