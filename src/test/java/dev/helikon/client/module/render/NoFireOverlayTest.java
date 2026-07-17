package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoFireOverlayTest {
    @Test
    void hidesOnlyWhileEnabledAndHasStableRenderMetadata() {
        NoFireOverlay module = new NoFireOverlay();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertEquals("no_fire_overlay", module.id());
        assertEquals("NoFireOverlay", module.name());
        assertFalse(module.defaultEnabled());
        assertFalse(module.hidesFireOverlay());

        registry.setEnabled(module, true);
        assertTrue(module.hidesFireOverlay());

        registry.setEnabled(module, false);
        assertFalse(module.hidesFireOverlay());
    }
}
