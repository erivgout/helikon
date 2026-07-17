package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Times an ordinary mace attack during a real falling smash window. It never
 * fabricates fall distance; Minecraft's server remains authoritative over the hit.
 */
public final class MaceDmg extends Module {
    public record Context(boolean maceHeld, boolean attackHeld, double attackCharge, boolean onGround,
                          boolean inFluid, boolean onClimbable, boolean fallFlying, boolean passenger,
                          double fallDistance, double verticalVelocity) {
        public Context {
            if (!Double.isFinite(attackCharge) || attackCharge < 0.0D || attackCharge > 1.0D
                    || !Double.isFinite(fallDistance) || fallDistance < 0.0D
                    || !Double.isFinite(verticalVelocity)) {
                throw new IllegalArgumentException("MaceDMG context facts are invalid");
            }
        }
    }

    private final NumberSetting minimumFallDistance;
    private final NumberSetting minimumAttackCharge;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting excludeFriends;
    private final IntegerSetting delayTicks;
    private long lastAttackTick = -1L;

    public MaceDmg() {
        super("mace_dmg", "MaceDMG",
                "Times a normal mace attack once a real fall has accumulated enough smash damage.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        minimumFallDistance = addSetting(new NumberSetting("minimum_fall_distance", "Minimum fall distance",
                "Observed real fall distance required before requesting the mace attack.",
                3.0D, 1.6D, 50.0D));
        minimumAttackCharge = addSetting(new NumberSetting("minimum_attack_charge", "Minimum attack charge",
                "Minimum vanilla attack-strength fraction required before attacking.",
                0.9D, 0.0D, 1.0D));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key",
                "Only request the timed smash while the physical Attack key is held.", true));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never attack a locally listed friend.", true));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay ticks",
                "Minimum client ticks between timed mace attack requests.", 5, 1, 40));
    }

    public boolean shouldAttack(long tick, CombatTarget target, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("MaceDMG inputs are invalid");
        }
        boolean smashWindow = context.maceHeld()
                && (!requireAttackKey.value() || context.attackHeld())
                && context.attackCharge() >= minimumAttackCharge.value()
                && !context.onGround()
                && !context.inFluid()
                && !context.onClimbable()
                && !context.fallFlying()
                && !context.passenger()
                && context.fallDistance() >= minimumFallDistance.value()
                && context.verticalVelocity() < 0.0D;
        CombatTargetFilter.Options options = new CombatTargetFilter.Options(
                true, true, false, excludeFriends.value(), true, 6.0D, 180.0D, true);
        if (!isEnabled() || !smashWindow || !CombatTargetFilter.allows(target, options)
                || (lastAttackTick >= 0L && tick - lastAttackTick < delayTicks.value())) {
            return false;
        }
        lastAttackTick = tick;
        return true;
    }

    public void onContextLost() {
        lastAttackTick = -1L;
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }
}
