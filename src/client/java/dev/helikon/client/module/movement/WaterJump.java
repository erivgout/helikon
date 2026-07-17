package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.Objects;

/** Requests only ordinary local jump input while exiting water at a safe observed one-block step. */
public final class WaterJump extends Module {
    public WaterJump() {
        super("water_jump", "WaterJump", "Requests a normal jump at a suitable local water edge.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
    }

    /** Requires water, forward movement, a loaded solid step, clear headroom, and no open screen. */
    public boolean shouldJump(WaterJumpContext context) {
        WaterJumpContext current = Objects.requireNonNull(context, "context");
        return isEnabled() && !current.screenOpen() && current.inWater() && current.movingForward()
                && current.solidStepAhead() && current.headroomClear();
    }
}
