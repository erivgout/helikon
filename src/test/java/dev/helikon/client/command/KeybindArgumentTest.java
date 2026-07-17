package dev.helikon.client.command;

import dev.helikon.client.input.Keybind;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindArgumentTest {
    private static final KeyNameResolver KEYS = name -> "r".equals(name)
            ? OptionalInt.of(GLFW.GLFW_KEY_R)
            : OptionalInt.empty();

    @Test
    void parsesKeyboardModifierCombination() {
        KeybindArgument bind = KeybindArgument.parse("ctrl+shift+r", KEYS).orElseThrow();

        assertEquals(Keybind.InputType.KEYBOARD, bind.inputType());
        assertEquals(GLFW.GLFW_KEY_R, bind.code());
        assertEquals(java.util.Set.of(Keybind.Modifier.CONTROL, Keybind.Modifier.SHIFT), bind.modifiers());
    }

    @Test
    void parsesNumberedMouseButton() {
        KeybindArgument bind = KeybindArgument.parse("alt+mouse4", KEYS).orElseThrow();

        assertEquals(Keybind.InputType.MOUSE_BUTTON, bind.inputType());
        assertEquals(GLFW.GLFW_MOUSE_BUTTON_4, bind.code());
        assertEquals(java.util.Set.of(Keybind.Modifier.ALT), bind.modifiers());
    }

    @Test
    void rejectsDuplicateModifiersAndInvalidMouseButtons() {
        assertTrue(KeybindArgument.parse("ctrl+control+r", KEYS).isEmpty());
        assertTrue(KeybindArgument.parse("mouse9", KEYS).isEmpty());
    }
}
