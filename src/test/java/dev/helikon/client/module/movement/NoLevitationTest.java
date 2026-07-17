package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoLevitationTest {
    @Test
    void suppressesOnlyUpwardLocalLevitationMotion() {
        NoLevitation module = new NoLevitation();

        assertEquals("no_levitation", module.id());
        assertFalse(module.defaultEnabled());
        assertTrue(module.suppressedVerticalVelocity(true, false, 0.4D).isEmpty());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(0.0D, module.suppressedVerticalVelocity(true, false, 0.4D).orElseThrow());
        assertTrue(module.suppressedVerticalVelocity(true, false, 0.0D).isEmpty());
        assertTrue(module.suppressedVerticalVelocity(true, false, -0.2D).isEmpty());
        assertTrue(module.suppressedVerticalVelocity(false, false, 0.4D).isEmpty());
        assertTrue(module.suppressedVerticalVelocity(true, true, 0.4D).isEmpty());
    }
}
