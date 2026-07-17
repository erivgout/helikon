package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiTotemAnimationTest {
    @Test
    void suppressesOnlyEnabledDeathProtectionActivations() {
        AntiTotemAnimation module = new AntiTotemAnimation();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertFalse(module.shouldSuppressItemActivation(true));
        assertFalse(module.shouldSuppressItemActivation(false));

        registry.setEnabled(module, true);
        assertTrue(module.shouldSuppressItemActivation(true));
        assertFalse(module.shouldSuppressItemActivation(false));

        registry.setEnabled(module, false);
        assertFalse(module.shouldSuppressItemActivation(true));
    }
}
