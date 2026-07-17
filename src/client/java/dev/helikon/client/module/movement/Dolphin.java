package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

import java.util.Objects;

/** Repeatedly requests ordinary Jump input while moving through water. */
public final class Dolphin extends Module {
    private final BooleanSetting forwardOnly;

    public Dolphin() {
        super("dolphin", "Dolphin", "Requests normal jumps while moving through water.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        forwardOnly = addSetting(new BooleanSetting("forward_only", "Forward only",
                "Require forward movement instead of allowing any movement direction.", true));
    }

    /** Returns whether this input tick should include ordinary Jump input. */
    public boolean shouldJump(DolphinContext context) {
        DolphinContext current = Objects.requireNonNull(context, "context");
        return isEnabled() && !current.screenOpen() && current.inWater()
                && (forwardOnly.value() ? current.movingForward() : current.moving())
                && !current.sneaking() && !current.passenger() && !current.abilityFlying() && !current.fallFlying();
    }
}
