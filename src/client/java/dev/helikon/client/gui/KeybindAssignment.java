package dev.helikon.client.gui;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Screen-independent in-GUI module-key capture. The GUI key is injected as a
 * reserved predicate so the same safety rule as the local command is reused.
 */
public final class KeybindAssignment {
    private final IntPredicate reservedKeys;

    private Module target;

    public KeybindAssignment(IntPredicate reservedKeys) {
        this.reservedKeys = Objects.requireNonNull(reservedKeys, "reservedKeys");
    }

    public Optional<Module> target() {
        return Optional.ofNullable(target);
    }

    public boolean isAssigning(Module module) {
        return target == module;
    }

    public void begin(Module module) {
        target = Objects.requireNonNull(module, "module");
    }

    /** Cancels any pending capture without changing the module's keybind. */
    public void cancel() {
        target = null;
    }

    /**
     * Handles one keyboard token. Escape cancels capture, Backspace/Delete
     * unbinds, invalid/reserved tokens leave capture active, and valid keys
     * preserve the module's current activation mode.
     */
    public Result acceptKey(int keyCode) {
        Module selected = target;
        if (selected == null) {
            return Result.IGNORED;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            target = null;
            return Result.CANCELLED;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            selected.setKeybind(Keybind.unbound());
            target = null;
            return Result.UNBOUND;
        }
        if (keyCode == Keybind.UNBOUND_KEY || !Keybind.isValidKeyCode(keyCode)) {
            return Result.INVALID;
        }
        if (reservedKeys.test(keyCode)) {
            return Result.RESERVED;
        }

        selected.setKeybind(new Keybind(keyCode, selected.keybind().activation()));
        target = null;
        return Result.ASSIGNED;
    }

    public enum Result {
        IGNORED,
        ASSIGNED,
        UNBOUND,
        CANCELLED,
        RESERVED,
        INVALID
    }
}
