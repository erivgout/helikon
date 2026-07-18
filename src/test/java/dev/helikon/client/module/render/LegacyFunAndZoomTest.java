package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.miscellaneous.LegacyFunModules;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyFunAndZoomTest {
    @Test
    void zoomAndFunEffectsHaveSafeDefaultsAndPureDecisions() {
        Zoom zoom = new Zoom();
        assertFalse(zoom.isEnabled());
        assertEquals(25, zoom.fieldOfView());
        assertEquals(90.0F, zoom.applyTo(90.0F));
        IntegerSetting fov = (IntegerSetting) zoom.settings().getFirst();
        fov.set(10);
        assertEquals(10, zoom.fieldOfView());
        ModuleRegistry zoomRegistry = new ModuleRegistry();
        zoomRegistry.register(zoom);
        zoomRegistry.setEnabled(zoom, true);
        assertEquals(10.0F, zoom.applyTo(90.0F));
        assertEquals(0.0F, zoom.applyTo(0.0F));

        LegacyFunModules.Derp derp = new LegacyFunModules.Derp();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(derp);
        registry.setEnabled(derp, true);
        assertNotEquals(derp.rotation(1), derp.rotation(2));

        LegacyFunModules.MileyCyrus miley = new LegacyFunModules.MileyCyrus();
        assertFalse(miley.shouldSwing(8));
        registry.register(miley);
        registry.setEnabled(miley, true);
        assertTrue(miley.shouldSwing(8));
    }
}
