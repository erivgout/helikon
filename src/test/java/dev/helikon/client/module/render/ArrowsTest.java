package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.render.EntityRenderFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowsTest {
    @Test
    void defaultsAreSafeAndExcludeFriends() {
        Arrows arrows = new Arrows();
        assertEquals("arrows", arrows.id());
        assertEquals(ModuleCategory.RENDER, arrows.category());
        assertFalse(arrows.defaultEnabled(), "an entity-targeting render aid defaults off");

        EntityRenderFilter.Options options = arrows.options();
        assertTrue(options.players());
        assertTrue(options.hostiles());
        assertFalse(options.passive());
        assertFalse(options.friends(), "friends are excluded by default");
        assertEquals(48.0D, options.maximumDistance(), 1.0e-9D);
        assertFalse(options.items());
        assertFalse(options.projectiles());
    }

    @Test
    void exposesValidatedGeometryAndColorChoices() {
        Arrows arrows = new Arrows();
        assertEquals(70.0D, arrows.fieldOfView(), 1.0e-9D);
        assertEquals(40.0D, arrows.ringRadius(), 1.0e-9D);
        assertEquals(9.0D, arrows.arrowLength(), 1.0e-9D);
        assertEquals(4.0D, arrows.arrowHalfWidth(), 1.0e-9D);
        assertEquals(16, arrows.maximumTargets());
        assertEquals(0xFFE84C3D, arrows.color(false));
        assertEquals(0xFF61D17B, arrows.color(true));
    }

    @Test
    void togglesThroughTheRegistryLifecycle() {
        Arrows arrows = new Arrows();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(arrows);

        assertFalse(arrows.isEnabled());
        registry.setEnabled(arrows, true);
        assertTrue(arrows.isEnabled());
        registry.setEnabled(arrows, false);
        assertFalse(arrows.isEnabled());
    }
}
