package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/** Selects one visible local target and requests one ordinary Minecraft attack at a bounded cadence. */
public final class KillAura extends Module {
    public enum TargetMode {
        SINGLE,
        SWITCH,
        MULTI
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting delayTicks;
    private final NumberSetting rotationSpeed;
    private final NumberSetting maxTargets;
    private final EnumSetting<TargetMode> targetMode;
    private final EnumSetting<CombatTargetFilter.Priority> priority;
    private String currentTargetId;
    private long lastAttackTick = -1L;

    public KillAura() {
        super("kill_aura", "KillAura", "Uses normal client attacks against one visible, eligible local target.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local target distance.", 3.0D, 1.0D, 6.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", "Maximum target view angle.",
                90.0D, 5.0D, 180.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Attack delay", "Minimum ticks between normal attacks.",
                4.0D, 2.0D, 40.0D));
        rotationSpeed = addSetting(new NumberSetting("rotation_speed", "Rotation speed",
                "Maximum local yaw or pitch change toward the selected target per client tick.", 8.0D, 0.25D, 30.0D));
        targetMode = addSetting(new EnumSetting<>("target_mode", "Target mode",
                "Keep one target, rotate targets, or attack multiple eligible targets.",
                TargetMode.class, TargetMode.SINGLE));
        maxTargets = addSetting(new NumberSetting("max_targets", "Max targets",
                "Maximum eligible targets attacked during each Multi cycle.", 3.0D, 2.0D, 10.0D));
        priority = addSetting(new EnumSetting<>("priority", "Priority", "Order eligible targets by distance, health, or angle.",
                CombatTargetFilter.Priority.class, CombatTargetFilter.Priority.DISTANCE));
    }

    public Optional<CombatTarget> nextAttack(long tick, List<CombatTarget> candidates, boolean attackReady) {
        List<CombatTarget> attacks = nextAttacks(tick, candidates, attackReady);
        return attacks.isEmpty() ? Optional.empty() : Optional.of(attacks.getFirst());
    }

    /** Selects one target normally or a bounded ordered group while Multi mode is active. */
    public List<CombatTarget> nextAttacks(long tick, List<CombatTarget> candidates, boolean attackReady) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("KillAura inputs are invalid");
        }
        if (!isEnabled() || !attackReady || (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value()))) {
            return List.of();
        }
        List<CombatTarget> allowed = CombatTargetFilter.ordered(candidates, targetOptions(), priority.value());
        if (allowed.isEmpty()) {
            currentTargetId = null;
            return List.of();
        }
        if (targetMode.value() == TargetMode.MULTI) {
            int count = Math.min(allowed.size(), (int) Math.round(maxTargets.value()));
            List<CombatTarget> selected = List.copyOf(allowed.subList(0, count));
            currentTargetId = selected.getFirst().id();
            lastAttackTick = tick;
            return selected;
        }
        CombatTarget selected = select(allowed);
        currentTargetId = selected.id();
        lastAttackTick = tick;
        return List.of(selected);
    }

    /** Returns a bounded local view adjustment for the selected target; it never creates a rotation packet. */
    public CombatAim.Rotation rotateToward(CombatTarget target, CombatAim.Rotation current) {
        if (target == null || current == null) {
            throw new IllegalArgumentException("KillAura rotation inputs must not be null");
        }
        return CombatAim.limit(current, CombatAim.predictedRotation(target, 1.0D, 0.0D, false), rotationSpeed.value());
    }

    private CombatTarget select(List<CombatTarget> allowed) {
        if (targetMode.value() == TargetMode.SINGLE && currentTargetId != null) {
            for (CombatTarget candidate : allowed) {
                if (candidate.id().equals(currentTargetId)) {
                    return candidate;
                }
            }
        }
        if (targetMode.value() == TargetMode.SWITCH && currentTargetId != null) {
            for (int index = 0; index < allowed.size(); index++) {
                if (allowed.get(index).id().equals(currentTargetId)) {
                    return allowed.get((index + 1) % allowed.size());
                }
            }
        }
        return allowed.getFirst();
    }

    private CombatTargetFilter.Options targetOptions() {
        // Requiring this locally observed line-of-sight fact prevents attacks through solid blocks.
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, range.value(), fieldOfView.value(), true);
    }

    @Override
    protected void onDisable() {
        currentTargetId = null;
        lastAttackTick = -1L;
    }
}
