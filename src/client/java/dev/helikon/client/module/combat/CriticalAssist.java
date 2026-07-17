package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Times an already-held normal attack only during Minecraft's ordinary falling critical conditions. */
public final class CriticalAssist extends Module {
    public record Context(boolean attackHeld, boolean attackReady, boolean onGround, boolean inWater,
                          boolean onClimbable, boolean fallFlying, double fallDistance, double verticalVelocity) {
        public Context {
            if (!Double.isFinite(fallDistance) || fallDistance < 0.0D || !Double.isFinite(verticalVelocity)) {
                throw new IllegalArgumentException("critical context facts are invalid");
            }
        }
    }

    private final NumberSetting delayTicks;
    private final BooleanSetting excludeFriends;
    private long lastAttackTick = -1L;

    public CriticalAssist() {
        super("critical_assist", "CriticalAssist", "Times normal held attacks only in legitimate falling critical windows.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal attacks.",
                4.0D, 2.0D, 40.0D));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never attack locally listed friends.", true));
    }

    public boolean shouldAttack(long tick, CombatTarget target, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("critical inputs are invalid");
        }
        boolean legitimateCriticalWindow = context.attackHeld() && context.attackReady() && !context.onGround()
                && !context.inWater() && !context.onClimbable() && !context.fallFlying()
                && context.fallDistance() > 0.0D && context.verticalVelocity() < 0.0D;
        CombatTargetFilter.Options options = new CombatTargetFilter.Options(true, true, false, excludeFriends.value(),
                true, 6.0D, 180.0D, true);
        if (!isEnabled() || !legitimateCriticalWindow || !CombatTargetFilter.allows(target, options)
                || (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value()))) {
            return false;
        }
        lastAttackTick = tick;
        return true;
    }

    @Override
    protected void onDisable() {
        lastAttackTick = -1L;
    }
}
