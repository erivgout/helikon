package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Performs a normal local attack only when the crosshair already rests on an eligible target. */
public final class TriggerBot extends Module {
    private final NumberSetting delayTicks;
    private final BooleanSetting weaponRequired;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private long lastAttackTick = -1L;

    public TriggerBot() {
        super("trigger_bot", "TriggerBot", "Uses normal attacks only for an eligible crosshair target.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal attacks.",
                4.0D, 2.0D, 40.0D));
        weaponRequired = addSetting(new BooleanSetting("weapon_required", "Weapon required",
                "Require a normal melee weapon in the selected hotbar slot.", true));
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
    }

    public boolean shouldAttack(long tick, CombatTarget target, boolean attackReady, boolean holdingWeapon) {
        if (tick < 0L) {
            throw new IllegalArgumentException("tick must not be negative");
        }
        if (!isEnabled() || !attackReady || (weaponRequired.value() && !holdingWeapon)
                || !CombatTargetFilter.allows(target, targetOptions())) {
            return false;
        }
        if (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value())) {
            return false;
        }
        lastAttackTick = tick;
        return true;
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, 6.0D, 180.0D, true);
    }

    @Override
    protected void onDisable() {
        lastAttackTick = -1L;
    }
}
