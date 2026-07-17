package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoShieldOverlayTest {
    @Test
    void hidesOnlyAnActivelyUsedShieldWhileEnabled() {
        NoShieldOverlay module = new NoShieldOverlay();

        assertEquals("no_shield_overlay", module.id());
        assertFalse(module.defaultEnabled());
        assertFalse(module.hidesRaisedShield(true, true));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertTrue(module.hidesRaisedShield(true, true));
        assertFalse(module.hidesRaisedShield(false, true));
        assertFalse(module.hidesRaisedShield(true, false));

        registry.setEnabled(module, false);
        assertFalse(module.hidesRaisedShield(true, true));
    }
}
