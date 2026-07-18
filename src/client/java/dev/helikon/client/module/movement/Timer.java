package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Supplies a bounded local client-timer multiplier; multiplayer remains server-authoritative. */
public final class Timer extends Module {
    private final NumberSetting tickMultiplier;
    private final BooleanSetting diggingOnly;
    private double diggingStepCredit;

    public Timer() {
        super("timer", "Timer", "Applies a bounded local client tick multiplier with a multiplayer warning.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        tickMultiplier = addSetting(new NumberSetting("tick_multiplier", "Tick multiplier",
                "Local client rate multiplier; servers remain authoritative.", 1.0D, 0.50D, 10.0D));
        diggingOnly = addSetting(new BooleanSetting("digging_only", "Digging only",
                "Apply acceleration only while holding Attack on an active block break.", false));
    }

    public float multiplier() {
        return isEnabled() && !diggingOnly.value() ? tickMultiplier.value().floatValue() : 1.0F;
    }

    public boolean usesDiggingOnlyMode() {
        return isEnabled() && diggingOnly.value();
    }

    /** Additional ordinary destroy-progress calls after vanilla's first active digging step. */
    public int extraDiggingSteps(boolean activeDigging) {
        if (!usesDiggingOnlyMode() || !activeDigging || tickMultiplier.value() <= 1.0D) {
            diggingStepCredit = 0.0D;
            return 0;
        }
        diggingStepCredit += tickMultiplier.value() - 1.0D;
        int steps = Math.min(4, (int) Math.floor(diggingStepCredit + 1.0E-9D));
        diggingStepCredit -= steps;
        return steps;
    }

    @Override
    protected void onDisable() {
        diggingStepCredit = 0.0D;
    }
}
