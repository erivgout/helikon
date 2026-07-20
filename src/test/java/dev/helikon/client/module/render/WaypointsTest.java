package dev.helikon.client.module.render;

import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointsTest {
    @Test
    void defaultsExposeLunarStyleMarkersAsAnIndependentRenderModule() {
        Waypoints module = new Waypoints();

        assertEquals("waypoints", module.id());
        assertTrue(module.defaultEnabled());
        assertTrue(module.labels());
        assertTrue(module.beams());
        assertTrue(module.alwaysOnTop());
        assertEquals(64, module.maximumWaypoints());
        assertEquals(1.0F, module.scale());
        assertEquals(1.0F, module.lineWidth());
        assertEquals(6, module.settings().size());
    }

    @Test
    void rendererFacingValuesFollowModuleSettings() {
        Waypoints module = new Waypoints();
        ((BooleanSetting) setting(module, "labels")).set(false);
        ((BooleanSetting) setting(module, "beams")).set(false);
        ((BooleanSetting) setting(module, "always_on_top")).set(false);
        ((IntegerSetting) setting(module, "maximum_waypoints")).set(12);
        ((NumberSetting) setting(module, "scale")).set(1.5D);
        ((NumberSetting) setting(module, "line_width")).set(2.0D);

        assertFalse(module.labels());
        assertFalse(module.beams());
        assertFalse(module.alwaysOnTop());
        assertEquals(12, module.maximumWaypoints());
        assertEquals(1.5F, module.scale());
        assertEquals(2.0F, module.lineWidth());
    }

    private static dev.helikon.client.setting.Setting<?> setting(Waypoints module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
