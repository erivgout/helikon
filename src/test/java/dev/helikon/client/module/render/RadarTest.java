package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarTest {
    @Test
    void persistentMapSettingsHaveStableSafeDefaultsAndVisibility() {
        Radar radar = new Radar();

        assertFalse(radar.minimap());
        assertTrue(radar.saveDiscoveredMap());
        assertEquals(512, radar.mapStorageLimitMb());
        assertFalse(radar.settings().stream().filter(setting -> setting.id().equals("save_discovered_map"))
                .findFirst().orElseThrow().isVisible());
        assertFalse(radar.settings().stream().filter(setting -> setting.id().equals("map_storage_limit_mb"))
                .findFirst().orElseThrow().isVisible());

        @SuppressWarnings("unchecked")
        dev.helikon.client.setting.Setting<Boolean> minimap = (dev.helikon.client.setting.Setting<Boolean>)
                radar.settings().stream().filter(setting -> setting.id().equals("minimap")).findFirst().orElseThrow();
        minimap.set(true);
        assertTrue(radar.settings().stream().filter(setting -> setting.id().equals("save_discovered_map"))
                .findFirst().orElseThrow().isVisible());
        assertTrue(radar.settings().stream().filter(setting -> setting.id().equals("map_storage_limit_mb"))
                .findFirst().orElseThrow().isVisible());
    }
}

