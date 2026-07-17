package dev.helikon.client.input;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindTest {
    @Test
    void unboundKeybindIsValidAndNotBound() {
        assertFalse(Keybind.unbound().isBound());
        assertTrue(Keybind.isValidKeyCode(Keybind.UNBOUND_KEY));
    }

    @Test
    void keyboardRangeCodesAreAccepted() {
        assertTrue(new Keybind(82, Keybind.Activation.TOGGLE).isBound());
        assertTrue(new Keybind(344, Keybind.Activation.HOLD).isBound());
    }

    @Test
    void codesOutsideTheKeyboardRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Keybind(0, Keybind.Activation.TOGGLE));
        assertThrows(IllegalArgumentException.class, () -> new Keybind(-5, Keybind.Activation.TOGGLE));
        assertThrows(IllegalArgumentException.class, () -> new Keybind(999999, Keybind.Activation.TOGGLE));
        assertFalse(Keybind.isValidKeyCode(999999));
    }

    @Test
    void codesInGlfwTokenGapsAreRejected() {
        // 100 and 200 sit between defined GLFW keyboard tokens; no physical key produces them.
        assertFalse(Keybind.isValidKeyCode(100));
        assertFalse(Keybind.isValidKeyCode(200));
        assertThrows(IllegalArgumentException.class, () -> new Keybind(100, Keybind.Activation.TOGGLE));
    }

    @Test
    void acceptsMouseButtonsAndImmutableModifierCombinations() {
        Keybind bind = new Keybind(Keybind.InputType.MOUSE_BUTTON, GLFW.GLFW_MOUSE_BUTTON_4,
                Set.of(Keybind.Modifier.CONTROL, Keybind.Modifier.SHIFT), Keybind.Activation.HOLD);

        assertTrue(bind.isMouseButton());
        assertTrue(bind.modifiers().contains(Keybind.Modifier.CONTROL));
        assertThrows(UnsupportedOperationException.class, () -> bind.modifiers().add(Keybind.Modifier.ALT));
    }

    @Test
    void mouseButtonsOutsideTheDefinedRangeAreRejected() {
        assertFalse(Keybind.isValidMouseButton(-1));
        assertFalse(Keybind.isValidMouseButton(GLFW.GLFW_MOUSE_BUTTON_LAST + 1));
        assertThrows(IllegalArgumentException.class, () -> new Keybind(
                Keybind.InputType.MOUSE_BUTTON, GLFW.GLFW_MOUSE_BUTTON_LAST + 1, Keybind.Activation.TOGGLE));
    }
}
