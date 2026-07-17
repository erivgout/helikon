package dev.helikon.client.gui;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Screen-independent in-GUI module-key capture. The GUI key is injected as a
 * reserved predicate so the same safety rule as the local command is reused.
 */
public final class KeybindAssignment {
    private final Predicate<Keybind> reservedKeybinds;

    private Module target;

    public KeybindAssignment(Predicate<Keybind> reservedKeybinds) {
        this.reservedKeybinds = Objects.requireNonNull(reservedKeybinds, "reservedKeybinds");
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
        return accept(Keybind.InputType.KEYBOARD, keyCode, 0);
    }

    /** Captures a key with the modifier mask delivered by Minecraft's input event. */
    public Result acceptKey(int keyCode, int modifierMask) {
        return accept(Keybind.InputType.KEYBOARD, keyCode, modifierMask);
    }

    /** Captures a mouse button with the modifier mask delivered by Minecraft's input event. */
    public Result acceptMouseButton(int button, int modifierMask) {
        return accept(Keybind.InputType.MOUSE_BUTTON, button, modifierMask);
    }

    private Result accept(Keybind.InputType inputType, int code, int modifierMask) {
        Module selected = target;
        if (selected == null) {
            return Result.IGNORED;
        }
        if (inputType == Keybind.InputType.KEYBOARD && code == GLFW.GLFW_KEY_ESCAPE) {
            target = null;
            return Result.CANCELLED;
        }
        if (inputType == Keybind.InputType.KEYBOARD
                && (code == GLFW.GLFW_KEY_BACKSPACE || code == GLFW.GLFW_KEY_DELETE)) {
            selected.setKeybind(Keybind.unbound());
            target = null;
            return Result.UNBOUND;
        }
        if (code == Keybind.UNBOUND_KEY || !Keybind.isValidCode(inputType, code)) {
            return Result.INVALID;
        }
        Set<Keybind.Modifier> modifiers = modifiersFromMask(modifierMask);
        Keybind next = new Keybind(inputType, code, modifiers, selected.keybind().activation());
        if (reservedKeybinds.test(next)) {
            return Result.RESERVED;
        }

        selected.setKeybind(next);
        target = null;
        return Result.ASSIGNED;
    }

    private static Set<Keybind.Modifier> modifiersFromMask(int modifierMask) {
        EnumSet<Keybind.Modifier> modifiers = EnumSet.noneOf(Keybind.Modifier.class);
        if ((modifierMask & GLFW.GLFW_MOD_SHIFT) != 0) {
            modifiers.add(Keybind.Modifier.SHIFT);
        }
        if ((modifierMask & GLFW.GLFW_MOD_CONTROL) != 0) {
            modifiers.add(Keybind.Modifier.CONTROL);
        }
        if ((modifierMask & GLFW.GLFW_MOD_ALT) != 0) {
            modifiers.add(Keybind.Modifier.ALT);
        }
        if ((modifierMask & GLFW.GLFW_MOD_SUPER) != 0) {
            modifiers.add(Keybind.Modifier.SUPER);
        }
        return Set.copyOf(modifiers);
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
