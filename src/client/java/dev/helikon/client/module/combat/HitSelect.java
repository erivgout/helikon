package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Selects the moment to release the user's already-held normal attack so it lands with a better
 * charge, and optionally only during a falling critical or sprint-knockback window. The decision is
 * Minecraft-free and unit-tested; a thin adapter feeds it observed local facts and invokes only
 * Minecraft's ordinary attack. The server stays authoritative and may still reject or correct a hit.
 */
public final class HitSelect extends Module {
    /** Local, version-independent facts describing the current attack opportunity. */
    public record Context(boolean attackHeld, double attackStrengthScale, boolean sprinting, boolean onGround,
                          boolean inWater, boolean onClimbable, boolean fallFlying, double fallDistance,
                          double verticalVelocity, boolean holdingWeapon) {
        public Context {
            if (!Double.isFinite(attackStrengthScale) || attackStrengthScale < 0.0D
                    || !Double.isFinite(fallDistance) || fallDistance < 0.0D || !Double.isFinite(verticalVelocity)) {
                throw new IllegalArgumentException("hit-select context facts are invalid");
            }
        }
    }

    private final NumberSetting chargeThreshold;
    private final BooleanSetting requireCritical;
    private final BooleanSetting requireSprint;
    private final BooleanSetting weaponRequired;
    private final NumberSetting delayTicks;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private long lastAttackTick = -1L;

    public HitSelect() {
        super("hit_select", "HitSelect",
                "Times an already-held normal attack for a stronger charge, critical, or knockback window.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        chargeThreshold = addSetting(new NumberSetting("charge_threshold", "Charge threshold",
                "Minimum normal attack-strength fraction (1.0 = full charge for the best trade; lower is faster but weaker).",
                0.95D, 0.1D, 1.0D));
        requireCritical = addSetting(new BooleanSetting("require_critical", "Require critical",
                "Only release inside an ordinary falling critical window.", false));
        requireSprint = addSetting(new BooleanSetting("require_sprint", "Require sprint",
                "Only release while sprinting so the hit keeps Minecraft's sprint knockback.", false));
        weaponRequired = addSetting(new BooleanSetting("weapon_required", "Weapon required",
                "Require a normal melee weapon in the selected hotbar slot.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal attacks.",
                4.0D, 2.0D, 40.0D));
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
    }

    /** Decides whether the currently held attack should be released this tick for the crosshair target. */
    public boolean shouldAttack(long tick, CombatTarget target, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("hit-select inputs are invalid");
        }
        if (!isEnabled() || !context.attackHeld() || context.attackStrengthScale() < chargeThreshold.value()
                || (weaponRequired.value() && !context.holdingWeapon())
                || (requireSprint.value() && !context.sprinting())
                || (requireCritical.value() && !inCriticalWindow(context))
                || !CombatTargetFilter.allows(target, targetOptions())) {
            return false;
        }
        if (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value())) {
            return false;
        }
        lastAttackTick = tick;
        return true;
    }

    private static boolean inCriticalWindow(Context context) {
        return !context.onGround() && !context.inWater() && !context.onClimbable() && !context.fallFlying()
                && context.fallDistance() > 0.0D && context.verticalVelocity() < 0.0D;
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
