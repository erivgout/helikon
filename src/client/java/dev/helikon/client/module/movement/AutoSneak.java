package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindInputConsumer;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;

import java.util.Objects;

/** Applies a local sneaking input policy without retaining a key state after disable. */
public final class AutoSneak extends Module implements KeybindInputConsumer {
    private final EnumSetting<AutoSneakMode> mode;

    public AutoSneak() {
        super("auto_sneak", "AutoSneak", "Applies a local sneak input policy while enabled.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        mode = addSetting(new EnumSetting<>("mode", "Mode", "Choose toggle, bound-key hold, or moving edge safety.",
                AutoSneakMode.class, AutoSneakMode.TOGGLE));
    }

    /** Applies the selected policy to a freshly polled local movement snapshot. */
    public MovementInput apply(MovementInput input, boolean screenOpen, boolean moduleKeyDown) {
        MovementInput current = Objects.requireNonNull(input, "input");
        if (!isEnabled() || screenOpen || !shouldSneak(current, moduleKeyDown)) {
            return current;
        }
        return new MovementInput(current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), true, current.sprint());
    }

    private boolean shouldSneak(MovementInput input, boolean moduleKeyDown) {
        return switch (mode.value()) {
            case TOGGLE -> true;
            case HOLD -> keybind().isBound() && moduleKeyDown;
            case EDGE_ONLY -> input.isMoving();
        };
    }

    /** Hold mode reserves the configured key for local sneak input, not module activation. */
    @Override
    public boolean consumesKeybindInput() {
        return mode.value() == AutoSneakMode.HOLD;
    }
}
