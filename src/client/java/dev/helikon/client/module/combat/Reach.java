package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/**
 * Extends the player's own melee combat: while Attack is held, it requests one
 * ordinary Minecraft attack for the target being aimed at just beyond vanilla
 * melee distance. It never modifies the reach attribute and never builds a
 * packet; the request travels on Minecraft's normal attack path, so the server
 * validates reach and may reject, correct, or ignore an out-of-range attack.
 */
public final class Reach extends Module {
    /**
     * Vanilla default {@code minecraft:entity_interaction_range} on Minecraft
     * 26.2. Targets at or within this distance are left to Minecraft's own
     * crosshair interaction; this module only adds distance beyond it.
     */
    private static final double VANILLA_ENTITY_INTERACTION_RANGE = 3.0D;

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting reach;
    private final NumberSetting fieldOfView;
    private final NumberSetting delayTicks;
    private long lastAttackTick = -1L;

    public Reach() {
        super("reach", "Reach", "Requests a normal attack for a target you aim at just beyond vanilla melee range.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
        reach = addSetting(new NumberSetting("reach", "Reach",
                "Maximum local attack distance requested while Attack is held. The server still validates reach.",
                4.0D, VANILLA_ENTITY_INTERACTION_RANGE, 6.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view",
                "Maximum view angle to the aimed target.", 6.0D, 1.0D, 30.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Attack delay",
                "Minimum ticks between reach attacks.", 4.0D, 2.0D, 40.0D));
    }

    /**
     * Selects at most one eligible target that lies beyond vanilla melee range
     * but within the configured reach and view angle. Returns empty when the
     * module is idle, the user is not attacking, the normal attack is not ready,
     * Minecraft already has a crosshair target, or no target qualifies.
     */
    public Optional<CombatTarget> reachAttack(long tick, List<CombatTarget> candidates, boolean attackHeld,
                                              boolean attackReady, boolean vanillaHasCrosshairTarget) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("Reach inputs are invalid");
        }
        if (!isEnabled() || !attackHeld || !attackReady || vanillaHasCrosshairTarget) {
            return Optional.empty();
        }
        if (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value())) {
            return Optional.empty();
        }
        List<CombatTarget> beyondVanilla = candidates.stream()
                .filter(target -> target != null && target.distance() > VANILLA_ENTITY_INTERACTION_RANGE)
                .toList();
        List<CombatTarget> allowed = CombatTargetFilter.ordered(beyondVanilla, targetOptions(),
                CombatTargetFilter.Priority.ANGLE);
        if (allowed.isEmpty()) {
            return Optional.empty();
        }
        lastAttackTick = tick;
        return Optional.of(allowed.getFirst());
    }

    private CombatTargetFilter.Options targetOptions() {
        // Line of sight is required so the module never reaches through solid blocks.
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, reach.value(), fieldOfView.value(), true);
    }

    @Override
    protected void onDisable() {
        lastAttackTick = -1L;
    }
}
