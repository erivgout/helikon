package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalGuiPotionSaverTest {
    @Test
    void portalOverrideAndSinglePlayerPauseAreExplicitlyGated() {
        ModuleRegistry registry = new ModuleRegistry();
        PortalGui portal = new PortalGui();
        PotionSaver saver = new PotionSaver();
        registry.register(portal);
        registry.register(saver);
        assertFalse(portal.allowsScreenInPortal(false));
        registry.setEnabled(portal, true);
        assertTrue(portal.allowsScreenInPortal(false));
        registry.setEnabled(saver, true);
        assertFalse(saver.shouldPause(false, true, false, false, 1000));
        assertFalse(saver.shouldPause(true, false, false, false, 1000));
        assertTrue(saver.shouldPause(true, true, false, false, 100));
    }
}
