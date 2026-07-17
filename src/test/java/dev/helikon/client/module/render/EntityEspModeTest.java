package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityEspModeTest {
    @Test
    void onlyGlowAndShaderUseTheNativeOutline() {
        assertFalse(EntityEspMode.OUTLINE.usesNativeOutline());
        assertFalse(EntityEspMode.BOX.usesNativeOutline());
        assertTrue(EntityEspMode.GLOW.usesNativeOutline());
        assertTrue(EntityEspMode.SHADER.usesNativeOutline());
    }

    @Test
    void onlyShaderOverridesTheOutlineColor() {
        assertFalse(EntityEspMode.OUTLINE.usesShaderColor());
        assertFalse(EntityEspMode.BOX.usesShaderColor());
        assertFalse(EntityEspMode.GLOW.usesShaderColor());
        assertTrue(EntityEspMode.SHADER.usesShaderColor());
    }
}
