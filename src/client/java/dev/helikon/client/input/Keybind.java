package dev.helikon.client.input;

import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * A serializable description of a module keybind. Input polling is deliberately
 * kept outside this value type so modules remain independent of Minecraft APIs.
 * (The GLFW references below are compile-time int constants, so this class
 * stays loadable without LWJGL at runtime.)
 */
public record Keybind(int keyCode, Activation activation) {
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

    public Keybind {
        Objects.requireNonNull(activation, "activation");
        if (!isValidKeyCode(keyCode)) {
            throw new IllegalArgumentException("Unsupported key code: " + keyCode);
        }
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

    public static Keybind unbound() {
        return new Keybind(UNBOUND_KEY, Activation.TOGGLE);
    }

    public boolean isBound() {
        return keyCode != UNBOUND_KEY;
    }

    public enum Activation {
        TOGGLE,
        HOLD,
        PRESS_ONCE
    }
}
