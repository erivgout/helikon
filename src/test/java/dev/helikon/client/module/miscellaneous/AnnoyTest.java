package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnoyTest {
    @Test
    void requestsOnlySparseSwingsWhenEnabledWithAPlayerAndNoScreen() {
        Annoy annoy = enabledModule();
        Annoy.Context ready = new Annoy.Context(true, false);

        assertTrue(annoy.shouldSwing(20L, ready));
        assertFalse(annoy.shouldSwing(59L, ready));
        assertTrue(annoy.shouldSwing(60L, ready));
        assertFalse(annoy.shouldSwing(61L, new Annoy.Context(true, true)));
        assertFalse(annoy.shouldSwing(61L, new Annoy.Context(false, false)));
    }

    @Test
    void disabledModuleNeverRequestsASwing() {
        Annoy annoy = new Annoy();

        assertFalse(annoy.shouldSwing(0L, new Annoy.Context(true, false)));
    }

    private static Annoy enabledModule() {
        Annoy annoy = new Annoy();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(annoy);
        registry.setEnabled(annoy, true);
        return annoy;
    }
}
