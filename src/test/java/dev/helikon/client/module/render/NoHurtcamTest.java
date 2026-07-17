package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoHurtcamTest {
    @Test
    void hidesOnlyWhenEnabled() {
        NoHurtcam module = new NoHurtcam();

        assertEquals("no_hurtcam", module.id());
        assertFalse(module.defaultEnabled());
        assertFalse(module.hidesHurtCamera());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertTrue(module.hidesHurtCamera());

        registry.setEnabled(module, false);
        assertFalse(module.hidesHurtCamera());
    }
}
