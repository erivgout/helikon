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

/** Selects one nearby visible target for an ordinary attack while the user holds Attack. */
public final class ClickAura extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting delayTicks;
    private long lastAttackTick = -1L;

    public ClickAura() {
        super("click_aura", "ClickAura", "Attacks one nearby eligible target only while Attack is held.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local target distance.", 3.0D, 1.0D, 6.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Attack delay", "Minimum ticks between normal attacks.",
                4.0D, 2.0D, 40.0D));
    }

    /** Returns the nearest eligible target only for a held normal attack and a ready vanilla cooldown. */
    public Optional<CombatTarget> nextAttack(long tick, List<CombatTarget> candidates, boolean attackHeld,
                                              boolean attackReady) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("click-aura inputs are invalid");
        }
        if (!isEnabled() || !attackHeld || !attackReady
                || (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value()))) {
            return Optional.empty();
        }
        List<CombatTarget> allowed = CombatTargetFilter.ordered(candidates, targetOptions(),
                CombatTargetFilter.Priority.DISTANCE);
        if (allowed.isEmpty()) {
            return Optional.empty();
        }
        lastAttackTick = tick;
        return Optional.of(allowed.getFirst());
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, range.value(), 180.0D, true);
    }

    @Override
    protected void onDisable() {
        lastAttackTick = -1L;
    }
}
