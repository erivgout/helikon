package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

import java.util.Objects;

/** Keeps the forward movement input active using only the user's local key map. */
public final class AutoWalk extends Module {
    private final BooleanSetting continueForward;
    private final BooleanSetting stopOnGui;
    private final BooleanSetting allowSteering;

    public AutoWalk() {
        super("auto_walk", "AutoWalk", "Continues local forward movement while enabled.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        continueForward = addSetting(new BooleanSetting("continue_forward", "Continue forward",
                "Force the local forward input while this module is enabled.", true));
        stopOnGui = addSetting(new BooleanSetting("stop_on_gui", "Stop on GUI",
                "Do not force movement while any Minecraft screen is open.", true));
        allowSteering = addSetting(new BooleanSetting("allow_steering", "Allow steering",
                "Keep the user's side and backward movement inputs active.", true));
    }

    /** Applies the configured local movement policy without touching Minecraft classes. */
    public MovementInput apply(MovementInput input, boolean screenOpen) {
        MovementInput current = Objects.requireNonNull(input, "input");
        if (!isEnabled() || !continueForward.value() || (screenOpen && stopOnGui.value())) {
            return current;
        }

        if (allowSteering.value()) {
            return new MovementInput(true, current.backward(), current.left(), current.right(),
                    current.jump(), current.shift(), current.sprint());
        }
        return new MovementInput(true, false, false, false,
                current.jump(), current.shift(), current.sprint());
    }
}
