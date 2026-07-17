package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XRayRenderStateTest {
    @Test
    void hidesOnlyNonTargetsWhileActive() {
        XRayRenderState active = new XRayRenderState(true, Set.of("minecraft:diamond_ore"), 0.85F);

        assertFalse(active.hides("minecraft:diamond_ore"));
        assertTrue(active.hides("minecraft:stone"));
        assertFalse(XRayRenderState.disabled().hides("minecraft:stone"));
    }

    @Test
    void rejectsUnsafeOpacity() {
        assertThrows(IllegalArgumentException.class, () -> new XRayRenderState(true, Set.of("minecraft:stone"), 0.09F));
        assertThrows(IllegalArgumentException.class, () -> new XRayRenderState(true, Set.of("minecraft:stone"), Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new XRayRenderState(true, Set.of(), 1.0F));
    }
}
