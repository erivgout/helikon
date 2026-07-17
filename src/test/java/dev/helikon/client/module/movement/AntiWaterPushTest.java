package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiWaterPushTest {
    @Test
    void blocksWaterCurrentOnlyWhileEnabled() {
        AntiWaterPush module = new AntiWaterPush();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertEquals("anti_water_push", module.id());
        assertEquals(ModuleCategory.MOVEMENT, module.category());
        assertFalse(module.defaultEnabled());
        assertFalse(module.blocksWaterCurrent());

        registry.setEnabled(module, true);
        assertTrue(module.blocksWaterCurrent());

        registry.setEnabled(module, false);
        assertFalse(module.blocksWaterCurrent());
    }
}
