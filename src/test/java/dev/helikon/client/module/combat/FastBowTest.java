package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastBowTest {
    @Test
    void requiresEnabledBowUseAndConfiguredDraw() {
        FastBow module = new FastBow();
        assertFalse(module.shouldRelease(true, true, 20, false));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertFalse(module.shouldRelease(false, true, 20, false));
        assertFalse(module.shouldRelease(true, true, 3, false));
        assertFalse(module.shouldRelease(true, true, 20, true));
        assertTrue(module.shouldRelease(true, true, 4, false));
    }
}
