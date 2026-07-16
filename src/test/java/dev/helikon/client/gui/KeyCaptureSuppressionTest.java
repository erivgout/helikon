package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyCaptureSuppressionTest {
    @Test
    void consumesKeyRepeatsAndCharactersUntilCapturedKeyIsReleased() {
        KeyCaptureSuppression suppression = new KeyCaptureSuppression();
        suppression.begin(82);

        assertTrue(suppression.consumesKeyPress(82));
        assertFalse(suppression.consumesKeyPress(70));
        assertTrue(suppression.consumesCharacterInput());
        assertFalse(suppression.release(70));
        assertTrue(suppression.consumesCharacterInput());
        assertTrue(suppression.release(82));
        assertFalse(suppression.consumesKeyPress(82));
        assertFalse(suppression.consumesCharacterInput());
    }

    @Test
    void terminalUnbindAndCancelKeysAreAlsoSuppressedUntilRelease() {
        KeyCaptureSuppression suppression = new KeyCaptureSuppression();

        suppression.begin(GLFW.GLFW_KEY_BACKSPACE);
        assertTrue(suppression.consumesKeyPress(GLFW.GLFW_KEY_BACKSPACE));
        assertTrue(suppression.release(GLFW.GLFW_KEY_BACKSPACE));

        suppression.begin(GLFW.GLFW_KEY_DELETE);
        assertTrue(suppression.consumesKeyPress(GLFW.GLFW_KEY_DELETE));
        assertTrue(suppression.release(GLFW.GLFW_KEY_DELETE));

        suppression.begin(GLFW.GLFW_KEY_ESCAPE);
        assertTrue(suppression.consumesKeyPress(GLFW.GLFW_KEY_ESCAPE));
        assertTrue(suppression.release(GLFW.GLFW_KEY_ESCAPE));
    }
}
