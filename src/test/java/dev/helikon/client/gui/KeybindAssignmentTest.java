package dev.helikon.client.gui;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindAssignmentTest {
    @Test
    void assignsAValidUnreservedKeyAndPreservesActivation() {
        TestModule module = new TestModule();
        module.setKeybind(new Keybind(GLFW.GLFW_KEY_R, Keybind.Activation.HOLD));
        KeybindAssignment assignment = new KeybindAssignment(keybind -> keybind.isKeyboard()
                && keybind.keyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT);
        assignment.begin(module);

        assertEquals(KeybindAssignment.Result.ASSIGNED, assignment.acceptKey(GLFW.GLFW_KEY_F));
        assertEquals(new Keybind(GLFW.GLFW_KEY_F, Keybind.Activation.HOLD), module.keybind());
        assertTrue(assignment.target().isEmpty());
    }

    @Test
    void reservedAndInvalidKeysKeepCaptureActive() {
        TestModule module = new TestModule();
        KeybindAssignment assignment = new KeybindAssignment(keybind -> keybind.isKeyboard()
                && keybind.keyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT);
        assignment.begin(module);

        assertEquals(KeybindAssignment.Result.RESERVED, assignment.acceptKey(GLFW.GLFW_KEY_RIGHT_SHIFT));
        assertTrue(assignment.isAssigning(module));
        assertEquals(KeybindAssignment.Result.INVALID, assignment.acceptKey(100));
        assertTrue(assignment.isAssigning(module));
        assertEquals(KeybindAssignment.Result.INVALID, assignment.acceptKey(Keybind.UNBOUND_KEY));
        assertTrue(assignment.isAssigning(module));
        assertFalse(module.keybind().isBound());
    }

    @Test
    void escapeCancelsAndBackspaceUnbinds() {
        TestModule module = new TestModule();
        module.setKeybind(new Keybind(GLFW.GLFW_KEY_R, Keybind.Activation.TOGGLE));
        KeybindAssignment assignment = new KeybindAssignment(keybind -> false);

        assignment.begin(module);
        assertEquals(KeybindAssignment.Result.CANCELLED, assignment.acceptKey(GLFW.GLFW_KEY_ESCAPE));
        assertEquals(new Keybind(GLFW.GLFW_KEY_R, Keybind.Activation.TOGGLE), module.keybind());

        assignment.begin(module);
        assertEquals(KeybindAssignment.Result.UNBOUND, assignment.acceptKey(GLFW.GLFW_KEY_BACKSPACE));
        assertFalse(module.keybind().isBound());
    }

    @Test
    void capturesMouseButtonAndInputModifiers() {
        TestModule module = new TestModule();
        KeybindAssignment assignment = new KeybindAssignment(keybind -> false);
        assignment.begin(module);

        assertEquals(KeybindAssignment.Result.ASSIGNED,
                assignment.acceptMouseButton(GLFW.GLFW_MOUSE_BUTTON_4, GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL));
        assertEquals(new Keybind(Keybind.InputType.MOUSE_BUTTON, GLFW.GLFW_MOUSE_BUTTON_4,
                java.util.Set.of(Keybind.Modifier.ALT, Keybind.Modifier.CONTROL), Keybind.Activation.TOGGLE),
                module.keybind());
    }

    @Test
    void reservedMouseButtonsKeepCaptureActive() {
        TestModule module = new TestModule();
        KeybindAssignment assignment = new KeybindAssignment(keybind -> keybind.isMouseButton()
                && keybind.keyCode() == GLFW.GLFW_MOUSE_BUTTON_4);
        assignment.begin(module);

        assertEquals(KeybindAssignment.Result.RESERVED,
                assignment.acceptMouseButton(GLFW.GLFW_MOUSE_BUTTON_4, 0));
        assertTrue(assignment.isAssigning(module));
    }

    private static final class TestModule extends Module {
        private TestModule() {
            super("test", "Test", "Keybind assignment test module.", ModuleCategory.MISCELLANEOUS,
                    false, Keybind.unbound());
        }
    }
}
