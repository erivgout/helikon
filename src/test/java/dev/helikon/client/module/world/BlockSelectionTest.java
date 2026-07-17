package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSelectionTest {
    @Test
    void formatsOnlyRequestedEnabledBoundedLocalDistanceLabels() {
        BlockSelection module = new BlockSelection();
        assertTrue(module.distanceLabel(3.45D).isEmpty());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertEquals("3.5 m", module.distanceLabel(3.45D).orElseThrow());
        assertTrue(module.distanceLabel(Double.NaN).isEmpty());
        assertTrue(module.distanceLabel(257.0D).isEmpty());

        setting(module, "distance_label").set(false);
        assertTrue(module.distanceLabel(3.45D).isEmpty());
        assertFalse(module.options().distanceLabel());
    }

    @Test
    void exposesTheConfiguredRenderOnlyStyle() {
        BlockSelection module = new BlockSelection();
        BlockSelection.Options options = module.options();
        assertEquals(0xFF80CBC4, options.outlineColor());
        assertTrue(options.fill());
        assertEquals(1.0F, options.lineWidth());
    }

    private static BooleanSetting setting(BlockSelection module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
