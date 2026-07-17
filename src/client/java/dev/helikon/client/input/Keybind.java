package dev.helikon.client.input;

import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A serializable description of a module keybind. Input polling is deliberately
 * kept outside this value type so modules remain independent of Minecraft APIs.
 * (The GLFW references below are compile-time int constants, so this class
 * stays loadable without LWJGL at runtime.)
 */
public record Keybind(InputType inputType, int keyCode, Set<Modifier> modifiers, Activation activation) {
    public static final int UNBOUND_KEY = -1;

    /** The inclusive ranges of defined GLFW keyboard key tokens, without the gaps between them. */
    private static final int[][] KEYBOARD_KEY_RANGES = {
            {GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_SPACE},
            {GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_APOSTROPHE},
            {GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_9},
            {GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_SEMICOLON},
            {GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_EQUAL},
            {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_RIGHT_BRACKET},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT},
            {GLFW.GLFW_KEY_WORLD_1, GLFW.GLFW_KEY_WORLD_2},
            {GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_END},
            {GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_PAUSE},
            {GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F25},
            {GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_EQUAL},
            {GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_MENU},
    };

    /** GLFW exposes eight mouse buttons, numbered from zero. */
    private static final int MIN_MOUSE_BUTTON = GLFW.GLFW_MOUSE_BUTTON_1;
    private static final int MAX_MOUSE_BUTTON = GLFW.GLFW_MOUSE_BUTTON_LAST;

    public Keybind {
        Objects.requireNonNull(inputType, "inputType");
        modifiers = modifiers == null || modifiers.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(modifiers));
        Objects.requireNonNull(activation, "activation");
        if (!isValidCode(inputType, keyCode)) {
            throw new IllegalArgumentException("Unsupported " + inputType.name().toLowerCase() + " code: " + keyCode);
        }
    }

    /** Compatibility constructor for the keyboard-only binds used by older configuration files. */
    public Keybind(int keyCode, Activation activation) {
        this(InputType.KEYBOARD, keyCode, Set.of(), activation);
    }

    public Keybind(InputType inputType, int keyCode, Activation activation) {
        this(inputType, keyCode, Set.of(), activation);
    }

    /** Whether the code is the unbound sentinel or a defined GLFW keyboard key token. */
    public static boolean isValidKeyCode(int keyCode) {
        if (keyCode == UNBOUND_KEY) {
            return true;
        }
        for (int[] range : KEYBOARD_KEY_RANGES) {
            if (keyCode >= range[0] && keyCode <= range[1]) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidMouseButton(int button) {
        return button >= MIN_MOUSE_BUTTON && button <= MAX_MOUSE_BUTTON;
    }

    public static boolean isValidCode(InputType inputType, int code) {
        return inputType == InputType.KEYBOARD ? isValidKeyCode(code) : isValidMouseButton(code);
    }

    public static Keybind unbound() {
        return new Keybind(UNBOUND_KEY, Activation.TOGGLE);
    }

    public boolean isBound() {
        return keyCode != UNBOUND_KEY;
    }

    public boolean isKeyboard() {
        return inputType == InputType.KEYBOARD;
    }

    public boolean isMouseButton() {
        return inputType == InputType.MOUSE_BUTTON;
    }

    public enum InputType {
        KEYBOARD,
        MOUSE_BUTTON
    }

    /** Modifier groups intentionally cover either physical left/right key. */
    public enum Modifier {
        SHIFT,
        CONTROL,
        ALT,
        SUPER
    }

    public enum Activation {
        TOGGLE,
        HOLD,
        PRESS_ONCE
    }
}
