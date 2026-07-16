package dev.helikon.client.input;

import java.util.Objects;

/** Edge-triggered, configurable panic key that never leaks through chat input. */
public final class PanicKeybindManager {
    private Keybind keybind = Keybind.unbound();
    private boolean previouslyDown;

    public Keybind keybind() {
        return keybind;
    }

    public void setKeybind(Keybind keybind) {
        this.keybind = Objects.requireNonNull(keybind, "keybind");
        previouslyDown = false;
    }

    /**
     * Processes a press edge. Open non-Helikon screens suppress it to avoid
     * typed-key activation; Helikon's own screens permit it so panic can close
     * them immediately.
     */
    public boolean tick(
            KeybindManager.KeyStateReader keys,
            boolean anyScreenOpen,
            boolean helikonScreenOpen,
            Runnable panicAction
    ) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(panicAction, "panicAction");
        if (!keybind.isBound()) {
            previouslyDown = false;
            return false;
        }

        boolean down = keys.isDown(keybind.keyCode());
        boolean pressed = down && !previouslyDown;
        previouslyDown = down;
        if (pressed && (!anyScreenOpen || helikonScreenOpen)) {
            panicAction.run();
            return true;
        }
        return false;
    }
}
