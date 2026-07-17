package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures collision-safe exit searching beyond a thin obstructing wall. */
public final class Phase extends Module {
    private final NumberSetting maximumDistance;
    private final NumberSetting stepDistance;
    private final IntegerSetting cooldownTicks;
    private long lastPhaseTick = Long.MIN_VALUE;

    public Phase() {
        super("phase", "Phase", "Attempts a short local move through a thin wall into collision-free space.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        maximumDistance = addSetting(new NumberSetting("maximum_distance", "Maximum distance",
                "Farthest exit position checked through the wall.", 1.5D, 0.4D, 3.0D));
        stepDistance = addSetting(new NumberSetting("step_distance", "Step distance",
                "Spacing between bounded exit checks.", 0.1D, 0.05D, 0.25D));
        cooldownTicks = addSetting(new IntegerSetting("cooldown_ticks", "Cooldown ticks",
                "Minimum ticks between phase attempts.", 10, 1, 100));
    }

    public boolean canAttempt(long tick, boolean horizontalCollision, boolean moving) {
        return isEnabled() && horizontalCollision && moving
                && (lastPhaseTick == Long.MIN_VALUE || tick - lastPhaseTick >= cooldownTicks.value());
    }

    public double maximumDistance() {
        return maximumDistance.value();
    }

    public double stepDistance() {
        return stepDistance.value();
    }

    public void markAttempt(long tick) {
        lastPhaseTick = tick;
    }

    public void onContextLost() {
        lastPhaseTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }
}
