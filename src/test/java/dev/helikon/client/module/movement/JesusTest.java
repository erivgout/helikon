package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JesusTest {
    @Test
    void holdsExactWaterHeightWithoutUpwardBobbing() {
        Jesus module = new Jesus();
        assertFalse(module.surfaceHeight(true, false, false, false, false, false,
                63.7D, 64.0D, -0.04D).isPresent());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(64.0D, module.surfaceHeight(true, false, false, false, false, false,
                63.7D, 64.0D, -0.04D).orElseThrow());
        assertEquals(64.0D, module.surfaceHeight(true, false, false, false, false, false,
                64.0D, 64.0D, 0.0D).orElseThrow());
        assertFalse(module.surfaceHeight(false, false, false, false, false, false,
                63.7D, 64.0D, -0.04D).isPresent());
        assertFalse(module.surfaceHeight(true, true, false, false, false, false,
                63.7D, 64.0D, -0.04D).isPresent());
        assertFalse(module.surfaceHeight(true, false, true, false, false, false,
                64.0D, 64.0D, 0.0D).isPresent());
        assertFalse(module.surfaceHeight(true, false, false, true, false, false,
                63.7D, 64.0D, -0.04D).isPresent());
        assertFalse(module.surfaceHeight(true, false, false, false, true, false,
                63.7D, 64.0D, -0.04D).isPresent());
        assertFalse(module.surfaceHeight(true, false, false, false, false, true,
                63.7D, 64.0D, -0.04D).isPresent());
        assertFalse(module.surfaceHeight(true, false, false, false, false, false,
                62.0D, 64.0D, -0.04D).isPresent());
        assertTrue(module.surfaceHeight(true, false, false, false, false, false,
                63.7D, 64.0D, -0.04D).isPresent());
    }
}
